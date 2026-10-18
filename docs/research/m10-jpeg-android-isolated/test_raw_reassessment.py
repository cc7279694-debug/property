"""Offline replay of one already-captured healthy run; no Android operations."""
import json
from pathlib import Path
import shutil
import tempfile
import unittest

import raw_reassessment as subject


class RawReassessmentTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "method-00"
        source = Path(__file__).resolve().parents[3] / "docs/research/m10-jpeg-android-runtime-resume/evidence/api26/method-00"
        shutil.copytree(source, self.path)

    def test_actual_crlf_capture_passes_without_changing_any_original(self):
        original = {p.name:p.read_bytes() for p in self.path.iterdir()}
        result = subject.assessment(self.path)
        self.assertEqual("PASS", result["status"])
        self.assertEqual("UNKNOWN", result["originalStatus"])
        self.assertFalse(result["supersedesOriginal"])
        self.assertEqual(original, {p.name:p.read_bytes() for p in self.path.iterdir()})

    def test_other_unknown_reason_cannot_be_reassessed(self):
        path = self.path / "result.json"; row = json.loads(path.read_text())
        row["lifecycle"]["errors"] = ["Worker death UNKNOWN"]
        path.write_text(json.dumps(row))
        with self.assertRaises(RuntimeError): subject.assessment(self.path)

    def test_changed_instrumentation_bytes_cannot_be_reassessed(self):
        path = self.path / "instrumentation.txt"; path.write_bytes(path.read_bytes()+b"other")
        with self.assertRaises(RuntimeError): subject.assessment(self.path)

    def test_published_gate_is_exactly_recomputed_not_trusted_as_a_flag(self):
        proof = subject.assessment(self.path)
        (self.path / "raw-byte-reassessment.json").write_text(json.dumps(proof))
        original = json.loads((self.path / "result.json").read_text())
        effective = subject.effective_result(self.path)
        self.assertEqual("PASS", effective["status"])
        self.assertEqual("UNKNOWN", original["status"])
        proof["sourceEvidence"]["result.json"]["sha256"] = "0"*64
        (self.path / "raw-byte-reassessment.json").write_text(json.dumps(proof))
        with self.assertRaises(RuntimeError): subject.effective_result(self.path)


if __name__ == "__main__": unittest.main(verbosity=2)
