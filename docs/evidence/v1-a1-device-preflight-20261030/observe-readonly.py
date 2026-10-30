"""One bounded observation through an existing ADB smart socket; never starts adb.
Only the historically authorized serial is selected. No serial/Boot UUID is saved.
"""
from pathlib import Path
import datetime, hashlib, json, re, socket, struct, time

ROOT = Path.cwd()
OUT = ROOT / "docs/evidence/v1-a1-device-preflight-20261030"
EXPECTED = "e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92"
PACKAGES = ["com.cooper.lithic.v1acceptance.debug", "com.cooper.lithic.v1acceptance.debug.test", "com.cooper.lithic.preview.debug"]
with (OUT / "session-claim.json").open("x", encoding="utf-8") as claim:
    json.dump({"authorizedSessions": 1, "serialSha256": EXPECTED, "hostEndpoint": "127.0.0.1:5037",
               "deviceWritesAuthorized": False, "createdUtc": datetime.datetime.now(datetime.timezone.utc).isoformat()}, claim)
START = time.monotonic()
SERIAL = None
TOTAL = 0
DEADLINE = START + 90
RECORDS = []
RESULT = {"startedUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(), "outcome": "BLOCKED_UNKNOWN",
          "serialSha256": EXPECTED, "packages": {p: {"status": "NOT_RUN"} for p in PACKAGES},
          "systemUiAnr": "UNKNOWN_NO_SAFE_PRECISE_QUERY", "installationAuthorized": False,
          "deviceWrites": 0, "appStarts": 0, "testExecutions": 0, "otherDeviceCommands": 0}
digest = lambda data: hashlib.sha256(data).hexdigest()


