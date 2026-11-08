#!/usr/bin/env python3
"""Per-mod client survey: loads many legacy mods into ONE headless-free real client world, enumerates their registries, places
blocks / spawns entities per mod with bulk JavaScript, takes two screenshots per mod, scans latest.log per namespace and rates every
mod from the evidence.  See docs/wiki/Development.md ("Mod survey").

  python tools/survey/survey.py run  --mc 1.12.2 --jars "<dir>" [--jars <dir2>] [--group 40] [--budget 480] [--limit N]
  python tools/survey/survey.py loop --mc 1.12.2 --jars "<dir>"          (same as run, never stops on a failed batch)
  python tools/survey/survey.py merge --mc 1.12.2 [--dry]                 (writes results/*.json ratings into docs/data/mods.json)
  python tools/survey/survey.py stats --mc 1.12.2

Results: run-survey/results/<mc>/<jar>.json (one per jar, resumable: finished jars are skipped), screenshots in run-survey/shots/<mc>/ (gitignored).
"""
import argparse, json, os, pathlib, re, shutil, subprocess, sys, time

ROOT = pathlib.Path(__file__).resolve().parents[2]
SURVEY = ROOT / "run-survey"
GAME = SURVEY / "game"
CONTROL = GAME / "control"
HERE = pathlib.Path(__file__).resolve().parent
DATA = ROOT / "docs" / "data" / "mods.json"
RANK = ["crashes", "fails", "loads", "partial", "works", "playable", "perfect"]
DATE = time.strftime("%Y-%m-%d")


def log(*a):
    print(time.strftime("%H:%M:%S"), *a, flush=True)


def find_perf():
    for p in [ROOT, *ROOT.parents]:
        if (p / "run-perfmods").is_dir():
            return p / "run-perfmods"
    return None


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


def wait_for_slot(max_games, timeout=3600):
    t0 = time.time()
    while time.time() - t0 < timeout:
        if len(game_javas()) < max_games:
            return True
        log(f"waiting for a game slot ({len(game_javas())} running elsewhere)")
        time.sleep(30)
    return False


def kill(pid):
    subprocess.run(["taskkill", "/PID", str(pid), "/F", "/T"], capture_output=True)


# ---------------------------------------------------------------- control channel
class Control:
    def __init__(self):
        self.n = 0
        self.ids = []
        (CONTROL / "in").mkdir(parents=True, exist_ok=True)
        (CONTROL / "out").mkdir(parents=True, exist_ok=True)

    def send(self, line):
        self.n += 1
        cid = f"s{int(time.time()) % 100000:05d}{self.n:06d}"
        tmp = CONTROL / "in" / f"{cid}.tmp"
        tmp.write_text(line, encoding="utf-8")
        tmp.rename(CONTROL / "in" / f"{cid}.cmd")
        self.ids.append(cid)
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
def prepare_game(jars, libs, mc, perf, keep_cache=True):
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
    gradle = subprocess.Popen(
        [str(ROOT / "gradlew.bat"), "runScreenshotTest", "-q", "-PshotDir=run-survey/game", "-PshotScenario=control",
         "-PshotWorldType=minecraft:flat", "-PshotRenderDistance=3", f"-PshotWidth={width}", f"-PshotHeight={height}"],
        cwd=ROOT, env=env, stdout=open(SURVEY / "gradle.log", "w"), stderr=subprocess.STDOUT)
    return gradle


def game_pid():
    for pid, cl in game_javas():
        if "run-survey" in cl.replace("\\", "/") or "run-survey" in cl:
            return pid
    return None


def stop_game(gradle):
    pid = game_pid()
    if pid:
        kill(pid)
    try:
        gradle.terminate()
    except Exception:
        pass
    time.sleep(1)
    pid = game_pid()
    if pid:
        kill(pid)


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
NOISE = re.compile(r"Skipping .*deprecated|Parsed \d+ |unrecognised option|Narrator|OpenAL|Sound|\[STDOUT\]", re.I)


