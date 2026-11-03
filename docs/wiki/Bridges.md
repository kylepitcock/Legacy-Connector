# Bridges

Library bridges and engine bridges. Back to [Home](Home.md); see also [Architecture](Architecture.md).

## Outdated dependencies

Libraries that were never updated are replaced by their modern equivalent instead of being loaded, when that mod is
installed (the same idea as Connector Extras bridging Trinkets to Curios):

| Legacy library | Modern replacement | Status |
|---|---|---|
| Baubles 1.0 (`baubles.api`) | [Curios](https://www.curseforge.com/minecraft/mc-mods/curios) | Baubles is skipped; `IBauble` items become curios in the ring/necklace/belt slots; `BaublesApi.getBaubles` reads the Curios slots. |
| NotEnoughItems (`codechicken.nei`) | [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | NEI is skipped; mods' `NEI*Config` plugins run; each legacy recipe handler becomes a JEI category drawn by the handler itself ([details](#nei--jei-recipe-categories)); NEI-hidden items are hidden in JEI. NEI always came with CodeChickenLib, so without a 1.7.10 CodeChickenLib jar its API (`codechicken.lib`: `GuiDraw`, `Rectangle4i`, ...) is bridged too (a modern CodeChickenLib in the pack does not count); NEI's GUI hooks (`INEIGuiHandler`, `INEIGuiAdapter`, `API.registerNEIGuiHandler`) are accepted and ignored, as JEI lays out its own panel. |
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
| Blocks and tile entities | `Block`, `TileEntity`, metadata | registered blocks and block entities, one per legacy block plus one per block subtype that has an item ([flattening](#block-subtypes-flattening)) |
| Items and ItemStacks | `Item`, `ItemStack` damage/NBT | one item per metadata subtype, `CUSTOM_DATA` NBT |
| Recipes and smelting | `GameRegistry` / `CraftingManager` / `OreDictionary`, custom `IRecipe` classes (IC2, AE2, Blood Magic, Witchery ...) | data-pack recipes, tags; a custom recipe is a `legacyconnector:legacy_recipe` that matches and crafts through the legacy object |
| Entities | `Entity`, spawn/save/load, mob AI | entity types, spawns, saving |
| Fluids | Forge `Fluid` / containers | fluid types |
| Villagers | `VillagerRegistry` ids, skins, trade handlers, village creation handlers | `minecraft:villager_<id>` professions (a vanilla villager with another job, so Jade names its mod "Minecraft"; named after the mod's skin file, e.g. "Wizard"), trades, clothes layer; some new unemployed villagers take a legacy profession; legacy village buildings are elements of the vanilla village house pools with their 1.7.10 share (see [World generation](World-Generation.md)) |
| GUIs and containers | `GuiHandler`, `Container` | menus and screens |
| Networking | FML `SimpleNetworkWrapper` / channels | NeoForge payloads |
| Commands | `ICommand`, server command manager | Brigadier |
| Dimension physics | `WorldProvider` gravity, fall damage and sound attenuation (Galacticraft's `IGalacticraftWorldProvider`) | gravity attribute, fall damage multiplier, client sound volume per dimension |
| Chest loot | `ChestGenHooks.addItem` for dungeon, mineshaft, pyramid, stronghold, village and bonus chests | an extra pool on the matching vanilla chest loot table; chests that legacy structures fill from one of these categories get the matching modern loot table instead, so the pack's loot (and modern mods' loot modifiers) applies |
| Fishing loot | `FishingHooks` fish, junk and treasure entries (MineFactoryReloaded's junk) | a global loot modifier swaps a catch of the same category for a mod entry with its 1.7.10 weight share |
| Brewing | `Item.getPotionEffect` ingredients, `PotionHelper` potion values and requirements | one brewing recipe running 1.7.10's potion values: mod ingredients and mod-effect potions brew as in 1.7.10 (redstone, glowstone, gunpowder apply), vanilla potions stay vanilla, water breathing (pufferfish) is back |
| Biomes, world types, mod dimensions | `BiomeGenBase`, `GenLayer`, `WorldProvider` | data-driven biomes placed by climate (a TerraBlender region, or the overworld parameter list), legacy world types as their own biome source, `legacyconnector:dim_<id>` |
| World generation | `IWorldGenerator`, decorators | one placed feature per legacy generator (`<mod>:legacy_worldgen/<generator>`) and one per generator for the layer below y=0 (`<mod>:legacy_worldgen_deep/<generator>`), added by biome modifiers |
| Events | Forge/FML event buses | NeoForge event bus |
| Sounds, potions, creative tabs | `SoundHandler`, `Potion`, `CreativeTabs` | sound events ([details](#sounds)), effects, tabs |
| Stats and achievements | `StatList`, `AchievementList`, `StatBase.addStat` | general stats count in their modern counterparts, mod stats become custom stats named after their legacy key (OpenBlocks' bricks dropped); the stat file reads them back |
| Translations | `.lang` files, `LanguageRegistry`, `StatCollector`/`StringTranslate`/`I18n`, vanilla 1.7.10 keys (`tile.stone.name`) | converted `lang/<code>.json` files, a generated lang file, and legacy keys resolved on a miss of the modern language ([translations](#translations)) |
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
| 1.16.5, 1.2.5 bridges | planned, in that order |

## NEI → JEI recipe categories

Every `TemplateRecipeHandler` a mod registers through its `NEI*Config` plugin becomes one JEI category
(`<mod>:nei_<handler class>`, 166 pixels wide like NEI's recipe area; 65 high, or 130 for handlers that show one
recipe a page). Its title is the handler's `getRecipeName()` (translated when it is a key); its icon and catalysts are the
machines that run it: the stacks registered with NEI's `API.addRecipeCatalyst`, else the mod's block or item whose name best
matches the title and the handler's class name, else the vanilla machine of the `crafting`/`smelting`/`brewing` ids. `<mod>` (the
mod JEI names) is the machine's mod, else the mod whose jar holds the handler, else the mod most of its results come from. Its recipes are the ones the handler lists for its overlay id and the ids of its transfer rectangles; JEI's
"recipes / uses of this item" ask the handler with `loadCraftingRecipes("item", stack)` / `loadUsageRecipes("item", stack)`, as
NEI did. Each recipe keeps the handler instance that loaded it and its index there, and JEI's `draw` runs the handler's own
`drawBackground(i)` and `drawForeground(i)` (which calls `drawExtras(i)`) inside the legacy GUI render context, at NEI's origin
(where the handler's `PositionedStack`s are). So whatever the handler draws appears: the machine picture, fuel/energy/fluid
gauges, progress bars, temperatures, text. `TemplateRecipeHandler` implements NEI's defaults (`drawBackground`: region
(5, 11, 166x65) of `getGuiTexture()`; `drawForeground`: rebinds it, then `drawExtras`; `drawProgressBar` in its eight
directions); `cycleticks` follows the clock, so progress bars animate. A handler whose drawing throws is logged once.

Texture names: `getGuiTexture()` is a legacy location string. Domain-qualified names (`witchery:textures/gui/distiller.png`)
map like any legacy texture. Reika's handlers return class path files (`/Reika/RotaryCraft/Textures/GUI/blastfurngui.png`) and
bind them through DragonAPI's `ReikaTextureHelper` (raw GL ids, see [Rendering](Rendering.md)); if such a name reaches the texture
manager anyway (`new ResourceLocation("/Reika/...")`) it is read from the mods' class path with its original case and
registered once as `legacyconnector:classpath/<path>` (a missing file gets the missing texture once, never a lookup per frame).

## Block subtypes (flattening)

A 1.7.10 block has 16 metadata values, and its item usually lists some of them as subtypes (wool colours, Chisel variants,
machine types). Legacy Connector registers one modern block per legacy block with the metadata as a `meta` property, and one
modern item per subtype (`mod:name`, `mod:name_1`, ...). Modern code takes a block item's block and places or compares its
default state (EvilCraft's display stand per planks item, Rechiseled, Cyclops Core's `BlockHelpers`, framed and copycat
blocks), so with one block every subtype came out as metadata 0 (EvilCraft even made duplicate creative tab stacks, see
`CreativeTabDuplicateMixin`). As in the 1.13 flattening, each subtype now has a block of its own
(`v1_7_10/bridge/BlockFlattening1710`, config `blocks.flattenSubtypes`, default on):

- For every metadata value `N > 0` that a subtype item places (`ItemBlock.getMetadata(damage)`), a *member* block
  `mod:name_N` is registered once all legacy mods have loaded. It is the same legacy block (a `LegacyBlockAdapter` with the
  base block's properties and behaviour) whose default state is metadata `N`. The subtype item's `BlockItem.getBlock()`,
  `Item.byBlock` and the member's `asItem()` point at each other, the member is named after its subtype, and it gets the
  mineable/tool tier tags of its own metadata.
- A member also holds the metadata values the block drops as that subtype (`damageDropped`): a birch log rotated to
  metadata 5, 9 or 13 stays in `name_1`. Its `meta` property only has those values (a single value: no property). Metadata
  without an item of its own stays in the base block `mod:name`, which keeps its id and all 16 values. Each legacy block
  gains at most 16 block states however many subtypes it has.
- Legacy code sees no difference: `World.getBlock`/`getBlockMetadata` read any member, `setBlock`/`setBlockMetadata`
  write the member that owns the metadata. A metadata change that moves the state to another member of the same legacy
  block is not a new block: no `breakBlock`/`onBlockAdded`, the tile entity stays (the host block entity type accepts
  every member), scheduled ticks and block events queued for the legacy block still run (`ServerLevelBlockFamilyMixin`) and the
  client keeps its block entity (`LevelChunkBlockFamilyMixin`). `LegacyBlockAdapter.metaOf(state)` reads the metadata of
  any member; `LegacyBlockAdapter.META` is only the base block's property.
- Models: a member's blockstate file points at the base block's per-metadata models (`block/name_mN`).
- Saved worlds hold `mod:name[meta=N]` base states. When a chunk loads, states whose metadata a member owns now are moved
  into it, in place (same legacy block and metadata, tile entity kept). With `blocks.flattenSubtypes = false` the members are
  still registered, nothing points at them, and the same conversion moves their states back into the base block: switching
  either way loses no block.
- Not flattened: blocks whose items all place metadata 0 (the subtype lives in the tile entity), deepslate ore twins (one
  twin per legacy block, all metadata), and a member id another block already has (that subtype stays in the base block).

1.12.2 is not flattened yet: there an `ItemBlock` is one modern item (no per-subtype items), so the flattening would first
need per-subtype block items.

## Translations

1.7.10 had two tables: the server's `StringTranslate` (what `StatCollector` reads) and the client's `Locale` (`I18n`, which
replaced the server table once resources loaded). FML read every mod jar's `assets/<domain>/lang/en_US.lang` into the server
table before constructing the mods, so mods translate in static initializers: ReactorCraft builds its ore names
(`ore.cadmium` -> "Cadmium Ore") in the constructor of its ore enum, and ElectriCraft and RotaryCraft do the same. How the
layer serves each source:

- Lang files of a jar become `assets/<ns>/lang/<code>.json` (loader, `LegacyResourceConverter`), which the client loads like
  any mod's. Before the first 1.7.10 mod is constructed, `Platform1710` also reads every legacy jar's `en_US.lang` into
  `GeneratedTranslations`' bundled table (`LanguageRegistry.mcc$loadLanguagesFor`), as FML did: `StatCollector` answers from it
  on the server and on a client whose languages are not loaded yet. These entries are not exported again.
- Code-registered strings (`LanguageRegistry.addStringLocalization`/`addName`, writes to `StringTranslate.languageList`) go to
  `GeneratedTranslations`, exported as `assets/legacyconnector/lang/<code>.json` of the generated pack on every reload.
- Vanilla 1.7.10 keys (`tile.stone.name`, `item.ingotIron.name`, subtypes such as `tile.cloth.red.name` or
  `item.dyePowder.blue.name`, entity and attribute names) map to their modern keys (`VanillaNames.modernKey`, through the legacy
  wrapper of the vanilla block or item, so the key names the modern object the bridge maps it to). The legacy `getLocalizedName`
  of a vanilla wrapper returns the modern block's name: ChromatiCraft's hidden ores name themselves `Blocks.stone.getLocalizedName()`.
- On the client, a key the loaded languages miss is looked up in all of the above (`ClientLanguageLegacyKeysMixin` ->
  `LegacyLang`), so strings registered after the last resource reload and legacy vanilla keys also resolve in a
  `Component.translatable` (item and block names, Jade/WTHIT). Only misses pay.
- Names: a legacy item that overrides `getItemStackDisplayName` is named by it; otherwise its name is the translatable
  `<unlocalized>.name`. A modern block's `getName()` (Jade without a pick item) is the legacy `getLocalizedName` when the block
  overrides it, else its lang key.

## Sounds

- World sounds (`World.playSoundEffect`, `playSoundAtEntity`, ...) become modern level sounds; client sounds (`SoundHandler.playSound` with
  `PositionedSoundRecord`s or a mod's own `ISound`/`ITickableSound`) become sound instances that keep the legacy object's volume, pitch,
  position, repeat and attenuation, and stop when the mod stops them. Sounds naming a file instead of an event (DragonAPI's
  `custom_path:Reika/.../x.ogg`) are played from that file (`LegacySoundFiles`).
- **Volume sliders:** 1.7.10 took a sound's category from its sounds.json event (`"category": "block"`); the loader keeps it as
  `legacy_category` (the modern format has none) and the sound plays under the matching `SoundSource` (block, hostile, neutral,
  player, ambient, weather, record, music, master). Events without one play under the entity's own slider (`Entity.playSound`,
  `playSoundAtEntity`), as blocks (other world sounds) or master (client sounds).
- **Names:** `modid:name` is the mod's event. A name without a domain was minecraft's in 1.7.10: one a mod declares in its own
  `assets/minecraft/sounds.json` plays that event, one in the mod's own sounds.json plays the mod's, and the 1.7.10 vanilla names
  (`mob.zombie.say`, `game.hostile.die`, `random.pop`...) are translated to their 1.9+ events (`SoundBridge`). Only names 1.7.10 itself
  declared are translated, each to its exact event: an unknown `mob.<mob>.<x>` is not guessed to a modern mob's ambient or hurt sound.
- **Mob sounds:** a legacy mob's idle, hurt and death sounds are its `getLivingSound`/`getHurtSound`/`getDeathSound`, and a mob that
  overrides `playLivingSound` plays its idle sounds through it. (OreSpawn's beaver sounding like its frog is the mod's own choice: both
  use `orespawn:scorpion_hit` as their hurt sound.)
- **Fall-off with distance** (`LegacyAttenuation`): 1.7.10's sound library computed a linear source's gain itself, `1 - distance / range`
  with range 16 blocks times the volume when that is above 1 (a volume above 1 only reaches further, the gain is capped at 1). The modern
  engine leaves the fall-off to OpenAL, which does not attenuate stereo files, and most mods of that time ship stereo files (111 of
  ChromatiCraft's 147): their hums and pulses played at full volume however far away. Legacy positioned sounds (client sounds, and world
  sounds of a legacy mod's namespace) therefore play without OpenAL attenuation and with the 1.7.10 gain recomputed every tick from the
  camera distance; loops out of range start silent and fade in.

## Vanilla texture and model renames (1.12.2)

Mod models and blockstates name vanilla textures the 1.12 way (`blocks/planks_oak`, `items/dye_powder_blue`). The loader's
`resource/VanillaTextureRenames` maps them to 1.21.1 (`block/oak_planks`, `item/lapis_lazuli`) from
`loader/src/main/resources/legacyconnector/resource/vanilla-texture-renames.tsv` (about 420 rows, block and item names kept apart); the runtime
`VanillaTextures1122` reads a copy that `build.gradle` puts into the mod jar. The table comes from the MIT-licensed Retromod project (see
`THIRD_PARTY_NOTICES.md`) and every row is checked against the 1.21.1 client jar: the new texture exists and the old one is gone. Vanilla model
parents that were renamed live in `LegacyResourceConverter.MODEL_RENAMES`. Ore dictionary names map to common tags in `OreDictBridge` (`SPECIAL`
for exact names, prefixes such as `ingot` for the rest).

