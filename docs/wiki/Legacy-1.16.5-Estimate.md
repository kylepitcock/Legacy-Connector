# Estimate: legacy 1.16.5 Forge mods on NeoForge 1.21.1

Research note, 2026-11-24. **Scoping only: no 1.16.5 support exists in the layer.** The plan ([1.16 to 1.19 plan](Legacy-1.16-1.19-Plan.md)) calls it stage A (1.16.5 to 1.18.2), "largest; three eras in one".
This page measures it the way the [1.19.2](Legacy-1.19.2-Estimate.md) and [1.18.2](Legacy-1.18.2-Estimate.md) pages did: 442 real Forge 1.16.5 jars, brought to Mojang names of their own version,
checked against Minecraft 1.21.1 + NeoForge, and every unresolved symbol walked along 1.16.5 to 1.18.2 to 1.19.2 to 1.20.1. Back to [Home](Home.md).
Data: `tools/dev/agent-tasks/1.16.5-symbols.md` (top 300 symbols), `1.16.5-gap-jars.tsv` (per jar), `1.16.5-gap-summary.txt` (all counts).

## Verdict

- **Names are the easy part.** The jars carry MCP class names (`net.minecraft.block.Block`, `PlayerEntity`, `TileEntity`, `MatrixStack`) and `func_`/`field_` ids; both map to the Mojang names of 1.16.5 completely: the
  opt-in table joins MCPConfig's `joined.tsrg` with Mojang's official 1.16.5 mappings and matches **35,370 of 35,370 methods and 19,841 of 19,841 fields** (43,023 id rows, 12,188 ids shared by overriding methods)
  plus **5,003 class rows** (MCP to Mojang; only 444 of 5,447 classes keep their name). Nothing here needs hand-written rules.
- **What is left is a large API delta.** Without any translation (own-version names and the 1.20.1 to 1.21.1 renames only; the 1.20.1 transformer does not know 1.16.5, so there is no "layer as it is" run) **24 of 439 jars link** (5.5%). Of 7,223 distinct unresolved Minecraft/Forge symbols:
  **a rename, move or table row: 167 at stage A** (72 Minecraft, 95 Forge), **1,212 changed or removed at stage A** (1,017 Minecraft, 195 Forge), 1,287 members that Forge's own binary patches added to Minecraft
  classes (`getRegistryName`, `harvestTool`/`ToolType`, `isAir(pos)`, `hasTileEntity`, `setISTER`: not diffable offline, most of them 1.16 to 1.17 Forge API breaks), then what the later stages add: **709 at stage B** (358 + 351 Forge) and
  **637 at stage C** (610 + 27 Forge); 2,330 symbols are unchanged up to 1.20.1 but unresolved in this raw run because the 1.20.1 to 1.21.1 delta is not translated (on the 1.18.2 sample the translation removed
  about 70% of that class).
- Share of jars that link against the **next stage's names** (1.18.2) once pure renames are applied: **12.8%** (56 of 439) counting Minecraft and Forge API, **32.3%** counting Minecraft alone (297 jars carry a changed or removed Minecraft symbol).
- Link ceiling once stages A, B and C are done (everything classified as a or b fixed, the rest unchanged): **56 of 439 (12.8%) as a lower bound** from the raw run; on 1.18.2 the same counting gave 47 raw and 100 after the real
  translation, so with the 1.20.1 layer behind it the figure would be about 110 (25%): an extrapolation, not a measurement.
- **Relative size.** Distinct symbols that need real work: stage A 1,212 (plus 1,287 Forge-patch members, plus the data and mixin work), stage B 709, stage C 637. Stage A is about **1.7 times stage B and about 1.9 times stage C
  by symbol count**; stage D (1.20.1 to 1.21.1, 25 to 30 agent-days) is the unit. **Honest estimate for stage A alone, with B, C and D in place: 35 to 50 agent-days (10 to 14 days wall-clock with about five agents) to the link
  ceiling, plus 10 to 14 for an Alpha**; plus or minus 40%. A 1.16.5 mod needs all four stages: 71 to 96 agent-days to the ceiling from nothing (A 35 to 50, B 14 to 18, C 22 to 28), i.e. 2.5 to 3.5 times stage D.
  The plan's guess "at least as large as stage D" holds and is probably low.

