"""Bounded offline identity/archive recheck and existing JVM container characterization.
Never builds native/App, downloads, installs, invokes devices, or treats archives as new execution.
"""
from pathlib import Path
import argparse, base64, datetime, hashlib, json, os, re, subprocess

ROOT = Path(__file__).resolve().parents[3]
BASE = "88e7edb201991f386b91b078f574a79f07ecde7b"
REF = "732333b7a2803ec1f4487e437a581979a270e3ee"
CI = "791d44e8ee2b3636031a1073c3df690585309853"
NATIVE = "docs/research/m10-jpeg-e01-native/"
ASSETS = "app/src/androidTest/assets/m10_jpeg_integrity/"
API36 = "docs/research/m10-api36-uid-resume/evidence/m10-uid-20261019-040339-e0b26457/"
LOCKS = {
    "complete.jpg": (59334, "2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907"),
    "entropy_cut_eoi_retained.jpg": (20194, "f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a"),
}

def sha(raw): return hashlib.sha256(raw).hexdigest()
def git(ref, path):
    raw = subprocess.check_output(["git", "show", ref + ":" + path], cwd=ROOT, timeout=10)
    if len(raw) > 2 * 1024 * 1024: raise RuntimeError("Audit source exceeded fixed bound")
    return raw

def save(out, name, value):
    with (out / name).open("x", encoding="utf-8", newline="\n") as f:
        json.dump(value, f, indent=2, ensure_ascii=True); f.write("\n")

