# Worker brief: read this INSTEAD of exploring

Legacy Connector runs unmodified legacy Forge mods on NeoForge 1.21.1: the loader (FML service) translates the jar, the runtime holds a
hand-written shim of the old API plus bridges that turn legacy registrations into modern ones. Paths are relative to the repo root;
`ORG` = `src/main/java/org/kylepitcock/legacyconnector`, `LDR` = `loader/src/main/java/org/kylepitcock/legacyconnector/loader`.

## Layout

| Area | Where |
|---|---|
| Loader entry, cache key, report | `LDR/LegacyModLocator.java` (cache + `legacyconnector-report.txt`), `LegacyJar*.java` (version detection), `Provenance.java` |
| Translation (renames, linking) | `LDR/transform/LegacyJarTransformer.java` (**TRANSFORMER_VERSION**), `LegacyRemapper`, `ShimLinker`, `mapping/McpMappings` (SRG/MCP names) |
| Call rewrites, Java 8 to 21 | `transform/JavaCompatRedirects`, `BridgeMethods`, `OptionalStripper`, `SideOnlyStripper`, `BuildHeightRewriter`; runtime helpers `ORG/compat/jdk` |
| Old-version forward layer 1.4.7-1.6.4 | `transform/ForwardRedirects`, `ForwardCalls`, `Living152ClassRemapper`, `mapping/Forward*`; runtime `ORG/v1_6_4/bridge` (`Redirects164`, `Redirects147`, `SpriteSheets147`, `Manager164`) |
| Sheet cutting, resource conversion | `LDR/resource/SheetCutter`, `LegacyResourceConverter`, `VanillaTextureRenames`, `CtmBridge` |
| Coremods | `transform/LegacyCoreModTransformers`, `CoremodPatches`; mod-specific fixes `transform/compat/` |
| API synthesis | `LDR/synth/*` (generated at load), `LDR/shim/*` (shim index, access widener), runtime `ORG/core/synth/Synth.java` |
| 1.20.1 in-place remap | `transform/Forge1201Transformer`, `Mc1201Migration`, `CallRewrites1201`, `SoftLink1201`, `Mc1201Tables`; runtime `ORG/v1_20_1/migrate/*`; probe `LDR/tools/Gap1201` (`./gradlew gap1201`) |
| Beta 1.7.3 | `LDR/BetaJars`, `transform/BetaDeobfuscator`, `mapping/BetaMappings`; runtime `ORG/vb1_7_3` (`PlatformB173`, `BridgeB173`) |
| Shim (legacy API) | `src/main/java/legacyconnector/v1_7_10`, `v1_12_2`, `v1_6_4`, `vb1_7_3`, `v1_20_1` (legacy package names, relocated) |
| Platform + bridges per version | 1.7.10: `ORG/v1_7_10/Platform1710`, `bridge/*`; 1.12.2: `ORG/v1_12_2/Platform1122`, `bridge/*1122`; 1.6.4/1.5.2/1.4.7 ride the 1.7.10 bridges |
| Client side per version | `ORG/v1_7_10/client`, `ORG/v1_12_2/client` (names end in `1122`) |
| Core (all versions) | `ORG/core`: `LegacyLifecycle`, `LegacyConfig`, `pack/` (generated resource+data pack), `worldgen/`, `network/`, `event/ReflectiveEventBus`, `recipe/`, `registry/` |
| Mixins | `ORG/mixin/*` + `src/main/resources/legacyconnector.mixins.json` (compat ones `legacyconnector.compat.mixins.json`); `ORG/mixinplugin/LegacyMixinPlugin` |
| Mod-specific code | only under `compat/` packages (`ORG/compat/*`, `ORG/v1_7_10/compat/*`, `LDR/transform/compat`); checked by `tools/dev/check_mod_specific.py` |
| Addon API | `ORG/api` (no shim types in signatures), `ORG/api/internal` (`CapabilityRules`, `BuiltinAdapters`) |
| Tests | `src/test/java`, `loader/src/test`; fixture mod `testmods/`; in-game self tests listed below |
| Tools | `tools/dev` (test runners, `gate.sh`, this digest), `tools/compat` (probe, corpus), `tools/survey`, `tools/gen_*.py` (generated tables) |
| Docs | `docs/wiki/*.md` (technical detail), `docs/data/mods.json` (compat site), `README.md` stays short |

