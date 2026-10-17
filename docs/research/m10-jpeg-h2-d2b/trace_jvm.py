"""Isolated real-Java trace helper checks. Never uses Android/ADB."""
from pathlib import Path
import hashlib, json, os, subprocess, sys

repo = Path(__file__).resolve().parents[3]
label = sys.argv[1]
if label not in ("trace-red", "trace-green", "trace-green-final"):
    raise SystemExit("fixed evidence label required")
evidence = repo / "docs/research/m10-jpeg-h2-d2b/evidence" / label
evidence.mkdir(parents=True, exist_ok=False)
build = repo / "build/d2b-trace-jvm" / label
build.mkdir(parents=True, exist_ok=False)
java_root = repo / "docs/research/m10-jpeg-r3/host/probe/src"
inputs = [java_root / "main/java/com/cooper/lithic/m10r3/probe/BoundedTraceWriter.java",
          java_root / "test/java/com/cooper/lithic/m10r3/probe/BoundedTraceWriterTest.java"]
jdk = Path("C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1/bin")
gradle = Path("C:/Users/CDD/.gradle/caches/modules-2/files-2.1")
junit = next((gradle / "junit/junit/4.13.2").rglob("*.jar"))
hamcrest = next((gradle / "org.hamcrest/hamcrest-core/1.3").rglob("*.jar"))
classpath = os.pathsep.join(map(str, [build, junit, hamcrest]))
commands = [[str(jdk / "javac.exe"), "-encoding", "UTF-8", "-cp", classpath, "-d", str(build), *map(str, inputs)],
            [str(jdk / "java.exe"), "-cp", classpath, "org.junit.runner.JUnitCore", "com.cooper.lithic.m10r3.probe.BoundedTraceWriterTest"]]
results = []
for index, argv in enumerate(commands):
    done = subprocess.run(argv, cwd=repo, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
    (evidence / f"command-{index}.log").write_bytes(done.stdout)
    results.append({"argv": argv, "exitCode": done.returncode})
    if index == 0 and done.returncode:
        break
record = {"kind": "REAL_JAVA_HELPER_JVM_ONLY", "results": results,
          "inputs": {f.relative_to(repo).as_posix(): {"bytes": f.stat().st_size, "sha256": hashlib.sha256(f.read_bytes()).hexdigest()} for f in inputs}}
if label == "trace-red":
    (evidence / "BoundedTraceWriter.synchronous-legacy-seam.java.txt").write_bytes(inputs[0].read_bytes())
(evidence / "result.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
print(json.dumps(record, indent=2))
print((evidence / "command-1.log").read_text(encoding="utf-8", errors="replace") if len(results) == 2 else "compile failed")
raise SystemExit(results[-1]["exitCode"])
