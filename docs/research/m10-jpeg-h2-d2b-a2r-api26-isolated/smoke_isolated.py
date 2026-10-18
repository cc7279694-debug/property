"""A2-R only: create/start one NEW API26 AVD, then one no-command Host launch.

This file deliberately has no general Android command/run/recovery interface.
Each mutation has a durable exclusive marker. Failure never triggers cleanup,
process termination, install retry, launch retry, or original-AVD inspection.
"""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import os
import re
import shlex
import socket
import subprocess
import time


HERE = Path(__file__).resolve().parent
ROOT = Path("E:/CODEX/.codex/research-env/lithic-a2r-api26")
SDK = ROOT / "sdk"
AVD_HOME = ROOT / "avds"
DATA = ROOT / "avd-data/Lithic_A2R_API26_20261018.avd"
USER_HOME = ROOT / "android-user"
EMULATOR_HOME = ROOT / "emulator-home"
AVD = "Lithic_A2R_API26_20261018"
PORT = 5610
SERIAL = "emulator-5610"
PKG = "com.cooper.lithic.m10r3.probe"
PACKAGE = "system-images;android-26;google_apis;x86_64"
APK = Path("E:/CODEX/.codex/worktrees/m10-jpeg-h2-d2b-api26-a1/砺石新APP/build/a1-artifacts/20261017T142151.230105Z/probe-debug.apk")
APK_SHA = "0d98316faf6cb18b589bad526a5d8260cea825a408ed949f32855bd7a7d31b8a"
JDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1")
ADB = SDK / "platform-tools/adb.exe"
EMULATOR = SDK / "emulator/emulator.exe"
MANAGER = SDK / "cmdline-tools/latest/bin/avdmanager.bat"
STATE = ROOT / "a2r-smoke-state"
IMAGE = SDK / "system-images/android-26/google_apis/x86_64"


