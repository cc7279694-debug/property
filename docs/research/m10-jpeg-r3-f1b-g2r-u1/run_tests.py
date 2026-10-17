"""HOST_ONLY: original 81 identities plus U1 tests; no subprocess may run."""
import argparse
import datetime
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import platform
import sys
import unittest
from unittest.mock import patch

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
G1 = HERE.parent / 'm10-jpeg-r3-f1b-g1'
sys.path[:0] = [str(HERE), str(G1), str(HERE.parent / 'm10-jpeg-r3-f1b')]
spec = importlib.util.spec_from_file_location('original_runner', G1 / 'run_host_tests.py')
original = importlib.util.module_from_spec(spec)
spec.loader.exec_module(original)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tag', required=True, choices=('red', 'green', 'final'))
    args = parser.parse_args()
    output = HERE / 'evidence'
    log_path, json_path = output / (args.tag + '.log'), output / (args.tag + '.json')
    if log_path.exists() or json_path.exists():
        parser.error('immutable evidence tag already used')
    files = list(G1.glob('*.py')) + list(HERE.glob('*.py')) + [
        HERE.parent / 'm10-jpeg-r3-f1b' / name for name in ('verify.py', 'stage_check.py', 'test_stage_check.py')]
    files += [HERE / 'evidence/uid-20261017-04/uid-diagnosis.json',
              HERE.parent / 'm10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked-service-record-detail.txt']
    inputs = {p.relative_to(REPO).as_posix(): {'bytes': p.stat().st_size,
              'sha256': hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(files)}
    suite = unittest.TestSuite()
    for name in ('test_service_record', 'test_gate', 'test_stage_check', 'test_uid'):
        suite.addTests(unittest.defaultTestLoader.loadTestsFromName(name))
    stream = io.StringIO()
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    with patch('subprocess.run', side_effect=AssertionError('HOST_ONLY: subprocess forbidden')):
        result = unittest.TextTestRunner(stream=stream, verbosity=2,
                                        resultclass=original.RecordedResult).run(suite)
    log_path.write_text(stream.getvalue(), encoding='utf-8', newline='\n')
    ids = [item['id'] for item in result.methods]
    old = json.loads((G1 / 'evidence/green-final.json').read_text(encoding='utf-8'))
    old_ids = {item['id'] for item in old['methods']}
    counts = {s: sum(x['status'] == s for x in result.methods) for s in ('PASS', 'FAIL', 'ERROR', 'SKIP')}
    passes = {x['id'] for x in result.methods if x['status'] == 'PASS'}
    report = {'scope': 'HOST_ONLY_NO_ANDROID_COMMANDS', 'startedAtUtc': started,
              'python': platform.python_version(), 'command': sys.argv,
              'testMethods': result.testsRun, 'uniqueMethods': len(set(ids)), 'counts': counts,
              'originalG1Identities': len(old_ids), 'originalG1Retained': old_ids <= set(ids),
              'originalG1Passed': len(old_ids & passes), 'subprocessTripwireEnabled': True,
              'methods': result.methods, 'inputs': inputs,
              'logSHA256': hashlib.sha256(log_path.read_bytes()).hexdigest(),
              'status': 'PASS' if result.wasSuccessful() and len(set(ids)) == len(ids) == result.testsRun
                       and len(old_ids) == 81 and old_ids <= passes else 'FAIL'}
    json_path.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: report[k] for k in ('status', 'counts', 'uniqueMethods', 'originalG1Passed')}))
    return 0 if report['status'] == 'PASS' else 1


if __name__ == '__main__':
    raise SystemExit(main())
