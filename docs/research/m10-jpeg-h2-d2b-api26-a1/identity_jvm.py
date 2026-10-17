"""Offline identity-gate RED/GREEN. Never invokes Android tools or devices.

RED compiles a deliberately documented legacy-semantics API adapter, then runs
four regression assertions. It is not the original Android source and does not
claim to reproduce Android scheduling. A compilation failure is never RED.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / 'docs/research/m10-jpeg-r3/host/probe/src'
PKG = 'com/cooper/lithic/m10r3/probe'
TEST = BASE / 'test/java' / PKG / 'ProcessIdentityGateTest.java'
SOURCE = BASE / 'main/java' / PKG / 'ProcessIdentityGate.java'
JDK = Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1')
CACHE = Path('C:/Users/CDD/.gradle/caches/modules-2/files-2.1')
JUNIT = next((CACHE / 'junit/junit/4.13.2').rglob('junit-4.13.2.jar'))
HAMCREST = next((CACHE / 'org.hamcrest/hamcrest-core/1.3').rglob('hamcrest-core-1.3.jar'))

LEGACY = r'''package com.cooper.lithic.m10r3.probe;
import java.io.*;
import java.util.*;
/** COMPILABLE RED ADAPTER ONLY: reproduces D2B role's known-name bypass and
 * UNKNOWN-main rejection. New APIs are adapters, not claims of old APIs. */
public final class ProcessIdentityGate {
 public enum State {PENDING,READY,FAILED}
 public interface NameReader {String read() throws Exception;}
 public interface NanoClock {long now() throws Exception;}
 private String name="UNKNOWN",failure; private State state=State.PENDING;
 public ProcessIdentityGate(){} ProcessIdentityGate(long timeoutMs){}
 private String known(String n){return "com.cooper.lithic.m10r3.probe".equals(n)?"HOST":"com.cooper.lithic.m10r3.probe:jpeg_worker".equals(n)?"WORKER":"REJECT";}
 public void frameworkHint(String n){name=n;}
 public String beginBackground(String n){name=n;return known(n);}
 public boolean completeBackground(){state=State.READY;return true;}
 public void fail(String why){failure=why;state=State.FAILED;}
 public State state(){return state;}
 public String failureReason(){return failure;}
 public String verifiedRole(){return state==State.READY?known(name):"REJECT";}
 public String role(String n,int api,boolean main,NameReader reader,NanoClock clock){return known(n);}
 public Map<String,Object> snapshot(){return new HashMap<>();}
 public static String readCmdline(InputStream in)throws IOException {byte[] b=new byte[256];int n=in.read(b),end=0;if(n<=0)throw new IOException("empty");while(end<n&&b[end]!=0)end++;if(end==n)throw new IOException("unbounded");return new String(b,0,end,java.nio.charset.StandardCharsets.UTF_8);}
}
'''

RUNNER = r'''import org.junit.runner.*;
import org.junit.runner.notification.*;
public final class IdentityTestRunner {
 public static void main(String[] args)throws Exception {
  Class<?> test=Class.forName("com.cooper.lithic.m10r3.probe.ProcessIdentityGateTest");
  JUnitCore core=new JUnitCore();core.addListener(new RunListener(){
   public void testFinished(Description d){System.out.println("METHOD "+d.getClassName()+"#"+d.getMethodName());}
  });
  int count=0,failed=0,ignored=0;
  String[] methods={"api26MainPendingProbeRecognizesWorkerWithoutReady","frameworkHintDoesNotAuthorizeEarlyBinder","failedBackgroundCannotBeBypassedByKnownClaim","exactProbeTimeBudgetRejects"};
  if(args[0].equals("red")){for(String m:methods){Result r=core.run(Request.method(test,m));count+=r.getRunCount();failed+=r.getFailureCount();ignored+=r.getIgnoreCount();for(Failure f:r.getFailures())System.out.print(f.getTrace());}}
  else{Result r=core.run(test);count=r.getRunCount();failed=r.getFailureCount();ignored=r.getIgnoreCount();for(Failure f:r.getFailures())System.out.print(f.getTrace());}
  System.out.println("TOTAL run="+count+" failed="+failed+" ignored="+ignored);
  System.exit(failed==0?0:1);
 }
}
'''

def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('phase',choices=['red','green'])
    parser.add_argument('--label')
    args=parser.parse_args()
    label=args.label or args.phase
    out=ROOT/'docs/research/m10-jpeg-h2-d2b-api26-a1/evidence'/('identity-'+label)
    out.mkdir(parents=True,exist_ok=False)
    classes=ROOT/'build/a1-identity-jvm'/str(time.time_ns())
    classes.mkdir(parents=True,exist_ok=False)
    runner=classes/'IdentityTestRunner.java'
    runner.write_text(RUNNER,encoding='utf-8')
    source=SOURCE
    if args.phase=='red':
        source=out/'ProcessIdentityGate.java'
        source.write_text(LEGACY,encoding='utf-8')
    cp=os.pathsep.join(map(str,[JUNIT,HAMCREST]))
    commands=[('javac',[str(JDK/'bin/javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(classes),str(source),str(TEST),str(runner)]),
              ('junit',[str(JDK/'bin/java.exe'),'-cp',str(classes)+os.pathsep+cp,'IdentityTestRunner',args.phase])]
    observations=[]
    for name,argv in commands:
        before=time.monotonic()
        result=subprocess.run(argv,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60)
        log=out/(name+'.log');log.write_bytes(result.stdout)
        observations.append({'name':name,'argv':argv,'exitCode':result.returncode,'elapsedSeconds':time.monotonic()-before,'bytes':len(result.stdout),'sha256':sha(log),'log':str(log.relative_to(ROOT))})
        print(result.stdout.decode('utf-8',errors='replace'))
        if name=='javac' and result.returncode:
            (out/'compile-failure.json').write_text(json.dumps(observations,indent=2)+'\n',encoding='utf-8')
            raise SystemExit(result.returncode)
    outcome={'kind':'HOST_JVM_ONLY','phase':args.phase,'redSeam':'Explicit legacy-semantics compilable adapter; not original source or Android execution' if args.phase=='red' else None,
             'inputs':[{'path':str(p.relative_to(ROOT)),'bytes':p.stat().st_size,'sha256':sha(p)} for p in [source,TEST]],'commands':observations}
    (out/'result.json').write_text(json.dumps(outcome,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    if args.phase=='red':
        raise SystemExit(0 if observations[-1]['exitCode']==1 else 2)
    raise SystemExit(observations[-1]['exitCode'])

if __name__=='__main__':main()
