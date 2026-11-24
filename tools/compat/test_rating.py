#!/usr/bin/env python3
"""Tests of the probe's rating rules: soft/hard reachability, the kind of a record (engine, native, soft, ...), merge precedence and the engine table.

run: python tools/compat/test_rating.py     (the reachability test compiles a tiny jar with the javac of JAVA_HOME or PATH and is skipped without one)
"""
import json
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import card_numbers  # noqa: E402
import engine_mods  # noqa: E402
import probe  # noqa: E402
import soft_links  # noqa: E402


def javac():
    home = os.environ.get("JAVA_HOME")
    for c in ([pathlib.Path(home) / "bin" / "javac"] if home else []) + [pathlib.Path(shutil.which("javac") or "")]:
        for cand in (c, c.with_suffix(".exe")):
            if cand.is_file():
                return str(cand)
    return None


SOURCES = {
    # stand-ins that are compiled against and then left out of the jar: they are the "unresolved" classes
    "net/minecraftforge/fml/common/Mod.java": "package net.minecraftforge.fml.common; public @interface Mod { String value(); }",
    "net/minecraft/Missing.java": "package net.minecraft; public class Missing { public static void hard() {} }",
    "other/mod/api/Thing.java": "package other.mod.api; public class Thing { public static void call() {} public static void optional() {} }",
    # the jar: Main (@Mod) -> Used (hard symbol) and -> compat.JeiPlugin (integration class, optional symbol); Orphan nothing reaches
    "a/Main.java": "package a; @net.minecraftforge.fml.common.Mod(\"a\") public class Main { public void go() { new Used().run(); new a.compat.JeiPlugin().register(); } }",
    "a/Used.java": "package a; public class Used { public void run() { net.minecraft.Missing.hard(); } }",
    "a/Orphan.java": "package a; public class Orphan { public void run() { other.mod.api.Thing.call(); } }",
    "a/compat/JeiPlugin.java": "package a.compat; public class JeiPlugin { public void register() { other.mod.api.Thing.optional(); } }",
}


