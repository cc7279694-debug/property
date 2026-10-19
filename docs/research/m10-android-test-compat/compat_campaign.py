"""Fixed test-APK compatibility continuation. No device operation on import.

Only the root executor may invoke this module. No arbitrary APK hash, method,
cleanup, retry, fault, or alternate-device entry point is provided.
"""
from pathlib import Path
import argparse
import json
import re
import sys
import uuid

PROJECT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(PROJECT / "docs/research/m10-jpeg-android-isolated"))
import campaign
import device_evidence
import natural_tests
import validate as v
from continuous_log import ContinuousLogCapture, read_verified
from continuous_evidence import consume_capture
from validate import need, immutable, utc

OLD_TEST_SHA = "3b0005a31afa753e9a9709323572250c0c5c63153c68ca1f882755c98724282a"
LOADER = ("com.cooper.lithic.data.media.M10Api26TestClassLoadingPreflight#"
          "natural_campaign_declared_methods_are_loadable")
HISTORICAL_CAPTURE = (PROJECT / "docs/research/m10-android-capture-handshake/evidence/"
                      "m10-final-20261019-014441-e6b2cb97/api26")


class CompatCampaign(campaign.CampaignSupervisor):
    @property
    def load_proof(self):
        return self.control / "class-load-proof.json"

    def loader_identity(self):
        need(v.TEST_SHA == natural_tests.TEST_SHA and v.TEST_SHA != OLD_TEST_SHA,
             "New fixed test-APK identity not installed in supervisor constants")
        return {"campaignId": self.campaign_id, "api": self.api, "serial": self.serial,
                "method": LOADER, "appSHA256": v.APP_SHA, "testSHA256": v.TEST_SHA,
                "component": v.INSTRUMENTATION, "sourceCommit": campaign.SOURCE}

    def require_loader(self):
        value = json.loads(self.load_proof.read_bytes())
        need(value.get("status") == "PASS" and
             all(value.get(k) == x for k, x in self.loader_identity().items()),
             "Current campaign/API/test-APK class-load admission not proved")

    def authenticated_baseline_diagnostics(self):
        lines = []
        if self.api == 26:
            for index in range(3):
                proof, _ = read_verified(HISTORICAL_CAPTURE / f"method-{index:02d}/continuous-log",
                                         expected_serial=self.serial)
                need(proof.get("readinessProtocol") == "DIAGNOSTIC_NONCE_V1",
                     "Historical capture has no independently verified nonce")
                lines.append(proof["handshake"]["authenticatedRawLine"])
        if self.load_proof.exists():
            self.require_loader()
            proof, _ = read_verified(self.evidence / "class-load/continuous-log", expected_serial=self.serial)
            need(proof.get("readinessProtocol") == "DIAGNOSTIC_NONCE_V1", "Loader nonce unavailable")
            lines.append(proof["handshake"]["authenticatedRawLine"])
        need(len(lines) == len(set(lines)), "Duplicate authenticated baseline diagnostic")
        return tuple(lines)

    def resource_snapshot(self):
        value = device_evidence.before(self, natural_tests.METHODS[0])
        need(value.get("status") == "PASS", "Unsafe or unknown prior Stage/consumer")
        return value

    def ownership(self):
        text = device_evidence.private(self, "stat -c '%u|%g|%d|%i|%f' .")
        fields = text.split("|")
        need(len(fields) == 5 and all(re.fullmatch(r"[0-9]+", x) for x in fields[:4])
             and re.fullmatch(r"[0-9a-f]+", fields[4]) is not None
             and int(fields[0]) == self.installed(v.APP, v.APP_SHA)
             and int(fields[4], 16) & 0o170000 == 0o040000,
             "Private data root ownership/type unknown")
        return text

    @staticmethod
    def unchanged(before, after):
        need(before.get("status") == after.get("status") == "PASS" and
             all(before.get(k) == after.get(k) for k in ("api", "serial", "uid", "stages", "observations")),
             "Application UID or prior synthetic resources changed")

    def update_test(self):
        need(self.api == 26, "Replacing Test APK is authorized only on existing dedicated API26")
        self.loader_identity()
        directory = self.evidence / "test-update"
        directory.mkdir(exist_ok=False)
        result = {**self.loader_identity(), "status": "BLOCKED", "installCommandsIssued": 0,
                  "oldTestSHA256": OLD_TEST_SHA}
        try:
            self.identity()
            audit = self.apk_audit()
            row = next(x for x in audit["apks"] if x["kind"] == "test")
            need(row["sha256"] == v.TEST_SHA, "APK audit differs from fixed new test identity")
            app_uid = self.installed(v.APP, v.APP_SHA)
            test_uid = self.installed(v.TEST, OLD_TEST_SHA)
            prior = self.resource_snapshot()
            need(prior.get("status") == "PASS", "Prior resources are not safe for Test update")
            owner = self.ownership()
            immutable(directory / "before.json", {"resource": prior, "ownership": owner,
                                                   "appUID": app_uid, "testUID": test_uid})
            self.marker("install-test-update")
            result["installCommandsIssued"] = 1
            result["status"] = "UNKNOWN"
            path = Path(row["path"])
            reply = self.adb("install", "-r", path.name, cwd=path.parent, timeout=120)
            immutable(directory / "install-output.json", {"rawDecoded": reply})
            need("Success" in reply.splitlines(), "Test update lacks explicit successful completion")
            self.identity()
            need(self.installed(v.APP, v.APP_SHA) == app_uid and
                 self.installed(v.TEST, v.TEST_SHA) == test_uid, "Update changed App/Test UID or byte identity")
            current = self.resource_snapshot()
            current_owner = self.ownership()
            immutable(directory / "after.json", {"resource": current, "ownership": current_owner,
                                                  "appUID": app_uid, "testUID": test_uid})
            self.unchanged(prior, current)
            need(owner == current_owner, "Application private data root ownership changed")
            result.update(status="PASS", appUID=app_uid, testUID=test_uid,
                          appReinstalled=False, privateBytesRead=False, priorStagesUnchanged=True)
        except Exception as error:
            result["error"] = {"type": type(error).__name__, "detail": str(error)}
            raise
        finally:
            result["finishedUTC"] = utc()
            immutable(directory / "result.json", result)
        return result

    def loader_capture(self, directory):
        prior = self.authenticated_baseline_diagnostics()
        nonce = uuid.uuid4().hex
        def write(message):
            need(message == "M10_CAPTURE_READY nonce=" + nonce, "Loader diagnostic identity conflict")
            self.marker("capture-class-load")
            need(self.adb("shell", "log", "-p", "i", "-t", "LITHIC_M10_CAPTURE", message, timeout=10) == "",
                 "Loader diagnostic command returned unexpected output")
            return {"status": "PASS", "serial": self.serial, "exitCode": 0,
                    "tag": "LITHIC_M10_CAPTURE", "nonce": nonce}
        return ContinuousLogCapture(v.TOOLS / "platform-tools/adb.exe", self.serial,
            directory / "continuous-log", env=self.env, handshake_writer=write,
            nonce=nonce, prior_diagnostics=prior), prior

    def class_load(self):
        need(not self.load_proof.exists(), "Loader admission already exists: no reissue")
        directory = self.evidence / "class-load"
        directory.mkdir(exist_ok=False)
        result = {**self.loader_identity(), "status": "BLOCKED", "instrumentationCommandsIssued": 0}
        collector = None
        capture = None
        capture_finished = False
        def finish_capture():
            nonlocal capture, capture_finished
            if not capture_finished:
                capture_finished = True
                try: capture = collector.finish()
                except Exception as error: capture = {"status": "UNKNOWN", "error": str(error)}
            return capture
        try:
            self.identity(); self.apk_audit()
            result["appUID"] = self.installed(v.APP, v.APP_SHA)
            result["testUID"] = self.installed(v.TEST, v.TEST_SHA)
            components = self.adb("shell", "pm", "list", "instrumentation").splitlines()
            target = [x for x in components if v.INSTRUMENTATION in x or f"(target={v.APP})" in x]
            need(target == [f"instrumentation:{v.INSTRUMENTATION} (target={v.APP})"], "Instrumentation component conflict")
            prior = self.resource_snapshot()
            need(prior.get("status") == "PASS", "Prior resources are not safe for class loading")
            owner = self.ownership()
            immutable(directory / "before.json", {"resource": prior, "ownership": owner})
            collector, diagnostics = self.loader_capture(directory)
            need(collector.start().get("status") == "PASS", "Class-load continuous observer not READY")
            need(collector.assert_live().get("status") == "PASS", "Class-load observer died before command")
            self.marker("class-load")
            immutable(directory / "attempt.json", {**self.loader_identity(), "status": "ATTEMPT_ISSUED"})
            result.update(status="UNKNOWN", instrumentationCommandsIssued=1, instrumentationIssuedUTC=utc())
            text = self.adb("shell", "am", "instrument", "-w", "-r", "-e", "class", LOADER,
                            v.INSTRUMENTATION, timeout=120)
            natural_tests.immutable(directory / "instrumentation.txt", text, text=True)
            result["instrumentation"] = natural_tests.parse_instrumentation(text, LOADER)
            current = self.resource_snapshot()
            current_owner = self.ownership()
            immutable(directory / "after.json", {"resource": current, "ownership": current_owner})
            self.unchanged(prior, current)
            need(owner == current_owner, "Class loading changed private root ownership")
            capture = finish_capture()
            result["capture"] = capture
            scoped = consume_capture(capture, directory / "continuous-log", prior["phaseLog"],
                expected_serial=self.serial, prior_diagnostics=diagnostics)
            need(scoped.get("newPhaseLog") == "", "Class loader unexpectedly produced Worker lifecycle")
            need(result["instrumentation"]["status"] == "PASS", "Class loading JUnit failed or unknown")
            result.update(status="PASS", noStageOrConsumerCreated=True, privateBytesRead=False)
            immutable(self.load_proof, result)
        except Exception as error:
            result["error"] = {"type": type(error).__name__, "detail": str(error)}
            raise
        finally:
            if collector is not None:
                result["capture"] = finish_capture()
            result["finishedUTC"] = utc()
            immutable(directory / "result.json", result)
        return result

    def run_method(self, index):
        self.require_loader()
        need(type(index) is int and 0 <= index < len(natural_tests.METHODS), "Unknown method index")
        return natural_tests.run_one(self, natural_tests.METHODS[index])


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("campaign_id")
    p.add_argument("api", type=int, choices=(26, 36))
    p.add_argument("operation", choices=("admit-running", "packages", "update-test", "class-load", "method", "ending-readonly", "exit"))
    p.add_argument("--index", type=int, choices=range(9))
    a = p.parse_args()
    s = CompatCampaign(a.api, a.campaign_id)
    need((a.operation == "method") == (a.index is not None), "Select index only for one natural method")
    if a.operation == "admit-running": s.admit_running()
    elif a.operation == "packages": s.ensure_apks()
    elif a.operation == "ending-readonly": s.close_readonly()
    elif a.operation == "exit":
        from normal_shutdown import normal_exit
        normal_exit(s)
    elif a.operation == "update-test": s.update_test()
    elif a.operation == "class-load": s.class_load()
    else: s.run_method(a.index)


if __name__ == "__main__": main()
