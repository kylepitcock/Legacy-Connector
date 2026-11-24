# Host 1.21.4 estimate (NeoForge 21.4 and later): measured symbol diff

The layer targets Minecraft 1.21.1 / NeoForge 21.1.258. [Host version upgrades](Host-Version-Upgrades.md) lists, from knowledge of the migration
primers, what changed in later hosts. This page is the measured counterpart (2026-11-24, offline, no game run): every Minecraft, NeoForge and
Mojang-library symbol that the compiled runtime and loader reference was resolved against the real NeoForge 21.4.161 jar (Minecraft 1.21.4) and,
for the trend, 21.5.101 (1.21.5), 21.8.57 (1.21.8) and 21.11.48 (1.21.11).

## Method

- Scanned: all 8986 classes of `build/classes` (runtime: hand-written shims under `legacyconnector/**`, bridges, core, mixins, API) and the loader
  (`loader/build/classes`). A small constant-pool parser (`tools/dev/host_symbol_diff.py`) collects every field/method/class reference whose owner
  is in `net.minecraft`, `net.neoforged`, `com.mojang` or `cpw.mods`, plus the host types in declared descriptors, superclasses and interfaces.
- Targets: the compile classpath of a scratch project with the `net.neoforged.moddev` plugin per host version (`neoforge-<v>.jar` is the
  Mojang-named, NeoForge-patched Minecraft; FML, Brigadier, DataFixerUpper and the other libraries come from the same classpath).
  Reproduce: build the classpath file (one jar per line) for 21.1.258 and for the target, then
  `python tools/dev/host_symbol_diff.py --classes build/classes/java/main --classes loader/build/classes/java/main --old cp-21.1.txt --new cp-21.4.txt --out diff.tsv`.
- Classification of each reference after resolving it through the class hierarchy: `unchanged`; `signature_changed` (same name, other descriptor);
  `removed`; `renamed?` (a single new member with the same descriptor); `class_moved_member_ok` (class changed package, member intact);
  `class_removed`; `type_removed` (a descriptor type is gone). Also checked: overrides of host methods (`override_*`), abstract methods the
  host added that our concrete subclasses do not implement (`new_abstract`), and mixin targets/selectors (class, `method`, `@At target`,
  `@Shadow`, accessors).
- Output committed in `tools/dev/agent-tasks/`: `host-1.21.4-symbol-diff.tsv` and `host-1.21.11-symbol-diff.tsv` (every non-unchanged row:
  kind, owner, name, descriptor, status, detail, using classes, subsystem) and `host-1.21.4-classes-affected.tsv` (our classes with the count of
  broken references).
- Limits. A symbol that is `unchanged` can still behave differently (1.21.2 changed hurt/tick/spawn semantics, recipe syncing, tooltip and
  rendering order); string-based access (reflection, the `Mc1201Tables` rewrite tables, Synth) is only covered where a host class name appears as a
  constant (73 such names no longer exist in 1.21.4, almost all also seen as real references); data formats (generated item models, recipes, blockstates,
  shader JSON) are not symbols and are listed separately below. Rename guesses (`renamed?`) are heuristics.

## Headline numbers

Member references (distinct field/method references to host classes that resolve in 1.21.1: 10 110):

| Host | Broken member refs | Of which mechanical (class moved, member intact, unique rename) | Signature changed | Removed | Removed or moved classes | Overrides broken (of 1738) | New abstract methods unimplemented | Mixin selectors broken (of 298) |
|---|---|---|---|---|---|---|---|---|
| 1.21.4 | 684 (6.8%) | 35 | 293 | 356 | 63 removed, 8 moved (of 2243) | 165 | 38 | 25, plus 1 target class gone |
| 1.21.5 | 1171 (11.6%) | 131 | 445 | 595 | 105 removed, 28 moved | 251 | 48 | 27 |
| 1.21.8 | 1319 (13.0%) | 138 | 522 | 659 | 114 removed, 29 moved | 318 | 52 | 32 |
| 1.21.11 | 2298 (22.7%) | 522 | 919 | 857 | 159 removed, 148 moved | 454 | 77 | 53, plus 9 target classes gone |

Our classes with at least one broken reference at 1.21.4: 657 of 8986 (7.3%): shim 1.12.2 154, bridges b1.7.3 96, bridges 1.12.2 89, bridges 1.7.10 88,
bridges 1.20.1 75, shim 1.7.10 75, mixins 24, core 17, shim 1.20.1 16, compat 10, rest below 10. The loader (FML service) has one broken class only from 1.21.5
and none at 1.21.4; the 77 access transformer lines break in 4 places at 1.21.4 (`Ingredient.<init>(Stream)`, `Blocks.leaves/log`, `ModelBakerImpl.<init>`) and in 20 at
1.21.5 (all of `GlStateManager` state classes gone).

