"""Pure host mocks: no Android, ADB, APK or actual runtime observations."""
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
import hashlib
import copy
import json
import tempfile
from natural_tests import parse_instrumentation

sys.path.insert(0,str(Path(__file__).resolve().parent))
import device_evidence as evidence
from test_lifecycle_evidence import fixture
from natural_tests import METHODS

METHOD="com.cooper.lithic.data.media.M10StageV2CompatibilityTest#png_still_uses_v1_preparation_and_original_output_limits"


# Exact non-sensitive API26 method-00 instrumentation wire bytes, observed 2026-10-18.
# Original immutable evidence remains unchanged; CRLF is part of the locked input.
ACTUAL_CRLF_INSTRUMENTATION=b'INSTRUMENTATION_STATUS: numtests=1\r\nINSTRUMENTATION_STATUS: stream=\r\ncom.cooper.lithic.data.media.M10WorkerRetirementReviewTest:\r\nINSTRUMENTATION_STATUS: id=AndroidJUnitRunner\r\nINSTRUMENTATION_STATUS: test=healthy_jpeg_retires_real_worker_before_stage_completion\r\nINSTRUMENTATION_STATUS: class=com.cooper.lithic.data.media.M10WorkerRetirementReviewTest\r\nINSTRUMENTATION_STATUS: current=1\r\nINSTRUMENTATION_STATUS_CODE: 1\r\nINSTRUMENTATION_STATUS: numtests=1\r\nINSTRUMENTATION_STATUS: stream=.\r\nINSTRUMENTATION_STATUS: id=AndroidJUnitRunner\r\nINSTRUMENTATION_STATUS: test=healthy_jpeg_retires_real_worker_before_stage_completion\r\nINSTRUMENTATION_STATUS: class=com.cooper.lithic.data.media.M10WorkerRetirementReviewTest\r\nINSTRUMENTATION_STATUS: current=1\r\nINSTRUMENTATION_STATUS_CODE: 0\r\nINSTRUMENTATION_RESULT: stream=\r\n\r\nTime: 2.041\r\n\r\nOK (1 test)\r\n\r\n\r\nINSTRUMENTATION_CODE: -1'

class Dummy:
    serial="emulator-5624"
    api=26

