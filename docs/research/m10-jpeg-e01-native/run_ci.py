"""Bounded Linux CI native experiment. No Android or product build operations."""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import tarfile
import tempfile
import time
import urllib.request

ARCHIVE_URL = "https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/3.2.0/libjpeg-turbo-3.2.0.tar.gz"
ARCHIVE_SHA = "6f30092cef9fb839779646608f4ee14ae3cbac989c47fa05e841b0841f09878e"
ARCHIVE_BYTES = 2537858
REPO = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
ENV = dict(os.environ, ASAN_OPTIONS="detect_leaks=1:halt_on_error=1:allocator_may_return_null=1",
           UBSAN_OPTIONS="halt_on_error=1:print_stacktrace=1")


def emit(kind, **fields):
    print("E01_" + kind + " " + json.dumps(fields, sort_keys=True), flush=True)


def run(label, argv, cwd, expected=(0,), timeout=120):
    start = time.monotonic()
    try:
        result = subprocess.run([str(x) for x in argv], cwd=cwd, env=ENV,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                timeout=timeout, check=False)
    except subprocess.TimeoutExpired as error:
        output = error.stdout or b""
        emit("TIMEOUT", label=label, seconds=timeout, outputSha256=hashlib.sha256(output).hexdigest())
        print(output.decode(errors="replace"), flush=True)
        raise RuntimeError("Native/build result UNKNOWN; no retry: " + label) from error
    emit("COMMAND", label=label, argv=[str(x) for x in argv], exitCode=result.returncode,
         wallSeconds=round(time.monotonic() - start, 6), bytes=len(result.stdout),
         sha256=hashlib.sha256(result.stdout).hexdigest())
    text = result.stdout.decode("utf-8", errors="strict")
    print(text, end="" if text.endswith("\n") else "\n", flush=True)
    if result.returncode not in expected:
        raise RuntimeError("Unexpected exit (crash is not image rejection): " + label)
    if any(word in text for word in ("ERROR: AddressSanitizer", "ERROR: LeakSanitizer", "runtime error:")):
        raise RuntimeError("Sanitizer error: " + label)
    return result.returncode, text


def last_json(text):
    for line in reversed(text.splitlines()):
        if line.startswith("{"):
            return json.loads(line)
    raise RuntimeError("Missing native structured result")


def fixture_inputs():
    lock = json.loads((HERE.parent / "m10-jpeg-e01-host/source-lock.json").read_text())
    selected = lock["fixtures"][:2]
    expected = {
        "app/src/androidTest/assets/m10_jpeg_integrity/complete.jpg":
            (59334, "2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907"),
        "app/src/androidTest/assets/m10_jpeg_integrity/entropy_cut_eoi_retained.jpg":
            (20194, "f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a"),
    }
    if len(selected) != 2 or {e["path"]: (e["bytes"], e["sha256"]) for e in selected} != expected:
        raise RuntimeError("Exactly the two original locked JPEGs are required")
    fixtures = []
    for entry in selected:
        path = REPO / entry["path"]
        data = path.read_bytes()
        if len(data) != entry["bytes"] or hashlib.sha256(data).hexdigest() != entry["sha256"]:
            raise RuntimeError("Locked JPEG identity mismatch")
        emit("INPUT", **entry)
        fixtures.append(path)
    emit("NOT_RUN", cases="Original local 40MP fixtures; unavailable in runner checkout, no substitutes")
    return fixtures


