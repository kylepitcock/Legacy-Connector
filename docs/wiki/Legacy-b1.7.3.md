# Legacy Beta 1.7.3 (ModLoader and Forge 1.0)

Status and design of Minecraft Beta 1.7.3 support. Back to [Home](Home.md). Started 2026-11-06.

**Status: started (blocks, items, recipes and their textures).** Beta 1.7.3 ModLoader / ModLoaderMP / Forge 1.0.x jars are detected,
deobfuscated and linked; the mods are constructed as ModLoader did, their blocks, items and recipes become modern content with their
own textures, one creative tab per mod. Not yet: block behaviour beyond drops, tile entities, entities, world generation, GUIs, tick hooks.

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
   now ("Slot N is already occupied") moves that `@MLProp` id and constructs the mod again. A mod that fails gives its block and item ids
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
  doors' negative indices (a mirrored face) count as their absolute value.
- **Models.** One block model per metadata, deduplicated: faces down, up, north, south, west, east are b1.7.3's sides 0-5, the block's
  bounds are the element (slabs, plates), render types 1 (crossed plants), 2 (torches) and 6 (crops) use the vanilla shapes, other render
  types (custom renderers) are a cube for now; non-opaque blocks are cutout, render layer 1 translucent. The blockstate maps `meta=0..15`.
  Block items use the block model (flat sprite for plants and torches), items a flat or handheld sprite.
- **Names**: `ModLoader.AddName` / `AddLocalization`, else the translation key made readable.
- **Creative tabs** (`CreativeTabsB173`): b1.7.3 had no creative inventory, so each mod gets one tab with its blocks then its items; a block
  item with subtypes lists each metadata whose sprites differ.
- A mod that re-creates a vanilla block or item at its vanilla id (NetherCraft's tools) keeps the vanilla 1.21.1 one: no new entry, model
  or tab slot.

The server test generates the assets too (`-PassetsDump=<dir>` writes them), which is how they can be checked without a client.

OpenAL: b1.7.3 mods call LWJGL's OpenAL directly (IndustrialCraft 2's audio). Initialised on the mod-loading thread, before the sound
engine made a context, the real AL classes fail for good and the client's first resource reload never finished (stuck loading screen).
`org/lwjgl/openal/` of b1.7.3 jars goes to the synthesized API (silent) instead (`LegacyRemapper`).

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

Dedicated server with 15 b1.7.3 client builds (`run-subtest/mods/b1.7.3/`): 10 construct (NetherCraft, Portal Gun, Minefactory Renewed,
all five RedPower modules, Butterflies, Twilight Forest b1.7.3); 51 blocks and 230 items registered, 51 of 51 blocks placed by the self test,
1487 recipes translated (3 name items nothing provides). Failing, as expected for now: ComputerCraft (reads a vanilla texture), Coral Reef
and More Creeps (need GuiAPI), Moon Mod (needs ShockAhPI), IndustrialCraft 2's client build (LWJGL on a dedicated server).

Client (dev client, 11 jars): NetherCraft, Portal Gun, Minefactory Renewed, RedPower, Butterflies load with textured blocks and items in
their creative tabs; 51 blocks, 159 items, 219 textures generated, 1 sprite reference without a texture. IndustrialCraft 2's client
build now gets past OpenAL and stops at `ModLoader.getPrivateValue` with an obfuscated field name (plan item 6).

## Plan

1. **Rendering, rest**: custom block renderers (`RenderWorldBlock`, render types 7-17: doors, stairs, fences, rails, ...), animated
   sprites (`ModTextureAnimation`), colour multipliers, item subtypes as their own items (today one modern item per legacy item).
2. **Behaviour**: block callbacks (`onUse`, `onPlaced`, `neighborUpdate`, `onTick`, redstone) through a `World` view, tile entities
   (`RegisterTileEntity`) on one modern block entity type, entities (`RegisterEntityID`), world generation (`GenerateSurface` /
   `GenerateNether` as placed features), fuel (`AddFuel`).
3. **Logic on the server**: plain ModLoader mods expected one world. Their logic (`OnTickInGame`, `mc.theWorld`, `mc.thePlayer`) runs
   against server-side views, rendering and GUIs stay on the client, `ModLoader.OpenGUI` becomes a synced menu; ModLoaderMP's
   `Packet230ModLoader` becomes a layer payload. Single player first, dedicated servers partial.
4. **Other APIs**: ShockAhPI, PlayerAPI, GuiAPI, ModOptionsAPI, AudioMod, ItemSpriteAPI (as bridges, the way Apron provides them).
5. LWJGL 2 / GL11 for b1.7.3 mods currently links to synthesized stubs; the 1.7.10 GL emulation should serve them too.
6. Reflection by obfuscated name (`ModLoader.getPrivateValue(cls, obj, "a")`) needs the official to named table at run time.
