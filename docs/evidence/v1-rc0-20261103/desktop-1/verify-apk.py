"""RC0 Preview17 APK static audit. No build, archive, install, or device access.

Reuse accepted SDK/DEX identity readers and compare unchanged R1 methods with
the locked Preview16 APK; static equivalence is not Android runtime evidence.
"""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import os
import re
import struct
import subprocess
import traceback
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[4]
EV = ROOT / "docs/evidence/v1-rc0-20261103/desktop-1"
BASELINE = "0e9b3610ad306ffd9b266022a672c580945fbc1c"
SDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/sdk")
TOOLS = SDK / "build-tools/36.0.0"
ANALYZER = SDK / "cmdline-tools/latest/bin/apkanalyzer.bat"
CERT = "29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9"
PACKAGE = "com.cooper.lithic.preview.debug"
PREVIEW = "com.cooper.lithic.preview.debug"
NS = "{http://schemas.android.com/apk/res/android}"
ENV = dict(os.environ)
ENV["JAVA_HOME"] = "C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1"
ENV["PATH"] = ENV["JAVA_HOME"] + "/bin;" + ENV["PATH"]
ARGS = argparse.ArgumentParser(description=__doc__)
ARGS.add_argument("--build-receipt", required=True, type=Path)
ARGS.add_argument("--evidence-name", default="artifact")
args = ARGS.parse_args()
assert re.fullmatch(r"artifact(?:-[0-9]+)?", args.evidence_name), "Use a new bounded evidence directory"
OUT = EV / args.evidence_name
OUT.mkdir(exist_ok=False)
COMMANDS = []


def metadata(path):
    data = path.read_bytes()
    return {"path": path.relative_to(ROOT).as_posix(), "bytes": len(data),
            "sha256": hashlib.sha256(data).hexdigest()}


def write_json(path, value):
    path.write_bytes((json.dumps(value, indent=2, ensure_ascii=True) + "\n").encode("utf-8"))


