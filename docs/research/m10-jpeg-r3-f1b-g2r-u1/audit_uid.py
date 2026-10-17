"""HOST_ONLY audit of the privacy-filtered U1 observation, never a ps fallback.

Live collection correlated USER/PID/PPID/NAME in memory. Those private rows
are intentionally unavailable for independent replay. This checks only the
retained label/numeric-UID agreement, completeness metadata and research IDs.
"""
import hashlib
import json
from pathlib import Path
import re
import sys

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / 'm10-jpeg-r3-f1b-g1'))
import gate


def validate(observation):
    gate.need(observation.get('status') == 'OBSERVED_NOT_PREFLIGHT' and
              observation.get('scope') == 'UID_DIAGNOSIS_ONLY_NOT_PREFLIGHT', 'missing observation status/scope')
    gate.need(observation.get('fullProcessStdoutArchived') is False and
              observation.get('unrelatedProcessNamesArchived') is False, 'privacy scope unknown')
    gate.need(observation.get('numericUIDColumnSupported') is True, 'numeric column support not established')
    identities = observation.get('installedIdentities')
    expected = {gate.PRODUCTION: 10216, gate.PROBE: 10218, gate.OUTSIDER: 10219}
    gate.need(isinstance(identities, dict) and set(identities) == set(expected), 'installed identity missing')
    for name, uid in expected.items():
        item = identities[name]
        gate.need(type(item.get('uid')) is int and item['uid'] == uid and
                  item.get('apkSHA256') in gate.KNOWN[name], 'installed identity conflicts')
    commands = observation.get('commands')
    gate.need(isinstance(commands, list), 'command provenance missing')
    process_commands = [item for item in commands if item.get('command', [])[3:6] == ['shell', 'ps', '-A']]
    gate.need(len(process_commands) == 2, 'exact two process capture records required')
    for item, columns in zip(process_commands, ('USER,PID,PPID,NAME', 'USER,UID,PID,PPID,NAME')):
        gate.need(item['command'] == ['C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe',
                  '-s', 'emulator-5596', 'shell', 'ps', '-A', '-o', columns], 'process command identity missing')
        gate.need(type(item.get('exitCode')) is int and item['exitCode'] == 0 and
                  item.get('complete') is True and item.get('stderrBytes') == 0,
                  'failed/incomplete process capture')
        gate.need(type(item.get('stdoutBytes')) is int and 0 < item['stdoutBytes'] <= 1024 * 1024 and
                  isinstance(item.get('stdoutSHA256'), str) and
                  re.fullmatch('[0-9a-f]{64}', item['stdoutSHA256']), 'process integrity metadata missing')
    labels = observation.get('exceptionLabels')
    gate.need(isinstance(labels, list) and len(labels) == 1, 'exact observed exception label required')
    item = labels[0]
    gate.need(item.get('label') == 'uwb' and item.get('format') == 'ASCII' and
              type(item.get('count')) is int and item['count'] == 1 and
              item.get('correlatedRows') == item['count'], 'exception row correlation missing/conflicting')
    numbers = item.get('numericUIDs')
    gate.need(isinstance(numbers, list) and len(numbers) == 1 and type(numbers[0]) is int,
              'numeric UID missing/invalid')
    resolved = gate.resolve_ps_uid(item['label'])
    gate.need(resolved is not None and resolved == numbers[0], 'numeric UID mismatch')
    gate.need(resolved not in expected.values(), 'exception overlaps application UID')
    gate.need(observation.get('researchRows') == [], 'this observation has unexpected research rows')
    return {'status': 'PASS', 'scope': 'HOST_AUDIT_OF_RETAINED_UID_EVIDENCE_ONLY',
            'label': item['label'], 'numericUID': resolved,
            'fullRowsReplayable': False, 'devicePreflightRun': False}


if __name__ == '__main__':
    source = HERE / 'evidence/uid-20261017-04/uid-diagnosis.json'
    result = validate(json.loads(source.read_text(encoding='utf-8')))
    result.update(sourceSHA256=hashlib.sha256(source.read_bytes()).hexdigest())
    target = HERE / 'evidence/uid-observation-audit.json'
    with target.open('x', encoding='utf-8') as handle:
        json.dump(result, handle, indent=2)
        handle.write('\n')
    print(json.dumps(result))
