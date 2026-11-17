# Mixin audit (performance mod compatibility)

Part of [Development](Development.md). Written 2026-11-16 after a boot crash in every pack with FerriteCore: the layer's
`StateHolderPropertyEqualsMixin` used a `@Redirect` on `Reference2ObjectArrayMap.get(...)` inside `StateHolder` with a handler parameter typed
`Reference2ObjectArrayMap`; FerriteCore retypes the `values` field to `Reference2ObjectMap`, so the handler failed JVM verification at bootstrap.

## Method (offline, nothing was run in a game)

1. Every mixin of the layer (`org/kylepitcock/legacyconnector/mixin/**`, 150 classes, two configs: `legacyconnector.mixins.json` with
   `defaultRequire: 1`, `legacyconnector.compat.mixins.json` with `defaultRequire: 0`) was read for its `@Mixin` target, every injector
   (186 in total) and every `@Shadow`/`@Accessor`/`@Invoker`.
2. The jars in `run-perfmods/` (Sodium, Sodium Extra, Iris, Iris Veil, Colorwheel, EuphoriaPatcher, FerriteCore, ModernFix, C2ME with all its
   bundled sub jars, ImmediatelyFast, EntityCulling, MoreCulling, BadOptimizations, Clumps, Bad Horse Fix) were opened, their mixin configs read, and the class files parsed
   for `@Mixin` targets and every injector's `method`/`at` (3,400 lines of annotations). Lithium has no jar in the set: it is covered by reasoning
   (its javadoc references in `LevelChunkStaticTickerMixin`) and listed as not verified.
3. Each of our (class, method) pairs was joined against that table; every hit and every shared class was then read by hand.

The mixin library is MixinExtras (`WrapOperation`, `WrapMethod`, `ModifyExpressionValue`, `@Local` are already used in 31 places (24 `WrapOperation`, 5 `ModifyExpressionValue`, 2 `WrapMethod`)), so the
type-agnostic style is available everywhere.

## Classes

