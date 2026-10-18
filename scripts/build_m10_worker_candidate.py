#!/usr/bin/env python3
"""Verify/reuse the previously compiled four-ABI candidate; no network/device calls.

This packages only exact locked binaries and license assets under app/build.
To reproduce those binaries, use docs/research/m10-jpeg-accelerated/build_android.py
with the fixed archive and installed NDK/CMake in a fresh ASCII build directory.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import shutil
import subprocess
import tarfile

ARCHIVE_SHA = "6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e"
NDK = "28.2.13676358"
LIBRARY = "libstrictjpeg_candidate.so"
CODE = (
    "docs/research/m10-jpeg-e01-native/strict_probe.c",
    "docs/research/m10-jpeg-e01-native/strict_core.h",
    "docs/research/m10-jpeg-e01-native/budget.c",
    "docs/research/m10-jpeg-e01-native/budget.h",
    "docs/research/m10-jpeg-e01-native/jmem_budget.c",
    "docs/research/m10-jpeg-accelerated/jni_bridge.c",
    "docs/research/m10-jpeg-accelerated/StrictJpegCandidate.java",
    "docs/research/m10-jpeg-accelerated/CMakeLists.txt",
)


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def same_source(current, compiled):
    if current == compiled:
        return "EXACT"
    require(current.replace(b"\r\n", b"\n") == compiled.replace(b"\r\n", b"\n"),
            "Compiled source tokens differ from the current candidate")
    return "CRLF_LF_ONLY"


def checked_child(parent, name):
    candidate = parent / name
    require(candidate.resolve().parent == parent.resolve(), "Unsafe child path")
    require(not candidate.is_symlink(), "Symlink build input/output rejected")
    return candidate


def check_output_shape(output, abis):
    directories = {"jniLibs", "assets", "assets/third_party",
                   "assets/third_party/libjpeg-turbo-3.2.0"}
    directories.update("jniLibs/" + abi for abi in abis)
    files = {"assembly.json"}
    files.update("jniLibs/" + abi + "/" + LIBRARY for abi in abis)
    files.update("assets/third_party/libjpeg-turbo-3.2.0/" + name for name in
                 ("LICENSE.md", "README.ijg", "LITHIC_CHANGES.txt"))
    if output.exists():
        for item in output.rglob("*"):
            relative = item.relative_to(output).as_posix()
            require(not item.is_symlink(), "Generated output symlink rejected")
            require((item.is_dir() and relative in directories) or
                    (item.is_file() and relative in files), "Unknown generated output: " + relative)


def check_java(repo):
    original = (repo / CODE[6]).read_bytes()
    application = (repo / "app/src/main/java/com/cooper/lithic/research/StrictJpegCandidate.java").read_bytes()
    same_source(application, original)
    text = application.decode("utf-8")
    require("package com.cooper.lithic.research;" in text and
            "public static native long[] validateReadonlyFd(int fd, long limit);" in text,
            "JNI declaration package/signature mismatch")
    require("System.load" not in text, "JNI declaration must not auto-load on the Host")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", type=Path, required=True)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--native-root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[1]
    native = args.native_root.resolve(strict=True)
    archive = args.archive.resolve(strict=True)
    output = args.output.resolve()
    require(output == (repo / "app/build/generated/m10StrictJpeg").resolve(),
            "Output must be the candidate's ignored generated directory")
    require(digest(archive.read_bytes()) == ARCHIVE_SHA, "Fixed official archive SHA mismatch")
    require(not native.is_symlink(), "Native root symlink rejected")
    fixed = json.loads((repo / "docs/research/m10-jpeg-accelerated/RESULTS.json").read_text(encoding="utf-8"))
    expected = fixed["androidCompile"]
    built = json.loads((native / "summary.json").read_text(encoding="utf-8"))
    require(built["status"] == "COMPILED_NOT_ANDROID_RUN" and built["ndk"] == NDK and
            built["cmake"] == "3.22.1" and built["archiveSHA256"] == ARCHIVE_SHA,
            "Previous build status/toolchain/source mismatch")
    records = json.loads((native / "source-inputs.json").read_text(encoding="utf-8"))
    record_map = {item["source"].replace("\\", "/"): item for item in records}
    locked_map = {item["source"]: item for item in expected["sourceInputs"]}
    require(len(records) == 9 and len(record_map) == 9 and
            set(record_map) == set(CODE) | {"docs/research/m10-jpeg-accelerated/build_android.py"},
            "Unexpected/missing/duplicate compiled input")
    source_checks = []
    for name in CODE:
        compiled = checked_child(native / "inputs", Path(name).name).read_bytes()
        current = (repo / name).read_bytes()
        record = record_map[name]
        require(digest(compiled) == record["sha256"] == locked_map[name]["sha256"] and
                len(compiled) == record["bytes"] == locked_map[name]["bytes"],
                "Locked compiled input mismatch: " + name)
        source_checks.append({"source": name, "compiledSHA256": digest(compiled),
                              "currentSHA256": digest(current), "match": same_source(current, compiled)})
    check_java(repo)
    sdk = args.sdk.resolve(strict=True)
    toolchain = sdk / "ndk" / NDK
    properties = (toolchain / "source.properties").read_text(encoding="utf-8")
    require(re.search(r"^Pkg.Revision\s*=\s*" + re.escape(NDK) + r"\s*$", properties, re.MULTILINE),
            "Installed NDK version mismatch")
    readelf = toolchain / "toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-readelf.exe"
    require(readelf.is_file(), "Existing NDK ELF auditor missing")
    audit_path = repo / "docs/research/m10-jpeg-accelerated/build_android.py"
    spec = importlib.util.spec_from_file_location("candidate_elf_audit", audit_path)
    auditor = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(auditor)
    require(set(built["abis"]) == set(expected["artifacts"]) == set(auditor.ABIS),
            "Four ABI identity mismatch")
    artifacts = []
    for abi, machine in auditor.ABIS.items():
        binary = native / (abi + "-candidate") / LIBRARY
        data = binary.read_bytes()
        locked = expected["artifacts"][abi]
        require(digest(data) == locked["sha256"] == built["abis"][abi]["sha256"] and
                len(data) == locked["bytes"], "Locked ABI binary mismatch: " + abi)
        def inspect(*arguments):
            command = [str(readelf), *arguments, str(binary)]
            result = subprocess.run(command, capture_output=True, encoding="utf-8", errors="strict",
                                    timeout=30, check=False)
            require(result.returncode == 0, "ELF inspection failed: " + abi)
            return result.stdout
        elf = inspect("-h", "-l", "-d", "-W")
        symbols = inspect("--dyn-syms", "-W")
        audit = auditor.audit(elf, symbols, machine)
        all_symbols = inspect("--symbols", "-W")
        require(not re.search(r"\s+main\s*$", all_symbols, re.MULTILINE), "Unexpected main symbol")
        artifacts.append({"abi": abi, "bytes": len(data), "sha256": digest(data), "audit": audit})
    # Validate all inputs/stale outputs before generating any packaging output.
    check_output_shape(output, set(auditor.ABIS))
    output.mkdir(parents=True, exist_ok=True)
    for artifact in artifacts:
        destination = output / "jniLibs" / artifact["abi"]
        destination.mkdir(parents=True, exist_ok=True)
        target = checked_child(destination, LIBRARY)
        shutil.copyfile(native / (artifact["abi"] + "-candidate") / LIBRARY, target)
        require(digest(target.read_bytes()) == artifact["sha256"], "Packaging copy hash mismatch")
    licenses = output / "assets/third_party/libjpeg-turbo-3.2.0"
    licenses.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive, "r:gz") as upstream:
        for name in ("LICENSE.md", "README.ijg"):
            member = upstream.getmember("libjpeg-turbo-3.2.0/" + name)
            require(member.isfile() and 0 < member.size < 65536, "Unexpected license archive member")
            stream = upstream.extractfile(member)
            require(stream is not None, "Missing source license")
            checked_child(licenses, name).write_bytes(stream.read())
    notice = ("This software is based in part on the work of the Independent JPEG Group.\n"
              "libjpeg-turbo 3.2.0: https://github.com/libjpeg-turbo/libjpeg-turbo/releases/tag/3.2.0\n"
              "Official source archive SHA-256: " + ARCHIVE_SHA + "\n"
              "Candidate modification: jmemnobs.c replaced by the LITHIC research budget backend\n"
              "docs/research/m10-jpeg-e01-native/jmem_budget.c; original decoder source otherwise retained.\n"
              "SIMD/TurboJPEG/tools disabled; application packages only libjpeg API and the JNI validator.\n")
    checked_child(licenses, "LITHIC_CHANGES.txt").write_text(notice, encoding="utf-8")
    report = {"status": "PACKAGING_VERIFIED_NOT_ANDROID_RUN", "archiveSHA256": ARCHIVE_SHA,
              "sourceInputs": source_checks, "artifacts": artifacts,
              "driver": "Original driver changed audit parsing after the historical build; not a compiled input",
              "licenseAssets": [{"name": path.name, "bytes": path.stat().st_size,
                                  "sha256": digest(path.read_bytes())}
                                 for path in sorted(licenses.iterdir())]}
    (output / "assembly.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"status": report["status"], "abis": [item["abi"] for item in artifacts],
                      "report": str(output / "assembly.json")}))


if __name__ == "__main__":
    main()
