"""Root-only exact process-fault evidence; one explicit index, no traversal.

0 Worker fault/live original recovery; 1 Host pin fault; 2 its recovery;
3 Host retired fault; 4 its recovery. Host interruptions are never JUnit PASS.
No ADB kill, force-stop, restart, installation, deletion or retry is offered.
"""
from __future__ import annotations
import argparse
import importlib.util
import json
from pathlib import Path
import re
import shlex
import sys
import time
import uuid

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import fresh_environment as fresh
import lifecycle_evidence as life
import device_evidence as dev
from natural_tests import parse_instrumentation

PREFIX = "com.cooper.lithic.data.media.M10B1ExactProcessFaultTest#"
METHODS = (PREFIX + "exact_worker_death_after_pin_before_data_fd",
           PREFIX + "exact_host_death_after_pin_before_data_fd",
           PREFIX + "recover_only_exact_ended_host_fault_without_new_consumer",
           PREFIX + "exact_host_death_after_real_retirement_before_receipt",
           PREFIX + "recover_only_exact_ended_host_fault_without_new_consumer")
KINDS = ("WORKER_BEFORE_DATA_FD", "HOST_BEFORE_DATA_FD", "HOST_BEFORE_DATA_FD",
         "HOST_AFTER_RETIRE_BEFORE_RECEIPT", "HOST_AFTER_RETIRE_BEFORE_RECEIPT")
UUID = r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
FAULT_NAMES = {"fault-checkpoint.json", "fault-observation.json", "fault-late-observation.json", "recovery-attempt.json"}
need = fresh.need
_EVIDENCE = None


def evidence_module():
    global _EVIDENCE
    if _EVIDENCE is None:
        spec = importlib.util.spec_from_file_location("b1_exact_fault_private_safety", HERE / "safety_device.py")
        module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
        module.ROOT_RE = re.compile(r"m10-(?:worker-review|resource-v2|b1-cancel|b1-delay|b1-fault)-" + UUID)
        module.SAFETY_NAMES = module.SAFETY_NAMES | FAULT_NAMES
        _EVIDENCE = module
    return _EVIDENCE


def boot_epoch(text):
    need(isinstance(text, str) and re.fullmatch(r"0|[1-9][0-9]*", text) and int(text) <= 2147483647,
         "BOOT_COUNT unavailable or noncanonical")
    return "6c697468-6963-4000-8000-" + format(int(text), "012x")


def plan(index, run_id, api, uid, boot, test_sha, target_proof, original=None, end_sha=None):
    need(type(index) is int and 0 <= index < 5 and api in (26, 36), "Unknown fault operation")
    need(type(uid) is int and uid > 0 and all(re.fullmatch(UUID, value or "") for value in (run_id, target_proof)), "Fault UID/UUID invalid")
    need(re.fullmatch(r"6c697468-6963-4000-8000-[0-9a-f]{12}", boot or "") and
         re.fullmatch(r"[0-9a-f]{64}", test_sha or ""), "Fault boot/APK identity invalid")
    values = {"class": METHODS[index], "m10SafetyPermit": "M10_B1B2_ISOLATED_V1", "m10SafetyRunId": run_id,
              "m10SafetyExpectedApi": str(api), "m10FaultPermit": "M10_B1B2_EXACT_FAULT_V1",
              "m10FaultKind": KINDS[index], "m10FaultExpectedUid": str(uid), "m10FaultExpectedBoot": boot,
              "m10FaultExpectedAppSHA256": fresh.APP_SHA, "m10FaultExpectedTestSHA256": test_sha,
              "m10FaultTargetProof": target_proof}
    if index in (2, 4):
        need(re.fullmatch(UUID, original or "") and original != run_id and re.fullmatch(r"[a-f0-9]{64}", end_sha or ""),
             "Recovery requires its original run and independent exact-death proof")
        values.update(m10FaultOriginalRunId=original, m10FaultExternalEndProofSHA256=end_sha)
    else: need(original is None and end_sha is None, "Injection cannot claim a recovery proof")
    args = ["shell", "am", "instrument", "-w", "-r"]
    for key, value in values.items(): args += ["-e", key, value]
    return {"index": index, "kind": KINDS[index], "method": METHODS[index], "runId": run_id,
            "arguments": values, "argv": args + [fresh.INSTRUMENTATION]}


