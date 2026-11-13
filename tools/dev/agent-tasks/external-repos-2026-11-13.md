# External repos: Retromod and Translator (research, 2026-11-13)

Clones (shallow, read only, never built): `C:/Users/Kyleb/AppData/Local/Temp/claude/Y--Programming-Minecraft-Retrofit/676f9795-c068-4b8a-b7ec-147914f18687/scratchpad/ext/retromod` and `.../ext/translator` (translator HEAD 5775d82, 2026-11-13).
Paths below are relative to the repo named in the heading. "Ours" paths are relative to the Retrofit checkout.
Method note: this is a read of docs, file lists, headers, table heads and the sizes of the relevant classes. I did not run either project, and I did not read every bridge line by line; "Ours already has X" claims come from grep/directory listings and should be re-checked by the worker before copying anything.

Our gap data used (current): `docs/data/probe.json` run 1.20.1/0.4.14/atm9 = 494 jars, 99 links, 264 `gap_shim`, 108 `gap_mod`, 2 `gap_lib`, 21 obsolete; 1.12.2/0.4.14/modrinth = 545 links, 33 stubs, 5 `gap_shim`, 39 `gap_mod`, 18 `gap_lib`. So 1.12.2 link gaps are nearly closed; the 1.12.2 problems are behaviour (issue triage classes model-loader 15, item-render 11, load-failure 6, entity-ai 5...). The 1.20.1 gaps are a long tail of single Minecraft symbols (SheetedDecalTextureGenerator, GameProfile.isComplete, AdvancementHolder$Builder, CrashReport.getFriendlyReport, Checkbox.<init>, Lighting.setupLevel, RegistrySetBuilder.buildPatch, ItemPredicate.<init>, MobEffect.applyEffectTick ...) plus the older `1.20.1-missing-references.md` list (criterion triggers, RecipeSerializer fromJson/toNetwork, CauldronInteraction, PortalInfo, dispenser behaviours, MapDecoration$Type).

---------------------------------------------------------------------------------------------------

## 1. Retromod (Bownlux/Retromod)

### 1.1 Licence (checked)

- `LICENSE`: "MIT License, Copyright (c) 2026 Bownlux". Standard MIT text, no extra conditions.
- Source headers: every Java file I sampled carries "Retromod - Backwards Compatibility Layer for Minecraft Mods / Copyright (c) 2026 Bownlux. Licensed under MIT License." Exceptions: `src/main/java/baubles/api/*` (stand-ins of the third-party Baubles API, no Retromod header; do not copy those).
- `THIRD_PARTY_NOTICES.md`: bundles ASM (BSD-3), Gson (Apache-2.0), SLF4J (MIT), toml4j (MIT). Mapping/name tables: Fabric intermediary CC0-1.0; Mojang official mappings (the Microsoft notice: "may not redistribute the mappings complete and unmodified"); MCPConfig (Copyright (c) 2018 Forge Development LLC); and for the 1.12.2 tables "MCP 1.12.2 names: member names from MCPBot's stable exports, class names from MCP ... not recorded in this repository" (licence not recorded). Fabric API files "adapted" would stay Apache-2.0 but none are adapted yet.
- README credits Sinytra Connector "parts of which Retromod adapts under the MIT License" (not itemised in the notices file).
- Consequence for us: code and scripts are free to copy with the MIT notice in `THIRD_PARTY_NOTICES.md` (copyright line + licence text). Name tables derived from Mojang/MCP data carry the Mojang "no complete unmodified redistribution" and MCP status; do not copy the big `srg-*`/`target-srg-*` tables verbatim, regenerate them from our own inputs with their harvest scripts instead (we already ship derived mapping data from the same sources).

### 1.2 Map

Purpose: Maven project (JDK 25 build, Java 17 bytecode) that rewrites old mod jars at load for newer hosts. Loaders it runs ON: Fabric, Quilt (same jar), Forge, NeoForge; hosts 1.20 to 26.3 (Forge 1.20-26.3, NeoForge 1.20.1-26.3). Mods it accepts: Fabric 1.14.4+, Forge 1.12.2+, NeoForge 1.20.1+. It also carries a CLI (`batch`, `aot`, `--verify`, `--mc-jar`) and a Swing GUI.

Size (4165 files in the clone): `src/main/java` 1197 Java files, `src/test/java` 1436, `src/legacyapi` 872 (rebuilt old Forge/NeoForge API classes plus compile-only stubs), `test-mods/` (in-game test mods per loader, about 250 Java files), 65 compatdb data files, 35 scripts. Resource tables in `src/main/resources/retromod` (about 17 MB, mostly 5 SRG tables).

