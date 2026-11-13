"""Bounded admission for a separately authorized, already-started disposable AVD.

Importing this module has no side effects. It never launches an emulator or an
ADB server. The caller owns the emulator Popen and its cleanup.

Controller hand-off for a future authorized attempt::

    launched_at = time.monotonic()  # immediately before the single owned Popen
    proc = subprocess.Popen(launch_command, ...)
    try:
        result = admit_owned_emulator(proc, launched_at, avd, console_port,
                                      transport_port, new_evidence_dir,
                                      adb_host="127.0.0.1", adb_port=5038)
        if result.status == "ADMITTED":
            check_guest_health_before_any_install(result.serial)
    finally:
        stop_only_the_owned_emulator(proc)

The launch command must specify the exact AVD and explicit port pair. Its
environment must direct the emulator to the same independent ADB server
(ANDROID_ADB_SERVER_PORT=5038 for the default endpoint) after clearing inherited
ADB_SERVER_SOCKET, ANDROID_ADB_SERVER_ADDRESS, ANDROID_ADB_SERVER_PORT and
ADB_SERVER_PORT overrides. Let the emulator register itself as emulator-<console_port>.
The controller checks host service identity and empty inventory before launch,
and guest health and package absence after admission. Use this hand-off as the
only admission path.
"""

from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from functools import partial
from ipaddress import ip_address
import json
import os
from pathlib import Path
import re
import socket
import struct
import subprocess
import time


WINDOW_SECONDS = 180.0
POLL_SECONDS = 3.0
ADB_HOST = "127.0.0.1"
ADB_PORT = 5038
SHARED_ADB_PORT = 5037
BOOT_PROPERTY = "getprop sys.boot_completed"
SHELL_PROBE = "printf 'LITHIC_SHELL_OK\\n'"
IDENTITY_PROPERTIES = (
    "getprop ro.boot.qemu.avd_name; getprop ro.kernel.qemu; "
    "getprop ro.build.version.sdk; getprop ro.product.cpu.abi"
)


@dataclass(frozen=True)
class Reply:
    stdout: str
    stderr: str = ""
    exit_code: int | None = None


@dataclass(frozen=True)
class AdmissionResult:
    status: str
    reason: str
    serial: str | None = None
    identity: dict | None = None


class ProtocolError(Exception):
    pass


def validate_server_address(adb_host, adb_port):
    if type(adb_port) is not int or not 1 <= adb_port <= 65535:
        raise ValueError("ADB port must be an integer in 1..65535")
    if adb_port == SHARED_ADB_PORT:
        raise ValueError("ADB port 5037 is reserved for the shared service")
    if adb_host == "localhost":
        adb_host = ADB_HOST
    if not isinstance(adb_host, str) or "%" in adb_host:
        raise ValueError("ADB host must be a loopback address")
    try:
        address = ip_address(adb_host)
    except ValueError as error:
        raise ValueError("ADB host must be a loopback address") from error
    if not address.is_loopback:
        raise ValueError("ADB host must be a loopback address")
    return str(address), adb_port


def _remaining(deadline):
    remaining = deadline - time.monotonic()
    if remaining <= 0:
        raise TimeoutError("Command deadline reached")
    return remaining


def _receive(sock, size, deadline):
    data = bytearray()
    while len(data) < size:
        sock.settimeout(_remaining(deadline))
        chunk = sock.recv(size - len(data))
        if not chunk:
            raise ProtocolError("Incomplete ADB response")
        data.extend(chunk)
    return bytes(data)


def _body(sock, deadline):
    length_hex = _receive(sock, 4, deadline)
    if not re.fullmatch(rb"[0-9a-fA-F]{4}", length_hex):
        raise ProtocolError("Invalid ADB response length")
    length = int(length_hex, 16)
    if length > 8192:
        raise ProtocolError("ADB response exceeds 8192 bytes")
    return _receive(sock, length, deadline)


