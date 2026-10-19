"""Synthetic host decisions and one locked real host input; zero device calls."""
import copy
import importlib.util
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location("b1b2_host_supervise", Path(__file__).with_name("supervise.py"))
s = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(s)


class HostPreconditionTest(unittest.TestCase):
    def setUp(self):
        self.observed = s.read_locked_fixture()
        self.ready_host = copy.deepcopy(self.observed)
        self.ready_host["freeKiB"] = s.REQUIRED_FREE_KIB

    def assertBlocked(self, value, reason):
        result = s.classify_host(value)
        self.assertEqual("BLOCKED", result["status"])
        self.assertIn(reason, result["reasons"])
        self.assertFalse(result["androidAdmission"])
        self.assertEqual({"avdCreate": 0, "emulatorStart": 0, "adb": 0, "apkInstall": 0,
                          "instrumentation": 0}, result["operationCounts"])

    def test_real_locked_input_blocks_memory_without_android_claim(self):
        self.assertEqual(2138072, self.observed["freeKiB"])
        self.assertEqual(2621440, s.REQUIRED_FREE_KIB)
        self.assertBlocked(self.observed, "INSUFFICIENT_FREE_HOST_MEMORY")

    def test_one_kib_below_unchanged_threshold_still_blocks(self):
        self.ready_host["freeKiB"] -= 1
        self.assertBlocked(self.ready_host, "INSUFFICIENT_FREE_HOST_MEMORY")

    def test_threshold_equality_is_host_only_not_android_admission(self):
        result = s.classify_host(self.ready_host)
        self.assertEqual("HOST_PRECONDITIONS_ONLY", result["status"])
        self.assertFalse(result["androidAdmission"])
        self.assertEqual("NOT_RUN", result["processIdentity"])
        self.assertEqual("NOT_RUN", result["androidIdentity"])

    def test_occupied_target_port_blocks(self):
        self.ready_host["ports"] = [{"LocalPort": 5664, "OwningProcess": 123}]
        self.assertBlocked(self.ready_host, "TARGET_PORTS_NOT_PROVEN_EMPTY")

    def test_existing_base_is_not_reused(self):
        self.ready_host["baseExists"] = True
        self.assertBlocked(self.ready_host, "FRESH_BASE_NOT_ABSENT")

    def test_any_existing_registration_blocks(self):
        for key in ("globalNewAPI26RegistrationExists", "globalNewAPI36RegistrationExists"):
            value = copy.deepcopy(self.ready_host)
            value[key] = True
            self.assertBlocked(value, "FRESH_REGISTRATIONS_NOT_ABSENT")

    def test_missing_malformed_or_bool_memory_is_unknown_blocked(self):
        for memory in (None, True, "2621440", -1, 0):
            value = copy.deepcopy(self.ready_host)
            value["freeKiB"] = memory
            self.assertBlocked(value, "MEMORY_OBSERVATION_INVALID")

    def test_old_base_or_wrong_scope_never_inherits_authorization(self):
        for key, value in (("base", "E:/CODEX/.codex/research-env/lithic-m10-android-20261018-v1"),
                           ("scope", "DEVICE_OR_OLD_AVD_SCOPE")):
            observation = copy.deepcopy(self.ready_host)
            observation[key] = value
            self.assertBlocked(observation, "OBSERVATION_SCOPE_CONFLICT")

    def test_missing_port_or_registration_observation_blocks(self):
        for key, reason in (("ports", "TARGET_PORTS_NOT_PROVEN_EMPTY"),
                            ("globalNewAPI26RegistrationExists", "FRESH_REGISTRATIONS_NOT_ABSENT")):
            value = copy.deepcopy(self.ready_host)
            value.pop(key)
            self.assertBlocked(value, reason)

    def test_memory_cannot_exceed_total_observed_memory(self):
        self.ready_host["totalKiB"] = self.ready_host["freeKiB"] - 1
        self.assertBlocked(self.ready_host, "MEMORY_OBSERVATION_INVALID")


if __name__ == "__main__":
    unittest.main()