Docs read first: `README.md`, `docs/architecture.md`, `docs/TECHNICAL_CAPABILITIES.md`, `docs/incompatible-mods.md`, `docs/changelog.md`, `CLAUDE.md` (house rules), `docs-dev/sinytra-connector-analysis.md`.

How translation works (`docs/architecture.md`):
1. Detect source MC version and loader from metadata.
2. Pick a chain of "version shims" (`shim/forge/Forge_1_12_2_to_1_13_2 ... Forge_1_20_to_NeoForge_1_21 ... Forge_26_2_to_26_3`, same for Fabric, about 70 transitions) found by breadth-first search; each registers class, method, field, constructor and signature redirects (`core/RetromodTransformer`, `core/FuzzyMethodResolver` 1676 lines).
3. Remap Fabric intermediary or Forge SRG names (tables above; `SrgToMojangMapper`).
4. Rewrite classes, mixin annotations, refmaps, access wideners, metadata, nested jars.
5. Embed replacement APIs ("polyfills", 36 providers; "Legacy*Bridge/Adapter/Synthetic" classes that generate small helper classes with ASM, `SyntheticEmbedder`).
6. Verify references; cache (`AotCompiler`).
Mixins: 25 classes, 17.4k lines (`mixin/AutomaticMixinTranslator` 1419, `MixinCompatibilityTransformer` 2395, `MixinHostDriftRepair` 1979, `MixinHandlerResignature` 754, refmap repair, local-slot repair, blocklist `mixin-blocklist.json` 19 KB, `mixin-selector-renames.tsv`). Repairs are proven against the actual target jar (`--mc-jar`); it refuses ambiguous cases.
Resources/data (`resources/`, 74 files): item flattening, lang `.lang`->JSON, forge_marker blockstates (`LegacyBlockStateMigrator` 1319), forge recipes + ore dictionary (`LegacyForgeRecipeMigrator` 496, `LegacyOreDictionary`), NBT-to-components in data (`ItemStackDataMigrator` 920, `ItemStackComponents` 543), loot conditions, smithing, pack format, dimension types, noise settings, spawners, structure data (many of these target 1.21.x to 26.x).
Runtime bridges: `shim/forge` 140 files, `shim/common` 236, `shim/fabric` 67 + `shim/api/fabric` 29 (Fabric API versions, REI/EMI/Cloth/CCA shims).
Tests: 1436 JUnit files (offline against a fake Minecraft host, `testsupport/fakemc`), plus `test-mods/` that run in a real launch and print pass/fail lines (Forge test mod has about 87 case classes against Forge 1.20.1).
Scripts: `scripts/*.py` and `ForgeEventTable.java`/`NeoForgeEventTable.java`/`LegacyApiLinkCheck.java` harvest tables from jars (reproducible).

Fabric: it is a first-class target (Fabric/Quilt hosts 1.20-26.3, mods 1.14.4+): intermediary to Mojang remap, Fabric API version shims, access widener repair. Does not apply to us today (we do not load Fabric mods; Sinytra Connector does). The only Fabric-adjacent idea worth keeping: `docs-dev/sinytra-connector-analysis.md` (what Retromod would learn from Sinytra), not for us.

### 1.3 Comparison with ours

