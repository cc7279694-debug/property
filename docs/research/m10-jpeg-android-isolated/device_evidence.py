"""Read-only fixed-serial natural-test evidence adapter; no commands on import.

Only run-as of the locked candidate is used. Records/observations are restricted
to fresh synthetic namespaces. Guard contents, databases and image bytes are
never read. Full process output stays in device shell memory; only its hash,
length and numeric target-UID rows reach the supervisor's persisted journal.
"""
from __future__ import annotations

import base64
import hashlib
import json
from pathlib import Path
import re
import shlex
import stat

from lifecycle_evidence import (validate_lifecycle, validate_phases, validate_stage,
                                strict_json, EvidenceError)
from natural_tests import APP, APP_SHA, METHODS

UUID=r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
ROOT_RE=re.compile(r"m10-(worker-review|jpeg-test|jpeg-protection|v2-candidate)-"+UUID)
OBS_RE=re.compile(r"([a-z0-9_-]+)-("+UUID+r")\.json")
RECORDS={"owner.v2","source.v2","output.v2","pin.v2","delivered.v2","revoked.v2","terminal.v2","prepared.v2","cleanup-intent.v2","cleaned.v2"}

def need(condition, detail):
    if not condition:raise ValueError(detail)

def allowed(s, method):
    need((s.api,s.serial) in {(26,"emulator-5624"),(36,"emulator-5626")},"Unauthorized API/serial")
    need(method in METHODS,"Unknown natural method")

def relative(path):
    need(isinstance(path,str) and path.startswith("no_backup") and len(path)<=512,"Unsafe private relative path")
    parts=path.split("/")
    need(parts[0]=="no_backup" and all(re.fullmatch(r"[A-Za-z0-9._-]+",part) and part not in {".",".."} for part in parts),"Linked/escaping private path")
    return path

def private(s, script):
    # adb shell joins remote argv; quote the *whole* inner script, rather than
    # letting the outer remote shell reinterpret its pipelines/conditionals.
    return s.adb("shell","run-as "+APP+" sh -c "+shlex.quote(script),timeout=30)

def parse_stat(text):
    fields=text.strip().split("|")
    need(len(fields)==4 and re.fullmatch(r"[0-9a-f]+",fields[0]) and all(re.fullmatch(r"0|[1-9][0-9]*",value) for value in fields[1:]),"Incomplete stat output")
    mode=int(fields[0],16)
    need(not stat.S_ISLNK(mode) and (stat.S_ISREG(mode) or stat.S_ISDIR(mode)),"Linked or nonregular private path")
    return {"type":"directory" if stat.S_ISDIR(mode) else "regular","device":int(fields[1]),"inode":int(fields[2]),"bytes":int(fields[3])}

def metadata(s,path):
    path=relative(path)
    return parse_stat(private(s,f"stat -c '%f|%d|%i|%s' '{path}'"))

def directory(s,path):
    need(metadata(s,path)["type"]=="directory","Private directory is not a directory")
    text=private(s,f"ls -1A '{relative(path)}'")
    need(len(text.encode())<=65536,"Directory inventory exceeded bound")
    values=text.splitlines() if text else []
    need(len(values)<=128 and len(values)==len(set(values)) and all(re.fullmatch(r"[A-Za-z0-9._-]+",name) and name not in {".",".."} for name in values),"Unknown/truncated directory inventory")
    return sorted(values)

def exists(s,path):
    path=relative(path)
    answer=private(s,f"if [ -L '{path}' ]; then echo LINK; elif [ -e '{path}' ]; then echo PRESENT; else echo ABSENT; fi")
    need(answer in {"PRESENT","ABSENT"},"Private path linked or status unknown")
    return answer=="PRESENT"

def read_text(s,path):
    path=relative(path)
    pieces=path.split("/")
    observation=(len(pieces)==3 and pieces[1]=="m10-jpeg-observations" and OBS_RE.fullmatch(pieces[2]) is not None)
    stage=(len(pieces)==6 and ROOT_RE.fullmatch(pieces[1]) is not None and pieces[2:4]==["media",".m10-jpeg-v2"] and re.fullmatch(UUID,pieces[4]) is not None and pieces[5] in RECORDS)
    need(observation or stage,"Content read outside explicit synthetic observation/Stage records")
    before=metadata(s,path)
    need(before["type"]=="regular" and 0<before["bytes"]<=16384,"Invalid synthetic record size/type")
    encoded=private(s,f"base64 '{path}'")
    need(len(encoded)<=24576,"Record read exceeded bound")
    data=base64.b64decode("".join(encoded.splitlines()),validate=True)
    need(len(data)==before["bytes"] and metadata(s,path)==before,"Record inode/length changed while reading")
    return data.decode("utf-8"),{**before,"sha256":hashlib.sha256(data).hexdigest()}