def expected_host_interruption(text, method):
    need(isinstance(text, str) and len(text.encode()) <= 2 * 1024 * 1024, "Missing/unbounded fault instrumentation")
    cls, name = method.split("#")
    need(set(re.findall(r"^INSTRUMENTATION_STATUS: class=(.+)$", text, re.M)) == {cls} and
         set(re.findall(r"^INSTRUMENTATION_STATUS: test=(.+)$", text, re.M)) == {name}, "Wrong interrupted method")
    need(re.findall(r"^INSTRUMENTATION_STATUS_CODE:\s*(-?\d+)\s*$", text, re.M) == ["1"] and
         re.findall(r"^INSTRUMENTATION_CODE:\s*(-?\d+)\s*$", text, re.M) == ["0"], "Interruption is not one started non-completed method")
    need(len(re.findall(r"^INSTRUMENTATION_RESULT: shortMsg=Process crashed\.?\s*$", text, re.M)) == 1 and
         not re.search(r"OK \(|FAILURES!!!|INSTRUMENTATION_STATUS: (?:stack|Error)=|TIMEOUT", text),
         "No explicit expected process interruption; never substitute JUnit success")
    return {"status": "EXPECTED_INSTRUMENTATION_INTERRUPTION_NOT_PASS"}


def checkpoint_identity(value, index, run_id, uid, boot, target_proof, api):
    need(value.get("protocol") == "M10_B1_EXACT_FAULT_CHECKPOINT_V1" and
         value.get("status") == "ARMED_BEFORE_EXACT_CALL_NOT_DEATH_PROOF" and value.get("runId") == run_id and
         value.get("kind") == KINDS[index] and value.get("method") == METHODS[index] and
         value.get("targetProof") == target_proof and value.get("bootId") == boot and
         type(value.get("api")) is int and value["api"] == api, "Checkpoint scope conflicts")
    for key in ("host", "worker"):
        row = value.get(key, {})
        need(set(row) == {"pid", "uid", "startTicks"} and row.get("uid") == uid and
             all(type(row.get(field)) is int and row[field] > 0 for field in ("pid", "startTicks")), "Checkpoint process identity incomplete")
    need(value["host"]["pid"] != value["worker"]["pid"] and value.get("pinDurable") is True and
         value.get("decodeReceiptCreated") is False and value.get("sourceAndOutputSubmittedToRoom") is False and
         value.get("killReturnNeverProvesDeath") is True and value.get("dataFdDelivered") is (index == 3),
         "Checkpoint does not describe this exact boundary")
    need(value.get("source", {}).get("bytes") == 59334 and value["source"].get("sha256") == life.SOURCES["SUCCESS"][1], "Checkpoint source differs")
    owner = value.get("owner", {})
    need(set(owner) == {"generation", "asset", "attempt", "session", "nonce"} and
         owner["generation"] == "m10-b1-fault-" + run_id and
         all(re.fullmatch(UUID, owner[key]) for key in ("asset", "attempt", "session", "nonce")), "Checkpoint Stage identity invalid")
    output = value.get("output", {})
    need(set(output) == {"device", "inode", "bytes", "sha256"} and type(output["bytes"]) is int and
         all(type(output[k]) is int and output[k] > 0 for k in ("device", "inode")) and
         re.fullmatch(r"[a-f0-9]{64}", output["sha256"]), "Checkpoint output identity incomplete")
    if index == 3:
        retired = value.get("realRetiredResult", {})
        need(retired.get("outcome") == "SUCCESS" and retired.get("resultCount") == 1 and
             all(retired.get(k) is True for k in ("sourceFdClosed", "outputFdClosed", "strictFinished", "decoderDestroyed",
                 "connectionCleaned", "binderDead", "matchingProcessDead", "guardReleased")) and
             0 < output["bytes"] <= 2097152, "Host post-retirement fault lacks real completed Client evidence")
    else: need(output["bytes"] == 0 and output["sha256"] == life.digest(""), "Pre-FD output is no longer empty")
    return value


