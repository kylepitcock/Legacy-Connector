#!/usr/bin/env python3
"""Repeatable offline 1.20.1 run over a Forge 1.20.1 pack's mods folder (the ATM 9 run of docs/wiki/Legacy-1.20.1-ATM9-Run.md).

usage: atm9_run.py <mods-dir> [--out build/atm9] [--label NAME] [--write] [--no-probe] [--no-native] [--version V]

One command: runs tools/compat/probe.py --mc 1.20.1 (gap1201 with -Pgap.translate=true, i.e. the report is of the jars AFTER the layer), keeps the raw
records and the full per-jar symbol list, asks Modrinth which mods also exist for NeoForge 1.21.1 (cached in <out>/native-cache.json), and writes
  <out>/summary.json   counts, top blockers, native/abandoned split (the file a later run is compared with; --baseline <summary.json> prints the delta)
  <out>/report.md      the tables the wiki page quotes
The mods folder is only read. Nothing from it is copied. Without --write docs/data/*.json stay untouched (probe --dry-run);
with --write the run is recorded in docs/data/mods.json and probe.json and mods with a 1.21.1 version get the note 'has a 1.21.1 version'.
--no-probe reuses <out>/records.json and <out>/detail.tsv. Needs JDK 21 on JAVA_HOME and a built neoforge artifact (see probe.py).
"""
import argparse, collections, datetime, json, pathlib, re, subprocess, sys, time, urllib.parse, urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
UA = "legacyconnector-atm9-run (https://github.com/kylepitcock)"


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def area(sym):
    owner = re.split(r"[.(:]", sym, 1)[0].replace("/", ".")
    if owner.startswith(("net.minecraftforge.", "net.neoforged.", "cpw.mods.", "legacyconnector.")):
        return "Forge API"
    if owner.startswith(("net.minecraft.client.", "com.mojang.blaze3d.", "com.mojang.math.")):
        return "Minecraft rendering/client"
    if owner.startswith(("net.minecraft.", "com.mojang.")):
        return "Minecraft common"
    if owner.startswith(("java.", "javax.", "sun.", "jdk.")):
        return "JDK"
    if owner.startswith(("org.spongepowered.", "mixin.", "org.objectweb.")):
        return "Mixin/ASM"
    return "Library or other mod"


def load_detail(path):
    miss = collections.defaultdict(set)   # jar -> {(kind, symbol)}
    for line in pathlib.Path(path).read_text(encoding="utf-8").splitlines():
        jar, k, sym = line.split("\t", 2)
        if k != "refs":
            miss[jar].add((k, sym))
    return miss


def api_get(url):
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=30) as r:
                return json.load(r)
        except Exception:
            time.sleep(2 * (attempt + 1))
    return None


def native_lookup(mod, cache):
    """Modrinth project with a NeoForge 1.21.1 build for this mod id/name, or None. Cached by mod id."""
    key = mod["id"].lower()
    if key in cache:
        return cache[key]
    found = None
    for q in dict.fromkeys([mod["id"], mod["name"]]):
        facets = urllib.parse.quote(json.dumps([["categories:neoforge"], ["versions:1.21.1"], ["project_type:mod"]]))
        res = api_get(f"https://api.modrinth.com/v2/search?query={urllib.parse.quote(q)}&limit=5&facets={facets}")
        time.sleep(0.25)
        for h in (res or {}).get("hits", []):
            if norm(h["slug"]) in (norm(mod["id"]), norm(mod["name"])) or norm(h["title"]) in (norm(mod["id"]), norm(mod["name"])):
                found = {"slug": h["slug"], "title": h["title"]}
                break
        if found:
            break
    cache[key] = found
    return found


