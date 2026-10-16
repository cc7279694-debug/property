"""Validate final Stage 2 originals, then publish their byte-preserving gallery.

Standard library only. No device, build, Git, image transformation, or acceptance
operation is performed. Run only after the root has pulled the final producers
and captured all final MainActivity originals. Missing provenance is an error.
"""

from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
import math
from pathlib import Path
import re
import struct
import sys
import xml.etree.ElementTree as ET
import zipfile


ROOT = Path(__file__).resolve().parents[2]
BUILD = ROOT / "build/ui-c1-stage2"
DOCS = ROOT / "docs/evidence/ui-c1/stage2"
SCREENS = DOCS / "screenshots"
PHASE = "stage2-final-restored-full"
PACKAGE = "com.cooper.lithic.debug"
PAGES = {
    "now": "现在",
    "project-list": "长期项目列表",
    "project-detail": "长期项目详情",
    "subscription-list": "订阅服务列表",
    "subscription-detail": "订阅服务详情",
    "review": "回看",
    "settings": "设置",
    "add-menu": "新增菜单",
}
PRODUCERS = {
    "subscriptions": (
        "UIC1Stage2EvidenceTest",
        "isolated_subscription_history_operations_and_error_states_supply_stage2_evidence",
    ),
    "empty": (
        "UIC1Stage2EmptyEvidenceTest",
        "isolated_empty_room_home_supplies_final_stage2_dual_theme_evidence_without_writes",
    ),
}
HEX64 = re.compile(r"[0-9a-f]{64}\Z")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def relative(path):
    return path.relative_to(ROOT).as_posix()


def safe_path(path, must_exist=True):
    """Reject links, junctions, and paths outside this worktree."""
    path = Path(path)
    if not path.is_absolute():
        path = ROOT / path
    require(path.is_relative_to(ROOT), f"Path outside worktree: {path}")
    current = ROOT
    for part in path.relative_to(ROOT).parts:
        require(part not in (".", ".."), f"Non-canonical path: {path}")
        current /= part
        require(not current.is_symlink(), f"Symlink is not allowed: {current}")
        require(not getattr(current, "is_junction", lambda: False)(), f"Junction is not allowed: {current}")
    require(path.resolve().is_relative_to(ROOT), f"Resolved path outside worktree: {path}")
    if must_exist:
        require(path.is_file(), f"Missing regular file: {path}")
    return path


def read_json(path):
    path = safe_path(path)
    raw = path.read_bytes()
    value = json.loads(raw.decode("utf-8-sig"))
    require(isinstance(value, dict), f"Expected JSON object: {path}")
    return value, {"file": relative(path), "sha256": sha(raw), "bytes": len(raw)}


def expected_hash(value, label):
    require(isinstance(value, str) and HEX64.fullmatch(value), f"Missing/invalid SHA-256: {label}")
    return value


def verify_blob(path, expected, size=None):
    path = safe_path(path)
    raw = path.read_bytes()
    require(sha(raw) == expected_hash(expected, str(path)), f"SHA-256 mismatch: {path}")
    if size is not None:
        require(len(raw) == size, f"Byte length mismatch: {path}")
    return raw


def png_info(path, metadata):
    raw = verify_blob(path, metadata.get("sha256"), metadata.get("bytes"))
    require(len(raw) >= 33 and raw[:8] == b"\x89PNG\r\n\x1a\n", f"Not a PNG: {path}")
    length, kind, width, height = struct.unpack(">I4sII", raw[8:24])
    require(length == 13 and kind == b"IHDR" and width > 0 and height > 0, f"Invalid PNG IHDR: {path}")
    return raw, width, height


def number(value, label):
    require(not isinstance(value, bool), f"Invalid numeric metadata: {label}")
    try:
        result = float(value)
    except (TypeError, ValueError) as error:
        raise ValueError(f"Missing/invalid numeric metadata: {label}") from error
    require(math.isfinite(result) and result > 0, f"Non-positive numeric metadata: {label}")
    return result


def nonempty(value, label):
    require(isinstance(value, str) and bool(value.strip()), f"Missing metadata: {label}")
    return value