## Where is X (1.7.10 file; 1.12.2 counterpart in brackets)

- Block: `ORG/v1_7_10/bridge/BlockBridge`, `adapter/LegacyBlockAdapter` [`bridge/BlockBridge1122`, `v1_12_2/adapter/Legacy1122Block` (meta property)]; subtypes `BlockFlattening1710`
- Item: `bridge/ItemBridge`, `adapter/LegacyItemAdapter` [`ItemBridge1122`, `ItemCapabilities1122`]
- Entity + hosts: `bridge/EntityBridge`, `entity/LegacyEntityHost|LegacyLivingHost|LegacyMobHost` [`bridge/EntityBridge1122`, `entity/EntityHost1122`, `Entities1122`]
- Tile entity: `bridge/TileEntityBridge`, `adapter/LegacyBlockEntity` [`TileEntityBridge1122`, `TileViews1122`]
- GUI/menu/screens: `bridge/GuiBridge`, `gui/LegacyMenu`, `client/LegacyContainerScreen`, `LegacyScreen` [`GuiBridge1122`, `gui/LegacyMenu1122`, `client/LegacyContainerScreen1122`]
- Render context (GL emulation): `client/RenderContext`, `LegacyGlState`, `DisplayLists`, `LegacyVertices` (Tessellator)
- ISBRH / TESR / item renderers: `client/IsbrhBlockModel`, `IsbrhQuads`, `LegacyBlockEntityRenderer`, `LegacyItemRenderer`, `VanillaBlockRenderers` [`client/LegacyBlockEntityRenderer1122`, `Models1122`, `CustomModels1122`, `VariantModel1122`]
- Models/assets generation: `ORG/v1_7_10/Assets1710`, `core/pack/GeneratedPack` [`v1_12_2/Assets1122`, `ObjAssets1122`, `client/ActualStateModels1122`]; Beta `vb1_7_3/AssetsB173`
- Recipes: `bridge/RecipeBridge`, `adapter/LegacyCraftingRecipe`, `OreDictBridge` [`RecipeBridge1122`, `JsonRecipes1122`, `Crafting1122`]
- Loot: `bridge/ChestLootBridge1710`, `LegacyFishingLoot` [`Loot1122`, `LootTables1122`]
- Worldgen/biomes/dimensions: `bridge/WorldGenBridge`, `LegacyBiomes`, `DimensionBridge`, `dimension/LegacyDimensions`, `core/worldgen/*` [`WorldGenBridge1122`, `BiomeBridge1122`, `v1_12_2/dimension`]
- Fluids: `bridge/FluidBridge` [`FluidBridge1122`]; energy: `EnergyBridge`, `ForeignEnergy1710` [`ModernEnergy1122`, `EnergyBridge1122`]
- Capabilities for legacy containers: `ORG/api/internal/CapabilityRules`, `BuiltinAdapters`, `bridge/ModernCapabilityTile`
- Network/packets: `bridge/NetworkBridge`, `FmlChannelBridge`, `core/network/LegacyPayload` [`NetworkBridge1122`]
- Events: `bridge/EventBridge1710`, `ForgeEvents1710`, `core/event/ReflectiveEventBus` [`EventBridge1122`, `entity/ForgeEvents1122`]
- Commands: `bridge/CommandBridge1710`, `DevCommands1710` (`/legacyconnector ...`) [`CommandBridge1122`]
- Config: `core/LegacyConfig` (keys and defaults); config docs `docs/wiki/Configuration.md`
- Self tests (run in the server test): `core/SilentFailureChecks`, `v1_7_10/entity/{Block,Entity,Dimension,WorldGen,Village}SelfTest`, `v1_12_2/PlacementSelfTest1122`, `v1_6_4/bridge/SelfTest164`, `client/ClientSelfTest`
- Client control session: `v1_7_10/client/RemoteControl` (files in `run-screenshots/control/in/*.cmd`), driven by `tools/dev/js.sh`, `quick-shots.sh`, `session.sh`

