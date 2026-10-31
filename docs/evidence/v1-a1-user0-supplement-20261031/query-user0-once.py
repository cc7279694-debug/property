"""One authorized User0 metadata query through an already-running ADB server.

No adb executable, daemon control, retry, App start, or device write is used.
Importing this module for local synthetic parsing checks performs no I/O.
"""
from pathlib import Path
import datetime
import hashlib
import json
import re
import socket
import struct
import time


OUT = Path(__file__).resolve().parent
PACKAGE = "com.cooper.lithic.preview.debug"
EXPECTED = "e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92"
BASELINE = "64badfe3f211b988138dab5b057c52413e005880"
AWK = r'''
BEGIN { sections=0; inside=0; target=0; packages=0; users=0; fields=0; bad=0; value="" }
/DUMP TIMEOUT|DUMP OF SERVICE .* FAILED|Error dumping service|Error dumping package|Can.t find service|Can.t find package|Permission Denial|Unknown package|No packages found/ { bad++; next }
$0 == "Packages:" { sections++; inside=1; target=0; next }
inside && /^[^[:space:]]/ { inside=0; target=0 }
inside && /^[[:space:]]*Package \[/ {
    target=0
    if ($0 ~ /^  Package \[com\.cooper\.lithic\.preview\.debug\] \([^)]*\):[[:space:]]*$/) {
        packages++; target=1
    }
    next
}
inside && target && /^    User 0:/ {
    users++
    for (i=1; i<=NF; i++) {
        if ($i ~ /^installed=/) {
            fields++
            if ($i == "installed=true") value="true"
            else if ($i == "installed=false") value="false"
            else bad++
        }
    }
}
END {
    printf "PACKAGES_SECTIONS=%d\nPACKAGE_MATCHES=%d\nUSER0_MATCHES=%d\nINSTALLED_FIELDS=%d\nINVALID_FIELDS=%d\n", sections, packages, users, fields, bad
    if (sections == 1 && packages == 1 && users == 1 && fields == 1 && bad == 0 && value != "") {
        print "PACKAGE=com.cooper.lithic.preview.debug"
        print "USER=0"
        print "INSTALLED=" value
    } else exit 65
}
'''.strip()

# The entire precise-package dump remains in device memory, never in a file or
# transmitted to the host. Both source and filter exits are explicitly reported.
COMMAND = (
    "dump=$(dumpsys package " + PACKAGE + " 2>&1); source_rc=$?; "
    "printf '%s\\n' \"$dump\" | awk '" + AWK + "'; filter_rc=$?; "
    "printf 'SOURCE_EXIT=%s\\nFILTER_EXIT=%s\\n' \"$source_rc\" \"$filter_rc\"; "
    "if [ \"$source_rc\" -ne 0 ]; then exit \"$source_rc\"; fi; exit \"$filter_rc\""
)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def parse_minimal(stdout, stderr, exit_code):
    if stderr or exit_code != 0:
        raise ValueError("QUERY_EXIT_OR_STDERR")
    text = stdout.decode("ascii", errors="strict")
    lines = text.splitlines()
    if len(lines) != 10 or not text.endswith("\n"):
        raise ValueError("MISSING_OR_EXTRA_RECEIPT_FIELDS")
    values = {}
    for line in lines:
        key, value = line.split("=", 1)
        if key in values:
            raise ValueError("DUPLICATE_RECEIPT_KEY")
        values[key] = value
    expected = {
        "PACKAGES_SECTIONS": "1", "PACKAGE_MATCHES": "1", "USER0_MATCHES": "1",
        "INSTALLED_FIELDS": "1", "INVALID_FIELDS": "0", "PACKAGE": PACKAGE,
        "USER": "0", "SOURCE_EXIT": "0", "FILTER_EXIT": "0",
    }
    if any(values.get(key) != value for key, value in expected.items()):
        raise ValueError("IDENTITY_UNIQUENESS_OR_SOURCE_EXIT")
    if values.get("INSTALLED") not in ("true", "false"):
        raise ValueError("INSTALLED_NOT_BOOLEAN")
    return values["INSTALLED"] == "true"


