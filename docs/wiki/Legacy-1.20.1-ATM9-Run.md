# 1.20.1 offline run over the All the Mods 9 pack

Back to [Legacy-1.20.1-Estimate](Legacy-1.20.1-Estimate.md) (section 9 has the earlier 44-jar and 140-jar corpora). Run on 2026-11-07, layer 0.4.5, no game started: this is
the link probe (`gap1201` with `-Pgap.translate=true`), so "links" means every reference resolves after the layer's renames, facades and migration helpers, never "works in game".

## 1. Method

- Input: the `mods` folder of a Forge 1.20.1 (47.4.13) ATM 9 instance, read only. 506 top-level `*.jar` (the instance also has 12 `*.jar.disabled` files, which are ignored, and a
  `.connector/` folder with 49 Fabric jars remapped by Sinytra Connector, which the probe does not read: they are not Forge jars).
- `tools/compat/atm9_run.py <mods-dir>` runs `probe.py --mc 1.20.1` (every jar is translated by `Forge1201Transformer`, then every reference is resolved against Minecraft 1.21.1 + NeoForge
  + the other jars of the set), keeps the raw records and the full per-jar symbol list, and asks the Modrinth search API whether a mod id/name has a NeoForge 1.21.1 build.
- Jars whose mod ids are all in `forward/1.20.1/libraries.tsv` are not probed (`obsolete`): the layer replaces them with the native 1.21.1 build (`NativeLibraries1201`).
- Limits of the "has a 1.21.1 version" column: Modrinth only (CurseForge needs an API key), matched by slug or title equal to the mod id or name. Mods that publish only on
  CurseForge (the FTB suite, Structurize and MineColonies, Croptopia...) can have a 1.21.1 build that is not seen, so "abandoned" below is an upper bound, and the matcher can
  miss mods whose Modrinth title differs from the mod name. Spot checks (Botania, Tinkers' Construct, Alex's Mobs, Blood Magic) agree with the Modrinth API: no NeoForge 1.21.1 file.
- Three tool defects were found and fixed while setting this up (they changed the numbers): a pack of 500 jars overflowed the Windows command line (`gap1201` now takes `@file`),
  the layer's own migrate helpers (`org.kylepitcock.legacyconnector.v1_20_1.migrate.*`) were reported as missing classes in 300 jars, and `DataConverter1201` crashed on a loot
  condition that is an object (translation of the whole set stopped).

## 2. Counts (baseline, 506 jars)

| Category | Jars |
|---|---|
| Links (no unresolved reference) | 50 |
| Unresolved references | 438 (29,363 distinct jar/symbol pairs) |
| Native library replaced by the 1.21.1 build (`libraries.tsv`) | 18 (Architectury, Balm, Blueprint, Bookshelf, Caelus, Citadel, Cloth Config, Curios, Framework, GeckoLib 4, Jade, JEI, Mantle, Moonlight, Patchouli, Player Animator, Resourceful Config, Resourceful Lib) |
| of the 488 probed jars: has a 1.21.1 version (Modrinth) | 295 (33 link, 262 unresolved) |
| of the 488 probed jars: abandoned / 1.20.1-only (none found) | 193 (17 link, 176 unresolved) |

Jars per kind of blocker (a jar counts in every row it has): Minecraft common 388, library or other mod 361, Forge API 326, Minecraft rendering/client 228, JDK 32 (`com.sun.management`, `org.w3c.dom`...), Mixin/ASM 2.
Among the 176 abandoned unresolved jars: 96 miss Minecraft, Forge and other-mod symbols together, 31 Minecraft plus other mods, 15 Minecraft plus Forge, 9 only Minecraft, 6 only Forge API, 5 only another mod.
54 abandoned jars miss 10 symbols or fewer (AE2 Things, Botarium, FTB Chunks/Teams/Ultimine, Item Filters, Jade Addons, LostCities, Photon, RFTools Base/Control, Thermal Integration...): the cheapest next targets.
Data-only or almost empty jars (3 classes or fewer; 25 of them): Terralith, Structory, Towns and Towers, Macaw's Paintings, Fuel Goes Here, No Trample... The pack has no server-side-only jar that needs special handling beyond these.
Not Forge mods at all, or loaders: Connector, Forgified Fabric API, Connector Extras, kotlinforforge, bclib/better-nether/better-end and owo-lib (Fabric-origin).

## 3. Top blockers (by number of jars)

Most frequent symbols over all unresolved jars (jars in brackets):

1. **Data generation classes** (the code that runs only in `runData`): `ShapedRecipeBuilder.unlockedBy/save` (112), `RecipeProvider.<init>(PackOutput)` (96), `LootTableProvider$SubProviderEntry.<init>` (89),
   `ShapelessRecipeBuilder.unlockedBy/save` (83), `FinishedRecipe` (65), `BlockLootSubProvider.<init>` (62), `Advancement$Builder.addCriterion` (51), `critereon.*` (46 to 37). This is dead code at run
   time in a modpack. A generic fix stubs or lazily drops the datagen classes of a mod (or provides the 1.20.1 `net.minecraft.data` signatures as facades); it would clear the largest single block of unresolved references. No jar is blocked by datagen alone, so it is necessary, not sufficient.
2. **Minecraft items and enchantments**: `PotionUtils` (76), `SwordItem`/`AxeItem`/`PickaxeItem` constructors with attack damage and speed (52), `EnchantmentCategory` (51), `Enchantment$Rarity` (43),
   `Enchantment.<init>` (37), `RecordItem` (42), `DyeableLeatherItem` (15), `ItemStack.serializeNBT` (50), `StairBlock`/`BucketItem` supplier constructors (46, 37).
