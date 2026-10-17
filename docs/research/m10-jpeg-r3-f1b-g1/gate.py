"""Pure research preflight composition. No ADB, cleanup, lease or launch authority.

Only a trusted complete read-only collector may supply these envelopes. A hash
detects accidental truncation; it is not authentication of an untrusted caller.
"""
import datetime
import hashlib
import importlib.util
from pathlib import Path
import re

HERE = Path(__file__).resolve().parent


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


SERVICE = module('g1_services', HERE / 'service_record.py')
STAGE = module('g1_stage', HERE.parent / 'm10-jpeg-r3-f1b/stage_check.py')
PROBE, OUTSIDER = SERVICE.PROBE, SERVICE.OUTSIDER
PRODUCTION = 'com.cooper.lithic.debug'
# Exact package name is checked against the existing supervisor below; no new APK.
FORMAL_SHA = '4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46'
KNOWN = {
    PROBE: {'230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0',
            'a3eea12dfe05736a69c473fa812d0b52a43c8100bbed2cffd0ba9c8f5f0ea684'},
    OUTSIDER: {'214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358',
               'a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879'},
    PRODUCTION: {FORMAL_SHA},
}


def need(ok, reason):
    if not ok:
        raise ValueError(reason)


def capture(result, profile):
    """Called only after subprocess.communicate completed, never on TimeoutExpired."""
    raw = result.stdout
    need(isinstance(raw, bytes) and isinstance(result.stderr, bytes), 'nonbyte command output')
    return {'stdout': raw.decode('utf-8', errors='strict'),
            'exitCode': result.returncode, 'stderr': result.stderr.decode('utf-8', errors='strict'),
            'complete': True, 'truncated': False, 'stdoutBytes': len(raw),
            'stdoutSHA256': hashlib.sha256(raw).hexdigest(), 'profile': profile}


def complete_text(value, profile):
    need(isinstance(value, dict), 'missing complete capture')
    need(type(value.get('exitCode')) is int and value['exitCode'] == 0,
         'command failed or exit status absent')
    need(value.get('complete') is True and value.get('truncated') is False,
         'command output incomplete/truncated')
    need(value.get('stderr') == '' and type(value.get('stderr')) is str,
         'command diagnostics or stderr missing')
    text = value.get('stdout')
    need(type(text) is str and text.endswith('\n') and '\ufffd' not in text,
         'invalid complete command text')
    raw = text.encode('utf-8', errors='strict')
    need(type(value.get('stdoutBytes')) is int and value['stdoutBytes'] == len(raw)
         and 0 < len(raw) <= 1024 * 1024, 'capture bytes mismatch/limit')
    need(value.get('stdoutSHA256') == hashlib.sha256(raw).hexdigest(), 'capture hash mismatch')
    need(value.get('profile') == profile, 'unsupported capture profile')
    return text


def process_rows(value, uids):
    """Validate every ps row, then retain only research identity rows in results.

    Android app UID labels and numeric UIDs are decoded. Named platform users
    have no app UID; unknown app-like labels fail closed, never silently skip.
    """
    lines = complete_text(value, 'ps-user-pid-ppid-name-v1').splitlines()
    need(lines and lines[0].split() == ['USER', 'PID', 'PPID', 'NAME'], 'unknown ps header')
    need(len(lines) > 1, 'ps has no process rows; completeness not established')
    rows, seen = [], set()
    for line in lines[1:]:
        fields = line.split()
        need(len(fields) == 4, 'malformed ps row')
        user, pid, ppid, name = fields
        need(pid.isascii() and pid.isdecimal() and int(pid) > 0 and
             ppid.isascii() and ppid.isdecimal(), 'invalid ps PID/PPID')
        need(int(pid) not in seen, 'duplicate ps PID')
        seen.add(int(pid))
        match = re.fullmatch(r'u(\d+)_a(\d+)', user)
        if user.isascii() and user.isdecimal():
            uid = int(user)
        elif match:
            uid = int(match[1]) * 100000 + 10000 + int(match[2])
        else:
            # Kernel/platform identities are names; an unknown app/user label
            # cannot be treated as a platform identity or silently discarded.
            need((re.fullmatch(r'[a-z][a-z0-9_-]*', user) is not None
                  and not user.startswith('u')) or user == 'usb', 'unknown ps UID label')
            uid = None
        related = name in (PROBE, OUTSIDER, SERVICE.PROCESS) or name.startswith((PROBE + ':', OUTSIDER + ':'))
        if related or uid in uids.values():
            need(uid is not None, 'research process UID not resolved')
            need(name != SERVICE.PROCESS, 'actual Worker process present')
            need(name in (PROBE, OUTSIDER), 'unknown process under research identity')
            need(uid == uids[name], 'research process UID conflicts with installed identity')
            rows.append({'uid': uid, 'pid': int(pid), 'ppid': int(ppid), 'name': name})
    return sorted(rows, key=lambda row: (row['uid'], row['pid']))


