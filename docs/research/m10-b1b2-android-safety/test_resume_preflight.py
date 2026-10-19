"""Host regression: real archived short-lived Instrumentation snapshots, not device execution."""
import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import Mock, patch

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import safety_device as s

class ResumePreflightTest(unittest.TestCase):
    def setUp(self):
        self.old = HERE / "evidence/api26-run/method-00"
        self.before = json.loads((self.old / "before.json").read_text(encoding="utf8"))
        self.after = json.loads((self.old / "after.json").read_text(encoding="utf8"))
        self.run = json.loads((self.old / "attempt.json").read_text(encoding="utf8"))["runId"]
        self.boot = "6c697468-6963-4000-8000-000000000001"
        # Explicit HOST-SYNTHETIC report content with REAL archived before/after
        # empty process lists. Actual device JSON is acquired separately by root.
        self.attempt = {"protocol":"M10_B1B2_SAFETY_ATTEMPT_V1", "method":s.METHODS[0],
            "runId":self.run, "api":26, "hostPid":5999, "hostUid":10077,
            "hostStartTicks":123456, "bootId":self.boot, "status":"ATTEMPT_ISSUED",
            "roomOrFormalCoverCommitted":False}
        stage = {"stageRoots":0,"stageMetadataRows":0,"stageMetadataSHA256":hashlib.sha256(b"").hexdigest(),
            "businessOrImageBytesRead":False,"evidenceNamespaceExcluded":"m10-b1b2-safety"}
        absent = {"status":"ABSENT_AT_OBSERVATION","source":"ACTIVITY_MANAGER_SAME_UID",
            "workerRows":0,"externalSupervisorProcVerification":"REQUIRED_SEPARATELY"}
        self.report = {"protocol":"M10_B1B2_PREFLIGHT_V1", "runId":self.run,"method":s.METHODS[0],
            "status":"PASS","noConsumerWork":True,"bootStable":True,"mainRoleAndExactHostInstance":True,
            "stageCreatedByMethod":False,"workerStartedByMethod":False,"before":stage,"after":stage,
            "processesBefore":absent,"processesAfter":absent,"bootIdBefore":self.boot,"bootIdAfter":self.boot,
            "reflectedClasses":[{"class":name,"declaredMethods":1,"scan":"PASS"} for name in sorted(s.REFLECTED_CLASSES)],
            "deviceAdmissionOutsideThisMethod":"REQUIRED"}
    def grouped(self):
        data = {}
        for name,obj in (("attempt.json",self.attempt),("result.json",self.report)):
            raw=json.dumps(obj,separators=(",",":"))+"\n"
            meta=copy.deepcopy(self.after["safety"][self.run]["files"][name])
            # Explicit synthetic report sizes; archive itself remains unchanged.
            meta.update(bytes=len(raw.encode()),sha256=hashlib.sha256(raw.encode()).hexdigest())
            self.after["safety"][self.run]["files"][name]={k:v for k,v in meta.items() if k!="sha256"}
            data["no_backup/m10-b1b2-safety/"+self.run+"/"+name]=(raw,meta)
        return data
    def check(self):
        return s.preflight_records(self.before,self.after,self.run,self.grouped(),"",self.boot)
    def test_real_empty_before_after_host_is_legal_short_instrumentation(self):
        self.assertEqual([],self.before["processes"]["rows"]);self.assertEqual([],self.after["processes"]["rows"])
        proof=self.check();self.assertEqual("PASS",proof["status"])
        self.assertEqual("DIRECT_IN_PROCESS_BEGIN_AND_ASSERT_HOST",proof["hostIdentityEvidence"]["source"])
        self.assertFalse(proof["hostIdentityEvidence"]["externalRunningHostObserved"])
        self.assertEqual("NO_CONSUMER_CREATED_NOT_A_RESOURCE_RELEASE_PROOF",proof["resourceScope"])
    def test_still_live_matching_host_is_supported(self):
        self.after["processes"]["rows"]=[{"pid":5999,"uid":10077,"name":s.APP,"startTicks":123456}]
        self.assertEqual("PASS",self.check()["status"])
    def test_different_live_pid_or_start_ticks_is_rejected(self):
        for field,value in (("pid",6000),("startTicks",123457),("uid",10078),("name",s.APP+":jpeg_worker")):
            self.after["processes"]["rows"]=[{"pid":5999,"uid":10077,"name":s.APP,"startTicks":123456,field:value}]
            with self.assertRaises((ValueError,RuntimeError)):self.check()
    def test_main_assertion_and_identity_fields_cannot_be_guessed(self):
        for field,value in (("hostPid",0),("hostStartTicks",0),("hostUid",10078),("bootId","unknown"),("api",27)):
            old=self.attempt[field];self.attempt[field]=value
            with self.assertRaises((ValueError,RuntimeError)):self.check()
            self.attempt[field]=old
        self.report["mainRoleAndExactHostInstance"]=False
        with self.assertRaises((ValueError,RuntimeError)):self.check()
    def test_boot_drift_rejects_even_if_host_has_disappeared(self):
        self.report["bootIdAfter"]="6c697468-6963-4000-8000-000000000002"
        with self.assertRaises((ValueError,RuntimeError)):self.check()
    def test_missing_reflected_class_or_no_worker_evidence_rejects(self):
        self.report["reflectedClasses"].pop()
        with self.assertRaises((ValueError,RuntimeError)):self.check()
        self.setUp();self.report["processesAfter"]["workerRows"]=1
        with self.assertRaises((ValueError,RuntimeError)):self.check()
    def test_worker_event_or_consumer_namespace_prevents_preflight(self):
        grouped=self.grouped()
        with self.assertRaises((ValueError,RuntimeError)):s.preflight_records(self.before,self.after,self.run,grouped,"LITHIC_JPEG: event",self.boot)
        self.after["stages"]={"unknown":{}}
        with self.assertRaises((ValueError,RuntimeError)):self.check()
    def test_live_file_length_sha_uid_or_inode_conflict_rejects(self):
        for key,value in (("bytes",3),("sha256","a"*64),("uid",10078),("inode",1)):
            grouped=self.grouped();path=next(iter(grouped));raw,meta=grouped[path]
            grouped[path]=(raw,{**meta,key:value})
            with self.assertRaises((ValueError,RuntimeError)):s.preflight_records(self.before,self.after,self.run,grouped,"",self.boot)
    def test_incomplete_external_process_proof_stays_unknown(self):
        for key,value in (("complete",False),("worker_absent",False),("rawSHA256","")):
            old=self.after["processes"][key];self.after["processes"][key]=value
            with self.assertRaises((ValueError,RuntimeError)):self.check()
            self.after["processes"][key]=old
    def test_original_evaluate_replays_real_empty_after_without_requiring_live_host(self):
        grouped=self.grouped()
        with patch.object(s,"read_bundle",return_value=grouped):
            self.assertEqual("PASS",s.evaluate(Mock(),0,self.run,self.before,self.after,"")["status"])
    def test_same_runtime_anchor_required(self):
        self.after["identity"]["processIdentityProof"]["anchor"]["processes"][0]["created"][0]+=1
        with self.assertRaises((ValueError,RuntimeError)):self.check()

    def test_archived_junit_nonce_and_hashes_replay_without_android(self):
        directory, legacy, hashes, phases=s._legacy_preflight()
        self.assertEqual("UNKNOWN",legacy["result.json"]["status"])
        self.assertEqual("PASS",legacy["result.json"]["instrumentation"]["status"])
        self.assertEqual("",phases)
        self.assertEqual("69bf66ee3188886b0998ca670cd6657aad340b7f3b7a8bbe4bfeea6549967369",hashes["instrumentation.txt"])
        self.assertEqual(self.old,directory)
    def temporary_admission(self):
        grouped=self.grouped()
        _,legacy,hashes,phases=s._legacy_preflight()
        legacy=copy.deepcopy(legacy)
        legacy["before.json"]=self.before;legacy["after.json"]=self.after
        temp=tempfile.TemporaryDirectory();self.addCleanup(temp.cleanup)
        root=Path(temp.name);(root/"evidence").mkdir();(root/"home").mkdir()
        obj=Mock(api=26,serial="emulator-5664",evidence=root/"evidence",home=root/"home")
        binding={k:self.before["identity"][k] for k in ("sourceCommit","api","avd","serial","data","base","appSHA256")}
        obj.binding.return_value=binding
        obj.require_proof.side_effect=lambda p:json.loads(p.read_text(encoding="utf8"))
        patcher=patch.object(s,"_legacy_preflight",return_value=(self.old,legacy,hashes,phases));patcher.start();self.addCleanup(patcher.stop)
        row=s.resume_preflight(obj,grouped,copy.deepcopy(self.after),self.boot)
        return obj,row
    def test_new_admission_preserves_unknown_and_contains_no_new_test_command(self):
        obj,row=self.temporary_admission()
        recovered,directory=s.admitted_method(obj,0,s.fresh.TEST_SHA)
        self.assertEqual("UNKNOWN",recovered["originalStatus"])
        self.assertEqual(0,recovered["instrumentationCommandsIssued"])
        self.assertEqual("PASS",recovered["deviceProof"]["status"])
        self.assertEqual(row,recovered);self.assertEqual(self.old,directory)
        obj.adb.assert_not_called()
        self.assertEqual("UNKNOWN",json.loads((self.old/"result.json").read_text(encoding="utf8"))["status"])
    def test_admission_cannot_be_overwritten_or_accepted_after_byte_tampering(self):
        obj,row=self.temporary_admission()
        with self.assertRaises((ValueError,RuntimeError,FileExistsError)):
            s.resume_preflight(obj,self.grouped(),copy.deepcopy(self.after),self.boot)
        path=obj.evidence/"preflight-resume/preflight-attempt.json"
        path.write_bytes(path.read_bytes()+b" ")
        with self.assertRaises((ValueError,RuntimeError)):
            s.admitted_method(obj,0,s.fresh.TEST_SHA)
    def test_admission_pointer_wrong_test_or_hash_rejected(self):
        obj,row=self.temporary_admission()
        with self.assertRaises((ValueError,RuntimeError)):
            s.admitted_method(obj,0,"a"*64)
        pointer=obj.home/"preflight-resume-admission.json"
        parsed=json.loads(pointer.read_text(encoding="utf8"));parsed["sha256"]="a"*64
        pointer.write_text(json.dumps(parsed),encoding="utf8")
        with self.assertRaises((ValueError,RuntimeError)):
            s.admitted_method(obj,0,s.fresh.TEST_SHA)

if __name__ == "__main__":unittest.main(verbosity=2)
