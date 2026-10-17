"""Repeatable host-only RED/GREEN for research pure Java decision code. No ADB."""
from pathlib import Path
import argparse, datetime, hashlib, json, subprocess

p = argparse.ArgumentParser()
p.add_argument('phase')
p.add_argument('classes', nargs='+')
a = p.parse_args()
base = Path(__file__).resolve().parents[1]
repo = base.parents[2]
out = repo/'docs/research/m10-jpeg-r3-f1a/evidence'/a.phase
out.mkdir(parents=True, exist_ok=False)
jdk = Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1/bin')
cache = Path('C:/Users/CDD/.gradle/caches/modules-2/files-2.1')
junit = next((cache/'junit/junit/4.13.2').rglob('*.jar'))
hamcrest = next((cache/'org.hamcrest/hamcrest-core/1.3').rglob('*.jar'))
sources = []
for module in ('probe','outsider'):
    root = base/'host'/module/'src'
    for f in root.rglob('*.java'):
        if f.stem in a.classes or (f.parent.name in ('probe','outsider') and '/main/' in f.as_posix() and not any(x in f.read_text(encoding='utf-8') for x in ('import android.', 'import org.json.'))):
            sources.append(f)
work = repo/'build/f1a-host-jvm'/a.phase
work.mkdir(parents=True, exist_ok=False)
cp = str(junit)+';'+str(hamcrest)
argv = [str(jdk/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(work)] + [str(f) for f in sources]
compiled = subprocess.run(argv, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
(out/'compile.log').write_bytes(compiled.stdout)
result = {'phase':a.phase,'hostOnly':True,'androidRun':False,'compileExit':compiled.returncode,'sourceInputs':{f.relative_to(repo).as_posix():{'bytes':f.stat().st_size,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for f in sources}}
if compiled.returncode == 0:
    qualified=[]
    for name in a.classes:
        f=next(x for x in sources if x.stem==name)
        package=next(line.strip()[8:].rstrip(';') for line in f.read_text(encoding='utf-8').splitlines() if line.startswith('package '))
        qualified.append(package+'.'+name)
    run = subprocess.run([str(jdk/'java.exe'),'-cp',str(work)+';'+cp,'org.junit.runner.JUnitCore']+qualified,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (out/'junit.log').write_bytes(run.stdout)
    result['testExit']=run.returncode
    print(run.stdout.decode('utf-8',errors='replace'))
else: print(compiled.stdout.decode('utf-8',errors='replace'))
(out/'result.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
raise SystemExit(result.get('testExit',compiled.returncode))