## 1. Method

Offline only: no game, server or client was started, and no downloaded jar is executed (only read by our tools). Everything new is opt-in and fetches nothing by default.

1. **Sample.** `python tools/compat/fetch_probe_mods.py 1.16.5 <dir> --limit 400 --content` into `test-mods-probe/1.16.5-modrinth-set/` (gitignored): 396 most downloaded content mods with a 1.16.5 Forge file plus 46 required dependencies (one dependency 404).
2. **Mapping.** `./gradlew :loader:genSrgMojang1165 -Pexperimental.mc1165=true`: MCPConfig `1.16.5-20210115.111550`, Mojang's official `client.txt` of 1.16.5 (SHA-1 `374c6b789574afbdc901371207155661e0509e17`, verified).
   `SrgMojangTable` learned the 1.16.5 formats: the tsrg1 joined file (no `tsrg2` header, one line per field and per method), `func_123_a_`/`field_123_a_` ids, and an optional fifth argument that writes the MCP to Mojang **class** rows
   (`mc-classes.tsv`, the format of the forward tables). Build-local, never added to the loader jar; 1.20.1 behaviour is unchanged (the gate passes).
3. **Gap run.** `./gradlew gap1165 -Pexperimental.mc1165=true -Pgap.extraRenames=<mc-classes.tsv>`: `Gap1201` with the 1.16.5 id table and the class rows first, then the 1.20.1 to 1.21.1 rows. **Raw: no translation.**
   (`-Pgap.translate=true` runs `Forge1201Transformer`, which expects Mojang class names and Java 17 code; running it on 1.16.5 jars was not attempted and would measure the wrong thing.)
4. **Mechanical diff by name.** `tools/dev/mc_name_diff.py` joins Mojang's official class and member lists of neighbouring versions by name. 1.16.5 ids were renumbered in 1.17 (as the plan says), so the id join of the 1.19.2 scoping is
   not available for this pair; Mojang's names of 1.16.5 already use the 1.17+ package layout (`net.minecraft.world.level.block.Block`), so names compare directly.
5. **Forge API.** `tools/compat/forge_api_index.py` indexes Forge 36.2.42 (universal and launcher jars, eventbus 4.0.0; MCP class names in descriptors mapped to Mojang names), 40.3.12, 43.5.2 and 47.4.29; Forge symbols are diffed like Minecraft ones.
6. **Classification.** `tools/compat/gap_stage_analyze.py --chain 1.16.5,1.18.2,1.19.2,1.20.1` (stages A, B, C), as on the 1.18.2 page. `tools/compat/survey_modern_jars.py` surveys the structure of the jars.

## 2. Sample

| | |
|---|---|
| Jars | **442** (396 content mods plus 46 dependencies; 400 projects) |
| With `META-INF/mods.toml` | 435; 20 also carry a Fabric or Quilt marker, 8 a `neoforge.mods.toml` (multi-version jars) |
| With code | 439 analysed (3 are data or resource packs) |
| Class file version | **426 jars are Java 8 (major 52)**, 3 Java 11, 6 Java 17, a few multi-release; `loaderVersion` `[36,)` in 216, `[34,)` in 42, `[32,)` in 40 |
| Mixins | 209 jars, 1,635 mixin classes in 251 configs; `compatibilityLevel` JAVA_8 in 200; 198 jars have a refmap, **149 with SRG ids inside** (`func_123_a_`) |
| Access transformers | 175 jars, **161 with SRG ids** (MCP class names plus `func_`/`field_`); `coremods.json` in 6 jars (6 JavaScript files); **no jar-in-jar** (0 jars; it arrived with Forge 40) |
| Data | plural folder names in 269 jars (112,505 files); `pack_format` 6 (326), 4 (31), 5 (29), 8 (15), 9 (11); Forge loot modifiers in 26 jars |
| World generation data | `template_pool` 2,167 files in 23 jars, `processor_list` 270 in 10, `configured_feature` 172 in 8, `configured_structure_feature` 120 in 3, biomes 44 in 6, `configured_surface_builder` 34 in 4, dimensions 47 in 9 |
| Libraries they link against (jars with unresolved references into them) | Curios (37), Waila/HWYLA (17), Architectury (14), The One Probe (13), CraftTweaker (13), Patchouli (11), FTB (9), Bookshelf (8), SuperMartijn642's Core Lib (7) |

