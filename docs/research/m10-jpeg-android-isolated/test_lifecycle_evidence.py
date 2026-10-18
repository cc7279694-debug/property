"""Synthetic interpreter fixtures; none constitute an Android execution PASS."""
import copy
import hashlib
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
from lifecycle_evidence import validate_lifecycle

def sha(text):
    return hashlib.sha256(text.encode("ascii")).hexdigest()

def record(**values):
    return "".join(f"{key}={value}\n" for key, value in sorted(values.items()))

def fixture(outcome="SUCCESS"):
    owner = record(protocol="lithic-m10-jpeg-stage", version="2", generation="m10-worker-review-generation",
                   assetId="00000000-0000-0000-0000-000000000001", attemptId="00000000-0000-0000-0000-000000000002",
                   sessionId="00000000-0000-0000-0000-000000000003", nonce="00000000-0000-0000-0000-000000000004",
                   guardDevice="7", guardInode="90", decoderPolicy="libjpeg-turbo-3.2.0-e01-strict-v1",
                   normalizationPolicy="m10-jpeg-normalize-v1")
    owner_hash = sha(owner)
    healthy = outcome == "SUCCESS"
    source_sha = "2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907" if healthy else "f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a"
    source = record(ownerHash=owner_hash, device="7", inode="91", bytes="59334" if healthy else "20194", sha256=source_sha)
    output = record(ownerHash=owner_hash, device="7", inode="92", bytes="0", sha256=sha(""))
    pin = record(ownerHash=owner_hash, sourceHash=source_sha, pid="202", uid="10088", startTicks="5000")
    terminal = record(ownerHash=owner_hash, pinHash=sha(pin), sourceHash=source_sha,
                      outcome="SUCCESS" if healthy else "IMAGE", resultCount="1",
                      sourceFdClosed="true", outputFdClosed="true", binderDead="true", matchingProcessDead="true",
                      connectionCleaned="true", strictFinished="true" if healthy else "false", decoderDestroyed="true",
                      outputDevice="7" if healthy else "none", outputInode="92" if healthy else "none",
                      outputBytes="1000" if healthy else "none", outputSha="a"*64 if healthy else "none",
                      width="256" if healthy else "none", height="256" if healthy else "none")
    proof = record(ownerHash=owner_hash, terminalHash=sha(terminal))
    records = {"owner.v2":owner, "source.v2":source, "output.v2":output,
               "pin.v2":pin, "delivered.v2":record(ownerHash=owner_hash,pinHash=sha(pin)),
               "terminal.v2":terminal, "revoked.v2":record(ownerHash=owner_hash,reasonHash="b"*64),
               "cleanup-intent.v2":proof, "cleaned.v2":proof, "consumer.guard":""}
    if healthy:
        records["prepared.v2"] = proof
    entries = {name:{"type":"regular", "device":7, "inode":90 if name=="consumer.guard" else 100+i,
                     "bytes":len(text.encode("ascii")), "sha256":sha(text)}
               for i,(name,text) in enumerate(records.items())}
    records["__metadata__"] = json.dumps({"complete":True,"entries":entries})
    observation = {"generation":"m10-worker-review-generation", "attempt":"00000000-0000-0000-0000-000000000002",
                   "session":"00000000-0000-0000-0000-000000000003", "outcome":outcome,
                   "hostPid":101,"hostUid":10088,"workerPid":202,"workerUid":10088,"workerStartTicks":5000,
                   "resultCount":1,"pinBeforeDelivery":True,"sourceClosed":True,"outputClosed":True,
                   "connectionCleaned":True,"binderDead":True,"matchingProcessDead":True,"guardReleased":True,
                   "strictFinished":healthy,"decoderDestroyed":True,"safeCleanup":False,
                   "detail":"STRICT_FINISHED_NORMALIZED" if healthy else "STRICT_JPEG_REJECTED"}
    terminal_observation = dict(observation, safeCleanup=True)
    observations = [{"name":"worker-review-result","value":observation},
                    {"name":"worker-review-terminal","value":terminal_observation}]
    phases = ["WORKER_RETIRE_ARMED","HOST_RETIRE_ACKNOWLEDGED","HOST_UNBIND_RETURNED",
              "WORKER_ON_UNBIND","WORKER_EXIT_AUTHENTICATED_DETACH","HOST_BINDER_DIED"]
    lines=[]
    for i,name in enumerate(phases):
        pid = 101 if name.startswith("HOST_") else 202
        lines.append(f"10-18 20:00:00.{i:03d} {pid:5d} {pid:5d} I LITHIC_JPEG: {name} attempt={observation['attempt']} session={observation['session']} worker=202/10088/5000 at={100+i}")
    instrumentation = "\n".join(["INSTRUMENTATION_STATUS: class=com.cooper.lithic.data.media.M10WorkerRetirementReviewTest",
        "INSTRUMENTATION_STATUS: test="+("healthy_jpeg_retires_real_worker_before_stage_completion" if healthy else "locked_damaged_jpeg_is_rejected_and_real_worker_safely_retires"),
        "INSTRUMENTATION_STATUS_CODE: 1", "INSTRUMENTATION_STATUS_CODE: 0", "OK (1 test)", "INSTRUMENTATION_CODE: -1"])
    process={"complete":True,"uid":10088,"rows":[{"pid":101,"uid":10088,"name":"com.cooper.lithic.debug","startTicks":4000}],
             "worker_absent":True,"worker_proc":{"pid":202,"startTicks":5000,"absent":True}}
    return [outcome,instrumentation,observations,"\n".join(lines),records,json.dumps(process)]

