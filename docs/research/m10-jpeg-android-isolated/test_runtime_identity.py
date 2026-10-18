"""Pure host identity checks; fixtures never execute the archived commands."""
import base64
import copy
import hashlib
import json
from pathlib import Path
import unittest

import runtime_identity as gate

HERE = Path(__file__).resolve().parent
ARCHIVE = HERE.parent / "m10-jpeg-android-isolated-resume/evidence"


def fixture():
    return json.loads((ARCHIVE / "ending-windows.json").read_text(encoding="utf-8"))


def tokens(command):
    # All paths in these locked fixtures have no whitespace/quoting. Production
    # injects CommandLineToArgvW rather than using this fixture-only tokenizer.
    return command.split()


def spec():
    raw = fixture()
    return {"avd": "Lithic_M10V_API26_20261018", "port": 5624,
            "launcherExe": raw["targets"][0]["ExecutablePath"],
            "qemuExe": raw["targets"][1]["ExecutablePath"],
            "argv": tokens(raw["targets"][0]["CommandLine"])}


def changed(callback):
    value = fixture()
    callback(value)
    return value


class FakeTime:
    def __init__(self, snapshots, elapsed_per_query=0):
        self.value = 0.0
        self.snapshots = list(snapshots)
        self.timeouts = []
        self.elapsed_per_query = elapsed_per_query
    def clock(self):
        return self.value
    def sleep(self, seconds):
        self.value += seconds
    def observe(self, *, timeout):
        self.timeouts.append(timeout)
        self.value += self.elapsed_per_query
        return copy.deepcopy(self.snapshots.pop(0) if len(self.snapshots) > 1 else self.snapshots[0])
    def run(self, anchor=None):
        return gate.await_complete(self.observe, fixture() if anchor is None else anchor,
                                   spec(), tokens, clock=self.clock, sleep=self.sleep)


