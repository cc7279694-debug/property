from pathlib import Path
import subprocess,re,json,hashlib
r=Path.cwd();base='f000639c62fce71be0a9240425c8e27b1a5ed974';out=[]
for folder in ('app/src/test','app/src/androidTest'):
 files=subprocess.check_output(['git','ls-tree','-r','--name-only',base,'--',folder],cwd=r).decode().splitlines()
 for p in files:
  if not p.endswith('.kt'):continue
  raw=subprocess.check_output(['git','show',base+':'+p],cwd=r);s=raw.decode('utf8');pkg=re.search(r'^package\s+(\S+)',s,re.M).group(1)
  classes=re.findall(r'^(?:internal\s+)?class\s+(\w+)',s,re.M)
  tests=re.findall(r'@Test\b(?:\([^)]*\))?\s*(?:\r?\n\s*)?(?:@\w+(?:\([^)]*\))?\s*)*fun\s+(\w+)\s*\(',s)
  if tests:
   assert len(classes)==1,(p,classes)
   out.extend({'kind':'jvm' if '/test/' in p else 'android','class':pkg+'.'+classes[0],'name':n,'path':p,'baseFileSha256':hashlib.sha256(raw).hexdigest()} for n in tests)
assert len(out)==701,len(out)
(r/'build/ui-c1-stage2/baseline-methods.json').write_text(json.dumps(out,indent=2)+'\n',encoding='utf8')
print(json.dumps({'baseline':base,'methods':len(out),'jvm':sum(x['kind']=='jvm' for x in out),'android':sum(x['kind']=='android' for x in out)}))
