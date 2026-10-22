"""Copy only named synthetic evidence returned by a successful M20 test."""
import hashlib,json,pathlib,re,stat,struct,subprocess,sys
from run_methods import ROOT,BASE,ADB,SERIAL,local_admission,write_json
group=sys.argv[1];assert group in ('gallery','platform')
local_admission()
result=BASE/'tests'/group
assert json.loads((result/'summary.json').read_text())['status']=='PASS'
raw=(result/'method-00/stdout.txt').read_bytes()
key='m20_'+group+'_evidence_root'
paths=re.findall(r'^INSTRUMENTATION_STATUS: '+key+r'=(\S+)\s*$',raw.decode(),re.M)
assert len(paths)==1
source=paths[0]
assert re.fullmatch(r'/data/(?:user/0|data)/com\.cooper\.lithic\.debug/no_backup/m20-'+group+r'-[a-f0-9-]{36}',source)
out=BASE/'screenshots'/group;out.mkdir(parents=True,exist_ok=False)
commands=[]
def run(args):
 cmd=[str(ADB),'-s',SERIAL,*args];r=subprocess.run(cmd,capture_output=True,timeout=30)
 commands.append(dict(command=cmd,exit_code=r.returncode,stdout_bytes=len(r.stdout),stdout_sha256=hashlib.sha256(r.stdout).hexdigest(),stderr_bytes=len(r.stderr),stderr_sha256=hashlib.sha256(r.stderr).hexdigest()))
 assert r.returncode==0 and not r.stderr,(cmd,r.returncode,r.stderr)
 return r.stdout
uid=json.loads((BASE/'device/installed-proof.json').read_text())['app']['uid']
def identity(path):
 b=run(['shell',f"run-as com.cooper.lithic.debug stat -c '%u %i %s %f %h %d' {path}"])
 parts=b.decode().split();assert len(parts)==6 and int(parts[0])==uid
 return parts
root=identity(source);assert stat.S_ISDIR(int(root[3],16))
names=(['01-preferences-and-actual-denial.png','02-permission-denial-explanation.png','03-local360-200-switches.png','04-local360-200-return.png','gallery-source.json'] if group=='gallery' else ['real-notification-tray.png','platform-result.json'])
files=[]
for name in names:
 path=source+'/'+name;before=identity(path)
 assert stat.S_ISREG(int(before[3],16)) and int(before[4])==1 and 0<int(before[2])<4*1024*1024
 data=run(['exec-out','run-as','com.cooper.lithic.debug','cat',path])
 after=identity(path);assert before==after and len(data)==int(before[2])
 device_sha=run(['shell','run-as','com.cooper.lithic.debug','sha256sum',path]).decode().split()[0]
 digest=hashlib.sha256(data).hexdigest();assert device_sha==digest
 item=dict(name=name,bytes=len(data),sha256=digest,source_identity=before)
 if name.endswith('.png'):
  assert data.startswith(b'\x89PNG\r\n\x1a\n');item['width'],item['height']=struct.unpack('>II',data[16:24])
 else:json.loads(data)
 (out/name).write_bytes(data);files.append(item)
assert identity(source)==root
write_json(out/'source.json',dict(serial=SERIAL,group=group,root=source,uid=uid,files=files,commands=commands))
print('PASS: copied',group,len(files),'owned stable evidence files')