def scan_log(text, namespaces):
    """-> ({ns: {'missing': n, 'errors': n, 'samples': [..]}}, group_level_errors, fatal_text)"""
    names = sorted((n for n in namespaces if len(n) >= 3), key=len, reverse=True)
    rx = re.compile(r"(?<![A-Za-z0-9_])(" + "|".join(re.escape(n) for n in names) + r")(?=[:/.\s\]\)\"',$])", re.I) if names else None
    res = {n: {"missing": 0, "errors": 0, "samples": [], "stack": 0} for n in namespaces}
    lower = {n.lower(): n for n in namespaces}
    group_err = 0
    last_ns = set()
    for line in text.splitlines():
        if line.startswith("\tat ") or line.startswith("Caused by") or line.startswith("\t... "):
            # continuation of an exception: attribute to the namespaces of the packages in the frames
            if rx and line.startswith("\tat "):
                for m in rx.finditer(line):
                    n = lower[m.group(1).lower()]
                    if n in last_ns:
                        res[n]["stack"] += 1
            continue
        is_missing = bool(MISSING.search(line))
        is_exc = bool(EXC.search(line)) and (bool(LEVEL.search(line)) or "Exception" in line[:200])
        last_ns = set()
        if not (is_missing or is_exc) or NOISE.search(line):
            continue
        found = {lower[m.group(1).lower()] for m in rx.finditer(line)} if rx else set()
        if not found:
            if is_exc:
                group_err += 1
            continue
        last_ns = found
        for n in found:
            if is_missing:
                res[n]["missing"] += 1
            if is_exc:
                res[n]["errors"] += 1
            if len(res[n]["samples"]) < 3:
                res[n]["samples"].append(re.sub(r"\s+", " ", line)[:200])
    return res, group_err


def failed_mods(text):
    out = {}
    for m in re.finditer(r"Legacy mod (\S+) \([^)]*\) failed during (\w+): (.*)", text):
        out[m.group(1).lower()] = f"{m.group(2)}: " + re.sub(r"\s+", " ", m.group(3))[:160]
    return out


# ---------------------------------------------------------------- images
def analyse_shots(mc, base_png, shot_png, tag):
    """-> dict(changed=share of pixels differing from the empty stage, magenta=share of changed pixels that are missing-texture magenta)"""
    from PIL import Image, ImageChops
    try:
        a = Image.open(base_png).convert("RGB")
        b = Image.open(shot_png).convert("RGB")
    except Exception as e:
        return {"error": str(e)}
    d = ImageChops.difference(a, b).convert("L").point(lambda v: 255 if v > 24 else 0)
    changed = sum(1 for v in d.get_flattened_data() if v) / max(1, a.width * a.height)
    mag = 0
    px, dp = b.load(), d.load()
    for y in range(0, b.height):
        for x in range(0, b.width):
            if dp[x, y]:
                r, g, bl = px[x, y]
                if r > 170 and bl > 170 and g < 70:
                    mag += 1
    n = sum(1 for v in d.get_flattened_data() if v)
    out = SURVEY / "shots" / mc
    out.mkdir(parents=True, exist_ok=True)
    b.save(out / f"{tag}.jpg", quality=90)
    return {"changed": round(changed, 4), "magenta": round(mag / max(1, n), 4), "magentaPx": mag}


# ---------------------------------------------------------------- one batch
def sample(names, k):
    if len(names) <= k:
        return list(names)
    step = len(names) / k
    return [names[int(i * step)] for i in range(k)]


