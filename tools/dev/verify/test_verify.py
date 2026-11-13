import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import verify

LOG = """[13Nov2026 13:25:50.000] [main/INFO] [LegacyConnector/]: Legacy mods loaded in 5123 ms (2 failed)
[13Nov2026 13:25:51.000] [main/INFO] [LegacyConnector/Content/]: Legacy content: 3 mods, 120 items, 85 blocks, 12 entity types, 4 spawn eggs
  thaumcraft   100 items    80 blocks    10 entities
[13Nov2026 13:25:52.000] [Server thread/INFO] [x]: 1.12.2 item placement: 70 of 80 placed (3 only while sneaking: [], failing: []
[13Nov2026 13:25:53.000] [Server thread/INFO] [x]: BLOCK SELF TEST: 85 placed, 80 still there after 60 ticks, 10 activated, 3 with problems
  foo: bar
[13Nov2026 13:25:54.000] [Server thread/INFO] [x]: ENTITY SELF TEST: 9 alive, 1 removed, 1 failed, 2 not spawned of 12 types
[13Nov2026 13:25:54.515] [Server thread/INFO] [x]: SILENT FAILURE CHECKS: 6 passed, 1 failed
java.lang.VerifyError: bad
"""


class ParseTest(unittest.TestCase):
    def test_parse(self):
        with tempfile.TemporaryDirectory() as d:
            open(os.path.join(d, "crash-1.txt"), "w").close()
            m = verify.parse_log(LOG, d)
        self.assertEqual(m["mods_failed"], 2)
        self.assertEqual((m["blocks_placed"], m["blocks_ok"], m["blocks_problems"]), (85, 80, 3))
        self.assertEqual((m["entities_alive"], m["entities_failed"], m["entities_not_spawned"]), (9, 1, 2))
        self.assertEqual(m["silent_failed"], 1)
        self.assertEqual((m["placement_placed"], m["placement_total"]), (70, 80))
        self.assertEqual((m["content_items"], m["content_blocks"], m["content_entities"]), (120, 85, 12))
        self.assertEqual((m["crash_reports"], m["verify_errors"], m["reached_selftest"]), (1, 1, 1))

    def test_no_selftest(self):
        m = verify.parse_log("nothing here", None)
        self.assertEqual(m["reached_selftest"], 0)
        self.assertNotIn("blocks_ok", m)


class CompareTest(unittest.TestCase):
    BASE = {"mods_failed": 0, "blocks_ok": 100, "blocks_problems": 1, "reached_selftest": 1}

    def test_pass(self):
        self.assertEqual(verify.compare(self.BASE, {"mods_failed": 0, "blocks_ok": 98, "blocks_problems": 1, "reached_selftest": 1})[0], "PASS")

    def test_bad_up(self):
        st, regs = verify.compare(self.BASE, {"mods_failed": 1, "blocks_ok": 100, "blocks_problems": 1, "reached_selftest": 1})
        self.assertEqual(st, "FAIL")
        self.assertIn("mods_failed", regs[0])

    def test_good_down_more_than_two_percent(self):
        self.assertEqual(verify.compare(self.BASE, {"blocks_ok": 97, "reached_selftest": 1})[0], "FAIL")

    def test_entities_alive_is_noisy(self):
        base = {"entities_alive": 100, "reached_selftest": 1}
        self.assertEqual(verify.compare(base, {"entities_alive": 92, "reached_selftest": 1})[0], "PASS")
        self.assertEqual(verify.compare(base, {"entities_alive": 85, "reached_selftest": 1})[0], "FAIL")

    def test_not_reached(self):
        self.assertEqual(verify.compare(self.BASE, {"reached_selftest": 0})[0], "FAIL")
        self.assertEqual(verify.compare(None, {"reached_selftest": 0})[0], "FAIL")

    def test_new(self):
        self.assertEqual(verify.compare(None, {"reached_selftest": 1})[0], "NEW")


if __name__ == "__main__":
    unittest.main()
