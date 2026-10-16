"""Read-only Git/source audit. Run only after the root explicitly freezes final input.

This is not a Kotlin compiler, test runner, APK/device/data audit or acceptance.
Only --output is written. No raw source, log, account or device data is emitted.
"""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

BASE = 'f000639c62fce71be0a9240425c8e27b1a5ed974'
BRANCH = 'feat/ui-c1-visual-redesign'
PKG = 'app/src/main/java/com/cooper/lithic/'
UI = 'app/src/androidTest/java/com/cooper/lithic/ui/UIComponentsTest.kt'
NAV = PKG + 'app/LithicNavigation.kt'
OLD_FORM = 'large_text_synthetic_form_keeps_errors_save_and_return_reachable_after_input'
BOUNDARY = 'synthetic_form_save_target_stays_above_system_navigation_insets'
DIAGNOSTIC_SHA = 'b65dbae6079e84648eb0dff296d2676e3d573fe33ca40731ed3969079b1d5c7f'
SCHEMA_ATTRIBUTE_STANZA = ('\n# Preserve frozen Room schema export bytes across Windows checkouts.\n'
                           'app/schemas/** -text\n')
SCHEMAS = tuple('app/schemas/com.cooper.lithic.data.local.LithicDatabase/' + str(n) + '.json'
                for n in (1, 2))
V1_FIXTURE = 'app/src/androidTest/java/com/cooper/lithic/data/local/V1DatabaseFixture.kt'
V1_EXPORT_SHA256 = '351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b'
DISPLAY = {PKG + p for p in (
    'app/LithicNavigation.kt', 'core/design/FormFields.kt', 'core/design/FormPresentation.kt',
    'feature/review/ReviewShell.kt', 'feature/settings/SettingsShell.kt',
    'feature/project/ProjectHistoryScreen.kt', 'feature/project/ProjectEditorScreen.kt',
    'feature/project/MoneyEventEditor.kt', 'feature/project/MoneyEventCorrectionScreen.kt',
    'feature/project/EndProjectDialog.kt', 'feature/project/DeleteProjectDialog.kt',
    'feature/project/ProjectInputs.kt', 'feature/project/CoverPicker.kt',
    'feature/media/CoverDisplay.kt', 'feature/subscription/SubscriptionListScreen.kt',
    'feature/subscription/SubscriptionHistoryScreen.kt', 'feature/subscription/SubscriptionDetailScreen.kt',
    'feature/subscription/SubscriptionEditorScreen.kt', 'feature/subscription/RenewalEditorScreen.kt',
    'feature/subscription/SubscriptionStateDialog.kt',
)}
DOCS = {'AGENTS.md', 'docs/CURRENT_STATE.md', 'docs/DECISIONS.md',
        'docs/design/UI_C1_SPEC.md', 'docs/planning/UI_C1_PLAN.md', '.gitattributes'}
TEST_ROOTS = ('app/src/test/', 'app/src/androidTest/')
# Strings remain full tokens. Whitespace/comments outside strings are omitted.
LEX = re.compile(r'\s+|//[^\n]*|/\*[\s\S]*?\*/|"""[\s\S]*?"""|'
                 r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])\'|[A-Za-z0-9_]+|[^\s]', re.M)