Same caveats as the other pages: most downloaded content mods, many of which have newer versions; the numbers measure the layer, not the demand. 1,133 Fabric intermediary symbols of multi-loader jars are dropped.

## 3. Link numbers

| Run | What it shows | Jars with code | Link fully | Link but for other mods/libraries | Unresolved |
|---|---|---|---|---|---|
| A: 1.16.5 table, renames only | the tables alone | 439 | 22 | 2 | 415 |
| (1.18.2, the same raw run) | for comparison | 442 | 26 | 4 | 412 |
| (1.19.2 raw run A, from its page) | for comparison | 431 | 26 | 10 | 395 |

Link rate by stage (raw run; "full" = nothing else left either):

| Done so far | No stage symbol left | All a/b symbols are rename rows | Full link after the work |
|---|---|---|---|
| nothing | 24 of 439 (5.5%) | | |
| stage A | 50 (11.4%) | 56 (12.8%) | 36 (8.2%) |
| stages A and B | 32 (7.3%) | 35 (8.0%) | 55 (12.5%) |
| stages A, B and C | 31 (7.1%) | 34 (7.7%) | 56 (12.8%) |

Stage-local link rate (only the symbols that break in one transition; renames applied): **stage A 56 of 439 (12.8%)**, 142 (32.3%) counting Minecraft alone; stage B 63 (14.4%); stage C 116 (26.4%).
Unlike 1.18.2, the fully-linking count rises only slowly with each stage: 1,287 Forge-patch members and the untranslated 1.20.1 to 1.21.1 delta sit in the "everything else" column, so the real ceiling needs the layer behind it (see the verdict).

## 4. Classification of the unresolved Minecraft/Forge symbols (7,223 distinct, 50,531 jar x symbol)

| Class | Meaning | Distinct symbols | Occurrences |
|---|---|---|---|
| a, stage A | a rename or move between 1.16.5 and 1.18.2 (Minecraft 72, Forge 95: `fml.network` to `network`, `RegistryObject`, `ClientRegistry`) | 167 | 2,745 |
| **b, stage A** | changed or removed at 1.17 and 1.18 (Minecraft 1,017, Forge 195) | **1,212** | **10,308** |
| a / b, stage B | carried to 1.19 (a 72, b 709) | 781 | 7,678 |
| a / b, stage C | carried to 1.20 (a 51, b 637) | 688 | 6,646 |
| D | unchanged up to 1.20.1, unresolved in the raw run (the 1.20.1 to 1.21.1 delta and layer gaps) | 2,330 (+329 Forge) | 17,907 (+1,420) |
| u | members of Minecraft classes added by Forge's binary patches (or by the layer): not in any list | 1,287 | 3,058 |
| n | not a Minecraft or Forge 1.16.5 member (libraries, mod APIs) | 429 | 769 |

### 4.1 What changed between 1.16.5 and 1.18.2 (mechanical, by name)

`mc_name_diff.py` over Mojang's official names (1.16.5 to 1.18.2 spans two Minecraft releases, 1.17 and 1.18, which Forge never shipped as a stable base for 1.17):

| | Same | Moved | Signature changed | Removed | Added |
|---|---|---|---|---|---|
| Classes | 5,187 | 265 | | 295 | 1,212 |
| Methods | 37,916 | 795 | 2,968 | 5,512 | 15,931 |
| Fields | 19,401 | | | 2,204 | 9,460 |

For comparison the next transitions (same tool): 1.18.2 to 1.19.2 removes 2,024 methods and 140 classes, 1.19.2 to 1.20.1 removes 3,756 methods and 189 classes. Stage A removes **5,512 methods and 295 classes**, about 2.7 times the 1.19 delta,
which matches the measured symbol ratio.