def minimal_text(stdout):
    text = stdout.decode("utf-8", errors="replace")
    safe = all(re.fullmatch(
        r"(?:PACKAGES_SECTIONS|PACKAGE_MATCHES|USER0_MATCHES|INSTALLED_FIELDS|INVALID_FIELDS|USER|SOURCE_EXIT|FILTER_EXIT)=\d+|PACKAGE=com\.cooper\.lithic\.preview\.debug|INSTALLED=(?:true|false)",
        line) for line in text.splitlines())
    return text if safe else "<UNEXPECTED_OUTPUT_NOT_SAVED>"


def main():
    # Exclusive local claim is created before the first network request. A second
    # invocation cannot open the smart socket, even after timeout or interruption.
    with (OUT / "session-claim.json").open("x", encoding="utf-8") as claim:
        json.dump({"authorizedSessions": 1, "maximumTargetShellQueries": 1,
                   "serialSha256": EXPECTED, "baseline": BASELINE,
                   "createdUtc": datetime.datetime.now(datetime.timezone.utc).isoformat()}, claim)
    start = time.monotonic()
    serial = None
    stdout, stderr, exit_code = b"", b"", None
    records = []
    result = {"baseline": BASELINE, "serialSha256": EXPECTED,
              "startedUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
              "outcome": "PREVIEW12_USER0_INSTALLED — UNKNOWN", "installed": None,
              "systemUiManualHealth": "PENDING_USER_CONFIRMATION",
              "installationAuthorized": False, "deviceWrites": 0,
              "hostEnumerations": 0, "targetShellRequests": 0,
              "otherDeviceQueries": 0, "automaticRetries": 0, "commands": records}

    def save():
        (OUT / "observation.json").write_text(
            json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    def receive(sock, length, deadline):
        data = b""
        while len(data) < length:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                raise TimeoutError("QUERY_DEADLINE")
            sock.settimeout(remaining)
            part = sock.recv(length - len(data))
            if not part:
                raise RuntimeError("TRUNCATED_PROTOCOL_REPLY")
            data += part
        return data

    def request(sock, service, deadline):
        data = service.encode("utf-8")
        sock.sendall(f"{len(data):04x}".encode("ascii") + data)
        status = receive(sock, 4, deadline)
        if status != b"OKAY":
            # Do not fetch/save a FAIL body that could contain an unredacted serial.
            raise RuntimeError("ADB_PROTOCOL_NOT_OKAY")

    save()
    try:
        deadline = time.monotonic() + 5
        host_row = {"request": "host:devices-l", "status": "STARTED",
                    "hostEndpoint": "127.0.0.1:5037", "processExitCode": "NOT_APPLICABLE_SMART_SOCKET"}
        records.append(host_row)
        result["hostEnumerations"] = 1
        save()
        with socket.create_connection(("127.0.0.1", 5037), timeout=5) as sock:
            request(sock, "host:devices-l", deadline)
            size = int(receive(sock, 4, deadline), 16)
            if size > 16384:
                raise RuntimeError("HOST_LIST_TOO_LARGE")
            raw = receive(sock, size, deadline)
        host_row.update(status="COMPLETED", protocolStatus="OKAY",
                        rawBytes=len(raw), rawSha256=sha(raw), rawDeviceListSaved=False)
        lines = [line.split() for line in raw.decode("utf-8", errors="strict").splitlines() if line.strip()]
        matched = [line for line in lines if sha(line[0].encode("utf-8")) == EXPECTED]
        result.update(enumeratedDeviceCount=len(lines), authorizedTargetMatches=len(matched))
        if len(matched) != 1 or len(matched[0]) < 2 or matched[0][1] != "device":
            result.update(outcome="ENVIRONMENT_BLOCKED", reason="TARGET_NOT_UNIQUE_CONNECTED_DEVICE")
            return
        serial = matched[0][0]
        if serial.startswith("emulator-") or ":" in serial:
            result.update(outcome="ENVIRONMENT_BLOCKED", reason="TARGET_TRANSPORT_NOT_EXPECTED_USB")
            return
        result["targetConnectionState"] = "device"
        save()

        deadline = time.monotonic() + 10
        row = {"transportSelector": "host:transport:<USB_SERIAL_REDACTED>",
               "serialSha256": EXPECTED, "service": "shell,v2,raw:" + COMMAND,
               "status": "STARTED", "exitCode": None, "exitPacketReceived": False}
        records.append(row)
        save()
        with socket.create_connection(("127.0.0.1", 5037), timeout=5) as sock:
            request(sock, "host:transport:" + serial, deadline)
            row["transportProtocolStatus"] = "OKAY"
            result["targetShellRequests"] = 1
            save()
            request(sock, "shell,v2,raw:" + COMMAND, deadline)
            row["shellProtocolStatus"] = "OKAY"
            while exit_code is None:
                channel, size = struct.unpack("<BI", receive(sock, 5, deadline))
                if size > 4096:
                    raise RuntimeError("SHELL_FRAME_TOO_LARGE")
                data = receive(sock, size, deadline)
                if channel == 1:
                    stdout += data
                elif channel == 2:
                    stderr += data
                elif channel == 3 and size in (1, 4):
                    exit_code = int.from_bytes(data, "little")
                else:
                    raise RuntimeError("UNEXPECTED_SHELL_CHANNEL")
                if len(stdout) + len(stderr) > 4096:
                    raise RuntimeError("SHELL_OUTPUT_TOO_LARGE")
        # Save only the allowlisted, device-filtered receipt. Unexpected output is
        # represented by its hash/length, not an unrestricted error/dump payload.
        row.update(status="COMPLETED", exitCode=exit_code, exitPacketReceived=True,
                   stdoutBytes=len(stdout), stdoutSha256=sha(stdout),
                   stderrBytes=len(stderr), stderrSha256=sha(stderr),
                   minimalStdout=minimal_text(stdout),
                   stderrSaved=False)
        save()
        installed = parse_minimal(stdout, stderr, exit_code)
        result.update(installed=installed, reason="UNIQUE_EXACT_PACKAGE_USER0_BOOLEAN_CONFIRMED",
                      outcome=("PREVIEW12_USER0_INSTALLED — CONFIRMED" if installed else
                               "PREVIEW12_USER0_NOT_INSTALLED — CONFIRMED"))
    except (ConnectionRefusedError, ConnectionResetError) as exc:
        result.update(reason="STOPPED_" + type(exc).__name__)
        if result["targetShellRequests"] == 0:
            result["outcome"] = "ENVIRONMENT_BLOCKED"
    except Exception as exc:
        result.update(reason="STOPPED_" + type(exc).__name__)
        if result["hostEnumerations"] and not result.get("targetConnectionState"):
            result["outcome"] = "ENVIRONMENT_BLOCKED"
    finally:
        for row in records:
            if row["status"] == "STARTED":
                row["status"] = "INCOMPLETE_EXIT_UNKNOWN"
                if "service" in row:
                    row.update(exitCode=exit_code, exitPacketReceived=exit_code is not None,
                               stdoutBytes=len(stdout), stdoutSha256=sha(stdout),
                               stderrBytes=len(stderr), stderrSha256=sha(stderr),
                               minimalStdout=minimal_text(stdout), stderrSaved=False)
        result.update(finishedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                      elapsedSeconds=round(time.monotonic() - start, 3))
        save()
        print(json.dumps({key: result[key] for key in (
            "outcome", "installed", "hostEnumerations", "targetShellRequests", "elapsedSeconds")}, ensure_ascii=True))


if __name__ == "__main__":
    main()
