"""Pinned local compiler / actual Kotlin Stage host tests; no Android or network."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
import re
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[3]
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('existing_stage_tools',ROOT/'docs/research/m10-final-safety-closure/run_stage_subset.py')
tools=importlib.util.module_from_spec(spec);spec.loader.exec_module(tools)

def main():
    phase=sys.argv[1]
    if phase not in ('red','green'):raise RuntimeError('Fixed evidence phase required')
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S.%fZ')
    out=ROOT/'build/m10-b1b2-stage'/f'{phase}-{stamp}';out.mkdir(parents=True,exist_ok=False)
    classes=out/'classes';classes.mkdir()
    specs=[('org.jetbrains.kotlin','kotlin-compiler-embeddable','2.3.10'),('org.jetbrains.kotlin','kotlin-stdlib','2.3.10'),('org.jetbrains.kotlin','kotlin-script-runtime','2.3.10'),('org.jetbrains.kotlin','kotlin-reflect','1.6.10'),('org.jetbrains.kotlin','kotlin-daemon-embeddable','2.3.10'),('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.8.0'),('org.jetbrains','annotations','13.0'),('junit','junit','4.13.2'),('org.hamcrest','hamcrest-core','1.3')]
    deps={a:tools.jar(g,a,v) for g,a,v in specs}
    sources=[ROOT/'app/src/main/java/com/cooper/lithic/data/media/jpegstage'/f for f in ('JpegStageV2.kt','StageRecords.kt')]+[ROOT/'app/src/test/java/com/cooper/lithic/data/media/jpegstage/JpegStageV2Test.kt',HERE/'StageRecoveryRunner.java']
    methods=re.findall(r'@Test fun (recovery_[A-Za-z0-9_]+)\(',sources[2].read_text(encoding='utf-8'))
    if not methods or len(set(methods))!=len(methods):raise RuntimeError('Selected methods absent or repeated')
    if len(sys.argv)>2:
        requested=sys.argv[2:]
        if len(set(requested))!=len(requested) or any(name not in methods for name in requested):raise RuntimeError('Unknown focused recovery identity')
        methods=requested
    sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();before={p.relative_to(ROOT).as_posix():sha(p) for p in sources}
    cp=';'.join(str(deps[k]) for k in ('kotlin-stdlib','junit','hamcrest-core','annotations'))
    commands=[[str(tools.JDK/'java.exe'),'-cp',';'.join(map(str,deps.values())),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','17','-classpath',cp,'-d',str(classes),*map(str,sources[:3])],[str(tools.JDK/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(classes),str(sources[3])],[str(tools.JDK/'java.exe'),'-cp',str(classes)+';'+cp,'StageRecoveryRunner',*methods]]
    results=[]
    for index,cmd in enumerate(commands):
        started=datetime.now(timezone.utc).isoformat();r=subprocess.run(cmd,cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=1500)
        (out/f'{index:02}-stdout.raw').write_bytes(r.stdout);(out/f'{index:02}-stderr.raw').write_bytes(r.stderr)
        results.append({'argv':cmd,'exit':r.returncode,'startedUTC':started,'finishedUTC':datetime.now(timezone.utc).isoformat(),'stdoutSHA256':sha(out/f'{index:02}-stdout.raw'),'stderrSHA256':sha(out/f'{index:02}-stderr.raw')})
        print(f'phase={phase} command={index} exit={r.returncode}',flush=True)
        if r.returncode and index<2:break
    record={'scope':'HOST_ONLY_REAL_KOTLIN_STAGE_FILES_NOT_ANDROID','phase':phase,'selectedMethods':methods,'selectedUniqueCount':len(set(methods)),'sourceSHA256':before,'sourcesUnchanged':before=={p.relative_to(ROOT).as_posix():sha(p) for p in sources},'commands':results,'outputDirectory':str(out)}
    (out/'RESULTS.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
    print('OUTPUT='+str(out),flush=True)
    if phase=='green' and (len(results)!=3 or any(c['exit'] for c in results) or not record['sourcesUnchanged']):raise SystemExit(1)

if __name__=='__main__':main()
