# Bridges

Library bridges and engine bridges. Back to [Home](Home.md); see also [Architecture](Architecture.md).

## Outdated dependencies

Libraries that were never updated are replaced by their modern equivalent instead of being loaded, when that mod is
installed (the same idea as Connector Extras bridging Trinkets to Curios):

| Legacy library | Modern replacement | Status |
|---|---|---|
| Baubles 1.0 (`baubles.api`) | [Curios](https://www.curseforge.com/minecraft/mc-mods/curios) | Baubles is skipped; `IBauble` items become curios in the ring/necklace/belt slots; `BaublesApi.getBaubles` reads the Curios slots. |
| NotEnoughItems (`codechicken.nei`) | [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | NEI is skipped; mods' `NEI*Config` plugins run; each legacy recipe handler becomes a JEI category drawn by the handler itself ([details](#nei--jei-recipe-categories)); NEI-hidden items are hidden in JEI. NEI always came with CodeChickenLib, so without a 1.7.10 CodeChickenLib jar its API (`codechicken.lib`: `GuiDraw`, `Rectangle4i`, ...) is bridged too (a modern CodeChickenLib in the pack does not count); NEI's GUI hooks (`INEIGuiHandler`, `INEIGuiAdapter`, `API.registerNEIGuiHandler`) are accepted and ignored, as JEI lays out its own panel. |
| Waila / HWYLA (`mcp.mobius.waila`) | [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) | Waila is skipped; legacy head/body/tail/stack providers show their lines in Jade and NBT providers sync server data to the client ([per version](#waila-and-nei-per-legacy-version)). |
| Patchouli 1.12 (`vazkii.patchouli.api`, 1.12.2 only) | [Patchouli](https://modrinth.com/mod/patchouli) | The legacy Patchouli is skipped; legacy mods' books are shown by the modern one ([details](#patchouli-books)). |
| CoFH RF API (`cofh.api.energy` 1.6.4/1.7.10, shaded by most tech mods; Redstone Flux `cofh.redstoneflux` 1.12.2, a separate mod) | NeoForge Energy capability (built in) | Always on: RF tiles and items expose `IEnergyStorage` (1 RF = 1 FE), so modern cables and machines connect. The API is re-implemented in the shim of each version, so the Redstone Flux library jar is not loaded either (`bridges.cofhEnergy.enabled=false` loads it). See [Energy per version](#energy-per-version). |

| IC2 EU, BuildCraft MJ, Forge fluids, `IInventory` | NeoForge Energy / Fluid / Item capabilities (built in) | Always on: legacy tiles and items are capabilities by default (EU at `energy.fePerEu` = 4 FE, MJ at `energy.fePerMj` = 10 FE; `.in`/`.out` suffixes set each direction, 0 turns it off, see [Configuration](Configuration.md)). Details in [Addon-API](Addon-API.md#capabilities). |

Without the modern mod, the legacy library is translated and loaded as usual (except the RF API, which needs none).

Mods that only make sense on 1.7.10's engine or launcher (performance/bug-fix patches such as FastCraft or Hodgepodge,
ID extenders, LWJGL 3 ports) are not loaded; client mods (JourneyMap, Inventory Tweaks, ...) are;
`legacyconnector-report.txt` says why.

New bridges are declared in `loader/.../LibraryBridges.java` and implemented under the shim namespace.

## Library decision order

A legacy library (any jar other legacy mods depend on) can clash with a native NeoForge 1.21.1 jar of the same mod id or with another legacy
version's copy. For every version the layer decides, in this order:

1. **Native bridge**: the library is re-implemented in the shim on top of the modern mod (Baubles to Curios, Waila to Jade, NEI to JEI, RF to NeoForge energy), or
   it is a 1.20.1 library of `libraries.tsv` whose native build is API compatible (Architectury, Curios, GeckoLib, Patchouli, Cloth Config, Resourceful Lib,
   Jade, ...): the legacy jar is skipped and its dependents link against the native build. Report: `native bridge: replaced by <id>`.
2. **Side by side** (`experimental.sideBySideLibraries`, default on): when the native API cannot be bridged (rows marked `sideBySide` in `libraries.tsv`,
   Citadel is the first) or another legacy version holds the same mod id (CodeChickenLib for 1.7.10 and 1.12.2), the legacy copy runs as an isolated unit next to
   the other one. Report: `side-by-side: citadel loads as citadel_l1201 next to its other copy ..., packages [...] relocated under ...`.
3. **Skipped** with a report line (switch off, `exclude`, no packages to isolate, or the older rule "another file holds the same mod in a newer version" within one version).

An isolated unit has: a distinct FML mod id `<id>_l<version>` (mods.toml `modId`, `[[dependencies.<id>]]` headers, `@Mod` value, `@Mod.EventBusSubscriber(modid)`,
the dependents' dependency entries and `ModList.isLoaded("<id>")` calls); its Java packages relocated under `legacyconnector_relocated/sbs_<version>/` in the library
and in every mod of the same legacy version (class references, descriptors, annotation and string constants such as `Class.forName` and mixin targets, `META-INF/services`
files and their contents, access transformers, mixin config `package`/`plugin`); renamed mixin configs (`x_l1201.mixins.json`); and a unique translated jar name,
so module names never clash. Registry namespaces and `assets`/`data` stay the library's own (`citadel:` ids resolve for the dependents); a library that registers the
same ids as its native build would clash at registration, which is the open limit of the policy. Older versions (1.4.7 to 1.12.2) already relocate packages shared
with modern mods or another version through `CrossVersionPackages`; the unit adds the id isolation there. Per-library overrides: `experimental.sideBySideLibraries.include`/`.exclude`.
Implementation: `loader/.../SideBySide.java` (plan, relocation), `SideBySidePlanner.java` (decision), `Forge1201Transformer` (1.20.1 application).

## Waila and NEI per legacy version

The bridged package roots are the same on every version (`mcp/mobius/waila/`, `codechicken/nei/`, `codechicken/lib/`, `baubles/`); a bridge
is on for a pack when Jade/JEI/Curios is installed and `bridges.<waila|nei|baubles>.enabled` is not `false` in `config/legacyconnector.properties`.

| Legacy version | Waila -> Jade | NEI -> JEI | Baubles -> Curios |
|---|---|---|---|
| 1.7.10 | `IMC "Waila" "register"` callbacks; head, body, tail and stack providers; NBT providers (server data synced through Jade); `TipList` as the tooltip list | recipe handlers, hidden items, catalysts (see below) | `baubles.api` |
| 1.6.4, 1.5.2, 1.4.7 | the 1.7.10 shim, plus `registerXProvider(provider, blockId)` by numeric id (`facade/.../IWailaRegistrar`) | the 1.7.10 shim plus the 1.6.4 `API.hideItem(int)` facade | resolved to the 1.7.10 shim when a mod uses it (no Baubles API existed on CurseForge before 1.7.10; no 1.4.7 / 1.5.2 / 1.6.4 mod of the test sets uses it) |
| 1.12.2 | `@WailaPlugin` / `IWailaPlugin.register(IWailaRegistrar)` (HWYLA 1.8 API incl. `ModuleRegistrar`, `ConfigHandler`, `ITaggedList`), block and entity providers, NBT providers | `IConfigureNEI` + `API.hideItem` (items hidden in JEI); recipe handlers do not exist on 1.12 NEI | capability API |

Multipart (FMP) providers, block decorators and custom tooltip renderers are accepted and ignored.

## Bridges at a glance

Two kinds of bridge exist. *Library bridges* replace an outdated legacy library by its modern equivalent (the table
above); *engine bridges* map a legacy system onto the 1.21 one it corresponds to. Status for 1.7.10:

**Implemented**

| Bridge | Legacy side | Modern side |
|---|---|---|
| Baubles → Curios | `baubles.api` | Curios slots |
| NEI → JEI | `codechicken.nei` | JEI categories and hidden items |
| Waila → Jade | `mcp.mobius.waila.api` | Jade tooltips |
| Patchouli 1.12 → Patchouli | `vazkii.patchouli.api` | books, multiblocks, custom components ([details](#patchouli-books)) |
| CoFH RF → NeoForge Energy | `cofh.api.energy` | `IEnergyStorage` (1 RF = 1 FE) |
| Blocks and tile entities | `Block`, `TileEntity`, metadata | registered blocks and block entities, one per legacy block plus one per block subtype that has an item ([flattening](#block-subtypes-flattening)) |
| Items and ItemStacks | `Item`, `ItemStack` damage/NBT | one item per metadata subtype, `CUSTOM_DATA` NBT |
| Recipes and smelting | `GameRegistry` / `CraftingManager` / `OreDictionary`, custom `IRecipe` classes (IC2, AE2, Blood Magic, Witchery ...) | data-pack recipes, tags; a custom recipe is a `legacyconnector:legacy_recipe` that matches and crafts through the legacy object |
| Entities | `Entity`, spawn/save/load, mob AI | entity types, spawns, saving |
| Fluids | Forge `Fluid` / containers, fluid blocks, buckets | fluid types; 1.12.2: source/flowing fluids held by the fluid blocks and buckets ([details](#1122-fluids)) |
| Villagers | `VillagerRegistry` ids, skins, trade handlers, village creation handlers | `minecraft:villager_<id>` professions (a vanilla villager with another job, so Jade names its mod "Minecraft"; named after the mod's skin file, e.g. "Wizard"), trades, clothes layer; some new unemployed villagers take a legacy profession; legacy village buildings are elements of the vanilla village house pools with their 1.7.10 share (see [World generation](World-Generation.md)) |
| GUIs and containers | `GuiHandler`, `Container` | menus and screens |
| Networking | FML `SimpleNetworkWrapper` / channels | NeoForge payloads |
| Commands | `ICommand`, server command manager | Brigadier |
| Dimension physics | `WorldProvider` gravity, fall damage and sound attenuation (Galacticraft's `IGalacticraftWorldProvider`) | gravity attribute, fall damage multiplier, client sound volume per dimension |
| Chest loot | `ChestGenHooks.addItem` for dungeon, mineshaft, pyramid, stronghold, village and bonus chests | an extra pool on the matching vanilla chest loot table; chests that legacy structures fill from one of these categories get the matching modern loot table instead, so the pack's loot (and modern mods' loot modifiers) applies |
| Fishing loot | `FishingHooks` fish, junk and treasure entries (MineFactoryReloaded's junk) | a global loot modifier swaps a catch of the same category for a mod entry with its 1.7.10 weight share |
| Brewing | `Item.getPotionEffect` ingredients, `PotionHelper` potion values and requirements | one brewing recipe running 1.7.10's potion values: mod ingredients and mod-effect potions brew as in 1.7.10 (redstone, glowstone, gunpowder apply), vanilla potions stay vanilla, water breathing (pufferfish) is back |
| Biomes, world types, mod dimensions | `BiomeGenBase`, `GenLayer`, `WorldProvider` | data-driven biomes placed by climate (a TerraBlender region), legacy world types as their own biome source, `legacyconnector:dim_<id>` |
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
| 1.20.1, 1.16.5, 1.2.5 bridges | planned, in that order |

## Patchouli books

With the modern Patchouli (NeoForge 1.21.1) installed, 1.12.2 mods' Patchouli books work through it and the 1.12 Patchouli jar is skipped
(`bridges.patchouli.enabled = false` loads the 1.12 jar instead; without the modern mod it loads as usual).

* **API** (`legacyconnector/v1_12_2/vazkii/patchouli/api`, implementation in `compat/patchouli`): `PatchouliAPI.instance` with `getBookStack`,
  `openBookGUI`, `setConfigFlag`/`getConfigFlag`, `registerMultiblock`/`getMultiblock`/`makeMultiblock`, the state matcher factories (1.12
  `IBlockState` and `Block` arguments become modern states), `registerTemplateAsBuiltin`, `serializeItemStack`/`deserializeItemStack` and
  the ingredient variants. Multiblocks and matchers are built when first used and registered with Patchouli once every mod's blocks exist
  (1.12 mods build them in preInit). `ICustomComponent` and `IComponentProcessor` implementations are also modern ones, so the class names in
  a book's json work; `@VariableHolder` string fields are filled from the page variables. The `BookEvent` classes exist but are not posted yet.
  Classes mods use that are not API (`vazkii.patchouli.client...`, `common...`) are not provided.
* **Books as resources.** 1.12 mods keep books in `assets/<mod>/patchouli_books/<book>/` (`book.json`, `<lang>/{categories,entries,templates}`).
  The modern Patchouli (1.20+) finds books by scanning each mod jar for `data/<mod>/patchouli_books/<book>/book.json` and requires
  `use_resource_pack: true`, with the contents in the resource packs (`assets/`), as in 1.12. So the loader writes the converted `book.json`
  into the translated jar's data folder (`PatchouliBookJson`, no registries needed) and stages the original folder under
  `legacyconnector_legacy/` (the originals stay in assets for the 1.12 Patchouli); the runtime writes the converted contents to the generated
  client resource pack when it is built (`PatchouliBooks`, unit tested; `PatchouliBooks1122` supplies the ids from the registries):
  items `mod:item:meta` become the flattened item (`mod:item_3`), also in icons, `spotlight` pages and template components; multiblock
  `mapping` blocks go through the state bridge (`mod:block[meta=3]`); `crafting` recipe names become the id of the data-pack recipe the
  layer generated (`mod:name_code`); `smelting` pages (1.12 names the input item) become recipe pages of the generated furnace
  recipe, or a `spotlight` of the input when there is none; page types get `patchouli:`; template pages and includes get the book's
  namespace; template component types get `patchouli:` (the modern reader drops unknown types, which left every template page blank);
  `advancement` locks are kept for vanilla advancements and for the mod advancements the layer converted (see [1.12.2 advancements](#1122-advancements));
  a lock on any other advancement would hide the entry for good and is dropped; values 1.12 processors return are read as modern ids (stacks are
  serialized with the modern item id, renamed vanilla entities such as `zombie_pigman` get their modern name); resource ids are lower-cased;
  `creative_tab` maps the 1.12 tab names to modern tabs (an unknown tab is dropped).
* **The book item.** `getBookStack("mod:book")` returns the modern guide book stack with its `patchouli:book` component, wrapped for legacy
  code (the `patchouli:guide_book` item is visible in the legacy item registry, so 1.12 recipes and loot that name it work, and a stack with
  the 1.12 NBT tag `patchouli:book` becomes the modern book stack and back).
* **Other legacy versions.** The loader bridge (`LibraryBridges`: `Bridge("patchouli", "vazkii/patchouli/api/", "patchouli", "patchouli")`,
  `bridges.patchouli.enabled`, skipping the legacy Patchouli jar) does not depend on the Minecraft version: it applies to every legacy version
  that is or will be added. Patchouli exists for 1.12.2 and for 1.14 to 1.20; the 1.20.1, 1.19.2, 1.18.2 and 1.16.5 layers are in-place remaps
  onto NeoForge 1.21.1, so their `PatchouliAPI.get()` calls resolve against the real modern API directly and only the books need converting.
  Each version adds one `BookConverter` (interface and registry `BookConverter.BY_VERSION`, with a unit test): 1.12.2 is the table above; for
  1.14 to 1.20 the files are already in `data/` with the modern structure, and the converter has to turn `mod:item{nbt}` stacks into
  `mod:item[components]`, move the contents to the resource pack (`use_resource_pack`), and map renamed fields (`extend`, multiblock `pattern`
  keys, `advancement`/`flag`). No converter is registered for them yet because those layers do not exist.

## 1.12.2 fluids

`FluidBridge1122` registers every Forge 1.12.2 `Fluid` the way a 1.21.1 mod adds a fluid. Each one gets a `FluidType` (density, viscosity
as swim speed, temperature, luminosity, gaseous fluids are not swimmable, fill/empty sounds, the legacy name; on the client
`client/Fluids1122` gives it the legacy still/flowing/overlay textures, added to the block atlas, and its colour). A fluid whose block is a
fluid block (`BlockFluidClassic`, `BlockFluidFinite`, any `BlockFluidBase`, or a mod's `BlockLiquid`) gets a source and a flowing
`BaseFlowingFluid` (`<mod>:<fluid>`, `<mod>:flowing_<fluid>`), and its block (`Legacy1122FluidBlock`) holds their states: level 0 is the
source, other levels are flowing states with the level scaled to 1-8 (`quantaPerBlock`; finite fluids by their quanta). So the fluid renderer
draws it in the block's legacy render layer (the block model is not drawn), entities swim and are pushed through vanilla code, and worldgen
lakes placed with the fluid block are fluids too. The legacy block still flows by its own `updateTick` (the modern fluid never ticks or
spreads), so `canDisplace`, `displaceIfPossible`, `onEntityCollision` effects, mixing in `neighborChanged` and `drain`/`place` keep running.
A fluid with a block or a universal bucket (`FluidRegistry.addBucketForFluid`) gets a bucket: the mod's own `ItemBucket(fluidBlock)` when
it has one, else a generated `BucketItem` `<mod>:<fluid>_bucket` (NeoForge's fluid container model: the vanilla bucket with the fluid's
texture and colour; in the Tools tab). The empty vanilla bucket picks a source up through the legacy `drain` (`BucketPickup`); a filled bucket
places the legacy block at level 0. Legacy code sees the generated bucket as Forge's universal bucket with the fluid in its tag (Forge's
format), so `FluidUtil.getFilledBucket`, `UniversalBucket.getFilledBucket(ForgeModContainer.getInstance().universalBucket, fluid)` and
`FluidBucketWrapper` work even in preInit, before the modern bucket exists, and recipes using that stack get the modern bucket. The buckets
have NeoForge's fluid item capability (tanks and pipes fill and empty them). Not mapped: a fluid block that is neither `BlockFluidBase` nor
`BlockLiquid` (a bare `IFluidBlock`) and a `BlockLiquid` of no registered Forge fluid stay plain legacy blocks; gases render like liquids
(the modern fluid renderer has no upward fluids).

## 1.12.2 advancements

1.12 mods ship advancements as `assets/<mod>/advancements/**.json` (read from the mod's jar); the modern game reads `data/<mod>/advancement/**.json` in
another format. `Advancements1122` converts every file when the data pack is built (item subtypes, entity types, recipes and biomes need the
registries) and serves the result from the bottom data pack, so world data packs replace or remove them like any mod's advancement; the
conversion rules are `AdvancementConverter1122` (pure JSON, unit tested). The server logs `1.12.2 advancements of mod jars: N converted ...` and,
once started, `1.12.2 advancements loaded by the server: X of N`.

- **Display:** icon `{item, data}` becomes `{id}` of the flattened item (an unknown item shows a barrier), title/description text components,
  `frame`, `background`, `hidden`, `show_toast`, `announce_to_chat` and `parent` carry over (mod advancements whose parent does not exist become roots).
- **Rewards:** `experience`, `recipes` (ids of the generated data pack recipes, `mod:name_code`) and `loot`; `function` is dropped (logged).
- **Vanilla triggers** become the modern conditions (`items` with `data` become the list of the item's flattened subtypes): `inventory_changed`,
  `consume_item`, `enchanted_item`, `item_durability_changed`, `used_totem`, `filled_bucket`, `placed_block`, `enter_block`, `recipe_unlocked`, `tick`,
  `location` (biome, dimension, position, vanilla structures), `changed_dimension`, `effects_changed`, `player_killed_entity`, `entity_killed_player`,
  `player_hurt_entity`, `entity_hurt_player`, `tame_animal`, `bred_animals`, `summoned_entity`, `villager_trade`, `cured_zombie_villager`,
  `construct_beacon`, `brewed_potion`, `levitation`, `nether_travel`, `impossible`. Entity predicates become `entity_properties` conditions.
  What cannot be expressed (an entity, item or block the layer did not register, a structure of a mod's own world generator, a trigger 1.21 does
  not have) makes that criterion `minecraft:impossible` and is listed in the log once per kind; NBT and damage-type conditions are dropped.
- **Triggers of mods** (`CriteriaTriggers.register(new MyTrigger())`): the criterion becomes `legacyconnector:legacy` carrying the original conditions;
  when a player's advancements start listening, the mod's own `ICriterionTrigger.deserializeInstance` parses them and its `addListener` gets a
  legacy listener whose `grantCriterion` awards the criterion. Mods keep firing their triggers from code as in 1.12.
- **Code:** `AdvancementManager.getAdvancement`, `EntityPlayerMP.getAdvancements().grantCriterion/revokeCriterion/getProgress` are views over the
  modern server's.
- **Not done:** 1.12 `.mcfunction` rewards, `killing_blow` and damage-type conditions, item/entity NBT conditions.

## 1.7.10 achievements

1.7.10 mods' achievements (`new Achievement(...).registerStat()`) and `AchievementPage`s are written to the generated data pack as advancements
(`Achievements1710`): each page is a tab (a generated root named after the page; achievements of no page, or hung off vanilla ones, are in a
"Legacy achievements" tab), parents are kept, the icon is the achievement's item, title and description are the achievement's translation keys.
The criterion is `impossible` and is awarded from `EntityPlayer.addStat` when legacy code earns the achievement (and at login for those already
recorded), so the advancements screen shows what the legacy record says. The legacy toast stays; the advancements do not toast or announce.

## Possible book bridges

Other book systems that could be shown as modern Patchouli books by a generic converter (not implemented; the effort is for a generic
conversion of the content into `book.json` + entries, not a per-mod fix):

| Legacy book system | Modern equivalent | Effort |
|---|---|---|
| Guide-API (1.7.10 and 1.12 `amerifrance.guideapi`) | Patchouli (or the Guide-API port) | Medium: books are registered in code (`IGuideBook`, categories, entries, pages of text/recipes/images); a generic walk of the registered structure could write Patchouli categories and entries |
| Thaumonomicon (Thaumcraft 4/6 research) | none (progression system, not a book) | High: research has prerequisites, aspects and a puzzle UI; only a read-only entry list is realistic |
| Lexica Botania (Botania 1.7.10 lexicon) | Botania's own Patchouli book | Low for the book itself (modern Botania ships it), but the legacy lexicon is mod-internal; only entry text could be exported |
| Tinkers' Construct manual (1.7.10 and 1.12) | Tinkers' Construct's modern Patchouli-style manual | Medium: pages are an in-jar XML and a custom page renderer; text/image/recipe pages map, interactive ones do not |
| Forestry and Mystcraft books | none | Forestry's handbook is not a book system; Mystcraft's notebooks are items holding data, not documentation |
| NEI handbook / Waila-style help | JEI | Already covered by the NEI bridge |

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

## Energy per version

Every legacy energy system is exposed as NeoForge's `Capabilities.EnergyStorage` (blocks and items) by default, in both directions: a legacy producer can feed a modern
consumer (the capability's `extractEnergy` calls the tile) and a modern producer can feed a legacy machine (its `receiveEnergy`). Modern block entities that offer
the capability are also visible to legacy code (1.7.10: `ModernCapabilityTile` is an `IEnergyHandler`; 1.12.2: a tile entity answering `CapabilityEnergy.ENERGY` and implementing
`IEnergyReceiver`/`IEnergyProvider`), so legacy generators and cables push into them. Rates are in [Configuration](Configuration.md).

| API | Versions | Adapter | Rate |
|---|---|---|---|
| CoFH RF `IEnergyReceiver` / `IEnergyProvider` / `IEnergyHandler` tiles, `IEnergyContainerItem` items, `EnergyStorage`, `TileEnergyHandler`, `ItemEnergyContainer` | 1.7.10 | `EnergyBridge` | 1 RF = 1 FE |
| CoFH RF monolithic `IEnergyHandler` (`canInterface`) | 1.6.4, 1.5.2, 1.4.7 (late builds) | forwarded to the 1.7.10 shim; a bridge method adds `canConnectEnergy` | 1 RF = 1 FE |
| Redstone Flux `cofh.redstoneflux.api` (`IEnergyReceiver`, `IEnergyProvider`, `IEnergyHandler`, `IEnergyConnection`, `IEnergyStorage`, `IEnergyContainerItem`) and `.impl` / `.util` helpers | 1.12.2 | `EnergyBridge1122` (tiles and items) | 1 RF = 1 FE |
| Forge Energy `CapabilityEnergy.ENERGY` on tiles and items | 1.12.2 | `BuiltinAdapters.energy1122`, `ItemCapabilities1122` (entities are not bridged) | 1:1 |
| IC2 EU `IEnergySink` / `IEnergySource` tiles, `IElectricItem` items | 1.7.10 only | `ForeignEnergy1710` (reflection) | `energy.fePerEu` = 4 |
| BuildCraft MJ `IPowerReceptor` (`PowerHandler` 4.x, `IPowerProvider` 3.x) | 1.7.10 only | `ForeignEnergy1710` (receive only) | `energy.fePerMj` = 10 |
