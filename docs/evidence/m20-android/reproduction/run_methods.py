"""Bounded M20 single-method runs. No installation, permission or cleanup actions."""
import datetime
import hashlib
import json
import pathlib
import re
import subprocess
import sys
import time

ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p / 'gradlew.bat').is_file())
BASE = ROOT / 'docs/evidence/m20-android'
ADB = pathlib.Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe')
SERIAL = 'emulator-5704'
AVD = 'Lithic_M20_API36_20261022'
RUNNER = 'com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner'
TIMEOUT_SECONDS = 180
COMPLETION_RUN = 'c9dc7cf5-54e3-42b2-b48a-5a63aef04816'
ORIGINAL = (
    ('data/backup/M20BackupRestoreTest.kt', 4),
    ('data/local/M20DatabaseMigrationTest.kt', 2),
    ('data/settings/M20ReminderSettingsTest.kt', 1),
    ('feature/settings/M20NotificationSettingsUiTest.kt', 7),
    ('feature/settings/M20NotificationSettingsViewModelTest.kt', 2),
    ('platform/notification/M20ReminderWorkerTest.kt', 6),
)
EXTRAS = {
    'gallery': ('feature/settings/M20NotificationGalleryTest.kt',
                'capture_actual_denial_and_360dp_local200_controls'),
    'platform': ('platform/notification/M20RealPlatformTest.kt',
                 'real_persistent_queue_publishes_q04_notice_and_saves_one_receipt'),
    'platform-recheck': ('platform/notification/M20RealPlatformTest.kt',
                         'real_persistent_queue_publishes_q04_notice_and_saves_one_receipt'),
    'completion': ('platform/notification/M20PlatformCompletionTest.kt',
                   'finish_existing_published_platform_run_without_new_work'),
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def file_sha(path):
    value = hashlib.sha256()
    with pathlib.Path(path).open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            value.update(block)
    return value.hexdigest()


def write_json(path, value):
    with pathlib.Path(path).open('x', encoding='utf-8') as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2)


def selection(group):
    selected = []
    sources = (ORIGINAL if group == 'original' else (ORIGINAL[-1],)
               if group == 'worker-recheck' else ((EXTRAS[group][0], 1),))
    for relative, count in sources:
        source = ROOT / 'app/src/androidTest/java/com/cooper/lithic' / relative
        raw = source.read_bytes()
        text = raw.decode('utf-8-sig')
        package = re.findall(r'^package\s+([\w.]+)\s*$', text, re.MULTILINE)
        methods = re.findall(r'@Test\s+fun\s+([A-Za-z_][A-Za-z_0-9]*)\s*\(', text)
        assert len(package) == 1 and len(methods) == count and len(set(methods)) == count, source
        if group in EXTRAS:
            assert methods == [EXTRAS[group][1]], 'Unexpected extra method'
        selected.extend(dict(class_name=package[0] + '.' + source.stem,
                             method=name, source=str(source.relative_to(ROOT)).replace('\\', '/'),
                             source_sha256=digest(raw)) for name in methods)
    assert len(selected) == (22 if group == 'original' else 6 if group == 'worker-recheck' else 1)
    return selected


def validate_correction(predecessor_bytes, correction):
    """Only the test APK bytes may change; the original proof remains immutable."""
    original = json.loads(predecessor_bytes.decode('utf-8-sig'))
    assert correction['predecessor_sha256'] == digest(predecessor_bytes), 'Wrong predecessor bytes'
    for field in ('status', 'serial', 'api', 'avd', 'boot_id', 'instrumentation'):
        assert correction[field] == original[field], ('Correction identity differs', field)
    for field in ('package', 'sha256', 'uid', 'certificate_sha256'):
        assert correction['app'][field] == original['app'][field], ('App changed', field)
    for field in ('package', 'uid', 'certificate_sha256'):
        assert correction['test'][field] == original['test'][field], ('Test identity changed', field)
    assert correction['test']['sha256'] != original['test']['sha256'], 'Correction did not change Test APK'


