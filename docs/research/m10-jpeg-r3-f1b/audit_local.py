"""Host-only audit of the locked F1-A inputs. No ADB, install, build or device access."""
from pathlib import Path
import hashlib, json, os, subprocess, sys, xml.etree.ElementTree as ET

HERE=Path(__file__).resolve().parent;REPO=HERE.parents[2]
out=Path(sys.argv[1]).resolve()
assert out.is_relative_to((HERE/'evidence').resolve())
target=out/'local-apk-audit';target.mkdir(exist_ok=False)
source=Path('E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1/砺石新APP/docs/research/m10-jpeg-r3/host')
sdk=Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
env=dict(os.environ,JAVA_HOME='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1')
expected={'probe':'a3eea12dfe05736a69c473fa812d0b52a43c8100bbed2cffd0ba9c8f5f0ea684','outsider':'a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879'}
records=[];commands=[];ns='{http://schemas.android.com/apk/res/android}'
for module in expected:
    apk=source/module/'build/outputs/apk/debug'/f'{module}-debug.apk'
    assert apk.is_file() and hashlib.sha256(apk.read_bytes()).hexdigest()==expected[module]
    for label,tool in [('manifest',[str(sdk/'cmdline-tools/latest/bin/apkanalyzer.bat'),'manifest','print']),('signature',[str(sdk/'build-tools/36.0.0/apksigner.bat'),'verify','--print-certs'])]:
        args=tool+[str(apk)];p=subprocess.run(args,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=40)
        (target/(module+'-'+label+'.txt')).write_bytes(p.stdout)
        commands.append({'argv':args,'exitCode':p.returncode});assert p.returncode==0
    manifest=ET.fromstring((target/(module+'-manifest.txt')).read_bytes())
    assert manifest.attrib['package']=='com.cooper.lithic.m10r3.'+module
    assert not manifest.findall('uses-permission')
    sdk_element=manifest.find('uses-sdk');assert sdk_element.attrib[ns+'minSdkVersion']=='26' and sdk_element.attrib[ns+'targetSdkVersion']=='36'
    if module=='probe':
        services=manifest.findall('application/service');assert len(services)==1 and services[0].attrib[ns+'exported']=='false' and services[0].attrib[ns+'process']==':jpeg_worker'
    records.append({'module':module,'path':str(apk),'bytes':apk.stat().st_size,'sha256':expected[module],'package':manifest.attrib['package'],'minSdk':26,'targetSdk':36,'permissions':[]})
inputs=json.loads((REPO/'docs/research/m10-jpeg-r3-f1a/evidence/build-runs/20261017T024420.362749Z/inputs.json').read_text())
assert all(hashlib.sha256((REPO/n).read_bytes()).hexdigest()==v['sha256'] for n,v in inputs.items())
result={'status':'PASS_HOST_ONLY','sourceInputsUnchangedFromLockedF1A':len(inputs),'apks':records,'commands':commands,'apkInstalled':False,'installedSigningContinuity':'NOT_RUN_SAFETY_PREFLIGHT_BLOCKED','androidVerification':'NOT_RUN'}
(target/'result.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result,indent=2))
