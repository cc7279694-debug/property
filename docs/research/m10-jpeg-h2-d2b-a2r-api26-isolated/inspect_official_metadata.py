"""Read bounded official image metadata; no image install or license acceptance."""
from pathlib import Path
import datetime
import hashlib
import json
import shutil
import urllib.request
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
URL = 'https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml'
PACKAGE = 'system-images;android-26;google_apis;x86_64'


def local(node):
    return node.tag.rsplit('}', 1)[-1]


def main():
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S.%fZ')
    out = HERE / 'evidence' / ('official-metadata-' + stamp)
    out.mkdir(parents=True, exist_ok=False)
    result = {'url': URL, 'startedUtc': stamp, 'package': PACKAGE,
              'downloadBudgetBytes': 3 * 1024 ** 3, 'licensesAccepted': False,
              'imageDownloadInvoked': False, 'installInvoked': False}
    try:
        with urllib.request.urlopen(URL, timeout=45) as response:
            data = response.read(4 * 1024 * 1024 + 1)
            if len(data) > 4 * 1024 * 1024:
                raise ValueError('Metadata exceeds 4MiB limit')
            result.update(httpStatus=response.status, finalUrl=response.url)
        (out / 'sys-img2-3.xml').write_bytes(data)
        result['metadata'] = {'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}
        tree = ET.fromstring(data)
        nodes = [n for n in tree if local(n) == 'remotePackage' and n.get('path') == PACKAGE]
        if len(nodes) != 1:
            raise ValueError('Expected exactly one official target package')
        p = nodes[0]
        result['targetXml'] = ET.tostring(p, encoding='unicode')
        refs = [n.get('ref') for n in p if local(n) == 'uses-license']
        result['licenses'] = []
        for ref in refs:
            licenses = [n for n in tree if local(n) == 'license' and n.get('id') == ref]
            if len(licenses) != 1:
                raise ValueError('Missing or conflicting license')
            text = licenses[0].text or ''
            (out / (ref + '.txt')).write_text(text, encoding='utf-8')
            result['licenses'].append({'id': ref, 'utf8Bytes': len(text.encode('utf-8')),
                'rawSha256': hashlib.sha256(text.encode('utf-8')).hexdigest(),
                'rawTrimmedSha1Candidate': hashlib.sha1(text.strip().encode('utf-8')).hexdigest(),
                'canonicalComparison': 'PENDING_ACTUAL_SDK_CANONICALIZER'})
        result['freeSpace'] = {drive: {'freeBytes': shutil.disk_usage(drive).free} for drive in ('C:/', 'E:/')}
        result['status'] = 'METADATA_READ_REQUIRES_LICENSE_AND_CAPACITY_GATE'
    except Exception as e:
        result.update(status='BLOCKED_METADATA', error=type(e).__name__ + ': ' + str(e))
    result['endedUtc'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