def capture(label, argv):
    argv = [str(x) for x in argv]
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    row = {"label": label, "argv": argv, "startedUtc": started}
    try:
        result = subprocess.run(argv, cwd=ROOT, env=ENV, capture_output=True, timeout=120)
        stdout, stderr = result.stdout, result.stderr
        row["exitCode"] = result.returncode
    except BaseException as error:
        stdout = getattr(error, "stdout", None) or b""
        stderr = getattr(error, "stderr", None) or b""
        row.update(exitCode=None, exceptionType=type(error).__name__, exception=str(error))
        raise
    finally:
        row["finishedUtc"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
        for channel, data in (("stdout", stdout), ("stderr", stderr)):
            path = OUT / (label + "." + channel + ".log")
            path.write_bytes(data)
            row[channel] = metadata(path)
        COMMANDS.append(row)
        write_json(OUT / "commands.json", COMMANDS)
    assert result.returncode == 0, (label, result.returncode)
    return result.stdout.decode("utf-8", errors="strict")


def dex_metadata(data):
    """Read only class/method identities and BuildConfig's encoded boolean value."""
    assert data[:4] == b"dex\n" and len(data) >= 112

    def checked(pos, size):
        assert 0 <= pos <= len(data) and 0 <= size <= len(data) - pos
        return pos

    def u32(pos):
        return struct.unpack_from("<I", data, checked(pos, 4))[0]

    def uleb(pos):
        value = 0
        for shift in range(0, 35, 7):
            item = data[checked(pos, 1)]
            pos += 1
            value |= (item & 127) << shift
            if not item & 128:
                assert value <= 0xffffffff
                return value, pos
        raise AssertionError("oversized ULEB128")

    assert u32(32) == len(data)

    def table(count_pos, offset_pos, stride):
        count, offset = u32(count_pos), u32(offset_pos)
        checked(offset, count * stride)
        return count, offset

    count, offset = table(0x38, 0x3c, 4)
    strings = []
    for index in range(count):
        pos = u32(offset + index * 4)
        _, pos = uleb(pos)
        end = data.index(b"\0", pos)
        strings.append(data[pos:end].decode("utf-8", errors="replace"))
    count, offset = table(0x40, 0x44, 4)
    types = [strings[u32(offset + index * 4)] for index in range(count)]
    count, offset = table(0x50, 0x54, 8)
    fields = []
    for index in range(count):
        owner, field_type, name = struct.unpack_from("<HHI", data, offset + index * 8)
        fields.append((types[owner], types[field_type], strings[name]))
    count, offset = table(0x58, 0x5c, 8)
    methods = set()
    for index in range(count):
        owner, _, name = struct.unpack_from("<HHI", data, offset + index * 8)
        methods.add((types[owner], strings[name]))

    def encoded(pos, depth=0):
        assert depth < 32
        header = data[checked(pos, 1)]
        pos += 1
        kind, argument = header & 31, header >> 5
        if kind == 0x1f:
            assert argument in (0, 1)
            return bool(argument), pos
        if kind == 0x1e:
            assert argument == 0
            return None, pos
        if kind == 0x1c:
            assert argument == 0
            size, pos = uleb(pos)
            for _ in range(size):
                _, pos = encoded(pos, depth + 1)
            return None, pos
        if kind == 0x1d:
            assert argument == 0
            _, pos = uleb(pos)
            size, pos = uleb(pos)
            for _ in range(size):
                _, pos = uleb(pos)
                _, pos = encoded(pos, depth + 1)
            return None, pos
        assert kind in (0x00, 0x02, 0x03, 0x04, 0x06, 0x10, 0x11, 0x15, 0x16,
                        0x17, 0x18, 0x19, 0x1a, 0x1b), kind
        checked(pos, argument + 1)
        return None, pos + argument + 1

    count, offset = table(0x60, 0x64, 32)
    classes, build_flag = set(), None
    for index in range(count):
        pos = offset + index * 32
        descriptor = types[u32(pos)]
        classes.add(descriptor)
        if descriptor != "Lcom/cooper/lithic/BuildConfig;":
            continue
        class_data, values = u32(pos + 24), u32(pos + 28)
        assert class_data and values
        static_count, cursor = uleb(class_data)
        for _ in range(3):
            _, cursor = uleb(cursor)
        static_fields, field_index = [], 0
        for _ in range(static_count):
            delta, cursor = uleb(cursor)
            _, cursor = uleb(cursor)
            field_index += delta
            static_fields.append(fields[field_index])
        value_count, cursor = uleb(values)
        assert value_count <= len(static_fields)
        for field in static_fields[:value_count]:
            value, cursor = encoded(cursor)
            if field == (descriptor, "Z", "PHOTO_LITE_EXPERIMENT"):
                assert type(value) is bool
                build_flag = value
    return classes, methods, build_flag


def apk_metadata(apk):
    classes, methods, flags = set(), set(), []
    with zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            if re.fullmatch(r"classes\d*\.dex", name):
                found_classes, found_methods, flag = dex_metadata(archive.read(name))
                assert not classes.intersection(found_classes), "duplicate DEX class"
                classes |= found_classes
                methods |= found_methods
                if flag is not None:
                    flags.append(flag)
        native = {name: hashlib.sha256(archive.read(name)).hexdigest()
                  for name in archive.namelist() if name.startswith("lib/") and name.endswith(".so")}
    return classes, methods, flags, native


def components(manifest, package):
    keys = ("name", "exported", "process", "isolatedProcess", "permission", "directBootAware", "authorities")
    found = []
    for element in manifest.find("application"):
        if element.tag in ("activity", "activity-alias", "provider", "service", "receiver"):
            attrs = {key: element.get(NS + key) for key in keys if element.get(NS + key) is not None}
            attrs = {key: value.replace(package, "{applicationId}") for key, value in attrs.items()}
            found.append({"kind": element.tag, **attrs})
    return sorted(found, key=lambda item: (item["kind"], item.get("name", "")))


def git_text(*arguments):
    return subprocess.check_output(["git", *arguments], cwd=ROOT, text=True, encoding="utf-8").strip()


def saved_text(name, value):
    path = OUT / name
    path.write_bytes(value.encode("utf-8"))
    return metadata(path)


def method_block(code, method):
    starts = list(re.finditer(r"(?m)^\s*\.method[^\r\n]*\s" + re.escape(method) + r"\([^\r\n]*\r?\n", code))
    assert len(starts) == 1, (method, "Expected one actual smali method", len(starts))
    end = re.search(r"(?m)^\s*\.end method\s*$", code[starts[0].end():])
    assert end is not None, (method, "Missing method terminator")
    return code[starts[0].start(): starts[0].end() + end.end()]


def handler_observations(method):
    # Trace only straight-line handler instructions and explicit goto edges.
    # Nested try/use exception coverage still requires the supervisor's review.
    lines = method.splitlines()
    labels = {}
    for index, line in enumerate(lines):
        found = re.fullmatch(r"\s*(:[\w]+)\s*(?:#.*)?", line)
        if found:
            label = found.group(1)
            assert label not in labels, "Duplicate smali label"
            labels[label] = index

    def trace_handler(target):
        assert target in labels, "Missing exception handler label"
        cursor = labels[target] + 1
        visited = set()
        instructions = []
        visited_labels = [target]
        for _ in range(4096):
            assert 0 <= cursor < len(lines), "Handler ended before a terminal throw"
            assert cursor not in visited, "Loop in exception handler"
            visited.add(cursor)
            line = lines[cursor].strip()
            cursor += 1
            if not line or line.startswith("#"):
                continue
            if line.startswith(".end method"):
                raise AssertionError("Handler ended before a terminal throw")
            if line.startswith("."):
                continue
            label = re.fullmatch(r"(:[\w]+)\s*(?:#.*)?", line)
            if label:
                visited_labels.append(label.group(1))
                continue
            assert len(instructions) < 128, "Exception handler instruction budget exceeded"
            operation = line.split(None, 1)[0]
            if operation.startswith("if-") or operation in ("packed-switch", "sparse-switch", "fill-array-data"):
                raise AssertionError("Branching exception handler requires explicit review")
            if operation.startswith("return"):
                raise AssertionError("Exception handler returned instead of throwing")
            instructions.append(line)
            if operation == "throw":
                return "\n".join(instructions) + "\n", visited_labels
            if operation.startswith("goto"):
                jump = re.fullmatch(r"goto(?:/16|/32)?\s+(:[\w]+)\s*(?:#.*)?", line)
                assert jump is not None and jump.group(1) in labels, "Unknown handler goto destination"
                cursor = labels[jump.group(1)]
        raise AssertionError("Exception handler traversal budget exceeded")

    catches = []
    pattern = r"\.catch\s+(L[^;]+;)\s+\{\s*(:[\w]+)\s+\.\.\s+(:[\w]+)\s*\}\s+(:[\w]+)"
    for found in re.finditer(pattern, method):
        kind, start, end, target = found.groups()
        body, traversed = trace_handler(target)
        catches.append({"type": kind, "startLabel": start, "endLabel": end,
                        "targetLabel": target, "handlerBody": body,
                        "handlerTraceLabels": traversed})
    return catches


def normalized_method(code):
    # Source line/local metadata may vary when configuration/source paths differ.
    # Every instruction, label, catch range and constant must remain identical.
    return "\n".join(line.strip() for line in code.splitlines()
                     if line.strip() and not line.strip().startswith(
                         ("#", ".line ", ".local ", ".end local", ".restart local")))


def r1_equivalence(paths, dex):
    evidence = {}
    for label, path in paths.items():
        code = capture(label + "-inspection-service-code", [ANALYZER, "dex", "code", "--class",
                       "com.cooper.lithic.data.backup.BackupInspectionService", path])
        copy = method_block(code, "copySelected")
        copy_evidence = saved_text(label + "-copySelected.smali", copy)
        catches = handler_observations(copy)
        assert any(row["type"] == "Ljava/util/concurrent/CancellationException;" for row in catches)
        assert any(row["type"] == "Ljava/lang/Exception;" and
                   "BackupInspectionService$InspectionUnavailable;" in row["handlerBody"] for row in catches)
        flows = []
        for index, descriptor in enumerate(sorted(item for item in dex[label][0] if re.fullmatch(
                r"Lcom/cooper/lithic/data/backup/BackupInspectionService\$inspect\$\d+;", item))):
            lambda_code = capture(label + "-inspection-lambda-" + str(index),
                                  [ANALYZER, "dex", "code", "--class", descriptor[1:-1].replace("/", "."), path])
            if "Lcom/cooper/lithic/data/backup/BackupValidator;->validate(" in lambda_code:
                flow = method_block(lambda_code, "invokeSuspend")
                copy_call = re.search(r"BackupInspectionService;->(?:access\$)?copySelected\(", flow)
                assert copy_call and copy_call.start() < flow.index("Lcom/cooper/lithic/data/backup/BackupValidator;->validate(")
                flows.append((flow, saved_text(label + "-inspection-flow.smali", flow)))
        assert len(flows) == 1
        evidence[label] = dict(copy=copy, copyEvidence=copy_evidence, catches=catches,
                               flow=flows[0][0], flowEvidence=flows[0][1])
    for key in ("copy", "flow"):
        assert normalized_method(evidence["app"][key]) == normalized_method(evidence["preview16"][key]), key
    result = {"status": "UNCHANGED_FROM_ACCEPTED_PREVIEW16",
              "copySelectedAndInspectionFlowInstructionsLabelsAndCatchesIdentical": True,
              "ignoredOnlySourceLineLocalAndBlankCommentMetadata": True,
              "evidence": {label: {key: value for key, value in row.items() if key not in ("copy", "flow")}
                           for label, row in evidence.items()},
              "androidRuntimeClassification": "NOT_RUN"}
    write_json(OUT / "r1-equivalence.json", result)
    return result


def main():
    receipt_path = args.build_receipt.resolve()
    assert receipt_path.is_relative_to(ROOT)
    receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
    assert receipt["exitCode"] == 0 and receipt["deviceOperations"] == 0
    assert receipt["androidExecution"] == "NOT_RUN" and "-PlithicPrivateTrial=true" in receipt["argv"]
    assert ":app:assembleDebug" in receipt["argv"]
    assert not any("androidtest" in value.lower() for value in receipt["argv"])
    source_commit = receipt["sourceCommit"]
    assert re.fullmatch(r"[0-9a-f]{40}", source_commit)
    assert subprocess.run(["git", "merge-base", "--is-ancestor", BASELINE, source_commit], cwd=ROOT, capture_output=True).returncode == 0
    source_tree = git_text("rev-parse", source_commit + ":app")
    assert receipt["sourceAppGitTree"] == source_tree == git_text("rev-parse", "HEAD:app")
    assert receipt["appInputStatusAtStart"] == ""
    assert not git_text("status", "--porcelain=v1", "--untracked-files=all", "--", "app")
    assert git_text("branch", "--show-current") == "codex/v1-b3-photo-storage"
    changed = set(git_text("diff", "--name-only", BASELINE, source_commit, "--", "app").splitlines())
    assert changed == {"app/build.gradle.kts"}, changed
    before = subprocess.check_output(["git", "show", BASELINE + ":app/build.gradle.kts"], cwd=ROOT)
    assert before.count(b"lithicPrivateTrial -> 32") == before.count(b'lithicPrivateTrial -> "0.1.0-preview.16"') == 1
    expected = before.replace(b"lithicPrivateTrial -> 32", b"lithicPrivateTrial -> 33").replace(
        b'lithicPrivateTrial -> "0.1.0-preview.16"', b'lithicPrivateTrial -> "0.1.0-preview.17"')
    actual = subprocess.check_output(["git", "show", source_commit + ":app/build.gradle.kts"], cwd=ROOT)
    assert actual == expected, "Unexpected change beyond the two Private Trial version values"
    assert not git_text("diff", "--name-only", BASELINE, source_commit, "--",
                        "app/src/main", "app/src/test", "app/src/androidTest", "app/schemas")
    preparation = json.loads((EV / "preparation.json").read_text(encoding="utf-8"))
    assert preparation["baseline"] == BASELINE and len(preparation["protectedApks"]) == 32
    assert len({row["path"] for row in preparation["protectedApks"]}) == 32
    protection = []
    for row in preparation["protectedApks"]:
        value = metadata(ROOT / row["path"])
        assert value["bytes"] == row["bytes"] and value["sha256"] == row["sha256"], row["path"]
        protection.append({**value, "unchanged": True})
    paths = {"app": ROOT / "app/build/rc0-preview17/outputs/apk/debug/app-debug.apk",
             "preview16": ROOT / "deliveries/lithic-preview-0.1.0-preview.16/Lithic-0.1.0-preview.16-debug.apk"}
    assert metadata(paths["preview16"])["sha256"] == "e8b2e2fe7b2db3af986bc9818f1a02a9da583ba160b723e63befc7f4a1eb9e94"
    assert metadata(paths["preview16"])["bytes"] == 20506555
    manifests, identity, dex = {}, {}, {}
    forbidden = {"android.permission.INTERNET", "android.permission.READ_EXTERNAL_STORAGE",
                 "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.MANAGE_EXTERNAL_STORAGE",
                 "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO",
                 "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"}
    for label, path in paths.items():
        package = PACKAGE if label == "app" else PREVIEW
        badging = capture(label + "-badging", [TOOLS / "aapt2.exe", "dump", "badging", path])
        manifest = ET.fromstring(capture(label + "-manifest", [ANALYZER, "manifest", "print", path]))
        cert_output = capture(label + "-certificate", [TOOLS / "apksigner.bat", "verify", "--verbose", "--print-certs", path])
        assert re.findall(r"Signer #\d+ certificate SHA-256 digest: ([a-f0-9]+)", cert_output) == [CERT]
        assert manifest.get("package") == package and ("name='" + package + "'") in badging
        assert manifest.get(NS + "sharedUserId") is None
        permissions = sorted(node.get(NS + "name") for node in manifest if node.tag.startswith("uses-permission"))
        assert not forbidden.intersection(permissions)
        manifests[label] = manifest
        dex[label] = apk_metadata(path)
        identity[label] = {**metadata(path), "applicationId": package, "certificateSha256": CERT,
                           "versionCode": manifest.get(NS + "versionCode"), "versionName": manifest.get(NS + "versionName"),
                           "permissions": permissions, "nativeEntries": dex[label][3]}
    app = manifests["app"]
    application = app.find("application")
    assert identity["app"]["versionCode"] == "33" and identity["app"]["versionName"] == "0.1.0-preview.17-debug"
    assert identity["preview16"]["versionCode"] == "32" and identity["preview16"]["versionName"] == "0.1.0-preview.16-debug"
    assert application.get(NS + "name") == "com.cooper.lithic.app.LithicApplication"
    assert application.get(NS + "label") == "砺石·体验版"
    assert application.get(NS + "allowBackup") == "false" and application.get(NS + "testOnly") != "true"
    assert not app.findall("instrumentation")
    assert app.find("uses-sdk").get(NS + "minSdkVersion") == "26" and app.find("uses-sdk").get(NS + "targetSdkVersion") == "36"
    activities = {e.get(NS + "name"): e for e in application.findall("activity")}
    assert set(activities) == {"com.cooper.lithic.app.MainActivity"}
    launcher = activities["com.cooper.lithic.app.MainActivity"]
    assert launcher.get(NS + "exported") == "true"
    assert any(any(action.get(NS + "name") == "android.intent.action.MAIN" for action in f.findall("action"))
               and any(category.get(NS + "name") == "android.intent.category.LAUNCHER" for category in f.findall("category"))
               for f in launcher.findall("intent-filter"))
    authorities = [e.get(NS + "authorities") for e in application.findall("provider")]
    assert authorities and all(value and all(item.startswith(PACKAGE + ".") for item in value.split(";")) for value in authorities)
    app_components = components(app, PACKAGE)
    assert app_components == components(manifests["preview16"], PREVIEW)
    assert [value.replace(PACKAGE, "{applicationId}") for value in identity["app"]["permissions"]] == [
        value.replace(PREVIEW, "{applicationId}") for value in identity["preview16"]["permissions"]]
    assert dex["app"][3] == dex["preview16"][3]
    app_classes, app_methods, flags, _ = dex["app"]
    assert flags == [True], "Preview17 PHOTO_LITE_EXPERIMENT actual DEX value must be true"
    required = ["app/LithicApplication", "app/MainActivity", "app/LedgerUiStateHolder", "app/LithicNavigationKt",
                "feature/media/PhotoLiteCoverController", "feature/media/PhotoLiteThumbnailPipeline",
                "feature/media/PhotoLiteCoverPanelKt", "feature/media/CoverPreviewsKt",
                "data/media/photolite/GenerationPhotoLiteCoverWriter", "data/media/photolite/PhotoLitePng",
                "data/media/photolite/PhotoLiteCoverChange", "data/media/photolite/PhotoLiteTarget",
                "data/media/photolite/PhotoLiteCommitResult$Cleared", "data/backup/BackupInspectionService",
                "data/backup/BackupInspectionStatus", "data/backup/BackupInspectionResult", "data/backup/BackupInspectionSummary",
                "data/backup/UnsupportedBackupFormatException", "feature/settings/BackupInspectionController",
                "feature/settings/BackupInspectionViewModel", "feature/settings/BackupInspectionDocumentPickerKt",
                "feature/settings/BackupInspectionUiState", "feature/settings/DataConfidenceSectionsKt",
                "feature/settings/SettingsShellKt", "data/media/PhotoStorageReader", "data/media/GenerationPhotoStorageReader",
                "data/media/PhotoStorageSummary", "data/media/PhotoStorageReadFailure", "data/media/PhotoStorageReadResult$Ready",
                "data/media/PhotoStorageReadResult$Unavailable", "feature/settings/PhotoStorageController",
                "feature/settings/PhotoStorageViewModel", "feature/settings/PhotoStorageUiState$Ready",
                "feature/settings/PhotoStorageUiState$Unavailable", "feature/appearance/LithicAppearance",
                "feature/appearance/AppearanceController", "feature/appearance/AppearanceProvider",
                "feature/appearance/DataStoreAppearanceStorage", "feature/appearance/AppearanceThemePickerKt", "core/design/LithicThemeKt"]
    for name in required:
        assert "Lcom/cooper/lithic/" + name + ";" in app_classes, name
    required_methods = [("feature/media/CoverPreviewsKt", "RecordCover"),
                        ("data/media/photolite/GenerationPhotoLiteCoverWriter", "capture"),
                        ("data/media/photolite/GenerationPhotoLiteCoverWriter", "commit"),
                        ("data/media/photolite/GenerationPhotoLiteCoverWriter", "remove"),
                        ("feature/media/PhotoLiteCoverController", "begin"), ("feature/media/PhotoLiteCoverController", "confirm"),
                        ("feature/media/PhotoLiteThumbnailPipeline", "load"), ("data/backup/BackupInspectionService", "inspect"),
                        ("data/backup/BackupInspectionService", "copySelected"), ("feature/settings/BackupInspectionController", "selected"),
                        ("feature/settings/BackupInspectionController", "beginSelection"), ("feature/settings/BackupInspectionController", "requestCancellation"),
                        ("feature/settings/BackupInspectionViewModel", "selected"), ("feature/settings/BackupInspectionViewModel", "cancelCheck"),
                        ("feature/settings/BackupInspectionDocumentPickerKt", "rememberBackupInspectionDocumentPicker"),
                        ("feature/settings/DataConfidenceSectionsKt", "BackupInspectionFeedback"),
                        ("feature/settings/DataConfidenceSectionsKt", "PhotoStorageSection"),
                        ("data/media/GenerationPhotoStorageReader", "read"), ("data/media/GenerationPhotoStorageReader", "isCurrent"),
                        ("feature/settings/PhotoStorageController", "refresh"), ("feature/settings/PhotoStorageController", "close"),
                        ("feature/settings/PhotoStorageViewModel", "refresh"),
                        ("feature/appearance/AppearanceThemePickerKt", "AppearanceThemePicker"), ("core/design/LithicThemeKt", "LithicTheme")]
    for owner, method in required_methods:
        assert ("Lcom/cooper/lithic/" + owner + ";", method) in app_methods, (owner, method)
    assert not [name for name in app_classes if name.startswith(("Lorg/junit/", "Ljunit/", "Lorg/hamcrest/", "Landroidx/test/", "Landroidx/compose/ui/test/"))]
    assert "Lcom/cooper/lithic/app/M13IsolatedRunner;" not in app_classes
    # Verify the actual B3 data path in bytecode, not merely the presence of class names.
    assert ("Lcom/cooper/lithic/data/media/PhotoStorageSummary;", "getEstimatedHistoricalBytes") in app_methods
    read_lambda = "com.cooper.lithic.data.media.GenerationPhotoStorageReader$read$2$summary$1$1"
    assert "L" + read_lambda.replace(".", "/") + ";" in app_classes
    query_code = capture("b3-historical-query-code", [ANALYZER, "dex", "code", "--class", read_lambda, paths["app"]])
    query_lines = [line for line in query_code.splitlines() if "SELECT COALESCE(SUM(media.byteLength), 0)" in line]
    assert len(query_lines) == 1
    assert all(fragment in query_lines[0] for fragment in ("WHERE NOT EXISTS", "long_term_projects",
               "UNION SELECT coverAssetId FROM subscriptions", "covers.coverAssetId = media.id"))
    assert "Landroidx/sqlite/db/SupportSQLiteDatabase;->query(" in query_code
    assert "PhotoStorageReaderKt;->photoStorageSummary(" in query_code
    ui_code = capture("b3-settings-section-code", [ANALYZER, "dex", "code", "--class",
                      "com.cooper.lithic.feature.settings.DataConfidenceSectionsKt", paths["app"]])
    ui_entry = method_block(ui_code, "PhotoStorageSection")
    # Compose moves section contents into a separate lambda method; keep both for review.
    ui_body = method_block(ui_code, "PhotoStorageSection$lambda$0")
    assert "PhotoStorageSummary;->getEstimatedHistoricalBytes()J" in ui_body
    assert "photo-storage-historical-bytes" in ui_body and "photo-storage-cleanup-unavailable" in ui_body
    settings_code = capture("b3-settings-shell-code", [ANALYZER, "dex", "code", "--class",
                            "com.cooper.lithic.feature.settings.SettingsShellKt", paths["app"]])
    assert "DataConfidenceSectionsKt;->PhotoStorageSection(" in settings_code
    b3_evidence = {"historicalQueryFromActualDex": saved_text("b3-historical-query.smali", query_code),
                   "settingsSectionFromActualDex": saved_text("b3-settings-section.smali", ui_entry + "\n" + ui_body),
                   "settingsShellFromActualDex": saved_text("b3-settings-shell.smali", settings_code),
                   "bothRecordTypesAndNotExists": True, "estimatedHistoricalBytesGetterAndUiTag": True,
                   "notEvidenceOfAndroidRuntime": "NOT_RUN"}
    appearance = capture("appearance-enum-code", [ANALYZER, "dex", "code", "--class", "com.cooper.lithic.feature.appearance.LithicAppearance", paths["app"]])
    for value in ("CHARCOAL", "PURE_WHITE", "STONE_MIST"):
        assert re.search(r"(?m)^\.field[^\r\n]*\benum\s+" + value + r":Lcom/cooper/lithic/feature/appearance/LithicAppearance;", appearance), value
    application_code = capture("application-code", [ANALYZER, "dex", "code", "--class", "com.cooper.lithic.app.LithicApplication", paths["app"]])
    assert all(value in application_code for value in ("onCreate", "ReminderRuntime", "AppContainer"))
    main_code = capture("main-activity-code", [ANALYZER, "dex", "code", "--class", "com.cooper.lithic.app.MainActivity", paths["app"]])
    assert all(value in main_code for value in ("onCreate", "LithicApplication", "getAppContainer"))
    r1 = r1_equivalence(paths, dex)
    # The reviewed icon source must be exactly the PNG packaged in both APKs.
    icon_source = ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_scene_p3.png"
    icon = metadata(icon_source)
    assert icon["sha256"] == "cf98e02ead3e72cdbf4902e0aeff991e28a74583921140029a3f9ac85254ba14"
    icons = {}
    for label, path in paths.items():
        with zipfile.ZipFile(path) as archive:
            names = [name for name in archive.namelist() if name.endswith("/ic_launcher_scene_p3.png")]
            assert len(names) == 1
            raw = archive.read(names[0])
            assert raw == icon_source.read_bytes()
            icons[label] = {"apkEntry": names[0], "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest()}
    icon_xml = {}
    for label, path in paths.items():
        with zipfile.ZipFile(path) as archive:
            entries = {}
            for resource in ("ic_launcher.xml", "ic_launcher_round.xml", "ic_launcher_foreground.xml", "ic_launcher_background.xml"):
                names = [name for name in archive.namelist() if name.startswith("res/") and name.endswith("/" + resource)]
                assert len(names) == 1
                entries[resource] = {"apkEntry": names[0], "sha256": hashlib.sha256(archive.read(names[0])).hexdigest()}
            icon_xml[label] = entries
    assert icon_xml["app"] == icon_xml["preview16"]
    for attribute in ("icon", "roundIcon"):
        assert application.get(NS + attribute) == manifests["preview16"].find("application").get(NS + attribute)
    for row in protection:
        assert metadata(ROOT / row["path"]) == {key: value for key, value in row.items() if key != "unchanged"}
    result = {"status": "DESKTOP_PASS", "scope": "RC0_PRIVATE_TRIAL_STATIC_AUDIT_ONLY",
              "app": identity["app"], "preview16": identity["preview16"],
              "buildReceipt": metadata(receipt_path), "buildSourceCommit": source_commit,
              "buildAppGitTree": source_tree, "acceptedBaseline": BASELINE,
              "appChanges": sorted(changed), "allMainTestSchemaSourcesUnchanged": True,
              "onlyTwoPrivateTrialVersionValuesChanged": True,
              "productionClassesChecked": required, "productionMethodsChecked": required_methods,
              "photoLiteActualDexBoolean": True, "threeThemeEnumFields": ["CHARCOAL", "PURE_WHITE", "STONE_MIST"],
              "normalApplicationAndLauncher": True, "providerAuthorities": authorities,
              "components": app_components,
              "permissionsAndNativeUnchangedAfterApplicationIdNormalization": True,
              "testFrameworkAndInstrumentationAbsent": True,
              "protectedLocalApks": protection, "r1Equivalence": r1, "b3DexEvidence": b3_evidence,
              "launcherIconMatchesAcceptedSourceAndPreview16": icons,
              "compiledAdaptiveIconXmlAndManifestBindingsUnchanged": icon_xml,
              "androidExecution": "NOT_RUN", "runtimeUidAndPrivateDirectoryIsolation": "NOT_RUN",
              "deviceOperations": 0, "newTestApk": False, "archivedByThisScript": False,
              "commands": COMMANDS}
    write_json(OUT / "identity.json", result)
    print(json.dumps({"status": result["status"], "app": identity["app"], "oldApksProtected": 32,
                      "r1Equivalence": r1["status"], "androidExecution": "NOT_RUN"}, ensure_ascii=True))


try:
    main()
except Exception:
    (OUT / "failure.txt").write_text(traceback.format_exc(), encoding="utf-8")
    raise
