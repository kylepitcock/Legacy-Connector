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
| `events.skip` | empty | Comma separated legacy event handlers that are not registered, as `full.ClassName#method` or `full.ClassName#*` (to find which legacy handler draws or does something unwanted) |

| `energy.fePerEu` | `4` | FE per IC2 EU for IC2 sinks, sources and electric items exposed as NeoForge energy |
| `energy.fePerMj` | `10` | FE per BuildCraft MJ for BuildCraft power receptors (BuildCraft's own 1 MJ = 10 RF) |

Memory switches (`perf.*`, see [Performance](Performance.md#memory)); each can be turned off on its own:

| Key | Default | Effect |
| --- | --- | --- |
| `perf.releaseUnloadedLevels` | `true` | Legacy views of a level (World, GameRules, MapStorage, chunk tickets, entity and tile views) are released when it unloads, those of a server (MinecraftServer, Scoreboard, 1.12 GameRules) when it has stopped |
| `perf.sweepRemovedPlayers` | `true` | Views of players removed for good (the old player object after a death and respawn, a player that logged out) are swept, after two sweeps in a row |
| `perf.sweepDeadViews` | `true` | Views of closed connections (1.7.10 `NetworkManager`), of legacy inventories whose block entity was removed or unloaded, and 1.12.2 views of the game's block entities are swept |
| `perf.weakStackViews1122` | `false` | 1.12.2 item stack views without capabilities are held weakly, so a stack nobody holds is collected with its view |
| `perf.releaseLoaderData` | `true` | The loader drops its translation tables (legacy hierarchy, methods, fields, MCP names, library class infos) after mod discovery (read by the loader at startup) |

Ores (full blocks whose name has `ore` as a word) get a hidden `<name>_deepslate` twin automatically: the same legacy block (drops, hardness, behaviour) with its stone background replaced by deepslate. Where a generator places an ore into deepslate (the deep layer, and the deepslate band just above y=0) the twin is placed instead. Ores with tile entities are not duplicated. The ore texture of the normal block can also sit on a deepslate background (`oreBackgrounds.stone=deepslate`).
