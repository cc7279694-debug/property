"""Read-only host artifact/source audit after the F1-A research build. No ADB."""
from pathlib import Path
import hashlib, json, os, re, subprocess, sys, xml.etree.ElementTree as ET, zipfile

base=Path(__file__).resolve().parents[1]; repo=base.parents[2]
out=Path(sys.argv[1]).resolve()
assert out.is_relative_to((repo/'docs/research/m10-jpeg-r3-f1a/evidence').resolve())
build=json.loads((out/'result.json').read_text(encoding='utf-8'))
inputs=json.loads((out/'inputs.json').read_text(encoding='utf-8'))
assert all(hashlib.sha256((repo/n).read_bytes()).hexdigest()==v['sha256'] for n,v in inputs.items())
tests=[]; lint={}; modules={}
for module in ('probe','outsider'):
    for f in (out/'reports'/module).rglob('TEST-*.xml'):
        tree=ET.parse(f)
        for c in tree.getroot().iter('testcase'):
            tests.append({'identity':c.attrib['classname']+'.'+c.attrib['name'],'pass':not any(c.find(x) is not None for x in ('failure','error','skipped'))})
    for f in (out/'reports'/module).rglob('lint-results-debug.xml'):
        issues=list(ET.parse(f).getroot().iter('issue'))
        lint[module]={'errors':sum(x.attrib.get('severity') in ('Error','Fatal') for x in issues),'warnings':sum(x.attrib.get('severity')=='Warning' for x in issues)}
sdk=Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
env=dict(os.environ,JAVA_HOME='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1')
commands=[]
for module in ('probe','outsider'):
    apk=Path(build[module+'Apk']['path']); prefix=out/(module+'-apk')
    for label,args in (
        ('manifest',[str(sdk/'cmdline-tools/latest/bin/apkanalyzer.bat'),'manifest','print',str(apk)]),
        ('badging',[str(sdk/'build-tools/36.0.0/aapt2.exe'),'dump','badging',str(apk)]),
        ('signing',[str(sdk/'build-tools/36.0.0/apksigner.bat'),'verify','--print-certs',str(apk)]),
        ('dex-packages',[str(sdk/'cmdline-tools/latest/bin/apkanalyzer.bat'),'dex','packages','--defined-only',str(apk)])):
        result=subprocess.run(args,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90)
        Path(str(prefix)+'-'+label+'.txt').write_bytes(result.stdout)
        commands.append({'module':module,'audit':label,'exitCode':result.returncode})
        assert result.returncode==0,(label,result.stdout)
    manifest=ET.fromstring(Path(str(prefix)+'-manifest.txt').read_text(encoding='utf-8'))
    ns='{http://schemas.android.com/apk/res/android}'
    permissions=[x.attrib[ns+'name'] for x in manifest.findall('uses-permission')]
    services=[dict(x.attrib) for x in manifest.findall('application/service')]
    assert manifest.attrib['package']=='com.cooper.lithic.m10r3.'+module
    sdk_node=manifest.find('uses-sdk'); assert sdk_node.attrib[ns+'minSdkVersion']=='26' and sdk_node.attrib[ns+'targetSdkVersion']=='36'
    assert not permissions
    if module=='probe':
        assert len(services)==1 and services[0][ns+'name'].endswith('.WorkerService') and services[0][ns+'exported']=='false' and services[0][ns+'process']==':jpeg_worker'
    else: assert not services
    with zipfile.ZipFile(apk) as z:
        assert not any(n.startswith('lib/') for n in z.namelist())
    packages=Path(str(prefix)+'-dex-packages.txt').read_text(encoding='utf-8')
    assert 'org.junit' not in packages and 'org.hamcrest' not in packages
    modules[module]={'applicationId':manifest.attrib['package'],'minSdk':26,'targetSdk':36,'permissions':permissions,'services':services,'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'nativeLibraries':0,'junitHamcrestPackaged':False}
old_test='docs/research/m10-jpeg-r3/host/probe/src/test/java/com/cooper/lithic/m10r3/probe/SessionGateTest.java'
locked='f513725b862d3422b371883536557c4233bed2e6'
prior=subprocess.check_output(['git','show',locked+':'+old_test],cwd=repo)
assert prior==(repo/old_test).read_bytes()
old_names=re.findall(r'@Test\s+public void (\w+)\(',prior.decode())
actual={x['identity'] for x in tests}
declared=set()
for module in ('probe','outsider'):
    for f in (base/'host'/module/'src/test').rglob('*.java'):
        source=f.read_text(encoding='utf-8')
        package=re.search(r'package\s+([\w.]+);',source).group(1)
        declared.update(package+'.'+f.stem+'.'+name for name in re.findall(r'@Test\s+public void (\w+)\(',source))
assert declared==actual,{'missingResults':sorted(declared-actual),'extraResults':sorted(actual-declared)}
assert len(old_names)==17
assert all('com.cooper.lithic.m10r3.probe.SessionGateTest.'+n in actual for n in old_names)
assert len(actual)==len(tests) and all(x['pass'] for x in tests)
result={'buildExit':build['exitCode'],'projectionInputsMatchCurrentSources':True,'inputCount':len(inputs),'old17SourceByteIdentical':True,'old17MethodIdentitiesRetained':True,'sourceTestIdentitiesExactlyMatchXml':True,'uniqueJUnitMethods':len(tests),'pass':sum(x['pass'] for x in tests),'fail':sum(not x['pass'] for x in tests),'androidTestsRun':False,'lint':lint,'apks':modules,'commands':commands,'tests':tests}
(out/'artifact-audit.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k not in ('tests','commands')},indent=2))