The first step (1.21.4) is therefore small by symbol count; most of the 1.21.4 breakage comes from three 1.21.2 refactors (item/armor/tool construction,
recipes, entity render state) and the `InteractionResult` merge. 1.21.5 adds the render-pipeline break (`GlStateManager` and the GL state emulation
dependencies, 131 broken blaze3d refs against 21 at 1.21.4) and the NBT getters returning `Optional` (35 refs). 1.21.11 is the expensive one: the Mojang renames
(`ResourceLocation` to `Identifier`, plus the entity/stat/advancement moves) turn 148 classes into moves.

## By subsystem (broken member refs)

| Subsystem | Member refs | 1.21.4 | 1.21.5 | 1.21.8 | 1.21.11 |
|---|---|---|---|---|---|
| entities | 1432 | 97 | 187 | 225 | 556 |
| items | 571 | 85 | 105 | 106 | 107 |
| rendering: renderer core (RenderType, shaders, buffers) | 181 | 43 | 57 | 64 | 116 |
| recipes | 94 | 39 | 39 | 39 | 39 |
| blocks | 487 | 35 | 43 | 43 | 48 |
| client core | 594 | 34 | 39 | 48 | 110 |
| neoforge client extensions | 348 | 34 | 44 | 60 | 113 |
| world misc (InteractionResult, FoodProperties...) | 197 | 33 | 35 | 39 | 39 |
| world/level | 505 | 33 | 47 | 49 | 88 |
| GUI: screens/graphics | 343 | 31 | 31 | 46 | 86 |
| rendering: blaze3d (GL/vertex) | 283 | 21 | 131 | 140 | 147 |
| rendering: models/baking | 76 | 21 | 41 | 41 | 58 |
| server | 579 | 21 | 36 | 38 | 103 |
| neoforge events | 590 | 18 | 44 | 44 | 47 |
| level storage/loot | 147 | 17 | 20 | 22 | 28 |
| registries | 206 | 14 | 15 | 15 | 27 |
| networking | 392 | 14 | 24 | 32 | 46 |
| core (BlockPos, Direction) | 211 | 13 | 16 | 16 | 27 |
| rendering: entity renderers | 30 | 11 | 12 | 13 | 19 |
| stats | 84 | 9 | 9 | 10 | 60 |
| rendering: block rendering/models | 46 | 8 | 26 | 26 | 28 |
| entities: players | 253 | 8 | 13 | 19 | 41 |
| worldgen/chunks | 99 | 7 | 8 | 8 | 12 |
| util | 74 | 6 | 11 | 11 | 14 |
| block entities | 170 | 4 | 7 | 15 | 24 |
| sounds | 261 | 4 | 12 | 14 | 15 |
| data components | 109 | 3 | 13 | 13 | 16 |
| damage/combat | 102 | 3 | 14 | 14 | 16 |
| neoforge common | 138 | 3 | 20 | 20 | 22 |
| NBT | 110 | 0 | 35 | 35 | 36 |
| advancements | 146 | 1 | 13 | 13 | 83 |
| capabilities (+ fluids) | 104 | 0 | 0 | 9 | 18 |
| FML / mod loading | 103 | 0 | 1 | 2 | 6 |
| all other (menus, AI, commands, worldgen, particles, codecs, datagen, tags, fluids, math, ...) | 1045 | 14 | 23 | 30 | 103 |
| **Total** | **10 110** | **684** | **1171** | **1319** | **2298** |

Stable across every host tested: codecs/serialization (0), datagen (0), fluids/materials (0), math/physics (0), tags (0 until 1.21.11), the FML service
(0 until 1.21.5, 6 at 1.21.11). Capabilities stay intact until the 1.21.9 transfer rewrite starts to show (18 at 1.21.11), so the default
capability exposure of legacy containers is not a 1.21.4 problem.

## Worst examples at 1.21.4 (by number of our classes affected)

- Entities: `EntityType.create(Level)` now takes `(CompoundTag, Level, ...)`: 41 classes; `EntityType.loadEntityRecursive` 14; `Entity.hurt(DamageSource,F)Z` is `hurtServer`-style (void) 7;
  `Entity.changeDimension(DimensionTransition)` gone 6; `EntityType.Builder.build(String)` takes a `ResourceKey` 5; `Mob.finalizeSpawn` 4; `MobSpawnType` removed (29 classes use it, replaced by `EntitySpawnReason`).
