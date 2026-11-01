"""LITHIC V1-B1 Preview15 Private Trial desktop APK audit only; no build, copy, device, or install.

Run only after the supervising agent confirms a successful Private Trial build:
  python docs/evidence/v1-b1-20261101/desktop-1/verify-apk.py --build-receipt <command.json>
SDK/DEX checks adapt the reviewed V1-A1 preparation audit. No APK is archived here.
"""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import os
import re
import struct
import subprocess
import traceback
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[4]
EV = ROOT / "docs/evidence/v1-b1-20261101/desktop-1"
B1_SOURCE_BASELINE = "af5528b689cf8c034947ba259b1693bea396e6c8"
SDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/sdk")
TOOLS = SDK / "build-tools/36.0.0"
ANALYZER = SDK / "cmdline-tools/latest/bin/apkanalyzer.bat"
CERT = "29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9"
PACKAGE = "com.cooper.lithic.preview.debug"
RUNNER = "androidx.test.runner.AndroidJUnitRunner"
PREVIEW_PACKAGE = "com.cooper.lithic.preview.debug"
NS = "{http://schemas.android.com/apk/res/android}"
ENV = dict(os.environ)
ENV["JAVA_HOME"] = "C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1"
ENV["PATH"] = ENV["JAVA_HOME"] + "/bin;" + ENV["PATH"]
ARGS = argparse.ArgumentParser(description=__doc__)
ARGS.add_argument("--build-receipt", required=True, type=Path)
args = ARGS.parse_args()
OUT = EV / "artifact"
OUT.mkdir(exist_ok=False)
COMMANDS = []


def metadata(path):
    data = path.read_bytes()
    return {"path": path.relative_to(ROOT).as_posix(), "bytes": len(data),
            "sha256": hashlib.sha256(data).hexdigest()}


def write_json(path, value):
    path.write_bytes((json.dumps(value, indent=2, ensure_ascii=True) + "\n").encode("utf-8"))


