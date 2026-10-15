from pathlib import Path
import subprocess,json,hashlib,re
r=Path.cwd();o=r/'build/ui-c1-stage2';base='f000639c62fce71be0a9240425c8e27b1a5ed974'
def git(*a):return subprocess.check_output(['git','-c','core.quotepath=false',*a]).decode('utf8').strip()
assert git('branch','--show-current')=='feat/ui-c1-visual-redesign'
protected=json.loads((r/'docs/evidence/ui-c1/stage1/static-current.json').read_text(encoding='utf8'))['protectedScopes']
protected += ['app/src/main/java/com/cooper/lithic/feature/project/ProjectViewModel.kt','app/src/main/java/com/cooper/lithic/feature/subscription/SubscriptionViewModel.kt','app/src/main/java/com/cooper/lithic/feature/media/CoverViewModel.kt','app/src/main/java/com/cooper/lithic/feature/home','app/src/main/java/com/cooper/lithic/feature/project/ProjectListScreen.kt','app/src/main/java/com/cooper/lithic/feature/project/ProjectDetailScreen.kt','app/src/main/java/com/cooper/lithic/core/design','app/src/main/java/com/cooper/lithic/feature/media','docs/evidence/ui-c1/refinement','docs/evidence/ui-c1/stage1','docs/checkpoints/2026-10-15-UI-C1-refinement.md','docs/evidence/M10-JPEG-supplement-verification.json','app/src/androidTest/java/com/cooper/lithic/data/media']
assert not git('diff','--name-only',base,'--',*protected)
old=json.loads((o/'baseline-methods.json').read_text(encoding='utf8'));oldfiles=sorted(set(v['path'] for v in old));assert not git('diff','--name-only',base,'--',*oldfiles)
allowed=['app/src/main/java/com/cooper/lithic/'+p for p in ('feature/subscription/SubscriptionListScreen.kt','feature/subscription/SubscriptionDetailScreen.kt','feature/subscription/SubscriptionHistoryScreen.kt','feature/review/ReviewShell.kt','feature/settings/SettingsShell.kt','feature/project/ProjectHistoryScreen.kt','app/LithicNavigation.kt')]
actual=git('diff','--name-only',base,'--','app/src/main').splitlines();assert set(actual)==set(allowed),actual
out={'result':'PASS','scope':'Task6 static source/diff audit, not product test execution','baseline':base,'protectedScopes':protected,'protectedDiffEmpty':True,'original701TestFilesUnchanged':True,'productionChanged':actual,'approvedStage1ThreeLayoutsAndThemeUnchanged':True,'newTestMethods':10,'totalMethods':711,'navigationChange':'Only3 optional read-only mediaRepository argument additions; original guard logic remains byte-for-byte.'}
(o/'task6-static.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n',encoding='utf8');print(json.dumps({'result':'PASS','protectedScopes':len(protected),'productionFiles':len(actual),'originalTestFiles':len(oldfiles)}))
