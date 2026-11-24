#!/usr/bin/env python3
"""Static-state audit: lists static fields of the layer whose type or name suggests per-world state.

State that survives a server stop in the same JVM (single player: quit to title, open another world) and still
references objects of the old world is a defect class (#113). This scanner reads the sources (no build needed) and writes
tools/dev/agent-tasks/static-state-audit.tsv with one row per candidate field:

  class, field, type, category, lifecycle, cleared-where

category   what the type or name says the field may hold (level, server, registry, blockentity, entity, player, storage,
           scoreboard, recipe, scheduler, itemstack, dimension-map, cache); "?" marks name-only matches
lifecycle  weak (weak keys/values: dies with its referent), cleared (some code clears/reassigns it), none (never cleared)
cleared-where  files:lines where the field is cleared, reset or reassigned null/new, plus the lifecycle hooks of the owner file

Usage: python tools/dev/static_state_audit.py [--summary] [--out FILE]
Review `none` rows by hand; add a justified entry to tools/dev/agent-tasks/static-state-reviewed.tsv
(class<TAB>field<TAB>verdict<TAB>reason) to record a world-independent field (shown as verdict in the TSV).
"""
import os
import re
import sys
from collections import Counter, defaultdict

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC_DIRS = [os.path.join(ROOT, "src", "main", "java", "org", "kylepitcock", "legacyconnector"),
            os.path.join(ROOT, "src", "main", "java", "legacyconnector")]
REVIEWED = os.path.join(ROOT, "tools", "dev", "agent-tasks", "static-state-reviewed.tsv")
OUT = os.path.join(ROOT, "tools", "dev", "agent-tasks", "static-state-audit.tsv")

TYPE_CATS = [
    ("level", r"\b(ServerLevel|ClientLevel|Level|LevelAccessor|LevelReader|WorldServer|WorldClient|World|IBlockAccess|WorldProvider|DimensionType|ChunkAccess|LevelChunk|Chunk|IChunkProvider|ChunkProvider\w*|WorldGenLevel|WorldGenRegion)\b"),
    ("server", r"\b(MinecraftServer|IntegratedServer|PlayerList|ServerLifecycle\w*)\b"),
    ("registry", r"\b(RegistryAccess|HolderLookup\w*|Holder|HolderSet|Registry|Enchantment\w*|Biome|DamageType|DamageSource|Structure|ConfiguredFeature|PlacedFeature|TrimMaterial|TrimPattern|BannerPattern|JukeboxSong|PaintingVariant|TagKey|LootTable|ResourceManager|ReloadableServerResources|RecipeManager|ServerAdvancementManager)\b"),
    ("blockentity", r"\b(BlockEntity|TileEntity|LegacyBlockEntity)\b"),
    ("entity", r"\b(Entity|LivingEntity|EntityLivingBase|EntityPlayer\w*|Mob)\b"),
    ("player", r"\b(Player|ServerPlayer|LocalPlayer|EntityPlayerMP|GameProfile)\b"),
    ("storage", r"\b(SavedData|WorldSavedData|MapStorage|DimensionDataStorage|ISaveHandler|SaveHandler|MapData)\b"),
    ("scoreboard", r"\b(Scoreboard|Team|PlayerTeam|ScorePlayerTeam|Objective|ScoreObjective)\b"),
    ("recipe", r"\b(RecipeManager|RecipeHolder|Recipe|IRecipe)\b"),
    ("scheduler", r"\b(LevelTicks|ScheduledTick|TickNextTickData|TickPriority|TickingBlockEntity|ServerTickList|TickScheduler)\b"),
    ("itemstack", r"\b(ItemStack)\b"),
]
NAME_CATS = [
    ("dimension-map", r"(?i)(^|_)(by)?(dim|dimension)s?($|_|[A-Z])|dimId|perDim|dimensions"),
    ("level", r"(?i)\b(world|level)s?\b|(^|_)(WORLD|LEVEL)S?($|_)|(world|level)s?[A-Z_]|[a-z](World|Level)s?$"),
    ("server", r"(?i)(^|_)server(s)?($|_)|^server|Server$"),
    ("player", r"(?i)players?"),
    ("scheduler", r"(?i)(pending|queue|queued|scheduled|deferred|tick(s|ing)?(Queue|List)?|todo|backlog)"),
    ("cache", r"(?i)cache|memo|registry|holder|lookup"),
]
MUTABLE_TYPE = re.compile(r"\b(Map|HashMap|IdentityHashMap|WeakHashMap|ConcurrentHashMap|ConcurrentMap|LinkedHashMap|TreeMap|List|ArrayList|LinkedList|Set|HashSet|"
                          r"LinkedHashSet|TreeSet|Queue|Deque|ArrayDeque|ConcurrentLinkedQueue|ConcurrentLinkedDeque|CopyOnWriteArrayList|BlockingQueue|PriorityQueue|"
                          r"Collection|ThreadLocal|AtomicReference|WeakReference|SoftReference|Cache|LoadingCache|Long2ObjectMap\w*|Int2ObjectMap\w*|Object2\w+Map\w*|"
                          r"Long2\w+Map\w*|Int2\w+Map\w*|\w*Multimap|\w*BiMap|Table|BitSet|\w+\[\])\b")
