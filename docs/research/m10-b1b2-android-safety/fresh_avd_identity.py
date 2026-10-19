"""Fresh-name adapter of the existing read-only AVD admission function.

Only the exact authorised new name differs. Original module and old AVDs are
never modified or opened. Shared bounded/link/property primitives are reused.
"""
from pathlib import Path
import hashlib
import re
import xml.etree.ElementTree as ET
from avd_identity import (require, unlinked_ancestors, properties, path_value,
                          contained, read_bytes, ConfigurationBlocked)

def verify_configuration(registry, data, sdk, name, api, expected_image_digest=None):
    """Return JSON-safe static observations, never a runtime/device PASS.

    expected_image_digest optionally maps image-relative *metadata* file names to
    independently locked SHA-256. Payload hashing remains the caller's audit.
    """
    registry, data, sdk = (Path(item) for item in (registry, data, sdk))
    require(api in (26, 36), "Unsupported API")
    require(name == f"Lithic_M10B1B2_API{api}_20261019", "Unexpected fresh B1/B2 AVD name")
    for item in (registry, data, sdk):
        require(item.is_absolute() and ".." not in item.parts, "Absolute unambiguous roots required")
        unlinked_ancestors(item)
        require(item.is_dir(), "Required root missing")
    home = registry.parent
    require(registry.name == "avds" and data.parent.name == "data" and
            data.parent.parent.resolve() == home.resolve() and
            data.name == name + ".avd", "Data/registration roots conflict")
    require(not contained(sdk, home) and not contained(home, sdk), "SDK/data roots overlap")
    registrations = list(registry.glob("*.ini"))
    descriptor = registry / (name + ".ini")
    require(len(registrations) == 1 and registrations[0].name == descriptor.name,
            "Registration filename is not unique")
    ini, descriptor_meta = properties(descriptor)
    require(ini.get("avd.ini.encoding") == "UTF-8", "Descriptor encoding identity missing")
    require(ini.get("target") == f"android-{api}", "Descriptor API mismatch")
    target_data = path_value(ini.get("path", ""), registry)
    require(Path(ini.get("path", "")).is_absolute() and
            target_data.resolve() == data.resolve(), "Descriptor data root mismatch")
    if "path.rel" in ini:
        # Android path.rel is relative to ANDROID_USER_HOME, not registry or cwd.
        rel = Path(ini["path.rel"].replace("\\", "/"))
        require(not rel.is_absolute() and ".." not in rel.parts,
                "Ambiguous relative registration path")
        require((home / "android-user" / rel).resolve() == data.resolve(),
                "Conflicting relative registration path")
    config, config_meta = properties(data / "config.ini")
    require(config.get("avd.ini.encoding") == "UTF-8", "Config encoding identity missing")
    require(config.get("target") == f"android-{api}", "AVD API mismatch")
    require(config.get("abi.type") == "x86_64" and config.get("hw.cpu.arch") == "x86_64",
            "AVD architecture mismatch")
    require(config.get("tag.id") == "google_apis" and
            config.get("tag.ids", "google_apis") == "google_apis", "AVD Google APIs tag mismatch")
    placeholders = {}
    for key in ("avd.id", "avd.name"):
        value = config.get(key)
        require(value in (name, "<build>"), "Missing/conflicting AVD identity: " + key)
        if value == "<build>":
            placeholders[key] = {"value": value, "meaning": "SET_BEFORE_LAUNCH_CORE_NOT_RUNTIME_IDENTITY"}
    if "AvdId" in config:
        require(config["AvdId"] == name, "Optional AvdId conflicts with locked registration")
    for key, value in config.items():
        if "<" in value or ">" in value:
            require((key in ("avd.id", "avd.name") and value == "<build>") or
                    (key == "disk.dataPartition.path" and value == "<temp>"),
                    "Unknown placeholder: " + key)
    image = sdk / f"system-images/android-{api}/google_apis/x86_64"
    unlinked_ancestors(image)
    require(image.is_dir(), "Approved image directory missing")
    actual_image = path_value(config.get("image.sysdir.1", ""), sdk)
    require(actual_image.resolve() == image.resolve() and
            not any(key.startswith("image.sysdir.") and key != "image.sysdir.1" for key in config),
            "Unapproved or multiple image paths")
    image_properties, image_meta = properties(image / "source.properties")
    revision = {26: "16", 36: "7"}[api]
    require(image_properties.get("AndroidVersion.ApiLevel") == str(api) and
            image_properties.get("SystemImage.Abi") == "x86_64" and
            image_properties.get("SystemImage.TagId") == "google_apis" and
            image_properties.get("Pkg.Revision") == revision, "SDK image identity/revision mismatch")
    package_bytes = read_bytes(image / "package.xml")
    try:
        package_root = ET.fromstring(package_bytes)
    except ET.ParseError as error:
        raise ConfigurationBlocked("Malformed SDK package metadata") from error
    packages = [item for item in package_root.iter() if item.tag.split("}")[-1] == "localPackage"]
    require(len(packages) == 1 and packages[0].get("path") ==
            f"system-images;android-{api};google_apis;x86_64", "SDK package path mismatch")
    revisions = [item for item in packages[0] if item.tag.split("}")[-1] == "revision"]
    require(len(revisions) == 1, "SDK package revision missing/duplicated")
    majors = [item for item in revisions[0] if item.tag.split("}")[-1] == "major"]
    require(len(majors) == 1 and (majors[0].text or "").strip() == revision,
            "SDK package/source revision conflict")
    # Official API36 r7 uses the packaged empty-data marker instead of userdata.img.
    # This proves the selected package layout only, not a generated guest data image.
    payloads = ("system.img", "userdata.img") if api == 26 else ("system.img", "data/empty_data_disk")
    for filename in payloads:
        item = image / filename
        unlinked_ancestors(item)
        require(item.is_file() and item.stat().st_size > 0, "Image payload absent: " + filename)
    metadata = {"source.properties": image_meta,
                "package.xml": {"bytes": len(package_bytes), "sha256": hashlib.sha256(package_bytes).hexdigest()}}
    if expected_image_digest is not None:
        require(isinstance(expected_image_digest, dict) and bool(expected_image_digest),
                "Locked metadata digest mapping required")
        require(set(expected_image_digest).issubset(metadata), "Unsupported metadata digest key")
        for key, expected in expected_image_digest.items():
            require(bool(re.fullmatch(r"[0-9a-f]{64}", expected)) and
                    metadata[key]["sha256"] == expected, "Locked image metadata SHA mismatch: " + key)
    paths = {}
    for key in ("disk.dataPartition.path", "disk.cachePartition.path", "sdcard.path",
                "disk.dataPartition.initPath", "disk.systemPartition.path", "disk.vendorPartition.path"):
        value = config.get(key)
        if key == "disk.dataPartition.path":
            require(bool(value), "Missing data partition path")
        if not value:
            continue
        if key == "disk.dataPartition.path" and value == "<temp>":
            placeholders[key] = {"value": value, "meaning": "TEMPORARY_FILE_LOCATION_UNPROVEN"}
            continue
        target = path_value(value, data)
        require(contained(target, data), "AVD writable path escapes dedicated data root: " + key)
        paths[key] = str(target.resolve())
    return {"status": "STATIC_CONFIGURATION_VERIFIED", "api": api, "registeredName": name,
            "registry": str(registry.resolve()), "dataRoot": str(data.resolve()), "sdkRoot": str(sdk.resolve()),
            "imageRoot": str(image.resolve()), "imageRevision": revision,
            "dataInitializationLayout": "USERDATA_IMAGE" if api == 26 else "OFFICIAL_EMPTY_DATA_DISK_MARKER",
            "descriptorIntegrity": descriptor_meta, "configIntegrity": config_meta,
            "imageMetadataIntegrity": metadata, "imagePayloadIntegrity": "CALLER_MUST_VERIFY_LOCKED_SHA",
            "placeholderFields": placeholders, "resolvedConfiguredPaths": paths,
            "explicitDatadirRequired": True, "runtimeIdentity": "NOT_RUN",
            "runtimeTemporaryFileConfinement": "UNKNOWN_UNTIL_RUNTIME_EVIDENCE",
            "configuration": config}
