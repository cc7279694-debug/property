"""Read installed SDK image metadata only. Never downloads, creates AVDs or uses ADB."""
from pathlib import Path
import datetime,hashlib,json,os,subprocess,sys,xml.etree.ElementTree as ET

repo=Path(__file__).resolve().parents[3]
out=Path(__file__).parent/'evidence/sdk-inventory'
out.mkdir(parents=True,exist_ok=False)
def meta(path):
    data=path.read_bytes()
    if len(data)>1048576:raise ValueError('SDK metadata exceeds read bound')
    return data,{'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()}
source=repo/'docs/research/m10-jpeg-h2-d2b-api26-a1/evidence/build-runs/20261017T142151.230105Z/result.json'
a1=json.loads(source.read_text(encoding='utf-8'))
ps="$rows=@(); foreach($scope in @('Process','User','Machine')) { foreach($name in @('ANDROID_HOME','ANDROID_SDK_ROOT')) { $rows += [PSCustomObject]@{scope=$scope;name=$name;value=[Environment]::GetEnvironmentVariable($name,$scope)} } }; $rows | ConvertTo-Json -Depth 4"
argv=['powershell.exe','-NoProfile','-NonInteractive','-Command',ps]
started=datetime.datetime.now(datetime.timezone.utc).isoformat()
env=subprocess.run(argv,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=30)
(out/'environment.stdout.json').write_bytes(env.stdout)
(out/'environment.stderr.log').write_bytes(env.stderr)
assert env.returncode==0,env.returncode
environment=json.loads(env.stdout.decode('utf-8-sig'))
roots=[('A1 recorded SDK',a1['sdk']),('Default Android SDK','C:/Users/CDD/AppData/Local/Android/Sdk')]
roots += [(row['scope']+' '+row['name'],row['value']) for row in environment if row['value']]
rows=[];seen=set()
for origin,logical in roots:
    resolved=Path(logical).resolve()
    key=str(resolved).lower()
    if key in seen:continue
    seen.add(key);images=resolved/'system-images'
    row={'origin':origin,'configuredRoot':logical,'resolvedRoot':str(resolved),'exists':resolved.is_dir(),
         'systemImagesExists':images.is_dir(),'api26DirectoryExists':(images/'android-26').is_dir(),'packages':[]}
    if images.is_dir():
        for p in sorted(images.rglob('package.xml')):
            data,integrity=meta(p);tree=ET.fromstring(data)
            packages=[n.get('path') for n in tree.iter() if n.tag.split('}')[-1]=='localPackage']
            api=[n.text for n in tree.iter() if n.tag.split('}')[-1]=='api-level']
            props=p.parent/'source.properties'
            property_data,property_integrity=meta(props)
            row['packages'].append({'metadata':str(p),'integrity':integrity,'packagePaths':packages,'apiLevels':api,
                'sourceProperties':str(props),'sourcePropertiesIntegrity':property_integrity,
                'sourcePropertiesApiLines':[line for line in property_data.decode('utf-8').splitlines() if line.startswith(('AndroidVersion.ApiLevel=','AndroidVersion.ApiLevel =','Pkg.Path='))]})
    rows.append(row)
has26=any(r['api26DirectoryExists'] or any(any(a in ('26','26.0') for a in p['apiLevels']) or any(';android-26;' in a for a in p['packagePaths']) for p in r['packages']) for r in rows)
result={'status':'BLOCKED' if not has26 else 'SDK_CANDIDATE_PRESENT_REQUIRES_SEPARATE_CHECKS',
    'reason':'NO_INSTALLED_API26_IMAGE_IN_CONFIGURED_SDK_ROOTS' if not has26 else 'This read-only inventory does not itself authorize launch',
    'startedUtc':started,'endedUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'invocation':[sys.executable,'-B',str(Path(__file__).resolve())],'scriptSHA256':meta(Path(__file__))[1]['sha256'],
    'a1SdkSource':{'path':str(source.relative_to(repo)),**meta(source)[1]},
    'environmentQuery':{'argv':argv,'exitCode':env.returncode,'stdout':meta(out/'environment.stdout.json')[1],'stderr':meta(out/'environment.stderr.log')[1]},
    'environment':environment,'sdkRoots':rows,'api26ImagePresent':has26,
    'sdkmanagerInvoked':False,'imageDownloadedOrUpgraded':False,'avdCreatedOrModified':False,
    'adbInvoked':False,'appsInstalledOrLaunched':False,'oldH2StageAccessed':False}
(out/'result.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps(result,indent=2))
