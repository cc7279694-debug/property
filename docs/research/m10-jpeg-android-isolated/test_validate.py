"""Host-only contracts for the real isolated supervisor; never runs Android tools."""
from __future__ import annotations

import ast
import base64
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch


SCRIPT = Path(__file__).with_name("validate.py")
SPEC = importlib.util.spec_from_file_location("isolated_android_validate_under_test", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
validate = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validate)


class SupervisorHostTest(unittest.TestCase):
    """Real Python decisions, synthetic paths/process responses, zero Android evidence."""

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="lithic-android-host-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.patches = [
            patch.object(validate, "ROOT", self.root / "repo"),
            patch.object(validate, "BASE", self.root / "isolated"),
            patch.object(validate, "TOOLS", self.root / "sdk"),
            patch.object(validate, "JDK", self.root / "jdk"),
        ]
        for item in self.patches:
            item.start()
            self.addCleanup(item.stop)
        self.s = validate.Supervisor.__new__(validate.Supervisor)
        self.s.api = 26
        self.s.name = "Lithic_M10V_API26_20261018"
        self.s.port = 5624
        self.s.serial = "emulator-5624"
        self.s.sdk = self.root / "research-sdk"
        self.s.home = validate.BASE / "api26"
        self.s.data = self.s.home / "data" / (self.s.name + ".avd")
        self.s.registry = self.s.home / "avds"
        self.s.evidence = self.root / "evidence"
        self.s.evidence.mkdir()
        self.s.journal = self.s.evidence / "commands.jsonl"
        with patch.dict(os.environ, {}, clear=True):
            self.s.env = self.s.environment()

    def journal(self):
        return [json.loads(line) for line in self.s.journal.read_text().splitlines()]

    def apk_fixture(self):
        directory = validate.ROOT / "build/android-validation"
        directory.mkdir(parents=True)
        rows = []
        for kind, package, data in (
            ("app", validate.APP, b"synthetic app bytes"),
            ("test", validate.TEST, b"synthetic test bytes"),
        ):
            path = directory / (kind + ".apk")
            path.write_bytes(data)
            rows.append({"kind": kind, "path": str(path), "bytes": len(data),
                         "sha256": hashlib.sha256(data).hexdigest(),
                         "certificateSHA256": validate.CERT, "packageName": package})
        report = {"status": "PASS", "applicationId": validate.APP,
                  "testApplicationId": validate.TEST, "certificateSHA256": validate.CERT,
                  "instrumentationComponent": validate.INSTRUMENTATION, "apks": rows}
        file = directory / "apk-audit.json"
        file.write_text(json.dumps(report))
        return report, file

    def test_targeted_adb_always_uses_fixed_serial(self):
        self.s.run = Mock(return_value="device")
        self.assertEqual("device", self.s.adb("get-state"))
        argv = self.s.run.call_args.args[0]
        self.assertEqual(["-s", "emulator-5624", "get-state"], argv[1:])

    def test_original_and_unknown_serials_are_rejected_before_command(self):
        self.s.run = Mock()
        for serial in ("emulator-5596", "emulator-5612", "emulator-5628", ""):
            with self.subTest(serial=serial):
                self.s.serial = serial
                with self.assertRaises(RuntimeError):
                    self.s.adb("get-state")
        self.s.run.assert_not_called()

    def test_legacy_home_is_removed_only_from_child_environment(self):
        parent = {"ANDROID_SDK_HOME": "unsafe-legacy", "ANDROID_AVD_HOME": "old-avd",
                  "ANDROID_USER_HOME": "old-user", "ANDROID_EMULATOR_HOME": "old-emulator"}
        with patch.dict(os.environ, parent, clear=True):
            child = self.s.environment()
            self.assertEqual("unsafe-legacy", os.environ["ANDROID_SDK_HOME"])
            self.assertNotIn("ANDROID_SDK_HOME", child)
            self.assertEqual(str(self.s.registry), child["ANDROID_AVD_HOME"])
            self.assertEqual(str(self.s.home / "android-user"), child["ANDROID_USER_HOME"])
            self.assertEqual(str(self.s.home / "emulator-home"), child["ANDROID_EMULATOR_HOME"])
            self.assertEqual(str(self.s.sdk), child["ANDROID_HOME"])
            self.assertEqual(child["ANDROID_HOME"], child["ANDROID_SDK_ROOT"])

    def test_adb_or_jvm_override_fails_closed(self):
        for key in ("ADB_SERVER_SOCKET", "ANDROID_ADB_SERVER_PORT", "ADB_VENDOR_KEYS",
                    "JAVA_TOOL_OPTIONS", "AVDMANAGER_OPTS"):
            with self.subTest(key=key), patch.dict(os.environ, {key: "unexpected"}, clear=True):
                with self.assertRaises(RuntimeError):
                    self.s.environment()

    def test_operation_marker_is_exclusive_and_preserves_first_attempt(self):
        self.s.home.mkdir(parents=True)
        self.s.marker("start")
        marker = self.s.home / "operations/start.json"
        first = marker.read_bytes()
        with self.assertRaises(FileExistsError):
            self.s.marker("start")
        self.assertEqual(first, marker.read_bytes())

    def test_duplicate_configuration_keys_are_rejected(self):
        file = self.root / "config.ini"
        file.write_text("AvdId=expected\nAvdId=other\n")
        with self.assertRaises(RuntimeError):
            validate.properties(file)

    def test_wrong_image_api_is_rejected_before_creation(self):
        audit_file = validate.ROOT / "build/android-validation/environment-audit.json"
        audit_file.parent.mkdir(parents=True)
        audit_file.write_text(json.dumps({
            "sourceCommit": "b756f96b699f3b70d43144f6c92815917fb563f6",
            "observationSourceCommit": "939e88b3fc79f3b9fd160737022f3bd795a07f37",
            "freshUtc": "2026-10-18T00:00:00+00:00",
            "api26": {"allOriginalFilesSizeAndShaMatch": True},
            "api36": {"generatedPackageXml": {"identityParsed": True}},
            "tools": {"primaryEmulatorAuthenticode": {"status": "Valid"}},
        }))
        self.s.sdk.mkdir()
        validate.JDK.mkdir()
        image = self.s.sdk / "system-images/android-26/google_apis/x86_64"
        image.mkdir(parents=True)
        (image / "source.properties").write_text(
            "AndroidVersion.ApiLevel=36\nSystemImage.Abi=x86_64\n"
            "SystemImage.TagId=google_apis\nPkg.Revision=16\n")
        with self.assertRaises(RuntimeError):
            self.s.audit()
        self.assertFalse(self.s.home.exists())

    def test_identity_uses_one_absolute_deadline_across_checks(self):
        now = [100.0]
        self.s.verify_avd = Mock()
        self.s.validate_process = Mock(side_effect=lambda **kw: now.__setitem__(0, now[0] + 2))
        calls = []
        def adb(*args, **kwargs):
            calls.append((args, kwargs))
            now[0] += 2
            return "device"
        self.s.adb = adb
        with patch.object(validate.time, "monotonic", side_effect=lambda: now[0]):
            with self.assertRaises(RuntimeError):
                self.s.identity(deadline=103.0)
        self.assertEqual(1, len(calls))
        self.assertEqual(("get-state",), calls[0][0])
        self.assertEqual(1, calls[0][1]["timeout"])

    def test_start_does_not_reset_boot_deadline_after_polling(self):
        self.s.home.mkdir(parents=True)
        audit = validate.ROOT / "build/android-validation/environment-audit.json"
        audit.parent.mkdir(parents=True)
        audit.write_bytes(b"{}")
        self.s.config_observation = {"configIntegrity": {"sha256": "synthetic-config"},
                                     "descriptorIntegrity": {"sha256": "synthetic-descriptor"}}
        (self.s.home / "adoption-proof.json").write_text(json.dumps({
            "status": "PASS", "api": 26, "avd": self.s.name, "data": str(self.s.data),
            "configuration": self.s.config_observation,
            "environmentAuditSHA256": hashlib.sha256(audit.read_bytes()).hexdigest()}))
        self.s.verify_avd = Mock(return_value={})
        self.s.audit = Mock(return_value=self.root / "image")
        self.s.processes = Mock(return_value={"targets": [], "freeKiB": 8 * 1024 * 1024})
        self.s.validate_process = Mock()
        self.s.identity = Mock()
        now = [100.0]
        calls = []
        def adb(*args, **kwargs):
            calls.append((args, kwargs))
            now[0] += 100
            return "offline"
        self.s.adb = adb
        child = Mock(pid=12345)
        child.poll.return_value = None
        with patch.object(validate.subprocess, "Popen", return_value=child) as popen, \
                patch.object(validate.time, "monotonic", side_effect=lambda: now[0]), \
                patch.object(validate.time, "sleep", side_effect=lambda seconds: now.__setitem__(0, now[0] + seconds)), \
                patch("socket.socket"):
            with self.assertRaises(RuntimeError):
                self.s.start()
        self.assertEqual(1, popen.call_count)
        self.assertEqual(3, len(calls))
        self.assertTrue(all(kwargs["timeout"] <= 10 for _, kwargs in calls))
        self.s.identity.assert_not_called()
        self.assertTrue((self.s.home / "operations/start.json").is_file())
        self.assertFalse((self.s.home / "boot-proof.json").exists())

    def test_nonzero_command_records_raw_result_and_stops(self):
        result = subprocess.CompletedProcess(["host-mock"], 7, b"partial", b"real error")
        with patch.object(validate.subprocess, "run", return_value=result) as run:
            with self.assertRaises(RuntimeError):
                self.s.run(["host-mock"])
        self.assertEqual(1, run.call_count)
        entry = self.journal()[0]
        self.assertEqual(7, entry["exitCode"])
        self.assertEqual(b"real error", base64.b64decode(entry["stderrBase64"]))
        self.assertEqual(hashlib.sha256(b"partial").hexdigest(), entry["stdoutSHA256"])

    def test_command_timeout_is_unknown_without_automatic_retry(self):
        error = subprocess.TimeoutExpired(["host-mock"], 2, output=b"partial", stderr=b"timeout")
        with patch.object(validate.subprocess, "run", side_effect=error) as run:
            with self.assertRaises(RuntimeError):
                self.s.run(["host-mock"], timeout=2)
        self.assertEqual(1, run.call_count)
        entry = self.journal()[0]
        self.assertEqual("COMMAND_TIMEOUT_UNKNOWN", entry["status"])
        self.assertEqual(b"partial", base64.b64decode(entry["stdoutBase64"]))

    def test_apk_certificate_conflict_is_rejected(self):
        report, file = self.apk_fixture()
        report["certificateSHA256"] = "wrong certificate"
        file.write_text(json.dumps(report))
        with self.assertRaises(RuntimeError):
            self.s.apk_audit()

    def test_apk_hash_conflict_is_rejected(self):
        report, _ = self.apk_fixture()
        Path(report["apks"][0]["path"]).write_bytes(b"x" * report["apks"][0]["bytes"])
        with self.assertRaises(RuntimeError):
            self.s.apk_audit()

    def test_matching_synthetic_apk_audit_can_pass_without_android(self):
        report, _ = self.apk_fixture()
        with patch.object(validate, "APP_SHA", report["apks"][0]["sha256"]), \
                patch.object(validate, "TEST_SHA", report["apks"][1]["sha256"]):
            self.assertEqual(report, self.s.apk_audit())

    def test_apk_instrumentation_conflict_is_rejected(self):
        report, file = self.apk_fixture()
        report["instrumentationComponent"] = validate.TEST + "/OtherRunner"
        file.write_text(json.dumps(report))
        with self.assertRaises(RuntimeError):
            self.s.apk_audit()

    def test_installed_uid_conflict_is_rejected(self):
        self.s.adb = Mock(side_effect=["package:/data/app/pkg/base.apk",
                                      validate.APP_SHA + "  /data/app/pkg/base.apk",
                                      "userId=10100\nuserId=10101\n"])
        with self.assertRaises(RuntimeError):
            self.s.installed(validate.APP, validate.APP_SHA)

    def test_process_data_path_prefix_does_not_prove_ownership(self):
        self.s.processes = Mock(return_value={"targets": [{
            "ProcessId": 12345,
            "ExecutablePath": str(validate.TOOLS / "emulator/emulator.exe"),
            "CommandLine": ('emulator.exe -avd ' + self.s.name + ' -port 5624 -datadir "' +
                            str(self.s.data) + '.other"')}]})
        with self.assertRaises(RuntimeError):
            self.s.validate_process()

    def test_missing_target_process_command_line_fails_closed(self):
        self.s.processes = Mock(return_value={"targets": [{
            "ProcessId": 12345,
            "ExecutablePath": str(validate.TOOLS / "emulator/emulator.exe"),
            "CommandLine": None}]})
        with self.assertRaises(RuntimeError):
            self.s.validate_process()

    def test_exact_normal_command_line_can_prove_target_process(self):
        executable = self.s.sdk / "emulator/emulator.exe"
        target = {"ProcessId": 12345, "ExecutablePath": str(executable),
                  "CommandLine": subprocess.list2cmdline([
                      str(executable), "-avd", self.s.name, "-port", str(self.s.port),
                      "-datadir", str(self.s.data), "-data", str(self.s.data / "userdata-qemu.img"),
                      "-cache", str(self.s.data / "cache.img")])}
        found = {"targets": [target]}
        self.s.processes = Mock(return_value=found)
        self.assertIs(found, self.s.validate_process())

    def test_existing_reparse_ancestor_blocks_nonexistent_new_root(self):
        ancestor = self.root / "synthetic-reparse-ancestor"
        ancestor.mkdir()
        target = ancestor / "new-root" / "api26"
        real_lstat = Path.lstat
        def reported_lstat(path, *args, **kwargs):
            actual = real_lstat(path, *args, **kwargs)
            if path == ancestor:
                return SimpleNamespace(st_mode=actual.st_mode, st_file_attributes=0x400)
            return actual
        with patch.object(Path, "lstat", autospec=True, side_effect=reported_lstat):
            with self.assertRaises(RuntimeError):
                validate.unlinked_ancestors(target)
        self.assertFalse(target.exists())

    def test_create_issues_one_non_forced_command_and_keeps_marker(self):
        # Same one-shot create contract, now using the only newly authorized API36
        # identity. API26 is adopted and must not be created a second time.
        self.s.api = 36
        self.s.name = "Lithic_M10V_API36_20261018"
        self.s.serial = "emulator-5626"
        self.s.home = validate.BASE / "api36"
        self.s.data = self.s.home / "data" / (self.s.name + ".avd")
        self.s.registry = self.s.home / "avds"
        self.s.api26_exit_gate = Mock()
        self.s.config_observation = {"configIntegrity": {}, "descriptorIntegrity": {}}
        audit = validate.ROOT / "build/android-validation/environment-audit.json"
        audit.parent.mkdir(parents=True)
        audit.write_bytes(b"{}")
        self.s.audit = Mock(return_value=self.root / "image")
        self.s.processes = Mock(return_value={"targets": []})
        self.s.verify_avd = Mock(return_value={})
        manager = self.s.sdk / "cmdline-tools/latest/bin/avdmanager.bat"
        manager.parent.mkdir(parents=True)
        manager.write_text("host fixture, never execute")
        self.s.run = Mock(return_value="created")
        self.s.create()
        self.assertEqual(1, self.s.run.call_count)
        argv = [str(item) for item in self.s.run.call_args.args[0]]
        self.assertEqual(["create", "avd"], argv[1:3])
        self.assertNotIn("--force", argv)
        self.assertNotIn("-f", argv)
        self.assertEqual(str(self.s.data), argv[-1])
        with self.assertRaises(RuntimeError):
            self.s.create()
        self.assertEqual(1, self.s.run.call_count)
        self.assertTrue((self.s.home / "operations/create.json").is_file())

    def test_source_has_no_global_adb_or_destructive_command_tokens(self):
        tree = ast.parse(SCRIPT.read_text(encoding="utf-8"))
        forbidden = {"kill", "kill-server", "force-stop", "clear", "uninstall",
                     "-wipe-data", "--force", "-f", "-snapshot", "devices"}
        tokens = []
        for node in ast.walk(tree):
            if isinstance(node, ast.Call):
                tokens.extend(item.value for item in node.args
                              if isinstance(item, ast.Constant) and isinstance(item.value, str))
        self.assertFalse(set(tokens) & forbidden)
        adb_source = ast.get_source_segment(SCRIPT.read_text(encoding="utf-8"),
            next(node for node in ast.walk(tree) if isinstance(node, ast.FunctionDef) and node.name == "adb"))
        self.assertIn('"-s", self.serial', adb_source)


if __name__ == "__main__":
    unittest.main(verbosity=2)
