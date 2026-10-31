# Bridges

Library bridges and engine bridges. Back to [Home](Home.md); see also [Architecture](Architecture.md).

## Outdated dependencies

Libraries that were never updated are replaced by their modern equivalent instead of being loaded, when that mod is
installed (the same idea as Connector Extras bridging Trinkets to Curios):

| Legacy library | Modern replacement | Status |
|---|---|---|
| Baubles 1.0 (`baubles.api`) | [Curios](https://www.curseforge.com/minecraft/mc-mods/curios) | Baubles is skipped; `IBauble` items become curios in the ring/necklace/belt slots; `BaublesApi.getBaubles` reads the Curios slots. |
| NotEnoughItems (`codechicken.nei`) | [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | NEI is skipped; mods' `NEI*Config` plugins run; each legacy recipe handler becomes a JEI category over its own GUI texture; NEI-hidden items are hidden in JEI. |
| Waila (`mcp.mobius.waila.api`) | [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) | Waila is skipped; legacy tooltip providers registered through Waila's IMC callback show their lines in Jade. |
| CoFH RF API (`cofh.api.energy`, shaded by most tech mods) | NeoForge Energy capability (built in) | Always on: RF tiles and items expose `IEnergyStorage` (1 RF = 1 FE), so modern cables and machines connect. |

| IC2 EU, BuildCraft MJ, Forge fluids, `IInventory` | NeoForge Energy / Fluid / Item capabilities (built in) | Always on: legacy tiles and items are capabilities by default (EU at `energy.fePerEu` = 4 FE, MJ at `energy.fePerMj` = 10 FE). Details in [Addon-API](Addon-API.md#capabilities). |

Without the modern mod, the legacy library is translated and loaded as usual (except the RF API, which needs none).

Mods that only make sense on 1.7.10's engine or launcher (performance/bug-fix patches such as FastCraft or Hodgepodge,
ID extenders, LWJGL 3 ports) are not loaded; client mods (JourneyMap, Inventory Tweaks, ...) are;
`legacyconnector-report.txt` says why.

New bridges are declared in `loader/.../LibraryBridges.java` and implemented under the shim namespace.

## Bridges at a glance

Two kinds of bridge exist. *Library bridges* replace an outdated legacy library by its modern equivalent (the table
above); *engine bridges* map a legacy system onto the 1.21 one it corresponds to. Status for 1.7.10:

**Implemented**

| Bridge | Legacy side | Modern side |
|---|---|---|
| Baubles → Curios | `baubles.api` | Curios slots |
| NEI → JEI | `codechicken.nei` | JEI categories and hidden items |
| Waila → Jade | `mcp.mobius.waila.api` | Jade tooltips |
| CoFH RF → NeoForge Energy | `cofh.api.energy` | `IEnergyStorage` (1 RF = 1 FE) |
| Blocks and tile entities | `Block`, `TileEntity`, metadata | registered blocks and block entities, one per legacy block |
| Items and ItemStacks | `Item`, `ItemStack` damage/NBT | one item per metadata subtype, `CUSTOM_DATA` NBT |
| Recipes and smelting | `GameRegistry` / `CraftingManager` / `OreDictionary`, custom `IRecipe` classes (IC2, AE2, Blood Magic, Witchery ...) | data-pack recipes, tags; a custom recipe is a `legacyconnector:legacy_recipe` that matches and crafts through the legacy object |
| Entities | `Entity`, spawn/save/load, mob AI | entity types, spawns, saving |
| Fluids | Forge `Fluid` / containers | fluid types |
| Villagers | `VillagerRegistry` ids, skins, trade handlers | `legacyconnector:villager_<id>` professions, trades, clothes layer; some new unemployed villagers take them |
| GUIs and containers | `GuiHandler`, `Container` | menus and screens |
| Networking | FML `SimpleNetworkWrapper` / channels | NeoForge payloads |
| Commands | `ICommand`, server command manager | Brigadier |
| Dimension physics | `WorldProvider` gravity, fall damage and sound attenuation (Galacticraft's `IGalacticraftWorldProvider`) | gravity attribute, fall damage multiplier, client sound volume per dimension |
| Chest loot | `ChestGenHooks.addItem` for dungeon, mineshaft, pyramid, stronghold, village and bonus chests | an extra pool on the matching vanilla chest loot table |
| Fishing loot | `FishingHooks` fish, junk and treasure entries (MineFactoryReloaded's junk) | a global loot modifier swaps a catch of the same category for a mod entry with its 1.7.10 weight share |
| Brewing | `Item.getPotionEffect` ingredients, `PotionHelper` potion values and requirements | one brewing recipe running 1.7.10's potion values: mod ingredients and mod-effect potions brew as in 1.7.10 (redstone, glowstone, gunpowder apply), vanilla potions stay vanilla, water breathing (pufferfish) is back |
| Biomes, world types, mod dimensions | `BiomeGenBase`, `GenLayer`, `WorldProvider` | data-driven biomes, `legacyconnector:dim_<id>` |
| World generation | `IWorldGenerator`, decorators | chunk generation hooks |
| Events | Forge/FML event buses | NeoForge event bus |
| Sounds, potions, creative tabs | `SoundHandler`, `Potion`, `CreativeTabs` | sound events, effects, tabs |
| Stats and achievements | `StatList`, `AchievementList`, `StatBase.addStat` | general stats count in their modern counterparts, mod stats become custom stats named after their legacy key (OpenBlocks' bricks dropped); the stat file reads them back |
| Coremods that patch mods | `IClassTransformer` plugins | applied to mod classes before translation |
| Java 8 → 21 | reflection hacks, removed JDK classes | `ReflectionCompat`, `ResourceCompat`, JDK stand-ins |
| API drift between mods | a library method whose return type changed | relinked to the installed signature |

**In progress / partial**

| Bridge | State |
|---|---|
| Entity rendering, armour and held items | bridged (including the player renderer and its `RenderPlayerEvent`s, creeper and large chest models, flame and breaking particles), not verified in game for every mod |
| Block rendering (`ISimpleBlockRenderingHandler`, TESRs) | Handlers draw in the world (Tinkers' tables, AE2, AgriCraft crops), renderers can bind vanilla textures by their 1.7.10 names, blocks that colour themselves (leaves, grass, `colorMultiplier`) are biome tinted; a single-colour handler quad on a block face has 1.7.10's face shade divided out and gets modern shading and ambient occlusion (tinted blocks, OpenBlocks' elevators), other single colours get AO; per-vertex colour is still missing, some handlers (Tinkers' drying rack, barricades) still draw nothing |
| Dimension sky/fog/clouds | Implemented (see *Mod dimensions*); Twilight Forest's fog and Galacticraft's planets work, but the skies of other mods' dimensions are not all verified |
| Portal placement | Twilight Forest's portal works; other mods' portals are not verified |
| Coremods that patch Minecraft itself | cannot run on the shim; the common cases (DragonAPI's recipe list and smelting hook) are handled one by one |
| Mods built for a different version of another mod | only return-type drift is relinked (Binnie's mods against Forestry 4.2.16 still fail) |
| 1.12.2 | alpha: most of the API mods use is implemented on both sides; see the status table and issue #64 |
| 1.20.1 (+ Sinytra), 1.16.5, 1.2.5 bridges | planned, in that order |
