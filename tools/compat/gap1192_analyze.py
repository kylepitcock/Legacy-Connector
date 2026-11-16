#!/usr/bin/env python3
"""EXPERIMENTAL (1.19.2 scoping, docs/wiki/Legacy-1.19.2-Estimate.md): classifies the unresolved symbols of a gap report of Forge 1.19.2 jars.

usage: gap1192_analyze.py <detail.tsv> <jar-dir> <diff-dir> <srg-mojang-1192.tsv> <forward-1.20.1-dir> <out-dir> [--limit 300]

  detail.tsv          <report>.detail.tsv of ./gradlew gap1201 -Pgap.translate=true (the layer as it is, 1.19.2 jars treated as 1.20.1 jars) or gap1192
  diff-dir            output of tools/dev/mc_member_diff.py (1.19.2 -> 1.20.1 by stable SRG ids: members-a/b.tsv, member-diff.tsv, class-diff.tsv)
  forward-1.20.1-dir  loader/src/main/resources/legacyconnector/forward/1.20.1 (class renames of the migration, applied backwards to compare with 1.20.1 names)

Writes <out-dir>/jars.tsv (per jar: unresolved by area), symbols.tsv (every Minecraft/Forge symbol: class a/b/c, shape, jar count) and summary.txt.
Classes: a = a pure rename/move between 1.19.2 and 1.20.1 exists (mechanical candidate), b = changed shape / removed between 1.19.2 and 1.20.1,
c = the symbol still exists unchanged in 1.20.1: the 1.20.1 layer lacks it too (not 1.19.2 work).
"""
import collections, pathlib, re, sys, zipfile

