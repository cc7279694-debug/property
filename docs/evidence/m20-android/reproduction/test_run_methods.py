"""Host-only parser checks; no device commands are executed."""
import copy
import json
import unittest

import run_methods as runner


CLASS = 'com.cooper.lithic.data.settings.M20ReminderSettingsTest'
METHOD = 'defaultsToggleWatermarkAndReopenUseOneSettingsOwner'


def successful_output():
    parts = []
    for code in (1, 0):
        for key, value in (('class', CLASS), ('current', '1'), ('numtests', '1'), ('test', METHOD)):
            parts.append(f'INSTRUMENTATION_STATUS: {key}={value}')
        parts.append(f'INSTRUMENTATION_STATUS_CODE: {code}')
    return ('\n'.join(parts) + '\nINSTRUMENTATION_RESULT: stream=\nTime: 0.01\n\nOK (1 test)\n\nINSTRUMENTATION_CODE: -1\n').encode()


class MethodRunnerTest(unittest.TestCase):
    def assess(self, stdout=None, stderr=b'', code=0, timeout=False):
        return runner.evaluate(successful_output() if stdout is None else stdout,
                               stderr, code, timeout, CLASS, METHOD)

    def test_one_exact_method_requires_both_junit_events_and_final_success(self):
        self.assertEqual('PASS', self.assess()['status'])

    def test_wrong_method_and_duplicate_run_fail(self):
        wrong = successful_output().replace(METHOD.encode(), b'another_method')
        self.assertEqual('FAIL', self.assess(wrong)['status'])
        self.assertEqual('FAIL', self.assess(successful_output() * 2)['status'])

    def test_outer_exit_zero_and_partial_output_are_insufficient(self):
        self.assertEqual('FAIL', self.assess(b'OK (1 test)\n')['status'])
        missing_end = successful_output().replace(b'INSTRUMENTATION_STATUS_CODE: 0', b'INSTRUMENTATION_STATUS_CODE: 2')
        self.assertEqual('FAIL', self.assess(missing_end)['status'])

    def test_skip_or_failure_is_rejected_even_with_ok_footer(self):
        for status in (-1, -2, -3, -4):
            output = successful_output().replace(b'INSTRUMENTATION_STATUS_CODE: 0',
                                                 f'INSTRUMENTATION_STATUS_CODE: {status}'.encode())
            self.assertEqual('FAIL', self.assess(output)['status'])

    def test_stderr_nonzero_and_invalid_bytes_are_rejected(self):
        self.assertEqual('FAIL', self.assess(stderr=b'warning')['status'])
        self.assertEqual('FAIL', self.assess(code=1)['status'])
        self.assertEqual('UNKNOWN', self.assess(b'\xff')['status'])

    def test_timeout_keeps_device_completion_unknown(self):
        result = self.assess(code=None, timeout=True)
        self.assertEqual('UNKNOWN', result['status'])
        self.assertIn('HOST_ADB_TIMEOUT_DEVICE_INSTRUMENTATION_STATE_UNKNOWN', result['reasons'])

    def test_extra_status_bundle_is_not_a_second_junit_method(self):
        output = (b'INSTRUMENTATION_STATUS: m20_gallery_evidence_root=/synthetic\n'
                  b'INSTRUMENTATION_STATUS_CODE: 2\n' + successful_output())
        self.assertEqual('PASS', self.assess(output)['status'])

    def test_original_22_and_two_extras_are_distinct_selections(self):
        original = runner.selection('original')
        extras = runner.selection('gallery') + runner.selection('platform')
        identities = lambda entries: {item['class_name'] + '#' + item['method'] for item in entries}
        self.assertEqual(22, len(identities(original)))
        self.assertEqual(2, len(identities(extras)))
        self.assertFalse(identities(original) & identities(extras))
        recheck = runner.selection('worker-recheck')
        self.assertEqual(6, len(recheck))
        self.assertTrue(identities(recheck) <= identities(original))
        self.assertTrue(all(item['class_name'].endswith('.M20ReminderWorkerTest') for item in recheck))

    def correction_fixture(self):
        certificate = 'c' * 64
        original = dict(status='PASS', serial=runner.SERIAL, api=36, avd=runner.AVD,
                        boot_id='00000000-0000-0000-0000-000000000001', instrumentation=runner.RUNNER,
                        app=dict(package='com.cooper.lithic.debug', sha256='a' * 64, uid=10216, certificate_sha256=certificate),
                        test=dict(package='com.cooper.lithic.debug.test', sha256='b' * 64, uid=10217, certificate_sha256=certificate))
        raw = json.dumps(original).encode()
        correction = copy.deepcopy(original)
        correction['test']['sha256'] = 'd' * 64
        correction['predecessor_sha256'] = runner.digest(raw)
        return raw, correction

    def test_test_only_correction_authenticates_exact_original_proof(self):
        raw, correction = self.correction_fixture()
        runner.validate_correction(raw, correction)
        with self.assertRaises(AssertionError):
            runner.validate_correction(raw + b' ', correction)

    def test_correction_rejects_app_uid_signature_boot_or_runner_change(self):
        raw, correction = self.correction_fixture()
        for label, field, value in (('app', 'sha256', 'e' * 64), ('app', 'uid', 10218),
                                    ('test', 'uid', 10218), ('test', 'certificate_sha256', 'e' * 64)):
            changed = copy.deepcopy(correction)
            changed[label][field] = value
            with self.assertRaises(AssertionError):
                runner.validate_correction(raw, changed)
        for field, value in (('boot_id', 'another-boot'), ('instrumentation', 'another/Runner'),
                             ('serial', 'emulator-5686')):
            changed = copy.deepcopy(correction)
            changed[field] = value
            with self.assertRaises(AssertionError):
                runner.validate_correction(raw, changed)

    def test_second_correction_must_authenticate_immediate_predecessor_not_original(self):
        original, first = self.correction_fixture()
        first_raw = json.dumps(first).encode()
        second = copy.deepcopy(first)
        second['predecessor_sha256'] = runner.digest(first_raw)
        second['test']['sha256'] = 'f' * 64
        runner.validate_correction(first_raw, second)
        with self.assertRaises(AssertionError):
            runner.validate_correction(original, second)

    def test_completion_has_one_fixed_run_argument_and_recheck_is_separate(self):
        completion = runner.selection('completion')
        self.assertEqual(1, len(completion))
        command = runner.command_for('completion', completion[0])
        self.assertEqual(['-s', runner.SERIAL], command[1:3])
        self.assertEqual(['-e', 'm20PlatformRun', runner.COMPLETION_RUN, runner.RUNNER], command[-4:])
        self.assertEqual(runner.selection('platform'), runner.selection('platform-recheck'))
        self.assertNotIn('m20PlatformRun', runner.command_for('platform-recheck', runner.selection('platform-recheck')[0]))


if __name__ == '__main__':
    unittest.main()