def observations(s):
    root="no_backup/m10-jpeg-observations"
    if not exists(s,root):return {}
    values={}
    for name in directory(s,root):
        need(OBS_RE.fullmatch(name) is not None,"Unknown observation child")
        text,meta=read_text(s,root+"/"+name)
        value=strict_json(text)
        need(isinstance(value,dict),"Observation is not an object")
        values[name]={"filename":name,"name":OBS_RE.fullmatch(name).group(1),"value":value,"metadata":meta}
    return values

def stages(s):
    values={}
    if not exists(s,"no_backup"):
        return values
    for root in directory(s,"no_backup"):
        if not root.startswith("m10-") or root=="m10-jpeg-observations":continue
        match=ROOT_RE.fullmatch(root)
        need(match is not None,"Unknown synthetic namespace must be retained")
        need(match.group(1)=="worker-review","Unfinished non-review synthetic root remains; preserve "+root)
        path="no_backup/"+root
        need(directory(s,path)==["media"],"Unexpected review root child")
        need(directory(s,path+"/media")==[".m10-jpeg-v2"],"Unexpected review media child")
        namespace=path+"/media/.m10-jpeg-v2"
        children=directory(s,namespace)
        need(".namespace.guard" in children,"Namespace guard absent")
        guard=metadata(s,namespace+"/.namespace.guard")
        need(guard["type"]=="regular" and guard["bytes"]==0,"Namespace guard is not empty/regular")
        assets=[name for name in children if name!=".namespace.guard"]
        need(len(assets)==1 and re.fullmatch(UUID,assets[0]),"Unknown/duplicate Stage directory")
        stage=namespace+"/"+assets[0]
        child_names=directory(s,stage)
        need(set(child_names)<=RECORDS|{"consumer.guard"},"Unknown Stage child or retained payload; preserve it")
        records={};entries={}
        for name in child_names:
            if name=="consumer.guard":
                item=metadata(s,stage+"/"+name)
                need(item["type"]=="regular" and item["bytes"]==0,"Consumer guard is not empty/regular")
                records[name]=""
                entries[name]={**item,"sha256":hashlib.sha256(b"").hexdigest(),"sha256Origin":"DERIVED_FROM_VERIFIED_ZERO_LENGTH_NO_CONTENT_READ"}
            else:
                records[name],entries[name]=read_text(s,stage+"/"+name)
        records["__metadata__"]=json.dumps({"complete":True,"entries":entries},sort_keys=True)
        values[root]={"asset":assets[0],"namespaceGuard":guard,"records":records}
    return values

def process_script(uid):
    need(type(uid) is int and uid>0,"Numeric target UID required")
    # Marker prevents shell substitution removing ps's original final newlines.
    return ("raw=\"$(ps -A -o PID,UID,NAME; rc=$?; printf '__M10_PS_RC__=%s\\n' \"$rc\")\"; "
            "rc=\"${raw##*__M10_PS_RC__=}\"; [ \"$rc\" = 0 ] || exit 61; "
            "data=\"${raw%__M10_PS_RC__=*}\"; "
            "printf 'PS_COMPLETE=1\\nPS_BYTES='; printf '%s' \"$data\" | wc -c; "
            "printf 'PS_SHA256='; printf '%s' \"$data\" | sha256sum | cut -d ' ' -f 1; "
            "printf '%s' \"$data\" | { IFS=' ' read -r p u n rest; "
            "[ \"$p/$u/$n/$rest\" = 'PID/UID/NAME/' ] || exit 62; printf 'PID UID NAME\\n'; "
            f"while IFS=' ' read -r p u n rest; do if [ \"$u\" = '{uid}' ]; then "
            "[ -z \"$rest\" ] || exit 63; printf '%s %s %s\\n' \"$p\" \"$u\" \"$n\"; fi; done; } || exit 64; "
            "printf 'PS_END\\n'")

