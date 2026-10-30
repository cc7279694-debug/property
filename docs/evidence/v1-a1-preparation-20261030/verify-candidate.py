"""Bounded static APK/source audit, adapted from the already reviewed C6-2D audit.
No ADB, device, installation, or test execution is used.
"""
from pathlib import Path
import datetime, hashlib, json, os, re, struct, subprocess, traceback, xml.etree.ElementTree as ET, zipfile

ROOT = Path.cwd()
EV = ROOT / "docs/evidence/v1-a1-preparation-20261030"
OUT = EV / "artifact"
OUT.mkdir()
SDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/sdk")
TOOLS = SDK / "build-tools/36.0.0"
ANALYZER = SDK / "cmdline-tools/latest/bin/apkanalyzer.bat"
CERT = "29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9"
PACKAGE = "com.cooper.lithic.v1acceptance.debug"
RUNNER = "com.cooper.lithic.app.M13IsolatedRunner"
ENV = dict(os.environ)
ENV["JAVA_HOME"] = "C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1"
ENV["PATH"] = ENV["JAVA_HOME"] + "/bin;" + ENV["PATH"]
NS = "{http://schemas.android.com/apk/res/android}"
COMMANDS = []
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()


def capture(label, argv):
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    proc = subprocess.run(list(map(str, argv)), env=ENV, capture_output=True, timeout=90)
    for name, data in (("stdout", proc.stdout), ("stderr", proc.stderr)):
        (OUT / f"{label}.{name}").write_bytes(data)
    COMMANDS.append({"label": label, "argv": list(map(str, argv)), "exitCode": proc.returncode,
                     "startedUtc": started, "finishedUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                     "stdoutBytes": len(proc.stdout), "stdoutSha256": hashlib.sha256(proc.stdout).hexdigest(),
                     "stderrBytes": len(proc.stderr), "stderrSha256": hashlib.sha256(proc.stderr).hexdigest()})
    (OUT / "commands.json").write_text(json.dumps(COMMANDS, indent=2) + "\n", encoding="utf-8")
    assert proc.returncode == 0, label
    return proc.stdout.decode("utf-8", errors="replace")


def dex_metadata(data):
    u32 = lambda p: struct.unpack_from("<I", data, p)[0]

    def uleb(pos):
        result = shift = 0
        while True:
            value = data[pos]
            pos += 1
            result |= (value & 127) << shift
            if not value & 128:
                return result, pos
            shift += 7

    strings = []
    for i in range(u32(0x38)):
        pos = u32(u32(0x3C) + i * 4)
        _, pos = uleb(pos)
        strings.append(data[pos:data.index(b"\0", pos)].decode("utf-8", errors="replace"))
    types = [strings[u32(u32(0x44) + i * 4)] for i in range(u32(0x40))]
    classes = {types[u32(u32(0x64) + i * 32)] for i in range(u32(0x60))}
    return classes, set(strings)


def apk_metadata(apk):
    classes, strings = set(), set()
    with zipfile.ZipFile(apk) as z:
        for name in z.namelist():
            if re.fullmatch(r"classes\d*\.dex", name):
                c, s = dex_metadata(z.read(name))
                classes |= c
                strings |= s
        native = {n: hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if n.startswith("lib/") and n.endswith(".so")}
    return classes, strings, native


