"""Fail-closed User 0 identity parsing for the two isolated research APKs.

Only metadata is interpreted here. Callers retain device, package-path, APK SHA,
signature and run ownership gates. No process or device operation is performed.
"""
from __future__ import annotations

import re
from pathlib import PurePosixPath


LOCKED_PACKAGES = frozenset(("com.cooper.lithic.debug", "com.cooper.lithic.debug.test"))


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def app_uid(value):
    # These are ordinary, non-shared application IDs in the isolated User 0.
    # Reject secondary-user, SDK-sandbox, isolated and system identities.
    require(re.fullmatch(r"[1-9][0-9]{0,4}", value) is not None,
            "Installed UID has malformed or cross-user value")
    result = int(value)
    require(10000 <= result <= 19999, "Installed UID outside ordinary User 0 application range")
    return result


def owned_permission_uids(lines, package, uid):
    """Check only explicitly owned permission definitions, never global uid text."""
    starts = [index for index, line in enumerate(lines) if line == "Permissions:"]
    require(len(starts) <= 1, "Permission definition region duplicated")
    if not starts:
        return
    start = starts[0] + 1
    end = next((index for index in range(start, len(lines))
                if lines[index] and not lines[index][0].isspace()), None)
    require(end is not None and re.fullmatch(r"[A-Za-z][A-Za-z0-9 /_-]*:", lines[end]),
            "Permission definition region incomplete")
    region = [line for line in lines[start:end] if line]
    if not region:
        return
    headers = [index for index, line in enumerate(region) if re.match(r"\s*Permission\b", line)]
    require(headers and headers[0] == 0, "Permission definition boundary malformed")
    for offset, position in enumerate(headers):
        limit = headers[offset + 1] if offset + 1 < len(headers) else len(region)
        record = region[position:limit]
        header = re.fullmatch(r"  Permission \[([^]\s]+)\] \([0-9a-f]+\):", record[0])
        require(header is not None and all(line.startswith("    ") for line in record[1:]),
                "Permission definition record boundary malformed")
        sources = [line for line in record[1:] if re.match(r"\s*sourcePackage\b", line)]
        require(len(sources) == 1 and re.fullmatch(r"    sourcePackage=[A-Za-z0-9_.]+", sources[0]),
                "Permission sourcePackage missing, duplicated or malformed")
        if sources[0] != "    sourcePackage=" + package:
            continue
        permission = re.escape(header.group(1))
        details = [line for line in record[1:] if re.match(r"\s*perm\b", line)]
        require(len(details) == 1 and re.fullmatch(
            r"    perm=Permission(?:Info)?\{[0-9a-f]+ " + permission + r"\}", details[0]),
            "Owned permission definition incomplete or conflicting")
        uid_lines = [line for line in record[1:] if re.match(r"\s*uid\b", line)]
        require(len(uid_lines) <= 1, "Owned permission UID duplicated")
        if uid_lines:
            match = re.fullmatch(r"    uid=([1-9][0-9]{0,4})(?: (.*))?", uid_lines[0])
            require(match is not None and len(re.findall(r"(?:^|\s)uid=", uid_lines[0])) == 1,
                    "Owned permission UID malformed")
            require(app_uid(match.group(1)) == uid, "Owned permission UID conflicts with package")


def package_uid(package_info, package, installed_apk_path):
    """Accept exactly one complete target Packages block and installed User 0."""
    require(package in LOCKED_PACKAGES, "UID target is not a locked research package")
    require(isinstance(package_info, str) and 0 < len(package_info) <= 1024 * 1024
            and "\0" not in package_info and "\t" not in package_info,
            "Installed package dump absent, oversized or malformed")
    lines = package_info.splitlines()
    starts = [index for index, line in enumerate(lines) if line == "Packages:"]
    require(len(starts) == 1, "Installed Packages region missing or duplicated")
    start = starts[0] + 1
    end = next((index for index in range(start, len(lines))
                if lines[index] and not lines[index][0].isspace()), None)
    require(end is not None and re.fullmatch(r"[A-Za-z][A-Za-z0-9 /_-]*:", lines[end]),
            "Installed Packages region incomplete or unknown boundary")
    block = lines[start:end]
    nonempty = [line for line in block if line]
    header = re.compile(r"  Package \[([^]\s]+)\] \([0-9a-f]+\):")
    headers = [line for line in nonempty if re.match(r"\s*Package\b", line)]
    require(len(headers) == 1 and header.fullmatch(headers[0]) is not None
            and header.fullmatch(headers[0]).group(1) == package
            and nonempty[0] == headers[0], "Installed target package boundary is ambiguous")
    require(all(line == headers[0] or line.startswith("    ") for line in nonempty),
            "Installed package region contains another or malformed record")

    ids = {}
    users = []
    paths = []
    for line in nonempty[1:]:
        if re.match(r"\s*(?:userId|appId)\b", line):
            match = re.fullmatch(r"    (userId|appId)=([1-9][0-9]{0,4})", line)
            require(match is not None and match.group(1) not in ids,
                    "Installed UID field malformed, nested or duplicated")
            ids[match.group(1)] = app_uid(match.group(2))
        if re.match(r"\s*codePath\b", line):
            match = re.fullmatch(r"    codePath=([A-Za-z0-9_./=+~\-]+)", line)
            require(match is not None, "Installed codePath malformed or nested")
            paths.append(match.group(1))
        if re.match(r"\s*User\b", line):
            match = re.fullmatch(r"    User ([0-9]+): (.+)", line)
            require(match is not None and match.group(1) == "0",
                    "Installed user identity is malformed or not isolated User 0")
            users.append(match.group(2))
    require(ids and len(set(ids.values())) == 1, "Installed userId/appId absent or conflicting")
    require(len(users) == 1, "Installed User 0 missing or duplicated")
    flags = re.findall(r"(?:^|\s)installed=([^\s]+)", users[0])
    require(flags == ["true"], "Target package is not explicitly installed for User 0")
    require(isinstance(installed_apk_path, str) and installed_apk_path.startswith("/data/app/")
            and PurePosixPath(installed_apk_path).name == "base.apk"
            and not any(part in (".", "..") for part in installed_apk_path.split("/"))
            and str(PurePosixPath(installed_apk_path)) == installed_apk_path,
            "Locked installed APK path is not canonical base.apk")
    require(paths == [str(PurePosixPath(installed_apk_path).parent)],
            "Installed codePath missing, duplicated or conflicts with pm path")
    uid = next(iter(ids.values()))
    owned_permission_uids(lines, package, uid)
    return uid


def listed_uid(listing, package):
    """Parse pm list packages -U --user 0; prefix filter may return locked sibling."""
    require(package in LOCKED_PACKAGES and isinstance(listing, str)
            and 0 < len(listing) <= 4096 and "\0" not in listing,
            "Independent User 0 UID output missing or malformed")
    values = {}
    for line in listing.splitlines():
        match = re.fullmatch(r"package:([A-Za-z0-9_.]+) uid:([1-9][0-9]{0,4})", line)
        require(match is not None and match.group(1) in LOCKED_PACKAGES
                and match.group(1).startswith(package) and match.group(1) not in values,
                "Independent User 0 package UID record unknown, duplicate or malformed")
        values[match.group(1)] = app_uid(match.group(2))
    require(package in values and len(set(values.values())) == len(values),
            "Independent target UID missing or shared by locked sibling")
    return values[package]
