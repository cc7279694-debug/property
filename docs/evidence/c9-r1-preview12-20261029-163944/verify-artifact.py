from pathlib import Path
import subprocess, os, json, hashlib, zipfile, struct, re, shutil
root=Path.cwd(); ev=root/"docs/evidence/c9-r1-preview12-20261029-163944"
assert json.loads((ev/"build-check/command.json").read_text(encoding="utf-8"))["exitCode"]==0
out=ev/"artifact"; out.mkdir()
sdk=Path("C:/Users/CDD/AppData/Local/MirraAndroid/sdk/build-tools/36.0.0")
env=dict(os.environ);env["JAVA_HOME"]="C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1";env["PATH"]=env["JAVA_HOME"]+"/bin;"+env["PATH"]
apk=root/"app/build/private-trial/outputs/apk/debug/app-debug.apk"
old=root/"deliveries/lithic-preview-0.1.0-preview.11/Lithic-0.1.0-preview.11-debug.apk"
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
oldsha="5bbf3c1f946fc2da4ab259ab377f8134e222255784fe81e20c7a73337e63cf90"
cert="29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9"
assert old.stat().st_size==15961550 and sha(old)==oldsha
cmds=[]
def capture(label,args):
 r=subprocess.run(args,env=env,capture_output=True)
 (out/(label+".stdout")).write_bytes(r.stdout);(out/(label+".stderr")).write_bytes(r.stderr)
 cmds.append({"argv":args,"exitCode":r.returncode});(out/"commands.json").write_text(json.dumps(cmds,indent=2)+"\n",encoding="utf-8")
 assert r.returncode==0,label
 return r.stdout.decode("utf-8",errors="replace")
b=capture("badging",[str(sdk/"aapt2.exe"),"dump","badging",str(apk)])
m=capture("manifest",[str(sdk/"aapt2.exe"),"dump","xmltree",str(apk),"--file","AndroidManifest.xml"])
c=capture("certificate",[str(sdk/"apksigner.bat"),"verify","--verbose","--print-certs",str(apk)])
assert "name='com.cooper.lithic.preview.debug'" in b and "versionCode='28'" in b and "versionName='0.1.0-preview.12-debug'" in b
assert "application-label:'砺石·体验版'" in b and cert in c.lower()
assert "com.cooper.lithic.app.LithicApplication" in m and "launchable-activity: name='com.cooper.lithic.app.MainActivity'" in b
assert "E: instrumentation" not in m and ":sharedUserId(" not in m and not re.search(r":testOnly\([^\n]*=true",m)
oldb=(ev/"preview11/badging.stdout").read_text(encoding="utf-8");oldm=(ev/"preview11/manifest.stdout").read_text(encoding="utf-8")
permissions=sorted(re.findall(r"uses-permission: name='([^']+)'",b));assert permissions==sorted(re.findall(r"uses-permission: name='([^']+)'",oldb)) and "android.permission.INTERNET" not in permissions
authorities=re.findall(r':authorities[^=]*="([^\"]+)"',m);assert authorities==re.findall(r':authorities[^=]*="([^\"]+)"',oldm)
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
classes,roles,strings,libs=apk_metadata(apk);_,oldroles,oldstrings,oldlibs=apk_metadata(old)
required=["app/LithicApplication","app/MainActivity","app/LithicAppKt","app/LithicNavigationKt","feature/appearance/AppearanceProvider","feature/appearance/AppearanceController","feature/appearance/DataStoreAppearanceStorage","feature/appearance/LithicAppearance","feature/appearance/AppearanceThemePickerKt","core/design/LithicThemeKt","core/design/LithicVisuals","core/design/C2RecordIdentityKt","core/design/CostChartKt","core/design/CostChartPageKt","feature/home/HomeShellKt","feature/review/ReviewShellKt","feature/review/TotalCostTrendScreenKt","feature/project/ProjectDetailScreenKt","feature/subscription/SubscriptionDetailScreenKt","feature/project/ProjectEditorScreenKt","feature/subscription/SubscriptionEditorScreenKt","feature/settings/SettingsShellKt","feature/media/CoverPreviewsKt","data/media/isolation/MediaSandboxService"]
presence={x:"Lcom/cooper/lithic/"+x+";" in classes for x in required};assert all(presence.values()),presence
r1roles={"brandSurface","identitySurface","identityForeground"};tags={"project-ownership-period","cost-chart-legend","cost-chart-axis-unit","chart-cutoff","total-trend-selection-label","total-trend-contributions","total-trend-axis-unit"}
assert r1roles<=roles and not r1roles&oldroles
assert tags<=strings and not tags&oldstrings
forbidden=[x for x in classes if x.startswith(("Lorg/junit/","Ljunit/","Landroidx/test/","Landroidx/compose/ui/test/")) or any(t in x for t in ["M13IsolatedRunner","C2VisualFixtures","C9R1DetailSemanticsTest","C9R1HomeHierarchyTest","C9R1ReviewSemanticsTest","CompleteJourneyTest"])]
assert not forbidden,forbidden[:10]
assert libs==oldlibs
assert not subprocess.check_output(["git","diff","3aa4f30","--","app/src","app/schemas","gradle"],text=True)
assert sha(root/"app/build.gradle.kts")==json.loads((ev/"build-check/command.json").read_text(encoding="utf-8"))["gradleSourceSha256"]
delivery=root/"deliveries/lithic-preview-0.1.0-preview.12/Lithic-0.1.0-preview.12-debug.apk";delivery.parent.mkdir();shutil.copyfile(apk,delivery)
assert sha(delivery)==sha(apk) and delivery.stat().st_size==apk.stat().st_size
result={"deliveryPath":str(delivery),"bytes":delivery.stat().st_size,"sha256":sha(delivery),"package":"com.cooper.lithic.preview.debug","versionCode":28,"versionName":"0.1.0-preview.12-debug","certificateSha256":cert,"permissions":permissions,"authorities":authorities,"productionClassDefinitions":presence,"r1DeclaredFields":sorted(r1roles),"r1NewTags":sorted(tags),"r1MarkersAbsentInPreview11":True,"testClassDefinitionsAbsent":True,"nativeEntriesUnchanged":libs,"baselineCommit":"3aa4f30f038843f91064529b147ccfc2e9bdc39e","preview11Preserved":{"sha256":sha(old),"bytes":old.stat().st_size},"sourceAndDeliveryByteIdentical":True,"deviceOperations":0}
(out/"identity.json").write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8")
print(json.dumps({k:result[k] for k in ["deliveryPath","bytes","sha256","certificateSha256"]},ensure_ascii=False,indent=2))
