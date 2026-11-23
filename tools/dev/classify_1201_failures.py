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

# --- other mods' API packages (internal names, longest prefix wins): (dependency label, mod id as in mods.toml) ----------------------------------------
DEPS = [
    ("mcjty/theoneprobe/", "The One Probe", "theoneprobe"), ("me/shedaniel/rei/", "REI", "roughlyenoughitems"), ("me/shedaniel/clothconfig2/", "Cloth Config", "cloth_config"),
    ("me/shedaniel/cloth/", "Cloth Config", "cloth_config"), ("me/shedaniel/architectury/", "Architectury", "architectury"), ("dev/architectury/", "Architectury", "architectury"),
    ("dev/emi/", "EMI", "emi"), ("mezz/jei/", "JEI", "jei"), ("com/simibubi/create/", "Create", "create"), ("com/jozufozu/flywheel/", "Flywheel", "flywheel"),
    ("dev/engine_room/flywheel/", "Flywheel", "flywheel"), ("com/tterrag/registrate/", "Registrate", "registrate"), ("net/createmod/catnip/", "Catnip", "catnip"),
    ("net/createmod/ponder/", "Ponder", "ponder"), ("net/createmod/", "Create (createmod)", "create"), ("top/theillusivec4/curios/", "Curios", "curios"),
    ("software/bernie/geckolib/", "GeckoLib", "geckolib"), ("software/bernie/", "GeckoLib", "geckolib"), ("dev/latvian/mods/kubejs/", "KubeJS", "kubejs"),
    ("dev/latvian/", "KubeJS/Rhino", "kubejs"), ("vazkii/patchouli/", "Patchouli", "patchouli"), ("vazkii/botania/", "Botania", "botania"), ("mekanism/", "Mekanism", "mekanism"),
    ("appeng/", "Applied Energistics 2", "ae2"), ("com/blamejared/crafttweaker/", "CraftTweaker", "crafttweaker"), ("com/blamejared/", "BlameJared libs", "crafttweaker"),
    ("snownee/jade/", "Jade", "jade"), ("mcp/mobius/waila/", "Waila/WTHIT", "waila"), ("snownee/", "Snownee libs", "snownee"), ("slimeknights/mantle/", "Mantle", "mantle"),
    ("slimeknights/tconstruct/", "Tinkers' Construct", "tconstruct"), ("net/darkhax/gamestages/", "Game Stages", "gamestages"), ("net/darkhax/bookshelf/", "Bookshelf", "bookshelf"),
    ("net/darkhax/", "Darkhax libs", "bookshelf"), ("com/teamresourceful/", "Resourceful libs", "resourcefullib"), ("com/github/alexthe666/citadel/", "Citadel", "citadel"),
    ("com/github/alexthe666/", "Alex's mods", "citadel"), ("journeymap/", "JourneyMap", "journeymap"), ("net/blay09/mods/balm/", "Balm", "balm"),
    ("net/blay09/", "Blay09 libs", "balm"), ("com/teamabnormals/blueprint/", "Blueprint", "blueprint"), ("net/mehvahdjukaar/moonlight/", "Moonlight Lib", "moonlight"),
    ("net/mehvahdjukaar/", "Moonlight/mehvahdjukaar mods", "moonlight"), ("com/supermartijn642/", "SuperMartijn642 libs", "supermartijn642corelib"),
    ("fuzs/puzzleslib/", "Puzzles Lib", "puzzleslib"), ("fuzs/", "Fuzs libs", "puzzleslib"), ("com/mrcrayfish/framework/", "Framework", "framework"),
    ("com/mrcrayfish/", "MrCrayfish libs", "framework"), ("cofh/", "CoFH Core", "cofh_core"), ("com/hollingsworth/arsnouveau/", "Ars Nouveau", "ars_nouveau"),
    ("com/hollingsworth/", "Ars libs", "ars_nouveau"), ("blusunrize/immersiveengineering/", "Immersive Engineering", "immersiveengineering"),
    ("com/rekindled/embers/", "Embers", "embers"), ("cc/tweaked/", "CC:Tweaked", "computercraft"), ("dan200/computercraft/", "CC:Tweaked", "computercraft"),
    ("thedarkcolour/", "Forestry/Modern Industrialization libs", "thedarkcolour"), ("com/github/glitchfiend/", "Glitchfiend (BoP libs)", "glitchcore"),
    ("terrablender/", "TerraBlender", "terrablender"), ("traben/", "ETF/EMF", "entity_texture_features"), ("me/jellysquid/", "Sodium API", "sodium"),
    ("org/embeddedt/embeddium/", "Embeddium API", "embeddium"), ("me/flashyreese/", "Sodium extra", "sodiumextra"), ("net/irisshaders/", "Iris API", "iris"),
    ("com/google/", "Guava/Gson", None), ("org/apache/", "Apache libs", None), ("kotlin/", "Kotlin", "kotlinforforge"), ("kotlinx/", "Kotlin", "kotlinforforge"),
    ("thedarkcolour/kotlinforforge/", "Kotlin for Forge", "kotlinforforge"), ("org/antlr/", "ANTLR", None), ("com/electronwill/", "NightConfig", None),
    ("org/jetbrains/", "Annotations", None), ("javax/", "JDK/javax", None), ("java/", "JDK", None), ("sun/", "JDK", None), ("org/lwjgl/", "LWJGL", None),
    ("io/netty/", "Netty", None), ("org/slf4j/", "SLF4J", None), ("org/apache/logging/", "Log4j", None), ("org/spongepowered/", "Mixin", None),
    ("com/llamalad7/", "MixinExtras", None), ("net/fabricmc/", "Fabric", "fabric"), ("org/joml/", "JOML", None),
]
DEPS.sort(key=lambda d: -len(d[0]))
MCPFX = ("net/minecraft/", "com/mojang/", "net/minecraftforge/", "net/neoforged/", "cpw/mods/", "legacyconnector/", "org/kylepitcock/legacyconnector/")
INTEGRATION = re.compile(r"(integration|compat|jei|rei|emi|plugin|waila|jade|probe|top/|kubejs|crafttweaker|curios|patchouli|\bjeresources)", re.I)