## Conventions

- `mcc$` prefix marks members the layer adds (virtual fields); shim code must read legacy-only fields through `mcc$get$<field>()` / `mcc$set$<field>()`
  accessors, otherwise `generateShimIndex` fails with "phantom field access". Moving files: compare source count with classes under `build/classes`.
- A view of an EXISTING native object (villager, horse, cloud, player) must not write legacy defaults into it: guard constructor defaults with
  `mcc$existingNativeView()`.
- Native extension points only (biome modifiers, loot tables, jigsaw pools, data packs, capabilities); no post-generation decorators or result-overriding mixins.
- Fix generically in the shim/bridge/synth first. A mod name outside a `compat/` package fails `check_mod_specific.py`.
- Never use `BlockTags` while mods load; chunk-generation code reads blocks through the generation region; name lookups O(1); per-tick/per-chunk logging throttled.
- Changing loader output (transform, remap, rewrites, sheet cutting): bump `TRANSFORMER_VERSION` in `LegacyJarTransformer.java`; delete `run-*/.legacyconnector/cache` before any test.
- `README.md` stays short: detail goes to the matching `docs/wiki` page; update `.github/description/description.md` with README changes.

## Verification (offline only; never start the game, a server or a client)

- `tools/dev/gate.sh` (compile --rerun, generateShimIndex, check; one line per step, errors only). Use it, not raw Gradle. Gradle only with `-q`.
- `./gradlew checkLegacyMod -q -PlegacyJar=a.jar,b.jar`: offline link + bytecode + resource check of local jars.
- `tools/dev/verify.sh` if present (planned verification suites); `python -m unittest` in `tools/dev` for the Python tools.
- `tools/dev/verify-client.sh [scenario ...]` runs scripted checks in a real client (scenarios in `tools/dev/verify/scenarios/`; opens a game window, one client at a time, only the lead-assigned worker may start it; see wiki Development, "Client scenarios").
- JDK 21: `export JAVA_HOME="C:/Program Files/Java/jdk-21"` (default JDK fails with class file major 70).
- Start every issue with `python tools/dev/issue_digest.py <n>` (about 150 lines instead of the raw logs; files land in a temp dir); grep the downloaded logs only for what the digest names.
- Local jars (gitignored, never commit): `test-mods`, `test-mods2`, `test-mods-1122`, `test-mods-164`, `test-mods-152`, `test-mods-147`, `test-mods-1201`, `test-mods-b173`, `run-subtest/mods/<version>`.
- Commit on the worktree branch, one per defect; message ends with
  `Co-Authored-By: Claude <noreply@anthropic.com>` and `Claude-Session: https://claude.ai/code/session_013WYWcaMHZ3xiptrnBVpgFk`. No push, no GitHub comments, no mod jars.
- Final report: branch, commits, per issue root cause, fix, confidence, what is left, and how the lead verifies it in a game client.

## Costly mistakes to avoid

1. Printing whole logs or crash reports: use the digest, then `grep -n -m5` / `sed -n` windows.
2. Running Gradle without `-q` (hundreds of warning lines): use `gate.sh`.
3. Re-reading CLAUDE.md sections or wiki pages unrelated to the task; this brief and the matching `docs/wiki` page are enough.
4. Starting the game, a server or a client (the lead verifies in game).
5. Broad greps over `src/` for names this map already gives; open the named class.
6. Fixing the mod in the title instead of the defect class (see `docs/wiki/Work-Plan.md`): look for every mod the same defect hits.
7. Forgetting `TRANSFORMER_VERSION` or the stale translation cache after a loader change.
8. Writing legacy defaults into native objects, or reading virtual fields directly in shim code.
9. Naming a mod outside `compat/` (the gate fails late); skipping the unit test of a loader rewrite.
10. Piping attachments into the repo: download outside it (the digest does).
