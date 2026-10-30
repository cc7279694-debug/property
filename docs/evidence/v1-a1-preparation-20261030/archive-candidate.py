"""Exclusive stable archive: never replace a file or use Gradle's output as sole approval copy."""
from pathlib import Path
import datetime, hashlib, json, shutil, sys

root = Path.cwd()
ev = root / "docs/evidence/v1-a1-preparation-20261030"
identity = json.loads((ev / "artifact/identity.json").read_text(encoding="utf-8"))
parameters = json.loads((ev / "parameter-results.json").read_text(encoding="utf-8"))
assert len(parameters) == 15
assert all(p["status"] in ("EXPECTED_IDENTITY", "EXPECTED_REJECTION") for p in parameters)
archive = root / "verification-candidates/v1-a1"
assert not archive.exists(), "Existing candidate identity must be preserved"
assert archive.resolve() == root.resolve() / "verification-candidates/v1-a1"
parent = archive.parent
assert not parent.is_symlink() and not parent.is_junction()
archive.mkdir(parents=True)
rows = []
for kind, filename in (("app", "Lithic-V1-A1-App-debug.apk"), ("test", "Lithic-V1-A1-AndroidTest.apk")):
    source = Path(identity[kind]["path"])
    expected = identity[kind]
    digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    assert source.stat().st_size == expected["bytes"] and digest(source) == expected["sha256"]
    destination = archive / filename
    with source.open("rb") as src, destination.open("xb") as dst:
        shutil.copyfileobj(src, dst)
    assert destination.stat().st_size == expected["bytes"] and digest(destination) == expected["sha256"]
    rows.append({**expected, "buildPath": str(source), "archivePath": str(destination), "copySha256Matched": True})
record = {"candidate": "V1-A1 / INDEPENDENT ACCEPTANCE ONLY / NOT A USER PREVIEW",
          "sourceCommit": identity["sourceCommit"], "sourceTree": identity["sourceTree"],
          "archivedUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(), "artifacts": rows,
          "androidExecution": "NOT_RUN", "deviceOperations": 0}
text = json.dumps(record, ensure_ascii=False, indent=2) + "\n"
with (archive / "identity.json").open("x", encoding="utf-8") as f:
    f.write(text)
with (archive / "README.txt").open("x", encoding="utf-8") as f:
    f.write("LITHIC V1-A1 独立综合验收候选，不是用户体验版或正式发行包。\n"
            "App保留正常LithicApplication/MainActivity；Test仅在获授权时使用M13 plain宿主。\n"
            "当前未授权设备安装或执行；不得覆盖Preview12/M10/M22/C3/C6。\n"
            "两个APK的身份、大小、SHA及源码绑定见identity.json。APK不是Git交付物。\n")
(ev / "archive.json").write_text(text, encoding="utf-8")
(ev / "archive-command.json").write_text(json.dumps({"argv": [sys.executable, str(Path(__file__).resolve())],
    "exitCode": 0, "archiveJsonSha256": hashlib.sha256((ev / "archive.json").read_bytes()).hexdigest(),
    "copiedApks": 2, "overwrittenFiles": 0}, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"archivedApks": 2, "allCopiesMatched": True, "sourceCommit": identity["sourceCommit"]}))
