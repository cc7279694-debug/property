"""Fail-closed interpretation of locked natural Worker admission evidence.

This module never runs Android commands. A host fixture proves the interpreter,
not Binder, native loading, or an actual Android lifecycle.
"""
from __future__ import annotations

import hashlib
import json
import re

APP = "com.cooper.lithic.debug"
TEST_CLASS = "com.cooper.lithic.data.media.M10WorkerRetirementReviewTest"
METHODS = {
    "SUCCESS": "healthy_jpeg_retires_real_worker_before_stage_completion",
    "INVALID_IMAGE": "locked_damaged_jpeg_is_rejected_and_real_worker_safely_retires",
}
SOURCES = {
    "SUCCESS": (59334, "2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907"),
    "INVALID_IMAGE": (20194, "f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a"),
}
PHASES = {
    "WORKER_RETIRE_ARMED", "HOST_RETIRE_ACKNOWLEDGED", "HOST_UNBIND_RETURNED",
    "WORKER_ON_UNBIND", "WORKER_EXIT_AUTHENTICATED_DETACH", "HOST_BINDER_DIED",
}
OWNER_KEYS = {"protocol", "version", "generation", "assetId", "attemptId", "sessionId", "nonce", "guardDevice", "guardInode", "decoderPolicy", "normalizationPolicy"}
FILE_KEYS = {"ownerHash", "device", "inode", "bytes", "sha256"}
PIN_KEYS = {"ownerHash", "sourceHash", "pid", "uid", "startTicks"}
PROOF_KEYS = {"ownerHash", "terminalHash"}
TERMINAL_KEYS = {"ownerHash", "pinHash", "sourceHash", "outcome", "resultCount", "sourceFdClosed", "outputFdClosed", "binderDead", "matchingProcessDead", "strictFinished", "decoderDestroyed", "connectionCleaned", "outputDevice", "outputInode", "outputBytes", "outputSha", "width", "height"}
OBS_KEYS = {"generation", "attempt", "session", "outcome", "hostPid", "hostUid", "workerPid", "workerUid", "workerStartTicks", "resultCount", "pinBeforeDelivery", "sourceClosed", "outputClosed", "connectionCleaned", "binderDead", "matchingProcessDead", "guardReleased", "strictFinished", "decoderDestroyed", "safeCleanup", "detail"}

class EvidenceError(ValueError):
    pass

def need(condition, message):
    if not condition:
        raise EvidenceError(message)

def digest(text):
    return hashlib.sha256(text.encode("ascii")).hexdigest()

def positive(value):
    return type(value) is int and 0 < value <= 9223372036854775807

def number(value, minimum=0):
    need(isinstance(value, str) and re.fullmatch(r"0|[1-9][0-9]*", value) is not None, "Noncanonical numeric record field")
    result = int(value)
    need(minimum <= result <= 9223372036854775807, "Numeric record outside allowed range")
    return result

def hash_value(value):
    need(isinstance(value, str) and re.fullmatch(r"[a-f0-9]{64}", value) is not None, "Malformed digest")
    return value

def strict_json(text):
    def unique(pairs):
        result={}
        for key,value in pairs:
            need(key not in result, "Duplicate JSON key")
            result[key]=value
        return result
    return json.loads(text, object_pairs_hook=unique)

def canonical_record(text, keys):
    need(isinstance(text, str) and 1 <= len(text) <= 16384 and text.endswith("\n"), "Missing or truncated Stage record")
    need(all(character == "\n" or 32 <= ord(character) <= 126 for character in text), "Noncanonical Stage bytes")
    pairs=[]
    for line in text[:-1].split("\n"):
        need(line.count("=") == 1, "Malformed Stage row")
        key,value=line.split("=")
        need(re.fullmatch(r"[A-Za-z][A-Za-z0-9]*", key) is not None and re.fullmatch(r"[A-Za-z0-9._:-]{1,256}", value) is not None, "Malformed Stage key/value")
        pairs.append((key,value))
    values=dict(pairs)
    need(len(values)==len(pairs) and set(values)==keys, "Duplicate, missing or unknown Stage field")
    need(text=="".join(f"{key}={values[key]}\n" for key in sorted(values)), "Unsorted Stage record")
    return values

