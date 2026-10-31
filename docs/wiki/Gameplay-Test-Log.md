# Gameplay test log

What has been exercised in a real client (not just loaded), so contributors know where coverage is thin. Back to [Home](Home.md). The method is in [Development](Development.md): `tools/dev/quick-shots.sh` with `SCENARIO=control`, then `tools/dev/js.sh` probes against the running game (place every block of a namespace with a fake player, spawn every entity type, drive a machine and read its tile state).

## 1.7.10

| Area | Checked |
|---|---|
| Tech | IC2 generators, macerator, compressor, solar, BatBox and cable networks; BuildCraft engines, mining well and pipes into chests; Thermal Expansion dynamo feeding a pulverizer; Railcraft, Big Reactors, MineFactory Reloaded placement; Reika mods (RotaryCraft engine and shafts, ElectriCraft generator and wire network, ReactorCraft, ChromatiCraft, GeoStrata) |
| Magic | Thaumcraft crucible (essentia from items), infusion altar (an Obsidian Rod infusion finishes and drains the jars), all 102 shaped arcane recipes craft with a charged wand, all recipe registries valid, Thaumonomicon GUI, Arcane Lamp, golems spawn; Witchery Witches' Oven, spinning wheel (on altar power), kettle, distillery and brewing cauldron; Botania flower to spreader mana |
| Storage and world | Iron Chest, Extra Utilities, Chisel, Carpenter's Blocks, Tinkers' Construct, OpenBlocks, AgriCraft crop sticks, Forestry apiary with flowers (queen produces combs), Biomes O' Plenty, Natura, Twilight Forest |
| Space and mobs | Galacticraft planets and Moon (oxygen warning), Traincraft rolling stock (505 entity types spawn and render), Fisk's Superheroes (the Flash speed ability, armor right-click equip), HBM Nuclear Tech |
| Fisk's Superheroes (headless server probes, `tools/dev/fisk`) | All 67 suits equip and are recognised; every interaction fires without errors; repulsor blast, heat vision, energy projection, sonic waves (damage on a golem), web rope flies and sticks, Captain America's shield is thrown, hits and returns, Flash speed attribute and step height, size changes resize the hitbox and eye, damage resistances and fall resistance, healing factor, Moon/Quantum gravity, slow-motion tick rate, 207 shaped/shapeless recipes plus the tachyon recipe craft, 175 items right-click and 102 blocks place without errors. Client rendering is not covered. |
| Network mods | Applied Energistics 2: parts (buses, panels, terminals) draw with their textures; a glass cable between a controller and a drive connects and the network shows its powered colour, also with IC2 loaded |
| Dimensions | Witchery (dream, torment, mirror), Thaumcraft Outer Lands, Twilight Forest, Galacticraft Moon, Mars and asteroids, OreSpawn; entering every dimension in turn does not hang the server thread |

## 1.12.2

| Area | Checked |
|---|---|
| Content | Mekanism enrichment chamber turns ore into dust using energy; Tinkers' Construct tool table and crafting station open; Aether renders |
| Dimensions | The Aether dimension (`legacyconnector:dim_4`) generates and renders, and the Aether portal teleports a player into it |
| Events | Player and living drops events reach mods (Tombstone takes a dying player's items) |
| Known gaps | Mekanism OBJ models show untextured, some placement results fail, OpenComputers fails in init (LWJGL 2 classes), mod biomes are plains, container GUI backgrounds and JEI overlay textures are missing |

## Recurring causes found this way

- A Forge hook the game calls before normal use (`onItemUseFirst`) must be called by probes too; items such as AgriCraft crop sticks and Thaumcraft golem placers only work through it.
- Legacy code that compares `instanceof` against a vanilla class (`ItemFood`, `ItemShears`) needs the vanilla item objects to be instances of those shim classes.
- Test network mods across separate server ticks: placing a controller, cables and a machine inside one tick (a single probe call) leaves AE2 tiles without their grid nodes, which looks like a bug but is not what a player does.
- Hard-coded height limits in legacy code (a lamp's minimum y of 5) misbehave in the deeper modern world.

## Thaumcraft 4.2.3.5 headless server audit

Driven with `./gradlew runServerSubTest -PselfTests=false -PserverDevScript=<dir>` (the server evaluates `<dir>/*.js` on its thread, see `core/ServerDevScript`; a mock `ServerPlayer` with a swallowing packet listener stands in for a client). Not a client check: rendering, GUIs and HUDs are not covered.

| Area | Result |
|---|---|
| Aura nodes and wand | A node generates (aspects, base aspects, regeneration over ticks, NBT and description packet). The wand attached to nothing before: a legacy ray trace now hits a block through its bounds when its selection box is empty (nodes), so a wand held on a node draws vis and drains it |
| Scanning, research notes | `ScanManager.completeScan` records the block and fills the aspect pool; all 162 research entries create parseable research notes (hex grid NBT) |
| Essentia | Alchemy furnace fills an alembic; tubes move essentia between two jars when the target has the higher suction (a label); every Thaumcraft tile survives 60 updates, an NBT round trip and a description packet |
| Taint | A biome set with `Utils.setBiomeAt` reads back as set (it read the fuzzy zoomed neighbour before, so taint fibres died at once); fibres then spread |
| Golems and trunk | A gather-core golem picks up dropped items into its home chest; the Travelling Trunk teleports to its owner beyond 20 blocks (needs solid ground there) |
| Tools | Thaumium, elemental and void tools have the right tool classes, harvest levels and speeds; the elemental axe fells the furthest log, the elemental shovel digs 3x3; mod hoes (Thaumium, elemental) now till grass and dirt |
| Worldgen | Infused ore, greatwood and silverwood trees, magical forest and tainted land biomes, the Outer Lands (eldritch traps, crystals, crab spawners generate) |
| Not verified | Arcane Lamp below y=5 (the lamp's own floor), goggles HUD and Thaumometer display, golem black spots, vis relay networks, thaumatorium recipes, hand mirror transport, eldritch guardian and warden fights, the sanity checker overlay (client only) |