def stage_signature(snapshot):
    need(isinstance(snapshot, dict), 'stage snapshot missing')
    proof = snapshot.get('collection')
    need(isinstance(proof, dict) and proof.get('complete') is True
         and proof.get('stable') is True and proof.get('truncated') is False
         and type(proof.get('exitCode')) is int and proof['exitCode'] == 0
         and proof.get('stderr') == '', 'stage collection incomplete/unstable')
    entries = snapshot.get('entries')
    need(isinstance(entries, list), 'stage entries missing')
    paths = [entry.get('path') for entry in entries if isinstance(entry, dict)]
    need(len(paths) == len(entries) and len(set(paths)) == len(paths), 'invalid/duplicate inventory path')
    for entry in entries:
        path = entry['path']
        need(isinstance(path, str) and (path == 'files' or path.startswith('files/'))
             and not any(part in ('', '.', '..') for part in path.split('/')),
             'noncanonical inventory path')
        if path in ('files', STAGE.ROOT):
            need(entry.get('type') == 'directory', 'unsafe namespace ancestor')
    stage = STAGE.validate_snapshot(snapshot)
    need(stage['status'] == 'PASS', 'unknown/nonterminal stage: ' + str(stage['reasons']))
    relevant = [entry for entry in entries if entry['path'] == STAGE.ROOT or
                entry['path'].startswith(STAGE.ROOT + '/')]
    # Metadata and object identities, not mtime-based permission to clean.
    relevant = [{k: e.get(k) for k in ('path', 'type', 'device', 'inode', 'size', 'mode')}
                for e in relevant]
    signature = {'entries': sorted(relevant, key=lambda e: e['path']),
                 'json': snapshot['json'], 'sha256': snapshot['sha256']}
    return stage, signature


def evaluate_preflight(samples):
    """Require two independently collected consistent samples; no action granted."""
    answer = {'classification': 'BLOCKED', 'reasons': [], 'samples': [],
              'scope': 'RESEARCH_SNAPSHOT_ONLY_NOT_LEASE_DEATH_OR_EXECUTION_AUTHORITY'}
    try:
        need(isinstance(samples, list) and len(samples) == 2, 'two complete snapshots required')
        signatures = []
        end = None
        ids = set()
        for value in samples:
            need(isinstance(value, dict), 'snapshot missing')
            sample_id = value.get('sampleId')
            need(isinstance(sample_id, str) and sample_id and sample_id not in ids, 'snapshot ID missing/reused')
            ids.add(sample_id)
            start = datetime.datetime.fromisoformat(value['startedAtUtc'])
            finish = datetime.datetime.fromisoformat(value['finishedAtUtc'])
            need(start.tzinfo is not None and finish.tzinfo is not None and start <= finish,
                 'snapshot time missing/inverted')
            need(end is None or end < start, 'snapshots overlap or chronology unknown')
            end = finish
            environment = value.get('environment')
            need(environment == {'serial': 'emulator-5596', 'avd': 'Lithic_M01_API_36',
                                 'api': 36, 'bootComplete': '1'}, 'unverified research environment')
            packages = value.get('packages')
            need(isinstance(packages, dict) and set(packages) == set(KNOWN), 'all three installed identities required')
            uids = {}
            package_signature = {}
            for name, data in packages.items():
                need(isinstance(data, dict) and data.get('package') == name
                     and data.get('present') is True, 'package name/presence mismatch')
                uid = data.get('uid')
                need(type(uid) is int and 10000 <= uid < 100000, 'invalid user-0 application UID')
                apks = data.get('apks')
                need(isinstance(apks, list) and len(apks) == 1 and apks[0].get('sha256') in KNOWN[name],
                     'installed APK bytes not recognized: ' + name)
                code_path = apks[0].get('codePath')
                need(isinstance(code_path, str) and re.fullmatch(
                    r'/data/app/~~[A-Za-z0-9_+=-]+/' + re.escape(name) +
                    r'-[A-Za-z0-9_+=-]+/base\.apk', code_path) is not None,
                    'unrecognized installed APK path')
                uids[name] = uid
                package_signature[name] = {'uid': uid, 'sha256': apks[0]['sha256'], 'codePath': code_path}
            need(len(set(uids.values())) == 3, 'formal/Probe/Outsider UID collision')
            rows = process_rows(value.get('processes'), {name: uids[name] for name in (PROBE, OUTSIDER)})
            service = SERVICE.classify_capture(value.get('services'), uids[PROBE], uids[OUTSIDER])
            need(service['classification'] in ('NO_TARGET_RECORD', 'INACTIVE_RECORD_CANDIDATE'),
                 'service blocked: ' + str(service['reasons']))
            if service['record'] is not None:
                need(service['record']['baseDir'] == package_signature[PROBE]['codePath'],
                     'service APK path conflicts with installed Probe identity')
            stage, stage_state = stage_signature(value.get('inventory'))
            signatures.append({'environment': environment, 'packages': package_signature,
                               'processes': rows, 'service': service, 'stage': stage_state})
            answer['samples'].append({'sampleId': sample_id, 'service': service,
                                      'processes': rows, 'stageCheck': stage})
        need(signatures[0] == signatures[1], 'two snapshot states disagree: stop')
        answer['classification'] = 'PREFLIGHT_ELIGIBLE'
    except (ValueError, TypeError, KeyError, AttributeError, UnicodeError, OverflowError) as error:
        answer['reasons'].append(str(error))
    return answer
