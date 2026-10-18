"""New authorization contracts; synthetic host data, never real Android commands."""
import json
import base64
import hashlib
from pathlib import Path
import unittest
from unittest.mock import Mock, patch

import test_validate as baseline
validate = baseline.validate


class RuntimeResumeTest(unittest.TestCase):
    def setUp(self):
        baseline.SupervisorHostTest.setUp(self)
        self.s.home.mkdir(parents=True)
        self.s.verify_avd = Mock(return_value={})
        self.s.validate_process = Mock(return_value={"targets": [{"ProcessId": 32880}, {"ProcessId": 32032}]})
        self.s.runtime_paths = Mock(return_value={"status": "PASS", "paths": {"synthetic": "not Android evidence"}})
        self.s.apk_audit = Mock(return_value={})
        self.s.adb = Mock(side_effect=["device", "1", "26", "x86_64", self.s.name + "\nOK"])
        self.s.process_proof = {"status": "PASS", "anchor": {"synthetic": True}}
        self.s.resume_anchor = Mock(return_value={"synthetic": True})

    def test_resume_writes_separate_proof_preserving_original_start_failure(self):
        original = self.s.home / "operations/start.json"
        original.parent.mkdir()
        original.write_bytes(b"ORIGINAL_START_MARKER")
        self.s.resume()
        proof = json.loads((self.s.home / "resumed-steady-state-proof.json").read_text())
        self.assertEqual("RESUMED_STEADY_STATE_PROOF", proof["type"])
        self.assertEqual("NOT_PROVEN", proof["first300SecondStartup"])
        self.assertEqual("1", proof["observedBootCompleted"])
        self.assertEqual(b"ORIGINAL_START_MARKER", original.read_bytes())
        self.assertFalse((self.s.home / "boot-proof.json").exists())
        self.assertEqual(5, self.s.adb.call_count)

    def test_identity_unknown_never_sends_adb(self):
        self.s.validate_process.side_effect = RuntimeError("UNKNOWN original process")
        with self.assertRaises(RuntimeError):
            self.s.resume()
        self.s.adb.assert_not_called()
        self.assertFalse((self.s.home / "resumed-steady-state-proof.json").exists())

    def test_runtime_path_failure_never_sends_adb(self):
        self.s.runtime_paths.side_effect = RuntimeError("Unknown runtime path")
        with self.assertRaises(RuntimeError):
            self.s.resume()
        self.s.adb.assert_not_called()

    def test_api26_start_is_prohibited_before_any_start_side_effect(self):
        with self.assertRaisesRegex(RuntimeError, "API26_START_NOT_AUTHORIZED"):
            self.s.start()
        self.s.verify_avd.assert_not_called()
        self.s.adb.assert_not_called()

    def test_normal_identity_requires_current_resume_proof_before_adb(self):
        with self.assertRaises((RuntimeError, OSError)):
            self.s.identity()
        self.s.adb.assert_not_called()

    def test_boot_observation_deadline_does_not_become_new_startup_proof(self):
        self.s.adb = Mock(side_effect=["device", ""])
        with patch.object(validate.time, "monotonic", side_effect=[0, 0, 0, 31]), patch.object(validate.time, "sleep"):
            with self.assertRaises(RuntimeError):
                self.s.resume()
        self.assertFalse((self.s.home / "resumed-steady-state-proof.json").exists())
        self.assertFalse((self.s.home / "boot-proof.json").exists())
        self.assertEqual(2, self.s.adb.call_count)

    def test_real_console_double_cr_is_not_a_conflicting_avd_name(self):
        raw = (Path(__file__).resolve().parents[3] / "docs/research/m10-jpeg-android-runtime-resume/evidence/console-avd-name.stdout").read_bytes()
        (self.s.home / "resumed-steady-state-proof.json").write_text(json.dumps({
            "type": "RESUMED_STEADY_STATE_PROOF", "status": "PASS", "serial": self.s.serial,
            "sourceCommit": "3474f639c2a831d26e74eeeff5ec8db0f6ea7a3f", "first300SecondStartup": "NOT_PROVEN"}))
        self.s.adb = Mock(side_effect=["device", "1", "26", "x86_64", raw.decode().strip()])
        self.s.identity()
        self.assertEqual(5, self.s.adb.call_count)

    def test_console_conflicting_payload_and_lone_cr_still_rejected(self):
        for value in ("Other\nOK", self.s.name + "\nOther\nOK", self.s.name + "\rOK", self.s.name + "\nKO"):
            with self.assertRaises(RuntimeError):
                validate.verify_console_name(value, self.s.name)

    def query_fixture(self):
        self.s.marker("runtime-resume")
        calls = [("get-state",), ("shell", "getprop", "sys.boot_completed"),
            ("shell", "getprop", "ro.build.version.sdk"), ("shell", "getprop", "ro.product.cpu.abi"),
            ("emu", "avd", "name")]
        values = [b"device\r\n", b"1\r\n", b"26\r\n", b"x86_64\r\n", (self.s.name+"\r\r\nOK\r\r\n").encode()]
        rows = [{"argv": [str(validate.TOOLS / "platform-tools/adb.exe"), "-s", self.s.serial, *call],
            "exitCode": 0, "stdoutBase64": base64.b64encode(value).decode(), "stdoutBytes": len(value),
            "stdoutSHA256": hashlib.sha256(value).hexdigest(), "stderrBase64": "", "stderrBytes": 0,
            "endUtc": "2026-10-18T13:45:00Z"} for call, value in zip(calls, values)]
        self.s.journal.write_text("".join(json.dumps(row)+"\n" for row in rows))
        return rows

    def test_reconciliation_consumes_existing_queries_without_reissuing_adb(self):
        self.query_fixture()
        self.s.reconcile_resume()
        self.s.adb.assert_not_called()
        proof = json.loads((self.s.home / "resumed-steady-state-proof.json").read_text())
        self.assertEqual("NOT_PROVEN", proof["first300SecondStartup"])
        self.assertEqual("EXISTING_FIVE_QUERIES_REPARSED_NO_REISSUE", proof["queryEvidence"])
        self.assertFalse((self.s.home / "boot-proof.json").exists())

    def test_reconciliation_rejects_wrong_serial_before_new_observation(self):
        rows = self.query_fixture(); rows[0]["argv"][2] = "emulator-5596"
        self.s.journal.write_text("".join(json.dumps(row)+"\n" for row in rows))
        with self.assertRaises(RuntimeError):
            self.s.reconcile_resume()
        self.s.adb.assert_not_called(); self.s.validate_process.assert_not_called()


if __name__ == "__main__":
    unittest.main(verbosity=2)
