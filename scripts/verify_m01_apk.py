"""Inspect the built skeleton APK with the local Android SDK, without installing it."""

import argparse
import hashlib
import json
import re
import subprocess
import tomllib
import xml.etree.ElementTree as ET
from pathlib import Path


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", type=Path, required=True)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--expected-version-code", default="1")
    parser.add_argument("--expected-version-name", default="0.1.0-m01-debug")
    parser.add_argument("--profile", choices=("m01", "m20"), default="m01")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    apk = args.apk.resolve(strict=True)
    aapt = args.sdk / "build-tools/36.0.0/aapt2.exe"
    require(aapt.is_file(), "Android Build Tools 36.0.0 aapt2.exe is required")
    args.output.mkdir(parents=True, exist_ok=True)

    def dump(kind: str, *extra: str) -> str:
        result = subprocess.run(
            [str(aapt), "dump", kind, str(apk), *extra],
            capture_output=True, encoding="utf-8", errors="strict", check=True,
        )
        return result.stdout

    badging = dump("badging")
    manifest = dump("xmltree", "--file", "AndroidManifest.xml")
    backup = dump("xmltree", "--file", "res/xml/backup_rules.xml")
    extraction = dump("xmltree", "--file", "res/xml/data_extraction_rules.xml")
    for name, value in {"badging": badging, "manifest": manifest,
                        "backup": backup, "extraction": extraction}.items():
        (args.output / f"apk-{name}.txt").write_text(value, encoding="utf-8")

    package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    require(package is not None, "APK package metadata is missing")
    application_id, version_code, version_name = package.groups()
    require(application_id == "com.cooper.lithic.debug", "Unexpected Debug applicationId")
    require(version_code == args.expected_version_code and version_name == args.expected_version_name,
            "Unexpected requested APK version")
    require("application-label:'砺石 LITHIC Debug'" in badging, "Debug label is missing")
    require("minSdkVersion:'26'" in badging and "targetSdkVersion:'36'" in badging, "Unexpected SDK range")

    permissions = set(re.findall(r"uses-permission(?:-sdk-23)?: name='([^']+)'", badging))
    # M01 needs no platform permission. AndroidX Core contributes this signature-only guard.
    allowed = {"com.cooper.lithic.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"}
    if args.profile == "m20":
        allowed |= {"android.permission.POST_NOTIFICATIONS", "android.permission.WAKE_LOCK",
                    "android.permission.ACCESS_NETWORK_STATE", "android.permission.RECEIVE_BOOT_COMPLETED"}
    require(permissions <= allowed, f"Unexpected permissions: {sorted(permissions - allowed)}")
    require(re.search(r":allowBackup.*=false", manifest) is not None, "APK allows system backup")
    require(re.search(r":protectionLevel.*=0x00000002", manifest) is not None,
            "APK internal receiver permission is not signature-only")
    require("androidx.emoji2.text.EmojiCompatInitializer" not in manifest,
            "APK enables downloadable Emoji font initialization")
    for attribute in ("fullBackupContent", "dataExtractionRules"):
        require(re.search(rf":{attribute}.*=@0x", manifest) is not None, f"APK misses {attribute} reference")

    domains = {"root", "file", "database", "sharedpref", "external", "device_root",
               "device_file", "device_database", "device_sharedpref"}

    def check_exclusions(text: str, label: str) -> None:
        found = re.findall(r'A: domain="([^"]+)"', text)
        paths = re.findall(r'A: path="([^"]+)"', text)
        require(len(found) == 9 and set(found) == domains, f"Incomplete {label} domains")
        require(paths == ["."] * 9, f"Incomplete {label} paths")
        require("E: include " not in text, f"Unexpected {label} include rule")

    check_exclusions(backup, "legacy backup")
    sections = re.split(r"E: (cloud-backup|device-transfer) \(line=\d+\)", extraction)
    require(len(sections) == 5 and set(sections[1::2]) == {"cloud-backup", "device-transfer"},
            "Both extraction sections are required")
    for label, content in zip(sections[1::2], sections[2::2]):
        check_exclusions(content, label)

    merged_files = list((root / "app/build/intermediates/merged_manifests/debug").rglob("AndroidManifest.xml"))
    require(len(merged_files) == 1, "Expected one final merged Debug manifest")
    merged = ET.parse(merged_files[0]).getroot()
    ns = "{http://schemas.android.com/apk/res/android}"
    merged_permissions = {node.attrib[ns + "name"] for node in merged.findall("uses-permission")}
    require(merged_permissions == permissions, "Merged manifest and APK permissions differ")
    for permission in merged.findall("permission"):
        require(permission.attrib[ns + "name"] in allowed and permission.attrib[ns + "protectionLevel"] == "signature",
                "Unexpected merged permission definition")
    app = merged.find("application")
    require(app is not None and app.attrib[ns + "allowBackup"] == "false", "Merged backup flag is wrong")
    require(app.attrib[ns + "fullBackupContent"] == "@xml/backup_rules", "Wrong legacy resource reference")
    require(app.attrib[ns + "dataExtractionRules"] == "@xml/data_extraction_rules", "Wrong extraction reference")
    require(not any(node.attrib.get(ns + "name") == "androidx.emoji2.text.EmojiCompatInitializer"
                    for node in app.iter("meta-data")), "Merged manifest enables downloadable Emoji fonts")
    if args.profile == "m20":
        require(app.attrib.get(ns + "name") == "com.cooper.lithic.app.LithicApplication", "Wrong unique storage owner")
        require(not any(node.attrib.get(ns + "name") == "androidx.work.WorkManagerInitializer"
                        for node in app.iter("meta-data")), "WorkManager would be initialized twice")
        require(not any(node.attrib.get(ns + "name") == "androidx.work.impl.foreground.SystemForegroundService"
                        for node in app.findall("service")), "Unused foreground service is present")

    catalog = tomllib.loads((root / "gradle/libs.versions.toml").read_text(encoding="utf-8"))
    require(all(re.fullmatch(r"\d+(?:\.\d+)+", value) for value in catalog["versions"].values()),
            "Version catalog contains a non-exact or pre-release version")
    wrapper = (root / "gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf-8")
    require("distributionSha256Sum=ed1a8d686605fd7c23bdf62c7fc7add1c5b23b2bbc3721e661934ef4a4911d7c" in wrapper,
            "Gradle distribution checksum is missing")
    wrapper_hash = hashlib.sha256((root / "gradle/wrapper/gradle-wrapper.jar").read_bytes()).hexdigest()
    require(wrapper_hash == "7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172", "Unexpected Wrapper JAR")

    report = {
        "apk": str(apk), "bytes": apk.stat().st_size,
        "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
        "applicationId": application_id, "versionCode": int(version_code), "versionName": version_name,
        "permissions": sorted(permissions), "mergedManifest": str(merged_files[0].relative_to(root)),
        "backupConfig": "allowBackup=false; legacy/cloud/device-transfer exclude all nine domains",
        "versions": catalog["versions"], "wrapperJarSha256": wrapper_hash,
        "scope": "Artifact/config checks only; signature, install and offline launch require separate evidence",
        "permissionProfile": args.profile,
    }
    (args.output / "apk-verification.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
