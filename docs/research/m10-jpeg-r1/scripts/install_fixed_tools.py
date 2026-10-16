"""R1-only explicit official SDK install. Requires prior scoped user consent.

No licenses command, yes-pipe, SDK update, global PATH or App mutation.
Captures even a failed installer result; installed versions need separate checks.
"""
from pathlib import Path
import argparse, datetime, hashlib, json, os, subprocess, time

ROOT = Path(__file__).resolve().parents[4]
SDK = Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
EVIDENCE = ROOT / 'docs/research/m10-jpeg-r1/evidence'
JDK = Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1')

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--direct', action='store_true', help='Use installed official Android CLI with slash package IDs shown by its own SDK list')
    args = parser.parse_args()
    stem = 'install-direct' if args.direct else 'install'
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    record = {
        'userConsent': '同意，仅限这两项研究工具',
        'scope': ['ndk;28.2.13676358', 'cmake;3.22.1'],
        'licenseCommandOrBulkAcceptance': False,
        'startedUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'argv': [str(SDK / 'cmdline-tools/latest/bin/sdkmanager.bat'),
                 '--sdk_root=' + str(SDK), '--install',
                 'ndk;28.2.13676358', 'cmake;3.22.1', '--channel=0'],
    }
    if args.direct:
        record['argv'] = [str(SDK / 'cmdline-tools/latest/bin/android.exe'),
                          '--sdk=' + str(SDK), 'sdk', 'install',
                          'ndk/28.2.13676358', 'cmake/3.22.1']
        record['reason'] = 'sdkmanager shim lost semicolon package components; use same bundled official successor and exact stable IDs from SDK list'
    env = os.environ.copy()
    env['JAVA_HOME'] = str(JDK)
    start = time.perf_counter()
    try:
        with (EVIDENCE / (stem + '.stdout.log')).open('wb') as out, (EVIDENCE / (stem + '.stderr.log')).open('wb') as err:
            result = subprocess.run(record['argv'], cwd=ROOT, env=env, input=b'',
                                    stdout=out, stderr=err, timeout=3600, check=False)
        record['exitCode'] = result.returncode
    except Exception as error:
        record['driverException'] = type(error).__name__ + ': ' + str(error)
        raise
    finally:
        record['wallSeconds'] = time.perf_counter() - start
        for name in (stem + '.stdout.log', stem + '.stderr.log'):
            p = EVIDENCE / name
            if p.exists():
                record[name] = {'bytes': p.stat().st_size, 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()}
        (EVIDENCE / (stem + '-command.json')).write_text(json.dumps(record, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        print(json.dumps(record, ensure_ascii=True))

if __name__ == '__main__':
    main()
