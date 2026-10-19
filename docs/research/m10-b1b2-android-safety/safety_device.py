"""Root-only, exactly one fixed isolated safety method; no traversal or cleanup.

Import is inert. Device calls occur only in run_one/main, using FreshSupervisor.
Every non-PASS consumes its exclusive attempt and blocks the following method.
"""
from __future__ import annotations
import argparse
import base64
import hashlib
import json
from pathlib import Path
import re
import shlex
import stat
import sys
import uuid

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import fresh_environment as fresh
import device_evidence as dev
import lifecycle_evidence as life
from natural_tests import parse_instrumentation

APP, TEST, APP_SHA = fresh.APP, fresh.TEST, fresh.APP_SHA
PREFIX = "com.cooper.lithic.data.media."
METHODS = (
    PREFIX + "M10B1B2SafetyPreflightTest#new_safety_classes_and_boot_role_are_ready_without_consumers",
    PREFIX + "M10WorkerRetirementReviewTest#healthy_jpeg_retires_real_worker_before_stage_completion",
    PREFIX + "M10JpegResourceEvidenceTest#baseline_420_40mp_worker_resource_evidence",
    PREFIX + "M10JpegResourceEvidenceTest#progressive_420_40mp_worker_resource_evidence",
    PREFIX + "M10JpegResourceEvidenceTest#progressive_444_40mp_worker_resource_evidence",
    PREFIX + "M10B1CancellationSafetyTest#cancellation_after_persistent_pin_before_fd_delivery_closes_only_failed_attempt",
    PREFIX + "M10B1CancellationSafetyTest#delayed_real_delivery_return_keeps_binding_until_original_failed_session_closes",
)
UUID = r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
ROOT_RE = re.compile(r"m10-(?:worker-review|resource-v2|b1-cancel|b1-delay)-" + UUID)
OBS_RE = re.compile(r"worker-review-(?:result|terminal|abandoned)-" + UUID + r"\.json")
PHASE_NAMES = ("OPEN", "NATIVE_COMPLETE", "NORMALIZATION_BEGIN", "NORMALIZATION_END", "FINISHED", "RETIRING")
REFLECTED_CLASSES = {
    PREFIX + "M10B1B2SafetyPreflightTest", PREFIX + "M10B1CancellationSafetyTest",
    PREFIX + "M10JpegResourceEvidenceTest", PREFIX + "M10ResourceSampler",
    PREFIX + "M10B1B2SafetySupport", PREFIX + "M10B1ClosureObservations",
    PREFIX + "M10B1ExactProcessFaultTest",
    PREFIX + "jpegworker.JpegResourceEvidence", PREFIX + "jpegworker.JpegResourceDiagnostics",
    PREFIX + "jpegworker.JpegResourceDiagnostics$Session", PREFIX + "jpegworker.JpegBootIdentity",
}
DIAG_NAMES = {"allow.properties", "worker-claim.json", "polling.json", "test-result.json"} | {
    f"{i:02d}-{n}.json" for i, n in enumerate(PHASE_NAMES)}
SAFETY_NAMES = {"attempt.json", "result.json", "observation-01.json", "observation-02.json", "callback-error.json",
                "in-flight-checkpoint.json", "initial-observation.json", "late-closure.json"}
RECORD_NAMES = set(dev.RECORDS) | {"supervisor.v2", "abandoned.v2", "abandoned-cleanup-intent.v2", "abandoned-cleaned.v2"}
FIXTURES = (
    (17163380, "c2c2fa54d49ab8155321175a66596ce8d35ed9927d9f3dfe2bbb6a00c5fbb5c7"),
    (15458857, "2b02f35f08a8cb36d31a0a4f01e8810f554be67619950c8e2b0d0d2be873c5c8"),
    (27445246, "0555234207d5ddf84d236d812487a659f2dac34a9d82109a941628553c7b67e6"),
)
need = fresh.need


def method_plan(index, run_id, api):
    need(type(index) is int and 0 <= index < len(METHODS), "Method outside fixed safety-method allowlist")
    need(api in (26, 36) and re.fullmatch(UUID, run_id) is not None, "API/run UUID is not authorized")
    return {"method": METHODS[index], "argv": ["shell", "am", "instrument", "-w", "-r", "-e", "class", METHODS[index],
        "-e", "m10SafetyPermit", "M10_B1B2_ISOLATED_V1", "-e", "m10SafetyRunId", run_id,
        "-e", "m10SafetyExpectedApi", str(api), fresh.INSTRUMENTATION]}


def prior_gate(row, index, api, serial, test_sha):
    expected = {"status": "PASS", "index": index, "method": METHODS[index], "api": api,
                "serial": serial, "appSHA256": APP_SHA, "testSHA256": test_sha}
    need(isinstance(row, dict) and all(row.get(k) == v for k, v in expected.items()), "Previous safety method not independently PASS")
    return True



def _preflight_processes(value, uid):
    need(isinstance(value, dict) and value.get("complete") is True and value.get("worker_absent") is True and
         value.get("uid") == uid and type(value.get("rawBytes")) is int and value["rawBytes"] > 0 and
         re.fullmatch(r"[a-f0-9]{64}", value.get("rawSHA256", "")), "Incomplete independent preflight process observation")
    rows = value.get("rows")
    need(isinstance(rows, list) and len(rows) <= 1, "Unknown or multiple same-UID preflight processes")
    for row in rows:
        need(row.get("name") == APP and row.get("uid") == uid and
             all(type(row.get(k)) is int and row[k] > 0 for k in ("pid", "startTicks")),
             "Active Worker or unknown same-UID preflight identity")
    return rows


def _same_preflight_environment(before, after):
    need(before.get("uid") == after.get("uid") and before.get("testUID") == after.get("testUID") and
         type(after.get("uid")) is int and type(after.get("testUID")) is int and
         after["uid"] > 0 and after["testUID"] > 0 and after["uid"] != after["testUID"], "Preflight App/Test UID changed")
    need(before.get("appSHA256") == after.get("appSHA256") == APP_SHA and
         before.get("testSHA256") == after.get("testSHA256") == fresh.TEST_SHA,
         "Preflight locked APK identity changed")
    a, b = before.get("identity", {}), after.get("identity", {})
    api = a.get("api"); profile = fresh.profile(api)
    need(a.get("status") == b.get("status") == "PASS" and
         all(a.get(k) == b.get(k) == profile[v] for k, v in (("api", "api"), ("avd", "name"), ("serial", "serial"))),
         "Preflight device identity conflicts")
    need(all(a.get(k) == b.get(k) for k in ("base", "data", "appSHA256", "runtimePaths")) and
         a.get("appSHA256") == APP_SHA and a.get("runtimePaths", {}).get("status") == "PASS", "Preflight runtime path/image changed")
    pa, pb = a.get("processIdentityProof", {}), b.get("processIdentityProof", {})
    need(pa.get("status") == pb.get("status") == "PASS" and pa.get("anchor") and pa.get("anchor") == pb.get("anchor"),
         "Preflight emulator instance/creation identity changed")
    return api


def preflight_records(before, after, run_id, grouped, phases, boot_id=None):
    """Pure verification of actual in-process identity, not a post-JUnit liveness demand.

    A report is only usable with locked instrumentation/capture and independent
    file/identity snapshots. No disappeared Host is used as FD/guard death proof.
    """
    api = _same_preflight_environment(before, after)
    uid = after["uid"]
    _preflight_processes(before.get("processes"), uid)
    end_rows = _preflight_processes(after.get("processes"), uid)
    need(not phases and all(not snap.get(key) for snap in (before, after) for key in ("stages", "observations", "diagnostics")),
         "Reflection unexpectedly created or encountered a consumer")
    added = delta(before, after, "safety")
    need(set(added) == {run_id} and added[run_id].get("children") == ["attempt.json", "result.json"], "Preflight namespace/record set conflicts")
    path = "no_backup/m10-b1b2-safety/" + run_id
    need(set(grouped) == {path + "/attempt.json", path + "/result.json"}, "Preflight read group is not exactly its two records")
    parsed = {}
    for name in ("attempt.json", "result.json"):
        raw, metadata = grouped[path + "/" + name]
        expected = added[run_id]["files"][name]
        need(isinstance(raw, str) and 0 < len(raw.encode("utf8")) <= 16384 and
             metadata == {**expected, "sha256": hashlib.sha256(raw.encode("utf8")).hexdigest()} and
             metadata.get("bytes") == len(raw.encode("utf8")) and metadata.get("uid") == uid and
             metadata.get("type") == "regular" and metadata.get("links") == 1,
             "Preflight raw byte/length/SHA/inode/UID proof conflicts")
        parsed[name] = life.strict_json(raw)
    attempt, result = parsed["attempt.json"], parsed["result.json"]
    need(attempt.get("protocol") == "M10_B1B2_SAFETY_ATTEMPT_V1" and attempt.get("status") == "ATTEMPT_ISSUED" and
         attempt.get("runId") == run_id and attempt.get("method") == METHODS[0] and attempt.get("api") == api and
         attempt.get("hostUid") == uid and attempt.get("roomOrFormalCoverCommitted") is False and
         all(type(attempt.get(k)) is int and attempt[k] > 0 for k in ("hostPid", "hostStartTicks")),
         "Direct preflight execution identity is missing/invalid")
    need(re.fullmatch(r"6c697468-6963-4000-8000-[a-f0-9]{12}", attempt.get("bootId", "")) and
         attempt["bootId"] == result.get("bootIdBefore") == result.get("bootIdAfter") and
         (boot_id is None or attempt["bootId"] == boot_id), "Preflight BOOT_COUNT identity changed or unknown")
    need(result.get("protocol") == "M10_B1B2_PREFLIGHT_V1" and result.get("status") == "PASS" and
         result.get("runId") == run_id and result.get("method") == METHODS[0] and
         all(result.get(k) is True for k in ("noConsumerWork", "bootStable", "mainRoleAndExactHostInstance")) and
         all(result.get(k) is False for k in ("stageCreatedByMethod", "workerStartedByMethod")) and
         result.get("deviceAdmissionOutsideThisMethod") == "REQUIRED", "Preflight direct boot/role/assertHost proof incomplete")
    stage = {"stageRoots": 0, "stageMetadataRows": 0, "stageMetadataSHA256": hashlib.sha256(b"").hexdigest(),
             "businessOrImageBytesRead": False, "evidenceNamespaceExcluded": "m10-b1b2-safety"}
    need(result.get("before") == result.get("after") == stage, "Preflight Stage metadata is not unchanged/consumer-free")
    no_worker = {"status": "ABSENT_AT_OBSERVATION", "source": "ACTIVITY_MANAGER_SAME_UID", "workerRows": 0,
                 "externalSupervisorProcVerification": "REQUIRED_SEPARATELY"}
    need(result.get("processesBefore") == result.get("processesAfter") == no_worker, "In-method Worker absence proof incomplete")
    reflected = result.get("reflectedClasses", [])
    need(len(reflected) == len(REFLECTED_CLASSES) and {x.get("class") for x in reflected} == REFLECTED_CLASSES and
         all(x.get("scan") == "PASS" and type(x.get("declaredMethods")) is int and x["declaredMethods"] > 0 for x in reflected),
         "New classes not actually reflected")
    if end_rows:
        need(end_rows[0]["pid"] == attempt["hostPid"] and end_rows[0]["startTicks"] == attempt["hostStartTicks"],
             "Post-JUnit live Host is another instance")
    return {"status": "PASS", "preflight": result, "attempt": attempt,
            "hostIdentityEvidence": {"source": "DIRECT_IN_PROCESS_BEGIN_AND_ASSERT_HOST",
                "externalRunningHostObserved": bool(end_rows),
                "afterLifetime": "EXACT_HOST_STILL_PRESENT" if end_rows else "HOST_NOT_PRESENT_AT_POST_INSTRUMENTATION_OBSERVATION",
                "externalObservationLimit": "NO_INDEPENDENT_DURING_EXECUTION_PS_WAS_REQUIRED_OR_CLAIMED"},
            "resourceScope": "NO_CONSUMER_CREATED_NOT_A_RESOURCE_RELEASE_PROOF"}