def _request(sock, service, deadline):
    command = service.encode("ascii")
    sock.settimeout(_remaining(deadline))
    sock.sendall(f"{len(command):04x}".encode("ascii") + command)
    status = _receive(sock, 4, deadline)
    if status == b"FAIL":
        raise ProtocolError(_body(sock, deadline).decode("utf-8", errors="replace"))
    if status != b"OKAY":
        raise ProtocolError(f"Unexpected ADB protocol status {status!r}")


def _host(service, deadline, *, adb_host=ADB_HOST, adb_port=ADB_PORT):
    server = validate_server_address(adb_host, adb_port)
    if service != "host:devices-l":
        raise ValueError("Admission only permits read-only host inventory")
    with socket.create_connection(server, timeout=_remaining(deadline)) as sock:
        _request(sock, service, deadline)
        return Reply(_body(sock, deadline).decode("utf-8", errors="replace"))


def _shell(serial, command, deadline, *, adb_host=ADB_HOST, adb_port=ADB_PORT):
    server = validate_server_address(adb_host, adb_port)
    if not re.fullmatch(r"emulator-[0-9]+", serial):
        raise ValueError("Shell probes require an automatically registered emulator serial")
    if command not in (BOOT_PROPERTY, SHELL_PROBE, IDENTITY_PROPERTIES):
        raise ValueError("Only fixed read-only admission probes are permitted")
    with socket.create_connection(server, timeout=_remaining(deadline)) as sock:
        _request(sock, f"host:transport:{serial}", deadline)
        _request(sock, f"shell,v2,raw:{command}", deadline)
        streams = {1: bytearray(), 2: bytearray()}
        while True:
            channel, size = struct.unpack("<BI", _receive(sock, 5, deadline))
            if size > 8192 or sum(map(len, streams.values())) + size > 16384:
                raise ProtocolError("ADB shell response exceeds byte limit")
            data = _receive(sock, size, deadline)
            if channel == 3 and size in (1, 4):
                return Reply(*(streams[key].decode("utf-8", errors="replace") for key in (1, 2)),
                             exit_code=int.from_bytes(data, "little"))
            if channel not in streams:
                raise ProtocolError(f"Unexpected shell protocol channel {channel}")
            streams[channel].extend(data)


def _windows_listener(proc, avd, console_port, transport_port, deadline):
    """Read ownership without connecting to the emulator's TCP endpoint."""
    avd_arg = re.escape(avd)
    ports = f"{console_port},{transport_port}"
    command = rf'''
$ErrorActionPreference = 'Stop'
$listeners = @(Get-NetTCPConnection -State Listen -ErrorAction Stop | Where-Object {{ $_.LocalPort -eq {transport_port} }})
if ($listeners.Count -eq 0) {{ 'NOT_LISTENING'; exit 0 }}
$children = @(Get-CimInstance Win32_Process -Filter 'ParentProcessId={proc.pid}' | Where-Object {{
    $_.Name -match '^qemu-system-x86_64(-headless)?[.]exe$' -and
    $_.CommandLine -match '(?:^|\s)"?-avd"?\s+"?{avd_arg}"?(?:\s|$)' -and
    $_.CommandLine -match '(?:^|\s)"?-ports"?\s+"?{ports}"?(?:\s|$)'
}} | Select-Object -ExpandProperty ProcessId)
$owners = @({proc.pid}) + $children
if (@($listeners | Where-Object {{ $_.OwningProcess -notin $owners -or $_.LocalAddress -notin '127.0.0.1','::1' }}).Count) {{
    'FOREIGN_OR_NON_LOOPBACK'; exit 0
}}
'OWNED_LOOPBACK'
'''
    process = subprocess.run(
        ["powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command],
        capture_output=True, encoding="utf-8", errors="replace", timeout=_remaining(deadline),
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0), check=False,
    )
    # subprocess.run kills and waits for its own direct client if it times out.
    return Reply(process.stdout, process.stderr, process.returncode)


