"""Offline, explicit-only new-test DEX repack. No Gradle, download or device operation.

Inputs are the existing audited javac/Kotlinc outputs and pinned local cache indexes.
This does not recompile the App or old tests. A reproduced APK is a NEW identity and
must be audited separately; its SHA is not assumed to equal an earlier signed APK.
Signing credentials are read by apksigner from environment variables, never logged.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

APP_SHA = "b7c90277fa6c27e6ab314aa52c293de8e445ba6a3deba9e984ab96f4280e8a4c"
OLD_TEST_SHA = "bb00c0f6e34059170d20124eab739b43ccbc257ecc265def28906077fc0cb78c"
CERT_SHA = "29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9"
SOURCE_SHA = {
    "M10B1B2SafetyPreflightTest.kt": "5a38b5770dd9d8ed076fda6ab2dfd3a2c042b920c1c1c8dc5b6866f4319fe205",
    "M10B1B2SafetySupport.kt": "c29957cb1122d90594b4f73d1348a244f6fbd4aa5ea0f9d23f244c90b527975a",
    "M10B1CancellationSafetyTest.kt": "411ccba3102a538b1a164ed9882b05617558f8fdefc78d41a2e292d0b7d32d8a",
    "M10B1ClosureObservations.java": "5e39b35fc45cde5a5a20fb92e7fad145058da8617c9e0f1c4bbeb3f7d30bbab2",
}

def digest(raw): return hashlib.sha256(raw).hexdigest()
def read(path):
    path = Path(path).resolve()
    return Path("\\\\?\\" + str(path)).read_bytes() if os.name == "nt" else path.read_bytes()
def metadata(path):
    raw = read(path)
    return {"path": str(Path(path).resolve()), "bytes": len(raw), "sha256": digest(raw)}
def need(value, reason):
    if not value: raise ValueError(reason)
def signing(name):
    return re.fullmatch(r"META-INF/(?:[^/]+\.(?:SF|RSA|DSA|EC)|MANIFEST\.MF)", name, re.I) is not None
def save(path, value):
    with Path(path).open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(value, stream, ensure_ascii=True, indent=2); stream.write("\n")

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for flag in ("repo", "old-app", "old-test", "compiled", "cache-inputs", "d8-build-index", "jdk", "sdk", "debug-keystore", "out"):
        parser.add_argument("--" + flag, required=True, type=Path)
    args = parser.parse_args()
    need(digest(read(args.old_app)) == APP_SHA, "Locked App changed")
    need(digest(read(args.old_test)) == OLD_TEST_SHA, "Locked old Test changed")
    need(args.out.is_absolute() and str(args.out).isascii() and not args.out.exists(), "Use a fresh short ASCII output directory")
    need(len(str(args.out)) < 100 and args.out.parent.is_dir(), "Output parent must exist and path must be short")
    for parent in args.out.parents:
        need(not parent.is_symlink(), "Linked output parent is forbidden")
    for name, expected in SOURCE_SHA.items():
        need(digest(read(args.repo / "app/src/androidTest/java/com/cooper/lithic/data/media" / name)) == expected, "Source pin changed: " + name)
    complete = json.loads(read(args.compiled / "NEW_CLASSES_COMPLETE.json"))
    need(complete["sources"] == SOURCE_SHA and complete["classFiles"] == 14, "Unreviewed compiled inventory")
    method_gate = json.loads(read(args.compiled / "JUNIT_METHOD_DESCRIPTORS.json"))
    need(method_gate["status"] == "PASS_STATIC_NOT_ANDROID" and all(x["descriptor"] == "()V" for x in method_gate["methods"]), "JUnit void gate is missing")
    cache = json.loads(read(args.cache_inputs))
    cached_files = {x["path"]: x["sha256"] for x in cache["compilerAndDependencies"]}
    build = json.loads(read(args.d8_build_index))
    d8 = Path(build["d8Jar"]["path"])
    need(digest(read(d8)) == build["d8Jar"]["sha256"] and d8.name == "builder-8.13.2.jar", "Only pinned cached AGP D8 is allowed")
    old_classpath = Path(build["classpathJar"]["path"])
    need(digest(read(old_classpath)) == build["classpathJar"]["sha256"], "Cached old-classpath JAR changed")
    argv = build["argv"]
    dependencies = [Path(argv[i + 1]) for i, value in enumerate(argv) if value == "--classpath"][1:]
    library = Path(argv[argv.index("--lib") + 1])
    need(library.resolve() == (args.sdk / "platforms/android-36/android.jar").resolve(), "Unexpected Android library")
    for path in dependencies + [library]:
        need(str(path) in cached_files and digest(read(path)) == cached_files[str(path)], "Cached dependency changed")
    # Values are never copied into command evidence or printed.
    for variable in ("LITHIC_DEBUG_STORE_PASSWORD", "LITHIC_DEBUG_KEY_PASSWORD"):
        need(bool(os.environ.get(variable)), "Provide local debug signing credentials through " + variable)
    args.out.mkdir()
    commands = []
    env = os.environ.copy(); env["JAVA_HOME"] = str(args.jdk)
    env["PATH"] = str(args.jdk / "bin") + os.pathsep + env.get("PATH", "")
    def run(name, command, timeout=120):
        result = subprocess.run(command, capture_output=True, env=env, timeout=timeout)
        for kind, raw in (("stdout", result.stdout), ("stderr", result.stderr)):
            with (args.out / (name + "." + kind + ".raw")).open("xb") as stream: stream.write(raw)
        commands.append({"name": name, "argv": command, "exit": result.returncode,
                         "stdoutSHA256": digest(result.stdout), "stderrSHA256": digest(result.stderr)})
        save(args.out / (name + ".json"), commands[-1])
        need(result.returncode == 0 and not result.stderr, "Tool failed or reported diagnostics: " + name)
        return result.stdout
    program = args.out / "new-test-classes.jar"
    count = 0
    with zipfile.ZipFile(program, "x", zipfile.ZIP_DEFLATED) as archive:
        for kind in ("java", "kotlin"):
            origin = args.compiled / ("new-" + kind + "-classes")
            for file in sorted(origin.rglob("*.class")):
                relative = file.relative_to(origin).as_posix(); raw = read(file)
                need(digest(raw) == complete["classAndModuleSHA256"][kind + "/" + relative], "Compiled class changed")
                archive.writestr(relative, raw); count += 1
    need(count == 14, "Incomplete program class input")
    dexout = args.out / "dex"; dexout.mkdir()
    command = [str(args.jdk / "bin/java.exe"), "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-XX:ReservedCodeCacheSize=64m", "-XX:+UseSerialGC",
               "-cp", str(d8), "com.android.tools.r8.D8", "--min-api", "26", "--debug", "--lib", str(library), "--classpath", str(old_classpath), "--output", str(dexout)]
    for path in dependencies: command += ["--classpath", str(path)]
    run("d8", command + [str(program)])
    dexfiles = list(dexout.glob("*.dex")); need(len(dexfiles) == 1, "Expected exactly one additions DEX")
    unsigned = args.out / "test-unsigned.apk"
    with zipfile.ZipFile(args.old_test) as old:
        names = old.namelist(); need(len(names) == len(set(names)), "Duplicate original APK entry")
        index = 2
        while "classes" + str(index) + ".dex" in names: index += 1
        new_entry = "classes" + str(index) + ".dex"
        with zipfile.ZipFile(unsigned, "x") as target:
            for entry in old.infolist():
                if not signing(entry.filename): target.writestr(entry, old.read(entry.filename))
            target.writestr(new_entry, read(dexfiles[0]), compress_type=zipfile.ZIP_DEFLATED)
    aligned = args.out / "test-aligned.apk"; signed = args.out / "app-debug-androidTest.apk"
    tools = args.sdk / "build-tools/36.0.0"
    run("zipalign", [str(tools / "zipalign.exe"), "-p", "4", str(unsigned), str(aligned)])
    run("sign", ["cmd.exe", "/d", "/c", str(tools / "apksigner.bat"), "sign", "--ks", str(args.debug_keystore), "--ks-key-alias", "androiddebugkey",
                 "--ks-pass", "env:LITHIC_DEBUG_STORE_PASSWORD", "--key-pass", "env:LITHIC_DEBUG_KEY_PASSWORD", "--min-sdk-version", "26", "--v1-signing-enabled", "false", "--out", str(signed), str(aligned)])
    verified = run("verify", ["cmd.exe", "/d", "/c", str(tools / "apksigner.bat"), "verify", "--print-certs", str(signed)])
    need(re.findall(rb"certificate SHA-256 digest:\s*([a-fA-F0-9]{64})", verified) == [CERT_SHA.encode("ascii")], "Debug certificate changed")
    with zipfile.ZipFile(args.old_test) as old, zipfile.ZipFile(signed) as new:
        before = {n: digest(old.read(n)) for n in old.namelist() if not signing(n)}
        after = {n: digest(new.read(n)) for n in new.namelist() if not signing(n)}
        need(set(after) == set(before) | {new_entry} and all(after[n] == value for n, value in before.items()), "An original non-signing payload changed")
    need(digest(read(args.old_app)) == APP_SHA and digest(read(args.old_test)) == OLD_TEST_SHA, "An old APK changed")
    result = {"status": "REPACKED_REQUIRES_INDEPENDENT_DEX_AND_RUNTIME_AUDIT", "newTest": metadata(signed), "addedDex": new_entry,
              "oldAppSHA256": APP_SHA, "oldTestSHA256": OLD_TEST_SHA, "certificateSHA256": CERT_SHA,
              "oldNonSigningPayloadsPreserved": len(before), "AppBuilt": False, "Android": "NOT_RUN", "deviceCommands": 0}
    save(args.out / "RESULTS.json", result); print(json.dumps(result, ensure_ascii=True))

if __name__ == "__main__": main()