def stage_proof(stage, checkpoint, *, closed, host_ended):
    need(not closed or not stage["remainingPayloadMetadata"], "Closed fault Stage still has payload")
    records = stage["records"]
    required = {"owner.v2", "source.v2", "output.v2", "pin.v2", "delivered.v2", "supervisor.v2", "consumer.guard", "__metadata__"}
    if closed: required |= {"revoked.v2", "abandoned.v2", "abandoned-cleanup-intent.v2", "abandoned-cleaned.v2"}
    else: required |= ({"revoked.v2"} if "revoked.v2" in records else set())
    need(set(records) == required, "Unknown/missing fault Stage child or fabricated successful receipt")
    inventory = life.strict_json(records["__metadata__"])
    entries = inventory.get("entries", {})
    need(inventory.get("complete") is True and set(entries) == required - {"__metadata__"}, "Fault record inventory incomplete")
    for name, row in entries.items():
        need(row.get("type") == "regular" and row.get("bytes") == len(records[name].encode("ascii")) and
             row.get("sha256") == life.digest(records[name]), "Fault record bytes/stat/SHA conflict")
    owner = life.canonical_record(records["owner.v2"], life.OWNER_KEYS); oh = life.digest(records["owner.v2"])
    cp = checkpoint["owner"]
    need(owner["protocol"] == "lithic-m10-jpeg-stage" and owner["version"] == "2" and
         owner["decoderPolicy"] == "libjpeg-turbo-3.2.0-e01-strict-v1" and owner["normalizationPolicy"] == "m10-jpeg-normalize-v1" and
         [owner[k] for k in ("generation", "assetId", "attemptId", "sessionId", "nonce")] ==
         [cp[k] for k in ("generation", "asset", "attempt", "session", "nonce")], "Fault owner identity changed")
    guard = entries["consumer.guard"]
    need(records["consumer.guard"] == "" and int(owner["guardDevice"]) == guard["device"] == checkpoint["guard"]["device"] and
         int(owner["guardInode"]) == guard["inode"] == checkpoint["guard"]["inode"], "Fault guard inode changed")
    source = life.canonical_record(records["source.v2"], life.FILE_KEYS)
    output = life.canonical_record(records["output.v2"], life.FILE_KEYS)
    need(source["ownerHash"] == output["ownerHash"] == oh and output["bytes"] == "0" and output["sha256"] == life.digest(""), "Source/output creation record conflict")
    for name, value in (("device", "device"), ("inode", "inode"), ("bytes", "bytes")):
        need(int(source[name]) == checkpoint["source"][value], "Original source identity changed")
    need(source["sha256"] == checkpoint["source"]["sha256"], "Original source bytes changed")
    pin = life.canonical_record(records["pin.v2"], life.PIN_KEYS)
    need(pin["ownerHash"] == oh and pin["sourceHash"] == source["sha256"] and
         tuple(int(pin[k]) for k in ("pid", "uid", "startTicks")) == tuple(checkpoint["worker"][k] for k in ("pid", "uid", "startTicks")), "Fault consumer pin changed")
    need(life.canonical_record(records["delivered.v2"], {"ownerHash", "pinHash"}) ==
         {"ownerHash": oh, "pinHash": life.digest(records["pin.v2"])}, "Fault pin/delivery mismatch")
    supervisor = life.canonical_record(records["supervisor.v2"], {"ownerHash", "bootId", "pid", "uid", "startTicks"})
    need(supervisor["ownerHash"] == oh and supervisor["bootId"] == checkpoint["bootId"] and
         tuple(int(supervisor[k]) for k in ("pid", "uid", "startTicks")) == tuple(checkpoint["host"][k] for k in ("pid", "uid", "startTicks")), "Fault Host supervisor changed")
    if "revoked.v2" in records:
        revoke = life.canonical_record(records["revoked.v2"], {"ownerHash", "reasonHash"})
        need(revoke["ownerHash"] == oh and re.fullmatch(r"[a-f0-9]{64}", revoke["reasonHash"]), "Fault revocation malformed")
    if closed:
        keys = {"version", "ownerHash", "supervisorHash", "pinHash", "sourceRecordHash", "sourceHash", "outputDevice", "outputInode", "outputBytes", "outputSha", "observedBootId", "hostClosure", "workerEnded", "hostFdsClosed", "noFutureRpc", "connectionCleaned", "noConsumerActorsEnded", "proof"}
        receipt = life.canonical_record(records["abandoned.v2"], keys)
        need(receipt["version"] == "1" and receipt["ownerHash"] == oh and receipt["supervisorHash"] == life.digest(records["supervisor.v2"]) and
             receipt["pinHash"] == life.digest(records["pin.v2"]) and receipt["sourceRecordHash"] == life.digest(records["source.v2"]) and receipt["sourceHash"] == source["sha256"], "Fault abandoned receipt ownership invalid")
        need(receipt["hostClosure"] == ("ORIGINAL_INSTANCE_ENDED" if host_ended else "SAME_SESSION_QUIESCENT") and
             receipt["observedBootId"] == checkpoint["bootId"] and receipt["proof"] == "NO_RESULT_CONSUMER" and receipt["noConsumerActorsEnded"] == "false" and
             all(receipt[k] == "true" for k in ("workerEnded", "hostFdsClosed", "noFutureRpc", "connectionCleaned")), "Fault actor closure incomplete")
        need((int(receipt["outputDevice"]), int(receipt["outputInode"]), int(receipt["outputBytes"]), receipt["outputSha"]) ==
             tuple(checkpoint["output"][k] for k in ("device", "inode", "bytes", "sha256")), "Fault output identity changed")
        expected = {"ownerHash": oh, "abandonedHash": life.digest(records["abandoned.v2"])}
        for name in ("abandoned-cleanup-intent.v2", "abandoned-cleaned.v2"):
            need(life.canonical_record(records[name], set(expected)) == expected, "Independent fault cleanup proof invalid")
    else:
        need(set(stage["remainingPayloadMetadata"]) == {"source.bin", "image.jpg"}, "Unrecovered payload missing or unknown")
        for name, expected in (("source.bin", checkpoint["source"]), ("image.jpg", checkpoint["output"])):
            actual = stage["remainingPayloadMetadata"][name]
            need(actual["type"] == "regular" and all(actual[k] == expected[k] for k in ("device", "inode", "bytes")), "Pending owned payload identity changed")
    return {"status": "PASS", "closed": closed, "abandonedNotDecodeSuccess": closed,
            "guardProof": "ACTUAL_APP_RECOVERY_FILELOCK_AND_DURABLE_RECEIPT" if closed else "NOT_A_CLEANUP_AUTHORITY"}