def sha(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def text(raw: bytes) -> str:
    return raw.decode('utf-8-sig').replace('\r\n', '\n')


def tokens(source: str) -> list[str]:
    return [m.group() for m in LEX.finditer(source)
            if not m.group().isspace() and not m.group().startswith(('//', '/*'))]


def digest(items: list[str]) -> str:
    return sha(json.dumps(items, ensure_ascii=False, separators=(',', ':')).encode())


def need(value: bool, label: str) -> None:
    if not value:
        raise ValueError(label)


def matching(t: list[str], start: int, left: str, right: str) -> int:
    need(t[start] == left, 'invalid delimiter start')
    depth = 0
    for i in range(start, len(t)):
        depth += t[i] == left
        depth -= t[i] == right
        if depth == 0:
            return i
    raise ValueError('unclosed delimiter')


def sequence_at(t: list[str], part: list[str]) -> list[int]:
    return [i for i in range(len(t) - len(part) + 1) if t[i:i + len(part)] == part]


def strip_sequence(t: list[str], part: list[str], expected: int | None = None) -> list[str]:
    starts = sequence_at(t, part)
    if expected is not None:
        need(len(starts) == expected, 'unexpected allowed-insertion count: ' + ''.join(part[:4]))
    for i in reversed(starts):
        del t[i:i + len(part)]
    return t


def function(t: list[str], name: str) -> tuple[int, int, int]:
    starts = sequence_at(t, ['fun', name, '('])
    need(len(starts) == 1, 'missing/duplicate supported function: ' + name)
    start = starts[0]
    end_args = matching(t, start + 2, '(', ')')
    brace = next((i for i in range(end_args + 1, len(t)) if t[i] == '{'), -1)
    need(brace >= 0 and 'fun' not in t[end_args + 1:brace], 'unsupported function form: ' + name)
    end = matching(t, brace, '{', '}') + 1
    return start, brace, end


def remove_function(t: list[str], name: str, test: bool) -> list[str]:
    start, _, end = function(t, name)
    if test:
        need(t[start - 2:start] == ['@', 'Test'], 'new test annotation changed: ' + name)
        start -= 2
    else:
        need(t[start - 1] == 'private', 'diagnostic helper visibility changed')
        start -= 1
    return t[:start] + t[end:]


def tests(path: str, raw: bytes) -> list[dict]:
    source, t = text(raw), tokens(text(raw))
    if ['@', 'Test'] not in [t[i:i + 2] for i in range(len(t) - 1)]:
        return []
    pkg = re.search(r'^package\s+([\w.]+)', source, re.M)
    # Repository convention: one unindented test class per file. Fail on ambiguity.
    classes = re.findall(r'^(?:internal\s+)?class\s+(\w+)', source, re.M)
    need(pkg is not None and len(classes) == 1, 'unsupported test owner: ' + path)
    out = []
    for i in sequence_at(t, ['@', 'Test']):
        cursor = i + 2
        if t[cursor] == '(':
            cursor = matching(t, cursor, '(', ')') + 1
        while t[cursor] == '@':
            cursor += 2
            if t[cursor] == '(':
                cursor = matching(t, cursor, '(', ')') + 1
        need(t[cursor] == 'fun', 'unsupported @Test declaration: ' + path)
        name = t[cursor + 1]
        need(t[cursor + 2] == '(', 'unsupported @Test signature: ' + path)
        end = matching(t, cursor + 2, '(', ')') + 1
        # Identity does not depend on whether Kotlin uses a block, = runBlocking,
        # or a brace-free expression body (the original M02 overflow tests do).
        # Whole-file equality below protects every original body/helper, and
        # the exceptional UI file is reconstructed to its entire old token stream.
        # This hash intentionally names only the annotation/signature, not a body.
        out.append({'kind': 'jvm' if '/test/' in path else 'android',
                    'class': pkg.group(1) + '.' + classes[0], 'name': name,
                    'path': path, 'annotationAndSignatureTokensSha256': digest(t[i:end])})
    return out


def identity(case: dict) -> tuple[str, str, str]:
    return case['kind'], case['class'], case['name']


def callback_blocks(t: list[str]) -> list[list[str]]:
    out = []
    for i in range(len(t) - 2):
        if t[i] in ('onClick', 'onDismissRequest') and t[i + 1:i + 3] == ['=', '{']:
            out.append(t[i:matching(t, i + 2, '{', '}') + 1])
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--root', type=Path, default=Path.cwd())
    ap.add_argument('--freeze-head', required=True, help='Full final HEAD recorded by root at explicit freeze')
    ap.add_argument('--output', type=Path, required=True, help='New audit JSON; raw logs are not read')
    args = ap.parse_args()
    root = args.root.resolve()

    def git(*parts: str) -> bytes:
        return subprocess.check_output(['git', '-c', 'core.quotepath=false', *parts],
                                       cwd=root, timeout=120, stderr=subprocess.PIPE)

    def git_paths(*parts: str) -> list[str]:
        return [x.decode('utf8') for x in git(*parts, '-z').split(b'\0') if x]

    report: dict = {'auditType': 'STATIC_ONLY', 'baseline': BASE, 'result': 'FAIL',
                    'limitations': ['No Kotlin compilation or business/runtime equivalence claim',
                                    'No test execution, APK/device/ledger verification or acceptance',
                                    'Allowed display-file behavior still needs human diff review'],
                    'rawLogsRead': False, 'failures': []}
    try:
        head = git('rev-parse', 'HEAD').decode().strip()
        need(head == args.freeze_head, 'HEAD differs from explicit frozen HEAD')
        need(git('branch', '--show-current').decode().strip() == BRANCH, 'wrong branch')
        git('merge-base', '--is-ancestor', BASE, head)
        baseline_paths = git_paths('ls-tree', '-r', '--name-only', BASE)
        baseline_set = set(baseline_paths)
        current_paths = set(git_paths('ls-files')) | set(git_paths('ls-files', '--others', '--exclude-standard'))
        deleted = [p for p in baseline_paths if not (root / p).is_file()]
        need(not deleted, 'baseline file deleted: ' + ', '.join(deleted[:8]))
        changed = set(git_paths('diff', '--name-only', '--no-renames', BASE))
        added = current_paths - baseline_set
        all_changes = sorted(changed | added)
        # Compare names only for historic evidence; never open raw old logs/data.
        forbidden = []
        for p in all_changes:
            if p in DISPLAY or p in DOCS or p == UI:
                continue
            if p.startswith(TEST_ROOTS) and p.endswith('.kt') and p not in baseline_set:
                continue
            if p.startswith('docs/evidence/ui-c1/stage2/') or p.startswith('docs/design/ui-c1/source-stage2/'):
                continue
            if p not in baseline_set and p.startswith('docs/checkpoints/') and p.endswith('.md'):
                continue
            if p not in baseline_set and re.fullmatch(r'docs/evidence/UI-C1-(?:task[678]|stage2)[\w-]*\.md', p):
                continue
            forbidden.append(p)
        need(not forbidden, 'outside allowlist / immutable history: ' + ', '.join(forbidden[:12]))
        # Exact authorized append only; no generic exception for attribute edits.
        task6_attributes = git('show', 'e9bb44f:.gitattributes').decode('utf8').replace('\r\n', '\n')
        actual_attributes = (root / '.gitattributes').read_bytes().decode('utf8').replace('\r\n', '\n')
        need(actual_attributes == task6_attributes + SCHEMA_ATTRIBUTE_STANZA,
             'gitattributes differs from Task6 plus exact authorized frozen-schema stanza')
        attribute_tokens = [x.decode('utf8') for x in
                            git('check-attr', '-z', 'text', '--', *SCHEMAS).split(b'\0') if x]
        need(len(attribute_tokens) == len(SCHEMAS) * 3, 'unexpected schema attribute result')
        schema_attributes = {attribute_tokens[i]: attribute_tokens[i + 2]
                             for i in range(0, len(attribute_tokens), 3)}
        need(all(schema_attributes.get(p) == 'unset' for p in SCHEMAS),
             'effective schema text attribute is not unset (-text)')
        frozen_fixture = text(git('show', BASE + ':' + V1_FIXTURE))
        actual_fixture = text((root / V1_FIXTURE).read_bytes())
        fixture_pattern = r'const\s+val\s+EXPORT_SHA256\s*=\s*"([0-9a-f]{64})"'
        frozen_constants = re.findall(fixture_pattern, frozen_fixture)
        actual_constants = re.findall(fixture_pattern, actual_fixture)
        need(frozen_constants == actual_constants == [V1_EXPORT_SHA256],
             'frozen V1 fixture export SHA constant changed')
        raw_schema_checks = []
        report['schemaRawByteChecks'] = raw_schema_checks
        for version, p in enumerate(SCHEMAS, 1):
            baseline_blob = git('show', BASE + ':' + p)
            head_blob = git('show', head + ':' + p)
            actual_raw = (root / p).read_bytes()
            row = {'path': p, 'schemaVersion': version,
                   'baselineBlobSha256': sha(baseline_blob), 'headBlobSha256': sha(head_blob),
                   'worktreeRawSha256': sha(actual_raw), 'bytes': len(actual_raw),
                   'worktreeEqualsHeadBlob': actual_raw == head_blob,
                   'headEqualsFrozenBaselineBlob': head_blob == baseline_blob,
                   'effectiveTextAttribute': schema_attributes[p]}
            raw_schema_checks.append(row)
            need(head_blob == baseline_blob, 'schema Git blob changed from frozen baseline: ' + p)
            need(actual_raw == head_blob, 'schema raw bytes differ from frozen HEAD blob: ' + p)
            need(json.loads(head_blob)['database']['version'] == version, 'unexpected schema export version: ' + p)
            if version == 1:
                row['frozenFixtureExpectedSha256'] = V1_EXPORT_SHA256
                need(sha(actual_raw) == sha(head_blob) == V1_EXPORT_SHA256,
                     'raw v1 export does not match frozen V1 fixture SHA')
        report['schemaAttributesPolicy'] = {
            'anchor': 'e9bb44f', 'exactAuthorizedAppend': SCHEMA_ATTRIBUTE_STANZA,
            'normalizedAttributesSha256': sha(actual_attributes.encode('utf8')),
            'frozenFixtureConstantUnchanged': True,
            'rawBytesCheckedWithoutLineEndingNormalization': True}
        baseline_test_paths = [p for p in baseline_paths if p.startswith(TEST_ROOTS) and p.endswith('.kt')]
        base_sources = {p: git('show', BASE + ':' + p) for p in baseline_test_paths}
        current_test_paths = sorted(p for p in current_paths if p.startswith(TEST_ROOTS) and p.endswith('.kt'))
        actual_disk = {p.relative_to(root).as_posix() for folder in TEST_ROOTS for p in (root / folder).rglob('*.kt')}
        need(actual_disk == set(current_test_paths), 'ignored/unregistered test source on disk')
        current_sources = {p: (root / p).read_bytes() for p in current_test_paths}
        snapshot = {p: sha((root / p).read_bytes()) for p in current_paths if
                    p.startswith('app/src/') or p.startswith('app/schemas/') or
                    p in ('app/build.gradle.kts', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', '.gitattributes') or
                    p.startswith('gradle/') or p in ('gradlew', 'gradlew.bat')}
        baseline_cases = [case for p, raw in base_sources.items() for case in tests(p, raw)]
        current_cases = [case for p, raw in current_sources.items() for case in tests(p, raw)]
        bmap, cmap = {identity(x): x for x in baseline_cases}, {identity(x): x for x in current_cases}
        need(len(baseline_cases) == len(bmap) == 701, 'baseline identity parser did not reproduce original 701')
        need(len(current_cases) == len(cmap), 'duplicate current method identity')
        need(not set(bmap) - set(cmap), 'missing original method identities')
        manifest_path = root / 'build/ui-c1-stage2/baseline-methods.json'
        if manifest_path.exists():
            manifest = json.loads(manifest_path.read_text(encoding='utf8'))
            need({identity(x) for x in manifest} == set(bmap), 'preflight manifest differs from Git-derived baseline')
        unchanged_old_files = []
        for p, before in base_sources.items():
            if p != UI:
                need(text(before) == text(current_sources[p]), 'old test/helper source changed: ' + p)
                unchanged_old_files.append(p)
        # Reconstruct the entire old UIComponents file, not merely assertion counts.
        ui_source = text(current_sources[UI])
        helper_match = re.search(r'    private fun recordSaveDiagnostic\([\s\S]*?(?=    private fun assertTextFits\()', ui_source)
        need(helper_match is not None and sha(helper_match.group().strip().encode()) == DIAGNOSTIC_SHA,
             'diagnostic helper differs from reviewed read-only diagnostic')
        original_ui, reconstructed = tokens(text(base_sources[UI])), tokens(ui_source)
        reconstructed = remove_function(reconstructed, BOUNDARY, True)
        reconstructed = remove_function(reconstructed, 'recordSaveDiagnostic', False)
        for phase in ('before-click', 'after-click'):
            reconstructed = strip_sequence(reconstructed, tokens('recordSaveDiagnostic("' + phase + '", saves.get())'), 1)
        start, _, end = function(reconstructed, 'LargeTextHost')
        host = strip_sequence(reconstructed[start:end], tokens('.safeDrawingPadding()'), 1)
        reconstructed[start:end] = host
        reconstructed = strip_sequence(reconstructed, tokens('import androidx.compose.foundation.layout.safeDrawingPadding'), 1)
        need(reconstructed == original_ui, 'original UIComponents tokens/assertions/helpers changed beyond reviewed additions')
        # No introduced skip annotations/assumptions: inherited assumptions are byte-protected.
        skip_re = re.compile(r'@(?:Ignore|Disabled)\b|\b(?:assumeTrue|assumeFalse|Assumptions|Assume)\b')
        for p, raw in current_sources.items():
            prior = collections.Counter(skip_re.findall(text(base_sources.get(p, b''))))
            now = collections.Counter(skip_re.findall(text(raw)))
            need(not now - prior, 'new skip/assumption marker: ' + p)
        # Freeze navigation logic while permitting only reviewed display wrappers.
        bnav, cnav = tokens(text(git('show', BASE + ':' + NAV))), tokens(text((root / NAV).read_bytes()))
        bstart, _, _ = function(bnav, 'LithicNavigation')
        cstart, _, _ = function(cnav, 'LithicNavigation')
        marker = tokens('pendingLeave?.let')
        bi, ci = sequence_at(bnav, marker)[-1], sequence_at(cnav, marker)[-1]
        injection = tokens(', mediaRepository = state.mediaRepository')
        bp, cp = bnav[bstart:bi], cnav[cstart:ci]
        need(len(sequence_at(cp, injection)) - len(sequence_at(bp, injection)) == 3, 'unexpected media wiring delta')
        need(strip_sequence(cp[:], injection) == strip_sequence(bp[:], injection), 'stable navigation prefix/guards changed')
        bhelper, _, _ = function(bnav, 'SubscriptionOperationContent')
        chelper, _, _ = function(cnav, 'SubscriptionOperationContent')
        need(bnav[bhelper:] == cnav[chelper:], 'subscription operation/recovery helpers changed')
        bt, ct = bnav[bi:bhelper], cnav[ci:chelper]
        need(callback_blocks(bt) == callback_blocks(ct), 'dialog dismiss/click guard bodies changed')
        recovery = tokens('val origin = projectOrigin(routes.last())')
        rb, rc = sequence_at(bt, recovery)[0], sequence_at(ct, recovery)[0]
        eb, ec = bt.index('Column', rb), ct.index('Column', rc)
        need(bt[rb:eb] == ct[rc:ec], 'pending recovery identity/target selection changed')
        bstrings = collections.Counter(x for x in bt if x.startswith('"'))
        cstrings = collections.Counter(x for x in ct if x.startswith('"'))
        need(not bstrings - cstrings, 'original leave/recovery warning or tag removed')
        need(head == git('rev-parse', 'HEAD').decode().strip(), 'HEAD changed during audit')
        need(all((root / p).is_file() and sha((root / p).read_bytes()) == h for p, h in snapshot.items()),
             'source/config input changed during audit')
        report.update(result='PASS', frozenHead=head, branch=BRANCH, changedPaths=all_changes,
                      baselineMethods=len(baseline_cases), currentMethods=len(current_cases),
                      baselineByKind=dict(collections.Counter(x['kind'] for x in baseline_cases)),
                      currentByKind=dict(collections.Counter(x['kind'] for x in current_cases)),
                      originalMethodsPreserved=701, originalMethodManifest=baseline_cases,
                      newMethods=[cmap[k] for k in sorted(set(cmap) - set(bmap))],
                      originalTestFilesOtherThanUiUnchanged=unchanged_old_files,
                      comparison='Git-visible exact content; old test text comparison normalizes CRLF only; UI token reconstruction is exact',
                      uiOriginalTokensSha256=digest(original_ui), uiReconstructedTokensSha256=digest(reconstructed),
                      diagnosticHelperSha256=DIAGNOSTIC_SHA, navigationGuards='EXACT_TOKEN_SEQUENCE_PASS',
                      inputFingerprint=snapshot)
    except (ValueError, OSError, subprocess.SubprocessError, UnicodeError, KeyError, IndexError) as exc:
        # Do not print subprocess stderr/raw source; report only bounded audit reason.
        report['failures'].append(str(exc)[:1000])
    output = args.output if args.output.is_absolute() else root / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(json.dumps({k: report[k] for k in ('auditType', 'result', 'failures')}))
    return 0 if report['result'] == 'PASS' else 1


if __name__ == '__main__':
    sys.exit(main())
