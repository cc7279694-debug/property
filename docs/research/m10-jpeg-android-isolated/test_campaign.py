"""Targeted host-only campaign seams; no device or Android command runs."""
from contextlib import ExitStack
import importlib
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch


ID = "m10-final-20261019-123456-0123abcd"


class CampaignTests(unittest.TestCase):
    def setUp(self):
        self.campaign = importlib.import_module("campaign")
        self.stack = ExitStack()
        self.addCleanup(self.stack.close)
        temporary = self.stack.enter_context(tempfile.TemporaryDirectory())
        self.root = Path(temporary) / "repo"
        self.base = Path(temporary) / "environments"
        self.stack.enter_context(patch.object(self.campaign, "ROOT", self.root))
        self.stack.enter_context(patch.object(self.campaign, "BASE", self.base))
        self.stack.enter_context(patch.object(self.campaign.CampaignSupervisor,
                                            "environment", return_value={}))

    def make(self, api=26, campaign_id=ID):
        return self.campaign.CampaignSupervisor(api, campaign_id)

    def test_campaign_id_rejects_path_traversal_and_malformed_identity(self):
        for value in ("../escape", ID + "/child", "m10-final-20261019-123456-0123ABCZ", "", None):
            with self.subTest(value=value), self.assertRaises((RuntimeError, ValueError, TypeError)):
                self.make(campaign_id=value)

    def test_evidence_and_operation_markers_are_new_campaign_scoped(self):
        supervisor = self.make()
        self.assertEqual(self.root / "build/m10-final-campaign" / ID / "api26",
                         supervisor.evidence)
        supervisor.marker("synthetic-host-only")
        expected = self.base / "campaigns" / ID / "api26/operations/synthetic-host-only.json"
        self.assertTrue(expected.is_file())
        self.assertFalse((self.base / "api26/operations/synthetic-host-only.json").exists())

    def test_duplicate_operation_marker_fails_and_keeps_original_bytes(self):
        supervisor = self.make()
        supervisor.marker("synthetic-once")
        target = self.base / "campaigns" / ID / "api26/operations/synthetic-once.json"
        original = target.read_bytes()
        with self.assertRaises(FileExistsError):
            supervisor.marker("synthetic-once")
        self.assertEqual(original, target.read_bytes())

    def test_new_process_identity_uses_current_anchor_without_old_resume_anchor(self):
        supervisor = self.make()
        anchor = {"campaign": ID, "targets": [{"ProcessId": 123}]}
        supervisor._runtime_anchor = anchor
        supervisor.resume_anchor = Mock(side_effect=AssertionError("Old PID anchor must never be used"))
        supervisor.process_spec = Mock(return_value={"synthetic": True})
        supervisor.processes = Mock()
        supervisor.note = Mock()
        proof = {"status": "PASS", "anchor": anchor, "snapshot": {"targets": []}}
        with patch.object(self.campaign.runtime_identity, "await_complete", return_value=proof) as observe:
            self.assertEqual(proof["snapshot"], supervisor.validate_process())
        self.assertIs(anchor, observe.call_args.args[1])
        supervisor.resume_anchor.assert_not_called()

    def test_identity_without_new_campaign_boot_proof_cannot_send_adb(self):
        supervisor = self.make()
        supervisor.adb = Mock(side_effect=AssertionError("No device command before current proof"))
        supervisor.verify_avd = Mock()
        supervisor.validate_process = Mock()
        supervisor.runtime_paths = Mock(return_value={})
        with self.assertRaises((RuntimeError, FileNotFoundError)):
            supervisor.identity()
        supervisor.adb.assert_not_called()

    def test_cold_start_arguments_preserve_disk_and_explicit_snapshot_disable(self):
        supervisor = self.make()
        arguments = supervisor.launch_arguments()
        self.assertEqual(1, arguments.count("-no-snapshot"))
        self.assertFalse(any(value.startswith("-wipe") for value in arguments))
        self.assertNotIn("-snapshot", arguments)
        self.assertEqual(str(supervisor.data), arguments[arguments.index("-datadir") + 1])
        supervisor.marker("start")
        with self.assertRaises(FileExistsError):
            supervisor.marker("start")

    def test_pm_substring_filter_accepts_only_the_two_pinned_packages(self):
        supervisor = self.make()
        supervisor.identity = Mock()
        app = "com.cooper.lithic.debug"
        test = app + ".test"
        supervisor.apk_audit = Mock(return_value={"apks": [
            {"packageName": app, "sha256": "a" * 64, "kind": "app"},
            {"packageName": test, "sha256": "b" * 64, "kind": "test"}]})
        supervisor.adb = Mock(side_effect=["package:" + app + "\npackage:" + test, "package:" + test])
        supervisor.installed = Mock(side_effect=[10077, 10078])
        supervisor.install = Mock(side_effect=AssertionError("Matching packages must not be installed again"))
        supervisor.note = Mock()
        supervisor.ensure_apks()
        self.assertEqual(2, supervisor.installed.call_count)
        supervisor.install.assert_not_called()
        supervisor.adb = Mock(return_value="package:" + app + "\npackage:unrelated.unknown")
        with self.assertRaises(RuntimeError):
            supervisor.ensure_apks()


if __name__ == "__main__":
    unittest.main()
