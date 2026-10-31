"""Bounded V1-A1-only execution through an existing ADB smart socket.

No serial is persisted. No retry, daemon control, force-stop, private-file read,
global dump, screenshot, setting change or implicit Instrumentation is available.
"""
from pathlib import Path
import datetime, hashlib, json, re, socket, struct, sys, time

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
SERIAL_SHA = "e4f0e1d2361b0ef560ff8ac99a067fb8f540594c827d28b6cd21a720e724af92"
APP = "com.cooper.lithic.v1acceptance.debug"
TEST = APP + ".test"
PREVIEW = "com.cooper.lithic.preview.debug"
CANDIDATES = {
    "app": ("Lithic-V1-A1-App-debug.apk", "626c9809b7566d56583d49e4dbb43abe90d44e151a5e9357d2ca90c62f35a97a"),
    "test": ("Lithic-V1-A1-AndroidTest.apk", "1fdfc6ae8b784b8041d8182089bd1bf7af00ccc0e5d1f047de9ad9358f7e1154"),
}
sha = lambda data: hashlib.sha256(data).hexdigest()


class Session:
    def __init__(self, action):
        self.action = action
        self.serial = None
        self.rows = []
        self.file = OUT / (action + ".json")
        with self.file.open("x", encoding="utf-8") as claim:
            claim.write("{}\n")
        self.data = {"action": action, "serialSha256": SERIAL_SHA,
                     "startedUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                     "status": "STARTED", "commands": self.rows,
                     "automaticRetries": 0, "otherDeviceQueries": 0, "user": 0}
        self.save()

    def save(self):
        self.file.write_text(json.dumps(self.data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    @staticmethod
    def receive(sock, length, deadline):
        data = b""
        while len(data) < length:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                raise TimeoutError("DEADLINE_NO_RETRY")
            sock.settimeout(remaining)
            part = sock.recv(length - len(data))
            if not part:
                raise RuntimeError("TRUNCATED_PROTOCOL_REPLY")
            data += part
        return data

    def request(self, sock, service, deadline):
        raw = service.encode("utf-8")
        sock.sendall(f"{len(raw):04x}".encode() + raw)
        if self.receive(sock, 4, deadline) != b"OKAY":
            raise RuntimeError("SMART_SOCKET_NOT_OKAY")

    def identify(self):
        deadline = time.monotonic() + 5
        with socket.create_connection(("127.0.0.1", 5037), timeout=5) as sock:
            self.request(sock, "host:devices-l", deadline)
            size = int(self.receive(sock, 4, deadline), 16)
            if size > 16384:
                raise ValueError("HOST_LIST_LIMIT")
            raw = self.receive(sock, size, deadline)
        entries = [line.split() for line in raw.decode("utf-8").splitlines() if line.strip()]
        matched = [entry for entry in entries if sha(entry[0].encode()) == SERIAL_SHA]
        self.data["hostEnumeration"] = {"service": "host:devices-l", "protocolStatus": "OKAY",
            "rawBytes": len(raw), "rawSha256": sha(raw), "rawListSaved": False,
            "entryCount": len(entries), "authorizedMatches": len(matched)}
        if len(matched) != 1 or len(matched[0]) < 2 or matched[0][1] != "device":
            raise RuntimeError("ENVIRONMENT_BLOCKED_TARGET_NOT_UNIQUE_CONNECTED")
        self.serial = matched[0][0]
        if self.serial.startswith("emulator-") or ":" in self.serial:
            raise RuntimeError("ENVIRONMENT_BLOCKED_NOT_AUTHORIZED_USB")
        self.save()

    def shell(self, label, command, allowed=(0,), stdin=None, timeout=15):
        assert self.serial is not None
        for forbidden in ("force-stop", "pm clear", "uninstall", "logcat", "screencap", "reboot", "remount", "jobscheduler", "/data/user", "/data/data"):
            assert forbidden not in command
        row = {"label": label, "transportSelector": "host:transport:<USB_SERIAL_REDACTED>",
               "service": "shell,v2,raw:" + command, "status": "STARTED", "exitCode": None,
               "timeoutSeconds": timeout, "stdinBytes": len(stdin) if stdin else 0}
        if stdin:
            row["stdinSha256"] = sha(stdin)
        self.rows.append(row)
        self.save()
        stdout, stderr, exit_code = b"", b"", None
        deadline = time.monotonic() + timeout
        try:
            with socket.create_connection(("127.0.0.1", 5037), timeout=5) as sock:
                self.request(sock, "host:transport:" + self.serial, deadline)
                self.request(sock, "shell,v2,raw:" + command, deadline)
                if stdin:
                    for offset in range(0, len(stdin), 65536):
                        remaining = deadline - time.monotonic()
                        if remaining <= 0:
                            raise TimeoutError("INSTALL_SEND_DEADLINE_NO_RETRY")
                        sock.settimeout(remaining)
                        chunk = stdin[offset:offset+65536]
                        sock.sendall(struct.pack("<BI", 0, len(chunk)) + chunk)
                    sock.sendall(struct.pack("<BI", 4, 0))
                    row["stdinCompletelySent"] = True
                    self.save()
                while exit_code is None:
                    channel, size = struct.unpack("<BI", self.receive(sock, 5, deadline))
                    if size > 32768:
                        raise ValueError("OUTPUT_FRAME_LIMIT")
                    chunk = self.receive(sock, size, deadline)
                    if channel == 1:
                        stdout += chunk
                    elif channel == 2:
                        stderr += chunk
                    elif channel == 3 and size in (1, 4):
                        exit_code = int.from_bytes(chunk, "little")
                    else:
                        raise RuntimeError("UNEXPECTED_SHELL_CHANNEL")
                    if len(stdout) + len(stderr) > 65536:
                        raise ValueError("OUTPUT_LIMIT")
            row["status"] = "COMPLETED"
        finally:
            # All commands are exact-package/identity-only; redact any repeated serial.
            row.update(exitCode=exit_code, exitPacketReceived=exit_code is not None,
                       stdoutBytes=len(stdout), stdoutSha256=sha(stdout), stderrBytes=len(stderr), stderrSha256=sha(stderr),
                       stdout=stdout.decode("utf-8", errors="replace").replace(self.serial, "<USB_SERIAL_REDACTED>"),
                       stderr=stderr.decode("utf-8", errors="replace").replace(self.serial, "<USB_SERIAL_REDACTED>"))
            if row["status"] == "STARTED":
                row["status"] = "INCOMPLETE_EXIT_UNKNOWN"
            self.save()
        if exit_code not in allowed or stderr:
            raise RuntimeError("COMMAND_FAILURE_NO_RETRY")
        return stdout.decode("utf-8", errors="strict").strip()

    def package(self, package):
        assert package in (APP, TEST, PREVIEW)
        pattern = "^package:" + re.escape(package) + "([[:space:]]|$)"
        command = (f"rows=$(cmd package list packages -u -U --show-versioncode --user 0 {package} 2>&1); rc=$?; "
                   "printf 'PM_EXIT=%s\\n' \"$rc\"; "
                   f"printf '%s\\n' \"$rows\" | grep -E '{pattern}'; match=$?; "
                   "[ \"$match\" -le 1 ] || exit \"$match\"; exit \"$rc\"")
        value = self.shell("package-record-" + package, command)
        lines = value.splitlines()
        assert lines[0] == "PM_EXIT=0" and len(lines) in (1, 2)
        if len(lines) == 1:
            return {"record": "ABSENT_IN_USER0_INCLUDING_RETAINED", "package": package}
        assert lines[1].startswith("package:" + package + " ")
        uid = re.search(r"\buid:(\d+)\b", lines[1])
        version = re.search(r"\bversionCode:(\d+)\b", lines[1])
        assert uid and version
        # Only necessary own-package fields cross the transport; the dump stays in device RAM.
        regex = re.escape(package)
        awk = r'''BEGIN {inside=0;t=0;p=0;u=0;f=0;bad=0;v="";name=""}
/DUMP TIMEOUT|Error dumping|Permission Denial/ {bad++;next}
$0=="Packages:" {inside=1;t=0;next}
inside && /^[^[:space:]]/ {inside=0;t=0}
inside && /^[[:space:]]*Package \[/ {t=0;if($0 ~ /^  Package \[PKGREGEX\] \([^)]*\):[[:space:]]*$/){p++;t=1};next}
inside && t && /^    versionName=/ {name=substr($0,index($0,"=")+1)}
inside && t && /^    User 0:/ {u++;for(i=1;i<=NF;i++){if($i ~ /^installed=/){f++;if($i=="installed=true")v="true";else if($i=="installed=false")v="false";else bad++}}}
END {printf "PACKAGE_MATCHES=%d\nUSER0_MATCHES=%d\nINSTALLED_FIELDS=%d\nINVALID_FIELDS=%d\n",p,u,f,bad;if(p==1&&u==1&&f==1&&bad==0&&v!=""){print "PACKAGE=PKGNAME";print "USER=0";print "INSTALLED="v;print "VERSION_NAME="name}else exit 65}'''
        awk = awk.replace("PKGREGEX", regex).replace("PKGNAME", package)
        command = (f"dump=$(dumpsys package {package} 2>&1); source_rc=$?; "
                   "printf '%s\\n' \"$dump\" | awk '" + awk + "'; filter_rc=$?; "
                   "printf 'SOURCE_EXIT=%s\\nFILTER_EXIT=%s\\n' \"$source_rc\" \"$filter_rc\"; "
                   "if [ \"$source_rc\" -ne 0 ]; then exit \"$source_rc\"; fi; exit \"$filter_rc\"")
        fields = dict(line.split("=", 1) for line in self.shell("user0-" + package, command).splitlines())
        assert fields["PACKAGE"] == package and fields["USER"] == "0"
        assert all(fields[key] == "1" for key in ("PACKAGE_MATCHES", "USER0_MATCHES", "INSTALLED_FIELDS"))
        assert all(fields[key] == "0" for key in ("SOURCE_EXIT", "FILTER_EXIT", "INVALID_FIELDS"))
        assert fields["INSTALLED"] in ("true", "false")
        return {"record": "PRESENT", "package": package, "uid": int(uid[1]), "versionCode": int(version[1]),
                "versionName": fields["VERSION_NAME"], "user": 0, "installed": fields["INSTALLED"] == "true"}


def prior(action):
    value = json.loads((OUT / (action + ".json")).read_text(encoding="utf-8"))
    assert value["status"] == "PASS", "PRIOR_GATE_NOT_PASS"
    return value


def preview_ok(value):
    assert value["record"] == "PRESENT" and value["installed"] is True
    assert value["uid"] == 10429 and value["versionCode"] == 28 and value["versionName"] == "0.1.0-preview.12-debug"


def main():
    action = sys.argv[1]
    assert action in ("preflight", "install-app", "verify-app", "install-test", "verify-test", "launch", "final")
    session = Session(action)
    try:
        session.identify()
        if action == "preflight":
            preparation = json.loads((OUT / "preparation.json").read_text(encoding="utf-8"))
            assert preparation["manualHealth"]["source"] == "USER_RESPONSE"
            assert preparation["manualHealth"]["statement"] == "已检查，均正常，手机已连接并解锁"
            info = session.shell("phone-identity", "printf 'manufacturer='; getprop ro.product.manufacturer; printf 'model='; getprop ro.product.model; printf 'api='; getprop ro.build.version.sdk")
            fields = dict(line.split("=", 1) for line in info.splitlines())
            assert fields == {"manufacturer":"Xiaomi", "model":"23127PN0CC", "api":"36"}
            session.data["deviceIdentity"] = fields
            packages = {}
            for package in (APP, TEST, PREVIEW):
                packages[package] = session.package(package)
                session.data["packages"] = packages
                session.save()
                if package in (APP, TEST):
                    assert packages[package]["record"] == "ABSENT_IN_USER0_INCLUDING_RETAINED", "PREEXISTING_PACKAGE_STOP"
                else:
                    preview_ok(packages[package])
            session.data["manualHealth"] = "USER_CONFIRMED_DESKTOP_NOTIFICATION_TOUCH_NORMAL_NO_ANR_DIALOG"
        elif action.startswith("install-"):
            kind = action.split("-", 1)[1]
            prior("preflight" if kind == "app" else "verify-app")
            filename, expected = CANDIDATES[kind]
            blob = (ROOT / "verification-candidates/v1-a1" / filename).read_bytes()
            assert sha(blob) == expected
            session.data["candidate"] = {"file":filename,"bytes":len(blob),"sha256":expected}
            command = "cmd package install --user 0 " + ("-t " if kind == "test" else "") + "-S " + str(len(blob))
            result = session.shell("install-" + kind + "-once", command, stdin=blob, timeout=90)
            assert result == "Success", "INSTALL_NOT_SUCCESS_NO_RETRY"
            session.data["installResult"] = result
        elif action in ("verify-app", "verify-test", "final"):
            prior("install-app" if action=="verify-app" else "install-test")
            packages = {APP:session.package(APP), PREVIEW:session.package(PREVIEW)}
            if action != "verify-app":
                packages[TEST] = session.package(TEST)
                target = session.shell("instrumentation-registration", "cmd package list instrumentation " + APP)
                assert target == "instrumentation:" + TEST + "/com.cooper.lithic.app.M13IsolatedRunner (target=" + APP + ")"
                session.data["instrumentationRegistration"] = target
            preview_ok(packages[PREVIEW])
            assert packages[APP]["installed"] and packages[APP]["versionCode"]==1 and packages[APP]["versionName"]=="0.1.0-v1-a1-debug"
            assert packages[APP]["uid"] != packages[PREVIEW]["uid"] and packages[APP]["uid"] < 100000
            if TEST in packages:
                assert packages[TEST]["installed"] and len({v["uid"] for v in packages.values()})==3
            session.data["packages"] = packages
            for package in (APP, TEST):
                pids = session.shell("exact-process-"+package, "pidof " + package, allowed=(0,1))
                assert not pids or all(pid.isdecimal() for pid in pids.split())
                session.data.setdefault("targetPids",{})[package] = pids.split()
        elif action == "launch":
            prior("verify-test")
            result = session.shell("normal-main-launch-once", "am start --user 0 -W -n " + APP + "/com.cooper.lithic.app.MainActivity", timeout=30)
            assert "Status: ok" in result and "Error:" not in result
            assert "Activity: " + APP + "/com.cooper.lithic.app.MainActivity" in result
            session.data["normalMainLaunch"] = "ACTIVITY_MANAGER_STATUS_OK_NOT_BUSINESS_OR_VISUAL_PASS"
        session.data["status"] = "PASS"
    except Exception as exc:
        session.data.update(status="BLOCKED", reason=type(exc).__name__ + ":" + str(exc).replace(session.serial or "<NONE>", "<USB_SERIAL_REDACTED>"))
    finally:
        session.data["finishedUtc"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
        session.save()
        print(json.dumps({k:v for k,v in session.data.items() if k not in ("commands",)},ensure_ascii=True))


if __name__ == "__main__":
    main()
