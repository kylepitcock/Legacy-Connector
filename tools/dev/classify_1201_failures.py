#!/usr/bin/env python3
"""Why do the Forge 1.20.1 jars that the offline link probe calls "fails" fail?  (docs/wiki/Legacy-1.20.1-Failure-Classes.md)

usage: classify_1201_failures.py <atm9_run_dir> <mods-dir> [--out build/failure-classes] [--tsv tools/dev/agent-tasks/1.20.1-failure-classes.tsv]
                                 [--no-modrinth] [--limit-dir <ignored>]

Input: the output of tools/compat/atm9_run.py (records.json, jars.json, detail.tsv) and the mods folder it ran over (read only).
Steps (every network answer and class scan is cached in --out, so a re-run is cheap):
  1. Modrinth (one request at a time, polite): sha1 -> project -> NeoForge 1.21.1 versions (class a: a native build exists).
  2. Class scan of each failing jar (pure Python class file reader): which classes of the jar contain the unresolved references (owner class + member descriptor
     of the report symbol), which are reachable from the @Mod class or a mixin class through class references, whether a missing class is a super type.
  3. One primary cause per jar (a engine/perf is checked before c/d), written to the TSV (jar, class, detail) and a JSON summary used by the wiki page.
Only reads jars; writes nothing outside --out and --tsv.
"""
import argparse, collections, hashlib, json, pathlib, re, struct, sys, time, urllib.parse, urllib.request, zipfile

UA = "legacyconnector-failure-classes (https://github.com/kylepitcock)"

# --- (b) rendering engine / performance / client library mods the layer should skip or bridge -------------------------------------------------------
ENGINE = {
    "embeddium": "rendering engine (Sodium port)", "rubidium": "rendering engine (Sodium port)", "oculus": "shader engine (Iris port)",
    "sodium": "rendering engine", "iris": "shader engine", "magnesium": "perf (Sodium add-on)", "rubidium_extra": "perf (Sodium add-on)",
    "embeddiumextras": "perf (Sodium add-on)", "embeddium_extras": "perf (Sodium add-on)", "sodiumextra": "perf (Sodium add-on)",
    "sodiumdynamiclights": "client lighting (Sodium add-on)", "immediatelyfast": "rendering performance", "ferritecore": "memory patch", "modernfix": "performance patch",
    "canary": "performance patch", "radium": "performance patch", "starlight": "lighting engine", "smoothboot": "performance patch", "entityculling": "culling",
    "moreculling": "culling", "badoptimizations": "performance patch", "c2me": "chunk performance", "clumps": "xp orb performance", "fastsuite": "recipe patch",
    "connectivity": "network patch", "dynamiclightsreforged": "client lighting", "lambdynlights": "client lighting", "ctm": "connected textures engine",
    "embeddiumplus": "perf (Sodium add-on)", "betterfpsrender": "performance", "fastleafdecay": "performance patch", "saturn": "performance patch",
    "memoryleakfix": "performance patch", "noisium": "worldgen performance", "lazydfu": "performance patch", "spark": "profiler", "observable": "profiler",
    "lithium": "performance", "rubidiumextra": "perf (Sodium add-on)", "oculus_flywheel": "shader engine bridge", "colorwheel": "shader engine bridge",
    "jeresources": "client", "fpsreducer": "performance", "mixinextras": "mixin library", "mixinbooster": "mixin library", "uncappedfps": "performance",
}
ENGINE_NAME = re.compile(r"embeddium|rubidium|oculus|sodium|iris shaders|ferrite|modernfix|immediatelyfast|canary|starlight|culling|c2me|smooth ?boot", re.I)

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / "compat"))
from soft_links import DEPS, MCPFX, INTEGRATION, area, parse_class, scan_jar, mods_toml_deps, analyse, alt_names, scan_entry   # noqa: E402

# --- Modrinth --------------------------------------------------------------------------------------------------------------------------------------
def http(url, cache, key):
    if key in cache:
        return cache[key]
    for attempt in range(4):
        try:
            time.sleep(0.35)
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=30) as r:
                cache[key] = json.load(r)
                return cache[key]
        except urllib.error.HTTPError as e:
            if e.code == 404:
                cache[key] = None
                return None
            time.sleep(3 * (attempt + 1))
        except Exception:
            time.sleep(3 * (attempt + 1))
    return None   # not cached: a later run retries


