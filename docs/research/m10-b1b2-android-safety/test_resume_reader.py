"""Reader-only regressions: host execution and locked Android failure replay, never ADB."""
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
HERE=Path(__file__).resolve().parent
SAFETY=HERE.parent/'m10-b1b2-android-safety'
sys.path.insert(0,str(SAFETY))
import safety_device as s
P='no_backup/m10-b1b2-safety/11111111-1111-4111-8111-111111111111/result.json'
BASH=Path('C:/Program Files/Git/bin/bash.exe')

def bundle(raw=b'{}\n'):
    ancestors=sorted('/'.join(P.split('/')[:i]) for i in range(1,len(P.split('/'))))
    dm='41c0|1|10|4096|10001|2'; fm=f'8180|1|22|{len(raw)}|10001|1'; sha=hashlib.sha256(raw).hexdigest()
    return '\n'.join(['M10_BUNDLE_BEGIN']+['A0|'+a+'|'+dm for a in ancestors]+['B|'+P+'|'+fm,'H0|'+P+'|'+sha,base64.b64encode(raw).decode(),'H1|'+P+'|'+sha,'E|'+P+'|'+fm]+['A1|'+a+'|'+dm for a in ancestors]+['M10_BUNDLE_END|1'])

class ResumeReaderTest(unittest.TestCase):
 def test_real_head_failure_stays_rejected_and_hashes_locked(self):
  f=SAFETY/'host/api26-head-read-fixture'; row=json.loads((f/'input.json').read_text(encoding='utf8'))
  for name in ('stdout','stderr'):
   raw=(f/(name+'.raw')).read_bytes(); self.assertEqual(row[name+'SHA256'],hashlib.sha256(raw).hexdigest()); self.assertEqual(row[name+'Bytes'],len(raw))
  paths=[line.split('|')[1] for line in (f/'stdout.raw').read_text().splitlines() if line.startswith('B|')]
  with self.assertRaises((RuntimeError,ValueError)):s.parse_bundle((f/'stdout.raw').read_text(),paths,10077,{p:16384 for p in paths},required_hashes=True)
 def test_live_hashes_required_payload_and_pre_post_must_agree(self):
  good=bundle(); self.assertEqual('{}\n',s.parse_bundle(good,[P],10001,{P:1024},required_hashes=True)[P][0])
  sha=hashlib.sha256(b'{}\n').hexdigest()
  for bad in (good.replace('H0|'+P+'|'+sha,'H0|'+P+'|'+'0'*64),good.replace('H1|'+P+'|'+sha,'H1|'+P+'|'+'0'*64),good.replace('H0|'+P+'|'+sha+'\n',''),good+'\nEXTRA'):
   with self.assertRaises((RuntimeError,ValueError)):s.parse_bundle(bad,[P],10001,{P:1024},required_hashes=True)
 def test_live_identity_uid_and_end_unchanged(self):
  good=bundle()
  for bad in (good.replace('E|'+P+'|8180|1|22','E|'+P+'|8180|1|23'),good.replace('E|'+P+'|8180|1|22|3|10001','E|'+P+'|8180|1|22|3|10002'),good.rsplit('\n',1)[0]):
   with self.assertRaises((RuntimeError,ValueError)):s.parse_bundle(bad,[P],10001,{P:1024},required_hashes=True)
 def test_live_reader_uses_64k_not_bytewise_and_same_deadline(self):
  text=s.bundle_script([P],{P:2*1024*1024}); self.assertIn('bs=65536 count=33',text); self.assertNotIn('bs=1 ',text); self.assertNotIn('2>/dev/null',text);self.assertNotIn('status=none',text)
  self.assertIn('set -o pipefail',text); self.assertIn('H0|',text); self.assertIn('H1|',text)
 def test_stderr_requires_dd_counts_and_refuses_unknown_diagnostics(self):
  row='0+1 records in\n0+1 records out\n3 bytes copied, 0.00001 s, 300 kB/s\n'
  self.assertEqual(3,s.validate_dd_stderr((row*3).encode(),[3])['passes'])
  for raw in ((row*3+'head: not integer: c\n').encode(),(row*3).replace('records out','records unknown',1).encode(),(row*3).replace('3 bytes','4 bytes',1).encode(),b'',(row*2).encode()):
   with self.assertRaises((RuntimeError,ValueError)):s.validate_dd_stderr(raw,[3])
 def test_android_dd_transfer_format_is_scoped_counts_not_error_suppression(self):
  row='0+1 records in\n0+1 records out\n3 bytes transferred in 0.001 secs (3000 bytes/sec)\n'
  self.assertEqual(3,s.validate_dd_stderr((row*3).encode(),[3])['passes'])
  with self.assertRaises((RuntimeError,ValueError)):s.validate_dd_stderr((row*3).replace('3 bytes transferred','3 bytes transferred ERROR',1).encode(),[3])
 def journal_case(self, row_change=None, suffix=b''):
  from types import SimpleNamespace
  from unittest.mock import Mock
  raw=bundle().encode();script=s.bundle_script([P],{P:1024})
  argv=['C:/sdk/adb.exe','-s','emulator-5664','shell','run-as '+s.APP+' sh -c '+s.shlex.quote(script)]
  stderr=('0+1 records in\n0+1 records out\n3 bytes copied, 0.001 s, 3 kB/s\n'*3).encode()
  row={'argv':argv,'utc':'2026-10-19T00:00:00+00:00','endUtc':'2026-10-19T00:00:00.500000+00:00','exitCode':0}
  for key,value in (('stdout',raw),('stderr',stderr)):
   row.update({key+'Base64':base64.b64encode(value).decode(),key+'Bytes':len(value),key+'SHA256':hashlib.sha256(value).hexdigest()})
  if row_change:row_change(row)
  with tempfile.TemporaryDirectory() as temp:
   journal=Path(temp)/'commands.jsonl';journal.write_bytes(b'prefix\n')
   offset=journal.stat().st_size
   journal.write_bytes(journal.read_bytes()+json.dumps(row).encode()+b'\n'+suffix)
   return s.reader_command_proof(SimpleNamespace(serial='emulator-5664',journal=journal),offset,raw.decode().strip(),[P],[1024])
 def test_exact_command_journal_binds_raw_bytes_and_serial(self):
  self.assertIn('stdout',self.journal_case())
  for change in (lambda r:r['argv'].__setitem__(2,'emulator-5624'),lambda r:r.__setitem__('exitCode',1),lambda r:r.__setitem__('stdoutSHA256','0'*64),lambda r:r.__setitem__('stderrBytes',0),lambda r:r.__setitem__('endUtc','2026-10-19T00:00:31+00:00'),lambda r:r.__setitem__('utc','2026-10-19T00:00:00')):
   with self.assertRaises((RuntimeError,ValueError)):self.journal_case(change)
 def test_extra_or_truncated_command_journal_never_accepted(self):
  with self.assertRaises((RuntimeError,ValueError)):self.journal_case(suffix=b'{}\n')
 def test_actual_live_entry_never_accepts_legacy_no_hash_or_stderr_warning(self):
  from types import SimpleNamespace
  for mode in ('good','legacy','stderr_error','command_failure','truncated_end'):
   with tempfile.TemporaryDirectory() as temp:
    journal=Path(temp)/'commands.jsonl';journal.write_bytes(b'')
    client=SimpleNamespace(serial='emulator-5664',journal=journal)
    def adb(*args,timeout):
     self.assertEqual(30,timeout);self.assertEqual('shell',args[0]);self.assertEqual(2,len(args))
     text=bundle()
     if mode=='legacy':text='\n'.join(x for x in text.splitlines() if not x.startswith(('H0|','H1|')))
     if mode=='truncated_end':text=text.rsplit('\n',1)[0]
     err=('0+1 records in\n0+1 records out\n3 bytes copied, 0.001 s, 3 kB/s\n'*3).encode()
     if mode=='stderr_error':err+=b'base64: warning unknown status\n'
     row={'argv':['C:/sdk/adb.exe','-s',client.serial,*args],'utc':'2026-10-19T00:00:00+00:00','endUtc':'2026-10-19T00:00:00.5+00:00','exitCode':0 if mode!='command_failure' else 23}
     for key,raw in (('stdout',text.encode()),('stderr',err)):
      row.update({key+'Base64':base64.b64encode(raw).decode(),key+'Bytes':len(raw),key+'SHA256':hashlib.sha256(raw).hexdigest()})
     with journal.open('ab') as stream:stream.write(json.dumps(row).encode()+b'\n')
     return text
    client.adb=adb
    if mode=='good':self.assertEqual('{}\n',s.read_bundle(client,[P],10001,{P:1024})[P][0])
    else:
     with self.assertRaises((RuntimeError,ValueError)):s.read_bundle(client,[P],10001,{P:1024})
 def test_real_host_2mib_pipeline_finishes_within_30s(self):
  if not BASH.is_file(): self.skipTest('Existing Git Bash unavailable; Android remains NOT_RUN')
  raw=b'{"sample":"'+b'x'*(2*1024*1024-14)+b'"}\n'; self.assertEqual(2*1024*1024,len(raw))
  with tempfile.TemporaryDirectory() as temp:
   path=Path(temp)/P; path.parent.mkdir(parents=True); path.write_bytes(raw)
   start=time.monotonic(); result=subprocess.run([str(BASH),'--noprofile','--norc','-c',s.bundle_script([P],{P:len(raw)})],cwd=temp,capture_output=True,timeout=30)
   self.assertEqual(0,result.returncode,result.stderr); self.assertLess(time.monotonic()-start,30)
   uid=int(next(x for x in result.stdout.decode().splitlines() if x.startswith('B|')).split('|')[-2])
   parsed=s.parse_bundle(result.stdout.decode(),[P],uid,{P:len(raw)},required_hashes=True); self.assertEqual(hashlib.sha256(raw).hexdigest(),parsed[P][1]['sha256'])
   self.assertEqual(3,s.validate_dd_stderr(result.stderr,[len(raw)])['passes'])
 def test_real_host_dd_and_base64_fail_stop_without_end(self):
  if not BASH.is_file(): self.skipTest('Existing Git Bash unavailable')
  with tempfile.TemporaryDirectory() as temp:
   path=Path(temp)/P;path.parent.mkdir(parents=True);path.write_bytes(b'{}\n')
   for command in ('dd','base64','sha256sum'):
    script=command+"(){ printf 'HOST_SYNTHETIC_"+command+"_ERROR\\n' >&2; return 23; }; "+s.bundle_script([P],{P:1024})
    result=subprocess.run([str(BASH),'--noprofile','--norc','-c',script],cwd=temp,capture_output=True,timeout=30)
    self.assertEqual(23,result.returncode);self.assertNotIn(b'M10_BUNDLE_END',result.stdout);self.assertIn(b'HOST_SYNTHETIC_',result.stderr)
if __name__=='__main__': unittest.main()
