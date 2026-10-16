from pathlib import Path
import subprocess,hashlib,json
r=Path.cwd();o=r/'build/ui-c1-stage2';results=[]
summary=json.loads((o/'stage2-final-full-summary.json').read_text(encoding='utf8'))
assert len(summary['actualCommandExecutions'])==3,'Wait for whole first run, including Lint/build'
assert summary['total']==718 and summary['jvm']==86 and summary['android']==632
assert not (o/'schema-raw-restoration.json').exists()
for version in (1,2):
 name='app/schemas/com.cooper.lithic.data.local.LithicDatabase/'+str(version)+'.json';p=r/name
 old=p.read_bytes();blob=subprocess.check_output(['git','show','HEAD:'+name])
 assert old.replace(b'\r\n',b'\n')==blob and json.loads(old)==json.loads(blob)
 if version==1:assert hashlib.sha256(blob).hexdigest()=='351b2e7c1fb952bce961c52948a6711c999f59c79dfd66e2da5564d2697b823b'
 p.write_bytes(blob)
 results.append({'path':name,'beforeBytes':len(old),'beforeSha256':hashlib.sha256(old).hexdigest(),'afterBytes':len(blob),'afterSha256':hashlib.sha256(blob).hexdigest(),'restoredExactHeadBlob':True,'jsonExactlyEqual':True})
p=r/'.gitattributes';s=p.read_text(encoding='utf8');stanza='\n# Preserve frozen Room schema export bytes across Windows checkouts.\napp/schemas/** -text\n';assert 'app/schemas/** -text' not in s;p.write_text(s+stanza,encoding='utf8')
assert not subprocess.check_output(['git','diff','--name-only','--','app/schemas']).strip()
(o/'schema-raw-restoration.json').write_text(json.dumps({'result':'PASS','scope':'Restore immutable committed export bytes; no schema/SQL/identity/version/assertion changes','schemaGitDiffEmpty':True,'files':results},indent=2)+'\n',encoding='utf8')
print(json.dumps(results,indent=2))