def libraries():
    f = ROOT / "loader" / "src" / "main" / "resources" / "legacyconnector" / "forward" / "1.20.1" / "libraries.tsv"   # (probe.py runs main() on import)
    return {l.split("	")[0] for l in f.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")}


def analyse(records, miss, native_by_jar, libs):
    jars = []
    for r in records:
        info = r.get("mods") or []
        m = info[0] if info else {"id": r["jar"], "name": r["jar"], "version": ""}
        syms = miss.get(r["jar"], set())
        if r["result"] == "obsolete":
            cat = "library_native"   # replaced by the native 1.21.1 build of the same mod id (libraries.tsv)
        elif r["result"] == "links":
            cat = "links"
        else:
            cat = "unresolved"
        jars.append({"jar": r["jar"], "id": m["id"], "name": m["name"], "version": m["version"], "classes": r.get("classes", 0), "cat": cat,
                     "native": native_by_jar.get(r["jar"]), "unresolved": len(syms), "symbols": sorted(syms)})
    return jars


def blockers(jars, only=None):
    """symbol -> number of jars, per area; plus owner classes (a missing class, or the class a missing member belongs to)."""
    sym_jars, own_jars = collections.defaultdict(set), collections.defaultdict(set)
    for j in jars:
        if only and not only(j):
            continue
        for kind, sym in j["symbols"]:
            sym_jars[(kind, sym)].add(j["jar"])
            owner = sym.split(".")[0] if kind != "class" else sym
            own_jars[owner.split("(")[0].split(":")[0]].add(j["jar"])
    top_sym = sorted(sym_jars.items(), key=lambda kv: -len(kv[1]))
    top_own = sorted(own_jars.items(), key=lambda kv: -len(kv[1]))
    return top_sym, top_own


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mods_dir")
    ap.add_argument("--out", default=str(ROOT / "build" / "atm9"))
    ap.add_argument("--label", default="ATM 9")
    ap.add_argument("--version", default="", help="layer version label of the run")
    ap.add_argument("--write", action="store_true", help="record the run in docs/data/mods.json and probe.json")
    ap.add_argument("--no-probe", action="store_true")
    ap.add_argument("--no-native", action="store_true", help="skip the Modrinth query (cache only)")
    ap.add_argument("--baseline", help="an earlier summary.json: print the difference")
    ap.add_argument("--top", type=int, default=40)
    a = ap.parse_args()
    out = pathlib.Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    rec_path, det_path = out / "records.json", out / "detail.tsv"
    if not a.version:
        m = re.search(r"^mod_version=(\S+)", (ROOT / "gradle.properties").read_text(encoding="utf-8"), re.M)
        a.version = m.group(1) if m else ""
    if not a.no_probe or a.write:
        cmd = [sys.executable, str(ROOT / "tools" / "compat" / "probe.py"), "--mc", "1.20.1", "--version", a.version, "--source", "atm9"]
        cmd += ["--from-json", str(rec_path)] if a.no_probe else [a.mods_dir, "--json", str(rec_path), "--detail", str(det_path)]
        if not a.write:
            cmd.append("--dry-run")
        r = subprocess.run(cmd, cwd=ROOT)
        if r.returncode:
            sys.exit(r.returncode)
    records = json.loads(rec_path.read_text(encoding="utf-8"))
    miss = load_detail(det_path)
    cache_path = out / "native-cache.json"
    cache = json.loads(cache_path.read_text(encoding="utf-8")) if cache_path.exists() else {}
    native_by_jar = {}
    for r in records:
        for m in (r.get("mods") or [])[:1]:
            if r["result"] == "obsolete":
                continue
            if a.no_native and m["id"].lower() not in cache:
                continue
            n = native_lookup(m, cache)
            if n:
                native_by_jar[r["jar"]] = n
    cache_path.write_text(json.dumps(cache, indent=1), encoding="utf-8")
    jars = analyse(records, miss, native_by_jar, libraries())
    n_total = len(jars)
    cats = collections.Counter(j["cat"] for j in jars)
    nat = [j for j in jars if j["native"]]
    aband = [j for j in jars if not j["native"] and j["cat"] != "library_native"]
    summary = {"date": datetime.date.today().isoformat(), "label": a.label, "jars": n_total, "links": cats["links"], "unresolved": cats["unresolved"],
               "library_native": cats["library_native"], "has_1211_version": len(nat), "has_1211_version_links": sum(1 for j in nat if j["cat"] == "links"),
               "abandoned": len(aband), "abandoned_links": sum(1 for j in aband if j["cat"] == "links"),
               "abandoned_unresolved": sum(1 for j in aband if j["cat"] == "unresolved"),
               "unresolved_symbols_total": sum(j["unresolved"] for j in jars)}
    top_sym_all, top_own_all = blockers(jars)
    top_sym_ab, top_own_ab = blockers(jars, lambda j: not j["native"] and j["cat"] == "unresolved")
    summary["top_symbols"] = [[k[1], len(v)] for k, v in top_sym_all[:a.top]]
    summary["top_symbols_abandoned"] = [[k[1], len(v)] for k, v in top_sym_ab[:a.top]]
    summary["top_owners_abandoned"] = [[k, len(v)] for k, v in top_own_ab[:a.top]]
    area_jars = collections.defaultdict(set)
    for (kind, sym), v in top_sym_all:
        area_jars[area(sym)] |= v
    summary["jars_per_area"] = {k: len(v) for k, v in sorted(area_jars.items(), key=lambda kv: -len(kv[1]))}
    (out / "summary.json").write_text(json.dumps(summary, indent=1), encoding="utf-8")
    slim = [{k: v for k, v in j.items() if k != "symbols"} | {"first": [s for _, s in j["symbols"][:5]]} for j in jars]
    (out / "jars.json").write_text(json.dumps(slim, indent=1), encoding="utf-8")

    md = [f"# {a.label}: {n_total} jars, {summary['links']} link, {summary['unresolved']} unresolved, {summary['library_native']} native library",
          "", f"has a 1.21.1 version: {len(nat)} ({summary['has_1211_version_links']} link); abandoned/1.20.1-only: {len(aband)} "
          f"({summary['abandoned_links']} link, {summary['abandoned_unresolved']} unresolved)", "", "## Top symbols, all jars", "", "| jars | symbol |", "|---|---|"]
    md += [f"| {len(v)} | `{k[1]}` |" for k, v in top_sym_all[:a.top]]
    md += ["", "## Top symbols, abandoned jars with unresolved references", "", "| jars | symbol |", "|---|---|"]
    md += [f"| {len(v)} | `{k[1]}` |" for k, v in top_sym_ab[:a.top]]
    md += ["", "## Top owner classes, abandoned", "", "| jars | class |", "|---|---|"] + [f"| {len(v)} | `{k}` |" for k, v in top_own_ab[:a.top]]
    md += ["", "## Jars per area", ""] + [f"- {k}: {v}" for k, v in summary["jars_per_area"].items()]
    md += ["", "## Per mod", "", "| mod | version | result | unresolved | 1.21.1 | first |", "|---|---|---|---|---|---|"]
    for j in sorted(slim, key=lambda j: (j["cat"], j["name"].lower())):
        md.append(f"| {j['name']} | {j['version']} | {j['cat']} | {j['unresolved']} | {(j['native'] or {}).get('slug', '')} | {'; '.join('`' + s + '`' for s in j['first'][:2])} |")
    (out / "report.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in summary.items() if not k.startswith("top")}, indent=1))

    if a.write:   # mark mods with a native build in mods.json (rows exist after probe.py --write)
        data_path = ROOT / "docs" / "data" / "mods.json"
        data = json.loads(data_path.read_text(encoding="utf-8"))
        names = {norm(m["name"]): True for j in nat for m in [j]}
        changed = 0
        for e in data["mods"]:
            if e.get("mc") == "1.20.1" and e.get("source") == "probe" and norm(e["name"]) in names and "has a 1.21.1 version" not in e.get("notes", ""):
                e["notes"] = (e.get("notes", "") + " has a 1.21.1 version").strip()
                changed += 1
        data_path.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"{changed} mods.json rows noted 'has a 1.21.1 version'")
    if a.baseline:
        old = json.loads(pathlib.Path(a.baseline).read_text(encoding="utf-8"))
        for k in ("links", "unresolved", "library_native", "has_1211_version_links", "abandoned_links", "abandoned_unresolved", "unresolved_symbols_total"):
            print(f"{k}: {old.get(k)} -> {summary[k]} ({summary[k] - old.get(k, 0):+d})")


main()
