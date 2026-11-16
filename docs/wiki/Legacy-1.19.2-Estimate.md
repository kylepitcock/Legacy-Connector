# Estimate: legacy 1.19.2 Forge mods on NeoForge 1.21.1

Research note, 2026-11-16. **Scoping only: no 1.19.2 support exists in the layer.** The roadmap ([Version-Roadmap](Version-Roadmap.md)) plans 1.19.2 as "renames and
diffs over the 1.20.1 pipeline" (stage C of [the 1.16 to 1.19 plan](Legacy-1.16-1.19-Plan.md)) at "1 to 2 days". This page replaces that guess with a measurement: 451 real
Forge 1.19.2 jars were run through the 1.20.1 pipeline as it is today, and what was left was classified. Back to [Home](Home.md). Model: [Legacy 1.20.1 estimate](Legacy-1.20.1-Estimate.md).
Data: `tools/dev/agent-tasks/1.19.2-symbols.md` (top 300 symbols), `1.19.2-gap-jars.tsv` (per jar), `1.19.2-gap-summary.txt` (all counts).

## Verdict

- The 1.20.1 pipeline links **50 of 428** Forge 1.19.2 jars with code (11.7%; 35 fully, 15 more except for other mods that are not in the sample). The same pipeline links
  141 of 506 (28%) real 1.20.1 jars (ATM 9 run). The difference is the 1.19.2 to 1.20.1 API delta, and it is **not renames**: of 2,406 distinct unresolved Minecraft/Forge symbols, **165 (7%) are
  mechanical** (one table change), **992 (41%) need real adapters** (changed or removed API), 737 are gaps the 1.20.1 layer has too, 283 are Forge API, 196 are unmapped members.
- Doing all of the 1.19.2 work raises the link rate from 11.7% to 29.7% (127 jars), which is the ceiling the 1.20.1 layer itself has on this kind of sample. Every further jar is 1.20.1 work.
- The roadmap's "1 to 2 days" holds for stage 1 below (tables only, 50 to 72 jars). **Honest estimate: 6 to 8 days wall-clock with about five agents (roughly 22 to 28 agent-days) to reach the 1.20.1 link
  rate; 10 to 14 days to an Alpha that matches the 1.20.1 "Current state" (server suites reach "Done"); client GUI parity another week.** Plus or minus 40%. It depends on 1.20.1 progress, because
  every 1.20.1 gap is also a 1.19.2 gap (class c).

## 1. Method

All offline; no game, server or client was started. The default build needs and fetches nothing of 1.19.2; every piece below is opt-in.

1. **Sample.** `python tools/compat/fetch_probe_mods.py 1.19.2 <dir> --limit 400 --content` (Modrinth, Forge files, most downloaded content mods, plus required dependencies, SHA-1 checked), into a folder outside the repository; never committed.
2. **SRG to Mojang table of 1.19.2.** `./gradlew :loader:genSrgMojang1192 -Pexperimental.mc1192=true`. The same chain as 1.20.1 (MCPConfig `joined.tsrg` of `1.19.2-20220805.130853` joined with Mojang's
   `client.txt` by obfuscated owner, name and descriptor), parameterised by version in `loader/build.gradle` (`srgMojangTables`). Mojang's file is fetched at build time and its SHA-1 verified
   (`8e8c9be5dc27802caba47053d4fdea328f7f89bd`), SHA-256 fingerprints and the row count are recorded in `build/reports/mappings/1.19.2/srg-mojang-provenance.json`; neither Mojang's file nor the derived table
   is committed or added to the loader jar. 59,186 rows (44,929/44,929 methods and 28,897/28,897 fields matched). Without the property nothing is declared, resolved or fetched.
3. **Gap tool.** `./gradlew gap1192 -Pexperimental.mc1192=true -PtargetJars=@list -PgapJars=<dir>` is `Gap1201` registered a second time with the 1.19.2 table (`registerGap` in `build.gradle`); `-Pgap.srgOverlay=<1.20.1 table>`
   lets a second table win. Three runs: **A** raw (1.19.2 table, 1.20.1 to 1.21.1 renames, no translation), **B** like A with the 1.20.1 table overlaid, **C** `./gradlew gap1201 -Pgap.translate=true`, i.e. the real
   `Forge1201Transformer` (call rewrites, stand-ins, facades, soft links) over the 1.19.2 jars as if they were 1.20.1 jars. **C is the measurement**; A and B only show what the tables alone do.
