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

Ores (full blocks whose name has `ore` as a word) get a hidden `<name>_deepslate` twin automatically: the same legacy block (drops, hardness, behaviour) with its stone background replaced by deepslate. Where a generator places an ore into deepslate (the deep layer, and the deepslate band just above y=0) the twin is placed instead. Ores with tile entities are not duplicated. The ore texture of the normal block can also sit on a deepslate background (`oreBackgrounds.stone=deepslate`).
