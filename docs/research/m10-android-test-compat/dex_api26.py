"""Host-only DEX declared-member API gate. No build, device, or third-party dependency.

This intentionally audits class definitions, rather than all method_ids (references).
It covers signatures that JUnit's getDeclaredMethods may resolve, fields and class
hierarchy. It does NOT prove all transitive code is API26 safe or execute a verifier.
The separate API28 helper is reported, never included in API26 class-load admission.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import xml.etree.ElementTree as ET
import zipfile
import zlib


TEST_CLASSES = (
    "com.cooper.lithic.data.media.M10WorkerRetirementReviewTest",
    "com.cooper.lithic.data.media.M10JpegIntegrityTest",
    "com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest",
    "com.cooper.lithic.data.media.M10StageV2CompatibilityTest",
)


class DexError(ValueError):
    pass


class Dex:
    def __init__(self, raw: bytes, name: str):
        self.raw, self.name = raw, name
        if len(raw) < 112 or raw[:4] != b"dex\n" or raw[7] != 0:
            raise DexError("missing standard DEX header")
        if raw[4:7] not in (b"035", b"037", b"038", b"039", b"040"):
            raise DexError("unsupported DEX version")
        if self.u32(32) != len(raw) or self.u32(36) != 112 or self.u32(40) != 0x12345678:
            raise DexError("DEX length/header/endian mismatch")
        if self.u32(8) != zlib.adler32(raw[12:]) & 0xffffffff:
            raise DexError("DEX Adler32 mismatch")
        if raw[12:32] != hashlib.sha1(raw[32:]).digest():
            raise DexError("DEX SHA1 mismatch")
        self.strings = []
        for (offset,) in self.table(56, 4, "<I"):
            length, start = self.uleb(offset)
            end = raw.find(b"\0", start)
            if end < start:
                raise DexError("unterminated string")
            try:
                text = raw[start:end].replace(b"\xc0\x80", b"\0").decode("utf-8", "surrogatepass")
            except UnicodeDecodeError as exc:
                raise DexError("invalid modified UTF8") from exc
            if len(text.encode("utf-16-le", "surrogatepass")) // 2 != length:
                raise DexError("string UTF16 size mismatch")
            self.strings.append(text)
        self.types = [self.index(self.strings, i) for (i,) in self.table(64, 4, "<I")]
        self.protos = []
        for shorty, returns, parameters in self.table(72, 12, "<III"):
            self.index(self.strings, shorty)
            self.protos.append((self.index(self.types, returns), self.type_list(parameters)))
        self.fields = list(self.table(80, 8, "<HHI"))
        self.methods = list(self.table(88, 8, "<HHI"))

    def bounds(self, offset, size):
        if offset < 0 or size < 0 or offset > len(self.raw) or size > len(self.raw) - offset:
            raise DexError("DEX range outside file")

    def u32(self, offset):
        self.bounds(offset, 4)
        return struct.unpack_from("<I", self.raw, offset)[0]

    def uleb(self, offset):
        value = 0
        for position in range(5):
            self.bounds(offset, 1)
            byte = self.raw[offset]
            offset += 1
            if position == 4 and byte > 15:
                raise DexError("uleb128 overflow")
            value |= (byte & 127) << (7 * position)
            if not byte & 128:
                return value, offset
        raise DexError("unterminated uleb128")

    def table(self, header_offset, width, fmt):
        size, offset = self.u32(header_offset), self.u32(header_offset + 4)
        if size and offset < 112:
            raise DexError("table overlaps header")
        self.bounds(offset, size * width)
        return [struct.unpack_from(fmt, self.raw, offset + i * width) for i in range(size)]

    @staticmethod
    def index(values, index):
        if not 0 <= index < len(values):
            raise DexError("DEX index outside table")
        return values[index]

    def type_list(self, offset):
        if not offset:
            return []
        size = self.u32(offset)
        self.bounds(offset + 4, size * 2)
        return [self.index(self.types, struct.unpack_from("<H", self.raw, offset + 4 + 2 * i)[0]) for i in range(size)]

    def classes(self):
        rows = []
        for class_index, flags, superclass, interfaces, _, _, class_data, _ in self.table(96, 32, "<8I"):
            row = {"dex": self.name, "descriptor": self.index(self.types, class_index),
                   "accessFlags": hex(flags),
                   "superclass": None if superclass == 0xffffffff else self.index(self.types, superclass),
                   "interfaces": self.type_list(interfaces), "declaredMethods": [], "declaredFields": []}
            if class_data:
                counts = []
                offset = class_data
                for _ in range(4):
                    count, offset = self.uleb(offset)
                    counts.append(count)
                for count in counts[:2]:
                    field_index = 0
                    for _ in range(count):
                        delta, offset = self.uleb(offset)
                        access, offset = self.uleb(offset)
                        field_index += delta
                        owner, type_index, name_index = self.index(self.fields, field_index)
                        if owner != class_index:
                            raise DexError("declared field owner mismatch")
                        row["declaredFields"].append({"name": self.index(self.strings, name_index),
                                                      "type": self.index(self.types, type_index), "accessFlags": hex(access)})
                for count in counts[2:]:
                    method_index = 0
                    for _ in range(count):
                        delta, offset = self.uleb(offset)
                        access, offset = self.uleb(offset)
                        code, offset = self.uleb(offset)
                        method_index += delta
                        owner, proto_index, name_index = self.index(self.methods, method_index)
                        if owner != class_index:
                            raise DexError("declared method owner mismatch")
                        returns, parameters = self.index(self.protos, proto_index)
                        name = self.index(self.strings, name_index)
                        row["declaredMethods"].append({"name": name, "returns": returns,
                            "parameters": parameters, "descriptor": "(" + "".join(parameters) + ")" + returns,
                            "synthetic": bool(access & 0x1000), "accessFlags": hex(access), "codeOffset": code})
            names = [(m["name"], m["descriptor"]) for m in row["declaredMethods"]]
            if len(set(names)) != len(names):
                raise DexError("duplicate declared methods")
            rows.append(row)
        return rows


def check_class(row, api_database, api):
    references = []
    if row["superclass"]:
        references.append(("superclass", row["superclass"]))
    references.extend(("interface", t) for t in row["interfaces"])
    references.extend(("field:" + f["name"], f["type"]) for f in row["declaredFields"])
    for method in row["declaredMethods"]:
        references.append(("return:" + method["name"], method["returns"]))
        references.extend(("parameter:" + method["name"] + ":" + str(i), t) for i, t in enumerate(method["parameters"]))
    checked, violations = [], []
    for origin, descriptor in references:
        clean = descriptor.lstrip("[")
        if not clean.startswith("Landroid/"):
            continue
        if not clean.endswith(";"):
            raise DexError("invalid Android type descriptor")
        since = api_database.get(clean[1:-1])
        detail = {"origin": origin, "descriptor": descriptor, "since": "UNKNOWN" if since is None else since}
        checked.append(detail)
        if since is None or since > api:
            violations.append(detail)
    status = "FAIL" if any(v["since"] != "UNKNOWN" for v in violations) else "UNKNOWN" if violations else "PASS"
    return {**row, "status": status, "androidTypeChecks": checked, "violations": violations}


def descriptor(name):
    return "L" + name.replace(".", "/") + ";"


def audit(apk, api_xml, targets, helpers, *, expected_sha=None, api=26):
    apk = Path(apk)
    raw_apk = apk.read_bytes()
    apk_sha = hashlib.sha256(raw_apk).hexdigest()
    if expected_sha is not None and apk_sha != expected_sha:
        raise DexError("APK SHA does not match locked input")
    raw_xml = Path(api_xml).read_bytes()
    root = ET.fromstring(raw_xml)
    database = {node.attrib["name"]: int(node.attrib.get("since", "1")) for node in root.findall("class")}
    all_classes, dex_rows = {}, []
    with zipfile.ZipFile(apk) as archive:
        names = [n for n in archive.namelist() if re.fullmatch(r"classes(?:[2-9]|[1-9][0-9]+)?\.dex", n)]
        if not names or len(names) != len(set(names)):
            raise DexError("missing or duplicate DEX entries")
        for name in sorted(names):
            payload = archive.read(name)
            parsed = Dex(payload, name)
            for row in parsed.classes():
                if row["descriptor"] in all_classes:
                    raise DexError("duplicate class definition across DEX files")
                all_classes[row["descriptor"]] = row
            dex_rows.append({"entry": name, "bytes": len(payload), "sha256": hashlib.sha256(payload).hexdigest()})
    results = []
    for name in targets:
        if descriptor(name) not in all_classes:
            results.append({"descriptor": descriptor(name), "status": "UNKNOWN", "reason": "CLASS_MISSING"})
        else:
            results.append(check_class(all_classes[descriptor(name)], database, api))
    helper_rows = []
    for name in helpers:
        key = descriptor(name)
        members = [row for desc, row in all_classes.items() if desc == key or desc.startswith(key[:-1] + "$" )]
        helper_rows.append({"class": name, "status": "NOT_LOAD_ON_API26" if members else "MISSING",
                            "meaning": "Declared as API28-only by the test design; parser does not prove execution guarding.",
                            "definitions": [check_class(row, database, api) for row in members]})
    return {"status": "PASS" if all(r["status"] == "PASS" for r in results) and
            all(r["status"] != "MISSING" for r in helper_rows) else "FAIL",
        "scope": "API26_DECLARED_CLASS_METADATA_ONLY", "apiLevel": api,
        "apk": {"path": str(apk), "bytes": len(raw_apk), "sha256": apk_sha},
        "apiDatabase": {"path": str(api_xml), "sha256": hashlib.sha256(raw_xml).hexdigest(), "classes": len(database)},
        "dexEntries": dex_rows, "targets": results, "api28Helpers": helper_rows,
        "limitations": ["Does not execute Android reflection or verify transitive code paths.",
            "Only Android framework class introduction versions in the local official SDK XML are evaluated.",
            "Annotations, method bodies, exception catch tables, transitive non-Android types and member-level API introductions are not admission proofs.",
            "API28 helper references are intentional behind SDK_INT guard; no claim that all DEX API28 code references are absent."]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True)
    parser.add_argument("--api-xml", required=True)
    parser.add_argument("--expected-sha")
    parser.add_argument("--extra-class", action="append", default=[])
    parser.add_argument("--helper", action="append", default=[])
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    result = audit(args.apk, args.api_xml, TEST_CLASSES + tuple(args.extra_class), args.helper, expected_sha=args.expected_sha)
    path = Path(args.out)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(result, stream, ensure_ascii=False, indent=2)
        stream.write("\n")
    print(json.dumps({"status": result["status"], "apkSHA256": result["apk"]["sha256"],
                      "targetClasses": len(result["targets"]), "violations": sum(len(r.get("violations", [])) for r in result["targets"]),
                      "output": str(path)}))
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
