"""Candidate-only Linux stress work; no Android/production operation or artifact upload."""
from __future__ import annotations

import hashlib
import importlib.util
import json
import os
from pathlib import Path
import platform
import resource
import shutil
import signal
import subprocess
import sys
import tarfile
import tempfile
import time
import urllib.request

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
CORE = HERE.parent / "m10-jpeg-e01-native"


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


base = load("candidate_e01_reuse", CORE / "run_ci.py")
derive = load("candidate_r1_derive", HERE.parent / "m10-jpeg-r1/scripts/derive_fixtures.py")


def emit(kind, **value):
    print("CANDIDATE_" + kind + " " + json.dumps(value, sort_keys=True), flush=True)


def sha(path):
    h = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(65536), b""):
            h.update(block)
    return h.hexdigest()


def configure(source, build, compiler, flags):
    base.run(compiler + "/configure", ["cmake", "-G", "Ninja", "-S", source, "-B", build,
        "-DCMAKE_C_COMPILER=" + compiler, "-DCMAKE_BUILD_TYPE=Debug",
        "-DCMAKE_POSITION_INDEPENDENT_CODE=ON",
        "-DCMAKE_C_FLAGS=" + " ".join(["-O1", "-g", "-fno-omit-frame-pointer", *flags, "-I" + str(CORE)]),
        "-DENABLE_SHARED=OFF", "-DENABLE_STATIC=ON", "-DWITH_TURBOJPEG=OFF",
        "-DWITH_TOOLS=OFF", "-DWITH_TESTS=OFF", "-DWITH_SIMD=OFF"], build.parent)
    base.run(compiler + "/build", ["cmake", "--build", build, "--target", "jpeg-static", "-j", "2"], build.parent, timeout=180)


def create_inputs(source, stock, root):
    generated = root / "generated"
    generated.mkdir()
    common = ["gcc", "-std=c11", "-O2", "-I", source / "src", "-I", stock]
    generator = root / "r1_generator"
    base.run("compile/r1-generator", [*common, HERE.parent / "m10-jpeg-r1/native/generate_fixtures.c",
                                     stock / "libjpeg.a", "-lm", "-o", generator], root)
    base.run("generate/r1-algorithm-new-run", [generator, generated, "--large"], root, timeout=120)
    extra = root / "extra_generator"
    base.run("compile/extra-generator", [*common, HERE / "generate_formats.c", stock / "libjpeg.a", "-lm", "-o", extra], root)
    base.run("generate/cmyk-ycck", [extra, generated], root)
    old = json.loads((HERE.parent / "m10-jpeg-e01-host/source-lock.json").read_text())
    r1_hashes = {Path(x["path"]).name: x["sha256"] for x in old["fixtures"][2:]}
    r1_hashes["large_40mp_progressive_444.jpg"] = r1_hashes["valid_40mp_progressive_444.jpg"]
    healthy = sorted(generated.glob("*.jpg"))
    if len(healthy) != 26:
        raise RuntimeError("Incomplete deterministic fixture generation")
    for path in healthy:
        emit("INPUT", name=path.name, origin="NEW_SYNTHETIC_R1_ALGORITHM" if not path.name.startswith(("cmyk", "ycck")) else "NEW_SYNTHETIC_CMYK_V1",
             bytes=path.stat().st_size, sha256=sha(path), matchesPriorR1Lock=sha(path) == r1_hashes.get(path.name))
    corrupt = []

    def mutate(name, parent, data, expected=("IMAGE_REJECT",)):
        path = root / name
        with path.open("xb") as target:
            target.write(data)
        emit("INPUT", name=name, origin="DETERMINISTIC_MUTATION", parent=parent.name,
             parentSha256=sha(parent), bytes=len(data), sha256=sha(path), expected=expected)
        corrupt.append((path, expected))

    for name in ("baseline_rgb_420.jpg", "progressive_rgb_420.jpg"):
        parent = generated / name
        data = parent.read_bytes()
        mutate("truncated-" + name, parent, data[:-32])
    for name in ("baseline_restart_420.jpg", "progressive_restart_420.jpg"):
        parent = generated / name
        data = parent.read_bytes()
        _, scans = derive.parse_original(data)
        start, end, code = next(scan for scan in scans if scan.restarts).restarts[0]
        mutate("bad-restart-" + name, parent, data[:end - 1] + bytes([0xD0 + ((code - 0xD0 + 3) % 8)]) + data[end:])
    parent = generated / "progressive_rgb_420.jpg"
    data = parent.read_bytes()
    _, scans = derive.parse_original(data)
    scan = max(scans, key=lambda s: s.end - s.start)
    cut1, cut2 = scan.start + (scan.end - scan.start) // 3, scan.start + 2 * (scan.end - scan.start) // 3
    mutate("progressive-entropy-cut-eoi.jpg", parent, data[:cut1] + data[cut2:])
    # Dropping an entire refinement scan can leave a legal JPEG. Observe this
    # capability boundary; do not invent a universally detectable corruption.
    final = scans[-1]
    mutate("progressive-whole-final-scan-removed.jpg", parent, data[:final.header.start] + data[final.end:], ("ACCEPT", "IMAGE_REJECT"))
    parent = generated / "baseline_rgb_420.jpg"
    data = bytearray(parent.read_bytes())
    markers, _ = derive.parse_original(bytes(data))
    sof = next(m for m in markers if m.code in derive.SOF_MARKERS)
    # Header-only capability probes, not complete valid compatibility samples.
    unsupported = bytearray(data)
    unsupported[sof.payload_offset - 3] = 0xC5
    mutate("unsupported-sof-header-only.jpg", parent, bytes(unsupported), ("INFRA_ERROR",))
    dimension = bytearray(data)
    dimension[sof.payload_offset + 1:sof.payload_offset + 5] = (1).to_bytes(2, "big") + (65501).to_bytes(2, "big")
    mutate("decoder-dimension-header-only.jpg", parent, bytes(dimension), ("RESOURCE_REJECT",))
    data[sof.payload_offset + 1:sof.payload_offset + 5] = (5001).to_bytes(2, "big") + (8000).to_bytes(2, "big")
    mutate("over-40mp-header.jpg", parent, bytes(data), ("RESOURCE_REJECT",))
    oversize = root / "over-64mib.jpg"
    with oversize.open("xb") as target:
        target.seek(64 * 1024 * 1024)
        target.write(b"\0")
    emit("INPUT", name=oversize.name, origin="SPARSE_SIZE_BOUNDARY", bytes=oversize.stat().st_size, sha256=sha(oversize))
    corrupt.append((oversize, ("RESOURCE_REJECT",)))
    # Source locks are checked but the old 114-run matrix is not repeated.
    originals = []
    for entry in old["fixtures"][:2]:
        path = REPO / entry["path"]
        if path.stat().st_size != entry["bytes"] or sha(path) != entry["sha256"]:
            raise RuntimeError("Original lock changed")
        originals.append(path)
    return generated, healthy, corrupt, originals