SHAPES = [
    ("creative tabs", r"CreativeModeTab|Item\$Properties\.tab|fillItemCategory|CreativeModeTabEvent"),
    ("GUI rendering (PoseStack/GuiComponent/Button/Font)", r"GuiComponent|renderTooltip|/Font\.draw|Button\.<init>|Button\$OnPress|RenderSystem\.(enable|disable)Texture|/AbstractWidget\.|Screen\.render|/Screen\.|ForgeHooksClient|ScreenEvent|ContainerScreenEvent|RenderGuiOverlayEvent|RenderGameOverlayEvent|ForgeIngameGui|IIngameOverlay|/Options\.|AbstractContainerScreen"),
    ("registry statics (Registry.X, BuiltinRegistries)", r"net/minecraft/core/Registry[.$;]|net/minecraft/core/Registry$|BuiltinRegistries|net/minecraft/core/DefaultedRegistry|WritableRegistry|MappedRegistry"),
    ("RegistryEvent / registries (Forge)", r"RegistryEvent|NewRegistryEvent|IForgeRegistry|ForgeRegistries|DeferredRegister|RegistryObject|RegisterEvent|GameData"),
    ("com.mojang.math (Vector3f/Matrix4f/Quaternion)", r"com/mojang/math/"),
    ("Material", r"level/material/Material[$;.]|level/material/Material$|MaterialColor"),
    ("damage sources", r"DamageSource|LivingDamageEvent|LivingHurtEvent"),
    ("text components (TextComponent/TranslatableComponent)", r"network/chat/(TextComponent|TranslatableComponent|KeybindComponent|Component\$Serializer|BaseComponent)"),
    ("chat/messages", r"ChatType|ServerChatEvent|ClientChatEvent|PlayerChatMessage|ChatPreview|SignedMessage|MessageSigner|SystemMessage|sendSystemMessage|sendMessage"),
    ("sounds/particles", r"SoundEvent|SoundEvents|ParticleType|ParticleTypes|SimpleParticleType|ParticleOptions|ParticleProvider|ParticleEngine|SoundSource"),
    ("network/packets", r"net/minecraft/network/(protocol|FriendlyByteBuf|Connection)|NetworkEvent|SimpleChannel|NetworkRegistry|NetworkDirection|PacketDistributor|NetworkHooks"),
    ("loot", r"LootContext|LootParams|LootTable|LootPool|LootItem|LootModifier|LootTableLoadEvent|storage/loot"),
    ("data generators", r"net/minecraft/data/|DataGenerator|DataProvider|HashCache|CachedOutput|GatherDataEvent|ExistingFileHelper|BlockStateProvider|ItemModelProvider|LanguageProvider|RecipeProvider"),
    ("recipes/smithing", r"Smithing|net/minecraft/world/item/crafting/|RecipeSerializer|RecipeType|IShapedRecipe|ShapedRecipe|ShapelessRecipe|Ingredient"),
    ("explosions", r"Explosion|ExplosionEvent"),
    ("worldgen (features/biomes/structures)", r"levelgen|world/level/biome|BiomeLoadingEvent|BiomeModifier|WorldgenRandom|ConfiguredFeature|PlacedFeature|StructureFeature|StructureStart|StructurePiece|ChunkGenerator|ChunkAccess|WorldGenLevel|FeaturePlaceContext"),
    ("rendering (entity renderers/models/layers/shaders)", r"net/minecraft/client/(renderer|model|particle)|com/mojang/blaze3d|EntityRenderersEvent|ModelEvent|RegisterRenderers|ModelBakeEvent|LayerDefinition|RenderLevelStageEvent|RenderLevelLastEvent|RenderHandEvent|TextureStitchEvent|ModelRegistryEvent|RegisterShadersEvent|ColorHandlerEvent|RegisterColorHandlersEvent|ClientRegistry|ItemBlockRenderTypes|RenderTypeLookup"),
    ("client other (resources/keys/gui screens)", r"net/minecraft/client/|net/minecraftforge/client/"),
    ("commands", r"CommandSourceStack|net/minecraft/commands/|RegisterCommandsEvent|CommandEvent|ArgumentTypes|ArgumentTypeInfos"),
    ("entities/attributes/AI", r"net/minecraft/world/entity/|EntityType|EntityAttributeCreationEvent|SpawnPlacements|MobSpawn|LivingEvent|EntityJoinWorldEvent|EntityJoinLevelEvent"),
    ("block/item/state", r"world/level/block|world/item/|BlockState|BlockBehaviour|ToolAction|ToolActions|ItemStack|Block\.|BlockEvent"),
    ("level/world/chunks/capabilities", r"net/minecraft/world/level/|net/minecraft/server/level|net/minecraftforge/common/capabilities|CapabilityManager|AttachCapabilitiesEvent|LazyOptional|WorldEvent|LevelEvent|ChunkEvent|TickEvent"),
    ("Forge: config/fml/mods", r"ForgeConfigSpec|ModLoadingContext|FMLJavaModLoadingContext|FMLCommonSetupEvent|InterModComms|ModList|IModBusEvent|FMLEnvironment|DistExecutor|ModConfig|ConfigTracker|IExtensionPoint|ConfigScreenHandler|NetworkConstants|FMLPaths|net/minecraftforge/fml/"),
    ("Forge: other API", r"net/minecraftforge/"),
    ("other Minecraft/server", r"net/minecraft/|com/mojang/"),
]
SHAPES = [(n, re.compile(p)) for n, p in SHAPES]


FABRIC = re.compile(r"net/minecraft/(class|method|field)_\d+|net/fabricmc/|net/minecraft/class_\d+")


STAGES = [
    ("stage 1 (tables: SRG overlay, registries, ctor/static rewrites)", ["rename/SRG-table rows (a)", "registry statics (Registry.X, BuiltinRegistries)", "other Minecraft/server", "sounds/particles", "level/world/chunks/capabilities", "explosions", "block/item/state", "commands", "chat/messages", "network/packets"]),
    ("stage 2 (stand-ins: creative tabs, Material, math, damage sources, entities, loot)", ["creative tabs", "Material", "com.mojang.math (Vector3f/Matrix4f/Quaternion)", "damage sources", "entities/attributes/AI", "loot"]),
    ("stage 3 (client: GUI, rendering, client other)", ["GUI rendering (PoseStack/GuiComponent/Button/Font)", "rendering (entity renderers/models/layers/shaders)", "client other (resources/keys/gui screens)"]),
    ("stage 4 (data and worldgen: smithing, recipes, worldgen, data generators)", ["recipes/smithing", "worldgen (features/biomes/structures)", "data generators"]),
]


