"""Synthetic headers contain no entropy data; none is an image acceptance test."""
import io
from pathlib import Path
import tempfile
import unittest

import e01_audit as audit


def header(width=8000, height=5000, sampling=(0x22, 0x11, 0x11), marker=0xC2,
           prefix=b"", precision=8):
    body = bytes([precision]) + height.to_bytes(2, "big") + width.to_bytes(2, "big")
    body += bytes([len(sampling)])
    body += b"".join(bytes([i + 1, s, 0]) for i, s in enumerate(sampling))
    return b"\xff\xd8" + prefix + bytes([255, marker]) + (len(body) + 2).to_bytes(2, "big") + body


def frame(width=8000, height=5000, sampling=((2, 2), (1, 1), (1, 1)), coding="progressive"):
    return {"width": width, "height": height, "precision": 8,
            "sampling": sampling, "coding": coding}


class ArithmeticTests(unittest.TestCase):
    def test_exact_32bit_boundary(self):
        self.assertEqual(audit.checked_add(4294967294, 1, 32), 4294967295)

    def test_32bit_add_overflow(self):
        with self.assertRaises(OverflowError):
            audit.checked_add(4294967295, 1, 32)

    def test_32bit_multiply_overflow(self):
        with self.assertRaises(OverflowError):
            audit.checked_mul(65536, 65536, 32)

    def test_exact_64bit_boundary(self):
        self.assertEqual(audit.checked_add(18446744073709551614, 1, 64), 18446744073709551615)

    def test_64bit_multiply_overflow(self):
        with self.assertRaises(OverflowError):
            audit.checked_mul(4294967296, 4294967296, 64)

    def test_negative_operand_rejected(self):
        with self.assertRaises(ValueError):
            audit.checked_add(-1, 1, 32)

    def test_oversize_operand_not_hidden_by_zero(self):
        with self.assertRaises(OverflowError):
            audit.checked_mul(4294967296, 0, 32)

    def test_unsupported_word_size_rejected(self):
        with self.assertRaises(ValueError):
            audit.checked_mul(1, 1, 16)


class HeaderTests(unittest.TestCase):
    def parse(self, raw):
        return audit.read_frame(io.BytesIO(raw), len(raw))

    def test_progressive_frame_metadata(self):
        self.assertEqual(self.parse(header()), frame())

    def test_baseline_header_does_not_prove_single_scan(self):
        self.assertEqual(self.parse(header(marker=0xC0)).get("coding"), "baseline_scan_layout_unknown")

    def test_metadata_segments_skipped_by_declared_length(self):
        raw = header(prefix=b"\xff\xe1\x00\x06a\xff\xc2b")
        self.assertEqual(self.parse(raw).get("width"), 8000)

    def test_header_only_is_enough_for_model_not_integrity(self):
        raw = header(1, 1, (0x11,))
        self.assertEqual(self.parse(raw).get("width"), 1)

    def test_truncated_segment_rejected(self):
        with self.assertRaises(ValueError):
            self.parse(header()[:-1])

    def test_invalid_segment_length_rejected(self):
        with self.assertRaises(ValueError):
            self.parse(b"\xff\xd8\xff\xe1\x00\x01")

    def test_sos_before_frame_rejected(self):
        with self.assertRaises(ValueError):
            self.parse(b"\xff\xd8\xff\xda\x00\x02")

    def test_zero_sampling_rejected(self):
        with self.assertRaises(ValueError):
            self.parse(header(sampling=(0x01,)))

    def test_duplicate_component_id_rejected(self):
        raw = bytearray(header())
        raw[-6] = 1
        with self.assertRaises(ValueError):
            self.parse(bytes(raw))

    def test_reported_file_size_mismatch_rejected(self):
        raw = header()
        with self.assertRaises(ValueError):
            audit.read_frame(io.BytesIO(raw), len(raw) - 1)


class PayloadTests(unittest.TestCase):
    def test_40mp_420_includes_vertical_mcu_padding(self):
        result = audit.coefficient_payload(frame(), 64)
        self.assertEqual(result.get("coefficientBytes"), 120192000)
        self.assertEqual(result.get("rowPointerBytes"), 10016)
        self.assertIsNone(result.get("totalNativeBytes"))
        self.assertEqual(result.get("integrity"), "NOT_EVALUATED")

    def test_40mp_444_payload(self):
        self.assertEqual(audit.coefficient_payload(frame(sampling=((1, 1),) * 3), 64).get("coefficientBytes"), 240000000)

    def test_40mp_gray_payload(self):
        self.assertEqual(audit.coefficient_payload(frame(sampling=((1, 1),)), 64).get("coefficientBytes"), 80000000)

    def test_32bit_row_pointer_payload(self):
        self.assertEqual(audit.coefficient_payload(frame(), 32).get("rowPointerBytes"), 5008)

    def test_tiny_420_padded_blocks(self):
        self.assertEqual(audit.coefficient_payload(frame(1, 1), 32).get("coefficientBytes"), 768)

    def test_narrow_tall_and_wide_420(self):
        for width, height in ((1, 40000), (40000, 1)):
            with self.subTest(width=width, height=height):
                self.assertEqual(audit.coefficient_payload(frame(width, height), 32).get("coefficientBytes"), 1920000)

    def test_baseline_full_buffer_remains_possible(self):
        result = audit.coefficient_payload(frame(coding="baseline_scan_layout_unknown"), 64)
        self.assertEqual(result.get("coefficientBytes"), 120192000)
        self.assertEqual(result.get("allocationCondition"), "IF_FULL_COEFFICIENT_BUFFER")

    def test_two_components_not_silently_rejected(self):
        self.assertEqual(audit.coefficient_payload(frame(1, 1, ((1, 1),) * 2), 64).get("coefficientBytes"), 256)

    def test_outside_pixel_model_rejected(self):
        with self.assertRaises(ValueError):
            audit.coefficient_payload(frame(8000, 5001), 64)


class SourceIdentityTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "unit.c").write_bytes(b"abc")
        self.lock = [{"path": "unit.c", "bytes": 3,
                      "sha256": "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"}]

    def test_matching_source_returns_verified_path(self):
        self.assertEqual(audit.verify_locked_tree(self.root, self.lock), ["unit.c"])

    def test_changed_allocator_source_blocks_reuse(self):
        (self.root / "unit.c").write_bytes(b"abd")
        with self.assertRaises(ValueError):
            audit.verify_locked_tree(self.root, self.lock)

    def test_missing_source_blocks_inventory(self):
        self.lock[0]["path"] = "absent.c"
        with self.assertRaises(ValueError):
            audit.verify_locked_tree(self.root, self.lock)

    def test_path_escape_rejected(self):
        self.lock[0]["path"] = "../unit.c"
        with self.assertRaises(ValueError):
            audit.verify_locked_tree(self.root, self.lock)

    def test_duplicate_entry_rejected(self):
        with self.assertRaises(ValueError):
            audit.verify_locked_tree(self.root, self.lock * 2)


if __name__ == "__main__":
    unittest.main(verbosity=2)
