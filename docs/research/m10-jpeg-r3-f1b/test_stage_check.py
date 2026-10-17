"""Synthetic host inventory tests only; NOT Android FD/lease/lifecycle tests."""
import copy
import hashlib
import json
import unittest

from stage_check import ROOT, validate_snapshot

ID = "00000000-0000-4000-8000-000000000001"
NONCE = "00000000-0000-4000-8000-000000000002"


def terminal(receipt=True):
    snapshot = {"entries": [], "json": {}, "sha256": {}}

    def entry(path, kind="regular", device=3, inode=1, size=0):
        snapshot["entries"].append({"path": path, "type": kind, "device": device,
                                    "inode": inode, "size": size, "mode": "synthetic"})

    def obj(path, value):
        raw = json.dumps(value, sort_keys=True).encode()
        entry(path, inode=100 + len(snapshot["entries"]), size=len(raw))
        snapshot["json"][path] = value
        snapshot["sha256"][path] = hashlib.sha256(raw).hexdigest()

    directory = ROOT + "/" + ID
    entry(ROOT, "directory", inode=10)
    entry(directory, "directory", inode=11)
    entry(directory + "/consumer.guard", inode=12)
    owner = {"protocol": "lithic-r3-synthetic-stage", "version": 2,
             "attemptId": ID, "sessionNonce": NONCE, "generationIdentity": "synthetic-generation-r3",
             "consumerLeaseName": "lithic-r3-" + ID, "stageDevice": 3, "stageInode": 11,
             "sourceSHA256": "a" * 64, "sourceSize": 8192, "sourceDevice": 3,
             "sourceInode": 13, "guardDevice": 3, "guardInode": 12}
    obj(directory + "/owner.json", owner)
    obj(ROOT + "/current.json", {"attemptId": ID})
    obj(directory + "/state.json", dict(owner, requestStatus="CLEANED_SYNTHETIC", pin=False))
    obj(directory + "/revoked.json", dict(owner, revoked=True))
    if receipt:
        r = {key: owner[key] for key in ("attemptId", "sessionNonce", "generationIdentity", "sourceSHA256", "sourceSize")}
        r.update(protocol="lithic-r3-synthetic-result", version=1, kind="SYNTHETIC_TASK_ONLY",
                 workerResult="SUCCESS", resourceCloseStatus="DATA_FDS_CLOSED", workerRetired=True, formalReferences=0)
        obj(directory + "/receipt.json", r)
        obj(directory + "/output-owner.json", {"attemptId": ID, "sessionNonce": NONCE, "device": 3, "inode": 14})
    obj(directory + "/cleanup.json", dict(owner, proof="EXCLUSIVE_KERNEL_LEASE_AND_OWNED_INODES",
        syntheticOnly=True, resourcesAbsent=True,
        receiptSHA256=snapshot["sha256"].get(directory + "/receipt.json", "ABSENT"),
        outputOwnerSHA256=snapshot["sha256"].get(directory + "/output-owner.json", "ABSENT")))
    return snapshot