def validate_instrumentation(text, expected_method):
    need(isinstance(text,str) and len(text) <= 2*1024*1024, "Instrumentation output unavailable/oversized")
    need(not re.search(r"FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|INSTRUMENTATION_ABORTED|\bTIMEOUT\b|shortMsg=|INSTRUMENTATION_STATUS: (?:Error|stack)=",text,re.I), "Instrumentation failure/crash/timeout evidence")
    need(len(re.findall(r"^\s*OK \(1 test\)\s*$",text,re.M))==1, "Exactly one instrumentation test must pass")
    need(re.findall(r"^INSTRUMENTATION_CODE:\s*(-?\d+)\s*$",text,re.M)==["-1"], "Instrumentation completion code absent or non-success")
    need(re.findall(r"^INSTRUMENTATION_STATUS_CODE:\s*(-?\d+)\s*$",text,re.M)==["1","0"], "Instrumentation start/end status not unique success")
    classes=re.findall(r"^INSTRUMENTATION_STATUS: class=(.+)$",text,re.M)
    methods=re.findall(r"^INSTRUMENTATION_STATUS: test=(.+)$",text,re.M)
    need(classes and set(item.strip() for item in classes)=={TEST_CLASS}, "Wrong instrumentation class")
    need(methods and set(item.strip() for item in methods)=={expected_method}, "Wrong instrumentation method")

def observations_pair(observations, expected_outcome):
    need(isinstance(observations,list) and len(observations)==2, "Exactly result and terminal observations required; no abandonment")
    found={}
    for item in observations:
        need(isinstance(item,dict), "Observation wrapper malformed")
        name=item.get("name")
        if name is None:
            name=item.get("filename", "")
            need(isinstance(name,str),"Malformed observation filename")
            name=next((prefix for prefix in ("worker-review-result","worker-review-terminal") if name.startswith(prefix+"-")),None)
        need(isinstance(name,str) and name in {"worker-review-result","worker-review-terminal"} and name not in found, "Unknown/duplicate/abandoned observation")
        value=item.get("value")
        need(isinstance(value,dict) and set(value)==OBS_KEYS, "Missing or unexpected observation field")
        found[name]=value
    first=found["worker-review-result"]; last=found["worker-review-terminal"]
    need(first["safeCleanup"] is False and last["safeCleanup"] is True, "Observation cleanup transition unproved")
    need({key:value for key,value in first.items() if key!="safeCleanup"}=={key:value for key,value in last.items() if key!="safeCleanup"}, "Observation identity or proof changed")
    need(first["outcome"]==expected_outcome, "Unexpected Worker outcome")
    for key in ("hostPid","hostUid","workerPid","workerUid","workerStartTicks"):
        need(positive(first[key]), "Invalid Worker or Host numeric identity")
    need(first["hostPid"]!=first["workerPid"] and first["hostUid"]==first["workerUid"], "Host/Worker isolation identity conflict")
    need(type(first["resultCount"]) is int and first["resultCount"]==1, "Structured result is not unique")
    for key in ("generation","attempt","session"):
        need(isinstance(first[key],str) and re.fullmatch(r"[A-Za-z0-9._:-]{1,128}",first[key]) is not None, "Malformed attempt/session/generation")
    for key in ("pinBeforeDelivery","sourceClosed","outputClosed","connectionCleaned","binderDead","matchingProcessDead","guardReleased","decoderDestroyed"):
        need(first[key] is True, "Unproved resource or lifecycle flag: "+key)
    need(first["strictFinished"] is (expected_outcome=="SUCCESS"), "Strict decode outcome conflict")
    detail=first["detail"]
    need(isinstance(detail,str) and 1<=len(detail)<=512 and not re.search(r"UNKNOWN|TIMEOUT|REVOKE|UNPREPARED|NO_STRUCTURED_RESULT|RESOURCE_CLOSE",detail,re.I), "Unresolved failure detail")
    return first

