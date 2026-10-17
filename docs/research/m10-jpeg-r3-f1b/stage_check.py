"""Host-only, read-only verifier for a collected synthetic research stage snapshot.

No Android APIs, subprocesses, file reads, cleanup, recovery, or authorization are
performed. The collector must use lstat and reject duplicate JSON keys while
parsing. A PASS describes this bounded snapshot, not a kernel lease/death proof.
"""
from __future__ import annotations

import re
import uuid
from typing import Any

ROOT = "files/synthetic-stages"
OWNER = {
    "protocol", "version", "attemptId", "sessionNonce", "generationIdentity",
    "consumerLeaseName", "stageDevice", "stageInode", "sourceSHA256",
    "sourceSize", "sourceDevice", "sourceInode", "guardDevice", "guardInode",
}
CHILDREN = {
    "owner.json", "consumer.guard", "state.json", "revoked.json", "cleanup.json",
    "receipt.json", "output-owner.json",
}
RECEIPT = {
    "protocol", "version", "attemptId", "sessionNonce", "generationIdentity",
    "sourceSHA256", "sourceSize", "kind", "workerResult", "resourceCloseStatus",
    "workerRetired", "formalReferences",
}


class Rejected(ValueError):
    pass


def need(ok: bool, reason: str) -> None:
    if not ok:
        raise Rejected(reason)


def integer(value: Any, minimum: int = 0) -> bool:
    return type(value) is int and value >= minimum


def canonical_uuid(value: Any) -> bool:
    try:
        return isinstance(value, str) and str(uuid.UUID(value)) == value
    except (ValueError, TypeError, AttributeError):
        return False


def digest(value: Any) -> bool:
    return isinstance(value, str) and re.fullmatch("[0-9a-f]{64}", value) is not None