def run_batch(jars, libs, mc, perf, args):
    """Returns (status, records): status 'ok' | 'crash' | 'timeout'; records {jar: record} when ok."""
    SURVEY.mkdir(exist_ok=True)
    prepare_game(jars, libs, mc, perf)
    log(f"batch of {len(jars)} jars: {', '.join(j.name for j in jars[:4])}...")
    gradle = start_game()
    t0 = time.time()
    ctl = Control()
    try:
        probe = ctl.send("js 'ready ' + (mc.level != null) + ' ' + (server != null)")
        first = None
        while time.time() - t0 < args.load_timeout:
            first = ctl.answer(probe, 1)
            if first is not None or gradle.poll() is not None:
                break
        if first is None:
            return "crash" if gradle.poll() is not None else "timeout", {}, 0
        t_ready = time.time()
        log(f"world ready after {t_ready - t0:.0f}s")
        census = ctl.answer(ctl.send("js " + strip_js((HERE / "enumerate.js").read_text())), 120)
        try:
            census = json.loads(census)
        except Exception:
            log("census failed:", str(census)[:200])
            return "crash", {}, 0
        tpl = strip_js((HERE / "showcase.js.tpl").read_text())
        ctl.send("js mc.options.hideGui = true; mc.options.renderDistance().set(3); 'ok'")
        for c in ("gamerule doMobSpawning false", "gamerule doDaylightCycle false", "time set noon", "weather clear", "gamemode spectator",
                  "tp @s 0 -54 -5 0 38"):
            ctl.send("cmd " + c)
        ctl.send("wait 40")
        stage = lambda ns, phase, names: tpl.replace("__NS__", ns).replace("__PHASE__", phase).replace("__NAMES__", ",".join(names))
        c_base = ctl.send("js " + stage("none", "clear", []))
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
        while time.time() < deadline:
            if ctl.answer(last, 1) is not None:
                break
            if gradle.poll() is not None:
                log("game exited during the batch")
                break
        finished = ctl.answer(last, 0) is not None
        log(f"stage run: {len(census)} namespaces, {time.time() - t_ready:.0f}s, finished={finished}")
        ctl.send("js 'bye'")
        time.sleep(1)
        text = (GAME / "logs" / "latest.log").read_text(encoding="utf-8", errors="replace") if (GAME / "logs" / "latest.log").exists() else ""
        report = parse_report(GAME / "logs" / "legacyconnector-report.txt")
        records = rate_batch(jars, libs, mc, census, plan, ctl, text, report, finished, args)
        return "ok", records, time.time() - t0
    finally:
        # copy the log before the game is torn down (the next batch deletes it)
        try:
            shutil.copy2(GAME / "logs" / "latest.log", SURVEY / "last-batch.log")
        except Exception:
            pass
        stop_game(gradle)


def rate_batch(jars, libs, mc, census, plan, ctl, text, report, finished, args):
    all_ns = {m[0] for mods in report.values() for m in mods} | set(census)
    ns_logs, group_err = scan_log(text, all_ns)
    failed = failed_mods(text)
    base = CONTROL / "out" / "base.png"
    records = {}
    for jar in jars:
        mods = report.get(jar.name, [])
        rec = {"jar": jar.name, "mc": mc, "date": DATE, "mods": []}
        if not mods:
            # translated nothing the report knows of (obsolete mod, library, or a hard load failure)
            rec["mods"].append({"ns": None, "name": jar.stem, "version": "", "rating": "loads", "evidence": "not in the layer's report (skipped as obsolete or a pure library)"})
        for ns, legacy, name, version in mods:
            c = census.get(ns, {"blocks": [], "items": [], "entities": [], "biomes": 0, "dims": 0})
            p = plan.get(ns)
            ev, problems = [], []
            counts = {k: (len(v) if isinstance(v, list) else v) for k, v in c.items()}
            fail = failed.get(legacy.lower()) or failed.get(ns.lower())
            total = counts["blocks"] + counts["items"] + counts["entities"]
            placed = {"ok": 0, "fail": 0, "errors": []}
            spawned = {"ok": 0, "fail": 0, "errors": []}
            vis = {}
            if p:
                for kind, acc in (("blocks", placed), ("entities", spawned)):
                    cid = p["ids"].get(kind)
                    a = ctl.answer(cid, 0) if cid else None
                    if a:
                        try:
                            r = json.loads(a)
                            acc.update(r)
                        except Exception:
                            acc["errors"].append(a[:100])
                if p["blocks"] and (CONTROL / "out" / f"b_{ns}.png").exists() and base.exists():
                    vis["blocks"] = analyse_shots(mc, base, CONTROL / "out" / f"b_{ns}.png", f"{ns}_blocks")
                if p["entities"] and (CONTROL / "out" / f"e_{ns}.png").exists() and base.exists():
                    vis["entities"] = analyse_shots(mc, base, CONTROL / "out" / f"e_{ns}.png", f"{ns}_entities")
            lg = ns_logs.get(ns, {"missing": 0, "errors": 0, "samples": [], "stack": 0})
            ev.append(f"{counts['items']} items, {counts['blocks']} blocks, {counts['entities']} entity types"
                      + (f", {counts['biomes']} biomes" if counts["biomes"] else "") + (f", {counts['dims']} dimensions" if counts["dims"] else ""))
            if p and p["blocks"]:
                ev.append(f"placed {placed['ok']}/{len(p['blocks'])} sampled blocks")
            if p and p["entities"]:
                ev.append(f"spawned {spawned['ok']}/{len(p['entities'])} entity types")
            if fail:
                problems.append(f"load failure during {fail}")
            if placed["fail"]:
                problems.append(f"{placed['fail']} block placements threw ({placed['errors'][0] if placed['errors'] else ''})")
            if spawned["fail"]:
                problems.append(f"{spawned['fail']} entity spawns threw ({spawned['errors'][0] if spawned['errors'] else ''})")
            if lg["missing"]:
                problems.append(f"{lg['missing']} missing texture/model log lines")
            if lg["errors"] + lg["stack"] > 0 and lg["errors"]:
                problems.append(f"{lg['errors']} error log lines")
            b = vis.get("blocks")
            if b and b.get("magentaPx", 0) > 60 and b.get("magenta", 0) > 0.03:
                problems.append("missing-texture magenta visible in the block shot")
            if b and p and placed["ok"] >= 4 and b.get("changed", 1) < 0.0008 and not lg["missing"]:
                problems.append("sampled blocks left no visible pixels")
            e = vis.get("entities")
            if e and e.get("magenta", 0) > 0.03 and e.get("magentaPx", 0) > 60:
                problems.append("missing-texture magenta visible in the entity shot")
            if e and p and spawned["ok"] >= 2 and e.get("changed", 1) < 0.0008:
                problems.append("spawned entities left no visible pixels (invisible?)")
            if not finished and not vis and total:
                rating, note = "unknown", "survey batch ran out of time before this mod was reached"
            elif total == 0:
                rating = "fails" if fail else "loads"
                note = "no content of its own (library or client tweak)" if not fail else ""
            else:
                bad = placed["fail"] + spawned["fail"]
                if fail and not (placed["ok"] or spawned["ok"]):
                    rating = "fails"
                elif problems:
                    rating = "partial"
                else:
                    rating = "works"
            rec["mods"].append({"ns": ns, "name": name, "version": version, "rating": rating if total or fail else rating,
                                "counts": counts, "placed": [placed["ok"], len(p["blocks"]) if p else 0],
                                "spawned": [spawned["ok"], len(p["entities"]) if p else 0], "log": {k: lg[k] for k in ("missing", "errors")},
                                "samples": lg["samples"], "vision": vis, "problems": problems, "evidence": "; ".join(ev + problems)})
        records[jar.name] = rec
    return records