def fault_events(text, checkpoint, *, retired):
    observed = {"attempt": checkpoint["owner"]["attempt"], "session": checkpoint["owner"]["session"],
                "hostPid": checkpoint["host"]["pid"], "workerPid": checkpoint["worker"]["pid"],
                "workerUid": checkpoint["worker"]["uid"], "workerStartTicks": checkpoint["worker"]["startTicks"]}
    if retired:
        life.validate_phases(text, observed)
        return {"status": "PASS", "kind": "REAL_NORMAL_RETIREMENT_BEFORE_HOST_FAULT"}
    events = {}
    allowed = {"HOST_BINDER_DIED", "HOST_UNBIND_RETURNED"} if checkpoint["kind"] == KINDS[0] else {
        "WORKER_ON_UNBIND", "UNPREPARED_DETACH", "WORKER_EXIT_UNPREPARED_DETACH", "WORKER_EXIT_HOST_BINDER_DIED"}
    for line in text.splitlines():
        match = re.fullmatch(r"\d\d-\d\d\s+\d\d:\d\d:\d\d\.\d{3,6}\s+(\d+)\s+(\d+)\s+I\s+LITHIC_JPEG\s*:\s*([A-Z_]+) attempt=(\S+) session=(\S+) worker=(\d+)/(\d+)/(\d+) at=(\d+)\s*", line)
        need(match is not None, "Malformed or unknown fault lifecycle event")
        emitter, _, phase, attempt, session, pid, uid, start, at = match.groups()
        need(phase in allowed and phase not in events and (attempt, session) == (observed["attempt"], observed["session"]) and
             tuple(map(int, (pid, uid, start))) == tuple(observed[k] for k in ("workerPid", "workerUid", "workerStartTicks")) and
             int(emitter) == observed["hostPid" if phase.startswith("HOST_") else "workerPid"], "Duplicate/foreign fault lifecycle identity")
        events[phase] = int(at)
    if checkpoint["kind"] == KINDS[0]: need(set(events) == allowed, "Worker fault needs actual Host death/unbind observations")
    else:
        need(sum(k.startswith("WORKER_EXIT_") for k in events) == 1, "Host fault lacks one matched Worker exit observation")
        if "UNPREPARED_DETACH" in events:
            need("WORKER_ON_UNBIND" in events and events["WORKER_ON_UNBIND"] <= events["UNPREPARED_DETACH"], "Unprepared fault detach order unknown")
        if "WORKER_EXIT_UNPREPARED_DETACH" in events:
            need("UNPREPARED_DETACH" in events and events["UNPREPARED_DETACH"] <= events["WORKER_EXIT_UNPREPARED_DETACH"], "Unprepared Worker exit not evidenced")
    return {"status": "PASS", "events": events, "normalRetirement": "NOT_CLAIMED_FOR_PRE_FD_FAULT"}


