"""Structural "soft or hard" verdict for the unresolved references of a Forge 1.20.1 jar (shared by tools/compat/probe.py and tools/dev/classify_1201_failures.py).

The layer's SoftLink1201 replaces every reference 1.21.1 cannot link by a stand-in that throws only when executed.  A jar whose unresolved references all
sit where nothing runs (a class no @Mod/mixin class reaches by class references, or an integration class of an optional mod) therefore still loads.
`scan_jar` reads the class files (pure Python), `analyse` finds the classes that hold each symbol and the classes reachable from the @Mod and mixin
classes (mixin classes count as reachable), `scan_entry` turns that into one record per jar and `soft_verdict` into the verdict.
"""
import collections, re, struct, zipfile

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



def alt_names(s):
    return {s, s.replace("net/neoforged/neoforge/", "net/minecraftforge/"), s.replace("net/neoforged/", "net/minecraftforge/")}


def scan_entry(jar_path, syms):
    """The per-jar record: classes, roots, one dict per unresolved symbol (area, holders, reachable holders, ...) and the mods.toml dependencies."""
    cl, holders, reach, roots = analyse(jar_path, syms)
    own = set(cl)
    per = []
    for k, s in syms:
        owner = s if k == "class" else s.split(".", 1)[0]
        ar = area(owner)
        if owner in own or ("/".join(owner.split("$")[0].split("/")) in own):
            ar = ("own", "the mod's own classes", None)
        hs = sorted(holders.get((k, s), ()))
        sup = lambda c: k == "class" and (cl[c]["sup"] in alt_names(s) or any(i in alt_names(s) for i in cl[c]["ifs"]))
        per.append({"k": k, "s": s, "area": ar[0], "dep": ar[1], "depid": ar[2], "holders": len(hs),
                    "reach": sum(1 for c in hs if c in reach), "integ": sum(1 for c in hs if INTEGRATION.search(c)),
                    "mixin": sum(1 for c in hs if cl[c]["mixin"]),
                    "super": sum(1 for c in hs if sup(c)), "reachsuper": sum(1 for c in hs if c in reach and sup(c)),
                    "reachnoninteg": sum(1 for c in hs if c in reach and not INTEGRATION.search(c)),
                    "holderClass": hs[0] if hs else ""})
    return {"classes": len(cl), "roots": len(roots), "syms": per, "deps": mods_toml_deps(jar_path)}


def sym_is_hard(s, deps):
    """One symbol: hard when a class that runs holds it (reachable, not an integration class), or a mixin class does; another mod's API only when that mod is mandatory."""
    if s["mixin"]:
        return True
    if s["area"] in ("mc", "own"):
        return bool(s["reachnoninteg"])
    return bool(s["reachnoninteg"]) and (not s["depid"] or bool(deps.get(s["depid"])))


def soft_verdict(entry):
    """(soft, hard symbol count, optional integrations that are off) of a scan_entry record. soft = no symbol is hard; a jar without a scan (no class found) is hard."""
    syms = entry["syms"]
    deps = entry["deps"]
    hard = [s for s in syms if sym_is_hard(s, deps)]
    off = sorted({s["dep"] for s in syms if s["area"] == "dep" and s["depid"] and not deps.get(s["depid"]) and not sym_is_hard(s, deps)} |
                 {s["dep"] for s in syms if s["area"] == "dep" and s["integ"] and not sym_is_hard(s, deps)})
    return (not hard and bool(syms)), len(hard), off