class RuntimeIdentityTests(unittest.TestCase):
    def classify(self, value):
        return gate.classify(value, fixture(), spec(), tokens)
    def blocked(self, value):
        self.assertEqual("BLOCKED", self.classify(value)["status"])
    def test_actual_ending_snapshot_has_complete_matching_identity(self):
        self.assertEqual("PASS", self.classify(fixture())["status"])
    def test_actual_first_null_input_is_not_backfilled_with_missing_creation_or_ports(self):
        rows = [json.loads(line) for line in (ARCHIVE / "commands.jsonl").read_text().splitlines()]
        observed = []
        for row in rows:
            if "stdoutBase64" not in row:
                continue
            raw = base64.b64decode(row["stdoutBase64"])
            if hashlib.sha256(raw).hexdigest() != row["stdoutSHA256"]:
                raise AssertionError("Archived command output SHA mismatch")
            try:
                value = json.loads(raw)
            except (ValueError, UnicodeError):
                continue
            if any(item.get("ExecutablePath") is None for item in value.get("targets", [])):
                observed.append(value)
        self.assertEqual(1, len(observed))
        self.assertNotIn("CreationDate", observed[0]["targets"][0])
        self.blocked(observed[0])
    def test_complete_new_snapshot_with_null_path_is_pending_not_pass(self):
        value = changed(lambda x: x["targets"][1].update(ExecutablePath=None))
        self.assertEqual("PENDING", self.classify(value)["status"])
    def test_null_then_two_complete_samples_succeeds(self):
        null = changed(lambda x: x["targets"][1].update(ExecutablePath=None))
        fake = FakeTime([null, fixture(), fixture()])
        self.assertEqual("PASS", fake.run()["status"])
        self.assertEqual(3, len(fake.timeouts))
    def test_persistent_null_exhausts_one_fixed_deadline(self):
        null = changed(lambda x: x["targets"][1].update(ExecutablePath=None))
        fake = FakeTime([null])
        result = fake.run()
        self.assertEqual("BLOCKED", result["status"])
        self.assertEqual("IDENTITY_WINDOW_EXHAUSTED", result["reason"])
        self.assertLessEqual(fake.value, 30.0)
    def test_wrong_executable_is_blocked_even_with_expected_command_line(self):
        self.blocked(changed(lambda x: x["targets"][1].update(ExecutablePath="C:/unknown/qemu.exe")))
    def test_command_line_cannot_substitute_for_empty_executable_path(self):
        value = changed(lambda x: x["targets"][0].update(ExecutablePath=""))
        self.assertEqual("PENDING", self.classify(value)["status"])
    def test_process_id_replacement_is_blocked(self):
        self.blocked(changed(lambda x: x["targets"][1].update(ProcessId=32033)))
    def test_creation_time_replacement_is_blocked(self):
        self.blocked(changed(lambda x: x["targets"][1].update(CreationDate="2026-10-18T21:16:38.0504601+08:00")))
    def test_dotnet_utc_seven_digit_creation_time_matches_original_instant(self):
        value = changed(lambda x: x["targets"][1].update(CreationDate="2026-10-18T13:16:38.0504600Z"))
        self.assertEqual("PASS", self.classify(value)["status"])
    def test_parent_child_change_is_blocked(self):
        self.blocked(changed(lambda x: x["targets"][1].update(ParentProcessId=32881)))
    def test_extra_target_qemu_is_blocked(self):
        value = fixture()
        value["targets"].append(dict(value["targets"][1], ProcessId=32033))
        self.blocked(value)
    def test_target_port_owned_by_another_process_is_blocked(self):
        self.blocked(changed(lambda x: x["ports"][0].update(OwningProcess=999)))
    def test_no_listening_port_is_pending_with_complete_process_identity(self):
        value = fixture()
        value["ports"] = []
        self.assertEqual("PENDING", self.classify(value)["status"])
    def test_original_argv_path_change_is_blocked(self):
        self.blocked(changed(lambda x: x["targets"][1].update(CommandLine=x["targets"][1]["CommandLine"].replace("-memory 1536", "-memory 2048"))))
    def test_additional_unknown_argument_is_blocked(self):
        self.blocked(changed(lambda x: x["targets"][1].update(CommandLine=x["targets"][1]["CommandLine"] + " -unknown")))
    def test_missing_required_identity_fields_is_blocked(self):
        self.blocked(changed(lambda x: x["targets"][0].pop("CreationDate")))
    def test_non_loopback_listener_is_blocked(self):
        self.blocked(changed(lambda x: x["ports"][0].update(LocalAddress="0.0.0.0")))
    def test_one_pass_sample_does_not_complete_the_gate(self):
        null = changed(lambda x: x["targets"][1].update(ExecutablePath=None))
        fake = FakeTime([fixture(), null])
        self.assertEqual("BLOCKED", fake.run()["status"])
    def test_conflict_after_one_complete_sample_stops_without_further_observation(self):
        conflict = changed(lambda x: x["ports"][0].update(OwningProcess=999))
        fake = FakeTime([fixture(), conflict, fixture()])
        self.assertEqual("BLOCKED", fake.run()["status"])
        self.assertEqual(2, len(fake.timeouts))
    def test_query_timeout_uses_remaining_total_budget(self):
        null = changed(lambda x: x["targets"][1].update(ExecutablePath=None))
        fake = FakeTime([null], elapsed_per_query=3)
        self.assertEqual("BLOCKED", fake.run()["status"])
        self.assertTrue(all(0 < value <= 30 for value in fake.timeouts))
        self.assertLess(fake.timeouts[-1], fake.timeouts[0])
    def test_collector_returning_after_total_deadline_cannot_pass(self):
        fake = FakeTime([fixture()], elapsed_per_query=31)
        self.assertEqual("BLOCKED", fake.run()["status"])
        self.assertEqual(1, len(fake.timeouts))
    def test_fresh_start_locks_first_observed_complete_process_identity(self):
        null = changed(lambda x: x["targets"][1].update(ExecutablePath=None))
        replacement = changed(lambda x: x["targets"][1].update(CreationDate="2026-10-18T13:16:38.0504601Z"))
        fake = FakeTime([null, replacement])
        result = gate.await_complete(fake.observe, None, spec(), tokens, clock=fake.clock, sleep=fake.sleep)
        self.assertEqual("BLOCKED", result["status"])
        self.assertEqual(2, len(fake.timeouts))
    def test_fresh_anchor_requires_creation_information_before_waiting(self):
        value = changed(lambda x: x["targets"][1].pop("CreationDate"))
        self.assertEqual("BLOCKED", gate.classify(value, None, spec(), tokens)["status"])
    def test_outer_timeout_can_only_shorten_the_identity_window(self):
        null = changed(lambda x: x["targets"][1].update(ExecutablePath=None))
        fake = FakeTime([null])
        result = gate.await_complete(fake.observe, fixture(), spec(), tokens, timeout=3,
                                    clock=fake.clock, sleep=fake.sleep)
        self.assertEqual("BLOCKED", result["status"])
        self.assertEqual(3, fake.value)
        self.assertEqual(3, result["windowSeconds"])
        for timeout in (0, -1, 31, float("inf"), float("nan"), True):
            result = gate.await_complete(fake.observe, fixture(), spec(), tokens, timeout=timeout,
                                        clock=fake.clock, sleep=fake.sleep)
            self.assertEqual("IDENTITY_WINDOW_ARGUMENT_INVALID", result["reason"])
    def test_argument_parser_failure_is_fail_closed(self):
        def reject(_):
            raise ValueError("malformed Windows arguments")
        self.assertEqual("BLOCKED", gate.classify(fixture(), fixture(), spec(), reject)["status"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
