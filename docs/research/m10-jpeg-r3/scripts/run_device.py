#!/usr/bin/env python3
"""External, tightly scoped ADB supervisor for the isolated M10-R3 app.

No device command is run by --help.  Production private bytes are never read:
the original package's file names/metadata exist only in memory while forming
an aggregate digest.  Their raw command output is deliberately NOT archived.
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import re
import shlex
import subprocess
import sys
import time
import uuid
from pathlib import Path
from typing import Any

ADB = Path("C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe")
SDK = ADB.parent.parent
SERIAL = "emulator-5596"
AVD = "Lithic_M01_API_36"
PRODUCTION = "com.cooper.lithic.debug"
PROBE = "com.cooper.lithic.m10r3.probe"
OUTSIDER = "com.cooper.lithic.m10r3.outsider"
RESEARCH = Path(__file__).resolve().parents[1]
DEVICE_ROOT = RESEARCH / "evidence" / "device"
NORMAL_CASES = (
    "normal", "identities", "cancel", "ignore_cancel", "late_success",
    "worker_kill", "unbind", "rebind", "fd_duplicate", "stage_model",
    "normal_finish", "revoked_reserve", "revoked_start", "death_race", "intent_replay",
)
FAULTS = (
    "before_pin", "after_pin", "before_fd", "after_fd_before_ack",
    "before_result", "after_result", "before_receipt", "receipt_pending",
    "receipt_renamed", "receipt_synced", "before_retire", "after_retire",
)
SETTINGS = (
    ("font_scale", ("settings", "get", "system", "font_scale")),
    ("user_rotation", ("settings", "get", "system", "user_rotation")),
    ("accelerometer_rotation", ("settings", "get", "system", "accelerometer_rotation")),
    ("night_mode_setting", ("settings", "get", "secure", "ui_night_mode")),
    ("night_mode_effective", ("cmd", "uimode", "night")),
    ("wm_size", ("wm", "size")),
    ("wm_density", ("wm", "density")),
)


class StopExperiment(RuntimeError):
    """A failed gate stops all subsequent experimental scenarios."""


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def save_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def run_id() -> str:
    return str(uuid.uuid4())


class Supervisor:
    def __init__(self, output: Path):
        self.output = output.resolve()
        if not self.output.is_relative_to(DEVICE_ROOT.resolve()):
            raise StopExperiment("Evidence output must stay inside this research device directory")
        self.output.mkdir(parents=True, exist_ok=True)
        self.command_log = self.output / "adb-commands.jsonl"
        self.results: list[dict[str, Any]] = []
        self.attempted: list[dict[str, Any]] = []
        self.research_uids: dict[str, int] = {}
        self.device_verified = False

    def command(self, args: list[str], *, label: str, timeout: float = 45,
                private_metadata: bool = False) -> subprocess.CompletedProcess[bytes]:
        if not 0 < timeout <= 45:
            raise StopExperiment("A child command timeout must be at most 45 seconds")
        started = time.monotonic()
        timed_out = False
        try:
            result = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                    timeout=timeout, check=False)
        except subprocess.TimeoutExpired as error:
            timed_out = True
            result = subprocess.CompletedProcess(args, 124, error.stdout or b"", error.stderr or b"")
        record: dict[str, Any] = {
            "atUtc": utc_now(), "label": label, "argv": args,
            "exitCode": result.returncode, "wallMs": round((time.monotonic() - started) * 1000, 3),
            "timeoutSeconds": timeout, "timedOut": timed_out,
        }
        if private_metadata:
            record.update({
                "stdout": "REDACTED: original-app private metadata; aggregate only",
                "stdoutBytes": len(result.stdout),
                "stderr": "REDACTED: may contain private paths",
                "stderrBytes": len(result.stderr),
                "privateContentRead": False,
            })
        else:
            record["stdout"] = result.stdout.decode("utf-8", "replace")
            record["stderr"] = result.stderr.decode("utf-8", "replace")
        with self.command_log.open("a", encoding="utf-8", newline="\n") as sink:
            sink.write(json.dumps(record, ensure_ascii=False) + "\n")
        return result

    def adb(self, *args: str, label: str, timeout: float = 45,
            private_metadata: bool = False, required: bool = True) -> subprocess.CompletedProcess[bytes]:
        result = self.command([str(ADB), "-s", SERIAL, *args], label=label,
                              timeout=timeout, private_metadata=private_metadata)
        if required and result.returncode != 0:
            raise StopExperiment(f"{label}: child exit {result.returncode}; inspect scoped command evidence")
        return result

    def text(self, *args: str, label: str, timeout: float = 45) -> str:
        return self.adb(*args, label=label, timeout=timeout).stdout.decode("utf-8", "replace").strip()

    def verify_device(self) -> dict[str, Any]:
        if not ADB.is_file():
            raise StopExperiment("The fixed existing ADB executable was not found")
        state = self.text("get-state", label="target-device-state")
        avd_lines = self.text("emu", "avd", "name", label="target-avd-name").splitlines()
        avd_name = next((line.strip() for line in avd_lines if line.strip() and line.strip() != "OK"), "")
        api = self.text("shell", "getprop", "ro.build.version.sdk", label="target-api")
        abi = self.text("shell", "getprop", "ro.product.cpu.abilist", label="target-abi-list")
        facts = {"serial": SERIAL, "state": state, "avd": avd_name, "api": api, "abis": abi}
        save_json(self.output / "device-identity.json", facts)
        if state != "device" or avd_name != AVD or api != "36":
            raise StopExperiment("Fixed serial/AVD/API identity did not match; no experiment permitted")
        self.device_verified = True
        return facts

    def package(self, package: str, *, required: bool = True) -> dict[str, Any]:
        if package not in (PRODUCTION, PROBE, OUTSIDER):
            raise StopExperiment("Package outside the explicit allowlist")
        path_result = self.adb("shell", "pm", "path", package,
                               label=f"package-path-{package}", required=required)
        paths = [line[8:].strip() for line in path_result.stdout.decode("utf-8", "replace").splitlines()
                 if line.startswith("package:")]
        if not paths:
            if required:
                raise StopExperiment(f"Required package is absent: {package}")
            return {"package": package, "present": False}
        uid_text = self.text("shell", "run-as", package, "id", "-u",
                             label=f"package-uid-{package}")
        exact_uid = re.fullmatch(r"(\d+)", uid_text.strip())
        if not exact_uid:
            raise StopExperiment(f"Cannot establish exact package UID: {package}")
        apks = []
        for path in sorted(paths):
            if not path.startswith("/data/app/") or any(char in path for char in "\r\n\x00"):
                raise StopExperiment("Unexpected APK path; refusing to read it")
            digest_output = self.text("shell", "sha256sum", shlex.quote(path),
                                      label=f"installed-apk-sha-{package}")
            match = re.match(r"^([0-9a-fA-F]{64})\s", digest_output)
            if not match:
                raise StopExperiment(f"Cannot establish installed APK SHA: {package}")
            apks.append({"codePath": path, "sha256": match.group(1).lower()})
        return {"package": package, "present": True, "uid": int(exact_uid.group(1)), "apks": apks}

    def private_metadata(self) -> dict[str, Any]:
        # Only metadata is requested. Paths and raw records never leave memory.
        script = (
            "for r in files no_backup databases shared_prefs; do "
            "if [ -e \"$r\" ]; then "
            "find \"$r\" -exec stat -c '%f|%u|%g|%s|%Y|%Z|%i|%d|%n' {} + || exit 43; "
            "else printf 'ABSENT|%s\\n' \"$r\"; fi; done"
        )
        result = self.adb("shell", "run-as", PRODUCTION, "sh", "-c", shlex.quote(script),
                          label="production-private-find-stat-AGGREGATE-ONLY", private_metadata=True)
        if result.stderr.strip():
            raise StopExperiment("Original private metadata traversal had diagnostics; snapshot is incomplete")
        lines = sorted(line for line in result.stdout.splitlines() if line)
        canonical = b"\n".join(lines) + b"\n"
        aggregate = {
            "status": "PASS", "method": "find + stat only; sorted aggregate; no private content read",
            "roots": ["files", "no_backup", "databases", "shared_prefs"],
            "recordCount": len(lines), "aggregateSHA256": hashlib.sha256(canonical).hexdigest(),
            "rawPathsArchived": False, "privateByteIntegrity": "NOT_RUN",
            "limitation": "Metadata equality is not byte-level ledger/cover integrity proof",
        }
        del lines, canonical, result
        return aggregate

    def snapshot(self, name: str) -> dict[str, Any]:
        if not self.device_verified:
            self.verify_device()
        settings = {key: self.text("shell", *argv, label=f"snapshot-{name}-{key}")
                    for key, argv in SETTINGS}
        value = {"atUtc": utc_now(), "name": name, "device": {"serial": SERIAL, "avd": AVD, "api": 36},
                 "productionPackage": self.package(PRODUCTION),
                 "productionPrivateMetadata": self.private_metadata(), "settings": settings,
                 "privateByteIntegrity": "NOT_RUN"}
        target = self.output / f"snapshot-{name}.json"
        if target.exists():
            raise StopExperiment(f"Refusing to overwrite a previous {name} snapshot")
        save_json(target, value)
        return value

    def source_manifest(self) -> None:
        inputs = []
        allowed = {".java", ".kt", ".aidl", ".kts", ".properties", ".xml", ".py", ".md"}
        candidates = list((RESEARCH / "host").rglob("*")) + [Path(__file__).resolve()]
        for source in sorted(set(candidates)):
            relative = source.relative_to(RESEARCH)
            if (not source.is_file() or source.suffix not in allowed or
                    any(part in ("build", ".gradle", ".idea") for part in relative.parts) or
                    source.name == "local.properties"):
                continue
            inputs.append({"path": relative.as_posix(), "bytes": source.stat().st_size,
                           "sha256": sha256_file(source)})
        save_json(self.output / "source-input-sha256.json", inputs)

    def install_research(self) -> None:
        aapt_candidates = list((SDK / "build-tools").glob("*/aapt2.exe"))
        if not aapt_candidates:
            raise StopExperiment("Existing SDK aapt2 is required for pre-install package identity validation")
        aapt = sorted(aapt_candidates, key=lambda path: tuple(int(p) if p.isdigit() else -1
                                                             for p in re.split(r"[.-]", path.parent.name)))[-1]
        installed = []
        for module, package in (("probe", PROBE), ("outsider", OUTSIDER)):
            apk = RESEARCH / "host" / module / "build" / "outputs" / "apk" / "debug" / f"{module}-debug.apk"
            if not apk.is_file():
                raise StopExperiment(f"Research APK not found: {apk}")
            badging = self.command([str(aapt), "dump", "badging", str(apk)], label=f"verify-apk-{module}")
            decoded = badging.stdout.decode("utf-8", "replace")
            identity = re.search(r"^package: name='([^']+)'", decoded, re.M)
            if badging.returncode != 0 or not identity or identity.group(1) != package:
                raise StopExperiment("APK package mismatch; no install was attempted for this input")
            if re.search(r"^uses-permission", decoded, re.M):
                raise StopExperiment("Research APK requests permissions; inspect before any install")
            if not re.search(r"^(?:minSdkVersion|sdkVersion):'26'\s*$", decoded, re.M):
                raise StopExperiment("Research APK does not declare the required API26 minimum")
            digest = sha256_file(apk)
            present = self.package(package, required=False)
            install_observation = "ALREADY_PRESENT_EXACT_BYTES"
            if not (present.get("present") and len(present["apks"]) == 1 and present["apks"][0]["sha256"] == digest):
                install = self.adb("install", "-r", "-t", str(apk), label=f"install-research-only-{module}", required=False)
                if install.returncode not in (0, 124):
                    raise StopExperiment(f"Research install returned {install.returncode}")
                install_observation = "COMMAND_EXIT_0" if install.returncode == 0 else "COMMAND_TIMEOUT_OUTCOME_RESOLVED_BY_EXACT_INSTALLED_BYTES"
            actual = self.package(package)
            if len(actual["apks"]) != 1 or actual["apks"][0]["sha256"] != digest:
                raise StopExperiment("Installed research APK bytes differ from the verified input")
            installed.append({"module": module, "sourcePath": str(apk), "sourceSHA256": digest,
                              "sourceBytes": apk.stat().st_size, "installed": actual, "installObservation": install_observation})
            self.research_uids[package] = actual["uid"]
        if installed[0]["installed"]["uid"] == installed[1]["installed"]["uid"]:
            raise StopExperiment("Outsider must have a genuinely different application UID")
        save_json(self.output / "research-installed-apks.json", installed)

    def activity(self, command: str, run: str, **fields: str) -> None:
        if not re.fullmatch(r"[0-9a-f-]{36}", run):
            raise StopExperiment("Invalid synthetic run identity")
        argv = ["shell", "am", "start", "-W", "-n", f"{PROBE}/.ProbeActivity",
                "--es", "command", command, "--es", "runId", run]
        for key, value in fields.items():
            if not re.fullmatch(r"[A-Za-z0-9_-]+", key + value):
                raise StopExperiment("Unsafe Activity argument")
            argv += ["--es", key, value]
        result = self.adb(*argv, label=f"activity-{command}-{run}")
        if b"Error:" in result.stdout or b"Exception" in result.stderr:
            raise StopExperiment("Activity command did not start cleanly")

    def wait_report(self, run: str, *, ready: bool = False, package: str = PROBE) -> dict[str, Any]:
        if package not in (PROBE, OUTSIDER):
            raise StopExperiment("Report package outside research scope")
        suffix = ".ready.json" if ready else ".json"
        relative = "files/result.json" if package == OUTSIDER else f"files/r3-reports/{run}{suffix}"
        remote = f"if [ -f {shlex.quote(relative)} ]; then cat {shlex.quote(relative)}; else exit 44; fi"
        deadline = time.monotonic() + 20
        last_error = "report absent"
        while time.monotonic() < deadline:
            remaining = deadline - time.monotonic()
            result = self.adb("shell", "run-as", package, "sh", "-c", shlex.quote(remote),
                              label=f"poll-{'ready' if ready else 'report'}-{run}",
                              timeout=max(0.05, min(3, remaining)), required=False)
            if result.returncode == 0:
                try:
                    report = json.loads(result.stdout)
                    if not isinstance(report, dict):
                        raise ValueError("Report root is not an object")
                    if package == PROBE and report.get("runId") != run:
                        raise ValueError("Report identity differs from requested run")
                except (ValueError, UnicodeDecodeError) as error:
                    last_error = type(error).__name__
                else:
                    target = self.output / "reports" / f"{run}{'.ready' if ready else ''}.json"
                    save_json(target, report)
                    if not ready and report.get("status") != "PASS":
                        raise StopExperiment(f"{run}: report was {report.get('status', 'UNKNOWN')}; later experiments stopped")
                    return report
            elif result.returncode not in (44, 124):
                last_error = f"report read exit {result.returncode}"
            if time.monotonic() < deadline:
                time.sleep(min(0.2, max(0, deadline - time.monotonic())))
        save_json(self.output / "reports" / f"{run}.timeout.json",
                  {"status": "UNKNOWN", "runId": run, "ready": ready, "lastError": last_error,
                   "waitLimitSeconds": 20})
        raise StopExperiment(f"{run}: no complete report within 20 seconds; UNKNOWN")

    def processes(self, label: str) -> list[dict[str, Any]]:
        # Filtering happens on-device: other application process details are not retrieved.
        remote = ("ps -A -o UID,PID,NAME | awk '$3 == \"" + PROBE +
                  "\" || $3 == \"" + PROBE + ":jpeg_worker\" {print $1, $2, $3}'")
        text = self.text("shell", "sh", "-c", shlex.quote(remote), label=f"processes-{label}")
        rows = []
        for line in text.splitlines():
            fields = line.split()
            if len(fields) != 3 or fields[2] not in (PROBE, PROBE + ":jpeg_worker"):
                raise StopExperiment("Unexpected process record outside the research Host/worker")
            if fields[0].isdigit():
                uid = int(fields[0])
            else:
                app_uid = re.fullmatch(r"u(\d+)_a(\d+)", fields[0])
                if not app_uid:
                    raise StopExperiment("Cannot establish numeric research process UID")
                uid = int(app_uid.group(1)) * 100000 + 10000 + int(app_uid.group(2))
            if self.research_uids.get(PROBE) != uid:
                raise StopExperiment("Research process UID differs from the installed probe identity")
            rows.append({"uid": uid, "pid": int(fields[1]), "name": fields[2]})
        save_json(self.output / "processes" / f"{label}.json", {"atUtc": utc_now(), "processes": rows})
        return rows

    def fd_metadata(self, ready: dict[str, Any], label: str, processes: list[dict[str, Any]]) -> None:
        pid = ready.get("workerPid")
        if pid is None or (isinstance(pid, int) and not isinstance(pid, bool) and pid <= 0):
            save_json(self.output / "fd" / f"{label}.json",
                      {"status": "NOT_RUN", "reason": "This fault window reports no worker PID"})
            return
        if isinstance(pid, bool) or not isinstance(pid, int) or pid <= 1:
            raise StopExperiment("Invalid ready workerPid")
        row = next((item for item in processes if item["pid"] == pid and
                    item["name"] == PROBE + ":jpeg_worker"), None)
        if row is None:
            save_json(self.output / "fd" / f"{label}.json",
                      {"status": "NOT_RUN", "reason": "Ready worker is already absent from scoped ps", "pid": pid})
            return
        # No descriptor contents are opened. The same-UID run-as limits access.
        remote = f"for f in /proc/{pid}/fd/*; do [ -L \"$f\" ] && printf '%s -> %s\\n' \"$f\" \"$(readlink \"$f\")\"; done"
        result = self.adb("shell", "run-as", PROBE, "sh", "-c", shlex.quote(remote),
                          label=f"research-worker-fd-links-{label}", required=False)
        save_json(self.output / "fd" / f"{label}.json",
                  {"status": "PASS" if result.returncode == 0 else "NOT_RUN",
                   "pid": pid, "method": "readlink only; no FD bytes read", "exitCode": result.returncode,
                   "links": result.stdout.decode("utf-8", "replace"),
                   "limitation": "Scoped snapshot only; PID is never used as a kill target"})

    def one_case(self, case: str) -> None:
        identifier = run_id()
        self.attempted.append({"kind": "case", "case": case, "runId": identifier})
        self.activity("run", identifier, case=case)
        report = self.wait_report(identifier)
        self.results.append({"kind": "case", "case": case, "runId": identifier,
                             "status": "PASS", "reportedStatus": report["status"]})
        save_json(self.output / "scenario-results.json", self.results)
        print(f"PASS case {case}", flush=True)

    def fault_case(self, fault: str, *, package_force_stop: bool = False) -> None:
        identifier = run_id()
        self.attempted.append({"kind": "fault", "fault": fault, "runId": identifier,
                               "packageForceStop": package_force_stop})
        label = f"{'force-stop' if package_force_stop else 'host-self-kill'}-{fault}-{identifier}"
        self.activity("fault", identifier, fault=fault)
        ready = self.wait_report(identifier, ready=True)
        before = self.processes(label + "-before")
        self.fd_metadata(ready, label, before)
        if not any(row["name"] == PROBE for row in before):
            raise StopExperiment("No current research Host was observed before the interruption")
        if package_force_stop:
            self.adb("shell", "am", "force-stop", PROBE, label=f"PACKAGE-force-stop-{identifier}")
        else:
            self.activity("kill_host", identifier)
        time.sleep(0.3)
        after = self.processes(label + "-after")
        old_host = {row["pid"] for row in before if row["name"] == PROBE}
        if any(row["pid"] in old_host and row["name"] == PROBE for row in after):
            raise StopExperiment("Original Host still observed after requested interruption")
        recovered = run_id()
        self.activity("recover", recovered)
        report = self.wait_report(recovered)
        self.processes(label + "-recovered")
        self.results.append({"kind": "fault", "fault": fault, "runId": identifier,
                             "recoveryRunId": recovered, "status": "PASS",
                             "interruption": "PACKAGE_FORCE_STOP" if package_force_stop else "HOST_SELF_SIGKILL",
                             "beforeProcesses": before, "afterProcesses": after,
                             "reportedStatus": report["status"],
                             "externalObservationNotInternalRecoveryProof": True})
        save_json(self.output / "scenario-results.json", self.results)
        print(f"PASS {label.split('-' + identifier)[0]}", flush=True)

    def outsider(self) -> None:
        self.attempted.append({"kind": "different_uid_bind"})
        # Remove only this research helper's single previous report, never app data.
        self.adb("shell", "run-as", OUTSIDER, "rm", "-f", "files/result.json",
                 label="remove-stale-outsider-research-report-only")
        result = self.adb("shell", "am", "start", "-W", "-n", f"{OUTSIDER}/.OutsiderActivity",
                          label="different-UID-outsider-bind-attempt")
        if b"Error:" in result.stdout:
            raise StopExperiment("Outsider Activity did not start")
        identifier = run_id()
        report = self.wait_report(identifier, package=OUTSIDER)
        if report.get("uid") != self.research_uids.get(OUTSIDER):
            raise StopExperiment("Outsider report UID does not match its different-UID installed identity")
        if not (report.get("bindingDenied") is True or report.get("bindingReturned") is False):
            raise StopExperiment("Outsider PASS lacks an explicit binding rejection")
        self.results.append({"kind": "different_uid_bind", "status": report["status"]})
        save_json(self.output / "scenario-results.json", self.results)

    def logcat(self, label: str) -> None:
        result = self.adb("logcat", "-d", "-v", "threadtime", "-s", "LITHIC_R3:V", "*:S",
                          label=f"tag-only-logcat-{label}", required=False)
        destination = self.output / f"logcat-LITHIC_R3-{label}.log"
        destination.write_bytes(result.stdout)
        if result.returncode != 0:
            save_json(self.output / f"logcat-{label}-status.json",
                      {"status": "UNKNOWN", "exitCode": result.returncode})

    def run(self) -> bool:
        self.verify_device()
        self.source_manifest()
        before = self.snapshot("before")
        completed = False
        failure: str | None = None
        protection: dict[str, Any] = {"status": "UNKNOWN"}
        try:
            self.install_research()
            for case in NORMAL_CASES:
                self.one_case(case)
            self.logcat("normal-cases")
            for fault in FAULTS:
                self.fault_case(fault)
            self.fault_case("before_result", package_force_stop=True)
            self.outsider()
            completed = True
        except (StopExperiment, OSError, ValueError) as error:
            failure = str(error)
            print(f"STOP: {failure}", file=sys.stderr, flush=True)
        finally:
            self.logcat("final")
            try:
                after = self.snapshot("after")
                comparisons = {
                    "productionAPKAndUIDUnchanged": before["productionPackage"] == after["productionPackage"],
                    "productionPrivateMetadataUnchanged": before["productionPrivateMetadata"] == after["productionPrivateMetadata"],
                    "deviceSettingsUnchanged": before["settings"] == after["settings"],
                }
                protection = {"status": "PASS" if all(comparisons.values()) else "FAIL",
                              **comparisons, "privateByteIntegrity": "NOT_RUN",
                              "limitation": "No private ledger/cover contents were read or copied"}
                save_json(self.output / "protection-comparison.json", protection)
            except (StopExperiment, OSError, ValueError) as error:
                protection = {"status": "UNKNOWN", "reason": str(error), "privateByteIntegrity": "NOT_RUN"}
                save_json(self.output / "protection-comparison.json", protection)
        success = completed and protection["status"] == "PASS"
        attempted_normal = {row.get("case") for row in self.attempted if row.get("kind") == "case"}
        attempted_faults = {row.get("fault") for row in self.attempted
                            if row.get("kind") == "fault" and not row.get("packageForceStop")}
        save_json(self.output / "run-summary.json", {
            "status": "PASS" if success else "BLOCKED", "atUtc": utc_now(), "failure": failure,
            "completedScenarios": self.results, "attemptedScenarios": self.attempted, "protection": protection,
            "notRunAfterStop": {"normalCases": [c for c in NORMAL_CASES if c not in attempted_normal],
                                "faultWindows": [f for f in FAULTS if f not in attempted_faults],
                                "packageForceStop": not any(row.get("packageForceStop") for row in self.attempted),
                                "outsider": not any(row.get("kind") == "different_uid_bind" for row in self.attempted)},
            "api26": "NOT_RUN", "realNativeSIGSEGV": "NOT_RUN", "realOOM": "NOT_RUN", "LMK": "NOT_RUN",
            "productionJPEGAssertions": "NOT_RUN; historical three FAIL unchanged",
            "privateByteIntegrity": "NOT_RUN", "noProductionAPKInstallOrPrivateContentRead": True,
            "researchSourceOnly": True,
        })
        return success


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--evidence-dir", type=Path,
                        help="Must be a new/scoped directory beneath this research evidence/device root")
    subcommands = parser.add_subparsers(dest="command")
    snapshot = subcommands.add_parser("snapshot", help="Read-only original APK/metadata/settings snapshot")
    snapshot.add_argument("--name", choices=("before", "after"), required=True)
    subcommands.add_parser("run", help="Install only verified research APKs, then execute the bounded scenario suite")
    args = parser.parse_args()
    directory = args.evidence_dir or DEVICE_ROOT / dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    supervisor = Supervisor(directory)
    try:
        if args.command == "snapshot":
            supervisor.source_manifest()
            supervisor.snapshot(args.name)
            print(str(directory), flush=True)
            return 0
        return 0 if supervisor.run() else 2
    except (StopExperiment, OSError, ValueError) as error:
        save_json(directory / "supervisor-stop.json", {"status": "BLOCKED", "reason": str(error), "atUtc": utc_now()})
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
