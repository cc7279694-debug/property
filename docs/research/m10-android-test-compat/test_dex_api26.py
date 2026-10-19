"""Small host-only tests for the declared-signature gate; no Android commands."""
import hashlib
import struct
import unittest
import zlib

from dex_api26 import Dex, DexError, check_class


def uleb(value):
    out = bytearray()
    while value >= 128:
        out.append((value & 127) | 128)
        value >>= 7
    out.append(value)
    return out


def fixture(*, synthetic=True, api28_parameter=True):
    """Make one checksummed DEX class with one declared encoded method."""
    strings = ["Lsample/Test;", "Ljava/lang/Object;", "V", "lambda",
               "Landroid/graphics/ImageDecoder$DecodeException;" if api28_parameter else "Ljava/lang/String;"]
    string_ids = 112
    type_ids = string_ids + 4 * len(strings)
    proto_ids = type_ids + 4 * 4
    method_ids = proto_ids + 12
    class_defs = method_ids + 8
    data_off = class_defs + 32
    data = bytearray(data_off)
    offsets = []
    for item in strings:
        offsets.append(len(data))
        data.extend(uleb(len(item)) + item.encode() + b"\0")
    while len(data) % 4:
        data.append(0)
    params = len(data)
    data.extend(struct.pack("<IH", 1, 3))
    class_data = len(data)
    data.extend(b"\0\0\1\0" + uleb(0) + uleb(0x1009 if synthetic else 9) + uleb(0))
    data[:8] = b"dex\n035\0"
    struct.pack_into("<I", data, 32, len(data))
    struct.pack_into("<II", data, 36, 112, 0x12345678)
    struct.pack_into("<II", data, 56, len(strings), string_ids)
    struct.pack_into("<II", data, 64, 4, type_ids)
    struct.pack_into("<II", data, 72, 1, proto_ids)
    struct.pack_into("<II", data, 88, 1, method_ids)
    struct.pack_into("<II", data, 96, 1, class_defs)
    struct.pack_into("<II", data, 104, len(data) - data_off, data_off)
    for i, offset in enumerate(offsets):
        struct.pack_into("<I", data, string_ids + i * 4, offset)
    for i, string_index in enumerate([0, 1, 2, 4]):
        struct.pack_into("<I", data, type_ids + i * 4, string_index)
    struct.pack_into("<III", data, proto_ids, 2, 2, params)
    struct.pack_into("<HHI", data, method_ids, 0, 0, 3)
    struct.pack_into("<8I", data, class_defs, 0, 1, 1, 0, 0xffffffff, 0, class_data, 0)
    data[12:32] = hashlib.sha1(data[32:]).digest()
    struct.pack_into("<I", data, 8, zlib.adler32(data[12:]) & 0xffffffff)
    return bytes(data)


class DexDeclaredSignatureTests(unittest.TestCase):
    def test_synthetic_method_is_actually_declared_and_audited(self):
        row = Dex(fixture(), "fixture.dex").classes()[0]
        self.assertTrue(row["declaredMethods"][0]["synthetic"])
        self.assertEqual(row["declaredMethods"][0]["parameters"],
                         ["Landroid/graphics/ImageDecoder$DecodeException;"])
        self.assertEqual(check_class(row, {"android/graphics/ImageDecoder$DecodeException": 28}, 26)["status"], "FAIL")

    def test_api26_compatible_signature_passes(self):
        row = Dex(fixture(api28_parameter=False), "fixture.dex").classes()[0]
        self.assertEqual(check_class(row, {}, 26)["status"], "PASS")

    def test_api28_return_value_is_rejected(self):
        row = Dex(fixture(api28_parameter=False), "fixture.dex").classes()[0]
        row["declaredMethods"][0]["returns"] = "Landroid/graphics/ImageDecoder;"
        result = check_class(row, {"android/graphics/ImageDecoder": 28}, 26)
        self.assertEqual(result["status"], "FAIL")
        self.assertIn("return", result["violations"][0]["origin"])

    def test_api28_array_field_is_rejected(self):
        row = Dex(fixture(api28_parameter=False), "fixture.dex").classes()[0]
        row["declaredFields"] = [{"name": "unsafe", "type": "[[Landroid/graphics/ImageDecoder;"}]
        self.assertEqual(check_class(row, {"android/graphics/ImageDecoder": 28}, 26)["status"], "FAIL")

    def test_unknown_android_type_blocks(self):
        row = Dex(fixture(), "fixture.dex").classes()[0]
        result = check_class(row, {}, 26)
        self.assertEqual(result["status"], "UNKNOWN")
        self.assertEqual(result["violations"][0]["since"], "UNKNOWN")

    def test_super_and_interface_are_checked(self):
        row = Dex(fixture(api28_parameter=False), "fixture.dex").classes()[0]
        row["superclass"] = "Landroid/graphics/ImageDecoder;"
        row["interfaces"] = ["Landroid/graphics/ImageDecoder$OnHeaderDecodedListener;"]
        result = check_class(row, {"android/graphics/ImageDecoder": 28,
                                  "android/graphics/ImageDecoder$OnHeaderDecodedListener": 28}, 26)
        self.assertEqual(len(result["violations"]), 2)

    def test_corrupt_checksum_rejected(self):
        raw = bytearray(fixture())
        raw[-1] ^= 1
        with self.assertRaises(DexError):
            Dex(bytes(raw), "fixture.dex")

    def test_truncated_dex_rejected(self):
        with self.assertRaises(DexError):
            Dex(fixture()[:-1], "fixture.dex")

    def test_uleb_overflow_rejected(self):
        parsed = Dex(fixture(), "fixture.dex")
        parsed.raw = b"\xff\xff\xff\xff\x10"
        with self.assertRaises(DexError):
            parsed.uleb(0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
