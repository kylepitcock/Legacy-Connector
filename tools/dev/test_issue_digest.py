#!/usr/bin/env python3
"""Unit tests of issue_digest.py: python -m unittest tools/dev/test_issue_digest.py (or from tools/dev)."""
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import issue_digest as d  # noqa: E402

PREFIX = "[13Nov2026 12:00:00.000] [Render thread/{lvl}] [{logger}]: {msg}"


def log(lvl, logger, msg):
    return PREFIX.format(lvl=lvl, logger=logger, msg=msg)


def analyze(lines):
    a = d.LogAnalyzer()
    for n, line in enumerate(lines, 1):
        a.feed(n, line)
    a.finish()
    return a


class FrameCleaning(unittest.TestCase):
    def test_transformer_and_jar_noise(self):
        raw = ("TRANSFORMER/neoforge@21.1.266/net.minecraft.world.Foo.bar(Foo.java:12) "
               "~[neoforge-21.1.266.jar%23123!/:?] {re:mixin,re:classloading}")
        self.assertEqual(d.clean_frame(raw), "net.minecraft.world.Foo.bar(Foo.java:12)")

    def test_jdk_module_prefix(self):
        self.assertEqual(d.clean_frame("java.base/java.util.ArrayList.get(ArrayList.java:427) ~[?:?] {}"),
                         "java.util.ArrayList.get(ArrayList.java:427)")

    def test_layer_prefix_and_plain(self):
        self.assertEqual(d.clean_frame("LAYER PLUGIN/legacyconnector@0.4.14/org.k.Foo.run(Foo.java:1)"),
                         "org.k.Foo.run(Foo.java:1)")
        self.assertEqual(d.clean_frame("buildcraft.lib.Foo.x(Foo.java:9) ~[bc.jar%23184!/:?] {re:classloading}"),
                         "buildcraft.lib.Foo.x(Foo.java:9)")

    def test_collapse_repeated_frames(self):
        frames = ["a.B.c(B.java:1)"] * 500 + ["x.Y.z(Y.java:2)"]
        self.assertEqual(d.collapse(frames), ["a.B.c(B.java:1)  (x500)", "x.Y.z(Y.java:2)"])


class ExceptionGrouping(unittest.TestCase):
    def trace(self, line_prefix, first_frame, count_cause=True):
        lines = [log("ERROR", "Legacy Connector", "Failed: " + "x")]
        lines += [line_prefix,
                  "\tat java.base/java.util.Objects.requireNonNull(Objects.java:233) ~[?:?] {}",
                  f"\tat TRANSFORMER/mod@1.0/{first_frame} ~[mod.jar%231!/:?] {{}}",
                  "\tat org.other.Caller.call(Caller.java:5)"]
        if count_cause:
            lines += ["Caused by: java.lang.IllegalStateException: root cause",
                      "\tat some.Deep.thing(Deep.java:7)", "\t... 12 more"]
        return lines

    def test_same_exception_and_frame_group_together(self):
        lines = []
        for i in range(3):
            lines += self.trace("java.lang.RuntimeException: boom " + str(i), "com.mod.Thing.go(Thing.java:%d)" % (10 + i))
        lines += self.trace("java.lang.RuntimeException: other", "com.mod.Else.go(Else.java:3)")
        a = analyze(lines)
        groups = sorted(a.groups.values(), key=lambda g: -g.count)
        self.assertEqual(len(groups), 2)
        self.assertEqual(groups[0].count, 3)
        self.assertEqual(groups[0].frame, "com.mod.Thing.go(Thing.java:10)")  # first non-JDK frame, noise stripped
        self.assertEqual(groups[0].first_line, 2)
        self.assertEqual(groups[0].causes[0][0], "java.lang.IllegalStateException")
        self.assertEqual(groups[1].count, 1)

    def test_message_line_with_exception_start_and_unprefixed_trace(self):
        lines = [log("ERROR", "x", "java.lang.NullPointerException: Cannot invoke \"a.B.c()\""),
                 "\tat a.Caller.run(Caller.java:1)",
                 "java.lang.IllegalArgumentException: bare",
                 "\tat b.Other.run(Other.java:2)"]
        a = analyze(lines)
        self.assertEqual({g.cls for g in a.groups.values()},
                         {"java.lang.NullPointerException", "java.lang.IllegalArgumentException"})

    def test_header_without_frames_is_not_counted(self):
        a = analyze([log("INFO", "x", "java.lang.RuntimeException: just text"), log("INFO", "x", "next")])
        self.assertEqual(len(a.groups), 0)

    def test_stack_overflow_frames_collapse(self):
        lines = [log("ERROR", "x", "java.lang.StackOverflowError")]
        lines += ["\tat a.Rec.loop(Rec.java:5)"] * 1024
        lines += ["\tat a.Start.main(Start.java:1)"]
        a = analyze(lines)
        g = next(iter(a.groups.values()))
        self.assertEqual(d.fmt_runs(g.runs, 6), ["a.Rec.loop(Rec.java:5)  (x1024)", "a.Start.main(Start.java:1)"])

    def test_suppressed_frames_do_not_pollute(self):
        lines = ["java.lang.RuntimeException: top", "\tat a.A.a(A.java:1)",
                 "\tSuppressed: java.io.IOException: closing", "\t\tat z.Z.z(Z.java:9)",
                 "Caused by: java.lang.Error: root", "\tat r.R.r(R.java:2)"]
        g = next(iter(analyze(lines).groups.values()))
        self.assertEqual([f for f, _ in g.runs], ["a.A.a(A.java:1)"])
        self.assertEqual(g.causes[0][2], ["r.R.r(R.java:2)"])


