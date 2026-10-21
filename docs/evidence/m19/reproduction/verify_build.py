import pathlib,os,subprocess,json,hashlib,shutil,time,sys
R=next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file());O=R/'docs/evidence/m19'/sys.argv[1];O.mkdir(exist_ok=False)
env=os.environ.copy();env['JAVA_HOME']='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1'
classes=['com.cooper.lithic.domain.report.AnnualReportEngineTest','com.cooper.lithic.domain.report.CategoryReportEngineTest','com.cooper.lithic.app.M19NavigationPolicyTest','com.cooper.lithic.data.local.CategoryNamesTest','com.cooper.lithic.feature.category.CategoryControllerTest','com.cooper.lithic.app.M14NavigationPolicyTest','com.cooper.lithic.app.M16NavigationPolicyTest','com.cooper.lithic.app.M17NavigationPolicyTest','com.cooper.lithic.app.M18NavigationPolicyTest','com.cooper.lithic.data.repository.TradeInFactsTest','com.cooper.lithic.domain.report.CostTimelineEngineTest','com.cooper.lithic.domain.report.TimelineSamplerTest','com.cooper.lithic.domain.report.OverviewEngineTest']
cmd=['gradlew.bat',':app:testDebugUnitTest']
if sys.argv[1]=='stale-green-build': cmd+=['--rerun']
for c in classes:cmd+=['--tests',c]
cmd+=[':app:lintDebug',':app:assembleDebug',':app:assembleDebugAndroidTest','-Pm13Isolated=true','--max-workers=2','--console=plain']
inputs=sorted([p for d in ['app/src','app/schemas','gradle'] for p in (R/d).rglob('*') if p.is_file()]+[R/p for p in ['app/build.gradle.kts','build.gradle.kts','settings.gradle.kts','gradle.properties']],key=lambda p:p.relative_to(R).as_posix())
def fp():
 h=hashlib.sha256()
 for p in inputs:h.update(p.relative_to(R).as_posix().encode()+b'\0'+hashlib.sha256(p.read_bytes()).digest())
 return dict(file_count=len(inputs),sha256=h.hexdigest())
before=fp();start=time.monotonic()
with (O/'stdout.txt').open('xb') as out,(O/'stderr.txt').open('xb') as err:r=subprocess.run(cmd,cwd=R,env=env,stdout=out,stderr=err,timeout=1200)
for c in classes:
 p=R/f'app/build/test-results/testDebugUnitTest/TEST-{c}.xml'
 if p.exists():shutil.copyfile(p,O/p.name)
result=dict(command=cmd,classes=classes,exit_code=r.returncode,seconds=round(time.monotonic()-start,3),inputs_before=before,inputs_after=fp())
(O/'result.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps(result));assert before==fp(),'Source changed during verification';raise SystemExit(r.returncode)
