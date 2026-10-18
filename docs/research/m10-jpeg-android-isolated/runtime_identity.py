"""Pure, fail-closed Windows emulator identity gate.

No process, filesystem, Android, or network operations occur in this module.
Only ExecutablePath temporarily null/empty and not-yet-listening target ports
may remain PENDING. The injected collector must include all processes related by
AVD name, private data root, anchored PID/parent, or either target port owner.
"""
from __future__ import annotations

from datetime import datetime, timezone
import math
import ntpath
import re
import time

WINDOW_SECONDS = 30.0
POLL_SECONDS = 2.0
QUERY_SECONDS = 10.0


class IdentityError(ValueError):
    pass


def _require(condition, reason):
    if not condition:
        raise IdentityError(reason)


def _integer(value, reason):
    _require(type(value) is int and value > 0, reason)
    return value


def _path(value):
    _require(isinstance(value, str) and bool(value.strip()) and value == value.strip(),
             "MALFORMED_EXECUTABLE_PATH")
    _require(re.match(r"^[A-Za-z]:[\\/]", value) is not None and
             ".." not in value.replace("\\", "/").split("/"), "NON_ABSOLUTE_OR_PARENT_PATH")
    return ntpath.normcase(ntpath.normpath(value))


def _instant(value):
    """Compare the entire .NET 100ns timestamp, without float/microsecond loss."""
    _require(isinstance(value, str), "CREATION_TIME_MISSING")
    match = re.fullmatch(r"(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d)(?:\.(\d{1,9}))?(Z|[+-]\d\d:\d\d)", value)
    _require(match is not None, "CREATION_TIME_INVALID")
    try:
        whole = datetime.fromisoformat(match[1] + ("+00:00" if match[3] == "Z" else match[3]))
        epoch = datetime(1970, 1, 1, tzinfo=timezone.utc)
        delta = whole.astimezone(timezone.utc) - epoch
        return [delta.days * 86400 + delta.seconds, int((match[2] or "").ljust(9, "0"))]
    except ValueError as error:
        raise IdentityError("CREATION_TIME_INVALID") from error


def _spec(spec):
    _require(isinstance(spec, dict), "SPEC_MISSING")
    _require(isinstance(spec.get("avd"), str) and re.fullmatch(r"[A-Za-z0-9_]+", spec["avd"]), "SPEC_AVD_INVALID")
    port = _integer(spec.get("port"), "SPEC_PORT_INVALID")
    _require(port < 65535 and port % 2 == 0, "SPEC_PORT_INVALID")
    launcher, qemu = _path(spec.get("launcherExe")), _path(spec.get("qemuExe"))
    _require(ntpath.basename(launcher) == "emulator.exe", "SPEC_LAUNCHER_INVALID")
    _require(qemu == ntpath.join(ntpath.dirname(launcher), "qemu", "windows-x86_64", "qemu-system-x86_64-headless.exe"),
             "SPEC_QEMU_SDK_PATH_INVALID")
    argv = spec.get("argv")
    _require(isinstance(argv, list) and len(argv) > 1 and all(isinstance(x, str) for x in argv), "SPEC_ARGV_INVALID")
    _require(_path(argv[0]) == launcher, "SPEC_ARGV_EXECUTABLE_CONFLICT")
    for key, expected in (("-avd", spec["avd"]), ("-port", str(port))):
        _require(argv.count(key) == 1 and argv.index(key) + 1 < len(argv) and argv[argv.index(key) + 1] == expected,
                 "SPEC_ARGUMENT_CONFLICT")
    return {"avd": spec["avd"], "port": port, "launcherExe": launcher,
            "qemuExe": qemu, "argv": list(argv)}