def run(out, name, argv):
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    result = subprocess.run(argv, cwd=ROOT, stdin=subprocess.DEVNULL, capture_output=True, timeout=30)
    receipt = {"argv": [str(x) for x in argv], "startedUtc": started,
               "endedUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(), "exitCode": result.returncode}
    for label, raw in (("stdout", result.stdout), ("stderr", result.stderr)):
        (out / (name + "." + label)).write_bytes(raw)
        receipt[label + "Bytes"] = len(raw); receipt[label + "Sha256"] = sha(raw)
    save(out, name + ".json", receipt)
    if result.returncode: raise RuntimeError(name + " failed; raw result preserved")
    return result.stdout.decode("utf-8", errors="replace")

def main():
    parser = argparse.ArgumentParser(); parser.add_argument("--out", required=True); args = parser.parse_args()
    out = (ROOT / args.out).resolve(); out.relative_to(ROOT / "docs/evidence")
    out.mkdir(exist_ok=False); (out / ".gitattributes").write_text("* -text -whitespace\n", encoding="utf-8")
    scratch = ROOT / "build/c6-jpeg-admission/offline-probe"; scratch.mkdir(exist_ok=False)
    assert subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip() == BASE
    inputs = {}; paths = []
    for name, (length, digest) in LOCKS.items():
        raw = git(REF, ASSETS + name); assert len(raw) == length and sha(raw) == digest
        inputs[name] = {"source": REF + ":" + ASSETS + name, "bytes": len(raw), "sha256": sha(raw)}
        path = scratch / name; path.write_bytes(raw); paths.append(path)
    complete, damaged = [p.read_bytes() for p in paths]
    assert damaged == complete[:20192] + bytes.fromhex("ffd9")
    cores = {}
    for name in ("strict_probe.c", "strict_core.h", "budget.c", "budget.h", "jmem_budget.c"):
        raw = git(REF, NATIVE + name); assert raw == git(CI, NATIVE + name)
        cores[name] = {"sha256": sha(raw), "bytes": len(raw), "sameAsSuccessfulCiSource": True}
    source = "app/src/test/java/com/cooper/lithic/data/backup/BackupJpegInspectionTest.kt"
    text = git(BASE, source).decode("utf-8").split("private fun jpeg(progressive:Boolean)", 1)[1].split("private fun withJpeg", 1)[0]
    strings = re.findall(r'"([A-Za-z0-9+/=]+)"', text); assert len(strings) == 2
    for name, encoded, length, digest in [
        ("existing_small_progressive.jpg", strings[0], 554, "fde98cbb8845e8863645fdbf31e7713760c9a8af4605a078f500866569f97a55"),
        ("existing_small_baseline.jpg", strings[1], 654, "da170836bb2630908e946fc80da3352e071d8e88410cb588f7ac173635ea05f4"),
    ]:
        raw = base64.b64decode(encoded, validate=True); assert len(raw) == length and sha(raw) == digest
        path = scratch / name; path.write_bytes(raw); paths.append(path)
        inputs[name] = {"source": BASE + ":" + source, "bytes": length, "sha256": digest,
                        "scope": "existing small synthetic fixture; NOT the unavailable historical R1 progressive bytes"}
    archive_path = "docs/research/m10-jpeg-accelerated/RESULTS.json"
    raw = git(REF, archive_path); archive = json.loads(raw)
    assert archive["codeCommit"] == CI
    outcomes = archive["lockedCoreOutcomes"]
    assert len(outcomes) == 4
    for item in outcomes:
        is_damaged = item["label"].endswith("entropy_cut_eoi_retained.jpg")
        assert item["status"] == ("IMAGE_REJECT" if is_damaged else "ACCEPT")
        assert item["live"] == item["badFrees"] == 0 and item["fdClosed"]
        assert item["finishComplete"] is (not is_damaged)
    for compiler in ("gcc", "clang"):
        for item in archive["native"][compiler]["large"].values():
            assert item["status"] == "ACCEPT" and item["rowsRead"] == 5000 and item["finishComplete"]
            assert item["live"] == item["liveBlocks"] == item["badFrees"] == 0
    historical = {"scope": "ARCHIVE_RECHECK_ONLY_NOT_RERUN", "ref": REF,
                  "nativeResultPath": archive_path, "nativeResultSha256": sha(raw),
                  "lockedCoreOutcomes": outcomes, "historicalHost40MP": {k: v["large"] for k,v in archive["native"].items()},
                  "api36OriginalFailureMethods": []}
    for index in (3, 4, 5):
        path = API36 + f"method-{index:02d}/result.json"; raw = git(REF, path); value = json.loads(raw)
        assert value["status"] == value["instrumentation"]["status"] == "PASS"
        assert value["instrumentation"]["testCount"] == 1
        junit_path = API36 + f"method-{index:02d}/instrumentation.txt"; junit = git(REF, junit_path)
        assert sha(junit) == value["instrumentation"]["decodedSHA256"]
        assert b"OK (1 test)" in junit
        (out / f"ARCHIVED-api36-method-{index:02d}.txt").write_bytes(junit)
        historical["api36OriginalFailureMethods"].append({"method": value["method"], "status": "ARCHIVED_PASS",
            "resultPath": path, "resultSha256": sha(raw), "junitPath": junit_path, "junitSha256": sha(junit)})
    save(out, "inputs-and-source.json", {"fixtures": inputs, "strictCore": cores, "removedBytes": 39140})
    save(out, "archived-result-recheck.json", historical)
    java = Path("C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1/bin")
    jars = list((Path.home()/".gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.3.10").glob("*/kotlin-stdlib-2.3.10.jar"))
    assert len(jars) == 1, "Use the already installed matching stdlib only"
    app_classes = ROOT / "app/build/tmp/kotlin-classes/debug"; classes = scratch / "classes"; classes.mkdir()
    binding = {}
    for name in ("BackupJpegInspection.class", "BackupJpegInspection$Frame.class", "BackupMediaException.class"):
        p = app_classes / "com/cooper/lithic/data/backup" / name; binding[name] = sha(p.read_bytes())
    production_source = "app/src/main/java/com/cooper/lithic/data/backup/BackupJpegInspection.kt"
    assert (ROOT/production_source).read_text(encoding="utf-8").replace("\r\n", "\n") == git(BASE, production_source).decode("utf-8")
    save(out, "container-runtime-binding.json", {"source": BASE+":"+production_source,
        "classOrigin": "Reused C6-1 successful desktop compile output; not rebuilt as App", "classSha256": binding,
        "existingStdlibSha256": sha(jars[0].read_bytes()), "entropyDecoder": False, "AndroidExecution": False})
    classpath = os.pathsep.join([str(app_classes), str(jars[0])])
    run(out, "javac", [str(java/"javac.exe"), "-encoding", "UTF-8", "-cp", classpath, "-d", str(classes),
                       str(Path(__file__).with_name("ContainerProbe.java"))])
    stdout = run(out, "container-probe", [str(java/"java.exe"), "-Xmx64m", "-cp", str(classes)+os.pathsep+classpath,
                                           "c6audit.ContainerProbe", *map(str,paths)])
    rows = [row.split("\t") for row in stdout.splitlines()]; assert len(rows) == 4
    assert all(row[1] == "CONTAINER_ACCEPT" for row in rows)
    assert all(row[2:] == ["256", "256"] for row in rows[:2])
    assert all(row[2:] == ["7", "5"] for row in rows[2:])
    save(out, "summary.json", {"status": "OFFLINE_IDENTITY_AND_ARCHIVE_RECHECK_COMPLETE",
        "freshContainerChecks": 4, "lockedDamagedContainerAcceptedAsExpected": True,
        "freshStrictNativeExecutions": 0, "strictNative": "NOT_RUN_ENVIRONMENT_NOT_ADMITTED",
        "platformPixelDecodes": 0, "deviceOperations": 0, "newDependencies": 0,
        "archivedResultsAreNewRuns": False, "productionAppModified": False, "mediaImport": "BLOCKED"})
    print(json.dumps({"out": args.out, "containerChecks": 4, "strictNative": "NOT_RUN", "deviceOperations": 0}))

if __name__ == "__main__": main()
