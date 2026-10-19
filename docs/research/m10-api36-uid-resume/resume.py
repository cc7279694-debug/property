"""Fixed API36 UID continuation; no device operation occurs on import.

Old campaign/start/install UNKNOWN evidence is never rewritten. This interface
cannot create/start/exit an emulator or install the App. Only the root executor
may invoke a device operation after the offline compatibility review.
"""
from pathlib import Path
import argparse
import json
import re
import sys

PROJECT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(PROJECT / "docs/research/m10-jpeg-android-isolated"))
sys.path.insert(0, str(PROJECT / "docs/research/m10-android-test-compat"))
from compat_campaign import CompatCampaign
import campaign
import validate as v
from validate import need, immutable, utc, digest

CID = "m10-final-20261019-022830-2a602443"
SOURCE = "d225dfc2d9ba74227cebab68a98176110145e50a"
LEGACY_SOURCE = "b2862062391a0c3008fe792db583b4abc5e146f9"
OLD_BOOT_SHA = "da8d64ff5c9575f6c1336f4004a1cf1c3761118f4b645572324bd3d4acb043a8"


def require_original_anchor(anchor):
    rows = anchor.get("processes", [])
    expected = [("launcher", 33512, [1792379583, 580570000]),
                ("qemu", 32252, [1792379583, 752254000])]
    need([(r.get("role"), r.get("pid"), r.get("created")) for r in rows] == expected,
         "Original launcher/QEMU PID or exact creation time conflicts")


