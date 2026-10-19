"""Prospective input-parent fix: synthetic host fixtures only, zero Android calls."""
import importlib.util
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock
SPEC=importlib.util.spec_from_file_location('prospective_stage_inputs',Path(__file__).with_name('stage_inputs.py'))
s=importlib.util.module_from_spec(SPEC);SPEC.loader.exec_module(s)
ROOT='41c0:10077:64512:1122';FILE='41c0:10077:64512:3344'
def output(mode='CREATED',root=ROOT,parent=FILE):
 return '\n'.join(['M10_INPUT_PARENT_BEGIN','ROOT_B|'+root,'FILES_B|'+(parent if mode=='EXISTING' else 'ABSENT'),'FILES_E|'+parent,'ROOT_E|'+root,'M10_INPUT_PARENT_END|'+mode])
class InputParentResumeTest(unittest.TestCase):
 def fake(self,directory,text=None):
  return SimpleNamespace(evidence=Path(directory),binding=lambda:{'serial':'emulator-5664'},scope=Mock(),adb=Mock(return_value=text or output()))
 def test_owned_absent_parent_is_created_after_exclusive_claim(self):
  with tempfile.TemporaryDirectory() as tmp:
   fake=self.fake(tmp);claim=s.claim_transfer(fake);v=s.ensure_owned_input_parent(fake,10077,claim)
   self.assertEqual('CREATED',v['status']);self.assertEqual(3344,v['parent']['inode']);self.assertEqual(1,fake.adb.call_count)
   args=fake.adb.call_args[0];self.assertEqual('shell',args[0]);self.assertIn('mkdir files',args[-1]);self.assertIn('umask 077',args[-1]);self.assertNotIn('mkdir -p',args[-1]);self.assertLess(args[-1].index("stat -c"),args[-1].index('mkdir files'));self.assertIn('10077',args[-1]);self.assertIn('/data/user/0/'+s.APP,args[-1])
 def test_owned_existing_parent_must_be_same_inode_and_not_rewritten(self):
  with tempfile.TemporaryDirectory() as tmp:
   fake=self.fake(tmp,output('EXISTING'));claim=s.claim_transfer(fake);v=s.ensure_owned_input_parent(fake,10077,claim)
   self.assertEqual('EXISTING',v['status']);self.assertNotIn('chmod',fake.adb.call_args[0][-1])
 def test_link_file_wrong_owner_or_device_is_fail_closed(self):
  for bad in ('a1ff:10077:64512:3344','8180:10077:64512:3344','41c0:10078:64512:3344','41c0:10077:22:3344'):
   with tempfile.TemporaryDirectory() as tmp:
    fake=self.fake(tmp,output(parent=bad));claim=s.claim_transfer(fake)
    with self.assertRaises((RuntimeError,ValueError)):s.ensure_owned_input_parent(fake,10077,claim)
    self.assertEqual(1,fake.adb.call_count)
 def test_race_partial_root_change_or_command_exception_never_retries(self):
  for text in (output().replace('ROOT_E|'+ROOT,'ROOT_E|41c0:10077:64512:1123'),output().rsplit('\n',1)[0],output('EXISTING').replace('FILES_E|'+FILE,'FILES_E|41c0:10077:64512:3345')):
   with tempfile.TemporaryDirectory() as tmp:
    fake=self.fake(tmp,text);claim=s.claim_transfer(fake)
    with self.assertRaises((RuntimeError,ValueError)):s.ensure_owned_input_parent(fake,10077,claim)
    self.assertEqual(1,fake.adb.call_count)
  with tempfile.TemporaryDirectory() as tmp:
   fake=self.fake(tmp);claim=s.claim_transfer(fake);fake.adb.side_effect=RuntimeError('exclusive mkdir raced; exit1')
   with self.assertRaisesRegex(RuntimeError,'raced'):s.ensure_owned_input_parent(fake,10077,claim)
   self.assertEqual(1,fake.adb.call_count)
 def test_missing_wrong_or_already_finished_claim_refuses_before_device(self):
  with tempfile.TemporaryDirectory() as tmp:
   fake=self.fake(tmp);claim=s.claim_transfer(fake);original=(claim/'attempt.json').read_bytes()
   (claim/'result.json').write_text('{"status":"UNKNOWN","files":[]}')
   with self.assertRaises(RuntimeError):s.ensure_owned_input_parent(fake,10077,claim)
   fake.adb.assert_not_called();self.assertEqual(original,(claim/'attempt.json').read_bytes())
   with self.assertRaises(FileExistsError):s.claim_transfer(fake)
if __name__=='__main__':unittest.main()
