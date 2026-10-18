"""Record A2-R license stop using the installed SDK canonicalizer. No acceptance."""
from pathlib import Path
import datetime
import hashlib
import json
import os
import shutil
import subprocess
import urllib.request
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
METADATA = HERE / 'evidence/official-metadata-20261018T023649.606169Z'
ENV_ROOT = Path('E:/CODEX/.codex/research-env/lithic-a2r-api26')
OLD_SDK = Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
JDK = Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1')
PACKAGE = 'system-images;android-26;google_apis;x86_64'
SOURCE = '349264371b8b9849dfe83a5ddee74f204129d4ad'


def meta(data):
    return {'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}


def main():
    out = HERE / 'evidence/license-capacity-gate'
    out.mkdir(parents=True, exist_ok=False)
    license_paths = [OLD_SDK / 'licenses/android-sdk-license',
                     Path('C:/Users/CDD/AppData/Local/Android/Sdk/licenses/android-sdk-license')]
    before = {str(p): (p.read_bytes() if p.is_file() else None) for p in license_paths}
    libs = OLD_SDK / 'cmdline-tools/latest/lib'
    cp = os.pathsep.join(str(libs / p) for p in (
        'repository/repository-core.jar',
        'external/jakarta/xml/bind/jakarta.xml.bind-api/2.3.2/jakarta.xml.bind-api-2.3.2.jar'))
    argv = [str(JDK / 'bin/java.exe'), '--class-path', cp, str(HERE / 'LicenseCanonical.java'),
            str(METADATA / 'android-sdk-license.txt')]
    start = datetime.datetime.now(datetime.timezone.utc).isoformat()
    r = subprocess.run(argv, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE, timeout=30)
    (out / 'canonicalizer.stdout.log').write_bytes(r.stdout)
    (out / 'canonicalizer.stderr.log').write_bytes(r.stderr)
    canonical = r.stdout.decode('ascii').strip()
    if r.returncode != 0 or len(canonical) != 40 or any(c not in '0123456789abcdef' for c in canonical):
        raise ValueError('Canonicalizer did not return exactly one SHA-1')
    raw = (METADATA / 'sys-img2-3.xml').read_bytes()
    root = ET.fromstring(raw)
    target = [p for p in root if p.tag.rsplit('}', 1)[-1] == 'remotePackage' and p.get('path') == PACKAGE]
    if len(target) != 1:
        raise ValueError('Expected unique locked package')
    complete = target[0].find('archives/archive/complete')
    archive_url = 'https://dl.google.com/android/repository/sys-img/google_apis/' + complete.findtext('url')
    size = int(complete.findtext('size'))
    head = {'method': 'HEAD', 'url': archive_url, 'imageBodyDownloaded': False}
    try:
        with urllib.request.urlopen(urllib.request.Request(archive_url, method='HEAD'), timeout=30) as response:
            head.update(status=response.status, finalUrl=response.url,
                        contentLength=response.headers.get('Content-Length'),
                        contentType=response.headers.get('Content-Type'))
    except Exception as e:
        head['error'] = type(e).__name__ + ': ' + str(e)
    accepted = []
    for p in license_paths:
        data = before[str(p)]
        after = p.read_bytes() if p.is_file() else None
        if data != after:
            raise ValueError('License file changed during read-only check')
        hashes = [] if data is None else [line.strip() for line in data.decode('ascii').splitlines() if line.strip()]
        accepted.append({'path': str(p), 'exists': data is not None,
                         'file': None if data is None else meta(data), 'acceptedHashes': hashes,
                         'matchesRequiredCanonicalHash': canonical in hashes, 'unchanged': True})
    inventory = []
    for p in sorted(ENV_ROOT.rglob('*')):
        if p.is_symlink():
            raise ValueError('Unexpected symlink in dedicated environment')
        inventory.append({'path': str(p.relative_to(ENV_ROOT)), 'directory': p.is_dir(),
                          **({} if p.is_dir() else meta(p.read_bytes()))})
    result = {'startedUtc': start, 'endedUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'sourceCommit': SOURCE, 'status': 'BLOCKED_NEW_LICENSE',
              'reason': 'Required canonical license hash is not present in existing acceptance records',
              'package': PACKAGE, 'revision': 16, 'channel': 'channel-0',
              'officialMetadata': {'path': str(METADATA / 'sys-img2-3.xml'), **meta(raw)},
              'archive': {'url': archive_url, 'declaredBytes': size,
                          'declaredSha1': complete.findtext('checksum'), 'declaredSha1IsNotMeasured': True,
                          'head': head},
              'budget': {'bytes': 3 * 1024 ** 3, 'archiveWithinBudget': size <= 3 * 1024 ** 3,
                         'archiveMiB': round(size / 1024 ** 2, 3),
                         'installedImageAndAvdPeak': 'NOT_RUN',
                         'fullInstructionBudgetDefinition': 'UNAVAILABLE_CONTENT_REFERENCE_ONLY'},
              'freeSpace': {drive: shutil.disk_usage(drive).free for drive in ('C:/', 'E:/')},
              'license': {'id': 'android-sdk-license', 'datePrintedInText': 'January 16, 2019',
                          'canonicalSha1': canonical, 'canonicalizer': 'Actual installed SDK TrimStringAdapter.unmarshal + UTF8 SHA1',
                          'argv': argv, 'exitCode': r.returncode, 'stdout': meta(r.stdout), 'stderr': meta(r.stderr),
                          'acceptanceRecords': accepted, 'acceptedByThisTask': False,
                          'researchSdkLicenseFileExists': (ENV_ROOT / 'sdk/licenses/android-sdk-license').exists()},
              'dedicatedEnvironment': {'logicalRoot': str(ENV_ROOT), 'resolvedRoot': str(ENV_ROOT.resolve()),
                                       'totalFileBytes': sum(row.get('bytes', 0) for row in inventory),
                                       'inventory': inventory},
              'imageInstallInvoked': False, 'avdCreatedOrStarted': False, 'adbInvoked': False,
              'apkInstalledOrLaunched': False, 'originalApi36OrH2Accessed': False}
    if any(row['matchesRequiredCanonicalHash'] for row in accepted):
        raise ValueError('No longer a new-license blocker; re-review before proceeding')
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
