# Legacy Beta 1.7.3 (ModLoader and Forge 1.0)

Status and design of Minecraft Beta 1.7.3 support. Back to [Home](Home.md). Started 2026-11-06.

**Status: alpha, server-tested (blocks, items, recipes, textures, block behaviour, GUIs; Mo' Creatures and MoreCreeps also have isolated offscreen client lineups).** Beta 1.7.3 ModLoader / ModLoaderMP / Forge 1.0.x jars are
detected, deobfuscated and linked; the mods are constructed as ModLoader did, their blocks, items and recipes become modern content with
their own textures, one creative tab per mod, and the blocks' own code runs (placing, breaking, neighbour updates, ticks, right and left
click, redstone) with their tile entities (saved, loaded, ticked) and their GUIs (machines, furnaces, crafting tables as modern menus).
Projectiles, external OGG/WAV audio and supported living mobs have partial bridges. Supported animal subclasses use their
original renderer/model callbacks and ModLoader spawn registrations in normal gameplay. Broader mob families, custom
render APIs, world generation and the mods' tick hooks remain incomplete.

## Putting the mods in

Put b1.7.3 mods in `mods/b1.7.3/`. Jars and zips are both read there (most ModLoader mods were distributed as zips). A mod dropped directly into `mods/` that is recognised as a b1.7.3 mod (see below) is moved into `mods/b1.7.3/` on launch (`legacy.autoSortMods=false` turns that off); libraries without a `mod_*` class are not detected outside the folder, so they stay where you put them. A jar directly in
`mods/` is also recognised when it has a `mod_*` class or references the ModLoader/Forge API and at least 60% of its references to the
game resolve in the b1.7.3 names (ModLoader mods for 1.0 to 1.2.5 look the same but name other members). Library jars without a
`mod_*` class (OverrideAPI) are only recognised in `mods/b1.7.3/`. Many mods came as a client build and a ModLoaderMP server build
with the same `mod_X` class; when both are present, the build for the running side loads and the other is skipped (logged).

## How a jar is translated

Beta mods use a stable `b173_` prefix for their FML identity and registered content namespace: for example,
`b173_aether:holystone` can coexist with a Forge mod's `aether:holystone`. The prefix applies even when only the
Beta copy is installed. Existing Java relocation, configuration names and resource paths remain unchanged.
This changes previous Beta registry IDs; save migration is not included.
Actual Beta Aether 1.02 and Aether II 1.7.10-1.6 have been checked together: both original mod instances initialize,
and separate blocks/items retain their original ownership, textures and saved states across a real client save/reopen.

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

## AudioMod external sounds

The shared AudioMod path now supports OGG and PCM WAV assets through the host sound engine. Install a mod's external
audio beneath the **active instance's game directory** (the directory containing `mods`), preserving its installer layout:

```
resources/mod/sound/       # positional effects and GUI sounds
resources/mod/streaming/   # records / streaming source
resources/mod/music/      # background music above ground
resources/mod/cavemusic/  # background music underground
```

The root comes from `FMLPaths.GAMEDIR`, with no `%APPDATA%` or home-directory fallback. This works with custom Prism and
portable instance locations. `Minecraft.getApplicationDirectory("minecraft/resources/...")` resolves against that same
instance. Explicit `SoundManager.loadSound/loadStreaming/loadMusic` and `SoundEntry.loadStatic` file arguments may point
outside it. Audio files are client-side resources; server packets carry sound names and parameters, never filesystem paths.

`SoundEntry` preserves Beta's names: slashes become dots, numbered effect/music variants share an event, and streaming
names retain their trailing digits. Discovery ignores dot files/directories, and resource reloads rescan the folders.
`Minecraft.loadResource`, positional/entity `World.playSound`, `World.playStreaming`, and both `SoundManager.playSound`
overloads route to native sound instances. A null streaming name stops the previous streaming source. Sound categories,
listener, attenuation and volume sliders belong to the host engine. Standard OGG effects use buffered effect channels;
WAV uses a PCM decoder on streaming channels. Music/cave music use AudioMod's delays and sky check without overlapping
the streaming source. No content-mod identity is used by the implementation.

Validation uses `:test --tests '*AudioB173Test'`: sound naming, portable roots, explicit external files, rescans, generated
assets, request serialization, native instance parameters and decoding. User-owned archive fixtures are tested separately:
PortalGun v5 (239 OGG files), Mo' Creatures 2.12.2 (168 OGG files), and Aether 1.01 (15 valid OGG/WAV files). Aether also
ships one MP3-framed file named `.wav`, which is rejected, and two files outside AudioMod's recognized folders. These
archive defects are recorded in tests; production discovery does not special-case them. Fixtures are not redistributed.
`AudioSelfTestB173` additionally exercises PortalGun's original Aerial Faith Plate launch callback when that mod and its
external resources are installed in an isolated server test. This passed with 239 external files on 2026-11-07. The broader
PortalGun run remains `failed-or-needs-review`: placing its Portal Spawner block accesses a null legacy client player.
The audio result is not a whole-mod compatibility claim.

This is partial AudioMod compatibility: tracker codecs (`.xm`, `.s3m`, `.mod`) and encrypted `.mus` are not implemented;
direct Paulscode/OpenAL APIs and synthesized `GameOptions`/sound-system initialization internals remain incomplete.
Headless decoding/callback tests do not establish audible playback, music timing, or complete mod compatibility in a client.

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
Standard fence render type 11 uses the shared Beta renderer; custom fences continue to call the original
ModLoader renderer. Fence tests cover isolated posts and all four neighbour directions using NetherCraft
and IndustrialCraft 2. The shared `DoubleInventory` now delegates to both live inventories, so NetherCraft
double chests expose all 54 slots. Tests open both halves on both axes, transfer items in both directions,
and check obstruction above either half.
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
path; custom block-item callbacks, eating animations and inventory ticks are separate work.

An overridden `Item.useOnEntity(ItemStack, LivingEntity)` runs through native main-hand item/entity interaction.
The server executes the original callback with canonical entity views and the initiating player's Minecraft context;
target changes and inventory edits commit before the context closes. The native caller's stack keeps its components,
and creative-mode stack copies retain native count behavior. Items without an override and offhand calls pass through.
The actual Mo' Creatures horse-saddle fixture covers adult and rejected targets, repeated use, creative counts,
custom components, offhand calls and ordinary items without this callback.

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

The vanilla `BucketItem` constructor and use API, plus Forge's `IBucketHandler` registration/first-result dispatch, are
implemented. Modern empty buckets reach original handlers through native `BucketPickup`; no mod-specific fluid IDs are used.
Both Forge spellings (`registerCustomBucketHandler` and the historical `registerCustomBucketHander`) are accepted.
An isolated BuildCraft Core + Energy 2.1.1 run checks original oil pickup, flowing-metadata refusal, inherited filled-bucket
placement, crafting remainder, stacked empty buckets, creative pickup/placement and denied placement. All pass.

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

### Vanilla item metadata and armor constructor APIs

`DyeItem` exposes Beta's original 16 subtype names/colors and icon/name selection. `WoolBlock` preserves the inverse
dye/wool metadata mapping and original wool textures. `ArmorItem` retains original slot/tier/texture/protection fields,
durability calculation and stack size. ModLoader reflection can access protection by original field index, obfuscated name,
or named alias. These APIs fixed PlasticCraft 2.1.3 construction without selecting that mod in production code.

