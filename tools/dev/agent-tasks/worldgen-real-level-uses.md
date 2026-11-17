# Real-level uses reachable from legacy generators (audit 2026-11-17)

Rule: legacy code that runs during chunk generation reads and writes blocks through the generation region, never the real `ServerLevel`
(the real level's `getChunk` from a worldgen thread waits on the main thread, which waits on the chunk being generated: deadlock).

## How the accessors work

- 1.7.10 `World`: `mcc$level()` is already region-aware (`lvl()`: a per-thread region from `mcc$enterRegion`, or the view's own region).
  `mcc$fullLevel()` always returns the real `Level` (entity construction, weather, time, saves); `mcc$liveLevel()` returns null in a region.
  All 1.7.10 block/tile/entity/light/biome methods of `World` use `lvl()`.
- 1.12.2 `World`: `mcc$access()` is region-aware (`acc()`); `mcc$level()` returns the REAL level even for a region view. This is the defect class:
  shim code that used `mcc$level()` for block reads/writes. Fix: use `mcc$access()` (a `LevelAccessor`) where only `LevelAccessor` API is needed.
- Guard: `core/worldgen/RealLevelGuard` (on in the server self test via `legacytest.autostop`, or `-Dlegacyconnector.realLevelGuard=true`) logs
  `REAL LEVEL GUARD: ...` once per call site (short stack) when `mcc$level()` (1.12.2) or `mcc$fullLevel()` (1.7.10) is called on a world that
  is a region. `verify.sh` shows the count as `rlSite` (regresses when above baseline). A report is not always a defect (entity creation needs the level object).

## Sites (1.12.2 shim, `mcc$level()` outside `World.java`: 170 sites in 70 files; 1.7.10: 41 `mcc$fullLevel()` + 13 `mcc$liveLevel()` outside `World.java`)

Verdicts. WRONG = fixed in this change.

| Site | Verdict |
|---|---|
| 1.12.2 `BlockGrass.grow` | wrong, fixed earlier (d8f81a3cd) |
| 1.12.2 `BlockGrass.isPlainDirt`, `BlockLadder.canAttachTo`, `BlockTorch.canPlaceOn` | wrong, fixed (`mcc$access()`): placement checks run in dungeon/structure generators |
| 1.12.2 `BlockTallGrass.grow`, `BlockCauldron` (level read/write), `BlockLiquid` (fluid level, flow), `BlockFurnace.setState`, `Block.rotateBlock` | wrong, fixed |
| 1.12.2 `BlockFalling` (`mcc$scheduleFall`, `checkFallable`) | wrong, fixed: tick scheduled into the region (proto chunk container); the fall check is skipped while generating (as vanilla) |
| 1.12.2 `Block.spawnAsEntity` | wrong, fixed: `addFreshEntity` goes through the accessor (region); entity object still built from the level |
| 1.12.2 `ItemBlock.canPlaceBlockOnSide` | wrong, fixed |
| 1.12.2 `TileEntityChest.isChestAt` (neighbour check) | wrong, fixed |
| 1.12.2 `Chunk.getLightFor`, `getLightSubtracted` | wrong, fixed (answer the default in a region, as `World.getLightFor`) |
| 1.12.2 `BlockSnapshot.mcc$tileData` | wrong, fixed |
| 1.12.2 `World.canBlockFreezeBody`, `canSnowAtBody` (biome and `shouldFreeze/shouldSnow` read the real level) | wrong, fixed (`acc()`) |
| 1.12.2 `World` light, `isRainingAt`, `getBiome`, tile entity, entity, tick, notify methods | safe: already `acc()` / `mcc$liveLevel()` (null in a region) |
| 1.12.2 `StructureVillagePieces` villager spawn | safe: uses the level only to construct the entity, adds through `mcc$access()` |
| 1.12.2 `World.mcc$generatorBiome` (`level.hasChunk` then `getBiome`) | uncertain: reads the real level only when the chunk is present |
| 1.12.2 `ForgeHooks.onCropsGrowPre/Post`, `ForgeEventFactory.*` (bonemeal, fluid place, arrow) | uncertain: NeoForge hooks take `Level`; reached by growth ticks, not by generators in practice |
| 1.12.2 `Item.use/onItemUse/rayTrace`, `ItemBlock.onItemUse`, `ItemDye`, `ItemBucket`, `FluidUtil`, `ForgeHooks.onPlaceItemIntoWorld/onBlockBreakEvent`, `Block.getPlayerRelativeBlockHardness/addCollisionBoxToList/dropXpOnBlockBreak` | safe: player or entity driven, not run by generators |
| 1.12.2 `BlockAnvil/Workbench/EnchantmentTable/Furnace GUI`, `BlockDaylightDetector`, `BlockTNT`, `BlockSilverfish`, `BlockSkull.checkWitherSpawn`, `TileEntityNote/EndGateway/LockableLoot`, `InventoryHelper` | safe: interaction or tick only |
| 1.12.2 `MobSpawnerBaseLogic` (9 sites) | uncertain: `setEntityId` during generation reads the level for `load`; the spawner is not ticked in a region (not changed) |
| 1.12.2 client, command, dispenser, entity tracker, player list, packets, map, village collection, `ChunkProviderServer`, `AnvilChunkLoader`, `WorldType`, forge chunk manager, farmland water | safe: server/client plumbing, not generator reachable |
| 1.7.10 `World` block, tile, light, biome, height, tick, spawn, snow/ice methods | safe: `lvl()` (region aware); `canLightningStrikeAt` uses `mcc$liveLevel()` (null in a region) |
| 1.7.10 `mcc$fullLevel()` users: entity construction (`EntityItem`, `EntityXPOrb`, `EntityList`), players, weather, time, saves, maps, chunk providers, forge hooks | safe: no block access; entity construction needs the level object (the guard will name any that run in a region) |
| 1.7.10 `Block.mcc$vanillaPick` (`fullLevel().getBlockState`) | safe: pick-block (client/player) only |
| 1.7.10 `BlockPortal.tryToCreatePortal`, `ItemDye.applyBonemeal` | uncertain: use the real level; player/neighbour-change driven, not by generators |
| Bridges (`WorldGenBridge`, `WorldGenBridge1122`, `LegacyVillagePoolElement1122`, core `worldgen/*`) | safe: hand the region to `World.mcc$wrap` / `WorldBridge1122.wrap` |
| `getServer().getLevel(..)` / `getChunkAt` in shims and bridges | safe: one site (`World.markBlockForUpdate` path, under `mcc$liveLevel()`) |

Counts: about 240 sites reviewed (grouped above); 21 wrong (fixed), about 110 safe, 3 groups (about 25 sites) uncertain.

## Guard reports of the first suite runs (2026-11-17): classification

Reports from `mixed` (10), `bop1122` (2), `villages1122` (6), `traincraft` (6). New accessors that the guard does NOT report, documented as
"the Level as an object, never block access": 1.7.10 `World.mcc$levelObject()`, 1.12.2 `World.mcc$levelObject()` (region views return the
dimension's Level). Block, height, biome, chunk and light access keeps going through `mcc$level()` (1.7.10) / `mcc$access()` (1.12.2).

| Site (version, member) | Triggered by | Verdict | Handling |
|---|---|---|---|
| 1.7.10 `ChunkProviderServer.mcc$loader` (via `World.getChunkProvider`) | Witchery/Traincraft `IWorldGenerator.generate`, `WorldGenBridge.decorate`, `DeepLayer1710` | legitimate: only reads the dimension folder path for the loader; `chunkExists`/`provideChunk` already answer from the region | `mcc$levelObject()` |
| 1.12.2 `ChunkProviderServer.mcc$chunkLoader` (via `World.mcc$serverChunkProvider`) | `WorldGenBridge1122.decorate` | legitimate (same) | `mcc$levelObject()` |
| 1.7.10 `World.mcc$get$provider` (`registerWorld`, `WorldInfo.getTerrainType`, `WorldInfo.level`) | any mod reading `world.provider` from a generator (Witchery, Thaumcraft, Traincraft, village pieces `getAverageGroundLevel`) | legitimate: creating the provider needs the level object and the dimension key; `WorldInfo` reads only dimension and server constants | `mcc$levelObject()` in `mcc$get$provider` and `WorldInfo.level()` |
| 1.7.10 `World.generatorBiome` (from `getBiomeGenForCoords`, `ChunkProviderGenerate.populate`) | off-thread biome fallback | legitimate: reads the generator's biome source and sampler, not a chunk | `mcc$levelObject()` |
| 1.7.10 `LegacyHosts.adopt`, `VanillaEntities.create` (Witchery guard, item frame; Traincraft minecart) | entity constructors during village generation | legitimate: entity construction needs the Level object | `mcc$levelObject()` |
| 1.7.10 `EntityBridge.spawn` | `World.spawnEntityInWorld` in village components | legitimate: the entity is added through `world.mcc$level()` (the region while generating); the level object is used for the entity lookup and host creation only | `mcc$levelObject()` |
| 1.12.2 `VanillaEntities1122.create` (item frame) | `EntityHanging.<init>` in a village piece | legitimate: entity construction | `mcc$levelObject()` |
| 1.12.2 `StructureVillagePieces$Village.spawnVillagers` | mod village piece | legitimate: constructs the villager from the Level; `finalizeSpawn` and `addFreshEntity` already go through `mcc$access()` | `mcc$levelObject()` |
| 1.12.2 `ModernFeatures1122.place` and `placeFeature` | `WorldGenBigTree.generate` etc. | legitimate: only `getChunkSource().getGenerator()`; the feature places into the `WorldGenLevel` region | `mcc$levelObject()` |
| 1.12.2 `BlockFaces1122.vanillaSturdy` (via `IBlockState.isSideSolid`) | Actually Additions `WorldGenLushCaves` | DEFECT: `isFaceSturdy(realLevel, pos, face)` reads blocks (and shapes) from the real level | now `mcc$access()` |

Known remaining potential reports (not seen in these suites, not changed): 1.7.10 `World` time, weather, spawn point, game rules, save handler,
scoreboard (`mcc$fullLevel()`): level constants; they will be reported (and classified) when a generator reaches them.