def parse_process_snapshot(text,uid):
    need(isinstance(text,str) and len(text)<=65536,"Process evidence missing/oversized")
    lines=text.splitlines()
    need(len(lines)>=5 and lines[0]=="PS_COMPLETE=1" and re.fullmatch(r"PS_BYTES=\s*[1-9][0-9]*",lines[1]) and re.fullmatch(r"PS_SHA256=[a-f0-9]{64}",lines[2]) and lines[3]=="PID UID NAME" and lines[-1]=="PS_END","Incomplete process status, hash or numeric UID header")
    rows=[];seen=set()
    for line in lines[4:-1]:
        fields=line.split()
        need(len(fields)==3 and re.fullmatch(r"[1-9][0-9]*",fields[0]) and fields[1]==str(uid) and re.fullmatch(r"[A-Za-z0-9._:-]+",fields[2]),"Unknown process identity or UID column")
        pid=int(fields[0]);need(pid not in seen,"Duplicate process row");seen.add(pid)
        rows.append({"pid":pid,"uid":uid,"name":fields[2]})
    return {"complete":True,"uid":uid,"rawBytes":int(lines[1].split("=",1)[1]),"rawSHA256":lines[2].split("=",1)[1],"rows":rows}

def proc_start(text,pid):
    need(isinstance(text,str) and 0<len(text)<=8192 and text.startswith(str(pid)+" ("),"Unexpected /proc process identity")
    index=text.rfind(")")
    need(index>0,"Malformed process comm")
    fields=text[index+1:].split()
    need(len(fields)>=20 and re.fullmatch(r"[1-9][0-9]*",fields[19]),"Truncated /proc startTicks")
    return int(fields[19])

def processes(s,uid):
    found=parse_process_snapshot(s.adb("shell","sh -c "+shlex.quote(process_script(uid)),timeout=30),uid)
    for row in found["rows"]:
        need(row["name"]==APP,"Active Worker or unknown same-UID process; preserve state")
        row["startTicks"]=proc_start(s.adb("shell","cat",f"/proc/{row['pid']}/stat",timeout=30),row["pid"])
    found["worker_absent"]=True
    return found

def worker_absence(s,pid,start):
    need(type(pid) is int and 0<pid<=2147483647 and type(start) is int and start>0,"Worker process proof identity invalid")
    value=s.adb("shell","sh -c "+shlex.quote(f"if [ -d /proc/{pid} ]; then echo PRESENT; else echo ABSENT; fi"),timeout=30)
    need(value=="ABSENT","Worker PID still present or absence unknown; no PID reuse guess")
    return {"pid":pid,"startTicks":start,"absent":True,"origin":"INDEPENDENT_TARGETED_PROC_DIRECTORY_CHECK"}

def logs(s):
    value=s.adb("logcat","-d","-v","threadtime","-s","LITHIC_JPEG:I","*:S",timeout=30)
    need(len(value.encode())<=2*1024*1024,"Lifecycle tag log exceeds complete evidence bound")
    return value

def new_log_lines(old,new):
    before_lines=[line for line in old.splitlines() if line and not line.startswith("--------- beginning of ")]
    after_lines=[line for line in new.splitlines() if line and not line.startswith("--------- beginning of ")]
    need(after_lines[:len(before_lines)]==before_lines,"Lifecycle log rotated or prior prefix changed")
    return "\n".join(after_lines[len(before_lines):])

def snapshot(s,method):
    allowed(s,method)
    uid=s.installed(APP,APP_SHA)
    need(type(uid) is int and uid>0,"Installed application UID unknown")
    need(private(s,"id -u")==str(uid),"run-as UID mismatch")
    no_backup=exists(s,"no_backup")
    return {"status":"PASS","method":method,"serial":s.serial,"api":s.api,"uid":uid,
            "noBackupStatus":"PRESENT" if no_backup else "ABSENT_FRESH_INSTALL_OBSERVATION",
            "processes":processes(s,uid),"stages":stages(s) if no_backup else {},
            "observations":observations(s) if no_backup else {},"phaseLog":logs(s)}

def before(s,method):
    result={"status":"BLOCKED","method":method,"serial":s.serial,"api":s.api,"errors":[]}
    try:
        result=snapshot(s,method)
        for root,stage in result["stages"].items():
            matching=[row["value"] for row in result["observations"].values() if row["name"]=="worker-review-terminal" and row["value"].get("generation")==root]
            need(len(matching)==1,"Old synthetic Stage has no unique terminal observation")
            observed=matching[0]
            validate_stage(stage["records"],observed,observed["outcome"])
            worker_absence(s,observed["workerPid"],observed["workerStartTicks"])
        return result
    except (ValueError,KeyError,OSError,RuntimeError,TypeError) as error:
        return {**result,"status":"BLOCKED","errors":[str(error)]}

