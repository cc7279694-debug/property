"""Targeted HOST-only nonce-handshake tests; no adb/device operation."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
from continuous_log import ContinuousLogCapture, read_verified

NONCE = "ab" * 16
OTHER = "cd" * 16
TAG = "LITHIC_M10_CAPTURE"


def line(nonce=NONCE):
    return f"10-19 04:05:06.123  2000  2000 I {TAG}: M10_CAPTURE_READY nonce={nonce}\r\n"


class CaptureHandshakeTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.children = []
        self.capture = None
        self.addCleanup(self.finish)
        self.calls = []

    def finish(self):
        if self.capture:
            self.capture.finish()
        for child in self.children:
            if child.poll() is None:
                child.terminate()
                child.wait(timeout=3)

    def make(self, *, emitted=None, initial=b"", delay=0, stderr=b"", exit_early=False,
             writer_error=False, writer_status="PASS", prior=(), timeout=.6):
        trigger = Path(self.temp.name) / "trigger"
        emitted = line().encode() if emitted is None else emitted
        code = ("import os,time,pathlib\n"
                f"os.write(1,{initial!r})\n"
                + ("raise SystemExit(19)\n" if exit_early else "")
                + f"p=pathlib.Path({str(trigger)!r})\n"
                + "while not p.exists(): time.sleep(.005)\n"
                + f"time.sleep({delay}); os.write(2,{stderr!r}); os.write(1,{emitted!r}); time.sleep(30)\n")
        def factory(argv, **kwargs):
            self.command = argv
            child = subprocess.Popen([sys.executable, "-u", "-c", code], **kwargs)
            self.children.append(child)
            return child
        def writer(message):
            self.calls.append(message)
            self.assertEqual("M10_CAPTURE_READY nonce=" + NONCE, message)
            self.assertTrue(all(state["thread"].is_alive() for state in self.capture._streams.values()))
            if writer_error:
                raise OSError("synthetic diagnostic unavailable")
            trigger.write_bytes(b"synthetic host trigger only")
            return {"status": writer_status, "serial": "emulator-5624", "exitCode": 0,
                    "tag": TAG, "nonce": NONCE}
        self.capture = ContinuousLogCapture(
            Path(self.temp.name) / "locked-adb.exe", "emulator-5624",
            Path(self.temp.name) / "capture", handshake_writer=writer, nonce=NONCE,
            prior_diagnostics=prior, popen_factory=factory, readiness_timeout=timeout,
            max_seconds=3)
        return self.capture

    def test_no_matching_history_can_become_ready_only_after_exact_handshake(self):
        capture = self.make()
        self.assertEqual("PASS", capture.start()["status"])
        proof = capture.finish()
        self.assertEqual("PASS", proof["status"], proof)
        self.assertEqual(["M10_CAPTURE_READY nonce=" + NONCE], self.calls)
        self.assertEqual(line(), proof["handshake"]["authenticatedRawLine"])
        self.assertEqual("DIAGNOSTIC_NONCE_V1", proof["readinessProtocol"])
        self.assertEqual(1, proof["handshake"]["diagnosticEmissionCount"])
        self.assertIn(TAG + ":I", self.command)
        self.assertEqual(proof, read_verified(capture.directory)[0])

    def test_header_alone_does_not_satisfy_handshake(self):
        capture = self.make(initial=b"--------- beginning of main\n", emitted=b"", timeout=.15)
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.assertIsNone(capture.finish()["ready"])

    def test_wrong_nonce_fails_closed(self):
        capture = self.make(emitted=line(OTHER).encode())
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.assertIn("HANDSHAKE_UNEXPECTED_DIAGNOSTIC", capture.finish()["errors"])

    def test_duplicate_current_diagnostic_fails_closed(self):
        capture = self.make(emitted=(line() + line()).encode())
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.assertIn("HANDSHAKE_DUPLICATE_DIAGNOSTIC", capture.finish()["errors"])

    def test_delay_within_fixed_bound_preserves_real_arrival(self):
        capture = self.make(delay=.08)
        begin = time.monotonic()
        self.assertEqual("PASS", capture.start()["status"])
        self.assertGreaterEqual(time.monotonic() - begin, .07)
        self.assertEqual("PASS", capture.finish()["status"])

    def test_filter_mismatch_times_out_instead_of_using_header(self):
        capture = self.make(emitted=b"", timeout=.15)
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.assertIn("READINESS_NOT_PROVEN", capture.finish()["errors"])

    def test_reader_early_exit_never_becomes_ready(self):
        capture = self.make(exit_early=True)
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.assertEqual(19, capture.finish()["process"]["exitCode"])

    def test_writer_exception_preserves_unknown_and_closes_owned_host_reader(self):
        capture = self.make(writer_error=True)
        self.assertEqual("UNKNOWN", capture.start()["status"])
        proof = capture.finish()
        self.assertTrue(proof["process"]["controlledHostObserverStop"])
        self.assertTrue(any("HANDSHAKE_WRITE_ERROR" in error for error in proof["errors"]))

    def test_stderr_and_nonpass_writer_cannot_make_ready(self):
        capture = self.make(stderr=b"logcat: invalid filter\n", writer_status="UNKNOWN")
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.assertIn("HANDSHAKE_WRITE_NOT_PROVEN", capture.finish()["errors"])

    def test_authenticated_old_diagnostic_is_allowed_only_before_current(self):
        capture = self.make(initial=line(OTHER).encode(), prior=(line(OTHER),))
        self.assertEqual("PASS", capture.start()["status"])
        proof = capture.finish()
        self.assertEqual("PASS", proof["status"], proof)
        self.assertEqual([line(OTHER)], proof["priorDiagnostics"])
        self.assertEqual("PASS", read_verified(capture.directory)[0]["status"])

    def test_replayed_old_diagnostic_after_current_is_rejected(self):
        capture = self.make(emitted=(line() + line(OTHER)).encode(), prior=(line(OTHER),))
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.assertIn("HANDSHAKE_LATE_PRIOR_DIAGNOSTIC", capture.finish()["errors"])

    def test_reverification_rejects_nonce_proof_tamper(self):
        capture = self.make()
        self.assertEqual("PASS", capture.start()["status"])
        self.assertEqual("PASS", capture.finish()["status"])
        path = capture.directory / "capture.json"
        proof = json.loads(path.read_bytes())
        proof["handshake"]["nonce"] = OTHER
        path.write_text(json.dumps(proof), encoding="utf-8")
        with self.assertRaises(ValueError):
            read_verified(capture.directory)

    def test_ready_rejects_observed_stderr_before_error_publication(self):
        capture = self.make()
        original = capture.handshake_writer

        def writer(message):
            result = original(message)
            # Model the exact interval after the reader records a byte under
            # its lock and before _abort() can publish its error outside it.
            with capture._lock:
                capture._streams["stderr"]["observed"] = 1
                self.assertEqual([], capture._errors)
            return result

        capture.handshake_writer = writer
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.assertIsNone(capture.finish()["ready"])

    def test_assert_live_rejects_observed_stderr_before_error_publication(self):
        capture = self.make()
        self.assertEqual("PASS", capture.start()["status"])
        with capture._lock:
            capture._streams["stderr"]["observed"] = 1
            self.assertEqual([], capture._errors)
        self.assertEqual("UNKNOWN", capture.assert_live()["status"])

    def test_ready_rejects_reader_eof_before_error_publication(self):
        capture = self.make()
        original = capture.handshake_writer

        def writer(message):
            result = original(message)
            with capture._lock:
                capture._streams["stderr"]["eof"] = True
                self.assertEqual([], capture._errors)
            return result

        capture.handshake_writer = writer
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.assertIsNone(capture.finish()["ready"])

    def test_ready_rejects_stopped_reader_before_error_publication(self):
        capture = self.make()
        original = capture.handshake_writer

        def writer(message):
            result = original(message)
            with capture._lock:
                capture._streams["stderr"]["readerStopped"] = True
                self.assertEqual([], capture._errors)
            return result

        capture.handshake_writer = writer
        self.assertEqual("UNKNOWN", capture.start()["status"])
        self.assertIsNone(capture.finish()["ready"])


if __name__ == "__main__":
    unittest.main()