def _legacy_preflight():
    directory = HERE / "evidence/api26-run/method-00"
    names = ("before.json", "after.json", "attempt.json", "result.json", "instrumentation.txt",
             "continuous-log/capture.json", "continuous-log/stdout.raw", "continuous-log/stderr.raw")
    raw = {name: fresh.avd_identity.read_bytes(directory / name, limit=1024 * 1024) for name in names}
    values = {name: life.strict_json(raw[name].decode("utf8")) for name in names if name.endswith(".json")}
    old = values["result.json"]
    need(old.get("status") == "UNKNOWN" and old.get("index") == 0 and old.get("instrumentationCommandsIssued") == 1 and
         old.get("appSHA256") == APP_SHA and old.get("testSHA256") == fresh.TEST_SHA,
         "Historical UNKNOWN is not the locked source")
    need(parse_instrumentation(raw["instrumentation.txt"].decode("utf8"), METHODS[0]) == old.get("instrumentation"),
         "Historical original JUnit bytes differ")
    plan = method_plan(0, old["runId"], old["api"])
    need(values["attempt.json"].get("argv") == plan["argv"] and old.get("method") == plan["method"],
         "Original issued method/run identity differs")
    proof, text = fresh.capture_module().read_verified(directory / "continuous-log", expected_serial=old["serial"])
    need(old.get("capture") == proof, "Historical original continuous capture conflicts")
    phases = scoped_lines(text, proof["handshake"]["authenticatedRawLine"], (), ())
    return directory, values, {name: hashlib.sha256(value).hexdigest() for name, value in raw.items()}, phases


def resume_preflight(s, grouped, current, boot_id):
    """Root supplies its single bounded current read/snapshot; this does no device call.
    Never changes old result/marker or emits another Instrumentation command.
    """
    directory, legacy, hashes, phases = _legacy_preflight()
    old = legacy["result.json"]
    need(s.api == old["api"] and s.serial == old["serial"], "Only the locked API26 UNKNOWN can be resumed")
    proof = preflight_records(legacy["before.json"], legacy["after.json"], old["runId"], grouped, phases, boot_id)
    _same_preflight_environment(legacy["after.json"], current)
    current_rows = _preflight_processes(current.get("processes"), current["uid"])
    need(not current_rows or (current_rows[0]["pid"] == proof["attempt"]["hostPid"] and
         current_rows[0]["startTicks"] == proof["attempt"]["hostStartTicks"]), "Resume encountered another live Host instance")
    need(all(current.get(k) == legacy["after.json"].get(k) for k in ("stages", "observations", "diagnostics", "safety")),
         "Synthetic state changed before independent preflight resume")
    evidence = s.evidence / "preflight-resume"
    fresh.avd_identity.unlinked_ancestors(evidence); evidence.mkdir(exist_ok=False)
    records = {}
    for path, (raw, metadata) in grouped.items():
        name = path.rsplit("/", 1)[1]
        filename = "preflight-" + name
        with (evidence / filename).open("xb") as stream: stream.write(raw.encode("utf8"))
        records[path] = {"file": filename, "metadata": metadata}
    payload = {**s.binding(), "protocol": "M10_B1B2_PREFLIGHT_READ_ONLY_RESUME_V1", "status": "PASS",
               "testSHA256": fresh.TEST_SHA, "runId": old["runId"], "index": 0, "method": METHODS[0],
               "instrumentationCommandsIssued": 0, "originalStatus": "UNKNOWN", "originalsSHA256": hashes,
               "bootId": boot_id, "records": records, "deviceProof": proof, "continuationSnapshot": current,
               "instrumentation": old["instrumentation"], "capture": old["capture"],
               "scope": "NEW_READ_ONLY_ADMISSION_PRESERVES_OLD_UNKNOWN_NOT_NEW_ANDROID_TEST_PASS"}
    fresh.immutable(evidence / "admission.json", payload)
    fresh.immutable(s.home / "preflight-resume-admission.json", {**s.binding(), "status": "PASS",
         "protocol": "M10_B1B2_PREFLIGHT_RESUME_POINTER_V1", "testSHA256": fresh.TEST_SHA,
         "sha256": hashlib.sha256(fresh.avd_identity.read_bytes(evidence / "admission.json", limit=1024 * 1024)).hexdigest(), "evidenceDirectory": str(evidence)})
    return payload


def admitted_method(s, index, test_sha):
    """One admission seam shared by ordinary methods, faults and safe AVD exit."""
    pointer_path = s.home / "preflight-resume-admission.json"
    if index != 0 or not pointer_path.exists():
        directory = s.evidence / "safety-methods" / f"method-{index:02d}"
        row = life.strict_json(fresh.avd_identity.read_bytes(directory / "result.json", limit=4 * 1024 * 1024).decode("utf8"))
        prior_gate(row, index, s.api, s.serial, test_sha)
        return row, directory
    pointer = s.require_proof(pointer_path)
    evidence = s.evidence / "preflight-resume"
    need(pointer.get("protocol") == "M10_B1B2_PREFLIGHT_RESUME_POINTER_V1" and pointer.get("testSHA256") == test_sha and
         pointer.get("evidenceDirectory") == str(evidence), "Independent preflight admission pointer conflicts")
    raw = fresh.avd_identity.read_bytes(evidence / "admission.json", limit=1024 * 1024)
    need(hashlib.sha256(raw).hexdigest() == pointer.get("sha256"), "Independent preflight admission bytes changed")
    row = life.strict_json(raw.decode("utf8")); directory, legacy, hashes, phases = _legacy_preflight()
    prior_gate(row, 0, s.api, s.serial, test_sha)
    need(row.get("protocol") == "M10_B1B2_PREFLIGHT_READ_ONLY_RESUME_V1" and row.get("originalStatus") == "UNKNOWN" and
         row.get("originalsSHA256") == hashes and row.get("runId") == legacy["result.json"]["runId"] and
         row.get("instrumentationCommandsIssued") == 0, "Independent admission changed the old UNKNOWN or source")
    grouped = {}
    for path, entry in row["records"].items():
        name = path.rsplit("/", 1)[1]
        need(entry.get("file") == "preflight-" + name and name in ("attempt.json", "result.json"), "Admission original path conflicts")
        grouped[path] = (fresh.avd_identity.read_bytes(evidence / entry["file"], limit=16384).decode("utf8"), entry["metadata"])
    proof = preflight_records(legacy["before.json"], legacy["after.json"], row["runId"], grouped, phases, row["bootId"])
    need(row.get("deviceProof") == proof and row.get("capture") == legacy["result.json"]["capture"] and
         row.get("instrumentation") == legacy["result.json"]["instrumentation"], "Independent admission revalidation differs")
    current = row["continuationSnapshot"]
    _same_preflight_environment(legacy["after.json"], current)
    current_rows = _preflight_processes(current.get("processes"), current["uid"])
    need(not current_rows or (current_rows[0]["pid"] == proof["attempt"]["hostPid"] and
         current_rows[0]["startTicks"] == proof["attempt"]["hostStartTicks"]), "Resume encountered another live Host instance")
    need(all(current.get(k) == legacy["after.json"].get(k) for k in ("stages", "observations", "diagnostics", "safety")),
         "Admission continuation baseline conflicts")
    return row, directory

