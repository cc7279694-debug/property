import hashlib, json, pathlib, sys
ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
files = sorted({p for folder in ['app/src','app/schemas','gradle'] for p in (ROOT/folder).rglob('*') if p.is_file()} | {ROOT/x for x in ['app/build.gradle.kts','build.gradle.kts','settings.gradle.kts','gradle.properties']}, key=lambda p:p.relative_to(ROOT).as_posix())
h = hashlib.sha256()
for p in files:
    h.update(p.relative_to(ROOT).as_posix().encode('utf-8') + b'\0' + hashlib.sha256(p.read_bytes()).digest())
result = dict(file_count=len(files), sha256=h.hexdigest(), method='sorted relative UTF-8 path + NUL + file SHA256 bytes; app/src, app/schemas, gradle and four build configuration files')
output=pathlib.Path(sys.argv[1])
if output.exists():
    assert json.loads(output.read_text(encoding='utf-8'))==result, 'Build input drift'
else:
    with output.open('x',encoding='utf-8') as f: json.dump(result,f,indent=2)
print(json.dumps(result))
