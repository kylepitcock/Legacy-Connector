#!/usr/bin/env python3
"""Link probe: translates a directory of legacy mod jars offline (no game) and adds the result to docs/data/mods.json,
like the Sinytra Connector compatibility checker does for Fabric.

usage: probe.py <jar-dir> [--mc 1.7.10] [--date YYYY-MM-DD] [--version <layer version>] [--json out.json] [--dry-run] [--no-history] [--fail-on-regression]
       probe.py --from-json probe.json [...]     (convert an earlier run without translating again)

The jars are only read (test-mods/ stays gitignored, never commit or redistribute them); only results are written.
All jars are translated as ONE mod set so mods that extend each other link. Per jar, ./gradlew checkLegacyMod yields:
  links       every reference resolves, few placeholder stubs        -> status "loads" (site: "Ready to test")
  stubs       links, but STUB_LIMIT+ synthesized empty members       -> status "loads" (site: "Partly placeholder")
  gap_shim    missing old Minecraft/Forge functionality (our shim)   -> status "fails"
  gap_mod     missing another mod's API (NEI, Thaumcraft, AE2, ...)  -> status "fails"
  gap_lib     missing removed Java/library code                      -> status "fails"
  obsolete    engine patch with nothing to do on 1.21.1 -> not listed
A probe proves "translates and links", never "works in game": entries get source "probe", which the site ranks below
maintainer, client and corpus entries (a real run of the same mod always wins) and which never overwrites them.
Every run is also appended to docs/data/probe.json (counts + per-mod list), which the pie chart page docs/probe.html shows.
"""
import argparse, datetime, json, pathlib, re, subprocess, sys, tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / "docs" / "data" / "mods.json"
HISTORY = ROOT / "docs" / "data" / "probe.json"   # one entry per run: the pie chart page (docs/probe.html) reads it
STUB_LIMIT = 10   # a mod that links but uses this many synthesized (empty) legacy API members is "stubs", not "links"
KEEP_MODS = 10   # runs per Minecraft version that keep their per-mod list; older ones keep only the counts


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def run_probe(jar_dir, out):
    jars = sorted(pathlib.Path(jar_dir).rglob("*.jar"))
    if not jars:
        sys.exit(f"no jars in {jar_dir}")
    # -PlegacyJar is a comma-separated list: a jar whose name contains a comma is probed through a symlink with a safe name
    safe = pathlib.Path(tempfile.mkdtemp(prefix="probe-links-"))
    for i, j in enumerate(jars):
        if "," in str(j):
            link = safe / f"{i}-{j.name.replace(',', '_')}"
            link.symlink_to(j.resolve())
            jars[i] = link
    cmd = [str(ROOT / "gradlew"), "-q", "checkLegacyMod", f"-PlegacyJar={','.join(map(str, jars))}", f"-PcheckJson={out}"]
    print(f"probing {len(jars)} jar(s) as one mod set ...", flush=True)
    r = subprocess.run(cmd, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if r.returncode != 0 or not pathlib.Path(out).exists():
        sys.exit(r.stdout[-3000:] + "\nprobe failed")


# what a missing symbol belongs to: our shim (old Minecraft/Forge not built yet), old Java/library code, or another mod's API
LIB_PREFIXES = ("java.", "javax.", "sun.", "jdk.", "com.sun.", "com.google.", "org.apache.", "org.objectweb.", "org.lwjgl.", "io.netty.",
                "joptsimple.", "org.spongepowered.", "com.mojang.", "org.slf4j.", "gnu.", "paulscode.", "org.w3c.", "org.xml.")
# APIs the layer provides itself (LibraryBridges: NEI -> JEI, Baubles -> Curios, Waila -> Jade, CodeChickenLib), so a gap there is ours to close
SHIM_PREFIXES = ("legacyconnector.v1_", "net.minecraft.", "net.minecraftforge.", "cpw.mods.",
                 "codechicken.nei.", "codechicken.lib.", "baubles.", "mcp.mobius.waila.")


def cause(missing):
    """gap_shim (needs old Minecraft/Forge functionality), gap_mod (another mod's API) or gap_lib (removed Java/library code)."""
    if any(m.startswith(SHIM_PREFIXES) for m in missing):
        return "gap_shim"
    if any(not m.startswith(LIB_PREFIXES) for m in missing):
        return "gap_mod"
    return "gap_lib"


def kind(rec):
    """The category a record counts as: links, stubs (links but largely placeholders), gap_shim / gap_mod / gap_lib, obsolete."""
    if rec["result"] == "links":
        return "stubs" if rec.get("synthesizedMembers", 0) >= STUB_LIMIT else "links"
    if rec["result"] == "unresolved":
        return cause(rec.get("missing") or [])
    return rec["result"]


GAP_TEXT = {
    "gap_shim": "needs Minecraft or Forge features the layer does not have yet",
    "gap_mod": "needs code from another mod or API that is not available or does not match",
    "gap_lib": "uses old Java or library code that no longer exists",
}


def note(rec, layer):
    base = f"Automated check {layer}: " if layer else "Automated check: "
    k = kind(rec)
    if k in ("links", "stubs"):
        stubs = rec.get("synthesizedMembers", 0)
        text = f"{base}translates offline and everything it uses exists ({rec.get('classes', 0)} classes)."
        if k == "stubs":
            text += f" But {stubs} features it uses are empty placeholders, so large parts probably do nothing."
        elif stubs:
            text += f" {stubs} minor feature(s) it uses are empty placeholders."
        return text + " The game was not started: this only shows the jar translates, not that it works."
    miss = ", ".join(rec.get("missing", [])[:5])
    more = f" (+{rec['unresolved'] - 5} more)" if rec.get("unresolved", 0) > 5 else ""
    return f"{base}{GAP_TEXT.get(k, 'has missing pieces')}: {miss}{more}."


def merge(records, mc, date, layer):
    data = json.loads(DATA.read_text(encoding="utf-8"))
    listed = {}
    for e in data["mods"]:
        if e.get("mc", "1.7.10") == mc:
            listed.setdefault(norm(e["name"]), []).append(e)
    hidden = {norm(h) for h in data.get("hidden", [])}
    added = refreshed = skipped = 0
    for rec in records:
        if rec["result"] not in ("links", "unresolved") or rec.get("mc") != mc:
            continue
        status = "loads" if rec["result"] == "links" else "fails"
        for m in rec["mods"]:
            name = (m.get("name") or m["id"]).strip()
            key = norm(name)
            if key in hidden:
                continue
            existing = listed.get(key, [])
            if any(e.get("source") != "probe" for e in existing):
                skipped += 1   # a maintainer, client or corpus result is better than a link probe
                continue
            entry = {"name": name, "version": (m.get("version") or "").strip(), "mc": mc, "source": "probe", "date": date,
                     "status": status, "notes": note(rec, layer)}
            if existing:
                existing[0].update(entry)
                refreshed += 1
            else:
                data["mods"].append(entry)
                listed[key] = [entry]
                added += 1
    if added:
        data["mods"].sort(key=lambda e: e["name"].lower())
    return data, added, refreshed, skipped


RANK = {"links": 0, "stubs": 1, "gap_shim": 2, "gap_mod": 2, "gap_lib": 2, "unresolved": 2}   # worse is higher; "obsolete" is not compared
MAX_CHANGES = 100


def environment(layer, jars):
    """What the result was produced with, so a change of status can be told apart from a change of tooling."""
    import hashlib
    env = {"layer": layer, "jars": jars}
    props = ROOT / "gradle.properties"
    if props.exists():
        for line in props.read_text(encoding="utf-8").splitlines():
            for key, name in (("neo_version=", "neoforge"), ("minecraft_version=", "minecraft")):
                if line.startswith(key):
                    env[name] = line[len(key):].strip()
    index = ROOT / "build" / "generated" / "shimIndex" / "legacyconnector" / "shim-index.txt"
    if index.exists():
        env["shim"] = hashlib.sha1(index.read_bytes()).hexdigest()[:12]   # changes whenever the hand-written shim does
    return env


def compare(previous, rows):
    """Per-mod status changes against the previous run of the same Minecraft version: {"regressed": [[name, from, to]], "fixed": [...]}."""
    old = {r[0].lower(): r[2] for r in previous["mods"]}
    out = {"date": previous["date"], "version": previous.get("version", ""), "regressed": [], "fixed": []}
    for name, version, k, _ in rows:
        was = old.get(name.lower())
        if was is None or was not in RANK or k not in RANK or was == k:
            continue
        if RANK[k] > RANK[was]:
            out["regressed"].append([name, was, k])
        elif RANK[k] < RANK[was]:
            out["fixed"].append([name, was, k])
    out["regressedCount"], out["fixedCount"] = len(out["regressed"]), len(out["fixed"])
    out["regressed"], out["fixed"] = out["regressed"][:MAX_CHANGES], out["fixed"][:MAX_CHANGES]
    return out


def history(records, mc, date, layer):
    """Appends this run to docs/data/probe.json: counts per result and the per-jar list (mod, result, first unresolved reference)."""
    rows, counts, seen = [], {"links": 0, "stubs": 0, "gap_shim": 0, "gap_mod": 0, "gap_lib": 0, "obsolete": 0}, set()
    for r in records:
        k = kind(r) if r.get("result") else ""
        if r.get("mc") != mc or k not in counts or not r.get("mods"):
            continue   # other versions, non-mod jars and libraries without a mod are not part of the statistics
        m = r["mods"][0]
        key = ((m.get("name") or m["id"]).strip().lower(), (m.get("version") or "").strip())
        if key in seen:
            continue   # the same mod from two sources (a corpus and Modrinth) counts once
        seen.add(key)
        counts[k] += 1
        stubs = r.get("synthesizedMembers", 0)
        detail = r.get("reason") or (r.get("missing") or [""])[0] if r["result"] != "links" else (f"{stubs} synthesized stub(s)" if stubs else "")
        rows.append([(m.get("name") or m["id"]).strip(), (m.get("version") or "").strip(), k, detail])
    rows.sort(key=lambda x: x[0].lower())
    h = json.loads(HISTORY.read_text(encoding="utf-8")) if HISTORY.exists() else {"runs": []}
    h["runs"] = [x for x in h["runs"] if not (x["mc"] == mc and x["date"] == date and x["version"] == layer)]
    prior = [x for x in h["runs"] if x["mc"] == mc and x.get("mods")]
    prior.sort(key=lambda x: (x["date"], x["version"]))
    vs = compare(prior[-1], rows) if prior else None
    run = {"mc": mc, "date": date, "version": layer, "counts": counts, "env": environment(layer, len(records)), "mods": rows}
    if vs:
        run["vs"] = vs
    h["runs"].append(run)
    h["runs"].sort(key=lambda x: (x["mc"], x["date"], x["version"]))
    seen = {}
    for x in reversed(h["runs"]):
        seen[x["mc"]] = seen.get(x["mc"], 0) + 1
        if seen[x["mc"]] > KEEP_MODS:
            x["mods"] = []
    return h, counts, vs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("jar_dir", nargs="?")
    ap.add_argument("--mc", default="1.7.10")
    ap.add_argument("--date", default=datetime.date.today().isoformat())
    ap.add_argument("--version", default="", help="layer version shown in the notes")
    ap.add_argument("--json", help="keep the raw probe records here")
    ap.add_argument("--from-json", help="skip translation, convert this probe file")
    ap.add_argument("--no-history", action="store_true", help="do not add the run to docs/data/probe.json")
    ap.add_argument("--fail-on-regression", action="store_true", help="exit 2 when a mod got worse than in the previous run (after writing the results)")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()
    if a.from_json:
        path = a.from_json
    elif a.jar_dir:
        path = a.json or tempfile.mktemp(suffix=".json")
        run_probe(a.jar_dir, path)
    else:
        ap.error("jar directory or --from-json required")
    records = json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
    counts = {}
    for r in records:
        counts[r["result"]] = counts.get(r["result"], 0) + 1
    print("jars:", ", ".join(f"{v} {k}" for k, v in sorted(counts.items())))
    data, added, refreshed, skipped = merge(records, a.mc, a.date, a.version)
    print(f"{added} added, {refreshed} refreshed, {skipped} kept (better source), {len(data['mods'])} total")
    if not a.dry_run and (added or refreshed):
        DATA.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    if not a.no_history:
        h, c, vs = history(records, a.mc, a.date, a.version)
        print(f"run {a.mc} {a.version or '-'} {a.date}: " + ", ".join(f"{v} {k}" for k, v in c.items()))
        if not a.dry_run:
            HISTORY.write_text(json.dumps(h, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        if vs:
            print(f"since {vs['date']} {vs['version'] or ''}: {vs['fixedCount']} fixed, {vs['regressedCount']} regressed")
            for name, was, now in vs["regressed"]:
                print(f"  REGRESSED {name}: {was} -> {now}")
            if a.fail_on_regression and vs["regressedCount"]:
                sys.exit(2)


main()
