# Legacy Beta 1.7.3 (ModLoader and Forge 1.0)

Status and design of Minecraft Beta 1.7.3 support. Back to [Home](Home.md). Started 2026-11-06.

**Status: started (blocks, items, recipes, textures, block behaviour, GUIs).** Beta 1.7.3 ModLoader / ModLoaderMP / Forge 1.0.x jars are
detected, deobfuscated and linked; the mods are constructed as ModLoader did, their blocks, items and recipes become modern content with
their own textures, one creative tab per mod, and the blocks' own code runs (placing, breaking, neighbour updates, ticks, right and left
click, redstone) with their tile entities (saved, loaded, ticked) and their GUIs (machines, furnaces, crafting tables as modern menus). Not
yet: entities, world generation, the mods' tick hooks.

## Putting the mods in

Put b1.7.3 mods in `mods/b1.7.3/`. Jars and zips are both read there (most ModLoader mods were distributed as zips). A jar directly in
`mods/` is also recognised when it has a `mod_*` class or references the ModLoader/Forge API and at least 60% of its references to the
game resolve in the b1.7.3 names (ModLoader mods for 1.0 to 1.2.5 look the same but name other members). Library jars without a
`mod_*` class (OverrideAPI) are only recognised in `mods/b1.7.3/`. Many mods came as a client build and a ModLoaderMP server build
with the same `mod_X` class; when both are present, the build for the running side loads and the other is skipped (logged).

## How a jar is translated

1. **Detection** (`loader/.../BetaJars`). No `mcmod.info`, no `@Mod`: a jar is a b1.7.3 mod by its `mod_*` classes (ModLoader's entry
   points, default package) or its references to `BaseMod`, `ModLoader`, `forge/*`. Its references to obfuscated game members are resolved
   against the client and the server names: the side with more hits is the jar's side (the b1.7.3 client and server were obfuscated
   separately: `Block` is `uu` on the client and `na` on the server). Each `mod_X` class becomes one legacy mod `X`.
