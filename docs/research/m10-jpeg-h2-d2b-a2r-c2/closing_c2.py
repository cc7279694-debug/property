"""Necessary read-only C2 ending checks; never starts/installs/stops an App."""
from pathlib import Path
import json
import smoke_c2 as smoke
from preflight_c2 import old_attempt, environment_query


def main():
    evidence = smoke.Evidence('closing')
    baseline = smoke.load(next((smoke.HERE / 'evidence').glob('preflight-*/old-a2r-host-preservation.json')))
    before_env = smoke.load(next((smoke.HERE / 'evidence').glob('preflight-*/environment.json')))['parentScopes']
    old = old_attempt()
    env = environment_query()
    states = {}
    if smoke.STATE.exists():
        for p in sorted(smoke.STATE.glob('*.json')):
            # Only synthetic C2 intent/result records, no image/userdata bytes.
            states[p.name] = smoke.load(p)
    evidence.record('c2-state-records.json', states)
    result = {'scope': 'READONLY_ENDING', 'oldA2rHostStateUnchanged': old == baseline,
              'oldA2rHostState': old, 'environmentUnchanged': env == before_env,
              'apk': smoke.file_meta(smoke.APK), 'originalApi36OrH2Accessed': False,
              'privateOriginalDataIntegrity': 'NOT_RUN_NOT_ACCESSED'}
    evidence.record('environment.json', env)
    log_snapshots = []
    for live in sorted((smoke.HERE / 'evidence').glob('start-*/emulator.*.log')):
        initial_size = live.stat().st_size
        smoke.require(initial_size <= 16 * 1024 * 1024, 'emulator log exceeds bounded snapshot limit')
        with live.open('rb') as stream:
            raw = stream.read(initial_size)
        name = live.name.replace('.log', '.snapshot.log')
        destination = evidence.out / name
        with destination.open('xb') as stream:
            stream.write(raw)
        log_snapshots.append({'localLivePath': str(live), 'snapshotPath': str(destination),
                              'bytes': len(raw), 'sha256': smoke.digest(raw),
                              'liveBytesAfter': live.stat().st_size,
                              'limit': 'Bounded prefix at ending observation; live file remains local and may grow.'})
    result['emulatorLogSnapshots'] = log_snapshots
    result['apkUnchanged'] = result['apk']['sha256'] == smoke.APK_SHA
    preflight = smoke.load(next((smoke.HERE / 'evidence').glob('preflight-*/locked-input-hashes.json')))
    image = []
    for row in preflight['imageFiles'] + [preflight['generatedMetadata']]:
        p = Path(row['path'])
        entry = {'path': str(p), 'sizeMatches': p.stat().st_size == row['bytes']}
        if p.name in ('source.properties', 'package.xml', 'kernel-ranchu', 'kernel-ranchu-64', 'ramdisk.img'):
            entry['shaMatches'] = smoke.file_meta(p)['sha256'] == row['sha256']
        image.append(entry)
    result['imageEndChecks'] = image
    result['imageEndCheckLimit'] = 'All original sizes; selected metadata/kernel/ramdisk SHA only. Full hashes were checked before create.'
    result['imageEndChecksPassed'] = all(row['sizeMatches'] and row.get('shaMatches', True) for row in image)
    if (smoke.STATE / 'discovery-confirmed.json').is_file() and (smoke.STATE / 'booted.json').is_file():
        result['device'] = smoke.identity(evidence, 'end-protection')
        result['buildProperties'] = {}
        for name in ('ro.build.fingerprint', 'ro.product.cpu.abi', 'ro.kernel.qemu'):
            result['buildProperties'][name] = evidence.adb('property-' + name, ['shell', 'getprop', name]).decode().strip()
        installed = (smoke.STATE / 'install-attempt.json').is_file()
        if installed:
            path = evidence.adb('probe-path-ending', ['shell', 'pm', 'path', smoke.PKG]).decode().strip()
            result['installedPath'] = path
            if path.startswith('package:/data/app/') and '\n' not in path:
                result['installedSha256'] = evidence.adb('probe-sha-ending', ['shell', 'sha256sum', path[8:]]).decode().split()[0]
            result['processes'] = smoke.process_rows(evidence, 'research-processes-ending')
            if (smoke.STATE / 'host-launch-attempt.json').is_file():
                result['files'] = smoke.probe_files(evidence, 'research-files-ending')
    else:
        result['deviceEnding'] = 'NOT_RUN_TARGET_BOOT_OR_DISCOVERY_NOT_CONFIRMED'
        if (smoke.STATE / 'discovery-confirmed.json').is_file():
            # Ending observation only, never a renewed gate or permission to install.
            result['endingState'] = evidence.adb('ending-state', ['get-state'], allow_nonzero=True).decode().strip()
            if result['endingState'] == 'device':
                result['endingAvdName'] = evidence.adb('ending-avd-name', ['emu', 'avd', 'name']).decode().splitlines()
                result['endingApi'] = evidence.adb('ending-api', ['shell', 'getprop', 'ro.build.version.sdk']).decode().strip()
                result['endingBoot'] = evidence.adb('ending-boot', ['shell', 'getprop', 'sys.boot_completed']).decode().strip()
    records = []
    for p in sorted((smoke.HERE / 'evidence').glob('*/[0-9]*-command.json')):
        records.append(smoke.load(p))
    labels = [r['label'] for r in records]
    result['actualOperations'] = {
        'avdCreateCommands': labels.count('avd-create'),
        'emulatorSpawnRecords': int('emulator-spawned.json' in states),
        'apkInstallCommands': labels.count('install-locked-probe'),
        'activityLaunchCommands': labels.count('single-no-command-launch'),
        'globalDeviceDiscoveryCommands': labels.count('single-global-device-discovery'),
    }
    result['status'] = 'PASS' if all(result[k] for k in ('oldA2rHostStateUnchanged', 'environmentUnchanged', 'apkUnchanged', 'imageEndChecksPassed')) else 'UNKNOWN_REQUIRES_REVIEW'
    evidence.record('result.json', result)
    print(json.dumps({'evidence': str(evidence.out), **{k: result[k] for k in ('status', 'oldA2rHostStateUnchanged', 'environmentUnchanged', 'actualOperations')}}))


if __name__ == '__main__':
    main()
