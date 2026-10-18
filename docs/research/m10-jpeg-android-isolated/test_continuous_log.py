"""Host-only observer tests. No test launches adb or an Android application.

Real Python child processes exercise byte pipes, EOF and bounded observer stop.
Historical log strings are replay fixtures, never new Android evidence.
"""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
try:
    from continuous_log import ContinuousLogCapture, read_verified
except ModuleNotFoundError:
    ContinuousLogCapture = None
    read_verified = None


HEADER = b"--------- beginning of main\r\n"
LINE = (b"10-18 13:50:49.261  7483  7495 I LITHIC_JPEG: "
        b"WORKER_RETIRE_ARMED attempt=878d3544-05fb-462a-9f19-b08ace2e7041 "
        b"session=fd71b830-e3d5-4fbe-ab66-f9d34131eb9c "
        b"worker=7483/10077/204264 at=2042874\r\n")
REPO = Path(__file__).resolve().parents[3]
HISTORY = REPO / "docs/research/m10-jpeg-android-runtime-resume/evidence/api26"


class ContinuousLogTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.children = []
        self.calls = []
        self.capture = None
        self.addCleanup(self.release_children)

    def release_children(self):
        if self.capture is not None:
            self.capture.finish()
        for child in self.children:
            if child.poll() is None:
                # These are synthetic, self-created HOST Python processes only.
                child.terminate()
                child.wait(timeout=3)

    def child_factory(self, source):
        def create(argv, **kwargs):
            self.calls.append((argv, kwargs))
            child = subprocess.Popen([sys.executable, "-u", "-c", source], **kwargs)
            self.children.append(child)
            return child
        return create

    def make(self, stdout=HEADER + LINE, stderr=b"", tail="time.sleep(30)", **options):
        self.assertIsNotNone(ContinuousLogCapture, "Continuous capture implementation is missing")
        source = ("import os,time\n"
                  f"os.write(1, {stdout!r})\n"
                  f"os.write(2, {stderr!r})\n" + tail)
        self.capture = ContinuousLogCapture(
            Path(self.temp.name) / "locked-adb.exe", "emulator-5624",
            Path(self.temp.name) / "capture", popen_factory=self.child_factory(source),
            readiness_timeout=options.pop("readiness_timeout", 1),
            max_seconds=options.pop("max_seconds", 5), **options)
        return self.capture

    def done(self, capture, want="PASS"):
        proof = capture.finish()
        self.assertEqual(want, proof["status"], proof)
        return proof

    def test_live_header_and_complete_pipe_bytes_are_preserved(self):
        capture = self.make()
        ready = capture.start()
        self.assertEqual("PASS", ready["status"], ready)
        self.assertEqual("PASS", capture.assert_live()["status"])
        proof = self.done(capture)
        raw = (capture.directory / "stdout.raw").read_bytes()
        self.assertEqual(HEADER + LINE, raw)
        self.assertEqual(len(raw), proof["stdout"]["bytes"])
        self.assertEqual(hashlib.sha256(raw).hexdigest(), proof["stdout"]["sha256"])
        self.assertTrue(proof["stdout"]["eof"])
        self.assertTrue(proof["stderr"]["eof"])
        self.assertTrue(proof["process"]["controlledHostObserverStop"])
        self.assertIsNotNone(proof["process"]["exitCode"])

    def test_command_is_serial_scoped_read_only_main_buffer(self):
        capture = self.make()
        self.assertEqual("PASS", capture.start()["status"])
        self.done(capture)
        self.assertEqual([
            str(capture.adb_path), "-s", "emulator-5624", "logcat", "-b", "main",
            "-v", "threadtime", "-s", "LITHIC_JPEG:I", "*:S"], self.calls[0][0])
        self.assertFalse(self.calls[0][1]["shell"])
        self.assertEqual(subprocess.DEVNULL, self.calls[0][1]["stdin"])

    def test_live_process_without_actual_header_never_becomes_ready(self):
        capture = self.make(stdout=b"", readiness_timeout=.15)
        self.assertEqual("UNKNOWN", capture.start()["status"])
        proof = self.done(capture, "UNKNOWN")
        self.assertIn("READINESS_NOT_PROVEN", proof["errors"])
        self.assertIsNone(proof["ready"])

    def test_event_without_header_does_not_prove_reader_ready(self):
        capture = self.make(stdout=LINE, readiness_timeout=.15)
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.done(capture, "UNKNOWN")

    def test_old_capture_directory_is_not_overwritten_or_started(self):
        capture = self.make()
        capture.directory.mkdir()
        (capture.directory / "preserve.raw").write_bytes(b"old evidence")
        with self.assertRaises(FileExistsError):
            capture.start()
        self.assertEqual([], self.calls)
        self.assertEqual(b"old evidence", (capture.directory / "preserve.raw").read_bytes())
        self.capture = None

    def test_same_capture_cannot_launch_a_second_observer(self):
        capture = self.make()
        self.assertEqual("PASS", capture.start()["status"])
        with self.assertRaises(RuntimeError):
            capture.start()
        self.done(capture)
        self.assertEqual(1, len(self.children))

    def test_finish_is_idempotent_and_does_not_rewrite_proof(self):
        capture = self.make()
        capture.start()
        first = self.done(capture)
        proof_bytes = (capture.directory / "capture.json").read_bytes()
        second = self.done(capture)
        self.assertEqual(first, second)
        self.assertEqual(proof_bytes, (capture.directory / "capture.json").read_bytes())

    def test_early_clean_exit_is_unknown_not_a_normal_capture_end(self):
        capture = self.make(tail="time.sleep(.12)")
        self.assertEqual("PASS", capture.start()["status"])
        self.children[0].wait(timeout=3)
        self.assertEqual("UNKNOWN", capture.assert_live()["status"])
        self.done(capture, "UNKNOWN")

    def test_early_nonzero_exit_is_preserved(self):
        capture = self.make(tail="time.sleep(.12); raise SystemExit(17)")
        capture.start()
        self.children[0].wait(timeout=3)
        proof = self.done(capture, "UNKNOWN")
        self.assertEqual(17, proof["process"]["exitCode"])
        self.assertFalse(proof["process"]["controlledHostObserverStop"])

    def test_stdout_overflow_retains_bounded_prefix_and_marks_unknown(self):
        capture = self.make(stdout=HEADER, max_stdout_bytes=256,
                            tail="time.sleep(.05); os.write(1,b'x'*1024); time.sleep(30)")
        capture.start()
        time.sleep(.15)
        proof = self.done(capture, "UNKNOWN")
        self.assertIn("STDOUT_LIMIT_EXCEEDED", proof["errors"])
        self.assertLessEqual(proof["stdout"]["bytes"], 256)
        self.assertGreater(proof["stdout"]["observedBytes"], 256)

    def test_stderr_overflow_is_not_hidden_by_complete_stdout(self):
        capture = self.make(max_stderr_bytes=64,
                            tail="time.sleep(.05); os.write(2,b'e'*256); time.sleep(30)")
        capture.start()
        time.sleep(.15)
        proof = self.done(capture, "UNKNOWN")
        self.assertIn("STDERR_LIMIT_EXCEEDED", proof["errors"])
        self.assertLessEqual(proof["stderr"]["bytes"], 64)

    def test_observable_drop_markers_are_unknown(self):
        for marker in (b"logcat: Unexpected EOF!\n", b"dropped 8 log messages\n",
                       b"buffer overflow\n", b"chatty: uid=10077 expire 3 lines\n"):
            with self.subTest(marker=marker):
                self.release_children()
                if self.capture is not None:
                    self.capture = None
                self.temp.cleanup()
                self.temp = tempfile.TemporaryDirectory()
                capture = self.make(stdout=HEADER + marker)
                capture.start()
                self.done(capture, "UNKNOWN")

    def test_second_main_header_is_not_silently_treated_as_a_reconnect(self):
        capture = self.make(stdout=HEADER + LINE + HEADER)
        capture.start()
        self.done(capture, "UNKNOWN")

    def test_invalid_utf8_is_not_replacement_decoded_into_success(self):
        capture = self.make(stdout=HEADER + b"\xff\n")
        capture.start()
        self.done(capture, "UNKNOWN")

    def test_partial_final_line_is_truncated_unknown(self):
        capture = self.make(stdout=HEADER + LINE.rstrip(b"\r\n"))
        capture.start()
        proof = self.done(capture, "UNKNOWN")
        self.assertIn("STDOUT_PARTIAL_FINAL_LINE", proof["errors"])

    def test_stderr_diagnostics_cannot_be_ignored(self):
        capture = self.make(stderr=b"adb: device offline\n")
        capture.start()
        self.done(capture, "UNKNOWN")

    def test_deadline_independently_bounds_a_blocked_observer(self):
        capture = self.make(max_seconds=.25, readiness_timeout=.1)
        self.assertEqual("PASS", capture.start()["status"])
        time.sleep(.35)
        proof = self.done(capture, "UNKNOWN")
        self.assertIn("CAPTURE_DEADLINE_EXCEEDED", proof["errors"])
        self.assertIsNotNone(self.children[0].poll())

    def test_capture_raw_bytes_can_be_independently_reverified(self):
        capture = self.make()
        capture.start()
        proof = self.done(capture)
        verified, text = read_verified(capture.directory, expected_serial="emulator-5624")
        self.assertEqual(proof, verified)
        self.assertEqual((HEADER + LINE).decode(), text)

    def test_post_capture_byte_tamper_is_rejected(self):
        capture = self.make()
        capture.start()
        self.done(capture)
        with (capture.directory / "stdout.raw").open("ab") as out:
            out.write(b"altered\n")
        with self.assertRaises(ValueError):
            read_verified(capture.directory)

    def test_unknown_capture_cannot_be_relabelled_by_reader(self):
        capture = self.make(stdout=HEADER + b"tail")
        capture.start()
        self.done(capture, "UNKNOWN")
        with self.assertRaises(ValueError):
            read_verified(capture.directory)

    def test_expected_serial_mismatch_is_rejected(self):
        capture = self.make()
        capture.start()
        self.done(capture)
        with self.assertRaises(ValueError):
            read_verified(capture.directory, expected_serial="emulator-5626")

    def edit_proof(self, capture, mutate):
        path = capture.directory / "capture.json"
        value = json.loads(path.read_bytes())
        mutate(value)
        path.write_bytes(json.dumps(value).encode())

    def test_status_pass_cannot_override_unclosed_reader_proof(self):
        capture = self.make()
        capture.start()
        self.done(capture)
        self.edit_proof(capture, lambda proof: proof["stdout"].update(eof=False))
        with self.assertRaises(ValueError):
            read_verified(capture.directory)

    def test_ready_byte_boundary_must_contain_the_actual_full_header(self):
        capture = self.make()
        capture.start()
        self.done(capture)
        # A CRLF header is not complete at the shorter LF-header byte count.
        self.edit_proof(capture, lambda proof: proof["ready"].update(stdoutBytes=len(HEADER)-1))
        with self.assertRaises(ValueError):
            read_verified(capture.directory)

    def test_wrong_command_is_rejected_even_when_status_claims_pass(self):
        capture = self.make()
        capture.start()
        self.done(capture)
        self.edit_proof(capture, lambda proof: proof["command"].append("-c"))
        with self.assertRaises(ValueError):
            read_verified(capture.directory)

    def test_reverse_capture_time_order_is_rejected(self):
        capture = self.make()
        capture.start()
        self.done(capture)
        self.edit_proof(capture, lambda proof: proof["ready"].update(
            monotonicNs=proof["started"]["monotonicNs"]-1))
        with self.assertRaises(ValueError):
            read_verified(capture.directory)

    def test_nonobject_evidence_fields_fail_closed_with_validation_error(self):
        capture = self.make()
        capture.start()
        proof = self.done(capture)
        path = capture.directory / "capture.json"
        for field in ("limits", "process", "stdout", "stderr"):
            with self.subTest(field=field):
                malformed = dict(proof, **{field: []})
                path.write_bytes(json.dumps(malformed).encode())
                with self.assertRaises(ValueError):
                    read_verified(capture.directory)

    def test_reader_exception_preserves_incomplete_eof_and_original_error(self):
        self.assertIsNotNone(ContinuousLogCapture)
        source = "import os,time; os.write(1," + repr(HEADER) + "); time.sleep(30)"
        underlying_factory = self.child_factory(source)
        class FailingPipe:
            def __init__(self, pipe):
                self.pipe = pipe
                self.once = False
            def read(self, count):
                if self.once:
                    raise OSError("synthetic pipe interruption")
                self.once = True
                return self.pipe.read(count)
            def close(self):
                self.pipe.close()
        def factory(argv, **kwargs):
            child = underlying_factory(argv, **kwargs)
            child.stdout = FailingPipe(child.stdout)
            return child
        self.capture = ContinuousLogCapture(Path(self.temp.name) / "adb.exe", "emulator-5624",
            Path(self.temp.name) / "capture", popen_factory=factory, readiness_timeout=1)
        self.capture.start()
        proof = self.done(self.capture, "UNKNOWN")
        self.assertFalse(proof["stdout"]["eof"])
        self.assertTrue(any("synthetic pipe interruption" in value for value in proof["errors"]))

    def test_historical_devices_are_not_capture_targets(self):
        self.assertIsNotNone(ContinuousLogCapture)
        for serial in ("emulator-5596", "emulator-5612", "", "emulator-5624 extra"):
            with self.subTest(serial=serial), self.assertRaises(ValueError):
                ContinuousLogCapture("adb.exe", serial, Path(self.temp.name) / "unused")

    def test_launch_error_is_retained_without_another_attempt(self):
        self.assertIsNotNone(ContinuousLogCapture)
        def missing(*args, **kwargs):
            raise OSError("synthetic host observer launch denied")
        self.capture = ContinuousLogCapture(Path(self.temp.name) / "missing-adb.exe",
            "emulator-5624", Path(self.temp.name) / "capture", popen_factory=missing)
        self.assertEqual("UNKNOWN", self.capture.start()["status"])
        proof = self.done(self.capture, "UNKNOWN")
        self.assertIsNone(proof["process"]["pid"])
        self.assertTrue(any("launch denied" in value for value in proof["errors"]))

    def history(self):
        before = json.loads((HISTORY / "method-01/before.json").read_bytes())
        after = json.loads((HISTORY / "method-01/after.json").read_bytes())
        previous = [line for line in before["phaseLog"].splitlines()
                    if line and not line.startswith("--------- beginning of ")]
        current = [line for line in after["phaseLog"].splitlines()
                   if line and not line.startswith("--------- beginning of ")]
        return previous, current

    def test_real_historical_rotation_is_replayed_without_old_prefix_dependency(self):
        previous, current = self.history()
        self.assertNotEqual(previous, current[:len(previous)])
        replay = HEADER + ("\r\n".join(previous + current) + "\r\n").encode()
        capture = self.make(stdout=replay)
        capture.start()
        self.done(capture)
        _, text = read_verified(capture.directory)
        self.assertEqual(replay, text.encode())
        self.assertEqual(12, len([line for line in text.splitlines() if "LITHIC_JPEG:" in line]))

    def validate_history_phases(self, lines):
        from lifecycle_evidence import validate_phases
        return validate_phases("\n".join(lines), {
            "attempt": "878d3544-05fb-462a-9f19-b08ace2e7041",
            "session": "fd71b830-e3d5-4fbe-ab66-f9d34131eb9c",
            "workerPid": 7483, "workerUid": 10077,
            "workerStartTicks": 204264, "hostPid": 7457})

    def test_real_history_duplicate_event_does_not_pass_phase_validator(self):
        _, lines = self.history()
        with self.assertRaises(ValueError):
            self.validate_history_phases(lines + lines[:1])

    def test_real_history_wrong_emitter_does_not_pass_phase_validator(self):
        _, lines = self.history()
        lines[0] = lines[0].replace("  7483  7495 I", "  7457  7495 I")
        with self.assertRaises(ValueError):
            self.validate_history_phases(lines)

    def test_real_history_missing_event_does_not_pass_phase_validator(self):
        _, lines = self.history()
        with self.assertRaises(ValueError):
            self.validate_history_phases(lines[:-1])

    def test_real_history_impossible_monotonic_order_does_not_pass(self):
        _, lines = self.history()
        lines[1] = lines[1].replace("at=2042876", "at=2042800")
        with self.assertRaises(ValueError):
            self.validate_history_phases(lines)

    def test_replaying_historical_data_does_not_change_original_unknown(self):
        original = (HISTORY / "method-01/result.json").read_bytes()
        self.assertEqual("UNKNOWN", json.loads(original)["status"])
        _, lines = self.history()
        self.validate_history_phases(lines)
        self.assertEqual(original, (HISTORY / "method-01/result.json").read_bytes())


if __name__ == "__main__":
    unittest.main(verbosity=2)
