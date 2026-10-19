"""Fresh B1/B2 supervisor; only root may invoke one explicit operation.

There is no test traversal, old-instance resume, reset, retry, cleanup, kill or
exit command. Import does not touch an SDK, AVD, device or evidence directory.
"""
from __future__ import annotations
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
OLD = ROOT / "docs/research/m10-jpeg-android-isolated"
sys.path.insert(0, str(OLD))
sys.path.insert(0, str(HERE))
import validate as legacy
import avd_identity
import runtime_identity
import fresh_avd_identity
from validate import need, immutable, utc, digest, file_digest

BASE = Path("E:/CODEX/.codex/research-env/lithic-m10-b1b2-20261019-v1")
SOURCE = "08ce4b663b52127f400fa18bd13eb12fe04a8797"
APP = "com.cooper.lithic.debug"
TEST = APP + ".test"
APP_SHA = "b7c90277fa6c27e6ab314aa52c293de8e445ba6a3deba9e984ab96f4280e8a4c"
OLD_TEST_SHA = "bb00c0f6e34059170d20124eab739b43ccbc257ecc265def28906077fc0cb78c"
TEST_SHA = "37ad9ec91964b6a0b8a870ec35032164ebcdb6a1c41522e780cb7af343fcd338"
CERT = legacy.CERT
INSTRUMENTATION = TEST + "/com.cooper.lithic.data.media.M10ProcessBoundaryRunner"
HISTORICAL = ROOT / "docs/research/m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/static/android-validation/environment-audit-complete.json"
HISTORICAL_SHA = "7e1c5a3907737067313579bc96c29ab87b3ee8021ccf6cd4cdf6be47db0781c4"
ARTIFACTS = HERE / "artifact-inputs.json"
ARTIFACTS_SHA = "5a515b293303841c67ce305938fef70d00b0d5db99b78b04b90fb114041ed5ae"
TEST_SUPPLEMENT = HERE / "test-artifact-audit-v3.json"
TEST_SUPPLEMENT_SHA = "444b67acc1681ca1e82ae95250284c42b8fc08f3721a9907303ab30a8c38d7d7"
OPERATIONS = ("audit", "create", "start", "identity", "install-app", "install-test", "ending-readonly")
SERIALS = frozenset({"emulator-5664", "emulator-5666"})
_CAPTURE = None


def profile(api):
    need(api in (26, 36), "Only API26/API36 fresh targets are allowed")
    port = {26: 5664, 36: 5666}[api]
    sdk = Path("E:/CODEX/.codex/research-env/lithic-a2r-api26/sdk") if api == 26 else legacy.TOOLS
    return {"api": api, "name": f"Lithic_M10B1B2_API{api}_20261019", "port": port,
            "serial": f"emulator-{port}", "sdk": sdk, "home": BASE / f"api{api}"}


def locked_json(path, expected):
    raw = avd_identity.read_bytes(path, limit=1024 * 1024)
    need(digest(raw) == expected, "Locked audit input changed: " + path.name)
    return json.loads(raw)


def capture_module():
    global _CAPTURE
    if _CAPTURE is None:
        spec = importlib.util.spec_from_file_location("b1b2_private_continuous_log", OLD / "continuous_log.py")
        need(spec is not None and spec.loader is not None, "Capture module unavailable")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        module.SERIALS = SERIALS  # private module only; old module/source untouched
        module.TAIL = ["logcat", "-b", "main", "-v", "threadtime", "-s",
                       "LITHIC_JPEG:I", "LITHIC_B2_RESOURCE:E", "*:S"]
        module.HANDSHAKE_TAIL = module.TAIL[:-1] + [module.DIAGNOSTIC_TAG + ":I", "*:S"]
        _CAPTURE = module
    return _CAPTURE


