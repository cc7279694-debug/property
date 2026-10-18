"""Host-only safety guard tests; no device/SDK calls."""
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import smoke_c2 as smoke


class C2Guards(unittest.TestCase):
    def test_legacy_home_removed_without_changing_parent(self):
        with patch.dict(os.environ, {'ANDROID_SDK_HOME': 'parent-conflict'}):
            self.assertNotIn('ANDROID_SDK_HOME', smoke.environment())
            self.assertEqual(os.environ['ANDROID_SDK_HOME'], 'parent-conflict')

    def test_sdk_and_avd_roots_have_separate_roles(self):
        env = smoke.environment()
        self.assertEqual(env['ANDROID_HOME'], env['ANDROID_SDK_ROOT'])
        self.assertEqual(env['ANDROID_SERIAL'], 'emulator-5612')
        self.assertTrue(smoke.DATA.is_relative_to(smoke.ROOT))
        self.assertFalse(smoke.DATA.is_relative_to(smoke.SDK))
        self.assertEqual(env['ANDROID_AVD_HOME'], str(smoke.ROOT / 'avds'))

    def test_durable_marker_refuses_overwrite(self):
        with tempfile.TemporaryDirectory() as temp:
            p = Path(temp) / 'marker.json'
            smoke.durable(p, {'first': True})
            original = p.read_bytes()
            with self.assertRaises(FileExistsError):
                smoke.durable(p, {'second': True})
            self.assertEqual(p.read_bytes(), original)

    def test_old_a2r_avd_cannot_pass_new_path_guard(self):
        with self.assertRaises(ValueError):
            smoke.contained(Path('E:/CODEX/.codex/research-env/lithic-a2r-api26/avds'))

    def test_targeted_adb_requires_prior_new_serial_discovery(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(smoke, 'STATE', Path(temp)):
            evidence = smoke.Evidence.__new__(smoke.Evidence)
            with patch.object(evidence, 'command') as command:
                with self.assertRaises(RuntimeError):
                    evidence.adb('forbidden', ['get-state'])
                command.assert_not_called()

    def test_second_start_stops_before_spawning(self):
        with tempfile.TemporaryDirectory() as temp, patch.object(smoke, 'STATE', Path(temp)), patch.object(smoke, 'verify_created'):
            smoke.durable(Path(temp) / 'start-attempt.json', {'alreadyAttempted': True})
            with patch.object(smoke.subprocess, 'Popen') as spawn:
                with self.assertRaises(RuntimeError):
                    smoke.start(smoke.Evidence.__new__(smoke.Evidence))
                spawn.assert_not_called()


if __name__ == '__main__':
    unittest.main(verbosity=2)
