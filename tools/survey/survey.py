#!/usr/bin/env python3
"""Per-mod client survey driver. Loads many legacy mods into ONE real client world, enumerates their registries, places blocks /
spawns entities per mod with bulk JavaScript, takes two screenshots per mod and scans latest.log per namespace. It only gathers
EVIDENCE (one JSON per jar); rating and the merge into docs/data/mods.json are done by apply.py. See docs/wiki/Development.md ("Mod survey").

  python tools/survey/survey.py run --all            start the detached background run over all 1.12.2 then 1.7.10 jars (returns at once)
  python tools/survey/survey.py run --all --foreground      same, in this console
  python tools/survey/survey.py run --mc 1.12.2 --jars "<dir>" [--limit N]     a foreground run over specific jar directories
  python tools/survey/survey.py status                       heartbeat, progress, rate
  python tools/survey/survey.py stop                         asks the run to stop (STOP file)

State lives in run-survey-main/ next to the main checkout (results/<mc>/<jar>.json, shots/, game/, progress.log, heartbeat.json, STOP, lock).
"""
import argparse, json, os, pathlib, re, shutil, subprocess, sys, threading, time

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[1]


def find_main():
    for p in [ROOT, *ROOT.parents]:
        if (p / "run-perfmods").is_dir():
            return p
    return ROOT


MAIN = find_main()
SURVEY = MAIN / "run-survey-main"
GAME = SURVEY / "game"
CONTROL = GAME / "control"
MARK = "run-survey-main"
STOP = SURVEY / "STOP"
TEMPLATES = {n: (HERE / n).read_text(encoding="utf-8") for n in ("enumerate.js", "showcase.js.tpl")}
LIBS = [l.strip().lower() for l in (HERE / "libs.txt").read_text().splitlines() if l.strip() and not l.startswith("#")]
STATE = {"phase": "init", "mc": "", "jarsDone": 0, "jarsTotal": 0, "batch": 0, "batchesOk": 0, "batchesBad": 0}
T_START = time.time()

DEFAULT_DIRS = {
    "1.12.2": ["1.12.2 test mods", "test-mods-1122", "run-subtest/mods/1.12.2"],
    "1.7.10": ["test-mods", "test-mods2", "test-mods3", "test-mods-1710-extra", "run-subtest/mods-1710", "run-screenshots/mods/1.7.10"],
}


def project_root():
    """Where gradle runs: the main checkout once the survey is merged there (it outlives a worktree), else the checkout of this script."""
    if (MAIN / "tools" / "survey" / "survey.py").exists() and (MAIN / "gradlew.bat").exists():
        return MAIN
    return ROOT


def log(*a):
    print(time.strftime("%H:%M:%S ") + " ".join(str(x) for x in a), flush=True)


def find_perf():
    d = MAIN / "run-perfmods"
    return d if d.is_dir() else None


# ---------------------------------------------------------------- processes
def ps(cmd):
    r = subprocess.run(["powershell", "-NoProfile", "-Command", cmd], capture_output=True, text=True)
    return r.stdout


def game_javas():
    """(pid, cmdline) of every game/server java on the machine (any agent)."""
    out = ps("Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | ForEach-Object { \"$($_.ProcessId)`t$($_.CommandLine)\" }")
    res = []
    for line in out.splitlines():
        pid, _, cl = line.partition("\t")
        if pid.strip().isdigit() and ("net.minecraft.client.main.Main" in cl or "ServerMain" in cl or "net.minecraft.server" in cl
                                      or "cpw.mods.bootstraplauncher" in cl):
            res.append((int(pid), cl))
    return res


def my_game_pids():
    return [pid for pid, cl in game_javas() if MARK in cl]


def stop_requested():
    return STOP.exists()


def wait_for_slot(max_games):
    """Waits politely until fewer than max_games game processes of other agents run. False when a STOP was requested."""
    while not stop_requested():
        others = [p for p, cl in game_javas() if MARK not in cl]
        if len(others) < max_games:
            return True
        STATE["phase"] = f"waiting for a game slot ({len(others)} running elsewhere)"
        time.sleep(20)
    return False


