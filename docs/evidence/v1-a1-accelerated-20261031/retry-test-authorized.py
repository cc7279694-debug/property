"""One additional Test install separately approved after its first rejection."""
from pathlib import Path
import datetime, importlib.util, json, sys

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("reviewed_session", HERE / "device-session.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
base.OUT = HERE / "retry-1"
auth = json.loads((base.OUT / "test-retry-authorization.json").read_text(encoding="utf-8"))
assert auth["additionalTestInstallAttempts"] == 1 and auth["automaticRetriesAllowed"] is False
actions = {"preflight":"test-retry-preflight", "install":"install-test-retry",
           "verify":"verify-test-retry", "launch":"launch-normal", "final":"final-identities"}
action = sys.argv[1]
assert action in actions
session = base.Session(actions[action])
try:
    session.identify()
    if action == "preflight":
        packages = {base.TEST:session.package(base.TEST)}
        session.data["packages"] = packages
        session.save()
        assert packages[base.TEST]["record"] == "ABSENT_IN_USER0_INCLUDING_RETAINED", "PREEXISTING_TEST_STOP"
        packages[base.APP] = session.package(base.APP)
        packages[base.PREVIEW] = session.package(base.PREVIEW)
        base.preview_ok(packages[base.PREVIEW])
        assert packages[base.APP]["installed"] and packages[base.APP]["uid"] == 10455
        assert packages[base.APP]["versionCode"] == 1 and packages[base.APP]["versionName"] == "0.1.0-v1-a1-debug"
        session.data["packages"] = packages
    elif action == "install":
        base.prior("test-retry-preflight")
        filename, expected = base.CANDIDATES["test"]
        blob = (base.ROOT / "verification-candidates/v1-a1" / filename).read_bytes()
        assert base.sha(blob) == expected and len(blob) == 3741308
        session.data["candidate"] = {"file":filename,"bytes":len(blob),"sha256":expected}
        value = session.shell("test-install-newly-authorized-once", "cmd package install --user 0 -t -S " + str(len(blob)), stdin=blob, timeout=90)
        assert value == "Success", "TEST_INSTALL_NOT_SUCCESS_NO_RETRY"
        session.data["installResult"] = value
    elif action in ("verify", "final"):
        base.prior("install-test-retry")
        packages = {p:session.package(p) for p in (base.APP,base.TEST,base.PREVIEW)}
        base.preview_ok(packages[base.PREVIEW])
        assert packages[base.APP]["installed"] and packages[base.APP]["uid"] == 10455
        assert packages[base.APP]["versionCode"] == 1 and packages[base.APP]["versionName"] == "0.1.0-v1-a1-debug"
        assert packages[base.TEST]["installed"] and 10000 <= packages[base.TEST]["uid"] < 100000
        assert len({p["uid"] for p in packages.values()}) == 3
        target = session.shell("instrumentation-registration-only", "cmd package list instrumentation " + base.APP)
        assert target == "instrumentation:" + base.TEST + "/com.cooper.lithic.app.M13IsolatedRunner (target=" + base.APP + ")"
        session.data.update(packages=packages, instrumentationRegistration=target,
                            instrumentationExecutionInferred=False)
        for package in (base.APP,base.TEST):
            pids = session.shell("exact-process-"+package,"pidof "+package,allowed=(0,1))
            assert not pids or all(p.isdecimal() for p in pids.split())
            session.data.setdefault("targetPids",{})[package] = pids.split()
    elif action == "launch":
        base.prior("verify-test-retry")
        value = session.shell("normal-main-launch-once", "am start --user 0 -W -n " + base.APP + "/com.cooper.lithic.app.MainActivity", timeout=30)
        assert "Status: ok" in value and "Error:" not in value
        assert "Activity: " + base.APP + "/com.cooper.lithic.app.MainActivity" in value
        session.data["normalMainLaunch"] = "ACTIVITY_MANAGER_STATUS_OK_NOT_BUSINESS_OR_VISUAL_PASS"
    session.data["status"] = "PASS"
except Exception as exc:
    session.data.update(status="BLOCKED",reason=type(exc).__name__ + ":" + str(exc).replace(session.serial or "<NONE>","<USB_SERIAL_REDACTED>"))
finally:
    session.data["finishedUtc"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    session.save()
    print(json.dumps({key:value for key,value in session.data.items() if key != "commands"},ensure_ascii=True))