Five focused tests cover dye/wool metadata and armor constructor/reflection behavior. The actual isolated mod constructs
15 blocks and 48 items, translates 134 recipes, and passes its exercised block callbacks. Armor equipment/rendering and
original GUI rendering are not established by these checks; the GUI test uses the current generic layouts. Its latex tap
requires a neighboring log, so a stone-only placement sweep does not establish tap placement compatibility.

The subsequent [PlasticCraft latex validation](../data/b173-plasticcraft-latex-validation-2026-11-09.json) reproduces the
Latex Extractor's bucket failure with actual PlasticCraft 2.1.3. The original bucket traces for the non-colliding tap, but
the old bridge traced collision boxes and selected the log behind it; the tap's original selection callback also called a
stubbed base method. Shared Beta world ray traces now invoke original block selection callbacks, and the base block API
clips its state-dependent selection bounds. The explicit collision-only filter and source-fluid pickup rules are retained;
native vanilla interactions are unchanged.

Native placement and survival right-clicks fill an actual plastic bucket on all four horizontal log faces. Plastic water
pickup/placement and a native vanilla bucket control pass, as do filled-bucket save/reopen and another fill from the same
existing extractor afterward. The original mod archive is byte-identical to Prism's 2.1.3 jar. The default build passes
568 portable tests without private mod archives; other PlasticCraft systems are outside this bounded interaction check.

### Crafting outputs larger than an inventory stack

Beta recipes can return several non-stackable items. The shared shaped/shapeless recipe serializers retain a separate
output count, preserving the item's original stack limit while avoiding the modern data codec's rejection. A crafting-result
hook applies only to these recipes: shift-click fills available slots, and hotbar/offhand swaps split oversized results.
Native result-slot logic still consumes ingredients and handles crafting remainders; excess items drop only when space runs out.

More Creeps & Weirdos 2.12 supplies the actual regression recipe: four frisbees, each with a stack limit of one. Its fresh
isolated run passes 48 transfer scenarios across the original recipe, a remainder fixture and an ordinary-recipe control.
Coverage includes both table and 2×2 menus, repeated shift-click, pickup, hotbar/offhand swaps, occupied destinations,
partial/full inventories, output conservation, ingredient consumption and bucket remainders. Two focused tests also cover
recipe codecs, network round-trips, matching and output copies. No frisbee-specific behavior is selected in production.

## GuiAPI settings and TWL models

The shared Beta GuiAPI bridge implements typed Boolean/Integer/Float/Multi settings, context values, original step rounding,
properties save/reload under the instance's `mods/<backend>/guiconfig.properties`, reflective `ModAction` callbacks and the
TWL widget/button-model hierarchy used by the tested mod. The loader relocates Beta TWL names into this bridge.

`GuiModScreen.show` now lays out the supported TWL windows, columns, buttons and sliders using native GUI drawing and input.
Buttons and slider thumbs use the shared Minecraft button sheet, including hover, focus and disabled states.
The Options screen exposes the registered mod-settings screens. Original settings callbacks retain ownership of
values and persistence. Unimplemented widget types remain outside this supported subset.

Plain entity screens execute on the initiating player's server thread. A shared packet connection carries drawing commands
and keyboard/pointer input; original Done callbacks mutate the server-owned creature. Nested screen requests wait until the
initiating inventory callback commits, so opening a naming screen cannot restore a consumed item. The actual Mo' Creatures
settings navigation, five naming-screen variants, and frame/input packet round-trip pass headless tests. Dynamic bitmap
uploads through server-owned screens are not implemented.

## First living-mob host

`RegisterEntityID` registrations for supported `CowEntity` subclasses can now become native entity types. This is a
family-based API bridge, not a Twilight Forest special case. Unsupported mob families, custom simulation overrides, and
callbacks depending on unfinished superclass bodies are rejected explicitly. Ordinary modern cows are unchanged.

The native host provides movement/navigation, collision, health/damage, tracking and save/load. It preserves the original
legacy object, dimensions, texture path, supported interaction and sound callbacks, and custom NBT. A rejected/canceled
world insertion clears the binding so the same original entity can be retried. Returning false from an original interaction
does not fall through to native cow milking. A native loot table supplies Beta's leather-only 0–2 default cow drops.

The client currently uses **native cow geometry and AI as approximations**, with the original mod's texture. The spawned
mob still has its own registered type and original identity; no duplicate vanilla cow is spawned. Original custom models,
arbitrary custom AI, direct legacy-field mutation outside supported callbacks, and natural spawn registration are incomplete.

The actual Twilight Forest 1p4 deer test passes cancellation/retry, identity, interaction, native/legacy damage, dimensions,
original texture lookup, registry-based save/reload, and loaded loot-table sampling. This is headless proof, not visual QA.
Its boar and bighorn families are not yet hosted. Mo' Creatures and More Creeps are separate user-selected tests for the
custom AI/model work; success with the deer does not establish support for either mod's custom creatures.

The shared `AnimalEntity`/living-entity superclass lifecycle has a neutral native `PathfinderMob` host. Original `tick`
enters the native base tick, which dispatches original `tickMovement`; its base movement phase
dispatches original `tickLiving` before native travel. A narrow host-only AI hook preserves the effect of skipping the
superclass AI call. Navigation and controls run only through that base call; no species goals are installed.

Shared APIs include mounting/dismounting, live world entity/player queries with original identities, visibility, collision,
fluid/spawn-volume checks, original sounds, damage/knockback/death callbacks, item drops and NBT. Explicit native-base
damage operations prevent a mod's `super.damage` from redispatching its override recursively. Queried host views commit
pending changes before a callback refresh and avoid replaying unchanged native state. Admission reads method metadata
without resolving unrelated API signatures and rejects unsupported synthesized callbacks.

Animal admission is enabled in normal gameplay when both simulation and original renderer/model dependencies are supported.
Unsupported callbacks, synthesized rendering bodies and unresolved render-state fields cause an explicit rejection.
General wandering uses modern navigation with the original path-preference callback; exact Beta path timing is unproven.

Fresh, separate actual-mod runs pass the original Mo' Creatures Bunny and More Creeps Hippo lifecycle fixtures. Bunny
checks growth, a 0.45 hop before travel, one tick/movement/AI phase, skipped-super AI with retained physics, native and
original mounting/dismounting, thrown velocity, registry NBT, damage, cross-host pending writes and original offspring.
Hippo checks visible/occluded target selection, retaliation, superclass damage without reentry, original lunging attack,
Hungry NBT, water eye-offset behavior and original deterministic porkchop/sugar-cane drops through native item entities.
These checks establish the exercised server behavior; they do not enable the mod's entire mob roster or prove visuals.

The shared `ModelPart`/`EntityModel` foundation now executes original model constructors and animation/render callbacks.
Beta cuboid UVs, mirror winding, the 0.1-pixel texture inset, single-cuboid replacement and rotation orders are preserved;
the client adapter submits those faces through the existing renderer context. Tests compare all vertex positions and UVs
against the unmodified Beta classes, including mirrored, expanded and zero-depth cuboids (3 tests, none skipped).
Separate actual-mod runs pass original Bunny (11 parts) and Hippo (16 parts) construction, textured geometry and animated
draw capture. Broader renderer APIs and mob families remain incomplete.

### Original animal renderers and native tracking

`BaseMod.AddRenderer` now runs against the shared renderer registry. Native client renderers delegate to the registered
original instance through the Beta living-renderer callback order, including custom render entry points, head-bob callbacks,
scaling, rotation, model animation and supported decoration passes. The common matrix context keeps model transforms out
of GUI drawing. Original PNGs become generated-pack resources, including conditional texture choices.

