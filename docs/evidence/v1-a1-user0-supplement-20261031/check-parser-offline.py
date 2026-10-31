"""Synthetic host-only checks. Does not open any socket or query a device."""
from pathlib import Path
import importlib.util
import json
import subprocess
import sys

folder = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("user0_reader", folder / "query-user0-once.py")
reader = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reader)
awk = Path("C:/Program Files/Git/usr/bin/awk.exe")
header = "Packages:\n  Package [com.cooper.lithic.preview.debug] (abc123):\n"
user_true = "    User 0: ceDataInode=123 installed=true hidden=false\n"
user_false = "    User 0: installed=false hidden=false\n"
cases = [
    ("exact-user0-true", header + user_true, True),
    ("exact-user0-false", header + user_false, False),
    ("other-user-does-not-overwrite", header + user_true + "    User 10: installed=false\n", True),
    ("only-other-user-rejected", header + "    User 10: installed=true\n", None),
    ("package-record-without-user-rejected", header + "    versionCode=28\n", None),
    ("other-package-cannot-supply-user", header + "  Package [other.package] (def):\n" + user_true, None),
    ("suffix-package-rejected", header.replace("preview.debug]", "preview.debug.test]") + user_true, None),
    ("hidden-section-rejected", "Hidden system packages:\n" + header.split("\n", 1)[1] + user_true, None),
    ("left-packages-section-rejected", header + "Queries:\n" + user_true, None),
    ("duplicate-package-rejected", header + user_true + header.split("\n", 1)[1] + user_true, None),
    ("duplicate-user0-rejected", header + user_true + user_true, None),
    ("duplicate-installed-rejected", header + "    User 0: installed=true installed=false\n", None),
    ("invalid-installed-rejected", header + "    User 0: installed=unknown\n", None),
    ("missing-installed-rejected", header + "    User 0: hidden=false\n", None),
    ("multiple-packages-sections-rejected", header + user_true + "Packages:\n", None),
    ("empty-response-rejected", "", None),
    ("trailing-dumpsys-timeout-rejected", header + user_true + "*** SERVICE DUMP TIMEOUT EXPIRED ***\n", None),
    ("service-error-rejected", "Error dumping service package\n" + header + user_true, None),
]
results = []
for name, data, expected in cases:
    process = subprocess.run([str(awk), reader.AWK], input=data.encode("ascii"), capture_output=True, timeout=5)
    receipt = process.stdout + b"SOURCE_EXIT=0\nFILTER_EXIT=" + str(process.returncode).encode() + b"\n"
    try:
        actual = reader.parse_minimal(receipt, process.stderr, process.returncode)
    except ValueError:
        actual = None
    if actual is not expected:
        raise AssertionError(name)
    results.append({"case": name, "passed": True, "scope": "SYNTHETIC_HOST_ONLY"})

process = subprocess.run([str(awk), reader.AWK], input=(header + user_true).encode(), capture_output=True, timeout=5)
good = process.stdout + b"SOURCE_EXIT=0\nFILTER_EXIT=0\n"
mutations = [
    ("source-failure-rejected", good.replace(b"SOURCE_EXIT=0", b"SOURCE_EXIT=1"), b"", 0),
    ("shell-failure-rejected", good, b"", 1),
    ("stderr-rejected", good, b"synthetic error", 0),
    ("truncated-receipt-rejected", good[:-1], b"", 0),
    ("extra-key-rejected", good + b"EXTRA=1\n", b"", 0),
    ("wrong-receipt-package-rejected", good.replace(reader.PACKAGE.encode(), b"other.package"), b"", 0),
    ("wrong-receipt-user-rejected", good.replace(b"USER=0\n", b"USER=10\n"), b"", 0),
]
for name, data, error, exit_code in mutations:
    try:
        reader.parse_minimal(data, error, exit_code)
    except ValueError:
        results.append({"case": name, "passed": True, "scope": "SYNTHETIC_HOST_ONLY"})
    else:
        raise AssertionError(name)

assert not (folder / "session-claim.json").exists(), "Device query must not have run during offline checks"
output = {"checks": len(results), "passed": len(results), "deviceQueries": 0,
          "readerSha256": reader.sha((folder / "query-user0-once.py").read_bytes()), "results": results}
receipt = sys.argv[1] if len(sys.argv) > 1 else "offline-checks.json"
if receipt not in ("offline-checks.json", "offline-checks-final.json"):
    raise ValueError("UNEXPECTED_LOCAL_RECEIPT_NAME")
with (folder / receipt).open("x", encoding="utf-8") as file:
    json.dump(output, file, indent=2)
    file.write("\n")
print(json.dumps({"offlineChecks": len(results), "passed": len(results), "deviceQueries": 0}))