class API36UidResume(CompatCampaign):
    historical_process_ids = "33512,32252"

    def __init__(self, resume_id):
        need(re.fullmatch(r"m10-uid-20261019-[0-9]{6}-[a-f0-9]{8}", resume_id) is not None,
             "Invalid exclusive UID continuation identity")
        need(campaign.SOURCE == LEGACY_SOURCE, "Historical campaign source cannot be rewritten")
        prior = v.BASE / "campaigns" / CID
        need((prior / "campaign.json").is_file(), "Existing campaign registration is required")
        original = v.avd_identity.read_bytes(prior / "api36/boot-proof.json")
        need(digest(original) == OLD_BOOT_SHA, "Original immutable boot proof changed")
        old = json.loads(original)
        need(old.get("campaignId") == CID and old.get("sourceCommit") == LEGACY_SOURCE and
             old.get("status") == "PASS" and old.get("api") == 36 and
             old.get("serial") == "emulator-5626", "Original boot provenance conflict")
        anchor = old["processIdentityProof"]["anchor"]
        require_original_anchor(anchor)
        super().__init__(36, CID)
        self.resume_id = resume_id
        self.control = self.campaign_root / "api36-uid-resume" / resume_id
        self.operations = self.control / "operations"
        self.evidence = PROJECT / "build/m10-api36-uid-resume" / resume_id
        self.boot_proof_path = self.control / "resume-boot-proof.json"
        for path in (self.control, self.operations, self.evidence):
            v.avd_identity.unlinked_ancestors(path)
            path.mkdir(parents=True, exist_ok=True)
        self.journal = self.evidence / "commands.jsonl"
        self._runtime_anchor = anchor
        self._launched_pid = 33512
        # Keep original SDK profile variables and argv. Redirecting self.home or
        # launch environment would change the already-running AVD's identity.

    def scope(self):
        need(self.api == 36 and self.serial == "emulator-5626" and self.campaign_id == CID and
             self.name == "Lithic_M10V_API36_20261018", "Only the fixed API36 continuation is authorized")

    def binding(self):
        self.scope()
        return {"campaignId": CID, "resumeId": self.resume_id, "sourceCommit": SOURCE,
                "api": 36, "serial": self.serial, "avd": self.name,
                "appSHA256": v.APP_SHA, "testSHA256": v.TEST_SHA}

    def marker(self, operation):
        allowed = r"admit-same-instance|confirm-app|install-test|class-load|capture-class-load|ending-readonly|(?:natural-test|capture-diagnostic)-0[0-8]"
        need(re.fullmatch(allowed, operation) is not None, "Operation is outside UID continuation permission")
        immutable(self.operations / (operation + ".json"), {
            **self.binding(), "operation": operation, "utc": utc(), "status": "ATTEMPT_ISSUED"})

    @property
    def app_proof(self): return self.control / "app-uid-admission.json"

    @property
    def test_proof(self): return self.control / "test-install-proof.json"

    def require_proof(self, path):
        result = json.loads(v.avd_identity.read_bytes(path))
        need(result.get("status") == "PASS" and all(result.get(k) == x for k, x in self.binding().items()),
             "UID continuation proof missing, unknown, or bound to a different input")
        return result

    def require_app(self): return self.require_proof(self.app_proof)

    def admit_same_instance(self):
        self.scope()
        need(not self.boot_proof_path.exists(), "This continuation has already been admitted")
        self.marker("admit-same-instance")
        self.api26_exit_gate()
        self.verify_avd()
        self.validate_process()  # Old exact anchor, two complete live CIM samples; no ADB yet.
        require_original_anchor(self.process_proof["anchor"])
        self.runtime_observation = self.runtime_paths()
        self.verify_android_identity()
        result = {**self.binding(), "status": "PASS", "type": "SAME_INSTANCE_UID_RESUME_ADMISSION",
                  "utc": utc(), "originalBootSHA256": OLD_BOOT_SHA,
                  "processIdentityProof": self.process_proof, "runtimePaths": self.runtime_observation,
                  "newCreate": 0, "newStart": 0, "oldInstallUnknownRewritten": False}
        immutable(self.boot_proof_path, result)
        return result

    def identity(self, deadline=None):
        self.require_proof(self.boot_proof_path)
        return super().identity(deadline)

    def absence(self):
        # Both successful queries must be empty. The second also lists retained
        # uninstalled package registrations; nonzero/unknown commands stay errors.
        need(self.adb("shell", "pm", "list", "packages", "-U", "--user", "0", v.TEST) == "" and
             self.adb("shell", "pm", "list", "packages", "-u", "--user", "0", v.TEST) == "",
             "Test package already present or absence is unknown; no reinstall authorized")

    def confirm_app(self):
        self.scope()
        directory = self.evidence / "app-confirm"
        directory.mkdir(exist_ok=False)
        self.identity(); self.apk_audit()
        uid = self.installed(v.APP, v.APP_SHA)
        self.absence()
        resource = self.resource_snapshot()
        need(resource.get("status") == "PASS" and resource.get("uid") == uid and
             not resource.get("stages") and not resource.get("observations") and
             not resource.get("processes", {}).get("rows"), "Prior App consumer or Stage state is not fresh/safe")
        owner = self.ownership()
        self.marker("confirm-app")
        result = {**self.binding(), "status": "PASS", "type": "UID_RESUME_NOT_OLD_INSTALL_PASS",
                  "utc": utc(), "appUID": uid, "resource": resource, "ownership": owner,
                  "newAppInstalls": 0, "privateLedgerOrCoverBytesRead": False}
        immutable(directory / "result.json", result)
        immutable(self.app_proof, result)
        return result

    def install_test_once(self):
        app = self.require_app()
        directory = self.evidence / "test-install"
        directory.mkdir(exist_ok=False)
        result = {**self.binding(), "status": "BLOCKED", "installCommandsIssued": 0}
        try:
            self.identity()
            audit = self.apk_audit()
            row = next(x for x in audit["apks"] if x["kind"] == "test")
            need(row["sha256"] == v.TEST_SHA and row["certificateSHA256"] == v.CERT and
                 row["packageName"] == v.TEST, "Test artifact identity conflicts")
            need(self.installed(v.APP, v.APP_SHA) == app["appUID"], "App UID changed before Test install")
            self.absence()
            before = self.resource_snapshot()
            self.unchanged(app["resource"], before)
            need(self.ownership() == app["ownership"], "Private App root owner changed")
            immutable(directory / "before.json", {"resource": before, "ownership": app["ownership"]})
            self.marker("install-test")
            result.update(status="UNKNOWN", installCommandsIssued=1)
            path = Path(row["path"])
            output = self.adb("install", path.name, cwd=path.parent, timeout=120)
            immutable(directory / "install-output.json", {"rawDecoded": output})
            need("Success" in output.splitlines(), "Test install completion is not explicit Success")
            self.identity()
            app_uid = self.installed(v.APP, v.APP_SHA)
            test_uid = self.installed(v.TEST, v.TEST_SHA)
            need(app_uid == app["appUID"] and test_uid != app_uid, "App/Test UID conflict after install")
            components = self.adb("shell", "pm", "list", "instrumentation").splitlines()
            target = [x for x in components if v.INSTRUMENTATION in x or f"(target={v.APP})" in x]
            need(target == [f"instrumentation:{v.INSTRUMENTATION} (target={v.APP})"], "Instrumentation identity conflict")
            after = self.resource_snapshot()
            self.unchanged(before, after)
            need(self.ownership() == app["ownership"], "App root ownership changed after Test install")
            immutable(directory / "after.json", {"resource": after, "ownership": app["ownership"]})
            result.update(status="PASS", appUID=app_uid, testUID=test_uid, appReinstalled=False,
                          privateLedgerOrCoverBytesRead=False)
            immutable(self.test_proof, result)
        except Exception as error:
            result["error"] = {"type": type(error).__name__, "detail": str(error)}
            raise
        finally:
            result["finishedUTC"] = utc()
            immutable(directory / "result.json", result)
        return result

    def loader_identity(self):
        return {**super().loader_identity(), **self.binding()}

    def class_load(self):
        self.require_proof(self.test_proof)
        return super().class_load()

    def run_method(self, index):
        self.require_proof(self.test_proof)
        return super().run_method(index)

    def ending_readonly(self):
        self.identity()
        self.marker("ending-readonly")
        uid = self.installed(v.APP, v.APP_SHA)
        resource = self.resource_snapshot()
        result = {**self.binding(), "status": "OBSERVED", "utc": utc(), "appUID": uid,
                  "resource": resource, "processIdentityProof": self.process_proof,
                  "deviceLeftRunning": True, "newCreate": 0, "newStart": 0,
                  "privateLedgerOrCoverBytesRead": False}
        if self.test_proof.exists():
            result["testUID"] = self.installed(v.TEST, v.TEST_SHA)
        immutable(self.evidence / "ending-protection.json", result)
        return result

    def forbidden(self, *args, **kwargs):
        raise RuntimeError("Operation is not authorized by this fixed UID continuation")

    create = start_campaign = admit_running = admit_started = ensure_apks = update_test = install = forbidden
    close_readonly = forbidden


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("resume_id")
    parser.add_argument("operation", choices=("admit-same-instance", "confirm-app", "install-test",
                                             "class-load", "method", "ending-readonly"))
    parser.add_argument("--index", type=int, choices=range(9))
    args = parser.parse_args()
    need((args.operation == "method") == (args.index is not None), "Index is only permitted for one fixed method")
    s = API36UidResume(args.resume_id)
    if args.operation == "admit-same-instance": s.admit_same_instance()
    elif args.operation == "confirm-app": s.confirm_app()
    elif args.operation == "install-test": s.install_test_once()
    elif args.operation == "class-load": s.class_load()
    elif args.operation == "method": s.run_method(args.index)
    else: s.ending_readonly()


if __name__ == "__main__": main()