2. **Deobfuscation** (`loader/.../transform/BetaDeobfuscator`), before the jar goes through the usual relocation:
   - game classes and members get the names of [biny](https://github.com/calmilamsy/biny-mappings) through
     [Babric intermediary](https://github.com/babric/intermediary) (both CC0; `uu` becomes `net/minecraft/block/Block`). The table is
     built at build time by `:loader:genBetaMappings` (`BetaMappings`, never committed);
   - obfuscated member names are unique only per class, so each reference is resolved up the hierarchy (the jar's own classes, the
     other b1.7.3 jars of the mod set, then the game) to the game class that declares it. A mod method that overrides a game method is
     renamed with it; a mod method whose new name would clash with another method of its class keeps its obfuscated name;
   - the ModLoader API (`BaseMod`, `ModLoader`, `MLProp`, ...) and Forge's `forge/*` are named `net/minecraft/src/...`, as in MCP;
     bundled copies of them are dropped (the layer provides them);
   - mod classes in the default package (not allowed in a Java module) or in `net/minecraft/` move to
     `legacyconnector_b173/<mod>/`. Addons refer to the classes of the mod they extend (`mod_BuildCraftCore`), so the moved names are
     indexed over every b1.7.3 jar of the set;
   - a default-package class no jar of the set has is another mod's API that came as a jar mod or an installer (ShockAhPI's
     `DimensionBase`, `SAPI`, `ToolBase`, `ACPage`; PlayerAPI's `PlayerBase`; GuiAPI's `ModSettings`): it is named `net/minecraft/src/...`
     too, so it links to the shim or the synthesized API instead of failing with `NoClassDefFoundError` (Aether constructs with them);
   - copies of game classes in the jar are **base class edits** (jar mods patched Minecraft directly). They are dropped and reported
     ("Dropped N modified Minecraft class(es)"); the mod misses whatever they changed. Like Apron, the layer cannot apply them.
3. **Relocation and linking**, as for 1.7.10: `net/minecraft/` moves to `legacyconnector/vb1_7_3/`; everything the shim does not have
   is synthesized from `loader/src/main/resources/legacyconnector/legacy/b1.7.3/` (`hierarchy.txt`, `fields.txt`, `methods.txt`: the
   game, ModLoader, ModLoaderMP and Forge 1.0.7 with client and server merged; Forge's additions such as `Block.isLadder` included).

String constants are not renamed: ModLoader finds mods by simple class name, and reflection by obfuscated member name
(`ModLoader.getPrivateValue(cls, obj, "a")`) must be mapped at run time by the shim (the table has the reverse direction for that).

## Runtime (server side)

`vb1_7_3/PlatformB173` runs the ModLoader lifecycle:

1. **Vanilla first** (`VanillaB173`): `Block.BLOCKS` / `Item.ITEMS` get a view of every b1.7.3 vanilla block and item at its original id
   (ids read from the game's own static initialisers by `tools/extract_beta_vanilla_ids.py` into `vanilla-ids.tsv`), mapped to the modern
   block/item (metadata variants such as wool and dye colours, logs and slabs to the flattened items). `Block.STONE` and the other constants
   are served on first read by a static resolver, as for 1.12.2.
2. **Every mod class is loaded and its `@MLProp` config read** (`config/mod_X.cfg`, written with the defaults), then block ids two mods
   share are moved apart: an `@MLProp` int named `...ID` with a block id that vanilla, another mod's block or an earlier mod already has gets
   the highest free id, saved in that mod's config (NetherCraft and ComputerCraft both default to 207). Forge's `Configuration` hands out
   ids the same way it did, and also skips ids other mods reserve through `@MLProp` (MineFactory's conveyor and RedPower Array, which
   creates its blocks only in `ModsLoaded`, both default to 125).
3. **Construction**: the `mod_*` class is constructed (a ModLoader mod registers everything from its constructor). A clash found only
   now ("Slot N is already occupied") moves that `@MLProp` id, or the Forge config property holding it (a config file written before the
   other mod was installed: Minefactory's conveyor at 253 next to Aether's purple flower), and constructs the mod again. Ids hard-coded in
   the mod (Moon Mod's lamp, 110, like Portal Gun's spawner) cannot move. A mod that fails gives its block and item ids
   back. Every block and item belongs to the mod whose jar defines its class (RedPower Array's `ModsLoaded` initialises Logic and Wiring).
4. **`ModsLoaded()`** for every mod, then **registration** (`BridgeB173`, registries are still open): each mod block becomes a
   `LegacyB173Block` (`mod:name` from its translation key, e.g. `nether:netherrack_ore`, else `mod:block_<id>`) with the legacy hardness,
   resistance, light, sound, slipperiness, material flags and bounds, one `meta` property (0-15) like 1.12.2, and drops asked from the
   legacy block (`getDroppedItemId/Count/Meta`); its `ItemBlock` becomes a modern `BlockItem` (metadata through the `block_state`
   component), other items become modern items (stack size, durability). `ModLoader.AddName` becomes the display name.
5. **Recipes** (`RecipesB173`): `AddRecipe`, `AddShapelessRecipe` and `AddSmelting` become data pack recipes when the pack is built
   (Block ingredients match any metadata, as in b1.7.3: any wool colour).

Texture overrides keep ModLoader's sprite indices (same free slots of terrain.png / gui/items.png); past the 256 slots of a sheet the
indices go on instead of failing ("No more empty item sprite indices left!" stopped NetherCraft and Portal Gun together), as InfSprites
allowed.

The server test logs `b1.7.3 content registered: ...` and, with `-Dlegacyconnector.entitySelfTest=true`, `B1.7.3 SELF TEST` (every
mod block placed, read back and asked for drops).

## Client assets

`AssetsB173` (a generated-pack contributor) turns sprite indices into textures and models:

- **Textures.** A b1.7.3 block names its sprites by index: `getTexture(side, meta)` into terrain.png, or into the block's own 256x256
  sheet when it is a Forge `ITextureProvider` (`getTextureFile()`); items likewise into gui/items.png. A mod's own sprite is a
  `ModLoader.addOverride` image (read from the mod's jar) at the index ModLoader handed out, a sheet sprite is that tile of the sheet; both
  are written as `assets/<mod>/textures/block/b173/*.png`. An index without either is the vanilla sprite at that place, mapped to the modern
  texture (tables of the terrain.png and gui/items.png layouts in `AssetsB173`, with the wool and dye formulas of BlockCloth and ItemDye);
  doors' negative indices select the absolute sprite index and mirror the face UVs.
- **Animated sprites.** `BaseMod.RegisterAnimation`, `ModLoader.addAnimation`, `TextureManager.addDynamicTexture`,
  `DynamicTexture` (Beta `TextureFX`) and `ModTextureAnimation` use shared APIs. Effects load their original resources,
  select terrain/items or a custom Forge sheet through `bind`, and retain their own tick code and RGBA frame buffers.
  Initial frames become generated atlas sprites; client texture ticks upload subsequent frames to the stitched locations
  and regenerate their mip levels. Resource reloads recreate hook-owned effects without accumulating registrations.
  Replicated tiles and multiple callbacks sharing a sprite retain Beta's upload order. No mod-specific animation rules.
  `compat/b173/AnimationSelfTest` compares the real factory and fizzler callbacks with their source PNG frames, checks
  conveyor sprite routing, shared-slot ordering, resource reloads and ModLoader strip timing. The server test verifies
  frame delivery without launching a graphical client; final GPU appearance still requires an in-game check.
  The supplied set passes all 8 factory effects and the portal fizzler over 32 ticks, including wraparound,
  with exact RGBA comparisons against the original horizontal strips.
- **Models.** One block model per metadata, deduplicated: faces down, up, north, south, west, east are b1.7.3's sides 0-5, the block's
  metadata-dependent bounds are the element (doors, slabs, plates). Render types 1 (crossed plants), 2 (floor/wall torches), 6 (crops)
  and 10 (oriented Beta stairs) use the vanilla model templates; other custom renderers fall back to their bounds. Non-opaque blocks are cutout, render layer 1 translucent, and so is any block whose sprites
  have transparent pixels (b1.7.3 drew all terrain with an alpha test). The blockstate maps `meta=0..15`.
- **Vanilla base classes with their behaviour.** Mods extend vanilla block classes; synthesized ones have none (NetherCraft's glass,
  a `BlockBreakable`, said it was an opaque cube and drew black). The shim has `TranslucentBlock` (glass base: not opaque, no faces
  between two blocks of the same id), `GlassBlock`, `IceBlock`, `TransparentBlock` (leaves base), `PlantBlock`, `TorchBlock`, `StairsBlock` and `DoorBlock`.
  Block items use the block model (flat sprite for plants and torches), items a flat or handheld sprite.
- **Block item subtypes.** One legacy block item is one modern item (`LegacyB173BlockItem`); the stack's damage is the metadata in its
  `block_state` component. An item with subtypes (Minefactory's machines, IndustrialCraft 2's machines, generators and storage blocks,
  RedPower's gates and covers are each one block with a metadata per kind) gets a model override per metadata on the item property
  `legacyconnector:b173_meta` (metadata / 16; registered on the client for these items) pointing to that metadata's block model (or its
  own flat icon from `getIconFromDamage`), and a name per metadata from the legacy `getItemNameIS(stack)` (`block.<mod>.<block>.m<meta>`).
- **Names**: `ModLoader.AddName` / `AddLocalization`, else the translation key made readable.
- **Creative tabs** (`CreativeTabsB173`): b1.7.3 had no creative inventory, so each mod gets one tab with its blocks then its items; a block
  item with subtypes lists each metadata whose sprites differ.
- A mod that re-creates a vanilla block or item at its vanilla id (NetherCraft's tools) keeps the vanilla 1.21.1 one: no new entry, model
  or tab slot.

The server test generates the assets too (`-PassetsDump=<dir>` writes them), which is how they can be checked without a client.

OpenAL: b1.7.3 mods call LWJGL's OpenAL directly (IndustrialCraft 2's audio). Initialised on the mod-loading thread, before the sound
engine made a context, the real AL classes fail for good and the client's first resource reload never finished (stuck loading screen).
`org/lwjgl/openal/` of b1.7.3 jars goes to the synthesized API (silent) instead (`LegacyRemapper`).

## Block behaviour

The modern block (`LegacyB173Block`) calls the legacy block's own code with a legacy view of the level (`World`, shim):

| Modern | b1.7.3 (MCP name) |
|---|---|
| `onPlace` / `onRemove` (another block) | `onBlockAdded` / `onBlockRemoval` |
| `neighborChanged` | `onNeighborBlockChange` (the neighbour's legacy id) |
| `tick` / `randomTick` | `updateTick` |
| `animateTick` (client) | `randomDisplayTick` (particles: b1.7.3 names such as `smoke`, `reddust`, `portal`) |
| `useWithoutItem` / `attack` | `blockActivated` / `onBlockClicked` |
| `getStateForPlacement` / `setPlacedBy` | `canPlaceBlockOnSide`, then `onBlockPlaced(side)` and `onBlockPlacedBy(player)` |
| `stepOn` / `entityInside` (players, mobs, dropped items and other entities) | `onEntityWalking` / `onEntityCollidedWithBlock` |
| Shape/collision queries | `updateBoundingBox`, `getCollisionShape`, `addIntersectingBoundingBox` |
| `isSignalSource` / `getSignal` / `getDirectSignal` | `canProvidePower` / `isPoweringTo` / `isIndirectlyPoweringTo` (b1.7.3 sides are modern 3D direction values) |
| `playerDestroy` | `onBlockDestroyedByPlayer`, and a mod's own `harvestBlock` instead of the modern drops |

- **The world view** (`World.mcc$wrap(level)`): block ids and metadata of modern states (`BlocksB173`: mod blocks by their `meta`
  property, vanilla blocks by their b1.7.3 id with wool/log/leaf/sapling/slab metadata and wheat's age; other modern blocks read as stone
  when solid, else air), block changes with or without neighbour notification (flags 3 / 2), scheduled ticks, redstone power, light, time,
  materials of vanilla blocks (judged from the modern block: fluid, plant, leaves, wood by its sound, ...), `dropBlockAsItem`.
- **Players** are views of the modern player (`PlayerViewB173`): position (eye height as b1.7.3 had it), rotation, sneaking, health and the
  36 main slots as legacy stacks; what the mod does to those slots (a used-up item) goes back to the modern inventory.
- **`ModLoader.getMinecraftInstance()`** is one legacy client instance whose `world` (`theWorld`) is the level of the callback being run:
  IndustrialCraft 2 decides by `mc.theWorld.isRemote` whether it runs the game logic.
- Every callback is guarded: a failure is logged the first three times per block and counted.

Block shapes now use a read-only world view (`BlockQueryB173`) and restore the legacy singleton's bounds after
each query. Collision boxes can form a union, so stairs have two steps instead of a full cube. Stair placement
uses Beta's four horizontal orientations. Torches attach to floors/walls, have no collision, and drop when their
support is removed. Doors use two metadata-bearing halves, thin rotating collision/model bounds, wooden-door
click behavior, redstone opening/closing, and paired removal when their support or other half disappears.
Pressure plates have inset, non-colliding outlines and models, pressed/unpressed heights, the original
entity activation filters, binary weak power, strong power into their support, scheduled release checks,
and support-loss removal.

Placed face textures use `getTextureId(BlockView, x, y, z, side)`, rather than the inventory-only callback.
Metadata models preserve furnace facing; chunk model data queries the render region for neighbour-dependent
chest fronts and paired textures. Dynamic sprite palettes are included before atlas stitching.

Entity views distinguish dropped items from living entities and apply changed velocity/ground fields after a
block callback. MineFactory's original conveyor callback therefore moves dropped items, mobs and players,
including slope metadata, sneaking bypass and redstone stopping. `isEmittingRedstonePower` checks weak sources
and power through adjacent solids; `canTransferPower` checks strong power entering a block. The earlier world
bridge had these two queries swapped. Custom render IDs now dispatch to their registering mod's
`RenderWorldBlock` callback. Shared `BlockRenderManager` cuboid helpers and a thread-local `Tessellator`
capture geometry and UVs for modern baked quads. This runs the original conveyor renderer, including
direction and slopes, with no mod-specific rendering rules. Animated atlas sprites use the shared TextureFX bridge;
arbitrary GL rendering remains unsupported. Renderer failures are logged and retain the generated bounds fallback.

`compat/b173/BlockFamiliesSelfTest` uses installed NetherCraft stairs/doors, NetherCraft/Aether torches,
IndustrialCraft doors and MineFactory conveyors. It checks modern placement, metadata, collision volumes,
generated models, support removal, door clicks/redstone, and all 12 conveyor states for items/mobs/players,
plus actual dropped-item ticks, pressure plate activation/release, furnace placement faces, chest neighbour
and paired textures, and the original conveyor renderer's vertices/UVs in all 12 states.
No graphical client is launched by these tests.
The supplied set passes 2 stair blocks, 3 torch blocks, 2 door items, 1 pressure plate, 2 furnace blocks,
1 chest block and the conveyor (all 12 metadata states for all three entity categories and original
rendered geometry/UVs, with direct-source and through-solid redstone checks).

The server test runs `B1.7.3 BEHAVIOUR TEST`: every mod block placed, given a neighbour change, a scheduled and a random tick, a right click by
a fake player, asked for redstone power and removed. With the 11 mods: 10 callbacks fail, all of blocks whose tile entity is missing (NetherCraft's
furnace and chest, Portal Gun's portals); 6 blocks use the right click. RedPower's lamp switches itself off without power.

## Item interactions

Ordinary mod items are hosted by `LegacyB173Item`. Main-hand use on a block calls the legacy `Item.useOnBlock`
with the clicked position and Beta side number; right-click in the air calls `Item.use`. Both use the existing
world and player views and bind `Minecraft.world` to that callback's level. Inventory changes are written back
on the server, including a returned replacement stack or null (the held stack was consumed). A mod can open
a menu through `ModLoader.OpenGUI` from either callback, using the same bridge as block GUIs.

An unhandled block use passes through. Items without an overridden air-use callback also pass through.
Offhand use passes through without calling Beta code (Beta only had a selected inventory slot).
Callback exceptions are contained and logged, at most three per item. Individual items can still need missing legacy APIs.
Block items retain their modern placement
path; custom block-item callbacks, entity interactions, combat, eating animations and inventory ticks are separate work.

The development server's `B1.7.3 ITEM USE TEST` checks position/side/world, consumption, returned replacements,
direct inventory replacement, preservation of unrelated modern items, offhand pass-through and an item callback
opening a real 63-slot server menu. The test fixture is registered only when the self-test flag is enabled.

### Buckets and bows

Fluid buckets now have actual raycasts, block hit results and permission checks. Raycasts distinguish source
fluids from flowing fluids; the world view supplies the dimension's water-evaporation flag. Lava is classified
without fluid tags, because those tags are not bound when the vanilla views are initialized. Plain-item metadata
round-trips through the stack's custom-data component, with item models for metadata 0–15: Aether's skyroot
bucket stores water as metadata 8 rather than as a different item. Waterlogged solid blocks are not exposed as
standalone legacy fluid blocks.

Arrow-like entities with `setArrowHeading` are hosted by `LegacyB173Arrow` (`legacyconnector:arrow_b1_7_3`).
The server runs the mod's original tick and pickup code: its velocity, collision tests, damage, fire effects,
embedded-arrow behavior and returned ammunition. Entity views connect queries and damage to modern targets;
direct writes to a target's fire timer are applied after the tick. Bounds follow both position-setting APIs.
The modern host provides entity tracking and saves the legacy class and NBT, including arrows a mod did not
register with `RegisterEntityID`; restoration only accepts arrow classes from loaded Beta mod modules.
Shooter identity is stored by the modern projectile. Bow/fireball/hit sounds use modern sound events.

Client arrows use the standard arrow geometry with the mod's own PNG when its registered renderer binds one
constant texture. `ProjectileTexturesB173` reads `AddRenderer` registrations and delegated registration methods
without loading client renderer classes on the server. The generated resource pack includes the original PNG;
the host synchronizes the texture identifier and restores it after loading. Conditional/animated texture
selection and custom geometry still need their renderer bridges. Skyroot
milk/poison/remedy effects and mod creatures are separate work.

Snowball/egg-style projectiles with `setSnowballHeading` or `setEggHeading` use the same server host through
`legacyconnector:thrown_b1_7_3`, with a separate camera-facing renderer. A standalone PNG supplies the full
billboard texture; registrations using `ProjectileEntityRenderer(item.getTextureId(0))` retain that item's
modern sprite/model. Texture or item-stack data is tracked to clients and reconstructed after loading.
Shooter fields are recovered by their legacy owner/thrower name or a unique living-entity field, including
private obfuscated fields. This fixes the previously rejected Hammer of Notch wave and NetherCraft slime eggs.
Slime-egg flight, damage and ignition run the mod's code; hatching custom slime mobs still requires the mob bridge.

In the supplied Aether 1.02 jar, the Hammer of Notch wave does damage and emits impact particles; its item and
projectile code do not spawn lightning. Legacy particle calls from server-hosted projectiles now send modern
particle packets to nearby clients, restoring the requested impact effects without inventing a lightning strike.
The actual-wave regression captures 40 client particle packets (poof, smoke, large smoke and flame) and
checks the mod's impact knockback. Its early area damage is distinct from its later collision effects.

`compat/b173/ThrownSelfTest` checks those two actual mod items through right-click, including use cost,
spawn, shooter, renderer data, save/reload, flight, mob impacts and wall impacts. Client appearance needs
an in-game check; the headless tests verify original wave PNG bytes and the slime egg's item sprite.

`compat/b173/BucketBowSelfTest` uses the actual installed NetherCraft and Aether classes through their modern
item entry points. Bucket checks cover source pickup, placement, flowing-fluid rejection, misses and denied
building permission. Bow checks cover four NetherCraft bow classes with each of their four ammunition types,
plus the Phoenix bow: no-ammo behavior, consumption, spawning, flight, mob damage, ignition, wall impacts,
pickup, original projectile PNG bytes and texture save/reload. These checks run with the existing headless server self tests; client appearance still
needs an in-game check.

Validated with NetherCraft 1.9b (ModLoader-only build) and Aether 1.02: both buckets and all five bow classes
pass (17 bow/ammunition combinations). Existing regression checks remain at 113 menus, 29/29 shift-click
cases, 2/2 crafting cases and zero block-callback failures.

### Item durability

Modern mining and melee callbacks invoke the item's original `postMine` and `postHit` methods. `ItemStack.damage`
applies the requested wear, keeps an item at exactly its maximum damage, and breaks it only when damage exceeds
that maximum, matching Beta. Creative players do not incur wear. Right-click callbacks use the same damage API.
Damage is stored in the modern stack component for inventory bars and saving; subtype metadata remains separate.
The `ToolItem`, `SwordItem` and `ToolMaterial` shims supply inherited durability behavior for mod subclasses.

`compat/b173/DurabilitySelfTest` exercises actual NetherCraft tools/swords and Aether Zanite tools/swords,
including mining versus combat costs, persistence, creative use, the last use and breakage. It also checks
NetherCraft's firestarter through the modern right-click entry point and protects Skyroot bucket metadata.
The supplied NetherCraft 1.9b/Aether 1.02 set passes all 22 registered tools/swords and the firestarter checks.
Vanilla item replacements retain the modern vanilla item's behavior.
Armor wear and tool mining-speed/attack attributes are separate work.

## Tile entities

- **NBT** is b1.7.3's own (`net.minecraft.nbt` in the shim: compound, list and the eight value tags, with the binary format of the time);
  `NbtB173` converts it to and from modern NBT (same tag types, nothing lost).
- **Hosting.** A b1.7.3 `BlockContainer` (`BlockWithEntity`) becomes a `LegacyB173EntityBlock`; one modern block entity type
  (`legacyconnector:legacy_tile_b1_7_3`, `TileEntitiesB173`) hosts every legacy tile entity (`LegacyB173BlockEntity`). The legacy one is
  made by the block's `getBlockEntity()` the first time it is asked for (as b1.7.3's chunk did) or loaded from the save: its `writeNbt`
  output (with the id `ModLoader.RegisterTileEntity` gave the class, also the overload with a renderer) is stored under `legacy`.
- **Ticking.** The server ticks it when its class has its own `updateEntity`; one that throws stops ticking (logged once).
- `World.getBlockTileEntity` / `setBlockTileEntity` / `removeBlockTileEntity` reach it; `markDirty` marks the modern block entity changed.
  Clients get the same NBT in the block entity data packet (for renderers later).

`B1.7.3 TILE ENTITY TEST` (server test): with the 11 mods, 21 blocks have a tile entity; all 21 make one, save it and load it back. No
callback of the behaviour test fails (`World.getChunkFromPos` / `getChunk` give a `Chunk` view: RedPower marks chunks dirty through it);
9 blocks use a right click (NetherCraft's furnaces and chest, Portal Gun, IndustrialCraft 2's machines among them).

Java compatibility found on the way: `Arrays.asList(...).toArray()` returned the backing typed array up to Java 8 and code cast it
(`(Integer[]) list.toArray()`, RedPower's power search); since Java 9 it is an `Object[]`. `JavaCompatRedirects` turns a `java.util`
collection's `toArray()` followed by a cast to a typed array into `toArray(new T[0])` (for every legacy version). `Vec3d` and
`EntityLiving.getLook` are hand-written (RedPower traces its click along the player's look).

## GUIs

A b1.7.3 mod opens its GUI from the block's `blockActivated` with `ModLoader.OpenGUI(player, new GuiX(player.inventory, tile))`. That
callback runs on the server's and on the client's level here (as every block callback), so `MenusB173` decides by side:

- **Server, a `GuiContainer`** (`HandledScreen`): a `LegacyB173Menu` (menu type `legacyconnector:legacy_b1_7_3`) hosts its `Container`.
  Slots of the player's inventory become slots of the modern inventory; the others are `LegacySlotB173`s over the legacy slots, so the
  container's inventory, `isItemValid`, stack limit and `onPickupFromSlot` apply (a crafting result uses up the grid). Clicking, dragging
  and syncing are modern; shift-click is the container's `transferStackInSlot` (the menu answers "done" once the slot stops changing,
  since the legacy default returned the unchanged stack and vanilla would repeat forever); `canInteractWith` and `onCraftGuiClosed` are
  called. The progress values the container sends its crafters (`updateCraftingInventoryInfo`) become menu data (32 values; shorts on
  the wire, as in b1.7.3 multiplayer). b1.7.3 stacks are converted, not shared: the modern stack shown for a slot stays the same object
  while the legacy one is unchanged, and modern code changing it in place is written back (`SyncedStackB173`, as `LegacySlot1122`).
- **Drawing.** The client's `LegacyB173Screen` (a modern container screen) calls the GUI's `drawScreen`: `drawGuiContainerBackgroundLayer`,
  the foreground translated to the GUI's corner, buttons; the modern screen draws slots, items, carried stack and tooltips over it and
  handles slot clicks; mouse clicks, keys (LWJGL 2 codes, `KeysB173`) and ticks also go to the GUI. In single player the GUI object the
  server built is handed over (same JVM, by id in the menu's extra data), so it reads the ticking tile entity itself, as b1.7.3's single
  player did. On a dedicated server the client uses the GUI its own block callback built (same class), which gets the menu data through
  `updateProgressBar`; without one (or after an exception) a plain panel with the slots is drawn.
- **Other screens** (no container: a timer's settings, a naming screen) open on the client in single player through
  `LegacyB173PlainScreen`; `Minecraft.displayGuiScreen` opens and closes them. A dedicated server cannot show a screen it built.
- **GUI drawing** goes through `GuiDrawB173`, a sink with plain types only, so the shim's `Gui` (`DrawContext`), `GuiScreen`,
  `GuiContainer`, `GuiButton`, `FontRenderer`, `RenderEngine` and `GL11` load on a dedicated server too; the client's `ClientGuiB173`
  draws into the screen's `GuiGraphics`. `GL11` of b1.7.3 mods is the shim's (colour, texture binding and the matrix stack; state switches
  are left to the modern renderer; the real GL11 is never called, the modern game runs a core profile). `RenderEngine.getTexture(path)`
  numbers textures as before: an image from the mods' jars (`/ic2/sprites/GUIMacerator.png`), else a vanilla sheet in its b1.7.3 layout
  built from the modern textures (`/gui/furnace.png` with flame and arrow from the modern sprites, `/gui/gui.png` buttons from the widget
  sprites, chest, crafting table, dispenser, inventory). Images are uploaded on first use on the render thread.
- **`displayGUIChest`** (`openChestScreen`) opens a vanilla chest menu over the legacy inventory (`LegacyInventoryB173`, whole rows only).
  Vanilla `TileEntityChest` (`ChestBlockEntity`), `ContainerChest` (`GenericContainerScreenHandler`) and `GuiChest` (`DoubleChestScreen`)
  are the shim's, for mods' own chests (Aether's treasure chests extend them).
- **Placing a subtype.** b1.7.3's `ItemBlock` set block and metadata together (`getPlacedBlockMetadata` of the damage), so `onBlockAdded`
  and the tile entity it makes see the subtype; `getStateForPlacement` does the same (before, a Minefactory translocator or crafter
  placed from its item got the tile entity of metadata 0 and opened nothing).
  `dropPlayerItem` drops from the modern player (crafting grids empty themselves into the world on close).
- **Recipes for mods' own tables and furnaces.** `CraftingManager` (`CraftingRecipeManager`) and `FurnaceRecipes` (`SmeltingRecipeManager`)
  are hand-written: recipes added through them become data pack recipes like `ModLoader.AddRecipe`'s; `findMatchingRecipe` and
  `getSmeltingResult` (IndustrialCraft 2's electric and induction furnaces, NetherCraft's furnace) ask the modern recipe manager (the
  vanilla and the converted recipes; smelting results cached per item id). `InventoryCrafting`, `InventoryCraftResult` and `SlotCrafting`
  are the shim's.

`B1.7.3 GUI TEST` uses a real `ServerPlayer` with a connection that sends nothing, exercising `ServerPlayer.openMenu`
(a fake player refuses menus). It also checks that players sharing an entity id receive distinct player views:
every block is placed from its item where it has one (as a player would) and right-clicked, blocks with a tile entity with each metadata
(492 tries with Aether in the set). 113 open a container GUI, 23 container classes (IndustrialCraft 2's iron and electric furnaces,
macerator-type machines, canner, miner, pump, electrolyzer, induction furnace, mass fabricator, generators, nuclear reactor, energy
storage, trade-o-mat; NetherCraft's furnace and crafting table; Minefactory's filter and crafter; Aether's freezer, enchanter, incubator
and treasure chests); the client layout rebuilt from the extra data matches every time; coal shift-clicked into the container and back:
29 of 29; a log in a crafting grid gives planks that can be taken (grid used up): 2 of 2; no container call fails. `FurnaceRecipes` gives an iron
ingot for iron ore, `CraftingManager` a crafting table for four planks.

## Reflection, Forge tables, fire

- **Reflection on vanilla classes.** ModLoader's `getPrivateValue` / `setPrivateValue` take a declared-field index or an obfuscated
  name (IndustrialCraft 2 reads `BlockFire`'s field 0, Portal Gun writes `Achievement`'s field 3, Minefactory reads `RenderEngine`'s field
  11). The shim's and synthesized classes have other fields, so `ReflectionB173` looks both up in b1.7.3's own declared fields
  (`src/main/resources/legacyconnector/b1.7.3/field-order.tsv`: declaration order, readable, client and server obfuscated name, written by
  `tools/extract_beta_signatures.py`) and finds the field by its readable name.
- **Fire.** `FireBlock` keeps b1.7.3's two tables by id (fields 0 and 1); after registration the mod blocks' values go to modern fire
  (`setFlammable`), so IndustrialCraft 2's rubber wood and leaves burn.
- **Forge tool tables.** `ForgeHooks.toolClasses` / `toolHarvestLevels` / `toolEffectiveness` exist and `MinecraftForge.setToolClass` /
  `setBlockHarvestLevel` fill them (not yet mapped to modern tool tags); Forge version checks pass as 1.0.7.
- **LWJGL 2 `BufferUtils`** of b1.7.3 jars is the shim's (plain direct NIO buffers), so client builds also load on a dedicated server.
- **Vanilla resources.** `/font/default.png` (ComputerCraft's terminal font) comes from the modern ASCII font sheet, which kept the layout.

## Reference data

`tools/fetch_devtools.sh` downloads the b1.7.3 client and server, Risugami's ModLoader, ModLoaderMP (client and server) and Forge 1.0.7 into
`build/devtools/b1.7.3/`. Then:

```
./gradlew :loader:genBetaMappings
python tools/extract_beta_signatures.py
python tools/extract_legacy_fields.py b1.7.3
python tools/extract_legacy_methods.py b1.7.3
```

## Offline results

`./gradlew checkLegacyMod -PlegacyJar=...` over 25 b1.7.3 jars (the mods Apron tests with plus NetherCraft): 23 detected as b1.7.3
(client builds on the client names, the three server builds on the server names), all with 96 to 100% of their game references resolved
and 0 failing methods in bytecode verification. NetherCraft, IndustrialCraft 2, RedPower (all five parts), Minefactory Renewed,
More Creeps and Weirdos, ComputerCraft, Portal Gun, Twilight Forest (b1.7.3 version), Moon Mod, Butterflies and Coral Reef link with
0 unresolved references. Not detected outside `mods/b1.7.3/`: the two OverrideAPI library jars (no `mod_*` class).

## Server results

Dedicated server with 15 b1.7.3 client builds (`run-subtest/mods/b1.7.3/`): 11 construct (IndustrialCraft 2, NetherCraft, Portal Gun,
Minefactory Renewed, all five RedPower modules, Butterflies, Twilight Forest b1.7.3); 82 blocks and 390 items registered, 82 of 82 blocks
placed by the self test, 1786 recipes translated (5 name items nothing provides). Failing, as expected for now: ComputerCraft (needs the
legacy client instance: `ModLoader.getMinecraftInstance().renderEngine`), Coral Reef and More Creeps (need GuiAPI), Moon Mod (needs ShockAhPI).

With Aether 1.02 (Modrinth `aether` b1.7.3-1.02: the zip's `Jar/` folder as a mod zip; it is a ModLoader mod, only its title screen is a
base class edit) the set has 17 mods: Aether and its `mod_Trivia` construct with ShockAhPI and PlayerAPI linked to synthesized classes
(`SAPI.interceptAdd`, `reachAdd`, `ToolBase`, `PlayerAPI.RegisterPlayerBase` do nothing yet) and register 35 blocks and 128 items; its
freezer, enchanter, incubator and treasure chests open their GUIs. Not yet: the Aether dimension (`DimensionBase`), its portal, mobs and
the player changes. Moon Mod now gets past ShockAhPI and stops at a hard-coded block id Portal Gun holds; More Creeps stops at GuiAPI's
settings classes (synthesized without their hierarchy: `VerifyError`).

Client (dev client, 11 jars): NetherCraft, Portal Gun, Minefactory Renewed, RedPower, Butterflies load with textured blocks and items in
their creative tabs; 51 blocks, 159 items, 219 textures generated, 1 sprite reference without a texture. IndustrialCraft 2's client
build now gets past OpenAL and stops at `ModLoader.getPrivateValue` with an obfuscated field name (plan item 6).

## Plan

0. **GUIs, rest**: ModLoaderMP's client side (`BaseModMp.HandleGUI` builds the client GUI for a server's `OpenGUI(player, id, inventory,
   container)`; today the client draws a plain panel for those), entity GUIs (item right-click callbacks are bridged), tile entity
   special renderers (`RegisterTileEntity(class, id, renderer)`, kept).
1. **Rendering, rest**: remaining standard block render types (fences, rails, ...), broader custom renderer helpers/GL calls,
   colour multipliers; item subtypes of plain items (block items have them: models and names per metadata).
2. **Behaviour**: block callbacks (`onUse`, `onPlaced`, `neighborUpdate`, `onTick`, redstone) through a `World` view, tile entities
   (`RegisterTileEntity`) on one modern block entity type, entities (`RegisterEntityID`), world generation (`GenerateSurface` /
   `GenerateNether` as placed features), fuel (`AddFuel`).
3. **Logic on the server**: plain ModLoader mods expected one world. Their logic (`OnTickInGame`, `mc.theWorld`, `mc.thePlayer`) runs
   against server-side views, rendering and GUIs stay on the client, `ModLoader.OpenGUI` becomes a synced menu; ModLoaderMP's
   `Packet230ModLoader` becomes a layer payload. Single player first, dedicated servers partial.
4. **Other APIs**: ShockAhPI, PlayerAPI, GuiAPI, ModOptionsAPI, AudioMod, ItemSpriteAPI (as bridges, the way Apron provides them).
5. LWJGL 2 / GL11 for b1.7.3 mods: the GUI subset is the shim's; world rendering calls (tile entity renderers) still go nowhere.
6. Reflection outside ModLoader (`Class.getDeclaredField("a")` with an obfuscated name) and by method name.
7. The legacy client instance (`ModLoader.getMinecraftInstance()`: `renderEngine`, `gameSettings`, `theWorld`, `thePlayer`) as views.
