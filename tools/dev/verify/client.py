#!/usr/bin/env python3
"""Scripted client scenarios for tools/dev/verify-client.sh (standard library only).

  client.py run <scenario> [...]       run scenarios (tools/dev/verify/scenarios/<name>.txt) in a real client, one at a time

A scenario file: header lines `mods: <version-folder> <jar fragment>` and `world: <preset>`, then one step per line:
  any RemoteControl verb as is (cmd /..., wait n, slot n, look yaw pitch, use, useitem, holduse n, creative, ...)
  expect <regex> :: <JavaScript>   evaluate the JavaScript in the game; fails unless the answer matches the regex (re.search); retried for 15 s
  shot <name>                      screenshot, copied to build/devtools/verify-client/<scenario>/<name>.png
  logabsent <regex>                fails when logs/latest.log of the game contains the pattern
  # comment
"""
import os
import re
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
JAR_ROOT = os.environ.get("JAR_ROOT", "Y:/Programming/Minecraft/Retrofit")
SEARCH = ("test-mods test-mods2 test-mods3 test-mods-1122 test-mods-147 test-mods-152 test-mods-164 test-mods-1710-extra test-mods-1201-a "
          "test-mods-1201-terramity test-mods-b173 run-subtest/mods run-subtest/mods.aside-20261113 run-screenshots/mods "
          "run-screenshots/mods.aside-20261113").split()
EXT = (".jar", ".zip", ".litemod")
# Iris Veil Compat crashes the mixin phase without the Veil library, which a real pack gets through Create Aeronautics/Sable
PERF_SKIP = ("irisveil",)
WORLD_TIMEOUT = 480
EXPECT_RETRY = 15


class ScenarioError(Exception):
    pass


class Step:
    def __init__(self, kind, line, no, text="", pattern=None, js=None):
        self.kind, self.line, self.no, self.text, self.pattern, self.js = kind, line, no, text, pattern, js

    def __repr__(self):
        return "Step(%s, %r)" % (self.kind, self.line)


class Scenario:
    def __init__(self, name, mods, world, steps):
        self.name, self.mods, self.world, self.steps = name, mods, world, steps


def _check_regex(name, no, pat):
    try:
        re.compile(pat)
    except re.error as e:
        raise ScenarioError("%s:%d: bad regex: %s" % (name, no, e))


