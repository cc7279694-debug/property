"""One further App installation explicitly authorized by the user's retry request.

Reuse the reviewed transport without editing/overwriting the first failed run.
No automatic retries; all subsequent original gates still apply.
"""
from pathlib import Path
import datetime, importlib.util, json, sys

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("reviewed_session", HERE / "device-session.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
base.OUT = HERE / "retry-1"
authorization = json.loads((base.OUT / "authorization.json").read_text(encoding="utf-8"))
assert authorization["userInstruction"] == "重新来一次"
assert authorization["additionalAppInstallAttempts"] == 1
assert authorization["automaticRetriesAllowed"] is False

if sys.argv[1] != "preflight":
    base.main()
else:
    session = base.Session("preflight")
    try:
        session.identify()
        info = session.shell("phone-identity", "printf 'manufacturer='; getprop ro.product.manufacturer; printf 'model='; getprop ro.product.model; printf 'api='; getprop ro.build.version.sdk")
        identity = dict(line.split("=", 1) for line in info.splitlines())
        assert identity == {"manufacturer":"Xiaomi", "model":"23127PN0CC", "api":"36"}
        session.data["deviceIdentity"] = identity
        packages = {}
        for package in (base.APP, base.TEST, base.PREVIEW):
            packages[package] = session.package(package)
            session.data["packages"] = packages
            session.save()
            if package in (base.APP, base.TEST):
                assert packages[package]["record"] == "ABSENT_IN_USER0_INCLUDING_RETAINED", "PREEXISTING_PACKAGE_STOP"
            else:
                base.preview_ok(packages[package])
        session.data["manualHealthReference"] = {
            "source":"Earlier actual user reply in this same acceptance session; not a new observation",
            "statement":"已检查，均正常，手机已连接并解锁",
            "newUserInstruction":"重新来一次"}
        session.data["status"] = "PASS"
    except Exception as exc:
        session.data.update(status="BLOCKED", reason=type(exc).__name__ + ":" + str(exc).replace(session.serial or "<NONE>", "<USB_SERIAL_REDACTED>"))
    finally:
        session.data["finishedUtc"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
        session.save()
        print(json.dumps({key:value for key,value in session.data.items() if key != "commands"}, ensure_ascii=True))