### 4.2 Class a and b of stage A, by shape (distinct symbols / occurrences / jars)

| Shape | Symbols (Minecraft b + a; Forge) | Occurrences | Jars | Approach | Size |
|---|---|---|---|---|---|
| Forge API: `fml.network.*` to `network.*` (95 renames), `RegistryObject` to `registries`, `DeferredRegister.register` signature, `ModLoadingContext.registerConfig` with `ForgeConfigSpec`, `ExtensionPoint`, `ClientRegistry`/`RenderingRegistry`/`IRenderFactory` (to `EntityRenderersEvent`), `ToolType`, `Capability.IStorage` and `CapabilityManager.register` (to `RegisterCapabilitiesEvent`), `FMLServerStartingEvent` and the other lifecycle events, `GatherDataEvent` | 290 | about 4,500 | 300 to 360 | Generated rename table for the moves (the cheap half); helpers for the removed ones (capability registration, renderer registration, extension points) | 5 to 6 days |
| Rendering: fixed-function `RenderSystem` calls (`color4f`, `pushMatrix`, `popMatrix`, `enableAlphaTest`: removed in 1.17), `BufferBuilder.begin(int, VertexFormat)` to `Mode`, `TextureManager.bind`, `ModelPart` constructors and `addBox`/`texOffs` (baked layer definitions in 1.17), `EntityRenderer`/`BlockEntityRenderer`/`MobRenderer` constructors taking a `Context` | 206 + 20 | 2,490 | 213 | Reuse: the GL state emulation of the 1.7.10 and 1.12.2 layers turns such calls into modern render state; layer definitions rebuilt from `ModelPart` calls; renderer constructors bridged | 8 to 10 days (client) |
| World generation: `ConfiguredDecorator` and `ConfiguredFeature.decorated` (to `PlacedFeature`, 1.18), surface builders (gone), `StructureFeature`, `Biome` builders, `Features`, noise settings, `BiomeLoadingEvent` code paths | 313 + 20 | 2,265 | 124 | Biome additions become biome modifiers, code-registered features become placed features through the generator the 1.12.2 layer uses; surface builders have no counterpart and are dropped with a report line | 8 to 10 days |
| Block entities and blocks: `TileEntity` constructor `(BlockEntityType)` to `(BlockEntityType, BlockPos, BlockState)`, `TickableBlockEntity`/`ITickableTileEntity` (to block-supplied tickers), `BlockEntity.load(BlockState, CompoundTag)` to `load(CompoundTag)`, `ClientboundBlockEntityDataPacket`, `BlockState.is(Tag)` to `TagKey`, `Block.is`, `Item.is`, `harvestTool` | 72 + 15 | 805 | 99 to 180 | Generated constructor and a holder for position and state set before the legacy constructor runs; the layer supplies a ticker for types whose class implemented the tick interface; tags through `TagKey` helpers | 5 to 6 days |
| Entities, attributes, AI | 146 + 13 + 32 | 920 | 169 | Rows plus bridges | 3 to 4 days |
| Level and world: `TickList` and `getLiquidTicks` (to scheduled ticks), height limits and section layout (1.18), `LevelAccessor` | 101 + 7 + 6 | 960 | 162 | Rows; height handled as for 1.12.2 chunks | 3 days |
| Tags: `Tag`, `TagCollection`, `Tag.Named`, `Tags.IOptionalNamedTag` (to `TagKey` and holder sets) | 16 + 1 | 100 | 55 to 108 | Helper over `TagKey` | 1.5 days |
| Other Minecraft/server | 57 + 6 | 592 | 163 | Rows | 2 days |
| Network (`ClientboundBlockEntityDataPacket`, packets), client other, data generators, loot, commands, sounds, recipes, explosions | about 100 | about 900 | | Rows and helpers | 4 to 5 days |
| Forge-patch members of Minecraft classes (class u: `getRegistryName`, `isAir(pos)`, `hasTileEntity`, `harvestLevel`, `canRenderInLayer`, `setISTER`, `IModelData` quads...) | 1,287 | 3,058 | 205+ | A facade/helper family: which of these still exist in Forge 40 is not diffable offline; most are removed or moved by 1.17 and 1.18 | 4 to 6 days |
| Plumbing: the version, a cache key, the tables as a build-time overlay, AT and refmap rewriting with the `func_` table, coremod JS (6 jars) | | | | | 4 to 5 days |
| Data: pre-1.18 world generation JSON (`configured_surface_builder`, `structure_feature`, `dimension`, biome JSON with carvers and categories), plural tags | | | | | 3 to 4 days |

