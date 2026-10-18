#!/usr/bin/env python3
"""One-operation isolated Android supervisor. No retries, cleanup, or test traversal.

Only the root executor may run this file. The test subcommand deliberately stops:
the root executor must inspect actual lifecycle evidence before authorizing a next
method. All subprocess bytes are kept once, in the ignored JSONL command journal.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import ctypes
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[3]
BASE = Path("E:/CODEX/.codex/research-env/lithic-m10-android-20261018-v1")
TOOLS = Path("C:/Users/CDD/AppData/Local/MirraAndroid/sdk")
JDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1")
APP = "com.cooper.lithic.debug"
APP_SHA = "0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf"
TEST = "com.cooper.lithic.debug.test"
TEST_SHA = "3b0005a31afa753e9a9709323572250c0c5c63153c68ca1f882755c98724282a"
INSTRUMENTATION = TEST + "/com.cooper.lithic.data.media.M10ProcessBoundaryRunner"
CERT = "29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9"
CONFIG = {
    26: ("Lithic_M10V_API26_20261018", 5624, Path("E:/CODEX/.codex/research-env/lithic-a2r-api26/sdk")),
    36: ("Lithic_M10V_API36_20261018", 5626, TOOLS),
}
CREATION_NO_WINDOW = 0x08000000 if os.name == "nt" else 0


def utc():
    return datetime.now(timezone.utc).isoformat()


def need(condition, message):
    if not condition:
        raise RuntimeError(message)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def immutable(path, value):
    """Exclusive file + fsync/readback; never removes or resets an operation marker."""
    path.parent.mkdir(parents=True, exist_ok=True)
    data = (json.dumps(value, sort_keys=True, indent=2) + "\n").encode()
    with path.open("xb") as stream:
        stream.write(data)
        stream.flush()
        os.fsync(stream.fileno())
    need(path.read_bytes() == data, "Persistent marker readback failed")


def properties(path):
    values = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        if raw.strip() and not raw.lstrip().startswith(("#", ";")) and "=" in raw:
            key, value = raw.split("=", 1)
            need(key.strip() not in values, "Duplicate configuration property")
            values[key.strip()] = value.strip()
    return values


def within(path, parent):
    try:
        path.resolve().relative_to(parent.resolve())
        return True
    except ValueError:
        return False


def unlinked_ancestors(path):
    for part in (path, *path.parents):
        if not part.exists():
            continue
        stat = part.lstat()
        need(not part.is_symlink() and not (getattr(stat, "st_file_attributes", 0) & 0x400),
             "Linked/reparse ancestor is forbidden: " + str(part))


def windows_arguments(command):
    count = ctypes.c_int()
    parse = ctypes.windll.shell32.CommandLineToArgvW
    parse.argtypes = [ctypes.c_wchar_p, ctypes.POINTER(ctypes.c_int)]
    parse.restype = ctypes.POINTER(ctypes.c_wchar_p)
    pointer = parse(command, ctypes.byref(count))
    need(bool(pointer), "Windows process arguments unknown")
    try:
        return [pointer[index] for index in range(count.value)]
    finally:
        ctypes.windll.kernel32.LocalFree.argtypes = [ctypes.c_void_p]
        ctypes.windll.kernel32.LocalFree(pointer)


class Supervisor:
    def __init__(self, api):
        self.api = api
        self.name, self.port, self.sdk = CONFIG[api]
        self.serial = f"emulator-{self.port}"
        self.home = BASE / f"api{api}"
        self.data = self.home / "data" / f"{self.name}.avd"
        self.registry = self.home / "avds"
        self.evidence = ROOT / "build/android-validation/evidence" / f"api{api}"
        self.evidence.mkdir(parents=True, exist_ok=True)
        self.journal = self.evidence / "commands.jsonl"
        self.env = self.environment()

    def environment(self):
        env = os.environ.copy()
        forbidden = ("ADB_SERVER_SOCKET", "ADB_SERVER_PORT", "ANDROID_ADB_SERVER_ADDRESS",
                     "ANDROID_ADB_SERVER_PORT", "ADB_VENDOR_KEYS", "ANDROID_VENDOR_KEYS",
                     "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "JAVA_OPTS", "GRADLE_OPTS",
                     "SDKMANAGER_OPTS", "AVDMANAGER_OPTS")
        need(not any(env.get(key) for key in forbidden), "Unexpected ADB/JVM override; do not inherit it")
        for key in list(env):
            if key.startswith("ANDROID_") or key in ("ADB_TRACE", "ADB_LOCAL_TRANSPORT_MAX_PORT"):
                env.pop(key, None)
        env.update({"JAVA_HOME": str(JDK), "ANDROID_HOME": str(self.sdk),
                    "ANDROID_SDK_ROOT": str(self.sdk), "ANDROID_USER_HOME": str(self.home / "android-user"),
                    "ANDROID_AVD_HOME": str(self.registry), "ANDROID_EMULATOR_HOME": str(self.home / "emulator-home"),
                    "ANDROID_SERIAL": self.serial})
        need("ANDROID_SDK_HOME" not in env, "Deprecated SDK home must be absent")
        return env

    def note(self, value):
        with self.journal.open("ab") as stream:
            stream.write((json.dumps(value, sort_keys=True) + "\n").encode())
            stream.flush()
            os.fsync(stream.fileno())

    def run(self, args, timeout=30, input_bytes=None, allowed=(0,), cwd=None):
        begin = utc()
        try:
            completed = subprocess.run([str(x) for x in args], input=input_bytes, capture_output=True,
                                       cwd=cwd, env=self.env, timeout=timeout, creationflags=CREATION_NO_WINDOW)
            code, stdout, stderr = completed.returncode, completed.stdout, completed.stderr
        except subprocess.TimeoutExpired as error:
            # Never retry, cancel an Android task, kill an emulator, or claim this is a rejection.
            self.note({"utc": begin, "endUtc": utc(), "argv": [str(x) for x in args], "cwd": str(cwd) if cwd else None,
                       "status": "COMMAND_TIMEOUT_UNKNOWN", "timeoutSeconds": timeout,
                       "stdoutBase64": base64.b64encode(error.stdout or b"").decode(),
                       "stderrBase64": base64.b64encode(error.stderr or b"").decode()})
            raise RuntimeError("Command supervision timeout: preserve device state") from error
        self.note({"utc": begin, "endUtc": utc(), "argv": [str(x) for x in args], "cwd": str(cwd) if cwd else None, "exitCode": code,
                   "stdoutBytes": len(stdout), "stdoutSHA256": digest(stdout),
                   "stderrBytes": len(stderr), "stderrSHA256": digest(stderr),
                   "stdoutBase64": base64.b64encode(stdout).decode(),
                   "stderrBase64": base64.b64encode(stderr).decode()})
        need(code in allowed, f"Command failed with exit {code}; no retry")
        return stdout.decode("utf-8", errors="strict").strip()

    def marker(self, operation):
        need(self.home.is_dir(), "Isolated environment has not been created")
        immutable(self.home / "operations" / f"{operation}.json",
                  {"utc": utc(), "operation": operation, "api": self.api, "avd": self.name,
                   "serial": self.serial, "data": str(self.data), "status": "ATTEMPT_ISSUED"})

    def audit(self):
        report = json.loads((ROOT / "build/android-validation/environment-audit.json").read_text(encoding="utf-8"))
        need(report.get("sourceCommit") == "b756f96b699f3b70d43144f6c92815917fb563f6" and
             report.get("api26", {}).get("allOriginalFilesSizeAndShaMatch") is True and
             report.get("api36", {}).get("generatedPackageXml", {}).get("identityParsed") is True and
             report.get("tools", {}).get("primaryEmulatorAuthenticode", {}).get("status") == "Valid",
             "Required measured environment checks are missing")
        unlinked_ancestors(self.home)
        unlinked_ancestors(self.sdk)
        need(JDK.is_dir() and self.sdk.is_dir(), "Existing JDK/SDK unavailable")
        image = self.sdk / f"system-images/android-{self.api}/google_apis/x86_64"
        found = properties(image / "source.properties")
        need(found.get("AndroidVersion.ApiLevel") == str(self.api), "Wrong API image")
        need(found.get("SystemImage.Abi") == "x86_64" and found.get("SystemImage.TagId") == "google_apis", "Wrong image identity")
        if self.api == 26:
            need(found.get("Pkg.Revision") == "16", "API26 must remain official locked r16")
        need((image / "system.img").is_file(), "Existing system image missing")
        return image

    def verify_avd(self):
        image = self.audit()
        for path in (self.data, self.registry, self.registry / f"{self.name}.ini", self.data / "config.ini"):
            unlinked_ancestors(path)
        ini = properties(self.registry / f"{self.name}.ini")
        need(Path(ini.get("path", "")).resolve() == self.data.resolve(), "AVD registry/data mismatch")
        cfg = properties(self.data / "config.ini")
        need(cfg.get("AvdId") == self.name and cfg.get("abi.type") == "x86_64", "AVD identity mismatch")
        image_path = Path(cfg.get("image.sysdir.1", "").replace("\\", "/"))
        if not image_path.is_absolute():
            image_path = self.sdk / image_path
        need(image_path.resolve() == image.resolve(), "AVD uses an unapproved image")
        for key in ("disk.dataPartition.path", "disk.cachePartition.path", "sdcard.path"):
            value = cfg.get(key)
            if value:
                candidate = Path(value)
                need(within(candidate if candidate.is_absolute() else self.data / candidate, self.home), "AVD file escapes isolation")
        need(within(self.data, self.home) and within(self.registry, self.home), "AVD isolation failed")
        return cfg

    def processes(self, timeout=30):
        # Full system process rows stay in PowerShell memory. Only matching new-AVD rows leave it.
        script = "$p=@(Get-CimInstance Win32_Process); $m=@($p | Where-Object {$_.Name -match '^(emulator|qemu-system).*\\.exe$'}); "
        script += f"$t=@($m | Where-Object {{$_.CommandLine -like '*{self.name}*'}}); "
        script += "$o=Get-CimInstance Win32_OperatingSystem; [pscustomobject]@{emulatorProcessCount=$m.Count; freeKiB=$o.FreePhysicalMemory; totalKiB=$o.TotalVisibleMemorySize; targets=@($t | Select-Object ProcessId,ParentProcessId,Name,ExecutablePath,CommandLine)} | ConvertTo-Json -Depth 4 -Compress"
        found = json.loads(self.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script], timeout=timeout))
        return found

    def validate_process(self, timeout=30):
        found = self.processes(timeout=timeout)
        targets = found.get("targets", [])
        need(bool(targets), "No independently identified new-AVD process")
        for target in targets:
            command = target.get("CommandLine") or ""
            tokens = windows_arguments(command)
            for key, expected in (("-avd", self.name), ("-port", str(self.port)), ("-datadir", str(self.data))):
                need(tokens.count(key) == 1, "Missing/duplicate process argument: " + key)
                index = tokens.index(key) + 1
                need(index < len(tokens), "Truncated process argument")
                if key == "-datadir":
                    need(Path(tokens[index]).resolve() == self.data.resolve(), "Process data directory ambiguous")
                else:
                    need(tokens[index] == expected, "Process argument conflicts: " + key)
            need(within(Path(target.get("ExecutablePath") or ""), self.sdk / "emulator"), "Unexpected emulator executable")
        return found

    def adb(self, *args, timeout=30, allowed=(0,), cwd=None):
        need(self.serial in ("emulator-5624", "emulator-5626"), "Unauthorized serial")
        return self.run([TOOLS / "platform-tools/adb.exe", "-s", self.serial, *args], timeout=timeout, allowed=allowed, cwd=cwd)

    def identity(self, deadline=None):
        def remaining():
            if deadline is None:
                return 30
            value = deadline - time.monotonic()
            need(value > 0, "Fixed boot deadline exhausted during identity proof")
            return min(30, value)
        self.verify_avd()
        self.validate_process(timeout=remaining())  # Mandatory before any targeted ADB request.
        need(self.adb("get-state", timeout=remaining()) == "device", "New target is not online")
        need(self.adb("shell", "getprop", "sys.boot_completed", timeout=remaining()) == "1", "System boot is not complete")
        need(self.adb("shell", "getprop", "ro.build.version.sdk", timeout=remaining()) == str(self.api), "Actual API mismatch")
        need(self.adb("shell", "getprop", "ro.product.cpu.abi", timeout=remaining()) == "x86_64", "Actual ABI mismatch")
        name = self.adb("emu", "avd", "name", timeout=remaining()).splitlines()
        need(name and name[0] == self.name and all(x == "OK" for x in name[1:]), "Actual AVD name mismatch")

    def create(self):
        self.audit()
        need(not self.home.exists(), "Do not reuse/overwrite an existing environment")
        before = self.processes()
        need(not before.get("targets"), "AVD name already has a process")
        self.home.mkdir(parents=True, exist_ok=False)
        self.marker("create")
        for path in (self.registry, self.home / "android-user", self.home / "emulator-home", self.data.parent):
            path.mkdir(parents=True, exist_ok=False)
        managers = list((self.sdk / "cmdline-tools").glob("*/bin/avdmanager.bat"))
        need(len(managers) == 1, "avdmanager version is ambiguous")
        self.run([managers[0], "create", "avd", "-n", self.name, "-k",
                  f"system-images;android-{self.api};google_apis;x86_64", "-p", self.data],
                 timeout=120, input_bytes=b"no\n")
        self.verify_avd()
        immutable(self.home / "create-proof.json", {"status": "PASS", "utc": utc(), "api": self.api,
                  "avd": self.name, "data": str(self.data), "registry": str(self.registry)})

    def start(self):
        self.verify_avd()
        need(json.loads((self.home / "create-proof.json").read_text())["status"] == "PASS", "Creation proof missing")
        observed = self.processes()
        need(not observed.get("targets"), "Do not launch a second instance")
        # Host admission estimate only, never a JPEG native/RSS cap. No process is killed to make room.
        need(int(observed.get("freeKiB", 0)) >= (1536 + 1024) * 1024, "Insufficient free host memory for guest plus headroom")
        for other in CONFIG:
            other_home = BASE / f"api{other}"
            if other != self.api:
                need(not (other_home / "operations/start.json").exists() or
                     (other_home / "boot-proof.json").is_file(), "Other isolated boot remains unproved; no parallel start")
        import socket
        for port in (self.port, self.port + 1):
            with socket.socket() as sock:
                sock.bind(("127.0.0.1", port))
        self.marker("start")
        args = [str(self.sdk / "emulator/emulator.exe"), "-avd", self.name, "-port", str(self.port),
                "-datadir", str(self.data), "-sysdir", str(self.audit()), "-no-snapshot", "-no-window", "-no-audio", "-no-boot-anim",
                "-memory", "1536", "-cores", "2"]
        began = time.monotonic()
        with (self.evidence / "emulator.log").open("xb") as log:
            child = subprocess.Popen(args, env=self.env, stdout=log, stderr=subprocess.STDOUT,
                                     creationflags=CREATION_NO_WINDOW)
        self.note({"utc": utc(), "operation": "EMULATOR_START", "argv": args, "pid": child.pid})
        deadline = began + 300
        while time.monotonic() < deadline:
            need(child.poll() is None, "Emulator launcher exited; preserve state and do not retry")
            remaining = min(10, deadline - time.monotonic())
            need(remaining > 0, "Fixed boot deadline exhausted")
            self.validate_process(timeout=remaining)
            remaining = min(10, deadline - time.monotonic())
            need(remaining > 0, "Fixed boot deadline exhausted")
            state = self.adb("get-state", timeout=remaining, allowed=(0, 1))
            if state == "device":
                remaining = min(10, deadline - time.monotonic())
                need(remaining > 0, "Fixed boot deadline exhausted")
                boot = self.adb("shell", "getprop", "sys.boot_completed", timeout=remaining)
                if boot == "1":
                    self.identity(deadline=deadline)
                    need(time.monotonic() <= deadline, "Boot proof exceeded the single fixed deadline")
                    immutable(self.home / "boot-proof.json", {"status": "PASS", "utc": utc(), "serial": self.serial,
                              "avd": self.name, "api": self.api, "launcherPid": child.pid})
                    return
            time.sleep(min(2, max(0, deadline - time.monotonic())))
        raise RuntimeError("Boot deadline exhausted; no retry or cleanup")

    def apk_audit(self):
        value = json.loads((ROOT / "build/android-validation/apk-audit.json").read_text(encoding="utf-8"))
        need(value.get("status") == "PASS" and value.get("applicationId") == APP and value.get("certificateSHA256") == CERT, "APK audit is not the locked PASS")
        test = value.get("testApplicationId")
        need(test == TEST and value.get("instrumentationComponent") == INSTRUMENTATION, "Unknown instrumentation identity")
        rows = value.get("apks", [])
        need(len(rows) == 2 and {row.get("kind") for row in rows} == {"app", "test"}, "APK audit shape")
        for row in rows:
            path = Path(row["path"])
            need(path.is_absolute() and path.is_file(), "Locked APK unavailable")
            need(row.get("certificateSHA256") == CERT and path.stat().st_size == row.get("bytes"), "APK identity mismatch")
            need(digest(path.read_bytes()) == row.get("sha256"), "APK SHA mismatch")
            need(row.get("packageName") == (APP if row["kind"] == "app" else test), "APK package mismatch")
            if row["kind"] == "app":
                need(row.get("sha256") == APP_SHA, "App APK differs from the locked source")
            else:
                need(row.get("sha256") == TEST_SHA, "Test APK differs from the locked source")
        return value

    def installed(self, package, expected_sha):
        paths = self.adb("shell", "pm", "path", package).splitlines()
        need(len(paths) == 1 and paths[0].startswith("package:/data/app/"), "Installed APK path is ambiguous")
        path = paths[0].removeprefix("package:")
        need(re.fullmatch(r"[A-Za-z0-9_./=+~\-]+", path), "Unsafe installed path")
        need(self.adb("shell", "sha256sum", path).split()[0] == expected_sha, "Installed bytes differ")
        package_info = self.adb("shell", "dumpsys", "package", package)
        uids = set(re.findall(r"\buserId=(\d+)\b", package_info))
        need(len(uids) == 1, "Installed UID unknown")
        return int(next(iter(uids)))

    def install(self, kind):
        self.identity()
        audit = self.apk_audit()
        row = next(item for item in audit["apks"] if item["kind"] == kind)
        self.marker(f"install-{kind}")
        absent = self.adb("shell", "pm", "list", "packages", row["packageName"])
        need(not absent, "Package present or absence unknown; no update/reinstall authorized")
        if kind == "test":
            self.installed(APP, APP_SHA)
        path = Path(row["path"])
        text = self.adb("install", path.name, cwd=path.parent, timeout=120)
        need("Success" in text.splitlines(), "Install result is not explicit success")
        self.identity()
        uid = self.installed(row["packageName"], row["sha256"])
        immutable(self.home / f"install-{kind}-proof.json", {"status": "PASS", "utc": utc(), "package": row["packageName"],
                  "sha256": row["sha256"], "uid": uid, "serial": self.serial})

    def close_readonly(self):
        self.marker("close-readonly")
        self.verify_avd()
        self.validate_process()
        state = self.adb("get-state", allowed=(0, 1))
        if state == "device":
            self.adb("emu", "avd", "name")
            self.adb("shell", "getprop", "ro.build.version.sdk")
            self.adb("shell", "getprop", "sys.boot_completed")
            if (self.home / "install-app-proof.json").exists():
                self.adb("shell", "pidof", APP, allowed=(0, 1))
                self.adb("shell", "pidof", APP + ":jpeg_worker", allowed=(0, 1))
                self.adb("shell", "dumpsys", "activity", "services", APP)
        # No file content/ledger/image access, cleanup, unbind, or process mutation.
        immutable(self.home / "close-readonly-proof.json", {"status": "OBSERVED", "utc": utc(), "serial": self.serial})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("api", type=int, choices=CONFIG)
    parser.add_argument("operation", choices=("create", "start", "install", "test", "close-readonly"))
    parser.add_argument("--kind", choices=("app", "test"))
    args = parser.parse_args()
    if args.operation == "test":
        raise RuntimeError("BLOCKED: lifecycle test execution/evidence validation is root-executor-only; no automatic test path")
    supervisor = Supervisor(args.api)
    if args.operation == "install":
        need(args.kind is not None, "Specify exactly one APK kind")
        supervisor.install(args.kind)
    else:
        need(args.kind is None, "APK kind is only valid for install")
        getattr(supervisor, args.operation.replace("-", "_"))()
    print(json.dumps({"status": "PASS", "operation": args.operation, "api": args.api,
                      "evidence": str(supervisor.evidence)}, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(json.dumps({"status": "BLOCKED", "errorType": type(error).__name__, "detail": str(error)}, sort_keys=True), file=sys.stderr)
        raise
