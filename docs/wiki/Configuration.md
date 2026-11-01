# Configuration

Back to [Home](Home.md).

Settings live in `config/legacyconnector.properties`, written with defaults and comments on first start.

| Key | Default | Effect |
| --- | --- | --- |
| `oreBackgrounds.enabled` | `true` | Replace the old stone/netherrack/end stone behind legacy ore textures |
| `oreBackgrounds.stone`, `.netherrack`, `.endStone` | `stone`, `netherrack`, `end_stone` | Vanilla block texture used instead (e.g. `deepslate`, `tuff`), or `legacy` to keep the old rock |
| `modernizeTextures` | `true` | Replace old vanilla art inside legacy mod textures with the modern vanilla textures: exact copies of old block, item and GUI textures, and old backgrounds (smooth stone, cobblestone, planks, bricks, ...) mods painted their art on (recognised from fingerprints, the mod's own art stays) |
| `deepLayer.enabled` | `true` | Spread legacy ores down to the bottom of the world: generators run a second time shifted below y=0, and the ores of the band both runs cover are split between them, so the total stays the original mod's (as one placed feature per generator, `<modid>:legacy_worldgen_deep/<generator class>`; `false` generates none, see [World generation](World-Generation.md#the-deep-layer)) |
| `deepLayer.excludeGenerators` | empty | Comma separated generator class prefixes that must not run below y=0 (their deep feature is not generated) |
| `blocks.flattenSubtypes` | `true` | Legacy block subtypes become blocks of their own, as in the 1.13 flattening: the item `mod:name_3` places and names the block `mod:name_3`, so modern mods that use an item's block (display stands, framed and copycat blocks) get the right subtype. `false`: one block per legacy block as before; the subtype blocks stay registered and their blocks in saved worlds turn back into the base block when the chunk loads (see [Bridges](Bridges.md#block-subtypes-flattening)) |
| `villages.legacyBuildings` | `true` | Legacy mods' village buildings join the vanilla village house pools (with their 1.7.10 share); `false` leaves villages vanilla |
| `compat.rocketnautics.plasmaEveryTicks` | `4` | Rocketnautics (Create Aeronautics) rebuilds its star plasma sky texture every tick even with its custom sky off (about 11% of frame time); it is rebuilt every N ticks instead. `1` restores the original |
| `perf.lazyPackMetadata` | `true` | The generated legacy data and asset packs build no content when the game opens them only to read their metadata (once per pack scan; every scan built all files). See [Performance](Performance.md#world-load) |
| `perf.biomeJarCache` | `true` | The vanilla biome templates and biome tags that legacy biomes are generated from are read from the game's jars once per session, not on every data pack build. See [Performance](Performance.md#world-load) |
| `perf.asyncBiomeSample` | `true` | The "Biome sample around spawn" log line (1089 biome lookups after the server started) is computed in the background, not on the server thread the joining player waits for |
| `events.skip` | empty | Comma separated legacy event handlers that are not registered, as `full.ClassName#method` or `full.ClassName#*` (to find which legacy handler draws or does something unwanted) |

| `energy.fePerEu` | `4` | FE per IC2 EU for IC2 sinks, sources and electric items exposed as NeoForge energy |
| `energy.fePerMj` | `10` | FE per BuildCraft MJ for BuildCraft power receptors (BuildCraft's own 1 MJ = 10 RF) |
| `perf.staticTileEntities` | `true` | Legacy tile entities that never ticked in their version (1.7.10: `canUpdate()` false or no `updateEntity()`; 1.12.2: not `ITickable`; or crashed while ticking) get no modern ticker |
| `perf.blockLookupCache` | `true` | The legacy block of a modern block and the 1.7.10 metadata of a vanilla state are kept on the block and the state (no lock per legacy block read) |
| `perf.itemLookupCache` | `true` | The 1.7.10 item of a vanilla or modern-mod item is kept on the item (no map lookup per legacy stack view) |
| `perf.worldViewCache` | `true` | The 1.7.10 world view of a level is kept on the level (no synchronized map lookup per legacy callback) |
| `perf.entityViewCache` | `true` | The 1.7.10 view of an entity is kept on the entity (no weak map lookup per legacy event or callback) |
| `perf.reuseBlockPos` | `true` | 1.7.10 `getBlock`/`getBlockMetadata` read with one mutable position per thread instead of a new `BlockPos` per call |
| `perf.hookOverrideCache` | `true` | "Does this legacy entity class override hook X" is answered per class without building a key or lambda per call |

The `perf.*` switches are read once at startup; each turns one tick optimization off if it ever misbehaves. They are all on by default
because each keeps 1.7.10/1.12.2 behaviour; see [Performance](Performance.md#server-tick-switches).

Memory switches (`perf.*`, see [Performance](Performance.md#memory)); each can be turned off on its own:

| Key | Default | Effect |
| --- | --- | --- |
| `perf.releaseUnloadedLevels` | `true` | Legacy views of a level (World, GameRules, MapStorage, chunk tickets, entity and tile views) are released when it unloads, those of a server (MinecraftServer, Scoreboard, 1.12 GameRules) when it has stopped |
| `perf.sweepRemovedPlayers` | `true` | Views of players removed for good (the old player object after a death and respawn, a player that logged out) are swept, after two sweeps in a row |
| `perf.sweepDeadViews` | `true` | Views of closed connections (1.7.10 `NetworkManager`), of legacy inventories whose block entity was removed or unloaded, and 1.12.2 views of the game's block entities are swept |
| `perf.weakStackViews1122` | `false` | 1.12.2 item stack views without capabilities are held weakly, so a stack nobody holds is collected with its view |
| `perf.releaseLoaderData` | `true` | The loader drops its translation tables (legacy hierarchy, methods, fields, MCP names, library class infos) after mod discovery (read by the loader at startup) |
| `perf.cullLegacyParticles` | `true` | Legacy particles drawn by the built-in particle classes are skipped outside the view (pixel-identical) |
| `perf.cullModParticles` | `false` | The same for particles mods draw themselves (a beam reaching into the view from outside could vanish) |
| `perf.particleSetting` | `true` | 1.7.10's `particleSetting` follows the vanilla particle option, so mods spawn fewer particles on Decreased/Minimal as in 1.7.10 |
| `perf.shareBlockModels` | `true` | Metadata values of a legacy block with identical generated models share one model file and one baked model |
| `perf.shareItemModels` | `true` | Legacy items with identical generated item models keep one baked model (off by itself with ModernFix) |
| `perf.packSpill` | `true` | Generated pack files move from the heap to one scratch file under `.legacyconnector/pack-spill` once read |
| `perf.cacheTextureLocations` | `true` | A legacy mod texture location remembers the modern location it binds |

See [Performance](Performance.md#client-memory-and-frame-time-switches) for what each `perf.*` switch saves.

Ores (full blocks whose name has `ore` as a word) get a hidden `<name>_deepslate` twin automatically: the same legacy block (drops, hardness, behaviour) with its stone background replaced by deepslate. Where a generator places an ore into deepslate (the deep layer, and the deepslate band just above y=0) the twin is placed instead. Ores with tile entities are not duplicated. The ore texture of the normal block can also sit on a deepslate background (`oreBackgrounds.stone=deepslate`).