def area(sym):
    owner = re.split(r"[.(:]", sym, 1)[0].replace("/", ".")
    if owner.startswith(("net.minecraftforge.", "net.neoforged.", "cpw.mods.", "legacyconnector.")):
        return "Forge API"
    if owner.startswith(("net.minecraft.client.", "com.mojang.blaze3d.", "com.mojang.math.")):
        return "Minecraft client"
    if owner.startswith(("net.minecraft.", "com.mojang.brigadier", "com.mojang.serialization", "com.mojang.datafixers", "com.mojang.authlib", "com.mojang.logging")):
        return "Minecraft common"
    if owner.startswith(("java.", "javax.", "sun.", "jdk.")):
        return "JDK"
    if owner.startswith(("org.spongepowered.", "mixin.", "org.objectweb.")):
        return "Mixin/ASM"
    return "Other mod or library"


def split_sym(kind, sym):
    if kind == "class":
        return sym, "", ""
    if kind == "method":
        m = re.match(r"^(.*?)\.([^.(]+)(\(.*)$", sym)
        return (m.group(1), m.group(2), m.group(3)) if m else (sym, "", "")
    m = re.match(r"^(.*?)\.([^.:]+):(.*)$", sym)
    return (m.group(1), m.group(2), m.group(3)) if m else (sym, "", "")


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    limit = 300
    if "--limit" in sys.argv:
        limit = int(sys.argv[sys.argv.index("--limit") + 1])
        args = [a for a in args if a != str(limit)]
    detail, jar_dir, diff_dir, table19, fwd, out = args[:6]
    out = pathlib.Path(out)
    out.mkdir(parents=True, exist_ok=True)
    diff = pathlib.Path(diff_dir)

    srg19 = {}
    for line in open(table19, encoding="utf-8"):
        t = line.rstrip("\n").split("\t")
        if len(t) == 2:
            srg19[t[0]] = t[1]

    # reverse of the layer's class renames (1.20.1 name <- name after the migration): symbols are compared with 1.20.1 names
    rev = {}
    for f in pathlib.Path(fwd).glob("mc-classes.tsv"):
        for line in open(f, encoding="utf-8"):
            if line.startswith("#"):
                continue
            a = line.rstrip("\n").split("\t")
            if len(a) == 2 and "(" not in a[0]:
                rev.setdefault(a[1], a[0])

    def unrename(s):
        return re.sub(r"[A-Za-z0-9_/$]+", lambda m: rev.get(m.group(0), m.group(0)) if "/" in m.group(0) else m.group(0), s)

    # 1.20.1 (b) and 1.19.2 (a) member sets
    def load_members(path):
        classes, mem, fld = set(), set(), set()
        by_name = collections.defaultdict(set)
        for line in open(path, encoding="utf-8"):
            o, n, d, sid = (line.rstrip("\n").split("\t") + ["", "", "", ""])[:4]
            if sid == "class":
                classes.add(o)
            elif d:
                mem.add((o, n, d))
                by_name[(n, d)].add(o)
            else:
                fld.add((o, n))
                by_name[(n, "")].add(o)
        return classes, mem, fld, by_name

    k20, m20, f20, bn20 = load_members(diff / "members-b.tsv")
    k19, m19, f19, bn19 = load_members(diff / "members-a.tsv")
    status = {}   # (owner, name) in 1.19.2 -> status vs 1.20.1 from member-diff
    id_by_19 = {}
    for line in open(diff / "member-diff.tsv", encoding="utf-8"):
        sid, kind, a, b, st = line.rstrip("\n").split("\t")
        id_by_19[a] = (sid, b, st)
    cls_status = {}
    removed19, added20 = collections.defaultdict(list), collections.defaultdict(list)
    for line in open(diff / "class-diff.tsv", encoding="utf-8"):
        s, a, b, st = line.rstrip("\n").split("\t")
        cls_status[a] = (b, st)
        if st == "added":
            added20[b.rsplit("/", 1)[-1].split("$")[-1]].append(b)
    for line in open(diff / "class-diff.tsv", encoding="utf-8"):
        s, a, b, st = line.rstrip("\n").split("\t")
        if st == "removed":
            removed19[a.rsplit("/", 1)[-1].split("$")[-1]].append(a)

    # jar kinds
    forge = {}
    for j in pathlib.Path(jar_dir).glob("*.jar"):
        try:
            names = set(zipfile.ZipFile(j).namelist())
        except Exception:
            continue
        forge[j.name] = ("META-INF/mods.toml" in names, "fabric.mod.json" in names or "quilt.mod.json" in names)

    fabric_dropped = 0
    refs = {}
    miss = collections.defaultdict(set)
    id_syms = set()   # symbols that still carried an SRG id: the 1.20.1 table does not know the id (names via the 1.19.2 table)
    for line in open(detail, encoding="utf-8"):
        jar, kind, sym = line.rstrip("\n").split("\t", 2)
        if kind == "refs":
            refs[jar] = int(sym) if sym.isdigit() else 0
        elif kind in ("class", "method", "field"):
            if FABRIC.search(sym):
                fabric_dropped += 1   # Fabric code of a multi-loader jar (intermediary names): never loaded on Forge
                continue
            had_id = re.search(r"\b[mf]_\d+_", sym) is not None
            sym = re.sub(r"\b[mf]_\d+_", lambda m: srg19.get(m.group(0), m.group(0)), sym)
            miss[jar].add((kind, sym))
            if had_id:
                id_syms.add((kind, sym))

    areas = ["Minecraft common", "Minecraft client", "Forge API", "JDK", "Mixin/ASM", "Other mod or library"]
    with open(out / "jars.tsv", "w", encoding="utf-8") as f:
        f.write("jar\tforge\tfabric\trefs\t" + "\t".join(areas) + "\tstatus\n")
        n_links = n_core = n_forge_jars = n_zero = 0
        for jar in sorted(refs):
            c = collections.Counter(area(s) for k, s in miss.get(jar, ()) if True)
            core = c["Minecraft common"] + c["Minecraft client"] + c["Forge API"] + c["JDK"] + c["Mixin/ASM"]
            total = sum(c.values())
            fg = forge.get(jar, (False, False))
            st = "links" if total == 0 else ("links but for other mods/libraries" if core == 0 else "unresolved")
            if refs[jar] == 0:
                st = "no code"
            f.write(f"{jar}\t{int(fg[0])}\t{int(fg[1])}\t{refs[jar]}\t" + "\t".join(str(c[a]) for a in areas) + f"\t{st}\n")

    # symbol table (Minecraft common/client + Forge API only)
    sym_jars = collections.defaultdict(set)
    for jar, ss in miss.items():
        for k, s in ss:
            if area(s) in ("Minecraft common", "Minecraft client", "Forge API", "Mixin/ASM"):
                sym_jars[(k, s)].add(jar)

    def classify(kind, sym):
        if area(sym) in ("Forge API", "Mixin/ASM"):
            return "F", "Forge API (Forge 43 -> 47 and Forge -> NeoForge: not diffed here, needs the Forge jars)"
        sym20 = unrename(sym)
        owner, name, desc = split_sym(kind, sym20)
        base = owner.split("$")[0]
        if kind == "class":
            if sym20 in k20 and sym20 in k19:
                return "c", "class unchanged in 1.20.1"
            if sym20 in k20 and sym20 not in k19:
                return "c", "class exists in 1.20.1 (not in 1.19.2: renamed by the id-join or a later class)"
            cand = [c for c in added20.get(sym20.rsplit("/", 1)[-1].split("$")[-1], []) if c != sym20]
            if cand:
                return "a", "class moved/renamed -> " + cand[0]
            if sym20 in k19:
                return "b", "class removed after 1.19.2"
            return "n", "class absent from both (Forge API or other)"
        if kind == "field":
            if (owner, name) in f20 and (kind, sym) in id_syms:
                return "a", "SRG id absent from the 1.20.1 table (1.20.1 keeps the Mojang name), member unchanged: the 1.19.2 table names it"
            if (owner, name) in f20:
                return "c", "field unchanged in 1.20.1"
            others = bn20.get((name, ""), set())
            if owner in k19 and owner not in k20:
                return "b", "owner class removed"
            if (owner, name) in f19:
                return ("b", "field removed in 1.20.1")
            if owner in k19 and owner in k20:
                return ("c", "unmapped member (enum constant, record component) of a class that exists in both")
            return ("n", "not a 1.19.2 member")
        key = f"{owner}.{name}{desc}"
        if (owner, name, desc) in m20 and (kind, sym) in id_syms:
            return "a", "SRG id absent from the 1.20.1 table (1.20.1 keeps the Mojang name), member unchanged: the 1.19.2 table names it"
        if (owner, name, desc) in m20:
            return "c", "method unchanged in 1.20.1"
        if key in id_by_19:
            sid, b, st = id_by_19[key]
            if st == "renamed":
                return "a", "renamed in 1.20.1 -> " + b
            if st == "moved":
                return "a", "moved in 1.20.1 -> " + b
            if st == "desc":
                return "b", "signature changed -> " + b
            if st == "removed":
                return "b", "removed in 1.20.1"
        if name == "<init>":
            return ("b", "constructor changed/removed") if (owner, name, desc) in m19 else ("n", "not a 1.19.2 constructor")
        if (owner, name, desc) in m19:
            return "b", "removed in 1.20.1 (no id)"
        if (name, desc) in bn20:
            return "c", "inherited/other owner in 1.20.1"
        if (name, desc) in bn19:
            return "b", "inherited member removed in 1.20.1 (declared in a superclass or interface in 1.19.2 only)"
        if owner in k19 and owner in k20:
            return "u", "member of a class that exists in both, in neither list (synthesised by the layer or not mapped)"
        return "n", "not a Minecraft 1.19.2 member (mod, library or Forge)"

    def shape(sym):
        for n, rx in SHAPES:
            if rx.search(sym):
                return n
        return "other"

    rows = []
    for (k, s), jars in sym_jars.items():
        cl, why = classify(k, s)
        rows.append((len(jars), k, s, cl, why, shape(s)))
    rows.sort(key=lambda r: (-r[0], r[2]))
    with open(out / "symbols.tsv", "w", encoding="utf-8") as f:
        f.write("jars\tkind\tsymbol\tclass\twhy\tshape\n")
        for r in rows:
            f.write("\t".join(map(str, r)) + "\n")

    with open(out / "summary.txt", "w", encoding="utf-8") as f:
        def w(t=""):
            f.write(t + "\n")
            print(t)
        forge_only = [j for j in refs if forge.get(j, (False, False))[0]]
        w(f"Fabric-intermediary symbols dropped (multi-loader jars): {fabric_dropped}")
        w(f"jars with code: {sum(1 for j in refs if refs[j] > 0)} of {len(refs)}; forge (mods.toml): {len(forge_only)}; fabric/quilt marker: {sum(1 for j in refs if forge.get(j, (0, 0))[1])}")
        st = collections.Counter()
        for line in open(out / "jars.tsv", encoding="utf-8").read().splitlines()[1:]:
            st[line.split("\t")[-1]] += 1
        w("jar status: " + ", ".join(f"{k} {v}" for k, v in st.most_common()))
        w("distinct Minecraft/Forge symbols: %d; occurrences (jar x symbol): %d" % (len(rows), sum(r[0] for r in rows)))
        byc = collections.defaultdict(lambda: [0, 0])
        for r in rows:
            byc[r[3]][0] += 1
            byc[r[3]][1] += r[0]
        w("by class (distinct/occurrences): " + ", ".join(f"{k}: {v[0]}/{v[1]}" for k, v in sorted(byc.items())))
        w("")
        w("shape x class (distinct symbols / occurrences / jars):")
        sh = collections.defaultdict(lambda: collections.defaultdict(lambda: [0, 0, set()]))
        for (k, s), jars in sym_jars.items():
            cl, _ = classify(k, s)
            e = sh[shape(s)][cl]
            e[0] += 1
            e[1] += len(jars)
            e[2] |= jars
        for name in sorted(sh, key=lambda n: -sum(v[1] for v in sh[n].values())):
            w(f"  {name}: " + "; ".join(f"{c}={v[0]}/{v[1]}/{len(v[2])}" for c, v in sorted(sh[name].items())))
        w("")
        # what-if: jars whose only blockers are 1.19.2-specific (class a/b) cleared by fixing whole shapes, greedily (the most jars first)
        classes = {}
        for (k, sy), jars in sym_jars.items():
            classes[(k, sy)] = classify(k, sy)[0]
        blockers = collections.defaultdict(lambda: collections.defaultdict(set))   # jar -> tag -> symbols
        other = collections.defaultdict(set)                                      # jar -> classes c/F/u/n it also has
        for (k, sy), jars in sym_jars.items():
            cl = classes[(k, sy)]
            for j in jars:
                if cl == "a":
                    blockers[j]["rename/SRG-table rows (a)"].add(sy)
                elif cl == "b":
                    blockers[j][shape(sy)].add(sy)
                else:
                    other[j].add(cl)
        coded = [j for j in refs if refs[j] > 0]
        w("")
        w("what-if (1.19.2 blockers only; c = 1.20.1 layer gaps, F = Forge API, u, n are NOT cleared by this work):")
        clean = [j for j in coded if not blockers[j] and not other[j]]
        w(f"  jars with no unresolved Minecraft/Forge symbol at all: {len(clean)} of {len(coded)}")
        only_ab = [j for j in coded if blockers[j] and not other[j]]
        w(f"  jars blocked ONLY by 1.19.2 deltas (a/b): {len(only_ab)}; blocked by 1.19.2 deltas and by other classes: {sum(1 for j in coded if blockers[j] and other[j])}; only by other classes (c/F/u/n): {sum(1 for j in coded if not blockers[j] and other[j])}")
        remaining = {j: set(blockers[j]) for j in coded if blockers[j]}
        cleared = len(clean) + 0
        fixed = []
        w("  greedy order of shapes to fix (jars that then link fully / jars cleared of 1.19.2 blockers):")
        total_clear_ab = 0
        while remaining:
            cnt = collections.Counter()
            sole = collections.Counter()
            for j, tags in remaining.items():
                for t in tags:
                    cnt[t] += 1
                if len(tags) == 1:
                    sole[next(iter(tags))] += 1
            t = (sole.most_common(1)[0][0] if sole else cnt.most_common(1)[0][0])
            fixed.append(t)
            for tags in remaining.values():
                tags.discard(t)
            done = [j for j, tags in remaining.items() if not tags]
            for j in done:
                del remaining[j]
                total_clear_ab += 1
                if not other[j]:
                    cleared += 1
            w(f"   {len(fixed):2d}. {t}: cleared of 1.19.2 blockers {total_clear_ab}, linking fully {cleared}")
        # the staged plan of the estimate page: cumulative jars that link (c/F/u/n of the 1.20.1 layer left aside) after each stage
        w("  staged plan (cumulative, jars linking fully / jars cleared of 1.19.2 blockers):")
        done_tags = set()
        for name, tags in STAGES:
            done_tags |= set(tags)
            cleared_ab = sum(1 for j in coded if blockers[j] and set(blockers[j]) <= done_tags)
            linking = sum(1 for j in coded if set(blockers[j]) <= done_tags and not other[j])
            w(f"   {name}: cleared {cleared_ab}, linking fully {linking}")
        w("")
        areac = collections.Counter()
        for j, ss in miss.items():
            for k, s in ss:
                areac[area(s)] += 1
        w("unresolved symbol occurrences by area: " + ", ".join(f"{k} {v}" for k, v in areac.most_common()))


if __name__ == "__main__":
    main()