def private_path(path, *, content=False):
    need(isinstance(path, str) and len(path) <= 512, "Invalid private path")
    parts = path.split("/")
    need(parts[0] == "no_backup" and all(re.fullmatch(r"[A-Za-z0-9._-]+", p) and p not in (".", "..") for p in parts), "Private path traversal")
    if not content:
        need(len(parts) == 1 or parts[1] in {"m10-b1b2-safety", "m10-b2-resource-diagnostics", "m10-jpeg-observations"} or ROOT_RE.fullmatch(parts[1]), "Metadata outside new synthetic roots")
        return path
    safety = len(parts) == 4 and parts[1] == "m10-b1b2-safety" and re.fullmatch(UUID, parts[2]) and parts[3] in SAFETY_NAMES
    diagnostic = len(parts) == 4 and parts[1] == "m10-b2-resource-diagnostics" and re.fullmatch(UUID, parts[2]) and parts[3] in DIAG_NAMES
    observation = len(parts) == 3 and parts[1] == "m10-jpeg-observations" and OBS_RE.fullmatch(parts[2])
    stage = len(parts) == 6 and ROOT_RE.fullmatch(parts[1]) and parts[2:4] == ["media", ".m10-jpeg-v2"] and re.fullmatch(UUID, parts[4]) and parts[5] in RECORD_NAMES
    need(safety or diagnostic or observation or stage, "Content outside explicit synthetic evidence whitelist")
    return path


def private(s, script):
    return s.adb("shell", "run-as " + APP + " sh -c " + shlex.quote(script), timeout=30)


def metadata(s, path, uid):
    private_path(path)
    return parse_metadata(private(s, "stat -c '%f|%d|%i|%s|%u|%h' '" + path + "'"), uid)


def parse_metadata(text, uid):
    values = text.split("|")
    need(len(values) == 6 and re.fullmatch(r"[0-9a-f]+", values[0]) and all(re.fullmatch(r"0|[1-9][0-9]*", x) for x in values[1:]), "Incomplete synthetic metadata")
    mode = int(values[0], 16)
    need(not stat.S_ISLNK(mode) and (stat.S_ISREG(mode) or stat.S_ISDIR(mode)) and int(values[4]) == uid, "Synthetic owner/type conflict")
    need(not stat.S_ISREG(mode) or int(values[5]) == 1, "Multiply linked synthetic file")
    return {"type": "regular" if stat.S_ISREG(mode) else "directory", "device": int(values[1]), "inode": int(values[2]), "bytes": int(values[3]), "uid": uid, "links": int(values[5])}


def metadata_many(s, paths, uid):
    paths = list(paths)
    need(len(paths) <= 64 and len(paths) == len(set(paths)), "Metadata group oversized/duplicate")
    script = "set -e; printf 'M10_META_BEGIN\\n'; "
    for path in paths:
        private_path(path)
        script += f"[ ! -L '{path}' ]; stat -c 'M|{path}|%f|%d|%i|%s|%u|%h' '{path}'; "
    script += f"printf 'M10_META_END|{len(paths)}\\n'"
    raw = private(s, script); rows = raw.splitlines()
    need(len(raw) <= 32768 and len(rows) == len(paths) + 2 and rows[0] == "M10_META_BEGIN" and rows[-1] == f"M10_META_END|{len(paths)}", "Metadata group truncated/unknown")
    result = {}
    for path, line in zip(paths, rows[1:-1]):
        need(line.startswith("M|" + path + "|"), "Metadata group path/order mismatch")
        result[path] = parse_metadata(line[len(path) + 3:], uid)
    return result


