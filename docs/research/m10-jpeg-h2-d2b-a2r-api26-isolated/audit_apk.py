"""Read-only host audit of the locked A1 APK; never builds or contacts Android.

Writes a new evidence/apk-audit directory exclusively. Reusing output is refused.
"""
from pathlib import Path
import datetime
import hashlib
import json
import os
import subprocess
import traceback
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
OUT = HERE / "evidence/apk-audit"
OUT.mkdir(parents=True, exist_ok=False)
APK = Path("E:/CODEX/.codex/worktrees/m10-jpeg-h2-d2b-api26-a1/砺石新APP/build/a1-artifacts/20261017T142151.230105Z/probe-debug.apk")
SDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/sdk")
JDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1")
A1 = "971f742452422ce6b31462123c351d112eb48101"
A2 = "349264371b8b9849dfe83a5ddee74f204129d4ad"
EXPECTED_APK_SHA = "0d98316faf6cb18b589bad526a5d8260cea825a408ed949f32855bd7a7d31b8a"
EXPECTED_CERT = "29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9"
BUILD = ROOT / "docs/research/m10-jpeg-h2-d2b-api26-a1/evidence/build-runs/20261017T142151.230105Z"
COMMANDS = []
ENV = dict(os.environ, JAVA_HOME=str(JDK))


def utc():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def sha(data):
    return hashlib.sha256(data).hexdigest()


def meta(path):
    raw = path.read_bytes()
    return {"path": str(path), "bytes": len(raw), "sha256": sha(raw)}


def write(name, value):
    (OUT / name).write_text(json.dumps(value, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")


def command(label, args):
    args = [str(x) for x in args]
    entry = {"label": label, "argv": args, "cwd": str(ROOT), "startedUtc": utc(),
             "environmentOverride": {"JAVA_HOME": str(JDK)}}
    stdout = stderr = b""
    try:
        result = subprocess.run(args, cwd=ROOT, env=ENV, capture_output=True, timeout=120, check=False)
        stdout, stderr = result.stdout, result.stderr
        entry["exitCode"] = result.returncode
    except BaseException as error:
        stdout = getattr(error, "stdout", None) or b""
        stderr = getattr(error, "stderr", None) or b""
        entry.update(exitCode=None, exceptionType=type(error).__name__, exception=str(error))
        raise
    finally:
        entry["endedUtc"] = utc()
        for stream, raw in (("stdout", stdout), ("stderr", stderr)):
            path = OUT / (label + "." + stream + ".log")
            path.write_bytes(raw)
            entry[stream] = meta(path)
        COMMANDS.append(entry)
        write("commands.json", COMMANDS)
    assert result.returncode == 0, (label, result.returncode)
    return stdout


result = {"startedUtc": utc(), "status": "UNKNOWN", "hostOnly": True,
          "apkBuilt": False, "apkInstalled": False, "adbInvoked": False,
          "sourceA1": A1, "sourceA2": A2,
          "sourceCorrespondenceLimit": "Locked A1 build provenance plus input byte comparison; no rebuild or source-to-bytecode proof."}
try:
    apk_before = meta(APK)
    assert apk_before["bytes"] == 83761 and apk_before["sha256"] == EXPECTED_APK_SHA
    result["apkBefore"] = apk_before
    build_record = json.loads((BUILD / "result.json").read_text(encoding="utf-8"))
    assert build_record["probeApk"]["sha256"] == EXPECTED_APK_SHA
    assert build_record["projectionInputsByteIdentical"] is True
    assert build_record["exitCode"] == 0
    inputs = json.loads((BUILD / "inputs.json").read_text(encoding="utf-8"))
    current_inputs = {}
    for name, expected in inputs.items():
        observed = meta(ROOT / name)
        assert observed["bytes"] == expected["bytes"] and observed["sha256"] == expected["sha256"], name
        current_inputs[name] = observed
    write("source-input-comparison.json", current_inputs)
    result["inputCount"] = len(inputs)
    result["allBuildInputsByteIdentical"] = True
    result["lockedBuildRecords"] = [meta(BUILD / "result.json"), meta(BUILD / "inputs.json"),
                                    meta(BUILD / "artifact-audit/result.json")]
    current_head = command("current-head", ["git", "rev-parse", "HEAD"]).decode("ascii").strip()
    assert current_head == A2
    result["observedHead"] = current_head
    delta = command("a1-a2-research-source-diff", ["git", "diff", "--name-only", A1, A2, "--", "docs/research/m10-jpeg-r3/host"])
    assert not delta.strip(), "A1/A2 research APK source differs"
    signer = SDK / "build-tools/36.0.0/apksigner.bat"
    analyzer = SDK / "cmdline-tools/latest/bin/apkanalyzer.bat"
    badging = SDK / "build-tools/36.0.0/aapt2.exe"
    result["tools"] = [meta(signer), meta(analyzer), meta(badging)]
    signature = command("probe-signature", [signer, "verify", "--verbose", "--print-certs", APK]).decode("utf-8")
    assert "Verified using v2 scheme (APK Signature Scheme v2): true" in signature
    assert "certificate SHA-256 digest: " + EXPECTED_CERT in signature
    result["certificateSha256"] = EXPECTED_CERT
    manifest_bytes = command("probe-manifest", [analyzer, "manifest", "print", APK])
    manifest = ET.fromstring(manifest_bytes)
    android = "{http://schemas.android.com/apk/res/android}"
    assert manifest.get("package") == "com.cooper.lithic.m10r3.probe"
    uses = manifest.find("uses-sdk")
    assert uses.get(android + "minSdkVersion") == "26"
    assert uses.get(android + "targetSdkVersion") == "36"
    assert len(manifest.findall("uses-permission")) == 0
    application = manifest.find("application")
    assert application.get(android + "allowBackup") == "false"
    services = application.findall("service")
    assert len(services) == 1
    worker = services[0]
    assert worker.get(android + "name") == "com.cooper.lithic.m10r3.probe.WorkerService"
    assert worker.get(android + "exported") == "false"
    assert worker.get(android + "process") == ":jpeg_worker"
    command("probe-badging", [badging, "dump", "badging", APK])
    result["manifest"] = {"package": manifest.get("package"), "minSdk": 26, "targetSdk": 36,
                          "usesPermissionCount": 0, "workerExported": False, "workerProcess": ":jpeg_worker", "allowBackup": False}
    result["apkAfter"] = meta(APK)
    assert result["apkAfter"] == apk_before
    result["status"] = "PASS"
except BaseException as error:
    result.update(status="FAIL", errorType=type(error).__name__, error=str(error), traceback=traceback.format_exc())
finally:
    result["endedUtc"] = utc()
    result["auditScript"] = meta(Path(__file__))
    write("result.json", result)
print(json.dumps({"status": result["status"], "evidence": str(OUT), "inputCount": result.get("inputCount")}, ensure_ascii=True))
raise SystemExit(0 if result["status"] == "PASS" else 1)