* (a) handler or `@Shadow` names a type or member a perf mod retypes, replaces or removes: **0 left**. `StateHolderPropertyEqualsMixin` was the only one and
  is already rewritten (inject at the not-found paths, public accessors only). All remaining `@Shadow`s are on plain vanilla fields of unchanged
  type (`Camera.eyeHeight`, `ClientLanguage.storage` (typed `Map`; ModernFix swaps in a lazy `Map`, ModernFix's own accessor reads the same field as `Map`),
  `Inventory.player`, `Player.foodData`, `ServerChunkCache.level`, `ChunkStep.Builder.status/blockStateWriteRadius`, `SynchedEntityData.Builder.itemsById`,
  `JigsawStructure.startPool`, `RecipeManager.registries`, `GameRenderer.minecraft`, ...). No perf mod in the set targets the classes
  `ChunkStep.Builder`, `SynchedEntityData(.Builder)`, `ChunkMap$TrackedEntity`, `Inventory`, `NearestAttackableTargetGoal`, `JigsawStructure`.
* (b) `@Redirect`/`@Overwrite`/`@ModifyArg`/`@ModifyConstant` on a call a perf mod also changes: **4 `@Redirect`s found** (the only exclusive injectors we have;
  `@Overwrite` is not used at all). None collided with a perf mod in the set, but they were the one pattern that can lose against another mod,
  so all four were converted to `@WrapOperation` with identical behaviour: `BlockLootSupplierMixin`, `ItemStackOversizedResultMixin`,
  `compat/ArchitecturyEventImplMixin` (a `NEW`), `compat/HbmTagStackDisplayMixin`. The 13 `@ModifyArg`, 1 `@ModifyArgs`, 12 `@ModifyConstant`,
  3 `@ModifyVariable` all sit on vanilla call sites nobody in the set touches (checked against the table); they stack with other mods' wraps and
  are left as they are.
* (c) `require` 0 hiding a silently unapplied injection: **97 mixin classes**, listed in the table. 65 of them are the coremod hooks (`*HookMixin`,
  `*CoremodSites*`): `require = 0` is deliberate there (a hook for a coremod's patch site; if a NeoForge or Minecraft update moves the target the
  coremod effect silently disappears, vanilla behaviour stays). 32 are feature mixins whose absence changes behaviour without an error
  (`StateHolderPropertyEquals`, `Varint21*` (payload size), `RecipeManagerContext`, `TreeGrowerLegacy`, the registry mixins, ...). Done: since 2026-11-16 a startup self check reports it (see "Startup self check" below); the 65/32 split of this table is superseded by the
  `@CoremodHook` list below.
* (d) fine: **49** (strict `require = 1` injectors on vanilla/Beta/Fisk-style targets and member-only mixins). 26 injectors keep the main
  config's default `require = 1`: they fail loudly if a target moves, which is wanted for core features; none of their targets is touched by a perf mod
  in the set at the injection point (see the overlap column).

Counts: (a) 0, (b) 4 (converted), (c) 97 (listed), (d) 49 = 150 mixin classes, 186 injectors (122 `@Inject`, 28 `@WrapOperation` after the change, 13 `@ModifyArg`,
12 `@ModifyConstant`, 5 `@ModifyExpressionValue`, 3 `@ModifyVariable`, 2 `@WrapMethod`, 1 `@ModifyArgs`).

## Overlaps worth knowing (all coexist; verified by reading both injectors)

| Perf mod | Where it meets us | Why it is fine |
|---|---|---|
| FerriteCore | `StateHolder` (`values`, `neighbours` retyped; Redirect on `Table.get` in `setValue`/`trySetValue`; Overwrite `populateNeighbours`) | our injects sit at `NEW IllegalArgumentException`/`RETURN` and use `getProperties()`/`getValue`/`setValue` only |
| FerriteCore, ModernFix | `BlockBehaviour.BlockStateBase` (cache classes, `initCache`) | ours adds one `@Unique` int field |
| ModernFix | `ClientLanguage` (lazy map via `ModifyArg` on the constructor) | our `storage` shadow is a `Map`; only `containsKey` is called |
| ModernFix | `MappedRegistry` (`replaceStorage` at constructor RETURN) | no shadows in our two mixins |
| Iris | `EntityRenderDispatcher.render/renderShadow/renderFlame`, `LightTexture.updateLightTexture`, `ClientLanguage.getOrDefault/has`, `ClientLevelData.getHorizonHeight`, `HumanoidArmorLayer.renderArmorPiece` | HEAD/RETURN injects or different call sites; MixinExtras wraps stack |
| Sodium / Sodium Extra | `OptionsScreen` button lambda, `ClientLevel.getSkyColor` (redirect inside), `LivingEntityRenderer`, `Camera` | disjoint injection points |
| C2ME | `ServerChunkCache.tickChunks` (wrap/redirect/inject at `getAllEntities`, `getTickingChunk`, `tickChunk`), `ChunkMap`, `LevelChunk` ctor/`isTicking`/`postProcessGeneration` | ours wrap `tickChunks` as a whole and the `spawnForChunk` call |
| BadOptimizations, ImmediatelyFast, EntityCulling, MoreCulling | `ClientLevel.getSkyColor`, `GameRenderer`, `BlockModel$Deserializer.deserialize` | Inject next to our Inject/WrapMethod |

Not verifiable here: Lithium (entity/collision/block entity ticking lists, POI) has no jar in `run-perfmods/`. Our hooks on `Entity.move/collide`,
`CollisionGetter.getBlockCollisions`, `LevelChunk.updateBlockEntityTicker` and `BlockStateBase.getCollisionShape` are HEAD/RETURN injects or `WrapOperation`s,
the style Lithium coexists with; re-run `tools/dev/agent-tasks` probes when a Lithium jar is added to the set.

## Rules for new mixins

* Never name a concrete collection or internal member type in a handler signature or `@Shadow` when the field is one a perf mod retypes
  (`StateHolder.values/neighbours`, `BlockStateBase` caches, `ClientLanguage.storage` contents, registry maps, chunk render classes): read through public accessors.
* No `@Redirect`: use `@WrapOperation` (`@WrapWithCondition`, `@ModifyExpressionValue`). Prefer `INVOKE` targets with an `ordinal` only when needed.
* Give an injector that carries an optional feature `require = 0` and a line in the Javadoc saying what silently stops working; keep core features at the default.
* Offline check: `tools/dev/gate.sh` compiles, and `tools/dev/agent-tasks` probes load every target. To compare against a new perf mod, extract its mixin config
  and diff the `method`/`at` pairs against the table below (the scan used for this page parsed the class files of every `*.mixins.json`).

## Startup self check (which injectors were applied)

`mixinplugin/MixinSelfCheck` (recorded from `LegacyMixinPlugin`'s Mixin extension, both configs): after all mixins were applied to a target class, the
class is scanned once for the merged handler methods (`prefix$ABC123$[source$]name`) and the call sites that reference each; an injector that found no
injection point leaves its handler in the class with no caller. After the game has loaded the targets (client title screen and server start;
`MixinSelfCheckHook`) the layer logs one line per trigger,

    Legacy Connector mixins: 151 injectors applied, 0 not applied (plus 2 optional coremod hook injectors not applied, by design), 12 not checked yet (target class not loaded)

and one WARN per unapplied FEATURE injector (mixin, handler, injector kind, target methods). Mixins marked `@CoremodHook` (below) are the deliberately optional
coremod hook replays: only counted. "Not checked yet" are injectors whose target class had not been transformed at that moment. Limit: two mixins on one
target with the same handler name are matched together (the check cannot tell them apart; it only flags when both are dead). The headless server self test
prints the line; `tools/dev/verify.sh` records `mixins_applied` and `mixins_not_applied` (a non-zero not-applied count in a suite whose baseline is 0
is a regression, so a perf mod removing an injection point fails the suite).

### Deliberate coremod hooks (`@CoremodHook`)

<!-- coremod-hooks:begin (generated from @CoremodHook; run the test with MIXIN_DOCS_UPDATE=1 to rewrite) -->

AbstractClientPlayerFovHookMixin, AnvilMenuCostHookMixin, AnvilScreenCostHookMixin, ArmorLayerHiddenHookMixin, AttackArrowCheckHookMixin, BannerTooltipLayersHookMixin, BeaconEffectsHookMixin, BlockExplodedHookMixin, BlockHarvestExhaustionHookMixin, BlockStateCollisionHookMixin, CakeEatHookMixin, ChorusFruitHookMixin, ClientCoremodSitesMixin1710, ClientPickBlockHookMixin, CollisionGetterHookMixin, CrossCollisionHookMixin, EnchantmentHelperHookMixin, EntityFlameHookMixin, EntityLightProbeHookMixin, EntityMoveHookMixin, EntityMovementHookMixin, ExplosionKnockbackHookMixin, FallingBlockFreeHookMixin, FireworkRocketLiftHookMixin, FlowingFluidBlockedHookMixin, FlyingMobTravelHookMixin, FoodDataHookMixin1122, FurnaceInputHookMixin, FurnaceResultHookMixin, FurnaceTakenOverHookMixin, GameRendererPickHookMixin, HungerEffectExhaustionHookMixin, InBedChatScreenHookMixin, InventoryDropAllHookMixin, ItemStackComponentHookMixin, ItemStackHookMixin, ItemSupportsEnchantmentHookMixin, LevelRainHookMixin, LevelRainingAtHookMixin, LevelSetBlockHookMixin, LightTextureNightVisionHookMixin, LiquidBlockAddedHookMixin, LivingEntityCoremodSitesMixin, LivingEntityEffectParticlesHookMixin, LivingEntityRendererNameHookMixin, LivingEntitySlipperinessHookMixin, LivingEntityUseDurationHookMixin, LivingRenderPoseHookMixin1710, LoomLayersHookMixin, MobAiHookMixin, MouseTurnHookMixin, NearestTargetPlayerHookMixin, ParrotTextureHookMixin, PlayerAttackHookMixin, PlayerBlockReachHookMixin, PlayerEntityReachHookMixin, PlayerExhaustionHookMixin, PlayerHurtSoundHookMixin, PlayerMovementExhaustionHookMixin, PressurePlateEntitiesHookMixin, ScreenEffectOverlayHookMixin, ServerLevelNeighborHookMixin, ServerLevelSleepHookMixin, ServerLevelWakeHookMixin, ServerLevelWeatherHookMixin, ServerSprintHookMixin, ShoulderParrotTextureHookMixin, ShulkerBoxEntityInsertHookMixin, ShulkerBoxInsertHookMixin, SignalGetterHookMixin, SlimeChunkHookMixin, TargetingCandidatesHookMixin, TargetingHiddenHookMixin, TrackedEntityRangeHookMixin, TrunkPlacerGroundHookMixin, UnloadedChunkSlowFallHookMixin, VehicleDestroyedHookMixin, WitherBlockHookMixin

<!-- coremod-hooks:end -->

## Full table

Columns: mixin class (package `org.kylepitcock.legacyconnector.mixin`, `perf/` and `compat/` subpackages as named), target, injections (`kind method@at(target) require`), class (a to d), overlap found in the perf jars, what was done.

| Mixin | Target | Injections | Class | Perf mod overlap | Done |
|---|---|---|---|---|---|
| AbstractClientPlayerFovHookMixin | AbstractClientPlayer | WrapOperation getFieldOfViewModifier@INVOKE(Abilities) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| Am2CameraPositionMixin | Camera | Inject setup@INVOKE(Camera) req1(default) / shadows: eyeHeight, eyeHeightOld, getPosition, setPosition | d | Sodium Extra instant_sneak.MixinCamera (shadows eyeHeight, injects Camera.tick HEAD): different method, plain private float shadow, not retyped | unchanged |
| Am2MouseMixin | MouseHandler | ModifyArgs turnPlayer@INVOKE(LocalPlayer) req1(default) | d | none found | unchanged |
| Am2PlayerModelMixin | PlayerRenderer | Inject setupRotations@TAIL req1(default) | d | none found | unchanged |
| AnvilMenuCostHookMixin | AnvilMenu | ModifyConstant createResult@CONSTANT req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| AnvilScreenCostHookMixin | AnvilScreen | ModifyConstant renderLabels@CONSTANT req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ArmorLayerHiddenHookMixin | HumanoidArmorLayer | Inject renderArmorPiece req1(default) | d | Iris entity_render_context.MixinHumanoidArmorLayer injects renderArmorPiece (TAIL): two injects coexist | unchanged |
| AttackArrowCheckHookMixin | ServerGamePacketListenerImpl$1 | ModifyExpressionValue onAttack@CONSTANT req0 / shadows: entity | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| BannerTooltipLayersHookMixin | BannerItem | ModifyConstant appendHoverTextFromBannerBlockEntityTag@CONSTANT req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| BeaconEffectsHookMixin | BeaconBlockEntity | Inject applyEffects@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| BetaAnimalAiStepMixin | Mob | Inject serverAiStep@HEAD req1(default) | d | none found | unchanged |
| BetaChunkPopulationMixin | NaturalSpawner | WrapOperation spawnMobsForChunkGeneration@INVOKE(MobSpawnSettings) req1(default) | d | none found | unchanged |
| BetaEntityShadowMixin | EntityRenderDispatcher | WrapOperation render@INVOKE req1(default); ModifyExpressionValue renderShadow@INVOKE(Mth) req1(default) | d | Iris MixinEntityRenderDispatcher injects renderShadow HEAD/RETURN, render (pushPose ModifyVariable, popPose inject): none touches renderShadow's Mth.lerp or the renderShadow call we wrap | unchanged |
| BetaGenerationDependenciesMixin | Builder | Inject build@HEAD req1(default) / shadows: status, blockStateWriteRadius, addRequirement | d | no perf mod targets ChunkStep(.Builder); C2ME rewrites ChunkMap/ServerChunkCache scheduling, not the step builder | unchanged |
| BetaSpawnMobAccess | Mob | (adds members / accessors only: A:spawnType) | d | none found | unchanged |
| BetaSpawnSchedulingMixin | ServerChunkCache | WrapMethod tickChunks req1(default); WrapOperation tickChunks@INVOKE req1(default) / shadows: level | d | C2ME notickvd/opts-scheduling MixinServerChunkManager: WrapOperation getAllEntities, Redirect getTickingChunk, Inject tickChunk in tickChunks; ours wraps tickChunks as a whole and the NaturalSpawner.spawnForChunk call: disjoint call sites, WrapMethod/WrapOperation stack | unchanged |
| BetaSpawnStateAccess | SpawnState | (adds members / accessors only: I:canSpawn, I:afterSpawn) | d | none found | unchanged |
| BlockEntityNullStateMixin | BlockEntity | Inject validateBlockState@HEAD req0 | c | none found | listed: require=0 |
| BlockExplodedHookMixin | Block | Inject wasExploded@TAIL req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| BlockExpressionModelMixin | BlockModel$Deserializer | WrapMethod deserialize req1(default) | d | MoreCulling models.cullshape.Deserializer_cullShapeMixin injects BlockModel$Deserializer.deserialize: Inject + WrapMethod coexist | unchanged |
| BlockHarvestExhaustionHookMixin | Block | WrapOperation playerDestroy@INVOKE(Player) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| BlockLootSupplierMixin | BlockBehaviour | WrapOperation (was Redirect) getLootTable@INVOKE(Supplier) req0 | b | no perf mod targets BlockBehaviour.getLootTable | converted to @WrapOperation (same behaviour) |
| BlockStateCollisionHookMixin | BlockStateBase | Inject getCollisionShape@HEAD req1(default) | d | none (vanilla method inject; hook for a coremod, require=0 by design) | unchanged |
| BowItemLegacyLooseMixin | BowItem | Inject releaseUsing@HEAD req1(default) | d | none found | unchanged |
| CakeEatHookMixin | CakeBlock | WrapOperation eat@INVOKE(FoodData) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ChorusFruitHookMixin | ChorusFruitItem | Inject finishUsingItem@INVOKE(LivingEntity) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ClientCoremodSitesMixin1710 | ItemInHandRenderer | Inject renderHandsWithItems@TAIL req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ClientLanguageLegacyKeysMixin | ClientLanguage | Inject getOrDefault@RETURN req1(default); Inject has@RETURN req1(default) / shadows: storage | d | ModernFix perf.dynamic_languages replaces the storage map (ModifyArg on the constructor); Iris MixinClientLanguage injects getOrDefault/has HEAD. The shadow is typed Map and ModernFix's own accessor reads the same field as Map, so a lazy map stays valid; we only call containsKey | unchanged |
| ClientLevelHorizonB173Mixin | ClientLevel$ClientLevelData | Inject getHorizonHeight@HEAD req1(default) | d | Iris sky.MixinClientLevelData_DisableVoidPlane injects getHorizonHeight HEAD (two cancellable HEAD injects: first to cancel wins, only in Beta worlds for ours) | unchanged |
| ClientLevelSkyColorMixin | ClientLevel | Inject getSkyColor@RETURN req1(default); Inject getStarBrightness@RETURN req1(default) | d | BadOptimizations tick.MixinClientWorld injects getSkyColor HEAD/RETURN; Sodium ClientLevelMixin redirects the CubicSampler call inside it: our RETURN inject runs after both | unchanged |
| ClientPickBlockHookMixin | Minecraft | Inject pickBlock@TAIL req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| CollisionGetterHookMixin | CollisionGetter | Inject getBlockCollisions@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ConfigValueUnloadedMixin | ModConfigSpec$ConfigValue | Inject get@HEAD req0 / shadows: spec, defaultSupplier | c | none found | listed: require=0 |
| CreativeTabDuplicateMixin | BuildCreativeModeTabContentsEvent | Inject assertNewEntryDoesNotAlreadyExists@HEAD req0 | c | none found | listed: require=0 |
| CrossCollisionHookMixin | CrossCollisionBlock | Inject getCollisionShape@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| CustomHeadLayerLegacyArmorMixin | CustomHeadLayer | Inject render@HEAD req1(default) | d | none found | unchanged |
| EnchantmentHelperHookMixin | EnchantmentHelper | ModifyExpressionValue getEnchantmentCost@INVOKE(ItemStack) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| EntityFlameHookMixin | EntityRenderDispatcher | Inject renderFlame@HEAD req0 | c | Iris MixinEntityRenderDispatcher injects renderFlame HEAD/RETURN: coexist | listed: require=0 |
| EntityGravityMixin | Entity | Inject getGravity@RETURN req0; Inject nextStep@RETURN req0; Inject canSpawnSprintParticle@RETURN req0 | c | none found | listed: require=0 |
| EntityLightProbeHookMixin | Entity | Inject getLightProbePosition@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| EntityMoveHookMixin | Entity | Inject move@HEAD req0; Inject move@INVOKE(Entity) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| EntityMovementHookMixin | Entity | Inject collide@RETURN req0; Inject move@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ExplosionKnockbackHookMixin | Explosion | WrapOperation explode@INVOKE(LivingEntity) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| FallingBlockFreeHookMixin | FallingBlock | Inject isFree@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| FireworkRocketLiftHookMixin | FireworkRocketEntity | ModifyConstant tick@CONSTANT req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| FlowingFluidBlockedHookMixin | FlowingFluid | Inject canHoldFluid@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| FlyingMobTravelHookMixin | FlyingMob | Inject travel@TAIL req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| FoodDataHealRateMixin | FoodData | Inject tick@HEAD req0; ModifyConstant tick@CONSTANT req0; ModifyConstant tick@CONSTANT req0 | c | none found | listed: require=0 |
| FoodDataHookMixin1122 | FoodData | Inject tick@HEAD req0; Inject needsFood@RETURN req0; ModifyConstant add@CONSTANT req0; ModifyVariable addExhaustion@HEAD req0; ModifyConstant addExhaustion@CONSTANT req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| FurnaceInputHookMixin | AbstractFurnaceBlockEntity | Inject canPlaceItem@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| FurnaceResultHookMixin | AbstractFurnaceBlockEntity | WrapOperation burn@INVOKE(AbstractCookingRecipe) req1(default) | d | none (vanilla method inject; hook for a coremod, require=0 by design) | unchanged |
| FurnaceTakenOverHookMixin | AbstractFurnaceBlockEntity | Inject serverTick@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| GameRendererPickHookMixin | GameRenderer | Inject pick@HEAD req0 / shadows: minecraft | c | several mods inject into GameRenderer (BadOptimizations tick, ImmediatelyFast, Iris); none targets pick(F) nor the private minecraft field type | listed: require=0 |
| HungerEffectExhaustionHookMixin | HungerMobEffect | WrapOperation applyEffectTick@INVOKE(Player) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| InBedChatScreenHookMixin | InBedChatScreen | Inject sendWakeUp@TAIL req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| InventoryDropAllHookMixin | Inventory | WrapOperation dropAll@INVOKE(ItemStack) req0 / shadows: player | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| InventoryScreenLegacyLayoutMixin | InventoryScreen | Inject renderBg@HEAD req1(default); Inject renderLabels@HEAD req1(default); Inject hasClickedOutside@HEAD req1(default) | d | none found | unchanged |
| ItemStackComponentHookMixin | ItemStack | Inject getDisplayName@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ItemStackHookMixin | ItemStack | Inject inventoryTick@TAIL req0; Inject useOn@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ItemStackLegacyNbtMixin | ItemStack |  | d | none found | unchanged |
| ItemStackOversizedResultMixin | ItemStack | WrapOperation (was Redirect) validateStrict@INVOKE(ItemStack) req0 | b | ModernFix perf.smart_ingredient_sync only touches Ingredient codecs | converted to @WrapOperation (same behaviour) |
| ItemSupportsEnchantmentHookMixin | IItemExtension | Inject supportsEnchantment@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| JigsawStructureLegacyVillageMixin | JigsawStructure | Inject findGenerationPoint@RETURN req0 / shadows: startPool | c | none found | listed: require=0 |
| LevelBlockInterceptionB173Mixin | Level | Inject setBlock@HEAD req1(default) | d | none found | unchanged |
| LevelChunkBlockFamilyMixin | LevelChunk | WrapOperation setBlockState@INVOKE(BlockState) req1(default) | d | C2ME MixinWorldChunk (LevelChunk <init>, isTicking, postProcessGeneration), ModernFix: none touch setBlockState's BlockState.is call | unchanged |
| LevelRainHookMixin | Level | Inject getRainLevel@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| LevelRainingAtHookMixin | Level | Inject isRainingAt@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| LevelSetBlockHookMixin | Level | Inject setBlock@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| LevelTimeAccessB173Mixin | LevelTimeAccess | Inject getTimeOfDay@RETURN req1(default) | d | none found | unchanged |
| LightTextureNightVisionHookMixin | LightTexture | WrapOperation updateLightTexture@INVOKE(LocalPlayer) req0; WrapOperation updateLightTexture@INVOKE(GameRenderer) req0 | c | Iris MixinLightTexture injects updateLightTexture at a getSkyDarken call; our WrapOperations wrap hasEffect and getNightVisionScale: different calls | listed: require=0 |
| LiquidBlockAddedHookMixin | LiquidBlock | Inject onPlace@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| LivingEntityCoremodSitesMixin | LivingEntity | ModifyArg tickEffects@INVOKE(Level) req0; Inject getArmorCoverPercentage@RETURN req0; ModifyVariable calculateEntityAnimation@STORE req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| LivingEntityEffectParticlesHookMixin | LivingEntity | Inject tickEffects@INVOKE(Level) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| LivingEntityRendererNameHookMixin | LivingEntityRenderer | Inject shouldShowName@RETURN req0 | c | Sodium Extra render.entity.MixinLivingEntityRenderer injects shouldShowName HEAD: coexist | listed: require=0 |
| LivingEntitySlipperinessHookMixin | LivingEntity | WrapOperation travel@INVOKE(BlockState) req1(default) | d | none (vanilla method inject; hook for a coremod, require=0 by design) | unchanged |
| LivingEntityTravelMixin | LivingEntity | Inject travel@HEAD req1(default); Inject setSprinting@HEAD req0; Inject getCurrentSwingDuration@RETURN req0 | c | none found | listed: require=0 |
| LivingEntityUseDurationHookMixin | LivingEntity | WrapOperation getTicksUsingItem@INVOKE(ItemStack) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| LivingRenderPoseHookMixin1710 | LivingEntityRenderer | WrapOperation render@INVOKE req0; Inject setupRotations@TAIL req0 | c | ModernFix bugfix.entity_pose_stack redirects the IEventBus.post call in render; Sodium Extra injects render: our WrapOperation wraps setupAnim, a different call | listed: require=0 |
| LoomLayersHookMixin | LoomMenu | WrapOperation slotsChanged@INVOKE(List) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| MappedRegistryIntrusiveHolderMixin | MappedRegistry | WrapOperation createIntrusiveHolder@INVOKE(MappedRegistry) req0 | c | ModernFix perf.compact_mojang_registries.MappedRegistryMixin injects the constructor RETURN and replaces storage maps; ours wraps the validateWrite call in createIntrusiveHolder and names no field | listed: require=0 |
| MappedRegistryLazyViewMixin | MappedRegistry | Inject getKey@HEAD req0 | c | same ModernFix MappedRegistry mixin: no common injection point; ours injects getKey(Object) HEAD | listed: require=0 |
| MinecraftAttackMixin | Minecraft | Inject startAttack@HEAD req0; Inject continueAttack@HEAD req0; ModifyConstant startUseItem@CONSTANT req0 | c | none found | listed: require=0 |
| MobAiHookMixin | Mob | Inject serverAiStep@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| MouseTurnHookMixin | MouseHandler | Inject turnPlayer@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| MultiNoiseBiomeSourceAccessor | MultiNoiseBiomeSource | (adds members / accessors only: I:parameters) | d | C2ME access.IMultiNoiseBiomeSource: a second invoker accessor on the same class (no conflict) | unchanged |
| MultiNoiseBiomeSourceLegacyBiomeMixin | MultiNoiseBiomeSource | Inject getNoiseBiome@HEAD req1(default) | d | none found | unchanged |
| MultiPlayerGameModeMixin | MultiPlayerGameMode | Inject destroyBlock@HEAD req0 | c | none found | listed: require=0 |
| NearestTargetPlayerHookMixin | NearestAttackableTargetGoal | Inject findTarget@TAIL req0 / shadows: targetType, target | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| OptionsScreenLegacySettingsMixin | OptionsScreen | Inject init@INVOKE(HeaderAndFooterLayout) req1(default) | d | Sodium features.gui.hooks.settings.OptionsScreenMixin injects the video-settings button lambda (HEAD); ours injects init at the visitWidgets call and captures the RowHelper local | unchanged |
| OversizedCraftingMenuMixin | AbstractContainerMenu | Inject moveItemStackTo@RETURN req1(default); Inject doClick@HEAD req1(default) / shadows: moveItemStackTo, resetQuickCraft | d | none found | unchanged |
| ParrotTextureHookMixin | ParrotRenderer | Inject getVariantTexture@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| PlacedFeatureOreGateMixin | PlacedFeature | Inject placeWithBiomeCheck@HEAD req1(default) | d | none | unchanged |
| PlayerAttackHookMixin | Player | WrapOperation attack@INVOKE(Entity) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| PlayerBlockReachHookMixin | Player | Inject blockInteractionRange@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| PlayerClassTravelB173Mixin | LivingEntity | Inject travel@HEAD req1(default) | d | none found | unchanged |
| PlayerDistancePredicateB173Mixin | Entity | Inject distanceToSqr@RETURN req1(default) | d | none found | unchanged |
| PlayerEntityReachHookMixin | Player | Inject entityInteractionRange@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| PlayerExhaustionHookMixin | Player | ModifyArg jumpFromGround@INVOKE req0; ModifyArg jumpFromGround@INVOKE req0; ModifyArg attack@INVOKE req0; ModifyArg actuallyHurt@INVOKE req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| PlayerFoodOwnerMixin | Player | Inject <init>@TAIL req0 / shadows: foodData | c | none found | listed: require=0 |
| PlayerHurtSoundHookMixin | Player | Inject getHurtSound@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| PlayerMovementExhaustionHookMixin | ServerPlayer | ModifyArg checkMovementStatistics@INVOKE req0; ModifyArg checkMovementStatistics@INVOKE req0; ModifyArg checkMovementStatistics@INVOKE req0; ModifyArg checkMovementStatistics@INVOKE req0; ModifyArg checkMovementStatistics@INVOKE req0; ModifyArg checkMovementStatistics@INVOKE req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| PlayerStepSoundMixin | Player | Inject playStepSound@HEAD req0 | c | none found | listed: require=0 |
| PlayerWaterPredicateB173Mixin | Entity | Inject isInWater@RETURN req1(default) | d | none found | unchanged |
| PlayerWaterTravelB173Mixin | LivingEntity | ModifyExpressionValue travel@INVOKE(LivingEntity) req2 | d | none found | unchanged |
| PressurePlateEntitiesHookMixin | BasePressurePlateBlock | Inject getEntityCount@HEAD req0; Inject tick@TAIL req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| RecipeManagerContextMixin | RecipeManager | Inject apply@HEAD req0 / shadows: registries | c | ModernFix perf.datapack_reload_exceptions redirects a call inside RecipeManager.apply; ours injects HEAD of apply | listed: require=0 |
| RegistryLegacyNamesMixin | Registry | Inject byNameCodec@HEAD req0; Inject holderByNameCodec@HEAD req0 | c | none found | listed: require=0 |
| ScreenEffectOverlayHookMixin | ScreenEffectRenderer | Inject renderScreenEffect@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ServerLevelBlockFamilyMixin | ServerLevel | WrapOperation tickBlock@INVOKE(BlockState) req1(default); WrapOperation doBlockEvent@INVOKE(BlockState) req1(default) | d | C2ME opts-scheduling MixinServerWorld injects tickBlock/tickFluid RETURN; ours wraps the BlockState.is call: disjoint | unchanged |
| ServerLevelNeighborHookMixin | ServerLevel | Inject updateNeighborsAt@HEAD req0; Inject updateNeighborsAt@TAIL req0; Inject neighborChanged@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ServerLevelSleepHookMixin | ServerLevel | WrapOperation tick@INVOKE(SleepStatus) req0; WrapOperation tick@INVOKE(SleepStatus) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ServerLevelWakeHookMixin | ServerLevel | Inject wakeUpAllPlayers@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ServerLevelWeatherHookMixin | ServerLevel | Inject advanceWeatherCycle@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ServerSprintHookMixin | ServerGamePacketListenerImpl | Inject handlePlayerCommand@INVOKE(ServerPlayer) req0 / shadows: player | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ShoulderParrotTextureHookMixin | ParrotOnShoulderLayer | Inject render@HEAD req0; Inject render@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ShulkerBoxEntityInsertHookMixin | ShulkerBoxBlockEntity | Inject canPlaceItemThroughFace@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| ShulkerBoxInsertHookMixin | ShulkerBoxSlot | Inject mayPlace@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| SignalGetterHookMixin | SignalGetter | Inject hasNeighborSignal@RETURN req0; Inject getSignal@HEAD req0; Inject getDirectSignal@HEAD req0; Inject getSignal@RETURN req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| SlimeChunkHookMixin | Slime | Inject checkSlimeSpawnRules@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| StateHolderPropertyEqualsMixin | StateHolder | Inject getValue@NEW(IllegalArgumentException) req0; Inject setValue@NEW(IllegalArgumentException) req0; Inject getOptionalValue@RETURN req0; Inject trySetValue@RETURN req0; Inject hasProperty@RETURN req0 | c | FerriteCore FastMapStateHolderMixin: Redirect Table.get in setValue/trySetValue, Overwrite populateNeighbours, Shadow values/neighbours (retyped). Ours inject at NEW IllegalArgumentException / RETURN and use only getProperties()/getValue(k)/setValue(k,v): no shadow, no handler type tied to the retyped fields. ModernFix perf.mojang_registry_size injects populateNeighbours RETURN (irrelevant) | already rewritten (FerriteCore boot crash); verified no remaining retyped member |
| SynchedEntityDataBuilderMixin | Builder | Inject build@HEAD req0 / shadows: entity, itemsById | c | no perf mod targets SynchedEntityData(.Builder) (name clash with ModernFix AttributeSupplier.Builder / FerriteCore Multipart builders in a name-only match) | listed: require=0 |
| SynchedEntityDataDefineIdMixin | SynchedEntityData | Inject defineId@RETURN req0 | c | none found | listed: require=0 |
| TargetingCandidatesHookMixin | TargetingConditions | Inject test@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| TargetingConditionsMixin | TargetingConditions | Inject test@HEAD req0 | c | none found | listed: require=0 |
| TargetingHiddenHookMixin | TargetingConditions | Inject test@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| TrackedEntityRangeHookMixin | ChunkMap$TrackedEntity | ModifyArg getEffectiveRange@INVOKE(ChunkMap$TrackedEntity) req0; ModifyArg updatePlayer@INVOKE(Math) req0 / shadows: entity | c | no perf mod targets ChunkMap$TrackedEntity (C2ME touches ChunkMap, its distance manager and ServerChunkCache only) | listed: require=0 |
| TranslatableArgumentsMixin | TranslatableContents | ModifyVariable <init>@HEAD req0 | c | none found | listed: require=0 |
| TreeGrowerLegacyMixin | TreeGrower | Inject getConfiguredFeature@HEAD req0 | c | none found | listed: require=0 |
| TrunkPlacerGroundHookMixin | TrunkPlacer | Inject setDirtAt@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| UnloadedChunkSlowFallHookMixin | LivingEntity | ModifyExpressionValue travel@INVOKE(Level) req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| Varint21FrameDecoderMixin | Varint21FrameDecoder | ModifyConstant copyVarint@CONSTANT req0; ModifyConstant <init>@CONSTANT req0 | c | none found | listed: require=0 |
| Varint21LengthFieldPrependerMixin | Varint21LengthFieldPrepender | ModifyConstant encode@CONSTANT req0 | c | none found | listed: require=0 |
| VehicleDestroyedHookMixin | VehicleEntity | Inject destroy@TAIL req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| WitherBlockHookMixin | WitherBoss | Inject canDestroy@HEAD req0 | c | none (vanilla method inject; hook for a coremod, require=0 by design) | listed: require=0 |
| compat/ArchitecturyEventImplMixin | EventFactory$EventImpl | WrapOperation (was Redirect) <init>@NEW(ArrayList) req0 | b | not in the perf set | converted to @WrapOperation (same behaviour) |
| compat/AurorasIrisStateMixin | AHelpers | Inject areShadersEnabled@HEAD req0 | d | none found | unchanged |
| compat/HbmTagStackDisplayMixin | RecipesCommon$TagStack | WrapOperation (was Redirect) getDisplayStacks@INVOKE(DefaultedRegistry) req0 | b | not in the perf set | converted to @WrapOperation (same behaviour) |
| compat/KubeJSBlockBuilderLegacyAliasesMixin | BlockBuilder |  | d | none found | unchanged |
| compat/KubeJSBlockEntityLegacyInventoryMixin | BlockEntityInfo |  | d | none found | unchanged |
| compat/KubeJSBlockEntityTickLegacyViewMixin | BlockEntityTickKubeEvent |  | d | none found | unchanged |
| compat/KubeJSItemBuilderLegacyGlowMixin | ItemBuilder |  | d | none found | unchanged |
| compat/KubeJSItemModificationLegacyBuilderMixin | ItemModificationKubeEvent$ItemModifications |  | d | none found | unchanged |
| compat/KubeJSKeybindLegacyMappingMixin | KeybindRegistryKubeEvent | Inject build@RETURN req1(default) | d | none found | unchanged |
| compat/RocketnauticsStarPlasmaMixin | SkyHandler | Inject ensureStarPlasmaTexture@HEAD req0 / shadows: STAR_PLASMA_TEXTURE_ID | d | none found | unchanged |
| perf/BlockSlotMixin | Block |  | d | none found | unchanged |
| perf/BlockStateSlotMixin | BlockStateBase |  | d | FerriteCore BlockStateBaseMixin/BlockStateCacheMixin and ModernFix reduce_blockstate_cache_rebuilds touch BlockStateBase: our mixin only adds a @Unique int field | unchanged |
| perf/EntitySlotMixin | Entity |  | d | none found | unchanged |
| perf/ItemSlotMixin | Item |  | d | none found | unchanged |
| perf/LevelChunkStaticTickerMixin | LevelChunk | WrapOperation updateBlockEntityTicker@INVOKE req0 | d | wraps BlockState.getTicker inside LevelChunk.updateBlockEntityTicker; no perf mod in run-perfmods touches it (Lithium, not available here, is the known one; its javadoc says why it is a WrapOperation) | unchanged |
| perf/LevelSlotMixin | Level |  | d | none found | unchanged |
