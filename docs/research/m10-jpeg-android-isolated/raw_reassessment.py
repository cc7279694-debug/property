"""One precise offline presentation repair; never retries Android or edits evidence.

Only API26's first healthy capture with the known universal-newline hash error
is eligible. Other UNKNOWN/FAIL outcomes remain blocking. A published supplement
is fully recomputed from original bytes whenever used as a prerequisite.
"""
import hashlib
import json
from pathlib import Path

from natural_tests import METHODS, APP_SHA, TEST_SHA, parse_instrumentation, immutable


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def assessment(directory):
    import device_evidence
    directory = Path(directory)
    require(directory.name == "method-00", "Only the first healthy captured method is eligible")
    raw = {name:(directory/name).read_bytes() for name in ("result.json", "before.json", "after.json", "instrumentation.txt")}
    require(all(0 < len(value) <= 1048576 for value in raw.values()), "Capture incomplete/oversized")
    old, before, after = (json.loads(raw[name].decode("utf-8", errors="strict")) for name in ("result.json", "before.json", "after.json"))
    expected = {"method":METHODS[0], "api":26, "serial":"emulator-5624", "index":0,
                "appSHA256":APP_SHA, "testSHA256":TEST_SHA, "instrumentationCommandsIssued":1}
    require(all(old.get(key) == value for key,value in expected.items()), "Captured method/identity mismatch")
    require(old.get("status") == "UNKNOWN" and old.get("instrumentation", {}).get("status") == "PASS" and
            old.get("lifecycle", {}).get("errors") == ["Raw instrumentation evidence does not match parsed result"],
            "Only the exact known byte-reading defect may be reassessed")
    text = raw["instrumentation.txt"].decode("utf-8", errors="strict")
    require(b"\r\n" in raw["instrumentation.txt"] and
            after.get("instrumentationText") == text.replace("\r\n", "\n").replace("\r", "\n"),
            "Original captured view is not precisely universal-newline conversion")
    parsed = parse_instrumentation(text, METHODS[0])
    require(parsed == old["instrumentation"] and parsed.get("status") == "PASS", "Original instrumentation bytes do not match parsed result")
    corrected = {**after, "instrumentationText":text}
    lifecycle = device_evidence.assess(METHODS[0], before, corrected, parsed)
    require(lifecycle.get("status") == "PASS", "Resource/lifecycle evidence remains unresolved: " + str(lifecycle))
    return {**expected, "status":"PASS", "scope":"OFFLINE_RAW_BYTE_REASSESSMENT_NO_DEVICE_REEXECUTION",
            "originalStatus":"UNKNOWN", "supersedesOriginal":False,
            "sourceEvidence":{name:{"bytes":len(value), "sha256":hashlib.sha256(value).hexdigest()} for name,value in raw.items()},
            "instrumentation":parsed, "lifecycle":lifecycle}


def effective_result(directory):
    directory = Path(directory)
    published = json.loads((directory/"raw-byte-reassessment.json").read_bytes().decode("utf-8", errors="strict"))
    require(published == assessment(directory), "Supplement differs from exact recomputation")
    original = json.loads((directory/"result.json").read_bytes().decode("utf-8", errors="strict"))
    return {**original, "status":"PASS", "originalStatus":"UNKNOWN",
            "gateEvidence":"EXPLICIT_OFFLINE_RAW_BYTE_REASSESSMENT", "lifecycle":published["lifecycle"]}


def publish(supervisor):
    require(supervisor.api == 26 and supervisor.serial == "emulator-5624", "Only the authorized existing API26 is eligible")
    directory = Path(supervisor.evidence) / "method-00"
    proof = assessment(directory)
    immutable(directory/"raw-byte-reassessment.json", proof)
    supervisor.note({"operation":"OFFLINE_RAW_BYTE_REASSESSMENT", "method":METHODS[0],
                     "androidCommandsIssued":0, "originalResultPreserved":True, "status":"PASS"})
    return proof
