"""Install only the authorized official API26 r16 image into the new SDK."""
from pathlib import Path
import datetime
import hashlib
import json
import os
import subprocess
import time
import argparse

HERE = Path(__file__).resolve().parent
ENV = Path('E:/CODEX/.codex/research-env/lithic-a2r-api26')
SDK = ENV / 'sdk'
CLI = Path('C:/Users/CDD/.android/bin/android-cli.exe')
OLD_LICENSE = Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk/licenses/android-sdk-license')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--retry-once', action='store_true')
    args = parser.parse_args()
    out = HERE / ('evidence/image-install-retry-1' if args.retry_once else 'evidence/image-install')
    out.mkdir(parents=True, exist_ok=False)
    correction = json.loads((HERE / 'evidence/license-correction-archive-metadata/result.json').read_text())
    assert correction['requiredCanonicalSha1FromActualSdkOnXml'] == '24333f8a63b6825ea9c5514f83c2829b004d1fee'
    assert correction['archiveMetadata']['declaredCompressedBytes'] < 3 * 1024 ** 3
    license_bytes = OLD_LICENSE.read_bytes()
    assert b'24333f8a63b6825ea9c5514f83c2829b004d1fee' in license_bytes
    assert SDK.is_dir() and not SDK.is_symlink()
    if args.retry_once:
        prior = json.loads((HERE / 'evidence/image-install/result.json').read_text())
        assert prior['exitCode'] != 0
        assert 2 * 980740438 + 84612760 + 4 * 1024 ** 2 < 3 * 1024 ** 3
        assert (SDK / 'licenses/android-sdk-license').read_bytes() == license_bytes
    else:
        (SDK / 'licenses').mkdir(exist_ok=False)
        (SDK / 'licenses/android-sdk-license').write_bytes(license_bytes)
    argv = [str(CLI), '--sdk=' + str(SDK), 'sdk', 'install',
            'system-images/android-26/google_apis/x86_64@16.0.0']
    env = os.environ.copy()
    env.update(TEMP=str(ENV / 'temp'), TMP=str(ENV / 'temp'))
    record = {'startedUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'argv': argv, 'budgetApproval': 'User: 只限制下载流量（允许展开及AVD超过3GiB）',
              'downloadBudgetBytes': 3 * 1024 ** 3, 'newLicenseAccepted': False,
              'copiedExistingMatchingAcceptance': hashlib.sha256(license_bytes).hexdigest(),
              'adbInvoked': False, 'avdOrAppStarted': False}
    record['retryOnce'] = args.retry_once
    record['twoFullImageTransfersPlusExistingCliSizeAndMetadataConservativeCeilingBytes'] = 2 * 980740438 + 84612760 + 4 * 1024 ** 2
    (out / 'intent.json').write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8')
    started = time.monotonic()
    try:
        with (out / 'stdout.log').open('wb') as stdout, (out / 'stderr.log').open('wb') as stderr:
            r = subprocess.run(argv, env=env, stdin=subprocess.DEVNULL,
                               stdout=stdout, stderr=stderr, timeout=1200)
        record['exitCode'] = r.returncode
    except Exception as e:
        record['error'] = type(e).__name__ + ': ' + str(e)
    record.update(endedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                  wallSeconds=round(time.monotonic() - started, 3))
    for name in ('stdout', 'stderr'):
        data = (out / (name + '.log')).read_bytes()
        record[name] = {'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}
    record['originalAcceptanceUnchanged'] = OLD_LICENSE.read_bytes() == license_bytes
    (out / 'result.json').write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(record, indent=2))


if __name__ == '__main__':
    main()