def parse_bundle(text, paths, uid, limits, *, required_hashes=False):
    """Pure parser for bounded raw bytes and before/after inode/length checks."""
    need(len(text.encode()) <= sum(4 * ((limits[p] + 3) // 3) + limits[p] // 40 + 1024 for p in paths) + 65536, "Evidence group exceeds bound")
    lines = text.splitlines(); need(lines and lines.pop(0) == "M10_BUNDLE_BEGIN", "Evidence group header missing")
    result = {}; ancestors = {}
    while lines and lines[0].startswith("A0|"):
        _, path, meta = lines.pop(0).split("|", 2)
        need(path not in ancestors and parse_metadata(meta, uid)["type"] == "directory", "Ancestor metadata conflict")
        ancestors[path] = meta
    expected_ancestors = sorted({"/".join(path.split("/")[:i]) for path in paths for i in range(1, len(path.split("/")))})
    need(sorted(ancestors) == expected_ancestors, "Missing/foreign synthetic ancestor proof")
    for path in paths:
        need(lines and lines[0].startswith("B|" + path + "|"), "Evidence group file/order mismatch")
        before = parse_metadata(lines.pop(0)[len(path) + 3:], uid)
        need(before["type"] == "regular" and 0 < before["bytes"] <= limits[path], "Record size/type invalid")
        file_hash = None
        if lines and lines[0].startswith("H0|"):
            value = lines.pop(0).split("|")
            need(len(value) == 3 and value[1] == path and re.fullmatch(r"[0-9a-f]{64}", value[2]), "Invalid before-file SHA")
            file_hash = value[2]
        need(not required_hashes or file_hash is not None, "Live reader requires device before/after SHA")
        encoded = []
        while lines and not lines[0].startswith(("E|", "H1|")):
            encoded.append(lines.pop(0))
        if file_hash is not None:
            need(lines and lines.pop(0) == "H1|" + path + "|" + file_hash, "File SHA changed during read")
        need(lines and lines[0].startswith("E|" + path + "|"), "Evidence group file terminator missing")
        after = parse_metadata(lines.pop(0)[len(path) + 3:], uid)
        raw = base64.b64decode("".join(encoded), validate=True)
        need(before == after and len(raw) == before["bytes"] and len(raw) <= limits[path], "Record changed/truncated while reading")
        need(file_hash is None or hashlib.sha256(raw).hexdigest() == file_hash, "Device/file/payload SHA mismatch")
        result[path] = (raw.decode("utf-8", "strict"), {**before, "sha256": hashlib.sha256(raw).hexdigest()})
    for path in expected_ancestors:
        need(lines and lines.pop(0) == "A1|" + path + "|" + ancestors[path], "Synthetic ancestor changed during read")
    need(lines == [f"M10_BUNDLE_END|{len(paths)}"], "Evidence group incomplete or extra output")
    return result


def bundle_script(paths, limits=None):
    """Bounded read with upstream pipeline failures preserved by the remote shell.
    Android dd/pipefail capability is not assumed from the host regression.
    """
    paths = sorted(paths); need(0 < len(paths) <= 32 and len(paths) == len(set(paths)), "Evidence group bound/duplicate")
    for path in paths: private_path(path, content=True)
    limits = {path: (limits or {}).get(path, 16384) for path in paths}
    need(all(type(n) is int and 1 <= n <= 2 * 1024 * 1024 for n in limits.values()), "Invalid record bound")
    ancestors = sorted({"/".join(path.split("/")[:i]) for path in paths for i in range(1, len(path.split("/")))})
    script = "set -e; set -o pipefail; printf 'M10_BUNDLE_BEGIN\\n'; "
    for path in ancestors:
        private_path(path)
        script += f"[ ! -L '{path}' ]; [ -d '{path}' ]; stat -c 'A0|{path}|%f|%d|%i|%s|%u|%h' '{path}'; "
    for path in paths:
        limit = limits[path]
        # The extra block makes concurrent growth observable without byte-at-a-time I/O.
        # All three reads stay bounded even if a writer grows the file after stat.
        bounded = f"dd if='{path}' bs=65536 count={limit // 65536 + 1}"
        script += f"[ ! -L '{path}' ]; [ -f '{path}' ]; n=$(stat -c '%s' '{path}'); [ \"$n\" -gt 0 ]; [ \"$n\" -le {limit} ]; stat -c 'B|{path}|%f|%d|%i|%s|%u|%h' '{path}'; "
        script += f"h=$({bounded} | sha256sum); h=${{h%% *}}; printf 'H0|{path}|%s\\n' \"$h\"; {bounded} | base64; h=$({bounded} | sha256sum); h=${{h%% *}}; printf 'H1|{path}|%s\\n' \"$h\"; stat -c 'E|{path}|%f|%d|%i|%s|%u|%h' '{path}'; "
    for path in ancestors:
        script += f"[ ! -L '{path}' ]; stat -c 'A1|{path}|%f|%d|%i|%s|%u|%h' '{path}'; "
    script += f"printf 'M10_BUNDLE_END|{len(paths)}\\n'"
    return script


def validate_dd_stderr(raw, file_sizes):
    """Accept only exact successful dd accounting, never discard remote stderr."""
    need(isinstance(raw, bytes) and len(raw) <= 65536, "Reader stderr oversized/unknown")
    lines = raw.decode("ascii", "strict").splitlines()
    passes = 0
    for size in file_sizes:
        for _ in range(3):
            full, partial = divmod(size, 65536)
            expected = f"{full}+{int(partial != 0)} records "
            need(len(lines) >= 2 and lines.pop(0) == expected + "in" and lines.pop(0) == expected + "out", "dd record accounting missing/conflicting")
            if lines and re.match(r"[0-9]+ bytes ", lines[0]):
                line = lines.pop(0)
                number = r"[0-9]+(?:\.[0-9]+)?(?:e[+-]?[0-9]+)?"
                gnu = rf"{size} bytes(?: \([^\r\n()]{{1,64}}\))? copied, {number} s, {number} [A-Za-z]+/s"
                android = rf"{size} bytes transferred in {number} secs \({number} bytes/sec\)"
                need(re.fullmatch(gnu, line) is not None or re.fullmatch(android, line) is not None, "Unknown dd transfer diagnostic")
            passes += 1
    need(not lines, "Unexpected reader stderr")
    return {"passes": passes, "stderrBytes": len(raw), "stderrSHA256": hashlib.sha256(raw).hexdigest()}


def reader_command_proof(s, offset, text, paths, sizes):
    """Bind stdout/stderr to exactly this serial command in its append-only journal."""
    maximum = sum(4 * ((size + 2) // 3) for size in sizes) * 2 + 256 * 1024
    with s.journal.open("rb") as stream:
        stream.seek(offset)
        addition = stream.read(maximum + 1)
    need(0 < len(addition) <= maximum and addition.endswith(b"\n") and len(addition.splitlines()) == 1, "Reader command journal missing/overlapped/truncated")
    row = life.strict_json(addition.decode("utf-8"))
    argv = row.get("argv")
    expected_shell = "run-as " + APP + " sh -c " + shlex.quote(bundle_script(paths, {p: n for p, n in zip(paths, sizes)}))
    # sizes passed here are read limits, not final byte counts; actual sizes come from parser below.
    need(isinstance(argv, list) and len(argv) == 5 and Path(argv[0]).name.lower() == "adb.exe" and argv[1:] == ["-s", s.serial, "shell", expected_shell], "Reader journal command/serial mismatch")
    need(row.get("exitCode") == 0 and isinstance(row.get("utc"), str) and isinstance(row.get("endUtc"), str), "Reader command status/time missing")
    from datetime import datetime
    start, end = (datetime.fromisoformat(row[k]) for k in ("utc", "endUtc"))
    need(start.tzinfo is not None and end.tzinfo is not None and 0 <= (end - start).total_seconds() <= 30,
         "Reader command timing outside fixed 30s boundary")
    decoded = {}
    for key in ("stdout", "stderr"):
        raw = base64.b64decode(row.get(key + "Base64", ""), validate=True)
        need(len(raw) == row.get(key + "Bytes") and hashlib.sha256(raw).hexdigest() == row.get(key + "SHA256"), "Reader raw command integrity mismatch")
        decoded[key] = raw
    need(decoded["stdout"].decode("utf-8", "strict").strip() == text, "Reader returned bytes differ from command journal")
    return decoded


def read_bundle(s, paths, uid, limits=None):
    paths = sorted(paths)
    limits = {path: (limits or {}).get(path, 16384) for path in paths}
    script = bundle_script(paths, limits)
    need(not s.journal.is_symlink() and s.journal.is_file(), "Reader journal unavailable")
    offset = s.journal.stat().st_size
    text = private(s, script)
    command = reader_command_proof(s, offset, text, paths, [limits[p] for p in paths])
    result = parse_bundle(text, paths, uid, limits, required_hashes=True)
    validate_dd_stderr(command["stderr"], [result[p][1]["bytes"] for p in paths])
    return result


def exists(s, path):
    private_path(path)
    text = private(s, f"if [ -L '{path}' ]; then echo LINK; elif [ -e '{path}' ]; then echo PRESENT; else echo ABSENT; fi")
    need(text in ("PRESENT", "ABSENT"), "Synthetic path linked or unknown")
    return text == "PRESENT"


def names(s, path, uid):
    need(metadata(s, path, uid)["type"] == "directory", "Synthetic directory missing")
    raw = private(s, "ls -1A '" + private_path(path) + "'")
    found = raw.splitlines() if raw else []
    need(len(raw.encode()) <= 65536 and len(found) <= 128 and len(found) == len(set(found)) and all(re.fullmatch(r"[A-Za-z0-9._-]+", n) and n not in (".", "..") for n in found), "Directory listing incomplete/unknown")
    return sorted(found)


def read_private(s, path, uid, limit=16384):
    return read_bundle(s, [path], uid, {path: limit})[path]


def json_private(s, path, uid, limit=16384):
    text, meta = read_private(s, path, uid, limit)
    value = life.strict_json(text)
    need(isinstance(value, dict), "Synthetic JSON is not an object")
    return {"value": value, "metadata": meta}


def stage_snapshot(s, root, uid):
    need(names(s, "no_backup/" + root, uid) == ["media"], "Unknown synthetic generation child")
    media = "no_backup/" + root + "/media"
    need(names(s, media, uid) == [".m10-jpeg-v2"], "Unknown synthetic media child")
    namespace = media + "/.m10-jpeg-v2"
    children = names(s, namespace, uid)
    need(".namespace.guard" in children, "Namespace guard absent")
    namespace_guard = metadata(s, namespace + "/.namespace.guard", uid)
    need(namespace_guard["type"] == "regular" and namespace_guard["bytes"] == 0, "Namespace guard invalid")
    assets = [x for x in children if x != ".namespace.guard"]
    need(len(assets) == 1 and re.fullmatch(UUID, assets[0]), "Unknown or multiple synthetic assets")
    path = namespace + "/" + assets[0]
    records, entries, payload = {}, {}, {}
    children = names(s, path, uid)
    content_paths = [path + "/" + n for n in children if n in RECORD_NAMES]
    grouped = read_bundle(s, content_paths, uid) if content_paths else {}
    meta_paths = [path + "/" + n for n in children if n in ("source.bin", "image.jpg", "consumer.guard")]
    grouped_metadata = metadata_many(s, meta_paths, uid) if meta_paths else {}
    for child in children:
        full = path + "/" + child
        if child in ("source.bin", "image.jpg"):
            payload[child] = grouped_metadata[full]  # Never opens image/source bytes.
        elif child == "consumer.guard":
            entry = grouped_metadata[full]
            need(entry["type"] == "regular" and entry["bytes"] == 0, "Consumer guard invalid")
            records[child] = ""; entries[child] = {**entry, "sha256": hashlib.sha256(b"").hexdigest()}
        else:
            need(child in RECORD_NAMES, "Unknown Stage child; preserve it")
            records[child], entries[child] = grouped[full]
    records["__metadata__"] = json.dumps({"complete": True, "entries": entries}, sort_keys=True)
    return {"asset": assets[0], "records": records, "remainingPayloadMetadata": payload, "namespaceGuard": namespace_guard}


def snapshot(s, test_sha):
    identity = s.identity()
    app_uid = s.installed(APP, APP_SHA); test_uid = s.installed(TEST, test_sha)
    app_proof = s.require_proof(s.home / "install-app-proof.json")
    test_proof = s.require_proof(s.home / "install-test-proof.json")
    need(app_proof.get("kind") == "app" and app_proof.get("package") == APP and app_proof.get("sha256") == APP_SHA and app_proof.get("uid") == app_uid,
         "Current App differs from its independent install proof")
    need(test_proof.get("kind") == "test" and test_proof.get("package") == TEST and test_proof.get("sha256") == test_sha and test_proof.get("uid") == test_uid,
         "Current Test differs from its independent install proof")
    need(app_uid != test_uid and private(s, "id -u") == str(app_uid), "Independent App/Test/run-as UID conflict")
    result = {"identity": identity, "uid": app_uid, "testUID": test_uid, "appSHA256": APP_SHA, "testSHA256": test_sha,
              "processes": dev.processes(s, app_uid), "stages": {}, "observations": {}, "diagnostics": {}, "safety": {}}
    if not exists(s, "no_backup"): return result
    for name in names(s, "no_backup", app_uid):
        if ROOT_RE.fullmatch(name): result["stages"][name] = stage_snapshot(s, name, app_uid)
        elif name == "m10-jpeg-observations":
            for child in names(s, "no_backup/" + name, app_uid):
                need(OBS_RE.fullmatch(child), "Unknown Worker observation")
                result["observations"][child] = json_private(s, "no_backup/" + name + "/" + child, app_uid)
        elif name in ("m10-b1b2-safety", "m10-b2-resource-diagnostics"):
            destination = result["safety" if name == "m10-b1b2-safety" else "diagnostics"]
            for attempt in names(s, "no_backup/" + name, app_uid):
                need(re.fullmatch(UUID, attempt), "Unknown diagnostic attempt")
                path = "no_backup/" + name + "/" + attempt
                items = names(s, path, app_uid)
                need(set(items) <= (SAFETY_NAMES if name == "m10-b1b2-safety" else DIAG_NAMES), "Unknown diagnostic child")
                need("callback-error.json" not in items, "Callback error persisted: stop")
                grouped = metadata_many(s, [path + "/" + x for x in items], app_uid)
                destination[attempt] = {"children": items, "files": {x: grouped[path + "/" + x] for x in items}}
        elif name.startswith("m10-"):
            raise RuntimeError("Unknown synthetic namespace; preserve it")
    return result


def terminal_stage(stage, expected_source=None, observed=None, abandoned=False, allow_failed_output=False):
    need(not stage["remainingPayloadMetadata"], "Source/output still present; consumer closure unknown")
    records = stage["records"]; base = {"owner.v2", "source.v2", "output.v2", "pin.v2", "delivered.v2", "revoked.v2", "consumer.guard", "__metadata__"}
    base |= {"supervisor.v2", "abandoned.v2", "abandoned-cleanup-intent.v2", "abandoned-cleaned.v2"} if abandoned else {"terminal.v2", "prepared.v2", "cleanup-intent.v2", "cleaned.v2"}
    if "supervisor.v2" in records: base.add("supervisor.v2")
    need(set(records) == base, "Unexpected/missing terminal Stage proof")
    metadata_record = life.strict_json(records["__metadata__"])
    need(metadata_record.get("complete") is True and set(metadata_record.get("entries", {})) == base - {"__metadata__"}, "Stage byte inventory incomplete")
    entries = metadata_record["entries"]
    for key, entry in entries.items():
        need(entry.get("type") == "regular" and entry.get("bytes") == len(records[key].encode("ascii")) and entry.get("sha256") == life.digest(records[key]), "Stage record byte/stat/hash mismatch")
    owner = life.canonical_record(records["owner.v2"], life.OWNER_KEYS); oh = life.digest(records["owner.v2"])
    need(owner["protocol"] == "lithic-m10-jpeg-stage" and owner["version"] == "2" and owner["assetId"] == stage["asset"], "Stage owner/protocol conflict")
    need(all(re.fullmatch(UUID, owner[key]) for key in ("assetId", "attemptId", "sessionId", "nonce")), "Stage UUID malformed")
    guard = entries["consumer.guard"]
    need(records["consumer.guard"] == "" and int(owner["guardDevice"]) == guard["device"] and int(owner["guardInode"]) == guard["inode"], "Actual consumer guard differs from owner")
    need(owner["decoderPolicy"] == "libjpeg-turbo-3.2.0-e01-strict-v1" and owner["normalizationPolicy"] == "m10-jpeg-normalize-v1", "Frozen image policy conflict")
    source = life.canonical_record(records["source.v2"], life.FILE_KEYS)
    output = life.canonical_record(records["output.v2"], life.FILE_KEYS)
    need(source["ownerHash"] == output["ownerHash"] == oh and output["bytes"] == "0" and output["sha256"] == life.digest(""), "File source/creation identity conflict")
    if expected_source is not None: need((int(source["bytes"]), source["sha256"]) == expected_source, "Locked source mismatch")
    pin = life.canonical_record(records["pin.v2"], life.PIN_KEYS)
    need(pin["ownerHash"] == oh and pin["sourceHash"] == source["sha256"], "Pin source/owner conflict")
    need(life.canonical_record(records["delivered.v2"], {"ownerHash", "pinHash"}) == {"ownerHash": oh, "pinHash": life.digest(records["pin.v2"])}, "Persistent delivery/pin mismatch")
    revoked = life.canonical_record(records["revoked.v2"], {"ownerHash", "reasonHash"})
    need(revoked["ownerHash"] == oh and re.fullmatch(r"[a-f0-9]{64}", revoked["reasonHash"]), "Revocation not authenticated")
    if observed:
        need((owner["generation"], owner["attemptId"], owner["sessionId"]) == (observed["generation"], observed["attempt"], observed["session"]), "Stage attempt identity mismatch")
        need(tuple(int(pin[k]) for k in ("pid", "uid", "startTicks")) == (observed["workerPid"], observed["workerUid"], observed["workerStartTicks"]), "Pinned Worker mismatch")
        for key in ("asset", "nonce"):
            if key in observed: need(owner["assetId" if key == "asset" else "nonce"] == observed[key], "Stage asset/nonce differs from observation")
    if "supervisor.v2" in records:
        supervisor = life.canonical_record(records["supervisor.v2"], {"ownerHash", "bootId", "pid", "uid", "startTicks"})
        need(supervisor["ownerHash"] == oh and re.fullmatch(r"6c697468-6963-4000-8000-[0-9a-f]{12}", supervisor["bootId"]), "Supervisor ownership/boot mismatch")
        if observed:
            need(int(supervisor["pid"]) == observed["hostPid"] and int(supervisor["uid"]) == observed["hostUid"], "Supervisor/observed Host mismatch")
            if "hostStartTicks" in observed: need(int(supervisor["startTicks"]) == observed["hostStartTicks"], "Supervisor Host instance changed")
            if "bootId" in observed: need(supervisor["bootId"] == observed["bootId"], "Supervisor boot changed")
    if abandoned:
        keys = {"version", "ownerHash", "supervisorHash", "pinHash", "sourceRecordHash", "sourceHash", "outputDevice", "outputInode", "outputBytes", "outputSha", "observedBootId", "hostClosure", "workerEnded", "hostFdsClosed", "noFutureRpc", "connectionCleaned", "noConsumerActorsEnded", "proof"}
        receipt = life.canonical_record(records["abandoned.v2"], keys)
        need(receipt["version"] == "1" and receipt["ownerHash"] == oh and receipt["supervisorHash"] == life.digest(records["supervisor.v2"]) and receipt["pinHash"] == life.digest(records["pin.v2"]) and receipt["sourceRecordHash"] == life.digest(records["source.v2"]) and receipt["sourceHash"] == source["sha256"], "Independent abandoned receipt mismatch")
        need(receipt["hostClosure"] == "SAME_SESSION_QUIESCENT" and receipt["observedBootId"] == supervisor["bootId"] and receipt["proof"] == "NO_RESULT_CONSUMER" and receipt["noConsumerActorsEnded"] == "false", "Wrong recovery authority")
        need(all(receipt[k] == "true" for k in ("workerEnded", "hostFdsClosed", "noFutureRpc", "connectionCleaned")), "Abandoned actors not ended")
        need(receipt["outputDevice"] == output["device"] and receipt["outputInode"] == output["inode"], "Abandoned output inode changed")
        if allow_failed_output:
            need(owner["generation"].startswith("m10-b1-delay-") and 0 <= int(receipt["outputBytes"]) <= 2097152 and re.fullmatch(r"[a-f0-9]{64}", receipt["outputSha"]), "Delayed-return abandoned output invalid")
            if receipt["outputBytes"] == "0": need(receipt["outputSha"] == life.digest(""), "Empty failed output SHA mismatch")
        else: need(receipt["outputBytes"] == "0" and receipt["outputSha"] == life.digest(""), "Pre-FD output identity changed")
        expected = {"ownerHash": oh, "abandonedHash": life.digest(records["abandoned.v2"])}
        for key in ("abandoned-cleanup-intent.v2", "abandoned-cleaned.v2"): need(life.canonical_record(records[key], set(expected)) == expected, "Abandoned cleanup mismatch")
    else:
        terminal = life.canonical_record(records["terminal.v2"], life.TERMINAL_KEYS)
        need(terminal["ownerHash"] == oh and terminal["pinHash"] == life.digest(records["pin.v2"]) and terminal["sourceHash"] == source["sha256"] and terminal["outcome"] == "SUCCESS" and terminal["resultCount"] == "1", "Terminal identity/outcome mismatch")
        need(all(terminal[k] == "true" for k in ("sourceFdClosed", "outputFdClosed", "binderDead", "matchingProcessDead", "strictFinished", "decoderDestroyed", "connectionCleaned")), "Missing successful termination fact")
        need(terminal["outputDevice"] == output["device"] and terminal["outputInode"] == output["inode"] and 0 < int(terminal["outputBytes"]) <= 2097152 and all(0 < int(terminal[k]) <= 1600 for k in ("width", "height")) and re.fullmatch(r"[a-f0-9]{64}", terminal["outputSha"]), "Normalized output contract mismatch")
        expected = {"ownerHash": oh, "terminalHash": life.digest(records["terminal.v2"])}
        for key in ("prepared.v2", "cleanup-intent.v2", "cleaned.v2"): need(life.canonical_record(records[key], set(expected)) == expected, "Terminal cleanup mismatch")
    return {"owner": owner, "source": source, "pin": pin}


def scoped_lines(raw, diagnostic, prior_diagnostics, prior_events):
    need(isinstance(raw, str) and raw.endswith("\n") and "\ufffd" not in raw, "Incomplete capture encoding/tail")
    header = "--------- beginning of main"
    lines = raw.splitlines()
    need(lines.count(header) <= 1 and (header not in lines or lines[0] == header), "Repeated/misplaced log header")
    known = set(prior_events); seen = set(); found = []; ready = False; diagnostics = set()
    for line in raw.splitlines(keepends=True):
        if line.strip() == "--------- beginning of main": continue
        if line == diagnostic:
            need(not ready, "Duplicate current diagnostic"); ready = True; continue
        if line in prior_diagnostics:
            need(not ready and line not in diagnostics, "Duplicate/late old diagnostic"); diagnostics.add(line); continue
        need(line not in seen, "Duplicate lifecycle event"); seen.add(line)
        if line in known:
            need(not ready, "Historical Worker event after readiness"); continue
        need(ready and re.fullmatch(r"\d\d-\d\d\s+\d\d:\d\d:\d\d\.\d{3,6}\s+\d+\s+\d+\s+I\s+LITHIC_JPEG\s*:\s*[A-Z_]+ attempt=[A-Za-z0-9._:-]+ session=[A-Za-z0-9._:-]+ worker=\d+/\d+/\d+ at=\d+\s*\n", line), "Unknown/interrupted/hook-error lifecycle line")
        found.append(line.rstrip("\r\n"))
    need(ready, "Authenticated readiness absent")
    return "\n".join(found)


def delta(before, after, key):
    need(all(after[key].get(k) == v for k, v in before[key].items()), "Prior immutable synthetic evidence changed: " + key)
    return {k: v for k, v in after[key].items() if k not in before[key]}


def observed_identity(value):
    return {k: value[k] for k in ("generation", "attempt", "session", "hostPid", "hostUid", "workerPid", "workerUid", "workerStartTicks")}


def cancellation_phases(text, observed):
    allowed = {"WORKER_REVOKE_ARMED", "HOST_REVOKE_ACKNOWLEDGED", "HOST_UNBIND_RETURNED",
               "WORKER_ON_UNBIND", "WORKER_EXIT_AUTHENTICATED_DETACH", "HOST_BINDER_DIED"}
    events = {}
    for line in text.splitlines():
        match = re.fullmatch(r"\d\d-\d\d\s+\d\d:\d\d:\d\d\.\d{3,6}\s+(\d+)\s+(\d+)\s+I\s+LITHIC_JPEG\s*:\s*([A-Z_]+) attempt=([A-Za-z0-9._:-]+) session=([A-Za-z0-9._:-]+) worker=(\d+)/(\d+)/(\d+) at=(\d+)\s*", line)
        need(match is not None, "Unknown cancelled lifecycle line")
        emitter, _, phase, attempt, session, pid, uid, start, at = match.groups()
        need(phase in allowed and phase not in events and (attempt, session) == (observed["attempt"], observed["session"]), "Duplicate/foreign cancellation event")
        need((int(pid), int(uid), int(start)) == (observed["workerPid"], observed["workerUid"], observed["workerStartTicks"]) and int(emitter) == observed["hostPid" if phase.startswith("HOST_") else "workerPid"], "Cancelled Worker/emitter identity conflict")
        events[phase] = int(at)
    need(set(events) == allowed, "Incomplete authenticated cancelled retirement")
    for first, last in (("WORKER_REVOKE_ARMED", "HOST_REVOKE_ACKNOWLEDGED"),
                        ("HOST_REVOKE_ACKNOWLEDGED", "HOST_UNBIND_RETURNED"),
                        ("HOST_REVOKE_ACKNOWLEDGED", "WORKER_ON_UNBIND"),
                        ("WORKER_ON_UNBIND", "WORKER_EXIT_AUTHENTICATED_DETACH"),
                        ("WORKER_EXIT_AUTHENTICATED_DETACH", "HOST_BINDER_DIED")):
        need(events[first] <= events[last], "Cancelled retirement partial order conflict")
    return events


def cancellation_observations(rows, observed):
    need(isinstance(rows, list) and 1 <= len(rows) <= 2, "Cancelled observation bound exceeded")
    origins = set(); complete = 0
    facts = ("hostFdsClosed", "rpcQuiescent", "originalSessionQuiescent", "binderDead", "matchingProcessDead", "guardReleased", "connectionCleaned")
    for ordinal, row in enumerate(rows, 1):
        origin = row.get("origin")
        need(row.get("ordinal") == ordinal and origin in ("INITIAL", "LATE_ORIGINAL_QUIESCENCE") and origin not in origins, "Cancelled observation source/order conflict")
        origins.add(origin)
        need(row.get("status") == "OBSERVED_NOT_CLEANUP_AUTHORITY" and row.get("outcome") in ("CANCELLED", "RESULT_UNKNOWN") and row.get("resultCount") == 0 and row.get("sourceFdClosed") is False and row.get("outputFdClosed") is False, "Invented Worker RESULT/data FD closure")
        if row.get("workerPid") is not None:
            need(all(row.get(key) == observed[key] for key in ("workerPid", "workerUid", "workerStartTicks")), "Cancelled observation Worker changed")
        ready = row.get("outcome") == "CANCELLED" and all(row.get(k) is True for k in facts) and all(row.get(k) == observed[k] for k in ("workerPid", "workerUid", "workerStartTicks"))
        need(row.get("originalSessionQuiescent") is not True or ready, "Inconsistent actual quiescence facts")
        if origin == "LATE_ORIGINAL_QUIESCENCE": need(row.get("detail") == "LATE_FAILED_SESSION_QUIESCENCE" and ready, "Late closure not proved")
        else: need(row.get("detail") != "LATE_FAILED_SESSION_QUIESCENCE", "Late observation mislabeled as initial")
        complete += int(ready)
    need(complete == 1, "Exactly one complete original-session closure required")
    return {"observations": len(rows), "completeClosureClaims": complete, "initialUnknownNotChangedToPass": True}


def delayed_return_observations(first, late, checkpoint, observed):
    keys = ("workerPid", "workerUid", "workerStartTicks")
    facts = ("hostFdsClosed", "rpcQuiescent", "originalSessionQuiescent", "binderDead", "matchingProcessDead", "guardReleased", "connectionCleaned")
    need(first.get("origin") == "INITIAL" and late.get("origin") == "LATE_ORIGINAL_QUIESCENCE", "Delayed-return observation origins conflict")
    for row in (first, late):
        need(row.get("status") == "OBSERVED_NOT_CLEANUP_AUTHORITY" and row.get("outcome") == "TIMEOUT" and type(row.get("resultCount")) is int and row["resultCount"] in (0, 1) and all(row.get(k) == observed[k] for k in keys), "Real delayed result/identity conflict")
        need(all(type(row.get(k)) is bool for k in ("sourceFdClosed", "outputFdClosed", *facts)), "Actual close flags missing")
    need(first.get("detail") != "LATE_FAILED_SESSION_QUIESCENCE" and late.get("detail") == "LATE_FAILED_SESSION_QUIESCENCE", "Late handoff mislabeled")
    need(all(first.get(k) is False for k in ("rpcQuiescent", "originalSessionQuiescent", "connectionCleaned")) and first.get("unbindCallsWhileReturnHeld") == 0 and first.get("durableRevokeConfirmed") is True, "Unproved initial failure used as cleanup authority")
    need(all(late.get(k) is True for k in facts) and late["resultCount"] == first["resultCount"], "Late original-session actors/result fence not closed")
    need(all(checkpoint.get(k) is True for k in ("actualKernelTransactAlreadyReturned", "realReturnAccepted", "clientJavaReturnStillHeld", "guardActuallyHeld", "activeCleanupRejected", "sourceIdentityPreserved")) and all(checkpoint.get(k) == observed[k] for k in keys) and type(checkpoint.get("realReturnElapsedMs")) is int and checkpoint["realReturnElapsedMs"] > 0, "Real Binder return/guard checkpoint absent")
    return {"realKernelTransactAlreadyReturned": True, "onlyTestJavaReturnDelayed": True,
            "initialFailureNotCleanupAuthority": True, "lateOriginalSessionClosure": True,
            "permanentlyBlockedKernelRPC": "NOT_RUN"}


def delayed_return_phases(text, observed):
    """This failure path has no fabricated RETIRE/REVOKE ACK.
    A rejected late RESULT can arm fail-closed detach before onUnbind; both actual
    branches still require the matched Binder death and independent OS/guard proof.
    """
    allowed = {"HOST_UNBIND_RETURNED", "WORKER_ON_UNBIND", "UNPREPARED_DETACH",
               "WORKER_EXIT_UNPREPARED_DETACH", "WORKER_EXIT_AUTHENTICATED_DETACH",
               "HOST_BINDER_DIED", "RESULT_REJECTED"}
    events = {}
    for line in text.splitlines():
        match = re.fullmatch(r"\d\d-\d\d\s+\d\d:\d\d:\d\d\.\d{3,6}\s+(\d+)\s+(\d+)\s+I\s+LITHIC_JPEG\s*:\s*([A-Z_]+) attempt=([A-Za-z0-9._:-]+) session=([A-Za-z0-9._:-]+) worker=(\d+)/(\d+)/(\d+) at=(\d+)\s*", line)
        need(match is not None, "Unknown delayed-return lifecycle line")
        emitter, _, phase, attempt, session, pid, uid, start, at = match.groups()
        need(phase in allowed and phase not in events and (attempt, session) == (observed["attempt"], observed["session"]), "Duplicate/foreign delayed-return event")
        need((int(pid), int(uid), int(start)) == (observed["workerPid"], observed["workerUid"], observed["workerStartTicks"]) and int(emitter) == observed["hostPid" if phase.startswith("HOST_") else "workerPid"], "Delayed-return Worker/emitter identity conflict")
        events[phase] = int(at)
    mandatory = {"HOST_UNBIND_RETURNED", "WORKER_ON_UNBIND", "HOST_BINDER_DIED"}
    need(mandatory <= set(events), "Delayed-return binding/death evidence missing")
    if "UNPREPARED_DETACH" in events:
        need("WORKER_EXIT_UNPREPARED_DETACH" in events and "WORKER_EXIT_AUTHENTICATED_DETACH" not in events and
             events["WORKER_ON_UNBIND"] <= events["UNPREPARED_DETACH"] <= events["WORKER_EXIT_UNPREPARED_DETACH"] <= events["HOST_BINDER_DIED"], "Unprepared detach partial order conflict")
        branch = "WORKER_EXIT_UNPREPARED_DETACH"
    else:
        need("WORKER_EXIT_AUTHENTICATED_DETACH" in events and "RESULT_REJECTED" in events and "WORKER_EXIT_UNPREPARED_DETACH" not in events and
             events["RESULT_REJECTED"] <= events["WORKER_ON_UNBIND"] <= events["WORKER_EXIT_AUTHENTICATED_DETACH"] <= events["HOST_BINDER_DIED"], "Rejected-result fail-closed detach not evidenced")
        branch = "RESULT_REJECTED_THEN_DETACH_NOT_HOST_ACK"
    return {"branch": branch, "events": events, "hostRetirementAcknowledgement": "NOT_OBSERVED_NOT_REQUIRED_FOR_ABANDONED_ONLY"}


def resource_proc(phase, observed):
    need(dev.proc_start(phase["procStatRaw"], observed["workerPid"]) == observed["workerStartTicks"], "Resource /proc start instance mismatch")
    fields = {}
    for line in phase["procStatusRaw"].splitlines():
        need(":" in line, "Resource status line malformed")
        key, value = line.split(":", 1); need(key not in fields, "Duplicate status field"); fields[key] = value.strip()
    need(fields.get("Pid") == fields.get("Tgid") == str(observed["workerPid"]) and fields.get("Uid", "").split() == [str(observed["workerUid"])] * 4, "Resource /proc PID/UID conflict")
    need(all(re.fullmatch(r"[0-9]+\s+kB", fields.get(k, "")) for k in ("VmRSS", "VmHWM")), "Resource RSS/HWM unavailable")
    need(int(fields["VmHWM"].split()[0]) >= int(fields["VmRSS"].split()[0]) >= 0, "Resource RSS/HWM conflict")


def evaluate(s, index, run_id, before, after, phases):
    uid = after["uid"]; need(uid == before["uid"] and after["testUID"] == before["testUID"], "Post-method UID changed")
    new_stages = delta(before, after, "stages"); new_obs = delta(before, after, "observations")
    new_diag = delta(before, after, "diagnostics"); new_safety = delta(before, after, "safety")
    if index == 0:
        need(not new_stages and not new_obs and not new_diag and set(new_safety) == {run_id} and not phases, "Reflection unexpectedly created a consumer")
        path = "no_backup/m10-b1b2-safety/" + run_id
        need(new_safety[run_id]["children"] == ["attempt.json", "result.json"], "Preflight evidence incomplete")
        grouped = read_bundle(s, [path + "/attempt.json", path + "/result.json"], uid)
        return preflight_records(before, after, run_id, grouped, phases)
    need(len(new_stages) == 1, "Expected exactly one new isolated Stage")
    generation, stage = next(iter(new_stages.items()))
    if index == 1:
        need(not new_diag and not new_safety and len(new_obs) == 2, "Healthy smoke observation set differs")
        wrappers = [{"filename": key, "value": row["value"]} for key, row in new_obs.items()]
        observed = life.observations_pair(wrappers, "SUCCESS")
        need(observed["generation"] == generation, "Healthy generation conflict")
        life.validate_stage(stage["records"], observed, "SUCCESS")
        checks = life.validate_phases(phases, observed)
    elif index in (2, 3, 4):
        need(not new_obs and not new_safety and len(new_diag) == 1, "40MP diagnostics not exclusive")
        attempt = next(iter(new_diag)); path = "no_backup/m10-b2-resource-diagnostics/" + attempt
        need(set(new_diag[attempt]["children"]) == DIAG_NAMES, "Incomplete resource phase set")
        paths = [path + "/" + name for name in DIAG_NAMES]
        grouped = read_bundle(s, paths, uid, {p: 2 * 1024 * 1024 if p.rsplit("/", 1)[1] in ("polling.json", "test-result.json") else 16384 for p in paths})
        evidence = {name: grouped[path + "/" + name][0] for name in DIAG_NAMES}
        s._resource_raw = evidence  # Preserve measured RED/UNKNOWN bytes before interpreting any field.
        result = life.strict_json(evidence["test-result.json"])
        need(result.get("status") == result.get("measurementStatus") == "MEASURED" and result.get("operationOutcome") == "SUCCESS" and result.get("closureStatus") == "TERMINAL_PROVED" and result.get("resourceEnded") is True and result.get("roomOrFormalCoverCommitted") is False and result.get("attempt") == attempt and result.get("generation") == generation, "Resource measurement/operation/closure not independently complete")
        need((result.get("sourceBytes"), result.get("sourceSha256")) == FIXTURES[index - 2], "40MP input differs")
        returned = result["workerResult"]
        need(returned.get("outcome") == "SUCCESS" and returned.get("resultCount") == 1 and all(returned.get(k) is True for k in ("sourceClosed", "outputClosed", "decoderDestroyed", "strictFinished", "binderDead", "matchingProcessDead", "guardReleased", "connectionCleaned")), "Incomplete actual Worker result")
        observed = {**{k: result[k] for k in ("generation", "asset", "attempt", "session", "nonce", "hostPid", "hostUid", "hostStartTicks", "bootId")}, **{k: returned[k] for k in ("workerPid", "workerUid", "workerStartTicks")}}
        claim = life.strict_json(evidence["worker-claim.json"])
        need(all(claim.get(k) == observed[k] for k in ("attempt", "session", "workerPid", "workerUid", "workerStartTicks")) and claim.get("nonce") == result["nonce"] and claim.get("bootId") == result["bootId"] and claim.get("allowSHA256") == hashlib.sha256(evidence["allow.properties"].encode()).hexdigest(), "Diagnostic permit/claim identity conflict")
        last = -1
        for phase_index, phase_name in enumerate(PHASE_NAMES):
            phase = life.strict_json(evidence[f"{phase_index:02d}-{phase_name}.json"])
            need(phase.get("status") == "MEASURED" and phase.get("errors") == [] and phase.get("phase") == phase_name and all(phase.get(k) == observed[k] for k in ("generation", "asset", "attempt", "session", "nonce")) and (phase.get("pid"), phase.get("uid"), phase.get("startTicks")) == (observed["workerPid"], uid, observed["workerStartTicks"]) and phase.get("bootId") == result["bootId"], "Resource phase identity/status mismatch")
            need(type(phase.get("elapsedRealtimeNs")) is int and phase["elapsedRealtimeNs"] > last and phase.get("pssKiB", 0) > 0, "Invalid phase clock/PSS")
            resource_proc(phase, observed)
            need(all(type(phase.get(k)) is int and phase[k] >= 0 for k in ("processCpuMs", "threadCpuNs", "nativeHeapAllocatedBytes", "javaUsedBytes", "observationCostWallNs", "openFdCountSample")), "Resource CPU/allocation/sample metric unknown")
            last = phase["elapsedRealtimeNs"]
            if phase_name == "NATIVE_COMPLETE":
                native = phase["extra"]["long16"]
                need(len(native) == 16 and all(type(n) is int for n in native) and native[0] == 0 and native[3:6] == [0, 0, 0] and native[6:8] == [1, 1] and native[8:11] == [5000, 5000, 1] and native[14:16] == [1, 1] and 0 <= native[2] <= 512 * 1024 * 1024 and all(native[i] >= 0 for i in (11, 12, 13)), "Native actual accounting/release incomplete")
            if phase_name == "FINISHED": need(phase["extra"] == {"outcome": "SUCCESS", "sourceClosed": True, "outputClosed": True, "scratchClean": True}, "Worker close/scratch evidence incomplete")
        polling = life.strict_json(evidence["polling.json"])
        need(polling.get("status") == "MEASURED" and polling.get("errors") == [] and polling.get("samplerStopped") is True and polling.get("samples") and all(row.get("pssKiB", 0) > 0 for row in polling["samples"]), "Resource polling UNKNOWN")
        need({row.get("role") for row in polling["samples"]} == {"HOST", "WORKER"} and polling.get("clockTicksPerSecond", 0) > 0, "Missing Host/Worker/CPU polling")
        for row in polling["samples"]:
            prefix = "host" if row["role"] == "HOST" else "worker"
            need((row.get("pid"), row.get("uid"), row.get("startTicks")) == (observed[prefix + "Pid"], uid, observed[prefix + "StartTicks"]) and row.get("bootId") == result["bootId"], "Polling identity/boot conflict")
            need(row["proc"].get("startTicks") == row["startTicks"] and row["proc"].get("hwmKiB", -1) >= row["proc"].get("rssKiB", -1) >= 0 and all(row["proc"].get(k, -1) >= 0 for k in ("utimeTicks", "stimeTicks")), "Polling metrics unknown")
        terminal_stage(stage, FIXTURES[index - 2], observed)
        checks = life.validate_phases(phases, observed)
    elif index == 5:
        need(not new_obs and not new_diag and set(new_safety) == {run_id}, "Cancellation used another synthetic namespace")
        path = "no_backup/m10-b1b2-safety/" + run_id
        children = new_safety[run_id]["children"]
        need(set(children) in ({"attempt.json", "result.json", "observation-01.json"},
                              {"attempt.json", "result.json", "observation-01.json", "observation-02.json"}), "Cancellation callback error/missing proof/extra evidence")
        grouped = read_bundle(s, [path + "/" + name for name in children], uid)
        evidence = {name: grouped[path + "/" + name][0] for name in children}
        s._cancellation_raw = evidence  # A failed assertion must not discard this real handoff.
        attempt = life.strict_json(evidence["attempt.json"]); result = life.strict_json(evidence["result.json"])
        need(attempt.get("runId") == result.get("runId") == run_id and attempt.get("method") == result.get("method") == METHODS[5] and attempt.get("hostUid") == uid, "Cancelled run identity mismatch")
        need(result.get("status") == "PASS" and result.get("callerOutcome") == "CANCELLED" and result.get("resourceEnded") is True and result.get("resultCount") == 0 and result.get("roomOrFormalCoverCommitted") is False, "Cancellation/closure is not actually proved")
        need(result.get("scope") == "AFTER_VERIFIED_HELLO_AND_PERSISTENT_PIN_BEFORE_DATA_FD_DELIVERY_ONLY" and all(result.get(k) == "NOT_RUN" for k in ("nativeComputeCancellation", "lateRpc", "workerFaultKill", "hostDeath")), "Cancellation scope was expanded")
        need(all(result.get(k) is True for k in ("hostFdsClosed", "rpcQuiescent", "originalSessionQuiescent", "binderDead", "matchingProcessDead", "guardReleased", "connectionCleaned", "abandonedReceipt", "ownedPayloadCleanup", "observationWindowClosed")) and all(result.get(k) is False for k in ("wireResultInvented", "workerDataFdCloseFlagsInvented", "decodeTerminalOrPreparedCreated")), "Failed-session resource proof incomplete")
        observed = {**{k: result[k] for k in ("generation", "asset", "attempt", "session", "nonce", "workerPid", "workerUid", "workerStartTicks")}, "hostPid": attempt["hostPid"], "hostUid": uid, "hostStartTicks": attempt["hostStartTicks"], "bootId": attempt["bootId"]}
        rows = [life.strict_json(evidence[name]) for name in sorted(children) if name.startswith("observation-")]
        observation_check = cancellation_observations(rows, observed)
        need(result.get("observationCount") == len(rows) and result.get("completeClosureClaims") == 1, "Cancelled callback count mismatched")
        terminal_stage(stage, life.SOURCES["SUCCESS"], observed, abandoned=True)
        checks = cancellation_phases(phases, observed)
    else:
        need(index == 6 and not new_obs and not new_diag and set(new_safety) == {run_id}, "Delayed return used another synthetic namespace")
        path = "no_backup/m10-b1b2-safety/" + run_id
        children = new_safety[run_id]["children"]
        need(set(children) == {"attempt.json", "result.json", "initial-observation.json", "late-closure.json", "in-flight-checkpoint.json"}, "Delayed-return callback error or incomplete proof")
        grouped = read_bundle(s, [path + "/" + name for name in children], uid)
        evidence = {name: grouped[path + "/" + name][0] for name in children}
        s._cancellation_raw = evidence
        attempt = life.strict_json(evidence["attempt.json"]); result = life.strict_json(evidence["result.json"])
        need(attempt.get("runId") == result.get("runId") == run_id and attempt.get("method") == result.get("method") == METHODS[6] and attempt.get("hostUid") == uid, "Delayed-return run identity mismatch")
        need(result.get("status") == "PASS" and result.get("operationOutcome") == "TIMEOUT" and result.get("closureStatus") == "SAME_ORIGINAL_SESSION_ABANDONED_AND_OWNED_PAYLOAD_CLEANED" and result.get("resourceEnded") is True and result.get("roomOrFormalCoverCommitted") is False, "Failed delayed-return Session is not independently closed")
        need(result.get("scope") == "REAL_DELIVER_RETURNED_TEST_JAVA_RETURN_DELAYED_ONLY" and result.get("kernelRpcPermanentlyInFlight") == "NOT_TESTED" and all(result.get(k) == "NOT_RUN" for k in ("nativeComputeCancellation", "workerFaultKill", "hostDeath")), "Delayed-return scope expanded")
        need(result.get("retirementKind") == "FAIL_CLOSED_DETACH_EXTERNAL_BRANCH_PROOF_REQUIRED" and result.get("realDeliveryCalls") == result.get("unbindCalls") == result.get("lateClosureCalls") == 1 and result.get("acceptedResultsAfterInitialFailure") == 0, "Delayed actual calls/accepted result count conflict")
        need(all(result.get(k) is True for k in ("hostFdsClosed", "rpcQuiescent", "originalSessionQuiescent", "binderDead", "matchingProcessDead", "guardReleased", "connectionCleaned", "abandonedReceipt", "ownedPayloadCleanup", "observationWindowClosed", "realKernelTransactReturnedBeforeTestHold", "activeGuardCleanupRejected", "unbindOnMain", "realUnbindSucceeded", "initialFailureWhileTicketHeld", "persistentRevokeConfirmed", "wrongSessionRejectedWithoutWrite", "duplicateRecoveryIdempotentWithoutWrite", "lateCompletionRejectedWithoutWrite")) and result.get("decodeTerminalOrPreparedCreated") is False, "Delayed-return failure/connection/cleanup proof incomplete")
        observed = {**{k: result[k] for k in ("generation", "asset", "attempt", "session", "nonce", "workerPid", "workerUid", "workerStartTicks")}, "hostPid": attempt["hostPid"], "hostUid": uid, "hostStartTicks": attempt["hostStartTicks"], "bootId": attempt["bootId"]}
        observation_check = delayed_return_observations(life.strict_json(evidence["initial-observation.json"]), life.strict_json(evidence["late-closure.json"]), life.strict_json(evidence["in-flight-checkpoint.json"]), observed)
        terminal_stage(stage, life.SOURCES["SUCCESS"], observed, abandoned=True, allow_failed_output=True)
        checks = {**delayed_return_phases(phases, observed), "observations": observation_check}
    need(observed["hostUid"] == observed["workerUid"] == uid and observed["hostPid"] != observed["workerPid"], "Host/Worker isolation conflict")
    process = {**after["processes"], "worker_proc": dev.worker_absence(s, observed["workerPid"], observed["workerStartTicks"])}
    life.validate_process(process, observed)
    return {"status": "PASS", "identity": observed, "retirement": checks, "independentProcess": process,
            "memoryCap": "NOT_PROVEN", "postRetiringTailPeak": "NOT_PROVEN"}


def persist_method_originals(s, directory):
    """Only already-read, bounded bytes from this method's new synthetic attempt.
    Run exactly once in finally, including failures; never replace an old original.
    """
    for attribute, dirname, names_allowed in (("_resource_raw", "resource-originals", DIAG_NAMES),
                                              ("_cancellation_raw", "cancellation-originals", SAFETY_NAMES)):
        if not hasattr(s, attribute): continue
        evidence = getattr(s, attribute)
        need(isinstance(evidence, dict) and set(evidence) <= names_allowed, "Unexpected current-method original file")
        originals = directory / dirname; originals.mkdir(exist_ok=False)
        for name, text in evidence.items():
            need(isinstance(text, str) and len(text.encode("utf-8")) <= 2 * 1024 * 1024, "Current original exceeded read bound")
            with (originals / name).open("xb") as stream: stream.write(text.encode("utf-8"))


def run_one(s, index):
    need(type(index) is int and 0 <= index < len(METHODS), "Only one exact index is allowed")
    for attribute in ("_resource_raw", "_cancellation_raw"):
        if hasattr(s, attribute): delattr(s, attribute)  # No preceding method's copy is eligible here.
    test_proof = s.require_proof(s.home / "install-test-proof.json")
    need(test_proof.get("kind") == "test" and test_proof.get("package") == TEST and re.fullmatch(r"[a-f0-9]{64}", test_proof.get("sha256", "")), "New Test proof is missing or malformed")
    test_sha = test_proof["sha256"]
    audit = s.apk_audit(); need(next(x for x in audit["artifacts"] if x["name"] == "test")["sha256"] == test_sha, "New Test audit/proof mismatch")
    root = s.evidence / "safety-methods"; fresh.avd_identity.unlinked_ancestors(root); root.mkdir(parents=True, exist_ok=True)
    prior_diagnostics, prior_events = [], []
    capture = fresh.capture_module()
    for previous in range(index):
        previous_row, old = admitted_method(s, previous, test_sha)
        proof, text = capture.read_verified(old / "continuous-log", expected_serial=s.serial)
        prior_diagnostics.append(proof["handshake"]["authenticatedRawLine"])
        prior_events.extend(line for line in text.splitlines(keepends=True) if " LITHIC_JPEG" in line and line not in prior_events)
    directory = root / f"method-{index:02d}"; directory.mkdir(exist_ok=False)
    run_id = str(uuid.uuid4()); plan = method_plan(index, run_id, s.api)
    result = {"status": "BLOCKED", "index": index, "method": plan["method"], "runId": run_id,
              "api": s.api, "serial": s.serial, "appSHA256": APP_SHA, "testSHA256": test_sha,
              "instrumentationCommandsIssued": 0, "deviceProof": {"status": "NOT_RUN"}}
    collector = None; finished = False; issued = False
    def finish():
        nonlocal finished
        need(not finished, "Capture finish cannot be retried"); finished = True
        return collector.finish()
    try:
        before = snapshot(s, test_sha)
        if index == 0:
            need(all(not before[key] for key in ("stages", "observations", "diagnostics", "safety")), "Fresh first-method synthetic namespaces are not empty")
        else:
            previous_row, previous_directory = admitted_method(s, index - 1, test_sha)
            previous_after = previous_row.get("continuationSnapshot")
            if previous_after is None:
                previous_after = life.strict_json(fresh.avd_identity.read_bytes(previous_directory / "after.json", limit=1024 * 1024).decode("utf-8"))
            need(all(before[key] == previous_after[key] for key in ("stages", "observations", "diagnostics", "safety")), "Synthetic evidence changed between authorized methods")
        for generation, stage in before["stages"].items(): terminal_stage(stage, abandoned="abandoned.v2" in stage["records"], allow_failed_output=generation.startswith("m10-b1-delay-"))
        components = s.adb("shell", "pm", "list", "instrumentation").splitlines()
        target = [line for line in components if fresh.INSTRUMENTATION in line or f"(target={APP})" in line]
        need(target == [f"instrumentation:{fresh.INSTRUMENTATION} (target={APP})"], "Instrumentation component conflict")
        fresh.immutable(directory / "before.json", before)
        operations = s.home / "safety-operations"; fresh.avd_identity.unlinked_ancestors(operations); operations.mkdir(exist_ok=True)
        fresh.immutable(operations / f"method-{index:02d}.json", {**s.binding(), "runId": run_id, "method": plan["method"], "testSHA256": test_sha, "status": "ATTEMPT_ISSUED"})
        fresh.immutable(directory / "attempt.json", {**result, "status": "ATTEMPT_ISSUED", "argv": plan["argv"]})
        nonce = uuid.uuid4().hex
        def writer(message):
            need(message == "M10_CAPTURE_READY nonce=" + nonce, "Capture nonce mismatch")
            fresh.immutable(operations / f"capture-{index:02d}.json", {"nonce": nonce, "serial": s.serial, "runId": run_id, "status": "ATTEMPT_ISSUED"})
            need(s.adb("shell", "log", "-p", "i", "-t", "LITHIC_M10_CAPTURE", message, timeout=10) == "", "Diagnostic command output unknown")
            return {"status": "PASS", "serial": s.serial, "exitCode": 0, "tag": "LITHIC_M10_CAPTURE", "nonce": nonce}
        collector = s.new_log_capture(directory, nonce=nonce, handshake_writer=writer, prior_diagnostics=tuple(prior_diagnostics))
        need(collector.start().get("status") == "PASS" and collector.assert_live().get("status") == "PASS", "Continuous capture not READY/live")
        issued = True; result["instrumentationCommandsIssued"] = 1
        output = s.adb(*plan["argv"], timeout=240)
        with (directory / "instrumentation.txt").open("xb") as stream: stream.write(output.encode())
        result["instrumentation"] = parse_instrumentation(output, plan["method"])
        proof = finish(); result["capture"] = proof
        verified, raw = capture.read_verified(directory / "continuous-log", expected_serial=s.serial)
        need(verified == proof and proof.get("status") == "PASS", "Continuous raw proof conflict")
        phases = scoped_lines(raw, proof["handshake"]["authenticatedRawLine"], tuple(prior_diagnostics), tuple(prior_events))
        after = snapshot(s, test_sha); fresh.immutable(directory / "after.json", after)
        need(result["instrumentation"].get("status") == "PASS", "Actual single-method JUnit not PASS")
        result["deviceProof"] = evaluate(s, index, run_id, before, after, phases)
        need(result["deviceProof"]["status"] == "PASS", "Independent device/resource gate not PASS")
        result["status"] = "PASS"
    except Exception as error:
        result["status"] = "UNKNOWN" if issued else "BLOCKED"
        result["error"] = {"type": type(error).__name__, "detail": str(error)}
    finally:
        if collector is not None and not finished:
            try: result["capture"] = finish()
            except Exception as error: result["capture"] = {"status": "UNKNOWN", "type": type(error).__name__}
        if issued and not (directory / "after.json").exists():
            try: fresh.immutable(directory / "after.json", snapshot(s, test_sha))
            except Exception as error: result["endingReadonly"] = {"status": "UNKNOWN", "type": type(error).__name__, "detail": str(error)}
        try: persist_method_originals(s, directory)
        except Exception as error:
            result["status"] = "UNKNOWN" if issued else "BLOCKED"
            result["originalsError"] = {"type": type(error).__name__, "detail": str(error)}
        fresh.immutable(directory / "result.json", result)
    need(result["status"] == "PASS", "Safety method " + result["status"] + "; no retry or next method")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("api", type=int, choices=(26, 36)); parser.add_argument("index", type=int, choices=range(len(METHODS)))
    args = parser.parse_args(); print(json.dumps(run_one(fresh.FreshSupervisor(args.api), args.index), sort_keys=True))


if __name__ == "__main__": main()
