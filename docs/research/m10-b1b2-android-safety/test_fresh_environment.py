"""Only synthetic host paths/commands; never invokes Android or SDK tools."""
import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch

SPEC = importlib.util.spec_from_file_location("fresh_b1b2_environment", Path(__file__).with_name("fresh_environment.py"))
s = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(s)


class FreshEnvironmentTest(unittest.TestCase):
    def test_both_profiles_have_only_new_roots_names_and_serials(self):
        for api, port in ((26, 5664), (36, 5666)):
            p = s.profile(api)
            self.assertEqual(f"Lithic_M10B1B2_API{api}_20261019", p["name"])
            self.assertEqual(port, p["port"])
            self.assertEqual(f"emulator-{port}", p["serial"])
            self.assertEqual(s.BASE / f"api{api}", p["home"])

    def make_supervisor(self):
        item = s.FreshSupervisor.__new__(s.FreshSupervisor)
        p = s.profile(26)
        item.api, item.name, item.port, item.serial, item.sdk, item.home = (
            p[x] for x in ("api", "name", "port", "serial", "sdk", "home"))
        item.registry = item.home / "avds"
        item.data = item.home / "data" / (item.name + ".avd")
        item.run = Mock(return_value="device")
        return item

    def test_new_targeted_adb_uses_only_5664(self):
        item = self.make_supervisor()
        self.assertEqual("device", item.adb("get-state"))
        self.assertEqual(["-s", "emulator-5664", "get-state"], item.run.call_args.args[0][1:])

    def test_old_and_unknown_serials_rejected_before_command(self):
        item = self.make_supervisor()
        for serial in ("emulator-5596", "emulator-5612", "emulator-5624", "emulator-5626", "emulator-5668"):
            item.serial = serial
            with self.assertRaises(RuntimeError): item.adb("get-state")
        item.run.assert_not_called()

    def test_child_environment_removes_old_home_without_global_change(self):
        item = self.make_supervisor()
        with patch.dict(os.environ, {"ANDROID_SDK_HOME": "old", "ANDROID_SERIAL": "emulator-5596"}, clear=True):
            child = item.environment()
            self.assertNotIn("ANDROID_SDK_HOME", child)
            self.assertEqual("emulator-5664", child["ANDROID_SERIAL"])
            self.assertEqual(str(item.registry), child["ANDROID_AVD_HOME"])
            self.assertEqual("old", os.environ["ANDROID_SDK_HOME"])

    def test_fresh_marker_is_exclusive_bound_and_cannot_reset(self):
        item = self.make_supervisor()
        with tempfile.TemporaryDirectory() as temporary:
            item.home = Path(temporary)
            item.scope = Mock()
            item.marker("create")
            path = item.home / "operations/create.json"
            first = path.read_bytes()
            with self.assertRaises(FileExistsError): item.marker("create")
            self.assertEqual(first, path.read_bytes())
            self.assertIn(b"08ce4b663b52127f400fa18bd13eb12fe04a8797", first)

    def test_old_adopt_resume_and_reconcile_are_disabled(self):
        item = self.make_supervisor()
        for name in ("adopt", "resume", "reconcile_resume", "resume_anchor"):
            with self.assertRaises(RuntimeError): getattr(item, name)()
        item.run.assert_not_called()

    def test_private_log_module_does_not_change_old_allowed_serials(self):
        item = self.make_supervisor()
        import continuous_log
        original = continuous_log.SERIALS
        fresh = s.capture_module()
        self.assertEqual(frozenset({"emulator-5664", "emulator-5666"}), fresh.SERIALS)
        self.assertEqual(original, continuous_log.SERIALS)

    def test_private_capture_exact_filter_keeps_resource_errors_and_nonce(self):
        import continuous_log
        original_tail = list(continuous_log.TAIL)
        original_handshake_tail = list(continuous_log.HANDSHAKE_TAIL)
        module = s.capture_module()
        item = self.make_supervisor()
        item.env = {}
        with tempfile.TemporaryDirectory() as directory:
            capture = item.new_log_capture(Path(directory), handshake_writer=lambda nonce: None,
                                           nonce="a" * 32)
            self.assertEqual(["-s", "emulator-5664", "logcat", "-b", "main", "-v", "threadtime", "-s",
                              "LITHIC_JPEG:I", "LITHIC_B2_RESOURCE:E", "LITHIC_M10_CAPTURE:I", "*:S"],
                             capture.command[1:])
            self.assertEqual(["logcat", "-b", "main", "-v", "threadtime", "-s",
                              "LITHIC_JPEG:I", "LITHIC_B2_RESOURCE:E", "*:S"], module.TAIL)
            self.assertEqual(original_tail, continuous_log.TAIL)
            self.assertEqual(original_handshake_tail, continuous_log.HANDSHAKE_TAIL)

    def test_no_test_cleanup_or_retry_cli_but_explicit_conditional_exit(self):
        self.assertEqual(("audit", "create", "start", "identity", "install-app", "install-test", "ending-readonly", "safe-exit"),
                         s.OPERATIONS)

    def test_global_adb_or_emulator_mutation_not_exposed(self):
        item = self.make_supervisor()
        for args in (("devices", "-l"), ("kill-server",), ("emu", "kill"), ("start-server",)):
            with self.assertRaises(RuntimeError): item.adb(*args)
        item.run.assert_not_called()

    def controlled_paths(self, item, temporary):
        item.home = temporary / "base/api26"
        item.data = item.home / "data" / (item.name + ".avd")
        item.registry = item.home / "avds"
        item.sdk = temporary / "sdk"
        item.scope = Mock()
        item.audit = Mock()
        item.api26_exit_gate = Mock()
        item.vacant = Mock(return_value={"targets": [], "ports": [], "freeKiB": 3000000})
        item.env = {}

    def test_create_claim_precedes_one_official_command_and_failure_stays_claimed(self):
        item = self.make_supervisor()
        with tempfile.TemporaryDirectory() as directory:
            temporary = Path(directory)
            self.controlled_paths(item, temporary)
            manager = item.sdk / "cmdline-tools/latest/bin/avdmanager.bat"
            manager.parent.mkdir(parents=True)
            manager.write_text("synthetic host fixture")
            def fail(*args, **kwargs):
                self.assertTrue((item.home / "operations/create.json").is_file())
                self.assertEqual(b"no\n", kwargs["input_bytes"])
                self.assertNotIn("--force", args[0])
                raise RuntimeError("SYNTHETIC_OFFICIAL_COMMAND_FAILURE")
            item.run = Mock(side_effect=fail)
            with patch.object(s, "BASE", temporary / "base"):
                with self.assertRaisesRegex(RuntimeError, "SYNTHETIC_OFFICIAL"):
                    item.create()
                with self.assertRaises(RuntimeError): item.create()
            self.assertEqual(1, item.run.call_count)
            self.assertTrue((item.home / "operations/create.json").exists())

    def test_memory_gate_failure_creates_no_environment_and_runs_no_tool(self):
        item = self.make_supervisor()
        with tempfile.TemporaryDirectory() as directory:
            temporary = Path(directory)
            self.controlled_paths(item, temporary)
            item.vacant = Mock(side_effect=RuntimeError("INSUFFICIENT_FREE_HOST_MEMORY"))
            with patch.object(s, "BASE", temporary / "base"):
                with self.assertRaisesRegex(RuntimeError, "INSUFFICIENT"):
                    item.create()
            self.assertFalse(item.home.exists())
            item.run.assert_not_called()

    def test_failed_start_retains_marker_and_cannot_launch_second_instance(self):
        item = self.make_supervisor()
        with tempfile.TemporaryDirectory() as directory:
            temporary = Path(directory)
            self.controlled_paths(item, temporary)
            item.home.mkdir(parents=True)
            (item.home / "runtime-temp").mkdir()
            item.evidence = temporary
            item.config_observation = {"configIntegrity": "synthetic", "descriptorIntegrity": "synthetic"}
            item.verify_avd = Mock()
            item.config_path_preflight = Mock(return_value={"status": "PASS"})
            item.require_proof = Mock(return_value={"configuration": item.config_observation})
            with patch.object(s.subprocess, "Popen", side_effect=OSError("SYNTHETIC_SPAWN_FAILURE")) as spawn:
                with self.assertRaisesRegex(OSError, "SYNTHETIC_SPAWN"):
                    item.start()
                with self.assertRaises(FileExistsError): item.start()
            self.assertEqual(1, spawn.call_count)
            self.assertTrue((item.home / "operations/start.json").is_file())

    def test_environment_proof_does_not_depend_on_rebuilt_test_apk(self):
        item = self.make_supervisor()
        original = item.binding()
        with patch.object(s, "TEST_SHA", "0" * 64):
            self.assertEqual(original, item.binding())

    def prepared_start(self, item, temporary):
        self.controlled_paths(item, temporary)
        item.home.mkdir(parents=True)
        (item.home / "runtime-temp").mkdir()
        item.evidence = temporary
        item.config_observation = {"configIntegrity": "synthetic", "descriptorIntegrity": "synthetic"}
        item.verify_avd = Mock()
        item.config_path_preflight = Mock(return_value={"status": "PASS"})
        item.require_proof = Mock(return_value={"configuration": item.config_observation})
        item.env = {"TEMP": str(item.home / "runtime-temp"), "TMP": str(item.home / "runtime-temp")}

    def test_empty_java_perf_directory_is_preserved_with_fresh_child_temp(self):
        item = self.make_supervisor()
        with tempfile.TemporaryDirectory() as directory:
            self.prepared_start(item, Path(directory))
            perf = item.home / "runtime-temp/hsperfdata_CDD"
            perf.mkdir()
            before = perf.stat()
            original_env = dict(item.env)
            with patch.object(s.subprocess, "Popen", side_effect=OSError("SYNTHETIC_SPAWN_FAILURE")) as spawn:
                with self.assertRaisesRegex(OSError, "SYNTHETIC_SPAWN"):
                    item.start()
            after = perf.stat()
            self.assertEqual((before.st_ino, before.st_mtime_ns), (after.st_ino, after.st_mtime_ns))
            self.assertEqual([], list(perf.iterdir()))
            child = item.home / "runtime-temp/emulator-session"
            self.assertTrue(child.is_dir())
            self.assertEqual(str(child), spawn.call_args.kwargs["env"]["TEMP"])
            self.assertEqual(str(child), spawn.call_args.kwargs["env"]["TMP"])
            self.assertEqual(original_env, item.env)

    def test_existing_child_or_nonempty_perf_stops_before_marker_and_popen(self):
        for existing_child in (True, False):
            item = self.make_supervisor()
            with tempfile.TemporaryDirectory() as directory:
                self.prepared_start(item, Path(directory))
                if existing_child:
                    (item.home / "runtime-temp/emulator-session").mkdir()
                else:
                    perf = item.home / "runtime-temp/hsperfdata_CDD"
                    perf.mkdir()
                    (perf / "unknown").write_bytes(b"preserve")
                with patch.object(s.subprocess, "Popen") as spawn:
                    with self.assertRaises((RuntimeError, FileExistsError)):
                        item.start()
                spawn.assert_not_called()
                self.assertFalse((item.home / "operations/start.json").exists())

    def test_start_waits_for_runtime_paths_before_any_targeted_adb(self):
        item = self.make_supervisor()
        with tempfile.TemporaryDirectory() as directory:
            temporary = Path(directory)
            self.controlled_paths(item, temporary)
            item.home.mkdir(parents=True)
            (item.home / "runtime-temp").mkdir()
            item.data.mkdir(parents=True)
            (item.home / "create-proof.json").write_text("{}")
            item.evidence = temporary
            item.config_observation = {"configIntegrity": "synthetic", "descriptorIntegrity": "synthetic"}
            item.verify_avd = Mock()
            item.config_path_preflight = Mock(return_value={"status": "PASS"})
            item.require_proof = Mock(return_value={"configuration": item.config_observation})
            item.process_proof = {"status": "PASS", "anchor": {"synthetic": True}}
            events = []
            item.validate_process = Mock(side_effect=lambda **kwargs: events.append("CIM"))
            item.runtime_paths = Mock(side_effect=lambda: events.append("PATHS") or {"status": "PASS"})
            def adb(*args, **kwargs):
                self.assertIn("PATHS", events)
                events.append("ADB")
                return "device" if args == ("get-state",) else "1"
            item.adb = Mock(side_effect=adb)
            item._android_identity = Mock()
            item.note = Mock()
            child = Mock(pid=123)
            child.poll.return_value = None
            def sleep(_):
                self.assertNotIn("ADB", events)
                (item.data / "hardware-qemu.ini").write_text("synthetic resolved runtime")
                events.append("HOST_WAIT")
            with patch.object(s.subprocess, "Popen", return_value=child), patch.object(s.time, "sleep", side_effect=sleep):
                item.start()
            self.assertEqual(["CIM", "HOST_WAIT", "CIM", "PATHS", "ADB", "ADB"], events[:6])
            self.assertTrue((item.home / "boot-proof.json").exists())

    def config_fixture(self, item, temporary, extra):
        self.controlled_paths(item, temporary)
        item.data.mkdir(parents=True)
        schema = item.sdk / "emulator/lib/hardware-properties.ini"
        schema.parent.mkdir(parents=True)
        schema.write_text("name = disk.dataPartition.path\nname = disk.cachePartition.path\nname = hw.sdCard.path\n")
        config = {"disk.dataPartition.path": "<temp>", "disk.cachePartition.path": "cache.img"}
        config.update(extra)
        item.config_observation = {"configuration": config}

    def test_unknown_nonempty_config_path_cannot_reach_launch(self):
        item = self.make_supervisor()
        with tempfile.TemporaryDirectory() as directory:
            self.config_fixture(item, Path(directory), {"unknown.path": "mystery.bin"})
            with self.assertRaisesRegex(RuntimeError, "Unknown startup path"):
                item.config_path_preflight()

    def test_known_startup_writable_path_must_stay_in_new_data_root(self):
        item = self.make_supervisor()
        with tempfile.TemporaryDirectory() as directory:
            self.config_fixture(item, Path(directory), {"hw.sdCard.path": str(Path(directory) / "outside.img")})
            with self.assertRaisesRegex(RuntimeError, "writable path escapes"):
                item.config_path_preflight()

    def test_legal_temp_placeholder_requires_explicit_owned_data_cache_arguments(self):
        item = self.make_supervisor()
        with tempfile.TemporaryDirectory() as directory:
            self.config_fixture(item, Path(directory), {})
            self.assertEqual("PASS", item.config_path_preflight()["status"])
            item.launch_arguments = Mock(return_value=["emulator.exe", "-data", "outside.img"])
            with self.assertRaisesRegex(RuntimeError, "Explicit launch"):
                item.config_path_preflight()


if __name__ == "__main__": unittest.main()