def kill(pid):
    subprocess.run(["taskkill", "/PID", str(pid), "/F", "/T"], capture_output=True)


# ---------------------------------------------------------------- control channel
class Control:
    def __init__(self):
        self.n = 0
        (CONTROL / "in").mkdir(parents=True, exist_ok=True)
        (CONTROL / "out").mkdir(parents=True, exist_ok=True)

    def send(self, line):
        self.n += 1
        cid = f"s{self.n:07d}"
        tmp = CONTROL / "in" / f"{cid}.tmp"
        tmp.write_text(line, encoding="utf-8")
        tmp.rename(CONTROL / "in" / f"{cid}.cmd")
        return cid

    def answer(self, cid, timeout=0):
        f = CONTROL / "out" / f"{cid}.txt"
        t0 = time.time()
        while True:
            if f.exists():
                return f.read_text(encoding="utf-8", errors="replace").strip()
            if time.time() - t0 >= timeout:
                return None
            time.sleep(0.1)


def strip_js(src):
    return "\n".join(l for l in src.splitlines() if not l.strip().startswith("//"))


# ---------------------------------------------------------------- game session
def prepare_game(jars, libs, mc, perf):
    for sub in ("mods", "world", "saves", "control", "logs", "crash-reports"):
        shutil.rmtree(GAME / sub, ignore_errors=True)
    (GAME / "mods" / mc).mkdir(parents=True)
    if perf:
        for j in perf.glob("*.jar"):
            if "irisveil" in j.name.lower():
                continue   # Iris Veil Compat needs the Veil library: its mixin crashes the client without it
            shutil.copy2(j, GAME / "mods" / j.name)
    for j in list(jars) + list(libs):
        shutil.copy2(j, GAME / "mods" / mc / j.name)
    (GAME / "options.txt").write_text("renderDistance:3\nguiScale:1\nmaxFps:60\nonboarding:false\nskipMultiplayerWarning:true\n"
                                      "pauseOnLostFocus:false\nlang:en_us\nsoundCategory_master:0.0\n")


def start_game(width=640, height=360):
    env = dict(os.environ, JAVA_HOME=os.environ.get("SURVEY_JAVA_HOME", "C:/Program Files/Java/jdk-21"))
    root = project_root()
    return subprocess.Popen(
        [str(root / "gradlew.bat"), "runScreenshotTest", "-q", f"-PshotDir={GAME.as_posix()}", "-PshotScenario=control",
         "-PshotWorldType=minecraft:flat", "-PshotRenderDistance=3", f"-PshotWidth={width}", f"-PshotHeight={height}"],
        cwd=root, env=env, stdout=open(SURVEY / "gradle.log", "w"), stderr=subprocess.STDOUT,
        creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))


def stop_game(gradle):
    """Kills only this driver's own client (exact PIDs of processes whose command line carries our run directory)."""
    for _ in range(3):
        for pid in my_game_pids():
            kill(pid)
        try:
            gradle.terminate()
        except Exception:
            pass
        time.sleep(1)
        if not my_game_pids():
            break


# ---------------------------------------------------------------- report / log parsing
def parse_report(path):
    """{jar name: [(modern ns, legacy id, name, version)]}"""
    out, cur = {}, None
    if not path.exists():
        return out
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        m = re.match(r"^(\S.*\.jar)\s+\[", line)
        if m:
            cur = m.group(1)
            out.setdefault(cur, [])
            continue
        m = re.match(r"^  mod (.+?) -> (\S+)\s+\((.*)\)\s*$", line)
        if m and cur:
            v = re.match(r"^(.*?)\s+(\d[^ ]*(?: .*)?)$", m.group(3))
            name, ver = (v.group(1), v.group(2)) if v else (m.group(3), "")
            out[cur].append((m.group(2), m.group(1), name.strip() or m.group(1), ver.strip()))
    return out


MISSING = re.compile(r"Missing texture|Unable to load model|Missing model|Failed to load model|Unable to resolve texture|Missing particle sprite|"
                     r"Unable to bake|Missing sprite|Could not load texture|Missing block ?state|missing_?texture|Unknown blockstate|No model for|"
                     r"Unable to locate sprite|Couldn't find texture", re.I)
