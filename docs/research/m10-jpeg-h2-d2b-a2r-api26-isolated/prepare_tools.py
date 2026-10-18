"""Copy existing official SDK tools to the isolated SDK; no downloads or ADB."""
from pathlib import Path
import datetime
import hashlib
import json
import shutil
import stat

HERE = Path(__file__).resolve().parent
SOURCE = Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
DEST = Path('E:/CODEX/.codex/research-env/lithic-a2r-api26/sdk')


def digest(p):
    h = hashlib.sha256()
    with p.open('rb') as f:
        for data in iter(lambda: f.read(1024 * 1024), b''):
            h.update(data)
    return h.hexdigest()


def main():
    out = HERE / 'evidence/local-tools-copy'
    out.mkdir(parents=True, exist_ok=False)
    result = {'startedUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'sourceSdkLogical': str(SOURCE), 'sourceSdkResolved': str(SOURCE.resolve()),
              'destinationSdk': str(DEST.resolve()), 'downloadedBytes': 0,
              'originalAvdDataAccessed': False, 'adbInvoked': False, 'files': []}
    for relative in ('cmdline-tools/latest', 'emulator', 'platform-tools'):
        source = SOURCE / relative
        dest = DEST / relative
        assert source.is_dir() and not dest.exists()
        files = []
        for p in [source, *source.rglob('*')]:
            attrs = p.lstat()
            assert not (getattr(attrs, 'st_file_attributes', 0) & stat.FILE_ATTRIBUTE_REPARSE_POINT), p
            assert p.is_file() or p.is_dir(), p
            if p.is_file():
                files.append((p.relative_to(source), attrs.st_size, digest(p)))
        shutil.copytree(source, dest, copy_function=shutil.copy2)
        for name, size, h in files:
            target = dest / name
            assert target.stat().st_size == size and digest(target) == h
            result['files'].append({'path': relative + '/' + str(name).replace('\\', '/'),
                                    'bytes': size, 'sha256': h})
    result.update(endedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                  status='PASS', fileCount=len(result['files']),
                  copiedBytes=sum(row['bytes'] for row in result['files']))
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: v for k, v in result.items() if k != 'files'}, indent=2))


if __name__ == '__main__':
    main()