class LifecycleEvidenceTest(unittest.TestCase):
    def check(self, args, expected):
        result=validate_lifecycle(*args)
        self.assertEqual(expected,result["status"],result)
        return result

    def test_complete_healthy_synthetic_evidence_passes_interpreter(self): self.check(fixture(),"PASS")
    def test_complete_damaged_synthetic_evidence_passes_interpreter(self): self.check(fixture("INVALID_IMAGE"),"PASS")
    def test_claim_levels_do_not_invent_independent_binder_or_ro_write(self):
        result=self.check(fixture(),"PASS")
        self.assertEqual("EXECUTED_CODE_INVARIANT_ONLY",result["evidence_levels"]["synchronous_hello"])
        self.assertEqual("NOT_RUN",result["evidence_levels"]["write_to_read_only_source"])
    def test_missing_result_flag_is_blocked(self):
        args=fixture();del args[2][0]["value"]["sourceClosed"];self.check(args,"BLOCKED")
    def test_wrong_uid_is_blocked(self):
        args=fixture();args[2][0]["value"]["workerUid"]=10089;self.check(args,"BLOCKED")
    def test_same_host_worker_pid_is_blocked(self):
        args=fixture();args[2][0]["value"]["workerPid"]=101;self.check(args,"BLOCKED")
    def test_duplicate_result_is_blocked(self):
        args=fixture();args[2].append(copy.deepcopy(args[2][0]));self.check(args,"BLOCKED")
    def test_abandoned_result_is_blocked(self):
        args=fixture();args[2].append({"name":"worker-review-abandoned","value":{}});self.check(args,"BLOCKED")
    def test_observation_identity_change_is_blocked(self):
        args=fixture();args[2][1]["value"]["session"]="other";self.check(args,"BLOCKED")
    def test_nonempty_failure_detail_is_not_silently_discarded(self):
        args=fixture();args[2][0]["value"]["detail"]="REVOKE_ACK_UNKNOWN";self.check(args,"BLOCKED")
    def test_missing_phase_is_blocked(self):
        args=fixture();args[3]="\n".join(args[3].splitlines()[:-1]);self.check(args,"BLOCKED")
    def test_duplicate_phase_is_blocked(self):
        args=fixture();args[3]+="\n"+args[3].splitlines()[0];self.check(args,"BLOCKED")
    def test_wrong_log_emitter_is_blocked(self):
        args=fixture();args[3]=args[3].replace("  202   202 I", "  777   777 I",1);self.check(args,"BLOCKED")
    def test_late_host_unbind_log_can_follow_worker_death(self):
        args=fixture();lines=args[3].splitlines();line=lines.pop(2).replace("at=102","at=106");lines.append(line);args[3]="\n".join(lines);self.check(args,"PASS")
    def test_ack_after_worker_detach_is_blocked(self):
        args=fixture();args[3]=args[3].replace("at=101","at=104");self.check(args,"BLOCKED")
    def test_forbidden_worker_event_is_blocked(self):
        args=fixture();args[3]+="\n"+args[3].splitlines()[0].replace("WORKER_RETIRE_ARMED","WORKER_REVOKE_ARMED");self.check(args,"BLOCKED")
    def test_unknown_stage_child_is_blocked(self):
        args=fixture();args[4]["unknown.txt"]="unknown";self.check(args,"BLOCKED")
    def test_source_payload_still_present_is_blocked(self):
        args=fixture();args[4]["source.bin"]="not deleted";self.check(args,"BLOCKED")
    def test_wrong_record_hash_is_blocked(self):
        args=fixture();args[4]["pin.v2"]=args[4]["pin.v2"].replace("sourceHash=2","sourceHash=9");self.check(args,"BLOCKED")
    def test_wrong_locked_source_hash_is_blocked_even_with_rehashed_metadata(self):
        args=fixture();args[4]["source.v2"]=args[4]["source.v2"].replace("sha256=2","sha256=9")
        meta=json.loads(args[4]["__metadata__"])
        meta["entries"]["source.v2"]["sha256"]=sha(args[4]["source.v2"])
        args[4]["__metadata__"]=json.dumps(meta)
        result=self.check(args,"BLOCKED")
        self.assertIn("Locked source size/SHA mismatch",result["errors"])
    def test_duplicate_stage_key_is_blocked(self):
        args=fixture();args[4]["owner.v2"]+="version=2\n";self.check(args,"BLOCKED")
    def test_missing_guard_inode_metadata_is_blocked(self):
        args=fixture();del args[4]["__metadata__"];self.check(args,"BLOCKED")
    def test_wrong_guard_inode_is_blocked(self):
        args=fixture();meta=json.loads(args[4]["__metadata__"]);meta["entries"]["consumer.guard"]["inode"]=999;args[4]["__metadata__"]=json.dumps(meta);self.check(args,"BLOCKED")
    def test_worker_still_running_is_blocked(self):
        args=fixture();proc=json.loads(args[5]);proc["rows"].append({"pid":202,"uid":10088,"name":"com.cooper.lithic.debug:jpeg_worker","startTicks":5000});args[5]=json.dumps(proc);self.check(args,"BLOCKED")
    def test_unknown_same_uid_process_is_blocked(self):
        args=fixture();proc=json.loads(args[5]);proc["rows"].append({"pid":333,"uid":10088,"name":"unknown","startTicks":5000});args[5]=json.dumps(proc);self.check(args,"BLOCKED")
    def test_proc_absence_not_inferred_from_ps_alone(self):
        args=fixture();proc=json.loads(args[5]);del proc["worker_proc"];args[5]=json.dumps(proc);self.check(args,"BLOCKED")
    def test_instrumentation_exit_zero_alone_is_blocked(self):
        args=fixture();args[1]="exitCode=0";self.check(args,"BLOCKED")
    def test_instrumentation_failure_or_crash_is_blocked(self):
        for suffix in ("INSTRUMENTATION_STATUS_CODE: -2", "INSTRUMENTATION_FAILED: crashed", "FAILURES!!!", "Process crashed.", "TIMEOUT"):
            with self.subTest(suffix=suffix):
                args=fixture();args[1]+="\n"+suffix;self.check(args,"BLOCKED")

if __name__=="__main__": unittest.main(verbosity=2)
