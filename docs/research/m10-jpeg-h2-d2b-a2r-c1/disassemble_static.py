"""Read existing SDK class bytecode with JDK javap; never run an SDK entrypoint."""
from pathlib import Path
import hashlib
import json
import subprocess

HERE = Path(__file__).resolve().parent
LIB = Path('E:/CODEX/.codex/research-env/lithic-a2r-api26/sdk/cmdline-tools/latest/lib')
JAVAP = Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1/bin/javap.exe')


def main():
    out = HERE / 'evidence/tool-static-followup'
    out.mkdir(parents=True, exist_ok=False)
    records = []
    targets = (
        ('locations-abstract', 'common/tools.common.jar', 'com.android.prefs.AbstractAndroidLocations'),
        ('sdk-handler', 'sdklib/sdklib.core.jar', 'com.android.sdklib.repository.AndroidSdkHandler'),
        ('sdk-handler-companion', 'sdklib/sdklib.core.jar', 'com.android.sdklib.repository.AndroidSdkHandler$Companion'),
    )
    for name, relative, cls in targets:
        jar = LIB / relative
        argv = [str(JAVAP), '-classpath', str(jar), '-c', '-p', cls]
        result = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30)
        (out / (name + '.stdout.txt')).write_bytes(result.stdout)
        (out / (name + '.stderr.log')).write_bytes(result.stderr)
        records.append({'argv': argv, 'exitCode': result.returncode,
                        'inputBytes': jar.stat().st_size, 'inputSha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
                        'stdoutBytes': len(result.stdout), 'stdoutSha256': hashlib.sha256(result.stdout).hexdigest(),
                        'stderrBytes': len(result.stderr), 'stderrSha256': hashlib.sha256(result.stderr).hexdigest()})
    (out / 'commands.json').write_text(json.dumps({'staticOnly': True, 'targetClassesExecuted': False,
                                                 'commands': records}, indent=2) + '\n', encoding='utf-8')
    if any(r['exitCode'] != 0 for r in records):
        raise SystemExit('Static reader failure; inspect raw evidence')
    print('3 static class reads completed; no Android/SDK entrypoint executed')


if __name__ == '__main__':
    main()
