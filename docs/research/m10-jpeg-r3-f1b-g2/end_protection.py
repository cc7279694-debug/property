"""G2 ending protection collection ONLY; never retries preflight or grants eligibility.

Calls existing read-only G1 collection helpers. The legacy finish function has an
unproduced before-file dependency and an overly broad text derivative; neither
is repaired here. This entry only collects after facts for offline comparison.
"""
import argparse
import hashlib
import importlib.util
from pathlib import Path
import re
import sys

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
spec = importlib.util.spec_from_file_location('g2_locked_verifier', HERE.parent / 'm10-jpeg-r3-f1b/verify.py')
V = importlib.util.module_from_spec(spec)
spec.loader.exec_module(V)


def scoped_target(d):
    result = d.adb('shell', 'dumpsys', 'activity', 'services', V.S.PROBE,
                   label='g2-after-services', private_metadata=True)
    capture = V.G.capture(result, 'api36-f1b-v1')
    # Feed the unchanged classifier the full actual stdout; never the derivative.
    classification = V.G.SERVICE.classify_capture(capture,
                        d.research_uids[V.S.PROBE], d.research_uids[V.S.OUTSIDER])
    text = capture['stdout']
    lines = text.splitlines(keepends=True)
    blocks = []
    spans = []
    for index, line in enumerate(lines):
        header = V.G.SERVICE._RECORD.fullmatch(line.rstrip('\r\n'))
        if header is None or header.group(3) != V.G.SERVICE.COMPONENT:
            continue
        end = index + 1
        while end < len(lines) and (not lines[end].strip() or lines[end].startswith('    ')):
            end += 1
        raw = ''.join(lines[index:end])
        # Withhold a whole block rather than drop fields and imply completeness.
        packages = re.findall(r'\b(?:com|org|net)\.[A-Za-z0-9_.]+', raw)
        if any(not value.startswith((V.S.PROBE, V.S.OUTSIDER)) for value in packages):
            spans.append({'firstLine': index + 1, 'lastLine': end, 'archived': False,
                          'reason': 'foreign package field; entire target block withheld'})
        else:
            blocks.append(raw)
            spans.append({'firstLine': index + 1, 'lastLine': end, 'archived': True})
    if blocks:
        (d.output / 'g2-after-target-ServiceRecord.txt').write_bytes(''.join(blocks).encode('utf-8'))
    V.save(d.output / 'g2-after-services.json', {
        'classification': classification,
        'capture': {key: value for key, value in capture.items() if key != 'stdout'},
        'fullStdoutArchived': False, 'derivativeIsFullCapture': False,
        'spans': spans, 'targetBlocks': len(spans),
        'purpose': 'AFTER_ONLY_PROTECTION_AND_SCOPED_EVIDENCE_NOT_PREFLIGHT_RETRY'})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    d = V.Bounded(args.output, 'g2-ending-protection')
    with (d.output / 'g2-ending-attempt.json').open('x', encoding='utf-8') as handle:
        import json
        json.dump({'atUtc': V.S.utc_now(), 'scriptSHA256': V.S.sha256_file(Path(__file__)),
                   'scope': 'READ_ONLY_ENDING_NO_INSTALL_OR_RUN'}, handle)
    try:
        identity = d.verify_device()
        V.require(d.text('shell', 'getprop', 'sys.boot_completed', label='g2-after-boot') == '1', 'boot incomplete')
        after = V.protection(d, 'after')
        packages = {name: d.package(name) for name in (V.S.PROBE, V.S.OUTSIDER)}
        V.save(d.output / 'g2-after-research-packages.json', packages)
        d.research_uids = {name: value['uid'] for name, value in packages.items()}
        process_capture = V.G.capture(d.adb('shell', 'ps', '-A', '-o', 'USER,PID,PPID,NAME',
                              label='g2-after-processes', private_metadata=True), 'ps-user-pid-ppid-name-v1')
        try:
            processes = {'status': 'PASS', 'rows': V.G.process_rows(process_capture, d.research_uids)}
        except ValueError as error:
            processes = {'status': 'BLOCKED', 'reason': str(error)}
        processes['capture'] = {key: value for key, value in process_capture.items() if key != 'stdout'}
        V.save(d.output / 'g2-after-processes.json', processes)
        scoped_target(d)
        inventory = d.inventory('g2-after-files')
        stage = V.validate_stages(inventory)
        V.save(d.output / 'g2-after-stage-check.json', stage)
        before_file = d.output / 'snapshot-before.json'
        comparison = {'status': 'UNKNOWN', 'privateByteIntegrity': 'NOT_RUN'}
        if before_file.exists():
            before = V.load(before_file)
            checks = {
                'formalAPKAndUIDUnchanged': before['productionPackage'] == after['productionPackage'],
                'formalPrivateMetadataUnchanged': before['productionPrivateMetadata'] == after['productionPrivateMetadata'],
                'deviceSettingsUnchanged': before['settings'] == after['settings'] and
                    V.load(d.output / 'before-extra-settings.json') == V.load(d.output / 'after-extra-settings.json')}
            comparison.update(checks, status='PASS' if all(checks.values()) else 'BLOCKED')
        V.save(d.output / 'g2-protection-comparison.json', comparison)
        V.save(d.output / 'g2-ending-result.json', {'status': 'READ_ONLY_COLLECTION_COMPLETE',
               'atUtc': V.S.utc_now(), 'identity': identity, 'protection': comparison,
               'processStatus': processes['status'], 'stage': stage,
               'installCount': 0, 'appLaunchCount': 0, 'preflightRetried': False})
        print('G2 read-only ending collection complete; no eligibility granted')
        return 0
    except Exception as error:
        V.save(d.output / 'g2-ending-error.json', {'status': 'BLOCKED', 'detail': str(error),
                                                 'atUtc': V.S.utc_now(), 'noRetry': True})
        print('G2 ending collection blocked; see scoped evidence')
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