Native spawn/metadata packets carry the entity type, pose, dimensions, texture, eye offset and original scalar/String fields.
Received field deltas preserve local cosmetic choices made by original render callbacks when the server value has not changed.
Such client-only choices remain client-local; arbitrary object/collection render state and downloaded skins are not implemented.
Client ticks and death handling do not execute the original server simulation, and rendering never writes its view back into
the host. Read-only view scopes prevent render-time queries from retaining simulation callback state.

ModLoader `AddSpawn`/`RemoveSpawn` record class/name, category, weight and biome selections. Accepted hosts produce native
`neoforge:add_spawns` biome modifiers; rejected mobs do not enter biome spawn lists. Default Beta overworld selections map
to the native overworld biome tag; explicit vanilla selections use shared biome-family tags. Custom unbridged biomes are
reported and omitted. Native natural-spawn checks invoke original `canSpawn`, and cluster limits use `getLimitPerChunk`.
Vanilla-class requests replace or remove existing native entries for the requested category and biome selection, rather
than adding duplicate weights. Final requests retain removals even when no added entry preceded them; later adds replace
those removals. Replacements run after native add/remove modifier phases. Unknown classes never fall back to a default entity.
Water-creature hosts use native water-animal obstruction checks, so valid liquid habitats survive the final placement check.

The initial isolated Mo' Creatures and More Creeps runs verified the original Bunny renderer (66 faces) and Hippo renderer (96),
including transforms, animation, exact PNG pack bytes, spawn/metadata packet replay, skin/eye-offset persistence and loaded
native biome weights/spawn rules. Two independent CLIENT-side CPU probes compare every native emitted position and UV
with the same original render invocation. No live window/GPU visual check was run.

### Additional creature families

The common host now supports original passive, pig, chicken, hostile, water, flying and plain-living families when their
callback dependencies pass admission. Original custom travel and movement remain virtual; explicit native superclass
operations prevent recursion and preserve skipped-super behavior. Water/flying navigation, rider input, bucking, fire
immunity, fall and placement callbacks, dropped-item age/pickup, typed arrow owners and mutable entity relations use shared
APIs. No production branch selects behavior by a tested mod's identity.

When original creatures ride a native player, passenger placement evaluates the original carried-offset getter inside the
shared callback transaction. This preserves both placement and getter side effects, including furniture pickup state.
Successful native attachment and detachment also send the player carrier its own passenger-list packet. Native tracking
normally sends that player's updates only to observers, which previously left a carried creature on the carrier's client
at its old ground position. Unchanged, rejected and canceled relationships retain native behavior.

Isolated actual-mod client checks exercise Mo' Creatures horse feeding, taming, saddling, chest transfers, steering,
jumping and dismounting; Bunny, Bird and Kitty carrying; and More Creeps Kid pickup/head placement, Rocket Giraffe riding,
Guinea Pig taming, stacking and commands. Real save/quit/reopen checks retain pet UUIDs, original saved fields and horse
inventory, eject carried pets coherently, and restore a ridden horse through native `RootVehicle` persistence.
The [client gameplay validation snapshot](../data/b173-client-gameplay-validation-2026-11-08.json) records the exact
fixture hashes, namespace counts, client assertions and separate final server gates.

Nested callbacks share an entity-view transaction. Queried objects keep stable identities, pending writes are committed
before a native refresh, and a nested damage callback cannot discard subsequent original fire or velocity changes.
Original post-spawn scalar changes are published before the next simulation tick. World entity-list snapshots preserve
indexed traversal when original code removes entries during the loop.

Vanilla model parents include biped, quadruped, chicken, pig, cow and sheep models, compared against unmodified Beta models.
Original render callbacks can submit held items, nameplates, health-bar quads, blending/depth state and texture matrices
through native rendering. ModLoader categories select native water/ground spawn placement, alongside original spawn checks.
Achievement registration and awards use native advancements; original progress queries read the same native state.
Top-level Beta achievements and those attached to vanilla parents are grouped under a synthetic category for their mod.
The category becomes visible automatically, with no toast or chat announcement; original achievements still require
their original award callbacks. Existing achievement IDs and custom parent chains remain intact. This lets Beta tabs
appear alongside Forge pages before a player earns the first Beta achievement. The
[advancement visibility validation](../data/b173-advancement-visibility-validation-2026-11-08.json) records the actual
Beta Aether/Aether II screen, unchanged Forge progress, one explicit legacy award and save/reopen persistence.

Fresh separate actual-mod runs pass all **28 Mo' Creatures registrations**, including original geometry, tracking packet
round-trips, movement callbacks, interaction, damage, persistence and bounded death/drop checks. Twenty-one focused gameplay
cases cover eggs, feeding/taming, rider controls and bucking, packhorse storage, naming, achievements, original destruction,
fire attacks, reproduction and item-age/projectile-owner behavior. All **53 More Creeps living registrations** pass their
core roster checks. These tests exercise original mod classes, rather than replacement behavior.

The cases also verify retained-target fire mutations, native boar saddling and inherited duck-wing tracking.
[The validation snapshot](../data/b173-living-validation-2026-11-07.json) records payload hashes, roster names and the exercised cases.

Additional original-callback checks cover Kitty litter use, courtship and bed selection, kitten protection and maturation,
aquatic prey/settings policies, contact damage and juvenile gates, Fishy offspring, and aquatic drops/persistence. A controlled
habitat also exercises Minecraft's native weighted spawn selection, original spawn checks, factory creation and world insertion.
This preserves original quirks, including a pregnant Kitty rejecting the nearest occupied bed instead of choosing a farther one.

Hosted legacy spawn requests now receive opportunities on every ordinary native world tick, independently of the native 400-tick
passive cadence. The scheduler reads current `SpawnGroup.capacity` values, including untouched Beta defaults of 70/15/5 and
successful mapped writes by field name or index. Mo' Creatures applies its configured 70/30/25 settings at launch rather than
on each slider change. Native category limits and per-player budgets remain unchanged for native spawning.

Each Beta group counts all loaded entities by its original class or marker interface, including persistent entities. A host's
original ancestry takes precedence over its native category; native entities use the corresponding Enemy, Animal and WaterAnimal
families. Thus a More Creeps monster registered in CREATURE still counts as a Monster, preserving the original mod's policy.
Player footprints are deduplicated unions of inclusive 17-by-17 chunk squares. A group starts when its count is at most
`capacity * footprintSize / 256`, with integer division. This gate runs once before the group's chunk loop; cluster overshoot
is retained, and the next group sees a fresh census. For one player, default thresholds are 79/16/5; Mo' Creatures' are 79/33/28.

The bridge captures chunks already admitted by native player proximity, world-border, gamerule and tick-rate checks. It keeps
native weighted entries, biome spawn costs, spawn events and insertion, while hosted originals use the Beta candidate algorithm.
One species is selected before each chunk's X/Y/Z origin draws, with the original fixed Y range of 0 through 127. Three walks
of four candidates consume the original zero-height RNG draws, preserve material/solid-floor checks and the 24-block player
and world-spawn exclusions, and call the original spawn predicate and cluster-size override. Checked-in full-cube flags for all
96 original vanilla blocks preserve unusual rules such as solid soul sand and non-solid pistons, alongside half/double slabs.
Original mod blocks retain their own full-cube API callback; modern blocks without an old counterpart use native geometry.
Modern mob initialization is skipped for these original hosts, while native finalization events and insertion accounting remain.
The native PotentialSpawns event excludes hosted entries from ordinary selection, scoped to category and biome policy.
No scheduler dispatch selects a content mod by name.