def ended(s, checkpoint, *, host_required, recovery_host=None):
    rounds = []
    for _ in range(2):
        need(boot_epoch(s.adb("shell", "settings", "get", "global", "boot_count")) == checkpoint["bootId"], "Boot changed during exact process-end proof")
        processes = dev.processes(s, checkpoint["host"]["uid"])
        worker = checkpoint["worker"]
        absent_worker = dev.worker_absence(s, worker["pid"], worker["startTicks"])
        if host_required:
            need(not processes["rows"] or (recovery_host is not None and all(
                 (r["pid"], r["uid"], r["startTicks"]) == tuple(recovery_host[k] for k in ("pid", "uid", "startTicks"))
                 for r in processes["rows"])), "An active/new Host must not be instrumented to manufacture old Host death")
            host = checkpoint["host"]
            absent_host = dev.worker_absence(s, host["pid"], host["startTicks"])
        else:
            need(all((r["pid"], r["uid"], r["startTicks"]) == tuple(checkpoint["host"][k] for k in ("pid", "uid", "startTicks")) for r in processes["rows"]), "Unexpected main process after Worker fault")
            absent_host = {"status": "NOT_REQUIRED_OR_INFERRED", "sameLiveOriginalSessionProof": True}
        rounds.append({"processes": processes, "worker": absent_worker, "host": absent_host})
    return {"status": "PASS", "bootId": checkpoint["bootId"], "hostRequired": host_required, "rounds": rounds,
            "persistentPidAloneUsed": False, "rootKillsOrRestartsIssued": 0}


