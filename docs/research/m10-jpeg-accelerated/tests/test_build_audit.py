"""Pure parser checks; these do not compile or execute Android artifacts."""
import importlib.util
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "build_android.py"
SPEC = importlib.util.spec_from_file_location("candidate_build_audit", SCRIPT)
BUILD = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BUILD)
ELF = """Machine: AArch64
  LOAD 0x000000 0x000000 0x000000 0x0100 0x0100 R E 0x4000
  0x00000001 (NEEDED) Shared library: [libc.so]
"""


def symbol(name, binding="GLOBAL", visibility="DEFAULT", section="12"):
    return f"  1: 0000000000001000 42 FUNC {binding} {visibility} {section} {name}\n"


class BuildAuditTest(unittest.TestCase):
    def audit(self, extra="", jni=None):
        return BUILD.audit(ELF, (symbol(BUILD.JNI_NAME) if jni is None else jni) + extra, "AArch64")

    def test_only_default_global_jni_definition_is_accepted(self):
        result = self.audit()
        self.assertEqual(result["definedVisibleExports"], [BUILD.JNI_NAME])

    def test_additional_weak_default_definition_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "Unexpected defined visible export"):
            self.audit(symbol("unexpected_weak", binding="WEAK"))

    def test_additional_protected_definition_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "Unexpected defined visible export"):
            self.audit(symbol("unexpected_protected", visibility="PROTECTED"))

    def test_additional_absolute_definition_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "Unexpected defined visible export"):
            self.audit(symbol("unexpected_absolute", section="ABS"))

    def test_additional_unique_definition_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "Unexpected defined visible export"):
            self.audit(symbol("unexpected_unique", binding="UNIQUE"))

    def test_undefined_global_and_weak_imports_are_not_exports(self):
        result = self.audit(symbol("malloc@LIBC", section="UND") +
                            symbol("optional_import", binding="WEAK", section="UND"))
        self.assertEqual(result["definedVisibleExports"], [BUILD.JNI_NAME])

    def test_hidden_internal_and_local_definitions_are_not_exports(self):
        extra = (symbol("hidden", visibility="HIDDEN") +
                 symbol("internal", visibility="INTERNAL") +
                 symbol("local", binding="LOCAL"))
        self.assertEqual(self.audit(extra)["definedVisibleExports"], [BUILD.JNI_NAME])

    def test_jni_must_itself_be_a_defined_global_default_symbol(self):
        variants = ({"section": "UND"}, {"visibility": "HIDDEN"},
                    {"visibility": "PROTECTED"}, {"binding": "WEAK"})
        for variant in variants:
            with self.subTest(variant=variant), self.assertRaisesRegex(RuntimeError, "JNI symbol"):
                self.audit(jni=symbol(BUILD.JNI_NAME, **variant))


if __name__ == "__main__":
    unittest.main()
