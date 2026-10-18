"""Host-only AVD admission regression; real CLI bytes, synthetic isolated trees."""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

import avd_identity as identity


EVIDENCE = Path(__file__).with_name("evidence")
REAL_CONFIG = EVIDENCE / "api26-generated-config.ini"
REAL_DESCRIPTOR = EVIDENCE / "api26-generated-descriptor.ini"


class AvdIdentityTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="lithic-avd-identity-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.name = "Lithic_M10V_API26_20261018"
        self.home = self.root / "api26"
        self.data = self.home / "data" / (self.name + ".avd")
        self.registry = self.home / "avds"
        self.sdk = self.root / "sdk"
        self.image = self.sdk / "system-images/android-26/google_apis/x86_64"
        for item in (self.data, self.registry, self.image):
            item.mkdir(parents=True)
        self.config = self.data / "config.ini"
        self.config.write_bytes(REAL_CONFIG.read_bytes())
        # Descriptor is the actual CLI fixture, with only its absolute data root
        # rebased into this test's exclusively owned temporary directory.
        descriptor = REAL_DESCRIPTOR.read_text(encoding="utf-8")
        self.descriptor = self.registry / (self.name + ".ini")
        self.descriptor.write_text("\n".join(
            "path=" + str(self.data) if row.startswith("path=") else row
            for row in descriptor.splitlines()) + "\n", encoding="utf-8")
        (self.image / "source.properties").write_text(
            "Pkg.Revision=16\nAndroidVersion.ApiLevel=26\nSystemImage.Abi=x86_64\n"
            "SystemImage.TagId=google_apis\n", encoding="utf-8")
        (self.image / "package.xml").write_text(
            '<repository><localPackage path="system-images;android-26;google_apis;x86_64">'
            '<revision><major>16</major></revision></localPackage></repository>', encoding="utf-8")
        (self.image / "system.img").write_bytes(b"synthetic system image, never Android evidence")
        (self.image / "userdata.img").write_bytes(b"synthetic userdata")

    def verify(self, **kwargs):
        return identity.verify_configuration(self.registry, self.data, self.sdk, self.name, 26, **kwargs)

    def set_config(self, key, value):
        rows = self.config.read_text(encoding="utf-8").splitlines()
        rows = [row for row in rows if not row.startswith(key + "=")]
        if value is not None:
            rows.append(key + "=" + value)
        self.config.write_text("\n".join(rows) + "\n", encoding="utf-8")

    def set_descriptor(self, key, value):
        rows = self.descriptor.read_text(encoding="utf-8").splitlines()
        rows = [row for row in rows if not row.startswith(key + "=")]
        if value is not None:
            rows.append(key + "=" + value)
        self.descriptor.write_text("\n".join(rows) + "\n", encoding="utf-8")

    def blocked(self):
        with self.assertRaises((identity.ConfigurationBlocked, FileNotFoundError)):
            self.verify()

    def test_actual_cli_fixture_without_AvdId_is_statically_accepted(self):
        self.assertNotIn(b"AvdId=", self.config.read_bytes())
        result = self.verify()
        self.assertEqual("STATIC_CONFIGURATION_VERIFIED", result["status"])
        self.assertEqual(hashlib.sha256(REAL_CONFIG.read_bytes()).hexdigest(),
                         result["configIntegrity"]["sha256"])
        json.dumps(result)

    def test_real_fixture_bytes_are_never_modified(self):
        before = REAL_CONFIG.read_bytes(), REAL_DESCRIPTOR.read_bytes()
        self.verify()
        self.assertEqual(before, (REAL_CONFIG.read_bytes(), REAL_DESCRIPTOR.read_bytes()))

    def test_build_placeholders_are_not_runtime_identity_proof(self):
        result = self.verify()
        self.assertEqual("NOT_RUN", result["runtimeIdentity"])
        self.assertEqual("SET_BEFORE_LAUNCH_CORE_NOT_RUNTIME_IDENTITY",
                         result["placeholderFields"]["avd.id"]["meaning"])
        self.assertTrue(result["explicitDatadirRequired"])

    def test_temp_placeholder_never_resolves_to_real_path(self):
        result = self.verify()
        self.assertNotIn("disk.dataPartition.path", result["resolvedConfiguredPaths"])
        self.assertEqual("UNKNOWN_UNTIL_RUNTIME_EVIDENCE", result["runtimeTemporaryFileConfinement"])
        self.assertEqual("TEMPORARY_FILE_LOCATION_UNPROVEN",
                         result["placeholderFields"]["disk.dataPartition.path"]["meaning"])

    def test_optional_matching_AvdId_is_accepted(self):
        self.set_config("AvdId", self.name)
        self.assertEqual("STATIC_CONFIGURATION_VERIFIED", self.verify()["status"])

    def test_optional_conflicting_AvdId_is_rejected(self):
        self.set_config("AvdId", "Lithic_M01_API_36")
        self.blocked()

    def test_concrete_avd_id_and_name_must_match_registration(self):
        self.set_config("avd.id", self.name)
        self.set_config("avd.name", self.name)
        self.assertNotIn("avd.id", self.verify()["placeholderFields"])

    def test_conflicting_avd_name_is_rejected(self):
        self.set_config("avd.name", "different")
        self.blocked()

    def test_missing_avd_id_is_rejected(self):
        self.set_config("avd.id", None)
        self.blocked()

    def test_missing_avd_name_is_rejected(self):
        self.set_config("avd.name", None)
        self.blocked()

    def test_missing_data_partition_path_is_rejected(self):
        self.set_config("disk.dataPartition.path", None)
        self.blocked()

    def test_unknown_identity_placeholder_is_rejected(self):
        self.set_config("avd.id", "<auto>")
        self.blocked()

    def test_unknown_data_placeholder_is_rejected(self):
        self.set_config("disk.dataPartition.path", "<build>")
        self.blocked()

    def test_unknown_placeholder_in_any_other_key_is_rejected(self):
        self.set_config("disk.cachePartition.path", "<temp>")
        self.blocked()

    def test_wrong_config_api_is_rejected(self):
        self.set_config("target", "android-36")
        self.blocked()

    def test_wrong_descriptor_api_is_rejected(self):
        self.set_descriptor("target", "android-36")
        self.blocked()

    def test_wrong_abi_is_rejected(self):
        self.set_config("abi.type", "arm64-v8a")
        self.blocked()

    def test_conflicting_cpu_arch_is_rejected(self):
        self.set_config("hw.cpu.arch", "arm64")
        self.blocked()

    def test_wrong_tag_is_rejected(self):
        self.set_config("tag.id", "google_apis_playstore")
        self.blocked()

    def test_wrong_image_directory_is_rejected(self):
        self.set_config("image.sysdir.1", "system-images/android-36/google_apis/x86_64")
        self.blocked()

    def test_second_image_directory_is_rejected(self):
        self.set_config("image.sysdir.2", str(self.image))
        self.blocked()

    def test_missing_source_properties_is_rejected(self):
        (self.image / "source.properties").unlink()
        self.blocked()

    def test_wrong_sdk_revision_is_rejected(self):
        file = self.image / "source.properties"
        file.write_text(file.read_text().replace("Revision=16", "Revision=15"))
        self.blocked()

    def test_wrong_sdk_api_metadata_is_rejected(self):
        file = self.image / "source.properties"
        file.write_text(file.read_text().replace("ApiLevel=26", "ApiLevel=36"))
        self.blocked()

    def test_api36_revision7_configuration_is_statically_accepted(self):
        name = "Lithic_M10V_API36_20261018"
        home = self.root / "api36"
        registry = home / "avds"
        data = home / "data" / (name + ".avd")
        image = self.sdk / "system-images/android-36/google_apis/x86_64"
        for item in (registry, data, image):
            item.mkdir(parents=True)
        # This is a synthetic API36 projection of the real API26 CLI fixture,
        # not a claim to have observed an API36 create or launch.
        (data / "config.ini").write_text(self.config.read_text(encoding="utf-8").replace(
            "android-26", "android-36"), encoding="utf-8")
        (registry / (name + ".ini")).write_text(
            "avd.ini.encoding=UTF-8\npath=" + str(data) + "\ntarget=android-36\n", encoding="utf-8")
        (image / "source.properties").write_text(
            "Pkg.Revision=7\nAndroidVersion.ApiLevel=36\nSystemImage.Abi=x86_64\n"
            "SystemImage.TagId=google_apis\n", encoding="utf-8")
        (image / "package.xml").write_text(
            '<repository><localPackage path="system-images;android-36;google_apis;x86_64">'
            '<revision><major>7</major></revision></localPackage></repository>', encoding="utf-8")
        (image / "system.img").write_bytes(b"synthetic system")
        (image / "data").mkdir()
        # This marker's real API36 r7 filename replaces the API26 userdata.img
        # assumption. Bytes are synthetic; payload integrity stays caller-only.
        (image / "data/empty_data_disk").write_bytes(b"synthetic marker, not official image evidence")
        result = identity.verify_configuration(registry, data, self.sdk, name, 36)
        self.assertEqual("7", result["imageRevision"])
        self.assertEqual("NOT_RUN", result["runtimeIdentity"])
        self.assertFalse((image / "userdata.img").exists())
        self.assertEqual("OFFICIAL_EMPTY_DATA_DISK_MARKER", result["dataInitializationLayout"])

    def test_wrong_package_path_is_rejected(self):
        file = self.image / "package.xml"
        file.write_text(file.read_text().replace("android-26", "android-36"))
        self.blocked()

    def test_conflicting_xml_package_revision_is_rejected(self):
        file = self.image / "package.xml"
        file.write_text(file.read_text().replace("<major>16</major>", "<major>17</major>"))
        self.blocked()

    def test_duplicate_xml_local_package_is_rejected(self):
        file = self.image / "package.xml"
        file.write_text(file.read_text().replace("</repository>",
            '<localPackage path="system-images;android-26;google_apis;x86_64"/></repository>'))
        self.blocked()

    def test_missing_package_xml_is_rejected(self):
        (self.image / "package.xml").unlink()
        self.blocked()

    def test_missing_payload_is_rejected(self):
        (self.image / "system.img").unlink()
        self.blocked()

    def test_expected_metadata_sha_is_checked(self):
        expected = hashlib.sha256((self.image / "source.properties").read_bytes()).hexdigest()
        self.assertEqual("STATIC_CONFIGURATION_VERIFIED", self.verify(
            expected_image_digest={"source.properties": expected})["status"])

    def test_wrong_expected_metadata_sha_is_rejected(self):
        with self.assertRaises(identity.ConfigurationBlocked):
            self.verify(expected_image_digest={"source.properties": "0" * 64})

    def test_payload_digest_is_not_claimed_without_callers_full_audit(self):
        self.assertEqual("CALLER_MUST_VERIFY_LOCKED_SHA", self.verify()["imagePayloadIntegrity"])

    def test_wrong_descriptor_filename_is_rejected(self):
        self.descriptor.rename(self.registry / "other.ini")
        self.blocked()

    def test_second_registration_is_rejected(self):
        (self.registry / "other.ini").write_bytes(self.descriptor.read_bytes())
        self.blocked()

    def test_descriptor_wrong_absolute_data_path_is_rejected(self):
        self.set_descriptor("path", str(self.home / "wrong.avd"))
        self.blocked()

    def test_descriptor_relative_data_path_is_rejected(self):
        self.set_descriptor("path", "data/relative.avd")
        self.blocked()

    def test_conflicting_path_rel_is_rejected(self):
        self.set_descriptor("path.rel", "avds/other.avd")
        self.blocked()

    def test_duplicate_config_key_is_rejected(self):
        with self.config.open("a", encoding="utf-8") as stream:
            stream.write("avd.id=<build>\n")
        self.blocked()

    def test_duplicate_descriptor_key_is_rejected(self):
        with self.descriptor.open("a", encoding="utf-8") as stream:
            stream.write("target=android-26\n")
        self.blocked()

    def test_case_variant_duplicate_key_is_rejected(self):
        with self.config.open("a", encoding="utf-8") as stream:
            stream.write("AVD.ID=<build>\n")
        self.blocked()

    def test_malformed_line_is_rejected(self):
        with self.config.open("a", encoding="utf-8") as stream:
            stream.write("missing-value-separator\n")
        self.blocked()

    def test_non_utf8_config_is_rejected(self):
        self.config.write_bytes(b"\xff\xfe\x00")
        self.blocked()

    def test_config_encoding_claim_must_match_utf8(self):
        self.set_config("avd.ini.encoding", "UTF-16")
        self.blocked()

    def test_missing_config_is_rejected(self):
        self.config.unlink()
        self.blocked()

    def test_data_partition_concrete_owned_path_is_accepted(self):
        self.set_config("disk.dataPartition.path", str(self.data / "userdata-qemu.img"))
        self.assertIn("disk.dataPartition.path", self.verify()["resolvedConfiguredPaths"])

    def test_absolute_writable_path_outside_data_is_rejected(self):
        self.set_config("disk.cachePartition.path", str(self.home / "unowned.img"))
        self.blocked()

    def test_relative_parent_escape_is_rejected(self):
        self.set_config("sdcard.path", "../unowned.img")
        self.blocked()

    def test_variable_path_expansion_is_rejected(self):
        self.set_config("sdcard.path", "%TEMP%/sdcard.img")
        self.blocked()

    def test_symlink_writable_path_is_rejected(self):
        target = self.root / "other-file"
        target.write_bytes(b"external")
        link = self.data / "linked.img"
        try:
            link.symlink_to(target)
        except OSError as error:
            self.skipTest("Host cannot create symlink: " + str(error))
        self.set_config("sdcard.path", str(link))
        self.blocked()

    def test_hardlinked_writable_path_is_rejected(self):
        target = self.root / "other-file"
        target.write_bytes(b"external")
        link = self.data / "hardlinked.img"
        os.link(target, link)
        self.set_config("sdcard.path", str(link))
        self.blocked()

    def test_hardlinked_configuration_file_is_rejected(self):
        os.link(self.config, self.root / "config-alias.ini")
        self.blocked()

    @unittest.skipUnless(os.name == "nt", "Windows junction contract")
    def test_junction_ancestor_is_rejected(self):
        external = self.root / "outside"
        external.mkdir()
        link = self.data / "junction"
        made = subprocess.run(["cmd", "/c", "mklink", "/J", str(link), str(external)],
                              capture_output=True, timeout=10)
        self.assertEqual(0, made.returncode, made.stderr.decode(errors="replace"))
        try:
            self.set_config("sdcard.path", str(link / "sd.img"))
            self.blocked()
        finally:
            link.rmdir()  # Only this test's empty junction, never its external target.

    def test_registration_directory_symlink_is_rejected(self):
        other = self.root / "alias"
        try:
            other.symlink_to(self.registry, target_is_directory=True)
        except OSError as error:
            self.skipTest("Host cannot create symlink: " + str(error))
        with self.assertRaises(identity.ConfigurationBlocked):
            identity.verify_configuration(other, self.data, self.sdk, self.name, 26)

    def test_original_h2_name_is_rejected(self):
        with self.assertRaises(identity.ConfigurationBlocked):
            identity.verify_configuration(self.registry, self.data, self.sdk, "Lithic_M01_API_36", 26)

    def test_relative_root_is_rejected(self):
        with self.assertRaises(identity.ConfigurationBlocked):
            identity.verify_configuration(Path("relative"), self.data, self.sdk, self.name, 26)


if __name__ == "__main__":
    unittest.main(verbosity=2)
