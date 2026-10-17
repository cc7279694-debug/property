"""Fixed pure-host unittest entry; never invokes Android tools or research APKs."""
from pathlib import Path
import ast
import datetime
import hashlib
import json
import os
import subprocess
import sys

HERE = Path(__file__).resolve().parent


def main():
    output = HERE / 'evidence/green-final'
    output.mkdir(exist_ok=False)
    inputs = {name: {'bytes': (HERE / name).stat().st_size,
                     'sha256': hashlib.sha256((HERE / name).read_bytes()).hexdigest()}
              for name in ('fence_model.py', 'test_fence_model.py', 'verify_models.py')}
    tree = ast.parse((HERE / 'test_fence_model.py').read_text())
    methods = [c.name + '.' + m.name for c in tree.body if isinstance(c, ast.ClassDef)
               for m in c.body if isinstance(m, ast.FunctionDef) and m.name.startswith('test_')]
    command = [sys.executable, '-B', '-X', 'utf8', '-m', 'unittest', '-v', 'test_fence_model']
    result = subprocess.run(command, cwd=HERE, capture_output=True, timeout=30,
                            env=dict(os.environ, PYTHONIOENCODING='utf-8'))
    raw = result.stdout + result.stderr
    (output / 'unittest.log').write_bytes(raw)
    value = {'scope': 'PURE_HOST_MODEL_ONLY', 'atUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
             'python': sys.version, 'argv': command, 'exitCode': result.returncode,
             'inputs': inputs, 'uniqueMethods': len(methods), 'methods': methods,
             'abstractPostFailureSchedules': 120, 'recoveryFacts': 20,
             'logBytes': len(raw), 'logSHA256': hashlib.sha256(raw).hexdigest(),
             'androidExecution': False, 'h2Lifecycle': 'BLOCKED_UNCHANGED'}
    (output / 'result.json').write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')
    assert len(methods) == len(set(methods)) == 22
    assert result.returncode == 0 and b'Ran 22 tests' in raw and b'\nOK' in raw
    print(json.dumps({'status': 'PASS_MODEL_ONLY', 'uniqueMethods': 22,
                      'schedules': 120, 'androidCalls': 0, 'output': str(output)}, ensure_ascii=True))


if __name__ == '__main__':
    main()