def run_one(s, index):
    need(type(index) is int and index in range(5), "Only one exact fault index is permitted")
    m = evidence_module()
    test = s.require_proof(s.home / "install-test-proof.json")
    test_sha = test.get("sha256", "")
    need(test.get("kind") == "test" and test.get("package") == fresh.TEST and re.fullmatch(r"[a-f0-9]{64}", test_sha), "Test installation proof missing")
    need(next(x for x in s.apk_audit()["artifacts"] if x["name"] == "test")["sha256"] == test_sha, "Locked Test audit differs")
    # These are prerequisite proofs, not repeated normal tests.
    need(len(m.METHODS) == 7, "Final seven-method normal driver contract is not frozen")
    prior_diagnostics, prior_events = [], []
    capture = fresh.capture_module()
    for normal in range(7):
        prerequisite, old = m.admitted_method(s, normal, test_sha)
        proof, text = capture.read_verified(old / "continuous-log", expected_serial=s.serial)
        prior_diagnostics.append(proof["handshake"]["authenticatedRawLine"])
        prior_events.extend(x for x in text.splitlines(keepends=True) if " LITHIC_JPEG" in x and x not in prior_events)
    root = s.evidence / "exact-fault-methods"; fresh.avd_identity.unlinked_ancestors(root); root.mkdir(exist_ok=True)
    previous = []
    for number in range(index):
        old = root / f"operation-{number:02d}"
        item = life.strict_json(fresh.avd_identity.read_bytes(old / "result.json", limit=4*1024*1024).decode())
        expected = "EXPECTED_INTERRUPTION_AWAITING_RECOVERY" if number in (1, 3) else "PASS"
        need(item.get("status") == expected and item.get("index") == number and item.get("testSHA256") == test_sha and
             item.get("serial") == s.serial and item.get("appSHA256") == fresh.APP_SHA, "Prior fault operation is unresolved/foreign")
        previous.append(item)
        proof, text = capture.read_verified(old / "continuous-log", expected_serial=s.serial)
        prior_diagnostics.append(proof["handshake"]["authenticatedRawLine"])
        prior_events.extend(x for x in text.splitlines(keepends=True) if " LITHIC_JPEG" in x and x not in prior_events)
    directory = root / f"operation-{index:02d}"; directory.mkdir(exist_ok=False)
    result = {**s.binding(), "index": index, "testSHA256": test_sha, "status": "BLOCKED", "instrumentationCommandsIssued": 0}
    collector = None; finished = False; issued = False
    def finish():
        nonlocal finished
        need(not finished, "Fault capture cannot be finished twice"); finished = True
        return collector.finish()
    try:
        before = m.snapshot(s, test_sha)
        boot = boot_epoch(s.adb("shell", "settings", "get", "global", "boot_count"))
        original = previous[index-1] if index in (2, 4) else None
        checkpoint = original["checkpoint"] if original else None
        end_proof = None
        if checkpoint:
            end_proof = ended(s, checkpoint, host_required=True)
            fresh.immutable(directory / "external-end-proof.json", end_proof)
        for generation, stage in before["stages"].items():
            if generation.startswith("m10-b1-fault-"):
                known = next((x["checkpoint"] for x in previous if x.get("checkpoint", {}).get("owner", {}).get("generation") == generation), None)
                need(known is not None, "Unknown fault Stage; preserve")
                pending = checkpoint is not None and checkpoint["owner"]["generation"] == generation
                stage_proof(stage, known, closed=not pending, host_ended=known["kind"] != KINDS[0])
            else: m.terminal_stage(stage, abandoned="abandoned.v2" in stage["records"], allow_failed_output=generation.startswith("m10-b1-delay-"))
        run_id = str(uuid.uuid4()); target = original["targetProof"] if original else str(uuid.uuid4())
        target_proof = {**s.binding(), "testSHA256": test_sha, "targetProof": target, "bootId": boot,
                        "appUID": before["uid"], "testUID": before["testUID"], "utc": fresh.utc(),
                        "actualHostIdentityComesFromAuthenticatedCheckpoint": True}
        fresh.immutable(directory / "target-proof.json", target_proof)
        selected = plan(index, run_id, s.api, before["uid"], boot, test_sha, target,
                        original["runId"] if original else None,
                        fresh.digest(fresh.avd_identity.read_bytes(directory / "external-end-proof.json")) if original else None)
        result.update(runId=run_id, targetProof=target, method=selected["method"], kind=selected["kind"])
        fresh.immutable(directory / "before.json", before)
        operations = s.home / "exact-fault-operations"; fresh.avd_identity.unlinked_ancestors(operations); operations.mkdir(exist_ok=True)
        fresh.immutable(operations / f"operation-{index:02d}.json", {**result, "status": "ATTEMPT_ISSUED", "argv": selected["argv"]})
        fresh.immutable(directory / "attempt.json", {**result, "status": "ATTEMPT_ISSUED", "argv": selected["argv"]})
        nonce = uuid.uuid4().hex
        def writer(message):
            need(message == "M10_CAPTURE_READY nonce=" + nonce, "Fault capture nonce mismatch")
            fresh.immutable(operations / f"capture-{index:02d}.json", {"nonce": nonce, "serial": s.serial, "runId": run_id})
            need(s.adb("shell", "log", "-p", "i", "-t", "LITHIC_M10_CAPTURE", message, timeout=10) == "", "Diagnostic returned unknown output")
            return {"status": "PASS", "serial": s.serial, "exitCode": 0, "tag": "LITHIC_M10_CAPTURE", "nonce": nonce}
        collector = s.new_log_capture(directory, nonce=nonce, handshake_writer=writer, prior_diagnostics=tuple(prior_diagnostics))
        need(collector.start().get("status") == "PASS" and collector.assert_live().get("status") == "PASS", "Fault log observer not READY/live")
        issued = True; result["instrumentationCommandsIssued"] = 1
        output = s.adb(*selected["argv"], timeout=180, allowed=(0, 1))
        with (directory / "instrumentation.txt").open("xb") as stream: stream.write(output.encode())
        result["capture"] = finish()
        verified, raw = capture.read_verified(directory / "continuous-log", expected_serial=s.serial)
        need(verified == result["capture"], "Fault capture raw proof mismatch")
        phases = m.scoped_lines(raw, verified["handshake"]["authenticatedRawLine"], tuple(prior_diagnostics), tuple(prior_events))
        if index in (1, 3): result["instrumentation"] = expected_host_interruption(output, selected["method"])
        else:
            result["instrumentation"] = parse_instrumentation(output, selected["method"])
            need(result["instrumentation"].get("status") == "PASS", "Actual fault/recovery assertions did not pass")
        if checkpoint is None:
            path = "no_backup/m10-b1b2-safety/" + run_id + "/fault-checkpoint.json"
            checkpoint = m.json_private(s, path, before["uid"])["value"]
            checkpoint_identity(checkpoint, index, run_id, before["uid"], boot, target, s.api)
        result["checkpoint"] = checkpoint
        recovery_host = None
        if index in (2, 4):
            attempt = m.json_private(s, "no_backup/m10-b1b2-safety/" + run_id + "/attempt.json", before["uid"])["value"]
            need(attempt.get("protocol") == "M10_B1B2_SAFETY_ATTEMPT_V1" and attempt.get("runId") == run_id and
                 attempt.get("method") == selected["method"] and attempt.get("bootId") == boot and
                 attempt.get("hostUid") == before["uid"], "Current recovery Host evidence conflicts")
            recovery_host = {"pid": attempt["hostPid"], "uid": attempt["hostUid"], "startTicks": attempt["hostStartTicks"]}
        result["processEndProof"] = ended(s, checkpoint, host_required=index != 0, recovery_host=recovery_host)
        after = m.snapshot(s, test_sha); fresh.immutable(directory / "after.json", after)
        generation = checkpoint["owner"]["generation"]
        for key in ("stages", "observations", "diagnostics", "safety"):
            for name, old in before[key].items():
                if key == "stages" and index in (2, 4) and name == generation:
                    continue  # Exact pending stage is independently checked as abandoned below.
                if key == "safety" and index in (2, 4) and name == original["runId"]:
                    current = after[key].get(name, {})
                    need(set(current.get("children", [])) == set(old["children"]) | {"recovery-attempt.json"} and
                         all(current.get("files", {}).get(n) == stat for n, stat in old["files"].items()),
                         "Original Host fault evidence changed beyond its exclusive recovery claim")
                else: need(after[key].get(name) == old, "Prior synthetic evidence changed: " + key + "/" + name)
        need(generation in after["stages"], "Fault Stage vanished without proof")
        result["stageProof"] = stage_proof(after["stages"][generation], checkpoint, closed=index not in (1, 3), host_ended=index != 0)
        if index in (2, 4):
            need(not phases, "Recovery must not start a Worker or issue lifecycle commands")
            report = m.json_private(s, "no_backup/m10-b1b2-safety/" + run_id + "/result.json", before["uid"])["value"]
            need(report.get("status") == "PASS" and report.get("originalRunId") == original["runId"] and
                 report.get("abandonedNotDecodeTerminal") is True and report.get("newWorkerOrRoomRecordCreated") is False,
                 "Host recovery did not prove independent non-success closure")
            result["status"] = "PASS"
        else:
            result["lifecycle"] = fault_events(phases, checkpoint, retired=index == 3)
            if index == 0:
                report = m.json_private(s, "no_backup/m10-b1b2-safety/" + run_id + "/result.json", before["uid"])["value"]
                facts = ("hostFdsClosed", "rpcQuiescent", "originalSessionQuiescent", "connectionCleaned", "binderDead", "matchingProcessDead", "guardReleased")
                need(report.get("status") == "PASS" and report.get("outcome") != "SUCCESS" and report.get("resultCount") == 0 and
                     all(report.get(k) is True for k in facts) and report.get("sourceFdClosed") is False and report.get("outputFdClosed") is False and
                     report.get("closureUsesOnlyLiveOriginalResult") is True, "Worker fault lacks its actual original Session closure")
                result["status"] = "PASS"
            else: result["status"] = "EXPECTED_INTERRUPTION_AWAITING_RECOVERY"
    except Exception as error:
        result["status"] = "UNKNOWN" if issued else "BLOCKED"
        result["error"] = {"type": type(error).__name__, "detail": str(error)}
    finally:
        if collector is not None and not finished:
            try: result["capture"] = finish()
            except Exception as error: result["capture"] = {"status": "UNKNOWN", "type": type(error).__name__}
        fresh.immutable(directory / "result.json", result)
    need(result["status"] in ("PASS", "EXPECTED_INTERRUPTION_AWAITING_RECOVERY"), "Fault evidence unresolved; preserve scene, do not retry")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("api", type=int, choices=(26, 36)); parser.add_argument("index", type=int, choices=range(5))
    args = parser.parse_args(); print(json.dumps(run_one(fresh.FreshSupervisor(args.api), args.index), sort_keys=True))


if __name__ == "__main__": main()