EXC = re.compile(r"StackOverflowError|OutOfMemoryError|Exception|Caught error loading resourcepacks|Reported exception thrown|"
                 r"Failed to ?(render|tick|load|register|create|bake)|Error (rendering|ticking|during)|VerifyError|NoSuchMethodError|NoSuchFieldError|"
                 r"AbstractMethodError|IncompatibleClassChangeError|ClassCastException|NullPointerException|NoClassDefFoundError")
LEVEL = re.compile(r"\[(WARN|ERROR|FATAL)\]|/(WARN|ERROR|FATAL)\]")
NOISE = re.compile(r"Skipping .*deprecated|Parsed \d+ |unrecognised option|Narrator|OpenAL|Sound|\[STDOUT\]|Error loading class: ", re.I)


def scan_log(text, namespaces):
    """-> {ns: {'missing': n, 'errors': n, 'stack': n, 'samples': [..]}}"""
    names = sorted((n for n in namespaces if len(n) >= 3), key=len, reverse=True)
    rx = re.compile(r"(?<![A-Za-z0-9_])(" + "|".join(re.escape(n) for n in names) + r")(?=[:/.\s\]\)\"',$])", re.I) if names else None
    res = {n: {"missing": 0, "errors": 0, "samples": [], "stack": 0} for n in namespaces}
    lower = {n.lower(): n for n in namespaces}
    last_ns = set()
    for line in text.splitlines():
        if line.startswith("\tat ") or line.startswith("Caused by") or line.startswith("\t... "):
            if rx and line.startswith("\tat "):
                for m in rx.finditer(line[:400]):
                    n = lower[m.group(1).lower()]
                    if n in last_ns:
                        res[n]["stack"] += 1
            continue
        line = line[:600]
        is_missing = bool(MISSING.search(line))
        is_exc = bool(EXC.search(line)) and (bool(LEVEL.search(line)) or "Exception" in line[:200])
        last_ns = set()
        if not (is_missing or is_exc) or NOISE.search(line):
            continue
        found = {lower[m.group(1).lower()] for m in rx.finditer(line)} if rx else set()
        if not found:
            continue
        last_ns = found
        for n in found:
            if is_missing:
                res[n]["missing"] += 1
            if is_exc:
                res[n]["errors"] += 1
            if len(res[n]["samples"]) < 3:
                res[n]["samples"].append(re.sub(r"\s+", " ", line)[:200])
    return res


def failed_mods(text):
    out = {}
    for m in re.finditer(r"Legacy mod (\S+) \([^)]*\) failed during (\w+): (.*)", text):
        out[m.group(1).lower()] = f"{m.group(2)}: " + re.sub(r"\s+", " ", m.group(3))[:160]
    return out


def crash_reason(text):
    """Description and exception line of the last crash report in a log, for the evidence note."""
    i = text.rfind("---- Minecraft Crash Report ----")
    if i < 0:
        return ""
    chunk = text[i:i + 6000]
    d = re.search(r"Description: (.*)", chunk)
    e = re.search(r"\n([\w.$]+(?:Error|Exception)[^\n]{0,160})", chunk)
    return ((d.group(1) if d else "") + " / " + (e.group(1)[:200] if e else "")).strip(" /")


# ---------------------------------------------------------------- images
def analyse_shot(base_png, shot_png, tag, mc):
    """Compares a showcase shot with the empty stage: share of pixels that changed and share of those that are missing-texture magenta."""
    from PIL import Image, ImageChops
    try:
        a, b = Image.open(base_png).convert("RGB"), Image.open(shot_png).convert("RGB")
    except Exception as e:
        return {"error": str(e)}
    d = ImageChops.difference(a, b).convert("L").point(lambda v: 255 if v > 24 else 0)
    n = d.histogram()[255]
    r, g, bl = b.split()
    mag = ImageChops.multiply(ImageChops.multiply(r.point(lambda v: 255 if v > 170 else 0), bl.point(lambda v: 255 if v > 170 else 0)),
                              g.point(lambda v: 255 if v < 70 else 0))
    mag = ImageChops.multiply(mag, d).histogram()[255]
    out = SURVEY / "shots" / mc
    out.mkdir(parents=True, exist_ok=True)
    b.save(out / f"{tag}.jpg", quality=90)
    return {"changed": round(n / max(1, a.width * a.height), 4), "magenta": round(mag / max(1, n), 4), "magentaPx": mag}


