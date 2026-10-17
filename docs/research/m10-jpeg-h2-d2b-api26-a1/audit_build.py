"""Audit a completed A1 offline research build. Never builds or contacts a device.

Usage: python docs/research/m10-jpeg-h2-d2b-api26-a1/audit_build.py <build-run-directory>
The run directory must be new A1 evidence; artifact-audit is created exclusively.
"""
from pathlib import Path
import datetime
import hashlib
import json
import os
import re
import subprocess
import sys
import traceback
import xml.etree.ElementTree as ET
import zipfile


BASELINE = "3792917c7938c2ec78789df0a53a27215f4ee099"
BASELINE_METHODS = 229
CERTIFICATE_SHA256 = "29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9"
HISTORICAL_RESULTS = {
    "F1A": "docs/research/m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/result.json",
    "D2A": "docs/research/m10-jpeg-h2-d2a/evidence/build-runs/20261017T122708.815561Z/result.json",
    "D2B": "docs/research/m10-jpeg-h2-d2b/evidence/build-runs/20261017T132201.765744Z/result.json",
}
repo = Path(__file__).resolve().parents[3]
if len(sys.argv) != 2:
    raise SystemExit(__doc__)
run = Path(sys.argv[1])
if not run.is_absolute():
    run = repo / run
run = run.resolve(strict=True)
evidence_root = (repo / "docs/research/m10-jpeg-h2-d2b-api26-a1/evidence/build-runs").resolve()
assert run.parent == evidence_root, "Only a completed A1 build-run directory may be audited"
record = json.loads((run / "result.json").read_text(encoding="utf-8"))
assert record["exitCode"] == 0
assert record["productionAppBuilt"] is False
assert record["apkInstalled"] is False
assert record["androidScenariosRun"] is False
assert record["projectionInputsByteIdentical"] is True
sdk = Path(record["sdk"])
env = dict(os.environ, JAVA_HOME=record["jdk"])
out = run / "artifact-audit"
out.mkdir(exist_ok=False)
commands = []


def utc():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def sha(data):
    return hashlib.sha256(data).hexdigest()


def file_meta(path):
    data = path.read_bytes()
    return {"path": str(path), "bytes": len(data), "sha256": sha(data)}


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")


def command(label, argv):
    """Keep command/exit/raw-output metadata even if a tool reports failure."""
    started = utc()
    argv = [str(arg) for arg in argv]
    entry = {"label": label, "argv": argv, "cwd": str(repo), "startedUtc": started}
    try:
        result = subprocess.run(argv, cwd=repo, env=env, stdout=subprocess.PIPE,
                                stderr=subprocess.PIPE, timeout=120, check=False)
        stdout, stderr = result.stdout, result.stderr
        entry["exitCode"] = result.returncode
    except BaseException as error:
        stdout = getattr(error, "stdout", None) or b""
        stderr = getattr(error, "stderr", None) or b""
        entry.update(exitCode=None, exceptionType=type(error).__name__, exception=str(error))
        raise
    finally:
        entry["endedUtc"] = utc()
        for channel, data in (("stdout", stdout), ("stderr", stderr)):
            path = out / (label + "." + channel + ".log")
            path.write_bytes(data)
            entry[channel] = file_meta(path)
        commands.append(entry)
        write_json(out / "commands.json", commands)
    assert result.returncode == 0, (label, result.returncode)
    return stdout


def source_methods(data, name):
    text = data.decode("utf-8")
    package_match = re.search(r"package\s+([^;]+);", text)
    assert package_match, name
    methods = re.findall(r"@Test(?:\([^)]*\))?\s+public\s+void\s+(\w+)\s*\(", text)
    assert len(methods) == text.count("@Test"), (name, "unparsed @Test annotation")
    identities = [package_match.group(1) + "." + Path(name).stem + "#" + method for method in methods]
    assert len(set(identities)) == len(identities), (name, "duplicate method")
    return identities


