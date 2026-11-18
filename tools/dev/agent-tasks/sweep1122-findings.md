# 1.12.2 stability sweep 1 (real client, survival, performance mods on)

Method: `tools/dev/quick-shots.sh` with `SCENARIO=control WORLD=minecraft:normal RD=6`, `ALSOFT_DRIVERS=null`, translation cache removed per set, mods in
`run-screenshots/mods/1.12.2`, driven with `js.sh` and command files (give, place, open GUIs, screenshots). Jars came from `1.12.2 test mods/`
(no download was needed). Screenshots: `docs/screenshots/1.12.2/sweep/` (JPEG q90). Sets, about 30 min each:

| Set | Mods | Result |
|---|---|---|
| 1 tech | EnderCore, Ender IO, AE2, AE2 Stuff (no bdlib), Hwyla, CCL | loads; machines, ME drive/inscriber/controller/cables, Ender IO conduits placed; GUIs open. Open: items below |
| 2 magic | Baubles, Botania, Blood Magic, Guide-API, Astral Sorcery, Bookshelf | loads; apothecary, rune altar, Blood Altar, Astral altar/marble place and render; Lexica Botania and Astral journal open. Special flowers open (F1) |
| 3 worldgen | Atum 2, Erebus, Twilight Forest | all three dimensions generate and render (Atum desert, Erebus cavern with night vision, TF forest). Open: F5, F6, F7 |
| 4 QoL/GUI | Chisel (no CTM), Chisels & Bits, Iron Chests, Quark, AutoRegLib, Storage Drawers (+Chameleon) | found and fixed a server crash (F-fixed 1); Iron Chest GUI with Quark buttons, Quark chests place |

## Fixed in this branch (verified in the client)

1. **Chisels & Bits crashed the server with a StackOverflowError** on the first tick with a chisel in the inventory. `Item.getMetadata(ItemStack)` of the shim
   read `stack.getItemDamage()`, which asks `Item.getDamage(stack)`; an item that defines `getDamage` through `getMetadata(stack)` (ItemChisel:
   `max(getMetadata(stack), nbt damage)`) recursed forever. It now reads the stored value (`ItemStack.getMetadata()`). Generic: any item overriding
   `getDamage` that way.
2. **Astral Sorcery's `ItemColorizationHelper` reload listener threw an NPE** (`ItemRenderer.getModel` with `bakedmodel` null): `ItemModelMesher.getItemModel`
   asked the game before its models were baked. It now answers the shim's missing model (which mods compare against), as `ModelManager.getMissingModel` does.
