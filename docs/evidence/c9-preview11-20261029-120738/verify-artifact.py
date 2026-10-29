from pathlib import Path
import subprocess, os, json, hashlib, zipfile, struct, re, shutil
root = Path.cwd()
evidence = root / "docs/evidence/c9-preview11-20261029-120738"
for stage in ["normal-debug", "private-trial"]:
    assert json.loads((evidence/stage/"command.json").read_text(encoding="utf-8"))["exitCode"] == 0
out = evidence / "artifact"
out.mkdir()
sdk = Path("C:/Users/CDD/AppData/Local/MirraAndroid/sdk/build-tools/36.0.0")
env = dict(os.environ)
env["JAVA_HOME"] = "C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1"
env["PATH"] = env["JAVA_HOME"] + "/bin;" + env["PATH"]
apk = root / "app/build/private-trial/outputs/apk/debug/app-debug.apk"
old = root / "deliveries/lithic-preview-0.1.0-preview.10/Lithic-0.1.0-preview.10-debug.apk"
normal = root / "app/build/outputs/apk/debug/app-debug.apk"
cert = "29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9"
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(old) == "55a3646e62661760c78797d14d9912a74a89b852c065037056d8d0fad840bc70"
commands = []
def capture(label, args):
    r = subprocess.run(args, env=env, capture_output=True)
    (out/(label+".stdout")).write_bytes(r.stdout)
    (out/(label+".stderr")).write_bytes(r.stderr)
    commands.append({"argv": args, "exitCode": r.returncode})
    (out/"commands.json").write_text(json.dumps(commands,indent=2)+"\n",encoding="utf-8")
    assert r.returncode == 0, label
    return r.stdout.decode("utf-8", errors="replace")
b = capture("badging", [str(sdk/"aapt2.exe"),"dump","badging",str(apk)])
m = capture("manifest", [str(sdk/"aapt2.exe"),"dump","xmltree",str(apk),"--file","AndroidManifest.xml"])
c = capture("certificate", [str(sdk/"apksigner.bat"),"verify","--verbose","--print-certs",str(apk)])
n = capture("normal-badging", [str(sdk/"aapt2.exe"),"dump","badging",str(normal)])
assert "name='com.cooper.lithic.preview.debug'" in b and "versionCode='27'" in b and "versionName='0.1.0-preview.11-debug'" in b
assert "application-label:'砺石·体验版'" in b
assert cert in c.lower()
assert "name='com.cooper.lithic.debug'" in n and "versionCode='17'" in n
assert "com.cooper.lithic.app.LithicApplication" in m and "com.cooper.lithic.app.MainActivity" in m
assert "E: instrumentation" not in m and ":sharedUserId(" not in m
assert not re.search(r":testOnly\([^\n]*=true",m)
assert "launchable-activity: name='com.cooper.lithic.app.MainActivity'" in b
permissions = sorted(re.findall(r"uses-permission: name='([^']+)'",b))
old_b = (evidence/"preview10/badging.stdout").read_text(encoding="utf-8")
assert permissions == sorted(re.findall(r"uses-permission: name='([^']+)'",old_b)) and "android.permission.INTERNET" not in permissions
old_m = (evidence/"preview10/manifest.stdout").read_text(encoding="utf-8")
authorities = re.findall(r':authorities[^=]*="([^\"]+)"',m)
assert authorities == re.findall(r':authorities[^=]*="([^\"]+)"',old_m)
def classdefs(data):
    u32 = lambda pos: struct.unpack_from("<I",data,pos)[0]
    strings, types = u32(0x3c), u32(0x44)
    result = set()
    for i in range(u32(0x60)):
        class_index = u32(u32(0x64)+i*32)
        pos = u32(strings+u32(types+class_index*4)*4)
        while data[pos] & 128: pos += 1
        pos += 1
        result.add(data[pos:data.index(b"\0",pos)].decode("utf-8"))
    return result
with zipfile.ZipFile(apk) as z, zipfile.ZipFile(old) as prior:
    classes = set().union(*(classdefs(z.read(x)) for x in z.namelist() if re.fullmatch(r"classes\d*\.dex",x)))
    required = ["app/LithicApplication","app/MainActivity","app/LithicAppKt","app/LithicNavigationKt","feature/appearance/AppearanceProvider","feature/appearance/AppearanceController","feature/appearance/DataStoreAppearanceStorage","feature/appearance/LithicAppearance","feature/appearance/AppearanceThemePickerKt","core/design/LithicThemeKt","core/design/LithicVisualsKt","feature/home/HomeShellKt","feature/review/ReviewShellKt","feature/project/ProjectDetailScreenKt","feature/subscription/SubscriptionDetailScreenKt","feature/project/ProjectEditorScreenKt","feature/subscription/SubscriptionEditorScreenKt","feature/settings/SettingsShellKt","feature/media/CoverPreviewsKt","data/media/isolation/MediaSandboxService"]
    present = {x: "Lcom/cooper/lithic/"+x+";" in classes for x in required}
    assert all(present.values()), present
    forbidden = [x for x in classes if x.startswith(("Lorg/junit/","Ljunit/","Landroidx/test/","Landroidx/compose/ui/test/")) or any(n in x for n in ["M13IsolatedRunner","C2VisualFixtures","C9ThemeMatrixTest","C9RecordThemeTest","CompleteJourneyTest"])]
    assert not forbidden, forbidden[:10]
    libs = {x:hashlib.sha256(z.read(x)).hexdigest() for x in z.namelist() if x.startswith("lib/") and x.endswith(".so")}
    oldlibs = {x:hashlib.sha256(prior.read(x)).hexdigest() for x in prior.namelist() if x.startswith("lib/") and x.endswith(".so")}
    assert libs == oldlibs
assert not subprocess.check_output(["git","diff","9c0956f","--","app/src","app/schemas","gradle"],text=True)
assert sha(root/"app/build.gradle.kts") == json.loads((evidence/"private-trial/command.json").read_text(encoding="utf-8"))["gradleSourceSha256"]
delivery = root / "deliveries/lithic-preview-0.1.0-preview.11/Lithic-0.1.0-preview.11-debug.apk"
delivery.parent.mkdir()
shutil.copyfile(apk,delivery)
assert sha(delivery) == sha(apk) and delivery.stat().st_size == apk.stat().st_size
record = {"deliveryPath":str(delivery),"bytes":delivery.stat().st_size,"sha256":sha(delivery),"package":"com.cooper.lithic.preview.debug","versionCode":27,"versionName":"0.1.0-preview.11-debug","certificateSha256":cert,"permissions":permissions,"authorities":authorities,"productionClassDefinitions":present,"testClassDefinitionsAbsent":True,"nativeEntriesUnchanged":libs,"sourceBaseline":"9c0956f85dcdc53d2cae2e0260f3dcaf23653078","sourceDiff":"Private Trial version lines only; production/schema/dependency files unchanged","preview10Sha256StillMatches":sha(old),"sourceAndDeliveryByteIdentical":True,"deviceOperations":0}
(out/"identity.json").write_text(json.dumps(record,indent=2)+"\n",encoding="utf-8")
print(json.dumps({k:record[k] for k in ["deliveryPath","bytes","sha256","certificateSha256"]},ensure_ascii=False,indent=2))
