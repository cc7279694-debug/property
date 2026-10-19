"""Pure fixed-input/path/one-attempt checks; no Android process is invoked."""
import copy
import importlib.util
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch

SPEC = importlib.util.spec_from_file_location("fresh_stage_inputs", Path(__file__).with_name("stage_inputs.py"))
s = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(s)


class InputTransferTest(unittest.TestCase):
    def test_only_three_locked_fixture_records_are_selected(self):
        audit = s.locked_json(s.ARTIFACTS, s.ARTIFACTS_SHA)
        rows = s.selected_inputs(audit)
        self.assertEqual(s.NAMES, tuple(row["basename"] for row in rows))
        for mutate in (lambda x: x["fixtures"].reverse(),
                       lambda x: x["fixtures"][0].update(sha256="0" * 64),
                       lambda x: x["fixtures"].append(copy.deepcopy(x["fixtures"][0]))):
            value = copy.deepcopy(audit)
            mutate(value)
            with self.assertRaises(RuntimeError): s.selected_inputs(value)

    def test_remote_paths_cannot_escape_fixed_fresh_private_directory(self):
        for name in s.NAMES:
            self.assertEqual(s.DESTINATION + "/" + name, s.private_path(name))
        for name in ("../elsewhere.jpg", "/absolute.jpg", "unknown.jpg", "a;echo bad.jpg"):
            with self.assertRaises(RuntimeError): s.private_path(name)

    def test_single_exclusive_claim_is_never_reset_after_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            supervisor = SimpleNamespace(evidence=Path(directory), binding=lambda: {"serial": "emulator-5664"},
                                         scope=Mock())
            claim = s.claim_transfer(supervisor)
            before = (claim / "attempt.json").read_bytes()
            with self.assertRaises(FileExistsError): s.claim_transfer(supervisor)
            self.assertEqual(before, (claim / "attempt.json").read_bytes())
            supervisor.scope.assert_called()

    def test_test_apk_proof_or_uid_conflict_stops_before_any_transfer(self):
        for change, test_uid in (({"sha256": "0" * 64}, 10001), ({"uid": 10002}, 10001),
                                 ({"kind": "app"}, 10001), ({"uid": 10000}, 10000)):
            proof = {"kind": "test", "package": s.TEST, "sha256": s.TEST_SHA, "uid": test_uid}
            proof.update(change)
            fake = SimpleNamespace(home=Path("synthetic-home"), identity=Mock(), apk_audit=Mock(),
                                   installed=Mock(side_effect=[10000, test_uid]),
                                   require_proof=Mock(return_value=proof), adb=Mock(), run=Mock())
            with patch.object(s, "FreshSupervisor", return_value=fake):
                with self.assertRaisesRegex(RuntimeError, "Test APK/User 0 UID"):
                    s.transfer(26)
            fake.adb.assert_not_called()
            fake.run.assert_not_called()


if __name__ == "__main__": unittest.main()
