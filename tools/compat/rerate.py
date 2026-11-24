#!/usr/bin/env python3
"""Stale-rating re-rater: runs the headless server self test again for rows of docs/data/mods.json that predate fixes and
feeds the logs to from_client.py (label "rerate <date>", source "client", which outranks "corpus"/"probe" rows on the site).

  rerate.py --mc 1.12.2 --status fails [--source client,corpus,none] [--name <fragment>] [--limit N] [--group-size 3]
            [--min-free-gb 6] [--plan] [--dry-run] [--date YYYY-MM-DD] [--deps deps.tsv]

Steps: select rows (--mc, --status, --source; comma lists; "none" matches rows without a source) -> resolve each mod's jar with
tools/dev/verify/picker.py (same folders as verify.sh, same version rules) plus the dependencies the row's notes name ("requires X",
NoClassDefFoundError of another jar's package; --deps takes a TSV `mod name<TAB>dep fragment,dep fragment` for the rest) -> plan small
groups of one Minecraft version -> per group write a temporary suite file, wait until enough RAM is free, run `tools/dev/verify.sh` on it
(own port via VERIFY_PORT_BASE; the run directory is run-verify/<suite>) and rate its log with from_client.py.

  --plan      print the groups and the rows that cannot be tested; start nothing
  --dry-run   run the tests but rate into a scratch copy of mods.json (the real file is not written)
Rows without a jar or with a required dependency that is not available are listed as "not tested" with the reason.
The summary (old -> new status per row) is printed at the end and written to build/devtools/rerate-<date>.txt.
Unit tests: python -m unittest tools/compat/test_rerate.py (a fake runner replaces the server).
"""
import argparse, json, os, pathlib, re, shutil, socket, subprocess, sys, tempfile, time

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(ROOT / "tools" / "dev" / "verify"))
import picker  # noqa: E402

DATA = ROOT / "docs" / "data" / "mods.json"
SUITES = ROOT / "tools" / "dev" / "verify" / "suites"
RANK = ["crashes", "fails", "loads", "partial", "works", "playable", "perfect"]
# the folders verify.sh searches (relative to JAR_ROOT, the main checkout that holds the gitignored jars)
SEARCH = ("test-mods-issues/298 test-mods-issues test-mods test-mods2 test-mods3 test-mods-1122 test-mods-147 test-mods-152 test-mods-164 "
          "test-mods-1710-extra test-mods-1201-a test-mods-1201-terramity test-mods-b173 run-subtest/mods run-subtest/mods.aside-20261113 "
          "run-screenshots/mods run-screenshots/mods.aside-20261113 test-mods-mcarchive/b1.7.3 test-mods-mcarchive/1.2.5").split()
BIG = "1.12.2 test mods"
# top-level package segments that never name a dependency jar
NOT_DEPS = {"java", "javax", "net", "com", "org", "io", "cpw", "scala", "kotlin", "sun", "jdk", "paulscode", "cofh", "codechicken", "mcp", "ibxm", "lzma"}


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def split_list(s):
    return [x.strip().lower() for x in (s or "").split(",") if x.strip()]


# ---------------------------------------------------------------- selection

def select(mods, mcs, statuses, sources=None, name=None):
    """Rows of mods.json (in file order) whose mc, status and source are in the lists; a source list may contain "none" (rows without a source)."""
    out = []
    for e in mods:
        if mcs and e.get("mc", "1.7.10") not in mcs:
            continue
        if statuses and e.get("status", "").lower() not in statuses:
            continue
        if sources and (e.get("source") or "none").lower() not in sources:
            continue
        if name and norm(name) not in norm(e["name"]):
            continue
        out.append(e)
    return out


# ---------------------------------------------------------------- jars and dependencies

def jar_index(job_roots):
    """[(path, basename, normalized stem)] of every jar below the given folders (picker.listing, de-duplicated by path)."""
    seen, out = set(), []
    for r in job_roots:
        if not os.path.isdir(r):
            continue
        for p in picker.listing(r):
            if p not in seen:
                seen.add(p)
                b = os.path.basename(p)
                out.append((p, b, norm(re.sub(r"\.(jar|zip|litemod)$", "", b, flags=re.I))))
    return out