def _inventory_rows(output):
    return [line.split() for line in output.splitlines() if line.strip()]


def _observe(*, launched_at, avd, console_port, transport_port, alive, host, shell, listener,
             record, clock=time.monotonic, sleep=time.sleep, server=(ADB_HOST, ADB_PORT)):
    deadline = launched_at + WINDOW_SECONDS
    serial = f"emulator-{console_port}"
    unexpected_loopback = f"127.0.0.1:{transport_port}"

    def attempt(command, maximum_seconds, action, *args):
        start = clock()
        limit = min(deadline, start + maximum_seconds)
        if limit <= start:
            return None
        event = {"command": command, "startedUtc": datetime.now(timezone.utc).isoformat(),
                 "elapsedFromLaunchSeconds": round(start - launched_at, 3),
                 "timeoutSeconds": round(limit - start, 3), "exitCode": None,
                 "adbHost": server[0], "adbPort": server[1]}
        try:
            reply = action(*args, limit)
            event.update(exitCode=reply.exit_code, stderr=reply.stderr,
                         status="COMPLETED" if reply.exit_code in (None, 0) and not reply.stderr else "FAILED")
            if command == "host:devices-l":
                # Host enumeration can contain a physical device. Keep its serial out of evidence.
                event["devices"] = [[row[0], row[1]] if len(row) >= 2 and row[0] in (serial, unexpected_loopback)
                                    else ["<unexpected transport>"] for row in _inventory_rows(reply.stdout)]
            else:
                event["stdout"] = reply.stdout
            if event["status"] == "COMPLETED" and clock() <= limit:
                return reply.stdout.strip()
            if clock() > limit:
                event.update(status="TIMEOUT", error="Response arrived after the command deadline")
        except (subprocess.TimeoutExpired, TimeoutError) as error:
            event.update(status="TIMEOUT", error=str(error))
            if isinstance(error, subprocess.TimeoutExpired):
                for stream in ("stdout", "stderr"):
                    data = getattr(error, stream)
                    if isinstance(data, bytes):
                        data = data.decode("utf-8", errors="replace")
                    event[stream] = data or ""
        except (OSError, ProtocolError) as error:
            event.update(status="FAILED", error=str(error))
        finally:
            event.update(endedUtc=datetime.now(timezone.utc).isoformat(), elapsedSeconds=round(clock() - start, 3))
            record(event)
        return None

    def blocked(reason):
        return AdmissionResult("ENVIRONMENT_BLOCKED", reason)

    def inventory():
        output = attempt("host:devices-l", 3, host, "host:devices-l")
        devices = _inventory_rows(output) if output is not None else []
        if any(row[0] == unexpected_loopback for row in devices):
            return False, blocked("Unexpected second ADB connection (TCP loopback transport)")
        if len(devices) > 1 or any(len(row) < 2 or row[0] != serial for row in devices):
            return False, blocked("Unexpected or ambiguous ADB transport inventory")
        return bool(devices and devices[0][1] == "device"), None

    while clock() < deadline:
        if not alive():
            return blocked("Owned emulator process exited during admission")
        ready, failure = inventory()
        if failure:
            return failure
        if ready:
            owner = attempt("Windows listener ownership", 8, listener)
            if owner == "FOREIGN_OR_NON_LOOPBACK":
                return blocked("ADB transport listener is not owned loopback")
            if owner == "OWNED_LOOPBACK" and alive():
                boot = attempt(f"{serial} shell {BOOT_PROPERTY}", 4, shell, serial, BOOT_PROPERTY)
                if boot == "1":
                    probe = attempt(f"{serial} shell {SHELL_PROBE}", 4, shell, serial, SHELL_PROBE)
                    if probe == "LITHIC_SHELL_OK":
                        identity = attempt(f"{serial} shell {IDENTITY_PROPERTIES}", 5, shell, serial, IDENTITY_PROPERTIES)
                        if identity is not None:
                            if identity.splitlines() != [avd, "1", "36", "x86_64"]:
                                return blocked("Guest AVD, QEMU, API or ABI identity mismatch")
                            ready, failure = inventory()
                            if failure:
                                return failure
                            if ready and clock() < deadline and alive():
                                return AdmissionResult("ADMITTED", "Validated ADB device and guest identity",
                                                       serial, {"avd": avd, "qemu": "1", "api": "36",
                                                                "abi": "x86_64", "bootCompleted": "1"})
        remaining = deadline - clock()
        if remaining > 0:
            sleep(min(POLL_SECONDS, remaining))
    return blocked("180-second window expired before admission was established")