def _arguments(value, expected, executable, argument_parser):
    _require(isinstance(value, str) and bool(value), "COMMAND_LINE_MISSING")
    try:
        actual = argument_parser(value)
    except Exception as error:
        raise IdentityError("WINDOWS_ARGUMENT_PARSE_FAILED") from error
    _require(isinstance(actual, list) and len(actual) == len(expected), "ARGUMENT_SHAPE_CONFLICT")
    _require(_path(actual[0]) == executable, "COMMAND_EXECUTABLE_CONFLICT")
    path_flags = {"-datadir", "-data", "-cache", "-sysdir"}
    for index in range(1, len(expected)):
        if expected[index - 1] in path_flags:
            _require(_path(actual[index]) == _path(expected[index]), "ARGUMENT_PATH_CONFLICT")
        else:
            _require(actual[index] == expected[index], "ARGUMENT_VALUE_CONFLICT")


def _processes(snapshot, spec, argument_parser, *, anchored=False):
    _require(isinstance(snapshot, dict) and isinstance(snapshot.get("targets"), list), "PROCESS_SNAPSHOT_INCOMPLETE")
    rows = snapshot["targets"]
    _require(len(rows) == 2, "TARGET_PROCESS_SET_CONFLICT")
    count = snapshot.get("emulatorProcessCount")
    _require(type(count) is int and count >= len(rows), "PROCESS_COUNT_INCOMPLETE")
    found, pending = {}, []
    for row in rows:
        _require(isinstance(row, dict), "PROCESS_ROW_INVALID")
        for key in ("Name", "ProcessId", "ParentProcessId", "CreationDate", "CommandLine", "ExecutablePath"):
            _require(key in row, "PROCESS_FIELD_MISSING:" + key)
        name = row["Name"]
        _require(isinstance(name, str), "PROCESS_NAME_INVALID")
        role = {"emulator.exe": "launcher", "qemu-system-x86_64-headless.exe": "qemu"}.get(name.lower())
        _require(role is not None and role not in found, "TARGET_PROCESS_ROLE_CONFLICT")
        executable = spec["launcherExe" if role == "launcher" else "qemuExe"]
        process = {"role": role, "pid": _integer(row["ProcessId"], "PROCESS_ID_INVALID"),
                   "parentPid": _integer(row["ParentProcessId"], "PARENT_ID_INVALID"),
                   "created": _instant(row["CreationDate"]), "name": name.lower(),
                   "expectedExecutable": executable}
        _arguments(row["CommandLine"], spec["argv"], executable, argument_parser)
        observed = row["ExecutablePath"]
        if observed is None or observed == "":
            _require(not anchored, "ANCHOR_EXECUTABLE_INCOMPLETE")
            pending.append(role + ":EXECUTABLE_PATH_NULL")
        else:
            _require(_path(observed) == executable, "EXECUTABLE_PATH_CONFLICT")
        found[role] = process
    launcher, qemu = found["launcher"], found["qemu"]
    _require(launcher["pid"] != qemu["pid"] and qemu["parentPid"] == launcher["pid"], "PARENT_CHILD_IDENTITY_CONFLICT")
    _require(qemu["created"] >= launcher["created"], "PROCESS_CREATION_ORDER_CONFLICT")
    return [launcher, qemu], pending


def _ports(snapshot, spec, qemu_pid):
    _require("ports" in snapshot and isinstance(snapshot["ports"], list), "PORT_SNAPSHOT_INCOMPLETE")
    found, seen = set(), set()
    for row in snapshot["ports"]:
        _require(isinstance(row, dict) and all(key in row for key in ("LocalAddress", "LocalPort", "OwningProcess")),
                 "PORT_ROW_INCOMPLETE")
        address, port, owner = row["LocalAddress"], row["LocalPort"], row["OwningProcess"]
        _require(address in ("127.0.0.1", "::1"), "NON_LOOPBACK_PORT_LISTENER")
        _require(type(port) is int and port in (spec["port"], spec["port"] + 1), "UNEXPECTED_PORT_RECORD")
        _require(type(owner) is int and owner == qemu_pid, "PORT_OWNER_CONFLICT")
        _require((address, port) not in seen, "DUPLICATE_PORT_RECORD")
        seen.add((address, port))
        found.add(port)
    return [] if found == {spec["port"], spec["port"] + 1} else ["TARGET_PORTS_NOT_YET_LISTENING"]