4. **Mechanical diff.** `tools/dev/mc_member_diff.py` joins the Mojang-named class and member lists of 1.19.2 and 1.20.1 by the stable SRG ids (class ids `C_n`, `m_n_`, `f_n_` are kept by MCPConfig across 1.19.2 and 1.20.1).
5. **Classification.** `tools/compat/gap1192_analyze.py <detail.tsv> <jars> <diff> <1.19.2 table> <forward/1.20.1> <out>` (classes below, shapes, what-if and staged curves). `tools/compat/data1192_extra.py` and the existing `data1201.py` scan the jars' `data/`.

The 1.19.2 mods are in SRG names (`m_46859_`) with Mojang class names, as 1.20.1 mods are, so no step of the 1.20.1 pipeline is missing: it only knows 1.20.1.

## 2. Sample

| | |
|---|---|
| Jars | **451** (399 most downloaded content mods that have a 1.19.2 Forge file, 52 required dependencies, 1 already present; 400 projects) |
| With `META-INF/mods.toml` (Forge) | 447; 55 also carry `fabric.mod.json` or `quilt.mod.json` (multi-loader), 10 also a `neoforge.mods.toml`; 4 jars have no `mods.toml` (2 Fabric only, 2 with neither marker) |
| With code | 428 analysed after translation (19 are data or resource packs, 4 failed to translate: ASM `MethodTooLargeException` in huge animation `<clinit>` methods, Cataclysm, EeeAbsMobs, Friends and Foes: a generic 1.20.1 defect, not 1.19.2) |
| Mixins | 297 jars have a mixin config, 3,605 mixin classes (see risks) |
| Data pack format | `pack_format` of the jars: 9 (207), 10 (67), 8 (62), 6 (40) and 15 (30: multi-version jars) |
| Libraries they link against | JEI (90 jars), Curios (42), Create (35, with Flywheel 26 and Registrate 26), REI (23), Puzzles Lib (21), Cloth Config (21), Architectury (21), Jade (21), Farmer's Delight (20), Patchouli (10), Blueprint (10), TheOneProbe (9), EMI (7) |

Not representative of the "abandoned" set the way the 1.20.1 corpus was (that one counted mods without a 1.21.1 port); this is the most downloaded content mods, so many have a 1.21.1 port. The numbers measure the layer, not the demand.

## 3. Link numbers

| Run | What it shows | Jars with code | Link fully | Link but for other mods/libraries | Unresolved |
|---|---|---|---|---|---|
| A: 1.19.2 table, renames only | the tables alone | 431 | 26 | 10 | 395 |
| B: A plus the 1.20.1 table overlaid | stable ids named as in 1.20.1 | 431 | 26 | 11 | 394 |
| **C: the 1.20.1 layer as it is** | what a 1.19.2 jar gets today | **428** | **35** | **15** | **378** |
| (ATM 9, real 1.20.1 jars, same layer) | for comparison | 506 | 141 (28%) | | |

Unresolved symbol occurrences of run C by area (jar times symbol): Minecraft common 7,982, Minecraft client 4,701, Forge API 693, Mixin/ASM 1, other mods and libraries 31,215 (missing from the sample
because it is the most-downloaded set, not a closed pack; they say nothing about the layer). Fabric intermediary names in the multi-loader jars (2,176 symbols) are dropped: that code is never loaded.

**Do the stable ids carry over?** Yes, and it helps. 24,663 of 30,289 methods and 26,779 of 28,897 fields that have a 1.19.2 id are identical in 1.19.2 and 1.20.1; 1,055 methods and 6 fields are *renamed*
under the same id (the 1.20.1 table names them with the 1.20.1 name, so a 1.19.2 jar run through the 1.20.1 table gets the rename for free). Only ids that 1.20.1 does not know need the 1.19.2 table
(removed members, and the classes 1.20.1 stopped obfuscating, see class a).

## 4. Classification of the unresolved Minecraft/Forge symbols (run C, 2,406 distinct, 13,377 jar x symbol)