def audit_tests():
    source_roots = ["docs/research/m10-jpeg-r3/host/" + module + "/src/test/java"
                    for module in ("probe", "outsider")]
    baseline_files = command("baseline-test-paths", ["git", "ls-tree", "-r", "--name-only", BASELINE,
                                                     "--"] + source_roots).decode("utf-8").splitlines()
    baseline_files = [name for name in baseline_files if name.endswith(".java")]
    baseline_identities, old_files = {}, {}
    for index, name in enumerate(baseline_files):
        original = command("baseline-test-%02d" % index, ["git", "show", BASELINE + ":" + name])
        current = (repo / name).read_bytes()
        assert current == original, (name, "baseline test bytes changed")
        old_files[name] = {"bytes": len(original), "sha256": sha(original)}
        for identity in source_methods(original, name):
            assert identity not in baseline_identities
            baseline_identities[identity] = name
    assert len(baseline_identities) == BASELINE_METHODS, len(baseline_identities)
    all_methods = {}
    for root in source_roots:
        for path in sorted((repo / root).rglob("*.java")):
            name = path.relative_to(repo).as_posix()
            for identity in source_methods(path.read_bytes(), name):
                assert identity not in all_methods, identity
                all_methods[identity] = name
    assert set(baseline_identities).issubset(all_methods)
    results, xmls = {}, []
    for path in sorted((run / "reports").rglob("TEST-*.xml")):
        root = ET.parse(path).getroot()
        assert root.tag == "testsuite", (path, root.tag)
        for field in ("failures", "errors", "skipped"):
            assert int(root.attrib[field]) == 0, (path, field)
        cases = list(root.iter("testcase"))
        assert int(root.attrib["tests"]) == len(cases), path
        xmls.append(file_meta(path))
        for case in cases:
            identity = case.attrib["classname"] + "#" + case.attrib["name"]
            assert identity not in results, (identity, "duplicate XML result")
            assert not list(case), (identity, "not an unqualified PASS")
            results[identity] = "PASS"
    assert set(results) == set(all_methods), {"extraXml": sorted(set(results) - set(all_methods)),
                                              "missingXml": sorted(set(all_methods) - set(results))}
    new_methods = {key: value for key, value in all_methods.items() if key not in baseline_identities}
    return {"uniquePassMethods": len(results), "failures": 0, "errors": 0, "skipped": 0,
            "baseline": BASELINE, "oldUnchangedMethods": len(baseline_identities),
            "oldUnchangedTestFiles": old_files, "newMethodCount": len(new_methods),
            "newMethods": new_methods, "allSourceMethods": all_methods,
            "allXmlResults": results, "xmlFiles": xmls}


def signing(label, apk):
    text = command(label, [sdk / "build-tools/36.0.0/apksigner.bat", "verify", "--verbose",
                           "--print-certs", apk]).decode("utf-8", errors="strict")
    certs = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([0-9a-f]+)", text)
    assert certs == [CERTIFICATE_SHA256], (label, certs)
    return certs[0]


def audit_apk(module):
    expected = record[module + "Apk"]
    apk = Path(expected["path"])
    actual = file_meta(apk)
    assert actual["bytes"] == expected["bytes"] and actual["sha256"] == expected["sha256"]
    manifest_text = command(module + "-manifest", [sdk / "cmdline-tools/latest/bin/apkanalyzer.bat",
                                                  "manifest", "print", apk]).decode("utf-8", errors="strict")
    manifest = ET.fromstring(manifest_text)
    android = "{http://schemas.android.com/apk/res/android}"
    package = "com.cooper.lithic.m10r3." + module
    assert manifest.get("package") == package
    assert not [element for element in manifest if element.tag.startswith("uses-permission")]
    uses = manifest.find("uses-sdk")
    assert uses is not None and uses.get(android + "minSdkVersion") == "26"
    assert uses.get(android + "targetSdkVersion") == "36"
    worker_meta = None
    if module == "probe":
        workers = [service for service in manifest.findall("application/service")
                   if service.get(android + "name") in (".WorkerService", package + ".WorkerService")]
        assert len(workers) == 1
        worker = workers[0]
        assert worker.get(android + "exported") == "false"
        assert worker.get(android + "process") == ":jpeg_worker"
        worker_meta = {"name": worker.get(android + "name"), "exported": False, "process": ":jpeg_worker"}
    certificate = signing(module + "-signature", apk)
    command(module + "-badging", [sdk / "build-tools/36.0.0/aapt2.exe", "dump", "badging", apk])
    dex_text = command(module + "-dex", [sdk / "cmdline-tools/latest/bin/apkanalyzer.bat", "dex",
                                        "packages", "--defined-only", apk]).decode("utf-8", errors="strict")
    for forbidden in ("org.junit", "org.hamcrest", "androidx.test"):
        assert forbidden not in dex_text, (module, forbidden)
    with zipfile.ZipFile(apk) as archive:
        entries = archive.namelist()
        assert not any(name.startswith("lib/") or name.endswith(".so") for name in entries)
        assert "classes.dex" in entries
    return dict(actual, applicationId=package, certificateSHA256=certificate, minSdk=26, targetSdk=36,
                permissions=[], nativeLibraries=False, testLibraries=False, worker=worker_meta)