3. **Null baked models during `ModelEvent.ModifyBakingResult`** (mods' `IModel.bake` run there): `Models1122.legacyModel` and the standalone path of
   `ModelLoaderRegistry.GameModel` answered null because the manager has no missing model on the first load. A shared `Models1122.missingModern()` falls
   back to the model being baked. This was the `NullPointerException: null value in entry: jadedAmaranthus=null` (16 lines per start) in Botania's
   `SpecialFlowerModel.bake` (Guava `ImmutableMap.Builder.put` of a null baked model).
4. **Blockstate variants of names no block owns** (Botania asks for `botania:jadedamaranthus#normal`/`#inventory`; the blockstate files are the subtiles',
   no block has that name): new `OrphanVariants1122` bakes the variant from the mod's own legacy blockstate json (model, textures, rotation) through the
   game's baking (`JsonBake1122.bakeVariant`; `JsonBake1122` also ends item chains in `ModelBakery.GENERATION_MARKER`). The special flowers' item models now
   come out (held daybloom shows the flower). The placed block is still wrong (F1).

## Fixed in sweep 2 (branch worktree-agent-ab2b45843d9898ec9)

- **F6 Twilight Forest worldgen** (verified in the client, TF + Botania + Baubles + Hwyla + EnderCore + Jade, perf mods, `dim_7`): the 1320
  `Block-attached entity at invalid position` lines and the 10 `Trying to set block entity StructureBlockEntity ... does not allow it` (plus the
  `ClassCastException TileEntityStructure -> TileEntityMobSpawner` that made the Druid Hut's chunk fail to populate) are gone. Four causes:
  1. `EntityHanging.mcc$placeModern` loaded the saved data of a new entity (position 0,0,0) with the wall 16+ blocks away, which vanilla's
     `HangingEntity.readAdditionalSaveData` rejects with that error and leaves the painting at the wrong place: the saved `Pos` is moved to the wall first.
  2. The vanilla `EntityPainting(world, pos, facing)` constructor picks a random art among those that fit the wall (`onValidSurface`); the shim kept the
     default art. It now does (the checked arts are the 1.12 list). The check reads the world the mod gave the entity, not the real level: the modern
     `survives()` read `ServerLevel.getBlockState` from the generator thread and deadlocked the chunk system (seen and fixed in this run).
  3. During generation a legacy generator asked the world for entities in a box after spawning some (TF's `checkPainting` refuses a painting that overlaps
     a hanging entity placed before it); the generation region stores spawned entities as saved data, so the answer was always empty and every
     overlapping painting was placed. `World` now remembers the entities spawned into the region per generator thread (last 512) and answers from them.
     Result: 443 paintings (434 of them failing the wall check and dropping after 100 ticks) became 74 (20 valid, screenshot
     `docs/screenshots/1.12.2/sweep2/twilight-lich-tower-painting.jpg`); the remaining 54 sit where TF itself chose a spot without wall (TF checks only
     collisions, as in 1.12).
  4. A proto chunk keeps the block entity of the block that stood there before: a template's structure block that a generator turns into a chest or spawner
     (TF data markers) kept answering `TileEntityStructure` for `getTileEntity`. `World.ensureGeneratedBlockEntity` now removes a block entity the new
     state does not accept.
- **F10 Hwyla 1.12.2 -> Jade** needed no code: the Waila bridge (`LibraryBridges`, `LegacyJade1122`) is on when Jade is installed; sweep 1 had no Jade. With
  Jade 15.10.6 in `mods/` Hwyla is not loaded ("bridging ... waila" in the log, `legacy mods` list has no `waila`) and Jade shows
  the tooltip for Botania's blocks (`docs/screenshots/1.12.2/sweep2/botania-special-flowers-jade.jpg`: "Pure Daisy / Botania"). Without Jade the legacy
  jar is loaded as before (nothing draws a tooltip then; not changed).
- **F3 EnderCore mixin loader**: `Loader` has the `discoverer` field (a `ModDiscoverer`, ASM table built on first use) that `EnderCorePlugin.findAnnotationMixins`
  reads by reflection. With EnderIO installed the log now shows `EnderCore Mixins: Found annotation mixin: ...` / `Registered mixin.` for
  `DarkSteelUpgradeMixin` and `InternalPoweredItemMixin` (the others skip for missing mods) and no `NoSuchFieldException`. What the two mixins change in game
  was not checked (see F12: that set does not reach a world here).
- **F1 Botania special flowers**: not a layer defect. The sweep placed `type:"daybloom"`, which this Botania (r1.10-363) does not have (no blockstate, no
  subtile class), so the block has no subtile and 1.12 draws its missing model too. All 11 valid special flowers placed with the creative-tab stack format
  (`custom_data={type:"endoflame"}` etc.: endoflame, manastar, hydroangeas, dandelifeon, rannuncarpus, spectrolus, bellethorn, orechid, agricarnation,
  gourmaryllis, kekimurus, puredaisy) render their models, with the subtile name synced to the client block entity (`subTileName`), verified in the client.
- **F9 Storage Drawers without Chameleon**: the load report already named the cause (`Storage Drawers requires chameleon ... which is not installed`, an error
  at preInit with `NoClassDefFoundError`, the loading-warnings screen on a client). The 36 (not 108: the test pack has fewer recipes) per-recipe
  `Could not translate legacy recipe minecraft:air_*: empty result` warnings, logged three times, are now one summary line per generation that points at
  `MISSING DEPENDENCY` in the load report (`RecipeBridge1122`); the per-recipe lines are debug.

## Open (not fixed)

- **F12 (new) EnderCore + EnderIO (+ Jade or not, Hwyla or not) never finish creating a world in the screenshot test** on this machine: the integrated
  server logs `Preparing spawn area` and `Changing simulation distance`, then its thread sits in `waitUntilNextTick` forever and the client in
  `Minecraft.doWorldLoad`'s wait for `isReady()` (no deadlock in `jstack`, no error). Reproduced three times, also on the unmodified master base commit
  (`36e6158bf`) with only `EnderCore` + `EnderIO` + the perf mods + Jade; the same pack without EnderIO reaches the world in about a minute. Sweep 1 ran
  EnderIO fine, so something between (perf mod set, world seed or timing) changed; not diagnosed.

- **F2 Ender IO / EnderCore log 204 `Missing translation for ...` errors at load**: `I18n.canTranslate` (`Lang.canLocalizeExact`) is asked in preInit/init
  before the language files are read. Display names and tooltips later translate fine (`Alloy Smelter`, `Electrical Steel Ingot` checked), so it is log noise
  unless a mod caches the untranslated string. Not checked against a real 1.12.2 client.
- **F4 AE2 `item/builtin/*` models** (`memory_card`, `biometric_card`, `encoded_pattern`, `color_applicator_colored`) and Ender IO
  `item_dark_steel_upgrade` log `Unable to load model` (AE2's `BuiltInModelLoader`, a custom loader model of the item). The items still draw (seen in the
  hotbar), so this is a warning only; whether the pattern's output preview and the card/applicator colours work was not checked.
- **F5 Twilight Forest shaders fail to create** (`Failed to create shader! (VALIDATE)` for twilight_sky, aurora, firefly, starburst, shield, spiral, pulsing
  twice per start; GLSL 1.2 shaders from the mod, AMD driver here). Effects of the mod's own sky/aurora/fireflies are missing; the world renders.
- **F7 Twilight Forest `Duplicate legacy registration of twilightforest:twilight_leaves_3 in minecraft:item`** and `spiral_bricks` falls back
  (`state mapper file minecraft:x_spiral_bricks not found`).
- **F8 Chisel needs CTM** (`NoClassDefFoundError: team.chisel.ctm...OffsetProviderRegistry` in `ClientProxy.registerModels`; CTM was not in the set, not a
  layer defect) and its `animations/strobe.png.mcmeta` is malformed json (the mod's own file, 1.12's reader was lenient; 1.21 logs
  `Unable to parse metadata`). A lenient re-parse of mcmeta files in the pack generator would cover it; not done.
- **F11 AE2 `EntityEntry ... registered twice`** (3 entities) and `dimension N: fallback biome (plains) answered: no provider/level yet` (5 lines per
  mod dimension, at load) are warnings with no visible effect.
- Not covered: crafting through recipe GUIs, machine processing (power), AE2 networks across ticks, Blood Magic rituals, Atum/Erebus mobs, Quark features,
  Chisel (no CTM), Hwyla tooltips, Storage Drawers interaction (drawer front clicks).
