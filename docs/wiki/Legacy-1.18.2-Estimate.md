# Estimate: legacy 1.18.2 Forge mods on NeoForge 1.21.1

Research note, 2026-11-24. **Scoping only: no 1.18.2 support exists in the layer.** The plan ([1.16 to 1.19 plan](Legacy-1.16-1.19-Plan.md)) calls it stage B (1.18.2 to 1.19.2) and the
roadmap ([Version-Roadmap](Version-Roadmap.md)) lists 1.18.2 as "renames and diffs over the 1.20.1 pipeline". This page replaces that guess with a measurement, the same way the
[1.19.2 estimate](Legacy-1.19.2-Estimate.md) did for stage C: 446 real Forge 1.18.2 jars were run through the 1.20.1 pipeline as it is today, and what was left was walked along
1.18.2 to 1.19.2 to 1.20.1 and classified. Back to [Home](Home.md). Sister page: [Legacy 1.16.5 estimate](Legacy-1.16.5-Estimate.md).
Data: `tools/dev/agent-tasks/1.18.2-symbols.md` (top 300 symbols), `1.18.2-gap-jars.tsv` (per jar), `1.18.2-gap-summary.txt` (all counts).

## Verdict

- The 1.20.1 pipeline links **34 of 441** Forge 1.18.2 jars with code today (7.7%; 28 fully, 6 more except for other mods that are not in the sample). The same pipeline gave 50 of 428
  (11.7%) for 1.19.2 and 141 of 506 (28%) for real 1.20.1 jars (ATM 9). Without any translation (own-version names and the 1.20.1 to 1.21.1 renames only) 30 of 442 link.
- The stage B delta (1.18.2 to 1.19.2) is **mostly changed Forge API and a few mass API changes, not renames**. Of 3,024 distinct unresolved Minecraft/Forge symbols of the translated run:
  **580 are one table row** (the 1.18.2 table names ids that the 1.20.1 table lacks: `com.mojang.blaze3d` and others), **100 more are renames or moves of stage B** (33 Minecraft, 67 Forge),
  **743 need real adapters at stage B** (467 Minecraft, 276 Forge), **704 are stage C work** (the 1.19.2 to 1.20.1 delta these jars still carry: 624 + 31 Forge changed or removed, 49 renames), 655 are gaps the
  1.20.1 layer has too (unchanged in 1.20.1), 161 are members added by Forge's own patches or by the layer, 26 are not Minecraft members.
- Share of jars that link against the **next stage's names** (1.19.2) once pure renames are applied: **14.5%** (64 of 441) counting Minecraft and Forge API, **22.2%** counting Minecraft alone.
  Stage B in isolation therefore unlocks little: 377 of 441 jars carry at least one changed or removed symbol of this stage (343 on the Minecraft side). The Forge API is the larger half of it
  (`RegistryEvent`, `IForgeRegistryEntry`, the world-to-level event renames, `BiomeLoadingEvent`, `ForgeIngameGui`).