def controlled_process_end(probe, path, root):
    """CI-owned native children only, not Android termination evidence."""
    resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
    for mode, final_signal in (("TIMEOUT", signal.SIGKILL), ("NATIVE_CRASH", signal.SIGABRT)):
        start = time.monotonic()
        child = subprocess.Popen([str(probe), str(path)], cwd=root, env=base.ENV,
                                 stdout=subprocess.PIPE, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            os.killpg(child.pid, signal.SIGSTOP)
        except ProcessLookupError:
            output, _ = child.communicate(timeout=10)
            emit("SUPERVISOR", mode=mode, status="NOT_RUN_CHILD_ALREADY_EXITED", returnCode=child.returncode)
            continue
        # Child not reaped => its PID cannot be recycled. All group members
        # are confined to this explicitly created native research process.
        if mode == "TIMEOUT":
            try:
                child.wait(timeout=0.1)
                raise RuntimeError("Stopped child unexpectedly completed")
            except subprocess.TimeoutExpired:
                pass
            os.killpg(child.pid, final_signal)
        else:
            os.killpg(child.pid, final_signal)
            os.killpg(child.pid, signal.SIGCONT)
        output, _ = child.communicate(timeout=10)
        if child.returncode != -final_signal:
            raise RuntimeError("Controlled native process end classification mismatch")
        emit("SUPERVISOR", mode=mode, status="OBSERVED", returnCode=child.returncode,
             wallSeconds=time.monotonic() - start, structuredNativeResult=False,
             nativeCleanup="UNKNOWN", phase="UNKNOWN_STOPPED_EARLY", androidTermination="NOT_RUN",
             outputBytes=len(output), outputSha256=hashlib.sha256(output).hexdigest())


def matrix(compiler, flags, source, build, root, generated, healthy, corrupt, originals):
    common = [compiler, "-std=c11", "-O1", "-g", "-fno-omit-frame-pointer", "-Wall", "-Wextra", "-Werror", *flags,
              "-I", CORE, "-I", source / "src", "-I", build]
    budget_test = build / "budget_test"
    base.run(compiler + "/compile-budget-tests", [*common, CORE / "budget_test.c", CORE / "budget.c", "-o", budget_test], root)
    _, raw = base.run(compiler + "/budget-tests", [budget_test], root)
    budget_result = base.last_json(raw)
    if budget_result.get("tests") != 12 or budget_result.get("failed") != 0 or budget_result.get("status") != "PASS":
        raise RuntimeError("Native allocation/free regression failed")
    emit("BUDGET_RESULT", compiler=compiler, **budget_result)
    probe = build / "candidate_probe"
    base.run(compiler + "/compile-core", [*common, CORE / "strict_probe.c", CORE / "budget.c", build / "libjpeg.a", "-lm", "-o", probe], root)
    fd_test = build / "fd_test"
    base.run(compiler + "/compile-fd-test", [*common, "-DE01_NO_MAIN", CORE / "fd_test.c", CORE / "strict_probe.c",
                                            CORE / "budget.c", build / "libjpeg.a", "-lm", "-o", fd_test], root)
    _, raw = base.run(compiler + "/fd-tests", [fd_test, originals[0]], root)
    fd_result = base.last_json(raw)
    if fd_result.get("failed") != 0 or not fd_result.get("callerOwnershipVerified") or not fd_result.get("ownedCloseSyscallVerified"):
        raise RuntimeError("FD contract tests did not pass")
    emit("FD_RESULT", compiler=compiler, **fd_result)
    results = []

    def case(label, path, args=(), expected=("ACCEPT",)):
        rc, output = base.run(compiler + "/" + label, [probe, path, *args], root, expected=(0, 10, 20, 40), timeout=60)
        value = base.last_json(output)
        if value["status"] not in expected or rc != {"ACCEPT": 0, "IMAGE_REJECT": 10, "RESOURCE_REJECT": 20, "INFRA_ERROR": 40}[value["status"]]:
            raise RuntimeError("Unexpected result: " + label)
        if any(value[k] != 0 for k in ("live", "liveBlocks", "badFrees")) or not value["fdClosed"] or not value["decoderMemNull"]:
            raise RuntimeError("Unsafe resource release: " + label)
        if value["createAttempted"] and not value["destroyCalled"]:
            raise RuntimeError("Decoder destroy omitted")
        if value["status"] == "ACCEPT" and (value["rowsRead"] != value["height"] or not value["finishComplete"] or value["warnings"]):
            raise RuntimeError("Incomplete JPEG accepted")
        if not value.get("timingsAvailable") or not value.get("rssAvailable"):
            raise RuntimeError("Real metrics unavailable on Linux runner")
        results.append((label, value))
        emit("OBSERVATION", compiler=compiler, label=label, **value)
        return value

    large_results = {}
    for path in healthy:
        value = case("healthy/" + path.name, path)
        if path.name.startswith(("cmyk", "ycck")):
            if (value["inputComponents"] != 4 or value["outputComponents"] != 4 or
                    value["jpegColorSpace"] != (5 if path.name.startswith("ycck") else 4) or
                    value["outputColorSpace"] != 4 or value["dataPrecision"] != 8):
                raise RuntimeError("Four-component compatibility path not exercised")
        if path.name.startswith("large_40mp_"):
            if value["width"] * value["height"] != 40000000:
                raise RuntimeError("Stress dimensions differ")
            large_results[path.name] = value
    if set(large_results) != {"large_40mp_baseline_420.jpg", "large_40mp_progressive_420.jpg", "large_40mp_progressive_444.jpg"}:
        raise RuntimeError("Required 40MP coverage missing")
    for path, expected in corrupt:
        case("mutation/" + path.name, path, expected=expected)
    for path in originals:
        case("new-core-lock/" + path.name, path, expected=("ACCEPT",) if path.name == "complete.jpg" else ("IMAGE_REJECT",))
    for name, normal in large_results.items():
        path = generated / name
        for mode in ("--fail-at", "--fail-from"):
            for index in range(1, normal["attempts"] + 1):
                expected = ("ACCEPT", "RESOURCE_REJECT") if mode == "--fail-at" else ("RESOURCE_REJECT",)
                value = case(name + "/" + mode[2:] + "/" + str(index), path, (mode, str(index)), expected)
                if value["injected"] < 1:
                    raise RuntimeError("Missing actual injection")
        # This explicitly separates 64MiB input from decoder working memory.
        value = case(name + "/request-cap-64MiB", path, ("--limit", str(64 * 1024 * 1024)), ("ACCEPT", "RESOURCE_REJECT"))
        if value["peak"] > 64 * 1024 * 1024:
            raise RuntimeError("Request budget exceeded")
    phase_parent = generated / "large_40mp_progressive_420.jpg"
    for phase in ("create", "header", "start", "row", "scanline", "finish", "release"):
        value = case("phase-fault/" + phase, phase_parent, ("--fault-phase", phase), ("INFRA_ERROR",))
        if not value["phaseFaultInjected"] or value["errorCode"] != -2100 or value["phase"] != phase:
            raise RuntimeError("Phase fault missing/misclassified")
        expected_rows = 1 if phase == "scanline" else (value["height"] if phase in ("finish", "release") else 0)
        if value["rowsRead"] != expected_rows or value["finishComplete"] != (phase == "release") or value["warnings"]:
            raise RuntimeError("Fault did not reach the required real decode boundary")
    if compiler == "gcc":
        controlled_process_end(probe, generated / "large_40mp_progressive_444.jpg", root)
        host_jni(common, source, build, root, generated, originals)
    emit("MATRIX_RESULT", compiler=compiler, sanitizer=bool(flags), probeRuns=len(results),
         accepts=sum(v["status"] == "ACCEPT" for _, v in results),
         allMeteredReleased=True, allAcquiredFdsClosed=True, large=large_results)


def host_jni(common, source, build, root, generated, originals):
    # Exercise this exact bridge in a real JVM. No Android lifecycle inference.
    javac, java = shutil.which("javac"), shutil.which("java")
    if not javac or not java:
        raise RuntimeError("Existing runner JDK missing; no automatic install")
    include = Path(javac).resolve().parents[1] / "include"
    if not (include / "jni.h").is_file() or not (include / "linux/jni_md.h").is_file():
        raise RuntimeError("Existing Linux JNI headers missing")
    base.run("host-jni/version", [java, "-version"], root)
    library = build / "libstrictjpeg_candidate_host.so"
    base.run("host-jni/compile-native", [*common, "-fPIC", "-shared", "-DE01_NO_MAIN",
        "-I", include, "-I", include / "linux", HERE / "jni_bridge.c",
        CORE / "strict_probe.c", CORE / "budget.c", build / "libjpeg.a", "-lm", "-o", library], root)
    classes = build / "classes"
    classes.mkdir()
    base.run("host-jni/javac", [javac, "-d", classes, HERE / "StrictJpegCandidate.java", HERE / "JniHostTest.java"], root)
    command = [java, "--add-opens=java.base/java.io=ALL-UNNAMED", "-cp", classes, "JniHostTest"]
    _, text = base.run("host-jni/execute", [*command, library, *originals, generated / "large_40mp_progressive_420.jpg"], root)
    value = base.last_json(text)
    if value.get("jniCalls") != 8 or not value.get("callerOffsetPreserved"):
        raise RuntimeError("JNI host runtime contract failed")
    emit("JNI_HOST", **value)
    _, text = base.run("host-jni/load-error", [*command, "load-error", root / "does-not-exist.so"], root)
    value = base.last_json(text)
    if not value.get("loadFailurePreserved"):
        raise RuntimeError("Host load failure swallowed")
    emit("JNI_HOST", **value)


def main():
    if platform.system() != "Linux" or os.environ.get("GITHUB_REF") != "refs/heads/codex/m10-jpeg-accelerated-candidate":
        raise RuntimeError("Restricted to candidate CI")
    for tool in ("gcc", "clang", "cmake", "ninja"):
        if not shutil.which(tool):
            raise RuntimeError("Existing tool missing; no automatic install")
        base.run("version/" + tool, [tool, "--version"], HERE)
    with tempfile.TemporaryDirectory(prefix="jpeg-candidate-") as name:
        root = Path(name)
        archive = root / "source.tar.gz"
        with urllib.request.urlopen(base.ARCHIVE_URL, timeout=60) as response:
            data = response.read(base.ARCHIVE_BYTES + 1)
        if len(data) != base.ARCHIVE_BYTES or hashlib.sha256(data).hexdigest() != base.ARCHIVE_SHA:
            raise RuntimeError("Fixed official source failed verification")
        archive.write_bytes(data)
        emit("SOURCE", bytes=len(data), sha256=base.ARCHIVE_SHA, commit=os.environ["GITHUB_SHA"])
        with tarfile.open(archive) as tar:
            tar.extractall(root, filter="data")
        source = root / "libjpeg-turbo-3.2.0"
        stock = root / "stock"
        configure(source, stock, "gcc", [])
        generated, healthy, corrupt, originals = create_inputs(source, stock, root)
        backend = source / "src/jmemnobs.c"
        if sha(backend) != "328ff841f437fa7c2de846c4e295e9145cc628b9041aed718a06eb087c55786c":
            raise RuntimeError("Backend source mismatch")
        shutil.copyfile(CORE / "jmem_budget.c", backend)
        for compiler, flags in (("gcc", []), ("clang", ["-fsanitize=address,undefined"])):
            build = root / (compiler + "-build")
            configure(source, build, compiler, flags)
            matrix(compiler, flags, source, build, root, generated, healthy, corrupt, originals)
        emit("RESULT", status="HOST_STRESS_PASS", Android="NOT_RUN", fullE01="NOT_PROVEN",
             productImport="BLOCKED_PERSISTENT_WORKER_PROTOCOL", historical114="REUSED_NOT_REPEATED")


if __name__ == "__main__":
    main()