Beta did not have the modern chunk-generation animal-population pass. Hosted Beta creature entries are therefore excluded
from that additional pass; native and modern mod entries retain their objects, order, weights and pack sizes. Terrain features
and the original `GenerateSurface` callback path remain unchanged. Ordinary vanilla and modern mod entities retain native
placement, population, caps and despawning. Original mods' explicit vanilla AddSpawn/RemoveSpawn requests still become biome
modifiers and remain on the native lifecycle lane. Modern terrain-generated fauna can still fill the correct Beta class budget.

Legacy living hosts now run the original despawn phase at their original superclass AI boundary. The phase calls the mod's
dynamic `canDespawn` override, removes eligible entities strictly beyond 128 blocks from the nearest player, and uses the
original age-over-600, 1-in-800 lottery with the 32-block reset/removal boundary. Skipped superclass AI also skips that phase.
Taming overrides, native despawn-event vetoes, explicit persistence, names, leashes and passengers remain protected. The older
cow-family host receives the same original eligibility and distance rules without modifying ordinary native cows.
[The cap and scheduling snapshot](../data/b173-cap-scheduling-validation-2026-11-08.json) records actual-mod and control proofs.
[The placement and ecology snapshot](../data/b173-placement-ecology-validation-2026-11-08.json) records separate normal-world
and classic-height grass-flat runs of both actual mods, native population/persistence comparisons, all 96 original full-cube
API results, and single/double slab metadata round trips. These fixtures are opt-in; normal builds need no private mod archive.
The original full-cube results and source-jar fingerprint are checked in with the geometry table; no external game jar or
metadata extraction tool is required by the build.

Additional actual-mod fixtures cover Kitty fear exclusions and tree search/timeouts, BigCat rope following and its original
null-path perimeter teleport, Nightmare rider fire clearing and charge exhaustion, and Bird wheat-seed taming. Rope distance
checks use original entity coordinates, including eye offsets. The Bird preserves its original whole-drop consumption quirk.
Native item entities without a Beta item representation are excluded from legacy World spatial queries and live entity lists,
preventing null-stack access by original callbacks while preserving the native item entity and its stack. Ordinary represented
Beta drops remain visible. This does not add post-Beta items to the old registry or change native mob/item behavior.

Hunchback cake recruitment and bone-triggered five-Skeleton army formation are exercised through original More Creeps callbacks,
including inventory/health boundaries, native advancement and canonical identities. Its original false interaction return is kept.
The conditional Hotdog renderer now receives original vanilla block texture indices, recorded for all 96 vanilla block views,
and living renderers share the existing terrain/item sheet routing used by nonliving renderers. Actual cold/fire callbacks verify
the original fire cell, resolved native texture, quad counts and sprite-local UVs. This is headless callback/resource proof;
live appearance and animated fire-frame sampling still need client validation.
[The pet, following and renderer snapshot](../data/b173-pet-follow-render-validation-2026-11-08.json) records this follow-up.

Hunchback autonomous following, range/visibility gates, cake-timer lottery and expiration, tame-versus-wild damage and retaliation
are now checked through original callbacks and ordinary host ticks. Army checks cover original allegiance, target acquisition,
ranged cooldowns, retained paths while firing, off-axis aiming, lifetime/texture boundaries and expiration. The original army is
autonomous: there is no master or formation controller to reproduce. Its allied-attack fall-through and omission of lifetime
from NBT remain original mod quirks.

Shared creature AI acquires a target before attacking on the next phase, re-reads targets after original attack/reset callbacks,
and uses the post-attack field to suppress route replacement while preserving existing navigation and aim-relative movement.
Exact vanilla Beta Arrow API spawns use a dedicated registered native arrow host with original fixed four damage, shooter
attribution, gravity, water drag and rejected-hit bounce. Registry NBT and native spawn-packet tests retain that host and owner;
ordinary native arrows retain their own factory and damage rules.

Original renderer callbacks can contain block gameplay. A cached method-level call graph detects renderer-to-World block
mutation paths and executes those original callbacks once per hosted server tick with discarded graphics. Pure visual
renderers are excluded; client renderer block writes are suppressed. Shared renderer/model instances are synchronized between
simulation and drawing. This fixes Ogre destruction that previously changed only the client's blocks: controlled actual-mod
tests now remove the authoritative server block, mark the native chunk dirty, and independently decode AIR from its serialized
section palette. The pass operates without a viewer rather than depending on render-frame timing. Reflective calls and arbitrary
helper-interface dispatch are not inferred by the detector.
[The army and Ogre validation snapshot](../data/b173-army-ogre-validation-2026-11-08.json) records these proofs and the reproduced
pre-fix failures. Live client rendering/interaction remains to be verified.

Vanilla door and stair metadata now translates through the Beta World view in both directions. Both Beta door halves carry
facing and open bits; later upper-half hinge metadata is not used. The id-only placement API recognizes an upper door placed
above its lower partner, so the original place-id-then-metadata sequence survives native neighbor updates. Existing hinge and
power properties survive metadata changes. The actual Lawyer jail regression exercises live neighbor updates, wooden and iron
door pairs, native opening/closing, and every step of the original eighteen-step spiral. Native placement rules are unchanged.

Vanilla torch placement through the Beta World API now selects west, east, north, south, then floor support in the original
order. The classic fence can support a floor torch. Metadata 1–4 maps to native wall-torch facings and 5 to a standing torch;
metadata writes can move between those native block variants while retaining the same Beta id. Redstone torches use the same
orientation encoding and retain their lit/unlit ids 76/75. Initial redstone placement normalizes only metadata zero, matching
the original subclass callback. Ordinary native placement and foreign torch blocks keep their own behavior.
Native torch blocks own lighting, support-loss removal and loot. Original Lawyer jail checks exercise all three wall torches,
wall/floor transitions, fence support, unchanged-id writes and drops, alongside the existing door/stair regression.
The actual jail's three torches also pass an isolated offscreen midnight check: native block light 14 at each torch and 13
in adjacent air, checked twice after lighting settles. Three reviewed wall-torch closeups supplement 481 passing tests.
[The wall-torch validation snapshot](../data/b173-wall-torch-validation-2026-11-08.json) records the reproduced standing-torch
failure, source hashes and accepted captures. Existing malformed torches are not migrated automatically; no mod versions changed.

Tracked legacy texture paths are canonicalized when restored from packets or saves, preserving original renderers that compare
paths with Java string literals. The actual Horse renderer now retains its saddle/chest passes after a native tracking packet.
Conditional renderer fixtures also cover Werewolf forms, Ogre decorations, aquatic skins/growth, KittyBed contents and LitterBox
UV changes, More Creeps variants and tracked poses, GooGoat blend passes, and all 233 original More Creeps PNG resources.

Immediate block/item sprite quads use the native stitched atlas when their transformed UVs remain within the sprite, allowing
animated sprites such as Hotdog fire to sample the native animation frame. The original texture matrix is applied once and
restored after drawing. Custom textures, missing sprites and wrapping UVs retain their original standalone texture binding.
Portable tests exercise the native quad backend's atlas coordinates, matrix restoration and wrapping fallback.

