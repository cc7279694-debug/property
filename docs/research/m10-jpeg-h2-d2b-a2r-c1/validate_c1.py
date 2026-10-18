"""Validate staged C1 documents/evidence only. Never run SDK/device commands."""
from pathlib import Path
import base64
import datetime
import hashlib
import html
import json
import re
import subprocess
from urllib.parse import unquote

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
SOURCE = '4ee27bfcbe5903d6a49d4a92203de8a63686ba66'
PREFIX = 'docs/research/m10-jpeg-h2-d2b-a2r-c1/'
MODIFIED = {
    'docs/CURRENT_STATE.md',
    'docs/research/M10_JPEG_H2_D2B_A2R_EXECUTION.md',
    'docs/research/M10_JPEG_H2_D2B_A2R_RESULTS.md',
    'docs/research/m10-jpeg-h2-d2b-a2r-api26-isolated/README.md',
    'docs/research/m10-jpeg-h2-d2b-a2r-api26-isolated/USER_SCOPE.md',
    'docs/research/m10-jpeg-r1/.gitignore',
}
ADDED = {
    'docs/research/m10-jpeg-h2-d2b-a2r-api26-isolated/.gitignore',
    'docs/research/M10_JPEG_H2_D2B_A2R_C1_REMEDIATION.md',
    'docs/research/M10_JPEG_H2_D2B_A2R_C1_SDK_STATIC_DIAGNOSIS.md',
    'docs/checkpoints/2026-10-18-M10-JPEG-H2-D2B-A2R-C1.md',
}


