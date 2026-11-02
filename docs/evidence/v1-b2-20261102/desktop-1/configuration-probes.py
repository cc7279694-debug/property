"""Small configuration-only regression; no APK or Android execution tasks."""
from pathlib import Path
import json, subprocess, sys

root = Path.cwd()
base = root / 'docs/evidence/v1-b2-20261102/desktop-1'
receipt = ['-I', str((base / 'identity.init.gradle').relative_to(root)), ':app:b2IdentityReceipt']
cases = [
    ('default-config', ['-Pb1QaVerification=false', *receipt], 0, None),
    ('trial-config', ['-Pb1QaVerification=false', '-PlithicPrivateTrial=true', *receipt], 0, None),
    ('invalid-config', ['-Pb1QaVerification=TRUE', ':app:b2IdentityReceipt'], 1,
     'b1QaVerification must be true or false'),
    ('collision-config', ['-Pb1QaVerification=true', '-PlithicPrivateTrial=true', ':app:b2IdentityReceipt'], 1,
     'B1 QA cannot share private trial or historical verification identities'),
]
results = []
for label, args, expected, message in cases:
    process = subprocess.run([sys.executable, str(base / 'invoke-gradle.py'), label, *args])
    directory = base / label
    if (directory / 'resource-blocked.json').exists():
        raise SystemExit('Fresh memory gate blocked; no continuation of this probe batch')
    command = json.loads((directory / 'command.json').read_text(encoding='utf-8'))
    assert process.returncode == command['exitCode'] == expected
    output = (directory / 'stdout.txt').read_bytes()
    errors = (directory / 'stderr.txt').read_bytes()
    if message:
        assert message.encode('ascii') in errors
        assert b'> Task ' not in output
        value = dict(expectedConfigurationRejection=message)
    else:
        lines = [line[len(b'B2_IDENTITY='):] for line in output.splitlines() if line.startswith(b'B2_IDENTITY=')]
        assert len(lines) == 1
        value = json.loads(lines[0])
    results.append(dict(label=label, exitCode=expected, expectedOutcomeMatched=True, configuration=value))
qa_command = json.loads((base / 'build-3/command.json').read_text(encoding='utf-8'))
assert qa_command['exitCode'] == 0
qa_lines = [line[len(b'B2_IDENTITY='):] for line in (base / 'build-3/stdout.txt').read_bytes().splitlines()
            if line.startswith(b'B2_IDENTITY=')]
assert len(qa_lines) == 1
(base / 'configuration.json').write_text(json.dumps(dict(status='DESKTOP_CONFIGURATION_PASS',
    qa=json.loads(qa_lines[0]), probes=results, androidExecution='NOT_RUN', deviceOperations=0),
    indent=2)+'\n', encoding='utf-8')
print(json.dumps(dict(status='DESKTOP_CONFIGURATION_PASS', successfulEvaluations=3, expectedRejections=2)))
