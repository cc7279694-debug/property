from pathlib import Path
import subprocess,os,json,hashlib,zipfile,struct,re,xml.etree.ElementTree as ET,shutil
root=Path.cwd();ev=root/'docs/evidence/c6-2d-preflight-20261029-172534';out=ev/'artifact';out.mkdir()
sdk=Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk/build-tools/36.0.0')
env=dict(os.environ);env['JAVA_HOME']='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1';env['PATH']=env['JAVA_HOME']+'/bin;'+env['PATH']
cert='29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9';sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
cmds=[]
def capture(label,args):
 p=subprocess.run(args,env=env,capture_output=True,timeout=60)
 (out/(label+'.stdout')).write_bytes(p.stdout);(out/(label+'.stderr')).write_bytes(p.stderr)
 cmds.append({'argv':args,'exitCode':p.returncode,'stdoutBytes':len(p.stdout),'stdoutSha256':hashlib.sha256(p.stdout).hexdigest(),'stderrBytes':len(p.stderr),'stderrSha256':hashlib.sha256(p.stderr).hexdigest()});(out/'commands.json').write_text(json.dumps(cmds,indent=2)+'\n',encoding='utf-8');assert p.returncode==0,label
 return p.stdout.decode('utf-8',errors='replace')
def dex_metadata(data):
 u32=lambda p:struct.unpack_from("<I",data,p)[0]
 def uleb(pos):
  value=shift=0
  while True:
   v=data[pos];pos+=1;value|=(v&127)<<shift
   if not v&128:return value,pos
   shift+=7
 strings=[]
 for i in range(u32(0x38)):
  pos=u32(u32(0x3c)+i*4);_,pos=uleb(pos);strings.append(data[pos:data.index(b"\0",pos)].decode("utf-8",errors="replace"))
 types=[strings[u32(u32(0x44)+i*4)] for i in range(u32(0x40))]
 classes=set();roles=set()
 for i in range(u32(0x60)):
  base=u32(0x64)+i*32;name=types[u32(base)];classes.add(name)
  if name!="Lcom/cooper/lithic/core/design/LithicVisuals;":continue
  pos=u32(base+24);counts=[]
  for _ in range(4):value,pos=uleb(pos);counts.append(value)
  for count in counts[:2]:
   index=0
   for _ in range(count):
    diff,pos=uleb(pos);index+=diff;_,pos=uleb(pos)
    roles.add(strings[u32(u32(0x54)+index*8+4)])
 return classes,roles,set(strings)

def apk_metadata(path):
 with zipfile.ZipFile(path) as z:
  classes=set();roles=set();strings=set()
  for n in z.namelist():
   if re.fullmatch(r"classes\d*\.dex",n):
    a,b,c=dex_metadata(z.read(n));classes|=a;roles|=b;strings|=c
  libs={n:hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if n.startswith("lib/") and n.endswith(".so")}
 return classes,roles,strings,libs
assert json.loads((ev/'build-check/command.json').read_text(encoding='utf-8-sig'))['exitCode']==0
app=root/'app/build-c6-isolation/outputs/apk/debug/app-debug.apk';test=root/'app/build-c6-isolation/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
results={}
for label,apk,pkg in [('app',app,'com.cooper.lithic.c6isolation.debug'),('test',test,'com.cooper.lithic.c6isolation.debug.test')]:
 b=capture(label+'-badging',[str(sdk/'aapt2.exe'),'dump','badging',str(apk)])
 m=capture(label+'-manifest',[str(sdk/'aapt2.exe'),'dump','xmltree',str(apk),'--file','AndroidManifest.xml'])
 c=capture(label+'-certificate',[str(sdk/'apksigner.bat'),'verify','--verbose','--print-certs',str(apk)])
 assert "name='"+pkg+"'" in b and cert in c.lower()
 assert 'android.permission.INTERNET' not in m and ':sharedUserId(' not in m
 classes,_,strings,libs=apk_metadata(apk)
 results[label]={'path':str(apk),'package':pkg,'bytes':apk.stat().st_size,'sha256':sha(apk),'certificateSha256':cert,'permissions':sorted(re.findall(r"uses-permission: name='([^']+)'",b)),'classCount':len(classes),'nativeEntries':libs}
 if label=='app':
  assert "versionCode='17'" in b and "versionName='0.1.0-c6isolation-debug'" in b and 'launchable-activity:' not in b
  assert 'E: instrumentation' not in m
  required=['app/LithicApplication','app/AppProcessRoles','data/media/isolation/MediaSandboxService','data/media/isolation/MediaSandboxProtocol','data/media/isolation/MediaIsolationChannel','data/media/isolation/IsolationProbeFiles','data/media/isolation/IsolationSessionState']
  assert all('Lcom/cooper/lithic/'+n+';' in classes for n in required)
  assert not [x for x in classes if x.startswith(('Lorg/junit/','Ljunit/','Landroidx/test/','Landroidx/compose/ui/test/')) or 'M13IsolatedRunner' in x]
 else:
  assert 'E: instrumentation' in m and 'com.cooper.lithic.app.M13IsolatedRunner' in m and 'com.cooper.lithic.c6isolation.debug' in m
  assert ':targetProcesses(' not in m
  assert all('Lcom/cooper/lithic/'+n+';' in classes for n in ['app/M13IsolatedRunner','app/MediaSandboxStartupTest','data/media/isolation/MediaIsolationChannelTest'])
  assert all(n in strings for n in ['PLAIN_HOST_ADMITTED_NOT_TEST_COMPLETION','OBSERVATION_NOT_JUNIT_COMPLETION','c6.observationFile','com.cooper.lithic.c6isolation.debug','com.cooper.lithic.c6isolation.debug.test'])