# ---------------------------------------------------------------- driver
def write_records(mc, records):
    d = SURVEY / "results" / mc
    d.mkdir(parents=True, exist_ok=True)
    for jar, rec in records.items():
        (d / f"{pathlib.Path(jar).stem}.json").write_text(json.dumps(rec, indent=1), encoding="utf-8")


def jar_done(mc, jar):
    return (SURVEY / "results" / mc / f"{jar.stem}.json").exists()


def process(jars, libs, mc, perf, args, depth=0):
    """Runs a group; bisects it when the game never reached the world."""
    if not jars:
        return
    status, records, secs = run_batch(jars, libs, mc, perf, args)
    if status == "ok":
        write_records(mc, records)
        n = len(jars)
        log(f"batch ok: {n} jars in {secs:.0f}s = {n / max(secs, 1) * 60:.1f} jars/min")
        with open(SURVEY / "timing.csv", "a") as f:
            f.write(f"{mc},{n},{secs:.0f}\n")
        return
    log(f"batch {status} with {len(jars)} jars")
    # keep the log of the failure for the single-jar case
    if len(jars) == 1:
        text = (SURVEY / "last-batch.log").read_text(encoding="utf-8", errors="replace") if (SURVEY / "last-batch.log").exists() else ""
        why = "; ".join(sorted({l.strip()[:200] for l in text.splitlines() if re.search(r"Exception|Error|crash", l) and "[ERROR]" in l})[:2])
        # a crash only counts against the jar when the log blames it; environment crashes (perf-mod mixins...) stay unknown
        blamed = status == "crash" and (re.search(r"Legacy mod .*failed|Legacy Connector.*(fail|cannot)", text, re.I) or
                                        jars[0].stem.split("-")[0].lower() in text.lower().split("crash report")[-1])
        rec = {"jar": jars[0].name, "mc": mc, "date": DATE,
               "mods": [{"ns": None, "name": jars[0].stem, "version": "", "rating": "fails" if blamed else "unknown",
                         "evidence": ("alone in a world the game " + ("crashed or never reached the world: " + why) if status == "crash"
                                      else "did not reach the world within the time limit")}]}
        write_records(mc, {jars[0].name: rec})
        return
    mid = len(jars) // 2
    process(jars[:mid], libs, mc, perf, args, depth + 1)
    process(jars[mid:], libs, mc, perf, args, depth + 1)