def utc():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def file_meta(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return {"path": str(path), "bytes": path.stat().st_size, "sha256": h.hexdigest()}


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def durable(path, value):
    with path.open("x", encoding="utf-8", newline="\n") as out:
        json.dump(value, out, indent=2, ensure_ascii=True)
        out.write("\n")
        out.flush()
        os.fsync(out.fileno())


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def contained(path):
    resolved = path.resolve()
    resolved.relative_to(ROOT.resolve())
    current = path
    while current != ROOT.parent:
        require(not current.is_symlink() and not getattr(current, "is_junction", lambda: False)(), "linked research path: " + str(current))
        current = current.parent
    return resolved


def environment():
    return dict(os.environ, JAVA_HOME=str(JDK), ANDROID_HOME=str(SDK),
                ANDROID_SDK_ROOT=str(SDK), ANDROID_AVD_HOME=str(AVD_HOME),
                ANDROID_USER_HOME=str(USER_HOME), ANDROID_EMULATOR_HOME=str(EMULATOR_HOME),
                ANDROID_SDK_HOME=str(ROOT / "legacy-home"))


class Evidence:
    def __init__(self, phase):
        self.out = HERE / "evidence" / (phase + "-" + datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ"))
        self.out.mkdir(parents=True, exist_ok=False)
        self.commands = []

    def record(self, name, value):
        durable(self.out / name, value)

    def command(self, label, argv, timeout=30, stdin=None, privacy_filter=None, allow_nonzero=False):
        entry = {"label": label, "argv": [str(a) for a in argv], "startedUtc": utc()}
        raw = err = b""
        try:
            result = subprocess.run(entry["argv"], env=environment(), input=stdin,
                                    capture_output=True, timeout=timeout, check=False,
                                    creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
            raw, err = result.stdout, result.stderr
            entry["exitCode"] = result.returncode
        except BaseException as error:
            raw = getattr(error, "stdout", None) or b""
            err = getattr(error, "stderr", None) or b""
            entry.update(exitCode=None, exception=type(error).__name__ + ": " + str(error))
            raise
        finally:
            entry["endedUtc"] = utc()
            for stream, value in (("stdout", raw), ("stderr", err)):
                entry[stream] = {"bytes": len(value), "sha256": digest(value)}
                saved = privacy_filter(value) if privacy_filter else value
                path = self.out / (str(len(self.commands)).zfill(3) + "-" + label + "." + stream + ".log")
                path.write_bytes(saved)
                entry[stream].update(savedBytes=len(saved), savedSha256=digest(saved), privacyFiltered=bool(privacy_filter))
            self.commands.append(entry)
            # Each command gets a distinct immutable record; no stdout truncation.
            durable(self.out / (str(len(self.commands)).zfill(3) + "-command.json"), entry)
        require(allow_nonzero or result.returncode == 0, "command failed: " + label)
        return raw

    def adb(self, label, args, **kwargs):
        # No arbitrary device or server selection. Every call pins the NEW serial.
        return self.command(label, [ADB, "-s", SERIAL] + list(args), **kwargs)


def image_gate():
    require(load(HERE / "evidence/apk-audit/result.json")["status"] == "PASS", "locked APK audit missing")
    installation = HERE / "evidence/image-install-retry-1/result.json"
    if not installation.exists():
        installation = HERE / "evidence/image-install/result.json"
    install = load(installation)
    require(install.get("originalAcceptanceUnchanged") is True, "original acceptance changed")
    require(load(HERE / "evidence/local-tools-copy/result.json").get("status") == "PASS", "isolated copied tools not audited")
    audit = load(HERE / "evidence/installed-image-audit/result.json")
    require(all(audit.get(key) == value for key, value in {"status": "PASS", "apiLevel": 26,
            "tag": "google_apis", "abi": "x86_64", "revision": 16, "package": PACKAGE}.items()), "independent installed image audit incomplete")
    require(audit.get("allOriginalArchiveFilesSizeAndCrcVerified") is True,
            "official extracted bytes not independently verified")
    require(load(HERE / "evidence/pre-avd-storage-gate/result.json").get("status") == "PASS",
            "image parent/metadata/writer gate missing")
    contained(SDK)
    props = {}
    for line in (IMAGE / "source.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    require(props.get("AndroidVersion.ApiLevel") == "26", "wrong API")
    require(props.get("SystemImage.TagId") == "google_apis" and props.get("SystemImage.Abi") == "x86_64", "wrong image identity")
    require(props.get("Pkg.Revision") == "16", "wrong locked revision")
    require(file_meta(APK)["sha256"] == APK_SHA, "locked APK changed")
    return props


def host_isolation(evidence):
    for path in (SDK, AVD_HOME, DATA, USER_HOME, EMULATOR_HOME, STATE):
        contained(path)
    # Only target/new command lines are retained; other emulator command lines stay in memory.
    ps = ("$p=Get-CimInstance Win32_Process | Where-Object {$_.Name -match '^(emulator|qemu-system).*\\.exe$'};"
          "$m=@($p | Where-Object {$_.CommandLine -match 'Lithic_A2R_API26_20261018|(?<![0-9])5610(?![0-9])'} | "
          "Select-Object ProcessId,Name,CommandLine); @{target=$m;totalEmulatorProcesses=@($p).Count}|ConvertTo-Json -Depth 5 -Compress")
    raw = evidence.command("host-emulator-inventory", ["powershell.exe", "-NoProfile", "-Command", ps])
    result = json.loads(raw)
    return result


def create(evidence):
    image_gate()
    require(not STATE.exists(), "creation already attempted; preserve state")
    require(not AVD_HOME.exists() and not DATA.exists(), "AVD registry/data already exists")
    # Official command-line help may initialize an otherwise empty research user
    # directory. Empty tool preferences are distinct from any existing AVD/data.
    # No directory with any child, link or prior session content can be reused.
    empty_homes = []
    for home in (USER_HOME, EMULATOR_HOME):
        contained(home)
        if home.exists():
            require(home.is_dir() and not list(home.iterdir()), "research user home is not empty")
            empty_homes.append({"path": str(home), "entries": [], "creationTimeNs": home.stat().st_ctime_ns})
    evidence.record("empty-research-user-homes.json", {"homes": empty_homes,
            "avdRegistryAbsent": True, "avdDataAbsent": True, "priorMutationMarkerAbsent": True})
    require(not host_isolation(evidence)["target"], "new AVD target already running")
    for port in (PORT, PORT + 1):
        with socket.socket() as check:
            check.bind(("127.0.0.1", port))
    require(MANAGER.is_file(), "copy the existing official command-line tools into isolated SDK before create")
    for path in (STATE, AVD_HOME, DATA.parent, USER_HOME, EMULATOR_HOME, ROOT / "legacy-home"):
        path.mkdir(parents=True, exist_ok=path in (USER_HOME, EMULATOR_HOME))
    durable(STATE / "create-attempt.json", {"startedUtc": utc(), "name": AVD, "data": str(DATA), "serial": SERIAL})
    evidence.command("avd-create", [MANAGER, "create", "avd", "--name", AVD,
                     "--path", DATA, "--package", PACKAGE, "--abi", "x86_64", "--tag", "google_apis", "--device", "pixel"], timeout=120, stdin=b"no\n")
    config = DATA / "config.ini"
    ini = AVD_HOME / (AVD + ".ini")
    require(config.is_file() and ini.is_file(), "new AVD descriptors incomplete")
    require(str(DATA).replace("\\", "/") in ini.read_text().replace("\\", "/"), "AVD descriptor points elsewhere")
    original = config.read_text()
    require("image.sysdir.1=system-images\\android-26\\google_apis\\x86_64\\" in original or
            "image.sysdir.1=system-images/android-26/google_apis/x86_64/" in original, "AVD image path ambiguous")
    # Configure only this freshly-created AVD, never an existing/private AVD.
    with config.open("a", encoding="utf-8", newline="\n") as out:
        out.write("\nfastboot.forceColdBoot=yes\nfastboot.forceFastBoot=no\nfastboot.forceChosenSnapshotBoot=no\nsnapshot.present=no\n")
        out.flush()
        os.fsync(out.fileno())
    image_names = ["source.properties", "package.xml", "system.img", "ramdisk.img"]
    kernel = "kernel-ranchu-64" if (IMAGE / "kernel-ranchu-64").is_file() else "kernel-ranchu"
    image_names.append(kernel)
    lock = {"name": AVD, "serial": SERIAL, "sdk": str(SDK), "data": str(DATA),
            "avdHome": str(AVD_HOME), "createdUtc": utc(), "config": file_meta(config), "descriptor": file_meta(ini),
            "image": [file_meta(IMAGE / name) for name in image_names]}
    durable(STATE / "created.json", lock)
    evidence.record("result.json", {"status": "PASS", "scope": "NEW_AVD_CREATED", **lock})


def verify_created(before_first_start=False):
    image_gate()
    created = load(STATE / "created.json")
    require(created["name"] == AVD and created["serial"] == SERIAL, "creation identity conflict")
    require(file_meta(Path(created["descriptor"]["path"])) == created["descriptor"], "AVD pointer changed")
    config = Path(created["config"]["path"])
    contained(config)
    if before_first_start:
        require(file_meta(config) == created["config"], "AVD config changed before first start")
    else:
        # Emulator may update configuration metadata while booting. Its SDK image
        # and data location remain exact; no descriptor fallback is permitted.
        text = config.read_text().replace("\\", "/")
        require("image.sysdir.1=system-images/android-26/google_apis/x86_64/" in text, "running AVD image identity changed")
    # Full image hashes were captured at creation; immutable package metadata is rechecked.
    for entry in created["image"]:
        path = Path(entry["path"])
        require(path.stat().st_size == entry["bytes"], "image size changed")
        if path.name in ("source.properties", "package.xml"):
            require(file_meta(path) == entry, "image metadata changed")
    return created


def identity(evidence, suffix):
    state = evidence.adb("device-state-" + suffix, ["get-state"]).decode().strip()
    require(state == "device", "new serial not online")
    avd = evidence.adb("avd-name-" + suffix, ["emu", "avd", "name"]).decode().splitlines()
    require(avd and avd[0].strip() == AVD, "wrong AVD for new serial")
    api = evidence.adb("api-" + suffix, ["shell", "getprop", "ro.build.version.sdk"]).decode().strip()
    boot = evidence.adb("boot-" + suffix, ["shell", "getprop", "sys.boot_completed"]).decode().strip()
    require(api == "26" and boot == "1", "wrong API or boot incomplete")
    return {"serial": SERIAL, "avd": AVD, "api": api, "bootCompleted": boot}


def start(evidence):
    verify_created(before_first_start=True)
    require(not (STATE / "start-attempt.json").exists(), "emulator start already attempted")
    require(not host_isolation(evidence)["target"], "new target process already exists")
    for port in (PORT, PORT + 1):
        with socket.socket() as check:
            check.bind(("127.0.0.1", port))
    argv = [str(EMULATOR), "-avd", AVD, "-port", str(PORT), "-datadir", str(DATA),
            "-sysdir", str(IMAGE), "-no-snapshot", "-no-snapshot-load", "-no-snapshot-save", "-no-snapstorage", "-no-window", "-no-audio", "-no-boot-anim",
            "-memory", "1536", "-cores", "2"]
    durable(STATE / "start-attempt.json", {"startedUtc": utc(), "argv": argv, "attemptsAllowed": 1})
    stdout = (evidence.out / "emulator.stdout.log").open("wb")
    stderr = (evidence.out / "emulator.stderr.log").open("wb")
    try:
        proc = subprocess.Popen(argv, env=environment(), stdout=stdout, stderr=stderr,
                                creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    finally:
        stdout.close()
        stderr.close()
    durable(STATE / "emulator-spawned.json", {"pid": proc.pid, "spawnedUtc": utc(), "argv": argv})
    deadline = time.monotonic() + 300
    last = None
    while time.monotonic() < deadline:
        require(proc.poll() is None, "new emulator exited; no retry")
        state = evidence.adb("boot-state-" + str(len(evidence.commands)), ["get-state"], allow_nonzero=True).decode().strip()
        if state == "device":
            boot = evidence.adb("boot-progress-" + str(len(evidence.commands)), ["shell", "getprop", "sys.boot_completed"]).decode().strip()
            if boot == "1":
                last = identity(evidence, "booted")
                break
            require(boot in ("", "0"), "unknown boot-completed value")
        time.sleep(3)
    require(last is not None, "new AVD boot deadline expired; preserve process and data")
    running = host_isolation(evidence)
    require(running["target"], "new emulator host process identity missing")
    for process in running["target"]:
        line = process.get("CommandLine", "").replace("\\", "/").lower()
        require(str(SDK).replace("\\", "/").lower() in line and AVD.lower() in line,
                "running new emulator path/AVD identity conflict")
    evidence.record("running-emulator-identity.json", running)
    durable(STATE / "booted.json", {"status": "PASS", "observedUtc": utc(), **last})
    evidence.record("result.json", {"status": "PASS", "scope": "NEW_API26_BOOT", **last})


def process_rows(evidence, label):
    def filtered(raw):
        lines = raw.splitlines()
        return b"\n".join(line for line in lines if PKG.encode() in line or line.startswith(b"USER")) + b"\n"
    raw = evidence.adb(label, ["shell", "ps", "-A", "-o", "UID,PID,NAME"], privacy_filter=filtered)
    rows = []
    lines = raw.decode("utf-8", errors="strict").splitlines()
    require(lines and "PID" in lines[0] and "NAME" in lines[0], "ps header unsupported")
    for line in lines[1:]:
        parts = line.split()
        require(len(parts) == 3, "ps row format incomplete")
        if PKG in parts[2]:
            require(parts[0].isdigit() and parts[1].isdigit(), "research process UID/PID unavailable")
            rows.append({"uid": int(parts[0]), "pid": int(parts[1]), "name": parts[2]})
    require(not any(row["name"] != PKG for row in rows), "unexpected Worker/other research process")
    return rows


def probe_files(evidence, label):
    # New isolated research package only: never reads any original AVD/app bytes.
    # Android toybox find lacks -printf; use stat on its bounded research tree.
    script = "find files -maxdepth 4 -exec stat -c '%F|%n|%s' {} \\;"
    raw = evidence.adb(label, ["shell", "run-as", PKG, "sh", "-c", shlex.quote(script)])
    entries = []
    for line in raw.decode("utf-8", errors="strict").splitlines():
        parts = line.split("|")
        require(len(parts) == 3 and parts[2].isdigit(), "research file stat format unknown")
        kind, path, size = parts[0], parts[1], int(parts[2])
        require(path == "files" or path.startswith("files/"), "metadata path outside research files")
        allowed_dirs = {"files", "files/synthetic-stages", "files/r3-reports", "files/r3-traces"}
        if kind == "directory":
            require(path in allowed_dirs, "unexpected stage or child directory: " + path)
        elif kind in ("regular empty file", "regular file"):
            require((path == "files/synthetic-main-owner" and size == 0) or
                    (re.fullmatch(r"files/r3-traces/trace-[0-9]+\.jsonl", path) is not None and 0 < size < 1024 * 1024), "unexpected research file: " + path)
        else:
            raise RuntimeError("unknown research file type: " + kind)
        entries.append({"type": kind, "path": path, "bytes": size})
    require(any(x["path"] == "files/synthetic-main-owner" and x["bytes"] == 0 for x in entries), "Host owner marker missing")
    return entries


def observe(evidence):
    verify_created()
    require((STATE / "booted.json").is_file(), "new AVD boot not confirmed")
    require(not (STATE / "install-attempt.json").exists(), "installation already attempted; no retry")
    ident = identity(evidence, "before-install")
    require(not process_rows(evidence, "research-processes-before"), "new AVD already has research process")
    packages = evidence.adb("probe-absent", ["shell", "pm", "list", "packages", PKG])
    require(not packages.strip(), "new AVD already has Probe installed")
    require(file_meta(APK)["sha256"] == APK_SHA, "locked APK changed")
    durable(STATE / "install-attempt.json", {"startedUtc": utc(), "apk": file_meta(APK), "serial": SERIAL, "attemptsAllowed": 1})
    installed = evidence.adb("install-locked-probe", ["install", str(APK)], timeout=120)
    require(installed.decode().strip().endswith("Success"), "single Probe installation failed")
    path_raw = evidence.adb("installed-path", ["shell", "pm", "path", PKG]).decode().strip()
    require(path_raw.count("\n") == 0 and path_raw.startswith("package:/data/app/") and path_raw.endswith("/base.apk"), "installed APK path ambiguous")
    installed_sha = evidence.adb("installed-sha", ["shell", "sha256sum", path_raw[8:]]).decode().split()[0]
    require(installed_sha == APK_SHA, "installed APK byte mismatch")
    package_info = evidence.adb("installed-uid", ["shell", "pm", "list", "packages", "-U", PKG]).decode().strip()
    match = re.fullmatch("package:" + re.escape(PKG) + r" uid:([0-9]+)", package_info)
    require(match is not None, "installed UID unavailable")
    uid = int(match.group(1))
    require(not process_rows(evidence, "processes-before-launch"), "single cold launch not guaranteed")
    window_start = evidence.adb("log-window-start", ["shell", "date", shlex.quote("+%m-%d %H:%M:%S.000")]).decode().strip()
    require(re.fullmatch(r"[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\.000", window_start), "log window time unsupported")
    # Durable intent before sending exactly one no-extras Activity launch.
    launch = ["shell", "am", "start", "-W", "-n", PKG + "/.ProbeActivity"]
    durable(STATE / "host-launch-attempt.json", {"startedUtc": utc(), "argv": [str(ADB), "-s", SERIAL] + launch,
            "extras": [], "attemptsAllowed": 1, "apkSha256": APK_SHA, "uid": uid})
    launch_raw = evidence.adb("single-no-command-launch", launch, timeout=45)
    require(b"Status: ok" in launch_raw, "single launch failed or uncertain")
    time.sleep(12)
    rows = process_rows(evidence, "research-processes-after")
    require(len(rows) == 1 and rows[0]["uid"] == uid, "Host missing or identity conflict")
    pid = rows[0]["pid"]
    # Only research-PID lines and package-matched system events are retained.
    def relevant(raw):
        result = []
        for line in raw.splitlines():
            if PKG.encode() in line or re.search(rb"\s" + str(pid).encode() + rb"\s", line):
                result.append(line)
        return b"\n".join(result) + b"\n"
    log = evidence.adb("bounded-system-log", ["logcat", "-b", "main", "-b", "system", "-b", "crash", "-b", "events", "-d", "-v", "threadtime", "-T", window_start], privacy_filter=relevant)
    require(len(log) < 16 * 1024 * 1024, "log window unexpectedly large")
    selected_log = relevant(log)
    require(not re.search(rb"ANR in " + re.escape(PKG.encode()) + rb"|FATAL EXCEPTION|am_(?:crash|anr).*" + re.escape(PKG.encode()) + rb"|Fatal signal|Abort message", selected_log), "Host ANR/crash observed")
    files = probe_files(evidence, "research-files-after")
    trace_names = [x["path"] for x in files if x["path"].startswith("files/r3-traces/")]
    require(trace_names == ["files/r3-traces/trace-" + str(pid) + ".jsonl"], "trace identity missing or unexpected")
    trace = evidence.adb("host-trace", ["shell", "run-as", PKG, "cat", trace_names[0]])
    events = [json.loads(line) for line in trace.decode("utf-8").splitlines()]
    require(events and all(x.get("pid") == pid and x.get("uid") == uid for x in events), "trace identity conflict")
    require(all(x.get("dropped") == 0 and x.get("writeFailures") == 0 and x.get("diagnosticStatus") in ("PENDING", "COMPLETE") for x in events), "diagnostic integrity unknown")
    role = [x for x in events if x.get("event") == "APPLICATION_ROLE"]
    require(len(role) == 1 and role[0].get("role") == "HOST" and role[0].get("syntheticOwner") is True, "Host role evidence missing")
    require(not any(x.get("event", "").startswith(("BIND", "GRANT", "RESERVE", "COMMAND_")) for x in events), "unexpected command/session evidence")
    closing = identity(evidence, "closing")
    closing_rows = process_rows(evidence, "research-processes-closing")
    closing_files = probe_files(evidence, "research-files-closing")
    # APPLICATION_ROLE is emitted BEFORE completeBackground(). It proves role and
    # owner setup only; there is no sanctioned no-command READY getter in this APK.
    result = {"status": "UNKNOWN", "scope": "ONE_API26_HOST_NO_COMMAND_COLD_LAUNCH",
              "hostLaunch": "PASS", "hostRole": "PASS", "noUnexpectedStageOrWorker": "PASS",
              "directReady": "UNKNOWN", "readyReason": "APPLICATION_ROLE precedes completeBackground; no direct READY evidence available without a research command.",
              "diagnosticFinalDrain": "UNKNOWN", "logWindowStart": window_start,
              "workerEarlyOnBind": "NOT_RUN", "watchdogAndroid": "NOT_RUN",
              "identityBefore": ident, "identityClosing": closing, "uid": uid, "pid": pid,
              "closingProcesses": closing_rows, "filesAfter": files, "filesClosing": closing_files,
              "originalApi36OrH2Accessed": False, "privateBytesRead": False,
              "otherScenariosRun": False, "extraLaunchAttempts": 0}
    evidence.record("result.json", result)
    durable(STATE / "observation-ended.json", {"endedUtc": utc(), "status": result["status"], "evidence": str(evidence.out)})
    return 2


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=("create", "start", "observe"))
    args = parser.parse_args()
    evidence = Evidence(args.phase)
    status = 0
    try:
        status = {"create": create, "start": start, "observe": observe}[args.phase](evidence) or 0
    except BaseException as error:
        evidence.record("failure.json", {"status": "BLOCKED", "error": type(error).__name__ + ": " + str(error), "atUtc": utc(),
                "policy": "No retry/cleanup/termination; preserve new AVD and evidence. Only independent read-only closing checks remain allowed."})
        status = 1
    print(json.dumps({"exitCode": status, "phase": args.phase, "evidence": str(evidence.out)}))
    return status


if __name__ == "__main__":
    raise SystemExit(main())