IMMUTABLE_TYPE = re.compile(r"^(int|long|short|byte|boolean|char|float|double|String|Pattern|Logger|Gson|Random|Method|Field|Class(<.*>)?|Constructor(<.*>)?|MethodHandle|VarHandle|"
                            r"Identifier|ResourceLocation|AtomicInteger|AtomicLong|AtomicBoolean|Lock|ReentrantLock|ReadWriteLock|Object|UUID|Path|File|Charset|DecimalFormat|Codec<.*>|MapCodec<.*>|"
                            r"StreamCodec<.*>|Direction|RandomSource|Unsafe|Throwable)$")

FIELD_RE = re.compile(r"^\s*((?:(?:public|private|protected|static|final|volatile|transient)\s+)+)([\w.<>,?\[\]& ]+?)\s+(\w+)\s*(=|;)")
GEN_RE = re.compile(r"<(.*)>")


def java_files():
    for base in SRC_DIRS:
        for dp, _, fs in os.walk(base):
            for f in fs:
                if f.endswith(".java"):
                    yield os.path.join(dp, f)


def classname(path):
    rel = os.path.relpath(path, ROOT).replace("\\", "/")
    rel = rel.split("src/main/java/", 1)[1][:-5]
    return rel.replace("/", ".")


def categories(typ, name):
    cats = []
    for c, rx in TYPE_CATS:
        if re.search(rx, typ):
            cats.append(c)
    if not cats:
        for c, rx in NAME_CATS:
            if re.search(rx, name):
                cats.append(c + "?")
    return cats


def lifecycle_events(text):
    ev = []
    for tag in ("ServerStoppedEvent", "ServerStoppingEvent", "ServerAboutToStartEvent", "ServerStartingEvent", "LevelEvent.Unload",
                "ClientPlayerNetworkEvent", "FMLServerStoppedEvent", "FMLServerStoppingEvent", "LevelReleases", "LevelEvent.Load", "ClientLogout", "LoggingOut"):
        if tag in text:
            ev.append(tag)
    return ev


