"""C2 host-only read audit of the immutable A1 APK; no Android connection/build.

Every output is newly created under evidence/apk-audit. Refuses reuse.
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
APK = Path("E:/CODEX/.codex/worktrees/m10-jpeg-h2-d2b-api26-a1/砺石新APP/build/a1-artifacts/20261017T142151.230105Z/probe-debug.apk")
SDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/sdk")
JDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1")
EXPECTED_SHA = "0d98316faf6cb18b589bad526a5d8260cea825a408ed949f32855bd7a7d31b8a"
EXPECTED_CERT = "29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9"
EXPECTED_BASE = "797eb591cc8293d9d68503243d2a94462fe79610"
EXPECTED_BRANCH = "research/m10-jpeg-h2-d2b-a2r-c2-isolated"
BUILD = ROOT / "docs/research/m10-jpeg-h2-d2b-api26-a1/evidence/build-runs/20261017T142151.230105Z"
ENV = dict(os.environ, JAVA_HOME=str(JDK))
COMMANDS = []


def utc():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def meta(path):
    raw = path.read_bytes()
    return {"path": str(path), "bytes": len(raw), "sha256": sha(raw)}


def write(name, value):
    with (OUT / name).open("x", encoding="utf-8", newline="\n") as stream:
        stream.write(json.dumps(value, ensure_ascii=True, indent=2) + "\n")


def command(label, args):
    argv = [str(item) for item in args]
    item = {"label": label, "argv": argv, "cwd": str(ROOT), "startedUtc": utc(),
            "environmentOverride": {"JAVA_HOME": str(JDK)}, "timeoutSeconds": 120}
    stdout = stderr = b""
    try:
        completed = subprocess.run(argv, cwd=ROOT, env=ENV, capture_output=True, timeout=120, check=False)
        stdout, stderr = completed.stdout, completed.stderr
        item["exitCode"] = completed.returncode
    except BaseException as error:
        stdout = getattr(error, "stdout", None) or b""
        stderr = getattr(error, "stderr", None) or b""
        item.update(exitCode=None, exceptionType=type(error).__name__, exception=str(error))
        raise
    finally:
        item["endedUtc"] = utc()
        for label_stream, raw in (("stdout", stdout), ("stderr", stderr)):
            path = OUT / (label + "." + label_stream + ".log")
            with path.open("xb") as stream:
                stream.write(raw)
            item[label_stream] = meta(path)
        COMMANDS.append(item)
    assert completed.returncode == 0, (label, completed.returncode)
    return stdout


def audit():
    OUT.mkdir(parents=True, exist_ok=False)
    result = {"startedUtc": utc(), "status": "UNKNOWN", "hostOnly": True,
              "apkBuilt": False, "apkInstalled": False, "adbInvoked": False,
              "sourceA1": "971f742452422ce6b31462123c351d112eb48101",
              "sourceC1": EXPECTED_BASE, "branch": EXPECTED_BRANCH,
              "sourceCorrespondenceLimit": "Locked A1 build records plus current input byte comparison; no rebuild or source-to-bytecode proof."}
    try:
        # These Git operations are reads and cannot contact a remote or Android.
        head = command("current-head", ["git", "rev-parse", "HEAD"]).decode("ascii").strip()
        branch = command("current-branch", ["git", "branch", "--show-current"]).decode("ascii").strip()
        assert head == EXPECTED_BASE and branch == EXPECTED_BRANCH
        result["observedHead"] = head
        before = meta(APK)
        assert before["bytes"] == 83761 and before["sha256"] == EXPECTED_SHA
        result["apkBefore"] = before
        build = json.loads((BUILD / "result.json").read_text(encoding="utf-8"))
        assert build["exitCode"] == 0
        assert build["projectionInputsByteIdentical"] is True
        assert build["probeApk"]["sha256"] == EXPECTED_SHA
        inputs = json.loads((BUILD / "inputs.json").read_text(encoding="utf-8"))
        compared = {}
        for name, expected in inputs.items():
            actual = meta(ROOT / name)
            assert (actual["bytes"], actual["sha256"]) == (expected["bytes"], expected["sha256"]), name
            compared[name] = actual
        write("source-input-comparison.json", compared)
        result.update(inputCount=len(compared), allBuildInputsByteIdentical=True)
        result["lockedBuildRecords"] = [meta(BUILD / "result.json"), meta(BUILD / "inputs.json"),
                                        meta(BUILD / "artifact-audit/result.json")]
        signer = SDK / "build-tools/36.0.0/apksigner.bat"
        analyzer = SDK / "cmdline-tools/latest/bin/apkanalyzer.bat"
        aapt = SDK / "build-tools/36.0.0/aapt2.exe"
        result["tools"] = [meta(signer), meta(analyzer), meta(aapt), meta(JDK / "bin/java.exe")]
        signature = command("probe-signature", [signer, "verify", "--verbose", "--print-certs", APK]).decode("utf-8")
        assert "Verified using v2 scheme (APK Signature Scheme v2): true" in signature
        assert "Number of signers: 1" in signature
        assert "certificate SHA-256 digest: " + EXPECTED_CERT in signature
        result["certificateSha256"] = EXPECTED_CERT
        manifest = ET.fromstring(command("probe-manifest", [analyzer, "manifest", "print", APK]))
        android = "{http://schemas.android.com/apk/res/android}"
        assert manifest.get("package") == "com.cooper.lithic.m10r3.probe"
        sdk = manifest.find("uses-sdk")
        assert sdk.get(android + "minSdkVersion") == "26"
        assert sdk.get(android + "targetSdkVersion") == "36"
        assert len(manifest.findall("uses-permission")) == 0
        assert len(manifest.findall("uses-permission-sdk-23")) == 0
        app = manifest.find("application")
        assert app.get(android + "allowBackup") == "false"
        assert app.get(android + "name") == "com.cooper.lithic.m10r3.probe.ProbeApplication"
        activities = app.findall("activity")
        assert len(activities) == 1
        assert activities[0].get(android + "name") == "com.cooper.lithic.m10r3.probe.ProbeActivity"
        assert activities[0].get(android + "exported") == "true"
        services = app.findall("service")
        assert len(services) == 1
        worker = services[0]
        assert worker.get(android + "name") == "com.cooper.lithic.m10r3.probe.WorkerService"
        assert worker.get(android + "exported") == "false"
        assert worker.get(android + "process") == ":jpeg_worker"
        command("probe-badging", [aapt, "dump", "badging", APK])
        result["manifest"] = {"package": manifest.get("package"), "minSdk": 26, "targetSdk": 36,
                              "usesPermissionCount": 0, "workerExported": False,
                              "workerProcess": ":jpeg_worker", "allowBackup": False,
                              "application": app.get(android + "name"),
                              "activity": activities[0].get(android + "name")}
        result["apkAfter"] = meta(APK)
        assert result["apkAfter"] == before
        result["status"] = "PASS"
    except BaseException as error:
        result.update(status="FAIL", errorType=type(error).__name__, error=str(error), traceback=traceback.format_exc())
    finally:
        result["endedUtc"] = utc()
        result["auditScript"] = meta(Path(__file__))
        write("commands.json", COMMANDS)
        write("result.json", result)
    print(json.dumps({"status": result["status"], "evidence": str(OUT), "inputCount": result.get("inputCount")}, ensure_ascii=True))
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(audit())
