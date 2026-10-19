"""Synthetic interpreter checks only; no Android/SDK/process command is issued."""
import copy
import importlib.util
import json
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location("b1_exact_fault_test", Path(__file__).with_name("fault_device.py"))
s = importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(s)
R = "11111111-1111-4111-8111-111111111111"
T = "22222222-2222-4222-8222-222222222222"
B = "6c697468-6963-4000-8000-000000000001"
U = 10077


def checkpoint(index=0):
    return {"protocol": "M10_B1_EXACT_FAULT_CHECKPOINT_V1", "status": "ARMED_BEFORE_EXACT_CALL_NOT_DEATH_PROOF",
            "runId": R, "kind": s.KINDS[index], "method": s.METHODS[index], "targetProof": T, "bootId": B, "api": 26,
            "host": {"pid": 101, "uid": U, "startTicks": 10}, "worker": {"pid": 202, "uid": U, "startTicks": 20},
            "pinDurable": True, "decodeReceiptCreated": False, "sourceAndOutputSubmittedToRoom": False,
            "killReturnNeverProvesDeath": True,
            "dataFdDelivered": index == 3,
            "owner": {"generation": "m10-b1-fault-" + R, "asset": T, "attempt": R, "session": T, "nonce": R},
            "source": {"device": 11, "inode": 21, "bytes": 59334, "sha256": s.life.SOURCES["SUCCESS"][1]},
            "output": {"device": 11, "inode": 22, "bytes": 0, "sha256": s.life.digest("")},
            "guard": {"device": 11, "inode": 23, "bytes": 0, "sha256": s.life.digest("")}}


def line(phase, emitter=101, attempt=R, worker=202):
    return f"10-19 10:20:30.123 {emitter} 100 I LITHIC_JPEG: {phase} attempt={attempt} session={T} worker={worker}/{U}/20 at=100\n"


def canonical(value): return "".join(f"{k}={value[k]}\n" for k in sorted(value))


def closed_stage(cp, host_ended=False):
    owner = {"protocol": "lithic-m10-jpeg-stage", "version": "2", "generation": cp["owner"]["generation"],
             "assetId": T, "attemptId": R, "sessionId": T, "nonce": R, "guardDevice": "11", "guardInode": "23",
             "decoderPolicy": "libjpeg-turbo-3.2.0-e01-strict-v1", "normalizationPolicy": "m10-jpeg-normalize-v1"}
    records = {"owner.v2": canonical(owner), "consumer.guard": ""}; oh = s.life.digest(records["owner.v2"])
    records["source.v2"] = canonical({"ownerHash": oh, **{k: str(v) for k, v in cp["source"].items()}})
    records["output.v2"] = canonical({"ownerHash": oh, "device": "11", "inode": "22", "bytes": "0", "sha256": s.life.digest("")})
    records["pin.v2"] = canonical({"ownerHash": oh, "sourceHash": cp["source"]["sha256"], **{k: str(v) for k, v in cp["worker"].items()}})
    records["delivered.v2"] = canonical({"ownerHash": oh, "pinHash": s.life.digest(records["pin.v2"])})
    records["supervisor.v2"] = canonical({"ownerHash": oh, "bootId": B, **{k: str(v) for k, v in cp["host"].items()}})
    records["revoked.v2"] = canonical({"ownerHash": oh, "reasonHash": "a" * 64})
    records["abandoned.v2"] = canonical({"version": "1", "ownerHash": oh, "supervisorHash": s.life.digest(records["supervisor.v2"]),
        "pinHash": s.life.digest(records["pin.v2"]), "sourceRecordHash": s.life.digest(records["source.v2"]), "sourceHash": cp["source"]["sha256"],
        "outputDevice": "11", "outputInode": "22", "outputBytes": str(cp["output"]["bytes"]), "outputSha": cp["output"]["sha256"],
        "observedBootId": B, "hostClosure": "ORIGINAL_INSTANCE_ENDED" if host_ended else "SAME_SESSION_QUIESCENT",
        "workerEnded": "true", "hostFdsClosed": "true", "noFutureRpc": "true", "connectionCleaned": "true",
        "noConsumerActorsEnded": "false", "proof": "NO_RESULT_CONSUMER"})
    for name in ("abandoned-cleanup-intent.v2", "abandoned-cleaned.v2"):
        records[name] = canonical({"ownerHash": oh, "abandonedHash": s.life.digest(records["abandoned.v2"])})
    meta = {name: {"type": "regular", "device": 11, "inode": 23 if name == "consumer.guard" else 100 + i,
                   "bytes": len(text), "sha256": s.life.digest(text)} for i, (name, text) in enumerate(records.items())}
    records["__metadata__"] = json.dumps({"complete": True, "entries": meta})
    return {"asset": T, "records": records, "remainingPayloadMetadata": {}}


