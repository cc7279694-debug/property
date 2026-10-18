"""Read-only final isolated SDK namespace/identity/writer check. Never uses ADB."""
from pathlib import Path
import datetime
import hashlib
import json
import stat
import struct
import subprocess
import sys
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
ENV = Path('E:/CODEX/.codex/research-env/lithic-a2r-api26')
SDK = ENV / 'sdk'
IMAGE = SDK / 'system-images/android-26/google_apis/x86_64'


def main():
    out = HERE / 'evidence/pre-avd-storage-gate'
    out.mkdir(parents=True, exist_ok=False)
    result = {'status': 'BLOCKED', 'startedUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'adbInvoked': False, 'avdCreatedOrStarted': False}
    try:
        audit = json.loads((HERE / 'evidence/installed-image-audit/result.json').read_text())
        assert audit['status'] == 'PASS' and audit['allOriginalArchiveFilesSizeAndCrcVerified']
        current = IMAGE
        parents = []
        while True:
            attrs = current.lstat()
            assert not (getattr(attrs, 'st_file_attributes', 0) & stat.FILE_ATTRIBUTE_REPARSE_POINT), current
            parents.append(str(current))
            if current == ENV:
                break
            current = current.parent
        IMAGE.resolve().relative_to(ENV.resolve())
        doc = ET.fromstring((IMAGE / 'package.xml').read_bytes())
        packages = [n for n in doc if n.tag.rsplit('}', 1)[-1] == 'localPackage']
        assert len(packages) == 1
        p = packages[0]
        assert p.get('path') == 'system-images;android-26;google_apis;x86_64'
        values = {}
        for n in p.iter():
            values.setdefault(n.tag.rsplit('}', 1)[-1], []).append(n.text)
        assert values['api-level'] == ['26'] and values['abi'] == ['x86_64']
        tag = p.find('type-details/tag/id')
        assert tag is not None and tag.text == 'google_apis'
        assert p.findtext('revision/major') == '16'
        assert p.find('uses-license').get('ref') == 'android-sdk-license'
        ps = "$r=@(Get-CimInstance Win32_Process | Where-Object {$_.Name -match '^(java|javaw|android-cli|android)\\.exe$' -and $_.CommandLine -like '*lithic-a2r-api26*'} | Select-Object ProcessId,ParentProcessId,Name,CommandLine); ConvertTo-Json -InputObject $r -Depth 4 -Compress"
        argv = ['powershell.exe', '-NoProfile', '-NonInteractive', '-Command', ps]
        r = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30)
        (out / 'target-sdk-writers.stdout.json').write_bytes(r.stdout)
        (out / 'target-sdk-writers.stderr.log').write_bytes(r.stderr)
        assert r.returncode == 0
        writers = json.loads(r.stdout.decode('utf-8-sig'))
        assert not writers, 'A research SDK writer remains; do not kill or continue'
        reference = json.loads((HERE / 'evidence/license-correction-archive-metadata/result.json').read_text())
        tail = Path(reference['archiveMetadata']['tailFile']).read_bytes()
        assert hashlib.sha256(tail).hexdigest() == reference['archiveMetadata']['tail']['sha256']
        cd = reference['archiveMetadata']['endOfCentralDirectory']
        begin = cd['centralDirectoryOffset'] - (980740438 - len(tail))
        raw_cd = tail[begin:begin + cd['centralDirectoryBytes']]
        offset = 0
        entries = []
        for _ in range(cd['entriesTotal']):
            f = struct.unpack_from('<4s6H3I5H2I', raw_cd, offset)
            assert f[0] == b'PK\x01\x02'
            name = raw_cd[offset + 46:offset + 46 + f[10]].decode('utf-8')
            assert name.startswith('x86_64/') and '..' not in name.split('/') and '\\' not in name
            mode = f[15] >> 16
            assert stat.S_IFMT(mode) in (0, stat.S_IFREG, stat.S_IFDIR)
            entries.append({'name': name, 'crc32': format(f[7], '08x'), 'compressedBytes': f[8],
                            'uncompressedBytes': f[9], 'unixMode': mode})
            offset += 46 + f[10] + f[11] + f[12]
        assert offset == len(raw_cd) and len({x['name'] for x in entries}) == len(entries)
        (out / 'official-central-directory.json').write_text(json.dumps({
            'url': 'https://dl.google.com/android/repository/sys-img/google_apis/x86_64-26_r16.zip',
            'offset': cd['centralDirectoryOffset'], 'bytes': len(raw_cd),
            'sha256': hashlib.sha256(raw_cd).hexdigest(), 'rawHex': raw_cd.hex(), 'entries': entries}, indent=2) + '\n', encoding='utf-8')
        result.update(status='PASS', checkedParents=parents, generatedPackageMetadata='PASS',
                      targetedSdkWriters=writers, writerQuery={'argv': argv, 'exitCode': r.returncode,
                      'stdoutBytes': len(r.stdout), 'stdoutSha256': hashlib.sha256(r.stdout).hexdigest()},
                      centralDirectoryNoUnknownEntryTypes=True,
                      imagePostconditionAvailable=True, installerExitStillFailure=True)
    except Exception as e:
        result['error'] = type(e).__name__ + ': ' + str(e)
    result['endedUtc'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))
    return 0 if result['status'] == 'PASS' else 2


if __name__ == '__main__':
    sys.exit(main())
