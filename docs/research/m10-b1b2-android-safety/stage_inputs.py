"""One-shot, root-invoked transfer of three locked synthetic B2 inputs.

No APK, private ledger, image discovery, consumer, deletion or retry entry.
The only device writes are fresh files beneath this task's new target package.
"""
from __future__ import annotations
import json
from pathlib import Path
import re
import shlex
import stat
import sys

from fresh_environment import FreshSupervisor, APP, APP_SHA, TEST, TEST_SHA, ARTIFACTS, ARTIFACTS_SHA
from fresh_environment import locked_json, immutable, file_digest, need, utc

NAMES = ("large_40mp_baseline_420.jpg", "large_40mp_progressive_420.jpg",
         "valid_40mp_progressive_444.jpg")
DESTINATION = "files/m10-b2-test-inputs"
LOCKED_SHA = (
    "c2c2fa54d49ab8155321175a66596ce8d35ed9927d9f3dfe2bbb6a00c5fbb5c7",
    "2b02f35f08a8cb36d31a0a4f01e8810f554be67619950c8e2b0d0d2be873c5c8",
    "0555234207d5ddf84d236d812487a659f2dac34a9d82109a941628553c7b67e6",
)


def selected_inputs(audit):
    rows = audit.get("fixtures", [])
    need(len(rows) == 3 and tuple(row.get("basename") for row in rows) == NAMES and
         tuple(row.get("sha256") for row in rows) == LOCKED_SHA, "Unexpected fixed fixture set/order/SHA")
    for row in rows:
        path = Path(row["path"])
        need(path.is_absolute() and path.name == row["basename"] and
             type(row.get("bytes")) is int and 0 < row["bytes"] <= 64 * 1024 * 1024,
             "Invalid fixed synthetic source record")
    return rows


def private_path(name):
    need(name in NAMES, "Unknown private input filename")
    return DESTINATION + "/" + name


def claim_transfer(supervisor):
    supervisor.scope()
    directory = supervisor.evidence / "input-transfer"
    directory.mkdir(exist_ok=False)
    immutable(directory / "attempt.json", {**supervisor.binding(), "utc": utc(),
              "operation": "FRESH_B2_INPUT_TRANSFER", "automaticRetry": False, "inputBasenames": NAMES})
    return directory


def ensure_owned_input_parent(supervisor, uid, claim):
    """Prospective only: prepare files/ within a new, unfinished exclusive claim.

    A consumed/UNKNOWN claim is never an entrance to retry this operation.
    Existing directories are inspected, never chmodded or replaced.
    """
    supervisor.scope()
    need(type(uid) is int and 0 < uid < 2 ** 31 and
         claim == supervisor.evidence / "input-transfer" and claim.is_dir() and not claim.is_symlink(),
         "Input parent lacks its exclusive transfer claim")
    marker = claim / "attempt.json"
    done = claim / "result.json"
    need(marker.is_file() and not marker.is_symlink() and marker.stat().st_nlink == 1 and
         not done.exists() and not done.is_symlink(), "Transfer claim already consumed or unowned")
    value = json.loads(marker.read_text(encoding="utf-8"))
    expected = {**supervisor.binding(), "operation": "FRESH_B2_INPUT_TRANSFER",
                "automaticRetry": False, "inputBasenames": list(NAMES)}
    need(set(value) == set(expected) | {"utc"} and all(value.get(k) == v for k, v in expected.items()) and
         isinstance(value.get("utc"), str) and value["utc"], "Transfer claim identity conflicts")
    root = "/data/user/0/" + APP
    script = ("set -e; umask 077; "
              f"[ \"$(id -u)\" = '{uid}' ]; [ \"$(pwd -P)\" = '{root}' ]; "
              f"[ -d . ]; [ \"$(stat -c '%u' .)\" = '{uid}' ]; "
              "printf 'M10_INPUT_PARENT_BEGIN\\n'; stat -c 'ROOT_B|%f:%u:%d:%i' .; "
              "[ ! -L files ]; "
              "if [ -e files ]; then [ -d files ]; stat -c 'FILES_B|%f:%u:%d:%i' files; state=EXISTING; "
              "else printf 'FILES_B|ABSENT\\n'; mkdir files; state=CREATED; fi; "
              "[ ! -L files ]; [ -d files ]; stat -c 'FILES_E|%f:%u:%d:%i' files; "
              "stat -c 'ROOT_E|%f:%u:%d:%i' .; printf 'M10_INPUT_PARENT_END|%s\\n' \"$state\"")
    raw = supervisor.adb("shell", "run-as " + APP + " sh -c " + shlex.quote(script))
    lines = raw.splitlines()
    need(len(lines) == 6 and lines[0] == "M10_INPUT_PARENT_BEGIN" and
         lines[-1] in ("M10_INPUT_PARENT_END|CREATED", "M10_INPUT_PARENT_END|EXISTING"),
         "Input parent proof incomplete or unknown")

    def metadata(line, prefix):
        need(line.startswith(prefix), "Input parent proof order conflicts")
        values = line[len(prefix):].split(":")
        need(len(values) == 4 and re.fullmatch(r"[0-9a-f]+", values[0]) and
             all(re.fullmatch(r"[1-9][0-9]*", x) for x in values[1:]), "Malformed input parent identity")
        mode, owner, device, inode = int(values[0], 16), *(int(x) for x in values[1:])
        need(stat.S_ISDIR(mode) and not stat.S_ISLNK(mode) and owner == uid,
             "Input parent/root is not an owned non-link directory")
        return {"mode": mode, "uid": owner, "device": device, "inode": inode}

    root_before, root_after = metadata(lines[1], "ROOT_B|"), metadata(lines[4], "ROOT_E|")
    parent = metadata(lines[3], "FILES_E|")
    status = lines[-1].split("|")[1]
    need(root_before == root_after and parent["device"] == root_before["device"] and
         parent["inode"] != root_before["inode"], "Input private root/parent identity changed")
    if status == "CREATED":
        need(lines[2] == "FILES_B|ABSENT" and stat.S_IMODE(parent["mode"]) == 0o700,
             "New input parent is not an exclusively created 0700 directory")
    else:
        need(metadata(lines[2], "FILES_B|") == parent, "Existing input parent changed during validation")
    return {"status": status, "path": "files", "privateRoot": root,
            "root": root_after, "parent": parent, "scope": "OWNED_SYNTHETIC_TEST_PACKAGE_ONLY"}