def main():
    args = sys.argv[1:]
    out = OUT
    if "--out" in args:
        out = args[args.index("--out") + 1]
    reviewed = {}
    if os.path.exists(REVIEWED):
        for ln in open(REVIEWED, encoding="utf-8"):
            p = ln.rstrip("\n").split("\t")
            if len(p) >= 4 and not ln.startswith("#"):
                reviewed[(p[0], p[1])] = (p[2], p[3])
    texts = {}
    for p in java_files():
        try:
            texts[p] = open(p, encoding="utf-8", errors="replace").read()
        except OSError:
            pass
    # clearing patterns per field name, searched in every file (static fields are mostly referenced by simple name in the owner)
    rows = []
    for p, text in texts.items():
        cn = classname(p)
        lines = text.split("\n")
        events = lifecycle_events(text)
        for i, ln in enumerate(lines):
            if "static" not in ln:
                continue
            m = FIELD_RE.match(ln)
            if not m or "static" not in m.group(1).split():
                continue
            mods, typ, name = m.group(1), m.group(2).strip(), m.group(3)
            if "(" in typ or typ in ("return", "new", "throw", "else"):
                continue
            if name.isupper() and "final" in mods.split() and not MUTABLE_TYPE.search(typ) and not re.search(TYPE_CATS[0][1], typ):
                continue  # constant
            if IMMUTABLE_TYPE.match(typ):
                continue
            cats = categories(typ, name)
            mutable = bool(MUTABLE_TYPE.search(typ))
            if not cats:
                continue
            if all(c.endswith("?") for c in cats) and not mutable:
                continue
            # lifecycle: weak / cleared / none
            span = "\n".join(lines[i:i + 3])
            weak = bool(re.search(r"weak|Weak|WeakHashMap|WeakReference|weakKeys|weakValues|softValues", typ + " " + span))
            clr_rx = re.compile(r"\b" + re.escape(name) + r"\s*(\.\s*(clear|removeIf|remove|set|reset|invalidateAll|cleanUp|clearAll|forEach)\s*\(|=\s*(null|new|Collections|List\.of|Map\.of|Set\.of)\b|=\s*[^=;]*(null|\bnew\b))")
            where = []
            simple = cn.rsplit(".", 1)[1]
            qual_rx = re.compile(r"\b" + re.escape(simple) + r"\s*\.\s*" + re.escape(name) + r"\b")
            for q, t in texts.items():
                own = q == p
                if not own and (simple + '.' + name not in t and simple + ' .' + name not in t or not qual_rx.search(t)):
                    continue
                tl = t.split(chr(10))
                for j, l2 in enumerate(tl):
                    if name in l2 and clr_rx.search(l2) and (own or qual_rx.search(l2)):
                        if own and j == i:
                            continue
                        where.append("%s:%d" % (os.path.basename(q)[:-5], j + 1))
                        if len(where) >= 6:
                            break
                if len(where) >= 6:
                    break
            final_ref = "final" in mods.split()
            life = "weak" if weak else ("cleared" if where else "none")
            if not mutable and cats and all(c.endswith("?") for c in cats):
                continue
            hooks = ",".join(events)
            # the class column of the reviewed file may be a dotted suffix (v1_7_10.net.minecraft.util.DamageSource); field "*" covers all fields
            rev = None
            for (rc, rf), v in reviewed.items():
                if rf in (name, "*") and (cn == rc or cn.endswith("." + rc)):
                    rev = v
                    break
            rows.append((cn, name, typ.replace("\t", " "), "+".join(cats), ("reviewed-" + rev[0]) if rev else life,
                         ";".join(where) + (" | hooks:" + hooks if hooks else "") + (" | " + rev[1] if rev else "")))
    rows.sort()
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write("class\tfield\ttype\tcategory\tlifecycle\tcleared-where\n")
        for r in rows:
            f.write("\t".join(r) + "\n")
    cnt = Counter()
    for r in rows:
        cnt[(r[3].split("+")[0].rstrip("?"), r[4])] += 1
    print("%d candidate fields -> %s" % (len(rows), os.path.relpath(out, ROOT)))
    if "--summary" in args:
        by = defaultdict(dict)
        for (c, l), n in cnt.items():
            by[c][l] = n
        for c in sorted(by):
            print("  %-14s %s" % (c, by[c]))


if __name__ == "__main__":
    main()