def after(s,method,prior,*,capture=None):
    result={"status":"BLOCKED","method":method,"serial":s.serial,"api":s.api,"errors":[]}
    try:
        result=snapshot(s,method)
        need(prior.get("uid")==result["uid"] and prior.get("serial")==result["serial"] and prior.get("api")==result["api"],"After identity conflicts with before")
        if capture is None:
            need(not getattr(s,"continuous_capture_required",False),"Continuous capture missing; snapshot fallback forbidden")
            result["newPhaseLog"]=new_log_lines(prior["phaseLog"],result["phaseLog"])
        else:
            from continuous_evidence import consume_capture
            proof=capture()
            directory=Path(s.evidence)/f"method-{METHODS.index(method):02d}"/"continuous-log"
            scoped=consume_capture(proof,directory,prior["phaseLog"],expected_serial=s.serial,
                                   prior_diagnostics=getattr(s,"capture_prior_diagnostics",()))
            result["newPhaseLog"]=scoped["newPhaseLog"]
            result["continuousCapture"]=scoped
        need(all(result["stages"].get(key)==value for key,value in prior["stages"].items()),"Old synthetic Stage changed/disappeared")
        need(all(result["observations"].get(key)==value for key,value in prior["observations"].items()),"Old immutable observation changed/disappeared")
        result["newStages"]={key:value for key,value in result["stages"].items() if key not in prior["stages"]}
        result["newObservations"]=[value for key,value in result["observations"].items() if key not in prior["observations"]]
        workers=set()
        for line in result["newPhaseLog"].splitlines():
            match=re.search(r" worker=(\d+)/(\d+)/(\d+) at=",line)
            need(match is not None and int(match[2])==result["uid"],"Lifecycle process identity missing/conflicting")
            workers.add((int(match[1]),int(match[3])))
        result["workerProc"]={str(pid):worker_absence(s,pid,start) for pid,start in workers}
        instrument=Path(s.evidence)/f"method-{METHODS.index(method):02d}"/"instrumentation.txt"
        need(instrument.is_file() and instrument.stat().st_size<=1024*1024,"Complete instrumentation raw output unavailable")
        # Preserve the exact decoded wire bytes: text-mode universal newline handling
        # would rewrite genuine CRLF and invalidate the parser/raw evidence digest.
        result["instrumentationText"]=instrument.read_bytes().decode("utf-8", errors="strict")
        return result
    except (ValueError,KeyError,OSError,RuntimeError,TypeError) as error:
        return {**result,"status":"BLOCKED","errors":[str(error)]}

def validate_other_phases(text,uid,worker_proofs,expected_count):
    groups={}
    for line in text.splitlines():
        match=re.search(r"attempt=([^ ]+) session=([^ ]+) worker=(\d+)/(\d+)/(\d+) at=",line)
        need(match is not None,"Unparsed lifecycle line")
        attempt,session,pid,claimed_uid,start=match.groups()
        need(int(claimed_uid)==uid,"Lifecycle app UID conflict")
        groups.setdefault((attempt,session,int(pid),int(start)),[]).append(line)
    need(len(groups)==expected_count,"Unexpected lifecycle invocation count")
    for (attempt,session,pid,start),lines in groups.items():
        host_lines=[line for line in lines if "HOST_RETIRE_ACKNOWLEDGED " in line]
        need(len(host_lines)==1,"Unique Host acknowledgement absent")
        emitter=re.match(r"\d\d-\d\d\s+\S+\s+(\d+)\s+\d+\s+I",host_lines[0])
        need(emitter is not None and int(emitter[1])!=pid,"Host/Worker PID not independent")
        validate_phases("\n".join(lines),{"attempt":attempt,"session":session,"workerPid":pid,"workerUid":uid,"workerStartTicks":start,"hostPid":int(emitter[1])})
        proof=worker_proofs.get(str(pid),{})
        need(proof.get("pid")==pid and proof.get("startTicks")==start and proof.get("absent") is True,"Independent matching Worker absence unavailable")