Sum about 50 to 60 agent-days of bottom-up work before overlap; shared rows, the generated tables and the 1.12.2/1.7.10 reuse bring it to **35 to 50 agent-days**.

### 4.3 Forge API delta (Forge 36 to 40, measured from the jars)

`forge_api_index.py`: Forge 36.2.42 has 1,326 indexed classes, Forge 40.3.12 1,340; **438 classes are removed (109 moved by simple name, 329 gone) and 452 added**; of the methods of surviving classes 114 changed signature, 559 were removed and 73 fields were removed.
The gone ones by package: `client.event` 48 (the `GuiScreenEvent` family, `DrawHighlightEvent`, `FOVUpdateEvent`, `RenderWorldLastEvent`, `GuiOpenEvent`), `common.model.animation` and `client.model.animation` (50, the animation framework), `fml.network` 24
(moved, not gone, mostly), `common.extensions` 16, `server.command` 14, `fml.relauncher.libraries` 10, `server.permission.context` 10. The moves: `fml.network.*` to `network.*`, `fml.client.registry.*` to `client.*`,
`fml.RegistryObject` to `registries.RegistryObject`, `EntityTeleportEvent` out of `event.entity.living`, `ClientModLoader`, `ConfigGuiHandler`, `GuiUtils` into `client`, `ForgeI18n`, `LogicalSidedProvider` into `common`.
Forge 40 to 43 and 43 to 47 are on the [1.18.2 page](Legacy-1.18.2-Estimate.md) (341 and 41 classes removed).

What 1.16.5 mods use that has no 1.18.2 counterpart as written: `@CapabilityInject` and `CapabilityManager.INSTANCE.register(Class, IStorage, Callable)` (66 jars reference `Capability.IStorage`), `ToolType`/`harvestTool`/`harvestLevel` (99 jars:
mining level became block tags), `ExtensionPoint.CONFIGGUIFACTORY` (54), `RenderingRegistry.registerEntityRenderingHandler` (98) and `ClientRegistry.bindTileEntityRenderer`, `Item.Properties.setISTER` (36),
`FMLServerStartingEvent` and the other FML lifecycle events (68), `ObfuscationReflectionHelper` (75 jars, its string arguments are SRG names), `IModelData`-based baked models.
Networking (`SimpleChannel`, `NetworkEvent.Context`, `PacketDistributor`, 100 to 156 jars) is a **pure package move**; config (`ForgeConfigSpec`) and `mods.toml` need no change beyond the `ModLoadingContext.registerConfig` signature.

### 4.4 Java 8, old mixin, coremod JavaScript

- **Java 8 bytecode (426 of 439 jars).** The layer already rewrites Java 8 constructs for 1.7.10 to 1.12.2 jars (`JavaCompatRedirects`, `compat/jdk`); 1.16.5 mods add the Forge 36 module-less layout (no `module-info`, shaded libraries relocated by hand).
  The class version itself is no problem for a Java 21 runtime; reflection into JDK internals and `sun.misc.Unsafe` use are the known risks and were not scanned.
- **Mixin (209 jars, 1,635 classes, 200 configs at JAVA_8).** The refmaps of 149 jars contain `func_`/`field_` ids that must be translated to the Mojang names of 1.21.1 with the 1.16.5 table and then repaired against the 1.21.1 target (the 1.20.1 repair does this for `m_` ids and
  does not know `func_`). Mixin targets of the 1.16.5 era (`ItemRenderer`, `WorldRenderer`, `ServerWorld`, `TileEntity`) are class names that changed in 1.17: the mixin config's `targets` string must pass through the class table too.
  JAVA_8 compatibility is accepted by Mixin 0.8 and later.
