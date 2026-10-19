"""Pure host regression: never runs an SDK, emulator, ADB or a process kill."""
import importlib.util
import copy
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock, patch

SPEC = importlib.util.spec_from_file_location("b1b2_resume_exit", Path(__file__).with_name("fresh_environment.py"))
s = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(s)


class ResumeExitTest(unittest.TestCase):
    def test_explicit_gated_normal_exit_entry_exists(self):
        self.assertIn("safe-exit", s.OPERATIONS)

    def anchor_and_spec(self):
        argv = ["C:/isolated/emulator/emulator.exe", "-avd", "Lithic_M10B1B2_API26_20261019", "-port", "5664"]
        launcher = {"role": "launcher", "pid": 400, "parentPid": 200, "name": "emulator.exe",
                    "created": s.runtime_identity._instant("2026-10-19T08:00:00Z"),
                    "expectedExecutable": s.runtime_identity._path(argv[0])}
        worker = {"role": "qemu", "pid": 500, "parentPid": 400, "name": "qemu-system-x86_64-headless.exe",
                  "created": s.runtime_identity._instant("2026-10-19T08:00:01Z"),
                  "expectedExecutable": s.runtime_identity._path("C:/isolated/emulator/qemu/windows-x86_64/qemu-system-x86_64-headless.exe")}
        return {"version": 1, "processes": [launcher, worker]}, {"port": 5664, "argv": argv}

    def snapshot(self, roles=(), ports=False):
        anchor, spec = self.anchor_and_spec()
        rows = []
        for old in anchor["processes"]:
            if old["role"] not in roles: continue
            argv = [old["expectedExecutable"], *spec["argv"][1:]]
            rows.append({"ProcessId": old["pid"], "ParentProcessId": old["parentPid"], "Name": old["name"],
                         "ExecutablePath": old["expectedExecutable"], "CreationDate": "2026-10-19T08:00:0" + ("0" if old["role"] == "launcher" else "1") + "Z",
                         "CommandLine": " ".join(argv)})
        return {"targets": rows, "ports": [{"LocalAddress": "127.0.0.1", "LocalPort": 5664, "OwningProcess": 500}] if ports else []}

    def classify(self, row):
        return s.classify_exit_observation(row, *self.anchor_and_spec())

    def test_absent_processes_and_ports_is_pass(self):
        self.assertEqual("PASS", self.classify(self.snapshot())["status"])

    def test_live_anchored_process_is_pending_not_ended(self):
        result = self.classify(self.snapshot(("qemu",), True))
        self.assertEqual("PENDING", result["status"])
        self.assertFalse(result["osProcessEnded"])
        self.assertFalse(result["portsEnded"])

    def test_missing_port_evidence_is_unknown(self):
        self.assertEqual("UNKNOWN", self.classify({"targets": []})["status"])

    def test_pid_reuse_with_other_creation_is_unknown(self):
        snapshot = self.snapshot(("qemu",))
        snapshot["targets"][0]["CreationDate"] = "2026-10-19T09:00:00Z"
        self.assertEqual("UNKNOWN", self.classify(snapshot)["status"])

    def test_foreign_child_is_unknown(self):
        snapshot = self.snapshot(("qemu",))
        snapshot["targets"][0]["ProcessId"] = 501
        self.assertEqual("UNKNOWN", self.classify(snapshot)["status"])

    def test_null_executable_does_not_count_as_dead(self):
        snapshot = self.snapshot(("qemu",))
        snapshot["targets"][0]["ExecutablePath"] = None
        self.assertEqual("UNKNOWN", self.classify(snapshot)["status"])

    def test_wrong_port_owner_is_unknown(self):
        snapshot = self.snapshot(ports=True)
        snapshot["ports"][0]["OwningProcess"] = 900
        self.assertEqual("UNKNOWN", self.classify(snapshot)["status"])

    def test_duplicate_port_is_unknown(self):
        snapshot = self.snapshot(ports=True)
        snapshot["ports"].append(copy.deepcopy(snapshot["ports"][0]))
        self.assertEqual("UNKNOWN", self.classify(snapshot)["status"])

    def test_changed_parent_or_arguments_is_unknown(self):
        for key, value in (("ParentProcessId", 401), ("CommandLine", "qemu --another-avd")):
            snapshot = self.snapshot(("qemu",))
            snapshot["targets"][0][key] = value
            self.assertEqual("UNKNOWN", self.classify(snapshot)["status"])

    def supervisor(self, home):
        value = s.FreshSupervisor.__new__(s.FreshSupervisor)
        value.api, value.serial, value.port, value.name = 26, "emulator-5664", 5664, "Lithic_M10B1B2_API26_20261019"
        value.home = home
        value.scope = Mock()
        value.binding = Mock(return_value={"api": 26, "serial": value.serial, "sourceCommit": s.SOURCE})
        value._exit_authorization = Mock(return_value={"normalMethods": 7, "faultOperations": 5, "references": [], "faults": []})
        value._exit_consumers_safe = Mock()
        value.marker = Mock()
        value.note = Mock()
        value.validate_process = Mock()
        value.process_proof = {"anchor": self.anchor_and_spec()[0]}
        value.process_spec = Mock(return_value=self.anchor_and_spec()[1])
        value.processes = Mock(return_value=self.snapshot())
        value.run = Mock(return_value="OK: killing emulator, bye bye\nOK")
        value.adb = Mock(return_value="1")
        return value

    def safe_snapshot(self):
        return {"identity": {"status": "PASS"}, "uid": 10077, "testUID": 10078,
                "stages": {}, "observations": {}, "diagnostics": {}, "safety": {}}

    def fault_adapter(self, *snapshots):
        import fault_device
        adapter = SimpleNamespace(snapshot=Mock(side_effect=list(snapshots) if snapshots else [self.safe_snapshot(), self.safe_snapshot()]))
        return patch.object(fault_device, "evidence_module", return_value=adapter)

    def test_missing_authorization_never_marks_or_quits(self):
        with tempfile.TemporaryDirectory() as directory:
            value = self.supervisor(Path(directory))
            value._exit_authorization.side_effect = RuntimeError("method-02 UNKNOWN")
            with self.assertRaises(RuntimeError): value.safe_exit()
            value.marker.assert_not_called(); value.run.assert_not_called()

    def test_unknown_consumer_never_marks_or_quits(self):
        with tempfile.TemporaryDirectory() as directory, self.fault_adapter():
            value = self.supervisor(Path(directory))
            value._exit_consumers_safe.side_effect = RuntimeError("consumer UNKNOWN")
            with self.assertRaises(RuntimeError): value.safe_exit()
            value.marker.assert_not_called(); value.run.assert_not_called()

    def test_two_consumer_snapshots_must_match(self):
        first, second = self.safe_snapshot(), self.safe_snapshot()
        second["safety"] = {"unexpected": {}}
        with tempfile.TemporaryDirectory() as directory, self.fault_adapter(first, second):
            value = self.supervisor(Path(directory))
            with self.assertRaises(RuntimeError): value.safe_exit()
            value.run.assert_not_called()

    def test_console_ack_does_not_replace_process_and_port_end(self):
        with tempfile.TemporaryDirectory() as directory, self.fault_adapter(), patch.object(s.time, "monotonic", side_effect=[0, 0, 1, 1, 1, 31]), patch.object(s.time, "sleep"):
            value = self.supervisor(Path(directory))
            value.processes.return_value = self.snapshot(("qemu",), True)
            with self.assertRaises(RuntimeError): value.safe_exit()
            value.run.assert_called_once()
            self.assertEqual("UNKNOWN", json.loads((Path(directory) / "safety-exit-result.json").read_bytes())["status"])
            self.assertFalse((Path(directory) / "safety-exit-proof.json").exists())

    def test_unknown_console_reply_is_not_retried(self):
        with tempfile.TemporaryDirectory() as directory, self.fault_adapter():
            value = self.supervisor(Path(directory)); value.run.return_value = "connection closed"
            with self.assertRaises(RuntimeError): value.safe_exit()
            value.run.assert_called_once()
            value.processes.assert_not_called()
            self.assertFalse((Path(directory) / "safety-exit-proof.json").exists())

    def test_two_real_ended_observations_publish_exit_proof(self):
        with tempfile.TemporaryDirectory() as directory, self.fault_adapter(), patch.object(s.time, "sleep"):
            value = self.supervisor(Path(directory))
            result = value.safe_exit()
            self.assertEqual("PASS", result["status"])
            self.assertEqual(2, value.processes.call_count)
            self.assertEqual(2, len(result["endedObservations"]))
            self.assertEqual("400,500", value.historical_process_ids)
            value.marker.assert_called_once_with("safe-exit")
            command = value.run.call_args.args[0]
            self.assertEqual(["-s", "emulator-5664", "emu", "kill"], command[1:])
            self.assertEqual(15, value.run.call_args.kwargs["timeout"])
            self.assertEqual(0, result["androidKillCommandsIssued"])
            self.assertTrue((Path(directory) / "safety-exit-proof.json").is_file())

    def test_port_replacement_after_quit_stays_unknown(self):
        with tempfile.TemporaryDirectory() as directory, self.fault_adapter():
            value = self.supervisor(Path(directory))
            row = self.snapshot(ports=True); row["ports"][0]["OwningProcess"] = 777
            value.processes.return_value = row
            with self.assertRaises(RuntimeError): value.safe_exit()
            value.run.assert_called_once()
            self.assertFalse((Path(directory) / "safety-exit-proof.json").exists())

    def test_old_target_is_rejected_before_authorization(self):
        with tempfile.TemporaryDirectory() as directory:
            value = self.supervisor(Path(directory)); value.serial = "emulator-5596"
            with self.assertRaises(RuntimeError): value.safe_exit()
            value._exit_authorization.assert_not_called(); value.run.assert_not_called()

    def test_public_adb_cannot_bypass_conditional_exit(self):
        value = s.FreshSupervisor.__new__(s.FreshSupervisor)
        value.scope = Mock(); value.serial = "emulator-5664"; value.run = Mock()
        with self.assertRaises(RuntimeError): value.adb("emu", "kill")
        value.run.assert_not_called()

    def test_legacy_boolean_only_exit_proof_does_not_open_api36(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(s, "BASE", Path(directory)):
            home = Path(directory) / "api26"; home.mkdir()
            (home / "safety-exit-proof.json").write_text(json.dumps({"sourceCommit": s.SOURCE, "status": "PASS", "serial": "emulator-5664", "api": 26,
                **{key: True for key in ("allAuthorizedMethodsPassed", "consumersSafe", "osProcessEnded", "portsEnded", "normalExitObserved")}}))
            value = s.FreshSupervisor.__new__(s.FreshSupervisor); value.api = 36
            with self.assertRaises(RuntimeError): value.api26_exit_gate()

    def test_full_exit_proof_opens_api36_but_before_tamper_blocks(self):
        with tempfile.TemporaryDirectory() as directory, self.fault_adapter(), patch.object(s.time, "sleep"), patch.object(s, "BASE", Path(directory)):
            home = Path(directory) / "api26"; home.mkdir()
            value = self.supervisor(home)
            value.binding.return_value.update(avd=value.name, base=str(s.BASE),
                data=str(home / "data" / (value.name + ".avd")), appSHA256=s.APP_SHA)
            value.safe_exit()
            target = s.FreshSupervisor.__new__(s.FreshSupervisor); target.api = 36
            target.api26_exit_gate()
            with (home / "safety-exit-before.json").open("ab") as stream: stream.write(b" ")
            with self.assertRaises(RuntimeError): target.api26_exit_gate()

    def test_incomplete_empty_process_list_is_not_consumer_proof(self):
        row = {"identity": {"status": "PASS"}, "appSHA256": s.APP_SHA, "testSHA256": s.TEST_SHA,
               "uid": 10077, "testUID": 10078, "processes": {"rows": []}}
        with self.assertRaises(RuntimeError): s.FreshSupervisor._exit_consumers_safe(row, {})

    def test_boot_epoch_conflict_blocks_before_console_quit(self):
        with tempfile.TemporaryDirectory() as directory, self.fault_adapter():
            value = self.supervisor(Path(directory))
            value._exit_authorization.return_value["faults"] = [{"checkpoint": {"bootId": "different"}}]
            with self.assertRaises(RuntimeError): value.safe_exit()
            value.run.assert_not_called(); value.marker.assert_not_called()

    def test_post_quit_observation_over_deadline_stays_unknown(self):
        with tempfile.TemporaryDirectory() as directory, self.fault_adapter(), patch.object(s.time, "monotonic", side_effect=[0, 0, 1, 31]):
            value = self.supervisor(Path(directory))
            with self.assertRaises(RuntimeError): value.safe_exit()
            value.run.assert_called_once()
            self.assertFalse((Path(directory) / "safety-exit-proof.json").exists())

    def test_exclusive_pre_exit_record_prevents_second_console_command(self):
        with tempfile.TemporaryDirectory() as directory, self.fault_adapter(), patch.object(s.time, "sleep"):
            value = self.supervisor(Path(directory)); value.safe_exit()
            with self.fault_adapter():
                with self.assertRaises(FileExistsError): value.safe_exit()
            value.run.assert_called_once()

    def authorization_fixture(self, directory):
        import safety_device as normal
        import fault_device as fault
        value = self.supervisor(Path(directory)); value.evidence = Path(directory)
        rows = []
        for index, method in enumerate(normal.METHODS):
            row = {**value.binding(), "index": index, "method": method, "appSHA256": s.APP_SHA,
                   "testSHA256": s.TEST_SHA, "status": "PASS", "capture": {"status": "PASS"},
                   "instrumentation": {"status": "PASS"}, "deviceProof": {"status": "PASS"}}
            rows.append((row, Path(directory) / f"normal-{index}"))
        for index, method in enumerate(fault.METHODS):
            group = 0 if index == 0 else (1 if index < 3 else 3)
            folder = value.evidence / "exact-fault-methods" / f"operation-{index:02d}"; folder.mkdir(parents=True)
            row = {**value.binding(), "index": index, "method": method, "kind": fault.KINDS[index],
                   "appSHA256": s.APP_SHA, "testSHA256": s.TEST_SHA, "instrumentationCommandsIssued": 1,
                   "status": "EXPECTED_INTERRUPTION_AWAITING_RECOVERY" if index in (1, 3) else "PASS",
                   "capture": {"status": "PASS"}, "instrumentation": {"status": "EXPECTED_INSTRUMENTATION_INTERRUPTION_NOT_PASS" if index in (1, 3) else "PASS"},
                   "processEndProof": {"status": "PASS"}, "stageProof": {"status": "PASS", "closed": index not in (1, 3)},
                   "checkpoint": {"owner": {"generation": "group-" + str(group)}},
                   "targetProof": "target-" + str(group), "runId": "run-" + str(index)}
            (folder / "result.json").write_text(json.dumps(row))
        (value.evidence / "exact-fault-methods/operation-04/after.json").write_text("{}")
        # The real authorizer is exercised; only device-free log authenticity and
        # normal admission reads are supplied by synthetic dependencies.
        return value, rows, normal

    def test_all_twelve_authorizations_include_separate_host_recovery(self):
        with tempfile.TemporaryDirectory() as directory:
            value, rows, normal = self.authorization_fixture(directory)
            with patch.object(normal, "admitted_method", side_effect=lambda supervisor, index, sha: rows[index]), patch.object(s, "capture_module", return_value=SimpleNamespace(read_verified=Mock(return_value=({"status": "PASS"}, "")))):
                result = s.FreshSupervisor._exit_authorization(value)
            self.assertEqual(12, len(result["references"]))
            self.assertTrue(result["hostInterruptionsAreNotJUnitPass"])

    def test_any_normal_unknown_blocks_exit_authorization(self):
        with tempfile.TemporaryDirectory() as directory:
            value, rows, normal = self.authorization_fixture(directory)
            rows[3][0]["status"] = "UNKNOWN"
            with patch.object(normal, "admitted_method", side_effect=lambda supervisor, index, sha: rows[index]), patch.object(s, "capture_module", return_value=SimpleNamespace(read_verified=Mock(return_value=({"status": "PASS"}, "")))):
                with self.assertRaises(RuntimeError): s.FreshSupervisor._exit_authorization(value)

    def test_host_interruption_without_matching_recovery_blocks(self):
        with tempfile.TemporaryDirectory() as directory:
            value, rows, normal = self.authorization_fixture(directory)
            path = value.evidence / "exact-fault-methods/operation-04/result.json"
            row = json.loads(path.read_text()); row["targetProof"] = "another-target"; path.write_text(json.dumps(row))
            with patch.object(normal, "admitted_method", side_effect=lambda supervisor, index, sha: rows[index]), patch.object(s, "capture_module", return_value=SimpleNamespace(read_verified=Mock(return_value=({"status": "PASS"}, "")))):
                with self.assertRaises(RuntimeError): s.FreshSupervisor._exit_authorization(value)

    def test_fault_unknown_or_unclosed_resource_never_authorizes_exit(self):
        with tempfile.TemporaryDirectory() as directory:
            value, rows, normal = self.authorization_fixture(directory)
            path = value.evidence / "exact-fault-methods/operation-00/result.json"
            row = json.loads(path.read_text()); row["stageProof"]["closed"] = False; path.write_text(json.dumps(row))
            with patch.object(normal, "admitted_method", side_effect=lambda supervisor, index, sha: rows[index]), patch.object(s, "capture_module", return_value=SimpleNamespace(read_verified=Mock(return_value=({"status": "PASS"}, "")))):
                with self.assertRaises(RuntimeError): s.FreshSupervisor._exit_authorization(value)


if __name__ == "__main__":
    unittest.main()
