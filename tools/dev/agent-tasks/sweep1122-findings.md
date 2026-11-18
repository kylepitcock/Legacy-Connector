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

## Open (not fixed)

- **F1 Botania special flowers (functional and generating) render as a missing-texture cube when placed.** Cause narrowed: the models now bake, but
  `SpecialFlowerBakedModel.getQuads` picks the variant from the extended state `BotaniaStateProps.SUBTILE_ID`, which the tile entity fills; with
  `give @s botania:specialflower[minecraft:custom_data={type:"daybloom"}]` the placed block still has no subtile (unverified whether the legacy
  NBT key `type` is read from `custom_data`; a creative-tab flower was not tried). Repro: Botania alone, place a special flower, look at it.
  Screenshot `set2-botania-specialflower-after.jpg`.
- **F2 Ender IO / EnderCore log 204 `Missing translation for ...` errors at load**: `I18n.canTranslate` (`Lang.canLocalizeExact`) is asked in preInit/init
  before the language files are read. Display names and tooltips later translate fine (`Alloy Smelter`, `Electrical Steel Ingot` checked), so it is log noise
  unless a mod caches the untranslated string. Not checked against a real 1.12.2 client.
- **F3 EnderCore mixin loader**: `NoSuchFieldException: discoverer` printed 10 times (`EnderCorePlugin.findAnnotationMixins` reads FML's ASM data
  table `discoverer` by reflection through `ReflectionCompat`); EnderCore's annotation mixins are therefore not applied. Effect not checked.
- **F4 AE2 `item/builtin/*` models** (`memory_card`, `biometric_card`, `encoded_pattern`, `color_applicator_colored`) and Ender IO
  `item_dark_steel_upgrade` log `Unable to load model` (AE2's `BuiltInModelLoader`, a custom loader model of the item). The items still draw (seen in the
  hotbar), so this is a warning only; whether the pattern's output preview and the card/applicator colours work was not checked.
- **F5 Twilight Forest shaders fail to create** (`Failed to create shader! (VALIDATE)` for twilight_sky, aurora, firefly, starburst, shield, spiral, pulsing
  twice per start; GLSL 1.2 shaders from the mod, AMD driver here). Effects of the mod's own sky/aurora/fireflies are missing; the world renders.
- **F6 Twilight Forest worldgen spams `Block-attached entity at invalid position`** (1320 lines in 4 minutes, from `c2me-worker`) and ends with 443
  `minecraft:painting` entities in the TF dimension. The Lich Tower wing code (`ComponentTFTowerWing`, `new EntityPainting(world, pos, facing)` +
  `checkPainting`, which only checks collisions, not `onValidSurface`) places paintings; the shim's `EntityHanging.onValidSurface` is the 1.12 wall check
  but TF never calls it. Also 10 `Trying to set block entity StructureBlockEntity ... but state spawner/chest/trapped_chest does not allow it` (a legacy
  tile entity created for the wrong modern block entity type during structure generation). Repro: Twilight Forest alone, `execute in legacyconnector:dim_7 run tp @s 0 100 0`, wait.
- **F7 Twilight Forest `Duplicate legacy registration of twilightforest:twilight_leaves_3 in minecraft:item`** and `spiral_bricks` falls back
  (`state mapper file minecraft:x_spiral_bricks not found`).
- **F8 Chisel needs CTM** (`NoClassDefFoundError: team.chisel.ctm...OffsetProviderRegistry` in `ClientProxy.registerModels`; CTM was not in the set, not a
  layer defect) and its `animations/strobe.png.mcmeta` is malformed json (the mod's own file, 1.12's reader was lenient; 1.21 logs
  `Unable to parse metadata`). A lenient re-parse of mcmeta files in the pack generator would cover it; not done.
- **F9 Storage Drawers recipes `Could not translate legacy recipe minecraft:air_*: empty result` (108 lines)** appear when Chameleon is missing (the mod
  then registers 13 items and 0 blocks); with Chameleon present all 9 blocks register. Not a layer defect, but the log should say that Chameleon is
  required (the loader prints the dependency warning).
- **F10 Hwyla 1.12.2 runs as a legacy mod** (`waila` loaded as a 1.12.2 legacy mod); the Waila -> Jade bridge in CLAUDE.md is not applied here. Not checked
  whether the tooltips draw.
- **F11 AE2 `EntityEntry ... registered twice`** (3 entities) and `dimension N: fallback biome (plains) answered: no provider/level yet` (5 lines per
  mod dimension, at load) are warnings with no visible effect.
- Not covered: crafting through recipe GUIs, machine processing (power), AE2 networks across ticks, Blood Magic rituals, Atum/Erebus mobs, Quark features,
  Chisel (no CTM), Hwyla tooltips, Storage Drawers interaction (drawer front clicks).
