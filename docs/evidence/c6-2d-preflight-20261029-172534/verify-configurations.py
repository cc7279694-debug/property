from pathlib import Path
import os,subprocess,json,re,time
root=Path.cwd();r=root/'docs/evidence/c6-2d-preflight-20261029-172534';out=r/'configurations';out.mkdir()
env=dict(os.environ);env['JAVA_HOME']='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1';env['ANDROID_HOME']='C:/Users/CDD/AppData/Local/MirraAndroid/sdk';env['PATH']=env['JAVA_HOME']+'/bin;'+env['PATH']
cases=[('ordinary',[],('com.cooper.lithic.debug',17,'0.1.0-m22-debug',True)),('trial',['-PlithicPrivateTrial=true'],('com.cooper.lithic.preview.debug',28,'0.1.0-preview.12-debug',True)),('m22',['-Pm22DeviceVerification=true','-Pm13Isolated=true'],('com.cooper.lithic.m22verify.debug',17,'0.1.0-m22-debug',True)),('c3',['-Pc2VisualVerification=true'],('com.cooper.lithic.c2visual.debug',17,'0.1.0-c2visual-debug',False)),('c6',['-Pc6IsolationVerification=true'],('com.cooper.lithic.c6isolation.debug',17,'0.1.0-c6isolation-debug',False)),('reject-invalid',['-Pc6IsolationVerification=invalid'],'must be true or false'),('reject-trial',['-Pc6IsolationVerification=true','-PlithicPrivateTrial=true'],'C6 isolation cannot use'),('reject-m22',['-Pc6IsolationVerification=true','-Pm22DeviceVerification=true','-Pm13Isolated=true'],'C6 isolation cannot use'),('reject-c3',['-Pc6IsolationVerification=true','-Pc2VisualVerification=true'],'C6 isolation cannot use'),('reject-runner',['-Pc6IsolationVerification=true','-Pm13Isolated=false'],'requires the plain Application'),('reject-process-death',['-Pc6IsolationVerification=true','-Pm12T04ProcessDeath=true'],'cannot enable historical process-death')]
results=[]
for label,props,expected in cases:
 args=[str(root/'gradlew.bat'),'--offline','--console=plain','--no-daemon','--max-workers=1','-I',str(r/'identity-check.init.gradle'),*props,':app:c6IdentitySnapshot']
 start=time.time();p=subprocess.run(args,env=env,capture_output=True,timeout=180);(out/(label+'.stdout')).write_bytes(p.stdout);(out/(label+'.stderr')).write_bytes(p.stderr);text=(p.stdout+p.stderr).decode('utf-8',errors='replace');row={'case':label,'args':args,'exitCode':p.returncode,'seconds':round(time.time()-start,3),'deviceOperations':0}
 if isinstance(expected,tuple):
  assert p.returncode==0,(label,text[-1000:]);m=re.search('C6_IDENTITY_JSON=(.*)',text);assert m,(label,text[-1000:]);d=json.loads(m.group(1));assert (d['debugPackage'],d['versionCode'],d['debugVersion'],d['releaseEnabled'])==expected,(label,d);assert d['releasePackage']=='com.cooper.lithic';row['identity']=d
  if label=='c6':assert d['testPackage']=='com.cooper.lithic.c6isolation.debug.test' and d['runner']=='com.cooper.lithic.app.M13IsolatedRunner' and d['buildDirectory'].replace('\\','/').endswith('/app/build-c6-isolation')
  if label=='ordinary':assert d['runner']=='androidx.test.runner.AndroidJUnitRunner'
  row['result']='PASS'
 else:
  assert p.returncode!=0 and expected in text,(label,text[-1000:]);row['result']='EXPECTED_REJECTION'
 results.append(row);(out/'results.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8');print(label,row['result'],flush=True)
print('CONFIGURATION_CHECKS_COMPLETE',len(results))
