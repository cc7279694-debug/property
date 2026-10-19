"""Host-only regression of actual installed() with locked dumpsys bytes."""
import hashlib
import json
from pathlib import Path
import unittest

import validate


FIXTURES = Path(__file__).with_name("evidence") / "installed-uid"
APP = validate.APP
TEST = validate.TEST
PATH = "/data/app/isolated/base.apk"
SHA = "a" * 64


def historical(api):
    return (FIXTURES / f"api{api}-dumpsys-package.raw").read_bytes().decode("utf-8")


def minimal(uid="appId=10216", user="User 0: installed=true", package=APP):
    return f"Packages:\n  Package [{package}] (abc):\n    {uid}\n    codePath=/data/app/isolated\n    {user}\n\nQueries:\n"


def historical_path(api):
    line = next(row for row in historical(api).splitlines() if row.startswith("    codePath="))
    return line.partition("=")[2] + "/base.apk"


class InstalledUidTest(unittest.TestCase):
    def installed(self, dump=None, listing=None, package=APP, paths=None, actual_sha=SHA,
                  query_error=False):
        self.calls = []
        instance = object.__new__(validate.Supervisor)
        actual_dump = historical(36) if dump is None else dump
        default_path = next((historical_path(api) for api in (26, 36)
                             if actual_dump == historical(api)), PATH)
        def adb(*args):
            self.calls.append(args)
            if args == ("shell", "pm", "path", package):
                return paths if paths is not None else "package:" + default_path
            if len(args) == 3 and args[:2] == ("shell", "sha256sum"):
                return actual_sha + "  " + args[2]
            if args == ("shell", "dumpsys", "package", package):
                return historical(36) if dump is None else dump
            if args == ("shell", "pm", "list", "packages", "-U", "--user", "0", package):
                if query_error:
                    raise RuntimeError("command failed")
                return f"package:{package} uid:10216" if listing is None else listing
            raise AssertionError(f"Unexpected command: {args}")
        instance.adb = adb
        return instance.installed(package, SHA)

    def blocked(self, *args, **kwargs):
        with self.assertRaises(RuntimeError):
            self.installed(*args, **kwargs)

    def test_locked_fixture_bytes_and_sha(self):
        for row in json.loads((FIXTURES / "provenance.json").read_text())["inputs"]:
            raw = (FIXTURES / row["file"]).read_bytes()
            self.assertEqual(len(raw), row["bytes"])
            self.assertEqual(hashlib.sha256(raw).hexdigest(), row["sha256"])

    def test_real_api26_userid_and_independent_uid(self):
        self.assertEqual(self.installed(historical(26), f"package:{APP} uid:10077"), 10077)
        self.assertIn(("shell", "pm", "list", "packages", "-U", "--user", "0", APP), self.calls)

    def test_real_api36_appid_and_independent_uid(self):
        self.assertEqual(self.installed(), 10216)

    def test_real_api36_independent_query_bytes(self):
        raw = (FIXTURES / "api36-independent-user0-uid.raw").read_bytes()
        self.assertEqual(self.installed(historical(36), raw.decode("utf-8")), 10216)

    def test_target_app_prefix_can_also_return_distinct_locked_test(self):
        self.assertEqual(self.installed(listing=f"package:{APP} uid:10216\npackage:{TEST} uid:10217"), 10216)

    def test_both_identity_fields_agree(self):
        self.assertEqual(self.installed(minimal("appId=10216\n    userId=10216")), 10216)

    def test_conflicting_appid_userid_rejected(self):
        self.blocked(minimal("appId=10216\n    userId=10217"))

    def test_duplicate_userid_even_same_value_rejected(self):
        self.blocked(minimal("userId=10216\n    userId=10216"))

    def test_duplicate_appid_even_same_value_rejected(self):
        self.blocked(minimal("appId=10216\n    appId=10216"))

    def test_permission_uid_cannot_supply_missing_package_identity(self):
        self.blocked("Permissions:\n  uid=10216 userId=10216\n" + minimal("versionCode=8"))

    def test_other_package_identity_cannot_supply_target_identity(self):
        self.blocked(minimal("versionCode=8") + f"Other:\n  Package [{TEST}] (abc):\n    userId=10216\n")

    def test_duplicate_packages_region_rejected(self):
        self.blocked(minimal("userId=10216") + minimal("userId=10216"))

    def test_duplicate_target_package_rejected(self):
        self.blocked(minimal("userId=10216").replace("\nQueries:", f"\n  Package [{APP}] (abc):\n    userId=10216\n    User 0: installed=true\n\nQueries:"))

    def test_foreign_package_in_packages_region_rejected(self):
        self.blocked(minimal("userId=10216").replace("\nQueries:", f"\n  Package [{TEST}] (abc):\n    userId=10217\n\nQueries:"))

    def test_installed_false_rejected(self):
        self.blocked(minimal("userId=10216", "User 0: installed=false"))

    def test_missing_installed_flag_rejected(self):
        self.blocked(minimal("userId=10216", "User 0: hidden=false"))

    def test_duplicate_installed_flag_rejected(self):
        self.blocked(minimal("userId=10216", "User 0: installed=true installed=true"))

    def test_other_user_cannot_satisfy_user_zero(self):
        self.blocked(minimal("userId=10216", "User 10: installed=true"))

    def test_duplicate_user_zero_rejected(self):
        self.blocked(minimal("userId=10216", "User 0: installed=true\n    User 0: installed=true"))

    def test_cross_user_uid_rejected(self):
        self.blocked(minimal("userId=110216"), f"package:{APP} uid:110216")

    def test_malformed_signed_uid_rejected(self):
        self.blocked(minimal("userId=+10216"))

    def test_uid_trailing_tokens_rejected(self):
        self.blocked(minimal("userId=10216 arbitrary=1"))

    def test_incomplete_packages_region_rejected(self):
        self.blocked(minimal("userId=10216").split("\nQueries:")[0])

    def test_wrong_target_package_rejected(self):
        self.blocked(minimal("userId=10216", package=TEST))

    def test_independent_uid_mismatch_rejected(self):
        self.blocked(minimal("userId=10216"), f"package:{APP} uid:10217")

    def test_independent_uid_missing_rejected(self):
        self.blocked(minimal("userId=10216"), "")

    def test_independent_target_duplicate_rejected(self):
        self.blocked(minimal("userId=10216"), f"package:{APP} uid:10216\npackage:{APP} uid:10216")

    def test_independent_wrong_package_rejected(self):
        self.blocked(minimal("userId=10216"), "package:other.app uid:10216")

    def test_independent_locked_sibling_same_uid_rejected(self):
        self.blocked(minimal("userId=10216"), f"package:{APP} uid:10216\npackage:{TEST} uid:10216")

    def test_independent_user_zero_query_failure_rejected(self):
        self.blocked(minimal("userId=10216"), query_error=True)

    def test_independent_query_unexpected_format_rejected(self):
        self.blocked(minimal("userId=10216"), f"package:{APP} uid=10216")

    def test_independent_query_unknown_suffix_rejected(self):
        self.blocked(minimal("userId=10216"), f"package:{APP} uid:10216 user:10")

    def test_pm_path_ambiguity_remains_rejected(self):
        self.blocked(paths="package:" + PATH + "\npackage:/data/app/split.apk")

    def test_installed_apk_sha_mismatch_remains_rejected(self):
        self.blocked(actual_sha="b" * 64)

    def test_nested_other_package_header_rejected(self):
        self.blocked(minimal("userId=10216").replace("\nQueries:", f"\n    Package [{TEST}] (abc):\n\nQueries:"))

    def test_nested_uid_field_rejected(self):
        self.blocked(minimal("userId=10216\n      appId=10216"))

    def test_system_uid_not_ordinary_application(self):
        self.blocked(minimal("userId=1000"), f"package:{APP} uid:1000")

    def test_isolated_uid_not_ordinary_application(self):
        self.blocked(minimal("userId=99001"), f"package:{APP} uid:99001")

    def test_user_zero_alias_is_not_canonical_user_zero(self):
        self.blocked(minimal("userId=10216", "User 00: installed=true"))

    def test_independent_query_for_test_cannot_return_app(self):
        self.blocked(minimal("userId=10217", package=TEST),
                     f"package:{TEST} uid:10217\npackage:{APP} uid:10216", package=TEST)

    def test_unknown_locked_prefix_package_rejected(self):
        self.blocked(minimal("userId=10216"), f"package:{APP} uid:10216\npackage:{APP}.unknown uid:10218")

    def test_real_target_codepath_must_match_pm_path(self):
        changed = historical(36).replace("    codePath=", "    codePath=/wrong-prefix", 1)
        self.blocked(changed, paths="package:" + historical_path(36))

    def test_missing_codepath_rejected(self):
        self.blocked(minimal("userId=10216").replace("    codePath=/data/app/isolated\n", ""))

    def test_duplicate_codepath_even_matching_rejected(self):
        self.blocked(minimal("userId=10216").replace("    codePath=/data/app/isolated", "    codePath=/data/app/isolated\n    codePath=/data/app/isolated"))

    def test_nested_codepath_rejected(self):
        self.blocked(minimal("userId=10216").replace("    codePath=", "      codePath="))

    def test_malformed_codepath_rejected(self):
        self.blocked(minimal("userId=10216").replace("codePath=/data/app/isolated", "codePath=/data/app/isolated trailing=value"))

    def test_real_owned_permission_uid_conflict_rejected(self):
        changed = historical(36).replace("    uid=10216 gids=[]", "    uid=10217 gids=[]", 1)
        self.blocked(changed, paths="package:" + historical_path(36))

    def test_duplicate_owned_permission_uid_rejected(self):
        changed = historical(36).replace("    uid=10216 gids=[]", "    uid=10216\n    uid=10216 gids=[]", 1)
        self.blocked(changed, paths="package:" + historical_path(36))

    def test_duplicate_permission_source_package_rejected(self):
        changed = historical(36).replace(f"    sourcePackage={APP}", f"    sourcePackage={APP}\n    sourcePackage={APP}", 1)
        self.blocked(changed, paths="package:" + historical_path(36))

    def test_owned_permission_uid_malformed_rejected(self):
        changed = historical(36).replace("    uid=10216 gids=[]", "    uid=+10216 gids=[]", 1)
        self.blocked(changed, paths="package:" + historical_path(36))

    def test_owned_permission_truncated_record_rejected(self):
        changed = historical(36).replace("    perm=PermissionInfo{d5bd57 com.cooper.lithic.debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION}", "", 1)
        self.blocked(changed, paths="package:" + historical_path(36))

    def test_other_permission_uid_does_not_provide_target_identity(self):
        prefix = "Permissions:\n  Permission [other.permission] (abc):\n    sourcePackage=other.app\n    uid=12345 gids=[] type=0\n    perm=PermissionInfo{abc other.permission}\n\n"
        self.assertEqual(self.installed(prefix + minimal("userId=10216")), 10216)

    def test_test_package_can_have_no_permission_definitions(self):
        self.assertEqual(self.installed(minimal("appId=10217", package=TEST),
                                       f"package:{TEST} uid:10217", package=TEST), 10217)


if __name__ == "__main__":
    unittest.main(verbosity=2)
