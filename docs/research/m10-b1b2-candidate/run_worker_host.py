"""Fresh direct javac/JUnit of Worker Java components only; never invokes Android or downloads."""
from pathlib import Path
import argparse, datetime, hashlib, json, os, re, subprocess

ROOT = Path(__file__).resolve().parents[3]
JDK = Path("C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1")
PROD = ROOT / "app/src/main/java/com/cooper/lithic/data/media/jpegworker"
TEST = ROOT / "app/src/test/java/com/cooper/lithic/data/media/jpegworker"
CACHE = Path("C:/Users/CDD/.gradle/caches/modules-2/files-2.1")
COMPONENTS = ("JpegAbandonedDelivery", "JpegBindingDuty", "JpegDecoderPolicy", "JpegResultWait",
    "JpegRetirementGate", "JpegRetirementHandshake", "JpegSessionFence")
EXISTING_TESTS = ("JpegAbandonedDeliveryTest", "JpegBindingDutyTest", "JpegDecoderPolicyTest",
    "JpegFailurePublicationTest", "JpegPublicationDeadlineTest", "JpegResultWaitTest",
    "JpegRetirementAcknowledgementTest", "JpegRetirementGateTest", "JpegRetirementHandshakeTest",
    "JpegRetirementOrderingTest", "JpegRpcBindingDutyTest", "JpegSessionFenceTest")

def sha(data): return hashlib.sha256(data).hexdigest()
def write(path, value):
    with path.open("x", encoding="utf-8") as stream: json.dump(value, stream, indent=2)
def jar(relative, expected):
    found = list((CACHE / relative).glob("*/*.jar"))
    if len(found) != 1 or sha(found[0].read_bytes()) != expected: raise RuntimeError("Locked cached dependency changed")
    return found[0]
def run(out, label, argv):
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    p = subprocess.run(list(map(str, argv)), cwd=ROOT, capture_output=True, timeout=90)
    for name, data in (("stdout",p.stdout),("stderr",p.stderr)):
        with (out / (label + "." + name + ".log")).open("xb") as f: f.write(data)
    write(out / (label + ".command.json"), {"argv":list(map(str,argv)),"exit":p.returncode,
        "utc":started,"stdoutBytes":len(p.stdout),"stdoutSHA256":sha(p.stdout),
        "stderrBytes":len(p.stderr),"stderrSHA256":sha(p.stderr)})
    return p
def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument("--phase",choices=("red","green"),required=True)
    parser.add_argument("--run-key",default="")
    args=parser.parse_args()
    if not re.fullmatch(r"[a-z0-9-]{0,40}",args.run_key): raise RuntimeError("Invalid exclusive host run identity")
    out=ROOT/"build/m10-b1b2-worker"/(args.phase+("-"+args.run_key if args.run_key else ""));out.mkdir(parents=True,exist_ok=False)
    classes=out/"classes";classes.mkdir()
    junit=jar("junit/junit/4.13.2","8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3")
    hamcrest=jar("org.hamcrest/hamcrest-core/1.3","66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9")
    sources=[PROD/(name+".java") for name in COMPONENTS]
    tests=[TEST/(name+".java") for name in EXISTING_TESTS+("JpegBindingContinuationTest",)]
    cp=os.pathsep.join(map(str,(junit,hamcrest)))
    inputs=[]
    for p in sources+tests+[Path(__file__),junit,hamcrest]:
        data=p.read_bytes();inputs.append({"path":str(p),"bytes":len(data),"sha256":sha(data)})
    write(out/"inputs.json",{"scope":"HOST_ONLY small exact input table","inputs":inputs})
    compiled=run(out,"compile",[JDK/"bin/javac.exe","--release","8","-encoding","UTF-8","-cp",cp,"-d",classes,*sources,*tests])
    if compiled.returncode: raise RuntimeError("javac failed; not a behavioral RED")
    result=run(out,"junit",[JDK/"bin/java.exe","-cp",str(classes)+os.pathsep+cp,"org.junit.runner.JUnitCore",*["com.cooper.lithic.data.media.jpegworker."+p.stem for p in tests]])
    text=result.stdout.decode("utf-8","replace");new=[p for p in tests if p.name=="JpegBindingContinuationTest.java"]
    old=[p for p in tests if p not in new];old_count=sum(p.read_text().count("@Test") for p in old);new_count=sum(p.read_text().count("@Test") for p in new)
    if args.phase=="red":
        if not result.returncode or "cleanup continuation" not in text: raise RuntimeError("Expected real missing-continuation assertion RED absent")
        status="EXPECTED_ASSERTION_RED"
    else:
        if result.returncode or f"OK ({old_count+new_count} tests)" not in text: raise RuntimeError("Actual JUnit GREEN/count mismatch")
        status="PASS_HOST_ONLY"
    write(out/"RESULTS.json",{"status":status,"phase":args.phase,"existingMethods":old_count,"newMethods":new_count,
        "totalUniqueMethods":old_count+new_count,"javaComponents":len(sources),"compileExit":compiled.returncode,"junitExit":result.returncode,
        "deviceCommands":0,"APKBuilds":0,"Android":"NOT_RUN","productFreeze":"NOT_GRANTED"})
    print(status,old_count,new_count,result.returncode)
if __name__=="__main__": main()
