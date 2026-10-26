from audit_helpers import *
def recorded_sdk(name,args):
 folder=OUT/name;m=json.loads((folder/'command.json').read_text(encoding='utf-8'))
 assert m['argv']==args and m['exitCode']==0
 b=(folder/'stdout').read_bytes();assert len(b)==m['stdoutBytes'] and hashlib.sha256(b).hexdigest()==m['stdoutSha256']
 return b.decode('utf-8',errors='strict')
assert json.loads((OUT/'trial-build-lint/command.json').read_text())['exitCode']==0
APK=ROOT/'app/build/private-trial/outputs/apk/debug/app-debug.apk'
assert APK.is_file() and APK.stat().st_size>0
sign=recorded_sdk('trial-signature',['cmd.exe','/d','/c',str(SDK/'apksigner.bat'),'verify','--verbose','--print-certs',str(APK.relative_to(ROOT))])
assert re.search(r'^Verifies\s*$',sign,re.M) and re.search(r'^Number of signers: 1\s*$',sign,re.M)
assert re.findall(r'^Signer #\d+ certificate SHA-256 digest: (\w+)',sign,re.M)==[CERT]
badging=recorded_sdk('trial-badging',[str(SDK/'aapt2.exe'),'dump','badging',str(APK.relative_to(ROOT))])
xmltree=recorded_sdk('trial-apk-manifest',[str(SDK/'aapt2.exe'),'dump','xmltree',str(APK.relative_to(ROOT)),'--file','AndroidManifest.xml'])
assert "package: name='com.cooper.lithic.preview.debug' versionCode='19' versionName='0.1.0-preview.3-debug'" in badging
assert "application-label:'砺石·体验版'" in badging and "minSdkVersion:'26'" in badging and "targetSdkVersion:'36'" in badging
assert 'com.cooper.lithic.app.LithicApplication' in xmltree and 'com.cooper.lithic.app.MainActivity' in xmltree
assert 'com.cooper.lithic.preview.debug.androidx-startup' in xmltree and 'android.permission.INTERNET' not in badging
merged=list((ROOT/'app/build/private-trial/intermediates/merged_manifests/debug').rglob('AndroidManifest.xml'));assert len(merged)==1
raw=merged[0].read_bytes();(OUT/'trial-merged-manifest.xml').write_bytes(raw)
manifest=ET.fromstring(raw);A='{http://schemas.android.com/apk/res/android}'
assert manifest.attrib['package']=='com.cooper.lithic.preview.debug' and manifest.attrib[A+'versionCode']=='19' and manifest.attrib[A+'versionName']=='0.1.0-preview.3-debug'
assert A+'sharedUserId' not in manifest.attrib and not manifest.findall('instrumentation')
app=manifest.find('application');assert app.attrib[A+'name']=='com.cooper.lithic.app.LithicApplication'
assert app.attrib[A+'label']=='砺石·体验版' and app.attrib[A+'debuggable']=='true' and app.attrib.get(A+'testOnly')!='true' and app.attrib[A+'allowBackup']=='false'
activities=[a.attrib[A+'name'] for a in app.findall('activity')];assert activities==['com.cooper.lithic.app.MainActivity'] and not app.findall('activity-alias')
launchers=[]
for activity in app.findall('activity'):
 for f in activity.findall('intent-filter'):
  if any(x.attrib.get(A+'name')=='android.intent.action.MAIN' for x in f.findall('action')) and any(x.attrib.get(A+'name')=='android.intent.category.LAUNCHER' for x in f.findall('category')):launchers.append(activity.attrib[A+'name'])
assert launchers==activities
providers=app.findall('provider');assert len(providers)==1 and providers[0].attrib[A+'authorities']=='com.cooper.lithic.preview.debug.androidx-startup' and providers[0].attrib[A+'exported']=='false'
assert not any(x.attrib.get(A+'name') in ['androidx.work.WorkManagerInitializer','androidx.emoji2.text.EmojiCompatInitializer'] for x in app.iter('meta-data'))
permissions=sorted({x.attrib[A+'name'] for tag in ['uses-permission','uses-permission-sdk-23'] for x in manifest.findall(tag)})
assert set(permissions)=={'android.permission.POST_NOTIFICATIONS','android.permission.WAKE_LOCK','android.permission.ACCESS_NETWORK_STATE','android.permission.RECEIVE_BOOT_COMPLETED','com.cooper.lithic.preview.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'}
classes=set();dexCount=0;dexBytes=b'';ownMethods=set()
with zipfile.ZipFile(APK) as z:
 assert len(z.namelist())==len(set(z.namelist())) and z.testzip() is None
 for name in z.namelist():
  if re.fullmatch(r'classes\d*\.dex',name):
   dexCount+=1;d=z.read(name);dexBytes+=d;assert d[:4]==b'dex\n' and struct.unpack_from('<I',d,32)[0]==len(d)
   ns,so=struct.unpack_from('<II',d,56);nt,to=struct.unpack_from('<II',d,64);nm,mo=struct.unpack_from('<II',d,88);nc,co=struct.unpack_from('<II',d,96)
   def string_at(idx):
    assert idx<ns;off=struct.unpack_from('<I',d,so+4*idx)[0]
    for _ in range(5):
     byte=d[off];off+=1
     if not byte&128:break
    return d[off:d.index(0,off)].decode('utf-8',errors='replace')
   def type_at(idx):
    assert idx<nt;return string_at(struct.unpack_from('<I',d,to+4*idx)[0])
   for i in range(nc):classes.add(type_at(struct.unpack_from('<I',d,co+32*i)[0]))
   for i in range(nm):
    ci,pi,ni=struct.unpack_from('<HHI',d,mo+8*i);owner=type_at(ci)
    if owner.startswith('Lcom/cooper/lithic/'):ownMethods.add(owner+'->'+string_at(ni))
 testassets=[n for n in z.namelist() if n.startswith('assets/') and ('schema' in n.lower() or 'LithicDatabase/' in n or n.endswith(('.jpg','.jpeg','.db','.sqlite','.html','.lithicbackup')))]
 assert not testassets,testassets
