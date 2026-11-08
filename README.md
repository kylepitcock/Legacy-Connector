<p align="center"><img src="docs/images/legacyconnector.png" alt="Legacy Connector logo" width="96" height="96"></p>

# Legacy Connector

[![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/vC6x4Kwwdq) [![CurseForge](https://img.shields.io/badge/CurseForge-F16436?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/legacy-connector) [![Modrinth](https://img.shields.io/badge/Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/mod/legacyconnector) [![Ko-fi](https://img.shields.io/badge/Ko--fi-FF5E5B?style=for-the-badge&logo=kofi&logoColor=white)](https://ko-fi.com/kylepitcock)

Run **unmodified legacy Forge mods on NeoForge 1.21.1**. Drop 1.7.10, 1.12.2, 1.6.4, 1.5.2, 1.4.7, Forge 1.20.1, or (alpha) Beta 1.7.3 mod jars into your `mods` folder next to
this mod; it is translated at load time, the same way [Sinytra Connector](https://github.com/Sinytra/Connector)
runs Fabric mods on NeoForge. The original jar is never modified. Legacy jars live in the folder of their Minecraft version (`mods/1.7.10/`, `mods/1.12.2/`,
`mods/1.6.4/`, `mods/1.5.2/`, `mods/1.4.7/`, `mods/1.20.1/`, `mods/b1.7.3/`): ones you drop into `mods/` itself are moved there for you on launch
(`legacy.autoSortMods=false` turns that off). It matters for mods that bundle their own Mixin (common in 1.12.2: Spartan Weaponry, MixinBooter mods):
NeoForge loads such jars from `mods/` as launcher plugins before they can be translated, and the game stops with
"Module ... contains package org.spongepowered..." (move them to the folder before the first launch).

> **Status: beta (0.4.x).** The point is mods that **only exist on these old versions** and never got a 1.21.1 port.
> 1.7.10 is in beta: Thaumcraft 4, Witchery, HBM's Nuclear Tech, the Reika mods (RotaryCraft, ReactorCraft, ChromatiCraft),
> OreSpawn, Fisk's Superheroes, Morph, and Galacticraft with its addons (per-planet gravity, silent space) run with their blocks, items,
> entities, recipes, GUIs, world generation, and dimensions, and large tech packs like GT New Horizons load most of their mods.
> 1.12.2 is in beta: Atum 2, The Midnight, The Betweenlands, Scape and Run: Parasites, and Abyssalcraft load and play in a real
> client with their own models, GUIs, mobs, dimensions, and world generation.
> 1.20.1 (Forge) is in alpha: mods are remapped in place onto NeoForge 1.21.1. Terramity (with its GeckoLib bosses) runs in a real client and Another Furniture registers its content; most other 1.20.1 mods still have unresolved references. It is on by default and does nothing unless a Forge 1.20.1 jar is installed.
> 1.6.4 is in beta (2026-11-08); 1.5.2 and 1.4.7 are new in 0.4 and in alpha: RedPower 2, BuildCraft, ComputerCraft, and Applied Energistics (1.4.7), Twilight Forest and
> Applied Energistics 1 (1.5.2), Tinkers' Construct, Mo' Creatures, and Goblins! (1.6.4), and many more run in a real client. Some
> features are still missing (see [Known compatibility issues](docs/wiki/Compatibility-and-Status.md#known-compatibility-issues)); back up your worlds before trying a new
> mod set.

Legacy content enters the game through the same hooks a modern mod uses: mod biomes through the pack's biome placement
(a TerraBlender region; TerraBlender is bundled), world generators as placed features in biome modifiers, village buildings
in the vanilla village pools, structure chest loot through loot tables, and items and machines as capabilities. So it works together
with the pack's other mods instead of being painted over them, and data packs can tune it like any modern content.

**Compatibility list and reports: https://kylepitcock.github.io/Legacy-Connector/** (check a mod, or report how it worked for you).

Screenshots of legacy mods running on 1.21.1 are at the [end of this page](#screenshots).

## Status

| Legacy version | Status |
|---|---|
| 1.7.10 | **Beta.** Mods with no modern version are the focus: Thaumcraft 4, Witchery, HBM's Nuclear Tech, OreSpawn, Fisk's Superheroes, Morph, the Reika mods (DragonAPI, RotaryCraft, ReactorCraft, ElectriCraft, ChromatiCraft) and Galacticraft with its addons run, with their mod biomes, dimensions and entities. The Betweenlands 1.0.6 loads and its dimension generates (its mob spawns are still being worked on). In-game self tests of the GT New Horizons pack and a 130-jar list rate 166 mods as working and 57 as partly working; 20 fail during loading. |
| 1.12.2 | **Beta (since 2026-11-07; issue #64).** Old-only mods are the focus: Atum 2, The Midnight, The Betweenlands, Scape and Run: Parasites, and Abyssalcraft load and play in a real client with their own models, GUIs, mobs and world generation, and mod dimensions (Atum, the Midnight, the Betweenlands) can be entered. Botania, Chisel, Thermal Foundation and Expansion, Tinkers' Construct, Immersive Engineering, Mekanism, Astral Sorcery, Actually Additions, Electroblob's Wizardry and Recurrent Complex now render and load (2026-11-06 sweeps); Applied Energistics 2, EnderIO, Industrial Foregoing and Extra Utilities 2 still have rendering defects. Mod advancements, Patchouli books through the modern Patchouli, and containers that move their slots at runtime work. |
| 1.6.4 | **Beta (2026-11-08).** A thin layer over the 1.7.10 support: numeric block/item ids, `Icon`, the old packet API, old tick handlers, `ModLoader` mods. Of 106 mods rated so far, 44 have working basics, 1 is mostly playable (Tinkers' Construct, smeltery included), 17 partly work, 33 start but are untested (libraries and add-ons) and 11 fail. Played in a real client in survival, with shaders: Tinkers' Construct, Mo' Creatures, Lycanites Mobs, OpenBlocks, Minechem, IndustrialCraft 2, Forestry, Witchery, Twilight Forest, ExtrabiomesXL, Ancient Warfare, MythicalCraft, Galaxy Warfare, TragicMC, Sorcery, Kitchens, Mad Science, The Lion King and others ([screenshots](docs/screenshots/old-versions/README.md), [estimate](docs/wiki/Legacy-1.6.4-Estimate.md)). Missing: Thaumcraft 4 and some mod-specific defects. |
| 1.5.2 | **Alpha (2026-11-07).** A small delta over 1.6.4 (same numeric ids and packet API; no `EntityLivingBase`, string texture paths). Of 54 mods rated so far, 19 have working basics, 14 partly work, 18 start but are untested (libraries and add-ons with nothing to play) and 3 fail. Grimoire of Gaia, Twilight Forest, Applied Energistics 1, Resource Engineering, BuildCraft, BiblioCraft, Mystcraft, Forestry (apiary screen), ComputerCraft, RedLogic, Liquid XP, MiscPeripherals, Backpacks, Ender Storage, Chicken Chunks, Dimensional Anchors and InfiniTubes run in a real client, with recipes crafted in the 3x3 grid. ForgeMultipart and Block Helper load; client-only mods are skipped quietly on a server. Better Than Wolves and other patched-`minecraft.jar` mods cannot work (they replace vanilla classes; reported as "not supported: patched-game jar mod") ([screenshots](docs/screenshots/old-versions/README.md), [estimate](docs/wiki/Legacy-1.5.2-Estimate.md)). |
| 1.4.7 | **Alpha (2026-11-07).** After 1.5.2: the same numeric ids and old packet API, with obfuscated class names, sprite-sheet textures (cut into tiles), texture animations and `ModLoader`/`BaseMod` mods. Of 85 mods rated, 47 have working basics (30 load, 5 partly work, 3 fail with a plain reason): RedPower 2, BuildCraft and Twilight Forest render and run in a real client; ComputerCraft (computers and turtles with a working terminal) and Applied Energistics (a powered ME network stores and lists items) work. Better Than Wolves cannot work ([screenshots](docs/screenshots/old-versions/README.md), [estimate](docs/wiki/Legacy-1.4.7-Estimate.md)). Survival checks since: RedPower 2 wires, gates, transposer, tubes, project table and ore generation, BuildCraft pipes and engines, AE terminal and drive screens; the 65-file corpus loads (Better Than Wolves and two JInput mods excepted); RedPower rubber trees generate in jungles. |
| 1.20.1 | **Alpha (2026-11-08; on by default, `experimental.forge1201=false` turns it off).** Forge 1.20.1 mods are remapped in place onto NeoForge 1.21.1 (SRG to Mojang names, Forge to NeoForge): the renames, the Minecraft 1.20.1 to 1.21.1 migration, Forge API facades, data conversion, datagen stubs and library bridges (Architectury, Curios, Patchouli use the native 1.21.1 builds). Terramity (a large content mod with GeckoLib bosses) runs in a real client and registers 207 blocks, 754 items and 81 entity types on a server; Another Furniture registers its content too. Most other mods still have unresolved references: on the 506 mods of an ATM 9 pack about 80 link, and most of the rest have a native 1.21.1 version anyway. The [compatibility site](https://kylepitcock.github.io/Legacy-Connector/#/automated/1.20.1) has a 1.20.1 tab with the automated link check (see the [estimate](docs/wiki/Legacy-1.20.1-Estimate.md)). |
| 1.19.2, 1.18.2 | Planned (after 1.20.1; renames and diffs over the same pipeline). |
| 1.16.5 | Planned (after 1.18.2). |
| 1.2.5 | Planned (after 1.16.5; needs `ModLoader`/`BaseMod` emulation). |
| b1.7.3 | **Alpha (server-tested).** Beta 1.7.3 ModLoader / Forge 1.0 mods in `mods/b1.7.3/` (jars or zips) are translated and their blocks, items and recipes load with their own textures, the blocks' own behaviour (placing, ticks, clicks, redstone), their tile entities and their GUIs (machines, furnaces, crafting tables as modern menus), one creative tab per mod (IndustrialCraft 2, NetherCraft, Portal Gun, RedPower, Minefactory Renewed). Fluid buckets, tool/sword durability and arrows with constant custom textures are bridged. Supported living-mob families run their original callbacks and models, with native spawning, sound and GuiAPI settings; general entities and world generation remain unfinished. Tested so far in headless server runs with Mo' Creatures and MoreCreeps (their results are on the compatibility site); no graphical client test yet. ([details](docs/wiki/Legacy-b1.7.3.md)). |

Order and reasons: [Version roadmap](docs/wiki/Version-Roadmap.md). Not planned: 1.8 to 1.11.2 and the Alpha/early-Beta jar-mod era; Beta 1.7.3 ModLoader mods were added at a contributor's request (row above).

Legacy tile entities and items are NeoForge item, fluid and energy capabilities by default (RF and Forge Energy, IC2 EU, BuildCraft MJ, Forge fluids, inventories), so hoppers, Create, and modern cables work with them ([details](docs/wiki/Addon-API.md#capabilities)). Outdated libraries are bridged to their modern equivalents instead of being loaded: Baubles to Curios, Waila to Jade, NEI to JEI, Patchouli books to Patchouli, and CoFH RF / Redstone Flux to NeoForge energy ([Bridges](docs/wiki/Bridges.md)).

Per-mod results, known issues, and what is not supported: [Compatibility and status](docs/wiki/Compatibility-and-Status.md). After
launching, read `logs/legacyconnector-report.txt`: for every legacy jar it lists the detected version and mod ids, references that
could not be linked, and which legacy APIs were synthesized.

## Configuration

Settings live in `config/legacyconnector.properties`, written with defaults and comments on first start.

| Key | Default | Effect |
| --- | --- | --- |
| `oreBackgrounds.enabled` | `true` | Replace the old stone/netherrack/end stone behind legacy ore textures |
| `oreBackgrounds.stone`, `.netherrack`, `.endStone` | `stone`, `netherrack`, `end_stone` | Vanilla block texture used instead (e.g. `deepslate`, `tuff`), or `legacy` to keep the old rock |
| `modernizeTextures` | `true` | Replace old vanilla art inside legacy mod textures with the modern vanilla textures: exact copies of old block, item and GUI textures, and old backgrounds (smooth stone, cobblestone, planks, bricks, ...) mods painted their art on (recognised from fingerprints, the mod's own art stays) |
| `textures.modernLeaves` | `true` | Rebuild legacy leaf textures that recolour the old vanilla leaves in the modern leaf pattern, in the mod's colours |
| `deepLayer.enabled` | `true` | Spread legacy ores down to the bottom of the world (below y=0) without adding any: the total per chunk stays the original mod's |
| `deepLayer.excludeGenerators` | empty | Comma separated generator class prefixes that must not run below y=0 |
| `blocks.flattenSubtypes` | `true` | Each legacy block subtype that has its own item becomes its own block (`mod:name_3`), as in the 1.13 flattening |
| `events.skip` | empty | Legacy event handlers to leave unregistered (`full.ClassName#method`), for tracking down a handler that misbehaves |

All keys and the deepslate ore twins are described on the [Configuration](docs/wiki/Configuration.md) page.

## FAQ

**What does it do?** It runs mods that were built for older Minecraft versions (Forge 1.4.7, 1.5.2, 1.6.4, 1.7.10, 1.12.2, and 1.20.1 (alpha)) on NeoForge 1.21.1, next to modern mods. Legacy jars are translated when the game loads; the original file is never modified.

**How is it different from Retromod and Retrofit?** They are different tools. These summaries come from their own project pages, so check those for the details.
- [Retromod](https://modrinth.com/mod/retromod) also rewrites old mods' bytecode when the game loads. It targets newer Minecraft versions across Fabric, Forge, and NeoForge, and its pages recommend it for content, quality-of-life, and library mods, not for mods that hook deep into the game (Create, Tinkers, Applied Energistics, and similar).
- [Retrofit](https://github.com/Astryxx/Retrofit) (Astryxx) is an interoperability mod: it lets 1.7.10 and 1.20.1 mods work together (energy systems such as RF and IC2, ore dictionary, and tags). It does not load old mods onto a modern game.
- Legacy Connector is for NeoForge 1.21.1 only. For each old version it provides the old game's API itself (the engine, ids, world generation, packets, and rendering of the old blocks and items), so mods that use the game's internals can run: Thaumcraft, BuildCraft, Thermal Expansion, RedPower, and many more. It bridges outdated libraries to modern equivalents (Baubles to Curios, Waila to Jade, NEI to JEI, and RF to NeoForge energy) and exposes legacy containers as NeoForge capabilities.

**Where do I put the mods?** Each legacy version has its own folder: `mods/1.7.10/`, `mods/1.12.2/`, `mods/1.6.4/`, `mods/1.5.2/`, `mods/1.4.7/`, `mods/1.20.1/`, `mods/b1.7.3/`. Legacy jars and zips you drop into `mods/` itself are moved there for you on launch (`legacy.autoSortMods=false` turns that off).

**Which versions work, and how well?** 1.7.10, 1.12.2, and 1.6.4 are in beta; 1.5.2, 1.4.7, 1.20.1, and Beta 1.7.3 are in alpha. The [Status table](#status) has the details, and the [compatibility site](https://kylepitcock.github.io/Legacy-Connector/) lists results per mod.

**Will mod X work?** Check the compatibility site. After a launch, `logs/legacyconnector-report.txt` lists each legacy jar, what was detected, and any references that could not be translated or are only stubbed. Mods that only patch the old engine (performance patches, id extenders) are skipped on purpose, and coremods that patch Minecraft's own classes are not supported.

**Does it work on a dedicated server?** Yes. Legacy mods run on a dedicated server too; client-only mods are skipped there.

**Does it work on Fabric or Forge?** No. It is a NeoForge 1.21.1 mod. (Fabric mods on NeoForge are what Sinytra Connector does; Legacy Connector is the equivalent for old Forge versions.)

**Why does the game stop with "Module ... contains package org.spongepowered..."?** A 1.12.2 jar bundles its own Mixin. Put it in `mods/1.12.2/` before the first launch.

**Something broke. How do I report it?** Open a [GitHub issue](https://github.com/kylepitcock/Legacy-Connector/issues) and attach `logs/latest.log` and `logs/legacyconnector-report.txt` (and the crash report if there is one). Please say the Legacy Connector version, the mod and its Minecraft version, and what you did. Crash reports of a translated mod belong here, not with the mod's author.

## Documentation

Technical documentation is in [`docs/wiki`](docs/wiki/Home.md) (start at [Home](docs/wiki/Home.md)):
[Architecture](docs/wiki/Architecture.md), [Bridges](docs/wiki/Bridges.md), [World generation](docs/wiki/World-Generation.md),
[Mod dimensions](docs/wiki/Mod-Dimensions.md), [Entities](docs/wiki/Entities.md), [Rendering](docs/wiki/Rendering.md),
[Create and Sable](docs/wiki/Create-and-Sable.md), [Changing legacy recipes](docs/wiki/Recipes.md),
[Performance](docs/wiki/Performance.md), [Compatibility and status](docs/wiki/Compatibility-and-Status.md) and
[Development](docs/wiki/Development.md). Per-mod results are on the
[compatibility site](https://kylepitcock.github.io/Legacy-Connector/).

## Writing addons for legacy mods

Addons for legacy mods (a mod that registers content for, or hooks into, a translated legacy mod of any supported version: 1.4.7, 1.5.2, 1.6.4, 1.7.10, 1.12.2, or 1.20.1) are described in
[Addon API](docs/wiki/Addon-API.md).

## Screenshots

All taken in a real game client (NeoForge 1.21.1) with unmodified legacy mods that have no modern version, running next to modern content. The older versions (1.6.4, 1.5.2, 1.4.7) have their own [screenshot gallery](docs/screenshots/old-versions/README.md); a few of them are at the end of this section.

A Thaumcraft 4 workshop: infusion altar with items on its pedestals, crucible over lava, alchemical furnace with alembics, essentia
jars, arcane workbench, research table, an aura node over a stabiliser, infused stones with crystal clusters, and greatwood and
silverwood trees grown by Thaumcraft's own generators:

![A Thaumcraft workshop](docs/images/thaumcraft-workshop.png)

**A Create Aeronautics airship (Sable physics) carrying 1.7.10 blocks: Thaumcraft essentia jars, crucible and arcane workbench, an Iron Chest, a RotaryCraft engine, and ChromatiCraft crystals**

![A Create Aeronautics airship carrying legacy blocks](docs/images/create-aeronautics-ship.png)

The Thaumonomicon, drawn by Thaumcraft's own font renderer and book code, and the Thaumometer held in both hands after scanning
stone (the Terra aspect in its lens, research points gained in the corner):

![The Thaumonomicon](docs/images/thaumonomicon.png)

![Scanning with the Thaumometer](docs/images/thaumometer.png)

A Witchery ritual site: chalk ritual circle around a heart glyph, candelabras, the altar with a chalice and crystal ball, a witch's
oven with its fume funnel, kettle, distillery, and spinning wheel (all drawn by Witchery's own tile entity renderers), a crop patch of
belladonna, mandrake, snowbell, wolfsbane, garlic, and wormwood, rowan saplings, a coven witch, a mandrake, and a toad:

![A Witchery ritual site](docs/images/witchery-ritual.png)

Fisk's Superheroes: Doctor Octopus's suit with its tentacles deployed, and the suit's abilities (tentacle jab, grab, and strike) on
Fisk's own HUD:

![Fisk's Superheroes Doctor Octopus](docs/images/fisk-doc-ock.png)

OreSpawn's The King on the plains, with a modern boss bar (OreSpawn itself shows its bosses' health only while you aim at them from 16 blocks or closer):

![OreSpawn's The King with its boss bar](docs/images/orespawn-king-bossbar.png)

A vanilla village in All the Mods 10 (Complementary shaders) with the buildings 1.7.10 mods add to villages: Witchery's town walls,
keep and watchtowers and Thaumcraft's wizard tower, all built by their legacy village code beside modern mods' trees and terrain:

![Legacy village components in a modern village](docs/images/legacy-village-components.png)

A nuclear explosion from HBM's Nuclear Tech Mod: the fireball and mushroom cloud with its condensation rings, flying debris, the
fallout-tinted sky and the scorched crater, all from HBM's own explosion and effect code:

![An HBM nuclear explosion](docs/images/hbm-nuke.png)

1.12.2: Atum 2, the desert dimension, generated by Atum's own chunk generator with its biomes, sandstorm haze, and sky colour:
Atum's creatures (camel, nomad, a mummy burning in daylight, bonestorm, desert wolf, and scarab) by a sandstone pyramid:

![Atum's creatures](docs/images/atum-creatures.png)

1.12.2: Scape and Run: Parasites. Primitive and adapted parasites (longarms, summoner, manducater, viscera, buglin, an infected human, bear, and golem),
drawn by the mod's own renderers under Complementary shaders:

![Scape and Run: Parasites on 1.21.1](docs/images/srparasites-lineup.png)

1.12.2: The Midnight, its own dimension: the Deceitful Bog with giant Bogshrooms, Nightstone ground, glowing plants, and the mod's night sky,
generated by the mod's own chunk generator and biomes:

![The Midnight's Deceitful Bog](docs/images/midnight-bog.png)

Older versions (alpha), taken with shaders in survival. 1.4.7: RedPower 2 logic, a lever, a red alloy wire, a NOT gate, and a lamp, and a ComputerCraft computer with its terminal:

![RedPower 2 on 1.4.7](docs/screenshots/old-versions/1.4.7/redpower-logic-not-gate-lever-lamp-lit-survival-shaders.jpg)

![ComputerCraft on 1.4.7](docs/screenshots/old-versions/1.4.7/computercraft-computer-terminal-help-shaders.jpg)

1.6.4: Tinkers' Construct's smeltery melting ore into molten metal, and Mo' Creatures' ostrich and elephant:

![Tinkers' Construct smeltery on 1.6.4](docs/screenshots/old-versions/1.6.4/tinkers-smeltery-molten-metal-survival-shaders.jpg)

![Mo' Creatures on 1.6.4](docs/screenshots/old-versions/1.6.4/mo-creatures-ostrich-elephant-shaders.jpg)

1.5.2: Applied Energistics 1's controller, terminal, and drive:

![Applied Energistics 1 on 1.5.2](docs/screenshots/old-versions/1.5.2/applied-energistics-1-controller-terminal-drive-shaders.jpg)

## Star history

[![Star History Chart](https://api.star-history.com/svg?repos=kylepitcock/Legacy-Connector&type=Date)](https://star-history.com/#kylepitcock/Legacy-Connector&Date)

## Legal

Legacy Connector is released under the [MIT License](LICENSE).

**Modpacks:** anyone may include Legacy Connector in a modpack, on any platform, without asking for permission. This covers
Legacy Connector only; the legacy mods you put next to it keep their own licenses.

This project contains no Minecraft, Forge, or third-party mod code. Do not commit or redistribute legacy mod jars;
users supply their own.
