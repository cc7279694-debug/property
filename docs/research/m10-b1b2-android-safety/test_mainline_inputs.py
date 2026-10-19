"""Mainline claim is a new authorization, never a retry of the archived UNKNOWN."""
import copy,importlib.util,json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock
HERE=Path(__file__).resolve().parent
SPEC=importlib.util.spec_from_file_location('mainline_stage_inputs',HERE/'stage_inputs.py');s=importlib.util.module_from_spec(SPEC);SPEC.loader.exec_module(s)
BOOT='11111111-1111-4111-8111-111111111111'
END=json.loads((HERE.parent/'m10-b1b2-android-resume/evidence/android/resume-ending-readonly.json').read_text(encoding='utf8'))
ROOT='41c0:10077:64512:1122';PARENT='41c0:10077:64512:3344'
PARENT_REPLY='\n'.join(['M10_INPUT_PARENT_BEGIN','ROOT_B|'+ROOT,'FILES_B|ABSENT','FILES_E|'+PARENT,'ROOT_E|'+ROOT,'M10_INPUT_PARENT_END|CREATED'])
class MainlineTransferTest(unittest.TestCase):
 def fake(self,temp):
  identity=copy.deepcopy(END['snapshot']['identity']);binding={key:identity[key] for key in ('sourceCommit','api','avd','serial','data','base','appSHA256')}
  return SimpleNamespace(evidence=Path(temp),binding=lambda:binding,scope=Mock(),serial='emulator-5664',adb=Mock(side_effect=[BOOT,'1',PARENT_REPLY])),identity
 def admission(self,fake,identity):
  rows=s.selected_inputs(s.locked_json(s.ARTIFACTS,s.ARTIFACTS_SHA))
  return s.mainline_admission(fake,identity,BOOT,'1',10077,10078,rows)
 def test_new_explicit_claim_does_not_touch_old_unknown(self):
  with tempfile.TemporaryDirectory() as temp:
   fake,identity=self.fake(temp);old=s.claim_transfer(fake);old_result=b'{"status":"UNKNOWN","files":[]}';(old/'result.json').write_bytes(old_result);old_attempt=(old/'attempt.json').read_bytes()
   new=s.claim_transfer(fake,mainline_admission=self.admission(fake,identity))
   self.assertEqual(s.MAINLINE_CLAIM,new.name);self.assertNotEqual(old,new);self.assertEqual(old_result,(old/'result.json').read_bytes());self.assertEqual(old_attempt,(old/'attempt.json').read_bytes())
   marker=json.loads((new/'attempt.json').read_text());self.assertEqual(s.MAINLINE_SOURCE,marker['mainlineSourceCommit']);self.assertEqual(s.TEST_SHA,marker['mainlineAdmission']['testSHA256']);self.assertEqual(3,len(marker['mainlineAdmission']['inputs']));fake.adb.assert_not_called()
 def test_second_new_claim_and_legacy_unknown_are_never_reused(self):
  with tempfile.TemporaryDirectory() as temp:
   fake,identity=self.fake(temp);proof=self.admission(fake,identity);new=s.claim_transfer(fake,mainline_admission=proof);before=(new/'attempt.json').read_bytes()
   with self.assertRaises(FileExistsError):s.claim_transfer(fake,mainline_admission=proof)
   (new/'result.json').write_text('{"status":"UNKNOWN"}')
   with self.assertRaises(RuntimeError):s.ensure_owned_input_parent(fake,10077,new)
   self.assertEqual(before,(new/'attempt.json').read_bytes());fake.adb.assert_not_called()
 def test_source_boot_uid_apk_fixture_and_runtime_conflicts_refuse_before_claim(self):
  changes=[lambda a:a.update(sourceCommit='0'*40),lambda a:a.update(bootId='unknown'),lambda a:a.update(testSHA256='0'*64),lambda a:a.update(testUID=10077),lambda a:a['inputs'][0].update(bytes=1),lambda a:a['identity'].update(serial='emulator-5624'),lambda a:a['identity']['runtimePaths'].update(status='UNKNOWN')]
  for change in changes:
   with tempfile.TemporaryDirectory() as temp:
    fake,identity=self.fake(temp);ad=self.admission(fake,identity);change(ad)
    with self.assertRaises((RuntimeError,ValueError)):s.claim_transfer(fake,mainline_admission=ad)
    self.assertEqual([],list(Path(temp).iterdir()));fake.adb.assert_not_called()
 def test_new_claim_reuses_owned_parent_check_only_after_actual_boot_match(self):
  with tempfile.TemporaryDirectory() as temp:
   fake,identity=self.fake(temp);new=s.claim_transfer(fake,mainline_admission=self.admission(fake,identity));result=s.ensure_owned_input_parent(fake,10077,new)
   self.assertEqual('CREATED',result['status']);self.assertEqual(3,fake.adb.call_count);self.assertIn('boot_id',str(fake.adb.call_args_list[0]));self.assertIn('boot_count',str(fake.adb.call_args_list[1]));self.assertIn('mkdir files',fake.adb.call_args_list[2].args[-1])
 def test_boot_change_stops_before_parent_write_and_wrong_claim_path_rejected(self):
  with tempfile.TemporaryDirectory() as temp:
   fake,identity=self.fake(temp);new=s.claim_transfer(fake,mainline_admission=self.admission(fake,identity));fake.adb.side_effect=['22222222-2222-4222-8222-222222222222']
   with self.assertRaises(RuntimeError):s.ensure_owned_input_parent(fake,10077,new)
   self.assertEqual(1,fake.adb.call_count)
   wrong=Path(temp)/'input-transfer-mainline-unapproved';wrong.mkdir();(wrong/'attempt.json').write_bytes((new/'attempt.json').read_bytes())
   with self.assertRaises(RuntimeError):s.ensure_owned_input_parent(fake,10077,wrong)
   self.assertEqual(1,fake.adb.call_count)
 def test_fixed_destination_and_three_complete_lengths_are_unchanged(self):
  self.assertEqual('files/m10-b2-test-inputs',s.DESTINATION);self.assertEqual((17163380,15458857,27445246),s.LOCKED_BYTES)
  with tempfile.TemporaryDirectory() as temp:
   fake,identity=self.fake(temp);ad=self.admission(fake,identity)
   self.assertEqual(s.MAINLINE_SOURCE,ad['sourceCommit']);self.assertEqual(s.APP_SHA,ad['appSHA256']);self.assertEqual(s.TEST_SHA,ad['testSHA256']);self.assertEqual(BOOT,ad['bootId']);self.assertEqual(identity,ad['identity'])
if __name__=='__main__':unittest.main()
