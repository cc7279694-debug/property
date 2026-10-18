"""C1 only: read scoped environment and new research SDK/marker metadata.

No Android/SDK command is executed. No global variable or research artifact is
modified; the sole output is a new, non-overwriting C1 evidence directory.
"""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import subprocess

HERE = Path(__file__).resolve().parent
RESEARCH = Path('E:/CODEX/.codex/research-env/lithic-a2r-api26')
REPO = HERE.parents[2]
SCRIPT = REPO / 'docs/research/m10-jpeg-h2-d2b-a2r-api26-isolated/smoke_isolated.py'


def digest(path):
    raw = path.read_bytes()
    return {'bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('phase', choices=('before', 'after'))
    args = parser.parse_args()
    out = HERE / 'evidence' / ('readonly-' + args.phase)
    out.mkdir(parents=True, exist_ok=False)
    query = "$r=@(); foreach($scope in @('Process','User','Machine')) { foreach($name in @('ANDROID_HOME','ANDROID_SDK_ROOT','ANDROID_USER_HOME','ANDROID_AVD_HOME','ANDROID_EMULATOR_HOME','ANDROID_SDK_HOME','JAVA_HOME')) {$r += [PSCustomObject]@{scope=$scope;name=$name;value=[Environment]::GetEnvironmentVariable($name,$scope)}} }; $r|ConvertTo-Json -Depth 3 -Compress"
    argv = ['powershell.exe', '-NoProfile', '-NonInteractive', '-Command', query]
    r = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30)
    (out / 'environment.stdout.json').write_bytes(r.stdout)
    (out / 'environment.stderr.log').write_bytes(r.stderr)
    if r.returncode != 0:
        raise RuntimeError('Read-only environment query failed')
    environment = json.loads(r.stdout.decode('utf-8-sig'))
    rows = []
    for relative in ('sdk', 'avds', 'avd-data', 'a2r-smoke-state', 'android-user', 'emulator-home', 'legacy-home'):
        base = RESEARCH / relative
        paths = [base, *base.rglob('*')] if base.exists() else [base]
        for path in sorted(paths):
            row = {'path': str(path.relative_to(RESEARCH)).replace('\\', '/'), 'exists': path.exists()}
            if path.exists():
                s = path.lstat()
                row.update(directory=path.is_dir(), bytes=s.st_size, attributes=getattr(s, 'st_file_attributes', None), mtimeNs=s.st_mtime_ns)
            rows.append(row)
    raw_rows = json.dumps(rows, sort_keys=True, separators=(',', ':')).encode('utf-8')
    selected = []
    for relative in ('sdk/cmdline-tools/latest/source.properties', 'sdk/emulator/source.properties',
                     'sdk/platform-tools/source.properties', 'sdk/system-images/android-26/google_apis/x86_64/source.properties',
                     'sdk/licenses/android-sdk-license', 'a2r-smoke-state/create-attempt.json'):
        p = RESEARCH / relative
        selected.append({'path': relative, 'exists': p.exists(), **(digest(p) if p.is_file() else {})})
    result = {'phase': args.phase, 'timeUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'environment': environment, 'query': {'argv': argv, 'exitCode': r.returncode,
              'stdoutBytes': len(r.stdout), 'stdoutSha256': hashlib.sha256(r.stdout).hexdigest(),
              'stderrBytes': len(r.stderr), 'stderrSha256': hashlib.sha256(r.stderr).hexdigest()},
              'researchRoot': str(RESEARCH), 'metadataEntryCount': len(rows),
              'metadataAggregateSha256': hashlib.sha256(raw_rows).hexdigest(),
              'selectedFileHashes': selected, 'smokeScript': digest(SCRIPT),
              'avdCreateOrAndroidToolsExecuted': False, 'originalApi36OrH2Accessed': False}
    if args.phase == 'after':
        before = json.loads((HERE / 'evidence/readonly-before/result.json').read_text())
        result['comparison'] = {'globalAndProcessEnvironmentUnchanged': environment == before['environment'],
                                'researchMetadataUnchanged': result['metadataAggregateSha256'] == before['metadataAggregateSha256'],
                                'selectedFilesUnchanged': selected == before['selectedFileHashes'],
                                'smokeScriptUnchanged': result['smokeScript'] == before['smokeScript']}
        if not all(result['comparison'].values()):
            result['status'] = 'CHANGED_OR_UNKNOWN_REQUIRES_REVIEW'
        else:
            result['status'] = 'PASS_READONLY_COMPARISON'
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: v for k, v in result.items() if k not in ('environment', 'query', 'selectedFileHashes')}, indent=2))


if __name__ == '__main__':
    main()