- Doing all of stage B, stage C and the table raises the link rate from 7.7% to **22.7% (100 jars)**, which is the ceiling of the 1.20.1 layer on this kind of sample (1.19.2's was 29.7%; the
  1.18.2 jars carry more stage B and C symbols and more gaps that are the 1.20.1 layer's).
- **Relative size** (distinct symbols that need real work, same sample): stage B 743, stage C 655, so stage B is about as large as stage C by symbol count and about 60 to 70% of its
  effort in days (no GUI-drawing rewrite, but the Forge 40 to 43 API break and the `Random` to `RandomSource` family). Stage D (1.20.1 to 1.21.1) is the unit: 25 to 30 agent-days to broad coverage
  ([1.20.1 estimate](Legacy-1.20.1-Estimate.md)).
- **Honest estimate for stage B alone, on top of an existing 1.19.2 stage: 14 to 18 agent-days (4 to 5 days wall-clock with about five agents) to the link ceiling, plus 6 to 9 for an Alpha**
  (server suites to "Done", mixin triage). Plus or minus 40%. Built before 1.19.2 exists, 1.18.2 would also carry stage C (22 to 28 agent-days), i.e. 36 to 46 agent-days in total.

## 1. Method

All offline; no game, server or client was started, and nothing downloaded here is executed (jars are only read by our own tools). The default build fetches and needs nothing of 1.18.2:
everything is opt-in, like the 1.19.2 table.

1. **Sample.** `python tools/compat/fetch_probe_mods.py 1.18.2 <dir> --limit 400 --content` (Modrinth, Forge files, most downloaded content mods plus required dependencies, SHA-1 checked), into
   `test-mods-probe/1.18.2-modrinth-set/` (gitignored, never committed).
2. **SRG to Mojang table of 1.18.2.** `./gradlew :loader:genSrgMojang1182 -Pexperimental.mc1182=true`: MCPConfig `1.18.2-20220404.173914` `joined.tsrg` joined with Mojang's `client.txt`
   (SHA-1 `a661c6a55a0600bd391bdbbd6827654c05b2109c`, verified) by obfuscated owner, name and descriptor, parameterised like the 1.19.2 table (`srgMojangTables` in `loader/build.gradle`).
   55,263 rows (42,102 of 42,102 methods and 27,073 of 27,073 fields matched). Build-local, never added to the loader jar; without the property nothing is declared or fetched.
3. **Gap runs.** `./gradlew gap1182 -Pexperimental.mc1182=true` (`Gap1201` registered with the 1.18.2 table) is **run A**: own-version names plus the 1.20.1 to 1.21.1 renames, no translation.
   `./gradlew gap1201 -Pgap.translate=true` over the same jars is **run C**, the real `Forge1201Transformer` (call rewrites, stand-ins, facades, soft links) as if they were 1.20.1 jars: it
   is the measurement, as for 1.19.2. Run C leaves SRG ids that the 1.20.1 table does not know; the analyzer names them with the 1.18.2 table and counts them as stage 0.
4. **Mechanical diffs.** `tools/dev/mc_name_diff.py` joins two versions' Mojang-named class and member lists by name (the right key between 1.18.2 and 1.19.2, because MCPConfig changes a method's
   id when its descriptor changes, e.g. `Random` to `RandomSource`: the id join of `tools/dev/mc_member_diff.py` sees such methods as removed plus added); `mc_member_diff.py` still supplies the
   renames and moves that keep their id. `tools/compat/forge_api_index.py` indexes the public API of the Forge jars (universal, fmlcore, javafmllanguage, fmlloader, eventbus of Forge 40.3.12,
   43.5.2 and 47.4.29) so Forge symbols are diffed too, which the 1.19.2 scoping could not do.
5. **Classification.** `tools/compat/gap_stage_analyze.py` walks every unresolved symbol along the chain and records the first transition that breaks it (stage B = 1.18.2 to 1.19.2, stage C = 1.19.2 to 1.20.1,
   stage 0 = a table row), the class (a = a rename or move row fixes it; b = signature changed or removed; D = unchanged up to 1.20.1; F-prefixed = Forge API; u = member of a Minecraft
   class that Mojang's list does not have, added by Forge's binary patches or by the layer; n = not a Minecraft member), the shape (the same list as 1.19.2) and the per-jar blockers.
   `tools/compat/survey_modern_jars.py` surveys the structure of the jars (class file version, mixin, access transformers, coremods, data layout).

## 2. Sample

