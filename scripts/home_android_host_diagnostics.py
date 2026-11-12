"""Read-only Windows host diagnostics. Never starts ADB or sends a device command."""

import argparse
from datetime import datetime, timezone
import ipaddress
import json
import locale
import os
from pathlib import Path
import re
import socket
import subprocess
import time


def utc_now():
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds")


def decode(data):
    if not data:
        return ""
    try:
        return data.decode("utf-8-sig")
    except UnicodeDecodeError:
        return data.decode(locale.getpreferredencoding(False), errors="replace")


def command(args, timeout=15):
    entry = {"command": [str(arg) for arg in args], "start_utc": utc_now(), "timeout_seconds": timeout}
    start = time.monotonic()
    try:
        process = subprocess.run(args, capture_output=True, timeout=timeout)
        entry.update(exit=process.returncode, stdout=decode(process.stdout), stderr=decode(process.stderr))
    except subprocess.TimeoutExpired as error:
        entry.update(exit=None, stdout=decode(error.stdout), stderr=decode(error.stderr), error=str(error))
    except OSError as error:
        entry.update(exit=None, error=str(error))
    entry.update(end_utc=utc_now(), elapsed_seconds=round(time.monotonic() - start, 3))
    return entry


def read_exact(stream, length, deadline):
    received = bytearray()
    while len(received) < length:
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            raise TimeoutError("Host reply exceeded its deadline")
        stream.settimeout(remaining)
        chunk = stream.recv(length - len(received))
        if not chunk:
            raise OSError("Incomplete host reply")
        received.extend(chunk)
    return bytes(received)


def status_fields(payload):
    # The existing server returns an AdbServerStatus protobuf. Keep only its public identity.
    # In particular, the response can contain paths to ADB authentication files.
    cursor = 0

    def varint():
        nonlocal cursor
        value = 0
        for shift in range(0, 70, 7):
            if cursor >= len(payload):
                raise ValueError("Truncated status field")
            octet = payload[cursor]
            cursor += 1
            value |= (octet & 127) << shift
            if octet < 128:
                return value
        raise ValueError("Oversized status field")

    public_fields = {5: "version", 6: "build", 7: "executable_path"}
    identity = {}
    while cursor < len(payload):
        tag = varint()
        field, wire = tag >> 3, tag & 7
        if wire == 0:
            varint()
            continue
        if wire == 2:
            length = varint()
        elif wire in (1, 5):
            length = 8 if wire == 1 else 4
        else:
            raise ValueError("Unexpected status field encoding")
        end = cursor + length
        if end > len(payload):
            raise ValueError("Truncated status payload")
        if wire == 2 and field in public_fields:
            identity[public_fields[field]] = decode(payload[cursor:end])
        cursor = end
    if not re.fullmatch(r"\d+\.\d+\.\d+", identity.get("version", "")):
        raise ValueError("Unrecognized server identity response")
    return identity


def diagnostic_address(host, port):
    if host == "localhost":
        host = "127.0.0.1"
    if (not isinstance(host, str) or not ipaddress.ip_address(host).is_loopback or
            type(port) is not int or not 1 <= port <= 65535):
        raise ValueError("Diagnostics require a loopback ADB host and a valid TCP port")
    return host, port