def save():
    RESULT.update(elapsedSeconds=round(time.monotonic() - START, 3), commands=RECORDS)
    (OUT / "observation.json").write_text(json.dumps(RESULT, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def receive(sock, length):
    data = b""
    while len(data) < length:
        remaining = min(DEADLINE, START + 90) - time.monotonic()
        if remaining <= 0:
            raise TimeoutError("READ_ONLY_QUERY_DEADLINE")
        sock.settimeout(min(8, remaining))
        part = sock.recv(length - len(data))
        if not part:
            raise RuntimeError("TRUNCATED_PROTOCOL_REPLY")
        data += part
    return data


def request(sock, service):
    data = service.encode("utf-8")
    sock.sendall(f"{len(data):04x}".encode() + data)
    status = receive(sock, 4)
    if status != b"OKAY":
        # A FAIL body can repeat a serial; never save it or expand the query.
        if status == b"FAIL":
            size = int(receive(sock, 4), 16)
            receive(sock, min(size, 8192))
        raise RuntimeError("ADB_PROTOCOL_NOT_OKAY")


def connect():
    global DEADLINE
    if time.monotonic() - START > 90:
        raise TimeoutError("OBSERVATION_BUDGET")
    DEADLINE = min(START + 90, time.monotonic() + 10)
    return socket.create_connection(("127.0.0.1", 5037), timeout=8)


def host(service):
    row = {"transport": "existing localhost smart socket", "request": service, "status": "STARTED"}
    RECORDS.append(row)
    save()
    with connect() as sock:
        request(sock, service)
        size = int(receive(sock, 4), 16)
        assert size <= 16384
        data = receive(sock, size)
    row.update(status="COMPLETED", protocolStatus="OKAY", rawBytes=len(data), rawSha256=digest(data), rawSerialsSaved=False)
    save()
    return data


def shell(label, command, allowed=(0,), secret=False, optional=False):
    global TOTAL
    assert SERIAL is not None
    assert not any(w in command for w in ("force-stop", "am start", "instrument", "install ", "uninstall", "clear ", "reboot", " root", "logcat", "jobscheduler", "/data/data", "/data/user", "screencap"))
    began = time.monotonic()
    row = {"label": label, "transportSelector": "host:transport:<USB_SERIAL_REDACTED>",
           "serialSha256": EXPECTED, "service": "shell,v2,raw:" + command, "status": "STARTED", "exitCode": None}
    RECORDS.append(row)
    save()
    stdout, stderr, exit_code = b"", b"", None
    with connect() as sock:
        request(sock, "host:transport:" + SERIAL)
        request(sock, "shell,v2,raw:" + command)
        while exit_code is None:
            assert time.monotonic() - began <= 10
            channel, size = struct.unpack("<BI", receive(sock, 5))
            assert size <= 8192
            data = receive(sock, size)
            if channel == 1:
                stdout += data
            elif channel == 2:
                stderr += data
            elif channel == 3:
                assert size in (1, 4)
                exit_code = int.from_bytes(data, "little")
            else:
                raise RuntimeError("UNEXPECTED_SHELL_CHANNEL")
            assert len(stdout) + len(stderr) <= 16384
    TOTAL += len(stdout) + len(stderr)
    assert TOTAL <= 131072
    text = stdout.decode("utf-8", errors="strict")
    safe = "<BOOT_ID_REDACTED_SHA256_ONLY>\n" if secret else text.replace(SERIAL, "<USB_SERIAL_REDACTED>")
    error = stderr.decode("utf-8", errors="replace").replace(SERIAL, "<USB_SERIAL_REDACTED>")
    row.update(status="COMPLETED", exitCode=exit_code, seconds=round(time.monotonic() - began, 3),
               stdoutBytes=len(stdout), stdoutSha256=digest(stdout), stderrBytes=len(stderr), stderrSha256=digest(stderr),
               sanitizedStdout=safe, sanitizedStderr=error)
    save()
    if exit_code not in allowed or stderr:
        if optional:
            return None
        raise RuntimeError("READ_ONLY_QUERY_FAILED_" + label)
    return text.strip()


def package_status(package):
    # Exact output filtering on device, including uninstalled retained records.
    pattern = "^package:" + package.replace(".", r"\.") + "([[:space:]]|$)"
    cmd = (f"rows=$(cmd package list packages -u -U --show-versioncode --user 0 {package}); rc=$?; "
           "printf 'SOURCE_EXIT=%s\\n' \"$rc\"; "
           f"printf '%s\\n' \"$rows\" | grep -E '{pattern}'; "
           "match=$?; [ \"$match\" -le 1 ] || exit \"$match\"; exit \"$rc\"")
    text = shell("package-retained-" + package, cmd)
    lines = text.splitlines()
    assert lines and lines[0] == "SOURCE_EXIT=0"
    body = lines[1:]
    assert len(body) <= 1
    if not body:
        return {"status": "ABSENT_IN_USER0_INCLUDING_RETAINED_RECORD_QUERY", "queryCompleted": True}
    assert body[0].startswith("package:" + package + " ")
    version = re.search(r"\bversionCode:(\d+)\b", body[0])
    uid = re.search(r"\buid:(\d+)\b", body[0])
    assert version and uid
    return {"status": "PACKAGE_RECORD_PRESENT", "versionCode": int(version[1]), "uid": int(uid[1]), "queryCompleted": True}


def main():
    if not json.loads((OUT / "host-service.json").read_text(encoding="utf-8-sig"))["ready"]:
        RESULT.update(outcome="ENVIRONMENT_BLOCKED", reason="ADB_NOT_READY")
        return
    version = host("host:version")
    RESULT["adbServerVersionHex"] = version.decode("ascii")
    raw = host("host:devices-l")
    lines = [line for line in raw.decode("utf-8").splitlines() if line.strip()]
    RESULT["enumeratedDeviceCount"] = len(lines)
    matched = []
    for line in lines:
        fields = line.split()
        if fields and digest(fields[0].encode()) == EXPECTED:
            matched.append(fields)
    RESULT["authorizedTargetMatches"] = len(matched)
    if not matched:
        RESULT.update(outcome="ENVIRONMENT_BLOCKED", reason="AUTHORIZED_TARGET_NOT_CONNECTED")
        return
    if len(matched) != 1 or len(matched[0]) < 2 or matched[0][1] != "device":
        RESULT.update(reason="TARGET_IDENTITY_OR_CONNECTION_AMBIGUOUS")
        return
    global SERIAL
    SERIAL = matched[0][0]
    assert not SERIAL.startswith("emulator-") and ":" not in SERIAL
    RESULT["connectionState"] = "device"
    properties = {}
    for name, prop in (("manufacturer", "ro.product.manufacturer"), ("model", "ro.product.model"),
                       ("api", "ro.build.version.sdk"), ("androidRelease", "ro.build.version.release"),
                       ("abi", "ro.product.cpu.abi"), ("abiList", "ro.product.cpu.abilist"), ("bootCompleted", "sys.boot_completed")):
        properties[name] = shell(name, "getprop " + prop)
        RESULT["identity"] = properties
        if name == "model" and properties[name] != "23127PN0CC":
            RESULT.update(reason="MODEL_MISMATCH")
            return
    assert properties["manufacturer"].lower() == "xiaomi" and properties["api"] == "36" and properties["abi"] == "arm64-v8a"
    assert properties["bootCompleted"] == "1"
    boot = shell("boot-id-initial", "cat /proc/sys/kernel/random/boot_id", secret=True)
    assert re.fullmatch(r"[0-9a-f-]{36}", boot)
    RESULT["bootIdSha256"] = digest(boot.encode())
    memory = shell("memory", "grep -E '^(MemTotal|MemAvailable):' /proc/meminfo")
    values = {k: int(v) for k, v in re.findall(r"(MemTotal|MemAvailable):\s+(\d+) kB", memory)}
    assert len(values) == 2
    RESULT["memoryKiB"] = values
    space = shell("data-space", "df -k /data")
    columns = space.splitlines()[-1].split()
    assert len(columns) >= 6
    RESULT["dataAvailableKiB"] = int(columns[3])
    battery = shell("battery", "set -o pipefail; dumpsys battery | grep -E '^  (level|scale|status|health|temperature|AC powered|USB powered|Wireless powered):'")
    RESULT["battery"] = dict(re.findall(r"^\s*([^:]+):\s*(.+)$", battery, re.M))
    assert all(k in RESULT["battery"] for k in ("level", "scale", "status", "temperature", "USB powered"))
    RESULT["basicResourceFloors"] = {"availableRamKiB": 2097152, "dataAvailableKiB": 2097152, "batteryPercent": 20, "temperatureBelowTenthsC": 450}
    if (values["MemAvailable"] < 2097152 or RESULT["dataAvailableKiB"] < 2097152 or
            int(RESULT["battery"]["level"]) * 100 / int(RESULT["battery"]["scale"]) < 20 or
            int(RESULT["battery"]["temperature"]) >= 450):
        RESULT.update(reason="BASIC_RESOURCE_OR_BATTERY_GATE")
        return
    pid1 = shell("systemui-initial", "pidof com.android.systemui", allowed=(0, 1))
    if not pid1 or not all(p.isdigit() for p in pid1.split()):
        RESULT.update(reason="SYSTEMUI_PROCESS_NOT_CONFIRMED")
        return
    RESULT["systemUiInitialPids"] = pid1.split()
    # Independent, explicitly authorized metadata reads; no Activity/Job/window dump is used.
    for package in PACKAGES:
        state = package_status(package)
        RESULT["packages"][package] = state
        if package != PACKAGES[2] and state["status"] == "PACKAGE_RECORD_PRESENT":
            RESULT.update(reason="PREEXISTING_PACKAGE")
            return
    preview = RESULT["packages"][PACKAGES[2]]
    if preview["status"] == "PACKAGE_RECORD_PRESENT":
        cmd = ("set -o pipefail; dumpsys package " + PACKAGES[2] +
               " | sed -n '/^Packages:/,/^Queries:/p' | grep -E '^Packages:|^  Package |^    appId=|^    versionCode=|^    versionName='")
        metadata = shell("preview-package-metadata", cmd)
        assert re.search(r"\bversionCode=28\b", metadata)
        preview["metadata"] = metadata
    # One follow-up observation, not a loop and not a daemon/device reconnect.
    time.sleep(15)
    pid2 = shell("systemui-followup", "pidof com.android.systemui", allowed=(0, 1))
    boot2 = shell("boot-id-followup", "cat /proc/sys/kernel/random/boot_id", secret=True)
    RESULT["systemUiFollowupPids"] = pid2.split()
    RESULT["systemUiPidStable"] = pid1 == pid2
    RESULT["bootStable"] = boot == boot2
    RESULT.update(reason="SYSTEMUI_ANR_SAFETY_FACT_UNKNOWN", outcome="BLOCKED_UNKNOWN")
    # Stable PIDs are intentionally not promoted to no-ANR or installation permission.


try:
    main()
except ConnectionRefusedError:
    RESULT.update(outcome="ENVIRONMENT_BLOCKED", reason="EXISTING_ADB_NOT_REACHABLE")
except Exception as exc:
    RESULT.update(outcome="BLOCKED_UNKNOWN", reason="STOPPED_" + type(exc).__name__)
finally:
    for record in RECORDS:
        if record["status"] == "STARTED":
            record["status"] = "INCOMPLETE_EXIT_UNKNOWN"
    RESULT.update(finishedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                  shellQueries=sum("service" in row for row in RECORDS), hostQueries=sum("request" in row for row in RECORDS),
                  completeShellBytes=TOTAL, automaticRetries=0, followupObservations=int("systemUiFollowupPids" in RESULT))
    save()
    print(json.dumps({k: RESULT[k] for k in ("outcome", "reason", "hostQueries", "shellQueries", "elapsedSeconds")}, ensure_ascii=True))