- Items: `Item.use` returns `InteractionResult` (10); `ArmorItem.getMaterial/getType` gone (9), `ArmorMaterial`, `ArmorMaterials`, `ArmorTrim` moved to `world.item.equipment`; `Tier`/`Tiers`/`TieredItem` removed (12 classes plus 8 overrides of `Tier`),
  `UseAnim`, `ArmorItem$Type`, `PotionContents.<init>`, `ItemStack.getBurnTime`.
- Recipes: `Ingredient.getItems()` removed (9 + one override), `RecipeHolder.id()` returns a `ResourceKey` (9), `RecipeHolder.<init>`, `RecipeManager.byKey/getAllRecipesFor/replaceRecipes`, `CraftingRecipe.getRemainingItems(CraftingInput)`,
  `SimpleCraftingRecipeSerializer`, `Ingredient$Value/TagValue`, `ICustomIngredient.getItems` to `items()` (new abstract).
- World: `InteractionResult` is now a sealed interface, so `InteractionResult.PASS/FAIL/SUCCESS/CONSUME` change type (22/14/12/7 classes); `InteractionResultHolder` and `ItemInteractionResult` removed (24 classes); `DimensionTransition` removed (6);
  `BlockState.getLightBlock()` / `isSolidRender()` lose their arguments, `Block.getCloneItemStack`, `neighborChanged` (override), `LootContextParams` become `ContextKey` (loot subsystem), `DirectionProperty` removed (20 string uses).
- Rendering/client: `BlockEntityWithoutLevelRenderer` removed (8), `ItemOverrides`/`BakedModel.getOverrides` removed, `ModelResourceLocation.inventory/standalone` removed (13), `TextureManager.getTexture(RL, Texture)` (8),
  shader getters on `GameRenderer` and `ShaderInstance` removed, `ItemProperties`/`ItemColors` removed (item models are data driven), `RegisterColorHandlersEvent$Item`, `BakedModelWrapper`, `IGeometryLoader`/`IUnbakedGeometry` removed (NeoForge custom geometry API replaced),
  `MovementInputUpdateEvent.getInput` returns `ClientInput`, `RenderPlayerEvent` accessors, `ToastComponent` and `GhostRecipe` removed.
- Entity render state (new abstract methods our subclasses do not implement): `EntityRenderer.createRenderState()`, `RenderLayer.render(...)` (state-based), `Toast.getWantedVisibility()`, `LevelHeightAccessor.getMinY()`, `PackResources.getMetadataSection(MetadataSectionType)`,
  `ICustomIngredient.items()`; the entity renderer family accounts for 21 of the 165 broken overrides.
- Mixins: 25 selectors broken at 1.21.4 (of 298), clustered in entity renderers (`PlayerRenderer.setupRotations`, `HumanoidArmorLayer.renderArmorPiece`, `CustomHeadLayer.render`, `EntityRenderDispatcher.renderShadow`),
  `EntityType.create/spawn` wrap targets, `ChorusFruitItem` (target class removed), `Entity.tryCheckInsideBlocks`, `EnchantmentHelper` and `NaturalSpawner.spawnForChunk` in the server.
  The `MixinSelfCheckHook` makes these loud failures; the full list is in the TSV (`kind` starting with `mixin_`).

## Where the work is, by our code area (1.21.4, classes with at least one broken reference)

| Area | Classes | Broken refs | Comment |
|---|---|---|---|
| bridges 1.20.1 (`v1_20_1/migrate`) | 75 | 385 | Densest per class: `RoundCompat`, `MiscCompat`, `LegacyArmorMaterials`, `ArmorCompat`, `BridgeHelpers`, `Round9Compat` map Forge 47 names onto 1.21.1; the right-hand sides need re-pointing, and the loader's `Mc1201Tables` / `CallRewrites1201` strings with them |
| shim 1.12.2 | 154 | 365 | Mostly wrappers over modern objects (`ForgeEventFactory`, `Entity`/`World` views) |
| bridges 1.12.2 | 89 | 334 | `ClientEvents1122`, `MobHost1122`, `Legacy1122Block`, `Vanilla1122` |
| bridges Beta 1.7.3 | 96 | 305 | Many tiny self tests and per-mod fixtures (`compat` ones belong here too); most are `MobSpawnType`/`InteractionResult` uses |
| bridges 1.7.10 | 88 | 303 | `LegacyBlockAdapter`, `EventBridge1710`, `LegacyMobHost`, `LegacyItemAdapter` |
| shim 1.7.10 | 75 | 197 | `World`, `Entity`, `EntityLivingBase` |
| core, mixins, compat, API | 17 / 24 / 10 / 0 | 38 / 35 / 25 / 0 | |
| shim 1.20.1, 1.6.4, b1.7.3 | 16 / 3 / 7 | 48 / 6 / 11 | |