class LogLines(unittest.TestCase):
    def test_version_patterns_loggers_and_mixin(self):
        lines = [log("INFO", "Legacy Connector", "Legacy Connector 0.4.14 starting"),
                 log("INFO", "Legacy Connector", "Legacy mods loaded in 1452 ms (2 failed)"),
                 log("ERROR", "Legacy Connector", "Legacy mod abc (1.12.2, from x.jar) failed during init: java.lang.Error: x"),
                 log("WARN", "mixin", "Mixin apply for mod foo failed foo.mixins.json:Bar -> baz.Qux: boom"),
                 log("WARN", "mixin", "Mixin apply for mod foo failed foo.mixins.json:Bar -> baz.Qux: boom"),
                 log("INFO", "Legacy Connector", "1.12.2 content registered: 72 blocks")]
        a = analyze(lines)
        self.assertEqual(a.version, "0.4.14")
        self.assertEqual(len(a.matches["failed during"]), 1)
        self.assertEqual(a.loggers[("Legacy Connector", "ERROR")], 1)
        self.assertEqual(a.loggers[("mixin", "WARN")], 2)
        self.assertEqual(next(iter(a.mixin.values()))[0], 2)

    def test_render_stays_small(self):
        lines = []
        for i in range(40):
            lines += ["java.lang.RuntimeException: m", "\tat p.C%d.m(C.java:1)" % i]
        out = d.render_log(analyze(lines), d.PARAM_STEPS[0])
        self.assertLessEqual(len([l for l in out if l.startswith("  x")]), 25)
        self.assertTrue(any("more groups" in l for l in out))


class Files(unittest.TestCase):
    def test_crash_report_and_dedupe_signature(self):
        text = "\n".join([
            "---- Minecraft Crash Report ----", "", "Time: now", "Description: charTyped event handler", "",
            "java.lang.IllegalStateException: unbound value",
            "\tat net.minecraft.core.Holder$Reference.key(Holder.java:142) ~[client.jar%23179!/:?] {re:mixin}",
            "\tat java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387) ~[?:?] {}", "",
            "-- Head --", "Thread: Render thread", "Stacktrace:", "\tat x.Y.z(Y.java:1)", "",
            "-- System Details --", "Details:", "\tMod List: ", "\t\ta.jar | A | a | 1 | Valid", "\t\tb.jar | B | b | 1 | Valid"])
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "crash-1.txt")
            with open(path, "w", encoding="utf-8") as f:
                f.write(text)
            c = d.analyze_crash(path)
            self.assertEqual(d.classify(path, "crash-1.txt"), "crash report")
        self.assertEqual(c["desc"], "charTyped event handler")
        self.assertEqual(c["runs"][0][0], "net.minecraft.core.Holder$Reference.key(Holder.java:142)")
        self.assertEqual(c["mods"], 2)
        self.assertEqual(c["sections"]["Head"][0], "Thread: Render thread")

    def test_report_sections(self):
        text = "\n".join([
            "Legacy Connector 0.4.14 legacy mod report - 2026", "====", "",
            "foo.jar  [1.12.2, links against x (3 classes)]", "  mod foo -> foo  (Foo 1)", "  note: Stripped 2",
            "  2 unresolved link(s):", "  Missing shim classes (legacy API not implemented yet) [2]",
            "    - a.b.C   <- foo.Main", "    - a.b.D   <- foo.Main (+3 more)",
            "  Unresolved external classes (optional mod integrations, usually harmless) [1]", "    - x.Y   <- foo.Z", "",
            "Bundled libraries:", "  trove: loaded"])
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "legacyconnector-report.txt")
            with open(path, "w", encoding="utf-8") as f:
                f.write(text)
            r = d.analyze_report(path)
        self.assertEqual(len(r["jars"]), 1)
        out = "\n".join(d.render_report(r, d.PARAM_STEPS[0]))
        self.assertIn("a.b.C", out)
        self.assertIn("external-missing 1", out)
        self.assertNotIn("trove", out)

    def test_issue_form_fields_and_attachments(self):
        body = ("### Mod\n\nthermal\n\n### Result\n\nPartially works\n\n### What works and what doesn't\n\n"
                "<img width=\"1\" src=\"https://x/y.png\" />\ntext\n\n### Other mods installed\n\n_No response_\n\n"
                "### latest.log (attach the file)\n\n[a.log](https://github.com/user-attachments/files/123/latest.log)")
        fields = d.pick_fields(d.form_fields(body))
        self.assertEqual(fields[0], ("Mod", "thermal"))
        self.assertIn("[image]", dict(fields)["What works / not"])
        self.assertNotIn("Other mods", dict(fields))
        self.assertEqual(d.attachments([body]), [("https://github.com/user-attachments/files/123/latest.log", "latest.log")])


if __name__ == "__main__":
    unittest.main()
