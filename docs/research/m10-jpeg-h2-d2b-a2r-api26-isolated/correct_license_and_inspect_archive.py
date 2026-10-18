"""Correct XML-vs-CRLF evidence, then bounded HEAD/range metadata only. No install."""
from pathlib import Path
import datetime
import hashlib
import json
import struct
import subprocess
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
META = HERE / 'evidence/official-metadata-20261018T023649.606169Z'
ENV = Path('E:/CODEX/.codex/research-env/lithic-a2r-api26')
URL = 'https://dl.google.com/android/repository/sys-img/google_apis/x86_64-26_r16.zip'
SIZE = 980740438


def meta(data):
    return {'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}


def main():
    out = HERE / 'evidence/license-correction-archive-metadata'
    out.mkdir(parents=True, exist_ok=False)
    xml = (META / 'sys-img2-3.xml').read_bytes()
    node = next(n for n in ET.fromstring(xml) if n.tag.rsplit('}', 1)[-1] == 'license' and n.get('id') == 'android-sdk-license')
    lf = node.text.encode('utf-8')
    (out / 'android-sdk-license.xml-extracted.utf8.txt').write_bytes(lf)
    old = (META / 'android-sdk-license.txt').read_bytes()
    sdk = Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
    libs = sdk / 'cmdline-tools/latest/lib'
    cp = ';'.join(str(libs / p) for p in (
        'repository/repository-core.jar',
        'external/jakarta/xml/bind/jakarta.xml.bind-api/2.3.2/jakarta.xml.bind-api-2.3.2.jar'))
    java = 'C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1/bin/java.exe'
    commands = []

    def run(name, argv, timeout=60):
        start = datetime.datetime.now(datetime.timezone.utc).isoformat()
        r = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                           stdin=subprocess.DEVNULL, timeout=timeout)
        (out / (name + '.stdout.log')).write_bytes(r.stdout)
        (out / (name + '.stderr.log')).write_bytes(r.stderr)
        commands.append({'name': name, 'argv': argv, 'startedUtc': start,
                         'endedUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                         'exitCode': r.returncode, 'stdout': meta(r.stdout), 'stderr': meta(r.stderr)})
        return r

    canonical = run('xml-canonicalizer', [java, '--class-path', cp, str(HERE / 'LicenseCanonical.java'),
                                         str(META / 'sys-img2-3.xml'), '--xml'])
    assert canonical.returncode == 0
    required = canonical.stdout.decode('ascii').strip()
    assert required == '24333f8a63b6825ea9c5514f83c2829b004d1fee'
    accepted_bytes = (sdk / 'licenses/android-sdk-license').read_bytes()
    assert required in accepted_bytes.decode('ascii').split()
    head = run('official-archive-head', ['curl.exe', '--proto', '=https', '--max-time', '45',
               '--head', URL])
    body = ENV / 'download-metadata/archive-tail.bin'
    body.parent.mkdir(parents=True, exist_ok=True)
    assert not body.exists()
    tail = run('official-archive-tail', ['curl.exe', '--proto', '=https', '--max-time', '45',
               '--fail', '--max-filesize', '1048576', '--range', str(SIZE - 65536) + '-' + str(SIZE - 1),
               '--dump-header', str(out / 'archive-tail.headers.log'), '--output', str(body), URL])
    archive = {'headExitCode': head.returncode, 'tailExitCode': tail.returncode,
               'declaredCompressedBytes': SIZE, 'tailFile': str(body)}
    if tail.returncode == 0:
        data = body.read_bytes()
        archive['tail'] = meta(data)
        assert len(data) <= 1048576
        end = data.rfind(b'PK\x05\x06')
        if end >= 0:
            fields = struct.unpack_from('<4s4H2IH', data, end)
            archive['endOfCentralDirectory'] = {'disk': fields[1], 'centralDirectoryDisk': fields[2],
                'entriesOnDisk': fields[3], 'entriesTotal': fields[4], 'centralDirectoryBytes': fields[5],
                'centralDirectoryOffset': fields[6], 'commentBytes': fields[7]}
            begin = fields[6] - (SIZE - len(data))
            if begin >= 0 and begin + fields[5] <= len(data):
                entries = []
                offset = begin
                for _ in range(fields[4]):
                    values = struct.unpack_from('<4s6H3I5H2I', data, offset)
                    assert values[0] == b'PK\x01\x02'
                    name_length, extra_length, comment_length = values[10:13]
                    name = data[offset + 46:offset + 46 + name_length].decode('utf-8')
                    entries.append({'name': name, 'compressedBytes': values[8], 'uncompressedBytes': values[9]})
                    offset += 46 + name_length + extra_length + comment_length
                archive['entries'] = entries
                archive['declaredUncompressedBytes'] = sum(e['uncompressedBytes'] for e in entries)
                archive['centralDirectoryVerifiedBytes'] = offset - begin
    assert (sdk / 'licenses/android-sdk-license').read_bytes() == accepted_bytes
    result = {'status': 'LICENSE_GATE_CORRECTED_MATCHES_EXISTING_ACCEPTANCE',
              'requiredCanonicalSha1FromActualSdkOnXml': required,
              'originalCrlfDerivedHashIsInvalidForXmlLicenseIdentity': '9cb2a03b2d1e90fca50530f79975338d0d278f2c',
              'reason': 'Original Python write_text on Windows converted LF XML text to CRLF before SDK canonicalization',
              'xmlExtractedText': meta(lf), 'originalCrlfFilePreserved': meta(old),
              'onlyNewlineDifference': old.replace(b'\r\n', b'\n') == lf,
              'newLicenseRequired': False, 'licenseAcceptedOrWrittenByThisTask': False,
              'acceptedFileUnchanged': meta(accepted_bytes), 'commands': commands,
              'archiveMetadata': archive, 'imageInstalled': False, 'adbInvoked': False,
              'avdOrAppStarted': False,
              'supersedes': '../license-capacity-gate/result.json (license conclusion only; original remains unchanged)'}
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
