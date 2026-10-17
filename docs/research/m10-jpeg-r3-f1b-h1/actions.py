"""H1 limited research updates. No normal/cancel/kill/cleanup entry exists."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
OUT = HERE.parent / 'm10-jpeg-r3-f1b/evidence/h1-20261017-01'
spec = importlib.util.spec_from_file_location('h1_locked_verifier', HERE.parent / 'm10-jpeg-r3-f1b/verify.py')
V = importlib.util.module_from_spec(spec)
spec.loader.exec_module(V)


def load(name):
    return json.loads((OUT / name).read_text(encoding='utf-8-sig'))


def save(name, value):
    target = OUT / name
    with target.open('x', encoding='utf-8') as handle:
        json.dump(value, handle, indent=2)
        handle.write('\n')


def need(ok, why):
    V.require(ok, why)


class H1(V.Bounded):
    def command(self, args, **kwargs):
        # Mutations are exactly the single phase's locked research update.
        install = args[3:4] == ['install']
        if install:
            proof = load('h1-host-apk-audit.json')['apks'][self.phase]
            need(args == [str(V.S.ADB), '-s', V.S.SERIAL, 'install', '-r', proof['path']],
                 'only this phase locked research APK update allowed')
            need((OUT / ('install-' + self.phase + '-issued.json')).exists(), 'update marker missing')
        try:
            result = (V.D.Diagnostic.command(self, args, **kwargs) if install else
                      super().command(args, **kwargs))
        except Exception:
            raise
        item = {'atUtc': V.S.utc_now(), 'phase': self.phase, 'label': kwargs['label'], 'argv': args,
                'exitCode': result.returncode, 'stdoutBytes': len(result.stdout),
                'stdoutSHA256': hashlib.sha256(result.stdout).hexdigest(),
                'stderrBytes': len(result.stderr), 'stderrSHA256': hashlib.sha256(result.stderr).hexdigest()}
        with (OUT / 'h1-actions-integrity.jsonl').open('a', encoding='utf-8', newline='\n') as handle:
            handle.write(json.dumps(item) + '\n')
        return result


def current_uids():
    before = load('snapshot-before.json')['productionPackage']
    return {V.S.PRODUCTION: before['uid'], **{k: v['uid'] for k, v in load('preflight-pass.json')['installed'].items()}}


def check_idle(d, label):
    identity = d.verify_device()
    need(d.text('shell', 'getprop', 'sys.boot_completed', label=label + '-boot') == '1', 'boot incomplete')
    packages = {p: d.package(p) for p in (V.S.PRODUCTION, V.S.PROBE, V.S.OUTSIDER)}
    uids = current_uids()
    for package, data in packages.items():
        need(data['uid'] == uids[package] and len(data['apks']) == 1 and
             data['apks'][0]['sha256'] in V.G.KNOWN[package], 'installed identity changed/unknown')
    need(len(set(uids.values())) == 3, 'UID collision')
    d.research_uids = {p: uids[p] for p in (V.S.PROBE, V.S.OUTSIDER)}
    processes = V.G.capture(d.adb('shell', 'ps', '-A', '-o', 'USER,PID,PPID,NAME',
                        label=label + '-ps', private_metadata=True), 'ps-user-pid-ppid-name-v1')
    rows = V.G.process_rows(processes, d.research_uids)
    # Installs require no active research app; pre-Outsider also starts idle.
    need(not rows, 'research process still active before permitted mutation')
    services = V.G.capture(d.adb('shell', 'dumpsys', 'activity', 'services', V.S.PROBE,
                        label=label + '-services', private_metadata=True), 'api36-f1b-v1')
    service = V.G.SERVICE.classify_capture(services, uids[V.S.PROBE], uids[V.S.OUTSIDER])
    need(service['classification'] in ('NO_TARGET_RECORD', 'INACTIVE_RECORD_CANDIDATE'),
         'service unsafe/unknown: ' + str(service['reasons']))
    if service['record'] is not None:
        need(service['record']['baseDir'] == packages[V.S.PROBE]['apks'][0]['codePath'], 'service code path conflicts')
    inventory = d.inventory(label + '-files')
    stage, _ = V.G.stage_signature(inventory)
    value = {'atUtc': V.S.utc_now(), 'status': 'PASS', 'identity': identity, 'packages': packages,
             'processes': rows, 'processCapture': {k: v for k, v in processes.items() if k != 'stdout'},
             'service': service, 'serviceCapture': {k: v for k, v in services.items() if k != 'stdout'},
             'stage': stage, 'scope': 'ONE_READONLY_SNAPSHOT_NOT_LEASE_OR_DEATH_PROOF'}
    save(label + '.json', value)
    return value


def outsider_result(d, label):
    # Only this known synthetic result may be read; no generic private file copy.
    script = "if [ ! -e files/result.json ]; then printf 'ABSENT\\n'; elif [ -L files ] || [ -L files/result.json ] || [ ! -f files/result.json ]; then exit 44; else stat -c '%f|%u|%s|%i' files/result.json; fi"
    text = d.text('shell', 'run-as', V.S.OUTSIDER, 'sh', '-c', V.shlex.quote(script), label=label + '-stat')
    if text == 'ABSENT':
        value = {'present': False}
    else:
        fields = text.split('|')
        need(len(fields) == 4 and int(fields[1]) == current_uids()[V.S.OUTSIDER] and
             int(fields[0], 16) & 0xf000 == 0x8000 and 0 < int(fields[2]) <= 8192, 'unsafe/oversize synthetic result')
        result = d.adb('exec-out', 'run-as', V.S.OUTSIDER, 'cat', 'files/result.json', label=label + '-read')
        need(not result.stderr and len(result.stdout) == int(fields[2]), 'result changed/incomplete')
        obj = json.loads(result.stdout.decode('utf-8'), object_pairs_hook=V.no_duplicate)
        allowed = {'uid', 'pid', 'status', 'phase', 'bindingOutcome', 'cleanupStatus', 'bindingDenied',
                   'error', 'bindingReturned', 'cleanupError', 'firstUnexpectedCleanupError'}
        need(set(obj) <= allowed and obj.get('uid') == current_uids()[V.S.OUTSIDER], 'unknown result fields/identity')
        value = {'present': True, 'stat': text, 'bytes': len(result.stdout),
                 'sha256': hashlib.sha256(result.stdout).hexdigest(), 'result': obj}
    save(label + '.json', value)
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('module', choices=('probe', 'outsider'))
    args = parser.parse_args()
    need(not (OUT / 'h1-blocked.json').exists(), 'prior blocker; no continuation')
    need(load('preflight-gate.json')['classification'] == 'PREFLIGHT_ELIGIBLE' and
         load('h1-preflight-execution.json')['entryReturnCode'] == 0, 'fresh H1 preflight required')
    audit = load('h1-host-apk-audit.json')
    need(audit['status'] == 'PASS', 'locked host APK audit absent')
    with (OUT / ('install-' + args.module + '-attempt.json')).open('x', encoding='utf-8') as handle:
        json.dump({'atUtc': V.S.utc_now(), 'module': args.module, 'scope': 'LOCKED_RESEARCH_UPDATE_ONLY'}, handle)
    d = H1(OUT, args.module)
    try:
        if args.module == 'outsider':
            need(load('install-probe-result.json')['status'] == 'PASS', 'Probe must already be verified')
        before = check_idle(d, 'before-install-' + args.module)
        facts = audit['apks'][args.module]
        path = Path(facts['path'])
        need(V.S.sha256_file(path) == facts['sha256'], 'locked APK changed')
        package = facts['package']
        old = before['packages'][package]
        old_digest = old['apks'][0]['sha256']
        need(old_digest in (facts['oldApkSHA256'], facts['sha256']), 'installed bytes lack audited certificate continuity')
        need(facts['oldCertificateMatches'] is True, 'certificate mismatch')
        old_result = outsider_result(d, 'before-outsider-update-result') if args.module == 'outsider' else None
        if old_digest == facts['sha256']:
            observation = 'ALREADY_TARGET_NO_INSTALL'
        else:
            save('install-' + args.module + '-issued.json', {'atUtc': V.S.utc_now(), 'package': package,
                                                          'apkSHA256': facts['sha256'], 'noRetry': True})
            result = d.command([str(V.S.ADB), '-s', V.S.SERIAL, 'install', '-r', str(path)],
                               label='install-' + args.module, timeout=45)
            need(result.returncode == 0 and result.stdout.strip().endswith(b'Success') and not result.stderr,
                 'update failed/unknown; do not retry or update another package')
            observation = 'INSTALL_R_SUCCESS'
        # Immediate post-update checks, before any other package or launch.
        actual = d.package(package)
        need(actual['uid'] == old['uid'] and actual['apks'][0]['sha256'] == facts['sha256'], 'post-update APK/UID mismatch')
        after = check_idle(d, 'after-install-' + args.module)
        before_files = load('before-install-' + args.module + '-files.json')
        after_files = load('after-install-' + args.module + '-files.json')
        need(before_files['entries'] == after_files['entries'] and before_files['json'] == after_files['json'],
             'Probe research files changed during update')
        if args.module == 'outsider':
            after_result = outsider_result(d, 'after-outsider-update-result')
            need(old_result == after_result, 'Outsider synthetic data changed during update')
        save('install-' + args.module + '-result.json', {'status': 'PASS', 'atUtc': V.S.utc_now(),
             'observation': observation, 'installed': actual, 'uidUnchanged': True,
             'probeFilesMetadataUnchanged': True, 'outsiderSyntheticResultUnchanged': args.module == 'outsider',
             'manifestEvidence': 'INSTALLED_SHA_EQUALS_HOST_AUDITED_APK; IDENTICAL_MANIFEST_AND_CERTIFICATE',
             'certificateSHA256': facts['certificateSHA256'], 'postUpdateSnapshot': 'after-install-' + args.module + '.json'})
        print(args.module + ': update identity and post-update safety checks PASS')
        return 0
    except Exception as error:
        save('h1-blocked.json', {'atUtc': V.S.utc_now(), 'phase': args.module, 'status': 'BLOCKED',
                              'detail': str(error), 'noRollbackOrRetry': True})
        print('H1 blocked; preserve partial state and perform only ending protection')
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
