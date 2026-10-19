"""Recompile current Stage Kotlin and run 15 existing safety tests on host files.

Uses installed JDK and cached, pinned dependencies; no Gradle, network, APK or
device operation. NTFS identity/fsync callbacks remain host test semantics.
"""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import subprocess

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
CACHE = Path('C:/Users/CDD/.gradle/caches/modules-2/files-2.1')
JDK = Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1/bin')
METHODS = (
    'persistent_revoke_survives_new_protocol_instance',
    'revoked_state_is_never_authorized',
    'cancelled_late_success_cannot_publish',
    'nonterminal_cleanup_is_blocked',
    'unknown_child_is_preserved_and_blocks_cleanup',
    'malformed_receipt_never_unlocks_pin_or_admission',
    'terminal_guard_taken_again_blocks_new_session',
    'unknown_outcome_cannot_end_resources',
    'success_needs_both_fd_closures',
    'complete_requires_released_real_file_lock',
    'source_copy_unclassified_failure_releases_own_fds_but_preserves_unknown_stage',
    'source_copy_publication_sync_unknown_preserves_stage_without_claiming_or_cleaning',
    'source_copy_blocking_reader_does_not_block_namespace_inspection_or_allow_new_session',
    'source_copy_reentrant_gc_and_restart_cannot_claim_unpublished_attempt',
    'source_copy_cancellation_retains_original_object_and_legacy_v1_bytes',
)


def jar(group, artifact, version):
    matches = list((CACHE / group / artifact / version).rglob('*.jar'))
    if len(matches) != 1:
        raise RuntimeError('Pinned dependency absent or ambiguous: ' + artifact)
    return matches[0]


def main():
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S.%fZ')
    out = ROOT / 'build/m10-final-safety-closure' / ('stage-' + stamp)
    out.mkdir(parents=True, exist_ok=False)
    classes = out / 'classes'
    classes.mkdir()
    specs = [('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.3.10'),
             ('org.jetbrains.kotlin', 'kotlin-stdlib', '2.3.10'),
             ('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.3.10'),
             ('org.jetbrains.kotlin', 'kotlin-reflect', '1.6.10'),
             ('org.jetbrains.kotlin', 'kotlin-daemon-embeddable', '2.3.10'),
             ('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.8.0'),
             ('org.jetbrains', 'annotations', '13.0'),
             ('junit', 'junit', '4.13.2'), ('org.hamcrest', 'hamcrest-core', '1.3')]
    deps = {a: jar(g, a, v) for g, a, v in specs}
    sources = [ROOT / 'app/src/main/java/com/cooper/lithic/data/media/jpegstage' / name
               for name in ('JpegStageV2.kt', 'StageRecords.kt')]
    sources += [ROOT / 'app/src/test/java/com/cooper/lithic/data/media/jpegstage/JpegStageV2Test.kt']
    sources += [HERE / 'StageSafetyRunner.java']
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    before = {p.relative_to(ROOT).as_posix(): sha(p) for p in sources}
    classpath = ';'.join(str(deps[k]) for k in ('kotlin-stdlib', 'junit', 'hamcrest-core', 'annotations'))
    commands = [
        [str(JDK / 'java.exe'), '-cp', ';'.join(str(p) for p in deps.values()),
         'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect',
         '-jvm-target', '17', '-classpath', classpath, '-d', str(classes), *map(str, sources[:3])],
        [str(JDK / 'javac.exe'), '-encoding', 'UTF-8', '-cp', classpath,
         '-d', str(classes), str(sources[3])],
        [str(JDK / 'java.exe'), '-cp', str(classes) + ';' + classpath, 'StageSafetyRunner', *METHODS],
    ]
    results = []
    for index, command in enumerate(commands):
        started = datetime.now(timezone.utc).isoformat()
        result = subprocess.run(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                timeout=240)
        stdout = out / f'{index:02d}-stdout.raw'
        stderr = out / f'{index:02d}-stderr.raw'
        stdout.write_bytes(result.stdout); stderr.write_bytes(result.stderr)
        results.append({'argv': command, 'exit': result.returncode, 'startedUTC': started,
                        'finishedUTC': datetime.now(timezone.utc).isoformat(),
                        'stdoutSHA256': sha(stdout), 'stderrSHA256': sha(stderr)})
        print(result.stdout.decode('utf-8', errors='replace'), end='', flush=True)
        print(result.stderr.decode('utf-8', errors='replace'), end='', flush=True)
        if result.returncode:
            break
    unchanged = before == {p.relative_to(ROOT).as_posix(): sha(p) for p in sources}
    success = len(results) == 3 and all(x['exit'] == 0 for x in results) and unchanged
    record = {'scope': 'CURRENT_KOTLIN_STAGE_HOST_ONLY_NO_ANDROID',
              'status': 'PASS' if success else 'FAIL', 'existingMethodsSelected': list(METHODS),
              'testCount': len(METHODS) if success else 'SEE_RAW_LOG', 'sourceSHA256': before,
              'sourcesUnchanged': unchanged, 'dependencySHA256': {a: sha(p) for a, p in deps.items()},
              'commands': results, 'outputDirectory': str(out),
              'limits': 'Real NTFS files; injected directory sync callback, no Android fsync, Binder or crash proof.'}
    (out / 'RESULTS.json').write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8')
    print('OUTPUT=' + str(out), flush=True)
    if not success:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
