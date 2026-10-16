"""Build/run only the isolated supervisor diagnostic helper with explicit NDK.

Abort/SEGV here are deliberate helper processes, not JPEG decoder crashes.
"""
from pathlib import Path
import json, subprocess
from run_android import ROOT, BASE, EVIDENCE, REMOTE, push, checked, adb, classify, sha, save

def main():
    ndk = Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk/ndk/28.2.13676358')
    source = ROOT/'docs/research/m10-jpeg-r1/native/runtime_edges.c'
    binary = BASE/'runtime_edges'
    cmd = [str(ndk/'toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe'),
           '--target=x86_64-linux-android26',
           '--sysroot='+str(ndk/'toolchains/llvm/prebuilt/windows-x86_64/sysroot'),
           '-O2','-fPIE','-pie','-Wl,-z,max-page-size=16384',str(source),'-ldl','-o',str(binary)]
    p = subprocess.run(cmd,capture_output=True)
    save('runtime-edges-build.json',{'argv':cmd,'exitCode':p.returncode,'stdout':p.stdout.decode('utf-8','replace'),
                                   'stderr':p.stderr.decode('utf-8','replace'),'sourceSha256':sha(source)})
    if p.returncode: return 1
    uploads = [push(binary,'runtime_edges')]
    for abi in ('x86_64','arm64-v8a','armeabi-v7a','x86'):
        uploads.append(push(BASE/f'native/{abi}/probe/libstrictjpeg_probe.so',f'libprobe-{abi}.so'))
    checked(['shell','chmod 700 '+REMOTE+'/runtime_edges'])
    prefix = REMOTE+'/runtime_edges '
    records = []
    for label, text in (
        ('SHARED_LOAD_X86_64','--load '+REMOTE+'/libprobe-x86_64.so'),
        ('MISSING_LIBRARY','--load '+REMOTE+'/intentionally_absent.so'),
        ('WRONG_ABI_LIBRARY','--load '+REMOTE+'/libprobe-arm64-v8a.so'),
        ('MISSING_SYMBOL','--missing-symbol '+REMOTE+'/libprobe-x86_64.so'),
        ('SUPERVISOR_ABORT','--abort'),('SUPERVISOR_SEGV','--segv'),
        ('SHARED_INVOKE_HEALTHY','--invoke '+REMOTE+'/libprobe-x86_64.so --mode a2 '+REMOTE+'/complete.jpg'),
        ('SHARED_INVOKE_DAMAGED','--invoke '+REMOTE+'/libprobe-x86_64.so --mode a2 '+REMOTE+'/entropy_cut_eoi_retained.jpg'),
    ):
        result=adb(['shell',prefix+text],timeout=30)
        # Non-JSON helper errors retain literal exitCode/stderr and are NEVER
        # counted as normal image REJECT. Only actual shared invokes use JSON.
        if label.startswith('SHARED_INVOKE'): result=classify(result)
        records.append({'label':label,**result})
    save('runtime-edges.json',{'uploads':uploads,'cases':records,'helperBinarySha256':sha(binary),
        'scope':'API36 x86_64 dlopen/dlsym and CLI entry; JNI remains NOT_RUN; intentional crashes are helper diagnostics'})
    print(json.dumps([{'label':r['label'],'exitCode':r['exitCode']} for r in records]))
    return 0

if __name__=='__main__': raise SystemExit(main())