def find_jar(mc, key, index):
    """Best jar of Minecraft version mc whose normalized name contains key (a normalized mod name). Returns (path, note) or (None, reason).
    The version rule is the picker's: labelled-with-mc jars win, unlabelled ones are accepted, jars of another version never."""
    if len(key) < 3:
        return None, "name too short to match a jar"
    cands = [c for c in index if key in c[2]]
    if not cands:
        return None, "no jar matches"
    roots = [("", [c[0] for c in cands])]
    best = None
    for p, b, _ in sorted(cands, key=lambda c: (len(c[2]), c[2])):   # shortest name first: "ae2" before "ae2-extras"
        status, path, note = picker.pick(mc, b, roots)
        if status == "ok":
            ver = picker.infer(path)
            rank = 0 if ver == mc else 1
            if best is None or rank < best[0]:
                best = (rank, path, note)
    if best:
        return best[1], best[2]
    return None, "only jars of other Minecraft versions match"


def dep_hints(notes):
    """(required, hints): names the notes say are required ("requires X", "missing X"), and package roots of NoClassDefFound/ClassNotFound errors."""
    required = []
    for m in re.finditer(r"(?:requires|missing|depends on)\s+(?:mod\s+)?['\"]?([A-Za-z0-9_][\w .\-]{2,40}?)['\"]?(?:[,;:)(]|\s+(?:version|[>=<]|but|which|is|to|for)\b|$)", notes or ""):
        required.append(m.group(1).strip())
    hints = []
    for m in re.finditer(r"(?:NoClassDefFoundError|ClassNotFoundException):\s*([\w./$]+)", notes or ""):
        seg = re.split(r"[./]", m.group(1))
        for s in seg[:2]:
            if s and s.lower() not in NOT_DEPS and len(s) > 3:
                hints.append(s)
                break
    return required, hints


def load_deps(path):
    out = {}
    if not path:
        return out
    for line in pathlib.Path(path).read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        a, _, b = line.partition("\t")
        out[norm(a)] = [x.strip() for x in b.split(",") if x.strip()]
    return out


def resolve_row(row, index, extra_deps=None):
    """-> {"row", "jar", "deps": [paths], "skip": reason or None}"""
    mc = row.get("mc", "1.7.10")
    res = {"row": row, "jar": None, "deps": [], "skip": None}
    jar, why = find_jar(mc, norm(row["name"]), index)
    if not jar:
        res["skip"] = "no jar (" + why + ")"
        return res
    res["jar"] = jar
    required, hints = dep_hints(row.get("notes", ""))
    for d in (extra_deps or {}).get(norm(row["name"]), []):
        required.append(d)
    for d in required:
        p, why = find_jar(mc, norm(d), index)
        if p and p != jar:
            if p not in res["deps"]:
                res["deps"].append(p)
        elif not p and norm(d) != norm(row["name"]) and not re.search(r"forge|minecraft|fml|java|neoforge", d, re.I):
            res["skip"] = f"missing dependency {d} ({why})"
            return res
    for h in hints:   # a hint only adds a jar when one exists: the class may also be the mod's own
        p, _ = find_jar(mc, norm(h), index)
        if p and p != jar and p not in res["deps"]:
            res["deps"].append(p)
    return res


# ---------------------------------------------------------------- grouping

def plan_groups(resolved, group_size=3):
    """Testable rows -> groups of one Minecraft version, at most group_size mods each (their dependencies are shared). Rows that
    share a dependency go into the same group first. Returns (groups, skipped); a group is {"mc", "rows", "jars"}."""
    ok = [r for r in resolved if not r["skip"]]
    skipped = [r for r in resolved if r["skip"]]
    groups = []
    by_mc = {}
    for r in ok:
        by_mc.setdefault(r["row"].get("mc", "1.7.10"), []).append(r)
    for mc in sorted(by_mc):
        rows = sorted(by_mc[mc], key=lambda r: (sorted(os.path.basename(d).lower() for d in r["deps"]), r["row"]["name"].lower()))
        cur = None
        for r in rows:
            if cur is None or len(cur["rows"]) >= group_size:
                cur = {"mc": mc, "rows": [], "jars": []}
                groups.append(cur)
            cur["rows"].append(r)
            for j in [r["jar"]] + r["deps"]:
                if j not in cur["jars"]:
                    cur["jars"].append(j)
    return groups, skipped


def format_plan(groups, skipped):
    lines = []
    for i, g in enumerate(groups, 1):
        lines.append(f"group {i} (mc {g['mc']}, {len(g['rows'])} mods, {len(g['jars'])} jars)")
        for r in g["rows"]:
            lines.append(f"  {r['row']['name']} [{r['row'].get('status')}/{r['row'].get('source', 'none')}] -> {os.path.basename(r['jar'])}"
                         + ("  + " + ", ".join(os.path.basename(d) for d in r["deps"]) if r["deps"] else ""))
    tested = sum(len(g["rows"]) for g in groups)
    lines.append(f"{tested} mods in {len(groups)} groups, {len(skipped)} not testable")
    for r in skipped:
        lines.append(f"  not tested: {r['row']['name']} [{r['row'].get('mc')}]: {r['skip']}")
    return "\n".join(lines)