@unittest.skipUnless(javac(), "no javac")
class Reachability(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = pathlib.Path(tempfile.mkdtemp(prefix="rating-test-"))
        src, out = cls.tmp / "src", cls.tmp / "classes"
        files = []
        for name, text in SOURCES.items():
            f = src / name
            f.parent.mkdir(parents=True, exist_ok=True)
            f.write_text(text, encoding="utf-8")
            files.append(str(f))
        subprocess.run([javac(), "-d", str(out), "-proc:none", *files], check=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        cls.jar = cls.tmp / "mod.jar"
        with zipfile.ZipFile(cls.jar, "w") as z:
            for f in out.rglob("*.class"):
                rel = f.relative_to(out).as_posix()
                if rel.startswith(("a/",)):
                    z.write(f, rel)

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.tmp, ignore_errors=True)

    def entry(self, syms):
        return soft_links.scan_entry(self.jar, syms)

    def test_symbol_in_class_reached_from_mod_is_hard(self):
        e = self.entry([("method", "net/minecraft/Missing.hard()V")])
        self.assertEqual(e["syms"][0]["holderClass"], "a/Used")
        self.assertEqual(soft_links.soft_verdict(e)[:2], (False, 1))

    def test_symbol_in_class_nothing_reaches_is_soft(self):
        e = self.entry([("method", "other/mod/api/Thing.call()V")])
        self.assertEqual(e["syms"][0]["holderClass"], "a/Orphan")
        self.assertEqual(e["syms"][0]["reach"], 0)
        self.assertEqual(soft_links.soft_verdict(e)[:2], (True, 0))

    def test_symbol_only_in_a_reachable_integration_class_is_soft(self):
        e = self.entry([("method", "other/mod/api/Thing.optional()V")])
        self.assertEqual(e["syms"][0]["holderClass"], "a/compat/JeiPlugin")
        self.assertEqual(e["syms"][0]["reach"], 1)
        self.assertTrue(soft_links.soft_verdict(e)[0])

    def test_one_hard_symbol_makes_the_jar_hard(self):
        e = self.entry([("method", "other/mod/api/Thing.call()V"), ("method", "net/minecraft/Missing.hard()V")])
        self.assertEqual(soft_links.soft_verdict(e)[:2], (False, 1))


class Verdict(unittest.TestCase):
    def sym(self, **kw):
        base = {"k": "class", "s": "x/Y", "area": "mc", "dep": "Minecraft/Forge", "depid": None, "holders": 1, "reach": 0, "integ": 0, "mixin": 0,
                "super": 0, "reachsuper": 0, "reachnoninteg": 0, "holderClass": "a/B"}
        base.update(kw)
        return base

    def test_mixin_symbols_are_always_hard(self):
        e = {"syms": [self.sym(mixin=1)], "deps": {}}
        self.assertFalse(soft_links.soft_verdict(e)[0])

    def test_other_mods_api_is_hard_only_when_that_mod_is_mandatory(self):
        s = self.sym(area="dep", dep="JEI", depid="jei", reachnoninteg=1)
        self.assertTrue(soft_links.soft_verdict({"syms": [s], "deps": {"jei": False}})[0])
        self.assertFalse(soft_links.soft_verdict({"syms": [s], "deps": {"jei": True}})[0])
        self.assertFalse(soft_links.soft_verdict({"syms": [s], "deps": {}})[0])   # not declared at all: not known to be optional

    def test_a_jar_without_symbols_is_not_soft(self):
        self.assertFalse(soft_links.soft_verdict({"syms": [], "deps": {}})[0])


class Kinds(unittest.TestCase):
    def rec(self, **kw):
        r = {"jar": "a.jar", "mc": "1.20.1", "mods": [{"id": "a", "name": "A", "version": "1"}], "result": "links"}
        r.update(kw)
        return r

    def test_kind_precedence(self):
        self.assertEqual(probe.kind(self.rec()), "links")
        self.assertEqual(probe.kind(self.rec(result="unresolved", missing=["net.minecraft.X"])), "gap_shim")
        self.assertEqual(probe.kind(self.rec(result="unresolved", missing=["net.minecraft.X"], soft=True)), "soft")
        self.assertEqual(probe.kind(self.rec(result="unresolved", soft=True, hasNative=True)), "native")
        self.assertEqual(probe.kind(self.rec(result="unresolved", soft=True, hasNative=True, engine="x")), "engine")
        self.assertEqual(probe.kind(self.rec(result="obsolete", hasNative=True, nativeLibrary=True)), "native")
        self.assertEqual(probe.kind(self.rec(result="obsolete", notTranslated="Fabric")), "obsolete")

    def test_engine_annotation_uses_the_loader_table_per_version(self):
        recs = [self.rec(mc="1.20.1", mods=[{"id": "embeddium", "name": "Embeddium", "version": "1"}]),
                self.rec(mc="1.7.10", mods=[{"id": "embeddium", "name": "x", "version": "1"}]),
                self.rec(mc="1.7.10", result="obsolete", mods=[{"id": "FastCraft", "name": "FastCraft", "version": "1"}]),
                self.rec(mc="1.7.10", mods=[{"id": "inventorytweaks", "name": "Inventory Tweaks", "version": "1"}])]
        probe.annotate_engine(recs)
        self.assertEqual([bool(r.get("engine")) for r in recs], [True, False, True, False])

    def test_engine_table_names_and_replacement(self):
        row = engine_mods.find("1.7.10", mod_names=["OptiFine HD"])
        self.assertEqual(row["id"], "optifine")
        self.assertIn("Sodium", row["replacement"])
        self.assertIsNone(engine_mods.find("1.7.10", mod_names=["Journeymap"]))
        self.assertIsNone(engine_mods.find("1.7.10", mod_ids=["fastcraft", "other"]))


class Merge(unittest.TestCase):
    def setUp(self):
        self.tmp = pathlib.Path(tempfile.mkdtemp(prefix="rating-merge-"))
        probe.DATA = self.tmp / "mods.json"

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def data(self, mods):
        probe.DATA.write_text(json.dumps({"mods": mods, "hidden": ["Hidden Engine"], "hidden_by_mc": {}}), encoding="utf-8")

    def rec(self, name, **kw):
        r = {"jar": name + ".jar", "mc": "1.20.1", "mods": [{"id": name.lower(), "name": name, "version": "1"}], "result": "links"}
        r.update(kw)
        return r

    def statuses(self, data):
        return {e["name"]: (e["status"], e["source"], e.get("nativeUrl")) for e in data["mods"]}

    def test_soft_loads_native_and_engine_statuses(self):
        self.data([])
        recs = [self.rec("Soft", result="unresolved", unresolved=1, missing=["x.Y"], soft=True, softOff=["JEI"]),
                self.rec("Hard", result="unresolved", unresolved=1, missing=["net.minecraft.Z"], soft=False),
                self.rec("Nat", hasNative=True, nativeUrl="https://modrinth.com/mod/nat"),
                self.rec("Eng", engine="e", engineReplacement="Sodium", engineReason="renderer")]
        data, added, _, _ = probe.merge(recs, "1.20.1", "2026-11-23", "t")
        st = self.statuses(data)
        self.assertEqual(st["Soft"][0], "loads")
        self.assertEqual(st["Hard"][0], "fails")
        self.assertEqual(st["Nat"], ("native", "probe", "https://modrinth.com/mod/nat"))
        self.assertEqual(st["Eng"][0], "engine")
        self.assertIn("JEI", [e for e in data["mods"] if e["name"] == "Soft"][0]["notes"])

    def test_a_better_source_is_never_overwritten_but_gets_the_native_hint(self):
        self.data([{"name": "Nat", "version": "1", "mc": "1.20.1", "source": "client", "date": "2026-11-01", "status": "works", "notes": "played"}])
        data, _, _, skipped = probe.merge([self.rec("Nat", hasNative=True, nativeUrl="https://modrinth.com/mod/nat")], "1.20.1", "2026-11-23", "t")
        self.assertEqual(skipped, 1)
        e = data["mods"][0]
        self.assertEqual((e["status"], e["source"], e["nativeUrl"]), ("works", "client", "https://modrinth.com/mod/nat"))

    def test_engine_entry_is_added_next_to_a_corpus_result_and_listed_although_hidden(self):
        self.data([{"name": "Hidden Engine", "version": "1", "mc": "1.7.10", "source": "corpus", "date": "d", "status": "fails", "notes": "n"}])
        r = self.rec("Hidden Engine", mc="1.7.10", result="obsolete", engine="h", engineReason="patch")
        data, added, _, _ = probe.merge([r], "1.7.10", "2026-11-23", "t")
        self.assertEqual(sorted((e["source"], e["status"]) for e in data["mods"]), [("corpus", "fails"), ("probe", "engine")])


class Cards(unittest.TestCase):
    def test_native_and_engine_are_left_out_of_the_card(self):
        mods = [{"name": "A", "mc": "1.20.1", "source": "probe", "status": "loads"}, {"name": "B", "mc": "1.20.1", "source": "probe", "status": "fails"},
                {"name": "C", "mc": "1.20.1", "source": "probe", "status": "native"}, {"name": "D", "mc": "1.20.1", "source": "probe", "status": "engine"},
                {"name": "E", "mc": "1.20.1", "source": "client", "status": "works"}, {"name": "E", "mc": "1.20.1", "source": "probe", "status": "engine"},
                {"name": "F", "mc": "1.20.1", "source": "corpus", "status": "loads"}, {"name": "F", "mc": "1.20.1", "source": "probe", "status": "engine"}]
        c = card_numbers.cards(mods)["1.20.1"]
        self.assertEqual((c["mods"], c["start_and_run_pct"], c["confirmed_working"], c["native"], c["engine"]), (3, 67, 1, 1, 2))


if __name__ == "__main__":
    unittest.main()