def assess(method,prior,current,instrumentation):
    result={"status":"BLOCKED","errors":[],"evidence_levels":{}}
    try:
        need(method in METHODS and prior.get("status")=="PASS" and current.get("status")=="PASS" and instrumentation.get("status")=="PASS" and instrumentation.get("method")==method,"Required before/after/instrumentation evidence missing, mismatched or non-PASS")
        for key in ("serial","api","uid","method"):
            need(key in prior and key in current and prior[key]==current[key],"Before/after identity absent or conflicting")
        raw=current["instrumentationText"]
        need(hashlib.sha256(raw.encode()).hexdigest()==instrumentation.get("decodedSHA256"),"Raw instrumentation evidence does not match parsed result")
        observations=current["newObservations"]
        if METHODS.index(method)<2:
            need(len(current["newStages"])==1,"Exactly one new terminal review Stage required")
            stage=next(iter(current["newStages"].values()))
            matches=[row["value"] for row in observations if row["name"]=="worker-review-result"]
            need(len(matches)==1,"Unique Worker result observation unavailable")
            worker=matches[0]
            process={**current["processes"],"worker_proc":current["workerProc"].get(str(worker.get("workerPid")))}
            result=validate_lifecycle("SUCCESS" if METHODS.index(method)==0 else "INVALID_IMAGE",raw,observations,current["newPhaseLog"],stage["records"],process)
        else:
            need(not current["newStages"],"Unexpected retained new Stage")
            index=METHODS.index(method)
            validate_other_phases(current["newPhaseLog"],current["uid"],current["workerProc"],1 if index in (2,3) else 2 if index in (4,5) else 0)
            if index==2:
                need(len(observations)==1 and observations[0]["name"]=="valid-control","Healthy control observation missing/duplicate")
                value=observations[0]["value"]
                need(value.get("result")=="SUCCESS" and value.get("mime")=="image/jpeg" and value.get("width")==256 and value.get("height")==256 and type(value.get("byteLength")) is int and 0<value["byteLength"]<=2097152 and re.fullmatch(r"[0-9a-f]{64}",value.get("storedSha256","")),"Healthy import observation invalid")
            elif index==3:
                need(len(observations)==1 and observations[0]["name"]=="entropy-cut-import","Damaged import observation missing/duplicate")
                value=observations[0]["value"]
                need(value.get("result")=="FAILURE" and value.get("kind")=="INVALID_IMAGE" and value.get("activePreparedLease") is False,"Damaged input not strictly rejected/released")
            elif index in (4,5):
                target="project" if index==4 else "subscription"
                found={row["name"]:row["value"] for row in observations}
                need(len(observations)==2 and set(found)=={f"old-cover-{target}",f"old-cover-{target}-cleanup"},"Cover protection/cleanup observations absent or duplicate")
                first=found[f"old-cover-{target}"];last=found[f"old-cover-{target}-cleanup"]
                need(last.get("failedPreparationCleanupVerified") is True and {key:value for key,value in last.items() if key!="failedPreparationCleanupVerified"}==first,"Cleanup observation does not preserve prior facts")
                need(first.get("targetType")==target.upper() and first.get("source")=="entropy_cut_eoi_retained.jpg" and first.get("prepareResult")=="INVALID_IMAGE","Wrong protection target/input/outcome")
                for key in ("prepareWronglyAccepted","newFormalMetadataExists","replacementPreparedLeaseActive"):
                    need(first.get(key) is False,"Unsafe replacement flag: "+key)
                for key in ("oldFactsIntact","mediaTableIntact","privateFinalFilesIntact"):
                    need(first.get(key) is True,"Business/cover fact preservation unproved: "+key)
                need(re.fullmatch(r"[0-9a-f]{64}",first.get("oldCoverSha256Before","")) and first["oldCoverSha256Before"]==first.get("oldCoverSha256After"),"Old cover digest changed")
            else:
                need(not observations,"Unexpected observation/retained unproved stage in PNG/v1 method")
            result={"status":"PASS","errors":[],"scope":"LOCKED_METHOD_ASSERTIONS_AND_AVAILABLE_OBSERVATIONS",
                    "evidence_levels":{"business_facts":"STRUCTURED_IN_PROCESS_OBSERVATION" if index in (4,5) else "EXECUTED_TEST_ASSERTIONS_ONLY",
                        "png_v1_contract":"EXECUTED_TEST_ASSERTIONS_ONLY" if index>=6 else "NOT_APPLICABLE",
                        "per_invocation_pin_guard_fd":"EXECUTED_CODE_INVARIANT_ONLY","authenticated_retirement_phases":"INDEPENDENT_OBSERVATION" if index<6 else "NOT_APPLICABLE",
                        "worker_os_absence":"INDEPENDENT_OBSERVATION","raw_binder_hello_fields":"NOT_RUN","write_to_read_only_source":"NOT_RUN"}}
    except (ValueError,KeyError,TypeError,OSError) as error:
        result={**result,"status":"BLOCKED","errors":[str(error)]}
    return result