- **Coremods.** 6 jars carry `coremods.json` with JavaScript (the Nashorn-era format: modern JVMs do not include Nashorn, and the old `ASMAPI` helper names are SRG based); the layer's own coremod handling (`LegacyCoreModTransformers`, `CoremodPatches`) is where these go;
  6 of 439 is a small number, a per-mod decision as for 1.12.2.
- **Access transformers (161 jars with SRG ids):** class and member names in the AT must go through the same two tables; the 1.20.1 AT handling reads Mojang class names and `m_` ids.
- **No jar-in-jar:** libraries are shaded; no nested-jar handling is needed (and none can help).

### 4.5 Data and resources

From `survey_modern_jars.py`: plural folders everywhere (`recipes`, `loot_tables`, `tags/blocks`; 269 jars, handled by the 1.20.1 data converter), `pack_format` 6 (326 jars; 1.21.1 uses 34), recipes and loot tables in the 1.16 formats that 1.20.1 reads
(`forge:` ids and conditions as in 1.20.1), 26 jars with Forge loot modifiers. World generation is the data cost: 1.16.5 `worldgen/configured_surface_builder` (4 jars) and `structure_feature`/`configured_structure_feature` (3 jars)
have no 1.21.1 counterpart in this form, pre-1.17 `dimension_type` (9 jars) lacks the height fields, and the biome JSON of 1.16.5 (6 jars) differs from 1.18 (carvers moved, `category` kept, `depth` and `scale` gone).
`template_pool` (2,167 files) and `processor_list` keep their format. Size: 3 to 4 days.

## 5. Risks

1. **Fixed-function rendering is the largest single risk** (rendering: 206 changed symbols in 213 jars). The reuse of the 1.7.10 and 1.12.2 GL emulation is untested for 1.16.5-era calls (`RenderSystem.pushMatrix`, `color4f`, `enableAlphaTest`),
   and custom `RenderType`s and shaders are rebuilt from legacy state. Parity is visual and needs real screenshots.
2. **Block-entity ticking and construction** change the object model (1.17), not a name: the holder and the ticker bridge have no precedent in the layer.
3. **Forge's binary patches** (1,287 symbols) are a long tail of small helpers; the offline measurement cannot say which still exist in 1.20.1 and Forge 47.
4. **World generation** (surface builders, decorators, `BiomeLoadingEvent`) must go through native hooks (biome modifiers, placed features): the 1.12.2 generator is the base, and world-gen mods are the hardest to verify without a client.
5. **Mixins and ATs in SRG names** need the 1.16.5 table at run time and a repair against 1.21.1 classes that differ by two game versions; a dropped injection silently removes a feature.
6. **Three stages in sequence.** A 1.16.5 mod runs A, B, C and D: every defect in a later stage appears here, and the plan's own risk (the first place where the shim-version tools and the remap pipeline meet) holds.
7. **Library bridges** (Curios 37, Waila 17, Architectury, TheOneProbe, CraftTweaker, Patchouli) are the 1.16.5 builds of libraries whose APIs changed in 1.17; the existing bridges target the 1.20.1 API.

## 6. Staged plan (after stages B, C and D exist)

| Stage | Content | Days |
|---|---|---|
| 0. Plumbing and tables | the 1.16.5 id and class tables as build-time overlay, AT and refmap rewriting with the 1.16.5 names, the generated rename table of stage A (167 symbols, 95 of them Forge), cache key, mixin target index | 4 to 5 |
| 1. Forge API | network and registry moves, capability registration helper, extension points, renderer registration, tool types to tags, lifecycle events | 5 to 6 |
| 2. Object model | block-entity constructor holder and tickers, tags, level ticks and height | 8 to 10 |
| 3. Rendering (client) | GL emulation for `RenderSystem` fixed-function, `ModelPart` layer definitions, renderer constructors | 8 to 10 |
| 4. World generation and data | decorators, surface builders, biome modifiers, structure and dimension JSON | 11 to 14 |
| 5. Forge-patch helpers and the rest | the 1,287 u symbols, entities, server rows, network, client, loot | 8 to 10 |
| 6. Run time | server suites to "Done", `verify.sh` suite, mixin triage, first client scenarios | open; 10 to 14 for an Alpha |

