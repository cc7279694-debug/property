#!/usr/bin/env python3
"""One-operation isolated Android supervisor. No retries, cleanup, or test traversal.

Only the root executor may run this file. Each test operation executes one fixed
natural method and requires its full evidence before any next method. All command
bytes are kept once in the ignored JSONL journal. No retries or cleanup are offered.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import sys
import time
import ctypes
from datetime import datetime, timezone

import avd_identity
import runtime_identity
import installed_uid

ROOT = Path(__file__).resolve().parents[3]
BASE = Path("E:/CODEX/.codex/research-env/lithic-m10-android-20261018-v1")
TOOLS = Path("C:/Users/CDD/AppData/Local/MirraAndroid/sdk")
JDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1")
APP = "com.cooper.lithic.debug"
APP_SHA = "0ada263192dbaa1256bff6c613e6d57a69e1160b17d4844d1e1cfcc9cf847dbf"
TEST = "com.cooper.lithic.debug.test"
TEST_SHA = "73d96ec9aceb23919183074a0dd101b1b7460187e4cb1ff5d3ed8ab9f9b3d98f"
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


def verify_console_name(text, expected):
    """Only the observed console CRCRLF/CRLF transport framing is normalized."""
    need(isinstance(text, str) and len(text) <= 1024, "Console name output missing/oversized")
    normalized = text.replace("\r\r\n", "\n").replace("\r\n", "\n")
    need("\r" not in normalized and normalized.strip("\n").split("\n") == [expected, "OK"],
         "Actual AVD name or console completion conflicts")


def digest(data):
    return hashlib.sha256(data).hexdigest()


def file_digest(path, expected_size):
    avd_identity.unlinked_ancestors(path)
    before = path.lstat()
    need(stat.S_ISREG(before.st_mode) and before.st_size == expected_size,
         "File type/size does not match locked evidence")
    measured, count = hashlib.sha256(), 0
    with path.open("rb") as stream:
        while block := stream.read(1024 * 1024):
            measured.update(block)
            count += len(block)
            need(count <= expected_size, "File grew during integrity read")
    after = path.lstat()
    need((before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns) ==
         (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns) and count == expected_size,
         "File identity changed during integrity read")
    return measured.hexdigest()


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
    continuous_capture_required = True

    def __init__(self, api):
        self.api = api
        self.name, self.port, self.sdk = CONFIG[api]
        self.serial = f"emulator-{self.port}"
        self.home = BASE / f"api{api}"
        self.data = self.home / "data" / f"{self.name}.avd"
        self.registry = self.home / "avds"
        self.evidence = ROOT / "build/android-final-natural/evidence" / f"api{api}"
        self.evidence.mkdir(parents=True, exist_ok=True)
        self.journal = self.evidence / "commands.jsonl"
        self.env = self.environment()

    def new_log_capture(self, directory):
        from continuous_log import ContinuousLogCapture
        return ContinuousLogCapture(TOOLS / "platform-tools/adb.exe", self.serial,
                                    Path(directory) / "continuous-log", env=self.env)

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
                    "ANDROID_SERIAL": self.serial, "TEMP": str(self.home / "runtime-temp"),
                    "TMP": str(self.home / "runtime-temp")})
        need("ANDROID_SDK_HOME" not in env, "Deprecated SDK home must be absent")
        return env

    def note(self, value):
        with self.journal.open("ab") as stream:
            stream.write((json.dumps(value, sort_keys=True) + "\n").encode())
            stream.flush()
            os.fsync(stream.fileno())

    def run(self, args, timeout=30, input_bytes=None, allowed=(0,), cwd=None, child_env=None):
        begin = utc()
        selected_env = self.env if child_env is None else child_env
        need(set(selected_env) == set(self.env) and
             all(selected_env[key] == value for key, value in self.env.items() if key not in ("TEMP", "TMP")),
             "Only explicit host diagnostic TEMP/TMP overrides are permitted")
        environment_overrides = {key: selected_env[key] for key in ("TEMP", "TMP")
                                 if selected_env[key] != self.env[key]}
        try:
            completed = subprocess.run([str(x) for x in args], input=input_bytes, capture_output=True,
                                       cwd=cwd, env=selected_env, timeout=timeout, creationflags=CREATION_NO_WINDOW)
            code, stdout, stderr = completed.returncode, completed.stdout, completed.stderr
        except subprocess.TimeoutExpired as error:
            # Never retry, cancel an Android task, kill an emulator, or claim this is a rejection.
            self.note({"utc": begin, "endUtc": utc(), "argv": [str(x) for x in args], "cwd": str(cwd) if cwd else None,
                       "status": "COMMAND_TIMEOUT_UNKNOWN", "timeoutSeconds": timeout,
                       "environmentOverrides": environment_overrides,
                       "stdoutBase64": base64.b64encode(error.stdout or b"").decode(),
                       "stderrBase64": base64.b64encode(error.stderr or b"").decode()})
            raise RuntimeError("Command supervision timeout: preserve device state") from error
        self.note({"utc": begin, "endUtc": utc(), "argv": [str(x) for x in args], "cwd": str(cwd) if cwd else None, "exitCode": code,
                   "environmentOverrides": environment_overrides,
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
             report.get("observationSourceCommit") == "3474f639c2a831d26e74eeeff5ec8db0f6ea7a3f" and
             bool(report.get("freshUtc")) and
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
        else:
            need(found.get("Pkg.Revision") == "7", "API36 must remain official locked r7")
        need((image / "system.img").is_file(), "Existing system image missing")
        return image

    def image_metadata_lock(self):
        report = json.loads((ROOT / "build/android-validation/environment-audit.json").read_text(encoding="utf-8"))
        current = report[f"api{self.api}"]
        rows = current["files" if self.api == 26 else "currentImageFiles"]
        selected = [row for row in rows if row.get("name") == "source.properties"]
        need(len(selected) == 1, "Unique current image metadata digest missing")
        return {"source.properties": selected[0]["sha256"],
                "package.xml": current["generatedPackageXml"]["sha256"]}

    def image_file_lock(self, filename):
        report = json.loads((ROOT / "build/android-validation/environment-audit.json").read_text(encoding="utf-8"))
        rows = report[f"api{self.api}"]["files" if self.api == 26 else "currentImageFiles"]
        selected = [row for row in rows if row.get("name") == filename]
        need(len(selected) == 1 and bool(re.fullmatch(r"[0-9a-f]{64}", selected[0].get("sha256", ""))),
             "Locked official image file digest missing")
        return selected[0]

    def verify_avd(self):
        self.audit()
        self.config_observation = avd_identity.verify_configuration(
            self.registry, self.data, self.sdk, self.name, self.api,
            expected_image_digest=self.image_metadata_lock())
        return self.config_observation["configuration"]

    def vacant(self):
        observed = self.processes()
        need(isinstance(observed.get("targets"), list) and not observed["targets"],
             "Current target process absence is not proven")
        need(int(observed.get("freeKiB", 0)) >= (1536 + 1024) * 1024,
             "Insufficient free host memory for guest plus headroom")
        import socket
        for port in (self.port, self.port + 1):
            with socket.socket() as sock:
                sock.bind(("127.0.0.1", port))
        return observed

    def adopt(self):
        """Admit the preserved API26 CLI output, without rewriting old evidence."""
        need(self.api == 26, "Only the preserved API26 AVD may be adopted")
        self.verify_avd()
        source = ROOT / "docs/research/m10-jpeg-android-isolated/evidence"
        locked = (
            (source / "api26-generated-config.ini", self.data / "config.ini"),
            (source / "api26-generated-descriptor.ini", self.registry / (self.name + ".ini")),
            (source / "api26-create-marker.json", self.home / "operations/create.json"),
        )
        identities = []
        for original, current in locked:
            before, after = avd_identity.read_bytes(original), avd_identity.read_bytes(current)
            need(before == after, "Preserved official-create byte identity changed: " + current.name)
            identities.append({"name": current.name, "bytes": len(after), "sha256": digest(after)})
        journal = avd_identity.read_bytes(source / "api26-commands.jsonl")
        rows = [json.loads(row) for row in journal.decode("utf-8").splitlines() if row]
        creates = [row for row in rows if "create" in row.get("argv", []) and "avd" in row.get("argv", [])]
        need(len(creates) == 1 and creates[0].get("exitCode") == 0, "Historical unique official create exit0 missing")
        argv = creates[0]["argv"]
        need(argv.count("-n") == 1 and argv[argv.index("-n") + 1] == self.name and
             argv.count("-p") == 1 and Path(argv[argv.index("-p") + 1]).resolve() == self.data.resolve(),
             "Historical official create target conflicts")
        need(not (self.home / "operations/start.json").exists() and
             not (self.home / "create-proof.json").exists(), "Preserved API26 must not have been started or reclassified")
        closing_bytes = avd_identity.read_bytes(source / "api26-closing.json")
        closing = json.loads(closing_bytes.decode("utf-8"))
        files = closing.get("files", [])
        need(len(files) == 4 and len({row.get("path") for row in files}) == 4,
             "Original preserved file inventory is not the four locked files")
        expected = {row["path"]: row for row in files}
        allowed_dirs = {"avds", "data", "data/" + self.name + ".avd", "operations", "android-user", "emulator-home"}
        present = set()
        def configured_temp_observation():
            temporary = self.home / "runtime-temp"
            provenance = "CURRENT_CHILD_TEMP_CONFIGURED_OBSERVED_EMPTY_NOT_OLD_FILE"
            if not temporary.exists():
                return {"exists": False, "path": str(temporary), "provenance": provenance}
            avd_identity.unlinked_ancestors(temporary)
            before = temporary.lstat()
            need(stat.S_ISDIR(before.st_mode) and not any(temporary.iterdir()),
                 "Configured child temporary directory is not empty")
            after = temporary.lstat()
            need((before.st_dev, before.st_ino, before.st_mtime_ns) ==
                 (after.st_dev, after.st_ino, after.st_mtime_ns),
                 "Configured child temporary directory changed during observation")
            return {"exists": True, "path": str(temporary), "empty": True,
                    "device": after.st_dev, "inode": after.st_ino,
                    "createdNs": after.st_ctime_ns, "modifiedNs": after.st_mtime_ns,
                    "fileAttributes": getattr(after, "st_file_attributes", 0),
                    "provenance": provenance}
        def visit(directory):
            for entry in directory.iterdir():
                avd_identity.unlinked_ancestors(entry)
                relative = entry.relative_to(self.home).as_posix()
                kind = entry.lstat()
                if stat.S_ISDIR(kind.st_mode):
                    if relative == "runtime-temp":
                        configured_temp_observation()
                        continue
                    need(relative in allowed_dirs, "Unknown preserved directory: " + relative)
                    visit(entry)
                else:
                    need(relative in expected, "Unknown preserved file: " + relative)
                    row = expected[relative]
                    need(file_digest(entry, row["bytes"]) == row["sha256"],
                         "Preserved file integrity differs: " + relative)
                    present.add(relative)
        visit(self.home)
        need(present == set(expected), "Preserved file inventory is incomplete")
        vacant = self.vacant()
        # The read-only child process can create its configured TEMP directory.
        # Recheck after that command; no file or nested directory is admitted.
        configured_temp = configured_temp_observation()
        immutable(self.home / "adoption-proof.json", {
            "status": "PASS", "scope": "STATIC_ADOPTION_NOT_RUNTIME", "utc": utc(),
            "sourceCommit": "939e88b3fc79f3b9fd160737022f3bd795a07f37", "api": self.api,
            "avd": self.name, "serial": self.serial, "data": str(self.data), "registry": str(self.registry),
            "preservedIdentities": identities, "originalJournalSHA256": digest(journal),
            "originalClosingSHA256": digest(closing_bytes), "preservedFileInventory": files,
            "configuredChildTemporaryDirectory": configured_temp,
            "configuration": self.config_observation, "vacancyObservation": vacant,
            "environmentAuditSHA256": digest((ROOT / "build/android-validation/environment-audit.json").read_bytes())})

    def api26_exit_gate(self):
        if self.api != 36:
            return
        proof = json.loads(avd_identity.read_bytes(BASE / "api26/normal-exit-proof.json").decode("utf-8"))
        need(proof.get("status") == "PASS" and proof.get("api") == 26 and
             proof.get("serial") == "emulator-5624" and
             all(proof.get(key) is True for key in ("allNaturalMethodsPassed", "consumersSafe", "osProcessEnded")),
             "API26 safe natural validation and normal exit are not proven")

    def processes(self, timeout=30):
        # Full system process rows stay in PowerShell memory. Only matching new-AVD rows leave it.
        # CIM is a host diagnostic, not Android runtime: it must not create a new
        # AVD home merely because PowerShell initializes its TEMP directory.
        host_env = self.env.copy()
        for key in ("TEMP", "TMP"):
            original = os.environ.get(key, "")
            candidate = Path(original)
            need(bool(original) and candidate.is_absolute() and candidate.is_dir() and
                 not within(candidate, BASE), "Existing host diagnostic temporary directory is not safe")
            avd_identity.unlinked_ancestors(candidate)
            host_env[key] = str(candidate)
        historical = getattr(self, "historical_process_ids", "32880,32032" if self.api == 26 else "")
        script = "$p=@(Get-CimInstance Win32_Process); $m=@($p | Where-Object {$_.Name -match '^(emulator|qemu-system).*\\.exe$'}); "
        script += f"$ports=@(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object {{$_.LocalPort -in @({self.port},{self.port+1})}}); "
        script += f"$named=@($m | Where-Object {{$_.CommandLine -like '*{self.name}*' -or $_.CommandLine -like '*{self.data}*'}}); "
        script += f"$ids=@(@({historical}) + @($named.ProcessId) + @($ports.OwningProcess)); "
        script += "$t=@($p | Where-Object {$_.ProcessId -in $ids -or ($_.Name -match '^(emulator|qemu-system).*\\.exe$' -and $_.ParentProcessId -in $ids)}); "
        script += "$rows=@($t | Select-Object ProcessId,ParentProcessId,Name,ExecutablePath,CommandLine,@{Name='CreationDate';Expression={if ($_.CreationDate) {$_.CreationDate.ToUniversalTime().ToString('o')} else {$null}}}); "
        script += "$o=Get-CimInstance Win32_OperatingSystem; [pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');emulatorProcessCount=$m.Count;freeKiB=$o.FreePhysicalMemory;totalKiB=$o.TotalVisibleMemorySize;targets=$rows;ports=@($ports | Select-Object LocalAddress,LocalPort,OwningProcess)} | ConvertTo-Json -Depth 5 -Compress"
        found = json.loads(self.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script],
                                    timeout=timeout, child_env=host_env))
        return found

    def validate_process(self, timeout=30):
        anchor = self.resume_anchor() if self.api == 26 else getattr(self, "_runtime_anchor", None)
        if self.api == 36 and anchor is None and (self.home / "boot-proof.json").is_file():
            anchor = json.loads(avd_identity.read_bytes(self.home / "boot-proof.json"))["processIdentityProof"]["anchor"]
        proof = runtime_identity.await_complete(self.processes, anchor, self.process_spec(),
                    windows_arguments, timeout=min(30, timeout), clock=time.monotonic, sleep=time.sleep)
        self.note({"operation": "BOUNDED_READ_ONLY_CIM_IDENTITY", "proof": proof})
        need(proof.get("status") == "PASS", "Process identity gate: " + str(proof.get("reason", proof.get("status"))))
        self.process_proof = proof
        self._runtime_anchor = proof["anchor"]
        return proof["snapshot"]

    def launch_arguments(self):
        return [str(self.sdk / "emulator/emulator.exe"), "-avd", self.name, "-port", str(self.port),
                "-datadir", str(self.data), "-data", str(self.data / "userdata-qemu.img"),
                "-cache", str(self.data / "cache.img"), "-sysdir",
                str(self.sdk / f"system-images/android-{self.api}/google_apis/x86_64"),
                "-no-snapshot", "-no-window", "-no-audio", "-no-boot-anim", "-memory", "1536", "-cores", "2"]

    def resume_anchor(self):
        path = ROOT / "docs/research/m10-jpeg-android-isolated-resume/evidence/ending-windows.json"
        value = json.loads(avd_identity.read_bytes(path))
        need({row.get("ProcessId") for row in value.get("targets", [])} == {32880, 32032},
             "Historical original instance anchor unavailable")
        return value

    def process_spec(self):
        launcher = self.sdk / "emulator/emulator.exe"
        qemu = self.sdk / "emulator/qemu/windows-x86_64/qemu-system-x86_64-headless.exe"
        if not getattr(self, "_runtime_tools_verified", False):
            report = json.loads((ROOT / "build/android-validation/environment-audit.json").read_text(encoding="utf-8"))
            need(file_digest(launcher, launcher.stat().st_size) == report["tools"]["bothEmulatorExecutableSha256"],
                 "Launcher binary differs from measured locked SDK")
            rows = [row for row in report["tools"]["qemuCurrentBinaries"] if Path(row["path"]).resolve() == qemu.resolve()]
            need(len(rows) == 1 and rows[0]["authenticode"]["status"] == "Valid" and
                 file_digest(qemu, rows[0]["bytes"]) == rows[0]["sha256"], "QEMU binary identity unavailable")
            self._runtime_tools_verified = True
        return {"avd": self.name, "port": self.port, "launcherExe": str(launcher),
                "qemuExe": str(qemu), "argv": self.launch_arguments()}

    def runtime_paths(self):
        """Observe actual resolved QEMU paths; child TEMP alone is never proof."""
        cfg, integrity = avd_identity.properties(self.data / "hardware-qemu.ini")
        definitions = avd_identity.read_bytes(self.sdk / "emulator/lib/hardware-properties.ini", limit=512 * 1024)
        official_names = set(re.findall(r"^name\s*=\s*(\S+)\s*$", definitions.decode("utf-8"), re.M))
        need(cfg.get("avd.name") == self.name and cfg.get("avd.id") == self.name,
             "Runtime AVD identity has not resolved to the isolated registration")
        expected = {"disk.dataPartition.path": self.data / "userdata-qemu.img",
                    "disk.cachePartition.path": self.data / "cache.img"}
        readonly = {
            "kernel.path": {"kernel-ranchu", "kernel-ranchu-64"},
            "disk.ramdisk.path": {"ramdisk.img"},
            "disk.dataPartition.initPath": {"userdata.img"},
            "disk.systemPartition.initPath": {"system.img"},
            "disk.vendorPartition.initPath": {"vendor.img"},
        }
        writable = {"disk.dataPartition.path", "disk.cachePartition.path", "hw.sdCard.path",
                    "hw.logcatOutput.path",
                    "disk.systemPartition.path", "disk.vendorPartition.path",
                    "disk.encryptionKeyPartition.path", "disk.snapStorage.path"}
        image = self.sdk / f"system-images/android-{self.api}/google_apis/x86_64"
        observed = {}
        for key, value in cfg.items():
            if not value or not (key.endswith(".path") or key.endswith(".initPath")):
                continue
            need(key in official_names, "Runtime path field is absent from installed official schema: " + key)
            need(key in writable or key in readonly, "Unknown runtime path field: " + key)
            path = avd_identity.path_value(value, self.data)
            if key in readonly:
                found = [filename for filename in readonly[key] if path.resolve() in
                         {(image / filename).resolve(), (self.data / filename).resolve()}]
                need(len(found) == 1, "Runtime read-only image path conflicts: " + key)
                lock = self.image_file_lock(found[0])
                if within(path, self.data):
                    need(file_digest(path, lock["bytes"]) == lock["sha256"],
                         "AVD template copy differs from locked official image: " + key)
            else:
                need(within(path, self.data) or
                     (key == "hw.logcatOutput.path" and within(path, self.home / "runtime-temp")),
                     "Unconfined runtime writable path: " + key)
            if key in expected:
                need(path.resolve() == expected[key].resolve(), "Explicit runtime storage path differs: " + key)
            observed[key] = str(path.resolve())
        need(all(key in observed for key in expected), "Actual data/cache runtime paths missing")
        return {"status": "PASS", "hardwareConfigIntegrity": integrity, "paths": observed,
                "officialHardwareSchemaSHA256": digest(definitions),
                "runtimeIdentity": self.name, "temporaryPaths": "NO_UNRESOLVED_WRITABLE_PATH_ACCEPTED"}

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
        if self.api == 26:
            proof = json.loads(avd_identity.read_bytes(self.home / "resumed-steady-state-proof.json"))
            need(proof.get("type") == "RESUMED_STEADY_STATE_PROOF" and proof.get("status") == "PASS" and
                 proof.get("sourceCommit") == "3474f639c2a831d26e74eeeff5ec8db0f6ea7a3f" and
                 proof.get("serial") == self.serial and proof.get("first300SecondStartup") == "NOT_PROVEN",
                 "Current resume proof is missing or not the authorized steady-state observation")
        self.verify_avd()
        self.validate_process(timeout=remaining())  # Mandatory before any targeted ADB request.
        self.runtime_observation = self.runtime_paths()
        need(self.adb("get-state", timeout=remaining()) == "device", "New target is not online")
        need(self.adb("shell", "getprop", "sys.boot_completed", timeout=remaining()) == "1", "System boot is not complete")
        need(self.adb("shell", "getprop", "ro.build.version.sdk", timeout=remaining()) == str(self.api), "Actual API mismatch")
        need(self.adb("shell", "getprop", "ro.product.cpu.abi", timeout=remaining()) == "x86_64", "Actual ABI mismatch")
        verify_console_name(self.adb("emu", "avd", "name", timeout=remaining()), self.name)

    def resume(self):
        """New steady-state evidence, never a rewrite of the failed initial boot."""
        need(self.api == 26, "Only the original isolated API26 is authorized for resume")
        self.marker("runtime-resume")
        self.validate_process(timeout=30)
        self.verify_avd()
        self.runtime_observation = self.runtime_paths()
        self.apk_audit()
        deadline = time.monotonic() + 30
        def remaining():
            value = deadline - time.monotonic()
            need(value > 0, "Steady-state boot observation deadline exhausted; preserve running instance")
            return min(10, value)
        while True:
            state = self.adb("get-state", timeout=remaining(), allowed=(0, 1))
            if state == "device":
                boot = self.adb("shell", "getprop", "sys.boot_completed", timeout=remaining())
                if boot == "1":
                    need(self.adb("shell", "getprop", "ro.build.version.sdk", timeout=remaining()) == "26", "Actual API mismatch")
                    need(self.adb("shell", "getprop", "ro.product.cpu.abi", timeout=remaining()) == "x86_64", "Actual ABI mismatch")
                    verify_console_name(self.adb("emu", "avd", "name", timeout=remaining()), self.name)
                    self.validate_process(timeout=remaining())
                    immutable(self.home / "resumed-steady-state-proof.json", {
                        "type": "RESUMED_STEADY_STATE_PROOF", "status": "PASS", "utc": utc(),
                        "sourceCommit": "3474f639c2a831d26e74eeeff5ec8db0f6ea7a3f", "serial": self.serial,
                        "api": 26, "avd": self.name, "observedBootCompleted": "1", "first300SecondStartup": "NOT_PROVEN",
                        "originalStartFailure": "PRESERVED_NOT_RECLASSIFIED", "newCreate": 0, "newStart": 0,
                        "processIdentityProof": self.process_proof, "runtimePaths": self.runtime_observation})
                    return
            remaining()
            time.sleep(min(2, max(0, deadline - time.monotonic())))

    def reconcile_resume(self):
        """Correct only the proven console framing defect, without reissuing queries."""
        need(self.api == 26 and not (self.home / "resumed-steady-state-proof.json").exists(),
             "Only this unproved API26 steady-state admission can be reconciled")
        marker = json.loads(avd_identity.read_bytes(self.home / "operations/runtime-resume.json"))
        need(marker.get("operation") == "runtime-resume" and marker.get("serial") == self.serial,
             "Original current-authorized resume attempt missing")
        raw = avd_identity.read_bytes(self.journal)
        all_rows = [json.loads(line) for line in raw.decode("utf-8").splitlines() if line]
        rows = [row for row in all_rows if row.get("argv") and str(row["argv"][0]).lower().endswith("adb.exe")]
        suffixes = [["get-state"], ["shell", "getprop", "sys.boot_completed"],
                    ["shell", "getprop", "ro.build.version.sdk"], ["shell", "getprop", "ro.product.cpu.abi"],
                    ["emu", "avd", "name"]]
        need(len(rows) == 5, "Exactly the five original admission queries are required")
        outputs = []
        for row, suffix in zip(rows, suffixes):
            need(row.get("argv") == [str(TOOLS / "platform-tools/adb.exe"), "-s", self.serial, *suffix] and
                 row.get("exitCode") == 0 and row.get("stderrBytes") == 0 and
                 base64.b64decode(row.get("stderrBase64", ""), validate=True) == b"", "Original query identity/status incomplete")
            output = base64.b64decode(row["stdoutBase64"], validate=True)
            need(len(output) == row.get("stdoutBytes") and digest(output) == row.get("stdoutSHA256"),
                 "Original query raw integrity missing")
            outputs.append(output.decode("utf-8", errors="strict"))
        need([value.strip() for value in outputs[:4]] == ["device", "1", "26", "x86_64"], "Captured boot/API/ABI conflict")
        need("\r\r\n" in outputs[4], "Only the observed CRCRLF framing defect is eligible")
        verify_console_name(outputs[4], self.name)
        self.marker("runtime-resume-console-reconcile")
        immutable(self.evidence / "resume-query-prefix.json", {"prefixBytes": len(raw), "prefixSHA256": digest(raw),
            "fiveQueries": rows, "scope": "ORIGINAL_QUERY_BYTES_NOT_REISSUED"})
        self.validate_process(timeout=30)
        self.verify_avd()
        paths = self.runtime_paths()
        self.apk_audit()
        immutable(self.home / "resumed-steady-state-proof.json", {
            "type": "RESUMED_STEADY_STATE_PROOF", "status": "PASS", "utc": utc(),
            "sourceCommit": "3474f639c2a831d26e74eeeff5ec8db0f6ea7a3f", "serial": self.serial, "api": 26,
            "avd": self.name, "observedBootCompleted": "1", "observedBootQueryEndUtc": rows[1]["endUtc"],
            "first300SecondStartup": "NOT_PROVEN", "originalStartFailure": "PRESERVED_NOT_RECLASSIFIED",
            "resumeSupervisorFailure": "PRESERVED_CRCRLF_PARSER_FAILURE", "queryEvidence": "EXISTING_FIVE_QUERIES_REPARSED_NO_REISSUE",
            "queryPrefixBytes": len(raw), "queryPrefixSHA256": digest(raw), "newCreate": 0, "newStart": 0,
            "processIdentityProof": self.process_proof, "runtimePaths": paths})

    def create(self):
        need(self.api == 36, "API26 creation is prohibited; only adopt the preserved CLI output")
        self.api26_exit_gate()
        self.audit()
        need(not self.home.exists(), "Do not reuse/overwrite an existing environment")
        before = self.processes()
        need(not before.get("targets"), "AVD name already has a process")
        self.home.mkdir(parents=True, exist_ok=False)
        self.marker("create")
        for path in (self.registry, self.home / "android-user", self.home / "emulator-home",
                     self.home / "runtime-temp", self.data.parent):
            path.mkdir(parents=True, exist_ok=False)
        managers = list((self.sdk / "cmdline-tools").glob("*/bin/avdmanager.bat"))
        need(len(managers) == 1, "avdmanager version is ambiguous")
        self.run([managers[0], "create", "avd", "-n", self.name, "-k",
                  f"system-images;android-{self.api};google_apis;x86_64", "-p", self.data],
                 timeout=120, input_bytes=b"no\n")
        self.verify_avd()
        immutable(self.home / "create-proof.json", {"status": "PASS", "utc": utc(), "api": self.api,
                  "avd": self.name, "data": str(self.data), "registry": str(self.registry),
                  "configuration": self.config_observation,
                  "environmentAuditSHA256": digest((ROOT / "build/android-validation/environment-audit.json").read_bytes())})

    def start(self):
        need(self.api == 36, "API26_START_NOT_AUTHORIZED: resume the original instance; never start again")
        self.verify_avd()
        self.api26_exit_gate()
        name = "adoption-proof.json" if self.api == 26 else "create-proof.json"
        proof = json.loads(avd_identity.read_bytes(self.home / name).decode("utf-8"))
        need(proof.get("status") == "PASS" and proof.get("api") == self.api and
             proof.get("avd") == self.name and Path(proof.get("data", "")).resolve() == self.data.resolve(),
             "Current matching adoption/creation proof missing")
        need(proof.get("configuration", {}).get("configIntegrity") == self.config_observation["configIntegrity"] and
             proof.get("configuration", {}).get("descriptorIntegrity") == self.config_observation["descriptorIntegrity"] and
             proof.get("environmentAuditSHA256") == digest((ROOT / "build/android-validation/environment-audit.json").read_bytes()),
             "Configuration/environment changed after immutable admission proof")
        self.vacant()
        for other in CONFIG:
            other_home = BASE / f"api{other}"
            if other != self.api:
                need(not (other_home / "operations/start.json").exists() or
                     (other_home / "normal-exit-proof.json").is_file(), "Other isolated instance has not safely exited")
        temporary = self.home / "runtime-temp"
        unlinked_ancestors(temporary)
        temporary.mkdir(exist_ok=True)
        need(not any(temporary.iterdir()), "Runtime temp is not a fresh empty dedicated directory")
        self.marker("start")
        args = self.launch_arguments()
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
                              "avd": self.name, "api": self.api, "launcherPid": child.pid,
                              "runtimePaths": self.runtime_observation, "processIdentityProof": self.process_proof})
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
        uid = installed_uid.package_uid(package_info, package, path)
        independent = self.adb("shell", "pm", "list", "packages", "-U", "--user", "0", package)
        need(installed_uid.listed_uid(independent, package) == uid,
             "Installed UID conflicts with independent User 0 query")
        return uid

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
    import natural_tests
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("api", type=int, choices=CONFIG)
    parser.add_argument("operation", choices=("resume", "reconcile-resume", "create", "start", "install", "test", "close-readonly"))
    parser.add_argument("--kind", choices=("app", "test"))
    parser.add_argument("--method", choices=natural_tests.METHODS)
    args = parser.parse_args()
    supervisor = Supervisor(args.api)
    if args.operation == "test":
        need(args.method is not None and args.kind is None, "Specify exactly one permitted natural test method")
        natural_tests.run_one(supervisor, args.method)
    elif args.operation == "install":
        need(args.kind is not None, "Specify exactly one APK kind")
        need(args.method is None, "Test method is only valid for test")
        supervisor.install(args.kind)
    else:
        need(args.kind is None and args.method is None, "APK/test selection conflicts with operation")
        getattr(supervisor, args.operation.replace("-", "_"))()
    print(json.dumps({"status": "PASS", "operation": args.operation, "api": args.api,
                      "evidence": str(supervisor.evidence)}, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(json.dumps({"status": "BLOCKED", "errorType": type(error).__name__, "detail": str(error)}, sort_keys=True), file=sys.stderr)
        raise