# ---------------------------------------------------------------- running

def free_gb():
    """Free physical memory in GB (Windows via PowerShell, Linux /proc/meminfo); None when unknown."""
    try:
        if os.name == "nt":
            out = subprocess.run(["powershell", "-NoProfile", "-Command", "(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory"],
                                 capture_output=True, text=True, timeout=60).stdout.strip()
            return int(out) / 1024 / 1024
        for line in open("/proc/meminfo"):
            if line.startswith("MemAvailable"):
                return int(line.split()[1]) / 1024 / 1024
    except Exception:
        pass
    return None


def wait_for_ram(min_gb, free=free_gb, sleep=time.sleep, log=print, poll=30):
    """Block until at least min_gb are free (never fails; an unknown amount counts as enough)."""
    waited = False
    while True:
        f = free()
        if f is None or f >= min_gb:
            return
        if not waited:
            log(f"  waiting for RAM: {f:.1f} GB free, {min_gb} GB needed")
            waited = True
        sleep(poll)


def free_port_base():
    """A base so that base+1 (server) and base+101 (rcon) are free."""
    import random
    for _ in range(50):
        base = random.randrange(26000, 30000)
        try:
            for p in (base + 1, base + 101):
                with socket.socket() as sk:
                    sk.bind(("", p))
            return base
        except OSError:
            continue
    return 25570