def area(owner):
    """('mc'|'fabric'|'dep'|'other', label, mod id) of a report symbol owner (internal name)."""
    if re.search(r"class_\d+", owner) or owner.startswith(("net/fabricmc/", "net/minecraft/class_")):
        return "fabric", "Fabric intermediary names", None
    if owner.startswith(MCPFX):
        return "mc", "Minecraft/Forge", None
    for p, label, mid in DEPS:
        if owner.startswith(p):
            return "dep", label, mid
    return "other", "/".join(owner.split("/")[:3]), None


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


# --- class file reader -----------------------------------------------------------------------------------------------------------------------------
def parse_class(b):
    """(name, super, interfaces, class refs, method/field refs {(owner, desc)}, is_mod, is_mixin) of a class file, or None."""
    if b[:4] != b"\xca\xfe\xba\xbe":
        return None
    n = struct.unpack(">H", b[8:10])[0]
    cp = [None] * n
    i, p = 1, 10
    while i < n:
        t = b[p]
        if t == 1:
            ln = struct.unpack(">H", b[p + 1:p + 3])[0]
            cp[i] = (1, b[p + 3:p + 3 + ln].decode("utf-8", "replace"))
            p += 3 + ln
        elif t in (3, 4):
            p += 5
        elif t in (5, 6):
            p += 9
            i += 1
        elif t in (7, 8, 16, 19, 20):
            cp[i] = (t, struct.unpack(">H", b[p + 1:p + 3])[0])
            p += 3
        elif t in (9, 10, 11, 12, 17, 18):
            cp[i] = (t, struct.unpack(">HH", b[p + 1:p + 5]))
            p += 5
        elif t == 15:
            p += 4
        else:
            return None
        i += 1

    def utf(k):
        return cp[k][1]

    def cls(k):
        return utf(cp[k][1]) if k and cp[k] and cp[k][0] == 7 else None
    p += 2
    this = cls(struct.unpack(">H", b[p:p + 2])[0])
    sup = cls(struct.unpack(">H", b[p + 2:p + 4])[0])
    ni = struct.unpack(">H", b[p + 4:p + 6])[0]
    ifs = [cls(struct.unpack(">H", b[p + 6 + 2 * k:p + 8 + 2 * k])[0]) for k in range(ni)]
    refs, members, mod, mixin = set(), set(), False, False
    for e in cp:
        if not e:
            continue
        if e[0] == 7:
            nm = utf(e[1])
            if nm.startswith("["):
                m = re.match(r"\[+L(.+);$", nm)
                nm = m.group(1) if m else None
            if nm:
                refs.add(nm)
        elif e[0] in (9, 10, 11):
            o = cls(e[1][0])
            nat = cp[e[1][1]]
            if o and nat and nat[0] == 12:
                members.add((o, utf(nat[1][1])))
        elif e[0] == 1:
            if e[1] == "Lnet/minecraftforge/fml/common/Mod;":
                mod = True
            elif e[1] == "Lorg/spongepowered/asm/mixin/Mixin;":
                mixin = True
    return this, sup, [x for x in ifs if x], refs, members, mod, mixin


