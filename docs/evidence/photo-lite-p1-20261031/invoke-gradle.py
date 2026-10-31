"""Desktop-only command receipts; no device task, clean, or network resolution."""
from pathlib import Path
import datetime, hashlib, json, os, subprocess, sys

root = Path.cwd()
assert root.name == "砺石新APP" and "lithic-m13" in str(root)
label, *arguments = sys.argv[1:]
assert label.replace("-", "").isalnum()
assert not any(any(w in a.lower() for w in ("connected", "install", "uninstall", "clean", "manageddevice")) for a in arguments)
out = root / "docs/evidence/photo-lite-p1-20261031" / label
out.mkdir()
memory = json.loads(subprocess.check_output([
    "powershell", "-NoProfile", "-Command",
    "(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory | ConvertTo-Json",
], text=True)) / 1024**2
assert memory >= 4, f"RAM gate blocked: {memory:.3f} GiB"
env = dict(os.environ)
env["JAVA_HOME"] = "C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1"
env["PATH"] = env["JAVA_HOME"] + "/bin;" + env["PATH"]
argv = [str(root / "gradlew.bat"), "--offline", "--console=plain", "--max-workers=1", "--no-daemon",
        "-Pkotlin.compiler.execution.strategy=in-process", *arguments]
receipt = dict(argv=argv, freeMemoryGiB=memory, sourceCommit=subprocess.check_output([
    "git", "rev-parse", "HEAD"], text=True).strip(), startedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
    exitCode=None, deviceOperations=0, androidExecution="NOT_RUN")
record = out / "command.json"
record.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
with (out / "stdout.txt").open("xb") as stdout, (out / "stderr.txt").open("xb") as stderr:
    result = subprocess.run(argv, env=env, stdout=stdout, stderr=stderr)
receipt.update(exitCode=result.returncode, finishedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat())
for name in ("stdout", "stderr"):
    data = (out / (name + ".txt")).read_bytes()
    receipt[name + "Bytes"] = len(data)
    receipt[name + "Sha256"] = hashlib.sha256(data).hexdigest()
record.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
print(json.dumps(dict(label=label, exitCode=result.returncode, freeMemoryGiB=memory)))
sys.exit(result.returncode)
