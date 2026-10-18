#!/usr/bin/env python3
"""Isolated compile/audit only; never installs, downloads or runs Android code."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tarfile

NDK = "28.2.13676358"
CMAKE = "3.22.1"
ARCHIVE_SHA = "6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e"
BACKEND_SHA = "328ff841f437fa7c2de846c4e295e9145cc628b9041aed718a06eb087c55786c"
JNI_NAME = "Java_com_cooper_lithic_research_StrictJpegCandidate_validateReadonlyFd"
ABIS = {"arm64-v8a": "AArch64", "armeabi-v7a": "ARM",
        "x86": "Intel 80386", "x86_64": "Advanced Micro Devices X86-64"}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def properties(path):
    return dict(tuple(part.strip() for part in line.split("=", 1))
                for line in path.read_text(encoding="utf-8").splitlines()
                if "=" in line and not line.lstrip().startswith("#"))


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")


def audit(elf, symbols, machine):
    actual_machine = re.search(r"^\s*Machine:\s*(.+)$", elf, re.MULTILINE)
    require(actual_machine and actual_machine.group(1).strip() == machine, "ELF machine mismatch")
    aligns = [int(line.split()[-1], 16) for line in elf.splitlines()
              if line.lstrip().startswith("LOAD ")]
    require(aligns and all(value >= 16384 for value in aligns), "ELF LOAD alignment below 16KiB")
    needed = re.findall(r"\(NEEDED\).*?Shared library: \[([^]]+)\]", elf)
    require("libc.so" in needed and set(needed) <= {"libc.so", "libm.so", "libdl.so"},
            "Unexpected or absent Android runtime dependency")
    require(not re.search(r"\s+main\s*$", symbols, re.MULTILINE), "Unexpected main symbol")
    exports = []
    jni_definition = False
    for line in symbols.splitlines():
        match = re.match(r"\s*\d+:\s+\S+\s+\S+\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)(?:\s+\(\d+\))?\s*$", line)
        if not match:
            continue
        kind, binding, visibility, section, name = match.groups()
        if (name == JNI_NAME and kind == "FUNC" and binding == "GLOBAL" and
                visibility == "DEFAULT" and section.isdecimal()):
            jni_definition = True
        if (section not in {"UND", "UNDEF"} and
                binding in {"GLOBAL", "WEAK", "UNIQUE", "GNU_UNIQUE"} and
                visibility in {"DEFAULT", "PROTECTED"}):
            exports.append(name)
    require(jni_definition, "JNI symbol not present as a defined default-visible global function")
    require(set(exports) == {JNI_NAME}, "Unexpected defined visible export")
    return {"machine": machine, "loadAlignments": aligns, "needed": needed,
            "definedVisibleExports": sorted(set(exports))}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", type=Path, required=True)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--build-root", type=Path, required=True)
    args = parser.parse_args()
    root = args.build_root.resolve()
    research = Path(__file__).resolve().parent
    repo = research.parents[2]
    require(not root.exists(), "Refusing an existing build root; preserve prior evidence")
    require(str(root).isascii() and not any(character.isspace() for character in str(root)),
            "Build root must have a fresh ASCII path without whitespace")
    require(not root.is_relative_to(repo) or "build" in root.relative_to(repo).parts,
            "In-repository output must be below an ignored build directory")
    require(sys.version_info >= (3, 12), "Python 3.12+ tar data filter is required")
    sdk, archive = args.sdk.resolve(), args.archive.resolve()
    ndk, cmake_root = sdk / "ndk" / NDK, sdk / "cmake" / CMAKE
    require(properties(ndk / "source.properties").get("Pkg.Revision", "").strip() == NDK,
            "NDK source.properties version mismatch")
    require(properties(cmake_root / "source.properties").get("Pkg.Revision", "").strip() == CMAKE,
            "CMake source.properties version mismatch")
    require(archive.is_file() and sha(archive) == ARCHIVE_SHA, "Locked source archive SHA mismatch")
    cmake, ninja = cmake_root / "bin/cmake.exe", cmake_root / "bin/ninja.exe"
    toolchain = ndk / "build/cmake/android.toolchain.cmake"
    toolbin = ndk / "toolchains/llvm/prebuilt/windows-x86_64/bin"
    readelf = toolbin / "llvm-readelf.exe"
    for tool in (cmake, ninja, toolchain, readelf, toolbin / "clang.exe"):
        require(tool.is_file(), "Missing existing tool: " + str(tool))
    root.mkdir(parents=True)
    logs = root / "logs"
    logs.mkdir()
    commands = []
    summary = {"status": "FAILED_NOT_ANDROID_RUN", "ndk": NDK, "cmake": CMAKE,
               "archiveSHA256": ARCHIVE_SHA, "commands": commands, "abis": {}}
    environment = os.environ.copy()
    environment.update({"ANDROID_HOME": str(sdk), "ANDROID_SDK_ROOT": str(sdk)})

    def run(label, arguments):
        log = logs / (str(len(commands)).zfill(2) + "-" + label + ".log")
        record = {"label": label, "arguments": [str(value) for value in arguments],
                  "exitCode": None, "log": str(log)}
        commands.append(record)
        try:
            with log.open("wb") as output:
                result = subprocess.run(record["arguments"], cwd=root, env=environment,
                                        stdout=output, stderr=subprocess.STDOUT,
                                        timeout=900, check=False)
            record["exitCode"] = result.returncode
        except Exception as error:
            record["failure"] = str(error)
            raise
        finally:
            if log.is_file():
                record["logSHA256"] = sha(log)
            write_json(root / "summary.json", summary)
        require(result.returncode == 0, label + " failed; inspect " + str(log))
        return log.read_text(encoding="utf-8", errors="replace")

    try:
        require("cmake version " + CMAKE in run("cmake-version", [cmake, "--version"]),
                "CMake executable version mismatch")
        summary["ninjaVersion"] = run("ninja-version", [ninja, "--version"]).strip()
        summary["clangVersion"] = run("clang-version", [toolbin / "clang.exe", "--version"]).splitlines()[0]
        inputs = root / "inputs"
        inputs.mkdir()
        sources = [research.parent / "m10-jpeg-e01-native" / name for name in
                   ("strict_probe.c", "strict_core.h", "budget.c", "budget.h", "jmem_budget.c")]
        sources += [research / name for name in
                    ("jni_bridge.c", "StrictJpegCandidate.java", "CMakeLists.txt", "build_android.py")]
        records = []
        for source_file in sources:
            destination = inputs / source_file.name
            shutil.copyfile(source_file, destination)
            require(sha(source_file) == sha(destination), "Raw source copy mismatch")
            records.append({"source": str(source_file.relative_to(repo)),
                            "bytes": destination.stat().st_size, "sha256": sha(destination)})
        write_json(root / "source-inputs.json", records)
        upstream = root / "upstream"
        upstream.mkdir()
        with tarfile.open(archive, "r:gz") as source_tar:
            source_tar.extractall(upstream, filter="data")
        source = upstream / "libjpeg-turbo-3.2.0"
        backend = source / "src/jmemnobs.c"
        require(sha(backend) == BACKEND_SHA, "Original upstream backend SHA mismatch")
        shutil.copyfile(inputs / "jmem_budget.c", backend)
        shutil.copyfile(inputs / "budget.h", backend.parent / "budget.h")
        require(sha(backend) == sha(inputs / "jmem_budget.c"), "Replacement backend copy mismatch")
        for abi, machine in ABIS.items():
            jpeg_build, candidate_build = root / (abi + "-jpeg"), root / (abi + "-candidate")
            common = ["-G", "Ninja", "-DCMAKE_MAKE_PROGRAM=" + ninja.as_posix(),
                      "-DCMAKE_TOOLCHAIN_FILE=" + toolchain.as_posix(),
                      "-DANDROID_ABI=" + abi, "-DANDROID_PLATFORM=android-26",
                      "-DCMAKE_SYSTEM_VERSION=26", "-DANDROID_STL=none", "-DCMAKE_BUILD_TYPE=Release",
                      "-DCMAKE_POSITION_INDEPENDENT_CODE=ON",
                      "-DCMAKE_C_FLAGS=-fvisibility=hidden -ffile-prefix-map=" + root.as_posix() + "=/candidate"]
            run(abi + "-jpeg-configure", [cmake, "-S", source, "-B", jpeg_build, *common,
                "-DENABLE_SHARED=OFF", "-DENABLE_STATIC=ON", "-DWITH_TURBOJPEG=OFF",
                "-DWITH_SIMD=OFF", "-DWITH_TOOLS=OFF", "-DWITH_TESTS=OFF", "-DWITH_FUZZ=OFF"])
            run(abi + "-jpeg-build", [cmake, "--build", jpeg_build, "--target", "jpeg-static", "--parallel", "2"])
            library = jpeg_build / "libjpeg.a"
            require(library.is_file(), "Expected static libjpeg archive missing")
            run(abi + "-candidate-configure", [cmake, "-S", inputs, "-B", candidate_build, *common,
                "-DJPEG_ARCHIVE=" + library.as_posix(), "-DJPEG_SOURCE_DIR=" + source.as_posix(),
                "-DJPEG_BINARY_DIR=" + jpeg_build.as_posix()])
            run(abi + "-candidate-build", [cmake, "--build", candidate_build,
                                         "--target", "strictjpeg_candidate", "--parallel", "2"])
            binary = candidate_build / "libstrictjpeg_candidate.so"
            elf = run(abi + "-elf", [readelf, "-h", "-l", "-d", "-W", binary])
            dynamic = run(abi + "-dynamic-symbols", [readelf, "--dyn-syms", "-W", binary])
            all_symbols = run(abi + "-all-symbols", [readelf, "--symbols", "-W", binary])
            require(not re.search(r"\s+main\s*$", all_symbols, re.MULTILINE), "Unexpected main definition")
            report = audit(elf, dynamic, machine)
            report.update({"status": "COMPILED_NOT_ANDROID_RUN", "path": str(binary),
                           "bytes": binary.stat().st_size, "sha256": sha(binary),
                           "staticJPEG_SHA256": sha(library)})
            summary["abis"][abi] = report
        summary["status"] = "COMPILED_NOT_ANDROID_RUN"
    except Exception as error:
        summary["error"] = str(error)
    finally:
        write_json(root / "summary.json", summary)
    print(json.dumps({"status": summary["status"], "summary": str(root / "summary.json"),
                      "compiledABIs": list(summary["abis"])}, ensure_ascii=True))
    return 0 if summary["status"] == "COMPILED_NOT_ANDROID_RUN" else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:
        print(str(error), file=sys.stderr)
        sys.exit(2)