3. **Holders and registries**: `Holder.get()` and `Holder$Reference.get()` (47, 42), `Serializer` for loot (56), `ParticleOptions$Deserializer` and `ParticleType.<init>(boolean, Deserializer)` (45, 43), `FriendlyByteBuf.writeItemStack(ItemStack, boolean)` (15 among abandoned).
4. **NeoForge API** (among the 176 abandoned jars): `CommonHooks` (30), `NeoForgeMod.BLOCK_REACH` and other attributes (27), `Tags$Items` (26), `FluidStack` (21), `TierSortingRegistry` (19), `BuildCreativeModeTabContentsEvent.accept(Supplier)` (19),
   `ModelEvent$RegisterGeometryLoaders` (18), `RenderLevelStageEvent.getPartialTick` (17), `ForgeHooksClient` (16), `ModConfigSpec` (14), `EventHooks` (13), `IConditionBuilder` (12).
5. **Library mods that are not in the pack as 1.21.1 builds** (symbols in other mods' APIs): REI (31 jars overall, 12 abandoned), The One Probe API (39 overall, 12), EMI (28, 9), Create's catnip and Registrate (17, 15), Flywheel (12), CraftTweaker (12),
   Fabric API (12; Fabric-origin jars), Waila/Jade 1.20 API (8), Thermal core (8), Game Stages (8), Citadel/Alex's Mobs (6). The first three are recipe-viewer and probe integrations (EMI/REI/TOP integrations are optional on 1.21.1: JEI is the pack's viewer, TOP and EMI exist natively).
6. **Mixins**: only 2 jars have Mixin/ASM symbols unresolved after triage; mixins are not a blocker of this pack at link level (runtime behaviour is not measured here).

The full ranked lists (top 40 symbols for all jars, for abandoned jars, owner classes, per-mod table) are generated in `build/atm9/report.md` by the script; `docs/data/atm9-baseline.json` has the numbers of this run.

## 4. Abandoned versus native

- 295 of the 488 probed jars (60%) have a NeoForge 1.21.1 build on Modrinth. For those the layer is the wrong tool (install the native build); they are marked "has a 1.21.1 version" in `docs/data/mods.json` and are low priority.
  Notable ones: AE2, Create, Mekanism family, Refined Storage, Sophisticated Backpacks/Storage, Farmer's Delight, Waystones, Supplementaries, Quark, KubeJS, Immersive Engineering, Ender IO, Draconic Evolution, Ars Nouveau, Mystical Agriculture. A name match can be wrong (a same-named 1.21.1 project of another mod), so the column is a hint.
- 193 (40%) have none, of which 17 already link and 176 do not. Large ones that matter and are not on Modrinth for 1.21.1 as far as the API shows: Botania, Tinkers' Construct, Alex's Mobs and Caves, Ice and Fire, Blue Skies,
  The Twilight Forest, Blood Magic, Thermal Expansion family and CoFH Core, ProjectRed, MineColonies/Structurize/Domum Ornamentum (possibly on CurseForge), the FTB suite (possibly CurseForge), RFTools family, Integrated Dynamics family,
  Croptopia, GregTech, Hyperbox, QuarryPlus, MrCrayfish's Furniture, Reliquary, Super Factory Manager.
- The 17 abandoned jars that already link are mostly small: AE2 Insert Export Card, BotanyTrees, Clean Swing Through Grass, Logprot, More Dragon Eggs, No Trample, PigPen, Polymorphic Energistics, ProjectRed Illumination/Integration/Transmission,
  Refined Polymorphism, SparseStructuresReforged, stylecolonies, SuperMartijn642's Config Library, kotlinforforge, JCPlugin.

## 5. Reading the result

The 10 percent link rate (50 of 488 probed) follows from three shared gaps rather than from many separate ones: datagen classes (item 1), Minecraft item/enchantment/holder signatures (items 2 and 3) and the NeoForge hooks (item 4).
A re-run after the other 1.20.1 agents merge shows how far each moves; the baseline numbers are in `docs/data/atm9-baseline.json`.

## 6. How to re-run

```
export JAVA_HOME="/c/Program Files/Java/jdk-21"            # JDK 21; needs one ./gradlew build (build/moddev/artifacts) in the repo or main checkout
python tools/compat/atm9_run.py "<instance>/minecraft/mods" [--out build/atm9] [--baseline docs/data/atm9-baseline.json] [--write]
```

About 6 minutes for 506 jars, plus the Modrinth lookups the first time (about 0.25 s per request, cached in `<out>/native-cache.json`; `--no-native` reuses the cache only). Without `--write` the compat site data stays untouched;
with `--write` the run goes into `docs/data/mods.json` (source `probe`, never over a better result) and `docs/data/probe.json` (source `atm9`, so it is compared only with earlier ATM 9 runs), and mods with a 1.21.1 build get the note "has a 1.21.1 version".
`--baseline docs/data/atm9-baseline.json` prints the change per counter (links, unresolved, abandoned links, total unresolved pairs). `--no-probe` re-analyses the previous records without translating. The mods folder is only read; nothing from it is copied.
Outputs in `<out>`: `summary.json` (numbers), `report.md` (ranked tables and the per-mod list), `jars.json` (per jar), `records.json`, `detail.tsv` (full symbol list).
