"""Verify already-downloaded official source metadata. Never build/install code."""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import json
import tarfile

parser = argparse.ArgumentParser()
parser.add_argument('--upstream-dir', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
up = args.upstream_dir
release = json.loads((up / 'release-3.2.0.json').read_text(encoding='utf-8-sig'))
tag = json.loads((up / 'tag-3.2.0.json').read_text(encoding='utf-8-sig'))
latest = json.loads((up / 'latest.json').read_text(encoding='utf-8-sig'))
signature = json.loads((up / 'gpg-verification.json').read_text(encoding='utf8'))
assert latest['tag_name'] == release['tag'] == '3.2.0'
assert not latest['prerelease'] and not latest['draft']
assert tag['object']['sha'] == 'c85e6b905bf237038faa936dab160ebfc5da0344'
assert signature['result'] == 'PASS' and signature['signatureExit'] == 0
assert signature['signingKeyFingerprint'] == '0338C8D8D9FDA62CF9C421BD7EC2DBB6F4DBF434'
assert signature['signingKeyFingerprint'] in signature['validsig']
assets = []
for name in ('libjpeg-turbo-3.2.0.tar.gz', 'libjpeg-turbo-3.2.0.tar.gz.sig'):
    a = next(a for a in release['assets'] if a['name'] == name)
    b = (up / name).read_bytes()
    sha = hashlib.sha256(b).hexdigest()
    assert len(b) == a['size'] and 'sha256:' + sha == a['digest']
    assets.append({**a, 'actualSha256': sha, 'actualBytes': len(b), 'match': True,
                   'committed': False, 'executed': False})
files = []
with tarfile.open(up / 'libjpeg-turbo-3.2.0.tar.gz') as archive:
    members = archive.getmembers()
    for member in members:
        path = PurePosixPath(member.name)
        assert not path.is_absolute() and '..' not in path.parts and ':' not in member.name
    for name in ('BUILDING.md', 'LICENSE.md', 'README.ijg', 'CMakeLists.txt',
                 'src/turbojpeg.h', 'src/turbojpeg.c', 'src/turbojpeg-mp.c',
                 'src/jmemnobs.c', 'src/jmemmgr.c', 'src/jdhuff.c', 'src/example.c'):
        b = archive.extractfile('libjpeg-turbo-3.2.0/' + name).read()
        files.append({'file': name, 'bytes': len(b), 'sha256': hashlib.sha256(b).hexdigest(),
                      'sourceUrl': 'https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/' + name})
out = {'kind': 'OFFICIAL_SOURCE_SUPPLY_CHAIN_ONLY', 'result': 'PASS',
       'release': release['html_url'], 'releasePublishedUtc': release['published_at'],
       'latestAtQuery': latest, 'tagCommit': tag['object']['sha'], 'assets': assets,
       'signature': signature, 'sourceTarEntries': len(members), 'archiveBulkExtracted': False,
       'selectedSourceViewsInIgnoredBuild': True,
       'scope': 'Read regular members only; no installer/binary executed, source compiled, decoder run or system keyring changed.',
       'sourceFilesInspected': files, 'nativeDecoderVerdict': 'NOT_RUN',
       'securityPolicy': {'sourceUrl': 'https://github.com/libjpeg-turbo/libjpeg-turbo/blob/3.2.0/.github/SECURITY.md',
                          'notInOfficialTar': True, 'scope': 'Official GitHub tag read separately; not an archive member hash.'},
       'officialSignaturePolicy': 'https://libjpeg-turbo.org/Downloads/DigitalSignatures'}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(out, indent=2) + '\n', encoding='utf8')
print(json.dumps({'result': out['result'], 'tagCommit': out['tagCommit'],
                  'sourceTarEntries': out['sourceTarEntries'], 'assets': assets,
                  'gpgFingerprint': signature['signingKeyFingerprint'],
                  'nativeDecoderVerdict': 'NOT_RUN'}, indent=2))