def main():
    pre = json.loads((EV / "preflight.json").read_text(encoding="utf-8"))
    assert json.loads((EV / "build/command.json").read_text(encoding="utf-8"))["exitCode"] == 0
    paths = {
        "app": ROOT / "app/build-v1-acceptance/outputs/apk/debug/app-debug.apk",
        "test": ROOT / "app/build-v1-acceptance/outputs/apk/androidTest/debug/app-debug-androidTest.apk",
    }
    identity = {}
    metadata = {}
    manifests = {}
    for label, apk in paths.items():
        package = PACKAGE + (".test" if label == "test" else "")
        badging = capture(label + "-badging", [TOOLS / "aapt2.exe", "dump", "badging", apk])
        xml = capture(label + "-manifest", [ANALYZER, "manifest", "print", apk])
        certificate = capture(label + "-certificate", [TOOLS / "apksigner.bat", "verify", "--verbose", "--print-certs", apk])
        assert f"name='{package}'" in badging and CERT in certificate.lower()
        m = ET.fromstring(xml)
        manifests[label] = m
        assert m.get("package") == package and m.get(NS + "sharedUserId") is None
        permissions = sorted(p.get(NS + "name") for p in m.findall("uses-permission"))
        assert "android.permission.INTERNET" not in permissions
        metadata[label] = apk_metadata(apk)
        identity[label] = {"path": str(apk), "package": package, "bytes": apk.stat().st_size,
                           "sha256": sha(apk), "certificateSha256": CERT,
                           "versionCode": m.get(NS + "versionCode"), "versionName": m.get(NS + "versionName"),
                           "permissions": permissions, "nativeEntries": metadata[label][2]}
    m = manifests["app"]
    a = m.find("application")
    assert identity["app"]["versionCode"] == "1" and identity["app"]["versionName"] == "0.1.0-v1-a1-debug"
    assert a.get(NS + "name") == "com.cooper.lithic.app.LithicApplication" and a.get(NS + "allowBackup") == "false"
    assert not m.findall("instrumentation") and a.get(NS + "testOnly") != "true"
    activities = {e.get(NS + "name"): e for e in a.findall("activity")}
    assert set(activities) == {"com.cooper.lithic.app.MainActivity", "androidx.activity.ComponentActivity"}
    main_activity = activities["com.cooper.lithic.app.MainActivity"]
    assert main_activity.get(NS + "exported") == "true"
    assert any(f.find("action").get(NS + "name") == "android.intent.action.MAIN" and
               any(c.get(NS + "name") == "android.intent.category.LAUNCHER" for c in f.findall("category")) for f in main_activity.findall("intent-filter"))
    host = activities["androidx.activity.ComponentActivity"]
    assert host.get(NS + "exported") == "false" and not host.findall("intent-filter")
    components = [{"kind": e.tag, **{k.removeprefix(NS): v for k, v in e.attrib.items()}}
                  for e in a if e.tag in ("activity", "activity-alias", "provider", "service", "receiver")]
    providers = {p.get(NS + "name"): p for p in a.findall("provider")}
    startup = providers["androidx.startup.InitializationProvider"]
    assert startup.get(NS + "authorities") == PACKAGE + ".androidx-startup"
    assert startup.get(NS + "process") == PACKAGE and startup.get(NS + "exported") == "false"
    assert not any(p.get(NS + "name") == "androidx.work.WorkManagerInitializer" for p in startup.findall("meta-data"))
    assert all(p.get(NS + "authorities", "").startswith(PACKAGE + ".") and p.get(NS + "exported") == "false" for p in providers.values())
    sandbox = next(s for s in a.findall("service") if s.get(NS + "name").endswith("MediaSandboxService"))
    assert sandbox.get(NS + "isolatedProcess") == "true" and sandbox.get(NS + "exported") == "false" and sandbox.get(NS + "process") == ":media_sandbox"
    test_instrumentations = manifests["test"].findall("instrumentation")
    assert len(test_instrumentations) == 1
    instrumentation = test_instrumentations[0]
    assert instrumentation.get(NS + "name") == RUNNER and instrumentation.get(NS + "targetPackage") == PACKAGE
    assert instrumentation.get(NS + "targetProcesses") is None
    app_classes, _, native = metadata["app"]
    test_classes, test_strings, _ = metadata["test"]
    app_required = ["app/LithicApplication", "app/MainActivity", "app/LithicNavigationKt", "app/AppContainer",
                    "data/backup/DataGenerationStore", "data/backup/BackupExportService", "data/backup/RestoreCoordinator",
                    "platform/notification/ReminderRuntime", "feature/home/HomeShellKt", "feature/review/ReviewShellKt",
                    "feature/review/TotalCostTrendViewModel", "feature/appearance/AppearanceProvider",
                    "data/media/GenerationCoverReader", "data/media/isolation/MediaSandboxService"]
    test_required = ["app/M13IsolatedRunner", "app/V1AcceptanceConfigurationTest", "ui/CompleteJourneyTest",
                     "performance/LongHistoryPerformanceTest", "ui/M22AccessibilityTest", "data/backup/M21BackupRestoreTest"]
    for name in app_required:
        assert "Lcom/cooper/lithic/" + name + ";" in app_classes, name
    for name in test_required:
        descriptor = "Lcom/cooper/lithic/" + name + ";"
        assert descriptor in test_classes and descriptor not in app_classes, name
    assert not [c for c in app_classes if c.startswith(("Lorg/junit/", "Ljunit/", "Landroidx/test/", "Landroidx/compose/ui/test/"))]
    assert all(n in test_strings for n in ("runtime_offline_journey", "full_twenty_year_dataset_reads_exact_global_facts_and_remains_responsive",
                                         "system200_real_ime_keeps_production_form_input_and_save_reachable", "plain_host_activity_configuration"))
    runner_code = capture("runner-code", [ANALYZER, "dex", "code", "--class", RUNNER, paths["test"]])
    assert "newApplication" in runner_code and "Landroid/app/Application;" in runner_code and "getName" in runner_code
    assert "LithicApplication" not in runner_code
    application_code = capture("application-code", [ANALYZER, "dex", "code", "--class", "com.cooper.lithic.app.LithicApplication", paths["app"]])
    assert "onCreate" in application_code and "ReminderRuntime" in application_code and "AppContainer" in application_code
    main_code = capture("main-activity-code", [ANALYZER, "dex", "code", "--class", "com.cooper.lithic.app.MainActivity", paths["app"]])
    assert "onCreate" in main_code and "LithicApplication" in main_code and "getAppContainer" in main_code
    test_application = manifests["test"].find("application")
    test_components = [{"kind": e.tag, **{k.removeprefix(NS): v for k, v in e.attrib.items()}}
                       for e in test_application if e.tag in ("activity", "activity-alias", "provider", "service", "receiver")]
    previous = ROOT / "deliveries/lithic-preview-0.1.0-preview.12/Lithic-0.1.0-preview.12-debug.apk"
    assert native == apk_metadata(previous)[2]
    for path, expected in pre["protectedSourceSha256"].items():
        assert sha(ROOT / path) == expected, path
    preservation = []
    for row in pre["preservedApks"]:
        path = ROOT / row["path"]
        if row["exists"]:
            assert path.is_file() and path.stat().st_size == row["bytes"] and sha(path) == row["sha256"], str(path)
        else:
            assert not path.exists(), str(path)
        preservation.append({**row, "unchanged": True})
    lint_path = ROOT / "app/build-v1-acceptance/reports/lint-results-debug.xml"
    lint = ET.parse(lint_path).getroot()
    severities = [e.get("severity") for e in lint.findall("issue")]
    assert not any(s in ("Fatal", "Error") for s in severities)
    (OUT / "lint.xml").write_bytes(lint_path.read_bytes())
    identity.update({"sourceCommit": subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip(),
                     "sourceTree": subprocess.check_output(["git", "rev-parse", "HEAD^{tree}"], text=True).strip(),
                     "components": components, "instrumentation": {k.removeprefix(NS): v for k, v in instrumentation.attrib.items()},
                     "testApplication": {k.removeprefix(NS): v for k, v in test_application.attrib.items()},
                     "testComponents": test_components,
                     "verifiedProductionClasses": app_required, "verifiedTestClasses": test_required,
                     "protectedSourceFilesUnchanged": len(pre["protectedSourceSha256"]), "preservedApks": preservation,
                     "nativeEntriesUnchangedFromPreview12": True, "lintErrors": 0, "lintWarnings": severities.count("Warning"),
                     "androidExecution": "NOT_RUN", "deviceOperations": 0, "testHostNote": "Non-exported empty ComponentActivity in acceptance App only; JUnit/Runner remain Test-only"})
    (OUT / "identity.json").write_text(json.dumps(identity, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: identity[k] for k in ("app", "test", "lintErrors", "lintWarnings", "protectedSourceFilesUnchanged")}, ensure_ascii=False))


try:
    main()
except Exception:
    (OUT / "failure.txt").write_text(traceback.format_exc(), encoding="utf-8")
    raise
