import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import picker


def roots_for(base, spec):
    """spec: [(folder, [file names])] -> ordered roots like the picker builds them"""
    out = []
    for folder, names in spec:
        d = os.path.join(base, folder)
        os.makedirs(d, exist_ok=True)
        for n in names:
            open(os.path.join(d, n), "w").close()
        out.append((d, picker.listing(d)))
    return out


class PickerTest(unittest.TestCase):
    def test_version_folder_beats_earlier_unlabelled_folder(self):
        with tempfile.TemporaryDirectory() as t:
            roots = roots_for(t, [("test-mods-issues/1", ["ThermalExpansion-3.0.0.jar"]), ("1.12.2 test mods", ["ThermalExpansion-1.12.2-5.5.7.1.jar"])])
            st, path, _ = picker.pick("1.12.2", "ThermalExpansion", roots)
            self.assertEqual(st, "ok")
            self.assertTrue(path.endswith("ThermalExpansion-1.12.2-5.5.7.1.jar"), path)

    def test_other_version_jar_is_never_picked(self):
        with tempfile.TemporaryDirectory() as t:
            roots = roots_for(t, [("test-mods-147", ["ThermalExpansion-3.0.0.jar"])])
            st, path, note = picker.pick("1.12.2", "ThermalExpansion", roots)
            self.assertEqual(st, "missing")
            self.assertIn("other Minecraft versions", note)

    def test_file_name_token_labels_a_jar(self):
        with tempfile.TemporaryDirectory() as t:
            roots = roots_for(t, [("misc", ["Foo-1.4.7-2.0.jar", "Foo-1.12.2-2.0.jar"])])
            st, path, _ = picker.pick("1.12.2", "Foo", roots)
            self.assertTrue(path.endswith("Foo-1.12.2-2.0.jar"), path)

    def test_unlabelled_competing_names_are_a_setup_error(self):
        with tempfile.TemporaryDirectory() as t:
            roots = roots_for(t, [("misc", ["Foo-a.jar", "Foo-b.jar"])])
            self.assertEqual(picker.pick("1.12.2", "Foo", roots)[0], "setup")

    def test_line_without_version_matching_two_versions_is_setup(self):
        with tempfile.TemporaryDirectory() as t:
            lib = os.path.join(t, "lib")
            os.makedirs(lib)
            for n in ("Foo-1.12.2.jar", "Foo-1.7.10.jar"):
                open(os.path.join(lib, n), "w").close()
            self.assertEqual(picker.pick("lib", "Foo", [], lib)[0], "setup")

    def test_wildcard_and_brackets(self):
        with tempfile.TemporaryDirectory() as t:
            roots = roots_for(t, [("test-mods-152", ["Twilight Forest - MC 1.5.2.jar", "x[1].jar"])])
            self.assertEqual(picker.pick("1.5.2", "Twilight*1.5.2", roots)[0], "ok")
            self.assertEqual(picker.pick("1.5.2", "x[1]", roots)[0], "ok")

    def test_beta_173_is_not_confused_with_a_mod_version(self):
        self.assertIsNone(picker.infer("mowziesmobs-1.7.3.jar"))
        self.assertEqual(picker.infer("Y:/r/test-mods-b173/Aether_1.7.3_v1.01.zip"), "b1.7.3")


if __name__ == "__main__":
    unittest.main()
