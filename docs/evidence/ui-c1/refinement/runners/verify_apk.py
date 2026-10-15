from pathlib import Path
import subprocess,hashlib,json,os,shutil,zipfile,tomllib,re
R=Path.cwd();O=R/'build/ui-c1-refinement';D=O/'apk';D.mkdir(exist_ok=True)
S=Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk');J=Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1');A=S/'platform-tools/adb.exe';env=os.environ.copy();env['JAVA_HOME']=str(J)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(cmd,name):
 p=subprocess.run([str(v) for v in cmd],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,env=env);(D/name).write_bytes(p.stdout);assert p.returncode==0,(name,p.stdout[-1000:]);return p.stdout.decode('utf-8','replace')
apk=O/'lithic-ui-c1-refinement-debug.apk';shutil.copyfile(R/'app/build/outputs/apk/debug/app-debug.apk',apk)
test=R/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
badging=run([S/'build-tools/36.0.0/aapt2.exe','dump','badging',apk],'apk-badging.txt')
manifest=run([S/'build-tools/36.0.0/aapt2.exe','dump','xmltree',apk,'--file','AndroidManifest.xml'],'apk-manifest.txt')
for file,name in [('res/xml/backup_rules.xml','apk-backup.txt'),('res/xml/data_extraction_rules.xml','apk-extraction.txt')]:run([S/'build-tools/36.0.0/aapt2.exe','dump','xmltree',apk,'--file',file],name)
sig=run([S/'build-tools/36.0.0/apksigner.bat','verify','--print-certs',apk],'signature.txt')
old=(R/'build/ui-c1-stage1/apk/signature.txt').read_text(encoding='utf-8');fingerprints=lambda s:[l for l in s.splitlines() if 'certificate SHA-256 digest:' in l];assert fingerprints(sig)==fingerprints(old)
assert "package: name='com.cooper.lithic.debug' versionCode='8' versionName='0.1.0-m10-debug'" in badging
permissions=re.findall(r"uses-permission: name='([^']+)'",badging);assert permissions==['com.cooper.lithic.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'],permissions
assert 'allowBackup' in manifest and '0xffffffff' not in next(l for l in manifest.splitlines() if 'allowBackup' in l)
for name in ('apk-backup.txt','apk-extraction.txt'):assert (D/name).read_bytes()==(R/'build/ui-c1-stage1/apk'/name).read_bytes(),name
with zipfile.ZipFile(apk) as z:
 names=z.namelist();bad=[n for n in names if any(t in n.lower() for t in ('fixture-source','ui-c1-refined','synthetic-','jpeg_probe'))];assert not bad,bad
 dex=b''.join(z.read(n) for n in names if n.endswith('.dex'))
 forbidden=[n for n in ['UIC1RefinedVisualEvidenceTest','UIC1RefinedHomeTest','UIC1RefinedDetailTest','UIC1RefinedListTest','fixture-source.json','轻薄笔记本','通勤耳机','旅行相机'] if n.encode('utf-8') in dex];assert not forbidden,forbidden
apkPath=subprocess.check_output([str(A),'-s','emulator-5596','shell','pm','path','com.cooper.lithic.debug']).decode().strip().split('package:')[1]
installed=subprocess.check_output([str(A),'-s','emulator-5596','shell','sha256sum',apkPath]).decode().split()[0];assert installed==sha(apk)
v={'apk':str(apk),'bytes':apk.stat().st_size,'sha256':sha(apk),'installedApkSha256':installed,'androidTestApkSha256':sha(test),'androidTestApkBytes':test.stat().st_size,'applicationId':'com.cooper.lithic.debug','versionCode':8,'versionName':'0.1.0-m10-debug','permissions':permissions,'signatureSha256Unchanged':fingerprints(sig),'backupConfigUnchanged':True,'testFixtureClassesAndSyntheticNamesAbsentFromTargetDex':True,'forbiddenAssetNames':bad,'versions':tomllib.loads((R/'gradle/libs.versions.toml').read_text(encoding='utf-8'))['versions'],'wrapperJarSha256':sha(R/'gradle/wrapper/gradle-wrapper.jar'),'scope':'Debug artifact only, not Release/OEM or system restore validation. Test host runtime data is in a separate androidTest APK/process-generation path; not real production records.'}
(D/'apk-verification.json').write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');print(json.dumps(v,ensure_ascii=True,indent=2))
versions=[]
for cmd in [[J/'bin/java.exe','-version'],[A,'version'],[S/'build-tools/36.0.0/aapt2.exe','version']]:
 p=subprocess.run([str(x) for x in cmd],stdout=subprocess.PIPE,stderr=subprocess.STDOUT);assert p.returncode==0;versions.append({'command':[str(x) for x in cmd],'exitCode':p.returncode,'output':p.stdout.decode('utf-8','replace')})
(O/'tool-versions.json').write_text(json.dumps(versions,indent=2)+'\n',encoding='utf-8')