def scan_jar(path):
    """{classes: {name: {sup, ifs, refs, members, mod, mixin}}} for a jar (members as [owner, desc] lists to be JSON-able)."""
    out = {}
    with zipfile.ZipFile(path) as z:
        for n in z.namelist():
            if n.endswith(".class") and not n.startswith("META-INF/"):
                try:
                    r = parse_class(z.read(n))
                except Exception:
                    r = None
                if r:
                    out[r[0]] = {"sup": r[1], "ifs": r[2], "refs": sorted(r[3]), "members": sorted(r[4]), "mod": r[5], "mixin": r[6]}
    return out


def mods_toml_deps(path):
    """{dep mod id: mandatory bool} from META-INF/mods.toml (regex: tolerant of old and new key spellings)."""
    deps = {}
    try:
        with zipfile.ZipFile(path) as z:
            name = next((n for n in ("META-INF/mods.toml", "META-INF/neoforge.mods.toml") if n in z.namelist()), None)
            if not name:
                return deps
            txt = z.read(name).decode("utf-8", "replace")
    except Exception:
        return deps
    for blk in re.split(r"\[\[dependencies\.[^\]]+\]\]", txt)[1:]:
        mid = re.search(r"modId\s*=\s*[\"']([^\"']+)", blk)
        if not mid:
            continue
        mand = re.search(r"mandatory\s*=\s*(true|false)", blk)
        typ = re.search(r"type\s*=\s*[\"'](\w+)", blk)
        required = (mand and mand.group(1) == "true") or (typ and typ.group(1) == "required")
        deps[mid.group(1).lower()] = bool(required)
    return deps


# --- analysis --------------------------------------------------------------------------------------------------------------------------------------
def analyse(jar_path, symbols):
    """For one jar: which classes hold the unresolved symbols and which of those a class reference walk from @Mod / mixin classes reaches."""
    cl = scan_jar(jar_path)
    own = set(cl)
    # classes holding a symbol
    holders = collections.defaultdict(set)   # symbol -> classes
    by_owner = collections.defaultdict(list)
    for c, d in cl.items():
        for o, desc in d["members"]:
            by_owner[o].append((c, desc))
    alt = lambda o: {o, o.replace("net/neoforged/neoforge/", "net/minecraftforge/"), o.replace("net/neoforged/", "net/minecraftforge/")}
    for kind, sym in symbols:
        if kind == "class":
            names = alt(sym)
            for c, d in cl.items():
                if names & set(d["refs"]):
                    holders[(kind, sym)].add(c)
        else:
            if kind == "method":
                owner, rest = sym.split(".", 1)
                desc = rest[rest.index("("):]
            else:
                owner, rest = sym.split(".", 1)
                desc = rest.split(":", 1)[1] if ":" in rest else ""
            for o in alt(owner):
                for c, d in by_owner.get(o, ()):
                    if d == desc or (kind == "field" and False):
                        holders[(kind, sym)].add(c)
    # reachability from @Mod and mixin classes
    roots = [c for c, d in cl.items() if d["mod"] or d["mixin"]]
    seen, stack = set(roots), list(roots)
    while stack:
        c = stack.pop()
        d = cl[c]
        nxt = set(d["refs"]) | ({d["sup"]} if d["sup"] else set()) | set(d["ifs"])
        for r in nxt:
            if r in cl and r not in seen:
                seen.add(r)
                stack.append(r)
    return cl, holders, seen, roots


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
            cl, holders, reach, roots = analyse(path, syms)
            own = set(cl)
            per = []
            for k, s in syms:
                owner = s if k == "class" else s.split(".", 1)[0]
                ar = area(owner)
                if owner in own or ("/".join(owner.split("$")[0].split("/")) in own):
                    ar = ("own", "the mod's own classes", None)
                hs = sorted(holders.get((k, s), ()))
                per.append({"k": k, "s": s, "area": ar[0], "dep": ar[1], "depid": ar[2], "holders": len(hs),
                            "reach": sum(1 for c in hs if c in reach), "integ": sum(1 for c in hs if INTEGRATION.search(c)),
                            "mixin": sum(1 for c in hs if cl[c]["mixin"]),
                            "super": sum(1 for c in hs if k == "class" and (cl[c]["sup"] in alt_names(s) or any(i in alt_names(s) for i in cl[c]["ifs"]))),
                            "reachsuper": sum(1 for c in hs if c in reach and k == "class" and (cl[c]["sup"] in alt_names(s) or any(i in alt_names(s) for i in cl[c]["ifs"]))),
                            "reachnoninteg": sum(1 for c in hs if c in reach and not INTEGRATION.search(c))})
            scan[name] = {"classes": len(cl), "roots": len(roots), "syms": per, "deps": mods_toml_deps(path)}
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


def alt_names(s):
    return {s, s.replace("net/neoforged/neoforge/", "net/minecraftforge/"), s.replace("net/neoforged/", "net/minecraftforge/")}


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