def collect_jars(dirs):
    jars = []
    for d in dirs:
        jars += sorted(pathlib.Path(d).glob("*.jar"), key=lambda p: p.name.lower())
    return jars


def cmd_run(args):
    libs_frag = [l.strip().lower() for l in (HERE / "libs.txt").read_text().splitlines() if l.strip() and not l.startswith("#")]
    jars = collect_jars(args.jars)
    libs = [j for j in jars if any(f in j.name.lower() for f in libs_frag)]
    todo = [j for j in jars if j not in libs and not jar_done(args.mc, j)]
    if args.limit:
        todo = todo[:args.limit]
    perf = None if args.noperf else find_perf()
    log(f"{len(jars)} jars, {len(libs)} libs always loaded, {len(todo)} to survey")
    for i in range(0, len(todo), args.group):
        if not wait_for_slot(args.max_games):
            log("no game slot, giving up")
            return
        process(todo[i:i + args.group], libs, args.mc, perf, args)


def cmd_stats(args):
    d = SURVEY / "results" / args.mc
    cnt = {}
    for f in d.glob("*.json"):
        for m in json.loads(f.read_text(encoding="utf-8"))["mods"]:
            cnt[m["rating"]] = cnt.get(m["rating"], 0) + 1
    print(cnt)
    t = SURVEY / "timing.csv"
    if t.exists():
        rows = [l.split(",") for l in t.read_text().splitlines() if l.startswith(args.mc)]
        jars, secs = sum(int(r[1]) for r in rows), sum(int(r[2]) for r in rows)
        print(f"{jars} jars in {secs}s = {jars / max(secs, 1) * 60:.1f} jars/min")


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def cmd_merge(args):
    data = json.loads(DATA.read_text(encoding="utf-8"))
    mods = data["mods"]
    changed = 0
    for f in sorted((SURVEY / "results" / args.mc).glob("*.json")):
        rec = json.loads(f.read_text(encoding="utf-8"))
        for m in rec["mods"]:
            if m["rating"] in ("unknown",) or not m.get("ns") and m["rating"] != "fails":
                continue
            name = m["name"]
            existing = [e for e in mods if e.get("mc") == args.mc and norm(e["name"]) == norm(name)]
            if any(e.get("source") not in ("probe", "corpus", "client") for e in existing):
                continue   # maintainer / player entries win
            best = max((RANK.index(e["status"]) for e in existing if e.get("source") in ("client",)), default=-1)
            if m["rating"] == "loads" and existing:
                continue   # no new evidence
            if best > RANK.index(m["rating"]) and any(e["version"] == m["version"] for e in existing if e.get("source") == "client"):
                continue
            entry = {"name": name, "version": m["version"], "mc": args.mc, "source": "client", "date": rec["date"], "status": m["rating"],
                     "notes": "Automated client survey: " + m["evidence"][:300]}
            if args.dry:
                print(entry["status"], name, "-", entry["notes"][:120])
            else:
                for e in [e for e in existing if e.get("source") in ("client",)]:
                    mods.remove(e)
                mods.append(entry)
            changed += 1
    if not args.dry:
        mods.sort(key=lambda e: e["name"].lower())
        DATA.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{changed} entries {'would be ' if args.dry else ''}written")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["run", "loop", "merge", "stats"])
    ap.add_argument("--mc", default="1.12.2")
    ap.add_argument("--jars", action="append", default=[])
    ap.add_argument("--group", type=int, default=40)
    ap.add_argument("--blocks", type=int, default=27)
    ap.add_argument("--entities", type=int, default=12)
    ap.add_argument("--budget", type=int, default=480, help="seconds for the staging part of a batch")
    ap.add_argument("--load-timeout", type=int, default=420, help="seconds to reach the world")
    ap.add_argument("--max-games", type=int, default=2)
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--noperf", action="store_true")
    ap.add_argument("--dry", action="store_true")
    args = ap.parse_args()
    {"run": cmd_run, "loop": cmd_run, "merge": cmd_merge, "stats": cmd_stats}[args.cmd](args)


if __name__ == "__main__":
    main()
