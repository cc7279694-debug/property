"""Narrow host-only compatibility admission tests; no Android commands."""
from contextlib import ExitStack
import importlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch


class CompatCampaignTests(unittest.TestCase):
    def setUp(self):
        self.m = importlib.import_module("compat_campaign")
        self.stack = ExitStack()
        self.addCleanup(self.stack.close)
        directory = Path(self.stack.enter_context(tempfile.TemporaryDirectory()))
        self.s = object.__new__(self.m.CompatCampaign)
        self.s.api = 26
        self.s.serial = "emulator-5624"
        self.s.evidence = directory / "evidence"
        self.s.evidence.mkdir()
        self.s.control = directory / "control"
        self.s.control.mkdir()
        self.s.campaign_id = "m10-final-20261019-123456-0123abcd"
        self.s.identity = Mock()
        self.s.marker = Mock()
        self.s.note = Mock()
        self.s.adb = Mock(return_value="Success")
        self.s.installed = Mock(side_effect=lambda package, sha: 10077 if package == self.m.v.APP else 10078)
        self.s.apk_audit = Mock(return_value={"apks": [{"kind": "test", "path": str(directory / "new.apk"), "sha256": "f" * 64}]})
        self.stack.enter_context(patch.object(self.m.v, "TEST_SHA", "f" * 64))
        self.stack.enter_context(patch.object(self.m.natural_tests, "TEST_SHA", "f" * 64))
        self.snapshot = {"status": "PASS", "api": 26, "serial": self.s.serial, "uid": 10077,
                         "stages": {"old": {"terminal": True}}, "observations": {"old": {"value": True}}, "phaseLog": ""}
        self.s.resource_snapshot = Mock(return_value=self.snapshot)
        self.s.ownership = Mock(return_value="10077|10077|1|2|41c0\n10077|10077|1|3|41c0")

    def test_update_uses_one_replacing_install_and_preserves_application(self):
        result = self.s.update_test()
        self.assertEqual("PASS", result["status"])
        self.s.adb.assert_called_once()
        self.assertEqual(("install", "-r", "new.apk"), self.s.adb.call_args.args)
        self.s.marker.assert_called_once_with("install-test-update")
        self.assertEqual(2, self.s.installed.call_args_list.count(((self.m.v.APP, self.m.v.APP_SHA), {})))

    def test_update_rejects_blocked_stage_before_install(self):
        self.s.resource_snapshot.return_value = {**self.snapshot, "status": "BLOCKED"}
        with self.assertRaises(RuntimeError): self.s.update_test()
        self.s.adb.assert_not_called()

    def test_update_rejects_wrong_api_without_install(self):
        self.s.api = 36
        with self.assertRaises(RuntimeError): self.s.update_test()
        self.s.adb.assert_not_called()

    def test_update_rejects_missing_explicit_success(self):
        self.s.adb.return_value = "Failure"
        with self.assertRaises(RuntimeError): self.s.update_test()
        self.assertEqual(1, self.s.adb.call_count)
        self.assertEqual("UNKNOWN", json.loads((self.s.evidence / "test-update/result.json").read_bytes())["status"])

    def test_update_detects_changed_private_owner(self):
        self.s.ownership.side_effect = ["before", "after"]
        with self.assertRaises(RuntimeError): self.s.update_test()
        self.assertEqual(1, self.s.adb.call_count)

    def test_update_detects_changed_stage_and_does_not_clean(self):
        self.s.resource_snapshot.side_effect = [self.snapshot, {**self.snapshot, "stages": {}}]
        with self.assertRaises(RuntimeError): self.s.update_test()
        self.assertEqual(1, self.s.adb.call_count)

    def test_update_detects_uid_change(self):
        self.s.installed.side_effect = [10077, 10078, 10079, 10078]
        with self.assertRaises(RuntimeError): self.s.update_test()

    def test_second_update_cannot_issue_another_install(self):
        self.s.update_test()
        with self.assertRaises(FileExistsError): self.s.update_test()
        self.assertEqual(1, self.s.adb.call_count)

    def test_method_without_loader_proof_never_calls_instrumentation(self):
        with self.assertRaises((RuntimeError, FileNotFoundError)): self.s.run_method(0)
        self.s.adb.assert_not_called()

    def test_loader_proof_is_bound_to_campaign_api_and_new_test_sha(self):
        p = self.s.loader_identity()
        self.m.immutable(self.s.load_proof, {**p, "status": "PASS"})
        self.s.require_loader()
        for field, value in (("api", 36), ("campaignId", "old"), ("testSHA256", self.m.OLD_TEST_SHA)):
            altered = {**p, "status": "PASS", field: value}
            self.s.load_proof.write_text(json.dumps(altered))
            with self.assertRaises(RuntimeError): self.s.require_loader()

    def test_unknown_loader_status_stays_closed(self):
        self.m.immutable(self.s.load_proof, {**self.s.loader_identity(), "status": "UNKNOWN"})
        with self.assertRaises(RuntimeError): self.s.require_loader()

    def test_api36_never_inherits_api26_historical_diagnostics(self):
        self.s.api = 36
        self.assertEqual((), self.s.authenticated_baseline_diagnostics())

    def test_historical_diagnostics_are_authenticated_without_borrowing_old_junit(self):
        proofs = [({"readinessProtocol": "DIAGNOSTIC_NONCE_V1", "handshake": {"authenticatedRawLine": f"line{i}"}}, "raw") for i in range(3)]
        with patch.object(self.m, "read_verified", side_effect=proofs) as read:
            self.assertEqual(("line0", "line1", "line2"), self.s.authenticated_baseline_diagnostics())
        self.assertEqual(3, read.call_count)
        self.assertTrue(all(c.kwargs == {"expected_serial": self.s.serial} for c in read.call_args_list))

    def test_duplicate_or_unverified_diagnostic_is_not_whitelisted(self):
        proof = ({"readinessProtocol": "DIAGNOSTIC_NONCE_V1", "handshake": {"authenticatedRawLine": "same"}}, "raw")
        with patch.object(self.m, "read_verified", return_value=proof), self.assertRaises(RuntimeError):
            self.s.authenticated_baseline_diagnostics()

    def test_unchanged_check_rejects_identity_observation_or_stage_difference(self):
        for field, value in (("uid", 1), ("observations", {}), ("stages", {}), ("status", "UNKNOWN")):
            with self.subTest(field=field), self.assertRaises(RuntimeError):
                self.s.unchanged(self.snapshot, {**self.snapshot, field: value})

    def prepare_loader(self):
        collector = Mock(spec=["start", "assert_live", "finish"])
        collector.start.return_value = {"status": "PASS"}
        collector.assert_live.return_value = {"status": "PASS"}
        collector.finish.return_value = {"status": "PASS"}
        self.s.loader_capture = Mock(return_value=(collector, ()))
        cls, name = self.m.LOADER.split("#")
        text = "".join(f"INSTRUMENTATION_STATUS: class={cls}\nINSTRUMENTATION_STATUS: test={name}\n"
                       f"INSTRUMENTATION_STATUS: numtests=1\nINSTRUMENTATION_STATUS_CODE: {status}\n" for status in (1, 0))
        text += "OK (1 test)\nINSTRUMENTATION_CODE: -1\n"
        self.s.adb.side_effect = [f"instrumentation:{self.m.v.INSTRUMENTATION} (target={self.m.v.APP})", text]
        self.stack.enter_context(patch.object(self.m, "consume_capture", return_value={"newPhaseLog": ""}))
        return collector

    def test_loader_pass_opens_only_current_fixed_method_and_records_proof(self):
        collector = self.prepare_loader()
        result = self.s.class_load()
        self.assertEqual("PASS", result["status"])
        self.assertEqual(1, result["instrumentationCommandsIssued"])
        self.assertEqual(self.m.LOADER, self.s.adb.call_args.args[7])
        self.s.require_loader()
        collector.finish.assert_called_once()

    def test_loader_without_ready_never_issues_instrumentation(self):
        collector = self.prepare_loader()
        collector.start.return_value = {"status": "UNKNOWN"}
        with self.assertRaises(RuntimeError): self.s.class_load()
        self.assertEqual(1, self.s.adb.call_count)
        self.assertFalse(self.s.load_proof.exists())
        collector.finish.assert_called_once()

    def test_loader_junit_failure_does_not_open_natural_methods(self):
        self.prepare_loader()
        self.s.adb.side_effect = [f"instrumentation:{self.m.v.INSTRUMENTATION} (target={self.m.v.APP})", "FAILURES!!!\n"]
        with self.assertRaises(RuntimeError): self.s.class_load()
        self.assertFalse(self.s.load_proof.exists())

    def test_loader_stage_change_is_unknown_and_does_not_clean(self):
        self.prepare_loader()
        self.s.resource_snapshot.side_effect = [self.snapshot, {**self.snapshot, "stages": {}}]
        with self.assertRaises(RuntimeError): self.s.class_load()
        self.assertFalse(self.s.load_proof.exists())
        self.assertEqual(2, self.s.adb.call_count)

    def test_loader_unexpected_worker_log_is_not_class_loading_success(self):
        self.prepare_loader()
        self.m.consume_capture.return_value = {"newPhaseLog": "unexpected Worker"}
        with self.assertRaises(RuntimeError): self.s.class_load()
        self.assertFalse(self.s.load_proof.exists())

    def test_loader_finish_exception_is_not_retried(self):
        collector = self.prepare_loader()
        collector.finish.side_effect = RuntimeError("unknown host capture drain")
        self.m.consume_capture.side_effect = RuntimeError("capture UNKNOWN")
        with self.assertRaises(RuntimeError): self.s.class_load()
        collector.finish.assert_called_once()
        self.assertFalse(self.s.load_proof.exists())

    def test_second_loader_has_no_second_activity_or_instrumentation_command(self):
        self.prepare_loader()
        self.s.class_load()
        with self.assertRaises(RuntimeError): self.s.class_load()
        self.assertEqual(2, self.s.adb.call_count)


if __name__ == "__main__": unittest.main()
