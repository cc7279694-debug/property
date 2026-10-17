"""Run only Python host tests with a subprocess execution tripwire and exact IDs."""
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
sys.path.insert(0, str(HERE))
sys.path.append(str(HERE.parent / 'm10-jpeg-r3-f1b'))


class RecordedResult(unittest.TextTestResult):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.methods = []

    def addSuccess(self, test):
        super().addSuccess(test)
        self.methods.append({'id': test.id(), 'status': 'PASS'})

    def addFailure(self, test, err):
        super().addFailure(test, err)
        self.methods.append({'id': test.id(), 'status': 'FAIL'})

    def addError(self, test, err):
        super().addError(test, err)
        self.methods.append({'id': test.id(), 'status': 'ERROR'})

    def addSkip(self, test, reason):
        super().addSkip(test, reason)
        self.methods.append({'id': test.id(), 'status': 'SKIP', 'reason': reason})

    def addSubTest(self, test, subtest, err):
        super().addSubTest(test, subtest, err)
        if err is not None and not any(item['id'] == test.id() for item in self.methods):
            self.methods.append({'id': test.id(), 'status': 'FAIL'})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tag', required=True)
    args = parser.parse_args()
    if not args.tag.replace('-', '').isalnum():
        parser.error('simple unique evidence tag required')
    output = HERE / 'evidence'
    output.mkdir(exist_ok=True)
    log_path, result_path = output / (args.tag + '.log'), output / (args.tag + '.json')
    if log_path.exists() or result_path.exists():
        parser.error('evidence is immutable; choose a new tag')
    files = list(HERE.glob('*.py')) + [HERE.parent / 'm10-jpeg-r3-f1b' / name
                                      for name in ('verify.py', 'stage_check.py', 'test_stage_check.py')]
    files.append(HERE.parent / 'm10-jpeg-r3-f1b/evidence/f1b-20261017-01/blocked-service-record-detail.txt')
    inputs = {str(path.relative_to(REPO)).replace('\\', '/'):
              {'bytes': path.stat().st_size, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
              for path in sorted(files)}
    suite = unittest.TestSuite()
    for name in ('test_service_record', 'test_gate', 'test_stage_check'):
        suite.addTests(unittest.defaultTestLoader.loadTestsFromName(name))
    stream = io.StringIO()
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    with patch('subprocess.run', side_effect=AssertionError('HOST_ONLY: subprocess execution forbidden')):
        result = unittest.TextTestRunner(stream=stream, verbosity=2, resultclass=RecordedResult).run(suite)
    text = stream.getvalue()
    log_path.write_text(text, encoding='utf-8', newline='\n')
    ids = [item['id'] for item in result.methods]
    unique = len(set(ids)) == len(ids) == result.testsRun
    counts = {status: sum(item['status'] == status for item in result.methods)
              for status in ('PASS', 'FAIL', 'ERROR', 'SKIP')}
    report = {'scope': 'HOST_ONLY_SYNTHETIC_AND_LOCKED_FIXTURES_NO_ANDROID_CALLS',
              'startedAtUtc': started, 'python': platform.python_version(), 'command': sys.argv,
              'testMethods': result.testsRun, 'uniqueMethods': len(set(ids)), 'counts': counts,
              'status': 'PASS' if result.wasSuccessful() and unique else 'FAIL',
              'subprocessTripwireEnabled': True, 'methods': result.methods, 'inputs': inputs,
              'logSHA256': hashlib.sha256(log_path.read_bytes()).hexdigest()}
    result_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(text, end='')
    print(json.dumps({'status': report['status'], 'counts': counts, 'uniqueMethods': len(set(ids))}))
    return 0 if report['status'] == 'PASS' else 1


if __name__ == '__main__':
    raise SystemExit(main())
