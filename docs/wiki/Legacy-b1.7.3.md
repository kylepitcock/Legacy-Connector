# Legacy Beta 1.7.3 (ModLoader and Forge 1.0)

Status and design of Minecraft Beta 1.7.3 support. Back to [Home](Home.md). Started 2026-11-06.

**Status: started (loader only).** Beta 1.7.3 ModLoader / ModLoaderMP / Forge 1.0.x jars are detected, deobfuscated, linked against a
synthesized b1.7.3 API and loaded into the game's module layer. The ModLoader runtime is not written yet: `PlatformB173` loads each `mod_*`
class but does not construct it, so **nothing of these mods enters the game yet**.

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

## Plan

1. **ModLoader runtime**: `BaseMod` lifecycle (constructor registration, `ModsLoaded`, `OnTickInGame`/`OnTickInGUI`, `AddRecipes`,
   `GenerateSurface`/`GenerateNether`, `AddRenderer`, `RegisterAnimation`, `KeyboardEvent`), `MLProp` config files, `ModLoader.*` static API,
   reflection by obfuscated name.
2. **Bridges**: numeric block/item ids (blocks under 256, items shifted by 256) and 4-bit metadata to registry entries, terrain.png /
   gui/items.png sprite indices and `ModLoader.addOverride` / Forge sprite sheets to atlas sprites, recipes to a data pack, tile entities,
   entities (`RegisterEntityID`), world generation to placed features, dimensions (Nether-style, ShockAhPI later). Apron's StationAPI
   modules (`ModContentRegistry`, `SpriteSheet`, `MetaBlockState`) are the reference for the id/sprite/metadata mapping.
3. **Logic on the server**: plain ModLoader mods expected one world. Their logic (`OnTickInGame`, `mc.theWorld`, `mc.thePlayer`) runs
   against server-side views, rendering and GUIs stay on the client, `ModLoader.OpenGUI` becomes a synced menu; ModLoaderMP's
   `Packet230ModLoader` becomes a layer payload. Single player first, dedicated servers partial.
4. **Other APIs**: ShockAhPI, PlayerAPI, GuiAPI, ModOptionsAPI, AudioMod, ItemSpriteAPI (as bridges, the way Apron provides them).
5. LWJGL 2 / GL11 for b1.7.3 mods currently links to synthesized stubs; the 1.7.10 GL emulation should serve them too.
