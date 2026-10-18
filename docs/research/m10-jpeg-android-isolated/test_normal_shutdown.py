"""Host decisions only; no emulator/ADB operations."""
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch

import normal_shutdown as subject
from natural_tests import APP_SHA, TEST_SHA, METHODS


class NormalShutdownTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.s = SimpleNamespace(api=26, serial="emulator-5624", port=5624,
                                 evidence=self.root, home=self.root,
                                 identity=Mock(), installed=Mock(), marker=Mock(),
                                 adb=Mock(return_value="OK"), validate_process=Mock(return_value={"targets": [{"ProcessId": 700}]}),
                                 processes=Mock(return_value={"targets": []}))
        self.adapter = SimpleNamespace(before=Mock(return_value={"status": "PASS"}))
        for index, method in enumerate(METHODS):
            folder = self.root / f"method-{index:02d}"
            folder.mkdir()
            (folder / "result.json").write_text(json.dumps({"status": "PASS", "method": method,
                "serial": self.s.serial, "api": 26, "appSHA256": APP_SHA, "testSHA256": TEST_SHA}))

    def test_missing_gate_never_exits(self):
        (self.root / "method-08/result.json").unlink()
        with self.assertRaises(FileNotFoundError):
            subject.normal_exit(self.s, self.adapter)
        self.s.adb.assert_not_called()

    def test_unknown_gate_never_exits(self):
        path = self.root / "method-01/result.json"
        row = json.loads(path.read_text()); row["status"] = "UNKNOWN"
        path.write_text(json.dumps(row))
        with self.assertRaises(RuntimeError):
            subject.normal_exit(self.s, self.adapter)
        self.s.adb.assert_not_called()

    def test_other_or_old_device_never_exits(self):
        for api, serial in ((36, "emulator-5626"), (26, "emulator-5612"), (36, "emulator-5596")):
            self.s.api, self.s.serial = api, serial
            with self.assertRaises(RuntimeError):
                subject.normal_exit(self.s, self.adapter)
        self.s.adb.assert_not_called()

    def test_unknown_consumer_never_exits(self):
        self.adapter.before.return_value = {"status": "BLOCKED"}
        with self.assertRaises(RuntimeError):
            subject.normal_exit(self.s, self.adapter)
        self.s.adb.assert_not_called()

    def test_once_and_os_proof_required(self):
        with patch.object(subject.socket, "socket") as socket:
            socket.return_value.__enter__.return_value.connect_ex.return_value = 1
            proof = subject.normal_exit(self.s, self.adapter)
        self.assertEqual("PASS", proof["status"])
        self.s.adb.assert_called_once_with("emu", "kill", timeout=15)
        self.s.marker.assert_called_once_with("normal-exit")
        self.assertTrue((self.root / "normal-exit-proof.json").is_file())

    def test_ack_unknown_does_not_retry(self):
        self.s.adb.return_value = "connection closed"
        with self.assertRaises(RuntimeError):
            subject.normal_exit(self.s, self.adapter)
        self.s.adb.assert_called_once()
        self.assertFalse((self.root / "normal-exit-proof.json").exists())


if __name__ == "__main__":
    unittest.main()
