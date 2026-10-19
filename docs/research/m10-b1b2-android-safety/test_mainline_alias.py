"""Host shell replay using real API26 alias metadata; never invokes Android."""
import importlib.util,json,shlex,subprocess,tempfile
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import Mock
HERE=Path(__file__).resolve().parent
SPEC=importlib.util.spec_from_file_location('alias_stage_inputs',HERE/'stage_inputs.py');s=importlib.util.module_from_spec(SPEC);SPEC.loader.exec_module(s)
FIX=HERE.parent/'m10-b1b2-mainline/evidence'
OBS=json.loads((FIX/'mainline-parent-alias-readonly.json').read_text(encoding='utf8'))
VALUES=dict(line.split('|',1) for line in OBS['raw'].splitlines() if '|' in line)
BASH=Path('C:/Program Files/Git/bin/bash.exe')
class MainlineRootAliasTest(unittest.TestCase):
 def prefix(self):
  with tempfile.TemporaryDirectory() as tmp:
   fake=SimpleNamespace(evidence=Path(tmp),binding=lambda:{'serial':'emulator-5664'},scope=Mock(),adb=Mock(return_value='HOST_ONLY_CAPTURE'))
   claim=s.claim_transfer(fake)
   with self.assertRaises(RuntimeError):s.ensure_owned_input_parent(fake,10077,claim)
   script=shlex.split(fake.adb.call_args.args[-1])[-1]
  return script.split("printf 'M10_INPUT_PARENT_BEGIN")[0]+"printf 'HOST_ONLY_PREFIX_REACHED\\n'"
 def run_prefix(self,script,pwd=None,user0=None,data=None):
  vals={'uid':VALUES['UID'],'pwd':pwd or VALUES['PWD'],'cwd':VALUES['CWD'],'user0':user0 or VALUES['USER0'],'data':data or VALUES['DATA']}
  stub='id(){ printf "%s\\n" '+shlex.quote(vals['uid'])+'; }; pwd(){ printf "%s\\n" '+shlex.quote(vals['pwd'])+'; }; '
  cases={'-c %u .':vals['uid'],'-c %f:%u:%d:%i .':vals['cwd'],'-c %f:%u:%d:%i /data/user/0/'+s.APP:vals['user0'],'-c %f:%u:%d:%i /data/data/'+s.APP:vals['data']}
  stub+='stat(){ case "$*" in '+''.join(shlex.quote(k)+') printf "%s\\n" '+shlex.quote(v)+' ;; ' for k,v in cases.items())+'*) printf "UNEXPECTED_HOST_STUB_CALL\\n" >&2; return 97 ;; esac; }; '
  return subprocess.run([str(BASH),'--noprofile','--norc','-c',stub+script],capture_output=True,timeout=5)
 def test_real_failed_prefix_still_replays_original_exit1_without_header(self):
  row=json.loads((FIX/'failed-parent-command.json').read_text(encoding='utf8'))
  if 'command' in row:row=row['command']
  old=shlex.split(row['argv'][-1])[-1].split("printf 'M10_INPUT_PARENT_BEGIN")[0]+"printf 'HOST_ONLY_PREFIX_REACHED\\n'"
  got=self.run_prefix(old);self.assertEqual((1,b'',b''),(got.returncode,got.stdout,got.stderr))
 def test_current_prefix_accepts_actual_data_data_alias_only_with_same_identity(self):
  self.assertEqual(VALUES['CWD'],VALUES['USER0']);self.assertEqual(VALUES['CWD'],VALUES['DATA'])
  got=self.run_prefix(self.prefix());self.assertEqual((0,b'HOST_ONLY_PREFIX_REACHED\n',b''),(got.returncode,got.stdout,got.stderr))
 def test_user0_alias_still_works_but_full_inode_uid_and_link_conflicts_reject(self):
  good=self.run_prefix(self.prefix(),pwd='/data/user/0/'+s.APP);self.assertEqual(0,good.returncode,good.stderr)
  for bad in ('41c0:10077:64512:115163','41c0:10078:64512:115162','a1ff:10077:64512:115162','41c0:10077:1:115162'):
   for which in ('user0','data'):
    result=self.run_prefix(self.prefix(),pwd='/data/user/0/'+s.APP,**{which:bad})
    self.assertEqual((1,b'',b''),(result.returncode,result.stdout,result.stderr),(which,bad))
 def test_unknown_foreign_or_nested_path_rejected_even_with_same_claimed_metadata(self):
  for bad in ('/data/user/10/'+s.APP,'/data/data/other.app','/data/data/'+s.APP+'/files','/data/data/'+s.APP+'/../'+s.APP):
   got=self.run_prefix(self.prefix(),pwd=bad);self.assertEqual((1,b'',b''),(got.returncode,got.stdout,got.stderr))
if __name__=='__main__':unittest.main()