def sha1(p):
    h = hashlib.sha1()
    with open(p, "rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""):
            h.update(b)
    return h.hexdigest()


def native_of(jar_path, mod_id, cache):
    """{'project': slug, 'how': 'hash'|'slug', 'version': ...} when Modrinth has a NeoForge 1.21.1 build, {'project': None} when it knows the mod without one."""
    h = sha1(jar_path)
    v = http("https://api.modrinth.com/v2/version_file/%s?algorithm=sha1" % h, cache, "hash:" + h)
    how, pid = "hash", (v or {}).get("project_id")
    if not pid and mod_id:
        p = http("https://api.modrinth.com/v2/project/" + urllib.parse.quote(mod_id.lower().replace("_", "-")), cache, "slug:" + mod_id.lower())
        if p and "mod" in (p.get("project_type") or "mod"):
            pid, how = p["id"], "slug"
    if not pid:
        return {"project": None, "how": "unknown"}
    q = urllib.parse.urlencode({"loaders": '["neoforge"]', "game_versions": '["1.21.1"]'})
    vs = http("https://api.modrinth.com/v2/project/%s/version?%s" % (pid, q), cache, "native:" + pid)
    proj = http("https://api.modrinth.com/v2/project/" + pid, cache, "project:" + pid) or {}
    slug = proj.get("slug", pid)
    if vs:
        return {"project": slug, "how": how, "version": vs[0].get("version_number"), "native": True}
    return {"project": slug, "how": how, "native": False}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("run_dir")
    ap.add_argument("mods_dir")
    ap.add_argument("--out", default="build/failure-classes")
    ap.add_argument("--tsv", default="tools/dev/agent-tasks/1.20.1-failure-classes.tsv")
    ap.add_argument("--no-modrinth", action="store_true")
    a = ap.parse_args()
    run, mods, out = pathlib.Path(a.run_dir), pathlib.Path(a.mods_dir), pathlib.Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    jars = json.loads((run / "jars.json").read_text(encoding="utf-8"))
    miss = collections.defaultdict(list)
    for line in (run / "detail.tsv").read_text(encoding="utf-8").splitlines():
        j, k, s = line.split("\t", 2)
        if k != "refs":
            miss[j].append((k, s.replace(".", "/", 0) if k != "class" else s))
    cache_f = out / "modrinth-cache.json"
    cache = json.loads(cache_f.read_text()) if cache_f.exists() else {}
    scan_f = out / "scan.json"
    scan = json.loads(scan_f.read_text()) if scan_f.exists() else {}
    rows, summary = [], {}
    failing = [j for j in jars if j["cat"] == "unresolved"]
    for n, j in enumerate(failing):
        name = j["jar"]
        path = mods / name
        mid = j["id"]
        syms = sorted(miss.get(name, []))
        # normalise class symbols to internal names
        syms = [(k, s.replace(".", "/") if k == "class" else s) for k, s in syms]
        nat = {"project": None, "how": "skipped"}
        if not a.no_modrinth and path.exists():
            nat = native_of(path, mid, cache)
            if n % 20 == 0:
                cache_f.write_text(json.dumps(cache))
        if name not in scan and path.exists():
            scan[name] = scan_entry(path, syms)
            if n % 10 == 0:
                scan_f.write_text(json.dumps(scan))
        print(n, name, nat.get("project"), nat.get("native"), file=sys.stderr)
        summary[name] = {"id": mid, "name": j["name"], "native": nat, "scan": scan.get(name)}
    cache_f.write_text(json.dumps(cache))
    scan_f.write_text(json.dumps(scan))
    # the jars that link: which of them also have a native 1.21.1 build (the proposed rating excludes those too)
    links = {}
    for j in (x for x in jars if x["cat"] == "links"):
        path = mods / j["jar"]
        if not a.no_modrinth and path.exists():
            links[j["jar"]] = native_of(path, j["id"], cache)
    (out / "links-native.json").write_text(json.dumps(links, indent=1))
    cache_f.write_text(json.dumps(cache))
    (out / "classes.json").write_text(json.dumps(summary, indent=1))
    classify(summary, pathlib.Path(a.tsv), out)



def is_engine(rec):
    i = rec["id"].lower().replace("-", "").replace("_", "")
    return ENGINE.get(rec["id"].lower()) or ENGINE.get(i) or (ENGINE_NAME.search(rec["name"] or "") and "engine/perf name") or None


def classify(summary, tsv, out):
    """One primary cause per jar. Order: a native 1.21.1 release, b engine/perf, then by the kinds of unresolved symbols (c other mods only, d Minecraft/Forge, e other)."""
    rows, res = [], {}
    for name, rec in sorted(summary.items()):
        sc = rec["scan"] or {"syms": [], "deps": {}}
        syms = sc["syms"]
        by = collections.Counter(s["area"] for s in syms)
        deps = collections.Counter(s["dep"] for s in syms if s["area"] == "dep")
        others = collections.Counter(s["dep"] for s in syms if s["area"] == "other")
        mcn = by["mc"] + by["own"]   # own: a Minecraft member seen through the mod's own subclass (overrides of renamed/changed methods, inherited fields)
        info = {"mc": mcn, "dep": by["dep"], "other": by["other"], "own": by["own"], "fabric": by["fabric"], "deps": dict(deps), "others": dict(others)}
        nat = rec["native"]
        eng = is_engine(rec)
        if nat.get("native"):
            cls, det = "a", "native 1.21.1: %s %s (%s match)" % (nat["project"], nat.get("version"), nat["how"])
        elif eng:
            cls, det = "b", eng
        elif by["fabric"] and by["fabric"] >= 0.5 * max(1, len(syms)):
            cls, det = "e", "Fabric-origin names (%d symbols)" % by["fabric"]
        elif mcn == 0 and (by["dep"] or by["other"]):
            top = (deps + others).most_common(1)[0][0] if (deps or others) else "?"
            cls, det = "c", "only other mods' APIs; main: %s (%s)" % (top, ", ".join("%s %d" % kv for kv in (deps + others).most_common(4)))
        elif mcn:
            few = mcn <= 5
            cls = "d1" if few else "d2"
            samp = [s["s"] for s in syms if s["area"] in ("mc", "own")][:3]
            det = "%d Minecraft/Forge symbols: %s" % (mcn, "; ".join(samp))
        else:
            cls, det = "e", "only the mod's own members (%d): its overrides/own API after renaming, likely a report artefact" % by["own"]
        # soft analysis: is any offending class reachable from @Mod / mixin classes, and is it an optional integration
        tmix = sum(1 for s in syms if s["mixin"])
        reach_mc = sum(1 for s in syms if s["area"] == "mc" and s["reachnoninteg"])
        reach_any = sum(1 for s in syms if s["reach"])
        reach_super = sum(1 for s in syms if s["reachsuper"])
        mand = {d: v for d, v in sc["deps"].items() if v}
        dep_mand_hits = sorted({s["depid"] for s in syms if s["area"] == "dep" and s["depid"] and sc["deps"].get(s["depid"])})
        unloc = sum(1 for s in syms if not s["holders"])
        hard_mc = sum(1 for s in syms if s["area"] in ("mc", "own") and s["reachnoninteg"])
        hard_dep = sum(1 for s in syms if s["area"] in ("dep", "other") and s["reachnoninteg"] and (not s["depid"] or sc["deps"].get(s["depid"])))
        hard_own = 0
        hard_mixin = tmix
        soft = (hard_mc + hard_dep + hard_own + hard_mixin) == 0
        info.update({"unlocated": unloc, "hard_mc": hard_mc, "hard_dep": hard_dep, "hard_own": hard_own, "soft": soft, "nsyms": len(syms)})
        info.update({"mixin_syms": tmix, "mc_reach_noninteg": reach_mc, "reach_any": reach_any, "reach_super": reach_super, "mandatory_dep_hits": dep_mand_hits})
        res[name] = {"class": cls, "detail": det, **info, "id": rec["id"], "name": rec["name"]}
        rows.append((name, cls, det + ("  [soft: every unresolved reference sits in an integration class or no class reachable from @Mod/mixins]" if soft else "  [hard: reachable]")))
    tsv.write_text("jar\tclass\tdetail\n" + "\n".join("%s\t%s\t%s" % r for r in rows) + "\n", encoding="utf-8")
    (out / "classified.json").write_text(json.dumps(res, indent=1))
    print(collections.Counter(r["class"] for r in res.values()))


if __name__ == "__main__":
    main()