| Class | Meaning | Distinct symbols | Occurrences | Is it 1.19.2 work? |
|---|---|---|---|---|
| **a** | one table row or one rename fixes it | 165 | 1,845 | yes, mechanical |
| **b** | API changed shape or was removed after 1.19.2 | 992 | 8,858 | yes, the work |
| **c** | still there, unchanged, in 1.20.1: the 1.20.1 layer lacks it too | 737 | 1,279 | **no** (1.20.1 work) |
| **F** | Forge API (Forge 43 to 47, Forge to NeoForge) | 283 | 694 | partly; not diffed here |
| u | member of a class that exists in both, in neither list (synthesised by the layer or not mapped) | 196 | 437 | no, noise |
| n | not a Minecraft member | 33 | 264 | no |

### 4.1 Class a: renames and moves between 1.19.2 and 1.20.1 (derived mechanically)

`mc_member_diff.py` over the two Mojang-named versions (members that have an SRG id; the rest of the file is in `members-a/b`):

| | Same | Renamed | Moved | Signature changed | Removed | Added |
|---|---|---|---|---|---|---|
| Classes | 6,537 | 0 (by id) | 0 | | 322 | 899 |
| Methods | 24,663 | 1,055 | 152 | 200 | 4,219 | 7,152 |
| Fields | 26,779 | 6 | 0 | 50 | 2,062 | 4,168 |

- **Classes are not renamed, they are replaced.** The class ids change when a class is rebuilt, so removed and added classes only pair by simple name: 47 pairs, 17 of them top-level, and most of those
  are just id churn (`GLX`, `RenderSystem`, `MinecraftServer`). Genuine moves found in the sample: `AtlasSet`, `Resource$IoSupplier` to `IoSupplier`, `ChunkHolder$FullChunkStatus` to `FullChunkStatus`,
  `Registry$RegistryBootstrap` and `BuiltinRegistries$RegistryBootstrap` to `RegistrySetBuilder$RegistryBootstrap`, the `TextureAtlasSprite` inner classes to `SpriteContents`: about ten rows. The 127 removed top-level
  classes are 1.19.3 chat signing and preview (`ChatPreview*`, `MessageSigner`, `ChatSender`, 21 classes), `Material`/`MaterialColor`, `com.mojang.math` (`Matrix3f`, `Quaternion`, `Vector3d`...), `GuiComponent`,
  `LootTables`/`PredicateManager`/`ItemModifierManager`, `UpgradeRecipe`, `Wearable`, the `Alternative` loot condition: those are class b.
- **The big class-a item is the SRG table itself.** 1.19.2 obfuscates `com.mojang.blaze3d` (and other "official" packages) with SRG ids (`RenderSystem.m_157456_` = `setShaderTexture`); 1.20.1 keeps the plain name, so the
  1.20.1 table has no row for it and run C leaves 145 distinct symbols (1,769 occurrences, 193 jars: `RenderSystem.setShaderTexture/setShaderColor/setShader/enableBlend...`, `BufferBuilder`, `VertexConsumer`) as bare
  ids. Using the 1.19.2 table underneath the 1.20.1 one (the 1.20.1 names win) fixes all of them: the `Gap1201 -Pgap.srgOverlay` run proves it. This is the cheapest and largest single item.
- Members renamed or moved under the same id (1,207) are fixed by the 1.20.1 table already when the id is looked up there: the only 1.19.2-specific part is "look up the 1.19.2 table second".

### 4.2 Class b: changed shape, by shape (distinct symbols / occurrences / jars; run C)

