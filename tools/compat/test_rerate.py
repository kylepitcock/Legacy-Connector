import argparse, json, os, pathlib, sys, tempfile, unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import rerate


def row(name, mc="1.12.2", status="fails", source="corpus", notes=""):
    return {"name": name, "version": "1", "mc": mc, "status": status, "source": source, "notes": notes, "date": "2026-10-27"}


class SelectTest(unittest.TestCase):
    def test_filters(self):
        mods = [row("A"), row("B", status="works"), row("C", mc="1.7.10"), row("D", source="probe"), {**row("E"), "source": None}]
        names = lambda **k: [r["name"] for r in rerate.select(mods, **k)]
        self.assertEqual(names(mcs=["1.12.2"], statuses=["fails"]), ["A", "D", "E"])
        self.assertEqual(names(mcs=["1.12.2"], statuses=["fails"], sources=["corpus", "probe"]), ["A", "D"])
        self.assertEqual(names(mcs=[], statuses=["works"]), ["B"])
        self.assertEqual(names(mcs=[], statuses=["fails"], sources=["none"]), ["E"])
        self.assertEqual(names(mcs=[], statuses=[], name="c"), ["C"])


class Fixture(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        base = pathlib.Path(self.tmp.name)
        for folder, names in {"test-mods-1122": ["Alpha-1.12.2-1.0.jar", "LibCore-1.12.2-2.0.jar", "Gamma-1.12.2.jar"],
                              "test-mods-147": ["Delta-1.4.7.jar"]}.items():
            (base / folder).mkdir()
            for n in names:
                (base / folder / n).write_bytes(b"")
        self.index = rerate.jar_index([str(base / "test-mods-1122"), str(base / "test-mods-147")])

    def tearDown(self):
        self.tmp.cleanup()


class ResolveTest(Fixture):
    def test_jar_by_version(self):
        self.assertTrue(rerate.find_jar("1.12.2", "alpha", self.index)[0].endswith("Alpha-1.12.2-1.0.jar"))
        self.assertIsNone(rerate.find_jar("1.12.2", "delta", self.index)[0])   # only a 1.4.7 jar
        self.assertIsNone(rerate.find_jar("1.12.2", "zeta", self.index)[0])

    def test_dependencies(self):
        r = rerate.resolve_row(row("Alpha", notes="Fails during preInit: java.lang.NoClassDefFoundError: libcore/api/Thing"), self.index)
        self.assertEqual([os.path.basename(d) for d in r["deps"]], ["LibCore-1.12.2-2.0.jar"])
        self.assertIsNone(r["skip"])
        r2 = rerate.resolve_row(row("Alpha", notes="Mod alpha requires Zeta, version 3"), self.index)
        self.assertIn("missing dependency Zeta", r2["skip"])
        self.assertIsNone(rerate.resolve_row(row("Alpha", notes="requires forge"), self.index)["skip"])
        self.assertTrue(rerate.resolve_row(row("Nothing"), self.index)["skip"].startswith("no jar"))

    def test_extra_deps(self):
        r = rerate.resolve_row(row("Gamma"), self.index, {"gamma": ["LibCore"]})
        self.assertEqual(len(r["deps"]), 1)


class GroupTest(Fixture):
    def test_groups_split_by_version_and_size(self):
        resolved = [rerate.resolve_row(row(n), self.index) for n in ("Alpha", "Gamma", "Delta", "Nothing")]
        resolved.append(rerate.resolve_row(row("Delta", mc="1.4.7"), self.index))
        groups, skipped = rerate.plan_groups(resolved, 2)
        self.assertEqual([r["row"]["name"] for r in skipped], ["Delta", "Nothing"])   # Delta has no 1.12.2 jar
        self.assertEqual(sorted((g["mc"], len(g["rows"])) for g in groups), [("1.12.2", 2), ("1.4.7", 1)])
        text = rerate.format_plan(groups, skipped)
        self.assertIn("3 mods in 2 groups, 2 not testable", text)
        self.assertIn("not tested: Nothing", text)

    def test_shared_dependency_listed_once(self):
        rows = [rerate.resolve_row(row(n, notes="NoClassDefFoundError: libcore/X"), self.index) for n in ("Alpha", "Gamma")]
        groups, _ = rerate.plan_groups(rows, 5)
        self.assertEqual(len(groups), 1)
        self.assertEqual(len(groups[0]["jars"]), 3)   # LibCore once


class RamTest(unittest.TestCase):
    def test_waits_then_continues(self):
        values = iter([2.0, 3.0, 7.0])
        slept, msgs = [], []
        rerate.wait_for_ram(6, lambda: next(values), slept.append, msgs.append, poll=1)
        self.assertEqual(slept, [1, 1])
        self.assertEqual(len(msgs), 1)
        rerate.wait_for_ram(6, lambda: None, slept.append, msgs.append)   # unknown counts as enough


class RunTest(Fixture):
    def test_summary_with_fake_runner(self):
        d = pathlib.Path(self.tmp.name)
        data = d / "mods.json"
        data.write_text(json.dumps({"mods": [row("Alpha"), row("Gamma")]}), encoding="utf-8")
        resolved = [rerate.resolve_row(row(n), self.index) for n in ("Alpha", "Gamma")]
        groups, skipped = rerate.plan_groups(resolved, 1)
        calls = []

        def runner(g, name, port):
            calls.append(name)
            return (d / "r.txt", d / "l.txt") if g["rows"][0]["row"]["name"] == "Alpha" else (None, None)

        def rate(report, log, label, mc, date, work):
            m = json.loads(work.read_text(encoding="utf-8"))
            m["mods"].append({"name": "Alpha", "version": "1", "mc": mc, "source": "client", "date": date, "status": "works", "notes": label})
            work.write_text(json.dumps(m), encoding="utf-8")
            return "1 mods rated"

        args = argparse.Namespace(dry_run=True, min_free_gb=6)
        text = rerate.run_all(groups, skipped, "2026-11-24", args, runner, free=lambda: 99, log=lambda *_: None, data_file=data, rate_fn=rate)
        self.assertEqual(len(calls), 2)
        self.assertIn("| Alpha | 1.12.2 | fails (corpus) | works (changed) |", text)
        self.assertIn("1 changed, 0 unchanged", text)
        self.assertIn("not tested: Gamma [1.12.2]: the group's run produced no log", text)
        self.assertEqual(len(json.loads(data.read_text(encoding="utf-8"))["mods"]), 2)   # dry run left the file alone


if __name__ == "__main__":
    unittest.main()