def sample(names, k):
    if len(names) <= k:
        return list(names)
    step = len(names) / k
    return [names[int(i * step)] for i in range(k)]


# ---------------------------------------------------------------- one batch
def run_batch(jars, libs, mc, perf, args):
    """-> (status 'ok'|'crash'|'timeout'|'stopped', records {jar: record}, seconds)"""
    SURVEY.mkdir(exist_ok=True)
    prepare_game(jars, libs, mc, perf)
    STATE["phase"] = f"loading {len(jars)} jars"
    log(f"batch of {len(jars)} jars: {', '.join(j.name for j in jars[:3])}...")
    gradle = start_game()
    t0 = time.time()
    ctl = Control()
    try:
        probe = ctl.send("js 'ready ' + (mc.level != null) + ' ' + (server != null)")
        first = None
        while time.time() - t0 < args.load_timeout:
            first = ctl.answer(probe, 1)
            if first is not None or gradle.poll() is not None or stop_requested():
                break
        if stop_requested():
            return "stopped", {}, 0
        if first is None:
            return ("crash" if gradle.poll() is not None else "timeout"), {}, time.time() - t0
        t_ready = time.time()
        log(f"world ready after {t_ready - t0:.0f}s")
        STATE["phase"] = "staging"
        census = ctl.answer(ctl.send("js " + strip_js(TEMPLATES["enumerate.js"])), 120)
        try:
            census = json.loads(census)
        except Exception:
            log("census failed:", str(census)[:200])
            return "crash", {}, time.time() - t0
        tpl = strip_js(TEMPLATES["showcase.js.tpl"])
        ctl.send("js mc.options.hideGui = true; 'ok'")
        for c in ("gamerule doMobSpawning false", "gamerule doDaylightCycle false", "time set noon", "weather clear", "gamemode spectator",
                  "tp @s 0 -54 -5 0 38"):
            ctl.send("cmd " + c)
        ctl.send("wait 40")
        stage = lambda ns, phase, names: tpl.replace("__NS__", ns).replace("__PHASE__", phase).replace("__NAMES__", ",".join(names))
        ctl.send("js " + stage("none", "clear", []))
        ctl.send("wait 30")
        ctl.send("shot base")
        plan = {}
        for ns, c in sorted(census.items()):
            blocks, ents = sample(c["blocks"], args.blocks), sample(c["entities"], args.entities)
            plan[ns] = {"blocks": blocks, "entities": ents, "ids": {}}
            if blocks:
                plan[ns]["ids"]["blocks"] = ctl.send("js " + stage(ns, "blocks", blocks))
                ctl.send("wait 12")
                ctl.send(f"shot b_{ns}")
            if ents:
                plan[ns]["ids"]["entities"] = ctl.send("js " + stage(ns, "entities", ents))
                ctl.send("wait 12")
                ctl.send(f"shot e_{ns}")
        ctl.send("js " + stage("none", "clear", []))
        last = ctl.send("js 'done'")
        deadline = t_ready + args.budget
        while time.time() < deadline and not stop_requested():
            if ctl.answer(last, 1) is not None or gradle.poll() is not None:
                break
        finished = ctl.answer(last, 0) is not None
        crashed = gradle.poll() is not None and not finished
        log(f"stage run: {len(census)} namespaces, {time.time() - t_ready:.0f}s, finished={finished}, crashed={crashed}")
        ctl.send("js 'bye'")
        time.sleep(1)
        lp = GAME / "logs" / "latest.log"
        text = lp.read_text(encoding="utf-8", errors="replace") if lp.exists() else ""
        report = parse_report(GAME / "logs" / "legacyconnector-report.txt")
        records = collect(jars, mc, census, plan, ctl, text, report, crashed)
        return "ok", records, time.time() - t0
    finally:
        try:
            shutil.copy2(GAME / "logs" / "latest.log", SURVEY / "last-batch.log")
        except Exception:
            pass
        stop_game(gradle)


