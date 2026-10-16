#!/usr/bin/env python3
"""Derive isolated research inputs from authored fixtures; never read locked JPEGs.

This byte parser locates original marker/scan ranges. It is not a JPEG decoder,
does not certify mutation validity, and cannot replace either strict PoC path.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from dataclasses import dataclass
from pathlib import Path
import sys
from typing import Any, TextIO


MIB = 1024 * 1024
INPUT_LIMIT = 64 * MIB
MAX_PARENT_BYTES = 4 * MIB
CHUNK_BYTES = 64 * 1024
LOCKED_NAMES = {"complete.jpg", "entropy_cut_eoi_retained.jpg"}
PARENT_NAMES = (
    "baseline_rgb_420.jpg",
    "progressive_rgb_420.jpg",
    "baseline_restart_420.jpg",
    "progressive_restart_420.jpg",
    "exif_orientation_1.jpg",
)
SOF_MARKERS = {0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7,
               0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF}


@dataclass(frozen=True)
class Marker:
    code: int
    start: int
    end: int
    length_offset: int | None
    payload_offset: int | None


@dataclass(frozen=True)
class Scan:
    header: Marker
    start: int
    end: int
    restarts: tuple[tuple[int, int, int], ...]


@dataclass(frozen=True)
class Edit:
    """Replace source[start:end] with pattern repeated repeat_count times."""

    start: int
    end: int
    pattern: bytes = b""
    repeat_count: int = 0

    @property
    def replacement_length(self) -> int:
        return len(self.pattern) * self.repeat_count

    def pieces(self):
        if self.repeat_count < 0 or (not self.pattern and self.repeat_count):
            raise ValueError("Invalid repeat specification")
        if not self.repeat_count:
            return
        repetitions = max(1, CHUNK_BYTES // len(self.pattern))
        full_piece = self.pattern * repetitions
        remaining = self.repeat_count
        while remaining >= repetitions:
            yield full_piece
            remaining -= repetitions
        if remaining:
            yield self.pattern * remaining


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def file_hash(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        while chunk := stream.read(CHUNK_BYTES):
            digest.update(chunk)
    return digest.hexdigest()


def parse_original(data: bytes) -> tuple[list[Marker], list[Scan]]:
    """Locate healthy-parent boundaries without interpreting compressed data."""
    if not data.startswith(b"\xff\xd8"):
        raise ValueError("Authored parent has no JPEG SOI")
    markers = [Marker(0xD8, 0, 2, None, None)]
    scans: list[Scan] = []
    position = 2
    while position < len(data):
        start = position
        if data[position] != 0xFF:
            raise ValueError(f"Expected parent marker at byte {position}")
        while position < len(data) and data[position] == 0xFF:
            position += 1
        if position >= len(data):
            raise ValueError("Parent ends in marker prefix")
        code = data[position]
        position += 1
        if code == 0xD9:
            marker = Marker(code, start, position, None, None)
            markers.append(marker)
            if position != len(data):
                raise ValueError("Authored parent has trailing bytes after EOI")
            return markers, scans
        if code in {0, 0xD8} or 0xD0 <= code <= 0xD7:
            raise ValueError("Unexpected standalone/stuffed parent marker")
        if code == 1:
            markers.append(Marker(code, start, position, None, None))
            continue
        length_offset = position
        if position + 2 > len(data):
            raise ValueError("Truncated parent marker length")
        length = int.from_bytes(data[position:position + 2], "big")
        if length < 2 or position + length > len(data):
            raise ValueError("Invalid parent marker length")
        marker = Marker(code, start, position + length,
                        length_offset, position + 2)
        markers.append(marker)
        position = marker.end
        if code != 0xDA:
            continue
        entropy_start = position
        restarts: list[tuple[int, int, int]] = []
        while True:
            prefix = data.find(b"\xff", position)
            if prefix < 0:
                raise ValueError("Parent scan has no closing marker")
            following = prefix + 1
            while following < len(data) and data[following] == 0xFF:
                following += 1
            if following >= len(data):
                raise ValueError("Parent scan ends inside marker")
            next_code = data[following]
            if next_code == 0:
                position = following + 1
                continue
            if 0xD0 <= next_code <= 0xD7:
                restarts.append((prefix, following + 1, next_code))
                position = following + 1
                continue
            scans.append(Scan(marker, entropy_start, prefix, tuple(restarts)))
            position = prefix
            break
    raise ValueError("Authored parent has no final EOI")


def validate_edits(data: bytes, edits: list[Edit]) -> list[Edit]:
    result = sorted(edits, key=lambda edit: (edit.start, edit.end))
    previous_end = 0
    for edit in result:
        if not (previous_end <= edit.start <= edit.end <= len(data)):
            raise ValueError("Overlapping/out-of-range byte edits")
        if edit.repeat_count < 0 or (edit.repeat_count and not edit.pattern):
            raise ValueError("Invalid edit repetition")
        previous_end = edit.end
    return result


def emit_mutation(output: Path, name: str, parent: dict[str, Any],
                  edits: list[Edit], expected: str, classification: str,
                  note: str) -> dict[str, Any]:
    if name in LOCKED_NAMES or name in PARENT_NAMES or Path(name).name != name:
        raise ValueError("Output must use a new non-locked fixture name")
    data: bytes = parent["data"]
    edits = validate_edits(data, edits)
    expected_length = len(data) + sum(
        edit.replacement_length - (edit.end - edit.start) for edit in edits)
    if not 0 <= expected_length <= INPUT_LIMIT + 1:
        raise ValueError("Mutation output exceeds explicit research file bound")
    path = output / name
    digest = hashlib.sha256()
    edit_records = []
    position = 0
    output_position = 0
    created = False
    try:
        with path.open("xb") as stream:
            created = True
            for edit in edits:
                unchanged = data[position:edit.start]
                stream.write(unchanged)
                digest.update(unchanged)
                output_position += len(unchanged)
                replacement_digest = hashlib.sha256()
                record = {
                    "source_start_inclusive": edit.start,
                    "source_end_exclusive": edit.end,
                    "source_removed_bytes": edit.end - edit.start,
                    "source_removed_sha256": sha256(data[edit.start:edit.end]),
                    "replacement_pattern_hex": edit.pattern.hex(),
                    "replacement_repeat_count": edit.repeat_count,
                    "replacement_bytes": edit.replacement_length,
                    "output_start_inclusive": output_position,
                    "output_end_exclusive": output_position + edit.replacement_length,
                }
                for chunk in edit.pieces():
                    stream.write(chunk)
                    digest.update(chunk)
                    replacement_digest.update(chunk)
                    output_position += len(chunk)
                record["replacement_sha256"] = replacement_digest.hexdigest()
                edit_records.append(record)
                position = edit.end
            tail = data[position:]
            stream.write(tail)
            digest.update(tail)
            output_position += len(tail)
        if output_position != expected_length or path.stat().st_size != expected_length:
            raise ValueError("Mutation output length mismatch")
        if file_hash(path) != digest.hexdigest():
            raise ValueError("Mutation reread hash mismatch")
    except BaseException:
        if created:
            path.unlink(missing_ok=True)
        raise
    return {
        "fixture": name,
        "provenance": "authored-generated-parent-plus-recorded-byte-edits",
        "parent_fixture": parent["name"],
        "parent_bytes": len(data),
        "parent_sha256": parent["sha256"],
        "source_coordinate_system": "zero-based byte offsets in original parent",
        "edits": edit_records,
        "output_bytes": expected_length,
        "output_sha256": digest.hexdigest(),
        "intended_expected_result": expected,
        "validity_classification": classification,
        "note": note,
        "decoder_test_status": "NOT_RUN",
    }


def dimension_edits(parent: dict[str, Any], width: int, height: int) -> list[Edit]:
    if not 0 <= width <= 65535 or not 0 <= height <= 65535:
        raise ValueError("SOF dimensions must fit two-byte fields")
    marker = next(marker for marker in parent["markers"] if marker.code in SOF_MARKERS)
    assert marker.payload_offset is not None
    # SOF payload: precision byte, height uint16, width uint16.
    offset = marker.payload_offset + 1
    return [Edit(offset, offset + 4,
                 height.to_bytes(2, "big") + width.to_bytes(2, "big"), 1)]


def exact_dimensions(pixels: int) -> tuple[int, int] | None:
    for height in range(min(65535, math.isqrt(pixels)), 0, -1):
        if pixels % height == 0 and pixels // height <= 65535:
            return pixels // height, height
    return None


def derive(parents: dict[str, dict[str, Any]], output: Path,
           include_size_boundaries: bool, records: TextIO) -> list[dict[str, Any]]:
    results = []

    def add(name: str, parent_name: str, edits: list[Edit], expected: str,
            classification: str, note: str):
        result = emit_mutation(output, name, parents[parent_name], edits,
                               expected, classification, note)
        results.append(result)
        records.write(json.dumps(result, ensure_ascii=True) + "\n")
        records.flush()

    progressive = parents["progressive_rgb_420.jpg"]
    longest = max(progressive["scans"], key=lambda scan: scan.end - scan.start)
    if longest.end - longest.start < 48:
        raise ValueError("Progressive parent has insufficient entropy for declared cut")
    cut_start = longest.start + (longest.end - longest.start) // 3
    cut_end = longest.start + 2 * (longest.end - longest.start) // 3
    add("progressive_entropy_middle_cut_eoi_retained.jpg", progressive["name"],
        [Edit(cut_start, cut_end)], "REJECT_EXPECTED_NOT_PROVEN",
        "UNVERIFIED_ENTROPY_DAMAGE_MAY_REMAIN_DECODABLE",
        "Delete middle third of the longest compressed scan, retaining all later markers and EOI; actual warning/fatal is measured separately.")
    final_scan = progressive["scans"][-1]
    if final_scan.end - final_scan.start < 8:
        raise ValueError("Last progressive scan too short for declared cut")
    add("progressive_final_entropy_tail_cut_eoi_retained.jpg", progressive["name"],
        [Edit(final_scan.start + (final_scan.end - final_scan.start) // 2,
              final_scan.end)], "REJECT_EXPECTED_NOT_PROVEN",
        "UNVERIFIED_ENTROPY_DAMAGE_MAY_REMAIN_DECODABLE",
        "Delete second half of final entropy scan while keeping final EOI. This is not a claim that every entropy mutation must be syntactically invalid.")
    add("progressive_final_whole_scan_removed.jpg", progressive["name"],
        [Edit(final_scan.header.start, final_scan.end)], "UNKNOWN",
        "EXPLORATORY_POTENTIALLY_LEGAL_LESS_REFINED_PROGRESSIVE",
        "Remove complete final SOS plus its entropy. A legal lower-refinement JPEG may remain; successful decode is a capability limit, not automatically a defect.")

    for parent_name, prefix in (("baseline_restart_420.jpg", "baseline"),
                                ("progressive_restart_420.jpg", "progressive")):
        parent = parents[parent_name]
        scan = next(scan for scan in parent["scans"] if scan.restarts)
        start, end, code = scan.restarts[0]
        add(f"{prefix}_restart_first_marker_removed.jpg", parent_name,
            [Edit(start, end)], "REJECT_EXPECTED_NOT_PROVEN",
            "UNVERIFIED_RESTART_SEQUENCE_DAMAGE",
            "Remove the first restart marker including any fill-prefix bytes; observe strict warning/fatal rather than assuming an outcome.")
        add(f"{prefix}_restart_first_marker_wrong_order.jpg", parent_name,
            [Edit(end - 1, end, bytes([0xD0 + ((code - 0xD0 + 3) % 8)]), 1)],
            "REJECT", "DECLARED_RESTART_SEQUENCE_VIOLATION",
            "Replace the first RST code with the code three positions later in its cyclic sequence, without changing byte count.")
        if start - scan.start < 16:
            raise ValueError("Insufficient entropy before first restart")
        add(f"{prefix}_restart_eight_bytes_before_removed.jpg", parent_name,
            [Edit(start - 8, start)], "REJECT_EXPECTED_NOT_PROVEN",
            "UNVERIFIED_ENTROPY_DAMAGE_AT_RESTART_BOUNDARY",
            "Delete exactly eight bytes immediately before the first RST; EOI retained.")
        following = scan.restarts[1][0] if len(scan.restarts) > 1 else scan.end
        if following - end < 16:
            raise ValueError("Insufficient entropy after first restart")
        add(f"{prefix}_restart_eight_bytes_after_removed.jpg", parent_name,
            [Edit(end, end + 8)], "REJECT_EXPECTED_NOT_PROVEN",
            "UNVERIFIED_ENTROPY_DAMAGE_AT_RESTART_BOUNDARY",
            "Delete exactly eight bytes immediately after the first RST; EOI retained.")

    baseline = parents["baseline_rgb_420.jpg"]
    eoi = baseline["markers"][-1]
    add("baseline_eoi_removed.jpg", baseline["name"],
        [Edit(eoi.start, eoi.end)], "REJECT", "MISSING_REQUIRED_EOI",
        "Complete original scan bytes remain; strict completion must not silently synthesize EOI.")
    exif = parents["exif_orientation_1.jpg"]
    app1 = next(marker for marker in exif["markers"] if marker.code == 0xE1)
    assert app1.length_offset is not None and app1.payload_offset is not None
    add("metadata_app1_length_one.jpg", exif["name"],
        [Edit(app1.length_offset, app1.length_offset + 2, b"\x00\x01", 1)],
        "REJECT", "DECLARED_INVALID_JPEG_SEGMENT_LENGTH",
        "JPEG segment length one cannot include its own two-byte length field.")
    add("metadata_exif_unknown_tiff_type.jpg", exif["name"],
        [Edit(app1.payload_offset + 18, app1.payload_offset + 20,
              b"\xff\xff", 1)], "UNKNOWN", "JPEG_SYNTAX_MAY_REMAIN_VALID_EXIF_SEMANTICS_OUTSIDE_DECODER",
        "Change orientation entry TIFF type to 65535. JPEG entropy remains original; libjpeg may intentionally ignore semantic EXIF errors.")
    if len(baseline["data"]) + 8 >= 65535:
        raise ValueError("Small baseline too large for declared APP1-beyond-EOF fixture")
    add("metadata_declared_app1_truncated.jpg", baseline["name"],
        [Edit(2, 2, b"\xff\xe1\xff\xffExif\x00\x00", 1)],
        "REJECT", "DECLARED_SEGMENT_EXTENDS_BEYOND_EOF",
        "Insert APP1 with declared 65535-byte segment but only six inserted payload bytes; the entire small original parent is shorter than the declaration.")
    add("metadata_4096_empty_comments.jpg", baseline["name"],
        [Edit(2, 2, b"\xff\xfe\x00\x02", 4096)], "UNKNOWN",
        "EXPLORATORY_MARKER_COMPLEXITY_SYNTACTICALLY_POSSIBLE",
        "4096 empty COM segments stress marker work. No acceptance claim beyond valid segment lengths; policy complexity limits may reject.")

    for name, width, height, note in (
        ("sof_over40mp_8000x5001.jpg", 8000, 5001,
         "40,008,000 claimed pixels; compressed scan is not regenerated, so only the pre-decode size guard is being isolated."),
        ("sof_extreme_65535x65535.jpg", 65535, 65535,
         "4,294,836,225 claimed pixels and 12,884,508,675 RGB bytes require checked wide multiplication before allocation."),
        ("sof_zero_width.jpg", 0, 193,
         "Zero width is invalid; do not allocate/decode using unchecked dimensions."),
        ("sof_zero_height.jpg", 257, 0,
         "Zero height would require a supported DNL protocol; no DNL is added, and PoC must reject unsupported dimensions."),
    ):
        add(name, baseline["name"], dimension_edits(baseline, width, height),
            "REJECT", "HEADER_ONLY_SIZE_MUTATION_NOT_HEALTHY_REENCODING", note)
    for pixels, label in ((40_000_000 - 1, "minus_one"),
                          (40_000_000, "exact"),
                          (40_000_000 + 1, "plus_one")):
        dimensions = exact_dimensions(pixels)
        if dimensions is not None:
            width, height = dimensions
            add(f"sof_40mp_{label}_{width}x{height}.jpg", baseline["name"],
                dimension_edits(baseline, width, height),
                "REJECT" if pixels > 40_000_000 else "UNKNOWN",
                "HEADER_ONLY_SIZE_MUTATION_NOT_HEALTHY_REENCODING",
                "Exact pixel arithmetic boundary, not a valid regenerated entropy image. Below/at the pixel cap does not imply this mutated JPEG should decode successfully.")

    if include_size_boundaries:
        for target, label in ((INPUT_LIMIT - 1, "minus_one"),
                              (INPUT_LIMIT, "exact"),
                              (INPUT_LIMIT + 1, "plus_one")):
            add(f"input_64mib_{label}_trailing_zero_padding.jpg", baseline["name"],
                [Edit(len(baseline["data"]), len(baseline["data"]), b"\x00",
                      target - len(baseline["data"]))],
                "REJECT" if target > INPUT_LIMIT else "UNKNOWN",
                "SIZE_GATE_ONLY_TRAILING_BYTES_NOT_HEALTHY_JPEG_CORPUS",
                "Append deterministic zeros after original EOI to exactly hit the file-size boundary. <=64MiB is only size-eligible; trailing-byte/complexity policy may independently reject, and accepting this input is not an integrity proof.")
    return results


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--generated-dir", required=True, type=Path,
                        help="Directory containing the authored generator's small JPEGs")
    parser.add_argument("--output-dir", required=True, type=Path,
                        help="New/empty separate ignored research output directory")
    parser.add_argument("--include-size-boundaries", action="store_true",
                        help="Also write three ~64MiB files; default creates no large inputs")
    args = parser.parse_args()
    source = args.generated_dir.resolve(strict=True)
    output = args.output_dir.resolve()
    if not source.is_dir() or source == output:
        raise ValueError("Input and output must be separate directories")
    if output.exists() and (not output.is_dir() or any(output.iterdir())):
        raise ValueError("Output must not contain existing files")
    parents: dict[str, dict[str, Any]] = {}
    for name in PARENT_NAMES:
        path = source / name
        if path.is_symlink() or not path.is_file() or path.stat().st_size > MAX_PARENT_BYTES:
            raise ValueError(f"Missing, linked, non-file or too-large authored parent: {name}")
        data = path.read_bytes()
        markers, scans = parse_original(data)
        if not scans:
            raise ValueError(f"Authored parent has no SOS: {name}")
        parents[name] = {"name": name, "path": path, "data": data,
                         "sha256": sha256(data), "markers": markers, "scans": scans}
    output.mkdir(parents=True, exist_ok=True)
    with (output / "mutation-records.jsonl").open(
            "x", encoding="utf-8", newline="\n") as records:
        results = derive(parents, output, args.include_size_boundaries, records)
    for parent in parents.values():
        if file_hash(parent["path"]) != parent["sha256"]:
            raise ValueError("Read-only authored parent changed during derivation")
    missing_exact = {
        str(pixels): "NO_FACTOR_PAIR_WITH_BOTH_DIMENSIONS_IN_UINT16"
        for pixels in (39_999_999, 40_000_000, 40_000_001)
        if exact_dimensions(pixels) is None
    }
    manifest = {
        "schema": "lithic-m10-r1-byte-mutations-v1",
        "generator_script_sha256": file_hash(Path(__file__).resolve()),
        "script_arguments": {"include_size_boundaries": args.include_size_boundaries},
        "generated_status": "DERIVED_NOT_DECODE_VERIFIED",
        "locked_sample_policy": "NO_LOCKED_NAMES_READ_WRITTEN_OR_REGENERATED",
        "parent_integrity_after_run": "UNCHANGED",
        "parents": [{"fixture": item["name"], "bytes": len(item["data"]),
                     "sha256": item["sha256"], "scan_count": len(item["scans"])}
                    for item in parents.values()],
        "unencodable_exact_pixel_boundaries": missing_exact,
        "mutations": results,
        "large_size_cases": "GENERATED" if args.include_size_boundaries else "NOT_GENERATED_OPT_IN_REQUIRED",
        "limits": "Byte generation and hashes do not certify decoder behavior or prove malformed-image classification.",
    }
    manifest_path = output / "mutation-manifest.json"
    with manifest_path.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(manifest, stream, ensure_ascii=True, indent=2)
        stream.write("\n")
    print(json.dumps({"status": manifest["generated_status"],
                      "fixture_count": len(results),
                      "manifest": str(manifest_path),
                      "manifest_sha256": file_hash(manifest_path)}, ensure_ascii=True))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, StopIteration) as error:
        print(json.dumps({"status": "DERIVATION_FAILED", "error": str(error)},
                         ensure_ascii=True), file=sys.stderr)
        raise SystemExit(1)
