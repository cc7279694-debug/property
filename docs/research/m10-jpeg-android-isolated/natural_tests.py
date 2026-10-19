"""One fixed natural Android method per invocation, with no retry or cleanup.

Only the root executor invokes run_one with a verified Supervisor. A host process
timeout never proves Android completion. Device evidence is supplied by the
separate read-only adapter; missing evidence cannot become a successful result.
"""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
from datetime import datetime, timezone
import zipfile

APP = "com.cooper.lithic.debug"
TEST = APP + ".test"
APP_SHA = "0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf"
TEST_SHA = "73d96ec9aceb23919183074a0dd101b1b7460187e4cb1ff5d3ed8ab9f9b3d98f"
COMPONENT = TEST + "/com.cooper.lithic.data.media.M10ProcessBoundaryRunner"
PREFIX = "com.cooper.lithic.data.media."
METHODS = (
    PREFIX + "M10WorkerRetirementReviewTest#healthy_jpeg_retires_real_worker_before_stage_completion",
    PREFIX + "M10WorkerRetirementReviewTest#locked_damaged_jpeg_is_rejected_and_real_worker_safely_retires",
    PREFIX + "M10JpegIntegrityTest#valid_control_jpeg_imports_successfully",
    PREFIX + "M10JpegIntegrityTest#jpeg_entropy_truncated_with_eoi_is_rejected",
    PREFIX + "M10JpegOldCoverProtectionTest#project_entropy_cut_jpeg_is_rejected_and_old_cover_and_money_are_preserved",
    PREFIX + "M10JpegOldCoverProtectionTest#subscription_entropy_cut_jpeg_is_rejected_and_old_cover_payments_and_states_are_preserved",
    PREFIX + "M10StageV2CompatibilityTest#png_still_uses_v1_preparation_and_original_output_limits",
    PREFIX + "M10StageV2CompatibilityTest#legacy_uncommitted_jpeg_is_preserved_without_fabricated_receipt",
    PREFIX + "M10StageV2CompatibilityTest#legacy_owned_final_is_viewable_but_does_not_prove_a_new_strict_preparation",
)
FIXTURES = {
    "assets/m10_jpeg_integrity/complete.jpg": (59334, "2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907"),
    "assets/m10_jpeg_integrity/entropy_cut_eoi_retained.jpg": (20194, "f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a"),
}


class MethodBlocked(RuntimeError):
    """A persisted non-PASS outcome; callers must not issue the next method."""


def immutable(path, value, *, text=False):
    data = value.encode("utf-8") if text else (json.dumps(value, sort_keys=True, indent=2) + "\n").encode()
    with path.open("xb") as stream:
        stream.write(data)
        stream.flush()
        os.fsync(stream.fileno())
    if path.read_bytes() != data:
        raise MethodBlocked("Evidence readback failed; preserve method state")


def parse_instrumentation(text, method):
    """Validate the actual AndroidJUnitRunner wire result, not adb exit status."""
    result = {"status": "UNKNOWN", "method": method, "decodedBytes": len(text.encode("utf-8")),
              "decodedSHA256": hashlib.sha256(text.encode("utf-8")).hexdigest()}
    if (len(text) > 1024 * 1024 or any(ord(c) < 32 and c not in "\t\n\r" for c in text)
            or "\ufffd" in text):
        return {**result, "reason": "Incomplete or invalid instrumentation text"}
    if re.search(r"INSTRUMENTATION_FAILED|shortMsg=|FAILURES!!!|INSTRUMENTATION_STATUS_CODE: -[1234]\b", text):
        return {**result, "status": "FAIL", "reason": "Explicit instrumentation failure/skipped/crash"}
    final = re.findall(r"^INSTRUMENTATION_CODE: (-?\d+)\s*$", text, re.M)
    codes = re.findall(r"^INSTRUMENTATION_STATUS_CODE: (-?\d+)\s*$", text, re.M)
    cls, name = method.split("#")
    classes = re.findall(r"^INSTRUMENTATION_STATUS: class=(.+?)\s*$", text, re.M)
    names = re.findall(r"^INSTRUMENTATION_STATUS: test=(.+?)\s*$", text, re.M)
    counts = re.findall(r"^INSTRUMENTATION_STATUS: numtests=(\d+)\s*$", text, re.M)
    if (text.count("INSTRUMENTATION_CODE:") != 1 or text.count("INSTRUMENTATION_STATUS_CODE:") != 2 or
            final != ["-1"] or codes != ["1", "0"] or classes != [cls, cls] or
            names != [name, name] or counts != ["1", "1"] or
            len(re.findall(r"^OK \(1 test\)\s*$", text, re.M)) != 1):
        return {**result, "reason": "Missing, mismatched or duplicate single-method completion"}
    return {**result, "status": "PASS", "testCount": 1, "reason": "Matching start, completion and single-test summary"}