def host_request(service, *, adb_host="127.0.0.1", adb_port=5038):
    if service not in ("host:version", "host:server-status", "host:devices-l"):
        raise ValueError("Only fixed read-only ADB host queries are permitted")
    host, port = diagnostic_address(adb_host, adb_port)
    timeout = 3
    endpoint = f"[{host}]:{port}" if ":" in host else f"{host}:{port}"
    entry = {"command": service, "endpoint": endpoint, "start_utc": utc_now(), "timeout_seconds": timeout}
    start = time.monotonic()
    deadline = start + timeout
    try:
        with socket.create_connection((host, port), timeout) as stream:
            request = service.encode("ascii")
            stream.settimeout(max(0.001, deadline - time.monotonic()))
            stream.sendall(f"{len(request):04x}".encode("ascii") + request)
            status = read_exact(stream, 4, deadline).decode("ascii")
            size = int(read_exact(stream, 4, deadline), 16)
            if size > 8192:
                raise ValueError("Host reply exceeds 8192 bytes")
            payload = read_exact(stream, size, deadline)
        entry.update(status=status, exit=None)
        if status != "OKAY":
            entry["error"] = decode(payload)
        elif service == "host:server-status":
            entry["identity"] = status_fields(payload)
            executable = entry["identity"].get("executable_path")
            entry["executable_path_exists"] = bool(executable and Path(executable).is_file())
        elif service == "host:devices-l":
            # A non-emulator serial must not enter the evidence file.
            rows = [row.split() for row in decode(payload).splitlines() if row.strip()]
            entry["devices"] = [
                {"id": row[0] if re.fullmatch(r"emulator-\d+|127\.0\.0\.1:\d+", row[0]) else "REDACTED",
                 "state": row[1] if len(row) > 1 else "UNKNOWN"}
                for row in rows
            ]
        else:
            entry["response"] = decode(payload)
    except (OSError, ValueError) as error:
        entry.update(exit=None, error=str(error))
    entry.update(end_utc=utc_now(), elapsed_seconds=round(time.monotonic() - start, 3))
    return entry


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", type=Path, required=True)
    parser.add_argument("--adb-host", default="127.0.0.1")
    parser.add_argument("--adb-port", type=int, default=5038)
    args = parser.parse_args()
    try:
        adb_host, adb_port = diagnostic_address(args.adb_host, args.adb_port)
    except ValueError as error:
        parser.error(str(error))
    sdk = args.sdk.resolve()
    adb = sdk / "platform-tools" / "adb.exe"
    emulator = sdk / "emulator" / "emulator.exe"
    powershell = ["powershell.exe", "-NoProfile", "-NonInteractive", "-Command"]
    listener_command = """
        $ports=@(5037,5038,17640,17641,ADB_PORT)+(5554..5585)
        $listeners=@(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
            Where-Object LocalPort -In $ports |
            Select-Object LocalAddress,LocalPort,OwningProcess)
        $adbProcesses=@(Get-CimInstance Win32_Process -Filter "Name='adb.exe'" |
            Select-Object Name,ProcessId,ExecutablePath)
        [pscustomobject]@{listeners=$listeners; adbProcesses=$adbProcesses} | ConvertTo-Json -Compress -Depth 4
    """.replace("ADB_PORT", str(adb_port))
    resources_command = """
        $os=Get-CimInstance Win32_OperatingSystem
        $disk=@(Get-PSDrive -Name C,E | Select-Object Name,Free)
        $emulators=@(Get-Process emulator,qemu-system-x86_64,qemu-system-x86_64-headless -ErrorAction SilentlyContinue |
            Select-Object ProcessName,Id)
        [pscustomobject]@{freePhysicalKiB=$os.FreePhysicalMemory; disks=$disk; emulators=$emulators} |
            ConvertTo-Json -Compress -Depth 4
    """
    checks = [command([adb, "version"]), command(["where.exe", "adb.exe"]), command([emulator, "-version"]),
              command([emulator, "-help-ports"]), command([*powershell, listener_command]),
              command([*powershell, resources_command])]
    checks += [command(["netsh.exe", "interface", version, "show", "excludedportrange", "protocol=tcp"])
               for version in ("ipv4", "ipv6")]
    host = [host_request(service, adb_host=adb_host, adb_port=adb_port)
            for service in ("host:version", "host:server-status", "host:devices-l")]
    candidates = {adb.resolve()}
    for name in ("ANDROID_HOME", "ANDROID_SDK_ROOT", "LOCALAPPDATA"):
        if os.environ.get(name):
            directory = Path(os.environ[name])
            if name == "LOCALAPPDATA":
                directory /= "Android/Sdk"
            candidate = directory / "platform-tools/adb.exe"
            if candidate.is_file():
                candidates.add(candidate.resolve())
    where = checks[1]
    if where.get("exit") == 0:
        candidates.update(Path(line.strip()).resolve() for line in where["stdout"].splitlines() if line.strip())
    server = host[1].get("identity", {}).get("executable_path")
    if server:
        candidates.add(Path(server).resolve())
    print(json.dumps({"sdk": str(sdk), "adb_host": adb_host, "adb_port": adb_port,
                      "commands": checks, "existing_server": host,
                      "observed_adb_paths": sorted(str(path) for path in candidates)}, indent=2))


if __name__ == "__main__":
    main()