class FreshSupervisor(legacy.Supervisor):
    historical_process_ids = ""

    def __init__(self, api):
        p = profile(api)
        for name in ("api", "name", "port", "serial", "sdk", "home"):
            setattr(self, name, p[name])
        self.data = self.home / "data" / (self.name + ".avd")
        self.registry = self.home / "avds"
        self.evidence = ROOT / "build/m10-b1b2-android-safety" / f"api{api}"
        avd_identity.unlinked_ancestors(self.evidence)
        self.evidence.mkdir(parents=True, exist_ok=True)
        self.journal = self.evidence / "commands.jsonl"
        self.env = self.environment()
        self.scope()

    def scope(self):
        p = profile(self.api)
        need(all(getattr(self, key) == p[key] for key in ("name", "port", "serial", "sdk", "home")),
             "Fresh B1/B2 target identity conflicts")
        need(self.data == self.home / "data" / (self.name + ".avd") and
             self.registry == self.home / "avds", "Fresh registration/data path conflicts")
        for path in (BASE, self.home, self.data, self.registry):
            avd_identity.unlinked_ancestors(path)

    def binding(self):
        return {"sourceCommit": SOURCE, "api": self.api, "avd": self.name,
                "serial": self.serial, "data": str(self.data), "base": str(BASE),
                "appSHA256": APP_SHA}

    def marker(self, operation):
        self.scope()
        need(operation in ("create", "start", "install-app", "install-test"), "Unknown one-shot operation")
        need(self.home.is_dir(), "Fresh environment has not been created")
        immutable(self.home / "operations" / (operation + ".json"),
                  {**self.binding(), "operation": operation, "utc": utc(), "status": "ATTEMPT_ISSUED"})

    def _historical(self):
        return locked_json(HISTORICAL, HISTORICAL_SHA)

    def image_metadata_lock(self):
        row = self._historical()[f"api{self.api}"]
        files = row["files" if self.api == 26 else "currentImageFiles"]
        return {"source.properties": next(x["sha256"] for x in files if x["name"] == "source.properties"),
                "package.xml": row["generatedPackageXml"]["sha256"]}

    def image_file_lock(self, filename):
        row = self._historical()[f"api{self.api}"]
        files = row["files" if self.api == 26 else "currentImageFiles"]
        found = [x for x in files if x["name"] == filename]
        need(len(found) == 1, "Image file has no unique historical lock")
        return found[0]

    def audit(self):
        self.scope()
        image = self.sdk / f"system-images/android-{self.api}/google_apis/x86_64"
        report = self._historical()
        if hasattr(self, "_audit_files"):
            need(all(self._file_stamp(path) == stamp for path, stamp in self._audit_files),
                 "Audited SDK/image identity changed in this invocation")
            return image
        need(legacy.JDK.is_dir() and self.sdk.is_dir(), "Fixed JDK/SDK unavailable")
        found = legacy.properties(image / "source.properties")
        need(found.get("AndroidVersion.ApiLevel") == str(self.api) and
             found.get("SystemImage.Abi") == "x86_64" and found.get("SystemImage.TagId") == "google_apis" and
             found.get("Pkg.Revision") == {26: "16", 36: "7"}[self.api], "Fixed image identity conflicts")
        selected = report[f"api{self.api}"]
        rows = selected["files" if self.api == 26 else "currentImageFiles"]
        expected = [(image / x["name"], x["bytes"], x["sha256"]) for x in rows]
        expected.append((image / "package.xml", selected["generatedPackageXml"]["bytes"],
                         selected["generatedPackageXml"]["sha256"]))
        manager = self.sdk / "cmdline-tools/latest/bin/avdmanager.bat"
        launcher = self.sdk / "emulator/emulator.exe"
        qemu = self.sdk / "emulator/qemu/windows-x86_64/qemu-system-x86_64-headless.exe"
        qrow = next(x for x in report["tools"]["qemuCurrentBinaries"] if Path(x["path"]).resolve() == qemu.resolve())
        expected += [(manager, manager.stat().st_size, report["tools"]["bothAvdmanagerBatchSha256"]),
                     (launcher, launcher.stat().st_size, report["tools"]["bothEmulatorExecutableSha256"]),
                     (qemu, qrow["bytes"], qrow["sha256"])]
        checked = []
        for path, size, sha in expected:
            need(file_digest(path, size) == sha, "Existing fixed SDK/image SHA differs: " + path.name)
            checked.append((path, self._file_stamp(path)))
        for path in (legacy.TOOLS / "platform-tools/adb.exe", legacy.JDK / "bin/java.exe"):
            avd_identity.unlinked_ancestors(path)
            need(path.is_file(), "Existing approved executable absent")
            checked.append((path, self._file_stamp(path)))
        self._audit_files = checked
        self.note({"operation": "FRESH_READ_ONLY_SDK_IMAGE_AUDIT", "utc": utc(),
                   "api": self.api, "historicalLockSHA256": HISTORICAL_SHA,
                   "filesCompared": len(expected), "status": "PASS", "downloads": 0,
                   "licenseTextCopied": False, "oldAvdDataRead": False})
        return image

    @staticmethod
    def _file_stamp(path):
        avd_identity.unlinked_ancestors(path)
        x = path.stat()
        return x.st_dev, x.st_ino, x.st_size, x.st_mtime_ns

    def verify_avd(self):
        self.audit()
        self.config_observation = fresh_avd_identity.verify_configuration(
            self.registry, self.data, self.sdk, self.name, self.api, self.image_metadata_lock())
        return self.config_observation["configuration"]

    def api26_exit_gate(self):
        if self.api != 36: return
        proof = json.loads(avd_identity.read_bytes(BASE / "api26/safety-exit-proof.json"))
        need(proof.get("sourceCommit") == SOURCE and proof.get("status") == "PASS" and
             proof.get("serial") == "emulator-5664" and proof.get("api") == 26 and
             all(proof.get(k) is True for k in ("allAuthorizedMethodsPassed", "consumersSafe", "osProcessEnded", "portsEnded", "normalExitObserved")),
             "This task API26 safe validation/exit is not proven; API36 remains NOT_RUN")

    def create(self):
        self.scope(); self.api26_exit_gate(); self.audit()
        need(not self.home.exists(), "Fresh environment exists; preserve it and do not retry")
        vacancy = self.vacant()  # inherited collector has explicit empty historical IDs
        need(not vacancy.get("ports"), "Target port occupancy is unknown")
        avd_identity.unlinked_ancestors(BASE)
        BASE.mkdir(parents=True, exist_ok=True)
        self.home.mkdir(exist_ok=False)
        self.marker("create")
        for path in (self.registry, self.home / "android-user", self.home / "emulator-home", self.home / "runtime-temp", self.data.parent):
            path.mkdir(parents=True, exist_ok=False)
        managers = list((self.sdk / "cmdline-tools").glob("*/bin/avdmanager.bat"))
        need(managers == [self.sdk / "cmdline-tools/latest/bin/avdmanager.bat"], "Ambiguous existing avdmanager")
        self.run([managers[0], "create", "avd", "-n", self.name, "-k",
                  f"system-images;android-{self.api};google_apis;x86_64", "-p", self.data], timeout=120, input_bytes=b"no\n")
        self.verify_avd()
        immutable(self.home / "create-proof.json", {**self.binding(), "status": "PASS", "utc": utc(),
                  "configuration": self.config_observation, "environmentLockSHA256": HISTORICAL_SHA})

    def process_spec(self):
        self.audit()
        return {"avd": self.name, "port": self.port, "launcherExe": str(self.sdk / "emulator/emulator.exe"),
                "qemuExe": str(self.sdk / "emulator/qemu/windows-x86_64/qemu-system-x86_64-headless.exe"),
                "argv": self.launch_arguments()}

    def validate_process(self, timeout=30):
        self.scope()
        anchor = getattr(self, "_runtime_anchor", None)
        proof_path = self.home / "boot-proof.json"
        if anchor is None and proof_path.is_file():
            anchor = self.require_proof(proof_path)["processIdentityProof"]["anchor"]
        proof = runtime_identity.await_complete(self.processes, anchor, self.process_spec(), legacy.windows_arguments,
                    timeout=min(30, timeout), clock=time.monotonic, sleep=time.sleep)
        self.note({"operation": "FRESH_BOUNDED_CIM_IDENTITY", "proof": proof})
        need(proof.get("status") == "PASS", "Fresh process identity UNKNOWN/BLOCKED: " + str(proof.get("reason")))
        if hasattr(self, "_launched_pid"):
            need(proof["anchor"]["processes"][0]["pid"] == self._launched_pid, "Launcher PID conflicts with owned Popen")
        self._runtime_anchor, self.process_proof = proof["anchor"], proof
        return proof["snapshot"]

    def require_proof(self, path):
        found = json.loads(avd_identity.read_bytes(path))
        need(found.get("status") == "PASS" and all(found.get(k) == value for k, value in self.binding().items()),
             "Fresh proof binding conflict")
        return found

    def config_path_preflight(self):
        """Prove every nonempty configured path before QEMU can consume it."""
        definitions = avd_identity.read_bytes(self.sdk / "emulator/lib/hardware-properties.ini", limit=512 * 1024)
        official = set(re.findall(r"^name\s*=\s*(\S+)\s*$", definitions.decode("utf-8"), re.M))
        args = self.launch_arguments()
        for flag, expected in (("-datadir", self.data), ("-data", self.data / "userdata-qemu.img"),
                               ("-cache", self.data / "cache.img")):
            need(args.count(flag) == 1 and args.index(flag) + 1 < len(args) and
                 Path(args[args.index(flag) + 1]).resolve() == expected.resolve(),
                 "Explicit launch data/cache confinement is missing")
        readonly = {"kernel.path": {"kernel-ranchu", "kernel-ranchu-64"},
                    "disk.ramdisk.path": {"ramdisk.img"}, "disk.dataPartition.initPath": {"userdata.img"},
                    "disk.systemPartition.initPath": {"system.img"}, "disk.vendorPartition.initPath": {"vendor.img"}}
        writable = {"disk.dataPartition.path", "disk.cachePartition.path", "hw.sdCard.path", "sdcard.path",
                    "hw.logcatOutput.path", "disk.systemPartition.path", "disk.vendorPartition.path",
                    "disk.encryptionKeyPartition.path", "disk.snapStorage.path"}
        image = self.sdk / f"system-images/android-{self.api}/google_apis/x86_64"
        observed = {}
        for key, value in self.config_observation["configuration"].items():
            if not value or not (key.endswith(".path") or key.endswith(".initPath")):
                continue
            need(key in official and (key in readonly or key in writable), "Unknown startup path: " + key)
            if key == "disk.dataPartition.path" and value == "<temp>":
                observed[key] = "KNOWN_TEMP_PLACEHOLDER_OVERRIDDEN_BY_EXPLICIT_DATA_ARGUMENT"
                continue
            # Reject lexical escape before inspecting ancestors of a foreign path.
            candidate = Path(value.replace("\\", "/"))
            need(".." not in candidate.parts and not any(x in value for x in ("<", ">", "$", "%", "\x00")),
                 "Unknown startup path placeholder/traversal: " + key)
            target = candidate if candidate.is_absolute() else self.data / candidate
            if key in readonly:
                matches = [filename for filename in readonly[key] if target.absolute() in
                           {(image / filename).absolute(), (self.data / filename).absolute()}]
                need(len(matches) == 1, "Startup read-only image path conflicts: " + key)
                avd_identity.unlinked_ancestors(target)
                lock = self.image_file_lock(matches[0])
                need(file_digest(target, lock["bytes"]) == lock["sha256"], "Startup template payload SHA mismatch")
            else:
                try:
                    target.absolute().relative_to(self.data.absolute())
                except ValueError as error:
                    raise RuntimeError("Startup writable path escapes fresh data root: " + key) from error
                avd_identity.unlinked_ancestors(target)
                need(legacy.within(target, self.data), "Startup writable path escapes fresh data root: " + key)
            observed[key] = str(target.absolute())
        return {"status": "PASS", "scope": "STARTUP_CONFIG_PATHS_NOT_RUNTIME_IDENTITY", "paths": observed,
                "officialHardwareSchemaSHA256": digest(definitions)}

    def adb(self, *args, timeout=30, allowed=(0,), cwd=None):
        self.scope()
        need(self.serial in SERIALS, "Old/unknown serial is forbidden")
        need(args and str(args[0]) in ("get-state", "shell", "exec-out", "install", "push", "pull", "emu"),
             "Global/server/unknown ADB command is not a targeted device operation")
        if args[0] == "emu":
            need(tuple(args[1:]) == ("avd", "name"), "Only read-only emulator identity query is exposed")
        return self.run([legacy.TOOLS / "platform-tools/adb.exe", "-s", self.serial, *args],
                        timeout=timeout, allowed=allowed, cwd=cwd)

    def _android_identity(self, deadline=None):
        def remaining():
            left = 30 if deadline is None else min(30, deadline - time.monotonic())
            need(left > 0, "Fixed fresh boot deadline exhausted")
            return left
        need(self.adb("get-state", timeout=remaining()) == "device", "Fresh target not online")
        need(self.adb("shell", "getprop", "sys.boot_completed", timeout=remaining()) == "1", "Fresh target not booted")
        need(self.adb("shell", "getprop", "ro.build.version.sdk", timeout=remaining()) == str(self.api), "Actual API mismatch")
        need(self.adb("shell", "getprop", "ro.product.cpu.abi", timeout=remaining()) == "x86_64", "Actual ABI mismatch")
        legacy.verify_console_name(self.adb("emu", "avd", "name", timeout=remaining()), self.name)

    def identity(self, deadline=None):
        self.scope(); self.require_proof(self.home / "boot-proof.json")
        self.verify_avd(); self.validate_process()
        self.runtime_observation = self.runtime_paths()
        self._android_identity(deadline)
        return {"status": "PASS", **self.binding(), "processIdentityProof": self.process_proof,
                "runtimePaths": self.runtime_observation}

    def start(self):
        self.scope(); self.api26_exit_gate(); self.verify_avd()
        self.startup_path_observation = self.config_path_preflight()
        before = self.require_proof(self.home / "create-proof.json")
        need(before["configuration"]["configIntegrity"] == self.config_observation["configIntegrity"] and
             before["configuration"]["descriptorIntegrity"] == self.config_observation["descriptorIntegrity"],
             "Fresh config changed after create proof")
        self.vacant()
        temporary = self.home / "runtime-temp"
        avd_identity.unlinked_ancestors(temporary)
        need(temporary.is_dir(), "Dedicated temporary parent is absent")
        emulator_temp = temporary / "emulator-session"
        if emulator_temp.exists() or emulator_temp.is_symlink():
            raise FileExistsError("Dedicated emulator temporary session already exists; do not reuse")
        for prior in temporary.iterdir():
            avd_identity.unlinked_ancestors(prior)
            need(prior.name == "hsperfdata_CDD" and prior.is_dir() and not any(prior.iterdir()),
                 "Unknown or nonempty pre-emulator temporary item; preserve and stop")
        emulator_temp.mkdir(exist_ok=False)
        emulator_env = dict(self.env)
        emulator_env.update(TEMP=str(emulator_temp), TMP=str(emulator_temp))
        self.marker("start")
        args, begin = self.launch_arguments(), time.monotonic()
        with (self.evidence / "emulator.log").open("xb") as log:
            child = subprocess.Popen(args, env=emulator_env, stdout=log, stderr=subprocess.STDOUT,
                                     creationflags=legacy.CREATION_NO_WINDOW)
        self._launched_pid = child.pid
        self.note({"operation": "FRESH_EMULATOR_START", "utc": utc(), "pid": child.pid, "argv": args,
                   "childTemp": str(emulator_temp), "priorEmptyJavaPerfPreserved": True})
        self.note({"operation": "FRESH_STARTUP_PATH_PROOF", "proof": self.startup_path_observation})
        deadline = begin + 300
        while time.monotonic() < deadline:
            need(child.poll() is None, "Fresh emulator launcher exited; no restart")
            self.validate_process(timeout=min(30, deadline - time.monotonic()))  # no ADB before exact CIM proof
            runtime = self.data / "hardware-qemu.ini"
            avd_identity.unlinked_ancestors(runtime)
            if not runtime.exists():
                self.note({"operation": "FRESH_RUNTIME_PATHS_PENDING", "utc": utc(),
                           "reason": "HARDWARE_QEMU_NOT_YET_PRESENT", "adbAuthorized": False})
                time.sleep(min(2, max(0, deadline-time.monotonic())))
                continue
            # Only absence can remain pending. Malformed/unsafe resolved paths
            # fail immediately; Android commands never precede this proof.
            self.runtime_observation = self.runtime_paths()
            def query_time():
                remaining = min(10, deadline-time.monotonic())
                need(remaining > 0, "Fixed fresh boot deadline exhausted")
                return remaining
            state = self.adb("get-state", timeout=query_time(), allowed=(0, 1))
            if state == "device" and self.adb("shell", "getprop", "sys.boot_completed", timeout=query_time()) == "1":
                self.verify_avd()
                self.runtime_observation = self.runtime_paths()
                self._android_identity(deadline)
                need(time.monotonic() <= deadline, "Fresh boot exceeded fixed 300s deadline")
                immutable(self.home / "boot-proof.json", {**self.binding(), "status": "PASS", "utc": utc(),
                          "launcherPid": child.pid, "processIdentityProof": self.process_proof,
                          "runtimePaths": self.runtime_observation, "createProofSHA256": digest(avd_identity.read_bytes(self.home / "create-proof.json"))})
                return
            time.sleep(min(2, max(0, deadline-time.monotonic())))
        raise RuntimeError("Fresh boot deadline exhausted; preserve state, no retries")

    def apk_audit(self):
        audit = locked_json(ARTIFACTS, ARTIFACTS_SHA)
        need(audit.get("gitHead") == SOURCE and audit.get("status") == "HOST_IDENTITIES_VERIFIED_ANDROID_NOT_RUN", "Artifact audit/source mismatch")
        rows = audit.get("artifacts", [])
        need(len(rows) == 2 and {x.get("name") for x in rows} == {"app", "test"}, "Unique artifact rows required")
        for row in rows:
            kind = row["name"]
            need(row.get("applicationId") == (APP if kind == "app" else TEST) and row.get("certificateSHA256") == CERT,
                 "Package/certificate conflict")
            expected = APP_SHA if kind == "app" else OLD_TEST_SHA
            path = Path(row["path"])
            need(path.is_absolute() and row.get("sha256") == expected and file_digest(path, row["bytes"]) == expected,
                 "Locked current APK bytes conflict")
            if kind == "app":
                need(row.get("contract") == {"worker": "com.cooper.lithic.data.media.jpegworker.JpegWorkerService", "exported": False, "process": ":jpeg_worker"}, "Worker manifest differs")
            else:
                need(row.get("contract") == {"instrumentation": INSTRUMENTATION, "targetPackage": APP}, "Instrumentation differs")
        supplement = locked_json(TEST_SUPPLEMENT, TEST_SUPPLEMENT_SHA)
        app = next(row for row in rows if row["name"] == "app")
        old_test = next(row for row in rows if row["name"] == "test")
        need(supplement.get("status") == "PASS_OFFLINE_TEST_ARTIFACT_ONLY" and
             supplement.get("sourceCommit") == SOURCE and supplement.get("applicationId") == TEST and
             supplement.get("certificateSHA256") == CERT and supplement.get("instrumentation") == INSTRUMENTATION and
             supplement.get("targetPackage") == APP and supplement.get("minSdk") == 26 and supplement.get("targetSdk") == 36 and
             supplement.get("manifestByteIdenticalToOldTest") is True and supplement.get("assetsByteIdenticalToOldTest") is True and
             supplement.get("nativeLibrariesInTest") == 0, "Independent Test supplement identity conflicts")
        locked_app, original, current = supplement["app"], supplement["oldTest"], supplement["newTest"]
        need(locked_app.get("sha256") == APP_SHA and locked_app.get("bytes") == app["bytes"] and
             Path(locked_app["path"]).resolve() == Path(app["path"]).resolve() and locked_app.get("rebuilt") is False and
             original.get("sha256") == OLD_TEST_SHA and original.get("preserved") is True and
             Path(original["path"]).resolve() == Path(old_test["path"]).resolve(), "App/original Test baseline changed")
        path = Path(current["path"])
        need(path.is_absolute() and current.get("sha256") == TEST_SHA and current.get("bytes") == 2118862 and
             file_digest(path, current["bytes"]) == TEST_SHA, "New locked Test APK bytes differ")
        resolved = json.loads(json.dumps(audit))
        current_row = next(row for row in resolved["artifacts"] if row["name"] == "test")
        current_row.update(path=str(path), bytes=current["bytes"], sha256=TEST_SHA,
                           supplementAuditSHA256=TEST_SUPPLEMENT_SHA)
        resolved["baselineArtifactAuditSHA256"] = ARTIFACTS_SHA
        resolved["testSupplementAuditSHA256"] = TEST_SUPPLEMENT_SHA
        return resolved

    def install(self, kind):
        need(kind in ("app", "test"), "Invalid install kind")
        self.identity()
        row = next(x for x in self.apk_audit()["artifacts"] if x["name"] == kind)
        package = row["applicationId"]
        need(self.adb("shell", "pm", "list", "packages", "-u", "--user", "0", package) == "", "Not freshly absent; no update/uninstall permission")
        if kind == "test":
            self.require_proof(self.home / "install-app-proof.json")
            self.installed(APP, APP_SHA)
        self.marker("install-" + kind)
        path = Path(row["path"])
        text = self.adb("install", path.name, cwd=path.parent, timeout=120)
        need(text.splitlines() == ["Performing Streamed Install", "Success"] or text.splitlines() == ["Success"], "Install not explicit bounded Success")
        uid = self.installed(package, row["sha256"])
        self.identity()
        immutable(self.home / f"install-{kind}-proof.json", {**self.binding(), "status": "PASS", "utc": utc(),
                  "kind": kind, "package": package, "sha256": row["sha256"], "uid": uid})

    def new_log_capture(self, directory, **kwargs):
        self.scope()
        return capture_module().ContinuousLogCapture(legacy.TOOLS / "platform-tools/adb.exe", self.serial,
                Path(directory) / "continuous-log", env=self.env, **kwargs)

    def ending_readonly(self):
        result = self.identity()
        result["appUID"] = self.installed(APP, APP_SHA) if (self.home / "install-app-proof.json").is_file() else None
        result["testUID"] = self.installed(TEST, TEST_SHA) if (self.home / "install-test-proof.json").is_file() else None
        return result  # observation only; no consumer-safe/normal-exit claim

    def _old_forbidden(self, *args, **kwargs):
        raise RuntimeError("Old adopt/resume/reconcile entry prohibited in this fresh task")
    adopt = resume = reconcile_resume = resume_anchor = close_readonly = _old_forbidden


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("api", type=int, choices=(26, 36))
    parser.add_argument("operation", choices=OPERATIONS)
    args = parser.parse_args()
    supervisor = FreshSupervisor(args.api)
    if args.operation.startswith("install-"):
        supervisor.install(args.operation.removeprefix("install-")); result = {"status": "PASS"}
    else:
        value = getattr(supervisor, args.operation.replace("-", "_"))()
        result = value if isinstance(value, dict) else {"status": "PASS", "scope": "OPERATION_ONLY_NOT_TEST_PASS"}
    print(json.dumps({**supervisor.binding(), **result, "operation": args.operation}, sort_keys=True))


if __name__ == "__main__":
    try: main()
    except Exception as error:
        print(json.dumps({"status": "BLOCKED", "type": type(error).__name__, "detail": str(error)}), file=sys.stderr)
        raise
