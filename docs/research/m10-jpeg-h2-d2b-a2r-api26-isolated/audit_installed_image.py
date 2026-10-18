"""Read/hash only the newly installed official image before any AVD is created."""
from pathlib import Path
import datetime
import hashlib
import json
import stat
import struct
import zlib
import sys
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
SDK = Path('E:/CODEX/.codex/research-env/lithic-a2r-api26/sdk')
IMAGE = SDK / 'system-images/android-26/google_apis/x86_64'
PACKAGE = 'system-images;android-26;google_apis;x86_64'


def sha_file(path, algorithm='sha256'):
    h = hashlib.new(algorithm)
    with path.open('rb') as f:
        for block in iter(lambda: f.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def main():
    out = HERE / 'evidence/installed-image-audit'
    out.mkdir(parents=True, exist_ok=False)
    result = {'startedUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'status': 'BLOCKED', 'sdk': str(SDK), 'image': str(IMAGE),
              'package': PACKAGE, 'adbInvoked': False, 'avdOrAppStarted': False}
    try:
        install = json.loads((HERE / 'evidence/image-install-retry-1/result.json').read_text())
        assert IMAGE.is_dir()
        IMAGE.resolve().relative_to(SDK.resolve())
        result['installerExitCode'] = install['exitCode']
        result['installerCommandPass'] = install['exitCode'] == 0
        result['postconditionMethod'] = 'Independent all-file size/CRC32 versus bounded official TLS ZIP central directory, plus hashes and SDK identity; never relabels installer exit'
        props = {}
        for line in (IMAGE / 'source.properties').read_text().splitlines():
            if '=' in line:
                k, v = line.split('=', 1)
                props[k.strip()] = v.strip()
        assert props['AndroidVersion.ApiLevel'] == '26'
        assert props['SystemImage.TagId'] == 'google_apis'
        assert props['SystemImage.Abi'] == 'x86_64'
        assert props['Pkg.Revision'] == '16'
        package_xml = ET.fromstring((IMAGE / 'package.xml').read_bytes())
        local = [n for n in package_xml if n.tag.rsplit('}', 1)[-1] == 'localPackage']
        assert len(local) == 1 and local[0].get('path') == PACKAGE
        reference = json.loads((HERE / 'evidence/license-correction-archive-metadata/result.json').read_text())
        tail_path = Path(reference['archiveMetadata']['tailFile'])
        tail = tail_path.read_bytes()
        assert hashlib.sha256(tail).hexdigest() == reference['archiveMetadata']['tail']['sha256']
        tail_headers = (HERE / 'evidence/license-correction-archive-metadata/archive-tail.headers.log').read_bytes()
        assert b'206 Partial Content' in tail_headers
        assert b'Content-Range: bytes 980674902-980740437/980740438' in tail_headers
        cd = reference['archiveMetadata']['endOfCentralDirectory']
        offset = cd['centralDirectoryOffset'] - (980740438 - len(tail))
        archive_crc = {}
        for _ in range(cd['entriesTotal']):
            fields = struct.unpack_from('<4s6H3I5H2I', tail, offset)
            assert fields[0] == b'PK\x01\x02'
            length, extra, comment = fields[10:13]
            name = tail[offset + 46:offset + 46 + length].decode('utf-8')
            assert name not in archive_crc
            archive_crc[name] = fields[7]
            offset += 46 + length + extra + comment
        rows = []
        expected_files = {'package.xml'}
        for entry in reference['archiveMetadata']['entries']:
            name = entry['name']
            assert name.startswith('x86_64/') and '..' not in name.split('/') and '\\' not in name
            if name.endswith('/'):
                continue
            p = IMAGE / name[len('x86_64/'):]
            expected_files.add(name[len('x86_64/'):])
            attrs = p.lstat()
            assert not (getattr(attrs, 'st_file_attributes', 0) & stat.FILE_ATTRIBUTE_REPARSE_POINT)
            assert p.stat().st_size == entry['uncompressedBytes'], name
            h = hashlib.sha256()
            crc = 0
            with p.open('rb') as f:
                for block in iter(lambda: f.read(1024 * 1024), b''):
                    h.update(block)
                    crc = zlib.crc32(block, crc)
            assert crc == archive_crc[name], 'Official archive CRC mismatch: ' + name
            rows.append({'path': str(p), 'bytes': p.stat().st_size, 'sha256': h.hexdigest(),
                         'crc32': format(crc, '08x'), 'officialCentralDirectoryCrc32Matches': True})
        actual_files = set()
        for p in IMAGE.rglob('*'):
            assert not (getattr(p.lstat(), 'st_file_attributes', 0) & stat.FILE_ATTRIBUTE_REPARSE_POINT)
            if p.is_file():
                actual_files.add(str(p.relative_to(IMAGE)).replace('\\', '/'))
        assert actual_files == expected_files, ('Unexpected/missing installed files', actual_files ^ expected_files)
        for name in ('source.properties', 'package.xml'):
            data = (IMAGE / name).read_bytes()
            (out / name).write_bytes(data)
        archives = []
        for p in SDK.rglob('*'):
            if p.is_file() and p.stat().st_size == 980740438:
                actual = sha_file(p, 'sha1')
                archives.append({'path': str(p), 'bytes': p.stat().st_size, 'sha1': actual,
                                 'matchesOfficial': actual == '0eb13cc91cf164b617eaf85e4030289cb18189de'})
                assert actual == '0eb13cc91cf164b617eaf85e4030289cb18189de'
        result.update(status='PASS', apiLevel=26, tag='google_apis', abi='x86_64', revision=16,
                      allOriginalArchiveFilesSizeAndCrcVerified=True,
                      installedFiles=rows, retainedArchiveCandidates=archives,
                      archiveDigestIndependentlyMeasured=bool(archives),
                      archiveDigestLimit='No retained archive means the official installer and installed byte inventory are evidence; do not invent a measured archive digest.')
    except Exception as e:
        result['error'] = type(e).__name__ + ': ' + str(e)
    result['endedUtc'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))
    return 0 if result['status'] == 'PASS' else 2


if __name__ == '__main__':
    sys.exit(main())