| Shape | Symbols | Occurrences | Jars | Adapter in the style of `v1_20_1/migrate`? | Size |
|---|---|---|---|---|---|
| Registry statics: `Registry.ITEM/BLOCK/...`, `*_REGISTRY` keys, `BuiltinRegistries` (1.19.3) | 162 | 1,821 | 267 | Yes, pure rows: GETSTATIC to `BuiltInRegistries`/`Registries` fields of the same name, generated from the class diff | 0.5 day |
| GUI drawing: `Screen.render(PoseStack,..)`, `GuiComponent.blit/fill`, `Font.draw(PoseStack,..)`, `Button(...)`, `RenderSystem.enableTexture` (1.19.3 to 1.20) | 70 | 990 | 186 | Yes, but not rows: bridge `render(PoseStack,..)` overrides to `render(GuiGraphics,..)` and a thread-local current-`GuiGraphics` helper for the statics, a `Button` stand-in over the protected 1.20.1 constructor | 2 to 3 days, client-only |
| Damage sources: `DamageSource.X` statics, `new DamageSource(String)`, `causeXDamage` (1.19.4 data driven) | 76 | 870 | 132 | Statics become `level.damageSources().x()`; `new DamageSource(id)` needs a generated `damage_type` data entry per id (the recipes mechanism) | 1 to 1.5 days |
| `com.mojang.math` `Vector3f/Quaternion/Matrix3f/Matrix4f` to JOML (1.19.3), `PoseStack.mulPose(Quaternion)`, `Pose.pose()`, `VertexConsumer.vertex(Matrix4f,..)` | 21 | 632 | 209 | Yes: legacy-API stand-in classes with conversion at the `PoseStack`/`VertexConsumer` boundary (precedent: `CompatBufferBuilder`) | 1 to 1.5 days |
| Creative tabs: `Item.Properties.tab(...)`, `new CreativeModeTab(String)`, `makeIcon`, `fillItemList` (1.19.3 to 1.20) | 63 (+126 occurrences of the constructor counted as n) | 556 | 215 | Yes: stand-in tab class registered as a real tab, `tab(x)` calls recorded by a helper and replayed into `BuildCreativeModeTabContentsEvent` (the layer already replays tab entries for 1.20.1) | 1 to 1.5 days |
| Rendering: `ItemTransforms.TransformType` to `ItemDisplayContext`, `AtlasSet`, `TextureAtlasSprite`, model baking, `TextureStitchEvent` | 89 | 689 | 188 | Mixed: enum map (rows) plus a few helpers | 1.5 to 2 days, client |
| Other Minecraft/server: `BlockPos.<init>(DDD)`, `BlockPos(Vec3)`, `Registry` helpers, constructor to factory | 69 | 545 | 204 | Yes: constructor-to-factory rows | 0.5 to 1 day |
| Entities/attributes/AI | 102 | 299 | 104 | Rows plus a few bridges | 1 day |
| Block/item/state | 44 | 345 | 113 | Rows | 0.5 day |
| Client other (resources, keys, gui components) | 68 | 322 | 126 | Rows, `Widget`/`AbstractWidget` stand-ins | 1 day |
| Sound events: `SoundEvent.<init>(ResourceLocation)` to `createVariableRangeEvent` | 15 | 162 | 116 | Yes: constructor-to-factory rows | 0.25 day |
| Loot: `LootTables`, `PredicateManager`, `LootContext.Builder`, `Alternative` condition | 47 | 369 | 87 | Rows plus helpers (1.20.1 to 1.21.1 reload stand-ins of the 1.20.1 layer apply afterwards) | 1 day |
| Level/world/chunks | 32 | 198 | 95 | Rows | 0.5 day |
| Recipes/smithing: `UpgradeRecipe`, `SmithingRecipe`, `Ingredient` | 28 | 277 | 76 | Class stand-in plus data conversion (below) | 1 day |
| `Material`, `MaterialColor`, `BlockState.getMaterial` (1.20) | 3 symbols but the constants are fields | 302 | **203** | Yes: stand-in `Material` constants carrying map colour, push reaction, replaceable, flammable; `BlockBehaviour.Properties.of(Material)` helper | 1 to 1.5 days |
| Worldgen: `StructureFeature`, `Feature`, `BiomeLoadingEvent`, `WorldgenRandom` | 36 | 170 | 64 | Rows, plus the event as in the 1.7.10 bridge to biome modifiers | 1 day |
| Data generators: `DataGenerator.addProvider`, `RecipeProvider.<init>(DataGenerator)`, `TagsProvider` | 29 | 72 | 35 | Cheapest by stripping: a `GatherDataEvent` listener never runs in game | 0.25 day |
| Explosions, commands (`sendSuccess(Component, boolean)` to a supplier), network packets, chat (1.19.3 signing) | 7 + 9 + 17 + 5 | 113 + 83 + 34 + 9 | 46 + 73 + 24 + 6 | Rows and a few helpers | 0.5 to 1 day |

The sum is about 15 to 19 agent-days for class b alone. Chat signing and previews (21 removed classes) and `Material` are the only areas without a mechanical mapping; both are small in the sample (chat 6 jars).

### 4.3 Class c: gaps the 1.20.1 layer has too (not 1.19.2 work)

