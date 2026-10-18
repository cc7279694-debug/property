"""A2-R host-only closing metadata. No ADB, mutation of AVD, or cleanup."""
from pathlib import Path
import datetime
import hashlib
import json
import subprocess

HERE = Path(__file__).resolve().parent
ENV = Path('E:/CODEX/.codex/research-env/lithic-a2r-api26')


def main():
    out = HERE / 'evidence/closing-host-snapshot'
    out.mkdir(parents=True, exist_ok=False)
    result = {'startedUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'status': 'BLOCKED_AVD_CREATION', 'avdCreateCommands': 1,
              'emulatorStarts': 0, 'adbCommands': 0, 'apkInstalls': 0,
              'activityStarts': 0, 'originalApi36OrH2Accessed': False,
              'cleanupOrRetryAfterAvdCreateFailure': False, 'paths': []}
    for relative in ('avds', 'avd-data', 'a2r-smoke-state', 'android-user', 'emulator-home'):
        root = ENV / relative
        paths = [root, *root.rglob('*')] if root.exists() else [root]
        for p in paths:
            row = {'path': str(p.relative_to(ENV)), 'exists': p.exists()}
            if p.exists():
                s = p.lstat()
                row.update(directory=p.is_dir(), fileBytes=s.st_size, attributes=getattr(s, 'st_file_attributes', None))
                if p.is_file() and p.name.endswith(('.ini', '.json', '.properties')) and s.st_size < 65536:
                    row['sha256'] = hashlib.sha256(p.read_bytes()).hexdigest()
            result['paths'].append(row)
    ps = "$r=@(Get-CimInstance Win32_Process | Where-Object {$_.Name -match '^(emulator|qemu-system|java|javaw|android-cli|android).*\\.exe$' -and $_.CommandLine -match 'Lithic_A2R_API26_20261018|lithic-a2r-api26|(?<![0-9])5610(?![0-9])'} | Select-Object ProcessId,ParentProcessId,Name,CommandLine); ConvertTo-Json -InputObject $r -Depth 4 -Compress"
    argv = ['powershell.exe', '-NoProfile', '-NonInteractive', '-Command', ps]
    r = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30)
    (out / 'target-processes.stdout.json').write_bytes(r.stdout)
    (out / 'target-processes.stderr.log').write_bytes(r.stderr)
    result['hostQuery'] = {'argv': argv, 'exitCode': r.returncode,
                           'stdoutBytes': len(r.stdout), 'stdoutSha256': hashlib.sha256(r.stdout).hexdigest(),
                           'stderrBytes': len(r.stderr), 'stderrSha256': hashlib.sha256(r.stderr).hexdigest()}
    result['targetProcesses'] = json.loads(r.stdout.decode('utf-8-sig')) if r.returncode == 0 else 'UNKNOWN'
    result['avdIniExists'] = (ENV / 'avds/Lithic_A2R_API26_20261018.ini').exists()
    result['avdDataExists'] = (ENV / 'avd-data/Lithic_A2R_API26_20261018.avd').exists()
    result['startAttemptExists'] = (ENV / 'a2r-smoke-state/start-attempt.json').exists()
    result['hostLaunchAttemptExists'] = (ENV / 'a2r-smoke-state/host-launch-attempt.json').exists()
    result['endedUtc'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