def validate_snapshot(snapshot: dict[str, Any], expected_current: str | None = None) -> dict[str, Any]:
    """Return PASS/BLOCKED, reasons, attempts, currentAttempt, and namespace.

    entries contains path/type/device/inode/size/mode; json and sha256 map exact
    POSIX relative paths to parsed objects and SHA-256. Non-namespace entries are
    ignored. expected_current is required for the post-normal assertion and
    makes an absent/empty namespace unacceptable.
    """
    result: dict[str, Any] = {
        "status": "BLOCKED", "reasons": [], "attempts": [],
        "currentAttempt": None, "namespace": "UNKNOWN",
        "evidenceScope": "HOST_SNAPSHOT_ONLY_NOT_ANDROID_LEASE_OR_DEATH_PROOF",
    }
    try:
        need(isinstance(snapshot, dict), "snapshot must be an object")
        entries = snapshot.get("entries")
        objects, hashes = snapshot.get("json"), snapshot.get("sha256")
        need(isinstance(entries, list) and isinstance(objects, dict) and isinstance(hashes, dict),
             "entries/json/sha256 inventory missing")
        if expected_current is not None:
            need(canonical_uuid(expected_current), "invalid expected current attempt")
        by_path: dict[str, dict[str, Any]] = {}
        for entry in entries:
            need(isinstance(entry, dict) and isinstance(entry.get("path"), str), "invalid inventory entry")
            path = entry["path"]
            if path != ROOT and not path.startswith(ROOT + "/"):
                continue
            need(path not in by_path, "duplicate inventory path: " + path)
            need("\\" not in path and all(part not in ("", ".", "..") for part in path.split("/")),
                 "noncanonical namespace path: " + path)
            need(entry.get("type") in ("regular", "directory"), "symlink/unknown entry: " + path)
            need(integer(entry.get("device")) and integer(entry.get("inode"), 1)
                 and integer(entry.get("size")) and isinstance(entry.get("mode"), str),
                 "invalid lstat metadata: " + path)
            if entry["type"] == "regular":
                need(entry["size"] <= 8192, "research metadata size bound exceeded: " + path)
            by_path[path] = entry
        for mapping in (objects, hashes):
            for path in mapping:
                need(isinstance(path, str), "nonstring metadata path")
                if path == ROOT or path.startswith(ROOT + "/"):
                    need(path in by_path and by_path[path]["type"] == "regular", "orphan metadata: " + path)
        if ROOT not in by_path:
            need(not by_path, "stage children without namespace directory")
            result["namespace"] = "ABSENT"
            need(expected_current is None, "post-normal stage namespace absent")
            result["status"] = "PASS"
            return result
        result["namespace"] = "PRESENT"
        need(by_path[ROOT]["type"] == "directory", "stage namespace is not a directory")
        ids: set[str] = set()
        for path, entry in by_path.items():
            if path == ROOT:
                continue
            parts = path[len(ROOT) + 1:].split("/")
            if parts == ["current.json"]:
                need(entry["type"] == "regular", "current is not regular")
            else:
                need(canonical_uuid(parts[0]), "unknown stage root entry: " + path)
                ids.add(parts[0])
                need((len(parts) == 1 and entry["type"] == "directory") or
                     (len(parts) == 2 and parts[1] in CHILDREN and entry["type"] == "regular"),
                     "unknown/nonterminal child preserved: " + path)

        def obj(path: str, keys: set[str]) -> dict[str, Any]:
            need(path in by_path and by_path[path]["type"] == "regular", "missing regular metadata: " + path)
            value = objects.get(path)
            need(isinstance(value, dict) and set(value) == keys, "metadata keys/JSON rejected: " + path)
            return value

        pointer_path = ROOT + "/current.json"
        if pointer_path in by_path:
            current = obj(pointer_path, {"attemptId"})["attemptId"]
            need(canonical_uuid(current) and current in ids, "current does not identify an inventoried stage")
            result["currentAttempt"] = current
        if expected_current is not None:
            need(result["currentAttempt"] == expected_current, "post-normal current attempt mismatch")
        for identifier in sorted(ids):
            directory = ROOT + "/" + identifier
            need(directory in by_path and by_path[directory]["type"] == "directory", "stage directory missing")
            owner = obj(directory + "/owner.json", OWNER)
            need(owner["protocol"] == "lithic-r3-synthetic-stage" and type(owner["version"]) is int
                 and owner["version"] == 2 and owner["attemptId"] == identifier
                 and canonical_uuid(owner["sessionNonce"])
                 and owner["generationIdentity"] == "synthetic-generation-r3"
                 and owner["consumerLeaseName"] == "lithic-r3-" + identifier
                 and digest(owner["sourceSHA256"]) and type(owner["sourceSize"]) is int
                 and owner["sourceSize"] == 8192, "owner identity rejected: " + identifier)
            for key in ("stageDevice", "sourceDevice", "guardDevice"):
                need(integer(owner[key]), "owner device type rejected: " + identifier)
            for key in ("stageInode", "sourceInode", "guardInode"):
                need(integer(owner[key], 1), "owner inode type rejected: " + identifier)
            stage = by_path[directory]
            guard = by_path.get(directory + "/consumer.guard", {})
            need((stage["device"], stage["inode"]) == (owner["stageDevice"], owner["stageInode"])
                 and guard.get("type") == "regular"
                 and (guard.get("device"), guard.get("inode")) == (owner["guardDevice"], owner["guardInode"]),
                 "stage/guard inode rejected: " + identifier)

            def record(name: str, extras: set[str]) -> dict[str, Any]:
                value = obj(directory + "/" + name, OWNER | extras)
                need(all(type(value[key]) is type(owner[key]) and value[key] == owner[key] for key in OWNER),
                     "full owner mismatch: " + name + " " + identifier)
                return value

            state = record("state.json", {"requestStatus", "pin"})
            need(state["requestStatus"] == "CLEANED_SYNTHETIC" and state["pin"] is False,
                 "old stage not safely terminal (including REVOKED): " + identifier)
            revoke = record("revoked.json", {"revoked"})
            need(revoke["revoked"] is True, "durable revoke missing: " + identifier)
            cleanup = record("cleanup.json", {"proof", "syntheticOnly", "resourcesAbsent", "receiptSHA256", "outputOwnerSHA256"})
            need(cleanup["proof"] == "EXCLUSIVE_KERNEL_LEASE_AND_OWNED_INODES"
                 and cleanup["syntheticOnly"] is True and cleanup["resourcesAbsent"] is True,
                 "terminal proof rejected: " + identifier)
            for name, key in (("receipt.json", "receiptSHA256"), ("output-owner.json", "outputOwnerSHA256")):
                path = directory + "/" + name
                if path not in by_path:
                    need(cleanup[key] == "ABSENT", "missing retained metadata: " + name)
                else:
                    need(digest(hashes.get(path)) and cleanup[key] == hashes[path], "retained hash mismatch: " + name)
                    if name == "receipt.json":
                        receipt = obj(path, RECEIPT)
                        need(receipt["protocol"] == "lithic-r3-synthetic-result"
                             and type(receipt["version"]) is int and receipt["version"] == 1
                             and receipt["kind"] == "SYNTHETIC_TASK_ONLY" and receipt["workerResult"] == "SUCCESS"
                             and receipt["resourceCloseStatus"] == "DATA_FDS_CLOSED"
                             and receipt["workerRetired"] is True
                             and type(receipt["formalReferences"]) is int and receipt["formalReferences"] == 0,
                             "receipt result/retirement rejected: " + identifier)
                        for key in ("attemptId", "sessionNonce", "generationIdentity", "sourceSHA256", "sourceSize"):
                            need(type(receipt[key]) is type(owner[key]) and receipt[key] == owner[key], "receipt identity rejected: " + key)
                    else:
                        output = obj(path, {"attemptId", "sessionNonce", "device", "inode"})
                        need(output["attemptId"] == identifier and output["sessionNonce"] == owner["sessionNonce"]
                             and integer(output["device"]) and integer(output["inode"], 1), "output owner rejected")
            if expected_current == identifier:
                need(directory + "/receipt.json" in by_path and directory + "/output-owner.json" in by_path,
                     "normal requires retained receipt and output owner")
            result["attempts"].append({"attemptId": identifier, "status": "TERMINAL_VERIFIED"})
        result["status"] = "PASS"
    except (Rejected, KeyError, TypeError, ValueError) as error:
        result["reasons"].append(str(error))
    return result
