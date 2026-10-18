"""Index original evidence without copying SDK license bodies into new files."""
from pathlib import Path
import hashlib
import json
import subprocess
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
SOURCE = '4ee27bfcbe5903d6a49d4a92203de8a63686ba66'
OFFICIAL_XML = 'https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml'
SDK_TERMS = 'https://developer.android.com/studio/terms'
PATHS = (
    'docs/research/m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/installed-image-audit/package.xml',
    'docs/research/m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/license-correction-archive-metadata/android-sdk-license.xml-extracted.utf8.txt',
    'docs/research/m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/official-metadata-20261018T023649.606169Z/android-sdk-license.txt',
    'docs/research/m10-jpeg-h2-d2b-a2r-api26-isolated/evidence/official-metadata-20261018T023649.606169Z/sys-img2-3.xml',
    'docs/research/m10-jpeg-r1/evidence/android-sdk-license.txt',
    'docs/research/m10-jpeg-r1/evidence/build-audit-raw/cmake-package.xml',
    'docs/research/m10-jpeg-r1/evidence/build-audit-raw/ndk-package.xml',
)


def git(*args):
    return subprocess.run(['git', *args], cwd=REPO, check=True, stdout=subprocess.PIPE).stdout


def digest(raw):
    return {'bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()}


def main():
    destination = HERE / 'evidence/license-inventory.json'
    rows = []
    for relative in PATHS:
        path = REPO / relative
        raw = path.read_bytes()
        blob = git('show', SOURCE + ':' + relative)
        if raw != blob:
            raise RuntimeError('Local original differs from locked Git blob: ' + relative)
        generated = relative.endswith('package.xml')
        licenses = []
        if path.suffix == '.xml':
            for node in ET.fromstring(raw).iter():
                if node.tag.rsplit('}', 1)[-1] == 'license':
                    body = ''.join(node.itertext()).encode('utf-8')
                    licenses.append({'id': node.get('id'), 'parsedUtf8Text': digest(body)})
        rows.append({'path': relative, 'originalLocalPath': str(path),
                     'sourceCommit': SOURCE,
                     'introducedCommit': git('log', '-1', '--diff-filter=A', '--format=%H', SOURCE, '--', relative).decode().strip(),
                     'gitBlob': git('rev-parse', SOURCE + ':' + relative).decode().strip(),
                     **digest(raw),
                     'artifactType': 'SDK_GENERATED_METADATA_LOCAL_ONLY' if generated else
                         ('ORIGINAL_OFFICIAL_XML_LOCAL_ONLY' if path.suffix == '.xml' else 'EXTRACTED_LICENSE_TEXT_LOCAL_ONLY'),
                     'officialUrls': [SDK_TERMS, OFFICIAL_XML] if 'a2r' in relative else [SDK_TERMS, 'https://dl.google.com/android/repository/repository2-1.xml'],
                     'xmlLicensesMetadataOnly': licenses,
                     'disposition': 'UNTRACK_WITH_LOCAL_ORIGINAL_PRESERVED',
                     'historyStillContainsOriginal': True})
    changed = git('diff-tree', '--no-commit-id', '--name-status', '-r', SOURCE).decode().splitlines()
    result = {'sourceCommit': SOURCE, 'scope': 'A2-R 86-file commit plus inherited current-HEAD duplicates',
              'a2rCommitFileCount': len(changed),
              'a2rAdded': sum(row.startswith('A\t') for row in changed),
              'a2rModified': sum(row.startswith('M\t') for row in changed),
              'bodyContainingFileCount': len(rows), 'files': rows,
              'redactedXmlCreated': False, 'historyRewritten': False,
              'note': 'Metadata and original byte hashes only. This index is not an original XML or a replacement license text.'}
    with destination.open('x', encoding='utf-8', newline='\n') as stream:
        stream.write(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'files': len(rows), 'a2rCommitFiles': len(changed),
                      'allLocalBytesMatchLockedGitBlobs': True}, indent=2))


if __name__ == '__main__':
    main()