The shared living renderer now executes Beta's original hurt/death and optional ARGB color passes after ordinary models,
decoration passes and `renderMore`. Ordinary decoration passes use `bindTexture`; overlays use `bindDecorationTexture`.
Hurt or death produces one brightness-scaled red pass with alpha 0.4, including when both timers are active. Custom original
render methods retain ownership of their passes. The Beta native adapter supplies no additional modern hurt overlay, avoiding
double tint. Its opt-in render frame preserves EQUAL depth and alpha blending, with body/overlay draw boundaries; existing
frame overloads and native entity renderers retain their behavior.

Actual offscreen client attacks exercise Bunny, Hippo and the custom Ogre/Werewolf renderers, including normal colors after
hurt expires and native Bunny/Hippo death poses. Actual cooked-pork interactions tame a Hotdog; a native redstone interaction
adds and tracks 250 firepower. GPU readback samples eight distinct uploaded fire frames during normal gameplay from the
native 32-frame sprite. A separate fixed-pose test advances the standard native atlas while world/entity ticks are frozen,
confirming changing visible flame pixels and transparent holes. Native tick freeze otherwise stops texture animation too.
Both unchanged original mod payloads also pass fresh isolated server regressions, alongside 497 portable tests.
The [render effects snapshot](../data/b173-render-effects-validation-2026-11-08.json) records source hashes, exact coverage,
pixel counts and reviewed captures. These remain bounded tests without Sodium/Iris or shader-pack coverage.

Isolated offscreen clients enumerate all 28 Mo' Creatures and 53 More Creeps registered entity types in eight lineup pictures.
Controlled scenes cover Horse equipment, Werewolf forms, Ogre and furniture, the actual Lawyer's wooden/iron doors and spiral,
and Hotdog fire sampled from the native 32-frame sprite. Both Normal-difficulty integrated clients repeat the original-mod
regressions successfully. These are bounded visual checks, with no Sodium/Iris coverage; they do not establish complete mod
compatibility. [The jail and client rendering snapshot](../data/b173-jail-client-render-validation-2026-11-08.json) records
the source hashes, 477 passing tests, reproduced failures and reviewed captures. Existing malformed structures are not migrated.

Ordinary native Squid and GlowSquid now expose canonical Beta `SquidEntity` / `WaterCreatureEntity` views to original callbacks
and typed World queries. The eight animation fields are read snapshots of the native instance. Native entity types, goals,
spawning, renderer and loot retain their existing lifecycle. Fresh translation caches are required so original class literals
resolve the handwritten Squid shim rather than the older synthesized placeholder.

Actual Mo' Creatures checks use its translated Squid class literal and exercise Shark/Piranha selection and damage plus Dolphin
exclusion. Those prey policies already passed before the identity fix; the reproduced failure was the missing typed ancestry.
Actual More Creeps Bullet callbacks hit wet native Squid and GlowSquid, alongside canonical-view, animation and native-goal/loot
checks. [The native Squid validation snapshot](../data/b173-native-squid-validation-2026-11-08.json) records isolated actual-mod
runs and the reproduced identity failures.

The opt-in `naturalSpawnTickSelfTest` observer uses a real tracked native player and ordinary server ticks, recording candidate
checks, finalization and confirmed world insertion separately. Isolated observations confirm original aquatic, hostile and
passive Mo' Creatures spawning, and More Creeps land spawning. The land observations discard counted nonpersistent creatures
once in disposable worlds to expose an available native budget; they do not establish spawn abundance in an untouched world.
A short More Creeps forest window admitted none, while a longer window admitted an original Kid and Hunchback; original sky/light checks
and per-candidate random gates remain intact. [The spawning and carrying snapshot](../data/b173-spawning-carry-validation-2026-11-08.json)
records these observations, the failed short window, three aquatic insertion regressions, four carried-placement cases and
positive/zero vanilla squid frequency checks across 53 native overworld biomes.

Terrain-population spawn checks read through their supplied `WorldGenRegion`, including block, light, height and collision
queries. Reading an unfinished chunk through the full server level made generation wait on itself; an actual SneakySal spawn
check exposed this during a fresh-world load. The scoped accessor preserves the original World identity, matches its backing
dimension and restores nested contexts. A diagnostic copy of the installed pack completed the failing seed and eight fresh
distant chunks after this shared fix. [The world-generation snapshot](../data/b173-worldgen-validation-2026-11-07.json) records that proof.

Spawn collision checks also validate the complete native scan footprint, including its epsilon-expanded one-block shell.
When a required chunk lies outside the generation region, the candidate is rejected through a dedicated internal signal;
unrelated original exceptions still propagate. Missing required block, tile, light, sky or height data cannot become an
empty-space success. Actual FloobShip checks cover clear and blocked interiors, body/shell boundary rejection, ordinary-world
fallback and scope cleanup. A fresh pack world and 56 new flying-route chunks pass the boundary regression.
[The generation-edge snapshot](../data/b173-generation-edge-validation-2026-11-07.json) records this follow-up.

Four further Mo' Creatures cases exercise Werewolf transformations and form persistence, rare-horse subtype stats and landing
damage, winged-horse animation/descent, and native cow/wolf prey exclusions. Native cows and wolves retain their Beta API class
identities in original callbacks, so the mod's own settings and retaliation rules apply to vanilla creatures.

Eleven further More Creeps cases cover Camel and Hotdog taming, GuineaPig commands, speed boosts, stacked carrying and level-five
progression, RobotTed/Todd shrink and removal thresholds, player/Floob ray attribution, and lethal Rocket splash achievements.
Shared entity transactions preserve cross-entity fall-distance writes; native damage refreshes the original health view without
discarding pending motion. Beta box raycasts test all six faces, including an exit when a segment starts inside a target.
These fixes contain no content-mod identity dispatch. Original quirks remain, including Camel cookies not being consumed and
GuineaPig carrying performing its effects while returning false.
[The advanced validation snapshot](../data/b173-advanced-validation-2026-11-07.json) records the regressions and remaining limits.

Ordinary native entity views also refresh dimensions and reciprocal mount relations. Actual Kitty prey selection now respects
small animals and baby-to-adult size changes; an actual More Creeps Bullet recognizes its shooter's native mount during its
original initial immunity window. Further original callbacks verify Bird leaf/tree steering, Thief partial/full theft and
damaged-item restitution, and Preacher contact/release effects.

Original zero movement speed pauses native navigation without discarding its route. The navigation timeout excludes paused
world ticks, while stale movement-control intent is cleared. Actual GuineaPig flower commands remain stationary for 240 world
ticks and resume the same route after FOLLOW, both with a retained target and with an explicit path.
[The gameplay validation snapshot](../data/b173-gameplay-validation-2026-11-07.json) records these cases and the pickup-copy regression.

Original position and rotation edits on native entity views are committed per changed coordinate, preserving unrelated native
corrections. Unmounted server players use native teleport synchronization. Hosted mounts dispatch the original
`updatePassengerPosition` and `getPassengerRidingHeight` APIs through Minecraft's rider-placement callback, including Beta eye offsets.
Actual Lawyer jail construction/teleport and Camel seat placement exercise these paths; a mixed-write test checks native corrections.

Native death completion also invokes the original `markDead` callback before final removal, retaining the native death sequence
and avoiding repeated removal. Actual Hotdog death creates one original Tombstone; its LifeGem callback consumes one gem and restores
the original saved pet statistics. Further actual callbacks cover ArmyGuy limb spawning/expiration, EvilScientist experiments,
Bunny landing retaliation, juvenile maturity differences and Mouse wall climbing.
[The death and placement snapshot](../data/b173-death-placement-validation-2026-11-07.json) records these checks and remaining coverage gaps.