# Verify the actual packaged plaintext manifest, in addition to the APK xmltree above.
ns='{http://schemas.android.com/apk/res/android}'
manifest=root/'app/build-c6-isolation/intermediates/packaged_manifests/debug/processDebugManifestForPackage/AndroidManifest.xml';m=ET.parse(manifest).getroot();a=m.find('application');assert a.get(ns+'name')=='com.cooper.lithic.app.LithicApplication';assert a.get(ns+'allowBackup')=='false';assert not a.findall('activity') and not a.findall('activity-alias') and not a.findall('receiver')
services=a.findall('service');providers=a.findall('provider');assert len(services)==len(providers)==1
s=services[0];assert s.get(ns+'name')=='com.cooper.lithic.data.media.isolation.MediaSandboxService' and s.get(ns+'exported')=='false' and s.get(ns+'isolatedProcess')=='true' and s.get(ns+'process')==':media_sandbox'
p=providers[0];assert p.get(ns+'name')=='androidx.startup.InitializationProvider' and p.get(ns+'process')=='com.cooper.lithic.c6isolation.debug' and p.get(ns+'authorities')=='com.cooper.lithic.c6isolation.debug.androidx-startup' and p.get(ns+'exported')=='false' and not list(p)
shutil.copyfile(manifest,out/'app-packaged-manifest.xml')
# Test instrumentation must name exactly the sole independent target.
tm=root/'app/build-c6-isolation/intermediates/packaged_manifests/debugAndroidTest/processDebugAndroidTestManifest/AndroidManifest.xml'
if not tm.is_file():tm=root/'app/build-c6-isolation/intermediates/merged_manifests/debugAndroidTest/processDebugAndroidTestManifest/AndroidManifest.xml'
t=ET.parse(tm).getroot();ins=t.findall('instrumentation');assert len(ins)==1 and ins[0].get(ns+'name')=='com.cooper.lithic.app.M13IsolatedRunner' and ins[0].get(ns+'targetPackage')=='com.cooper.lithic.c6isolation.debug' and ins[0].get(ns+'targetProcesses') is None;shutil.copyfile(tm,out/'test-packaged-manifest.xml')
pre=json.loads((ev/'preflight.json').read_text(encoding='utf-8'))
for name,digest in pre['productionMainSourceSha256'].items():assert sha(root/name)==digest,name
assert sha(root/'app/src/androidTest/java/com/cooper/lithic/app/M13IsolatedRunner.kt')==pre['runnerSha256']
for prior in pre['preservedDeliveryApks']:assert sha(root/prior['path'])==prior['sha256'] and (root/prior['path']).stat().st_size==prior['bytes']
_,_,_,priorlibs=apk_metadata(root/'deliveries/lithic-preview-0.1.0-preview.12/Lithic-0.1.0-preview.12-debug.apk');assert results['app']['nativeEntries']==priorlibs
assert not subprocess.check_output(['git','diff',pre['baselineCommit'],'--','app/src/main','app/schemas','gradle'],text=True)
results.update({'runtime':'NOT_RUN','deviceOperations':0,'appComponents':{'application':'LithicApplication','services':1,'providers':1,'activities':0,'receivers':0,'initializerMetadata':0},'runner':'com.cooper.lithic.app.M13IsolatedRunner','instrumentationTargetProcesses':'DEFAULT_ONLY','preservedHistoricalApkCount':len(pre['preservedDeliveryApks']),'productionMainSourceUnchanged':True,'nativeEntriesUnchangedFromPreview12':True})
(out/'identity.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8');print(json.dumps({k:{a:results[k][a] for a in ['path','bytes','sha256','permissions']} for k in ['app','test']},ensure_ascii=False,indent=2))