class ExactFaultHostTest(unittest.TestCase):
    def test_fixed_five_plans_have_exact_permits_no_adb_kill_or_default_traversal(self):
        for index in range(5):
            extra = {"original": T, "end_sha": "b" * 64} if index in (2, 4) else {}
            p = s.plan(index, R, 26, U, B, "a" * 64, T, **extra)
            self.assertEqual(s.METHODS[index], p["arguments"]["class"])
            self.assertEqual("M10_B1B2_EXACT_FAULT_V1", p["arguments"]["m10FaultPermit"])
            self.assertEqual(["shell", "am", "instrument", "-w", "-r"], p["argv"][:5])
            self.assertNotIn("kill", p["argv"])
        with self.assertRaises(RuntimeError): s.plan(5, R, 26, U, B, "a" * 64, T)

    def test_recovery_requires_original_run_and_external_exact_death_reference(self):
        for extra in ({}, {"original": R, "end_sha": "b" * 64}, {"original": T, "end_sha": "UNKNOWN"}):
            with self.assertRaises(RuntimeError): s.plan(2, R, 26, U, B, "a" * 64, T, **extra)

    def test_boot_counter_is_not_defaulted_or_guessed(self):
        self.assertEqual(B, s.boot_epoch("1"))
        for value in ("", "null", "-1", "01", "2147483648", "1\nUNKNOWN"):
            with self.assertRaises(RuntimeError): s.boot_epoch(value)

    def test_host_crash_is_expected_interruption_and_never_junit_pass(self):
        text = (f"INSTRUMENTATION_STATUS: class={s.PREFIX[:-1]}\nINSTRUMENTATION_STATUS: test={s.METHODS[1].split('#')[1]}\n"
                "INSTRUMENTATION_STATUS_CODE: 1\nINSTRUMENTATION_RESULT: shortMsg=Process crashed.\nINSTRUMENTATION_CODE: 0\n")
        self.assertEqual("EXPECTED_INSTRUMENTATION_INTERRUPTION_NOT_PASS", s.expected_host_interruption(text, s.METHODS[1])["status"])
        for bad in (text + "OK (1 test)\n", text.replace("Process crashed.", "Timeout"), text.replace("CODE: 0", "CODE: -1")):
            with self.assertRaises(RuntimeError): s.expected_host_interruption(bad, s.METHODS[1])

    def test_checkpoint_rejects_wrong_uid_boot_boundary_or_locked_source(self):
        cp = checkpoint()
        self.assertEqual(cp, s.checkpoint_identity(cp, 0, R, U, B, T, 26))
        for key, value in (("bootId", "wrong"), ("dataFdDelivered", True), ("api", 36), ("targetProof", R)):
            bad = copy.deepcopy(cp); bad[key] = value
            with self.assertRaises(RuntimeError): s.checkpoint_identity(bad, 0, R, U, B, T, 26)
        bad = copy.deepcopy(cp); bad["worker"]["uid"] += 1
        with self.assertRaises(RuntimeError): s.checkpoint_identity(bad, 0, R, U, B, T, 26)

    def test_private_evidence_adapter_does_not_relax_shared_normal_module(self):
        import safety_device
        original_names = safety_device.SAFETY_NAMES
        module = s.evidence_module()
        self.assertIn("fault-checkpoint.json", module.SAFETY_NAMES)
        self.assertEqual(original_names, safety_device.SAFETY_NAMES)
        self.assertFalse(safety_device.ROOT_RE.fullmatch("m10-b1-fault-" + R))
        self.assertTrue(module.ROOT_RE.fullmatch("m10-b1-fault-" + R))

    def test_worker_fault_needs_actual_unbind_and_death_for_one_identity(self):
        cp = checkpoint()
        raw = line("HOST_BINDER_DIED") + line("HOST_UNBIND_RETURNED")
        self.assertEqual("PASS", s.fault_events(raw, cp, retired=False)["status"])
        for bad in (line("HOST_BINDER_DIED"), raw + line("HOST_UNBIND_RETURNED"), raw.replace("worker=202", "worker=999")):
            with self.assertRaises(RuntimeError): s.fault_events(bad, cp, retired=False)

    def test_host_pre_fd_fault_requires_matched_worker_exit_not_normal_success(self):
        cp = checkpoint(1)
        self.assertEqual("PASS", s.fault_events(line("WORKER_EXIT_HOST_BINDER_DIED", emitter=202), cp, retired=False)["status"])
        with self.assertRaises(RuntimeError): s.fault_events("", cp, retired=False)
        with self.assertRaises(RuntimeError): s.fault_events(line("WORKER_EXIT_HOST_BINDER_DIED", emitter=999), cp, retired=False)

    def test_worker_abandoned_and_host_abandoned_keep_distinct_authority(self):
        cp = checkpoint()
        stage = closed_stage(cp)
        self.assertTrue(s.stage_proof(stage, cp, closed=True, host_ended=False)["abandonedNotDecodeSuccess"])
        with self.assertRaises(RuntimeError): s.stage_proof(stage, cp, closed=True, host_ended=True)
        host = checkpoint(3); host["output"].update(bytes=100, sha256="c" * 64)
        self.assertEqual("PASS", s.stage_proof(closed_stage(host, True), host, closed=True, host_ended=True)["status"])

    def test_unknown_or_successful_receipt_payload_is_not_abandoned(self):
        cp = checkpoint(); stage = closed_stage(cp)
        for name in ("terminal.v2", "prepared.v2", "unknown.tmp"):
            bad = copy.deepcopy(stage); bad["records"][name] = "not allowed\n"
            with self.assertRaises(RuntimeError): s.stage_proof(bad, cp, closed=True, host_ended=False)
        bad = copy.deepcopy(stage); bad["remainingPayloadMetadata"]["source.bin"] = {"bytes": 59334}
        with self.assertRaises(RuntimeError): s.stage_proof(bad, cp, closed=True, host_ended=False)


if __name__ == "__main__": unittest.main()