def parse_scenario(name, text):
    mods, world, steps = [], "minecraft:flat", []
    for no, raw in enumerate(text.splitlines(), 1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        low = line.lower()
        if low.startswith("mods:"):
            parts = line[5:].strip().split(None, 1)
            if len(parts) != 2:
                raise ScenarioError("%s:%d: mods needs '<version-folder> <jar fragment>'" % (name, no))
            mods.append((parts[0], parts[1].strip()))
        elif low.startswith("world:"):
            world = line[6:].strip()
        elif low.startswith("expect "):
            rest = line[7:]
            if " :: " not in rest:
                raise ScenarioError("%s:%d: expect needs '<regex> :: <JavaScript>'" % (name, no))
            pat, js = rest.split(" :: ", 1)
            pat, js = pat.strip(), js.strip()
            if not pat or not js:
                raise ScenarioError("%s:%d: empty regex or JavaScript" % (name, no))
            _check_regex(name, no, pat)
            steps.append(Step("expect", line, no, pattern=pat, js=js))
        elif low.startswith("logabsent "):
            pat = line[10:].strip()
            _check_regex(name, no, pat)
            steps.append(Step("logabsent", line, no, pattern=pat))
        elif low.startswith("shot "):
            steps.append(Step("shot", line, no, text=line[5:].strip().split()[0]))
        else:
            steps.append(Step("control", line, no, text=line))
    if not steps:
        raise ScenarioError("%s: no steps" % name)
    return Scenario(name, mods, world, steps)


def matches(pattern, answer):
    return re.search(pattern, answer.strip(), re.S) is not None


def log_hit(pattern, log_text):
    """The first line of the log matching the pattern, or None."""
    rx = re.compile(pattern)
    for l in log_text.splitlines():
        if rx.search(l):
            return l.strip()
    return None


def step_timeout(step):
    """Seconds a step may take to answer: a minute plus the ticks it holds keys or waits."""
    m = re.match(r"(wait|holduse|walk|attack|sneak|jump)\s+(?:\w+\s+)?(\d+)", step.line)
    return 60 + (int(m.group(2)) / 10 if m else 0)


def bad_control_answer(answer):
    a = answer.strip()
    return a.startswith(("error:", "unknown command", "use: no block", "screenshot failed", "holdkey: no key"))


def format_result(name, status, detail=""):
    return "%-22s %s%s" % (name, status, (" " + detail) if detail else "")


def find_jar(fragment, roots=None):
    frag = fragment.lower()
    for r in (roots or [os.path.join(JAR_ROOT, s) for s in SEARCH]):
        if not os.path.isdir(r):
            continue
        hits = []
        for dp, _, fn in os.walk(r):
            for f in fn:
                if f.lower().endswith(EXT) and frag in f.lower():
                    hits.append(os.path.join(dp, f))
        if hits:
            return sorted(hits)[0]
    return None


# ---- runner ----

class Client:
    def __init__(self, game_dir):
        self.dir = game_dir
        self.cin = os.path.join(game_dir, "control", "in")
        self.cout = os.path.join(game_dir, "control", "out")
        self.proc = None
        self.n = 0

    def send(self, line, timeout):
        self.n += 1
        cid = "v%04d" % self.n
        os.makedirs(self.cin, exist_ok=True)
        tmp = os.path.join(self.cin, cid + ".tmp")
        with open(tmp, "w", encoding="utf-8") as f:
            f.write(line)
        os.replace(tmp, os.path.join(self.cin, cid + ".cmd"))
        ans = os.path.join(self.cout, cid + ".txt")
        end = time.time() + timeout
        while time.time() < end:
            if os.path.isfile(ans):
                time.sleep(0.05)
                with open(ans, encoding="utf-8", errors="replace") as f:
                    return f.read().strip()
            if self.proc is not None and self.proc.poll() is not None:
                raise RuntimeError("the game exited (code %s)" % self.proc.returncode)
            time.sleep(0.2)
        raise TimeoutError("no answer to '%s' within %ds" % (line[:80], timeout))

    def log(self):
        p = os.path.join(self.dir, "logs", "latest.log")
        if not os.path.isfile(p):
            return ""
        with open(p, encoding="utf-8", errors="replace") as f:
            return f.read()


def kill_client(game_dir):
    ps = os.path.join(HERE, "kill-client.ps1")
    subprocess.run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", ps, "-Root", ROOT, "-GameDir", os.path.abspath(game_dir)],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def prepare(jars, game_dir):
    for sub in ("mods", "saves", "logs", "control", "crash-reports", os.path.join(".legacyconnector", "cache")):
        shutil.rmtree(os.path.join(game_dir, sub), ignore_errors=True)
    os.makedirs(os.path.join(game_dir, "mods"), exist_ok=True)
    perf = os.path.join(JAR_ROOT, "run-perfmods")
    if os.path.isdir(perf) and not os.environ.get("NOPERF"):
        for f in os.listdir(perf):
            if f.endswith(".jar") and not f.lower().startswith(PERF_SKIP):
                shutil.copy2(os.path.join(perf, f), os.path.join(game_dir, "mods", f))
    for ver, jar in jars:
        d = os.path.join(game_dir, "mods", ver)
        os.makedirs(d, exist_ok=True)
        shutil.copy2(jar, d)


def run_scenario(sc, out_root):
    jars, missing = [], []
    for ver, frag in sc.mods:
        j = find_jar(frag)
        (jars.append((ver, j)) if j else missing.append(frag))
    if missing:
        return "SKIP", "missing: " + ", ".join(missing)
    game_dir = os.path.join(ROOT, "run-verify-client", sc.name)
    shots = os.path.join(out_root, sc.name)
    shutil.rmtree(shots, ignore_errors=True)
    os.makedirs(shots, exist_ok=True)
    prepare(jars, game_dir)
    cl = Client(game_dir)
    env = dict(os.environ, SCENARIO="control", WORLD=sc.world, EXTRA="-PshotDir=run-verify-client/" + sc.name)
    env.setdefault("JAVA_HOME", "C:/Program Files/Java/jdk-21")
    glog = open(os.path.join(shots, "gradle.log"), "w")
    cl.proc = subprocess.Popen([os.environ.get("BASH_EXE", "bash"), "tools/dev/quick-shots.sh"], cwd=ROOT, env=env, stdout=glog, stderr=subprocess.STDOUT)
    status, detail = "PASS", ""
    try:
        end = time.time() + WORLD_TIMEOUT
        while True:
            left = end - time.time()
            if left <= 0:
                return "FAIL", "player not in the world after %ds" % WORLD_TIMEOUT
            try:
                if cl.send("js mc.player != null && mc.level != null && mc.screen == null", left) == "true":
                    break
            except TimeoutError:
                return "FAIL", "player not in the world after %ds" % WORLD_TIMEOUT
            except RuntimeError as e:
                return "FAIL", str(e) + " before the world loaded (see %s/gradle.log)" % shots
            time.sleep(2)
        cl.send("wait 40", 60)
        for st in sc.steps:
            try:
                bad = run_step(cl, st)
            except (TimeoutError, RuntimeError) as e:
                bad = str(e)
            if bad:
                try:
                    cl.send("shot failure", 20)
                except Exception:
                    pass
                status, detail = "FAIL", "step line %d '%s': %s" % (st.no, st.line[:100], bad)
                break
    finally:
        try:
            cl.send("quit", 15)
        except Exception:
            pass
        time.sleep(3)
        kill_client(game_dir)
        if cl.proc.poll() is None:
            cl.proc.terminate()
        glog.close()
        out = os.path.join(game_dir, "control", "out")
        if os.path.isdir(out):
            for f in os.listdir(out):
                if f.endswith(".png"):
                    shutil.copy2(os.path.join(out, f), os.path.join(shots, f))
        lg = os.path.join(game_dir, "logs", "latest.log")
        if os.path.isfile(lg):
            shutil.copy2(lg, os.path.join(shots, "latest.log"))
    return status, detail


def run_step(cl, st):
    """Runs one step; returns None when it passed, else the failure text."""
    if st.kind == "logabsent":
        hit = log_hit(st.pattern, cl.log())
        return "log contains: " + hit[:200] if hit else None
    if st.kind == "expect":
        end = time.time() + EXPECT_RETRY
        while True:
            ans = cl.send("js " + st.js, 60)
            if matches(st.pattern, ans):
                return None
            if time.time() >= end:
                return "expected /%s/ but the game answered: %s" % (st.pattern, ans[:300])
            time.sleep(1)
    if st.kind == "shot":
        ans = cl.send("shot " + st.text, 60)
        return None if ans.startswith("saved") else ans
    ans = cl.send(st.line, step_timeout(st))
    return ("answered: " + ans[:300]) if bad_control_answer(ans) else None


def main(argv):
    names = argv or sorted(f[:-4] for f in os.listdir(os.path.join(HERE, "scenarios")) if f.endswith(".txt"))
    out_root = os.path.join(ROOT, "build", "devtools", "verify-client")
    results, failed = [], False
    for n in names:
        p = os.path.join(HERE, "scenarios", n + ".txt")
        if not os.path.isfile(p):
            line = format_result(n, "FAIL", "unknown scenario")
            failed = True
        else:
            try:
                with open(p, encoding="utf-8") as f:
                    sc = parse_scenario(n, f.read())
                print("=== scenario %s" % n, file=sys.stderr, flush=True)
                status, detail = run_scenario(sc, out_root)
                failed |= status == "FAIL"
                line = format_result(n, status, detail)
            except ScenarioError as e:
                line = format_result(n, "FAIL", str(e))
                failed = True
        print(line, flush=True)
        results.append(line)
    os.makedirs(out_root, exist_ok=True)
    with open(os.path.join(out_root, "report.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(results) + "\n")
    return 1 if failed else 0


if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] != "run":
        print(__doc__)
        sys.exit(2)
    sys.exit(main(sys.argv[2:]))