class DeviceEvidenceTest(unittest.TestCase):
    def test_unapproved_serial_never_issues_commands(self):
        s=Dummy();s.serial="emulator-5596";s.adb=lambda *_args,**_kwargs:self.fail("device command")
        self.assertEqual("BLOCKED",evidence.before(s,METHOD)["status"])
    def test_unknown_method_never_issues_commands(self):
        s=Dummy();s.adb=lambda *_args,**_kwargs:self.fail("device command")
        self.assertEqual("BLOCKED",evidence.before(s,"x#y")["status"])
    def test_fixed_api_and_serial_mismatch_blocks(self):
        s=Dummy();s.api=36;s.adb=lambda *_args,**_kwargs:self.fail("device command")
        self.assertEqual("BLOCKED",evidence.before(s,METHOD)["status"])
    def test_private_ps_filter_requires_full_command_status(self):
        with self.assertRaises(ValueError):evidence.parse_process_snapshot("PID UID NAME\n10 10088 com.cooper.lithic.debug",10088)
    def test_private_ps_filter_accepts_explicit_numeric_uid(self):
        text="PS_COMPLETE=1\nPS_BYTES=1032\nPS_SHA256="+"a"*64+"\nPID UID NAME\n10 10088 com.cooper.lithic.debug\nPS_END"
        parsed=evidence.parse_process_snapshot(text,10088)
        self.assertEqual(10,parsed["rows"][0]["pid"]);self.assertEqual(1032,parsed["rawBytes"])
    def test_private_ps_unknown_same_uid_process_is_retained_for_blocking(self):
        text="PS_COMPLETE=1\nPS_BYTES=1032\nPS_SHA256="+"a"*64+"\nPID UID NAME\n10 10088 unknown\nPS_END"
        self.assertEqual("unknown",evidence.parse_process_snapshot(text,10088)["rows"][0]["name"])
    def test_private_ps_other_uid_or_named_uid_is_rejected(self):
        for uid in ("10089","u0_a88"):
            with self.subTest(uid=uid):
                text="PS_COMPLETE=1\nPS_BYTES=1032\nPS_SHA256="+"a"*64+f"\nPID UID NAME\n10 {uid} wrong\nPS_END"
                with self.assertRaises(ValueError):evidence.parse_process_snapshot(text,10088)
    def test_guard_is_never_read_as_content(self):
        s=Dummy();s.adb=lambda *_args,**_kwargs:self.fail("guard content command")
        with self.assertRaises(ValueError):evidence.read_text(s,"no_backup/m10-worker-review-x/media/.m10-jpeg-v2/a/consumer.guard")
    def test_any_ledger_path_is_forbidden_before_command(self):
        s=Dummy();s.adb=lambda *_args,**_kwargs:self.fail("ledger content command")
        with self.assertRaises(ValueError):evidence.read_text(s,"no_backup/account/database.sqlite")
    def test_log_prefix_rotation_is_not_silently_ignored(self):
        with self.assertRaises(ValueError):evidence.new_log_lines("one\ntwo","two\nthree")
    def test_log_delta_only_contains_new_lines(self):
        self.assertEqual("three",evidence.new_log_lines("one\ntwo","one\ntwo\nthree"))
    def test_link_stat_is_rejected(self):
        with self.assertRaises(ValueError):evidence.parse_stat("a1ff|7|90|4")
    def test_real_proc_stat_extracts_start_ticks(self):
        data="202 (jpeg worker) S "+" ".join(["1"]*18+["5000"]+["1"]*5)
        self.assertEqual(5000,evidence.proc_start(data,202))
    def test_truncated_proc_stat_is_rejected(self):
        with self.assertRaises(ValueError):evidence.proc_start("202 (worker) S 1 2",202)
    def test_preflight_missing_evidence_cannot_be_assessed_pass(self):
        self.assertEqual("BLOCKED",evidence.assess(METHOD,{"status":"PASS"},{"status":"PASS"},{"status":"PASS"})["status"])
    def test_before_and_after_serial_conflict_is_blocked(self):
        self.assertEqual("BLOCKED",evidence.assess(METHOD,{"status":"PASS","serial":"emulator-5624"},{"status":"PASS","serial":"emulator-5596"},{"status":"PASS"})["status"])
    def completed(self,index=0):
        args=fixture("SUCCESS" if index==0 else "INVALID_IMAGE")
        method=METHODS[index]
        common={"status":"PASS","method":method,"serial":"emulator-5624","api":26,"uid":10088}
        prior={**common,"stages":{},"observations":{},"phaseLog":""}
        current={**common,"newStages":{"m10-worker-review-generation":{"records":args[4]}},
                 "newObservations":args[2],"newPhaseLog":args[3],"processes":json.loads(args[5]),
                 "workerProc":{"202":{"pid":202,"startTicks":5000,"absent":True}},"instrumentationText":args[1]}
        parsed={"status":"PASS","method":method,"decodedSHA256":hashlib.sha256(args[1].encode()).hexdigest()}
        return method,prior,current,parsed
    def test_healthy_adapter_assessment_uses_real_parser_contract(self):
        self.assertEqual("PASS",evidence.assess(*self.completed())["status"])
    def test_damaged_adapter_assessment_uses_real_parser_contract(self):
        self.assertEqual("PASS",evidence.assess(*self.completed(1))["status"])
    def test_raw_instrumentation_hash_mismatch_stops(self):
        args=list(self.completed());args[-1]["decodedSHA256"]="a"*64
        self.assertEqual("BLOCKED",evidence.assess(*args)["status"])
    def test_png_success_remains_assertion_level_not_independent_media_bytes(self):
        method,prior,current,parsed=self.completed();method=METHODS[6]
        prior["method"]=current["method"]=method
        parsed["method"]=method
        current.update(newStages={},newObservations=[],newPhaseLog="",workerProc={})
        value=evidence.assess(method,prior,current,parsed)
        self.assertEqual("PASS",value["status"])
        self.assertEqual("EXECUTED_TEST_ASSERTIONS_ONLY",value["evidence_levels"]["png_v1_contract"])
    def test_png_unexpected_worker_log_blocks(self):
        method,prior,current,parsed=self.completed();method=METHODS[6]
        prior["method"]=current["method"]=method
        parsed["method"]=method
        current.update(newStages={},newObservations=[])
        self.assertEqual("BLOCKED",evidence.assess(method,prior,current,parsed)["status"])
    def test_shell_script_is_one_explicitly_quoted_remote_command(self):
        s=Dummy();calls=[]
        s.adb=lambda *args,**kwargs:calls.append(args) or "10088"
        evidence.private(s,"printf '%s' \"value\" | wc -c")
        self.assertEqual(2,len(calls[0]));self.assertEqual("shell",calls[0][0])
        import shlex
        tokens=shlex.split(calls[0][1])
        self.assertEqual(["run-as",evidence.APP,"sh","-c","printf '%s' \"value\" | wc -c"],tokens)
    def test_fresh_install_absent_no_backup_is_observed_without_creating_it(self):
        s=Dummy();s.installed=lambda *_:10088
        calls=[]
        s.adb=lambda *args,**kwargs:calls.append(args) or "10088"
        with patch.object(evidence,"exists",return_value=False), patch.object(evidence,"processes",return_value={"complete":True,"rows":[]}), patch.object(evidence,"logs",return_value=""), patch.object(evidence,"stages",side_effect=AssertionError("absent directory read")), patch.object(evidence,"observations",side_effect=AssertionError("absent observation read")):
            value=evidence.before(s,METHOD)
        self.assertEqual("PASS",value["status"])
        self.assertEqual("ABSENT_FRESH_INSTALL_OBSERVATION",value["noBackupStatus"])
        self.assertEqual({},value["stages"]);self.assertEqual({},value["observations"])
        self.assertFalse(any("mkdir" in str(call) for call in calls))
    def test_stages_function_does_not_list_absent_no_backup(self):
        with patch.object(evidence,"exists",return_value=False),patch.object(evidence,"directory",side_effect=AssertionError("absent directory list")):
            self.assertEqual({},evidence.stages(Dummy()))
    def protection(self):
        method,prior,current,parsed=self.completed();method=METHODS[4]
        prior["method"]=current["method"]=parsed["method"]=method
        phases=current["newPhaseLog"]
        import re
        second=phases.replace("worker=202/10088/5000","worker=203/10088/6000")
        second=second.replace("  202   202 I","  203   203 I")
        second=second.replace("attempt=00000000-0000-0000-0000-000000000002","attempt=00000000-0000-0000-0000-000000000099")
        second=re.sub(r"at=(\d+)",lambda m:"at="+str(int(m[1])+100),second)
        value={"targetType":"PROJECT","source":"entropy_cut_eoi_retained.jpg","prepareResult":"INVALID_IMAGE",
               "prepareWronglyAccepted":False,"newFormalMetadataExists":False,"replacementPreparedLeaseActive":False,
               "oldFactsIntact":True,"mediaTableIntact":True,"privateFinalFilesIntact":True,
               "oldCoverSha256Before":"a"*64,"oldCoverSha256After":"a"*64}
        current.update(newStages={},newPhaseLog=phases+"\n"+second,
                       newObservations=[{"name":"old-cover-project","value":value},
                                        {"name":"old-cover-project-cleanup","value":dict(value,failedPreparationCleanupVerified=True)}],
                       workerProc={"202":{"pid":202,"startTicks":5000,"absent":True},"203":{"pid":203,"startTicks":6000,"absent":True}})
        return method,prior,current,parsed
    def test_protection_success_requires_both_observations_and_both_workers(self):
        self.assertEqual("PASS",evidence.assess(*self.protection())["status"])
    def test_protection_missing_old_facts_flag_blocks(self):
        args=list(self.protection());del args[2]["newObservations"][0]["value"]["oldFactsIntact"]
        self.assertEqual("BLOCKED",evidence.assess(*args)["status"])
    def test_protection_missing_cleanup_proof_blocks(self):
        args=list(self.protection());args[2]["newObservations"].pop()
        self.assertEqual("BLOCKED",evidence.assess(*args)["status"])
    def test_instrumentation_parsed_method_mismatch_blocks(self):
        args=list(self.completed());args[-1]["method"]=METHODS[1]
        self.assertEqual("BLOCKED",evidence.assess(*args)["status"])

    def after_from_instrumentation_bytes(self, raw):
        method, prior, completed, _ = self.completed()
        observed = {f"observation-{index}.json": row for index, row in enumerate(completed["newObservations"])}
        snapshot = {**completed, "stages": completed["newStages"], "observations": observed,
                    "phaseLog": completed["newPhaseLog"]}
        snapshot.pop("instrumentationText", None)
        with tempfile.TemporaryDirectory() as temporary:
            s = Dummy(); s.evidence = Path(temporary)
            directory = s.evidence / "method-00"; directory.mkdir()
            (directory / "instrumentation.txt").write_bytes(raw)
            with patch.object(evidence, "snapshot", return_value=snapshot), patch.object(evidence, "worker_absence", return_value={"pid":202,"startTicks":5000,"absent":True}):
                return method, prior, evidence.after(s, method, prior)
    def test_actual_api26_crlf_instrumentation_bytes_survive_after_collection(self):
        self.assertEqual(873, len(ACTUAL_CRLF_INSTRUMENTATION))
        self.assertEqual("43f01ee4afe4a4cdd19fa0f9e0d07c1e6934e6c9571c7ae73276cfed760f4640", hashlib.sha256(ACTUAL_CRLF_INSTRUMENTATION).hexdigest())
        _, _, current = self.after_from_instrumentation_bytes(ACTUAL_CRLF_INSTRUMENTATION)
        self.assertEqual("PASS", current["status"])
        self.assertEqual(ACTUAL_CRLF_INSTRUMENTATION, current["instrumentationText"].encode("utf-8"))
    def test_actual_api26_crlf_hash_matches_parser_and_lifecycle_assessment(self):
        method, prior, current = self.after_from_instrumentation_bytes(ACTUAL_CRLF_INSTRUMENTATION)
        parsed = parse_instrumentation(ACTUAL_CRLF_INSTRUMENTATION.decode("utf-8"), method)
        self.assertEqual("PASS", parsed["status"])
        self.assertEqual("PASS", evidence.assess(method, prior, current, parsed)["status"])
    def test_invalid_utf8_instrumentation_is_rejected_without_replacement_decode(self):
        _, _, current = self.after_from_instrumentation_bytes(ACTUAL_CRLF_INSTRUMENTATION+b"\xff")
        self.assertEqual("BLOCKED", current["status"])
        self.assertNotIn("instrumentationText", current)
        self.assertIn("utf-8", current["errors"][0])

if __name__=="__main__":unittest.main(verbosity=2)
