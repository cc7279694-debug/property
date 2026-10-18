"""C2 preflight: local files/environment only. No SDK/device entrypoint."""
import hashlib
import json
import os
from pathlib import Path
import socket
import subprocess
import xml.etree.ElementTree as ET
import smoke_c2 as smoke

HERE = Path(__file__).resolve().parent
OLD = Path('E:/CODEX/.codex/research-env/lithic-a2r-api26')
MARKER_SHA = '5fc9cf89b241adbfc6349f3c50cc8fb09c3c15d8b4bcc8b91074dd4c77fb2509'


def old_attempt():
    marker = OLD / 'a2r-smoke-state/create-attempt.json'
    data = OLD / 'avd-data/Lithic_A2R_API26_20261018.avd'
    descriptor = OLD / 'avds/Lithic_A2R_API26_20261018.ini'
    return {'marker': smoke.file_meta(marker), 'dataPath': str(data),
            'dataDirectory': data.is_dir(), 'dataEntries': sorted(p.name for p in data.iterdir()),
            'descriptorExists': descriptor.exists()}


def environment_query():
    query = "$r=@(); foreach($scope in @('Process','User','Machine')) { foreach($name in @('ANDROID_HOME','ANDROID_SDK_ROOT','ANDROID_USER_HOME','ANDROID_AVD_HOME','ANDROID_EMULATOR_HOME','ANDROID_SDK_HOME','ANDROID_PREFS_ROOT','JAVA_HOME','JAVA_OPTS','AVDMANAGER_OPTS','JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS','ADB_SERVER_SOCKET','ANDROID_ADB_SERVER_PORT')) {$r += [PSCustomObject]@{scope=$scope;name=$name;value=[Environment]::GetEnvironmentVariable($name,$scope)}} }; $r|ConvertTo-Json -Depth 3 -Compress"
    r = subprocess.run(['powershell.exe', '-NoProfile', '-NonInteractive', '-Command', query],
                       capture_output=True, timeout=30, check=True)
    rows = json.loads(r.stdout.decode('utf-8-sig'))
    flags = {'JAVA_OPTS', 'AVDMANAGER_OPTS', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS'}
    for row in rows:
        if row['name'] in flags:
            value = row.pop('value')
            row.update(present=bool(value), bytes=len((value or '').encode()),
                       sha256=hashlib.sha256(value.encode()).hexdigest() if value else None)
    return rows


def main():
    evidence = smoke.Evidence('preflight')
    # Evidence gets a stable alias only after actual success. Existing output is refused.
    try:
        smoke.require(not smoke.ROOT.exists(), 'C2 root already exists')
        for path in (smoke.ROOT, smoke.AVD_HOME, smoke.DATA, smoke.USER_HOME, smoke.EMULATOR_HOME, smoke.STATE, smoke.SDK):
            smoke.contained(path)
        env = smoke.environment()
        smoke.require('ANDROID_SDK_HOME' not in env, 'legacy SDK home inherited')
        smoke.require(env['ANDROID_HOME'] == env['ANDROID_SDK_ROOT'] == str(smoke.SDK), 'SDK roots conflict')
        settings = environment_query()
        for row in settings:
            if row['name'] in ('ANDROID_PREFS_ROOT', 'ADB_SERVER_SOCKET', 'ANDROID_ADB_SERVER_PORT'):
                smoke.require(not row.get('value'), 'unexpected path/server override: ' + row['name'])
            if row['name'] in ('JAVA_OPTS', 'AVDMANAGER_OPTS', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS'):
                smoke.require(not row.get('present'), 'unreviewed JVM option variable: ' + row['name'])
        evidence.record('environment.json', {'parentScopes': settings, 'childOverrides': {k: env[k] for k in ('ANDROID_HOME','ANDROID_SDK_ROOT','ANDROID_USER_HOME','ANDROID_AVD_HOME','ANDROID_EMULATOR_HOME','ANDROID_SERIAL','JAVA_HOME')}, 'ANDROID_SDK_HOME_present': False, 'globalEnvironmentModified': False})
        old = old_attempt()
        smoke.require(old['marker']['sha256'] == MARKER_SHA and old['dataDirectory'] and not old['dataEntries'] and not old['descriptorExists'], 'old A2-R host evidence changed/unknown')
        evidence.record('old-a2r-host-preservation.json', old)
        smoke.image_gate()
        audit = smoke.load(smoke.HISTORY / 'evidence/installed-image-audit/result.json')
        checked = []
        for row in audit['installedFiles']:
            p = Path(row['path'])
            p.resolve().relative_to(smoke.IMAGE.resolve())
            smoke.contained(p)
            actual = smoke.file_meta(p)
            smoke.require(actual['sha256'] == row['sha256'] and actual['bytes'] == row['bytes'], 'locked image bytes changed: ' + p.name)
            checked.append(actual)
        smoke.require(len(checked) == 11, 'original image inventory incomplete')
        generated = smoke.IMAGE / 'package.xml'
        smoke.require(smoke.file_meta(generated)['sha256'] == 'aaab7f4381969c6de68a7c081e0d33d167e90ea6eb9047b98e684df5d635e002', 'generated image metadata changed')
        xml = ET.fromstring(generated.read_bytes())
        local = next(n for n in xml.iter() if n.tag.rsplit('}', 1)[-1] == 'localPackage')
        smoke.require(local.get('path') == smoke.PACKAGE, 'installed image package identity mismatch')
        copied = smoke.load(smoke.HISTORY / 'evidence/local-tools-copy/result.json')
        tool_checks = []
        for row in copied['files']:
            p = smoke.SDK / row['path']
            smoke.contained(p)
            actual = smoke.file_meta(p)
            smoke.require(actual['sha256'] == row['sha256'] and actual['bytes'] == row['bytes'], 'copied tool bytes changed: ' + row['path'])
            tool_checks.append(actual)
        evidence.record('locked-input-hashes.json', {'imageFiles': checked, 'generatedMetadata': smoke.file_meta(generated), 'tools': tool_checks, 'licensesCopiedOrAccepted': False, 'downloadedBytes': 0})
        smoke.require(not smoke.host_isolation(evidence)['target'], 'C2 emulator identity already running')
        for port in (smoke.PORT, smoke.PORT + 1):
            with socket.socket() as sock:
                sock.bind(('127.0.0.1', port))
        import shutil
        free = shutil.disk_usage(smoke.ROOT.parent).free
        smoke.require(free > 10 * 1024 ** 3, 'insufficient disk budget for new AVD')
        result = {'status': 'PASS', 'scope': 'HOST_STATIC_PREFLIGHT_ONLY', 'sourceC1': '797eb591cc8293d9d68503243d2a94462fe79610', 'name': smoke.AVD, 'serial': smoke.SERIAL, 'newRoot': str(smoke.ROOT), 'sdk': str(smoke.SDK), 'imageFilesVerified': len(checked), 'toolFilesVerified': len(tool_checks), 'portPairFreeAtCheck': [smoke.PORT, smoke.PORT + 1], 'freeBytes': free, 'sdkEntrypointsExecuted': 0, 'adbInvoked': False}
        evidence.record('result.json', result)
        # Exclusive local alias contains no new SDK/AVD data and no license body.
        alias = HERE / 'evidence/preflight'
        alias.mkdir(exist_ok=False)
        smoke.durable(alias / 'result.json', dict(result, originalEvidence=str(evidence.out)))
        print(json.dumps(result))
    except BaseException as error:
        evidence.record('failure.json', {'status': 'BLOCKED', 'error': str(error), 'noRetry': True})
        raise


if __name__ == '__main__':
    main()
