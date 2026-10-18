"""HOST_ONLY input identities and full-array payload model. Never decodes JPEG.

No subprocess, device, download, build or file-writing operations. The output is
not an integrity verdict, allocator measurement, memory cap or admission policy.
"""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import struct

INPUT_LIMIT = 64 * 1024 * 1024
PIXEL_LIMIT = 40000000


def _operands(a, b, bits):
    if bits not in (32, 64) or type(a) is not int or type(b) is not int or min(a, b) < 0:
        raise ValueError("Expected unsigned 32/64-bit model operands")
    maximum = (1 << bits) - 1
    if max(a, b) > maximum:
        raise OverflowError("Operand outside model word size")
    return maximum


def checked_add(a, b, bits):
    maximum = _operands(a, b, bits)
    if a > maximum - b:
        raise OverflowError("Model addition overflow")
    return a + b


def checked_mul(a, b, bits):
    maximum = _operands(a, b, bits)
    if b and a > maximum // b:
        raise OverflowError("Model multiplication overflow")
    return a * b


def read_frame(stream, file_bytes):
    """Read through the first SOF0/SOF2 only; do not consume entropy or EOI.

    A returned frame is deliberately possible for a truncated/corrupt image.
    Other JPEG coding modes are outside this model, not product-invalid inputs.
    """
    if not 0 < file_bytes <= INPUT_LIMIT:
        raise ValueError("File outside 64MiB model domain")
    stream.seek(0, 2)
    if stream.tell() != file_bytes:
        raise ValueError("File length differs from observation")
    stream.seek(0)

    def take(count):
        if count < 0 or stream.tell() + count > file_bytes:
            raise ValueError("Truncated header")
        value = stream.read(count)
        if len(value) != count:
            raise ValueError("Short header read")
        return value

    if take(2) != b"\xff\xd8":
        raise ValueError("Missing SOI")
    while stream.tell() < file_bytes:
        if take(1) != b"\xff":
            raise ValueError("Expected marker prefix")
        marker = take(1)[0]
        while marker == 0xFF:
            marker = take(1)[0]
        if marker in (0, 1, 0xD8, 0xD9, 0xDA) or 0xD0 <= marker <= 0xD7:
            raise ValueError("Unexpected marker before SOF")
        length = int.from_bytes(take(2), "big")
        if length < 2 or stream.tell() + length - 2 > file_bytes:
            raise ValueError("Invalid/truncated marker length")
        if marker in (0xC0, 0xC2):
            body = take(length - 2)
            if len(body) < 6:
                raise ValueError("Short SOF")
            precision, height, width, components = struct.unpack(">BHHB", body[:6])
            if not width or not height or not 1 <= components <= 4 or len(body) != 6 + 3 * components:
                raise ValueError("Invalid SOF dimensions/component count")
            ids, sampling = set(), []
            for offset in range(6, len(body), 3):
                identity, factors, _ = body[offset:offset + 3]
                h, v = factors >> 4, factors & 15
                if identity in ids or not 1 <= h <= 4 or not 1 <= v <= 4:
                    raise ValueError("Invalid/duplicate SOF component")
                ids.add(identity)
                sampling.append((h, v))
            return {"width": width, "height": height, "precision": precision,
                    "sampling": tuple(sampling), "coding": "progressive" if marker == 0xC2
                    else "baseline_scan_layout_unknown"}
        if 0xC0 <= marker <= 0xCF and marker not in (0xC4, 0xC8, 0xCC):
            raise ValueError("JPEG coding mode outside SOF0/SOF2 model")
        stream.seek(length - 2, 1)
    raise ValueError("No frame header")