Original nonliving entities and custom `ItemEntity` subclasses use two shared native host types. Original ticks, collisions,
pickup and NBT callbacks retain their original identities; explicit superclass calls select the base phase. A custom item
tick does not receive an extra modern item-physics or merging phase. Tracking includes original scalar fields, dimensions,
eye offsets and item ages, with canceled spawns rolling back their binding. Unregistered original classes can restore only
from an installed mod with a public World constructor and supported direct API dependencies.

Nine actual More Creeps cases pass: Bullet, Ray, Shrink, Frisbee, EvilEgg, Rocket, DesertLizardFireball, Money and GooDonut.
The fixtures exercise actual weapon costs, launch motion, collision damage/removal, pickup, tracking codecs, bounds and NBT.
Seven registered original sprite renderers emit geometry with their item-sheet cell mapped to a standalone texture and
normalized UVs. Cross-cell atlas sampling remains unsupported. Rocket and Fireball have no original renderer registration;
no replacement geometry is invented. The original Frisbee constructor leaves its owner unset; a separately controlled test
verifies its owner-only pickup branch. Original shadow offsets are applied only to shadow projection, preserving model position.
[The nonliving validation snapshot](../data/b173-neutral-validation-2026-11-07.json) records the payloads and exercised cases.

Native pickup-animation copies of hosted items preserve their host type, independent stack and original render snapshot.
Vanilla's inherited item copy kept the custom entity type while returning a plain item entity, causing the Beta renderer to
fail during pickup. Actual Money and GooDonut copy checks now exercise the virtual copy call and original sprite geometry;
the fix applies to the shared item host rather than selecting either mod class.

GuiAPI settings appear in the native Options grid as **Legacy Mod Settings**, with Back and Escape returning to Options.
Eight original particle classes pass constructor, callback, selected-sprite, packet-state, geometry/UV/color and lifetime tests.
These prove the exercised headless paths; genuine client ticking, GPU drawing and every subtype/AI decision have not received
equivalent visual gameplay proof.

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
- **Text fields and language labels** use the shared Beta `TextFieldWidget` and `TranslationStorage` APIs. Text fields retain
  the original focus, enabled state, character filter, backspace, maximum length and blinking end caret. An explicit Ctrl-V
  event carries the client clipboard only for that screen callback, including server-owned naming screens. Beta's separate
  32-character paste limit is preserved. Original screen callbacks still decide whether Save, Cancel or Done commits a name.
- **Entity query results** retain Beta's list ownership rules: `World.getEntities(Entity, Box)` clears and returns one mutable
  list per world; `collectEntitiesByClass` returns an independent mutable list. This matters when original gameplay retains
  an earlier query while issuing another one, including Mo' Creatures' bunny breeding loop. Native world queries are unchanged.

The [pet naming and query validation snapshot](../data/b173-pet-naming-validation-2026-11-08.json) records actual original
More Creeps Giraffe and Mo' Creatures Horse naming through native client interactions, keyboard/pointer events and real
save/reopen. More Creeps covers focus, typing, its original 31-character limit, explicit paste, Save and Cancel; Mo' Creatures
retains its own 15-character `MoCGUI` and Done behavior. An actual original Bunny breeding tick also exercises the wide-query /
empty-nearby-query crash boundary. Both mods pass fresh isolated server regressions. Hurt/death tint and Hotdog animation
are covered separately by the render effects snapshot above.

The [natural lifecycle validation snapshot](../data/b173-natural-lifecycle-validation-2026-11-08.json) records subsequent
client checks of original Bunny, Horse, Dolphin and Kitty births, both day/night Werewolf transformations, and native
save/reopen of offspring and stable forms. Parents receive ordinary food, taming and carrying interactions; original
readiness, growth, gestation and transformation fields and RNG are not forced. Already-born observation fixtures receive
native persistence only after birth so their UUIDs can be checked after reopening. Original counters absent from the mods'
NBT remain absent. More Creeps has no equivalent paired mating or Werewolf family; its actual EvilChicken lays its original
egg through normal ticks in a separate client run.

Shared fixes supporting these checks preserve the original APIs:

- Reachable original renderer methods that directly write Beta entity fields join the existing authoritative renderer-effect
  tick lane. This includes original variant initialization used by gameplay, rather than leaving server-side type and stats
  at their constructor defaults. Renderer/model-only field writes do not activate this lane.
- Raw position fields, dimensions and collision bounds remain independent through host synchronization and nested callbacks.
  Original `setBoundingBoxSpacing` changes dimensions only; `setPosition` explicitly rebuilds bounds. Movement derives position
  from the existing box and translates it through the native collision engine, including zero moves. This prevents original
  transformation animation offsets from becoming collision displacement through walls.
- Collidable hosted Beta passengers opt into native rider interaction targeting. An ordinary native crosshair/right-click
  check picks up and releases the original Bunny; the original callback still decides the mount toggle and throw behavior.
  Vanilla and modern entity targeting implementations are unchanged.

Fresh isolated server runs also exercise the controlled bounds API on the original Werewolf, EvilChicken and Money entity,
plus the original mods' existing regression suites. These are opt-in fixtures; the default build needs no private mod jars.
At that snapshot, complete compatibility, remote multiplayer and Sodium/Iris were unverified.

The [mob API checkpoint](../data/b173-mob-api-checkpoint-2026-11-08.json) adds shared ModLoader generation callbacks through
native placed features, generation-region block/entity writes, deferred chest/spawner attachment, component-preserving chest
views, ShockAhPI achievement pages and reflective entity-registry aliases. Original line pairs, loops and strips use the
native renderer; mod-declared entity references track native IDs plus UUIDs and resolve delayed spawn packets. The Beta path
wrapper returns null for an unreached start-only native path, matching clean Beta bytecode while preserving reached
singletons, useful partial routes and cursors. These changes apply through Beta APIs; vanilla and modern mob rules are unchanged.

The checkpoint passed 526 portable build tests and fresh isolated server suites with the supplied Mo' Creatures and More
Creeps archives. Mo' Creatures client checks with Sodium/Iris cover natural fish/shark egg hatching, original models and
textures, rare-horse riding/flight, Nightmare fire and charge expiry, rope attachment/following/teleport/detachment, and a
visible native rope render. A separate Kitty cohort completed milk consumption, litter use and native sand cleaning with
only initial adult/state prerequisites controlled; subsequent RNG and timers ran normally. Seven original actor UUIDs/classes
survived native save/quit/reopen. The original Fishy feeding limitation, Kitty sleep/tree/random drink interruption and
unsaved rope-holder policy remain intact. A fallen elevated-platform cat was explicitly replaced under a new fixture UUID;
its replacement is not presented as survival of the deceased actor.

A fresh normal-terrain More Creeps server completed 434 original generation callbacks and saved/stopped successfully.
That additional optional run exposed a bounds-test setup issue: the original EvilChicken damage callback assumes a nearby
player, which the normal-spawn fixture did not provide. It is recorded separately from the passing isolated suites.
Advanced More Creeps client gameplay, additional naming screens, advancement-page display, deferred-loot/structure census,
mixed-pack gameplay, shader packs and separate multiplayer checks remain pending. This checkpoint is bounded validation,
not full compatibility certification; actual-mod fixtures remain opt-in and the default build needs no private archives.

The subsequent [Prism load-failure checkpoint](../data/b173-prism-load-fixes-2026-11-08.json) fixes shared ScotTools tool
inheritance and descriptor-specific ModLoader generation override lookup. Original Machetes, MineFactory and PortalGun
archives each pass isolated loading and native item registration; the copied Prism pack loads all 14 legacy mods without
construction or lifecycle failures. The default build passes 540 portable tests without private mod archives. Three existing
PortalGun record IDs (2267–2269) overlap Butterfly jar items in that pack; settings and original versions were preserved.
These load checks do not establish complete gameplay compatibility.