| | |
|---|---|
| Jars | **446** (397 most downloaded content mods that have a 1.18.2 Forge file plus 49 required dependencies; 400 projects) |
| With `META-INF/mods.toml` | 440; 49 also carry a `fabric.mod.json` or `quilt.mod.json` (multi-loader), 9 also a `neoforge.mods.toml` |
| With code | 442 (441 analysed in run C; 4 are data or resource packs) |
| Class file version | 420 jars Java 17 (major 61), 11 Java 16, 9 Java 8 (52); `loaderVersion` `[40,)` in 255, `[38,)` in 59 |
| Mixins | 287 jars, 3,117 mixin classes in 402 configs; `compatibilityLevel` JAVA_17 228, JAVA_8 32, JAVA_16 28; 269 jars have a refmap, **215 with SRG ids inside** (`m_123_`, which need the table) |
| Access transformers | 175 jars, 144 with SRG ids; 8 jars with `coremods.json` (31 JavaScript files); 20 with jar-in-jar |
| Data | plural folder names (`recipes`, `loot_tables`, `tags/blocks`) in 286 jars (126,921 files; the 1.20.1 converter already handles them); `pack_format` 9 (168), 8 (159), 6 (51), 15 (16); Forge loot modifiers in 28 jars |
| World generation data | `configured_structure_feature` 769 files in 30 jars, `template_pool` 2,441 in 33, `structure_set` 218 in 33, `placed_feature` 1,290 in 9, `configured_feature` 1,151 in 10, biomes 164 in 4 |
| Libraries they link against (jars with unresolved references into them) | JEI (73), Curios (34), Create (31, with Registrate 25 and Flywheel 25), Cloth Config (18), Farmer's Delight (16), Puzzles Lib (16), REI (14), Architectury (14), Blueprint (14), Mod Menu (13) |

Same caveat as the 1.19.2 sample: this is the most downloaded set, not the abandoned one, so many of these mods have a 1.21.1 port; the numbers measure the layer, not the demand.
Fabric intermediary names of multi-loader jars (1,349 symbols) are dropped: that code never loads on Forge.

## 3. Link numbers

| Run | What it shows | Jars with code | Link fully | Link but for other mods/libraries | Unresolved |
|---|---|---|---|---|---|
| A: 1.18.2 table, renames only | the tables alone | 442 | 26 | 4 | 412 |
| **C: the 1.20.1 layer as it is** | what a 1.18.2 jar gets today | **441** | **28** | **6** | **407** |
| (1.19.2, same layer, from the 1.19.2 page) | for comparison | 428 | 35 | 15 | 378 |
| (ATM 9, real 1.20.1 jars) | for comparison | 506 | 141 | | |

