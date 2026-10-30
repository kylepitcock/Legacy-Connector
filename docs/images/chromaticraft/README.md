# ChromatiCraft (1.7.10) test screenshots

ChromatiCraft V33a with DragonAPI V33b, on NeoForge 1.21.1 through Legacy Connector (master, 2026-10-30), in a real client
(`SCENARIO=control WORLD=minecraft:normal tools/dev/quick-shots.sh`). Both mods load: 1,197 items, 118 blocks, 26 entity types.

| File | What it shows |
|---|---|
| `blocks.png`, `blocks_close.png` | A showcase built with one row per block type, each in all its metadata variants: crystals, lamps, runes, pylon structure blocks, tiered ores, rainbow and dye leaves, decorative flowers, glow logs, tiered plants |
| `blocks_night.png` | The same rows at night: the runes glow; the crystals give off no light (in 1.7.10 they are light sources) |
| `lexicon.png` | The Lexicon, ChromatiCraft's guide book (frame and tabs; a new player sees locked, scrambled entries) |
| `ender_forest.png` | ChromatiCraft's Ender Forest biome in the overworld, with a dye tree |

Fixed on the way (all generic): DragonAPI's shader registry, sound categories, paulscode stream thread, the vanilla renderer
map, the order of item subtype discovery, SRG-named shim methods, FML message buffers and the tile entity update packet's
fields. Still open: crystal light emission, one block's inventory icon (`BlockLightedLeaf`) and ChromatiCraft's pylon
generator in the deep layer.
