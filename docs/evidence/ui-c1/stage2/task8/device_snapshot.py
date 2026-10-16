from pathlib import Path
import subprocess,sqlite3,json,sys,hashlib
R=Path.cwd();O=R/'build/ui-c1-stage2';A='C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe';P='com.cooper.lithic.debug';S='emulator-5596';phase=sys.argv[1]
def cmd(*a):return subprocess.run([A,'-s',S,*a],capture_output=True,check=True).stdout
def run(*a):return cmd(*a).decode('utf8','replace').strip()
assert run('get-state')=='device' and run('shell','getprop','ro.boot.qemu.avd_name')=='Lithic_M01_API_36'
assert not (O/('ledger-'+phase+'.json')).exists(),'Preserve existing snapshot, choose a new phase'
run('shell','am','force-stop',P);assert not subprocess.run([A,'-s',S,'shell','pidof',P],capture_output=True).stdout.strip()
pointer=json.loads(run('shell','run-as',P,'cat','no_backup/active-generation'));root='no_backup/generations/'+pointer['activeId']+'/'
dest=O/('ledger-'+phase);dest.mkdir(exist_ok=True);names=run('shell','run-as',P,'ls',root).splitlines()
for name in ('data.db','data.db-wal','data.db-shm'):
 if name in names:(dest/name).write_bytes(cmd('exec-out','run-as',P,'cat',root+name))
c=sqlite3.connect((dest/'data.db').as_uri()+'?mode=ro',uri=True);c.row_factory=sqlite3.Row
version=c.execute('pragma user_version').fetchone()[0];identity=c.execute('select identity_hash from room_master_table where id=42').fetchone()[0];integrity=c.execute('pragma integrity_check').fetchone()[0];fk=c.execute('pragma foreign_key_check').fetchall();assert version==2 and integrity=='ok' and not fk
tables=('categories','long_term_projects','project_money_events','local_media_assets','subscriptions','subscription_renewals','subscription_state_changes');facts={t:[dict(r) for r in c.execute('select * from '+t+' order by id')] for t in tables};c.close()
storage={'pointer':pointer,'ready':json.loads(run('shell','run-as',P,'cat',root+'ready.json')),'settingsSha256':run('shell','run-as',P,'sha256sum',root+'settings.preferences_pb').split()[0]}
if phase!='before':
 pre=json.loads((O/'ledger-before.json').read_text(encoding='utf8'));assert storage==pre['storage']
 for t in tables:
  current={row['id']:row for row in facts[t]}
  assert all(current.get(row['id'])==row for row in pre['facts'][t]),'Existing M09 fact changed in '+t
apk=run('shell','pm','path',P).split('package:')[1].splitlines()[0]
result={'phase':phase,'source':'readonly DB/WAL copies after controlled force-stop; ignored local files only','schemaVersion':version,'roomIdentity':identity,'facts':facts,'storage':storage,'integrityCheck':integrity,'foreignKeyCheck':[],'installedApkSha256':run('shell','sha256sum',apk).split()[0],'result':'PASS'}
(O/('ledger-'+phase+'.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({'phase':phase,'schemaVersion':version,'tableCounts':{t:len(v) for t,v in facts.items()},'installedApkSha256':result['installedApkSha256'],'integrity':integrity,'fk':[],'sameGenerationAndOldFacts':'PASS' if phase!='before' else 'BASELINE_CAPTURED'}))