def validate_phases(text, observed):
    need(isinstance(text,str) and len(text)<=2*1024*1024, "Lifecycle log missing or unbounded")
    events={}
    for line in text.splitlines():
        if not line.strip() or line.startswith("--------- beginning of "):
            continue
        match=re.fullmatch(r"\d\d-\d\d\s+\d\d:\d\d:\d\d\.\d{3,6}\s+(\d+)\s+(\d+)\s+I\s+LITHIC_JPEG\s*:\s*([A-Z_]+) attempt=([A-Za-z0-9._:-]+) session=([A-Za-z0-9._:-]+) worker=(\d+)/(\d+)/(\d+) at=(\d+)\s*",line)
        need(match is not None, "Unknown, truncated or unfiltered lifecycle log line")
        emitter,_,phase,attempt,session,pid,uid,start,at=match.groups()
        need(phase in PHASES and phase not in events, "Missing/duplicate/unsafe lifecycle event: "+phase)
        need((attempt,session)==(observed["attempt"],observed["session"]), "Lifecycle event belongs to other attempt")
        need((int(pid),int(uid),int(start))==(observed["workerPid"],observed["workerUid"],observed["workerStartTicks"]), "Lifecycle Worker identity mismatch")
        need(int(emitter)==observed["hostPid" if phase.startswith("HOST_") else "workerPid"], "Lifecycle log emitter PID mismatch")
        events[phase]=int(at)
    need(set(events)==PHASES,"Incomplete six-phase authenticated retirement evidence")
    for before,after in (("WORKER_RETIRE_ARMED","HOST_RETIRE_ACKNOWLEDGED"),
                         ("HOST_RETIRE_ACKNOWLEDGED","HOST_UNBIND_RETURNED"),
                         ("HOST_RETIRE_ACKNOWLEDGED","WORKER_ON_UNBIND"),
                         ("WORKER_ON_UNBIND","WORKER_EXIT_AUTHENTICATED_DETACH"),
                         ("WORKER_EXIT_AUTHENTICATED_DETACH","HOST_BINDER_DIED")):
        need(events[before]<=events[after],"Illegal authenticated retirement partial order")
    return events