def verify_runner(group, name, port_base, timeout=1800):
    """Runs verify.sh on a temporary suite of the group's jars (the same function, picks and server settings as the regression suites).
    Returns (report path, log path) or (None, None) when the run produced no log."""
    SUITES.mkdir(parents=True, exist_ok=True)
    suite = SUITES / (name + ".txt")
    suite.write_text("".join(f"{group['mc']} {os.path.basename(j)}\n" for j in group["jars"]), encoding="utf-8")
    try:
        env = dict(os.environ, VERIFY_PORT_BASE=str(port_base))
        subprocess.run([os.environ.get("BASH", "bash"), "tools/dev/verify.sh", name], cwd=ROOT, env=env, timeout=timeout,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except subprocess.TimeoutExpired:
        pass
    finally:
        suite.unlink(missing_ok=True)
    d = ROOT / "run-verify" / name
    log, report = d / "logs" / "latest.log", d / "logs" / "legacyconnector-report.txt"
    return (report, log) if log.exists() and log.stat().st_size and report.exists() else (None, None)


def rate(report, log, label, mc, date, data_file):
    """Runs from_client.py against data_file (a copy of mods.json with the script placed beside it, so the real file stays untouched)."""
    if data_file == DATA:
        script = HERE / "from_client.py"
        tmp = None
    else:
        tmp = pathlib.Path(tempfile.mkdtemp(prefix="rerate-"))
        (tmp / "tools" / "compat").mkdir(parents=True)
        (tmp / "docs" / "data").mkdir(parents=True)
        shutil.copy(HERE / "from_client.py", tmp / "tools" / "compat" / "from_client.py")
        shutil.copy(data_file, tmp / "docs" / "data" / "mods.json")
        script = tmp / "tools" / "compat" / "from_client.py"
    r = subprocess.run([sys.executable, "-I", str(script), str(report), str(log), label, mc, date], capture_output=True, text=True)
    if tmp:
        shutil.copy(tmp / "docs" / "data" / "mods.json", data_file)
        shutil.rmtree(tmp, ignore_errors=True)
    return r.stdout.strip() or r.stderr.strip()


# ---------------------------------------------------------------- summary

def best_client(mods, name, mc, label_date):
    """Status of the client row rated by this run (source client, same date), else None."""
    hits = [e for e in mods if e.get("source") == "client" and e.get("mc") == mc and norm(e["name"]) == norm(name) and e.get("date") == label_date]
    return hits[0]["status"] if hits else None


def summarize(rows_old, mods_after, date, skipped, not_run=()):
    """Table old -> new per tested row, then the untested rows with the reason."""
    lines = ["| mod | mc | old | new |", "|---|---|---|---|"]
    changed = same = 0
    for r in rows_old:
        new = best_client(mods_after, r["name"], r.get("mc", "1.7.10"), date)
        if new is None:
            continue
        old = r["status"]
        mark = "" if new == old else " (changed)"
        changed += new != old
        same += new == old
        lines.append(f"| {r['name']} | {r.get('mc')} | {old} ({r.get('source', 'none')}) | {new}{mark} |")
    lines.append("")
    lines.append(f"{changed} changed, {same} unchanged, {len(skipped)} not tested, {len(not_run)} in groups that produced no log")
    for r in skipped:
        lines.append(f"not tested: {r['row']['name']} [{r['row'].get('mc')}]: {r['skip']}")
    for r in not_run:
        lines.append(f"not tested: {r['name']} [{r.get('mc')}]: the group's run produced no log")
    return "\n".join(lines)


def run_all(groups, skipped, date, args, runner=None, free=free_gb, sleep=time.sleep, log=print, data_file=None, rate_fn=rate):
    """Runs every group; returns the summary text. runner(group, name, port_base) -> (report, log) | (None, None)."""
    data_file = data_file or DATA
    runner = runner or (lambda g, n, p: verify_runner(g, n, p))
    work = data_file
    if args.dry_run:
        work = pathlib.Path(tempfile.mkdtemp(prefix="rerate-dry-")) / "mods.json"
        shutil.copy(data_file, work)
    rows_old = [r["row"] for g in groups for r in g["rows"]]
    not_run = []
    base = free_port_base() if runner is not None and not os.environ.get("RERATE_NO_PORT") else 25570
    for i, g in enumerate(groups, 1):
        wait_for_ram(args.min_free_gb, free, sleep, log)
        name = f"zz-rerate-{date.replace('-', '')}-{i}"
        log(f"group {i}/{len(groups)}: {', '.join(r['row']['name'] for r in g['rows'])}")
        report, lg = runner(g, name, base)
        if report is None:
            not_run += [r["row"] for r in g["rows"]]
            log("  no log produced")
            continue
        log("  " + rate_fn(report, lg, f"rerate {date}", g["mc"], date, work))
    after = json.loads(work.read_text(encoding="utf-8"))["mods"]
    if args.dry_run:
        shutil.rmtree(work.parent, ignore_errors=True)
    return summarize(rows_old, after, date, skipped, not_run)


def build_plan(args, data_file=None, roots=None):
    mods = json.loads((data_file or DATA).read_text(encoding="utf-8"))["mods"]
    rows = select(mods, split_list(args.mc), split_list(args.status), split_list(args.source), args.name)
    seen, uniq = set(), []
    for r in rows:   # one row per mod and version (several jar versions of a mod are listed separately in mods.json)
        k = (norm(r["name"]), r.get("mc", "1.7.10"))
        if k not in seen:
            seen.add(k)
            uniq.append(r)
    rows = uniq[:args.limit] if args.limit else uniq
    jar_root = pathlib.Path(os.environ.get("JAR_ROOT", "Y:/Programming/Minecraft/Retrofit"))
    index = jar_index(roots if roots is not None else [str(jar_root / d) for d in SEARCH] + [str(jar_root / BIG)])
    extra = load_deps(args.deps)
    resolved = [resolve_row(r, index, extra) for r in rows]
    groups, skipped = plan_groups(resolved, args.group_size)
    return rows, groups, skipped


def parse_args(argv):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--mc", default="", help="Minecraft version(s), comma list (default: all)")
    ap.add_argument("--status", default="fails", help="rating(s) to re-test, comma list (default fails)")
    ap.add_argument("--source", default="", help="source(s): client, corpus, probe, none (default: all)")
    ap.add_argument("--name", default="", help="only rows whose name contains this")
    ap.add_argument("--limit", type=int, default=0, help="at most N rows")
    ap.add_argument("--group-size", type=int, default=3)
    ap.add_argument("--min-free-gb", type=float, default=6.0)
    ap.add_argument("--deps", default="", help="TSV: mod name<TAB>dependency fragment,fragment")
    ap.add_argument("--date", default=time.strftime("%Y-%m-%d"))
    ap.add_argument("--plan", action="store_true", help="print the groups, start nothing")
    ap.add_argument("--dry-run", action="store_true", help="run, but do not write mods.json")
    return ap.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    rows, groups, skipped = build_plan(args)
    print(f"{len(rows)} rows selected")
    print(format_plan(groups, skipped))
    if args.plan or not groups:
        return 0
    text = run_all(groups, skipped, args.date, args)
    print(text)
    out = ROOT / "build" / "devtools"
    out.mkdir(parents=True, exist_ok=True)
    (out / f"rerate-{args.date}.txt").write_text(text + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
