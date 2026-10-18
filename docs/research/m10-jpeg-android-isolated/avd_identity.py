"""Read-only AVD configuration admission; placeholders never prove runtime identity.

No Android command, file creation, SDK mutation, or device operation is performed.
The caller separately verifies image payload SHA, tool identity, port/process
availability and actual runtime confinement before admitting device operations.
"""
from __future__ import annotations

import hashlib
import os
from pathlib import Path
import re
import stat
import xml.etree.ElementTree as ET


class ConfigurationBlocked(RuntimeError):
    pass


def require(condition, message):
    if not condition:
        raise ConfigurationBlocked(message)


def unlinked_ancestors(path):
    """Check lstat before resolve so junctions/symlinks cannot disappear."""
    for item in (path, *path.parents):
        try:
            found = item.lstat()
        except FileNotFoundError:
            continue
        require(not stat.S_ISLNK(found.st_mode) and
                not (getattr(found, "st_file_attributes", 0) & 0x400),
                "Linked/reparse path is forbidden: " + str(item))
        require(not stat.S_ISREG(found.st_mode) or found.st_nlink == 1,
                "Multiply-linked file is forbidden: " + str(item))


def read_bytes(path, limit=131072):
    unlinked_ancestors(path)
    found = path.lstat()
    require(stat.S_ISREG(found.st_mode), "Required regular file missing: " + str(path))
    require(found.st_size <= limit, "Configuration exceeds bounded read")
    data = path.read_bytes()
    after = path.lstat()
    require((found.st_dev, found.st_ino, found.st_size, found.st_mtime_ns) ==
            (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns) and
            len(data) == found.st_size, "Configuration changed during read")
    return data


def properties(path):
    data = read_bytes(path)
    try:
        text = data.decode("utf-8", errors="strict")
    except UnicodeDecodeError as error:
        raise ConfigurationBlocked("Configuration must be UTF-8") from error
    require(not text.startswith("\ufeff") and "\x00" not in text,
            "Unexpected BOM/NUL in configuration")
    result, folded = {}, set()
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith(("#", ";")):
            continue
        require("=" in line, "Malformed configuration line")
        key, value = (part.strip() for part in line.split("=", 1))
        require(bool(re.fullmatch(r"[A-Za-z0-9_.-]+", key)), "Malformed configuration key")
        require(key.casefold() not in folded, "Duplicate configuration property: " + key)
        folded.add(key.casefold())
        result[key] = value
    return result, {"bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}


def contained(path, root):
    try:
        path.resolve().relative_to(root.resolve())
        return True
    except ValueError:
        return False


def path_value(value, relative_to):
    require(bool(value) and not any(token in value for token in ("<", ">", "$", "%", "\x00")),
            "Unknown placeholder or empty path")
    # Real CLI emits Windows separators; replacement also makes fixture tests portable.
    candidate = Path(value.replace("\\", "/"))
    require(".." not in candidate.parts, "Parent traversal in path")
    require(not (re.match(r"^[A-Za-z]:", value) and os.name != "nt"),
            "Foreign absolute path cannot be resolved on this host")
    if not candidate.is_absolute():
        candidate = relative_to / candidate
    unlinked_ancestors(candidate)
    return candidate


def verify_configuration(registry, data, sdk, name, api, expected_image_digest=None):
    """Return JSON-safe static observations, never a runtime/device PASS.

    expected_image_digest optionally maps image-relative *metadata* file names to
    independently locked SHA-256. Payload hashing remains the caller's audit.
    """
    registry, data, sdk = (Path(item) for item in (registry, data, sdk))
    require(api in (26, 36), "Unsupported API")
    require(name == f"Lithic_M10V_API{api}_20261018", "Unexpected isolated AVD name")
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