def source_identity():
    summary, proof = read_json(BUILD / f"{PHASE}-summary.json")
    require(summary.get("phase") == PHASE, "Wrong final full-run phase")
    require(summary.get("inputsStable") is True, "Final full-run inputs were not stable")
    head = summary.get("frozenSourceHead")
    require(isinstance(head, str) and re.fullmatch(r"[0-9a-f]{40}", head), "Missing frozen source HEAD")
    inputs = summary.get("sourceInputs")
    require(isinstance(inputs, list) and bool(inputs), "Missing final source inputs")
    require(len({row["path"] for row in inputs}) == len(inputs), "Duplicate source input paths")
    fingerprint = sha(json.dumps(inputs, sort_keys=True, separators=(",", ":")).encode())
    require(fingerprint == summary.get("sourceInputsSha256"), "Source-input fingerprint mismatch")
    for row in inputs:
        verify_blob(ROOT / row["path"], row.get("sha256"), row.get("bytes"))
    archive = safe_path(BUILD / summary["sourceZip"])
    verify_blob(archive, summary.get("sourceZipSha256"))
    with zipfile.ZipFile(archive) as bundle:
        names = bundle.namelist()
        require(len(names) == len(set(names)) and set(names) == {r["path"] for r in inputs}, "Final source ZIP entry set differs")
        for row in inputs:
            raw = bundle.read(row["path"])
            require(len(raw) == row["bytes"] and sha(raw) == row["sha256"], f"Archived source mismatch: {row['path']}")
    executions = summary.get("actualCommandExecutions", [])
    require(len(executions) == 3, "Final full run must contain all three command executions")
    require(all(row.get("inputsStable") is True for row in executions), "An actual command used unstable inputs")
    android = [row for row in executions if ":app:connectedDebugAndroidTest" in row.get("command", [])]
    require(len(android) == 1, "Expected one final unfiltered connected test command")
    command = android[0]["command"]
    require(not any("InstrumentationRunnerArguments" in arg or arg == "--tests" for arg in command), "Filtered Android run cannot supply final evidence")
    for row in executions:
        verify_blob(BUILD / row["log"], row.get("logSha256"))
    apk, apk_proof = read_json(BUILD / "final-apk.json")
    final_apk = expected_hash(apk.get("sha256"), "final APK")
    final_test_apk = expected_hash(apk.get("androidTestApkSha256"), "final androidTest APK")
    require(apk.get("installedApkSha256") == final_apk, "Final APK verification did not prove installed APK identity")
    apk_path = Path(apk.get("apk", str(BUILD / "lithic-ui-c1-stage2-debug.apk")))
    verify_blob(apk_path, final_apk, apk.get("bytes"))
    verify_blob(ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk", final_test_apk, apk.get("androidTestApkBytes"))
    identity = {
        "apkSha256": final_apk,
        "androidTestApkSha256": final_test_apk,
        "sourceInputsSha256": fingerprint,
        "frozenSourceHead": head,
        "fullRunSummary": proof,
        "apkVerification": apk_proof,
        "fullRunActualResult": {key: summary.get(key) for key in ("phase", "total", "jvm", "android", "pass", "fail", "skipped", "overall")},
    }
    return summary, identity, {row["path"]: row for row in inputs}


def junit_proofs(summary):
    rows = []
    report_proofs = []
    for report in summary.get("reports", []):
        path = safe_path(BUILD / report["path"])
        raw = verify_blob(path, report.get("sha256"), report.get("bytes"))
        tree = ET.fromstring(raw)
        device = [p.get("value") for p in tree.findall("./properties/property") if p.get("name") == "device"]
        proof = {"file": relative(path), "sha256": sha(raw), "bytes": len(raw), "devices": device}
        report_proofs.append(proof)
        for case in tree.iter("testcase"):
            result = "FAIL" if case.find("failure") is not None or case.find("error") is not None else "SKIPPED" if case.find("skipped") is not None else "PASS"
            rows.append({"kind": report["kind"], "class": case.get("classname"), "name": case.get("name"), "result": result, "report": proof})
    actual = Counter((r["kind"], r["class"], r["name"], r["result"]) for r in rows)
    described = Counter((r["kind"], r["class"], r["name"], r["result"]) for r in summary.get("cases", []))
    require(actual == described and sum(actual.values()) == summary.get("total"), "Final summary and JUnit method results differ")
    proofs = {}
    for part, (class_name, method) in PRODUCERS.items():
        matches = [row for row in rows if row["kind"] == "android" and row["class"] == f"com.cooper.lithic.ui.{class_name}" and row["name"] == method]
        require(len(matches) == 1 and matches[0]["result"] == "PASS", f"Final producer method is not one actual JUnit PASS: {part}")
        require(bool(matches[0]["report"]["devices"]), f"Missing actual JUnit device metadata: {part}")
        proofs[part] = matches[0]
    return proofs, report_proofs


def effective_wm(value, label, pattern):
    value = nonempty(value, label)
    physical = re.findall(r"Physical " + pattern, value)
    override = re.findall(r"Override " + pattern, value)
    require(bool(physical), f"Missing physical {label}: {value}")
    return (override or physical)[-1]


def main_image(path, identity, normal_names):
    meta, meta_proof = read_json(BUILD / f"{path.name}.json")
    require(meta.get("file") == relative(path), f"Capture file metadata mismatch: {path.name}")
    raw, width, height = png_info(path, meta)
    require(meta.get("installedApkSha256") == identity["apkSha256"], f"MainActivity installed APK differs: {path.name}")
    require(str(meta.get("api")) == "36", f"MainActivity device is not API 36: {path.name}")
    require("unmodified adb screencap" in nonempty(meta.get("source"), "Main capture source") and "MainActivity" in meta["source"], f"Unexpected capture source: {path.name}")
    require("synthetic QA ledger" in meta["source"], f"Unknown MainActivity data source: {path.name}")
    focus = meta.get("focusedWindow")
    resumed = meta.get("resumed")
    require(isinstance(focus, list) and isinstance(resumed, list), f"Missing focus/resumed arrays: {path.name}")
    hosts = re.findall(re.escape(PACKAGE) + r"/[A-Za-z0-9_.$]+MainActivity", "\n".join(resumed))
    require(bool(hosts), f"MainActivity is not resumed: {path.name}")
    focused_main = any("mCurrentFocus=" in line and PACKAGE + "/" in line and "MainActivity" in line for line in focus)
    popup = meta.get("popupOwner")
    require(focused_main or (isinstance(popup, str) and "package=" + PACKAGE + " " in popup and any("mFocusedApp=" in line and PACKAGE + "/" in line and "MainActivity" in line for line in focus)), f"No MainActivity/owned-popup focus proof: {path.name}")
    dimensions = effective_wm(meta.get("size"), "size", r"size:\s*(\d+)x(\d+)")
    require((width, height) == tuple(map(int, dimensions)), f"PNG and effective wm size differ: {path.name}")
    dpi = int(effective_wm(meta.get("density"), "density", r"density:\s*(\d+)"))
    require(dpi > 0, f"Invalid effective density: {path.name}")
    mode = re.fullmatch(r"(?:Night mode:\s*)?(no|yes)\s*", nonempty(meta.get("night"), "actual night mode"), re.IGNORECASE)
    require(mode is not None, f"Ambiguous actual system theme: {path.name}")
    theme = "dark" if mode[1].lower() == "yes" else "light"
    suffix = re.search(r"-(light|dark)\.png$", path.name)
    if suffix:
        require(suffix[1] == theme, f"Filename and actual system theme differ: {path.name}")
    font = number(meta.get("fontScale"), "system fontScale")
    normal = path.name in normal_names
    if normal:
        require(math.isclose(font, 1.0, abs_tol=0.001), f"Mandatory normal image does not use normal font: {path.name}")
    stem = path.stem.removeprefix("final-")
    page_key = stem.rsplit("-", 1)[0] if normal else None
    return {
        "file": relative(path), "sha256": sha(raw), "bytes": len(raw),
        "evidenceType": "EXISTING_QA_MAINACTIVITY", "mandatoryNormalImage": normal,
        "page": PAGES[page_key] if normal else path.stem,
        "pageLabelSource": "Mandatory page capture name" if normal else "Literal extra capture name; content requires direct visual inspection",
        "state": "Existing synthetic QA ledger; exact visible viewport preserved in original PNG",
        "theme": theme, "themeSource": "ACTUAL_SYSTEM_NIGHT_MODE_IN_MAINACTIVITY_CAPTURE",
        "systemNightModeRaw": meta["night"], "systemFontScale": font,
        "composeLocalFontScaleOverride": None,
        "widthPixels": width, "heightPixels": height, "densityDpi": dpi,
        "widthDp": width * 160 / dpi, "heightDp": height * 160 / dpi,
        "dpSource": "Original PNG IHDR and effective physical/override wm density",
        "api": 36, "device": nonempty(meta.get("device"), "MainActivity device"),
        "avd": nonempty(meta.get("avd"), "MainActivity AVD"),
        "actualHostActivity": hosts[0], "foregroundProof": {"focusedWindow": focus, "resumed": resumed, "popupOwner": popup},
        "capturedGuestUtc": nonempty(meta.get("guestUtc"), "MainActivity capture time"),
        "originalMetadata": meta, "originalMetadataProof": meta_proof,
        "binding": dict(identity, apkBindingSource="Per-capture installedApkSha256 equals final APK", androidTestApkRole="Final regression artifact; this capture is MainActivity, not androidTest-hosted"),
        "visualReview": "PENDING_DIRECT_ORIGINAL_IMAGE_REVIEW",
    }


def producer_images(part, proof, identity, input_rows):
    folder = BUILD / "final-producers" / part
    fixture, fixture_proof = read_json(folder / "fixture-source.json")
    require(all(fixture.get(key) is False for key in ("productionRecordSource", "fixtureUsesActiveLedger", "fixtureWritesActiveLedger")), f"Fixture is not isolated from the active ledger: {part}")
    class_name, method = PRODUCERS[part]
    source_path = f"app/src/androidTest/java/com/cooper/lithic/ui/{class_name}.kt"
    source = safe_path(ROOT / source_path).read_text(encoding="utf-8")
    require(source_path in input_rows, f"Producer missing from final input set: {part}")
    require(re.search(r"@Test\s+fun\s+" + re.escape(method) + r"\s*\(", source), f"Producer @Test method differs from final source: {part}")
    require("LithicTheme(darkTheme = dark)" in source and 'dark = theme == "dark"' in source, f"Cannot establish explicit Compose theme from producer source: {part}")
    require("LocalDensity" not in source, f"Unrecorded Compose font override in producer source: {part}")
    captures = fixture.get("captures")
    require(isinstance(captures, list), f"Missing capture metadata: {part}")
    if part == "subscriptions":
        expected = {f"subscription-{name}-{theme}.png" for name in ("list", "detail", "stale-error", "read-error", "cover-error") for theme in ("light", "dark")}
        expected |= {f"{name}-light.png" for name in ("subscription-payment-history", "subscription-delete-payment", "subscription-history", "project-history")}
        require(fixture.get("source") == "ISOLATED_ANDROID_TEST_HOST", "Wrong subscription fixture source")
        require(fixture.get("fixtureRowsCoverBytesAndRecordZoneUnchangedAfterCaptures") is True, "Missing unchanged fixture facts/covers/zone assertion")
        require(bool(fixture.get("injectedDisplayOnlyCases")), "Missing injected-state limitation")
    else:
        expected = {"empty-home-light.png", "empty-home-dark.png"}
        require(fixture.get("source") == "ISOLATED_EMPTY_ROOM_NAVIGATION_HOST", "Wrong empty fixture source")
        require(fixture.get("projectCount") == fixture.get("subscriptionCount") == 0, "Fixture is not genuinely empty")
        require(all(fixture.get(k) is True for k in ("emptyFactsAndSettingsUnchangedAfterCaptures", "originalActivePointerUnchanged", "ownUuidRootRemoved")), "Missing empty fixture protection assertions")
    names = [capture.get("file") for capture in captures]
    require(len(names) == len(set(names)) and set(names) == expected, f"Unexpected producer capture set/count: {part}")
    require({p.name for p in folder.glob("*.png")} == expected, f"Unexpected/missing producer original PNG files: {part}")
    rows, copies = [], []
    for meta in captures:
        name = meta["file"]
        require(Path(name).name == name, f"Nested producer image path: {name}")
        original = safe_path(folder / name)
        raw, width, height = png_info(original, meta)
        require((width, height) == (meta.get("widthPixels"), meta.get("heightPixels")), f"Producer IHDR and metadata dimensions differ: {name}")
        require(meta.get("sdkInt") == 36, f"Producer is not API 36: {name}")
        dpi = number(meta.get("densityDpi"), "producer densityDpi")
        width_dp = number(meta.get("widthDp"), "producer widthDp")
        require(math.isclose(width_dp, width * 160 / dpi, abs_tol=0.01), f"Producer dp width mismatch: {name}")
        font = number(meta.get("fontScale"), "producer resource fontScale")
        theme = meta.get("theme")
        require(theme in ("light", "dark") and name.endswith(f"-{theme}.png"), f"Producer theme mismatch: {name}")
        host = nonempty(meta.get("actualHostActivity"), "actual producer host")
        require(host.startswith(PACKAGE + "/") and "MainActivity" not in host, f"Isolated producer mislabelled as MainActivity: {name}")
        require(bool(meta.get("generationId")), f"Missing generation identity: {name}")
        scenario = nonempty(meta.get("scenario"), "producer scenario")
        source_kind = nonempty(meta.get("source"), "producer source")
        if part == "empty":
            require(meta.get("installedApkSha256") == identity["apkSha256"], f"Empty host installed APK differs: {name}")
            require(meta.get("actualForegroundHostActivity") == host, f"Empty host foreground identity differs: {name}")
            require(meta.get("themeSource") == "EXPLICIT_COMPOSE_THEME_IN_TEST_HOST", f"Empty theme provenance differs: {name}")
            require(meta.get("state") == "GENUINELY_EMPTY_ROOM", f"Empty state provenance differs: {name}")
            require(source_kind == "ISOLATED_EMPTY_ROOM_NAVIGATION_HOST", f"Empty source provenance differs: {name}")
            require("systemNightMode" in meta, f"Missing empty actual system configuration: {name}")
            nonempty(meta.get("device"), "empty Build.DEVICE")
            evidence_type = "ISOLATED_EMPTY_ROOM_TEST_HOST"
            limitation = "真实空 Room，经生产 readHomeOverview/Repository 读取；显式 Compose 主题测试宿主，未使用真实账本。"
        elif "stale-error" in name or "read-error" in name:
            expected_scenario = "INJECTED_ERROR_WITH_REPOSITORY_ROWS" if "stale-error" in name else "INJECTED_ERROR_WITH_NO_CONFIRMED_ROWS"
            require(source_kind == "ISOLATED_PRESENTATION_HOST" and scenario == expected_scenario, f"Injected read-error provenance differs: {name}")
            evidence_type = "SYNTHETIC_PRESENTATION_STATE_TEST_HOST"
            limitation = "只注入读取错误展示态；不是实际磁盘故障、损坏或真实 MainActivity 读取失败。"
        elif "cover-error" in name:
            require(source_kind == "ISOLATED_PRESENTATION_HOST" and scenario == "UNPERSISTED_MISSING_COVER_REFERENCE_ON_REPOSITORY_DETAIL", f"Missing-cover provenance differs: {name}")
            evidence_type = "SYNTHETIC_PRESENTATION_STATE_TEST_HOST"
            limitation = "在真实隔离 Repository 详情上注入未持久化的缺失封面引用；不是实际丢图、磁盘故障或数据损坏。"
        else:
            require(source_kind == "ISOLATED_NAVIGATION_HOST" and scenario == "REPOSITORY_FACTS", f"Navigation fixture provenance differs: {name}")
            evidence_type = "ISOLATED_SYNTHETIC_ROOM_TEST_HOST"
            limitation = "隔离合成 Room，使用生产 Repository/领域引擎/导航；不是原有真实账本或 MainActivity。"
        destination = safe_path(SCREENS / f"final-isolated-{part}-{name}", must_exist=False)
        if destination.exists():
            require(destination.read_bytes() == raw, f"Existing isolated original differs; refusing overwrite: {destination}")
        copies.append((destination, raw))
        rows.append({
            "file": relative(destination), "sha256": sha(raw), "bytes": len(raw),
            "evidenceType": evidence_type, "page": nonempty(meta.get("page"), "producer page"),
            "state": meta.get("state", scenario), "scenario": scenario,
            "theme": theme, "themeSource": "EXPLICIT_COMPOSE_THEME_IN_TEST_HOST",
            "themeSourceProof": {"sourceFile": source_path, "sourceFileSha256": input_rows[source_path]["sha256"], "statement": "LithicTheme(darkTheme = dark); dark = theme == dark (theme string)"},
            "systemNightMode": meta.get("systemNightMode"),
            "systemNightModeLimitation": None if "systemNightMode" in meta else "Not recorded by this producer; explicit Compose theme is recorded separately",
            "systemFontScale": font, "fontScaleSource": "Activity/context resources.configuration; no producer LocalDensity override",
            "composeLocalFontScaleOverride": None,
            "widthPixels": width, "heightPixels": height, "densityDpi": dpi,
            "widthDp": width_dp, "heightDp": height * 160 / dpi,
            "api": 36, "device": proof["report"]["devices"], "deviceSource": "Actual final unfiltered producer JUnit XML device property",
            "deviceBuildMetadata": {k: meta[k] for k in ("device", "model", "manufacturer") if k in meta},
            "actualHostActivity": host, "actualForegroundHostActivity": meta.get("actualForegroundHostActivity"),
            "foregroundProofLimitation": "Exact foreground host and focus asserted by empty producer" if part == "empty" else "Producer records host component and waits for target package foreground; it does not embed a per-image Activity-focus assertion",
            "sourceOriginal": relative(original), "originalMetadata": meta,
            "fixtureSourceProof": fixture_proof, "producerJUnit": proof,
            "binding": dict(identity, apkBindingSource="Embedded installedApkSha256 and final producer JUnit" if part == "empty" else "External join to this producer PASS in final unfiltered JUnit; subscription JSON does not embed APK/input hashes"),
            "limitation": limitation, "pixelHandling": "Raw original PNG bytes copied unchanged; no decoding, resize, edit, or composition",
            "visualReview": "PENDING_DIRECT_ORIGINAL_IMAGE_REVIEW",
        })
    return rows, copies, {"part": part, "proof": fixture_proof, "metadata": fixture, "producerJUnit": proof}


def caption(row):
    dp = f"{row['widthDp']:.2f}"
    return (f"{row['theme']}；{row['widthPixels']}×{row['heightPixels']} px；{dp}dp；"
            f"{row['densityDpi']:g}dpi；字体 {row['systemFontScale']:g}；API {row['api']}；"
            f"宿主 `{row['actualHostActivity']}`；来源 `{row['evidenceType']}`")


def image_block(row):
    image = "screenshots/" + Path(row["file"]).name
    return (f"### {row['page']} — {row['theme']}\n\n"
            f"![{Path(image).name}]({image})\n\n"
            f"{caption(row)}。\n\n"
            f"{row.get('limitation', 'MainActivity 原有合成 QA 账本。附图名称仅标识采集意图，内容与交互结论须逐张核验。')}\n\n"
            f"原文件 `{Path(image).name}`；SHA-256 `{row['sha256']}`。\n\n")


def main():
    summary, identity, inputs = source_identity()
    producers, report_proofs = junit_proofs(summary)
    normal_names = {f"final-{page}-{theme}.png" for page in PAGES for theme in ("light", "dark")}
    require(SCREENS.is_dir(), "Final screenshot directory is missing")
    main_paths = sorted(p for p in SCREENS.glob("final-*.png") if not p.name.startswith("final-isolated-"))
    require(normal_names <= {p.name for p in main_paths}, "Mandatory final 8-page × light/dark normal-font originals are missing")
    rows = [main_image(safe_path(path), identity, normal_names) for path in main_paths]
    pressure = [r for r in rows if math.isclose(r["widthDp"], 360, abs_tol=0.01) and math.isclose(r["systemFontScale"], 2, abs_tol=0.001)]
    require({r["theme"] for r in pressure} == {"light", "dark"}, "Missing actual 360dp / system fontScale 2.0 MainActivity evidence in both themes")
    fixtures, copies = [], []
    for part in PRODUCERS:
        isolated_rows, isolated_copies, fixture = producer_images(part, producers[part], identity, inputs)
        rows.extend(isolated_rows)
        copies.extend(isolated_copies)
        fixtures.append(fixture)
    expected_isolated = {path.name for path, _ in copies}
    existing_isolated = {p.name for p in SCREENS.glob("final-isolated-*.png")}
    require(existing_isolated <= expected_isolated, "Unexpected final-isolated PNG from another producer/run; refusing to include or remove it")
    gallery_path = safe_path(DOCS / "FINAL_GALLERY.md", must_exist=False)
    manifest_path = safe_path(DOCS / "final-screenshot-manifest.json", must_exist=False)
    result = identity["fullRunActualResult"]
    manifest = {
        "schemaVersion": 1, "generatedUtc": datetime.now(timezone.utc).isoformat(),
        "purpose": "Final Stage 2 evidence gallery; metadata/byte validation only, not visual acceptance",
        "identity": identity, "validatedJUnitReports": report_proofs,
        "mandatoryMainActivityOriginals": 16,
        "extraMainActivityOriginals": sum(r["evidenceType"] == "EXISTING_QA_MAINACTIVITY" and not r["mandatoryNormalImage"] for r in rows),
        "isolatedProducerOriginals": len(copies), "totalOriginals": len(rows),
        "integrityValidation": "All listed source/archive/log/JUnit/APK/metadata/image hashes and original PNG IHDR dimensions checked",
        "visualReview": "PENDING_DIRECT_ORIGINAL_IMAGE_REVIEW_AGAINST_BOTH_CONFIRMED_DESIGN_REFERENCES",
        "acceptance": "NOT_GRANTED", "fixtures": fixtures, "images": rows,
        "limitations": [
            "Each image still requires direct inspection of its unedited original; this script cannot establish UI quality or interaction success.",
            "The final full-suite result is preserved verbatim, including failures; passing producer methods do not make the suite pass.",
            "MainActivity, isolated navigation, injected presentation states, and genuinely empty Room hosts are separate evidence types.",
            "Subscription producer APK/source/device identity is an explicit external join to final JUnit; no nonexistent embedded metadata is invented.",
            "Isolated themes are explicit Compose choices, and do not establish actual system dark/light mode.",
            "IME, submission, scroll reachability, dangerous confirmations, and error semantics require the root's runtime/visual observations; filenames alone do not prove them.",
            "No Stage 1, refinement, Task 6, or task7-producer-first originals are included.",
        ],
    }
    text = (
        "# UI-C1 阶段2最终原图画廊\n\n"
        "状态：**待逐张直接看图与独立视觉验收**。本文件只归集并校验原图及来源，"
        "不授予视觉 PASS、UI-C1 接受或 M10 接受。\n\n"
        f"最终源码 HEAD `{identity['frozenSourceHead']}`；输入指纹 `{identity['sourceInputsSha256']}`。\n\n"
        f"最终 Debug APK SHA-256 `{identity['apkSha256']}`；androidTest APK SHA-256 `{identity['androidTestApkSha256']}`。\n\n"
        f"最终无过滤运行 `{PHASE}`：{result['total']} 方法，{result['pass']} PASS、{result['fail']} FAIL、"
        f"{result['skipped']} SKIPPED；整体 `{result['overall']}`。这里只要求两个截图 producer 方法在该运行中确实 PASS，"
        "不会把图像产生成功当作整体回归成功。\n\n"
        "[逐图完整登记](final-screenshot-manifest.json)包含每图原始元数据、图片/最终 APK/测试 APK/输入指纹、"
        "设备/API、px/dp、字体、主题、实际宿主和 JUnit/fixture 哈希。\n\n"
        "## 16张正常字体 MainActivity 原图\n\n"
        "以下为最终已安装 APK 的真实 MainActivity，数据是原有合成 QA 账本。"
        "Light/Dark 来自每次采集的真实系统 night 配置，字体为实际系统比例1.0。\n\n"
        "| 页面 | Light | Dark |\n|---|---|---|\n"
    )
    by_name = {Path(r["file"]).name: r for r in rows}
    for page, label in PAGES.items():
        text += f"| {label} | ![{label} light](screenshots/final-{page}-light.png) | ![{label} dark](screenshots/final-{page}-dark.png) |\n"
    text += "\n### 正常原图逐图来源\n\n"
    for page in PAGES:
        for theme in ("light", "dark"):
            row = by_name[f"final-{page}-{theme}.png"]
            text += f"- `{Path(row['file']).name}`：{caption(row)}；SHA-256 `{row['sha256']}`。\n"
    text += "\n## MainActivity 附加原图\n\n真实系统360dp／200%图按逐图元数据识别。文件名表示采集意图；真实IME、错误、滚动、表单与危险操作可达结论须结合运行观察逐项确认。\n\n"
    for row in rows:
        if row["evidenceType"] == "EXISTING_QA_MAINACTIVITY" and not row["mandatoryNormalImage"]:
            text += image_block(row)
    text += "## 隔离合成 Room／展示态补图\n\n以下14张来自最终无过滤运行的 `UIC1Stage2EvidenceTest`，宿主身份按原始元数据登记。"
    text += "主题为显式 Compose 主题，不能写成系统深浅切换或 MainActivity。订阅 producer 未内嵌 APK/输入哈希，"
    text += "本登记显式通过最终 JUnit 中的确切 producer 方法 PASS、最终测试 APK 与源码输入建立外部关联。\n\n"
    for row in rows:
        if row["evidenceType"] in ("ISOLATED_SYNTHETIC_ROOM_TEST_HOST", "SYNTHETIC_PRESENTATION_STATE_TEST_HOST"):
            text += image_block(row)
    text += "## 真实空 Room 测试宿主\n\n以下2张来自最终 `UIC1Stage2EmptyEvidenceTest`；原始 JSON 内嵌的已安装 APK 哈希必须与最终 APK 相同，"
    text += "前台宿主、真正空的生产读取结果、原有指针不变与自身UUID目录清理断言均来自该 producer。"
    text += "主题为显式 Compose 主题；同时保留原始系统 uiMode 数值，不把二者混同。\n\n"
    for row in rows:
        if row["evidenceType"] == "ISOLATED_EMPTY_ROOM_TEST_HOST":
            text += image_block(row)
    text += "## 原图与验收边界\n\n"
    text += "全部 PNG 按原字节保留；隔离图只改目标文件名复制，不解码、缩放、修图或拼图。"
    text += "读取错误与缺失封面是明确标注的展示态注入，不代表真实磁盘故障、文件损坏或正式账本错误。\n\n"
    text += "必须逐张打开原图，直接对照 [Light原稿](../../../design/ui-c1/source-v1/ui-reference-light.png) 与 "
    text += "[Dark原稿](../../../design/ui-c1/source-v1/ui-reference-dark.png)，检查布局、间距、封面、金额、状态、按钮、导航与一致性。"
    text += "脚本未执行这项视觉工作；全部逐图视觉审查状态仍为 PENDING。\n"
    manifest_raw = (json.dumps(manifest, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    # Validate everything above before creating any published derived artifact.
    for destination, raw in copies:
        if not destination.exists():
            with destination.open("xb") as output:
                output.write(raw)
        require(destination.read_bytes() == raw, f"Raw-byte copy verification failed: {destination}")
    manifest_path.write_bytes(manifest_raw)
    gallery_path.write_text(text, encoding="utf-8")
    print(json.dumps({"gallery": relative(gallery_path), "manifest": relative(manifest_path), "originals": len(rows), "mandatoryMain": 16, "isolated": len(copies), "visualReview": "PENDING", "acceptance": "NOT_GRANTED"}, ensure_ascii=False))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, ET.ParseError, json.JSONDecodeError, zipfile.BadZipFile) as error:
        print(f"Final gallery was not published: {error}", file=sys.stderr)
        raise SystemExit(1) from error
