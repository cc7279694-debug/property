"""Host-only assembly gate checks; no Android execution or compilation."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

REPO = Path(__file__).resolve().parents[3]
SPEC = importlib.util.spec_from_file_location(
    "candidate_build", REPO / "scripts/build_m10_worker_candidate.py")
BUILD = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BUILD)


class NativeAssemblyInputTests(unittest.TestCase):
    def test_exact_source_bytes_match(self):
        self.assertEqual(BUILD.same_source(b"int x;\n", b"int x;\n"), "EXACT")

    def test_only_crlf_lf_conversion_is_allowed(self):
        self.assertEqual(BUILD.same_source(b"int x;\r\n", b"int x;\n"), "CRLF_LF_ONLY")

    def test_modified_tokens_are_rejected(self):
        with self.assertRaises(RuntimeError):
            BUILD.same_source(b"int x;\n", b"int y;\n")

    def test_bare_carriage_return_is_not_normalized(self):
        with self.assertRaises(RuntimeError):
            BUILD.same_source(b"int x;\r", b"int x;\n")

    def test_nested_apk_asset_paths_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with self.assertRaises(RuntimeError):
                BUILD.checked_child(root, "../foreign")

    def test_java_native_mapping_is_exact(self):
        BUILD.check_java(REPO)

    def test_stale_extra_native_library_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "jniLibs/x86_64"
            target.mkdir(parents=True)
            (target / "unexpected.so").write_bytes(b"stale")
            with self.assertRaises(RuntimeError):
                BUILD.check_output_shape(root, {"x86_64"})

    def test_unknown_abi_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "jniLibs/unknown").mkdir(parents=True)
            with self.assertRaises(RuntimeError):
                BUILD.check_output_shape(root, {"x86_64"})

    def test_known_generated_output_can_be_rechecked(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "jniLibs/x86_64").mkdir(parents=True)
            (root / "jniLibs/x86_64/libstrictjpeg_candidate.so").write_bytes(b"known")
            BUILD.check_output_shape(root, {"x86_64"})


if __name__ == "__main__":
    unittest.main()
