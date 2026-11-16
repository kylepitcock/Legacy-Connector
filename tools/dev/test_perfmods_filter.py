import unittest

import perfmods_filter as pf

TOML = '''
[[mods]]
modId="irisveil"
[[dependencies.irisveil]]
    modId="neoforge"
    type="required"
[[dependencies.irisveil]]
    modId="iris"
    type="required"
[[dependencies.irisveil]]
    modId="veil"
    type="optional"
'''
OLD = '''
[[mods]]
modId="old"
[[dependencies.old]]
    modId="lib"
    mandatory=true
'''


class T(unittest.TestCase):
    def test_parse(self):
        p, r = pf._parse(TOML)
        self.assertEqual(p, {"irisveil"})
        self.assertEqual(r["irisveil"], {"neoforge", "iris"})
        self.assertEqual(pf._parse(OLD)[1]["old"], {"lib"})
        self.assertEqual(pf._parse_fallback(TOML)[1]["irisveil"], {"neoforge", "iris"})

    def test_select(self):
        iris = ({"iris"}, {})
        veilcompat = pf._parse(TOML)
        kept, skipped = pf.select({"iris.jar": iris, "irisveil.jar": veilcompat})
        self.assertEqual(kept, ["iris.jar"])  # veil absent: extra requirement
        self.assertEqual(skipped, {"irisveil.jar": ["veil"]})
        kept, skipped = pf.select({"iris.jar": iris, "irisveil.jar": veilcompat}, {"veil"})
        self.assertEqual(sorted(kept), ["iris.jar", "irisveil.jar"])

    def test_missing_required_and_chain(self):
        a = ({"a"}, {"a": {"missinglib"}})
        b = ({"b"}, {"b": {"a"}})
        kept, skipped = pf.select({"a.jar": a, "b.jar": b})
        self.assertEqual(kept, [])
        self.assertEqual(skipped["a.jar"], ["missinglib"])
        self.assertEqual(skipped["b.jar"], ["a"])


if __name__ == "__main__":
    unittest.main()
