"""Host-only Java evidence. No Android, APK build, dependency fetch or production write.

The extra characterization test may PASS while proving an OPEN safety blocker.
It is never included in the product-safety PASS count.
"""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import os
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
JAVA_HOME = Path("C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1")
PROD = ROOT / "app/src/main/java/com/cooper/lithic/data/media/jpegworker"
TESTS = ROOT / "app/src/test/java/com/cooper/lithic/data/media/jpegworker"
COMPONENTS = ("JpegAbandonedDelivery", "JpegBindingDuty", "JpegDecoderPolicy", "JpegResultWait",
              "JpegRetirementGate", "JpegRetirementHandshake", "JpegSessionFence")
PREFIX = "com.cooper.lithic.data.media.jpegworker."


def stamp():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def sha(data):
    return hashlib.sha256(data).hexdigest()


def metadata(path):
    data = path.read_bytes()
    try:
        name = path.relative_to(ROOT).as_posix()
    except ValueError:
        name = str(path)
    return {"path": name, "bytes": len(data), "sha256": sha(data)}


def write_json(path, value):
    with path.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(value, stream, indent=2)
        stream.write("\n")


def cached_jar(relative, expected_sha):
    parent = Path("C:/Users/CDD/.gradle/caches/modules-2/files-2.1") / relative
    found = list(parent.glob("*/*.jar"))
    if len(found) != 1 or sha(found[0].read_bytes()) != expected_sha:
        raise RuntimeError("Fixed cached dependency unavailable or changed: " + str(parent))
    return found[0]


def command(out, label, argv):
    before = stamp()
    try:
        result = subprocess.run([str(value) for value in argv], cwd=ROOT, capture_output=True, timeout=90)
        row = {"label": label, "argv": [str(v) for v in argv], "startedUtc": before,
               "endedUtc": stamp(), "exitCode": result.returncode,
               "stdoutBytes": len(result.stdout), "stdoutSHA256": sha(result.stdout),
               "stderrBytes": len(result.stderr), "stderrSHA256": sha(result.stderr)}
        for suffix, data in (("stdout.log", result.stdout), ("stderr.log", result.stderr)):
            with (out / (label + "." + suffix)).open("xb") as stream:
                stream.write(data)
        write_json(out / (label + ".command.json"), row)
        return result
    except subprocess.TimeoutExpired as error:
        # This is only our host compiler/JUnit process; no Android process is involved.
        write_json(out / (label + ".timeout.json"), {"startedUtc": before, "endedUtc": stamp(),
                   "classification": "HOST_COMMAND_TIMEOUT_UNKNOWN", "seconds": error.timeout})
        raise


def group_result(result, group, expected):
    text = result.stdout.decode("utf-8", "replace")
    pattern = (r"^SUMMARY group=" + group + r" declared=(\d+) executed=(\d+) failures=(\d+) "
               r"ignored=(\d+) assumptionFailures=(\d+) identityMatch=(true|false)\r?$")
    matches = re.findall(pattern, text, re.MULTILINE)
    if result.returncode != 0 or matches != [(str(expected), str(expected), "0", "0", "0", "true")]:
        raise RuntimeError("Actual JUnit execution failed or count/identity did not match: " + group)
    return {"group": group, "tests": expected, "uniqueTests": expected, "failures": 0,
            "ignored": 0, "assumptionFailures": 0, "actualJUnitExit": result.returncode}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", required=True, help="Fresh evidence directory beneath this worktree build/")
    args = parser.parse_args()
    out = Path(args.out).resolve()
    if not out.is_relative_to((ROOT / "build").resolve()):
        raise RuntimeError("Evidence output must stay under this worktree build/")
    out.mkdir(parents=True, exist_ok=False)
    classes = out / "classes"
    classes.mkdir()
    java, javac = JAVA_HOME / "bin/java.exe", JAVA_HOME / "bin/javac.exe"
    junit = cached_jar("junit/junit/4.13.2", "8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3")
    hamcrest = cached_jar("org.hamcrest/hamcrest-core/1.3", "66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9")
    production = [PROD / (name + ".java") for name in COMPONENTS]
    tests = sorted(TESTS.glob("*Test.java"))
    if len(tests) != 12 or sum(path.read_text(encoding="utf-8").count("@Test") for path in tests) != 143:
        raise RuntimeError("Locked existing 12 test classes/143 methods changed; do not expand silently")
    sources = production + tests + [HERE / "HostSafetyRunner.java", HERE / "LateRpcOpenBlockerTest.java"]
    inputs = [metadata(path) for path in sources + [Path(__file__), junit, hamcrest, java, javac]]
    write_json(out / "inputs.json", {"scope": "Small exact input table; no archives or binaries copied", "inputs": inputs})
    compile_result = command(out, "compile", [javac, "--release", "8", "-encoding", "UTF-8", "-cp",
        os.pathsep.join((str(junit), str(hamcrest))), "-d", classes, *sources])
    if compile_result.returncode != 0:
        raise RuntimeError("Fresh compilation failed; retain raw compiler output")
    cp = os.pathsep.join(map(str, (classes, junit, hamcrest)))
    existing = command(out, "existing-143", [java, "-cp", cp, "HostSafetyRunner", "EXISTING_COMPONENT_REGRESSION",
        *[PREFIX + path.stem for path in tests]])
    regression = group_result(existing, "EXISTING_COMPONENT_REGRESSION", 143)
    blocker = command(out, "open-blocker-1", [java, "-cp", cp, "HostSafetyRunner", "REPRODUCES_OPEN_BLOCKER",
        PREFIX + "LateRpcOpenBlockerTest"])
    reproduction = group_result(blocker, "REPRODUCES_OPEN_BLOCKER", 1)
    if "REPRODUCES_OPEN_BLOCKER: two_cleanup_opportunities_rejected;" not in blocker.stdout.decode("utf-8", "replace"):
        raise RuntimeError("The real component did not produce the required open-blocker observation")
    for before, path in zip(inputs[:len(sources)], sources):
        if metadata(path) != before:
            raise RuntimeError("Input changed during host execution")
    regression["status"] = "PASS_HOST_COMPONENT_REGRESSION_ONLY"
    reproduction["status"] = "REPRODUCES_OPEN_BLOCKER"
    write_json(out / "RESULTS.json", {"finishedUtc": stamp(), "classification": "HOST_ONLY",
        "sourceCommit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "productionComponents": 7, "existingTestClasses": 12, "compilation": "PASS",
        "regression": regression, "newCharacterization": reproduction,
        "productSafetyGate": "BLOCKED_NOT_FIXED", "Android": "NOT_RUN",
        "limits": "Real JpegBindingDuty only. Not the full Client, Binder, Service, FD, native or Android lifecycle.",
        "deviceCommands": 0, "APKBuilds": 0, "productionChanges": 0})
    print("143 existing component methods PASS; 1 new method REPRODUCES_OPEN_BLOCKER; Android NOT_RUN")


if __name__ == "__main__":
    main()