| Feature | Retromod | Ours | Verdict |
|---|---|---|---|
| 1.20.1 -> 1.21.1 renames | SRG-to-Mojang, class moves, `Forge_1_20_to_NeoForge_1_21` (803 lines) + about 130 `Legacy*Bridge` | `mapping/McpMappings`, `forward/1.20.1/*.tsv` (1.5 MB), `Mc1201Migration`, `CallRewrites1201`, `Forge1201Transformer` | comparable; theirs is generated per symbol class, ours is table-driven |
| Call/constructor rewrites | ASM helper classes generated per mod | static helpers in `v1_20_1/migrate/*` (about 60 classes) | equivalent; their catalogue is larger on recipes, criteria, enchantments, teleporter, dispenser |
| Forge events / abstract events | generated tables `forge-event-renames.json`, `neoforge-abstract-events.tsv` (135 rows for 1.21.1), `forge-eventbus7-cancellable.tsv` | `forge-event-renames.tsv` (312 rows), hand-curated `ABSTRACT_EVENTS` set | theirs is generated from jars; worth diffing |
| Mixins | 17k lines, signature repair proven against target jar, blocklist, local-slot repair | `transform/Mixin1201` 447 lines: rename strings, drop `require`, drop mixins with missing targets, config not required | large gap in repair depth |
| Data/resources 1.20.1 -> 1.21.1 | dirs singular, NBT->components in recipes/loot/advancements, loot conditions, smithing | `resource/DataConverter1201` 1112 lines (regex NBT) , `FunctionConverter1201`, `StructureNbtConverter1201` | overlap; theirs more structured |
| 1.12.2 | lifecycle + event bus + registration of simple content, class moves 1182 rows, SRG members 7.7k, flattening tables | full shim `legacyconnector/v1_12_2` + `ORG/v1_12_2` (blocks with `meta`, tile entities, GUIs, fluids, worldgen, rendering) | ours is far ahead; their changelog admits 1.12 worldgen/GUIs/entity rendering "do not run" |
| 1.12.2 item flattening | `legacy-item-flattening.tsv` 377 rows | `Vanilla1122` uses the game's own data fixers via `core.flatten.VanillaFlattening` | ours is better; table not needed |
| Tests | 1436 offline tests + real-launch test mods | `checkLegacyMod`, probe, server self test, corpus | they have a fake-host unit test layer we lack |
| Gap reporting | `--verify` per-jar reports, compatdb | probe.json, `Gap1201`, site | comparable |

### 1.4 Candidates from Retromod (most valuable first)

