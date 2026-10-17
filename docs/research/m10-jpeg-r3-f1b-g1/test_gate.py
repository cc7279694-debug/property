"""HOST_ONLY. Fake collectors never execute Android commands or create Android results."""
import hashlib
import copy
import importlib.util
from pathlib import Path
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

import gate
from test_service_record import synthetic_capture, body

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
OLD = HERE.parent / 'm10-jpeg-r3-f1b'


def supervisor():
    spec = importlib.util.spec_from_file_location('g1_supervisor', OLD / 'verify.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def installed(name, uid, digest):
    # SYNTHETIC identities. Only the Probe path copies the locked public fixture.
    path = ('/data/app/~~synthetic/' + name + '-synthetic/base.apk')
    if name == gate.PROBE:
        path = next(line.strip().split('=', 1)[1] for line in body().splitlines()
                    if line.strip().startswith('baseDir='))
    return {'package': name, 'present': True, 'uid': uid,
            'apks': [{'codePath': path, 'sha256': digest}]}


class SyntheticCollector:
    """Complete in-memory HOST_SYNTHETIC inputs. No subprocess/ADB implementation."""
    def __init__(self, output, module):
        self.output = output
        self.module = module
        self.calls = []
        self.research_uids = {}

    def package(self, name):
        uid = {self.module.S.PRODUCTION: 10216, self.module.S.PROBE: 10218,
               self.module.S.OUTSIDER: 10219}[name]
        return installed(name, uid, self.module.EXPECTED[name])

    def processes(self, label):
        return {'rows': [{'uid': 10218, 'pid': 3928, 'ppid': 528,
                          'name': self.module.S.PROBE}]}

    def inventory(self, label):
        return {'entries': [], 'json': {}, 'sha256': {},
                'collection': {'complete': True, 'exitCode': 0, 'stderr': '',
                               'stable': True, 'truncated': False}}

    def verify_device(self):
        return {'serial': 'emulator-5596', 'state': 'device',
                'avd': 'Lithic_M01_API_36', 'api': '36'}

    def text(self, *args, **kwargs):
        self.calls.append(tuple(args))
        return {'sys.boot_completed': '1', 'ro.build.version.sdk': '36'}.get(
            args[-1], 'Lithic_M01_API_36')

    def adb(self, *args, **kwargs):
        self.calls.append(tuple(args))
        if args == ('shell', 'getprop', 'sys.boot_completed'):
            raw = b'1\n'
        elif args == ('shell', 'ps', '-A', '-o', 'USER,PID,PPID,NAME'):
            raw = ('USER PID PPID NAME\nu0_a218 3928 528 '
                   + self.module.S.PROBE + '\n').encode()
        elif args == ('shell', 'dumpsys', 'activity', 'services', self.module.S.PROBE):
            path = OLD / 'evidence/f1b-20261017-01/blocked-service-record-detail.txt'
            raw = path.read_bytes()
            assert hashlib.sha256(raw).hexdigest() == '4080746899e8fcfb71f63c77599f5ff10c972ae3ab70983126155679b9add571'
            body = b'\n'.join(raw.splitlines()[1:]) + b'\n'
            raw = (b'ACTIVITY MANAGER SERVICES (dumpsys activity services)\n'
                   b'  User 0 active services:\n' + body)
        else:
            raise AssertionError('unexpected synthetic command: ' + repr(args))
        return SimpleNamespace(stdout=raw, stderr=b'', returncode=0)


class SupervisorIntegrationTests(unittest.TestCase):
    def test_supervisor_accepts_complete_consistent_synthetic_idle_snapshots(self):
        module = supervisor()
        with tempfile.TemporaryDirectory() as directory:
            fake = SyntheticCollector(Path(directory), module)
            try:
                result = module.safe_idle(fake, 'host-synthetic')
            except module.S.StopExperiment as error:
                self.fail('complete safe synthetic record must not be name-blocked: ' + str(error))
            self.assertEqual('PREFLIGHT_ELIGIBLE', result['classification'])
            self.assertEqual(2, sum(call[:3] == ('shell', 'dumpsys', 'activity') for call in fake.calls))

    def test_actual_supervisor_keeps_unknown_inventory_blocked(self):
        module = supervisor()
        with tempfile.TemporaryDirectory() as directory:
            fake = SyntheticCollector(Path(directory), module)
            fake.inventory = lambda _: {'entries': [], 'json': {}, 'sha256': {}}
            with self.assertRaises((module.S.StopExperiment, ValueError)):
                module.safe_idle(fake, 'host-synthetic')

    def test_host_suite_never_launches_subprocess_android_or_otherwise(self):
        with patch('subprocess.run', side_effect=AssertionError('NO HOST TEST MAY EXECUTE ADB')):
            module = supervisor()
            with tempfile.TemporaryDirectory() as directory:
                result = module.safe_idle(SyntheticCollector(Path(directory), module), 'host-synthetic')
                self.assertEqual('PREFLIGHT_ELIGIBLE', result['classification'])


def ps_capture(text=None):
    raw = (text or 'USER PID PPID NAME\nu0_a218 3928 528 ' + gate.PROBE + '\n').encode()
    return gate.capture(SimpleNamespace(stdout=raw, stderr=b'', returncode=0),
                        'ps-user-pid-ppid-name-v1')


def samples():
    packages = {name: installed(name, uid, sorted(gate.KNOWN[name])[0])
                for name, uid in ((gate.PRODUCTION, 10216), (gate.PROBE, 10218), (gate.OUTSIDER, 10219))}
    first = {'sampleId': 'HOST_SYNTHETIC_1',
             'startedAtUtc': '2026-10-17T00:00:00+00:00',
             'finishedAtUtc': '2026-10-17T00:00:01+00:00',
             'environment': {'serial': 'emulator-5596', 'avd': 'Lithic_M01_API_36',
                             'api': 36, 'bootComplete': '1'},
             'packages': packages, 'processes': ps_capture(), 'services': synthetic_capture(),
             'inventory': {'entries': [], 'json': {}, 'sha256': {},
                           'collection': {'complete': True, 'stable': True, 'truncated': False,
                                          'exitCode': 0, 'stderr': ''}}}
    second = copy.deepcopy(first)
    second.update(sampleId='HOST_SYNTHETIC_2', startedAtUtc='2026-10-17T00:00:02+00:00',
                  finishedAtUtc='2026-10-17T00:00:03+00:00')
    return [first, second]


class PreflightCompositionTests(unittest.TestCase):
    def blocked(self, pair):
        result = gate.evaluate_preflight(pair)
        self.assertEqual('BLOCKED', result['classification'], result)
        self.assertTrue(result['reasons'])

    def test_complete_consistent_synthetic_pair_is_eligible_in_research_scope(self):
        result = gate.evaluate_preflight(samples())
        self.assertEqual('PREFLIGHT_ELIGIBLE', result['classification'], result)
        self.assertIn('NOT_LEASE_DEATH_OR_EXECUTION_AUTHORITY', result['scope'])

    def test_formal_app_normal_process_does_not_become_research_consumer(self):
        pair = samples()
        for item in pair:
            item['processes'] = ps_capture('USER PID PPID NAME\nu0_a216 5000 528 ' + gate.PRODUCTION + '\n')
        self.assertEqual('PREFLIGHT_ELIGIBLE', gate.evaluate_preflight(pair)['classification'])

    def test_actual_worker_blocks_even_idle_service_and_absent_stage(self):
        pair = samples()
        pair[0]['processes'] = ps_capture('USER PID PPID NAME\nu0_a218 5000 528 ' + gate.SERVICE.PROCESS + '\n')
        self.blocked(pair)

    def test_process_uid_conflict_and_unknown_same_uid_process_block(self):
        for row in ('u0_a219 5000 528 ' + gate.PROBE,
                    'u0_a218 5000 528 com.example.unknown',
                    'u0_a218 5000 528 ' + gate.PROBE + ':unexpected'):
            with self.subTest(row=row):
                pair = samples()
                pair[0]['processes'] = ps_capture('USER PID PPID NAME\n' + row + '\n')
                self.blocked(pair)

    def test_malformed_or_incomplete_ps_cannot_be_silently_empty(self):
        for text in ('USER PID PPID NAME\nmalformed\n', 'bad header\n',
                     'USER PID PPID NAME\nu0_z218 1 0 some.process\n',
                     'USER PID PPID NAME\nroot x 0 init\n',
                     'USER PID PPID NAME\nroot 1 0 init\nroot 1 0 init\n'):
            with self.subTest(text=text):
                pair = samples()
                pair[0]['processes'] = ps_capture(text)
                self.blocked(pair)

    def test_ps_header_without_actual_process_rows_is_incomplete(self):
        pair = samples()
        for item in pair:
            item['processes'] = ps_capture('USER PID PPID NAME\n')
        self.blocked(pair)

    def test_process_capture_failure_diagnostic_or_truncation_blocks(self):
        for key, value in (('exitCode', 1), ('stderr', 'error'), ('complete', False),
                           ('truncated', True), ('stdoutSHA256', '0' * 64), ('stdoutBytes', 1)):
            with self.subTest(field=key):
                pair = samples()
                pair[0]['processes'][key] = value
                self.blocked(pair)

    def test_service_app_null_alone_is_not_enough(self):
        pair = samples()
        pair[0]['services'] = synthetic_capture('app=null\n')
        self.blocked(pair)

    def test_active_service_association_blocks_independent_of_ps(self):
        pair = samples()
        text = pair[0]['services']['stdout'].replace('app=null', 'app=ProcessRecord{abcdef 5000:worker/u0a218}')
        pair[0]['services'] = synthetic_capture(text)
        self.blocked(pair)

    def test_missing_stage_collection_or_instability_blocks(self):
        for key, value in (('complete', False), ('stable', False), ('truncated', True),
                           ('stderr', 'permission denied'), ('exitCode', 1)):
            with self.subTest(field=key):
                pair = samples()
                pair[0]['inventory']['collection'][key] = value
                self.blocked(pair)

    def test_unknown_stage_and_unknown_child_remain_blocked_not_deleted(self):
        pair = samples()
        inventory = pair[0]['inventory']
        inventory['entries'] = [{'path': gate.STAGE.ROOT, 'type': 'directory', 'device': 3,
                                 'inode': 10, 'size': 0, 'mode': 'synthetic'},
                                {'path': gate.STAGE.ROOT + '/unknown', 'type': 'regular',
                                 'device': 3, 'inode': 11, 'size': 0, 'mode': 'synthetic'}]
        before = copy.deepcopy(pair)
        self.blocked(pair)
        self.assertEqual(before, pair)

    def test_revoked_or_nonterminal_stage_never_bypasses_existing_verifier(self):
        sys.path.insert(0, str(OLD))
        from test_stage_check import terminal, ID, ROOT
        for status in ('REVOKED', 'FD_SENT', 'DELIVERY_UNKNOWN'):
            with self.subTest(status=status):
                pair = samples()
                stage = terminal()
                stage['collection'] = pair[0]['inventory']['collection']
                stage['json'][ROOT + '/' + ID + '/state.json'].update(requestStatus=status, pin=True)
                pair[0]['inventory'] = stage
                self.blocked(pair)

    def test_terminal_stage_uses_unchanged_owner_receipt_inode_checks(self):
        sys.path.insert(0, str(OLD))
        from test_stage_check import terminal, ID, ROOT
        pair = samples()
        for sample in pair:
            stage = terminal()
            stage['collection'] = sample['inventory']['collection']
            sample['inventory'] = stage
        self.assertEqual('PREFLIGHT_ELIGIBLE', gate.evaluate_preflight(pair)['classification'])
        pair[1]['inventory']['json'][ROOT + '/' + ID + '/owner.json']['stageInode'] += 1
        self.blocked(pair)

    def test_namespace_ancestor_symlink_blocks_even_without_stage(self):
        pair = samples()
        pair[0]['inventory']['entries'] = [{'path': 'files', 'type': 'symlink'}]
        self.blocked(pair)

    def test_missing_or_unknown_installed_identity_blocks(self):
        for package in gate.KNOWN:
            with self.subTest(package=package):
                pair = samples()
                pair[0]['packages'][package]['apks'][0]['sha256'] = '0' * 64
                self.blocked(pair)
        pair = samples()
        del pair[0]['packages'][gate.PRODUCTION]
        self.blocked(pair)

    def test_package_name_presence_and_path_must_match_identity(self):
        for key, value in (('package', 'com.example.wrong'), ('present', False), ('present', 1)):
            with self.subTest(field=key, value=value):
                pair = samples()
                pair[0]['packages'][gate.PROBE][key] = value
                self.blocked(pair)
        pair = samples()
        pair[0]['packages'][gate.PROBE]['apks'][0]['codePath'] = '/tmp/wrong.apk'
        self.blocked(pair)

    def test_service_apk_path_must_equal_actual_installed_probe_path(self):
        pair = samples()
        for item in pair:
            item['packages'][gate.PROBE]['apks'][0]['codePath'] = (
                '/data/app/~~different/' + gate.PROBE + '-different/base.apk')
        self.blocked(pair)

    def test_uid_collisions_invalid_types_or_second_snapshot_change_block(self):
        for value in (10216, True, None, '10218'):
            with self.subTest(uid=value):
                pair = samples()
                pair[1]['packages'][gate.PROBE]['uid'] = value
                self.blocked(pair)

    def test_incorrect_or_missing_environment_blocks(self):
        for key, value in (('serial', 'emulator-5554'), ('avd', 'other'), ('api', 26), ('bootComplete', '0')):
            with self.subTest(field=key):
                pair = samples()
                pair[0]['environment'][key] = value
                self.blocked(pair)

    def test_one_snapshot_reused_id_or_overlapping_times_block(self):
        self.blocked(samples()[:1])
        pair = samples()
        pair[1]['sampleId'] = pair[0]['sampleId']
        self.blocked(pair)
        pair = samples()
        pair[1]['startedAtUtc'] = pair[0]['startedAtUtc']
        self.blocked(pair)

    def test_two_service_or_process_states_disagree_stop(self):
        pair = samples()
        pair[1]['services'] = synthetic_capture(pair[1]['services']['stdout'].replace('17f772c', '17f772d'))
        self.blocked(pair)
        pair = samples()
        pair[1]['processes'] = ps_capture('USER PID PPID NAME\nu0_a218 3929 528 ' + gate.PROBE + '\n')
        self.blocked(pair)

    def test_two_individually_safe_stage_snapshots_must_still_match(self):
        pair = samples()
        pair[1]['inventory']['entries'] = [{'path': gate.STAGE.ROOT, 'type': 'directory',
                                           'device': 3, 'inode': 10, 'size': 0, 'mode': 'synthetic'}]
        self.assertEqual('PASS', gate.STAGE.validate_snapshot(pair[1]['inventory'])['status'])
        self.blocked(pair)

    def test_old_actual_f1b_evidence_remains_incomplete_not_android_eligible(self):
        import json
        evidence = OLD / 'evidence/f1b-20261017-01'
        actual = json.loads((evidence / 'preflight-services.json').read_text('utf-8'))
        self.assertNotIn('stdout', actual)
        pair = samples()
        pair[0]['services'] = actual
        self.blocked(pair)
        self.assertFalse((evidence / 'preflight-files.json').exists())

    def test_gate_cannot_be_given_precomputed_stage_pass_instead_of_facts(self):
        pair = samples()
        pair[0]['inventory'] = {'status': 'PASS'}
        self.blocked(pair)


if __name__ == '__main__':
    unittest.main(verbosity=2)