def collect(jars, mc, census, plan, ctl, text, report, crashed):
    """Raw evidence per jar and mod (no rating: apply.py rates)."""
    all_ns = {m[0] for mods in report.values() for m in mods} | set(census)
    ns_logs = scan_log(text, all_ns)
    failed = failed_mods(text)
    base = CONTROL / "out" / "base.png"
    culprit_done = False
    records = {}
    for jar in jars:
        rec = {"jar": jar.name, "mc": mc, "date": time.strftime("%Y-%m-%d"), "attempts": 1, "mods": []}
        for ns, legacy, name, version in report.get(jar.name, []):
            c = census.get(ns, {"blocks": [], "items": [], "entities": [], "biomes": 0, "dims": 0})
            p = plan.get(ns)
            m = {"ns": ns, "legacy": legacy, "name": name, "version": version, "reached": True, "crashedHere": False,
                 "counts": {k: (len(v) if isinstance(v, list) else v) for k, v in c.items()},
                 "fail": failed.get(legacy.lower()) or failed.get(ns.lower()) or "",
                 "placed": [0, 0, 0, []], "spawned": [0, 0, 0, []], "vision": {}}
            lg = ns_logs.get(ns, {"missing": 0, "errors": 0, "stack": 0, "samples": []})
            m["log"] = {"missing": lg["missing"], "errors": lg["errors"], "stack": lg["stack"]}
            m["samples"] = lg["samples"]
            if p:
                for kind, key, prefix in (("blocks", "placed", "b_"), ("entities", "spawned", "e_")):
                    if not p[kind]:
                        continue
                    a = ctl.answer(p["ids"][kind], 0)
                    shot = CONTROL / "out" / f"{prefix}{ns}.png"
                    if a is None or not shot.exists():
                        m["reached"] = False
                        if crashed and not culprit_done:
                            m["crashedHere"], culprit_done = True, True
                        continue
                    try:
                        r = json.loads(a)
                        m[key] = [r["ok"], len(p[kind]), r["fail"], r["errors"][:2]]
                    except Exception:
                        m[key] = [0, len(p[kind]), 0, [a[:100]]]
                    if base.exists():
                        m["vision"][kind] = analyse_shot(base, shot, f"{ns}_{kind}", mc)
            rec["mods"].append(m)
        if not rec["mods"]:
            rec["mods"].append({"ns": None, "name": jar.stem, "version": "", "reached": True, "counts": {}, "fail": "",
                                "note": "not in the layer's report (skipped as obsolete, or a library)"})
        if crashed:
            rec["crashReason"] = crash_reason(text)
        records[jar.name] = rec
    return records


# ---------------------------------------------------------------- driver
def write_records(mc, records):
    d = SURVEY / "results" / mc
    d.mkdir(parents=True, exist_ok=True)
    for jar, rec in records.items():
        f = d / f"{pathlib.Path(jar).stem}.json"
        if f.exists():
            try:
                rec["attempts"] = max(rec.get("attempts", 1), json.loads(f.read_text(encoding="utf-8")).get("attempts", 1) + 1)
            except Exception:
                pass
        f.write_text(json.dumps(rec, indent=1), encoding="utf-8")


def jar_done(mc, jar):
    f = SURVEY / "results" / mc / f"{jar.stem}.json"
    if not f.exists():
        return False
    try:
        rec = json.loads(f.read_text(encoding="utf-8"))
    except Exception:
        return False
    return rec.get("attempts", 1) >= 2 or all(m.get("reached", True) for m in rec["mods"])