737 distinct symbols, 1,279 occurrences: a long, thin tail (most symbols occur in one or two jars). It overlaps `tools/dev/agent-tasks/1.20.1-symbols-leftovers.md` (14 of the entries match it by name: static animal food fields,
`BannerBlockEntity`, `CauldronInteraction`, `ClientboundExplodePacket`, `LootingEnchantFunction`...); the larger items are `SoundEvents.UI_BUTTON_CLICK` (40 jars; now a holder), `SkinManager`,
`registryAccess()` on `LevelAccessor`/`WorldGenLevel`, the custom payload packet constructors (`ClientboundCustomPayloadPacket(ResourceLocation, FriendlyByteBuf)`), `StateSwitchingButton`, and 95 data-generator symbols. These do not
move with the 1.19.2 work; they are why the ceiling is 29.7% and not higher.

### 4.4 Forge API (class F: 283 symbols, 694 occurrences)

Small. Top: `TextureStitchEvent.Pre` (46 jars; removed in 1.19.3, `TextureStitchEvent` no longer has `Pre`/`Post`), `RegisterParticleProvidersEvent.register` (42 and 13 jars for the two overloads), `NeoForgeMod.REACH_DISTANCE/ATTACK_RANGE`
(now holders), `LivingSpawnEvent.CheckSpawn/SpecialSpawn`, `CapabilityItemHandler`/`CapabilityFluidHandler`/`CapabilityEnergy` (the capability constants), the `getPoseStack()` accessors of `RenderGuiOverlayEvent`/`ScreenEvent.Render`
(which follow the GUI work), `BiomeGenerationSettingsBuilder.addFeature`, `RenderLevelLastEvent`, `RegistryEvent` (8 jars: most 1.19.2 mods already use `DeferredRegister`). A proper Forge 43 to 47 diff needs the Forge jars, which this scoping did not fetch;
it is stage 0 below. Same size as the 1.20.1 Forge facade round for the removed-in-1.19.3 events (about 1 day).

### 4.5 Data and resources (sample scan of the jars' `data/` and `assets/`)

`tools/compat/data1201.py` (the 1.20.1 to 1.21.1 formats) over the 451 jars: plural folder names 124,537 files in 303 jars (1.19.2 uses plural folders as 1.20.1 does: already converted), recipe results 33,075 files/203 jars, `forge:` tags
11,327/162, `forge:` ids 10,376/106, recipe conditions 9,922/52, item predicates 5,202/116, advancement icons 2,036/89, conditional recipes 1,923/17, loot functions 1,514/99, model loaders 1,033/26, `forge:` directories 572/84, renamed ids 195/55, NBT in recipes 143/14, loot conditions 108/22.
These are what `DataConverter1201` converts today; they are the same for 1.19.2 data.