def git(*args):
    return subprocess.run(['git', *args], cwd=REPO, check=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout


def all_blobs(entries):
    process = subprocess.Popen(['git', 'cat-file', '--batch'], cwd=REPO,
                               stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    cache = {}
    try:
        for path, oid in entries.items():
            if oid not in cache:
                process.stdin.write((oid + '\n').encode('ascii'))
                process.stdin.flush()
                header = process.stdout.readline().decode().strip().split()
                if len(header) != 3 or header[1] != 'blob':
                    raise RuntimeError('Unexpected object response: ' + str(header))
                size = int(header[2])
                raw = process.stdout.read(size)
                if len(raw) != size or process.stdout.read(1) != b'\n':
                    raise RuntimeError('Incomplete blob read')
                cache[oid] = raw
            yield path, cache[oid]
    finally:
        process.stdin.close()
        if process.wait(timeout=30) != 0:
            raise RuntimeError('git cat-file failed')


def body_marker(raw):
    title = 'This is the Android ' + 'Software Development Kit License Agreement'
    text = html.unescape(raw.decode('utf-8', errors='replace'))
    if raw[:1] in (b'{', b'['):
        try:
            text += json.dumps(json.loads(text), ensure_ascii=False)
        except (ValueError, TypeError):
            pass
    encoded = base64.b64encode(title.encode()).decode()[:48]
    return title.lower() in text.lower() or encoded in text


def links(path, raw, tracked):
    checked, missing = 0, []
    for target in re.findall(r'(?<!!)\[[^\]]*\]\(([^)\n]+)\)', raw.decode('utf-8', errors='replace')):
        if target.startswith(('http:', 'https:', 'chatgpt:', 'sandbox:', '#', 'mailto:', 'codex:')):
            continue
        target = unquote(target.strip('<>').split('#', 1)[0])
        if not target:
            continue
        resolved = ((REPO / path).parent / target).resolve()
        try:
            relative = resolved.relative_to(REPO).as_posix()
        except ValueError:
            missing.append(target)
            continue
        checked += 1
        if relative not in tracked and not any(p.startswith(relative.rstrip('/') + '/') for p in tracked):
            missing.append(target)
    return checked, missing


def main():
    output = HERE / 'evidence/validation.json'
    if output.exists():
        raise RuntimeError('Evidence output already exists; do not overwrite')
    inventory = json.loads((HERE / 'evidence/license-inventory.json').read_text())
    removed = {r['path'] for r in inventory['files']}
    source = {}
    for row in git('ls-tree', '-r', '-z', SOURCE).split(b'\0'):
        if row:
            info, name = row.split(b'\t', 1)
            source[name.decode()] = info.split()[2].decode()
    staged = {}
    for row in git('ls-files', '-s', '-z').split(b'\0'):
        if row:
            info, name = row.split(b'\t', 1)
            mode, oid, stage = info.split()
            if stage != b'0':
                raise RuntimeError('Unmerged index')
            staged[name.decode()] = oid.decode()
    changed = {p for p in set(source) | set(staged) if source.get(p) != staged.get(p)}
    unexpected = sorted(p for p in changed if p not in removed | MODIFIED | ADDED and not p.startswith(PREFIX))
    if unexpected:
        raise RuntimeError('Out-of-scope index changes: ' + repr(unexpected))
    originals = []
    for row in inventory['files']:
        raw = (REPO / row['path']).read_bytes()
        ignore = git('check-ignore', '-v', '--', row['path']).decode().strip()
        originals.append({'path': row['path'], 'notTracked': row['path'] not in staged,
                          'localOriginalUnchanged': len(raw) == row['bytes'] and hashlib.sha256(raw).hexdigest() == row['sha256'],
                          'historicalBlobUnchanged': source[row['path']] == row['gitBlob'],
                          'ignoreRule': ignore})
    source_body, staged_body, indexed_bytes = [], [], 0
    for path, raw in all_blobs(source):
        if body_marker(raw):
            source_body.append(path)
    missing_links, link_count = [], 0
    secret_candidates = []
    secret_patterns = [re.compile(b'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE' + b' KEY-----'),
                       re.compile(b'github_' + b'pat_[A-Za-z0-9_]{30,}'),
                       re.compile(b'gh[pousr]_[A-Za-z0-9]{30,}'),
                       re.compile(b'AKIA[0-9A-Z]{16}')]
    for path, raw in all_blobs(staged):
        indexed_bytes += len(raw)
        if body_marker(raw):
            staged_body.append(path)
        if path in changed:
            if any(pattern.search(raw) for pattern in secret_patterns):
                secret_candidates.append(path)
            if path.endswith('.md'):
                count, missing = links(path, raw, set(staged) | {PREFIX + 'evidence/validation.json'})
                link_count += count
                if missing:
                    missing_links.append({'path': path, 'targets': missing})
    smoke = 'docs/research/m10-jpeg-h2-d2b-a2r-api26-isolated/smoke_isolated.py'
    after = json.loads((HERE / 'evidence/readonly-after/result.json').read_text())
    whitespace = subprocess.run(['git', 'diff', '--cached', '--check'], cwd=REPO,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    checks = {
        'branchMatches': git('branch', '--show-current').decode().strip() == 'research/m10-jpeg-h2-d2b-a2r-api26-isolated',
        'sourceStillAncestor': subprocess.run(['git', 'merge-base', '--is-ancestor', SOURCE, 'HEAD'], cwd=REPO).returncode == 0,
        'sourceScanMatchesSevenOriginals': set(source_body) == removed,
        'currentIndexLicenseBodyMatchesZero': not staged_body,
        'sevenOriginalsUntrackedIgnoredAndPreserved': all(r['notTracked'] and r['localOriginalUnchanged'] and r['historicalBlobUnchanged'] and r['ignoreRule'] for r in originals),
        'onlyAuthorizedChanges': not unexpected,
        'originalLaunchScriptUnchanged': staged[smoke] == source[smoke],
        'noMissingTrackedDocumentLinks': not missing_links,
        'noSecretPatternCandidates': not secret_candidates,
        'noWhitespaceErrors': whitespace.returncode == 0,
        'readonlyEnvironmentSdkMarkerAndScriptUnchanged': all(after['comparison'].values()),
    }
    result = {'timeUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'sourceCommit': SOURCE, 'branch': git('branch', '--show-current').decode().strip(),
              'status': 'PASS_STATIC_DELIVERY' if all(checks.values()) else 'FAIL_REQUIRES_REVIEW',
              'checks': checks, 'sourceTrackedFilesScanned': len(source),
              'currentIndexFilesScanned': len(staged), 'currentIndexBytesScanned': indexed_bytes,
              'selfOutputExcludedFromContentScan': True,
              'sourceBodyMatches': source_body, 'currentIndexBodyMatches': staged_body,
              'originals': originals, 'changedPaths': sorted(changed),
              'documentLinksChecked': link_count, 'missingLinks': missing_links,
              'secretPatternCandidates': secret_candidates,
              'whitespaceExitCode': whitespace.returncode, 'whitespaceOutput': whitespace.stdout.decode(errors='replace'),
              'scanLimit': 'Raw UTF8/HTML-entity text, JSON decoding and known base64-title prefix; not arbitrary encrypted payload detection.',
              'androidSdkEntrypointsExecuted': 0, 'adbOrAppOrEmulatorOperations': 0,
              'androidTestsBuildsAndRuntimeFixVerification': 'NOT_RUN'}
    with output.open('x', encoding='utf-8', newline='\n') as stream:
        stream.write(json.dumps(result, indent=2) + '\n')
    print(json.dumps({k: result[k] for k in ('status', 'checks', 'sourceTrackedFilesScanned', 'currentIndexFilesScanned', 'documentLinksChecked', 'missingLinks')}, indent=2))
    if not all(checks.values()):
        raise SystemExit(1)


if __name__ == '__main__':
    main()
