"""HOST_ONLY. Only the UID token is real; process rows below are synthetic."""
import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

import audit_uid
import gate
from test_gate import ps_capture, samples, supervisor, SyntheticCollector

HERE = Path(__file__).resolve().parent
LOCK = HERE / 'evidence/uid-20261017-04/uid-diagnosis.json'
LOCK_SHA = 'fe912e2f0fdcd196e63891b1013ca8f0c415bd689e495866d407273b26d180da'
UIDS = {gate.PROBE: 10218, gate.OUTSIDER: 10219}


def observation():
    raw = LOCK.read_bytes()
    assert hashlib.sha256(raw).hexdigest() == LOCK_SHA
    return json.loads(raw)


def mixed(row='uwb 700 1 synthetic-platform'):
    return ps_capture('USER PID PPID NAME\n' + row + '\nu0_a218 701 1 ' + gate.PROBE + '\n')


class UidCompatibilityTests(unittest.TestCase):
    def test_locked_actual_uwb_token_is_accepted_without_skipping_research_rows(self):
        self.assertEqual('uwb', observation()['exceptionLabels'][0]['label'])
        try:
            rows = gate.process_rows(mixed(), UIDS)
        except ValueError as error:
            self.fail('observed platform UID must be supported: ' + str(error))
        self.assertEqual([{'uid': 10218, 'pid': 701, 'ppid': 1, 'name': gate.PROBE}], rows)

    def test_shared_decoder_resolves_exact_observed_numeric_uid(self):
        try:
            actual = gate.resolve_ps_uid('uwb')
        except ValueError as error:
            self.fail('exact observed UID must decode: ' + str(error))
        self.assertEqual(1083, actual)

    def test_two_synthetic_snapshots_still_use_full_gate(self):
        pair = samples()
        for item in pair:
            item['processes'] = mixed()
        self.assertEqual('PREFLIGHT_ELIGIBLE', gate.evaluate_preflight(pair)['classification'])

    def test_actual_supervisor_uses_corrected_parser_with_fake_collector(self):
        module = supervisor()
        with tempfile.TemporaryDirectory() as directory:
            fake = SyntheticCollector(Path(directory), module)
            original = fake.adb

            def collect(*args, **kwargs):
                result = original(*args, **kwargs)
                if args == ('shell', 'ps', '-A', '-o', 'USER,PID,PPID,NAME'):
                    result.stdout = mixed()['stdout'].encode()
                return result

            fake.adb = collect
            try:
                result = module.safe_idle(fake, 'u1-host-synthetic')
            except (module.S.StopExperiment, ValueError) as error:
                self.fail('real supervisor parser must support observed token: ' + str(error))
            self.assertEqual('PREFLIGHT_ELIGIBLE', result['classification'])

    def test_unknown_u_labels_and_uwb_lookalikes_remain_blocked(self):
        for label in ('uwb1', 'uwb_extra', 'u0_i5', 'u0_z218', 'UWB', 'uwb!', 'u_unknown'):
            with self.subTest(label=label):
                with self.assertRaises(ValueError):
                    gate.process_rows(mixed(label + ' 700 1 synthetic-platform'), UIDS)

    def test_platform_label_cannot_disguise_research_package_uid(self):
        for name in (gate.PROBE, gate.OUTSIDER):
            with self.subTest(name=name):
                with self.assertRaisesRegex(ValueError, 'UID conflicts'):
                    gate.process_rows(mixed('uwb 700 1 ' + name), UIDS)

    def test_platform_label_never_hides_worker(self):
        with self.assertRaisesRegex(ValueError, 'actual Worker'):
            gate.process_rows(mixed('uwb 700 1 ' + gate.SERVICE.PROCESS), UIDS)

    def test_unknown_same_uid_process_or_research_child_still_blocks(self):
        for row in ('u0_a218 700 1 synthetic-unknown',
                    '10218 700 1 synthetic-unknown',
                    'u0_a218 700 1 ' + gate.PROBE + ':unexpected',
                    'uwb 700 1 ' + gate.PROBE + ':unexpected'):
            with self.subTest(row=row):
                with self.assertRaisesRegex(ValueError, 'unknown process'):
                    gate.process_rows(mixed(row), UIDS)

    def test_uid_fix_does_not_add_numeric_column_preflight_fallback(self):
        value = mixed()
        value['profile'] = 'ps-user-uid-pid-ppid-name-v1'
        with self.assertRaisesRegex(ValueError, 'unsupported capture profile'):
            gate.process_rows(value, UIDS)

    def test_capture_completeness_and_integrity_still_fail_closed_with_uwb(self):
        for key, value in (('exitCode', 1), ('stderr', 'failure'), ('complete', False),
                           ('truncated', True), ('stdoutSHA256', '0' * 64), ('stdoutBytes', 1)):
            with self.subTest(field=key):
                item = mixed()
                item[key] = value
                with self.assertRaises(ValueError):
                    gate.process_rows(item, UIDS)

    def test_missing_identity_columns_or_duplicate_pid_still_block(self):
        for row in ('uwb 700 synthetic-platform', 'uwb 701 1 synthetic-platform',
                    'uwb x 1 synthetic-platform'):
            with self.subTest(row=row):
                with self.assertRaises(ValueError):
                    gate.process_rows(mixed(row), UIDS)

    def test_existing_numeric_app_and_platform_users_preserved(self):
        self.assertEqual(10218, gate.resolve_ps_uid('u0_a218'))
        self.assertEqual(10218, gate.resolve_ps_uid('10218'))
        self.assertEqual(110218, gate.resolve_ps_uid('u1_a218'))
        self.assertIsNone(gate.resolve_ps_uid('root'))
        self.assertIsNone(gate.resolve_ps_uid('usb'))