def process(jars, libs, mc, perf, args):
    """Runs a group; bisects it when the game never reached the world."""
    if not jars or stop_requested():
        return
    STATE["batch"] += 1
    status, records, secs = run_batch(jars, libs, mc, perf, args)
    if status == "stopped":
        return
    if status == "ok":
        write_records(mc, records)
        STATE["batchesOk"] += 1
        STATE["jarsDone"] += len(jars)
        log(f"batch ok: {len(jars)} jars in {secs:.0f}s = {len(jars) / max(secs, 1) * 60:.1f} jars/min")
        with open(SURVEY / "timing.csv", "a") as f:
            f.write(f"{mc},{len(jars)},{secs:.0f}\n")
        return
    STATE["batchesBad"] += 1
    log(f"batch {status} with {len(jars)} jars")
    if len(jars) == 1:
        lp = SURVEY / "last-batch.log"
        text = lp.read_text(encoding="utf-8", errors="replace") if lp.exists() else ""
        failed = failed_mods(text)
        why = crash_reason(text)
        # a crash only counts against the jar when the log blames it; environment problems stay unknown
        blamed = status == "crash" and bool(failed or re.search(r"Legacy Connector.*(fail|cannot)", text) or
                                           jars[0].stem.split("-")[0].lower() in text.lower().split("crash report")[-1])
        rec = {"jar": jars[0].name, "mc": mc, "date": time.strftime("%Y-%m-%d"), "attempts": 2,
               "mods": [{"ns": None, "name": jars[0].stem, "version": "", "reached": True, "counts": {}, "fail": "",
                         "loadCrash": blamed, "unknown": not blamed,
                         "note": (f"alone in a world the game crashed before it started: {why}" if status == "crash"
                                  else "did not reach the world within the time limit")}]}
        write_records(mc, {jars[0].name: rec})
        STATE["jarsDone"] += 1
        return
    mid = len(jars) // 2
    process(jars[:mid], libs, mc, perf, args)
    process(jars[mid:], libs, mc, perf, args)


def collect_jars(dirs):
    seen, jars = set(), []
    for d in dirs:
        d = pathlib.Path(d)
        if not d.is_absolute():
            d = MAIN / d
        if not d.is_dir():
            continue
        for j in sorted(d.glob("*.jar"), key=lambda p: p.name.lower()):
            if j.name.lower() not in seen:
                seen.add(j.name.lower())
                jars.append(j)
    return jars


def run_apply():
    try:
        subprocess.run([sys.executable, str(HERE / "apply.py"), "--all"], cwd=str(project_root()), timeout=300, capture_output=True)
    except Exception as e:
        log("apply failed:", e)


def run_mc(mc, dirs, args):
    jars = collect_jars(dirs)
    libs = [j for j in jars if any(f in j.name.lower() for f in LIBS)]
    perf = None if args.noperf else find_perf()
    for pass_no in (1, 2):
        todo = [j for j in jars if j not in libs and not jar_done(mc, j)]
        if args.limit:
            todo = todo[:args.limit]
        STATE.update(mc=mc, jarsTotal=len(jars) - len(libs), jarsDone=len(jars) - len(libs) - len(todo))
        log(f"{mc} pass {pass_no}: {len(jars)} jars, {len(libs)} libs always loaded, {len(todo)} to survey")
        if not todo:
            break
        since_apply = 0
        for i in range(0, len(todo), args.group):
            if stop_requested() or not wait_for_slot(args.max_games):
                return
            group = todo[i:i + args.group]
            process(group, libs, mc, perf, args)
            since_apply += len(group)
            if since_apply >= 50:
                run_apply()
                since_apply = 0
        run_apply()


def heartbeat_loop():
    while True:
        try:
            tf = SURVEY / "timing.csv"
            rows = [l.split(",") for l in tf.read_text().splitlines()] if tf.exists() else []
            jars, secs = sum(int(r[1]) for r in rows), sum(int(r[2]) for r in rows)
            hb = dict(STATE, time=time.strftime("%Y-%m-%d %H:%M:%S"), pid=os.getpid(), uptimeMin=round((time.time() - T_START) / 60, 1),
                      jarsPerMin=round(jars / max(secs, 1) * 60, 1), myGamePids=my_game_pids())
            (SURVEY / "heartbeat.json").write_text(json.dumps(hb, indent=1))
        except Exception:
            pass
        time.sleep(10)


def pid_alive(pid):
    if not pid:
        return False
    out = subprocess.run(["tasklist", "/FI", f"PID eq {pid}", "/NH"], capture_output=True, text=True).stdout
    return str(pid) in out