def audit_fixtures(audit):
    rows = [row for row in audit.get("apks", []) if row.get("kind") == "test"]
    if len(rows) != 1:
        raise MethodBlocked("Missing unique locked test APK")
    path = Path(rows[0]["path"])
    if hashlib.sha256(path.read_bytes()).hexdigest() != TEST_SHA:
        raise MethodBlocked("Test APK no longer matches locked SHA")
    values = []
    with zipfile.ZipFile(path) as archive:
        for name, (size, sha) in FIXTURES.items():
            matches = [entry for entry in archive.infolist() if entry.filename == name]
            if len(matches) != 1 or matches[0].file_size != size:
                raise MethodBlocked("Missing/ambiguous/changed locked JPEG fixture")
            with archive.open(matches[0]) as stream:
                data = stream.read(size + 1)
            if len(data) != size or hashlib.sha256(data).hexdigest() != sha:
                raise MethodBlocked("Locked fixture bytes differ")
            values.append({"name": name, "bytes": size, "sha256": sha})
    return {"status": "PASS", "testAPK": TEST_SHA, "fixtures": values}


def run_one(supervisor, method, evidence_api=None, *, capture_factory=None):
    """Issue exactly one permitted method; any non-PASS durably stops traversal.

    The adapter protocol is before(s, method), after(s, method, before), and
    assess(method, before, after, instrumentation). It must collect genuine
    bounded device evidence and distinguish lifecycle proof from assertions.
    No adapter action may mutate/clean a stage or stop an Android process.
    """
    if method not in METHODS:
        raise MethodBlocked("Method is outside the fixed permission allowlist")
    index = METHODS.index(method)
    root = Path(supervisor.evidence)
    for preceding in range(index):
        path = root / f"method-{preceding:02d}" / "result.json"
        if not path.is_file():
            raise MethodBlocked("Previous single-method gate is not proven")
        try:
            prior = prior_result(path, preceding)
        except (ValueError, OSError) as error:
            raise MethodBlocked("Previous method evidence unreadable: no new command") from error
        if any(prior.get(key) != value for key, value in {
            "status": "PASS", "method": METHODS[preceding], "serial": supervisor.serial,
            "api": supervisor.api, "appSHA256": APP_SHA, "testSHA256": TEST_SHA,
        }.items()):
            raise MethodBlocked("Previous method failed, unknown, or identity mismatched")
    directory = root / f"method-{index:02d}"
    try:
        directory.mkdir(exist_ok=False)
    except FileExistsError as error:
        raise MethodBlocked("Method evidence already exists: no reissue or overwrite") from error
    result = {"status": "BLOCKED", "method": method, "index": index, "serial": supervisor.serial,
              "api": supervisor.api, "appSHA256": APP_SHA, "testSHA256": TEST_SHA,
              "instrumentationCommandsIssued": 0, "startedUTC": datetime.now(timezone.utc).isoformat(),
              "instrumentation": {"status": "NOT_RUN"}, "lifecycle": {"status": "NOT_RUN"}}
    before = None
    issued = False
    collector = None
    capture_finished = False
    capture_proof = None

    def finish_capture():
        # A failed drain must never trigger a second stop/finish operation.
        nonlocal capture_finished, capture_proof
        if not capture_finished:
            capture_finished = True
            try:
                capture_proof = collector.finish()
                if not isinstance(capture_proof, dict):
                    raise ValueError("Log observer finish did not return structured evidence")
            except Exception as error:
                capture_proof = {"status": "UNKNOWN", "reason": str(error),
                                 "errorType": type(error).__name__}
        return capture_proof

    try:
        if evidence_api is None:
            import device_evidence as evidence_api
        if capture_factory is None and callable(getattr(supervisor, "new_log_capture", None)):
            capture_factory = lambda s, directory: s.new_log_capture(directory)
        if getattr(supervisor, "continuous_capture_required", False) and capture_factory is None:
            raise MethodBlocked("Continuous log observer required; snapshot-only fallback forbidden")
        supervisor.identity()
        audit = supervisor.apk_audit()
        result["fixtures"] = audit_fixtures(audit)
        result["hostUID"] = supervisor.installed(APP, APP_SHA)
        result["testUID"] = supervisor.installed(TEST, TEST_SHA)
        components = supervisor.adb("shell", "pm", "list", "instrumentation").splitlines()
        target = [line for line in components if f"(target={APP})" in line or COMPONENT in line]
        if target != [f"instrumentation:{COMPONENT} (target={APP})"]:
            raise MethodBlocked("Instrumentation target/component missing, duplicated or conflicting")
        before = evidence_api.before(supervisor, method)
        immutable(directory / "before.json", before)
        if before.get("status") != "PASS":
            raise MethodBlocked("Current device/worker/stage evidence is not eligible")
        if capture_factory is not None:
            collector = capture_factory(supervisor, directory)
            ready = collector.start()
            result["logCaptureStarted"] = ready
            if not isinstance(ready, dict) or ready.get("status") != "PASS":
                raise MethodBlocked("Continuous log observer readiness UNKNOWN")
            live = collector.assert_live()
            result["logCaptureLiveBeforeCommand"] = live
            if not isinstance(live, dict) or live.get("status") != "PASS":
                raise MethodBlocked("Continuous log observer liveness UNKNOWN")
        supervisor.marker(f"natural-test-{index:02d}")
        immutable(directory / "attempt.json", {"status": "ATTEMPT_ISSUED", "method": method,
                  "serial": supervisor.serial, "api": supervisor.api, "component": COMPONENT})
        supervisor.note({"operation": "ONE_NATURAL_METHOD", "method": method, "serial": supervisor.serial,
                         "evidence": str(directory), "automaticRetry": False})
        issued = True
        result["instrumentationCommandsIssued"] = 1
        result["instrumentationIssuedUTC"] = datetime.now(timezone.utc).isoformat()
        # am instrument may stop/relaunch only this isolated test target as part of normal
        # instrumentation; no force-stop, fault extra, clear, kill, or retry is issued.
        try:
            output = supervisor.adb("shell", "am", "instrument", "-w", "-r", "-e", "class", method,
                                    COMPONENT, timeout=240)
            immutable(directory / "instrumentation.txt", output, text=True)
            result["instrumentation"] = parse_instrumentation(output, method)
        except Exception as error:
            result["instrumentation"] = {"status": "UNKNOWN", "reason": str(error), "errorType": type(error).__name__,
                                         "rawBytes": "See immutable supervisor command journal; device completion unproved"}
        result["instrumentationReturnedUTC"] = datetime.now(timezone.utc).isoformat()
        # This remains a read-only ending observation even after a failed or timed-out method.
        after = (evidence_api.after(supervisor, method, before, capture=finish_capture)
                 if collector is not None else evidence_api.after(supervisor, method, before))
        immutable(directory / "after.json", after)
        result["lifecycle"] = evidence_api.assess(method, before, after, result["instrumentation"])
        states = (result["instrumentation"].get("status"), result["lifecycle"].get("status"))
        if states == ("PASS", "PASS"):
            result["status"] = "PASS"
        elif "FAIL" in states:
            result["status"] = "FAIL"
        else:
            result["status"] = "UNKNOWN"
    except Exception as error:
        result["status"] = "UNKNOWN" if issued else "BLOCKED"
        result["error"] = {"type": type(error).__name__, "detail": str(error)}
    finally:
        if collector is not None:
            result["logCapture"] = finish_capture()
            if result["logCapture"].get("status") != "PASS" and result["status"] == "PASS":
                result["status"] = "UNKNOWN"
    result["finishedUTC"] = datetime.now(timezone.utc).isoformat()
    immutable(directory / "result.json", result)
    supervisor.note({"operation": "ONE_NATURAL_METHOD_RESULT", "method": method, "status": result["status"],
                     "result": str(directory / "result.json"), "automaticRetry": False})
    if result["status"] != "PASS":
        raise MethodBlocked(f"{method}: {result['status']}; preserve device and method evidence")
    return result


def prior_result(path, index):
    """Only the specifically proved byte-reading supplement can resolve gate 0."""
    path = Path(path)
    original = json.loads(path.read_bytes().decode("utf-8", errors="strict"))
    if index == 0 and original.get("status") == "UNKNOWN" and (path.parent/"raw-byte-reassessment.json").is_file():
        from raw_reassessment import effective_result
        return effective_result(path.parent)
    return original