class HostStageSnapshotTest(unittest.TestCase):
    def assert_blocked(self, snapshot, expected=None):
        result = validate_snapshot(snapshot, expected)
        self.assertEqual("BLOCKED", result["status"], result)
        self.assertTrue(result["reasons"])

    def test_absent_namespace_safe_only_for_preflight(self):
        empty = {"entries": [], "json": {}, "sha256": {}}
        self.assertEqual("PASS", validate_snapshot(empty)["status"])
        self.assert_blocked(empty, ID)

    def test_terminal_with_matching_normal_current_passes(self):
        result = validate_snapshot(terminal(), ID)
        self.assertEqual("PASS", result["status"], result)
        self.assertEqual(ID, result["currentAttempt"])
        self.assertEqual([{"attemptId": ID, "status": "TERMINAL_VERIFIED"}], result["attempts"])

    def test_terminal_without_receipt_preflight_only(self):
        self.assertEqual("PASS", validate_snapshot(terminal(False))["status"])
        self.assert_blocked(terminal(False), ID)

    def test_revoked_state_blocks_without_cleanup_or_recovery(self):
        value = terminal()
        value["json"][ROOT + "/" + ID + "/state.json"].update(requestStatus="REVOKED", pin=True)
        original = copy.deepcopy(value)
        self.assert_blocked(value)
        self.assertEqual(original, value)

    def test_active_pin_and_unknown_request_state_block(self):
        for status in ("FD_SENT", "DELIVERY_UNKNOWN", "UNKNOWN"):
            value = terminal()
            value["json"][ROOT + "/" + ID + "/state.json"].update(requestStatus=status, pin=True)
            self.assert_blocked(value)

    def test_wrong_owner_guard_or_stage_inode_blocks(self):
        for field in ("stageInode", "guardInode", "stageDevice", "guardDevice"):
            value = terminal()
            value["json"][ROOT + "/" + ID + "/owner.json"][field] = 999
            self.assert_blocked(value)

    def test_full_identity_mismatch_and_extra_keys_block(self):
        for name in ("state.json", "revoked.json", "cleanup.json"):
            value = terminal()
            value["json"][ROOT + "/" + ID + "/" + name]["sessionNonce"] = ID
            self.assert_blocked(value)
        value = terminal()
        value["json"][ROOT + "/" + ID + "/owner.json"]["extra"] = 1
        self.assert_blocked(value)

    def test_unknown_child_pending_source_output_and_nested_path_block(self):
        for name in ("unknown.bin", "state.json.pending", "source.bin", "output.bin", "unknown/x"):
            value = terminal()
            value["entries"].append({"path": ROOT + "/" + ID + "/" + name, "type": "regular",
                                      "device": 3, "inode": 987, "size": 0, "mode": "synthetic"})
            self.assert_blocked(value)

    def test_symlink_and_noncanonical_paths_block(self):
        value = terminal()
        value["entries"][1]["type"] = "symlink"
        self.assert_blocked(value)
        value = terminal()
        value["entries"][1]["path"] += "/.."
        self.assert_blocked(value)

    def test_current_missing_or_mismatch_blocks_postnormal(self):
        value = terminal()
        value["json"][ROOT + "/current.json"] = {"attemptId": NONCE}
        self.assert_blocked(value, ID)
        value = terminal()
        value["entries"] = [entry for entry in value["entries"] if entry["path"] != ROOT + "/current.json"]
        del value["json"][ROOT + "/current.json"]
        del value["sha256"][ROOT + "/current.json"]
        self.assert_blocked(value, ID)

    def test_receipt_hash_retirement_and_identity_mismatch_block(self):
        for field, replacement in (("workerRetired", False), ("attemptId", NONCE), ("formalReferences", True)):
            value = terminal()
            value["json"][ROOT + "/" + ID + "/receipt.json"][field] = replacement
            self.assert_blocked(value)
        value = terminal()
        value["sha256"][ROOT + "/" + ID + "/receipt.json"] = "b" * 64
        self.assert_blocked(value)

    def test_missing_or_malformed_terminal_proof_blocks(self):
        value = terminal()
        value["json"][ROOT + "/" + ID + "/cleanup.json"] = {"syntheticOnly": True}
        self.assert_blocked(value)
        value = terminal()
        value["json"][ROOT + "/" + ID + "/revoked.json"]["revoked"] = False
        self.assert_blocked(value)

    def test_duplicate_inventory_bool_numbers_and_oversize_metadata_block(self):
        value = terminal()
        value["entries"].append(copy.deepcopy(value["entries"][0]))
        self.assert_blocked(value)
        value = terminal()
        value["entries"][0]["device"] = True
        self.assert_blocked(value)
        value = terminal()
        value["entries"][-1]["size"] = 8193
        self.assert_blocked(value)

    def test_orphan_metadata_and_missing_directory_block(self):
        value = terminal()
        value["entries"] = [entry for entry in value["entries"] if entry["path"] != ROOT]
        self.assert_blocked(value)
        value = terminal()
        value["json"][ROOT + "/untracked.json"] = {}
        self.assert_blocked(value)

    def test_output_owner_mismatch_blocks(self):
        value = terminal()
        value["json"][ROOT + "/" + ID + "/output-owner.json"]["sessionNonce"] = ID
        self.assert_blocked(value)

    def test_namespace_external_entries_ignored_and_snapshot_unmodified(self):
        value = terminal()
        value["entries"].append({"path": "files/r3-traces/test.jsonl", "type": "regular"})
        original = copy.deepcopy(value)
        self.assertEqual("PASS", validate_snapshot(value)["status"])
        self.assertEqual(original, value)


if __name__ == "__main__":
    unittest.main(verbosity=2)
