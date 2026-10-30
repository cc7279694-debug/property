"""Configuration-only matrix. Expected rejects are safety checks, not product FAILs."""
from pathlib import Path
import json, subprocess, sys

root = Path.cwd()
ev = root / "docs/evidence/v1-a1-preparation-20261030"
cases = [
    ("v1-on", ["-Pv1AcceptanceVerification=true"], "v1"),
    ("v1-off", ["-Pv1AcceptanceVerification=false"], "ordinary"),
    ("ordinary", [], "ordinary"),
    ("private", ["-PlithicPrivateTrial=true"], "private"),
    ("m22", ["-Pm22DeviceVerification=true", "-Pm13Isolated=true"], "m22"),
    ("c3", ["-Pc2VisualVerification=true"], "c3"),
    ("c6", ["-Pc6IsolationVerification=true"], "c6"),
    ("invalid-value", ["-Pv1AcceptanceVerification=TRUE"], "reject-value"),
    *[("conflict-" + name, ["-Pv1AcceptanceVerification=true", "-P" + flag + "=true"], "reject-identity")
      for name, flag in [("private", "lithicPrivateTrial"), ("m22", "m22DeviceVerification"),
                         ("c3", "c2VisualVerification"), ("c6", "c6IsolationVerification")]],
    ("conflict-death-true", ["-Pv1AcceptanceVerification=true", "-Pm12T04ProcessDeath=true"], "reject-death"),
    ("conflict-death-false", ["-Pv1AcceptanceVerification=true", "-Pm12T04ProcessDeath=false"], "reject-death"),
    ("conflict-plain-false", ["-Pv1AcceptanceVerification=true", "-Pm13Isolated=false"], "reject-plain"),
]
expected = {
    "ordinary": (".debug", 17, "0.1.0-m22-debug", "androidx.test.runner.AndroidJUnitRunner"),
    "private": (".preview.debug", 28, "0.1.0-preview.12-debug", "androidx.test.runner.AndroidJUnitRunner"),
    "m22": (".m22verify.debug", 17, "0.1.0-m22-debug", "com.cooper.lithic.app.M13IsolatedRunner"),
    "c3": (".c2visual.debug", 17, "0.1.0-c2visual-debug", "com.cooper.lithic.app.M13IsolatedRunner"),
    "c6": (".c6isolation.debug", 17, "0.1.0-c6isolation-debug", "com.cooper.lithic.app.M13IsolatedRunner"),
    "v1": (".v1acceptance.debug", 1, "0.1.0-v1-a1-debug", "com.cooper.lithic.app.M13IsolatedRunner"),
}
messages = {"reject-value": "v1AcceptanceVerification must be true or false",
            "reject-identity": "V1 acceptance cannot use private trial, M22, C3 visual or C6 isolation identities",
            "reject-death": "V1 acceptance cannot enable historical process-death experiments",
            "reject-plain": "V1 acceptance requires the plain Application M13IsolatedRunner"}
rows = []
for label, flags, mode in cases:
    # A separate project cache prevents these configuration observations inheriting old output history.
    command = [sys.executable, str(ev / "invoke-gradle.py"), "parameter-" + label,
               "--project-cache-dir", str(root / "app/build-v1-acceptance/configuration-cache"),
               "--init-script", str(ev / "identity.init.gradle"), *flags, ":app:v1IdentityAudit"]
    p = subprocess.run(command)
    folder = ev / ("parameter-" + label)
    stdout = (folder / "stdout.txt").read_text(encoding="utf-8", errors="replace")
    stderr = (folder / "stderr.txt").read_text(encoding="utf-8", errors="replace")
    if mode.startswith("reject-"):
        assert p.returncode != 0 and messages[mode] in stdout + stderr, label
        row = {"case": label, "status": "EXPECTED_REJECTION", "exitCode": p.returncode}
    else:
        assert p.returncode == 0, label
        data = json.loads(next(line.split("=", 1)[1] for line in stdout.splitlines() if line.startswith("V1_IDENTITY_JSON=")))
        suffix, code, version, runner = expected[mode]
        assert (data["debugPackage"], data["debugVersionCode"], data["debugVersionName"], data["runner"]) == (
            "com.cooper.lithic" + suffix, code, version, runner), label
        if mode in ("v1", "c3", "c6"):
            assert data["variants"] == ["debug"], label
        if mode == "v1":
            assert data["testPackage"] == "com.cooper.lithic.v1acceptance.debug.test"
            assert Path(data["buildDirectory"]) == root / "app/build-v1-acceptance"
        if mode == "ordinary":
            assert (data["releasePackage"], data["releaseVersionCode"], data["releaseVersionName"]) == ("com.cooper.lithic", 17, "0.1.0-m22")
        row = {"case": label, "status": "EXPECTED_IDENTITY", **data}
    rows.append(row)
    (ev / "parameter-results.json").write_text(json.dumps(rows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"case": label, "status": row["status"]}), flush=True)
assert len(rows) == 15
print("15 configuration safety cases complete; no Android methods executed", flush=True)
