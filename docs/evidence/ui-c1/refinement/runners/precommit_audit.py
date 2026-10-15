from pathlib import Path
import subprocess,json,hashlib,re,zipfile,urllib.parse,collections
R=Path.cwd();D=R/'docs/evidence/ui-c1/refinement';BASE='53fd944fe70a912fd4c1e1fa76d4ec31d1a103d5'
def git(*a):return subprocess.check_output(['git','-c','core.quotepath=false',*a],cwd=R,timeout=120).decode('utf8').strip()
def sha(b):return hashlib.sha256(b).hexdigest()
def put(n,v):(D/n).write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
assert git('branch','--show-current')=='feat/ui-c1-visual-redesign'
assert git('rev-parse','HEAD')==BASE
protected=json.loads((R/'docs/evidence/ui-c1/stage1/static-current.json').read_text(encoding='utf8'))['protectedScopes']
protected += ['app/src/main/java/com/cooper/lithic/app', 'app/src/main/java/com/cooper/lithic/core/design', 'docs/evidence/ui-c1/stage1', 'docs/evidence/UI-C1-stage1-verification.md', 'docs/checkpoints/2026-10-15-UI-C1-stage1.md']
protected+=['app/src/main/java/com/cooper/lithic/core/design/FormFields.kt','app/src/androidTest/java/com/cooper/lithic/data/media/M10JpegIntegrityTest.kt','app/src/androidTest/java/com/cooper/lithic/data/media/M10JpegOldCoverProtectionTest.kt','docs/evidence/M10-JPEG-supplement-verification.json','docs/checkpoints/2026-10-15-UI-C1-stage0.md','docs/design/ui-c1/source-v1','docs/design/ui-c1/source-v2','docs/design/ui-c1/SOURCES.md']
assert not git('diff','--name-only',BASE,'--',*protected)
changes=git('diff','--name-only','--no-renames',BASE).splitlines()
new=sorted(set(git('ls-files','--others','--exclude-standard').splitlines()+git('diff','--name-only','--diff-filter=A',BASE).splitlines()))
assert not git('diff','--diff-filter=D','--name-only',BASE)
allfiles=sorted(set(changes+new))
assert not any(p.startswith('build/') or p.endswith(('.apk','.aab','.db','.db-wal','.db-shm','.jks','.keystore')) or Path(p).name in ('local.properties','.env','.env.local') for p in allfiles)
nonEvidence=[p for p in allfiles if not p.startswith('docs/evidence/ui-c1/refinement/')]
inventory=['# UI-C1 阶段1视觉精修文件清单','',f'基线 `{BASE}`。无删除；四个生产展示文件及相关测试/文档/新证据，范围见[精修报告](../../UI-C1-refinement-verification.md)。','', '| 路径 | 类型 |','|---|---|']
for p in nonEvidence:inventory.append('| `'+p+'` | '+('新增' if p in new else '修改')+' |')
inventory += ['', '## 新证据目录', '', '27张复审原始PNG、另列的失败/转场诊断、10个实际执行阶段的日志/鲜取XML/输入ZIP/逐方法summary、最终APK及测试APK指纹/签名/Manifest/备份配置、真实工具输出、Lint、前后数据保护摘要、可复核脚本、图册及来源登记。', '', '原APK/测试APK/DB/WAL/SHM/私有原图/密钥不提交。原43份规划、130案例、16组边界、53fd944原19图/旧报告/checkpoint和M10证据不改。', '', '原692个方法身份保留，本轮4个新测试class共9方法，总701；旧7份测试只做等价计数/返回语义选择器与隔离空态截图窗口适配。旧表单/JPEG源码和Nav3守卫均无改动。', '', '完整字节清单见file-manifest.json；提交前检查见precommit-checks.json。输入ZIP为各阶段登记构建输入，不是完整工具环境或用户数据备份。', '']
(D/'files.md').write_text('\n'.join(inventory),encoding='utf8')
checks=[R/p for p in nonEvidence if p.endswith('.md')]+[D/'GALLERY.md',D/'files.md']
links=[];missing=[]
for f in checks:
 for raw in re.findall(r'\]\((<?[^)\n]+>?)\)',f.read_text(encoding='utf8')):
  target=raw.strip('<>').split('#')[0]
  if not target or re.match(r'^[a-zA-Z][a-zA-Z0-9+.-]*:',target):continue
  path=(f.parent/urllib.parse.unquote(target)).resolve();links.append({'source':f.relative_to(R).as_posix(),'target':raw})
  if not path.exists():missing.append(links[-1])
assert not missing,missing
patterns=[re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),re.compile(r'\b(?:ghp|gho|github_pat)_[A-Za-z0-9_]{25,}'),re.compile(r'\bAKIA[A-Z0-9]{16}\b'),re.compile(r'\bsk-(?:proj-)?[A-Za-z0-9_-]{25,}')]
textExt={'.kt','.xml','.kts','.toml','.properties','.md','.txt','.json','.py','.log','.ps1','.js'};scanned=0;zipEntries=0;hits=[]
for p in sorted(set(allfiles+[str((D/'files.md').relative_to(R)).replace('\\','/')])):
 f=R/p
 if f.suffix in textExt:
  content=f.read_text(encoding='utf8',errors='replace');scanned+=1
  if any(x.search(content) for x in patterns):hits.append(p)
 if f.suffix=='.zip':
  with zipfile.ZipFile(f) as z:
   for name in z.namelist():
    assert not name.startswith(('/','\\')) and '..' not in Path(name).parts and ':' not in name
    assert not name.endswith(('.apk','.db','.keystore','.jks','local.properties','.env','.env.local')),name
    if Path(name).suffix in textExt:
     content=z.read(name).decode('utf8','replace');zipEntries+=1
     if any(x.search(content) for x in patterns):hits.append(p+':'+name)
assert not hits,hits
files=sorted(set(changes+git('ls-files','--others','--exclude-standard').splitlines()))
manifest=[{'path':p,'kind':'new' if p in new or p.endswith('/files.md') else 'modified','bytes':(R/p).stat().st_size,'sha256':sha((R/p).read_bytes())} for p in files if p not in ('docs/evidence/ui-c1/refinement/file-manifest.json','docs/evidence/ui-c1/refinement/precommit-checks.json')]
put('file-manifest.json',manifest)
put('precommit-checks.json',{'result':'PASS','baseline':BASE,'branch':git('branch','--show-current'),'protectedScopes':protected,'protectedScopeDiffEmpty':True,'deletedFiles':0,'localLinksChecked':len(links),'missingLinks':missing,'secretScan':{'textFiles':scanned,'zipTextEntries':zipEntries,'matches':hits,'scope':'Private key headers and common GitHub/AWS/OpenAI credential patterns plus forbidden filenames/paths; not a universal secret-detection guarantee.'},'noApkDatabaseKeystoreOrLocalProperties':True,'manifestEntries':len(manifest),'note':'Document/scope/byte checks only, not a new product test run. Git staged byte verification and remote receipt follow separately.'})
print(json.dumps({'files':len(manifest),'links':len(links),'secretTextFiles':scanned,'zipTextEntries':zipEntries,'protectedScopes':len(protected),'result':'PASS'},indent=2))