Sum 44 to 55 agent-days including some overlap, 35 to 50 to the link ceiling. Order by jars unlocked: the greedy list in `1.16.5-gap-summary.txt` starts with the Forge config/FML shape (14 jars), then the stage B event renames.

## 7. What this measurement cannot show

- **No translated run.** The 1.20.1 transformer was not run on 1.16.5 jars (it expects Mojang class names and 1.17+ code), so class D (2,330 distinct, 17,907 occurrences) is the untranslated 1.20.1 to 1.21.1 delta plus layer gaps; the ceiling
  (56 of 439) is a lower bound and its extrapolation (about 25%) is an assumption from the 1.18.2 sample.
- **Forge's binary patches** (1,287 symbols of class u) have no index: their fate in 1.18 to 1.20 is unknown offline. The same holds for classes the layer synthesises.
- **Behaviour.** Linking says nothing about rendering, tick order, world generation or networking; the 1.16.5 object model changes (block entity ticking, chunk height, GL state) are only visible at run time.
- **Libraries and other mods** (Curios, Waila, Architectury...) are not on the probe's class path, and their 1.16.5 APIs against 1.21.1 are not measured.
- **Sample bias:** most downloaded content mods; abandoned 1.16.5 packs (the stage's real audience) will be heavier on rendering and world generation.
- **Classification is mechanical** (names and descriptors; renames only when the owner and descriptor pair up uniquely; field types are not in Mojang's lists). 1.16.5 to 1.18.2 spans two Minecraft versions, so a symbol marked "removed" may be a rename
  that crossed 1.17 and the heuristic cannot pair.
- **Mixin repair, resources (textures, models, sounds) and GUI parity** are not measured at all; the data survey counts files, it does not convert them.

## 8. Reproducing

```
export JAVA_HOME="/c/Program Files/Java/jdk-21"
python tools/compat/fetch_probe_mods.py 1.16.5 <dir> --limit 400 --content
./gradlew :loader:genSrgMojang1165 :loader:genSrgMojang1182 :loader:genSrgMojang1192 :loader:genSrgMojang1201 -Pexperimental.mc1165=true -Pexperimental.mc1182=true -Pexperimental.mc1192=true
python tools/dev/mc_name_diff.py loader/build/mojang-mappings/1.16.5-client.txt loader/build/mojang-mappings/1.18.2-client.txt <out>
python tools/compat/forge_api_index.py <idx>/forge-1.16.5.tsv forge-1.16.5-36.2.42-universal.jar forge-1.16.5-36.2.42-launcher.jar eventbus-4.0.0.jar \
   --rename loader/build/generated/srg-mojang-1165/legacyconnector/mappings/1.16.5/mc-classes.tsv      # and the 1.18.2, 1.19.2, 1.20.1 indexes without --rename
./gradlew gap1165 -Pexperimental.mc1165=true -Pgap.extraRenames=loader/build/generated/srg-mojang-1165/legacyconnector/mappings/1.16.5/mc-classes.tsv \
   -PtargetJars=@<targets.txt> -PgapJars=<dir> -PgapReport=<out>/rawA.txt
python -I tools/compat/gap_stage_analyze.py --detail <out>/rawA.txt.detail.tsv --jars <dir> --table <srg-mojang-1165.tsv> --forward loader/src/main/resources/legacyconnector/forward/1.20.1 \
   --out <ana> --chain 1.16.5,1.18.2,1.19.2,1.20.1 --client 1.16.5=... --client 1.18.2=... --client 1.19.2=... --client 1.20.1=... --forge 1.16.5=... (and the other three) \
   --iddiff 1.18.2:1.19.2=<diff>/member-diff.tsv --iddiff 1.19.2:1.20.1=<diff>/member-diff.tsv --md <symbols.md>
python -I tools/compat/survey_modern_jars.py <dir>
```

The raw run takes about 3 minutes for 442 jars. `docs/data/` is untouched by all of this.
