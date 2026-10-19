"""Offline host precondition classifier; no Android/SDK execution interface.

The root executor captured host-initial.json before the memory gate blocked.
This small file cannot create/start an AVD, use ADB, install an APK, or resume a
device. A favourable host observation is never Android execution admission.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import stat

BASE = "E:/CODEX/.codex/research-env/lithic-m10-b1b2-20261019-v1"
SOURCE = "08ce4b663b52127f400fa18bd13eb12fe04a8797"
REQUIRED_FREE_KIB = (1536 + 1024) * 1024
FIXTURE_SHA256 = "56e3ef0af7710ebe36aed92413ba79490154a0cf016ef1668e6288c37a7d91ee"
FIXTURE = Path(__file__).with_name("evidence") / "host-initial.json"


def classify_host(snapshot):
    """Classify only the supplied host observation, never live device readiness.

    The original 1536MiB guest plus 1024MiB host headroom is unchanged. Passing
    this one precondition would still leave current process, tool, disk, Android
    identity and consumer gates unverified, so no PASS/admission is produced.
    """
    observed = snapshot if isinstance(snapshot, dict) else {}
    reasons = []
    if (observed.get("scope") != "HOST_ONLY_NEW_TARGETS_NO_ADB_NO_OLD_AVD_READ" or
            observed.get("base") != BASE):
        reasons.append("OBSERVATION_SCOPE_CONFLICT")
    if observed.get("baseExists") is not False:
        reasons.append("FRESH_BASE_NOT_ABSENT")
    if observed.get("ports") != []:
        reasons.append("TARGET_PORTS_NOT_PROVEN_EMPTY")
    if any(observed.get(key) is not False for key in
           ("globalNewAPI26RegistrationExists", "globalNewAPI36RegistrationExists")):
        reasons.append("FRESH_REGISTRATIONS_NOT_ABSENT")
    free, total = observed.get("freeKiB"), observed.get("totalKiB")
    valid_memory = type(free) is int and type(total) is int and 0 < free <= total
    if not valid_memory:
        reasons.append("MEMORY_OBSERVATION_INVALID")
    elif free < REQUIRED_FREE_KIB:
        reasons.append("INSUFFICIENT_FREE_HOST_MEMORY")
    return {"status": "BLOCKED" if reasons else "HOST_PRECONDITIONS_ONLY",
            "sourceCommit": SOURCE, "reasons": reasons, "androidAdmission": False,
            "observationUtc": observed.get("utc"), "freeKiB": free,
            "requiredFreeKiB": REQUIRED_FREE_KIB,
            "classification": "LOCKED_HOST_OBSERVATION_NOT_CURRENT_DEVICE_PROOF",
            "processIdentity": "NOT_RUN", "androidIdentity": "NOT_RUN",
            "operationCounts": {"avdCreate": 0, "emulatorStart": 0, "adb": 0,
                                "apkInstall": 0, "instrumentation": 0}}


def read_locked_fixture():
    before = FIXTURE.lstat()
    if (FIXTURE.is_symlink() or getattr(before, "st_file_attributes", 0) & 0x400 or
            not stat.S_ISREG(before.st_mode) or before.st_nlink != 1 or before.st_size > 32768):
        raise ValueError("Locked host observation is not a bounded regular file")
    raw = FIXTURE.read_bytes()
    after = FIXTURE.lstat()
    if ((before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns) !=
            (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns) or
            len(raw) != before.st_size or hashlib.sha256(raw).hexdigest() != FIXTURE_SHA256):
        raise ValueError("Locked host observation SHA changed")
    return json.loads(raw)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=("report",))
    parser.parse_args()
    print(json.dumps(classify_host(read_locked_fixture()), sort_keys=True))


if __name__ == "__main__":
    main()