def validate_stage(records, observed, expected_outcome):
    need(isinstance(records,dict), "Stage snapshot unavailable")
    required={"owner.v2","source.v2","output.v2","pin.v2","delivered.v2","terminal.v2","revoked.v2","cleanup-intent.v2","cleaned.v2","consumer.guard","__metadata__"}
    if expected_outcome=="SUCCESS":required.add("prepared.v2")
    need(set(records)==required,"Unknown Stage child, missing proof or remaining payload")
    need(all(isinstance(value,str) and len(value)<=16384 for key,value in records.items() if key!="__metadata__"),"Stage text missing or oversized")
    need(isinstance(records["__metadata__"],str) and len(records["__metadata__"])<=65536,"Stage metadata missing or oversized")
    metadata=strict_json(records["__metadata__"])
    need(isinstance(metadata,dict) and metadata.get("complete") is True and isinstance(metadata.get("entries"),dict), "Complete independently enumerated Stage metadata required")
    entries=metadata["entries"]
    need(set(entries)==required-{"__metadata__"}, "Stage listing and record inventory differ")
    for name,entry in entries.items():
        need(isinstance(entry,dict) and entry.get("type")=="regular" and type(entry.get("device")) is int and entry["device"]>=0 and positive(entry.get("inode")), "Stage link/nonregular/unidentified child")
        need(type(entry.get("bytes")) is int and entry["bytes"]==len(records[name].encode("ascii")) and entry.get("sha256")==digest(records[name]), "Stage raw bytes/hash/metadata mismatch")
    need(records["consumer.guard"]=="", "Guard must be empty")
    owner=canonical_record(records["owner.v2"],OWNER_KEYS);owner_hash=digest(records["owner.v2"])
    need(owner["protocol"]=="lithic-m10-jpeg-stage" and owner["version"]=="2" and owner["decoderPolicy"]=="libjpeg-turbo-3.2.0-e01-strict-v1" and owner["normalizationPolicy"]=="m10-jpeg-normalize-v1","Unknown Stage protocol/policy")
    need((owner["generation"],owner["attemptId"],owner["sessionId"])==(observed["generation"],observed["attempt"],observed["session"]),"Stage owner differs from executed attempt")
    for key in ("assetId","attemptId","sessionId","nonce"):
        need(re.fullmatch(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",owner[key]) is not None,"Noncanonical owner UUID")
    guard=entries["consumer.guard"]
    need(number(owner["guardDevice"])==guard["device"] and number(owner["guardInode"],1)==guard["inode"],"Guard inode differs from owner")
    source=canonical_record(records["source.v2"],FILE_KEYS)
    output=canonical_record(records["output.v2"],FILE_KEYS)
    for value in (source,output):
        need(value["ownerHash"]==owner_hash,"File owner digest mismatch")
        number(value["device"]);number(value["inode"],1);number(value["bytes"]);hash_value(value["sha256"])
    need((number(source["bytes"]),source["sha256"])==SOURCES[expected_outcome],"Locked source size/SHA mismatch")
    need(output["bytes"]=="0" and output["sha256"]==digest(""),"Output creation identity is not empty")
    pin=canonical_record(records["pin.v2"],PIN_KEYS)
    need(pin["ownerHash"]==owner_hash and pin["sourceHash"]==source["sha256"],"Persistent pin ownership/source mismatch")
    need(tuple(number(pin[key],1) for key in ("pid","uid","startTicks"))==(observed["workerPid"],observed["workerUid"],observed["workerStartTicks"]),"Persistent consumer identity mismatch")
    delivery=canonical_record(records["delivered.v2"],{"ownerHash","pinHash"})
    need(delivery=={"ownerHash":owner_hash,"pinHash":digest(records["pin.v2"])},"Delivery does not authenticate durable pin")
    revoked=canonical_record(records["revoked.v2"],{"ownerHash","reasonHash"})
    need(revoked["ownerHash"]==owner_hash,"Revocation owner mismatch");hash_value(revoked["reasonHash"])
    terminal=canonical_record(records["terminal.v2"],TERMINAL_KEYS)
    need(terminal["ownerHash"]==owner_hash and terminal["pinHash"]==digest(records["pin.v2"]) and terminal["sourceHash"]==source["sha256"],"Terminal identity digest mismatch")
    need(terminal["outcome"]==("SUCCESS" if expected_outcome=="SUCCESS" else "IMAGE") and terminal["resultCount"]=="1","Terminal outcome/result conflict")
    for key in ("sourceFdClosed","outputFdClosed","binderDead","matchingProcessDead","connectionCleaned","decoderDestroyed"):
        need(terminal[key]=="true","Terminal resource proof missing: "+key)
    need(terminal["strictFinished"]==("true" if expected_outcome=="SUCCESS" else "false"),"Terminal strict proof conflict")
    if expected_outcome=="SUCCESS":
        need(terminal["outputDevice"]==output["device"] and terminal["outputInode"]==output["inode"],"Terminal output inode changed")
        need(1<=number(terminal["outputBytes"],1)<=2*1024*1024,"Output exceeds frozen limit")
        hash_value(terminal["outputSha"])
        need(terminal["width"]=="256" and terminal["height"]=="256","Locked healthy dimensions changed")
    else:
        need(all(terminal[key]=="none" for key in ("outputDevice","outputInode","outputBytes","outputSha","width","height")),"Rejected image falsely retains prepared output")
    expected_proof={"ownerHash":owner_hash,"terminalHash":digest(records["terminal.v2"])}
    for name in ("prepared.v2","cleanup-intent.v2","cleaned.v2"):
        if name in records:
            need(canonical_record(records[name],PROOF_KEYS)==expected_proof,"Completion/cleanup proof hash mismatch")
    return {"owner":owner,"source":source,"terminal":terminal}

def validate_process(text, observed):
    process=strict_json(text) if isinstance(text,str) else text
    need(isinstance(process,dict) and process.get("complete") is True and process.get("uid")==observed["hostUid"] and type(process.get("uid")) is int,"Independent process snapshot missing or identity conflict")
    need(process.get("worker_absent") is True and isinstance(process.get("rows"),list),"No complete worker absence evidence")
    seen=set()
    for row in process["rows"]:
        need(isinstance(row,dict) and positive(row.get("pid")) and positive(row.get("uid")) and positive(row.get("startTicks")),"Malformed process observation")
        need(row["pid"] not in seen,"Duplicate process observation");seen.add(row["pid"])
        need(row["uid"]==observed["hostUid"] and row.get("name")==APP and row["pid"]==observed["hostPid"],"Unknown same-UID process or live Worker")
    proof=process.get("worker_proc")
    need(isinstance(proof,dict) and proof.get("pid")==observed["workerPid"] and proof.get("startTicks")==observed["workerStartTicks"] and proof.get("absent") is True,"Independent matching /proc absence proof unavailable")
    return process

def validate_lifecycle(expected_outcome, instrumentation_text, observations,
                       phase_text, stage_records, process_text):
    """Interpret actual bounded snapshots, preserving unknowns rather than inferring PASS.

    stage_records maps every actual basename to its raw canonical ASCII text, with
    __metadata__ containing complete independently enumerated stat/hash evidence.
    process_text is JSON: complete/uid/rows/worker_absent/worker_proc. PID reuse is
    deliberately BLOCKED pending a separately reviewed adapter, never guessed dead.
    """
    result={"status":"BLOCKED","errors":[],"checks":{},"identity":{},"evidence_levels":{
        "synchronous_hello":"EXECUTED_CODE_INVARIANT_ONLY",
        "host_fd_open_close":"EXECUTED_CODE_INVARIANT_ONLY",
        "source_fd_read_only_mode":"EXECUTED_CODE_INVARIANT_ONLY",
        "write_to_read_only_source":"NOT_RUN",
        "raw_binder_hello_fields":"NOT_RUN",
        "retirement_ack_phase_identity":"INDEPENDENT_OBSERVATION",
        "worker_fd_closure":"STRUCTURED_IN_PROCESS_OBSERVATION",
        "guard_release":"STRUCTURED_IN_PROCESS_OBSERVATION",
        "persistent_pin_terminal_cleanup":"INDEPENDENT_OBSERVATION",
        "worker_os_absence":"INDEPENDENT_OBSERVATION",
        "native_loading":"EXECUTED_CODE_INVARIANT_ONLY",
    }}
    try:
        need(expected_outcome in METHODS,"Outcome outside fixed natural lifecycle scope")
        validate_instrumentation(instrumentation_text,METHODS[expected_outcome]);result["checks"]["instrumentation"]="PASS"
        observed=observations_pair(observations,expected_outcome);result["checks"]["structured_observations"]="PASS"
        result["identity"]={key:observed[key] for key in ("generation","attempt","session","hostPid","hostUid","workerPid","workerUid","workerStartTicks")}
        result["phases"]=validate_phases(phase_text,observed);result["checks"]["authenticated_retirement_partial_order"]="PASS"
        validate_stage(stage_records,observed,expected_outcome);result["checks"]["durable_stage_ownership_and_terminal_cleanup"]="PASS"
        validate_process(process_text,observed);result["checks"]["independent_process_absence"]="PASS"
        result["status"]="PASS"
    except (EvidenceError,ValueError,TypeError,KeyError,UnicodeError,OverflowError) as error:
        result["errors"].append(str(error))
    return result