def local_admission():
    proofs = {}
    names = ['device-admission.json', 'installed-proof.json']
    correction = BASE / 'device' / 'installed-correction-proof.json'
    if correction.exists():
        names.append(correction.name)
    platform_correction = BASE / 'device' / 'installed-platform-correction-proof.json'
    if platform_correction.exists():
        assert correction.is_file(), 'Second correction requires the first immutable correction'
        names.append(platform_correction.name)
    for name in names:
        path = BASE / 'device' / name
        raw = path.read_bytes()
        proof = json.loads(raw.decode('utf-8-sig'))
        assert proof['status'] == 'PASS' and proof['serial'] == SERIAL
        assert proof['api'] == 36 and proof['avd'] == AVD
        assert re.fullmatch(r'[0-9a-fA-F-]{36}', proof['boot_id'])
        proofs[name] = dict(value=proof, sha256=digest(raw), bytes=len(raw))
    device = proofs['device-admission.json']['value']
    installed = proofs['installed-proof.json']['value']
    if correction.name in proofs:
        replacement = proofs[correction.name]['value']
        validate_correction((BASE / 'device' / 'installed-proof.json').read_bytes(), replacement)
        installed = replacement
    if platform_correction.name in proofs:
        replacement = proofs[platform_correction.name]['value']
        validate_correction(correction.read_bytes(), replacement)
        installed = replacement
    assert device['boot_id'] == installed['boot_id']
    assert installed['instrumentation'] == RUNNER
    for label, package, relative in (
        ('app', 'com.cooper.lithic.debug', 'app/build/outputs/apk/debug/app-debug.apk'),
        ('test', 'com.cooper.lithic.debug.test', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'),
    ):
        item = installed[label]
        assert item['package'] == package
        assert type(item['uid']) is int and 10000 <= item['uid'] < 100000
        assert re.fullmatch(r'[0-9a-f]{64}', item['sha256'])
        assert re.fullmatch(r'[0-9a-f]{64}', item['certificate_sha256'])
        assert file_sha(ROOT / relative) == item['sha256'], ('Local APK differs', label)
    assert installed['app']['uid'] != installed['test']['uid']
    assert installed['app']['certificate_sha256'] == installed['test']['certificate_sha256']
    assert ADB.is_file()
    return proofs


def evaluate(stdout, stderr, exit_code, timed_out, class_name, method):
    """Require one matching JUnit start/end, not just an outer command exit."""
    reasons = []
    if timed_out:
        reasons.append('HOST_ADB_TIMEOUT_DEVICE_INSTRUMENTATION_STATE_UNKNOWN')
    if exit_code != 0:
        reasons.append('NONZERO_OR_UNKNOWN_OUTER_EXIT')
    if stderr:
        reasons.append('NONEMPTY_STDERR')
    try:
        text = stdout.decode('utf-8', errors='strict').replace('\r\n', '\n')
    except UnicodeDecodeError:
        return dict(status='UNKNOWN', reasons=reasons + ['INVALID_STDOUT_UTF8'])
    if len(stdout) > 8 * 1024 * 1024:
        reasons.append('UNEXPECTED_OUTPUT_SIZE_RAW_PRESERVED')
    blocks = []
    fields = {}
    for line in text.splitlines():
        if line.startswith('INSTRUMENTATION_STATUS: '):
            key_value = line[len('INSTRUMENTATION_STATUS: '):]
            if '=' in key_value:
                key, value = key_value.split('=', 1)
                if key in fields:
                    reasons.append('DUPLICATE_STATUS_FIELD')
                fields[key] = value
        elif line.startswith('INSTRUMENTATION_STATUS_CODE: '):
            try:
                code = int(line.split(':', 1)[1].strip())
            except ValueError:
                reasons.append('INVALID_STATUS_CODE')
                code = None
            blocks.append((code, fields))
            fields = {}
    if fields:
        reasons.append('UNTERMINATED_STATUS_BLOCK')
    tests = []
    for code, fields in blocks:
        if code in (-1, -2, -3, -4):
            reasons.append('JUNIT_FAILURE_OR_SKIP')
        if 'class' in fields or 'test' in fields:
            tests.append((code, fields))
    if [code for code, _ in tests] != [1, 0]:
        reasons.append('NOT_EXACTLY_ONE_ORDERED_TEST_START_AND_SUCCESS_END')
    for _, fields in tests:
        if fields.get('class') != class_name or fields.get('test') != method:
            reasons.append('TEST_IDENTITY_MISMATCH')
        if fields.get('numtests') != '1' or fields.get('current') != '1':
            reasons.append('TEST_COUNT_MISMATCH')
    if re.findall(r'^INSTRUMENTATION_CODE:\s*(-?\d+)\s*$', text, re.MULTILINE) != ['-1']:
        reasons.append('FINAL_INSTRUMENTATION_CODE_NOT_MINUS_ONE')
    if len(re.findall(r'^OK\s*\(1 test\)\s*$', text, re.MULTILINE)) != 1:
        reasons.append('FINAL_JUNIT_OK_ONE_TEST_MISSING_OR_REPEATED')
    if re.search(r'INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|FAILURES!!!|Process crashed|shortMsg=|INSTRUMENTATION_STATUS: stack=|INSTRUMENTATION_STATUS: (?:failure|error)=', text, re.IGNORECASE):
        reasons.append('FAILURE_OR_ABORT_EVIDENCE')
    return dict(status='PASS' if not reasons else ('UNKNOWN' if timed_out else 'FAIL'),
                reasons=list(dict.fromkeys(reasons)), junit_method=class_name + '#' + method,
                matched_start_end=len(tests))


def command_for(group, item):
    command = [str(ADB), '-s', SERIAL, 'shell', 'am', 'instrument', '-w', '-r',
               '-e', 'class', item['class_name'] + '#' + item['method']]
    if group == 'completion':
        command.extend(['-e', 'm20PlatformRun', COMPLETION_RUN])
    return command + [RUNNER]


def execute(group):
    selected = selection(group)
    proofs = local_admission()
    output = BASE / 'tests' / group
    output.mkdir(parents=True, exist_ok=False)
    write_json(output / 'claim.json', dict(group=group, serial=SERIAL, avd=AVD,
               runner=RUNNER, methods=selected, proofs=proofs, adb_sha256=file_sha(ADB),
               at=datetime.datetime.now(datetime.timezone.utc).isoformat(), maximum_attempts_per_method=1,
               timeout_seconds=TIMEOUT_SECONDS, extra_methods_counted_in_original=False))
    results = []
    for index, item in enumerate(selected):
        fresh = local_admission()
        assert {key: value['sha256'] for key, value in fresh.items()} == {
            key: value['sha256'] for key, value in proofs.items()}, 'Admission proof changed'
        directory = output / f'method-{index:02d}'
        directory.mkdir(exist_ok=False)
        target = item['class_name'] + '#' + item['method']
        command = command_for(group, item)
        write_json(directory / 'claim.json', dict(command=command, method=item,
                   attempt=1, at=datetime.datetime.now(datetime.timezone.utc).isoformat()))
        started = time.monotonic()
        timed_out = False
        try:
            completed = subprocess.run(command, capture_output=True, timeout=TIMEOUT_SECONDS)
            stdout, stderr, code = completed.stdout, completed.stderr, completed.returncode
        except subprocess.TimeoutExpired as failure:
            stdout, stderr, code = failure.stdout or b'', failure.stderr or b'', None
            timed_out = True
        (directory / 'stdout.txt').write_bytes(stdout)
        (directory / 'stderr.txt').write_bytes(stderr)
        result = evaluate(stdout, stderr, code, timed_out, item['class_name'], item['method'])
        result.update(command=command, exit_code=code, timed_out=timed_out,
                      wall_seconds=time.monotonic() - started,
                      stdout_bytes=len(stdout), stdout_sha256=digest(stdout),
                      stderr_bytes=len(stderr), stderr_sha256=digest(stderr))
        write_json(directory / 'result.json', result)
        results.append(result)
        print(result['status'], target, flush=True)
        if result['status'] != 'PASS':
            write_json(output / 'summary.json', dict(status='BLOCKED', results=results,
                       planned=len(selected), not_run=len(selected) - len(results), original=group == 'original'))
            return 2
    write_json(output / 'summary.json', dict(status='PASS', results=results, planned=len(selected),
               not_run=0, original=group == 'original'))
    return 0


if __name__ == '__main__':
    if len(sys.argv) != 2 or sys.argv[1] not in ('original', 'worker-recheck', 'gallery', 'platform', 'completion', 'platform-recheck'):
        raise SystemExit('Usage: run_methods.py original|worker-recheck|gallery|platform|completion|platform-recheck (exclusive groups, no retries)')
    raise SystemExit(execute(sys.argv[1]))
