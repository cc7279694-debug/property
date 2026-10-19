"""Pure host admission seams. Android operations are always mocked."""
from contextlib import ExitStack
import importlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch


RID = "m10-uid-20261019-123456-0123abcd"


class ResumeTests(unittest.TestCase):
    def setUp(self):
        self.m = importlib.import_module("resume")
        self.stack = ExitStack()
        self.addCleanup(self.stack.close)
        self.root = Path(self.stack.enter_context(tempfile.TemporaryDirectory()))
        self.s = object.__new__(self.m.API36UidResume)
        self.s.api, self.s.serial = 36, "emulator-5626"
        self.s.name = "Lithic_M10V_API36_20261018"
        self.s.campaign_id, self.s.resume_id = self.m.CID, RID
        self.s.control = self.root / "control"
        self.s.control.mkdir()
        self.s.operations = self.s.control / "operations"
        self.s.operations.mkdir()
        self.s.evidence = self.root / "evidence"
        self.s.evidence.mkdir()
        self.s.boot_proof_path = self.s.control / "resume-boot-proof.json"
        self.s.data = self.root / "same-existing-data"
        self.s.identity = Mock()
        self.s.note = Mock()
        self.s.installed = Mock(side_effect=lambda p, _: 10216 if p == self.m.v.APP else 10217)
        self.snapshot = {"status": "PASS", "api": 36, "serial": self.s.serial, "uid": 10216,
                         "stages": {}, "observations": {}, "phaseLog": "", "processes": {"rows": []}}
        self.s.resource_snapshot = Mock(return_value=self.snapshot)
        self.s.ownership = Mock(return_value="10216|10216|1|2|41c0")
        self.test = {"kind": "test", "path": str(self.root / "test.apk"),
                     "sha256": self.m.v.TEST_SHA, "certificateSHA256": self.m.v.CERT,
                     "packageName": self.m.v.TEST}
        self.s.apk_audit = Mock(return_value={"apks": [self.test]})
        self.s.adb = Mock(return_value="")

    def write_app(self):
        self.m.immutable(self.s.app_proof, {**self.s.binding(), "status": "PASS", "appUID": 10216,
            "resource": self.snapshot, "ownership": self.s.ownership.return_value})

    def test_only_exact_api36_scope_is_accepted(self):
        self.s.api = 26
        with self.assertRaises(RuntimeError): self.s.confirm_app()
        self.s.adb.assert_not_called()

    def test_confirm_app_has_new_uid_proof_and_no_install(self):
        result = self.s.confirm_app()
        self.assertEqual("PASS", result["status"])
        self.assertEqual(10216, result["appUID"])
        self.assertTrue(self.s.app_proof.is_file())
        self.assertFalse(any(c.args and c.args[0] == "install" for c in self.s.adb.call_args_list))

    def test_app_absence_stops_without_install(self):
        self.s.installed.side_effect = RuntimeError("App absent")
        with self.assertRaises(RuntimeError): self.s.confirm_app()
        self.s.adb.assert_not_called()

    def test_unsafe_stage_prevents_app_admission(self):
        self.s.resource_snapshot.return_value = {**self.snapshot, "status": "BLOCKED"}
        with self.assertRaises(RuntimeError): self.s.confirm_app()
        self.assertFalse(self.s.app_proof.exists())

    def test_app_admission_does_not_accept_prior_worker_or_stage(self):
        self.s.resource_snapshot.return_value = {**self.snapshot, "stages": {"unknown": {}}}
        with self.assertRaises(RuntimeError): self.s.confirm_app()

    def test_test_package_must_really_be_absent(self):
        self.s.adb.return_value = "package:" + self.m.v.TEST
        with self.assertRaises(RuntimeError): self.s.confirm_app()
        self.assertFalse(self.s.app_proof.exists())

    def test_installed_uid_listing_blocks_test_package_absence(self):
        self.s.adb.side_effect = [f"package:{self.m.v.TEST} uid:10217"]
        with self.assertRaises(RuntimeError): self.s.absence()
        self.s.adb.assert_called_once_with("shell", "pm", "list", "packages", "-U", "--user", "0", self.m.v.TEST)

    def test_retained_uninstalled_registration_blocks_absence(self):
        self.s.adb.side_effect = ["", f"package:{self.m.v.TEST}"]
        with self.assertRaises(RuntimeError): self.s.absence()
        self.assertEqual(2, self.s.adb.call_count)
        self.assertEqual(("shell", "pm", "list", "packages", "-u", "--user", "0", self.m.v.TEST),
                         self.s.adb.call_args.args)

    def test_install_without_app_admission_is_closed(self):
        with self.assertRaises((RuntimeError, FileNotFoundError)): self.s.install_test_once()
        self.s.adb.assert_not_called()

    def test_uid_proof_source_or_campaign_conflict_is_closed(self):
        for field, value in (("sourceCommit", "old"), ("campaignId", "other"),
                             ("resumeId", "other"), ("testSHA256", "bad")):
            self.s.app_proof.write_text(json.dumps({**self.s.binding(), "status": "PASS", field: value}))
            with self.subTest(field=field), self.assertRaises(RuntimeError): self.s.require_app()

    def prepare_install(self):
        self.write_app()
        component = f"instrumentation:{self.m.v.INSTRUMENTATION} (target={self.m.v.APP})"
        self.s.adb.side_effect = ["", "", "Success", component]

    def test_install_is_test_only_once_and_retains_app_uid(self):
        self.prepare_install()
        result = self.s.install_test_once()
        installs = [x for x in self.s.adb.call_args_list if x.args[0] == "install"]
        self.assertEqual(1, len(installs))
        self.assertEqual(("install", "test.apk"), installs[0].args)
        self.assertEqual("PASS", result["status"])
        self.assertEqual(10217, result["testUID"])

    def test_already_installed_test_refuses_reinstallation(self):
        self.prepare_install()
        self.s.adb.side_effect = ["package:" + self.m.v.TEST]
        with self.assertRaises(RuntimeError): self.s.install_test_once()
        self.assertFalse(any(x.args[0] == "install" for x in self.s.adb.call_args_list))

    def test_second_install_cannot_issue_a_second_command(self):
        self.prepare_install()
        self.s.install_test_once()
        count = len(self.s.adb.call_args_list)
        with self.assertRaises(FileExistsError): self.s.install_test_once()
        self.assertEqual(count, len(self.s.adb.call_args_list))

    def test_changed_app_uid_after_install_preserves_unknown(self):
        self.prepare_install()
        self.s.installed.side_effect = [10216, 10218, 10217]
        with self.assertRaises(RuntimeError): self.s.install_test_once()
        result = json.loads((self.s.evidence / "test-install/result.json").read_bytes())
        self.assertEqual("UNKNOWN", result["status"])
        self.assertFalse(self.s.test_proof.exists())

    def test_test_uid_must_be_independent(self):
        self.prepare_install()
        self.s.installed.side_effect = [10216, 10216, 10216]
        with self.assertRaises(RuntimeError): self.s.install_test_once()

    def test_no_explicit_install_success_keeps_original_failure(self):
        self.prepare_install()
        self.s.adb.side_effect = ["", "", "Failure"]
        with self.assertRaises(RuntimeError): self.s.install_test_once()
        result = json.loads((self.s.evidence / "test-install/result.json").read_bytes())
        self.assertEqual(1, result["installCommandsIssued"])
        self.assertEqual("UNKNOWN", result["status"])

    def test_class_load_requires_this_resume_test_admission(self):
        with patch.object(self.m.CompatCampaign, "class_load") as call:
            with self.assertRaises((RuntimeError, FileNotFoundError)): self.s.class_load()
            call.assert_not_called()

    def test_method_without_test_admission_does_not_run(self):
        with patch.object(self.m.CompatCampaign, "run_method") as call:
            with self.assertRaises((RuntimeError, FileNotFoundError)): self.s.run_method(0)
            call.assert_not_called()

    def test_operation_markers_are_exclusive_and_do_not_touch_old_files(self):
        old = self.root / "old-install-app.json"
        old.write_bytes(b"original UNKNOWN")
        self.s.marker("install-test")
        raw = (self.s.operations / "install-test.json").read_bytes()
        with self.assertRaises(FileExistsError): self.s.marker("install-test")
        self.assertEqual(raw, (self.s.operations / "install-test.json").read_bytes())
        self.assertEqual(b"original UNKNOWN", old.read_bytes())

    def test_forbidden_parent_operations_are_not_exposed(self):
        for name in ("create", "start_campaign", "admit_running", "admit_started", "ensure_apks", "update_test"):
            with self.subTest(name=name), self.assertRaises(RuntimeError): getattr(self.s, name)()
        with self.assertRaises(RuntimeError): self.s.install("app")
        with self.assertRaises(RuntimeError): self.s.marker("install-app")

    def test_anchor_replacement_is_rejected(self):
        good = {"processes": [{"role": "launcher", "pid": 33512, "created": [1792379583, 580570000]},
                              {"role": "qemu", "pid": 32252, "created": [1792379583, 752254000]}]}
        self.m.require_original_anchor(good)
        good["processes"][1]["created"][1] += 1
        with self.assertRaises(RuntimeError): self.m.require_original_anchor(good)

    def test_matching_pid_without_exact_creation_time_is_not_an_anchor(self):
        for changed in ((33513, [1792379583, 580570000]), (33512, [1792379583, 580570001])):
            rows = [{"role": "launcher", "pid": changed[0], "created": changed[1]},
                    {"role": "qemu", "pid": 32252, "created": [1792379583, 752254000]}]
            with self.subTest(changed=changed), self.assertRaises(RuntimeError):
                self.m.require_original_anchor({"processes": rows})

    def constructor_environment(self):
        original = (self.m.PROJECT / "docs/research/m10-android-test-compat/evidence" /
                    self.m.CID / "api36/control/boot-proof.json").read_bytes()
        base = self.root / "sdk-environments"
        repo = self.root / "repo"
        old = base / "campaigns" / self.m.CID
        (old / "api36").mkdir(parents=True)
        (old / "api36/boot-proof.json").write_bytes(original)
        registration = {"campaignId": self.m.CID, "sourceCommit": self.m.LEGACY_SOURCE,
                        "methods": list(self.m.campaign.natural_tests.METHODS), "automaticRetry": False}
        (old / "campaign.json").write_text(json.dumps(registration))
        self.stack.enter_context(patch.object(self.m.v, "BASE", base))
        self.stack.enter_context(patch.object(self.m.v, "ROOT", repo))
        self.stack.enter_context(patch.object(self.m.campaign, "BASE", base))
        self.stack.enter_context(patch.object(self.m.campaign, "ROOT", repo))
        self.stack.enter_context(patch.object(self.m, "PROJECT", repo))
        self.stack.enter_context(patch.object(self.m.v.Supervisor, "environment", return_value={}))
        return old

    def test_constructor_preserves_old_bytes_and_separates_all_new_paths(self):
        old = self.constructor_environment()
        boot = (old / "api36/boot-proof.json").read_bytes()
        registered = (old / "campaign.json").read_bytes()
        new = self.m.API36UidResume(RID)
        self.assertEqual(old / "api36-uid-resume" / RID, new.control)
        self.assertEqual(new.control / "operations", new.operations)
        self.assertNotEqual(old / "api36/boot-proof.json", new.boot_proof_path)
        self.assertEqual(new.evidence / "commands.jsonl", new.journal)
        self.assertEqual(boot, (old / "api36/boot-proof.json").read_bytes())
        self.assertEqual(registered, (old / "campaign.json").read_bytes())
        self.assertFalse(new.boot_proof_path.exists())

    def test_constructor_refuses_changed_original_boot(self):
        old = self.constructor_environment()
        (old / "api36/boot-proof.json").write_bytes(b"changed")
        with self.assertRaises(RuntimeError): self.m.API36UidResume(RID)

    def test_constructor_refuses_historical_source_or_campaign_conflicts(self):
        old = self.constructor_environment()
        record = json.loads((old / "campaign.json").read_bytes())
        record["sourceCommit"] = self.m.SOURCE
        (old / "campaign.json").write_text(json.dumps(record))
        with self.assertRaises(RuntimeError): self.m.API36UidResume(RID)
        with patch.object(self.m.campaign, "SOURCE", self.m.SOURCE), self.assertRaises(RuntimeError):
            self.m.API36UidResume(RID)

    def test_admit_checks_old_anchor_before_android_queries(self):
        self.s._runtime_anchor = {"original": True}
        self.s.verify_avd = Mock()
        self.s.api26_exit_gate = Mock()
        self.s.validate_process = Mock(side_effect=RuntimeError("anchor conflict"))
        self.s.runtime_paths = Mock()
        self.s.verify_android_identity = Mock()
        with self.assertRaises(RuntimeError): self.s.admit_same_instance()
        self.s.verify_android_identity.assert_not_called()
        self.s.adb.assert_not_called()
        self.assertFalse(self.s.boot_proof_path.exists())


if __name__ == "__main__": unittest.main()