Unresolved symbol occurrences of run C by area: libraries and other mods 29,712 (missing from the sample, not the layer's), Minecraft common 12,578, Minecraft client 4,688, Forge API 4,201.

**Link rate by stage** (run C; every cumulative line also counts the table, stage 0; "full" = nothing else is left unresolved either, i.e. the jar would link on 1.21.1):

| Done so far | Jars with no symbol of those stages left (no work) | Jars whose symbols there are all rename rows | Full link after the work | |
|---|---|---|---|---|
| nothing (today) | 34 of 441 (7.7%) | | 34 | measured |
| + stage 0 (table) and stage B | 45 (10.2%) | 64 (14.5%) | 62 (14.1%) | projection from the symbol list |
| + stage C | 36 (8.2%) | 43 (9.8%) | **100 (22.7%)** | projection: the ceiling |

(The "no work" column shrinks while the stages grow, because each stage adds blockers; read it as "jars that need none of these stages". The projections assume every a and b symbol is fixed
and every symbol that survives the chain is the 1.20.1 layer's, as in the 1.19.2 page.)

Stage-local (only the symbols that break in exactly one transition): **stage B: 64 of 441 jars (14.5%) have no changed or removed symbol** (Minecraft and Forge), 98 (22.2%) counting Minecraft alone;
stage C: 83 (18.8%).

## 4. Classification of the unresolved Minecraft/Forge symbols (run C: 3,024 distinct, 21,467 jar x symbol)

| Class | Meaning | Distinct symbols | Occurrences | Is it stage B work? |
|---|---|---|---|---|
| a, stage 0 | the 1.20.1 table lacks the SRG id, the 1.18.2 table names it (the member is unchanged up to 1.20.1) | 580 | 5,500 | yes, mechanical (one table) |
| a, stage B | a rename or move between 1.18.2 and 1.19.2 | 100 (33 Minecraft, 67 Forge) | 870 | yes, mechanical |
| **b, stage B** | changed or removed at 1.19 (Minecraft 467, Forge 276) | **743** | **5,893** | **yes, the work** |
| a / b, stage C | still carried: changed or removed at 1.19.3 to 1.20 (49 a, 655 b incl. Forge) | 704 | 6,012 | no, stage C |
| D | unchanged up to 1.20.1, still unresolved: a gap of the 1.20.1 layer | 655 (+53 Forge) | 2,106 (+159) | no |
| u | member of a Minecraft class that is in no Mojang list (Forge patches, the layer's stand-ins) | 161 | 675 | partly (Forge patches) |
| n, F | not a Minecraft member; Forge API not diffed | 28 | 252 | no |

### 4.1 What changed between 1.18.2 and 1.19.2 (mechanical, by name)

`mc_name_diff.py` over Mojang's official names (classes, methods with descriptor, fields):

| | Same | Moved | Signature changed | Removed | Added |
|---|---|---|---|---|---|
| Classes | 6,107 | 152 | | 140 | 752 |
| Methods | 49,121 | 832 | 1,870 | 2,024 | 8,790 |
| Fields | 27,542 | | | 1,319 | 3,151 |

("Moved" = gone from its class, the same name and descriptor added elsewhere or the class moved; "signature changed" = the class still has a method of that name with another descriptor.)
The same table for 1.19.2 to 1.20.1 (stage C): classes 6,584 same / 86 moved / 189 removed, methods 50,922 same / 774 moved / 2,459 signature changed / 3,756 removed, fields 28,703 same / 1,990 removed.
Stage B is the smaller Minecraft delta of the two (fewer removed methods and classes) with the larger Forge delta.

### 4.2 Class b of stage B, by shape (distinct symbols / occurrences / jars; Minecraft "b" plus Forge "Fb")

| Shape | Symbols | Occurrences | Jars | Adapter in the style of `v1_20_1/migrate` | Size |
|---|---|---|---|---|---|
| Text components: `TextComponent`, `TranslatableComponent` constructors to `Component.literal/translatable` | 4 | 475 | 284 | Constructor-to-factory rows (and the two classes become stand-ins for `instanceof` and subclasses) | 0.25 to 0.5 day |
| Registration (Forge): `IForgeRegistryEntry`, `ForgeRegistryEntry`, `setRegistryName`/`getRegistryName`, `RegistryEvent.Register`, `IForgeRegistry.getValue/getKey/register`, `ForgeRegistries.ENTITIES/BLOCK_ENTITIES/CONTAINERS...` (renamed in 1.19) | 23 + 2 | 726 | 239 | A registry-name helper recording the name set in the constructor chain, `RegistryEvent` as a view over the register event, field rows | 2 days |
| Forge events: world to level (`BlockEvent.BreakEvent` moved to `event.level`), `PlayerEvent.getPlayer()` to `getEntity()` and the same on `PlayerInteractEvent`, `EntityJoinWorldEvent`, `LivingUpdateEvent`, `TickEvent.WorldTickEvent`, `ColorHandlerEvent`, `ParticleFactoryRegisterEvent`, `InputEvent.KeyInputEvent`, `ClientChatReceivedEvent` | about 150 | about 1,600 | 200 to 240 | Generated event table (as the NeoForge one) plus getter rows, a few helpers | 2 to 2.5 days |
| Entities and attributes (event getters excluded) | 82 | 316 | 110 | Rows plus a few bridges | 1.5 days |
| `Random` to `RandomSource` (`getRandom()`, `FeaturePlaceContext.random()`, `Level.random`, `RandomizableContainer`, loot and worldgen signatures): spans entities, level, block, worldgen | 177 across shapes | 1,103 | most jars | Signature bridge both ways (an override taking `Random` is bridged; a call passing `Random` is wrapped): the most error-prone item | 1.5 to 2 days |
| Block/item/state | 70 | 247 | 87 | Rows | 1 day |
| World generation: `StructureFeature` to `Structure`, `ConfiguredStructureFeature`, `Biome.BiomeCategory` (removed), `BiomeLoadingEvent` (gone in 1.19, replaced by biome modifiers), surface and decorator signatures | 78 + 2 | 514 | 89 | Rows, biome-loading emulation through biome modifiers (the layer has the machinery for 1.7.10 and 1.12.2), structure data conversion (below) | 3 days |
| Other Minecraft/server: `Resource.getInputStream`, `ResourceManager.getResource`/`listResources` (Optional and Map), `GameProfile` accessors, `Ingredient.fromJson` | 65 | 416 | 145 | Rows and small helpers | 1 day |
| Client: `ForgeIngameGui` and `IIngameOverlay` (to the 1.19 overlay registry), `ClientRegistry`, `ModelRegistryEvent`, `IItemRenderProperties` (to client extensions), `ConfigGuiHandler.ConfigGuiFactory`, `ItemLayerModel` | 109 | about 750 | 100 to 180 | Rows and facades; the overlay API needs a helper | 2 to 2.5 days (client only) |
| Network (`NetworkHooks.openGui`), chat (`sendMessage(Component, UUID)` to `sendSystemMessage`), sounds, commands, loot, data generators, recipes | 77 | about 720 | | Rows and helpers | 1.5 days |

Sum 16 to 20 agent-days of class b before overlap; with the shared tables (stage 0 and the generated Forge event table do part of the first rows) **14 to 18 agent-days** is the honest figure. Material,
creative tabs, `com.mojang.math`, damage sources and the GUI-drawing rewrite are stage C (1.19.3 to 1.20) and appear in this sample as stage C symbols; they are not part of B.

### 4.3 Forge API delta (Forge 40 to 43 to 47, measured from the jars)

`forge_api_index.py` over Forge 40.3.12, 43.5.2 and 47.4.29:

| Transition | Forge classes | Removed (moved by simple name / gone) | Added | Methods of surviving classes |
|---|---|---|---|---|
| 1.18.2 to 1.19.2 (Forge 40 to 43) | 1,340 to 1,373 | 341 (44 / 297) | 374 | 184 signature changed, 230 removed, 34 fields removed |
| 1.19.2 to 1.20.1 (Forge 43 to 47) | 1,373 to 1,413 | 41 (1 / 40) | 81 | 216 signature changed, 181 removed, 23 fields removed |

The 1.19 transition is the Forge break: of the 297 gone classes about 100 are events (client: `ColorHandlerEvent`, `ModelRegistryEvent`, `ParticleFactoryRegisterEvent`, `RenderGameOverlayEvent`, the old
`InputEvent` and `ScreenEvent` families, `EntityViewRenderEvent`; common: `RegistryEvent$Register`, `BiomeLoadingEvent`, `EntityJoinWorldEvent`, `LivingUpdateEvent`), plus the model package
(`client.model.b3d`, `pipeline`, `obj`, 44 + 24 + 19 + 10 classes), `ClientRegistry`, `FluidAttributes` (to `FluidType`), `ForgeRegistryEntry`/`IForgeRegistryEntry`, `ForgeIngameGui` and `IIngameOverlay`.
Moved: `event.world` to `event.level` (44 classes), `client.gui.ScrollPanel` to `client.gui.widget`, `ModelDataManager`. Unchanged in shape and therefore free: `SimpleChannel`,
`ForgeConfigSpec`, `ModConfig`, `DistExecutor`, `CapabilityManager`/`RegisterCapabilitiesEvent`/`Capability` (the 1.17 capability API is already the one of 1.19.2 and 1.20.1),
`NetworkHooks` (except `openGui`), `AddReloadListenerEvent`, `ICondition` and the conditional recipe machinery. Network, capabilities and config are stage-B-free, which is why they are not in the table above.

Mods.toml and loader differences: Forge 40 to 43 keeps `modLoader="javafml"` with `loaderVersion` `[40,)` becoming `[43,)`; both are Java 17; the 1.20.1 pipeline's `mods.toml` handling applies unchanged. What changes
is the dependency block (`forge versionRange`, `minecraft versionRange [1.18.2,1.19)` to `[1.19.2,1.20)`), which the layer ignores. Registry freezing is earlier in 1.19, so code that reads vanilla
registries before the freeze behaves differently; the 1.20.1 layer's tolerant registry already covers it. 1.19.1 and 1.19.2 added **signed chat**: `ServerChatEvent`, `ClientChatReceivedEvent`
and `Player.sendMessage` changed (11 + 4 symbols here).

### 4.4 Data and resources

Same formats as stage C's data work plus two stage B changes, measured on the sample by `survey_modern_jars.py`: **structures** moved from `configured_structure_feature` (769 files in 30 jars) to
`worldgen/structure` + `structure_set` + `tags/worldgen/biome/has_structure` (218 `structure_set` files in 33 jars already exist; 1.18.2 itself introduced sets), and **biome features** from the
`BiomeLoadingEvent` code path to biome modifiers; `template_pool` (2,441 files, 33 jars), `processor_list` and `placed_feature`/`configured_feature` keep their 1.18.2 format and are the same JSON in 1.20.1.
Plural folder names and `forge:` ids are converted by the existing 1.20.1 data converter. Size: 1.5 to 2 days including tests.

## 5. Risks

1. **Mixins.** 287 of 446 jars carry a mixin config (3,117 classes), 215 with SRG ids in their refmap. 1.19 and 1.19.3 changed GUI, `Level`, `ItemStack`, registry and `RandomSource` targets;
   `mixin-body unresolved` is 3,028 in this run (3,755 raw). The layer's per-injection repair applies, but a dropped injection silently removes a feature. A 1.19.2 index of targets is part of stage 0.
2. **`Random` to `RandomSource` is a semantic change in many override signatures**, not a rename: a mod subclass overriding `Block.randomTick(.., Random)` must still be called with a `RandomSource`,
   and a mod passing its own `Random` must be accepted. This is the main run-time risk of stage B.
3. **Forge API removal is behavioural for events** (`BiomeLoadingEvent`, `RegistryEvent`): a mod that registers late or reads registries early has no counterpart in the 1.19 lifecycle; expect a defect tail.
4. **Libraries** are 1.18.2 builds (JEI, Curios, Create, REI, Architectury, Patchouli, Blueprint, Cloth Config): each needs a check of its 1.18.2 API against the bridge rules written for 1.20.1; their 1.18.2 versions were not inspected.
5. **Dependence on 1.20.1 and 1.19.2.** Every 1.19.2 and 1.20.1 gap is also a 1.18.2 gap; 655 distinct symbols here are the 1.20.1 layer's own, and stage C's 704 symbols are 1.19.2 work that 1.18.2 inherits.
6. **Transformer version.** Every loader output change needs a `TRANSFORMER_VERSION` bump; a 1.18.2 mode must have its own cache key so that a 1.18.2 and a 1.20.1 jar never share an entry.

## 6. Staged plan (after 1.19.2 and 1.20.1 exist)

| Stage | Content | Unlocks | Days |
|---|---|---|---|
| 0. Plumbing | `LegacyVersion` for 1.18.2 (`loaderVersion [40,)`, `minecraft [1.18.2]`), cache key, the 1.18.2 table under the 1.20.1 and 1.19.2 ones (580 symbols, in 345 jars), Forge 40 to 43 diff facades from the generated event table, 1.18.2 mixin target index | nothing visible | 2 to 3 |
| 1. Tables | text components, registration helper, `event.level` renames, getter rows (`getPlayer` to `getEntity`), the ten real class moves | 45 to 64 of 441 jars clear stage B | 3 to 4 |
| 2. Helpers | `RandomSource` bridges, registry-name helper and `RegistryEvent` view, `BiomeLoadingEvent` emulation, overlay registry, client extensions | most stage B jars | 5 to 7 |
| 3. Data | structure and biome data conversion | worldgen mods | 1.5 to 2 |
| 4. Run time | server suites with 1.18.2 content mods to "Done", `verify.sh` suite, mixin triage | what links runs | 6 to 9 |

Sum 17 to 25 agent-days including the Alpha; 14 to 18 of them to the link ceiling. Order inside the stages by jars unlocked (the greedy list in `1.18.2-gap-summary.txt` starts with registration, entities, level, text components).

## 7. What this measurement cannot show

- **Behaviour.** A jar that links can still fail at run time (registration order, `RandomSource`, events fired at other times); only server and client runs show it. The link rate is an upper bound for "works".
- **Libraries and other mods.** 29,712 unresolved occurrences point into JEI, Curios, Create and other libraries that are not on the probe's class path; the numbers above are about Minecraft and Forge only.
  Library API changes between their 1.18.2 and 1.20.1 builds are not measured.
- **Forge's binary patches.** Members that Forge adds to Minecraft classes (161 symbols of class u) are not in any jar of the index, so their changes cannot be diffed offline.
- **Sample bias.** The most downloaded content mods, most of which have newer versions; abandoned packs of 1.18.2 will differ. The 1.18.2 sample is also small (441 jars) and its stage-local percentages carry
  a few points of noise.
- **Stage C and D symbols are measured on 1.18.2 jars**, so the stage C counts here (704) differ from those of the 1.19.2 page (1,157): different jars and different libraries, not different work.
- **Classification is mechanical.** "Removed" and "signature changed" come from names and descriptors; a method can be inherited from a new superclass and still be found, and a rename the heuristic
  does not pair (same owner, same descriptor, exactly one new name) shows as removed. Fields carry no type in Mojang's lists, so field renames are paired by count only.
- **Mixin repair, resources (textures, models, sounds) and GUI parity** are not measured at all.

## 8. Reproducing

```
export JAVA_HOME="/c/Program Files/Java/jdk-21"
python tools/compat/fetch_probe_mods.py 1.18.2 <dir> --limit 400 --content
./gradlew :loader:genSrgMojang1182 :loader:genSrgMojang1192 :loader:genSrgMojang1201 -Pexperimental.mc1182=true -Pexperimental.mc1192=true
python tools/dev/mc_name_diff.py loader/build/mojang-mappings/1.18.2-client.txt loader/build/mojang-mappings/1.19.2-client.txt <out>
python tools/dev/mc_member_diff.py loader/build/tmp/mcpconfig1182/joined.tsrg loader/build/mojang-mappings/1.18.2-client.txt \
   loader/build/generated/srg-mojang-1182/legacyconnector/mappings/1.18.2/srg-mojang.tsv loader/build/tmp/mcpconfig1192/joined.tsrg \
   loader/build/mojang-mappings/1.19.2-client.txt loader/build/generated/srg-mojang-1192/legacyconnector/mappings/1.19.2/srg-mojang.tsv <diff-182-192>
python tools/compat/forge_api_index.py <idx>/forge-1.18.2.tsv forge-1.18.2-40.3.12-universal.jar fmlcore-...jar javafmllanguage-...jar fmlloader-...jar eventbus-5.0.3.jar   # also 1.19.2 and 1.20.1
./gradlew gap1201 -PtargetJars=@<targets.txt> -PgapJars=<dir> -PgapReport=<out>/transC.txt -Pgap.translate=true        # run C
./gradlew gap1182 -Pexperimental.mc1182=true -PtargetJars=@<targets.txt> -PgapJars=<dir> -PgapReport=<out>/rawA.txt  # run A
python -I tools/compat/gap_stage_analyze.py --detail <out>/transC.txt.detail.tsv --jars <dir> --table <srg-mojang-1182.tsv> --forward loader/src/main/resources/legacyconnector/forward/1.20.1 \
   --out <ana> --chain 1.18.2,1.19.2,1.20.1 --client 1.18.2=... --client 1.19.2=... --client 1.20.1=... --forge 1.18.2=... --forge 1.19.2=... --forge 1.20.1=... --iddiff 1.18.2:1.19.2=<diff>/member-diff.tsv --md <symbols.md>
python -I tools/compat/survey_modern_jars.py <dir>
```

Run C took about 9 minutes for 446 jars (single JVM), run A about 2 minutes. `docs/data/` is untouched by all of this.
