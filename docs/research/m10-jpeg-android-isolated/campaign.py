"""Explicitly authorized fresh natural-test campaign; no operations on import.

Reuses the fixed APKs, collector, configuration and lifecycle validators. Old
start/method/UNKNOWN records are never an admission proof for this new batch.
"""
from pathlib import Path
import argparse
import json
import re
import subprocess
import time

import avd_identity
import natural_tests
import runtime_identity
from validate import (Supervisor, ROOT, BASE, TOOLS, CONFIG, need, immutable,
                      utc, digest, properties, file_digest, windows_arguments, CREATION_NO_WINDOW)

SOURCE = "946ac16c217cb1d96cde77558aacb9802bcd5c4f"


class CampaignSupervisor(Supervisor):
    historical_process_ids = ""  # New cold-start epoch; never borrow the former PID anchor.

    def __init__(self, api, campaign_id):
        need(re.fullmatch(r"m10-final-20261019-[0-9]{6}-[a-f0-9]{8}", campaign_id) is not None,
             "Invalid exclusive campaign identity")
        super().__init__(api)
        self.campaign_id = campaign_id
        self.campaign_root = BASE / "campaigns" / campaign_id
        self.control = self.campaign_root / f"api{api}"
        self.operations = self.control / "operations"
        self.boot_proof_path = self.control / "boot-proof.json"
        self.normal_exit_proof_path = self.control / "normal-exit-proof.json"
        self.evidence = ROOT / "build/m10-final-campaign" / campaign_id / f"api{api}"
        for path in (self.control, self.operations, self.evidence):
            avd_identity.unlinked_ancestors(path)
            path.mkdir(parents=True, exist_ok=True)
        identity = self.campaign_root / "campaign.json"
        expected = {"campaignId": campaign_id, "sourceCommit": SOURCE,
                    "methods": list(natural_tests.METHODS), "automaticRetry": False}
        if identity.exists():
            need(json.loads(avd_identity.read_bytes(identity)) == expected, "Campaign identity conflict")
        else:
            immutable(identity, expected)
        self.journal = self.evidence / "commands.jsonl"
        self.env.update({"TEMP": str(self.control / "runtime-temp"),
                         "TMP": str(self.control / "runtime-temp"),
                         "ANDROID_USER_HOME": str(self.control / "android-user"),
                         "ANDROID_EMULATOR_HOME": str(self.control / "emulator-home")})
        self._runtime_anchor = None

    def marker(self, operation):
        need(re.fullmatch(r"[a-z0-9-]+", operation) is not None, "Invalid operation marker")
        immutable(self.operations / (operation + ".json"), {
            "campaignId": self.campaign_id, "sourceCommit": SOURCE, "utc": utc(),
            "operation": operation, "api": self.api, "serial": self.serial,
            "avd": self.name, "data": str(self.data), "status": "ATTEMPT_ISSUED"})

    def audit(self):
        report = json.loads((ROOT / "build/android-validation/environment-audit.json").read_bytes())
        need(report.get("observationSourceCommit") == SOURCE and bool(report.get("freshUtc")) and
             report.get("api26", {}).get("allOriginalFilesSizeAndShaMatch") is True and
             report.get("api36", {}).get("generatedPackageXml", {}).get("identityParsed") is True and
             report.get("tools", {}).get("primaryEmulatorAuthenticode", {}).get("status") == "Valid",
             "Fresh measured environment audit missing")
        avd_identity.unlinked_ancestors(self.home)
        avd_identity.unlinked_ancestors(self.sdk)
        image = self.sdk / f"system-images/android-{self.api}/google_apis/x86_64"
        p = properties(image / "source.properties")
        need(p.get("AndroidVersion.ApiLevel") == str(self.api) and
             p.get("SystemImage.Abi") == "x86_64" and p.get("SystemImage.TagId") == "google_apis" and
             p.get("Pkg.Revision") == ("16" if self.api == 26 else "7"), "Image identity conflict")
        return image

    def validate_process(self, timeout=30):
        if self._runtime_anchor is None and self.boot_proof_path.is_file():
            proof = json.loads(avd_identity.read_bytes(self.boot_proof_path))
            need(proof.get("campaignId") == self.campaign_id and proof.get("status") == "PASS",
                 "Current campaign boot identity conflict")
            self._runtime_anchor = proof["processIdentityProof"]["anchor"]
        proof = runtime_identity.await_complete(self.processes, self._runtime_anchor,
            self.process_spec(), windows_arguments, timeout=min(30, timeout),
            clock=time.monotonic, sleep=time.sleep)
        self.note({"operation": "CAMPAIGN_PROCESS_IDENTITY", "proof": proof})
        need(proof.get("status") == "PASS", "Campaign process gate: " + str(proof.get("reason")))
        if hasattr(self, "_launched_pid"):
            need(proof["anchor"]["processes"][0]["pid"] == self._launched_pid, "Launcher PID changed")
        self.process_proof = proof
        self._runtime_anchor = proof["anchor"]
        return proof["snapshot"]

    def process_spec(self):
        report = json.loads((ROOT / "build/android-validation/environment-audit-complete.json").read_bytes())
        launcher = self.sdk / "emulator/emulator.exe"
        qemu = self.sdk / "emulator/qemu/windows-x86_64/qemu-system-x86_64-headless.exe"
        if not getattr(self, "_runtime_tools_verified", False):
            need(file_digest(launcher, launcher.stat().st_size) == report["tools"]["bothEmulatorExecutableSha256"],
                 "Launcher binary changed")
            rows = [r for r in report["tools"]["qemuCurrentBinaries"] if Path(r["path"]).resolve() == qemu.resolve()]
            need(len(rows) == 1 and rows[0].get("authenticode", {}).get("status") == "Valid" and
                 "Google LLC" in rows[0]["authenticode"].get("signer", "") and
                 file_digest(qemu, rows[0]["bytes"]) == rows[0]["sha256"], "Actual QEMU signature/SHA unproved")
            self._runtime_tools_verified = True
        return {"avd": self.name, "port": self.port, "launcherExe": str(launcher),
                "qemuExe": str(qemu), "argv": self.launch_arguments()}

    def admit_started(self):
        """Read-only admission of this campaign's sole already-launched instance.

        The first host audit-schema interruption stays failed. This operation
        never issues Popen, retries a start, or reclassifies an old boot proof.
        """
        need(not self.boot_proof_path.exists(), "Current boot proof already exists")
        marker = json.loads(avd_identity.read_bytes(self.operations / "start.json"))
        need(marker.get("campaignId") == self.campaign_id, "Campaign start marker mismatch")
        rows = [json.loads(line) for line in self.journal.read_bytes().splitlines()]
        starts = [r for r in rows if r.get("operation") == "CAMPAIGN_COLD_START"]
        need(len(starts) == 1 and starts[0].get("argv") == self.launch_arguments(), "Sole original launch unproved")
        self._launched_pid = starts[0]["pid"]
        self.marker("admit-started")
        self._starting = True
        self.verify_avd()
        self.validate_process()
        self.runtime_observation = self.runtime_paths()
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline:
            remaining = min(10, deadline-time.monotonic())
            if self.adb("get-state", allowed=(0, 1), timeout=remaining) == "device" and self.adb(
                    "shell", "getprop", "sys.boot_completed", timeout=remaining) == "1":
                self.identity(deadline)
                immutable(self.boot_proof_path, {
                    "type": "CAMPAIGN_SAME_LAUNCH_STEADY_STATE_PROOF", "campaignId": self.campaign_id,
                    "sourceCommit": SOURCE, "status": "PASS", "utc": utc(), "api": self.api,
                    "serial": self.serial, "avd": self.name, "newStartCommands": 0,
                    "initialAdmission": "PRESERVED_HOST_AUDIT_SCHEMA_FAILURE",
                    "initial300SecondBound": "NOT_PROVEN", "processIdentityProof": self.process_proof,
                    "runtimePaths": self.runtime_observation})
                self._starting = False
                return
            time.sleep(min(2, max(0, deadline-time.monotonic())))
        raise RuntimeError("Same-launch boot observation timeout; preserve scene")

    def identity(self, deadline=None):
        need(self.boot_proof_path.is_file() or getattr(self, "_starting", False),
             "No current campaign boot admission")
        self.verify_avd()
        remaining = 30 if deadline is None else min(30, deadline - time.monotonic())
        need(remaining > 0, "Campaign boot deadline exhausted")
        self.validate_process(timeout=remaining)
        self.runtime_observation = self.runtime_paths()
        self.verify_android_identity(deadline)

    def verify_android_identity(self, deadline=None):
        def budget():
            value = 30 if deadline is None else min(30, deadline - time.monotonic())
            need(value > 0, "Campaign identity deadline exhausted")
            return value
        need(self.adb("get-state", timeout=budget()) == "device", "Target is not online")
        for prop, expected in (("sys.boot_completed", "1"), ("ro.build.version.sdk", str(self.api)),
                               ("ro.product.cpu.abi", "x86_64")):
            need(self.adb("shell", "getprop", prop, timeout=budget()) == expected, "Actual identity: " + prop)
        from validate import verify_console_name
        verify_console_name(self.adb("emu", "avd", "name", timeout=budget()), self.name)

    def api26_exit_gate(self):
        if self.api == 26:
            return
        path = self.campaign_root / "api26/normal-exit-proof.json"
        proof = json.loads(avd_identity.read_bytes(path))
        need(proof.get("campaignId") == self.campaign_id and proof.get("status") == "PASS" and
             proof.get("serial") == "emulator-5624" and
             all(proof.get(k) is True for k in ("allNaturalMethodsPassed", "consumersSafe", "osProcessEnded")),
             "This campaign API26 natural tests/exit not proved")

    def start_campaign(self):
        self.api26_exit_gate()
        self.verify_avd()
        self.vacant()
        if self.api == 26:
            # No image/ledger bytes are inspected; validate current disk references.
            paths = self.runtime_paths()
            config = self.config_observation
            immutable(self.evidence / "reuse-admission.json", {
                "campaignId": self.campaign_id, "configuration": config,
                "priorRuntimePaths": paths, "oldProofsRewritten": False,
                "guestConsumerState": "MUST_BE_CHECKED_AFTER_BOOT_BEFORE_TEST"})
        for path in (self.control / "runtime-temp", self.control / "android-user",
                     self.control / "emulator-home"):
            path.mkdir(exist_ok=False)
        self.marker("start")
        args = self.launch_arguments()
        began = time.monotonic()
        with (self.evidence / "emulator.log").open("xb") as log:
            child = subprocess.Popen(args, env=self.env, stdout=log, stderr=subprocess.STDOUT,
                                     creationflags=CREATION_NO_WINDOW)
        self._launched_pid = child.pid
        self.note({"operation": "CAMPAIGN_COLD_START", "utc": utc(), "argv": args,
                   "pid": child.pid, "campaignId": self.campaign_id})
        deadline = began + 300
        self._starting = True
        # Wait only for this exact launcher to create its expected child. An
        # unrelated process/port is never treated as incomplete startup.
        while True:
            need(child.poll() is None, "Launcher exited; do not retry")
            sample = self.processes(timeout=min(10, max(1, deadline-time.monotonic())))
            rows = sample.get("targets", [])
            if len(rows) == 2:
                break
            need(time.monotonic()-began < 30 and len(rows) <= 1 and not sample.get("ports") and
                 all(r.get("ProcessId") == child.pid and r.get("Name", "").lower() == "emulator.exe"
                     for r in rows), "Unexpected startup process/port or launch window exhausted")
            time.sleep(.5)
        self.validate_process(timeout=min(30, deadline-time.monotonic()))
        while time.monotonic() < deadline:
            need(child.poll() is None, "Launcher exited; preserve scene")
            remaining = min(10, deadline-time.monotonic())
            if self.adb("get-state", allowed=(0, 1), timeout=remaining) == "device":
                if self.adb("shell", "getprop", "sys.boot_completed", timeout=remaining) == "1":
                    self.identity(deadline)
                    immutable(self.boot_proof_path, {
                        "type": "CAMPAIGN_COLD_BOOT_PROOF", "campaignId": self.campaign_id,
                        "sourceCommit": SOURCE, "status": "PASS", "utc": utc(), "api": self.api,
                        "serial": self.serial, "avd": self.name, "elapsedSeconds": time.monotonic()-began,
                        "processIdentityProof": self.process_proof, "runtimePaths": self.runtime_observation,
                        "oldBootAndExitProofsRewritten": False})
                    self._starting = False
                    return
            time.sleep(min(2, max(0, deadline-time.monotonic())))
        raise RuntimeError("Campaign boot deadline exhausted; no retry")

    def ensure_apks(self):
        self.identity()
        audit = self.apk_audit()
        for row in audit["apks"]:
            present = self.adb("shell", "pm", "list", "packages", row["packageName"])
            if present:
                # pm's argument is a substring filter: the application query
                # legitimately also returns its distinct, pinned test package.
                names = present.splitlines()
                need(len(names) == len(set(names)) and "package:" + row["packageName"] in names and
                     set(names) <= {"package:com.cooper.lithic.debug", "package:com.cooper.lithic.debug.test"},
                     "Unknown/ambiguous installed package")
                uid = self.installed(row["packageName"], row["sha256"])
                self.note({"operation": "REUSE_LOCKED_INSTALLED_APK", "package": row["packageName"],
                           "uid": uid, "sha256": row["sha256"]})
            else:
                self.install(row["kind"])


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("campaign_id")
    p.add_argument("api", type=int, choices=(26, 36))
    p.add_argument("operation", choices=("preflight", "start", "admit-started", "packages", "method", "exit", "create36"))
    p.add_argument("--index", type=int, choices=range(9))
    a = p.parse_args()
    s = CampaignSupervisor(a.api, a.campaign_id)
    if a.operation == "preflight":
        s.verify_avd(); immutable(s.evidence / "vacancy.json", s.vacant())
    elif a.operation == "start": s.start_campaign()
    elif a.operation == "admit-started": s.admit_started()
    elif a.operation == "packages": s.ensure_apks()
    elif a.operation == "method":
        need(a.index is not None, "Select exactly one method")
        natural_tests.run_one(s, natural_tests.METHODS[a.index])
    elif a.operation == "exit":
        from normal_shutdown import normal_exit
        normal_exit(s)
    elif a.operation == "create36":
        need(a.api == 36, "Only API36 creation is needed")
        s.create()
    print(json.dumps({"status": "PASS", "operation": a.operation, "api": a.api,
                      "campaignId": a.campaign_id}))


if __name__ == "__main__": main()