def native_checks(compiler, build, source, flags, fixtures):
    unit = build / "budget_test"
    probe = build / "strict_probe"
    common = [compiler, "-std=c11", "-g", "-O1", "-fno-omit-frame-pointer",
              "-Wall", "-Wextra", "-Werror", *flags, "-I", HERE]
    red = build / "budget_test_red"
    run(compiler + "/compile-red", [*common, "-DE01_RED_BACKEND", HERE / "budget_test.c",
                                     HERE / "budget.c", "-o", red], build)
    rc, text = run(compiler + "/red-mutation", [red], build, expected=(1,))
    red_result = last_json(text)
    if rc != 1 or red_result.get("failed", 0) < 1:
        raise RuntimeError("RED mutation was not detected")
    run(compiler + "/compile-unit", [*common, HERE / "budget_test.c", HERE / "budget.c", "-o", unit], build)
    _, text = run(compiler + "/unit", [unit], build)
    unit_result = last_json(text)
    if unit_result.get("failed") != 0 or unit_result.get("tests") != 12:
        raise RuntimeError("Native unit contract did not pass")
    run(compiler + "/compile-probe", [*common, "-I", source / "src", "-I", build,
                                       HERE / "strict_probe.c", HERE / "budget.c",
                                       build / "libjpeg.a", "-lm", "-o", probe], build)
    counts = {"probeRuns": 0, "failAt": 0, "failFrom": 0, "fallbackAccepts": 0}

    def case(label, path, args=(), statuses=("ACCEPT",)):
        rc, raw = run(compiler + "/" + label, [probe, path, *args], build,
                     expected=(0, 10, 20), timeout=20)
        value = last_json(raw)
        if value.get("status") not in statuses:
            raise RuntimeError("Wrong structured native result: " + label)
        if rc != {"ACCEPT": 0, "IMAGE_REJECT": 10, "RESOURCE_REJECT": 20}[value["status"]]:
            raise RuntimeError("Native status and exit code disagree: " + label)
        for field in ("live", "liveBlocks", "badFrees"):
            if value.get(field) != 0:
                raise RuntimeError("Incomplete native cleanup: " + label + "/" + field)
        if value.get("fdClosed") != 1 or value.get("decoderMemNull") != 1:
            raise RuntimeError("Incomplete FD/decoder cleanup: " + label)
        if value.get("createAttempted") and value.get("destroyCalled") != 1:
            raise RuntimeError("Partial/complete decoder was not destroyed: " + label)
        counts["probeRuns"] += 1
        return value

    for path in fixtures:
        healthy = path.name == "complete.jpg"
        original = "ACCEPT" if healthy else "IMAGE_REJECT"
        baseline = case(path.stem + "/baseline", path, statuses=(original,))
        if not baseline["smallCalls"] or not baseline["compressedRead"]:
            raise RuntimeError("No real libjpeg/source consumption")
        if healthy and (not baseline["largeCalls"] or baseline["rowsRead"] != baseline["height"]
                        or baseline["finishComplete"] != 1):
            raise RuntimeError("Healthy JPEG was not fully consumed")
        for mode, count_field in (("--fail-at", "failAt"), ("--fail-from", "failFrom")):
            for index in range(1, baseline["attempts"] + 1):
                statuses = (original, "RESOURCE_REJECT") if mode == "--fail-at" else ("RESOURCE_REJECT",)
                value = case(path.stem + "/" + mode[2:] + "/" + str(index), path,
                             (mode, str(index)), statuses)
                if not value["injected"]:
                    raise RuntimeError("Failure injection not reached")
                if mode == "--fail-at" and value["injected"] != 1:
                    raise RuntimeError("Single failure injection fired more than once")
                if index == 2 and mode == "--fail-from":
                    if value["createAttempted"] != 1 or value["destroyCalled"] != 1 or value["smallCalls"] != 1:
                        raise RuntimeError("Pre-create manager failure not exercised")
                counts[count_field] += 1
                counts["fallbackAccepts"] += int(mode == "--fail-at" and value["status"] == "ACCEPT")
        if healthy:
            value = case("budget-zero", path, ("--limit", "0"), ("RESOURCE_REJECT",))
            if not value["denied"]:
                raise RuntimeError("Budget rejection missing")
            for cap in (baseline["peak"], baseline["peak"] // 2):
                value = case("budget-" + str(cap), path, ("--limit", str(cap)),
                             ("ACCEPT", "RESOURCE_REJECT"))
                if value["peak"] > cap:
                    raise RuntimeError("Charged memory exceeded research cap")
    emit("COMPILER_RESULT", compiler=compiler, nativeUnitTests=unit_result["tests"],
         nativeAssertions=unit_result["assertions"], sanitizer=bool(flags), **counts)
    return counts


def main():
    if (platform.system() != "Linux" or os.environ.get("CI") != "true" or
            os.environ.get("GITHUB_REF") != "refs/heads/codex/m10-r2-e01-host"):
        raise RuntimeError("This experiment is restricted to the authorized Linux research CI")
    for tool in ("gcc", "clang", "cmake", "ninja"):
        if not shutil.which(tool):
            raise RuntimeError("Existing runner tool missing; no installation: " + tool)
        run("version/" + tool, [tool, "--version"], HERE)
    emit("PLATFORM", system=platform.platform(), machine=platform.machine(),
         pointerBits=8 * __import__("struct").calcsize("P"), commit=os.environ["GITHUB_SHA"])
    fixtures = fixture_inputs()
    with tempfile.TemporaryDirectory(prefix="m10-e01-") as temporary:
        root = Path(temporary)
        archive = root / "libjpeg-turbo-3.2.0.tar.gz"
        with urllib.request.urlopen(ARCHIVE_URL, timeout=60) as response:
            data = response.read(ARCHIVE_BYTES + 1)
        if len(data) != ARCHIVE_BYTES or hashlib.sha256(data).hexdigest() != ARCHIVE_SHA:
            raise RuntimeError("Official fixed source identity failed; stop without replacement")
        archive.write_bytes(data)
        emit("SOURCE", url=ARCHIVE_URL, bytes=len(data), sha256=ARCHIVE_SHA)
        with tarfile.open(archive, "r:gz") as tar:
            tar.extractall(root, filter="data")
        source = root / "libjpeg-turbo-3.2.0"
        backend = source / "src/jmemnobs.c"
        before = hashlib.sha256(backend.read_bytes()).hexdigest()
        if before != "328ff841f437fa7c2de846c4e295e9145cc628b9041aed718a06eb087c55786c":
            raise RuntimeError("Unexpected original backend")
        shutil.copyfile(HERE / "jmem_budget.c", backend)
        emit("PATCH", source="src/jmemnobs.c", originalSha256=before,
             researchSha256=hashlib.sha256(backend.read_bytes()).hexdigest())
        results = []
        for compiler, flags in (("gcc", []), ("clang", ["-fsanitize=address,undefined"])):
            build = root / (compiler + "-build")
            build.mkdir()
            cflags = " ".join(["-g", "-O1", "-fno-omit-frame-pointer", *flags, "-I" + str(HERE)])
            run(compiler + "/configure", ["cmake", "-G", "Ninja", "-S", source, "-B", build,
                 "-DCMAKE_C_COMPILER=" + compiler, "-DCMAKE_BUILD_TYPE=Debug", "-DCMAKE_C_FLAGS=" + cflags,
                 "-DENABLE_SHARED=OFF", "-DENABLE_STATIC=ON", "-DWITH_TURBOJPEG=OFF",
                 "-DWITH_TOOLS=OFF", "-DWITH_TESTS=OFF", "-DWITH_SIMD=OFF"], root)
            run(compiler + "/build", ["cmake", "--build", build, "--target", "jpeg-static", "-j", "2"], root, timeout=180)
            results.append(native_checks(compiler, build, source, flags, fixtures))
        emit("RESULT", status="HOST_LIMITED_PASS", compilerConfigurations=2,
             original40MP="NOT_RUN", native32Bit="NOT_RUN", android="NOT_RUN",
             totalProcessMemory="UNKNOWN", e01FullCoverage="NOT_PROVEN",
             totalProbeRuns=sum(result["probeRuns"] for result in results))


if __name__ == "__main__":
    main()
