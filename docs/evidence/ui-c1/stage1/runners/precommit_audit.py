from pathlib import Path
import subprocess,json,hashlib,re,zipfile,urllib.parse,collections
R=Path.cwd();D=R/'docs/evidence/ui-c1/stage1';BASE='edc8be772a795f40b44f15da79ebf62a529bc074'
def git(*a):return subprocess.check_output(['git','-c','core.quotepath=false',*a],cwd=R,timeout=120).decode('utf8').strip()
def sha(b):return hashlib.sha256(b).hexdigest()
def put(n,v):(D/n).write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
assert git('branch','--show-current')=='feat/ui-c1-visual-redesign'
assert git('rev-parse','HEAD')==BASE
protected=json.loads((D/'static-current.json').read_text(encoding='utf8'))['protectedScopes']
protected+=['app/src/main/java/com/cooper/lithic/core/design/FormFields.kt','app/src/androidTest/java/com/cooper/lithic/data/media/M10JpegIntegrityTest.kt','app/src/androidTest/java/com/cooper/lithic/data/media/M10JpegOldCoverProtectionTest.kt','docs/evidence/M10-JPEG-supplement-verification.json','docs/checkpoints/2026-10-15-UI-C1-stage0.md','docs/design/ui-c1/source-v1','docs/design/ui-c1/source-v2','docs/design/ui-c1/SOURCES.md']
assert not git('diff','--name-only',BASE,'--',*protected)
changes=git('diff','--name-only','--no-renames',BASE).splitlines()
new=sorted(set(git('ls-files','--others','--exclude-standard').splitlines()+git('diff','--name-only','--diff-filter=A',BASE).splitlines()))
assert not git('diff','--diff-filter=D','--name-only',BASE)
allfiles=sorted(set(changes+new))
assert not any(p.startswith('build/') or p.endswith(('.apk','.aab','.db','.db-wal','.db-shm','.jks','.keystore')) or Path(p).name in ('local.properties','.env','.env.local') for p in allfiles)
nonEvidence=[p for p in allfiles if not p.startswith('docs/evidence/ui-c1/stage1/')]
inventory=['# UI-C1阶段1文件清单','',f'本轮基线 `{BASE}`；无删除。测试/来源/最终APK边界见[报告](../../UI-C1-stage1-verification.md)。','', '## 产品/测试/当前文档','', '| 路径 | 类型 |','|---|---|']
for p in nonEvidence:inventory.append('| `'+p+'` | '+('新增' if p in new else '修改')+' |')
inventory+=['','## 证据目录','', '本目录包含19张实际PNG、8个测试阶段的原始日志/XML/登记输入ZIP/summary、最终APK静态校验和独立Lint/assemble记录、逐图登记、数据/设置校验摘要、复核脚本与图册。','', '完整新/改文件字节清单见 `file-manifest.json`（排除它自己与后生成的提交前登记，避免递归哈希）。原DB/WAL/SHM、私有图片原文件、APK及密钥均不在提交范围。截图只有已有合成QA与隔离测试宿主。','', '新增7份测试辅助/测试文件中6个class含35个方法；UIC1TestActions仅驱动helper，不增加测试方法。旧11份测试只做界面入口/等价断言/IME同步适配，原方法身份保留。','', '阶段0原件、正式矩阵、原130条及旧证据不改。原始输入ZIP是各阶段登记的构建输入快照，不是完整工具环境或活动数据库/用户文件备份；未在逐次快照中的原Debug字符串/启动脚本另以supplemental-inputs.json登记当前零差异。','']
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
manifest=[{'path':p,'kind':'new' if p in new or p.endswith('/files.md') else 'modified','bytes':(R/p).stat().st_size,'sha256':sha((R/p).read_bytes())} for p in files if p not in ('docs/evidence/ui-c1/stage1/file-manifest.json','docs/evidence/ui-c1/stage1/precommit-checks.json')]
put('file-manifest.json',manifest)
put('precommit-checks.json',{'result':'PASS','baseline':BASE,'branch':git('branch','--show-current'),'protectedScopes':protected,'protectedScopeDiffEmpty':True,'deletedFiles':0,'localLinksChecked':len(links),'missingLinks':missing,'secretScan':{'textFiles':scanned,'zipTextEntries':zipEntries,'matches':hits,'scope':'Private key headers and common GitHub/AWS/OpenAI credential patterns plus forbidden filenames/paths; not a universal secret-detection guarantee.'},'noApkDatabaseKeystoreOrLocalProperties':True,'manifestEntries':len(manifest),'note':'Document/scope/byte checks only, not a new product test run. Git staged byte verification and remote receipt follow separately.'})
print(json.dumps({'files':len(manifest),'links':len(links),'secretTextFiles':scanned,'zipTextEntries':zipEntries,'protectedScopes':len(protected),'result':'PASS'},indent=2))
