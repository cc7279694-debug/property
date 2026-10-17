"""U1 one-shot read-only UID diagnosis. Never archives complete ps stdout.

Only fixed identity queries, one legacy ps, and one numeric-column ps are
allowed. No preflight, service/stage access, app lifecycle or cleanup entry.
"""
import datetime
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import sys

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
ADB = 'C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe'
SERIAL = 'emulator-5596'
PACKAGES = ('com.cooper.lithic.debug', 'com.cooper.lithic.m10r3.probe',
            'com.cooper.lithic.m10r3.outsider')
EXPECTED = ('4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46',
            '230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0',
            '214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358')


def utc():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def main():
    # A fresh directory is also a no-retry marker; never overwrite evidence.
    output = HERE / 'evidence' / 'uid-20261017-04'
    output.mkdir(parents=True, exist_ok=False)
    records = []
    report = {'scope': 'UID_DIAGNOSIS_ONLY_NOT_PREFLIGHT', 'startedAtUtc': utc(),
              'scriptSHA256': digest(Path(__file__).read_bytes()),
              'serial': SERIAL, 'fullProcessStdoutArchived': False,
              'unrelatedProcessNamesArchived': False, 'appLifecycleCalls': 0,
              'legacyPsLimit': 1, 'numericPsLimit': 1}

    def save():
        report['commands'] = records
        (output / 'uid-diagnosis.json').write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

    def call(*args):
        record = {'command': [ADB, '-s', SERIAL, *args], 'startedAtUtc': utc()}
        records.append(record)
        try:
            result = subprocess.run(record['command'], capture_output=True, timeout=20)
        except subprocess.TimeoutExpired:
            record.update(status='TIMEOUT', complete=False, finishedAtUtc=utc())
            save()
            raise ValueError('bounded identity/ps command timeout') from None
        record.update(exitCode=result.returncode, stdoutBytes=len(result.stdout),
                      stdoutSHA256=digest(result.stdout), stderrBytes=len(result.stderr),
                      stderrSHA256=digest(result.stderr), complete=True,
                      finishedAtUtc=utc())
        save()
        if result.returncode != 0 or result.stderr or not result.stdout or len(result.stdout) > 1024 * 1024:
            raise ValueError('command failed/diagnostics/empty/oversize; raw output withheld')
        text = result.stdout.decode('utf-8', errors='strict')
        if not text.endswith('\n') or '\ufffd' in text:
            raise ValueError('command text incomplete; raw output withheld')
        return text

    try:
        state = call('get-state').strip()
        avd = [line.strip() for line in call('emu', 'avd', 'name').splitlines() if line.strip()]
        api = call('shell', 'getprop', 'ro.build.version.sdk').strip()
        boot = call('shell', 'getprop', 'sys.boot_completed').strip()
        if (state, avd, api, boot) != ('device', ['Lithic_M01_API_36', 'OK'], '36', '1'):
            raise ValueError('device identity/boot mismatch')
        report['device'] = {'state': state, 'avd': avd[0], 'api': int(api), 'bootComplete': boot}
        identities = {}
        for name, expected in zip(PACKAGES, EXPECTED):
            uid = call('shell', 'run-as', name, 'id', '-u').strip()
            if not uid.isascii() or not uid.isdecimal():
                raise ValueError('package UID malformed')
            path_text = call('shell', 'pm', 'path', name).strip().splitlines()
            if len(path_text) != 1 or not re.fullmatch(
                    r'package:/data/app/~~[A-Za-z0-9_+=-]+/' + re.escape(name) +
                    r'-[A-Za-z0-9_+=-]+/base\.apk', path_text[0]):
                raise ValueError('package APK path not recognized')
            path = path_text[0].removeprefix('package:')
            actual = call('shell', 'sha256sum', path).split()
            if actual != [expected, path]:
                raise ValueError('installed APK byte identity mismatch')
            identities[name] = {'uid': int(uid), 'apkSHA256': expected}
        if len({item['uid'] for item in identities.values()}) != 3:
            raise ValueError('installed UID collision')
        report['installedIdentities'] = identities
        # Deliberately keep both complete process outputs only in this memory.
        legacy = call('shell', 'ps', '-A', '-o', 'USER,PID,PPID,NAME')
        legacy_lines = legacy.splitlines()
        if legacy_lines[0].split() != ['USER', 'PID', 'PPID', 'NAME']:
            raise ValueError('legacy header mismatch')
        parsed = []
        anomalies = {}
        seen = set()
        for line in legacy_lines[1:]:
            row = line.split()
            if len(row) != 4:
                raise ValueError('legacy row incomplete; private row withheld')
            label, pid, ppid, name = row
            if not pid.isascii() or not pid.isdecimal() or not ppid.isascii() or not ppid.isdecimal() or int(pid) <= 0 or pid in seen:
                raise ValueError('legacy row identity invalid')
            seen.add(pid)
            parsed.append(row)
            supported = ((label.isascii() and label.isdecimal()) or
                         re.fullmatch(r'u(\d+)_a(\d+)', label) or
                         ((re.fullmatch(r'[a-z][a-z0-9_-]*', label) and not label.startswith('u')) or label == 'usb'))
            if not supported:
                anomalies.setdefault(label, {'label': label, 'count': 0,
                    'format': 'ASCII' if label.isascii() else 'NON_ASCII'})['count'] += 1
        report['legacyRowCount'] = len(parsed)
        report['exceptionLabels'] = list(anomalies.values())
        report['legacyGateError'] = 'unknown ps UID label' if anomalies else None
        save()

        numeric = call('shell', 'ps', '-A', '-o', 'USER,UID,PID,PPID,NAME')
        numeric_lines = numeric.splitlines()
        if numeric_lines[0].split() != ['USER', 'UID', 'PID', 'PPID', 'NAME']:
            raise ValueError('numeric UID column not supported as requested')
        index = {}
        relevant = []
        for line in numeric_lines[1:]:
            fields = line.split()
            if len(fields) != 5:
                raise ValueError('numeric row incomplete; private row withheld')
            label, uid, pid, ppid, name = fields
            if not all(s.isascii() and s.isdecimal() for s in (uid, pid, ppid)) or int(pid) <= 0 or pid in index:
                raise ValueError('numeric UID/PID malformed or duplicate')
            index[pid] = fields
            if name in PACKAGES or name.startswith((PACKAGES[1] + ':', PACKAGES[2] + ':')) or int(uid) in (identities[PACKAGES[1]]['uid'], identities[PACKAGES[2]]['uid']):
                # Unknown same-UID names must not be archived; block instead.
                if name not in PACKAGES or int(uid) != identities[name]['uid']:
                    raise ValueError('research identity conflict/unknown same-UID process')
                relevant.append({'package': name, 'uid': int(uid), 'pid': int(pid), 'ppid': int(ppid), 'userLabel': label})
        for label, item in anomalies.items():
            matches = []
            for row in parsed:
                if row[0] != label:
                    continue
                other = index.get(row[1])
                if other is None or [other[0], *other[2:]] != row:
                    raise ValueError('exception label row not consistently correlated')
                matches.append(int(other[1]))
            item.update(numericUIDs=sorted(set(matches)), correlatedRows=len(matches))
        report.update(numericUIDColumnSupported=True, numericRowCount=len(index),
                      exceptionLabels=list(anomalies.values()), researchRows=relevant,
                      status='OBSERVED_NOT_PREFLIGHT', finishedAtUtc=utc())
        save()
        print(json.dumps({key: report[key] for key in ('status', 'device', 'exceptionLabels', 'numericUIDColumnSupported', 'researchRows')}, ensure_ascii=False))
        return 0
    except (ValueError, UnicodeError, OSError) as error:
        report.update(status='BLOCKED', reason=str(error), finishedAtUtc=utc())
        save()
        print('U1 diagnostic BLOCKED; only privacy-filtered evidence saved')
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