def lock_pid(lock):
    try:
        return int(lock.read_text().strip())
    except Exception:
        return 0


def cmd_run(args):
    SURVEY.mkdir(exist_ok=True)
    lock = SURVEY / "lock"
    if args.all and not args.foreground and not args.worker:
        if pid_alive(lock_pid(lock)):
            print(f"already running (pid {lock_pid(lock)}); see: python tools/survey/survey.py status")
            return
        STOP.unlink(missing_ok=True)
        # a copy of the scripts outside any worktree, so removing the worktree does not break the run
        stable = SURVEY / "tools"
        shutil.copytree(HERE, stable, dirs_exist_ok=True)
        extra = [a for a in sys.argv[2:] if a not in ("--all", "--foreground", "--worker")]
        p = subprocess.Popen([sys.executable, str(stable / "survey.py"), "run", "--all", "--worker"] + extra,
                             stdout=open(SURVEY / "progress.log", "a"), stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL,
                             cwd=str(MAIN), creationflags=0x00000008 | 0x00000200 | 0x08000000)   # DETACHED | NEW_PROCESS_GROUP | NO_WINDOW
        print(f"started detached (pid {p.pid}); progress: {SURVEY / 'progress.log'}, heartbeat: {SURVEY / 'heartbeat.json'}, stop: create {STOP}")
        return
    if (args.all or args.worker) and pid_alive(lock_pid(lock)) and lock_pid(lock) != os.getpid():
        print("another survey run is active")
        return
    lock.write_text(str(os.getpid()))
    threading.Thread(target=heartbeat_loop, daemon=True).start()
    try:
        if args.all:
            for mc in ("1.12.2", "1.7.10"):
                if stop_requested():
                    break
                run_mc(mc, DEFAULT_DIRS[mc], args)
            if not stop_requested():
                STATE["phase"] = "done"
                (SURVEY / "DONE").write_text(time.strftime("%Y-%m-%d %H:%M:%S"))
        else:
            run_mc(args.mc, args.jars, args)
        run_apply()
    finally:
        phase = "stopped" if stop_requested() else "finished"
        STATE["phase"] = phase
        try:
            (SURVEY / "heartbeat.json").write_text(json.dumps(dict(STATE, time=time.strftime("%Y-%m-%d %H:%M:%S"), pid=os.getpid())))
            lock.unlink(missing_ok=True)
        except Exception:
            pass
        for pid in my_game_pids():
            kill(pid)


def cmd_status(args):
    hb = SURVEY / "heartbeat.json"
    print(hb.read_text() if hb.exists() else "no heartbeat (not started?)")
    for mc in ("1.12.2", "1.7.10"):
        d = SURVEY / "results" / mc
        print(mc, len(list(d.glob("*.json"))) if d.exists() else 0, "jar results")
    print("DONE" if (SURVEY / "DONE").exists() else "not done")


def cmd_stop(args):
    SURVEY.mkdir(exist_ok=True)
    STOP.write_text("stop")
    print("STOP file written; the run stops after the current step (delete it to allow a new start)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["run", "status", "stop"])
    ap.add_argument("--all", action="store_true", help="all 1.12.2 then 1.7.10 jars of the default directories, detached")
    ap.add_argument("--worker", action="store_true")
    ap.add_argument("--foreground", action="store_true")
    ap.add_argument("--mc", default="1.12.2")
    ap.add_argument("--jars", action="append", default=[])
    ap.add_argument("--group", type=int, default=40)
    ap.add_argument("--blocks", type=int, default=27)
    ap.add_argument("--entities", type=int, default=12)
    ap.add_argument("--budget", type=int, default=480, help="seconds for the staging part of a batch")
    ap.add_argument("--load-timeout", type=int, default=480, help="seconds to reach the world")
    ap.add_argument("--max-games", type=int, default=3, help="machine-wide cap of game processes (others' + ours)")
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--noperf", action="store_true")
    args = ap.parse_args()
    {"run": cmd_run, "status": cmd_status, "stop": cmd_stop}[args.cmd](args)


if __name__ == "__main__":
    main()