required={'app/LithicApplication','app/MainActivity','app/AppContainer','app/LithicNavigationKt','core/design/C2PageKt','core/design/C2RecordIdentityKt','core/design/LithicThemeKt','core/design/AmountTextKt','feature/home/HomeShellKt','feature/home/SearchScreenKt','feature/project/ProjectListScreenKt','feature/project/ProjectHistoryScreenKt','feature/project/ProjectDetailScreenKt','feature/project/ProjectEditorScreenKt','feature/project/MoneyEventEditorKt','feature/project/MoneyEventCorrectionScreenKt','feature/project/TradeInEditorScreenKt','feature/project/ReplacementComparisonScreenKt','feature/subscription/SubscriptionListScreenKt','feature/subscription/SubscriptionHistoryScreenKt','feature/subscription/SubscriptionDetailScreenKt','feature/subscription/SubscriptionEditorScreenKt','feature/subscription/RenewalEditorScreenKt','feature/subscription/SubscriptionStateDialogKt','feature/subscription/SubscriptionPriceChartKt','feature/review/ReviewShellKt','feature/review/LongTermReviewScreenKt','feature/review/AnnualReviewScreenKt','feature/review/TotalCostTrendScreenKt','feature/category/CategoryAnalysisScreenKt','feature/settings/SettingsShellKt','feature/settings/RestorePreviewScreenKt','feature/settings/BackupExportController','feature/settings/BackupRestoreController','feature/settings/InstalledAppInformationReaderKt','feature/settings/ProductInformationScreenKt'}
required={'Lcom/cooper/lithic/'+c+';' for c in required};assert required<=classes,required-classes
forbidden=sorted(c for c in classes if c.startswith(('Lorg/junit/','Ljunit/','Landroidx/test/','Landroidx/compose/ui/test/')) or (c.startswith('Lcom/cooper/lithic/') and c.split('$')[0].rstrip(';').endswith(('Test','Runner'))))
assert not forbidden,forbidden
for name in ['HomeRecordCounts','RecordCount']:
 assert 'Lcom/cooper/lithic/feature/home/HomeShellKt;->'+name in ownMethods
for text in ['home-record-counts','home-open-add','home-scroll-viewport','数量与成本口径','可丢弃数据','唯一副本']:
 assert text.encode('utf-8') in dexBytes,text
lint=ROOT/'app/build/private-trial/reports/lint-results-debug.xml';lr=ET.parse(lint).getroot();counts={s:sum(i.get('severity')==s for i in lr.findall('issue')) for s in ['Error','Fatal','Warning']};assert counts['Error']==counts['Fatal']==0;shutil.copyfile(lint,OUT/'trial-lint.xml')
pre=json.loads((OUT/'before-identities.json').read_text());debug=identity((OUT/'debug-quality/stdout').read_text(encoding='utf-8',errors='replace'));afterM22=identity((OUT/'after-m22-identity/stdout').read_text(encoding='utf-8',errors='replace'));trial=identity((OUT/'trial-build-lint/stdout').read_text(encoding='utf-8',errors='replace'))
assert pre['default']==debug and pre['m22']==afterM22
assert trial['variants']['debug']['applicationId']=='com.cooper.lithic.preview.debug' and trial['variants']['debug']['versionCode']==19 and trial['variants']['debug']['versionName']=='0.1.0-preview.3-debug'
for rel,old in json.loads((OUT/'before-artifacts.json').read_text()).items():
 if old['exists']:assert hashlib.sha256((ROOT/rel).read_bytes()).hexdigest()==old['sha256']
result={'status':'STATIC_DELIVERY_GATES_PASS_WITH_KNOWN_RISKS','sourceBase':state['source'],'apk':{'buildPath':str(APK.relative_to(ROOT)).replace('\\','/'),'bytes':APK.stat().st_size,'sha256':hashlib.sha256(APK.read_bytes()).hexdigest(),'applicationId':'com.cooper.lithic.preview.debug','versionCode':19,'versionName':'0.1.0-preview.3-debug','label':'砺石·体验版','minSdk':26,'targetSdk':36},'certificateSha256':CERT,'signatureVerified':True,'signerCount':1,'newSigningKeyGenerated':False,'application':app.attrib[A+'name'],'activities':activities,'launchers':launchers,'providerAuthorities':[p.attrib[A+'authorities'] for p in providers],'permissions':permissions,'noInstrumentationTestOnlySharedUID':True,'internet':False,'dex':{'files':dexCount,'definedClasses':len(classes),'requiredProductionClasses':sorted(required),'testFrameworkOrOwnTests':forbidden,'homeCountMethodsPresent':True,'homeFabViewportAndRiskStringsPresent':True,'testAssetsOrHTML':testassets},'privateLint':counts,'profiles':{'default':debug,'m22':afterM22,'privateTrial':trial},'oldLocalArtifactsUnchanged':True,'runtimeInstallationUpgradeVisual':'NOT_RUN','APP_VISUAL_PENDING':True,'MEDIA_DISPLAY_PENDING':True,'deviceOperations':0}
save('artifact-audit',result);print(json.dumps({'artifactGates':'PASS','apk':result['apk'],'dexClasses':len(classes),'lint':counts},ensure_ascii=False))