def admit_owned_emulator(proc, launched_at, avd, console_port, transport_port, evidence_dir,
                         *, adb_host=ADB_HOST, adb_port=ADB_PORT):
    """Observe once using the 180-second deadline measured before the caller's Popen.

    `evidence_dir` is a new, caller-owned directory. The caller must have verified
    an empty inventory before launching one fresh AVD with explicit `-ports`.
    ADB requests use the selected independent loopback server only.
    No command in this module runs before that separately authorized launch.
    """
    server = validate_server_address(adb_host, adb_port)
    if os.name != "nt" or not isinstance(proc, subprocess.Popen):
        raise ValueError("An owned Windows emulator Popen is required")
    if not re.fullmatch(r"[A-Za-z0-9_]+", avd) or not (
        1024 <= console_port < 65535 and transport_port == console_port + 1
    ) or {server[1], SHARED_ADB_PORT}.intersection((console_port, transport_port)):
        raise ValueError("Invalid AVD name or explicit emulator port pair")
    arguments = proc.args
    ports = f"{console_port},{transport_port}"
    if (not isinstance(arguments, (list, tuple)) or not arguments or
            Path(arguments[0]).name.lower() != "emulator.exe" or
            arguments.count("-avd") != 1 or arguments.count("-ports") != 1 or
            tuple(arguments[arguments.index("-avd") + 1:arguments.index("-avd") + 2]) != (avd,) or
            tuple(arguments[arguments.index("-ports") + 1:arguments.index("-ports") + 2]) != (ports,)):
        raise ValueError("Emulator process arguments do not match the intended AVD and port pair")
    if not 0 <= time.monotonic() - launched_at < WINDOW_SECONDS:
        return AdmissionResult("ENVIRONMENT_BLOCKED", "Launch timestamp is outside the admission window")

    directory = Path(evidence_dir)
    # Exclusive claim prevents overwriting old observations or retrying in this evidence directory.
    directory.mkdir(parents=True, exist_ok=True)
    with (directory / "admission-claim.json").open("x", encoding="utf-8") as claim:
        json.dump({"pid": proc.pid, "avd": avd, "ports": [console_port, transport_port],
                   "windowSeconds": WINDOW_SECONDS, "maxConnectRequests": 0,
                   "registration": "emulator-auto", "expectedSerial": f"emulator-{console_port}",
                   "adbHost": server[0], "adbPort": server[1]}, claim)
    with (directory / "admission-commands.jsonl").open("x", encoding="utf-8") as log:
        def record(event):
            log.write(json.dumps(event, ensure_ascii=False) + "\n")
            log.flush()

        result = _observe(launched_at=launched_at, avd=avd, console_port=console_port,
                          transport_port=transport_port, alive=lambda: proc.poll() is None,
                          host=partial(_host, adb_host=server[0], adb_port=server[1]),
                          shell=partial(_shell, adb_host=server[0], adb_port=server[1]),
                          listener=lambda limit: _windows_listener(proc, avd, console_port, transport_port, limit),
                          record=record, server=server)
    with (directory / "admission-result.json").open("x", encoding="utf-8") as output:
        json.dump({**asdict(result), "adbHost": server[0], "adbPort": server[1]}, output, indent=2)
        output.write("\n")
    return result


if __name__ == "__main__":
    raise SystemExit("This module must be called by the authorized controller that owns the emulator Popen")
