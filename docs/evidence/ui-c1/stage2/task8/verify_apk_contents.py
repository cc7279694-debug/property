from pathlib import Path
import zipfile,re,json,hashlib,subprocess
R=Path.cwd();O=R/'build/ui-c1-stage2';D=R/'docs/evidence/ui-c1/stage2/task8/apk'
a=json.loads((O/'final-apk.json').read_text(encoding='utf8'))
apk=Path(a['apk']);assert hashlib.sha256(apk.read_bytes()).hexdigest()==a['sha256']
types=[]
for prefix in ('app/src/test','app/src/androidTest'):
 for p in sorted((R/prefix).rglob('*.kt')):
  source=p.read_text(encoding='utf8');pack=re.search(r'(?m)^package\s+([\w.]+)',source)[1].replace('.','/')
  for m in re.finditer(r'(?m)^(?:(?:internal|private|public|data|sealed|enum|abstract|open|value|annotation|fun)\s+)*(?:class|object|interface)\s+(\w+)',source):
   types.append({'source':p.relative_to(R).as_posix(),'descriptorPrefix':'L'+pack+'/'+m[1]})
  types.append({'source':p.relative_to(R).as_posix(),'descriptorPrefix':'L'+pack+'/'+p.stem+'Kt'})
with zipfile.ZipFile(apk) as z:
 entries=z.namelist();dex=b''.join(z.read(n) for n in entries if n.endswith('.dex'))
 matches=[t for t in types if (t['descriptorPrefix']+';').encode() in dex or (t['descriptorPrefix']+'$').encode() in dex]
 assert not matches,matches
 fixture_names=('音乐年卡','云盘空间','阅读月卡','旧健身订阅','随身相机','旧通勤耳机','UIC1_DRAFT','ui-c1-stage2-isolated','fixture-source.json')
 assert all(n.encode('utf8') not in dex for n in fixture_names)
 test_assets=[p.relative_to(R/'app/src/androidTest/assets').as_posix() for p in (R/'app/src/androidTest/assets').rglob('*') if p.is_file()]
 assert not any(n=='assets/'+t for n in entries for t in test_assets)
 assert not any(n.endswith('/1.json') or n.endswith('/2.json') for n in entries)
testapk=R/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
assert hashlib.sha256(testapk.read_bytes()).hexdigest()==a['androidTestApkSha256']
resources=[]
with zipfile.ZipFile(testapk) as z:
 for p in sorted((R/'app/schemas').rglob('*.json')):
  rel=p.relative_to(R).as_posix();raw=z.read('assets/'+p.relative_to(R/'app/schemas').as_posix())
  original=subprocess.check_output(['git','cat-file','blob','f000639c62fce71be0a9240425c8e27b1a5ed974:'+rel])
  assert raw==p.read_bytes()==original
  resources.append({'path':rel,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest(),'packagedTestAssetEqualsFrozenGitBlob':True})
result={'result':'PASS','apkSha256':a['sha256'],'testApkSha256':a['androidTestApkSha256'],'sourceTestTypePrefixesChecked':types,'matches':matches,'syntheticDexStringsAbsent':fixture_names,'androidTestAssetPathsAbsentFromTarget':test_assets,'schemaExportsAbsentFromTargetApk':True,'testApkFrozenSchemaResources':resources,'scope':'Literal DEX type-prefix and string scan of the final artifacts plus ZIP assets; original AndroidX Debug ComponentActivity test manifest dependency is inherited, not a Release quality claim.'}
(D/'class-and-resource-inventory.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({'result':'PASS','typePrefixes':len(types),'schemas':len(resources)}))