The breakage is concentrated: the same few host symbols (`MobSpawnType`, `InteractionResult`, `ArmorItem`/`Tier`, `EntityType.create`, `Ingredient.getItems`)
account for a large share of the 657 classes, so fixing them behind a small number of adapters (a `HostCompat` facade or a shared helper per symbol) repairs most classes at once.

## What the symbol diff does not see (work that comes on top)

1. Item and block construction: `Item.Properties`/`BlockBehaviour.Properties` need `setId(ResourceKey)` before construction (1.21.2); every bridge that constructs legacy blocks and items has to pass it.
2. Generated resource formats (`core/pack/GeneratedPack`, `Assets1710`, `Assets1122`): item model definitions (`assets/<ns>/items/*.json`) replace the item model resolution by `BakedModel`; recipes JSON changed (ingredient
   syntax, `recipe` display data); 1.21.5 changes loot, enchantment and model JSON; GLSL core shaders and `post_effect` JSON change in 1.21.2 and 1.21.6.
3. The GL emulation (`RenderContext`, `LegacyGlState`, `DisplayLists`, `LegacyVertices`, `IsbrhBlockModel`): symbol-wise only 21 refs at 1.21.4, but behavior is bound to `RenderType`, `BufferBuilder` and GL state; 1.21.5 removes the state manager it patches
   (`GlStateManager` AT lines) and 1.21.6 introduces `RenderPipeline`. This is the single piece that cannot be re-pointed mechanically.
4. Recipe syncing: the client no longer receives all recipes (1.21.2); JEI/NEI bridging and the legacy recipe book/crafting GUI paths need display data (`RecipeDisplay`).
5. Optional bridge dependencies (Curios, JEI, Jade, TerraBlender, Rhino) track the host too and were not scanned; each needs a version that targets 1.21.4+.
6. FML: the loader compiles against FML 4.0.45; NeoForge 21.4 ships FML 6.0.18. No loader reference broke, but the service-loading path was not exercised.

## Effort estimate

Assumes the agent pace of this project (an automated worker fixing a defect class per round, gates and the 1.7.10/1.12.2/1.20.1 suites as acceptance); numbers are working time for one lead plus workers, not elapsed days,
and only meaningful as relative sizes.

| Step | Scope | Estimate |
|---|---|---|
| 1. Compile-clean on 1.21.4 | 657 classes, 684 member refs, 71 class removals/moves, 165 overrides, 38 new abstract methods; adapters for `MobSpawnType`, `InteractionResult`, `Tier`/`ArmorMaterial`, `EntityType.create`, `Ingredient`; AT and mixin selector fixes | 4 to 6 days |
| 2. Items, armor, tools, `setId`, item model definitions | bridges 1.7.10/1.12.2/1.20.1/Beta item paths plus pack generation | 3 to 5 days |
| 3. Recipes (recipe holder keys, `Ingredient`, `CraftingInput`, recipe sync, JEI/NEI bridge) | 39 refs but design work | 3 to 4 days |
| 4. Entity render state and renderers (`EntityRenderState`, layers, `RenderContext` replay) | 11 refs, 21 overrides, 5 mixins, most of the risk | 5 to 8 days |
| 5. Entities and spawning (`hurtServer`, `EntityType.create/spawn`, `MobSpawnType`, dimension change) | 97 refs, mixins on `EntityType` | 3 to 5 days |
| 6. Verification: server self tests for every version, client scenarios, ATM-style packs, fix long tail | all suites, `tools/dev/verify.sh` baselines re-recorded | 5 to 10 days |
| **Total to a 1.21.4 build that passes today's suites** | | about 3 to 5 weeks of work |
| Additional for 1.21.5 | NBT getters (`Optional`), GL state/render-pipeline groundwork (131 blaze3d refs), loot/enchantment JSON, data component and codec changes (~490 more refs) | 1.5 to 2.5 weeks |
| Additional for 1.21.8 | small delta over 1.21.5 (148 more refs, mostly client/GUI, block entities) | 3 to 5 days |
| Additional for 1.21.11 | ~980 more refs, 148 class moves (`ResourceLocation` to `Identifier` is mechanical but touches the whole code base), 9 mixin target classes, 21.9+ transfer API for capabilities | 3 to 4 weeks |

Recommendation: do not bump the single build to 1.21.4 in place. The mechanical part (renames, moves) is cheap, but a separate host-adapter layer (one class per changed concept, selected at build time) lets 1.21.1 builds
keep shipping while 1.21.4 is brought up; the symbol TSV lists the adapter surface (rows sorted by `users` show which few symbols touch the most classes). 1.21.4 is the sensible first target (largest user base after 1.21.1, no render pipeline change yet); aim for 1.21.5+ only once the
GL emulation has a decision (port to `RenderPipeline` or keep a compatibility renderer).