def transfer(api):
    s = FreshSupervisor(api)
    s.identity()
    uid = s.installed(APP, APP_SHA)
    s.apk_audit()
    proof = s.require_proof(s.home / "install-test-proof.json")
    test_uid = s.installed(TEST, TEST_SHA)
    need(proof.get("kind") == "test" and proof.get("package") == TEST and
         proof.get("sha256") == TEST_SHA and proof.get("uid") == test_uid and test_uid != uid,
         "Current locked Test APK/User 0 UID does not match its independent install proof")
    # Source records were acquired by the host audit, never by a device scan.
    audit = locked_json(ARTIFACTS, ARTIFACTS_SHA)
    rows = selected_inputs(audit)
    for row in rows:
        path = Path(row["path"])
        need(path.is_absolute() and file_digest(path, row["bytes"]) == row["sha256"],
             "Locked synthetic source changed")
    directory = claim_transfer(s)
    result = {"status": "UNKNOWN", "source": "LOCKED_SYNTHETIC_NOT_USER_IMAGES", "files": []}
    try:
        worker = s.adb("shell", "pidof", APP + ":jpeg_worker", allowed=(0, 1))
        need(worker == "", "Worker exists or process absence is unknown")
        result["inputParent"] = ensure_owned_input_parent(s, uid, directory)
        script = (f"[ ! -e {DESTINATION} ] && [ ! -L {DESTINATION} ] && "
                  f"mkdir {DESTINATION} && chmod 700 {DESTINATION}")
        s.adb("shell", "run-as " + APP + " sh -c " + shlex.quote(script))
        for row in rows:
            remote = private_path(row["basename"])
            # Verify once more before transmission; binary stdin is not placed in a command journal.
            with Path(row["path"]).open("rb") as source:
                data = source.read(row["bytes"] + 1)
            need(len(data) == row["bytes"] and __import__("hashlib").sha256(data).hexdigest() == row["sha256"],
                 "Synthetic input bytes changed before transfer")
            command = f"umask 077; set -C; [ ! -e {remote} ] && [ ! -L {remote} ] && cat > {remote}"
            argv = [s.sdk / "platform-tools/adb.exe", "-s", s.serial, "shell", "-T",
                    "run-as " + APP + " sh -c " + shlex.quote(command)]
            # API26's secondary SDK need not contain adb; always use the existing approved primary tool.
            from fresh_environment import legacy
            argv[0] = legacy.TOOLS / "platform-tools/adb.exe"
            s.note({"operation": "KNOWN_SYNTHETIC_INPUT_BYTES", "basename": row["basename"],
                    "bytes": len(data), "sha256": row["sha256"], "serial": s.serial})
            s.scope()
            s.run(argv, input_bytes=data, timeout=90)
            stat = s.adb("shell", "run-as", APP, "stat", "-c", "%f:%u:%h:%s:%d:%i", remote)
            parts = stat.split(":")
            need(len(parts) == 6 and int(parts[0], 16) & 0xF000 == 0x8000 and
                 int(parts[0], 16) & 0o777 == 0o600 and int(parts[1]) == uid and
                 int(parts[2]) == 1 and int(parts[3]) == row["bytes"] and
                 int(parts[4]) > 0 and int(parts[5]) > 0, "Private source identity/mode conflict")
            sha = s.adb("shell", "run-as", APP, "sha256sum", remote).split()
            need(sha == [row["sha256"], remote], "Private source bytes conflict")
            result["files"].append({"basename": row["basename"], "bytes": row["bytes"],
                                    "sha256": sha[0], "privateStat": stat, "uid": uid})
        listing = s.adb("shell", "run-as", APP, "ls", "-1A", DESTINATION).splitlines()
        need(sorted(listing) == sorted(NAMES), "Unknown or incomplete input directory")
        directory_stat = s.adb("shell", "run-as", APP, "stat", "-c", "%f:%u", DESTINATION).split(":")
        need(len(directory_stat) == 2 and int(directory_stat[0], 16) & 0xF000 == 0x4000 and
             int(directory_stat[0], 16) & 0o777 == 0o700 and int(directory_stat[1]) == uid,
             "Private directory identity/mode conflict")
        result.update(status="PASS", directory=DESTINATION, directoryStat=directory_stat,
                      privateOwnershipVerified=True, personalContentAccessed=False)
    except Exception as error:
        result["error"] = {"type": type(error).__name__, "detail": str(error)}
        raise
    finally:
        immutable(directory / "result.json", result)
    return result


if __name__ == "__main__":
    need(len(sys.argv) == 2 and sys.argv[1] in ("26", "36"), "Only an explicit fresh API target is supported")
    print(json.dumps(transfer(int(sys.argv[1])), sort_keys=True))