R1. Mixin repair algorithms (1.20.1). Files: `src/main/java/com/retromod/mixin/AutomaticMixinTranslator.java` (1419), `MixinHandlerResignature.java` (754), `MixinRefmapTargetRepair.java` (264), `MixinRefmapRepairIndex.java` (116), `MixinLocalSlotRepair.java` (160), `MixinTargetKindRepair.java` (111), `MixinShadowFieldDemotion.java` (193), `MixinSelectorRenames.java` (127) + `retromod/mixin-selector-renames.tsv`, `scripts/mixin-*.py`. Gap: our `Mixin1201` only renames strings and drops `require`, so a mixin whose target method gained a parameter or changed return type silently injects nothing (Translator's table also lists "mixins not applied" as its biggest class). Use: re-implement the "ordered-subsequence parameter addition, unique same-name target, proven against the host class index" rule on top of our `Mc1201Hierarchy`/`mc-members.index.gz`, which already is a host-class index. Effort L. Licence: MIT (copy with notice; our index API differs so it is a port, not a paste).

R2. Their Forge 1.20.1 in-game test mod as regression fixture. Files: `test-mods/retromod-test-mod-forge/src/main/java/com/retromod/testmod/forge/*` (87 files: FakePlayerCase, ClientConnectionCase, GeometryLoaderCases, BakedModelCases, EventMemberCases, EntityTypeKeys, DensityFunctionCases ...), `test-mods/README.md`, `AGENTS.md` (regression process). Gap: we have no source-built Forge 1.20.1 fixture mod; our 1.20.1 testing depends on user-supplied ATM 9 jars (`test-mods*/` gitignored). Use: build it locally with Forge 1.20.1 tooling (outside our repo, build by a human or trusted step, not by this research), transform with our loader, run as a server/client test; failures become targeted issues. Effort M (setup) then S per case. Licence: MIT, keep in a separate fixture folder with the notice; no need to ship in the jar.

R3. Legacy API bridges that match our current ATM 9 gap symbols. Check each against `probe.json` before porting:
- `shim/common/LegacyAdvancementBridge.java` (327) + `embedded/LegacyCriteria.java` (506) + `LegacyCriterionTriggerBridge.java` (459): `AdvancementHolder$Builder` (7 jars), `ItemPredicate.<init>` (3), `CriterionTrigger.getId`, `InventoryChangeTrigger$TriggerInstance` (2), `DeserializationContext`/`AbstractCriterionTriggerInstance` (46/42 jars in the older list; ours already has `LegacyCriterionInstance`/`LegacyDeserializationContext`, so compare design only).
- `shim/common/Legacy1205GameplayBridge.java` (716): `MobEffect.applyEffectTick` (3 jars), item attribute modifier formatting (`ATTRIBUTE_MODIFIER_FORMAT`, 23 jars), food/maxStackSize components.
- `shim/common/LegacyWidgetOverrideAdapter.java` (345): `Checkbox.<init>` (4), widget overrides.
- `shim/common/LegacyAnimalFoodBridge.java` (91): `Chicken.FOOD_ITEMS` (2 jars).
- `shim/forge/LegacyTeleporterBridge.java` (264): `PortalInfo`, `findRespawnPositionAndUseSpawnBlock` (20 jars for PortalInfo in the older list).
- `shim/common/LegacyDispenserBridge.java` (241): `AbstractProjectileDispenseBehavior` (26 jars older list).
- `shim/common/embedded/LegacyCauldronInteractions.java` (160): `CauldronInteraction.WATER` (24 jars older list).
- `shim/common/LegacyCraftingRecipeBridge.java` (183) + `LegacyRecipeApiBridge.java` (768) + `LegacyIngredientSerializerBridge` (155): `RecipeSerializer.fromJson/toNetwork`, `getRemainingItems`, `Ingredient$Value.serialize`.
- `shim/common/LegacyEnchantmentBridge.java` (542) + `embedded/LegacyEnchantmentSupport.java` (685): code-defined enchantments rebuilt as data pack enchantments (ours: `v1_20_1/migrate/LegacyEnchantment*`; compare).
Gap closed: the named symbols above (Minecraft area, the second largest bucket after libraries). Use: algorithm to re-implement as static helpers in `ORG/v1_20_1/migrate` plus rows in `forward/1.20.1/mc-calls.tsv`/`mc-bridges.tsv`; their approach (ASM-generated helper per mod) is not needed. Effort S to M each (about 14 independent items). Licence: MIT.

R4. NBT-to-components and data migrators for recipes/loot/advancements. Files: `resources/ItemStackDataMigrator.java` (920), `ItemStackComponents.java` (543), `LegacyLootConditionMigrator.java` (319), `LegacySmithingRecipeMigrator.java` (139), `LegacyForgeRecipeMigrator.java` (496), `LegacyDataMigrations.java`, `VanillaTagRenames.java` (232) + `retromod/forge-convention-tags.tsv` (271 rows, Forge path to `c:` path with the first NeoForge version that has it). Gap: our `DataConverter1201` uses regexes (`POTION`, `ENCHANT_ENTRY`) for NBT-in-data and `c-tags.txt`; theirs is a structured converter with ledger/stats. Use: port the structured NBT/component mapping and the Forge-to-`c:` tag table (cross-check against our `TAG_SEGMENTS`/`TAG_PATHS`/`c-tags.txt`). Effort M. Licence: MIT. Most of their loot/spawner/noise code targets 26.x and is not useful.

R5. Event-table generators and the abstract/cancellable event tables. Files: `scripts/NeoForgeEventTable.java` (145), `scripts/ForgeEventTable.java` (297), `src/main/resources/retromod/neoforge-abstract-events.tsv` (section "1.21.1": 135 rows from neoforge-21.1.252 / bus 8.0.5), `forge-event-renames.json` (310 rows), `shim/forge/NeoForgeAbstractEventListeners.java`. Gap: our abstract-event list is a hand-curated set in the event bridge (`ReflectiveEventBus`/`ForgeApiRewrites1201` area) and our `forward/1.20.1/forge-event-renames.tsv` is 312 rows; a generated table would catch missing parents (e.g. `LivingEvent`, `PlayerEvent`, `BlockEvent`, `ExplosionEvent` children). Use: run the generator on NeoForge 21.1.x jars (data only; our scripts, their algorithm), diff the output against ours. Effort S. Licence: MIT (algorithm re-implemented; the 1.21.1 table is facts read from NeoForge jars, safe).

R6. 1.12.2 `forge_marker` blockstate expansion cross-check. File: `resources/LegacyBlockStateMigrator.java` (1319; handles defaults, per-property maps, textures, submodels, `model` name prefix per Forge version, refuses OBJ/custom). Gap: ours is `v1_12_2/Assets1122.java` (1846) plus `StatesBridge1122`; issue class model-loader (15 issues) is mostly `ICustomModelLoader`/state mappers, which Retromod does not do, but its explicit list of forge_marker rules and refusals is a checklist for our rewriter. Effort S (review) to M. Licence: MIT.

R7. Ore dictionary and 1.12 resource tables as a second opinion. Files: `retromod/forge-ore-dictionary.tsv` (177 rows: ore name to `forge:`/`c:` tag), `resources/LegacyOreDictionary.java` (132), `retromod/flattening-texture-renames.tsv` (434, "blocks/" to "block/" basenames), `resources/LegacyLangMigrator.java` (424, `.lang` to JSON), `LegacyIdentifierCase.java` (lowercasing mixed-case ids). Gap: unknown; our equivalents are `OreDictBridge` (1.7.10, reused by `RecipeBridge1122`), `loader/resource/VanillaTextureRenames` (+ `resource/vanilla-texture-renames.tsv` 17 KB) and `LegacyResourceConverter`. Use: diff row sets; add missing rows. Effort S. Licence: MIT (tables are facts from game jars; they were "read with javap and scripts").

R8. Harvest scripts as the way to regenerate our 1.20.1 and 1.12.2 tables. Files: `scripts/harvest-*.py` (18), `scripts/mixin-method-breaks.py`, `scripts/LegacyApiLinkCheck.java`. Gap: our `forward/1.20.1/mc-*.tsv` and `legacy/1.12.2/*.txt` have generators somewhere in `tools/`; theirs derive per-version diffs with descriptor checks ("every target exists on the host"). Use: borrow the validation step (reject a rename whose target is missing from the host jar). Effort M. Licence: MIT.

R9. Fake-host unit tests. Files: `src/test/java/com/retromod/testsupport/fakemc/*` (FakeMinecraftHost, FakeRegistries ...), `src/test/java/com/retromod/shim/forge/*` (176 tests). Gap: we have no Gradle unit-test layer that exercises bridges without a game; a fake host would let us test `ORG/v1_20_1/migrate` helpers offline. Effort L. Licence: MIT. Lower priority, because our value is checked in game.

R10. Docs patterns: `docs/incompatible-mods.md` (named list of mods that cannot be translated and why, e.g. coremod renderers) and `docs/technical.md`. Could feed our compatibility page wording. Effort S. Licence: MIT (prose; rewrite).

### 1.5 Do not take from Retromod

- `srg-to-mojang.tsv` (4.2 MB), `target-srg-1.20.*.tsv` (11 MB), `srg-1.12.2-to-mojang.tsv`: derived from Mojang/MCP mappings with the "no complete unmodified redistribution" notice and an unrecorded MCP licence; we regenerate from our own inputs.
- Fabric/Quilt shims, intermediary tables, `shim/api/fabric/*`, `loaderapi/*`: not our scope.
- 26.x hosts: `Mc26_*`, `Worldgen26_3Migrator`, `NoiseSettingsMigrator`, `ClientItemDefinitionMigrator`, `EquipmentAssetMigrator`, `legacyapi` (they follow NeoForge 26.x; we follow 1.21.1 only for now; revisit when we target 1.21.4+).
- `src/main/java/baubles/api/*` stand-ins (third-party API, no licence header).
- Anything under `docs-dev/sinytra-connector-analysis.md` as code (analysis of someone else's code).
- 1.12.2 runtime pieces (`shim/forge/embedded/Legacy*1122`): weaker than our shim.
- Their jar/CLI/GUI/AOT/cache machinery: we already have loader + cache.

---------------------------------------------------------------------------------------------------

## 2. Translator (Xponer2/translator)

### 2.1 Licence (checked)

- `LICENSE` first line: "Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)", then the standard text of "PolyForm Noncommercial License 1.0.0".
- Every Java file (all 725, checked with `grep -L`) starts with a header: "Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2 / Copyright (c) 2026 Xponer2 ... Licensed under the PolyForm Noncommercial License 1.0.0 ... free to use, change and share for any noncommercial purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it. / Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)".
- README "License": free for noncommercial use; "Keep the credit to the author: the Required Notice line of the license and the license header at the top of every source file stay in every copy and in every changed version"; "No commercial use". Bundled libs: DataFixerUpper MIT (Microsoft), JvmDowngrader LGPL-2.1 (unchanged), ASM BSD-3.
- `CONTRIBUTING.md`: contributions go under the same licence; "No Minecraft code or assets", write bridges from what the method does; "Code taken from other projects needs a compatible license".
- README states the project "is made entirely with AI (Claude)". That does not change the licence text, but it is a legal uncertainty for the author's copyright claim (maintainer decision, see section 5).
- Consequences of PolyForm Noncommercial (section "Distribution License", "Notices"): anyone who gets a copy of any part from us must also get the licence URL/text and the Required Notice lines. A file we copy stays noncommercial. Our jar is distributed on Modrinth and CurseForge, which pay authors; shipping a noncommercial file inside it is at best a grey area (the licence permits distribution "for any permitted purpose", and a permitted purpose is noncommercial only). The author's allowance in issue #269 (keep licence + header, README says those parts are noncommercial) is a written permission; the maintainer should keep a copy of it, ideally as a file in the repo, because the licence text alone does not allow commercial distribution.

### 2.2 Map

Purpose: a Forge 1.12.2 coremod+mod (RetroFuturaGradle build, Java 8 target, JDK 21 to run Gradle) that runs ORIGINAL NeoForge 1.21.1 jars on Minecraft 1.12.2. Direction is the reverse of ours. State (README): experimental, development environment only (MCP names; the normal obfuscated game does not work yet); 90 mods tested (12 work, 65 partly, 4 not); converter v233; 300-mod corpus all convert, 74 need nothing more, 98 miss 1-10 calls. Plan/roadmap: ladder 1.21 -> 1.20 -> ... -> target, mix versions in one game; stage 3 (1.13-1.20 mods lifted to 1.21.1) and stage 5 (Fabric mods, lower targets) are only planned; nothing for 1.20.1 or Fabric exists in the code (I grepped for 1.20/1.19: only two incidental mentions).

Size: 1021 files; 725 Java files (about 108k lines: `src/main/java` 702, `src/carriedStubs` 23 compile-only stubs); about 200 data JSON (c: tags with `ore:` entries, `data/c/tags/item/**` 48 ingots, 35 storage blocks, 20 gems...), 5 tables in `src/main/resources/assets/translator/runtime` (below), lang, a few models/shaders. No `src/test` (testing is the in-game `/translatortest` self test plus an offline "missing API ranking" over 300 mods).

Modules (package `io.github.xponer2.translator`): `runtime` (converter: `JarConverter`, `RemapTable`, `Mappings`, `MethodGuard` 3086 lines, `ModRemapper`, `FacadeRemapper`, `ApiCarry`, `JarJar`, `LateMixins`, `AccessorMixins`), `runtime/redirect` (77 files of static helper bridges: `MiscBridges` 2653, `EntityApiBridges` 1750, `LevelGapBridges` 1666, `DataApiBridges` 1287, `FlattenedItems` 1184), `runtime/mc` (86 facade classes of 1.21 Minecraft types), `runtime/neoforge` (42: event bus, ModContainer, config loader), `component` (data components), `recipe`, `registry`, `menu`, `client` (90 incl. 14 model, 11 entity), `world` (42), `network` (payloads), `capabilities`, `jei`, `selftest` (8), `core` (coremod: `StateIdsTransformer`, `BlockIdLimit` raising the 4096 block limit to 65536, `MixinRepair`, `SafeMixins`).
How translation works: at startup each 1.21.1 jar is converted: names via `neoforge-1.21.1.tsv` (Mojang 1.21.1 names to MCP 1.12.2 names or Translator classes), `R` redirect lines send missing methods to static helpers, `S` supers lines make mod classes extend Translator facades, `X`/`m`/`f` method/field special cases, bytecode downgraded Java 21 to 8 by JvmDowngrader, mixins converted and applied (MixinBooter), missing calls stubbed and logged in `translator/cache/<mod>-*.stubbed.txt`. 1.21 vanilla ids translated to 1.12.2 id+meta, recipes with missing results get a similar item, copper added.
Tables (`src/main/resources/assets/translator/runtime`): `neoforge-1.21.1.tsv` 16821 rows (1696 `C` class, 4912 `M` method, 5424 `F` field, 2452 `f`, 2336 `m`; "generated by tools/runtime/build_tables.py from the source converter data" - that tool is not in the clone), `-redirects.tsv` 865, `-runtime.tsv` 590, `-supers.tsv` 94, `-carried.tsv` 416.

Fabric: none. Roadmap stage 5 only.

### 2.3 Comparison with ours

Their direction (modern code on 1.12.2) is the inverse of ours (1.12.2 code on modern). Where the work overlaps is the 1.12.2 <-> 1.21.1 correspondence of vanilla API and the knowledge of what each side lacks.

| Area | Translator | Ours (1.12.2) | Value to us |
|---|---|---|---|
| Class/method/field correspondence MCP 1.12.2 <-> Mojang 1.21.1 | `neoforge-1.21.1.tsv`: 1696 classes, 4912 methods, 5424 fields (e.g. `Framebuffer`, `ShaderLoader`, `BakedQuad.getVertexData`) | `Synth`/`Vanilla1122.resolveStatic`, shim classes (hand-written), loader `legacy/1.12.2/{fields,methods,hierarchy}.txt` | useful as an oracle to find 1.12.2 symbols our shim lacks (probe: 33 stubs, 5 gap_shim) |
| Missing-API ranking | offline ranking over 300 mods ("stubs left", "missing API parts") | `tools/compat/probe.py`, `Gap1201` | same idea |
| Self test | `/translatortest`: every block placed/saved/loaded/ticked/right-clicked/broken, menus with every slot clicked, items used, models checked; report grouped by "which missing call breaks how many blocks/items" | server self test prints block/item/entity results; no menu/slot clicks | idea only |
| Tag/ore dictionary | `data/c/tags/item/**` JSON with both `minecraft:iron_ingot` and `ore:ingotIron` values | `OreDictBridge`, c-tag names in `RecipeBridge1122` | cross-check of ore-name to `c:` tag pairs |
| Data components, recipes, menus, capabilities, codecs on 1.12.2 | large (`component`, `recipe`, `menu`) | irrelevant (we are on 1.21.1 already) | none |
| Mixins on 1.12.2 | `MixinRepair`, `SafeMixins` | irrelevant | none |
| Block id limit | `BlockIdLimit`, `StateIdsTransformer` (raise 4096 to 65536) | irrelevant (1.21.1 registry) | none |
| 1.20.1 -> 1.21.1 | nothing | whole pipeline | none |

### 2.4 Candidates from Translator

Split by what the maintainer must accept.

(a) Ideas and facts we can re-implement independently (no licence consequence, since facts and method names are not protected expression; do not copy their text or table files):
- T1. Use `neoforge-1.21.1.tsv` as a read-only oracle (not shipped, not copied): extract the set of 1.12.2 MCP classes/methods/fields it maps (the `C`/`M`/`F` right-hand sides) and diff it against `legacy/1.12.2/{fields,methods,hierarchy}.txt` and our shim classes, to produce a list of 1.12.2 vanilla symbols that real 1.21.1 mods needed as counterparts and that our shim lacks or stubs. Then fill the gaps ourselves. Effort M. Licence class: (a), provided the tsv never enters our repo or jar (an internal diff list of names is not the table). If the output list is published, keep it a list of 1.12.2 names only.
- T2. Self-test report design: group failures by "missing call -> number of blocks/items it breaks" and run menu slot-click checks (`selftest/ModChecks.java`, `ScreenTour.java`, `WorldChecks.java`; read for method only). Our server self test (`tools/dev/run-subset.sh`) prints per-block results; add grouping by cause and a slot-click pass for 1.12.2 menus (issue class gui-sync #125, #150, #181, #248). Effort M. Class (a).
- T3. Behaviour notes from their data bridges (`runtime/redirect/*`, `FlattenedItems`): what they had to emulate when 1.12.2 lacks a 1.21 behaviour is the mirror of what we must emulate (recipe results with no counterpart, tile entity NBT, menus, fluid handlers, particles). Use as a checklist of semantic traps while reviewing our `bridge/*1122`. Effort S. Class (a).
- T4. `data/c/tags/item/**` pairs (ore dictionary name to c: tag): read and compare with our ore-name mapping (also with Retromod R7). Effort S. Class (a) if we only diff and add missing pairs by hand; a copy of the JSON files would be (b).
- T5. Their candidate list of "converter-wide" gaps (stack map frames naming missing classes, mixin fields missing) shows the order they tackled things; no code needed.

(b) Code or tables that would have to be copied (file stays PolyForm Noncommercial with its header):
- T6. `neoforge-1.21.1.tsv` (1.27 MB) + `-redirects.tsv` + `-runtime.tsv` + `-supers.tsv` + `-carried.tsv`, inverted, as our 1.12.2 to 1.21.1 unresolved-class table. Would close: some of the 33 stubs/5 gap_shim, plus what `Synth` synthesizes blindly. Effort M (invert, validate against our shim) . Licence: the data file stays noncommercial; it also contains Mojang/MCP names (Mojang notice). Not recommended: (a) T1 gets most of the value.
- T7. `runtime/redirect/*` static helper classes (e.g. `MiscBridges`, `LevelGapBridges`, `DataApiBridges`). They implement 1.21 behaviour on 1.12 code, so their direction does not fit our shim (we need the 1.12 API on 1.21 code). Do not copy.
- T8. `selftest/*` (8 files): in-game harness written against 1.12.2 APIs and MCP names; not portable.

### 2.5 Do not take from Translator

- Any Java file: all PolyForm, all target the 1.12.2 runtime, none is useful on 1.21.1 without a rewrite.
- `neoforge-1.21.1.tsv` and its siblings as files (see T6): noncommercial, Mojang/MCP-derived, and the same value is reachable by T1.
- `core/BlockIdLimit`, `StateIdsTransformer`, `MixinRepair`, `SafeMixins`, `lwjgl/*`, `jei/*` (1.12.2 specifics).
- The carried stubs (`src/carriedStubs`): empty compile-only stand-ins.
- JvmDowngrader integration (LGPL; we already run on Java 21; no use).
- The c: tag JSON as files (T4(b)).

---------------------------------------------------------------------------------------------------

## 3. Combined ranked list: ten best candidates

| # | Candidate | Repo | Scope | Effort | Licence class |
|---|---|---|---|---|---|
| 1 | Mixin repair by host-index proof (R1) | Retromod | 1.20.1 | L | MIT (notice) |
| 2 | Per-symbol bridges for current ATM 9 gaps (R3: AdvancementHolder$Builder, ItemPredicate, applyEffectTick, Checkbox, FOOD_ITEMS, PortalInfo/Teleporter, dispenser, cauldron, recipe serializer APIs, enchantments) | Retromod | 1.20.1 | M per item, about 14 items | MIT (notice) |
| 3 | Forge 1.20.1 in-game test mod as regression fixture (R2) | Retromod | 1.20.1 | M | MIT (notice) |
| 4 | Event-table generators + abstract/cancellable events (R5) | Retromod | 1.20.1 (and follow-on hosts) | S | MIT (notice) |
| 5 | Structured NBT-to-components and `c:` tag migration (R4) | Retromod | 1.20.1 | M | MIT (notice) |
| 6 | Oracle diff of 1.12.2 MCP symbols vs Translator's correspondence table (T1) | Translator | 1.12.2 | M | Translator class (a): ideas/facts, file not copied |
| 7 | forge_marker rule checklist vs `Assets1122` (R6) | Retromod | 1.12.2 | S-M | MIT (notice) |
| 8 | Self-test grouping by cause + menu slot-click pass (T2) | Translator | 1.12.2 (also 1.20.1) | M | Translator class (a): idea |
| 9 | Ore dictionary, texture rename and lang rows cross-check (R7, T4) | Retromod, Translator | 1.12.2 | S | MIT; Translator (a) |
| 10 | Harvest-script validation step ("target must exist on host") (R8) | Retromod | 1.20.1, 1.12.2 | M | MIT (notice) |

Honest summary: Retromod is the useful one (MIT, same Forge-to-NeoForge direction for 1.20.1, much larger mixin and bridge catalogue). Translator offers almost nothing for 1.20.1 and little for 1.12.2 beyond ideas and a mapping oracle, because its code implements the opposite shim.

## 4. Fabric

- Retromod: Fabric and Quilt are first-class targets (1.14.4+ mods on 1.20-26.3 hosts): intermediary-to-Mojang remap, 24 `Fabric_*` version shims, 29 Fabric API shims, access widener repair, REI/EMI/Cloth/CCA shims. Not applicable now (we load no Fabric mods; Sinytra Connector does). Useful later only if we ever read Fabric jars ourselves.
- Translator: no Fabric support; stage 5 on its roadmap.

## 5. Decisions for the maintainer

1. Retromod: confirm that we may port MIT code (R1, R3, R4, R5) and add a "Retromod, Copyright (c) 2026 Bownlux, MIT" section to `THIRD_PARTY_NOTICES.md` (with the licence text). The notice only needs the copyright line and licence once, not per file; adding a one-line "adapted from Retromod <path>" comment per file is advisable.
2. Retromod data tables derived from Mojang or MCP mappings: do not copy verbatim (Mojang notice; MCP 1.12.2 licence "not recorded"); regenerate with our own inputs.
3. Translator: the noncommercial question. Copying any Translator file puts a PolyForm Noncommercial file into a jar sold or supported through Modrinth and CurseForge. Options: (i) take only ideas and facts and rewrite independently (recommended; classes (a) above), nothing to disclose; (ii) copy files per the author's issue #269 terms, keep each header, state in README that those parts are noncommercial, accept that the whole jar is then arguably not freely commercial; (iii) keep any copied file in a separate optional jar or addon so the main jar stays clean. The maintainer should also save the author's permission (issue #269) in the repo, because the licence text alone does not grant what he promised.
4. The Translator README says the project is entirely AI-written. Whether a copyright claim exists over such code is uncertain; this is another reason to prefer option (i).
5. Test fixture (R2): decide whether a Forge 1.20.1 test mod may live in our repo (MIT notice, separate folder) or only in a gitignored local folder like our other test mods.
