#!/usr/bin/env python3
"""Build the isolated R1 probe, never the production App.

Requires explicitly supplied SDK/JDK paths and already installed official tools.
Does not download, install, accept licenses, change global PATH, or invoke ADB.
The fixed official source archive is authenticated separately by the root R1
workflow; this script also checks its locked SHA and records the source tree.
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

ABIS = ("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
NDK_VERSION = "28.2.13676358"
CMAKE_VERSION = "3.22.1"
SOURCE_SHA = "6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e"


def sha(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def required_file(path: Path) -> Path:
    if not path.is_file() or path.is_symlink():
        raise ValueError(f"Required regular file not available: {path}")
    return path.resolve()


def properties(path: Path) -> dict[str, str]:
    result = {}
    for line in required_file(path).read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def snapshot_tree(root: Path) -> list[dict[str, object]]:
    result = []
    for path in sorted(root.rglob("*")):
        if path.is_symlink():
            raise ValueError(f"Source tree contains a symbolic link: {path}")
        if path.is_file():
            result.append({"path": path.relative_to(root).as_posix(),
                           "bytes": path.stat().st_size, "sha256": sha(path)})
    return result


class Recorder:
    def __init__(self, run_dir: Path, env: dict[str, str], report: dict):
        self.run_dir = run_dir
        self.env = env
        self.report = report

    def run(self, label: str, command: list[str], cwd: Path, timeout: int = 1800) -> str:
        number = len(self.report["commands"])
        log = self.run_dir / f"{number:02d}-{label}.log"
        start = time.perf_counter()
        item = {"label": label, "argv": command, "cwd": str(cwd),
                "startedUtc": now(), "log": str(log), "exitCode": None}
        self.report["commands"].append(item)
        write_json(self.run_dir / "manifest.json", self.report)
        try:
            with log.open("wb") as output:
                completed = subprocess.run(command, cwd=cwd, env=self.env,
                                           stdout=output, stderr=subprocess.STDOUT,
                                           timeout=timeout, check=False)
            item["exitCode"] = completed.returncode
        except (OSError, subprocess.TimeoutExpired) as error:
            item["driverException"] = type(error).__name__ + ": " + str(error)
            raise
        finally:
            item["wallSeconds"] = round(time.perf_counter() - start, 6)
            item["endedUtc"] = now()
            if log.exists():
                item["logBytes"] = log.stat().st_size
                item["logSha256"] = sha(log)
            write_json(self.run_dir / "manifest.json", self.report)
            print(json.dumps({"stage": label, "exitCode": item["exitCode"],
                              "wallSeconds": item["wallSeconds"], "log": str(log)}), flush=True)
        text = log.read_text(encoding="utf-8", errors="replace")
        if item["exitCode"] != 0:
            raise RuntimeError(f"{label} failed with exit {item['exitCode']}; see {log}")
        return text


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", type=Path, required=True)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--source", type=Path)
    parser.add_argument("--source-archive", type=Path, required=True)
    selection = parser.add_mutually_exclusive_group(required=True)
    selection.add_argument("--abi", action="append", choices=ABIS)
    selection.add_argument("--all", action="store_true")
    parser.add_argument("--jobs", type=int, default=2)
    args = parser.parse_args()
    if not 1 <= args.jobs <= 8:
        parser.error("--jobs must be 1..8")
    repo = Path(__file__).resolve().parents[4]
    research = repo / "docs/research/m10-jpeg-r1"
    base = repo / "build/m10-jpeg-r1"
    source = (args.source or base / "source/libjpeg-turbo-3.2.0").resolve()
    archive = required_file(args.source_archive)
    if sha(archive) != SOURCE_SHA:
        raise ValueError("Source archive SHA differs from the authorized 3.2.0 release")
    required_file(source / "CMakeLists.txt")
    required_file(source / "src/jpeglib.h")
    required_file(source / "src/turbojpeg.h")
    harness_files = [required_file(research / "native/CMakeLists.txt"),
                     required_file(research / "native/strictjpeg_probe.c"),
                     Path(__file__).resolve()]
    fixture_source = research / "native/generate_fixtures.c"
    if fixture_source.is_file():
        harness_files.append(required_file(fixture_source))
    sdk = args.sdk.resolve()
    jdk = args.jdk.resolve()
    ndk = sdk / "ndk" / NDK_VERSION
    cmake_root = sdk / "cmake" / CMAKE_VERSION
    if properties(ndk / "source.properties").get("Pkg.Revision") != NDK_VERSION:
        raise ValueError("Installed NDK revision differs from the authorized version")
    if properties(cmake_root / "source.properties").get("Pkg.Revision") != CMAKE_VERSION:
        raise ValueError("Installed CMake revision differs from the authorized version")
    cmake = required_file(cmake_root / "bin/cmake.exe")
    ninja = required_file(cmake_root / "bin/ninja.exe")
    required_file(jdk / "bin/java.exe")
    toolchain = required_file(ndk / "build/cmake/android.toolchain.cmake")
    toolbin = ndk / "toolchains/llvm/prebuilt/windows-x86_64/bin"
    clang = required_file(toolbin / "clang.exe")
    readelf = required_file(toolbin / "llvm-readelf.exe")
    strip = required_file(toolbin / "llvm-strip.exe")
    ar = required_file(toolbin / "llvm-ar.exe")
    selected = list(ABIS) if args.all else list(dict.fromkeys(args.abi))
    run_id = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ") + f"-{os.getpid()}"
    run_dir = base / "build-runs" / run_id
    run_dir.mkdir(parents=True, exist_ok=False)
    env = os.environ.copy()
    env["JAVA_HOME"] = str(jdk)
    env["ANDROID_HOME"] = str(sdk)
    env["ANDROID_SDK_ROOT"] = str(sdk)
    # Only this child environment is altered.  Compiler and Ninja selection are
    # explicit; no machine or user environment setting is written.
    report = {"schemaVersion": 1, "purpose": "RESEARCH_ONLY_NOT_PRODUCTION_JNI",
              "runId": run_id, "startedUtc": now(), "status": "RUNNING",
              "repo": str(repo), "source": str(source),
              "sourceArchive": {"path": str(archive), "sha256": sha(archive),
                                "bytes": archive.stat().st_size},
              "sdk": str(sdk), "jdk": str(jdk), "ndk": str(ndk),
              "ndkVersion": NDK_VERSION, "cmakeVersion": CMAKE_VERSION,
              "androidApi": 26, "selectedAbis": selected, "commands": [], "abis": [],
              "notes": ["WITH_SIMD=OFF is a research configuration, not a production performance result.",
                        "3.2.0 TurboJPEG includes upstream bundled spng/zlib objects even with tools/tests OFF.",
                        "Shared probe includes main() and is for ELF/size inspection only; no JNI export/load proof."]}
    record = Recorder(run_dir, env, report)
    source_before = snapshot_tree(source)
    source_manifest = run_dir / "source-inputs.json"
    write_json(source_manifest, source_before)
    report["sourceManifest"] = {"path": str(source_manifest), "sha256": sha(source_manifest),
                                "fileCount": len(source_before)}
    harness_before = [{"path": str(p), "bytes": p.stat().st_size, "sha256": sha(p)} for p in harness_files]
    report["researchInputs"] = harness_before
    try:
        cmake_text = record.run("cmake-version", [str(cmake), "--version"], repo)
        if f"cmake version {CMAKE_VERSION}" not in cmake_text:
            raise ValueError("CMake executable version mismatch")
        record.run("ninja-version", [str(ninja), "--version"], repo)
        record.run("clang-version", [str(clang), "--version"], repo)
        for abi in selected:
            abi_root = base / "native" / abi
            upstream = abi_root / "upstream"
            prefix = abi_root / "prefix"
            probe = abi_root / "probe"
            artifacts = abi_root / "artifacts"
            artifacts.mkdir(parents=True, exist_ok=True)
            common = ["-G", "Ninja", f"-DCMAKE_MAKE_PROGRAM={ninja.as_posix()}",
                      f"-DCMAKE_TOOLCHAIN_FILE={toolchain.as_posix()}",
                      f"-DANDROID_ABI={abi}", "-DANDROID_PLATFORM=android-26",
                      "-DCMAKE_SYSTEM_VERSION=26", "-DANDROID_STL=none",
                      "-DCMAKE_BUILD_TYPE=Release", "-DCMAKE_POSITION_INDEPENDENT_CODE=ON",
                      "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
                      "-DCMAKE_EXE_LINKER_FLAGS=-Wl,-z,max-page-size=16384",
                      "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384"]
            configure_upstream = [str(cmake), "-S", str(source), "-B", str(upstream), *common,
                                  f"-DCMAKE_INSTALL_PREFIX={prefix.as_posix()}",
                                  "-DCMAKE_INSTALL_LIBDIR=lib", "-DCMAKE_INSTALL_INCLUDEDIR=include",
                                  "-DENABLE_SHARED=OFF", "-DENABLE_STATIC=ON", "-DWITH_SIMD=OFF",
                                  "-DWITH_TURBOJPEG=ON", "-DWITH_TOOLS=OFF", "-DWITH_TESTS=OFF",
                                  "-DWITH_FUZZ=OFF", "-DWITH_JNA=", "-DWITH_SYSTEM_SPNG=OFF",
                                  "-DWITH_SYSTEM_ZLIB=OFF"]
            record.run(f"{abi}-upstream-configure", configure_upstream, repo)
            record.run(f"{abi}-upstream-build", [str(cmake), "--build", str(upstream),
                                               "--target", "jpeg-static", "turbojpeg-static",
                                               "--parallel", str(args.jobs)], repo)
            record.run(f"{abi}-upstream-install", [str(cmake), "--install", str(upstream)], repo)
            for relative in ("include/jpeglib.h", "include/turbojpeg.h", "include/jconfig.h",
                             "lib/libjpeg.a", "lib/libturbojpeg.a"):
                required_file(prefix / relative)
            record.run(f"{abi}-probe-configure", [str(cmake), "-S", str(research / "native"),
                                                 "-B", str(probe), *common,
                                                 f"-DR1_JPEG_PREFIX={prefix.as_posix()}"], repo)
            record.run(f"{abi}-probe-build", [str(cmake), "--build", str(probe),
                                             "--parallel", str(args.jobs)], repo)
            item = {"abi": abi, "status": "COMPILED", "runtimeStatus": "NOT_RUN", "outputs": []}
            for file_name in ("strictjpeg_probe", "libstrictjpeg_probe.so", "generate_fixtures"):
                binary = probe / file_name
                if file_name == "generate_fixtures" and not fixture_source.is_file():
                    continue
                required_file(binary)
                text = record.run(f"{abi}-{file_name}-elf", [str(readelf), "-h", "-l", "-d", "-W", str(binary)], repo)
                record.run(f"{abi}-{file_name}-symbols", [str(readelf), "--dyn-syms", "-W", str(binary)], repo)
                load_alignments = []
                for line in text.splitlines():
                    if re.match(r"\s*LOAD\s", line):
                        load_alignments.append(int(line.split()[-1], 16))
                if not load_alignments or any(value < 16384 for value in load_alignments):
                    raise ValueError(f"{abi}/{file_name}: actual PT_LOAD alignment below 16KiB")
                stripped = artifacts / f"{file_name}.stripped"
                record.run(f"{abi}-{file_name}-strip", [str(strip), "--strip-unneeded", "-o", str(stripped), str(binary)], repo)
                stripped_elf = record.run(f"{abi}-{file_name}-stripped-elf", [str(readelf), "-h", "-l", "-d", "-W", str(stripped)], repo)
                stripped_alignments = [int(line.split()[-1], 16) for line in stripped_elf.splitlines()
                                      if re.match(r"\s*LOAD\s", line)]
                if stripped_alignments != load_alignments:
                    raise ValueError("Strip unexpectedly changed ELF PT_LOAD alignment")
                item["outputs"].append({"path": str(binary), "bytes": binary.stat().st_size,
                                        "sha256": sha(binary), "ptLoadAlignments": load_alignments,
                                        "stripped": {"path": str(stripped), "bytes": stripped.stat().st_size,
                                                     "sha256": sha(stripped)},
                                        "needed": re.findall(r"\(NEEDED\).*?\[(.*?)\]", text)})
            for lib in ("jpeg", "turbojpeg"):
                path = prefix / f"lib/lib{lib}.a"
                record.run(f"{abi}-{lib}-archive-members", [str(ar), "t", str(path)], repo)
                item["outputs"].append({"path": str(path), "bytes": path.stat().st_size, "sha256": sha(path)})
            item["configuration"] = []
            for path in [upstream / "CMakeCache.txt", upstream / "compile_commands.json",
                         probe / "CMakeCache.txt", probe / "compile_commands.json"]:
                required_file(path)
                item["configuration"].append({"path": str(path), "bytes": path.stat().st_size, "sha256": sha(path)})
            report["abis"].append(item)
        if source_before != snapshot_tree(source):
            raise ValueError("Official source tree changed during the build")
        if harness_before != [{"path": str(p), "bytes": p.stat().st_size, "sha256": sha(p)} for p in harness_files]:
            raise ValueError("Research source changed during the build; do not mix this result with later source")
        report["status"] = "COMPILED_NOT_RUNTIME_VERIFIED"
        report["inputsStable"] = True
    except Exception as error:
        report["status"] = "BUILD_FAILED"
        report["failure"] = type(error).__name__ + ": " + str(error)
        print(report["failure"], file=sys.stderr)
    finally:
        report["endedUtc"] = now()
        write_json(run_dir / "manifest.json", report)
        print(json.dumps({"status": report["status"], "manifest": str(run_dir / "manifest.json")}))
    return 0 if report["status"] == "COMPILED_NOT_RUNTIME_VERIFIED" else 1


if __name__ == "__main__":
    raise SystemExit(main())