def classify(snapshot, anchor, spec, argument_parser):
    """Return PASS/PENDING/BLOCKED; a single PASS is not an admission proof.

    An existing-instance anchor is the immutable archived ending snapshot. For a
    newly authorized start, anchor=None captures the first complete PID, parent,
    creation-time, argv and expected SDK identity, even if its path is still null.
    Future calls must pass the returned anchor; never recapture after a conflict.
    """
    try:
        checked = _spec(spec)
        current, pending = _processes(snapshot, checked, argument_parser)
        if anchor is None:
            stable = {"version": 1, "processes": current, "spec": checked}
        elif isinstance(anchor, dict) and anchor.get("version") == 1:
            _require(anchor.get("spec") == checked and anchor.get("processes") == current, "ANCHORED_PROCESS_IDENTITY_CONFLICT")
            stable = anchor
        else:
            previous, _ = _processes(anchor, checked, argument_parser, anchored=True)
            _require(current == previous, "ANCHORED_PROCESS_IDENTITY_CONFLICT")
            stable = {"version": 1, "processes": previous, "spec": checked}
        pending += _ports(snapshot, checked, current[1]["pid"])
        return {"status": "PENDING" if pending else "PASS", "reason": pending or "COMPLETE_IDENTITY_OBSERVED",
                "anchor": stable, "pending": pending}
    except (IdentityError, KeyError, TypeError, IndexError) as error:
        return {"status": "BLOCKED", "reason": str(error)}


def await_complete(snapshot_fn, anchor, spec, argument_parser, *, timeout=WINDOW_SECONDS, clock=time.monotonic, sleep=time.sleep):
    """Read-only observation, one fixed 30s window; two consecutive PASS samples.

    snapshot_fn(timeout=...) receives min(10s, remaining total window). It must
    enforce that subprocess timeout. Timeout/error is UNKNOWN and stops; it never
    authorizes ADB or any device mutation. timeout can only shorten the fixed
    maximum 30s window (e.g. an enclosing boot deadline). No callback can extend it.
    """
    if type(timeout) not in (int, float) or not math.isfinite(timeout) or not (0 < timeout <= WINDOW_SECONDS):
        return {"status": "BLOCKED", "reason": "IDENTITY_WINDOW_ARGUMENT_INVALID"}
    begin = clock()
    deadline = begin + timeout
    observed, consecutive, stable = [], 0, anchor
    while True:
        remaining = deadline - clock()
        if remaining <= 0:
            return {"status": "BLOCKED", "reason": "IDENTITY_WINDOW_EXHAUSTED", "observations": observed,
                    "windowSeconds": timeout, "elapsedSeconds": clock() - begin}
        try:
            snapshot = snapshot_fn(timeout=min(QUERY_SECONDS, remaining))
        except Exception as error:
            return {"status": "BLOCKED", "reason": "CIM_OBSERVATION_UNKNOWN", "errorType": type(error).__name__,
                    "detail": str(error), "observations": observed, "elapsedSeconds": clock() - begin}
        if clock() > deadline:
            return {"status": "BLOCKED", "reason": "IDENTITY_WINDOW_EXHAUSTED", "observations": observed,
                    "windowSeconds": timeout, "elapsedSeconds": clock() - begin}
        result = classify(snapshot, stable, spec, argument_parser)
        observed.append({"elapsedSeconds": clock() - begin, "status": result["status"], "reason": result["reason"]})
        if result["status"] == "BLOCKED":
            return {**result, "observations": observed, "elapsedSeconds": clock() - begin}
        stable = result["anchor"]
        consecutive = consecutive + 1 if result["status"] == "PASS" else 0
        if consecutive == 2:
            return {"status": "PASS", "reason": "TWO_COMPLETE_MATCHING_OBSERVATIONS", "anchor": stable,
                    "observations": observed, "snapshot": snapshot, "windowSeconds": timeout,
                    "elapsedSeconds": clock() - begin, "scope": "CURRENT_READ_ONLY_WINDOWS_IDENTITY"}
        remaining = deadline - clock()
        if remaining > 0:
            sleep(min(POLL_SECONDS, remaining))
