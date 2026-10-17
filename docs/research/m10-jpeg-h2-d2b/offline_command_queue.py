"""Compile/run only the isolated Java command-queue tests. No Android/device commands.

RED uses a documented compilable old scheduling-semantics witness, not an assertion
that the pre-D2B source already exposed this new helper's API.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[3]
PACKAGE = 'com.cooper.lithic.m10r3.probe'
BASE = ROOT / 'docs/research/m10-jpeg-r3/host/probe/src'
JAVA_HOME = Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1')
CACHE = Path('C:/Users/CDD/.gradle/caches/modules-2/files-2.1')
JUNIT = next((CACHE / 'junit/junit/4.13.2').rglob('junit-4.13.2.jar'))
HAMCREST = next((CACHE / 'org.hamcrest/hamcrest-core/1.3').rglob('hamcrest-core-1.3.jar'))
TEST = BASE / 'test/java/com/cooper/lithic/m10r3/probe/HostCommandQueueTest.java'
PRODUCTION = BASE / 'main/java/com/cooper/lithic/m10r3/probe/HostCommandQueue.java'

LEGACY = r'''package com.cooper.lithic.m10r3.probe;
/** RED witness for old Activity/Runner synchronous bootstrap and claim scheduling only.
 * Not the old production source, nor a claim of Android behavior. */
public final class HostCommandQueue implements AutoCloseable {
    public interface Initializer { void initialize() throws Exception; }
    public interface Claim { boolean claim() throws Exception; }
    public interface Task { void run() throws Exception; }
    public interface Observer { void onComplete(String c, String r, Outcome o, Throwable e) throws Exception; }
    public enum Readiness { BOOTSTRAPPING, READY, UNKNOWN, CLOSED }
    public enum Submission { ACCEPTED, INVALID, QUEUE_FULL, DUPLICATE, INITIALIZATION_UNKNOWN, CLOSED }
    public enum Outcome { EXECUTED, CLAIM_REPLAY, CLAIM_UNKNOWN, TASK_FAILED, INITIALIZATION_UNKNOWN }
    private Readiness ready=Readiness.BOOTSTRAPPING;
    private Throwable error, observerError;
    private long observerFailures;
    private final Observer observer;
    public HostCommandQueue(int capacity,Initializer init){this(capacity,init,null);}
    public HostCommandQueue(int capacity,Initializer init,Observer observer){
        if(capacity<=0)throw new IllegalArgumentException("capacity");
        java.util.Objects.requireNonNull(init,"init");this.observer=observer;
        try{init.initialize();ready=Readiness.READY;}catch(Throwable e){error=e;ready=Readiness.UNKNOWN;}
    }
    public Readiness readiness(){return ready;}
    public Throwable initializationError(){return error;}
    public Throwable observerError(){return observerError;}
    public long observerFailures(){return observerFailures;}
    public Submission submit(String c,String r,Claim claim,Task task){
        if(ready==Readiness.CLOSED)return Submission.CLOSED;
        if(ready==Readiness.UNKNOWN)return Submission.INITIALIZATION_UNKNOWN;
        Outcome o=Outcome.EXECUTED;Throwable failure=null;
        try{if(!claim.claim())o=Outcome.CLAIM_REPLAY;else task.run();}catch(Throwable e){o=Outcome.TASK_FAILED;failure=e;}
        if(observer!=null)try{observer.onComplete(c,r,o,failure);}catch(Throwable e){observerError=e;observerFailures++;}
        return Submission.ACCEPTED;
    }
    public void close(){ready=Readiness.CLOSED;}
}
'''

RUNNER = r'''import org.junit.runner.*;
import org.junit.runner.notification.Failure;
public final class QueueTestRunner {
    public static void main(String[] args) throws Exception {
        Class<?> test=Class.forName("com.cooper.lithic.m10r3.probe.HostCommandQueueTest");
        Request request=args.length>0&&args[0].equals("red")
            ?Request.method(test,"initializationRunsOffSubmittingThread"):Request.aClass(test);
        Result result=new JUnitCore().run(request);
        for(Failure failure:result.getFailures())System.out.println(failure.getTrace());
        System.out.println("run="+result.getRunCount()+" failed="+result.getFailureCount()+" ignored="+result.getIgnoreCount());
        System.exit(result.wasSuccessful()?0:1);
    }
}
'''

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('phase', choices=['red','green'])
    args=parser.parse_args()
    evidence=ROOT/'docs/research/m10-jpeg-h2-d2b/evidence'/('command-queue-'+args.phase)
    evidence.mkdir(parents=True,exist_ok=False)
    classes=ROOT/'build/d2b-command-queue'/str(time.time_ns())
    classes.mkdir(parents=True,exist_ok=False)
    runner=classes/'QueueTestRunner.java'
    runner.write_text(RUNNER,encoding='utf-8')
    if args.phase=='red':
        source=evidence/'HostCommandQueue.java'
        source.write_text(LEGACY,encoding='utf-8')
    else:
        source=PRODUCTION
    cp=os.pathsep.join(map(str,[JUNIT,HAMCREST]))
    commands=[]
    compile_cmd=[str(JAVA_HOME/'bin/javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(classes),str(source),str(TEST),str(runner)]
    run_cmd=[str(JAVA_HOME/'bin/java.exe'),'-cp',str(classes)+os.pathsep+cp,'QueueTestRunner',args.phase]
    for label,cmd in [('javac',compile_cmd),('junit',run_cmd)]:
        before=time.monotonic()
        result=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90)
        log=evidence/(label+'.log')
        log.write_bytes(result.stdout)
        commands.append({'label':label,'argv':cmd,'exitCode':result.returncode,'elapsedSeconds':time.monotonic()-before,'log':str(log.relative_to(ROOT)),'bytes':len(result.stdout),'sha256':sha(log)})
        print(result.stdout.decode('utf-8',errors='replace'))
        if label=='javac' and result.returncode:
            raise SystemExit(result.returncode)
    result={'phase':args.phase,'kind':'HOST_JVM_ONLY','redWitness':'compilable old synchronous scheduling semantics; not old source API' if args.phase=='red' else None,
            'inputs':[{'path':str(p.relative_to(ROOT)),'bytes':p.stat().st_size,'sha256':sha(p)} for p in [source,TEST]],'commands':commands}
    (evidence/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    if args.phase=='red':
        raise SystemExit(0 if commands[-1]['exitCode']==1 else 2)
    raise SystemExit(commands[-1]['exitCode'])

if __name__=='__main__':
    main()