def capture(label, argv):
    argv = [str(x) for x in argv]
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    row = {"label": label, "argv": argv, "startedUtc": started}
    try:
        result = subprocess.run(argv, cwd=ROOT, env=ENV, capture_output=True, timeout=120)
        stdout, stderr = result.stdout, result.stderr
        row["exitCode"] = result.returncode
    except BaseException as error:
        stdout = getattr(error, "stdout", None) or b""
        stderr = getattr(error, "stderr", None) or b""
        row.update(exitCode=None, exceptionType=type(error).__name__, exception=str(error))
        raise
    finally:
        row["finishedUtc"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
        for channel, data in (("stdout", stdout), ("stderr", stderr)):
            path = OUT / (label + "." + channel + ".log")
            path.write_bytes(data)
            row[channel] = metadata(path)
        COMMANDS.append(row)
        write_json(OUT / "commands.json", COMMANDS)
    assert result.returncode == 0, (label, result.returncode)
    return result.stdout.decode("utf-8", errors="strict")


def dex_metadata(data):
    """Read only class/method identities and BuildConfig's encoded boolean value."""
    assert data[:4] == b"dex\n" and len(data) >= 112

    def checked(pos, size):
        assert 0 <= pos <= len(data) and 0 <= size <= len(data) - pos
        return pos

    def u32(pos):
        return struct.unpack_from("<I", data, checked(pos, 4))[0]

    def uleb(pos):
        value = 0
        for shift in range(0, 35, 7):
            item = data[checked(pos, 1)]
            pos += 1
            value |= (item & 127) << shift
            if not item & 128:
                assert value <= 0xffffffff
                return value, pos
        raise AssertionError("oversized ULEB128")

    assert u32(32) == len(data)

    def table(count_pos, offset_pos, stride):
        count, offset = u32(count_pos), u32(offset_pos)
        checked(offset, count * stride)
        return count, offset

    count, offset = table(0x38, 0x3c, 4)
    strings = []
    for index in range(count):
        pos = u32(offset + index * 4)
        _, pos = uleb(pos)
        end = data.index(b"\0", pos)
        strings.append(data[pos:end].decode("utf-8", errors="replace"))
    count, offset = table(0x40, 0x44, 4)
    types = [strings[u32(offset + index * 4)] for index in range(count)]
    count, offset = table(0x50, 0x54, 8)
    fields = []
    for index in range(count):
        owner, field_type, name = struct.unpack_from("<HHI", data, offset + index * 8)
        fields.append((types[owner], types[field_type], strings[name]))
    count, offset = table(0x58, 0x5c, 8)
    methods = set()
    for index in range(count):
        owner, _, name = struct.unpack_from("<HHI", data, offset + index * 8)
        methods.add((types[owner], strings[name]))

    def encoded(pos, depth=0):
        assert depth < 32
        header = data[checked(pos, 1)]
        pos += 1
        kind, argument = header & 31, header >> 5
        if kind == 0x1f:
            assert argument in (0, 1)
            return bool(argument), pos
        if kind == 0x1e:
            assert argument == 0
            return None, pos
        if kind == 0x1c:
            assert argument == 0
            size, pos = uleb(pos)
            for _ in range(size):
                _, pos = encoded(pos, depth + 1)
            return None, pos
        if kind == 0x1d:
            assert argument == 0
            _, pos = uleb(pos)
            size, pos = uleb(pos)
            for _ in range(size):
                _, pos = uleb(pos)
                _, pos = encoded(pos, depth + 1)
            return None, pos
        assert kind in (0x00, 0x02, 0x03, 0x04, 0x06, 0x10, 0x11, 0x15, 0x16,
                        0x17, 0x18, 0x19, 0x1a, 0x1b), kind
        checked(pos, argument + 1)
        return None, pos + argument + 1

    count, offset = table(0x60, 0x64, 32)
    classes, build_flag = set(), None
    for index in range(count):
        pos = offset + index * 32
        descriptor = types[u32(pos)]
        classes.add(descriptor)
        if descriptor != "Lcom/cooper/lithic/BuildConfig;":
            continue
        class_data, values = u32(pos + 24), u32(pos + 28)
        assert class_data and values
        static_count, cursor = uleb(class_data)
        for _ in range(3):
            _, cursor = uleb(cursor)
        static_fields, field_index = [], 0
        for _ in range(static_count):
            delta, cursor = uleb(cursor)
            _, cursor = uleb(cursor)
            field_index += delta
            static_fields.append(fields[field_index])
        value_count, cursor = uleb(values)
        assert value_count <= len(static_fields)
        for field in static_fields[:value_count]:
            value, cursor = encoded(cursor)
            if field == (descriptor, "Z", "PHOTO_LITE_EXPERIMENT"):
                assert type(value) is bool
                build_flag = value
    return classes, methods, build_flag


def apk_metadata(apk):
    classes, methods, flags = set(), set(), []
    with zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            if re.fullmatch(r"classes\d*\.dex", name):
                found_classes, found_methods, flag = dex_metadata(archive.read(name))
                assert not classes.intersection(found_classes), "duplicate DEX class"
                classes |= found_classes
                methods |= found_methods
                if flag is not None:
                    flags.append(flag)
        native = {name: hashlib.sha256(archive.read(name)).hexdigest()
                  for name in archive.namelist() if name.startswith("lib/") and name.endswith(".so")}
    return classes, methods, flags, native


def components(manifest, package):
    keys = ("name", "exported", "process", "isolatedProcess", "permission", "directBootAware", "authorities")
    found = []
    for element in manifest.find("application"):
        if element.tag in ("activity", "activity-alias", "provider", "service", "receiver"):
            attrs = {key: element.get(NS + key) for key in keys if element.get(NS + key) is not None}
            attrs = {key: value.replace(package, "{applicationId}") for key, value in attrs.items()}
            found.append({"kind": element.tag, **attrs})
    return sorted(found, key=lambda item: (item["kind"], item.get("name", "")))


def main():
    receipt_path = args.build_receipt.resolve()
    receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
    assert receipt["exitCode"] == 0 and receipt["deviceOperations"] == 0
    assert receipt["androidExecution"] == "NOT_RUN"
    assert "-PlithicPrivateTrial=true" in receipt["argv"]
    assert "-PphotoLiteExperiment=true" not in receipt["argv"]
    source_commit = receipt["sourceCommit"]
    assert re.fullmatch(r"[0-9a-f]{40}", source_commit), "Expected an exact build-source SHA"
    assert subprocess.run(["git", "merge-base", "--is-ancestor", B1_SOURCE_BASELINE, source_commit],
                          cwd=ROOT, capture_output=True).returncode == 0, "Build source is not a B1 preparation descendant"
    source_tree = subprocess.check_output(["git", "rev-parse", source_commit + ":app"], cwd=ROOT, text=True).strip()
    current_tree = subprocess.check_output(["git", "rev-parse", "HEAD:app"], cwd=ROOT, text=True).strip()
    assert receipt["sourceAppGitTree"] == source_tree, "Build receipt did not bind its exact App tree"
    assert receipt["appInputStatusAtStart"] == "", "Build started with dirty or untracked App inputs"
    assert source_tree == current_tree, "Build-source app tree changed"
    # Post-preparation fixes may touch only this B1 implementation's files and its explicit trial version.
    approved_paths = set(subprocess.check_output(["git", "diff-tree", "--no-commit-id", "--name-only", "-r",
                                                  B1_SOURCE_BASELINE, "--", "app"], cwd=ROOT, text=True).splitlines())
    approved_paths.add("app/build.gradle.kts")
    changed_after_preparation = set(subprocess.check_output(["git", "diff", "--name-only", B1_SOURCE_BASELINE,
                                                            source_commit, "--", "app"], cwd=ROOT, text=True).splitlines())
    assert changed_after_preparation <= approved_paths, "App changes extend beyond B1 fixes and the trial version"
    assert not subprocess.check_output(["git", "diff", "--name-only", source_commit, "--", "app"], cwd=ROOT, text=True).strip()
    assert not subprocess.check_output(["git", "status", "--porcelain=v1", "--untracked-files=all", "--", "app"],
                                       cwd=ROOT, text=True).strip(), "Uncommitted or untracked App inputs"
    assert subprocess.check_output(["git", "branch", "--show-current"], cwd=ROOT, text=True).strip() == "codex/v1-b1-data-confidence"
    paths = {"app": ROOT / "app/build/private-trial/outputs/apk/debug/app-debug.apk",
             "test": ROOT / "app/build/private-trial/outputs/apk/androidTest/debug/app-debug-androidTest.apk"}
    preview = ROOT / "deliveries/lithic-preview-0.1.0-preview.12/Lithic-0.1.0-preview.12-debug.apk"
    assert preview.stat().st_size == 15977934
    assert metadata(preview)["sha256"] == "4b6b3ed03674fc971b67a124171fedbb89fce155d2c6fd181ed597cde6250a1b"
    preview14 = ROOT / "deliveries/lithic-preview-0.1.0-preview.14/Lithic-0.1.0-preview.14-debug.apk"
    assert preview14.stat().st_size == 20457403
    assert metadata(preview14)["sha256"] == "0994fa279ccbdc92c44e1ee963f7d404b113a4334f4194c1afd5def73ce764bf"
    manifests, identity, dex = {}, {}, {}
    forbidden = {"android.permission.INTERNET", "android.permission.READ_EXTERNAL_STORAGE",
                 "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.MANAGE_EXTERNAL_STORAGE",
                 "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO",
                 "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"}
    for label, path in {**paths, "preview12": preview, "preview14": preview14}.items():
        badging = capture(label + "-badging", [TOOLS / "aapt2.exe", "dump", "badging", path])
        manifest = ET.fromstring(capture(label + "-manifest", [ANALYZER, "manifest", "print", path]))
        certificate = capture(label + "-certificate", [TOOLS / "apksigner.bat", "verify", "--verbose", "--print-certs", path])
        certs = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([a-f0-9]+)", certificate)
        assert certs == [CERT]
        package = PREVIEW_PACKAGE if label in ("preview12", "preview14") else PACKAGE + (".test" if label == "test" else "")
        assert manifest.get("package") == package and f"name='{package}'" in badging
        assert manifest.get(NS + "sharedUserId") is None
        permissions = sorted(p.get(NS + "name") for p in manifest if p.tag.startswith("uses-permission"))
        assert not forbidden.intersection(permissions)
        manifests[label], dex[label] = manifest, apk_metadata(path)
        identity[label] = {**metadata(path), "applicationId": package, "certificateSha256": CERT,
                           "versionCode": manifest.get(NS + "versionCode"), "versionName": manifest.get(NS + "versionName"),
                           "permissions": permissions, "nativeEntries": dex[label][3]}
    app = manifests["app"]
    application = app.find("application")
    assert identity["app"]["versionCode"] == "31"
    assert identity["app"]["versionName"] == "0.1.0-preview.15-debug"
    assert identity["preview12"]["versionCode"] == "28"
    assert identity["preview12"]["versionName"] == "0.1.0-preview.12-debug"
    assert identity["preview14"]["versionCode"] == "30"
    assert identity["preview14"]["versionName"] == "0.1.0-preview.14-debug"
    assert application.get(NS + "label") == "砺石·体验版"
    assert application.get(NS + "name") == "com.cooper.lithic.app.LithicApplication"
    assert application.get(NS + "allowBackup") == "false" and application.get(NS + "testOnly") != "true"
    assert not app.findall("instrumentation")
    uses = app.find("uses-sdk")
    assert uses.get(NS + "minSdkVersion") == "26" and uses.get(NS + "targetSdkVersion") == "36"
    activities = {e.get(NS + "name"): e for e in application.findall("activity")}
    assert set(activities) == {"com.cooper.lithic.app.MainActivity"}, "Private Trial must not register a Compose test host"
    assert activities["com.cooper.lithic.app.MainActivity"].get(NS + "exported") == "true"
    assert any(f.find("action").get(NS + "name") == "android.intent.action.MAIN" and
               any(c.get(NS + "name") == "android.intent.category.LAUNCHER" for c in f.findall("category"))
               for f in activities["com.cooper.lithic.app.MainActivity"].findall("intent-filter"))
    instrumentation = manifests["test"].findall("instrumentation")
    assert len(instrumentation) == 1
    instrumentation = instrumentation[0]
    assert instrumentation.get(NS + "name") == RUNNER
    assert instrumentation.get(NS + "targetPackage") == PACKAGE and instrumentation.get(NS + "targetProcesses") is None
    baseline_components = components(manifests["preview12"], PREVIEW_PACKAGE)
    app_components = components(app, PACKAGE)
    assert app_components == baseline_components
    assert app_components == components(manifests["preview14"], PREVIEW_PACKAGE)
    authorities = [e.get(NS + "authorities") for e in application.findall("provider")]
    assert authorities and all(a and all(item.startswith(PACKAGE + ".") for item in a.split(";")) for a in authorities)
    normalize_permissions = lambda values, package: sorted(p.replace(package, "{applicationId}") for p in values)
    assert normalize_permissions(identity["app"]["permissions"], PACKAGE) == normalize_permissions(identity["preview12"]["permissions"], PREVIEW_PACKAGE)
    assert normalize_permissions(identity["app"]["permissions"], PACKAGE) == normalize_permissions(identity["preview14"]["permissions"], PREVIEW_PACKAGE)
    assert dex["app"][3] == dex["preview12"][3]
    assert dex["app"][3] == dex["preview14"][3]
    assert not dex["test"][3]
    assert dex["app"][2] == [True], "Actual DEX BuildConfig PHOTO_LITE_EXPERIMENT must be true"
    app_classes, app_methods, _, _ = dex["app"]
    test_classes, _, _, _ = dex["test"]
    required = ["app/LithicApplication", "app/MainActivity", "app/LedgerUiStateHolder", "app/LithicNavigationKt",
                "feature/media/PhotoLiteCoverController", "feature/media/PhotoLiteThumbnailPipeline",
                "feature/media/PhotoLiteCoverPanelKt", "feature/media/CoverPreviewsKt",
                "data/media/photolite/GenerationPhotoLiteCoverWriter", "data/media/photolite/PhotoLitePng",
                "data/media/photolite/PhotoLiteCoverChange", "data/media/photolite/PhotoLiteTarget",
                "data/media/photolite/PhotoLiteCommitResult$Cleared",
                "data/backup/BackupInspectionService", "data/backup/BackupInspectionStatus",
                "data/backup/BackupInspectionResult", "data/backup/BackupInspectionSummary",
                "data/backup/UnsupportedBackupFormatException",
                "feature/settings/BackupInspectionController", "feature/settings/BackupInspectionViewModel",
                "feature/settings/BackupInspectionDocumentPickerKt", "feature/settings/BackupInspectionUiState",
                "feature/settings/DataConfidenceSectionsKt", "feature/settings/SettingsShellKt",
                "data/media/PhotoStorageReader", "data/media/GenerationPhotoStorageReader",
                "data/media/PhotoStorageSummary", "data/media/PhotoStorageReadFailure",
                "data/media/PhotoStorageReadResult$Ready", "data/media/PhotoStorageReadResult$Unavailable",
                "feature/settings/PhotoStorageController", "feature/settings/PhotoStorageViewModel",
                "feature/settings/PhotoStorageUiState$Ready", "feature/settings/PhotoStorageUiState$Unavailable"]
    for name in required:
        assert "Lcom/cooper/lithic/" + name + ";" in app_classes, name
    for owner, method in (("feature/media/CoverPreviewsKt", "RecordCover"),
                          ("data/media/photolite/GenerationPhotoLiteCoverWriter", "capture"),
                          ("data/media/photolite/GenerationPhotoLiteCoverWriter", "commit"),
                          ("data/media/photolite/GenerationPhotoLiteCoverWriter", "remove"),
                          ("data/media/photolite/PhotoLiteTarget", "getOriginalCoverAssetId"),
                          ("feature/media/PhotoLiteCoverController", "begin"),
                          ("feature/media/PhotoLiteCoverController", "confirm"),
                          ("feature/media/PhotoLiteThumbnailPipeline", "load"),
                          ("data/backup/BackupInspectionService", "inspect"),
                          ("feature/settings/BackupInspectionController", "beginSelection"),
                          ("feature/settings/BackupInspectionController", "selected"),
                          ("feature/settings/BackupInspectionController", "requestCancellation"),
                          ("feature/settings/BackupInspectionViewModel", "beginSelection"),
                          ("feature/settings/BackupInspectionViewModel", "selected"),
                          ("feature/settings/BackupInspectionViewModel", "cancelCheck"),
                          ("feature/settings/BackupInspectionDocumentPickerKt", "rememberBackupInspectionDocumentPicker"),
                          ("feature/settings/DataConfidenceSectionsKt", "BackupInspectionFeedback"),
                          ("feature/settings/DataConfidenceSectionsKt", "PhotoStorageSection"),
                          ("data/media/GenerationPhotoStorageReader", "read"),
                          ("data/media/GenerationPhotoStorageReader", "isCurrent"),
                          ("feature/settings/PhotoStorageController", "refresh"),
                          ("feature/settings/PhotoStorageController", "close"),
                          ("feature/settings/PhotoStorageViewModel", "refresh")):
        assert ("Lcom/cooper/lithic/" + owner + ";", method) in app_methods, (owner, method)
    test_required = ["app/M13IsolatedRunner", "feature/media/PhotoLiteCoverControllerTest",
                     "feature/media/PhotoLiteThumbnailPipelineTest", "feature/media/PhotoLiteCoverPresentationTest",
                     "data/media/photolite/GenerationPhotoLiteCoverWriterTest",
                     "data/media/GenerationPhotoStorageReaderTest", "feature/settings/DataConfidencePresentationTest"]
    for name in test_required:
        descriptor = "Lcom/cooper/lithic/" + name + ";"
        assert descriptor in test_classes and descriptor not in app_classes, name
    assert not [name for name in app_classes if name.startswith(("Lorg/junit/", "Ljunit/", "Lorg/hamcrest/", "Landroidx/test/", "Landroidx/compose/ui/test/"))]
    # This compiled Test APK targets the normal Application with AndroidJUnitRunner.
    # M13IsolatedRunner is compiled as a historical test class, but is NOT registered.
    # No Android test is executed by this audit; synthetic Owner tests must not be
    # run against this ordinary trial host under an assumed plain Application.
    assert "Landroidx/test/runner/AndroidJUnitRunner;" in test_classes
    assert instrumentation.get(NS + "name") != "com.cooper.lithic.app.M13IsolatedRunner"
    application_code = capture("application-code", [ANALYZER, "dex", "code", "--class", "com.cooper.lithic.app.LithicApplication", paths["app"]])
    assert all(term in application_code for term in ("onCreate", "ReminderRuntime", "AppContainer"))
    main_code = capture("main-activity-code", [ANALYZER, "dex", "code", "--class", "com.cooper.lithic.app.MainActivity", paths["app"]])
    assert all(term in main_code for term in ("onCreate", "LithicApplication", "getAppContainer"))
    preparation = json.loads((ROOT / "docs/evidence/v1-b1-20261101/preparation.json").read_text(encoding="utf-8"))
    old = preparation["protectedApks"]
    assert len(old) == 16 and len({item["path"] for item in old}) == 16
    protection = []
    for expected in old:
        actual = metadata(ROOT / expected["path"])
        assert actual["bytes"] == expected["bytes"] and actual["sha256"] == expected["sha256"], expected["path"]
        protection.append({**actual, "unchanged": True})
    result = {"status": "PASS", "scope": "OFFLINE_ARTIFACT_AUDIT_ONLY", "deviceOperations": 0,
              "androidExecution": "NOT_RUN", "buildReceipt": metadata(receipt_path), "apks": identity,
              "buildSourceCommit": source_commit, "buildAppGitTree": source_tree,
              "b1SourceBaseline": B1_SOURCE_BASELINE, "buildSourceIsB1Descendant": True,
              "appInputStatusAtBuildStartWasClean": True,
              "postPreparationAppChanges": sorted(changed_after_preparation),
              "b1InspectionAndPhotoStorageClassesPresent": True,
              "photoLiteExperimentActualDexBoolean": True, "productionClassesChecked": required,
              "testOnlyClassesChecked": test_required, "registeredRunnerUsesPlainApplicationReplacement": False,
              "m13RunnerCompiledButNotRegistered": True,
              "testHostMode": "NORMAL_APPLICATION_COMPILE_ONLY_NOT_AUTHORIZED_FOR_SYNTHETIC_EXECUTION",
              "preview12MainComponentsAndPermissionsUnchanged": True,
              "preview14StaticUpgradeIdentityChecked": True, "preview14MainComponentsAndPermissionsUnchanged": True,
              "actualUpgradeExecuted": "NOT_RUN", "appComponents": app_components,
              "providerAuthorities": authorities, "productionComposeTestHostRegistered": False,
              "nativeEntriesUnchangedFromPreview12": True, "nativeEntriesUnchangedFromPreview14": True,
              "preview14ByteIdentityAndSigningContinuityVerified": True,
              "protectedLocalApks": protection,
              "instrumentation": {key.removeprefix(NS): value for key, value in instrumentation.attrib.items()},
              "testApplication": {key.removeprefix(NS): value for key, value in manifests["test"].find("application").attrib.items()},
              "commands": COMMANDS}
    write_json(OUT / "identity.json", result)
    print(json.dumps({"status": result["status"], "app": identity["app"], "test": identity["test"],
                      "protectedLocalApks": len(protection), "deviceOperations": 0}, ensure_ascii=True))


try:
    main()
except Exception:
    (OUT / "failure.txt").write_text(traceback.format_exc(), encoding="utf-8")
    raise