class NumericObservationAuditTests(unittest.TestCase):
    def test_actual_filtered_observation_agrees_with_shared_uid_decoder(self):
        try:
            result = audit_uid.validate(observation())
        except ValueError as error:
            self.fail('actual numeric UID observation must agree: ' + str(error))
        self.assertEqual(1083, result['numericUID'])
        self.assertFalse(result['fullRowsReplayable'])
        self.assertFalse(result['devicePreflightRun'])

    def test_numeric_uid_mismatch_including_research_uid_is_rejected(self):
        for uid in (1084, 10218, 10219):
            with self.subTest(uid=uid):
                value = observation()
                value['exceptionLabels'][0]['numericUIDs'] = [uid]
                with self.assertRaisesRegex(ValueError, 'numeric UID mismatch'):
                    audit_uid.validate(value)

    def test_numeric_uid_missing_nonnumeric_or_ambiguous_is_rejected(self):
        for numbers in (None, [], ['1083'], [True], [1083, 1084]):
            with self.subTest(numbers=numbers):
                value = observation()
                value['exceptionLabels'][0]['numericUIDs'] = numbers
                with self.assertRaisesRegex(ValueError, 'numeric UID missing'):
                    audit_uid.validate(value)

    def test_missing_or_conflicting_row_correlation_is_rejected(self):
        for key, number in (('correlatedRows', 0), ('count', 2), ('correlatedRows', None)):
            with self.subTest(field=key):
                value = observation()
                value['exceptionLabels'][0][key] = number
                with self.assertRaisesRegex(ValueError, 'row correlation'):
                    audit_uid.validate(value)

    def test_numeric_column_support_must_be_actually_observed(self):
        value = observation()
        del value['numericUIDColumnSupported']
        with self.assertRaisesRegex(ValueError, 'column support'):
            audit_uid.validate(value)

    def test_failed_or_truncated_process_command_is_rejected(self):
        for key, val in (('exitCode', 1), ('complete', False), ('stderrBytes', 7)):
            with self.subTest(field=key):
                value = observation()
                value['commands'][-1][key] = val
                with self.assertRaisesRegex(ValueError, 'failed/incomplete'):
                    audit_uid.validate(value)

    def test_missing_command_hash_bytes_or_second_sample_is_rejected(self):
        for key in ('stdoutSHA256', 'stdoutBytes'):
            with self.subTest(field=key):
                value = observation()
                del value['commands'][-1][key]
                with self.assertRaisesRegex(ValueError, 'integrity metadata'):
                    audit_uid.validate(value)
        value = observation()
        value['commands'].pop()
        with self.assertRaisesRegex(ValueError, 'two process capture'):
            audit_uid.validate(value)

    def test_installed_research_uid_conflict_or_missing_identity_is_rejected(self):
        value = observation()
        value['installedIdentities'][gate.PROBE]['uid'] = 1083
        with self.assertRaisesRegex(ValueError, 'identity conflicts'):
            audit_uid.validate(value)
        value = observation()
        del value['installedIdentities'][gate.OUTSIDER]
        with self.assertRaisesRegex(ValueError, 'identity missing'):
            audit_uid.validate(value)

    def test_unknown_or_additional_exception_label_is_rejected(self):
        value = observation()
        value['exceptionLabels'][0]['label'] = 'u0_z218'
        with self.assertRaisesRegex(ValueError, 'correlation'):
            audit_uid.validate(value)
        value = observation()
        value['exceptionLabels'].append(copy.deepcopy(value['exceptionLabels'][0]))
        with self.assertRaisesRegex(ValueError, 'exact observed exception'):
            audit_uid.validate(value)

    def test_wrong_device_command_or_new_research_row_is_rejected(self):
        value = observation()
        value['commands'][-1]['command'][2] = 'emulator-5554'
        with self.assertRaisesRegex(ValueError, 'command identity'):
            audit_uid.validate(value)
        value = observation()
        value['researchRows'] = [{'uid': 10218}]
        with self.assertRaisesRegex(ValueError, 'unexpected research'):
            audit_uid.validate(value)


if __name__ == '__main__':
    unittest.main(verbosity=2)
