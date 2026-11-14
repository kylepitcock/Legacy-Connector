import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import client

TEXT = """# comment
mods: 1.7.10 Thaumcraft-1.7.10
mods: 1.12.2 some mod.jar
world: minecraft:normal

cmd /give @s minecraft:bow
wait 20
expect ^a :: b :: c
logabsent Stack.*Error
shot one
"""


class ParseTest(unittest.TestCase):
    def test_parse(self):
        sc = client.parse_scenario("x", TEXT)
        self.assertEqual(sc.mods, [("1.7.10", "Thaumcraft-1.7.10"), ("1.12.2", "some mod.jar")])
        self.assertEqual(sc.world, "minecraft:normal")
        self.assertEqual([s.kind for s in sc.steps], ["control", "control", "expect", "logabsent", "shot"])
        e = sc.steps[2]
        self.assertEqual((e.pattern, e.js, e.no), ("^a", "b :: c", 8))
        self.assertEqual(sc.steps[4].text, "one")

    def test_default_world(self):
        self.assertEqual(client.parse_scenario("x", "wait 1").world, "minecraft:flat")

    def test_errors(self):
        for bad in ["expect nothing", "expect (  :: js", "logabsent (", "mods: onlyversion\nwait 1", "# nothing"]:
            with self.assertRaises(client.ScenarioError, msg=bad):
                client.parse_scenario("x", bad)

    def test_shipped_scenarios_parse(self):
        d = os.path.join(os.path.dirname(os.path.abspath(__file__)), "scenarios")
        for f in os.listdir(d):
            with open(os.path.join(d, f), encoding="utf-8") as fh:
                client.parse_scenario(f, fh.read())


class MatchTest(unittest.TestCase):
    def test_matches(self):
        self.assertTrue(client.matches("LegacyScreen", "x.y.LegacyScreen\n"))
        self.assertTrue(client.matches("^15$", " 15 \n"))
        self.assertFalse(client.matches("^15$", "150"))
        self.assertTrue(client.matches("a.b", "a\nb"))
        self.assertFalse(client.matches("true", "none"))

    def test_log_hit(self):
        self.assertEqual(client.log_hit("Stack.*Error", "a\n  java.lang.StackOverflowError: x\nb"), "java.lang.StackOverflowError: x")
        self.assertIsNone(client.log_hit("Stack.*Error", "fine"))

    def test_control_answers(self):
        self.assertTrue(client.bad_control_answer("error: java.lang.NullPointerException"))
        self.assertTrue(client.bad_control_answer("use: no block in reach"))
        self.assertFalse(client.bad_control_answer("ok"))
        self.assertFalse(client.bad_control_answer("useitem -> SUCCESS"))

    def test_timeout_and_format(self):
        sc = client.parse_scenario("x", "holduse 300\nwait 40\ncmd /x")
        self.assertEqual([client.step_timeout(s) for s in sc.steps], [90, 64, 60])
        self.assertEqual(client.format_result("a", "PASS"), "a                      PASS")


class JarTest(unittest.TestCase):
    def test_find_jar(self):
        with tempfile.TemporaryDirectory() as d:
            os.makedirs(os.path.join(d, "sub"))
            p = os.path.join(d, "sub", "Thaumcraft-1.7.10-4.2.jar")
            open(p, "w").close()
            open(os.path.join(d, "readme.txt"), "w").close()
            self.assertEqual(client.find_jar("thaumcraft-1.7.10", [d]), p)
            self.assertIsNone(client.find_jar("nothing", [d]))


if __name__ == "__main__":
    unittest.main()