The legacy client player lookup also retains the current same-level connected actor during the native death screen,
matching Beta's player lifetime without caching logged-out actors or borrowing players from other dimensions. Actual
More Creeps Robot Ted callbacks survive native death and 100 subsequent ticks, and native respawn resolves the new player
object with the same UUID. The distributed jar contains its Pehkui and TerraBlender dependencies; missing Pehkui in the
development-only test classpath produced separately recorded Witchery callbacks, not a passing Witchery gameplay claim.

The [Machetes gameplay regression](../data/b173-machetes-gameplay-validation-2026-11-08.json) subsequently found that its
distribution still contained an installation folder around the textures. The archive was explicitly repacked with identical
class and image bytes; no archive-layout support was added to the loader. All six original sprites match the uploaded GPU
pixels and render in the hand/hotbar. The shared Beta `Item.getAttackDamage(Entity)` callback now contributes to native
attacks with native cooldown and critical handling preserved. Actual attacks produce the original gold/iron/diamond/stone/
wood/slime totals of 5/7/9/6/4/7, with two durability wear per hit; native unarmed/sword, Strength, Sharpness and cooldown
controls pass. A normal client attack-key hit independently deals nine damage with the diamond machete. The default build
passes 544 portable tests; mining and every other tool callback are outside this bounded check.

The [generation and painting validation snapshot](../data/b173-generation-painting-validation-2026-11-08.json) reproduces
an actual More Creeps Castle hang: two sampled neighboring chunks had only reached structure starts and returned entirely
empty columns, so the original downward ground search never found three supports. Native `FEATURES` prerequisites now
require completed `CARVERS` terrain throughout the existing eight-chunk read area when Beta generators are registered.
The Beta world view also rejects cached chunks that have not completed noise generation, while preserving explicit pending
writes. Native feature tasks, write radius and the loading pyramid retain their original behavior. This increases the
accumulated full-chunk dependency radius from 11 to 18 in Beta generation packs; a vanilla-only server retains radius 11.

The previously hanging chunk completes through its original native callback. Original Pyramid/Castle generators succeed
on explicitly controlled terrain, and 90 recorded native block states, chest Items and spawner SpawnData survive real
save/reopen. Those controlled calls are distinct from ordinary random site selection.

The subsequent [natural structures and chest validation](../data/b173-natural-structures-chests-validation-2026-11-08.json)
records positive original More Creeps 2.12 Pyramid and Castle placement through native `FEATURES` in fresh, unedited terrain.
The original readiness counters, random rolls and generator bodies run unchanged. Read-only debugger observations,
completed native callbacks and physical native contents independently identify the successful sites. Exact block states,
chest Items and spawner SpawnData at 88 recorded positions survive a real native save/reopen.

The original Castle fills only one half of its central double chest. Explicit Beta vanilla-chest writes now pair native
chest states and retain matching facing, preserving each original block entity, inventory and item components. Both
highest-floor halves open the same six-row native menu with the original loot. Portable tests and an explicit native
fixture cover both axes, metadata changes, ambiguous triples and deferred cross-chunk inventories. Existing SINGLE chest
states are not scanned or migrated; pairing applies to subsequent explicit Beta writes. Modern chest placement has no
global hook. The default build passes 558 portable tests without private mod archives.

The [Hotdog, Scientist and EvilEgg validation](../data/b173-hotdog-scientist-evil-egg-validation-2026-11-09.json)
adds generic native-lightning dispatch to the original Beta callback across hosted entity families. The Beta default
uses its fire-immune damage helper and original ignition counter; mod overrides retain control. Vanilla entities retain
native lightning handling. An actual More Creeps Scientist survives its original experiment lightning, builds and climbs
its tower, produces offspring and gains the original 50 health on transformation. An in-progress experiment retains its
UUID, tower and stage across native save/reopen; its timer continues on entity ticks rather than assuming every startup
world tick also ticks the entity.

Hotdog's original 2.12 reader deliberately raises `DogSize` below `0.7` to `0.7`; this is preserved. Native feeding, death,
Tombstone/LifeGem revival and save/reopen match an independent original-reader oracle. The opt-in
`-Dlegacyconnector.hotdogPersistenceSelfTest=true` fixture covers sizes `0.6`, `0.7` and `0.8`, repeated native registry
loads, saved stats, item components and renderer tracking, without requiring the private mod during a default build.

Actual native EvilEgg inputs, owner tracking, collision and original hatch callbacks are observed with unchanged RNG.
The original code can explode after a successful hatch, including after detecting its own newborn; that behavior remains
intact. A later wall-impact hatch survives and lays a real original egg through its natural timer. The same offspring UUID
and health survive native save/reopen. Safe native observation placement happens only after the hatch and laying proofs;
it does not replace the offspring or force its gameplay fields. Original unsaved laying timers remain unsaved.
The default build passes 562 portable tests; these bounded actual-mod checks do not establish complete mod compatibility.

The shared Beta painting API retains all 25 original motives, facing and wall checks, then uses native painting tracking,
rendering and persistence. Double-precision bounds prevent thin boxes collapsing at far modern coordinates. Native-region
tests cover overlap, deferred supports and proto-chunk entity NBT; a visible valid-wall painting retains its UUID, motive,
facing and support after real reopen. The tested Castle's ten painting requests target air support cells and have no fitting
motives; those original rejections are preserved. The positive painting fixture is separate from Castle placement.
The default build passes 548 portable tests without private archives; full-pack startup checks retain all 14 legacy mods.

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

## Historical combined-pack results

These earlier combined-pack results are retained as history, not current isolated compatibility ratings. In particular, the
old attribution of More Creeps failures to GuiAPI is not supported by the newly audited v2.12 payload: it has no GuiAPI/TWL
references. The isolated v2.12 test constructs and exposes an oversized crafting-output issue instead. Mo' Creatures does
require GuiAPI; its current isolated construction result is documented above.

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

## Archive-first compatibility audit (2026-11-07)

Keep the existing Beta foundation and implement shared APIs/original callbacks only. First establish compatibility with one mod
and its reviewed required dependencies in a fresh game directory; combined packs come later. A class that links through a synthesized
default is not a working API. Named mod checks belong in regression fixtures, not production behavior.

The local archive scan covers 1,040 ZIP/JAR downloads plus two loose class files. Four downloads are empty; four files named `.zip`
are actually RAR and were read with 7-Zip. Nested archives and install roots produce 1,222 units: 548 mod payload variants with no
detected base replacements, 572 with unknown vanilla-class replacements, 20 with byte-identical known API patches, 24 API distributions,
54 library/documentation units, two loose-class cases and two path-review cases. These are not counts of unique mods.

The scanner compares internal class identities and SHA-256 hashes against the original client/server jars. Known API installers
are fingerprinted separately. Direct class/descriptor references, reflection-style class strings, and README dependency mentions are
separate evidence; optional integrations and external resources still need manual review. Absence of replacement classes does not prove
absence of dynamic engine patching. Byte differences identify replacements, not necessarily semantic changes.