def audit_historical_apks():
    histories = {}
    for stage, relative in HISTORICAL_RESULTS.items():
        result_path = repo / relative
        historical = json.loads(result_path.read_text(encoding="utf-8"))
        assert historical["exitCode"] == 0
        artifacts = {}
        for module in ("probe", "outsider"):
            expected = historical[module + "Apk"]
            path = Path(expected["path"])
            actual = file_meta(path)
            assert actual["bytes"] == expected["bytes"] and actual["sha256"] == expected["sha256"], (stage, module)
            assert path.resolve() != Path(record[module + "Apk"]["path"]).resolve(), (stage, "overwritten path")
            artifacts[module] = dict(actual, certificateSHA256=signing("historical-" + stage + "-" + module, path))
        histories[stage] = {"sourceResult": file_meta(result_path), "unchanged": True, "artifacts": artifacts}
    return histories


summary = {"hostOnly": True, "androidRun": False, "productionAppBuilt": False,
           "apkInstalled": False, "baseline": BASELINE, "startedUtc": utc(), "status": "RUNNING"}
try:
    inputs = json.loads((run / "inputs.json").read_text(encoding="utf-8"))
    projected = Path(record["asciiSourceProjection"])
    host_root = repo / "docs/research/m10-jpeg-r3/host"
    verified_inputs = {}
    for name, expected in inputs.items():
        path = repo / name
        actual = file_meta(path)
        assert actual["bytes"] == expected["bytes"] and actual["sha256"] == expected["sha256"], name
        projection = projected / path.relative_to(host_root)
        assert projection.read_bytes() == path.read_bytes(), (name, "projection differs")
        verified_inputs[name] = dict(actual, projectionPath=str(projection))
    assert verified_inputs
    summary["inputCount"] = len(verified_inputs)
    summary["inputs"] = verified_inputs
    summary["buildResult"] = file_meta(run / "result.json")
    summary["gradleLog"] = file_meta(run / "gradle.log")
    log_bytes = (run / "gradle.log").read_bytes()
    assert b"BUILD SUCCESSFUL" in log_bytes and b"BUILD FAILED" not in log_bytes
    summary["tests"] = audit_tests()
    lint = {}
    for module in ("probe", "outsider"):
        path = run / "reports" / module / "reports/lint-results-debug.xml"
        issues = list(ET.parse(path).getroot().iter("issue"))
        unknown = [issue.get("severity") for issue in issues
                   if issue.get("severity") not in ("Error", "Fatal", "Warning", "Information", "Ignore")]
        assert not unknown, unknown
        lint[module] = {"errors": sum(issue.get("severity") in ("Error", "Fatal") for issue in issues),
                        "warnings": sum(issue.get("severity") == "Warning" for issue in issues),
                        "issues": [{"id": issue.get("id"), "severity": issue.get("severity"),
                                    "message": issue.get("message")} for issue in issues],
                        "report": file_meta(path)}
        assert lint[module]["errors"] == 0
    summary["lint"] = lint
    summary["apk"] = {module: audit_apk(module) for module in ("probe", "outsider")}
    summary["historicalApksUnchanged"] = audit_historical_apks()
    summary["status"] = "PASS"
except BaseException as error:
    summary.update(status="FAIL", errorType=type(error).__name__, error=str(error))
    (out / "failure-traceback.log").write_text(traceback.format_exc(), encoding="utf-8")
    raise
finally:
    summary["endedUtc"] = utc()
    summary["commands"] = commands
    write_json(out / "result.json", summary)
print(json.dumps({"status": summary["status"], "inputCount": summary["inputCount"],
                  "tests": summary["tests"]["uniquePassMethods"],
                  "oldUnchangedMethods": summary["tests"]["oldUnchangedMethods"],
                  "newMethods": summary["tests"]["newMethodCount"],
                  "lint": summary["lint"], "apk": summary["apk"],
                  "historicalStagesUnchanged": list(summary["historicalApksUnchanged"])}, indent=2))