def coefficient_payload(frame, pointer_bits):
    """CALCULATED full-buffer payload, excluding pools/allocator/other memory.

    Fixed 3.2.0 jdinput + jdcoefct pad blocks to sampling-factor multiples.
    64 coefficients x sizeof(JCOEF=2) = 128 bytes per block (R1 8-bit build).
    Baseline may be multiscan, so its full-buffer payload remains conditional.
    """
    width, height, sampling = frame["width"], frame["height"], frame["sampling"]
    if (frame["precision"] != 8 or frame["coding"] not in
            ("progressive", "baseline_scan_layout_unknown") or
            not 1 <= width <= 65535 or not 1 <= height <= 65535 or
            checked_mul(width, height, pointer_bits) > PIXEL_LIMIT or
            not 1 <= len(sampling) <= 4 or
            any(not 1 <= h <= 4 or not 1 <= v <= 4 for h, v in sampling)):
        raise ValueError("Frame outside the fixed 8-bit payload model")
    max_h, max_v = max(h for h, _ in sampling), max(v for _, v in sampling)
    mcu_columns = checked_add(width, 8 * max_h - 1, pointer_bits) // (8 * max_h)
    mcu_rows = checked_add(height, 8 * max_v - 1, pointer_bits) // (8 * max_v)
    coefficient_bytes, row_count = 0, 0
    for h, v in sampling:
        columns = checked_mul(mcu_columns, h, pointer_bits)
        rows = checked_mul(mcu_rows, v, pointer_bits)
        blocks = checked_mul(columns, rows, pointer_bits)
        coefficient_bytes = checked_add(coefficient_bytes, checked_mul(blocks, 128, pointer_bits), pointer_bits)
        row_count = checked_add(row_count, rows, pointer_bits)
    return {"coefficientBytes": coefficient_bytes, "arrayRows": row_count,
            "rowPointerBytes": checked_mul(row_count, pointer_bits // 8, pointer_bits),
            "allocationCondition": "IF_FULL_COEFFICIENT_BUFFER",
            "totalNativeBytes": None, "integrity": "NOT_EVALUATED",
            "evidenceKind": "CALCULATED_NOT_NATIVE_MEASURED"}


def verify_locked_tree(root, entries):
    """Verify only listed inputs, not an entire source tree or build graph."""
    root = Path(root).resolve(strict=True)
    seen = set()
    if not entries:
        raise ValueError("Empty source/input lock")
    for entry in entries:
        name = entry["path"]
        relative = PurePosixPath(name)
        if (not name or relative.is_absolute() or "\\" in name or ":" in name or
                any(part in ("", ".", "..") for part in name.split("/")) or name in seen):
            raise ValueError("Unsafe/duplicate locked path")
        seen.add(name)
        path = root.joinpath(*relative.parts)
        if any(root.joinpath(*relative.parts[:n]).is_symlink() for n in range(1, len(relative.parts) + 1)):
            raise ValueError("Linked input is not a locked regular file")
        if not path.is_file() or not path.resolve().is_relative_to(root):
            raise ValueError("Missing/outside locked input: " + name)
        if path.stat().st_size != entry["bytes"] or not 0 <= entry["bytes"] <= INPUT_LIMIT:
            raise ValueError("Locked length mismatch: " + name)
        digest = hashlib.sha256()
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(65536), b""):
                digest.update(chunk)
        if digest.hexdigest() != entry["sha256"]:
            raise ValueError("Locked SHA mismatch: " + name)
    return sorted(seen)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--r1-worktree", required=True, type=Path)
    args = parser.parse_args()
    here = Path(__file__).resolve().parent
    lock = json.loads((here / "source-lock.json").read_text(encoding="utf-8"))
    r1_build = args.r1_worktree / "build/m10-jpeg-r1"
    verified = {
        "source": verify_locked_tree(r1_build / "source/libjpeg-turbo-3.2.0", lock["source"]),
        "archive": verify_locked_tree(r1_build, lock["archive"]),
        "research": verify_locked_tree(here.parents[2], lock["research"]),
        "fixtures": verify_locked_tree(args.r1_worktree, lock["fixtures"]),
    }
    samples = []
    for entry in lock["fixtures"]:
        with (args.r1_worktree / entry["path"]).open("rb") as stream:
            observed = read_frame(stream, entry["bytes"])
        samples.append({**entry, "frame": observed,
                        "model32": coefficient_payload(observed, 32),
                        "model64": coefficient_payload(observed, 64)})
    print(json.dumps({"sourceVersion": "3.2.0", "verifiedInputs": verified,
                      "samples": samples, "nativeAllocationCoverage": "NOT_RUN",
                      "nativeFailureRelease": "NOT_RUN", "totalNativeBudget": "UNKNOWN",
                      "e01Status": "BLOCKED_HOST_NATIVE_TOOLCHAIN",
                      "note": "Header/model results cannot validate JPEG integrity or release memory."}, indent=2))


if __name__ == "__main__":
    main()