Run `python tools/dev/audit_b173_archive.py "<archive folder>"`. Full `inventory.json`, `inventory.csv`, and `priority-review.csv`
go to `build/b173-archive-audit/`; the curated queue and API-demand CSVs from this audit are beside them. The compact tracked
[audit snapshot](../data/b173-archive-audit-summary.json) records the queue, limitations, generic-code review and isolated results.
The queue is a curated major-mod shortlist, not a measured popularity ranking. It includes PlasticCraft, BuildCraft, Equivalent
Exchange, IC2, MineFactory, RedPower, Portal Gun, Millenaire, Mo Creatures, Aether 1.01 and Twilight Forest, with versions and payload
paths pinned. Aether 1.02, both archived NetherCraft 1.9b variants and Better Than Wolves remain in the base-edit review group.

For a fresh single-target run, use:

```text
python tools/dev/test_b173_one.py buildcraft-client-A-core-2.1.1_b173.zip --label buildcraft-core-211 --run
python tools/dev/test_b173_one.py "PlasticCraft_(v2.1.3)_b173.zip" --root minecraft/ --label plasticcraft-213 --run
```

Set `JAVA_HOME` to JDK 21 first. The runner stages only the selected payload into a new `run-b173-single/` directory, checks source
hashes, binds the headless server to loopback on an ephemeral port, and writes `audit-manifest.json` and `test.log`. It does not change
Prism or `run-subtest`. Do not launch tests concurrently or rebuild classes while a server is running. Required dependency modules
and external resource folders must be reviewed separately; the runner intentionally does not guess or install original API patches.

Initial isolated results on the merged upstream code:

- BuildCraft Core 2.1.1 constructs and registers six items, but logs a missing `buildcraft.energy.PneumaticPowerFramework` class.
  This requires module/dependency review, not a working-mod rating; zero-block GUI checks do not validate machines or pipes.
- PlasticCraft 2.1.3 fails alone in `ItemBlockPlastic.getTranslationKey` with an empty-array access. This proves its construction
  failure is independent of other mods' block IDs. No mod-specific workaround was added.

Follow-up isolated tests and original bytecode review:

- Core deliberately catches the missing optional Energy framework and falls back to its bundled redstone framework.
  The logged exception by itself was not a connector defect. Core + Energy exposed the missing vanilla/Forge bucket APIs;
  after those generic fixes, both modules pass the headless smoke test (3 blocks, 10 items, 9 recipes, engine tile save/reload).
  GUI registration, liquid state, dirty-block notifications and model methods still include stubs; machines/pipes are not fully verified.
- PlasticCraft now passes the isolated headless smoke test after shared dye/wool and armor API fixes. Spawn registration,
  achievements, healing, native armor equipment and original GUI rendering remain incomplete or unverified.
- The runner requires every staged entrypoint to construct and rejects logged callback failures even without exception stacks.
  A successful Gradle exit alone never establishes compatibility.

The initial generic review covered 131 Beta production/loader files; `python tools/dev/check_b173_generic.py` and the broader
identity guard remain required checks for subsequent fixes.
Review of item/block callbacks, fence geometry, inventories, animations, entity views and projectile texture discovery found no
content-mod identity selectors in those production paths. The texture-discovery bytecode heuristic remains limited to supported
patterns/constant paths; being generic does not make it a complete renderer. The guard supplements review, not semantic proof.

## Plan

0. **GUIs, rest**: ModLoaderMP's client side (`BaseModMp.HandleGUI` builds the client GUI for a server's `OpenGUI(player, id, inventory,
   container)`; today the client draws a plain panel for those), entity GUIs (item right-click callbacks are bridged), tile entity
   special renderers (`RegisterTileEntity(class, id, renderer)`, kept).
1. **Rendering, rest**: remaining standard block render types required by installed mods, broader custom renderer helpers/GL calls,
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

## Archive.org test run (2026-11-08)

Mods downloaded from archive.org copies of their original distributions (the "Minecraft Beta 1.7.3 Mod Archive", the NetherCraft and BuildCraft items), each
tested alone in a fresh game directory with only its reviewed dependencies, server side first (`runServerSubTest`), then a real graphical client with the
usual performance mods for three of them. Results are in `docs/data/mods.json`; screenshots in `docs/screenshots/b1.7.3/`.

| Mod | Server run | Client |
|---|---|---|
| NetherCraft 1.9b (ModLoader-only zip) | 35 blocks, 110 items, 0 failed callbacks, 3 tile entities round-trip | blocks render, textured |
| The Aether 1.01 (+ Trivia) | 29 blocks, 112 items, 0 failed callbacks | blocks render, textured |
| Equivalent Exchange (Forge) 3.55 | 11 blocks, 42 items, 8 tile entities round-trip | blocks render, textured |
| Mars Mod 1.2 | 10 blocks, 20 items | not run |
| PremiumWood 14.4 (+ ScotTools 10.1) | 34 blocks, 41 items | not run |
| Dragon Mod 2.3 | 3 blocks, 7 items; dragons not hosted (original `jump` / `moveNonSolid` overrides) | not run |
| Elemental Creepers 1.4 | 9 creeper species hosted with their renderers (see below) | creepers render, textured |
| BuildCraft Core + Energy 2.1.1 | 3 blocks, 10 items | not run |

Defects found and fixed generically:

- Classes of the default package shared one package in the original game; each mod now has its own package, so one mod's use of another's package-private
  or protected members (ScotTools' `ToolMaterial` constructor from PremiumWood) failed with `IllegalAccessError`. The deobfuscator widens members and classes
  that came from the default package to public.
- A block right click now runs with the clicking player as the legacy client's player (`mc.thePlayer`), also when the player is not in the level's player list
  yet (Mars Mod's vacuum heater read a null player).

Not usable as test input: NetherCraft's ScotTools zip needs ScotTools (use the ModLoader-only zip), and the Fringe Lands archive is only obfuscated base class
edits (no `mod_` class, 0 classes translated). Dragon Mod shows the next mob-host gap (`jump` / `moveNonSolid` overrides).

### Second round (2026-11-08)

More archive.org mods, server runs in fresh directories: IndustrialCraft 2 8.55 (67 blocks, 159 items, 46 tile entities round-trip, 0 failed callbacks),
RedPower 1.7.1 (Core, Wiring, Logic, Lighting, Array together), MineFactory Reloaded 1.1.2 (its IC2 compat mod needs IC2), Portal Gun v5, More Creeps
and Weirdos 2.12 (53 mobs hosted), Too Many Items and Rei's Minimap (construct). Single Player Commands 2.11.1 and Fringe Lands are base class edits
without a `mod_` class and cannot be loaded.

**Elemental Creepers** were reported as "base mob family is not hosted yet". That message, printed by the cow-only host (`MobsB173`) for every
non-cow mob, was misleading (it is gone; `AnimalsB173` admits or reports every other family). The real blockers were generic gaps: the shim had no
`DataTracker` (now a real per-entity slot store on `Entity`), no `CreeperEntityModel`, `LightType`, `World.getBrightness(LightType, ...)`,
`World.ambientDarkness`, `Entity.onStruckByLightning` or `LivingEntity.animateSpawn`; and the admission lists did not accept the renderer's
`getOverlayColor` (accepted, not drawn: the fuse flash is missing) nor the no-op lightning/spawn callbacks. All 9 species now host.

Real client check (superflat test area, performance mods on, mobs summoned and the tick frozen for the shot): `docs/screenshots/b1.7.3/mobs-*.jpg`.
Elemental Creepers and Mo' Creatures animals render with their own models and textures. Follow-up isolated More Creeps lineups
and exact archive-byte checks found the expected textures, including intentionally bright BubbleScum artwork. The Portal Gun
audio self-test skips when the mod's external sound files are not installed.
