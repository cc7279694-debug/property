"""Pure host capture interpretation tests; no Android result is produced here."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

from continuous_evidence import consume_capture
from lifecycle_evidence import validate_phases
from test_lifecycle_evidence import fixture


HISTORY = Path(__file__).resolve().parents[1] / "m10-jpeg-android-runtime-resume/evidence/api26/method-01"
HEADER = "--------- beginning of main\n"


class ContinuousEvidenceTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.directory = Path(self.tmp.name)
        self.lines = fixture()[3].splitlines()
        self.observed = fixture()[2][0]["value"]

    def capture(self, text=None, stderr=b""):
        raw = (HEADER + "\n".join(self.lines) + "\n").encode() if text is None else text
        for name, data in (("stdout", raw), ("stderr", stderr)):
            (self.directory / (name + ".raw")).write_bytes(data)
        serial = "emulator-5624"
        proof = {"format": "LITHIC_SINGLE_METHOD_CONTINUOUS_LOG_V1", "status": "PASS", "serial": serial,
                 "command": [str(Path("C:/locked/adb.exe").resolve()), "-s", serial, "logcat", "-b", "main", "-v", "threadtime", "-s", "LITHIC_JPEG:I", "*:S"],
                 "started": {"utc": "2026-10-18T00:00:00+00:00", "monotonicNs": 100},
                 "ready": {"utc": "2026-10-18T00:00:01+00:00", "monotonicNs": 1000000100, "stdoutBytes": len(raw)},
                 "stopRequested": {"utc": "2026-10-18T00:00:02+00:00", "monotonicNs": 2000000100},
                 "ended": {"utc": "2026-10-18T00:00:03+00:00", "monotonicNs": 3000000100},
                 "process": {"pid": 12345, "createdByThisCapture": True, "exitCode": 1, "controlledHostObserverStop": True},
                 "limits": {"stdoutBytes": 2097152, "stderrBytes": 65536, "seconds": 360}, "errors": []}
        for name, data in (("stdout", raw), ("stderr", stderr)):
            proof[name] = {"file": name + ".raw", "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest(),
                           "observedBytes": len(data), "eof": True, "readerStopped": True}
        (self.directory / "capture.json").write_text(json.dumps(proof), encoding="utf-8")
        return proof

    def run_capture(self, proof=None, prior=""):
        proof = self.capture() if proof is None else proof
        # This is an explicitly synthetic host fixture. Persist changes so an
        # invalid field exercises the real verifier, not only caller equality.
        (self.directory / "capture.json").write_text(json.dumps(proof), encoding="utf-8")
        return consume_capture(proof, self.directory, prior, expected_serial="emulator-5624")

    def test_complete_capture_is_preserved_without_sorting(self):
        proof = self.capture()
        before = (self.directory / "stdout.raw").read_bytes()
        result = self.run_capture(proof)
        self.assertEqual("\n".join(self.lines), result["newPhaseLog"])
        self.assertEqual(before, (self.directory / "stdout.raw").read_bytes())

    def test_no_continuous_proof_cannot_be_promoted(self):
        self.capture()
        with self.assertRaises(ValueError):
            self.run_capture({})

    def test_unknown_capture_status_stays_unknown(self):
        proof = self.capture(); proof["status"] = "UNKNOWN"
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_missing_channel_evidence_is_rejected(self):
        proof = self.capture(); del proof["stderr"]
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_caller_proof_cannot_disagree_with_persisted_proof(self):
        proof = self.capture(); proof["process"]["pid"] = 23456
        with self.assertRaises(ValueError): consume_capture(proof, self.directory, "", expected_serial="emulator-5624")

    def test_capture_from_other_authorized_serial_cannot_cross_devices(self):
        proof = self.capture(); proof["serial"] = "emulator-5626"; proof["command"][2] = proof["serial"]
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_missing_real_observer_fields_cannot_be_trusted_as_pass(self):
        proof = self.capture(); del proof["process"]
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_unowned_process_cannot_be_trusted_as_pass(self):
        proof = self.capture(); proof["process"]["createdByThisCapture"] = False
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_missing_readiness_cannot_be_trusted_as_pass(self):
        proof = self.capture(); proof["ready"] = None
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_uncontrolled_exit_cannot_be_trusted_as_pass(self):
        proof = self.capture(); proof["process"]["controlledHostObserverStop"] = False
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_observed_byte_loss_cannot_be_trusted_as_pass(self):
        proof = self.capture(); proof["stdout"]["observedBytes"] += 1
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_explicit_error_cannot_be_trusted_as_pass(self):
        proof = self.capture(); proof["errors"] = ["READER_INTERRUPTED"]
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_wrong_raw_sha_is_rejected(self):
        proof = self.capture(); proof["stdout"]["sha256"] = "0" * 64
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_wrong_raw_byte_count_is_rejected(self):
        proof = self.capture(); proof["stdout"]["bytes"] += 1
        with self.assertRaises(ValueError): self.run_capture(proof)

    def test_missing_or_false_eof_is_rejected(self):
        for channel in ("stdout", "stderr"):
            proof = self.capture(); proof[channel]["eof"] = False
            with self.subTest(channel=channel), self.assertRaises(ValueError): self.run_capture(proof)

    def test_nonempty_stderr_cannot_be_hidden(self):
        with self.assertRaises(ValueError): self.run_capture(self.capture(stderr=b"logcat interrupted\n"))

    def test_invalid_utf8_is_rejected(self):
        with self.assertRaises(ValueError): self.run_capture(self.capture(HEADER.encode() + b"\xff\n"))

    def test_missing_main_header_is_rejected(self):
        with self.assertRaises(ValueError): self.run_capture(self.capture(("\n".join(self.lines) + "\n").encode()))

    def test_other_buffer_header_is_rejected(self):
        text = HEADER.replace("main", "system") + "\n".join(self.lines) + "\n"
        with self.assertRaises(ValueError): self.run_capture(self.capture(text.encode()))

    def test_repeated_header_is_rejected(self):
        with self.assertRaises(ValueError): self.run_capture(self.capture((HEADER + HEADER).encode()))

    def test_incomplete_tail_is_rejected(self):
        with self.assertRaises(ValueError): self.run_capture(self.capture((HEADER + self.lines[0]).encode()))

    def test_crlf_wire_is_allowed_without_rewriting_raw(self):
        raw = (HEADER + "\n".join(self.lines) + "\n").replace("\n", "\r\n").encode()
        result = self.run_capture(self.capture(raw))
        self.assertEqual("\n".join(self.lines), result["newPhaseLog"])
        self.assertEqual(raw, (self.directory / "stdout.raw").read_bytes())

    def test_bare_cr_is_rejected(self):
        with self.assertRaises(ValueError): self.run_capture(self.capture((HEADER + self.lines[0] + "\r").encode()))

    def test_duplicate_new_line_is_rejected(self):
        text = HEADER + "\n".join(self.lines + [self.lines[0]]) + "\n"
        with self.assertRaises(ValueError): self.run_capture(self.capture(text.encode()))

    def test_duplicate_event_with_changed_timestamp_is_rejected(self):
        repeated = self.lines[0].replace("at=100", "at=999")
        text = HEADER + "\n".join(self.lines + [repeated]) + "\n"
        with self.assertRaises(ValueError): self.run_capture(self.capture(text.encode()))

    def test_non_lithic_line_is_rejected(self):
        text = HEADER + self.lines[0].replace("LITHIC_JPEG", "OTHER") + "\n"
        with self.assertRaises(ValueError): self.run_capture(self.capture(text.encode()))

    def test_unknown_phase_is_rejected(self):
        text = HEADER + self.lines[0].replace("WORKER_RETIRE_ARMED", "WORKER_UNKNOWN") + "\n"
        with self.assertRaises(ValueError): self.run_capture(self.capture(text.encode()))

    def test_blank_or_unparsed_line_is_rejected(self):
        with self.assertRaises(ValueError): self.run_capture(self.capture((HEADER + "\n").encode()))

    def historic(self):
        expected = {"before.json": "a9f89a9d5859defdab28b5b192bb2795409bfe6e629ad0d9a488231299b05f94",
                    "after.json": "33b929372c1ed87d38f50eef5e82460d579e74e08ea5c995916d63448f5c65ae",
                    "result.json": "1ff34aa43aec6f3cf07aaf701a8ce0126b415faf5fef9d593be9f303ffa3d559"}
        loaded = {}
        for name, sha in expected.items():
            raw = (HISTORY / name).read_bytes()
            self.assertEqual(sha, hashlib.sha256(raw).hexdigest())
            loaded[name] = json.loads(raw)
        return loaded

    def test_real_rotated_history_can_be_new_capture_fixture_without_changing_old_unknown(self):
        old = self.historic()
        new_lines = [line for line in old["after.json"]["phaseLog"].splitlines() if not line.startswith("---------")]
        result = self.run_capture(self.capture((HEADER + "\n".join(new_lines) + "\n").encode()), old["before.json"]["phaseLog"])
        self.assertEqual(6, len(result["newPhaseLog"].splitlines()))
        self.assertEqual("UNKNOWN", old["result.json"]["status"])
        self.historic()

    def test_complete_old_lines_at_front_are_only_backlog(self):
        old = self.historic()
        previous = [line for line in old["before.json"]["phaseLog"].splitlines() if not line.startswith("---------")]
        raw = (HEADER + "\n".join(previous[-2:] + self.lines) + "\n").encode()
        result = self.run_capture(self.capture(raw), old["before.json"]["phaseLog"])
        self.assertEqual("\n".join(self.lines), result["newPhaseLog"])
        self.assertEqual(2, result["metadata"]["oldBacklogLines"])

    def test_old_line_after_new_line_is_rejected(self):
        old = self.historic(); prior = old["before.json"]["phaseLog"]
        previous = [line for line in prior.splitlines() if not line.startswith("---------")]
        raw = (HEADER + "\n".join(self.lines + previous[-1:]) + "\n").encode()
        with self.assertRaises(ValueError): self.run_capture(self.capture(raw), prior)

    def test_duplicate_old_backlog_is_rejected(self):
        old = self.historic(); prior = old["before.json"]["phaseLog"]
        previous = [line for line in prior.splitlines() if not line.startswith("---------")]
        raw = (HEADER + "\n".join(previous[-1:] * 2 + self.lines) + "\n").encode()
        with self.assertRaises(ValueError): self.run_capture(self.capture(raw), prior)

    def test_changed_old_event_is_not_reclassified_as_new(self):
        old = self.historic(); prior = old["before.json"]["phaseLog"]
        previous = [line for line in prior.splitlines() if not line.startswith("---------")]
        raw = (HEADER + previous[-1].replace("at=1777451", "at=1777452") + "\n").encode()
        with self.assertRaises(ValueError): self.run_capture(self.capture(raw), prior)

    def test_same_new_phase_with_wrong_pid_remains_for_identity_validator(self):
        wrong = self.lines.copy(); wrong[0] = wrong[0].replace("  202", "  303", 1)
        result = self.run_capture(self.capture((HEADER + "\n".join(wrong) + "\n").encode()))
        with self.assertRaises(ValueError): validate_phases(result["newPhaseLog"], self.observed)

    def test_line_order_is_not_sorted_to_hide_illegal_phase_times(self):
        wrong = self.lines.copy(); wrong[0] = wrong[0].replace("at=100", "at=999")
        result = self.run_capture(self.capture((HEADER + "\n".join(wrong) + "\n").encode()))
        self.assertEqual("\n".join(wrong), result["newPhaseLog"])
        with self.assertRaises(ValueError): validate_phases(result["newPhaseLog"], self.observed)


if __name__ == "__main__":
    unittest.main(verbosity=2)
