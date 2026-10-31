# Configuration

Back to [Home](Home.md).

Settings live in `config/legacyconnector.properties`, written with defaults and comments on first start.

| Key | Default | Effect |
| --- | --- | --- |
| `oreBackgrounds.enabled` | `true` | Replace the old stone/netherrack/end stone behind legacy ore textures |
| `oreBackgrounds.stone`, `.netherrack`, `.endStone` | `stone`, `netherrack`, `end_stone` | Vanilla block texture used instead (e.g. `deepslate`, `tuff`), or `legacy` to keep the old rock |
| `modernizeTextures` | `true` | Replace old vanilla art inside legacy mod textures with the modern vanilla textures: exact copies of old block, item and GUI textures, and old backgrounds (smooth stone, cobblestone, planks, bricks, ...) mods painted their art on (recognised from fingerprints, the mod's own art stays) |
| `deepLayer.enabled` | `true` | Run legacy world generators a second time so ores also spawn below y=0 |
| `deepLayer.excludeGenerators` | empty | Comma separated generator class prefixes that must not run below y=0 |

| `energy.fePerEu` | `4` | FE per IC2 EU for IC2 sinks, sources and electric items exposed as NeoForge energy |
| `energy.fePerMj` | `10` | FE per BuildCraft MJ for BuildCraft power receptors (BuildCraft's own 1 MJ = 10 RF) |

Ores (full blocks whose name has `ore` as a word) get a hidden `<name>_deepslate` twin automatically: the same legacy block (drops, hardness, behaviour) with its stone background replaced by deepslate. Where a generator places an ore into deepslate (the deep layer, and the deepslate band just above y=0) the twin is placed instead. Ores with tile entities are not duplicated. The ore texture of the normal block can also sit on a deepslate background (`oreBackgrounds.stone=deepslate`).
