"""Complete a single exclusive synthetic fixture; preserve all earlier bytes.

No App or source-archive changes. Explicit compiler/static library already used
by the four-ABI build; a driver timeout is not a successful generation result.
"""
from pathlib import Path
import json, subprocess
from run_android import ROOT, BASE, EVIDENCE, REMOTE, push, checked, adb, sha, save, run

def main():
    ndk=Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk/ndk/28.2.13676358')
    source=ROOT/'docs/research/m10-jpeg-r1/native/generate_single_444.c'
    prefix=BASE/'native/x86_64/prefix'
    binary=BASE/'generate_single_444'
    cmd=[str(ndk/'toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe'),
         '--target=x86_64-linux-android26','--sysroot='+str(ndk/'toolchains/llvm/prebuilt/windows-x86_64/sysroot'),
         '-O3','-fPIE','-pie','-I'+str(prefix/'include'),str(source),str(prefix/'lib/libjpeg.a'),
         '-lm','-Wl,-z,max-page-size=16384','-o',str(binary)]
    p=subprocess.run(cmd,capture_output=True)
    save('single-444-build.json',{'argv':cmd,'exitCode':p.returncode,'stdout':p.stdout.decode('utf-8','replace'),
        'stderr':p.stderr.decode('utf-8','replace'),'sourceSha256':sha(source),
        'includedGeneratorSha256':sha(source.with_name('generate_fixtures.c')),
        'staticLibrarySha256':sha(prefix/'lib/libjpeg.a')})
    if p.returncode:return 1
    identity=push(binary,'generate_single_444')
    checked(['shell','chmod 700 '+REMOTE+'/generate_single_444; mkdir '+REMOTE+'/single444'])
    generation=adb(['shell',REMOTE+'/generate_single_444 '+REMOTE+'/single444'],timeout=480)
    save('single-444-generation.json',{'binary':identity,'generation':generation})
    if generation['exitCode'] != 0:return 1
    out=BASE/'single444'
    out.mkdir(exist_ok=False)
    pulled=checked(['pull',REMOTE+'/single444/.',str(out)])
    fixture=out/'valid_40mp_progressive_444.jpg'
    data=fixture.read_bytes()
    if data[-2:] != b'\xff\xd9':raise RuntimeError('Completed generator output lacks EOI')
    identity=push(fixture,fixture.name)
    first=BASE/'fixtures/large_40mp_progressive_444.jpg'
    original=first.read_bytes()
    info={'fixture':identity,'firstCatalogueFixtureSha256':sha(first),
          'firstBytesArePrefixOfCompletedFile':data[:len(original)]==original,
          'firstMissingEoi':original[-2:]!=b'\xff\xd9','pull':pulled,'cases':[]}
    save('single-444.json',info)
    for mode in ('a2','a1'):
        case={'mode':mode,'input':fixture.name,**run(mode,fixture.name,timeout=180)}
        info['cases'].append(case);save('single-444.json',info)
        print(json.dumps({'mode':mode,'status':case.get('supervisorStatus')}),flush=True)
    return 0

if __name__=='__main__':raise SystemExit(main())
