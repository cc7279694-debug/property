"""Offline A2-R evidence/scope verification. No ADB, download, or mutation outside evidence."""
from pathlib import Path
import ast
import datetime
import hashlib
import json
import re
import subprocess
from urllib.parse import unquote

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
SOURCE = '349264371b8b9849dfe83a5ddee74f204129d4ad'
BRANCH = 'research/m10-jpeg-h2-d2b-a2r-api26-isolated'
DOCS = [ROOT / 'docs/CURRENT_STATE.md',
        ROOT / 'docs/research/M10_JPEG_H2_D2B_A2R_EXECUTION.md',
        ROOT / 'docs/research/M10_JPEG_H2_D2B_A2R_RESULTS.md',
        ROOT / 'docs/checkpoints/2026-10-18-M10-JPEG-H2-D2B-A2R.md']


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git(*args):
    r = subprocess.run(['git', '-c', 'core.quotepath=false', *args], cwd=ROOT,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=45)
    if r.returncode:
        raise RuntimeError((args, r.returncode, r.stderr.decode('utf-8', errors='replace')))
    return r.stdout


def main():
    assert git('branch', '--show-current').decode().strip() == BRANCH
    assert git('rev-parse', 'HEAD').decode().strip() == SOURCE, 'Run before the new delivery commit'
    changed = git('diff', '--name-only', '--diff-filter=MDRT', SOURCE).decode('utf-8').splitlines()
    assert changed == ['docs/CURRENT_STATE.md'], 'Unexpected modification of original tracked inputs'
    untracked = git('ls-files', '--others', '--exclude-standard', '-z').decode('utf-8').split('\0')
    allowed = {str(p.relative_to(ROOT)).replace('\\', '/') for p in DOCS[1:]}
    prefix = str(HERE.relative_to(ROOT)).replace('\\', '/') + '/'
    assert all(not p or p in allowed or p.startswith(prefix) for p in untracked)
    added = git('diff', '--name-only', '--diff-filter=A', SOURCE).decode('utf-8').splitlines()
    assert all(p in allowed or p.startswith(prefix) for p in added)
    queries = []
    for directory in sorted((HERE / 'evidence').glob('sdk-query-*')):
        record = json.loads((directory / 'result.json').read_text())
        for name in ('stdout', 'stderr'):
            data = (directory / (name + '.log')).read_bytes()
            assert record[name] == {'bytes': len(data), 'sha256': sha(data)}
        queries.append({'directory': directory.name, 'exitCode': record['exitCode'],
                        'targetLines': record['targetLines']})
    assert [q['exitCode'] for q in queries] == [1, 3221226505]
    assert any('android-26/google_apis/x86_64' in line for line in queries[1]['targetLines'])
    gate = json.loads((HERE / 'evidence/license-capacity-gate/result.json').read_text())
    assert gate['status'] == 'BLOCKED_NEW_LICENSE'
    assert gate['license']['canonicalSha1'] == '9cb2a03b2d1e90fca50530f79975338d0d278f2c'
    assert all(not row['matchesRequiredCanonicalHash'] and row['unchanged']
               for row in gate['license']['acceptanceRecords'])
    assert not gate['license']['acceptedByThisTask']
    assert gate['archive']['declaredBytes'] == 980740438
    for key in ('imageInstallInvoked', 'avdCreatedOrStarted', 'adbInvoked',
                'apkInstalledOrLaunched', 'originalApi36OrH2Accessed'):
        assert gate[key] is False
    for name in ('stdout', 'stderr'):
        data = (HERE / ('evidence/license-capacity-gate/canonicalizer.' + name + '.log')).read_bytes()
        assert gate['license'][name] == {'bytes': len(data), 'sha256': sha(data)}
    md = HERE / 'evidence/official-metadata-20261018T023649.606169Z'
    assert sha((md / 'sys-img2-3.xml').read_bytes()) == gate['officialMetadata']['sha256']
    # Preserve, but never reuse, the initial CRLF-derived false license blocker.
    assert sha((md / 'android-sdk-license.txt').read_bytes()) == '00eceefbf21af958ba76722fdf3330143d7176e4626f341c3d8925a6e40726ea'
    corrected_dir = HERE / 'evidence/license-correction-archive-metadata'
    corrected = json.loads((corrected_dir / 'result.json').read_text())
    assert corrected['requiredCanonicalSha1FromActualSdkOnXml'] == '24333f8a63b6825ea9c5514f83c2829b004d1fee'
    assert corrected['newLicenseRequired'] is False and corrected['onlyNewlineDifference'] is True
    assert sha((corrected_dir / 'android-sdk-license.xml-extracted.utf8.txt').read_bytes()) == '1f8729233617b193fd619213792ae16a41b95d2bbbf525dfe66998252ba68b16'
    image = json.loads((HERE / 'evidence/installed-image-audit/result.json').read_text())
    assert image['status'] == 'PASS' and image['installerCommandPass'] is False
    assert len(image['installedFiles']) == 11 and image['allOriginalArchiveFilesSizeAndCrcVerified']
    storage = json.loads((HERE / 'evidence/pre-avd-storage-gate/result.json').read_text())
    assert storage['status'] == 'PASS' and storage['targetedSdkWriters'] == []
    cd = json.loads((HERE / 'evidence/pre-avd-storage-gate/official-central-directory.json').read_text())
    assert sha(bytes.fromhex(cd['rawHex'])) == cd['sha256'] and len(cd['entries']) == 15
    for folder in ('image-install', 'image-install-retry-1'):
        record = json.loads((HERE / 'evidence' / folder / 'result.json').read_text())
        assert record['exitCode'] == 3221226505
        for stream in ('stdout', 'stderr'):
            data = (HERE / 'evidence' / folder / (stream + '.log')).read_bytes()
            assert record[stream] == {'bytes': len(data), 'sha256': sha(data)}
    closing = json.loads((HERE / 'evidence/closing-host-snapshot/result.json').read_text())
    assert closing['avdCreateCommands'] == 1 and closing['avdIniExists'] is False
    assert closing['targetProcesses'] == [] and closing['avdDataExists'] is True
    assert all(closing[key] == 0 for key in ('emulatorStarts', 'adbCommands', 'apkInstalls', 'activityStarts'))
    create_commands = json.loads((HERE / 'evidence/create-20261018T025903.791601Z/002-command.json').read_text())
    assert create_commands['exitCode'] == 1 and create_commands['label'] == 'avd-create'
    assert not list((HERE / 'evidence').glob('observe-*'))
    assert not list((HERE / 'evidence').glob('start-*'))
    links = []
    for p in DOCS + [HERE / 'USER_SCOPE.md', HERE / 'README.md']:
        for target in re.findall(r'\[[^\]]*\]\(([^)]+)\)', p.read_text(encoding='utf-8')):
            if '://' in target or target.startswith('#'):
                continue
            target = unquote(target.strip('<>').split('#')[0])
            destination = (p.parent / target).resolve()
            self_result = (HERE / 'evidence/delivery-verification.json').resolve()
            assert destination == self_result or destination.exists(), (p, target)
            links.append({'from': str(p.relative_to(ROOT)), 'target': target})
    files = sorted(set(DOCS + [p for p in HERE.rglob('*') if p.is_file()
                             and p.name != 'delivery-verification.json']))
    secrets = re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|ghp_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|sk_live_[A-Za-z0-9]{20,}')
    inventory = []
    for p in files:
        data = p.read_bytes()
        assert p.suffix.lower() not in ('.apk', '.zip', '.dll', '.exe', '.keystore', '.jks'), p
        assert not secrets.search(data), 'Potential secret in ' + str(p)
        if p.suffix == '.py':
            ast.parse(data.decode('utf-8'))
        inventory.append({'path': str(p.relative_to(ROOT)).replace('\\', '/'),
                          'bytes': len(data), 'sha256': sha(data)})
    result = {'status': 'PASS', 'timeUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'branch': BRANCH, 'sourceCommit': SOURCE, 'trackedChanged': changed,
              'stagedNewFilesInAllowedScope': len(added),
              'checkedFileCount': len(inventory), 'localLinksChecked': len(links),
              'localLinks': links, 'files': inventory, 'queries': queries,
              'highConfidenceSecretScan': 'NO_MATCH', 'forbiddenBinaryFileScan': 'NO_MATCH',
              'pythonSyntax': 'PASS', 'oldTrackedInputsChangedOutsideCurrentState': False,
              'initialCrlfLicenseBlocker': 'SUPERSEDED_FALSE_DIFFERENCE',
              'currentBlocker': 'AVD_CREATE_SDK_DIRECTORY_DISCOVERY_FAILURE',
              'deviceExperiments': 'NOT_RUN', 'productTestsLintBuild': 'NOT_RUN',
              'rawEvidenceExcludedFromWhitespaceNormalization': True}
    (HERE / 'evidence/delivery-verification.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    assert (HERE / 'evidence/delivery-verification.json').is_file()
    print(json.dumps({k: v for k, v in result.items() if k not in ('files', 'localLinks')}, indent=2))


if __name__ == '__main__':
    main()
