"""Synthetic interpreter tests; verifier mocking does not prove Android READY."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from continuous_evidence import consume_capture
from test_lifecycle_evidence import fixture


NONCE = "0123456789abcdef0123456789abcdef"
OLD_NONCE = "abcdef0123456789abcdef0123456789"


def diagnostic(nonce=NONCE, tag="LITHIC_M10_CAPTURE"):
    return "10-19 01:02:03.004  909  909 I " + tag + ": M10_CAPTURE_READY nonce=" + nonce + "\n"


class HandshakeConsumerTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.directory = Path(self.tmp.name)
        self.phases = fixture()[3].splitlines()

    def consume(self, raw, *, prior=(), proof_prior=None, authenticated=None, prior_phases=""):
        text = raw.decode("utf-8", errors="strict")
        proof = {"status": "PASS", "serial": "emulator-5624",
                 "readinessProtocol": "DIAGNOSTIC_NONCE_V1",
                 "priorDiagnostics": list(prior if proof_prior is None else proof_prior),
                 "handshake": {"tag": "LITHIC_M10_CAPTURE", "nonce": NONCE,
                               "authenticatedRawLine": diagnostic() if authenticated is None else authenticated,
                               "diagnosticEmissionCount": 1},
                 "stdout": {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest()},
                 "stderr": {"bytes": 0, "sha256": hashlib.sha256(b"").hexdigest()}}
        (self.directory / "capture.json").write_bytes(json.dumps(proof).encode("utf-8"))
        (self.directory / "stdout.raw").write_bytes(raw)
        with patch("continuous_log.read_verified", return_value=(proof, text)):
            return consume_capture(proof, self.directory, prior_phases,
                                   expected_serial="emulator-5624", prior_diagnostics=prior)

    def test_no_historical_business_log_is_required_for_matching_diagnostic(self):
        raw = (diagnostic() + "\n".join(self.phases) + "\n").encode()
        result = self.consume(raw)
        self.assertEqual("\n".join(self.phases), result["newPhaseLog"])
        self.assertEqual(1, result["metadata"]["authenticatedDiagnosticsExcluded"])

    def test_main_header_is_context_and_not_readiness_evidence(self):
        raw = ("--------- beginning of main\n" + diagnostic() + "\n".join(self.phases) + "\n").encode()
        self.assertEqual(6, len(self.consume(raw)["newPhaseLog"].splitlines()))

    def test_wrong_nonce_diagnostic_cannot_be_discarded(self):
        raw = (diagnostic(OLD_NONCE) + "\n".join(self.phases) + "\n").encode()
        with self.assertRaises(ValueError): self.consume(raw)

    def test_duplicate_current_diagnostic_is_rejected(self):
        raw = (diagnostic() * 2 + "\n".join(self.phases) + "\n").encode()
        with self.assertRaises(ValueError): self.consume(raw)

    def test_only_independently_authenticated_prior_diagnostics_are_backlog(self):
        prior = diagnostic(OLD_NONCE)
        raw = (prior + diagnostic() + "\n".join(self.phases) + "\n").encode()
        result = self.consume(raw, prior=(prior,))
        self.assertEqual(2, result["metadata"]["authenticatedDiagnosticsExcluded"])
        self.assertEqual(raw, (self.directory / "stdout.raw").read_bytes())

    def test_unproven_prior_allowlist_in_proof_is_rejected(self):
        prior = diagnostic(OLD_NONCE)
        raw = (prior + diagnostic() + "\n".join(self.phases) + "\n").encode()
        with self.assertRaises(ValueError): self.consume(raw, proof_prior=(prior,))

    def test_duplicate_prior_diagnostic_is_rejected(self):
        prior = diagnostic(OLD_NONCE)
        raw = (prior * 2 + diagnostic() + "\n".join(self.phases) + "\n").encode()
        with self.assertRaises(ValueError): self.consume(raw, prior=(prior,))

    def test_prior_diagnostic_after_current_handshake_is_rejected(self):
        prior = diagnostic(OLD_NONCE)
        raw = (diagnostic() + prior + "\n".join(self.phases) + "\n").encode()
        with self.assertRaises(ValueError): self.consume(raw, prior=(prior,))

    def test_new_business_before_ready_is_rejected(self):
        raw = (self.phases[0] + "\n" + diagnostic() + "\n".join(self.phases[1:]) + "\n").encode()
        with self.assertRaises(ValueError): self.consume(raw)

    def test_old_business_after_current_diagnostic_is_not_treated_as_backlog(self):
        raw = (diagnostic() + self.phases[0] + "\n").encode()
        with self.assertRaises(ValueError): self.consume(raw, prior_phases=self.phases[0])

    def test_unknown_diagnostic_tag_or_format_is_not_treated_as_business(self):
        for extra in (diagnostic(tag="LITHIC_OTHER"), diagnostic().replace("nonce=", "bad=")):
            with self.subTest(extra=extra), self.assertRaises(ValueError):
                self.consume((diagnostic() + extra + "\n".join(self.phases) + "\n").encode())

    def test_crlf_diagnostic_matches_exact_raw_line_without_rewriting(self):
        current = diagnostic().replace("\n", "\r\n")
        raw = (current + "\n".join(self.phases) + "\n").encode()
        result = self.consume(raw, authenticated=current)
        self.assertEqual("\n".join(self.phases), result["newPhaseLog"])
        self.assertEqual(raw, (self.directory / "stdout.raw").read_bytes())

    def test_real_host_pipe_handshake_proof_is_consumed_without_mocking_verifier(self):
        from test_capture_handshake import CaptureHandshakeTest, line
        helper = CaptureHandshakeTest()
        helper.setUp()
        self.addCleanup(helper.doCleanups)
        capture = helper.make(emitted=(line() + "\n".join(self.phases) + "\n").encode())
        self.assertEqual("PASS", capture.start()["status"])
        proof = capture.finish()
        self.assertEqual("PASS", proof["status"], proof)
        result = consume_capture(proof, capture.directory, "", expected_serial="emulator-5624")
        self.assertEqual("\n".join(self.phases), result["newPhaseLog"])
        self.assertEqual(1, result["metadata"]["authenticatedDiagnosticsExcluded"])


if __name__ == "__main__": unittest.main(verbosity=2)
