from pathlib import Path
import json,shutil,hashlib,subprocess,zipfile
R=Path.cwd();O=R/'build/ui-c1-stage2';D=R/'docs/evidence/ui-c1/stage2/task8';D.mkdir(exist_ok=True)
phase='stage2-final-restored-full'
s=json.loads((O/(phase+'-summary.json')).read_text(encoding='utf8'))
assert len(s['actualCommandExecutions'])==3 and s['inputsStable']
assert s['total']==718 and s['jvm']==86 and s['android']==632 and not s['missingOriginal701']
assert s['skipped']==0 and len({(c['class'],c['name']) for c in s['cases']})==718
for p in O.glob(phase+'*'):
 if p.is_file() and p.suffix in ('.json','.log','.xml','.zip'):shutil.copyfile(p,D/p.name)
for n in ('stage2-final-restored-static.json','final-local-scope-review.md','final_static_audit.py','full_run.py','publish_final_run.py','verify_apk.py','tool-versions.json'):
 p=O/n
 if p.exists():shutil.copyfile(p,D/p.name)
ad=D/'apk';ad.mkdir(exist_ok=True)
for p in (O/'apk').iterdir():
 if p.is_file():shutil.copyfile(p,ad/p.name)
baseline=json.loads((O/'baseline-methods.json').read_text(encoding='utf8'))
old={(c['class'],c['name']) for c in baseline}
new=[c for c in s['cases'] if (c['class'],c['name']) not in old]
assert len(new)==17
proof={'frozenSourceHead':s['frozenSourceHead'],'sourceInputsSha256':s['sourceInputsSha256'],'originalMethods':701,'retainedOriginalMethods':len(old),'missing':s['missingOriginal701'],'newMethods':new,'failures':[c for c in s['cases'] if c['result']!='PASS'],'overall':s['overall'],'total':s['total'],'jvm':s['jvm'],'android':s['android'],'pass':s['pass'],'fail':s['fail'],'skipped':s['skipped'],'sourceArchiveSha256':s['sourceZipSha256'],'testAssertionsAudit':'stage2-final-restored-static.json; original UIComponents test retained with separately disclosed diagnostics/test-host safe insets only'}
(D/'method-identity-results.json').write_text(json.dumps(proof,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({k:proof[k] for k in ('total','jvm','android','pass','fail','skipped','overall')}))
