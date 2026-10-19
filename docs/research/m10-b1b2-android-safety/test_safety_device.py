"""Pure source/fixture checks only; imports never invoke Android."""
import importlib.util
from pathlib import Path
import sys
import unittest
from unittest.mock import Mock
HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE))
import safety_device as s
class SafetyDeviceTest(unittest.TestCase):
 def test_only_fixed_seven_methods_no_old_fault_or_traversal(self):
  self.assertEqual(7,len(s.METHODS));self.assertEqual(7,len(set(s.METHODS)))
  for value in (-1,7,99,True):
   with self.assertRaises((ValueError,RuntimeError)):s.method_plan(value,'11111111-1111-4111-8111-111111111111',26)
  self.assertNotIn('Outsider',' '.join(s.METHODS));self.assertNotIn('SIGKILL',' '.join(s.METHODS))
 def test_exact_method_and_opt_in_parameters_are_assembled_once(self):
  p=s.method_plan(0,'11111111-1111-4111-8111-111111111111',26)
  self.assertEqual(s.METHODS[0],p['method']);self.assertEqual(1,p['argv'].count('instrument'))
  for value in ('m10SafetyPermit','m10SafetyRunId','m10SafetyExpectedApi'):self.assertEqual(1,p['argv'].count(value))
  for value in ('action','command','case','fault','--no-hidden-api-checks'):self.assertNotIn(value,p['argv'])
 def test_private_content_whitelist_excludes_images_and_business(self):
  good='no_backup/m10-b1b2-safety/11111111-1111-4111-8111-111111111111/result.json'
  self.assertEqual(good,s.private_path(good,content=True))
  for value in ('databases/app.db','no_backup/generation/media/x.jpg','no_backup/m10-resource-v2-11111111-1111-4111-8111-111111111111/media/.m10-jpeg-v2/11111111-1111-4111-8111-111111111111/source.bin','no_backup/../a','no_backup/m10-b1b2-safety/x/result.json'):
   with self.assertRaises((ValueError,RuntimeError)):s.private_path(value,content=True)
 def test_unknown_diagnostic_duplicate_or_hook_error_is_not_filtered_success(self):
  diagnostic='10-19 12:00:00.000 1 1 I LITHIC_M10_CAPTURE: M10_CAPTURE_READY nonce='+'a'*32+'\n'
  self.assertEqual('',s.scoped_lines(diagnostic,diagnostic,(),()))
  for raw in (diagnostic+diagnostic,diagnostic+'10-19 12:00:00.001 1 1 E LITHIC_JPEG: HOOK_UNKNOWN\n','junk\n'+diagnostic):
   with self.assertRaises((ValueError,RuntimeError)):s.scoped_lines(raw,diagnostic,(),())
 def test_prior_failed_or_different_apk_blocks_without_new_command(self):
  good={'status':'PASS','index':0,'method':s.METHODS[0],'api':26,'serial':'emulator-5664','appSHA256':s.APP_SHA,'testSHA256':'a'*64}
  self.assertTrue(s.prior_gate(good,0,26,'emulator-5664','a'*64))
  for key,value in [('status','UNKNOWN'),('index',1),('serial','emulator-5624'),('testSHA256','b'*64)]:
   bad=dict(good);bad[key]=value
   with self.assertRaises((ValueError,RuntimeError)):s.prior_gate(bad,0,26,'emulator-5664','a'*64)
 def test_api26_and_api36_only_uuid_required(self):
  for api,rid in ((27,'11111111-1111-4111-8111-111111111111'),(26,'../bad')):
   with self.assertRaises((ValueError,RuntimeError)):s.method_plan(0,rid,api)

 def test_bound_bundle_consumes_exact_bytes_and_end_marker(self):
  path='no_backup/m10-b1b2-safety/11111111-1111-4111-8111-111111111111/result.json'
  ancestors=sorted('/'.join(path.split('/')[:i]) for i in range(1,len(path.split('/'))))
  dm='41c0|1|10|4096|10001|2';fm='8180|1|22|3|10001|1'
  lines=['M10_BUNDLE_BEGIN']+['A0|'+a+'|'+dm for a in ancestors]+['B|'+path+'|'+fm,'e31cbi==','E|'+path+'|'+fm]+['A1|'+a+'|'+dm for a in ancestors]+['M10_BUNDLE_END|1']
  # actual synthetic bytes are {} followed by newline, not the literal backslash-n.
  import base64
  lines[1+len(ancestors)+1]=base64.b64encode(b'{}\n').decode()
  raw='\n'.join(lines)
  value=s.parse_bundle(raw,[path],10001,{path:1024})
  self.assertEqual('{}\n',value[path][0])
  with self.assertRaises((ValueError,RuntimeError)):s.parse_bundle(raw.rsplit('\n',1)[0],[path],10001,{path:1024})
  with self.assertRaises((ValueError,RuntimeError)):s.parse_bundle(raw.replace('E|'+path+'|'+fm,'E|'+path+'|8180|1|23|3|10001|1'),[path],10001,{path:1024})
 def test_cancel_initial_unknown_plus_complete_late_is_not_duplicate_result(self):
  observed={'workerPid':22,'workerUid':10001,'workerStartTicks':333}
  row={'origin':'INITIAL','ordinal':1,'outcome':'RESULT_UNKNOWN','detail':'INITIAL_PENDING','status':'OBSERVED_NOT_CLEANUP_AUTHORITY','resultCount':0,'sourceFdClosed':False,'outputFdClosed':False,'workerPid':22,'workerUid':10001,'workerStartTicks':333}
  for key in ('hostFdsClosed','rpcQuiescent','originalSessionQuiescent','binderDead','matchingProcessDead','guardReleased','connectionCleaned'):row[key]=False
  late={**row,'origin':'LATE_ORIGINAL_QUIESCENCE','ordinal':2,'outcome':'CANCELLED','detail':'LATE_FAILED_SESSION_QUIESCENCE'}
  for key in ('hostFdsClosed','rpcQuiescent','originalSessionQuiescent','binderDead','matchingProcessDead','guardReleased','connectionCleaned'):late[key]=True
  self.assertEqual(1,s.cancellation_observations([row,late],observed)['completeClosureClaims'])
  for bad in ([row],[late,late],[{**late,'ordinal':1},{**late,'ordinal':2}], [row,{**late,'sourceFdClosed':True}]):
   with self.assertRaises((ValueError,RuntimeError)):s.cancellation_observations(bad,observed)
 def test_cancel_revoke_phases_require_real_identity_and_partial_order(self):
  observed={'attempt':'a','session':'s','hostPid':11,'hostUid':10001,'workerPid':22,'workerUid':10001,'workerStartTicks':333}
  phases=['WORKER_REVOKE_ARMED','HOST_REVOKE_ACKNOWLEDGED','HOST_UNBIND_RETURNED','WORKER_ON_UNBIND','WORKER_EXIT_AUTHENTICATED_DETACH','HOST_BINDER_DIED']
  raw='\n'.join('10-19 12:00:00.000 '+str(11 if p.startswith('HOST_') else 22)+' 1 I LITHIC_JPEG: '+p+' attempt=a session=s worker=22/10001/333 at='+str(i+1) for i,p in enumerate(phases))
  self.assertEqual(6,len(s.cancellation_phases(raw,observed)))
  for bad in (raw.replace('worker=22/10001/333','worker=23/10001/333',1),raw.rsplit('\n',1)[0],raw+'\n'+raw.splitlines()[0]):
   with self.assertRaises((ValueError,RuntimeError)):s.cancellation_phases(bad,observed)
 def test_payload_residual_blocks_terminal_without_content_read(self):
  with self.assertRaises((ValueError,RuntimeError)):s.terminal_stage({'remainingPayloadMetadata':{'source.bin':{'bytes':8192}},'records':{}})


 def historical_healthy(self):
  import json
  fixture=HERE.parent/'m10-android-test-compat/evidence/m10-final-20261019-022830-2a602443/api26/method-00/after.json'
  evidence=json.loads(fixture.read_text(encoding='utf-8'))
  observed=s.life.observations_pair(evidence['newObservations'],'SUCCESS')
  stage=next(iter(evidence['newStages'].values()))
  s.life.validate_stage(stage['records'],observed,'SUCCESS')
  return evidence,observed,{**stage,'remainingPayloadMetadata':{}}
 def test_replays_actual_archived_healthy_stage_without_device(self):
  _,observed,stage=self.historical_healthy()
  result=s.terminal_stage(stage,s.life.SOURCES['SUCCESS'],observed)
  self.assertEqual(observed['attempt'],result['owner']['attemptId'])
 def test_synthetic_supervisor_schema_on_real_healthy_fixture_is_not_history(self):
  import copy,json
  evidence,observed,stage=self.historical_healthy();stage=copy.deepcopy(stage)
  owner_hash=s.life.digest(stage['records']['owner.v2'])
  # Explicit HOST-SYNTHETIC schema mutation; never written back into historical evidence.
  values={'ownerHash':owner_hash,'bootId':'6c697468-6963-4000-8000-000000000001','pid':str(observed['hostPid']),'uid':str(observed['hostUid']),'startTicks':'123'}
  raw=''.join(k+'='+values[k]+'\n' for k in sorted(values));stage['records']['supervisor.v2']=raw
  metadata=json.loads(stage['records']['__metadata__']);metadata['entries']['supervisor.v2']={'type':'regular','device':1,'inode':99,'bytes':len(raw),'sha256':s.life.digest(raw)}
  stage['records']['__metadata__']=json.dumps(metadata)
  self.assertEqual(observed['attempt'],s.terminal_stage(stage,s.life.SOURCES['SUCCESS'],observed)['owner']['attemptId'])
  stage['records']['supervisor.v2']=raw.replace('uid='+str(observed['hostUid']),'uid=99999')
  with self.assertRaises((ValueError,RuntimeError)):s.terminal_stage(stage,s.life.SOURCES['SUCCESS'],observed)
 def test_actual_archived_six_phase_log_replays_without_new_android_claim(self):
  evidence,observed,_=self.historical_healthy()
  self.assertEqual(6,len(s.life.validate_phases(evidence['newPhaseLog'],observed)))
 def test_b2_error_tag_is_unknown_not_a_legacy_command_failure(self):
  diagnostic='10-19 12:00:00.000 1 1 I LITHIC_M10_CAPTURE: M10_CAPTURE_READY nonce='+'a'*32+'\n'
  with self.assertRaisesRegex(RuntimeError,'Unknown/interrupted/hook-error'):
   s.scoped_lines(diagnostic+'10-19 12:00:00.001 2 2 E LITHIC_B2_RESOURCE: DIAGNOSTIC_PHASE_UNKNOWN:IOException\n',diagnostic,(),())

 def test_delayed_real_return_requires_initial_incomplete_then_exact_late_closure(self):
  observed={'workerPid':22,'workerUid':10001,'workerStartTicks':333}
  first={'origin':'INITIAL','outcome':'TIMEOUT','detail':'PHASE_TIMEOUT:TimeoutException','status':'OBSERVED_NOT_CLEANUP_AUTHORITY','resultCount':1,'sourceFdClosed':True,'outputFdClosed':True,**observed,'unbindCallsWhileReturnHeld':0,'durableRevokeConfirmed':True}
  for key in ('hostFdsClosed','rpcQuiescent','originalSessionQuiescent','binderDead','matchingProcessDead','guardReleased','connectionCleaned'):first[key]=False
  first['hostFdsClosed']=True
  late={**first,'origin':'LATE_ORIGINAL_QUIESCENCE','detail':'LATE_FAILED_SESSION_QUIESCENCE'}
  for key in ('hostFdsClosed','rpcQuiescent','originalSessionQuiescent','binderDead','matchingProcessDead','guardReleased','connectionCleaned'):late[key]=True
  checkpoint={**observed,'realReturnElapsedMs':1234}
  for key in ('actualKernelTransactAlreadyReturned','realReturnAccepted','clientJavaReturnStillHeld','guardActuallyHeld','activeCleanupRejected','sourceIdentityPreserved'):checkpoint[key]=True
  self.assertTrue(s.delayed_return_observations(first,late,checkpoint,observed)['onlyTestJavaReturnDelayed'])
  for a,b,c in ((first,{**late,'resultCount':0},checkpoint),({**first,'rpcQuiescent':True},late,checkpoint),(first,late,{**checkpoint,'actualKernelTransactAlreadyReturned':False}),(first,{**late,'workerPid':23},checkpoint),(first,{**late,'connectionCleaned':False},checkpoint)):
   with self.assertRaises((ValueError,RuntimeError)):s.delayed_return_observations(a,b,c,observed)

 def test_delayed_return_actual_fail_closed_detach_is_not_auth_ack(self):
  observed={'attempt':'a','session':'s','hostPid':11,'hostUid':10001,'workerPid':22,'workerUid':10001,'workerStartTicks':333}
  def raw(phases):
   return '\n'.join('10-19 12:00:00.000 '+str(11 if p.startswith('HOST_') else 22)+' 1 I LITHIC_JPEG: '+p+' attempt=a session=s worker=22/10001/333 at='+str(i+1) for i,p in enumerate(phases))
  initial=['HOST_UNBIND_RETURNED','WORKER_ON_UNBIND','UNPREPARED_DETACH','WORKER_EXIT_UNPREPARED_DETACH','HOST_BINDER_DIED']
  self.assertEqual('WORKER_EXIT_UNPREPARED_DETACH',s.delayed_return_phases(raw(initial),observed)['branch'])
  rejected=['RESULT_REJECTED','HOST_UNBIND_RETURNED','WORKER_ON_UNBIND','WORKER_EXIT_AUTHENTICATED_DETACH','HOST_BINDER_DIED']
  self.assertEqual('RESULT_REJECTED_THEN_DETACH_NOT_HOST_ACK',s.delayed_return_phases(raw(rejected),observed)['branch'])
  for bad in (initial[:-1],initial+['HOST_REVOKE_ACKNOWLEDGED'],['HOST_UNBIND_RETURNED','WORKER_ON_UNBIND','WORKER_EXIT_AUTHENTICATED_DETACH','HOST_BINDER_DIED'],initial+['HOST_BINDER_DIED']):
   with self.assertRaises((ValueError,RuntimeError)):s.delayed_return_phases(raw(bad),observed)

 def test_delayed_scope_paths_are_explicit_and_fault_methods_are_separate(self):
  p='no_backup/m10-b1-delay-11111111-1111-4111-8111-111111111111/media/.m10-jpeg-v2/11111111-1111-4111-8111-111111111111/abandoned.v2'
  self.assertEqual(p,s.private_path(p,content=True))
  self.assertEqual(11,len(s.REFLECTED_CLASSES))
  self.assertIn('delayed_real_delivery_return',s.method_plan(6,'11111111-1111-4111-8111-111111111111',26)['method'])
  self.assertNotIn('M10B1ExactProcessFaultTest',' '.join(s.METHODS))

 def test_current_method_red_originals_survive_and_cannot_be_overwritten(self):
  import tempfile
  from types import SimpleNamespace
  raw='{"status":"UNKNOWN","errors":["ACTUAL_MEASUREMENT_FAILED"]}\n'
  with tempfile.TemporaryDirectory() as temp:
   root=Path(temp);supervisor=SimpleNamespace(_resource_raw={'test-result.json':raw})
   s.persist_method_originals(supervisor,root)
   self.assertEqual(raw.encode(),(root/'resource-originals/test-result.json').read_bytes())
   with self.assertRaises(FileExistsError):s.persist_method_originals(supervisor,root)
   self.assertEqual(raw.encode(),(root/'resource-originals/test-result.json').read_bytes())

 def test_failure_originals_whitelist_never_accepts_business_or_old_bytes(self):
  import tempfile
  from types import SimpleNamespace
  with tempfile.TemporaryDirectory() as temp:
   root=Path(temp)
   with self.assertRaises((ValueError,RuntimeError)):
    s.persist_method_originals(SimpleNamespace(_resource_raw={'../old-ledger.db':'secret'}),root)
   self.assertEqual([],list(root.iterdir()))

 def test_actual_api26_head_failure_fixture_stays_rejected(self):
  import hashlib,json
  fixture=HERE/'host/api26-head-read-fixture';meta=json.loads((fixture/'input.json').read_text())
  raw=(fixture/'stdout.raw').read_bytes();err=(fixture/'stderr.raw').read_bytes()
  self.assertEqual(meta['stdoutSHA256'],hashlib.sha256(raw).hexdigest())
  self.assertEqual(meta['stderrSHA256'],hashlib.sha256(err).hexdigest())
  self.assertEqual(b'head: not integer: c\r\nhead: not integer: c\r\n',err)
  paths=sorted(line.split('|',2)[1] for line in raw.decode().splitlines() if line.startswith('B|'))
  self.assertEqual(2,len(paths));self.assertEqual(0,meta['exitCode'])
  with self.assertRaisesRegex(RuntimeError,'Record changed/truncated'):
   s.parse_bundle(raw.decode(),paths,10077,{p:16384 for p in paths})

 def test_bundle_script_requires_bounded_dd_and_pipefail_before_output(self):
  path='no_backup/m10-b1b2-safety/11111111-1111-4111-8111-111111111111/result.json'
  script=s.bundle_script([path],{path:16384})
  self.assertTrue(script.startswith('set -e; set -o pipefail; '))
  self.assertNotIn('head ',script)
  self.assertIn("dd if='"+path+"' bs=1 count=16385 | base64",script)
  for part in ("B|"+path+"|%f|%d|%i|%s|%u|%h","E|"+path+"|%f|%d|%i|%s|%u|%h","M10_BUNDLE_END|1"):
   self.assertIn(part,script)

 def test_host_posix_first_pipeline_failure_stops_without_complete_end(self):
  import os,subprocess,tempfile
  bash=Path('C:/Program Files/Git/bin/bash.exe')
  if not bash.is_file():self.skipTest('Existing Git Bash absent; Android shell remains NOT_RUN')
  path='no_backup/m10-b1b2-safety/11111111-1111-4111-8111-111111111111/result.json'
  script=s.bundle_script([path],{path:16384})
  with tempfile.TemporaryDirectory() as temp:
   target=Path(temp)/path;target.parent.mkdir(parents=True);target.write_bytes(b'{}\n')
   env={k:v for k,v in os.environ.items() if k not in ('BASH_ENV','ENV')}
   good=subprocess.run([str(bash),'--noprofile','--norc','-c',script],cwd=temp,env=env,capture_output=True,timeout=10)
   self.assertEqual(0,good.returncode,good.stderr)
   uid=int(next(line for line in good.stdout.decode().splitlines() if line.startswith('A0|')).rsplit('|',6)[5])
   self.assertEqual('{}\n',s.parse_bundle(good.stdout.decode(),[path],uid,{path:16384})[path][0])
   fail="dd(){ printf 'HOST_SYNTHETIC_DD_FAILURE\\n' >&2; return 23; }; "+script
   bad=subprocess.run([str(bash),'--noprofile','--norc','-c',fail],cwd=temp,env=env,capture_output=True,timeout=10)
   self.assertEqual(23,bad.returncode);self.assertIn(b'HOST_SYNTHETIC_DD_FAILURE',bad.stderr)
   self.assertNotIn(b'M10_BUNDLE_END',bad.stdout)

if __name__=='__main__':unittest.main()
