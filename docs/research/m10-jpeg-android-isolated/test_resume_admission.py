"""Eight integration contracts; temporary synthetic files and mocked commands only."""
import hashlib
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import Mock, patch

import natural_tests
import test_validate as baseline

validate = baseline.validate


class ResumeAdmissionTest(unittest.TestCase):
    def setUp(self):
        baseline.SupervisorHostTest.setUp(self)
        self.s.config_observation = {"configIntegrity": {"sha256": "synthetic-config"},
                                     "descriptorIntegrity": {"sha256": "synthetic-descriptor"}}
        self.s.verify_avd = Mock(return_value={})
        self.s.vacant = Mock(return_value={"targets": [], "freeKiB": 4000000})
        self.audit = validate.ROOT / "build/android-validation/environment-audit.json"
        self.audit.parent.mkdir(parents=True)
        self.audit.write_bytes(b"{}")

    def adoption_fixture(self):
        source = validate.ROOT / "docs/research/m10-jpeg-android-isolated/evidence"
        source.mkdir(parents=True)
        contents = {
            "avds/" + self.s.name + ".ini": b"synthetic-descriptor",
            "data/" + self.s.name + ".avd/config.ini": b"synthetic-config",
            "data/" + self.s.name + ".avd/userdata.img": b"synthetic-userdata",
            "operations/create.json": b'{"operation":"create","status":"ATTEMPT_ISSUED"}',
        }
        for relative, data in contents.items():
            file = self.s.home / relative
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_bytes(data)
        for name, relative in (
            ("api26-generated-config.ini", "data/" + self.s.name + ".avd/config.ini"),
            ("api26-generated-descriptor.ini", "avds/" + self.s.name + ".ini"),
            ("api26-create-marker.json", "operations/create.json"),
        ):
            (source / name).write_bytes(contents[relative])
        (source / "api26-commands.jsonl").write_text(json.dumps({
            "argv": ["official-avdmanager-fixture", "create", "avd", "-n", self.s.name,
                     "-p", str(self.s.data)], "exitCode": 0}) + "\n")
        (source / "api26-closing.json").write_text(json.dumps({"files": [
            {"path": relative, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
            for relative, data in contents.items()]}))
        return contents

    def runtime_fixture(self):
        self.s.data.mkdir(parents=True)
        self.schema = self.s.sdk / "emulator/lib/hardware-properties.ini"
        self.schema.parent.mkdir(parents=True)
        keys = ("disk.dataPartition.path", "disk.cachePartition.path", "disk.dataPartition.initPath")
        self.schema.write_text("\n".join("name = " + key for key in keys) + "\n")
        self.runtime = self.s.data / "hardware-qemu.ini"
        (self.s.data / "userdata.img").write_bytes(b"template")
        self.s.image_file_lock = Mock(return_value={"bytes": 8, "sha256": hashlib.sha256(b"template").hexdigest()})
        self.runtime.write_text("avd.name=" + self.s.name + "\navd.id=" + self.s.name + "\n" +
            "disk.dataPartition.path=" + str(self.s.data / "userdata-qemu.img") + "\n" +
            "disk.cachePartition.path=" + str(self.s.data / "cache.img") + "\n" +
            "disk.dataPartition.initPath=" + str(self.s.data / "userdata.img") + "\n")

    def test_adoption_publishes_new_proof_and_preserves_original_four_files(self):
        original = self.adoption_fixture()
        self.s.adopt()
        proof = json.loads((self.s.home / "adoption-proof.json").read_text())
        self.assertEqual("STATIC_ADOPTION_NOT_RUNTIME", proof["scope"])
        self.assertEqual(4, len(proof["preservedFileInventory"]))
        self.assertEqual(original, {key: (self.s.home / key).read_bytes() for key in original})
        self.assertFalse((self.s.home / "create-proof.json").exists())

    def test_unknown_child_blocks_adoption_without_deletion_or_proof(self):
        self.adoption_fixture()
        file = self.s.data / "unknown-pin.json"
        file.write_bytes(b"retain-unknown")
        with self.assertRaises(RuntimeError):
            self.s.adopt()
        self.assertEqual(b"retain-unknown", file.read_bytes())
        self.assertFalse((self.s.home / "adoption-proof.json").exists())

    def test_configured_empty_child_temp_is_observed_without_claiming_old_inventory(self):
        self.adoption_fixture()
        temporary = self.s.home / "runtime-temp"
        temporary.mkdir()
        self.s.adopt()
        proof = json.loads((self.s.home / "adoption-proof.json").read_text())
        observed = proof["configuredChildTemporaryDirectory"]
        self.assertTrue(observed["exists"] and observed["empty"])
        self.assertEqual("CURRENT_CHILD_TEMP_CONFIGURED_OBSERVED_EMPTY_NOT_OLD_FILE", observed["provenance"])
        self.assertEqual(4, len(proof["preservedFileInventory"]))
        self.assertEqual([], list(temporary.iterdir()))

    def test_configured_child_temp_with_any_unknown_child_is_blocked_and_preserved(self):
        self.adoption_fixture()
        temporary = self.s.home / "runtime-temp"
        temporary.mkdir()
        unknown = temporary / "unknown-consumer"
        unknown.mkdir()
        with self.assertRaises(RuntimeError):
            self.s.adopt()
        self.assertTrue(unknown.is_dir())
        self.assertFalse((self.s.home / "adoption-proof.json").exists())

    def test_wrong_adoption_proof_identity_blocks_before_process_or_launch(self):
        self.s.home.mkdir(parents=True)
        (self.s.home / "adoption-proof.json").write_text(json.dumps({
            "status": "PASS", "api": 36, "avd": self.s.name, "data": str(self.s.data)}))
        with patch.object(validate.subprocess, "Popen") as launch:
            with self.assertRaises(RuntimeError):
                self.s.start()
        launch.assert_not_called()
        self.s.vacant.assert_not_called()

    def test_runtime_copied_template_requires_actual_locked_sha(self):
        self.runtime_fixture()
        self.assertEqual("PASS", self.s.runtime_paths()["status"])
        (self.s.data / "userdata.img").write_bytes(b"tampered")
        with self.assertRaises(RuntimeError):
            self.s.runtime_paths()

    def test_runtime_unknown_writable_path_is_rejected(self):
        self.runtime_fixture()
        self.runtime.write_text(self.runtime.read_text().replace(
            str(self.s.data / "cache.img"), str(self.root / "outside-cache.img")))
        with self.assertRaises(RuntimeError):
            self.s.runtime_paths()

    def test_fixed_method_cli_dispatches_exactly_once(self):
        method = natural_tests.METHODS[0]
        with patch.object(validate, "Supervisor", return_value=self.s), \
                patch.object(natural_tests, "run_one") as execute, \
                patch.object(sys, "argv", ["validate.py", "26", "test", "--method", method]):
            validate.main()
        execute.assert_called_once_with(self.s, method)

    def test_api26_create_is_rejected_before_any_tool_command(self):
        self.s.audit = Mock()
        self.s.run = Mock()
        with self.assertRaises(RuntimeError):
            self.s.create()
        self.s.audit.assert_not_called()
        self.s.run.assert_not_called()

    def test_runtime_temp_is_child_only_and_never_inferred_as_path_proof(self):
        parent = {"TEMP": "old-temp", "TMP": "old-tmp", "ANDROID_SDK_HOME": "old-home"}
        with patch.dict(validate.os.environ, parent, clear=True):
            child = self.s.environment()
            self.assertEqual(parent["TEMP"], validate.os.environ["TEMP"])
        self.assertEqual(str(self.s.home / "runtime-temp"), child["TEMP"])
        self.assertEqual(child["TEMP"], child["TMP"])
        self.assertNotIn("ANDROID_SDK_HOME", child)
        self.assertFalse((self.s.home / "boot-proof.json").exists())

    def test_host_cim_uses_existing_host_temp_without_creating_new_avd_home(self):
        temporary = self.root / "host-temp"
        temporary.mkdir()
        self.s.run = Mock(return_value='{"targets":[],"freeKiB":4000000}')
        with patch.dict(validate.os.environ, {"TEMP": str(temporary), "TMP": str(temporary)}):
            self.s.processes()
        chosen = self.s.run.call_args.kwargs["child_env"]
        self.assertEqual(str(temporary), chosen["TEMP"])
        self.assertEqual(str(temporary), chosen["TMP"])
        self.assertEqual(self.s.env["ANDROID_SERIAL"], chosen["ANDROID_SERIAL"])
        self.assertEqual(str(self.s.home / "runtime-temp"), self.s.env["TEMP"])
        self.assertFalse(self.s.home.exists())


if __name__ == "__main__":
    unittest.main(verbosity=2)
