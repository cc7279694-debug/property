"""H2 evidence-only launcher: one locked preflight and one locked ending.

Observes the same subprocess result without modifying it or parser rules.
Full ps/dumpsys/private metadata remain in memory. No second ps for diagnosis.
"""
import argparse
import collections
import datetime
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import runpy
import subprocess
import sys
import time

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
F1B = HERE.parent / 'm10-jpeg-r3-f1b'
SOURCE = '0dc49621f44f31eed0cdfc4d0dbb56d8756b667a'
ADB = 'C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe'
SERIAL = 'emulator-5596'


def utc():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('phase', choices=('preflight', 'ending'))
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    output = args.output.resolve()
    if output.parent != (F1B / 'evidence').resolve() or not output.is_dir():
        parser.error('fresh H2 evidence directory must already be exclusively created')
    contract = json.loads((output / 'h2-contract.json').read_text(encoding='utf-8'))
    if contract['sourceCommit'] != SOURCE or contract['branch'] != 'research/m10-jpeg-strict-r3-f1b-h2':
        parser.error('wrong contract')
    for name, data in contract['lockedScripts'].items():
        if sha((REPO / name).read_bytes()) != data['sha256']:
            parser.error('locked source changed')
    report_path = output / ('h2-' + args.phase + '-execution.json')
    with report_path.open('x', encoding='utf-8') as handle:
        json.dump({'status': 'ATTEMPT_STARTED_NO_RETRY', 'phase': args.phase, 'startedAtUtc': utc()}, handle)
    if args.phase == 'preflight':
        entry, argv = F1B / 'verify.py', ['preflight', '--output', str(output)]
    else:
        if not (output / 'h2-preflight-execution.json').exists():
            parser.error('ending requires this unique preflight attempt record')
        entry, argv = HERE.parent / 'm10-jpeg-r3-f1b-g2/end_protection.py', ['--output', str(output)]
    spec = importlib.util.spec_from_file_location('u2_locked_gate', HERE.parent / 'm10-jpeg-r3-f1b-g1/gate.py')
    gate = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(gate)
    real_run = subprocess.run
    commands = []
    started = utc()
    sequence = 0

    def observe(command, *positional, **keywords):
        nonlocal sequence
        # Only the exact trusted read-only call path is executed. This layer
        # records provenance, never converts failure to success or retries.
        if (str(command[0]).replace('\\', '/') != ADB or command[1:3] != ['-s', SERIAL]):
            raise RuntimeError('unexpected subprocess outside fixed ADB')
        sequence += 1
        item = {'index': sequence, 'phase': args.phase, 'startedAtUtc': utc(), 'argv': command}
        commands.append(item)
        begin = time.monotonic()
        try:
            result = real_run(command, *positional, **keywords)
        except subprocess.TimeoutExpired as error:
            raw, err = error.stdout or b'', error.stderr or b''
            item.update(exitCode=None, timedOut=True, complete=False,
                        stdoutBytes=len(raw), stdoutSHA256=sha(raw), stderrBytes=len(err), stderrSHA256=sha(err))
            raise
        else:
            raw, err = result.stdout, result.stderr
            item.update(exitCode=result.returncode, timedOut=False, complete=True,
                        stdoutBytes=len(raw), stdoutSHA256=sha(raw), stderrBytes=len(err), stderrSHA256=sha(err))
            if command[3:] == ['shell', 'ps', '-A', '-o', 'USER,PID,PPID,NAME']:
                # Counts/tokens only, no unrelated name/PID leaves memory.
                labels = collections.Counter()
                malformed = 0
                uwb = 0
                try:
                    lines = raw.decode('utf-8', errors='strict').splitlines()
                    header = bool(lines and lines[0].split() == ['USER', 'PID', 'PPID', 'NAME'])
                    for line in lines[1:]:
                        fields = line.split()
                        if len(fields) != 4:
                            malformed += 1
                            continue
                        label = fields[0]
                        uwb += label == 'uwb'
                        try:
                            gate.resolve_ps_uid(label)
                        except ValueError:
                            safe = label if re.fullmatch(r'[A-Za-z0-9_-]{1,64}', label) else 'TOKEN_WITHHELD:' + sha(label.encode())
                            labels[safe] += 1
                    item['psEvidence'] = {'headerMatches': header, 'uwbCount': uwb,
                        'uwbMappedUID': gate.resolve_ps_uid('uwb'), 'unknownUIDLabels': dict(labels),
                        'malformedRowCount': malformed, 'fullProcessRowsArchived': False}
                except UnicodeError:
                    item['psEvidence'] = {'strictDecoding': 'FAILED', 'fullProcessRowsArchived': False}
            return result  # the unchanged object and unchanged bytes
        finally:
            item.update(finishedAtUtc=utc(), wallMs=round((time.monotonic() - begin) * 1000, 3))
            with (output / 'h2-command-integrity.jsonl').open('a', encoding='utf-8', newline='\n') as handle:
                handle.write(json.dumps(item, ensure_ascii=True) + '\n')

    code = None
    error = None
    old_argv = sys.argv
    try:
        subprocess.run = observe
        sys.argv = [str(entry), *argv]
        try:
            runpy.run_path(str(entry), run_name='__main__')
            code = 0
        except SystemExit as stopped:
            code = stopped.code
    except Exception as stopped:
        error = {'type': type(stopped).__name__, 'detail': str(stopped)}
        code = 2
    finally:
        subprocess.run = real_run
        sys.argv = old_argv
        report = {'phase': args.phase, 'startedAtUtc': started, 'finishedAtUtc': utc(),
                  'entry': entry.relative_to(REPO).as_posix(), 'argv': argv,
                  'entrySHA256': sha(entry.read_bytes()), 'launcherSHA256': sha(Path(__file__).read_bytes()),
                  'entryReturnCode': code, 'unexpectedException': error,
                  'adbCommands': len(commands), 'preflightRetried': False,
                  'fullPrivateOutputArchived': False, 'parserRulesChanged': False,
                  'resultObjectPassedThroughUnchanged': True,
                  'status': 'ENTRY_COMPLETED' if code == 0 else 'BLOCKED'}
        report_path.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    return code if type(code) is int else 2


if __name__ == '__main__':
    raise SystemExit(main())
