# Legacy Connector

[![Modrinth](https://img.shields.io/badge/Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/mod/legacyconnector) [![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/kylepitcock/Legacy-Connector)

**Play unmodified 1.7.10 and 1.12.2 Forge mods on NeoForge 1.21.1.**

| Legacy version | Support |
| --- | --- |
| **1.7.10** | ✅ Supported (beta) |
| **1.12.2** | ✅ Supported (alpha) |

Drop 1.7.10 or 1.12.2 mod jars into your `mods` folder next to Legacy Connector. They get translated when the game loads and run as normal NeoForge mods. It's the same idea as Sinytra Connector, but for old Forge mods instead of Fabric. Your original jars are never modified.

**Beta:** 1.7.10 content mods like Iron Chest, Twilight Forest, Thaumcraft, HBM, RFTools, Thermal Expansion, Botania, Biomes O' Plenty and Witchery run with their blocks, items, mobs, recipes, GUIs, world generation and dimensions. Galacticraft and its addons run, with each planet's own gravity. 1.12.2 support is in alpha: big content mods like Botania, Blood Magic, Mekanism, EnderIO, Immersive Engineering, Tinkers' Construct and Scape and Run: Parasites load and play with their own models, mobs, GUIs and world generation. Back up your worlds before trying a new mod set.

**[Compatibility list & reports](https://kylepitcock.github.io/Legacy-Connector/)**: check whether a mod works, or report how it went for you.

---

## Installation

1. Create a **Minecraft 1.21.1 / NeoForge 21.1.x** profile (Java 21) and install Legacy Connector.
2. Download the **1.7.10** or **1.12.2** versions of the mods you want from their CurseForge pages (Files tab, filter by game version). The CurseForge app won't add old-version mods to a 1.21.1 profile, so add them by hand: in the app, open your profile, click **⋯ → Open Folder**, and drop the jars into `mods`. Mods that bundle their own Mixin (common in 1.12.2, e.g. Spartan Weaponry) go in `mods/1.12.2` instead; in `mods` itself NeoForge would load them as launcher plugins before they can be translated, and the game would stop.
3. If a mod needs a library (CodeChickenLib, CoFH Core, etc.), add that library's jar for the same old version too.
4. Start the game. The first launch translates each legacy jar, with a progress bar on the loading screen, and caches the result, so later launches are fast.
5. Check `logs/legacyconnector-report.txt` to see what was translated and anything that couldn't be linked.

---

## Screenshots

All taken in a real game client on NeoForge 1.21.1, with unmodified 1.7.10 mods that have no modern version running next to modern content.

**A Thaumcraft 4 workshop: infusion altar, crucible, alembics, essentia jars, research table, an aura node and magical trees**

![A Thaumcraft workshop](https://raw.githubusercontent.com/kylepitcock/Legacy-Connector/master/docs/images/thaumcraft-workshop.png)

**A Create Aeronautics airship (Sable physics) carrying 1.7.10 blocks: Thaumcraft essentia jars, crucible and arcane workbench, an Iron Chest, a RotaryCraft engine and ChromatiCraft crystals**

![A Create Aeronautics airship](https://raw.githubusercontent.com/kylepitcock/Legacy-Connector/master/docs/images/create-aeronautics-ship.png)

**A Witchery ritual site: chalk circle, altar, witch's oven, kettle, distillery and spinning wheel, crops and Witchery creatures**

![A Witchery ritual site](https://raw.githubusercontent.com/kylepitcock/Legacy-Connector/master/docs/images/witchery-ritual.png)

**Thaumcraft's Arcane Workbench, drawn by its own 1.7.10 GUI code, next to JEI**

![Legacy block GUI](https://raw.githubusercontent.com/kylepitcock/Legacy-Connector/master/docs/images/legacy-block-gui.png)

**Fisk's Superheroes: Doctor Octopus with his tentacles deployed and the suit's abilities**

![Fisk's Superheroes Doctor Octopus](https://raw.githubusercontent.com/kylepitcock/Legacy-Connector/master/docs/images/fisk-doc-ock.png)

**OreSpawn's The King, with a modern boss bar**

![OreSpawn's The King](https://raw.githubusercontent.com/kylepitcock/Legacy-Connector/master/docs/images/orespawn-king-bossbar.png)

**A modern village enclosed by Witchery's town walls and watchtowers, with AgriCraft greenhouses, built by their legacy code**

![Legacy village components](https://raw.githubusercontent.com/kylepitcock/Legacy-Connector/master/docs/images/legacy-village-components.png)

**A nuclear explosion from HBM's Nuclear Tech Mod, drawn by its own effect code**

![An HBM nuclear explosion](https://raw.githubusercontent.com/kylepitcock/Legacy-Connector/master/docs/images/hbm-nuke.png)

**1.12.2: Scape and Run: Parasites**, with its own models, AI and attacks:

![Scape and Run: Parasites](https://raw.githubusercontent.com/kylepitcock/Legacy-Connector/master/docs/images/srparasites.png)

---

## How it works

1. **Inspect:** detects legacy mods and their Minecraft version from `mcmod.info`, `@Mod` annotations and FML references
2. **Translate:** remaps old Minecraft/Forge classes and SRG names, and fixes Java 8 → 21 and LWJGL 2 → 3 incompatibilities
3. **Link:** every legacy API call resolves against a hand-written re-implementation of that version's API built on NeoForge. Anything not implemented yet is generated automatically, so mods *load* instead of crashing with `NoClassDefFoundError`
4. **Convert resources:** lang files, textures, blockstates and models are converted to 1.21 formats

Mods work because the legacy API they were written against is rebuilt on top of NeoForge, not because of patches for individual mods. Mod-specific fixes are a rare last resort, kept small and isolated.

## What's bridged (1.7.10)

- **Blocks, items and tile entities:** metadata subtypes become real items, and NBT is preserved
- **Recipes and smelting:** converted to data-pack recipes, including mods' custom recipe types (IC2, AE2, Blood Magic, Witchery). The Ore Dictionary becomes tags
- **Loot and fishing:** items mods add to dungeon, mineshaft, temple, stronghold and village chests appear in the matching vanilla loot tables, and mods' fishing catches are caught in modern fishing
- **Brewing:** mod ingredients and mod potions brew in the brewing stand as on 1.7.10, and redstone, glowstone and gunpowder apply to them
- **Entities:** legacy mobs spawn naturally, tick, use their own AI, fight and save. Their renderers and models draw in game, including custom player rendering. Projectiles fly and hit as on 1.7.10. Legacy bosses get a boss health bar
- **GUIs and HUD:** mods' own custom GUIs (Thaumcraft, AE2, HBM, RFTools), anvils and HUD overlays. Custom Main Menu can replace the title screen
- **Networking, commands, sounds, potions, creative tabs, dispensers and events**
- **Achievements and statistics:** achievements are earned per player and shown as toasts; mod statistics count in the modern statistics screen
- **Villagers and villages:** modded professions and trades appear on villagers. Mods' village buildings, such as Witchery's shops and towers and AgriCraft's greenhouses, generate inside modern villages along the village streets
- **Fluids:** flow, render and expose NeoForge fluid capabilities
- **World generation:** mod biomes become real 1.21 biomes placed by the mod's own biome layers. Mod world types (e.g. Biomes O' Plenty's) appear on the Create World screen. Ore generators, dungeons and structures run normally
- **Deepslate ores:** legacy ores also generate below y=0 and get an automatic deepslate variant with the same drops and behaviour
- **Mod dimensions:** run their own terrain, biomes, fog, clouds and sky. Dimensions that mods register while the game runs get their own world too. Per-dimension gravity, fall damage and sound work, so Galacticraft's planets have low gravity and space is silent. Twilight Forest's portal works
- **Block and item rendering:** includes custom block render handlers, TESRs, item renderers, OBJ and Techne models, and mods' own texture loaders
- **Coremods that patch other mods** (CoFH, AE2, OpenMods and more)
- **Scala mods,** plus the libraries 1.7.10 shipped (Scala 2.11, Trove, vecmath)

## What's bridged (1.12.2, alpha)

- **Registries:** blocks, items, potions, enchantments, sounds, biomes, entities, recipes, villager professions, and mods' own custom registries
- **Blocks and items:** block states, items with subtypes, tile entities with client sync, tools, armour and food
- **Recipes:** shaped, shapeless, ore and furnace recipes become data-pack recipes. The Ore Dictionary becomes tags
- **Capabilities:** legacy tiles and items are NeoForge item, fluid and energy capabilities by default (RF, IC2 EU, BuildCraft MJ, Forge fluids, inventories), so hoppers, Create and modern cables work with them; legacy tools count as tools
- **Networking, IMC, coremod plugins** and the usual annotations (`@ObjectHolder`, `@SidedProxy`, `@EventBusSubscriber`)
- **Models:** mod blockstates and item models are translated to 1.21 formats (only lightly tested)
- **`@Config` classes** are filled from their config files
- **Commands:** the commands mods register when the server starts become modern commands, with their permissions and tab completion
- **Not yet:** entities and AI, containers and GUIs, in-world fluids, world generation, custom recipe classes and most client rendering

### Modern replacements for outdated libraries

| Legacy | Becomes | Notes |
| --- | --- | --- |
| Baubles | **Curios** | Bauble items go into Curios slots |
| NotEnoughItems | **JEI** | Legacy recipe handlers become JEI categories and answer recipe/usage lookups |
| Waila | **Jade** | Legacy tooltip providers show in Jade |
| CoFH RF API | **NeoForge Energy** | Always on. 1 RF = 1 FE, so modern cables connect |

If the modern mod isn't installed, the legacy library loads as usual. Each replacement can also be turned off in the config to load the legacy library instead.

### Create contraptions and Create Aeronautics ships

Create contraptions and Create Aeronautics/Sable ships can carry legacy blocks (1.7.10 and 1.12.2): tile entity data and inventories move with the block, and blocks that legacy pistons could not move are refused. Tested in game with a mechanical bearing and an Aeronautics airship (1.7.10: Iron Chest, Thaumcraft, RotaryCraft, ChromatiCraft). Limitations: blocks are not rotated when placed back turned, and legacy inventories are not contraption storage.

## Performance

Thaumcraft 4 measured against no legacy mods in a real client: about +38 MB of heap after a full GC. Frame rate and tick
time vary more between identical runs than between the two setups. Loading Thaumcraft takes about 5.5 s per launch;
translating its jar adds about 0.5 s on the first launch only (the result is cached). Details are in the [documentation on GitHub](https://github.com/kylepitcock/Legacy-Connector/blob/master/docs/wiki/Home.md).

## Changing legacy recipes

Legacy recipes are normal modern recipes with stable ids (`<mod>:<kind>/<output item>`, e.g. `ic2:shaped/macerator`), in a pack
below your world data packs. Replace or remove them with a data pack, edit them with KubeJS (`event.remove({mod: 'ic2'})`), or
keep using MineTweaker 3 scripts (1.7.10): `recipes.addShaped`, `recipes.remove`, `furnace.addRecipe` and `furnace.remove`
become modern recipes. KubeJS changes also apply in mod crafting tables, autocrafters and furnaces. Removing vanilla recipes from MineTweaker scripts is not supported yet; use a data pack or KubeJS for those.

## Configuration

Settings are in `config/legacyconnector.properties`, which is created with comments on first launch.

| Setting | Default | What it does |
| --- | --- | --- |
| `oreBackgrounds.enabled` | `true` | Replaces the old stone/netherrack/end stone behind legacy ore textures with modern textures |
| `oreBackgrounds.stone` (also `.netherrack`, `.endStone`) | `stone` | Texture to use behind ores, e.g. `deepslate` or `tuff`, or `legacy` to keep the old one |
| `modernizeTextures` | `true` | Replaces old vanilla art inside legacy mod textures (copies of old blocks, items and GUIs, old stone/plank/brick backgrounds behind a mod's art) with the modern vanilla textures |
| `deepLayer.enabled` | `true` | Runs legacy ore generators again below y=0 |
| `deepLayer.excludeGenerators` | empty | Generators that shouldn't run below y=0 |

## Current status

**1.7.10 (beta):** Iron Chest, Twilight Forest, Thaumcraft, HBM, RFTools, Thermal Expansion and others work in game. Galacticraft and its addons run with per-planet gravity. In-game tests of the **GT New Horizons** pack and a 130-jar 1.7.10 mod list, which place every block, use every item and spawn every mob, rate **166 mods** as working and **57** as partly working.

**1.12.2 (alpha):** content mods load and play in a real client: blocks and items with their models, mobs with their own renderers and AI (Scape and Run: Parasites' parasites hunt you), GUIs, world generation, loot, natural spawning and Forge events. In a server test of 32 big mods (Applied Energistics 2, Blood Magic, Botania, Biomes O' Plenty, Chisel, EnderIO, Forestry, Immersive Engineering, Mekanism, Thaumcraft, Thermal Expansion, Tinkers' Construct, Twilight Forest and more), every mod but BuildCraft Silicon loaded fully: about 2,060 blocks, 3,480 items and 3,680 recipes. In a larger test of 116 mods, about 5,050 blocks and 10,170 items register, and an automatic self test places and right-clicks every block and uses every item without errors. Performance and bug-fix mods like FoamFix, BetterFPS and VanillaFix are skipped.

See the **[compatibility site](https://kylepitcock.github.io/Legacy-Connector/)** for per-mod results.

**Not done yet (1.7.10):** per-vertex colour on handler-drawn blocks. Portals other than Twilight Forest's and skies in other mods' dimensions haven't all been verified.

**Not supported:** coremods and tweakers that patch *Minecraft's own* classes. They're reported in the log, and the rest of the mod still loads. When a coremod only changes engine behaviour that has a modern equivalent, like Galacticraft's gravity, Legacy Connector recreates that behaviour instead.

Engine and launcher patches (FastCraft, Hodgepodge, NEID and other ID extenders) are skipped on purpose, since they only make sense on the old engine.

## Troubleshooting

After launching, check **`logs/legacyconnector-report.txt`**. For each legacy jar, it lists the detected version, the mod IDs, any unresolved references, and which APIs are stubbed. Known issues and workarounds (ID conflicts, mismatched Forestry/Binnie versions, etc.) are on the GitHub page. Please report results on the [compatibility site](https://kylepitcock.github.io/Legacy-Connector/).

**The game stops on launch with "Module ... contains package org.spongepowered...":** a 1.12.2 jar in `mods/` bundles its own Mixin (Spartan Weaponry, MixinBooter mods, SuperMartijn642's Core Lib). NeoForge loads such jars as launcher plugins before any mod code (including Legacy Connector) can run, so this cannot be fixed from inside the mod. Move the jar into `mods/1.12.2/` (or `mods/1.7.10/`); Legacy Connector reads those folders and removes the bundled Mixin when it translates the jar.

### Commands (operator)

| Command | What it does |
| --- | --- |
| `/legacyconnector mods` | List loaded legacy mods |
| `/legacyconnector list <blocks/items/entities> <mod>` | List a mod's IDs |
| `/legacyconnector inspect` | Describe the block or entity you're looking at (legacy class, metadata, tile entity, render type) |
| `/legacyconnector give <item> [meta] [count]` | Give a legacy item with metadata |
| `/legacyconnector place <block> [meta]` | Place a legacy block with metadata |
| `/legacyconnector spawn <entity> [count]` | Spawn a legacy mob |
| `/legacyconnector gui <mod> <id> [pos]` | Open a mod's GUI by its handler ID |
| `/legacyconnector dim list` / `dim <id> [pos]` | List or travel to legacy mod dimensions |
| `/legacyconnector biome <id>` | Jump to a legacy biome |

IDs tab-complete.

## Writing addons

Other NeoForge mods can add modern support to legacy mods through a small public API: lifecycle events, access to legacy classes,
modern/legacy stack and tile conversion, and NeoForge capabilities for legacy tile entities. Guide and example addon (modern aspects for Thaumcraft 4):
[Addon API](https://github.com/kylepitcock/Legacy-Connector/blob/master/docs/wiki/Addon-API.md).

## Roadmap

✅ 1.7.10 → 🧪 **1.12.2** (in progress) → **1.20.1** (Forge and Fabric via Sinytra) → **1.16.5** → **1.2.5**

---

**Legal:** Legacy Connector is released under the MIT License. It contains no Minecraft, Forge or third-party mod code. You supply your own mod jars; please don't redistribute them.

[Modrinth](https://modrinth.com/mod/legacyconnector) · [Source & issue tracker (GitHub)](https://github.com/kylepitcock/Legacy-Connector) · [Compatibility list](https://kylepitcock.github.io/Legacy-Connector/)