`tools/compat/data1192_extra.py` (what is new for 1.19.2): **old smithing recipes** (`minecraft:smithing`, 250 files in 34 jars; since 1.19.4 `smithing_transform` needs a template item, only 1 file already uses it): the converter has to pick a template
(the netherite upgrade template as vanilla's own conversion does, or a generated invisible one) and keep the recipe usable; damage-type tags/json 36 files in 7 jars (new in 1.19.4: only matter once damage sources are mapped);
`pack_format` 9 and 10 of the jars (what 1.19.2 shipped) against 15 (1.20.1) and 34 (1.21.1): the pack format check is already bypassed for translated jars; `alternative` loot condition (262 files, 32 jars; `minecraft:alternative` is `any_of` since 1.20);
no feature flags and no chat types in the sample. Atlas rules (1.19.3: loose textures are invisible unless listed in the atlas) may hide block or item textures of mods that rely on directory stitching; the converter must list them (the 1.12.2 layer does this for code-referenced sprites).
Size: 1 to 1.5 days including tests; small next to the 1.20.1 data work because the folder, recipe and `forge:` conversions already exist.

## 5. Risks

1. **Mixins.** 297 of 451 jars carry a mixin config (3,605 classes), more than the 1.20.1 samples: GUI, `Screen`, `ItemRenderer`, `PoseStack` and `Material` targets all changed in 1.19.3 to 1.20. The layer's per-injection repair and drop applies, but a dropped GUI mixin silently removes a feature. The 1.20.1 mixin tooling covers 1.20.1 targets only; the 1.19.2 index of targets (a fourth jar to fetch) is part of stage 0.
2. **GUI and rendering parity is visual**, cannot be proven offline, and the client path needs real screenshots (a week of client verification, as the 1.12.2 render sweep).
3. **Semantic stand-ins (`Material`, creative tabs, damage sources)** are approximations: `Material` held behaviours that are now spread over block properties, tabs held an item order, damage sources had no registry. Expect a defect tail per mod family.
4. **Libraries** are 1.19.2 builds (Create 0.5, Curios 5, Jade 8, Farmer's Delight 1.2, Patchouli, Architectury 6/8): the 1.20.1 bridge rules apply only where the 1.19.2 API equals the 1.20.1 one; each library needs a check of its 1.19.2 class list (JEI `mezz.jei` 90 jars and Curios 42 are the big two; the 1.20.1 JEI/Curios bridges are the starting point, not the answer).
5. **Dependence on 1.20.1.** The class c tail and the 1.20.1 data converters are shared; progress on 1.20.1 raises the 1.19.2 ceiling for free, and a 1.20.1 regression breaks both.
6. **Transformer version.** Every loader output change needs a `TRANSFORMER_VERSION` bump and invalidates the caches; the 1.19.2 mode must be keyed so a 1.19.2 jar and a 1.20.1 jar never share a cache entry.

## 6. Staged plan

Curves are measured on the sample: "link" = jars with no unresolved Minecraft/Forge symbol left, "cleared" = jars with no remaining 1.19.2-specific (class a/b) symbol (373 of 428 jars have at least one). Baseline today: 50 link. Ceiling with all stages: 127.

| Stage | Content | Unlocks (cumulative) | Days |
|---|---|---|---|
| 0. Plumbing | `LegacyVersion.V1_19_2` detection (`mods.toml` `loaderVersion [43,)`, `minecraft [1.19.2]`), a cache key per version, the 1.19.2 table as a build-time resource overlay under the 1.20.1 one (same licence rule as 1.20.1: derived at build time, not committed), Forge 43 to 47 and Forge to NeoForge diff of the Forge facades (`TextureStitchEvent`, `RegistryEvent`, capability constants...), 1.19.2 mixin target index | nothing visible; class a (SRG table) is part of it: 145 symbols in 193 jars | 1.5 to 2 |
| 1. Tables | registry statics, constructor-to-factory rows (`BlockPos`, `SoundEvent`), level/world/block/commands/network/chat rows, the ten real class moves | 33 cleared, **72 link** | 1.5 to 2 |
| 2. Stand-ins | creative tabs, `Material`, `com.mojang.math`, damage sources (with generated `damage_type` data), entities, loot | 107 cleared, **92 link** | 5 to 6 |
| 3. Client | GUI (`GuiGraphics` bridges), rendering, client other | 245 cleared, **118 link** | 5 to 6 |
| 4. Data and worldgen | smithing conversion, recipes, worldgen rows, datagen stripping, atlas listing | 373 cleared, **127 link** (the ceiling) | 3 to 4 |
| 5. Run time | server suites with 1.19.2 content mods until "Done", `verify.sh` suite, mixin triage, first client scenarios | what links runs | open; 1.20.1's equivalent took about a week of rounds |

The order inside stages is by jars unlocked, not by size; stage 2 and 3 parallelise (different agents, different files). `./gradlew gap1192` and `gap1192_analyze.py` are the progress meter: re-run, compare the staged curve.

## 7. Effort and pace

Pace of the 1.20.1 work (`git log --since=2026-11-05 -i --grep="1.20.1"`): 253 commits on 9 active days (2026-11-06: 4, 11-07: 65, 11-08: 62, 11-09: 7, 11-13: 33, 11-14: 29, 11-15: 45, a few days of nothing in between). The migration
tables, Forge facades and first server loads came in about two days (131 commits, 11-06 to 11-08); the ATM 9 link rate went from the first measurement to 28% over the rounds of 11-13 to 11-15. The 1.20.1 corpus was a full port from nothing; 1.19.2 reuses the transformer, the gap tool, the data converter, the soft link, the mixin repair, the facades and the library table, so the first link-rate is cheaper than 1.20.1's first week, but it is about 1,000 distinct symbols of changed API, not a table.

| Milestone | Wall-clock with about 5 agents | Agent-days |
|---|---|---|
| Stage 0 + 1 (tables; link 50 to 72) | 1.5 to 2 days (the roadmap's figure) | 3 to 4 |
| Stage 2 (stand-ins; 92 link) | +2 days | 8 to 10 |
| Stages 3 + 4 (client, data; 127 link, equal to the 1.20.1 layer) | +3 to 4 days | 12 to 14 |
| **Link parity with 1.20.1** | **6 to 8 days** | **22 to 28** |
| Alpha (server suites reach "Done", regression suite, mixin triage) | +4 to 6 days | +10 to 14 |
| Client verification of GUI and rendering | +about 1 week, partly parallel | +10 |
| Long tail (issue list) | open-ended | |

Plus or minus 40%. This is about 4 to 5 times the roadmap's "1 to 2 days": the 1.20.1 estimate also began with a "renames and diffs" intuition, and the measured API change was larger. **Recommendation:** do stage 0 and 1 when 1.20.1 stabilises (cheap, shows a real number, and its two days are the roadmap's guess),
re-measure with the same scripts, then decide on stage 2 to 4 with the abandoned-mod share of the 1.19.2 corpus (this sample is the most downloaded, not the abandoned set; fetch with `--slug` for a pack).

## 8. What 1.18.2 adds on top (Forge 40, Java 17, same as 1.19.2)

Stage B of the plan (1.18.2 to 1.19.2) is a second, independent delta; it was **not measured**. Expected from the API history: `TextComponent`/`TranslatableComponent` to `Component` factories (constructor-to-factory rows, cheap); `Random` to `RandomSource` in
most game methods (override bridges both ways, the most error-prone item); the Forge 1.19 event renames (`World` to `Level`), `ForgeRegistryEntry`/`setRegistryName` removal (a registry-name helper), `FluidAttributes` to `FluidType`;
**registry freezing and holders**: 1.19 made vanilla registries intrusive-holder based and froze them earlier, so code that registers late or reads `Registry.X.get(...)` before the freeze behaves differently (the 1.20.1 layer already has a tolerant registry for this);
worldgen: `StructureFeature` to `Structure`, configured structures and biome data formats, `BiomeLoadingEvent` (alive in 1.19.2, gone later); chat: 1.19 signed chat and `sendMessage(Component, UUID)` to `sendSystemMessage`. Class names and SRG ids of 1.18.2 are Mojang names and `m_` ids, so the id-join should work for 1.18.2 to 1.19.2 as well (to be verified with the 1.18.2 MCPConfig; the 1.16.5 step cannot use it: those ids were renumbered). Expect about 60 to 70% of the 1.19.2 effort again, with a
larger share of worldgen and data format conversion and a smaller share of GUI and rendering (the 1.19.3/1.20 GUI delta would already be solved by stage 3), i.e. 4 to 5 days wall-clock after 1.19.2, and the same measure-first rule: fetch the 1.18.2 sample and the 1.18.2 table with the same opt-in switch (`srgMojangTables('1.18.2', ...)`).

## 9. Reproducing

```
export JAVA_HOME="/c/Program Files/Java/jdk-21"
python tools/compat/fetch_probe_mods.py 1.19.2 C:/Users/Kyleb/probe-jars/1.19.2 --limit 400 --content
./gradlew :loader:genSrgMojang1192 :loader:genSrgMojang1201 -Pexperimental.mc1192=true
python tools/dev/mc_member_diff.py loader/build/tmp/mcpconfig1192/joined.tsrg loader/build/mojang-mappings/1.19.2-client.txt \
   loader/build/generated/srg-mojang-1192/legacyconnector/mappings/1.19.2/srg-mojang.tsv loader/build/tmp/mcpconfig1201/joined.tsrg \
   loader/build/mojang-mappings/1.20.1-client.txt loader/build/generated/srg-mojang/legacyconnector/mappings/1.20.1/srg-mojang.tsv <diff-dir>
./gradlew gap1201 -PtargetJars=@<targets.txt> -PgapJars=<jar-dir> -PgapReport=<out>/transC.txt -Pgap.translate=true      # run C (targets: probe.py gap1201_targets)
./gradlew gap1192 -Pexperimental.mc1192=true -PtargetJars=@<targets.txt> -PgapJars=<jar-dir> -PgapReport=<out>/rawA.txt  # run A (add -Pgap.srgOverlay=<1.20.1 table> for B)
python -I tools/compat/gap1192_analyze.py <out>/transC.txt.detail.tsv <jar-dir> <diff-dir> <1.19.2 table> loader/src/main/resources/legacyconnector/forward/1.20.1 <out>/ana
python -I tools/compat/data1201.py <jar-dir>; python -I tools/compat/data1192_extra.py <jar-dir>
```

Run C takes about 25 minutes for 451 jars (single JVM, 3 GB). `docs/data/` is untouched by all of this.
