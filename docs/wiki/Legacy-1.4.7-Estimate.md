# Estimate: legacy 1.4.7 Forge mods on NeoForge 1.21.1

Written 2026-11-06 after the M0 work (corpus, scanner, tables, loader groundwork). Measured numbers are in [Legacy-1.4.7-Corpus](Legacy-1.4.7-Corpus.md); the model is the
1.6.4 one ([Legacy-1.6.4-Estimate](Legacy-1.6.4-Estimate.md)), with 1.5.2 as the version in between. Back to [Home](Home.md), plan in [Version-Roadmap](Version-Roadmap.md).

## Short answer

**About 1.5 to 2.5 days of wall-clock time after the 1.5.2 layer loads (the roadmap said 0.5 to 1 day), 3 to 5 agents in parallel, plus or minus 50%.** The delta over 1.5.2 is mostly three things the
roadmap did not count: **sprite-sheet textures** (1.4.7 has no `Icon`), the **ModLoader `BaseMod` emulation** (27% of the corpus touches it) and the **notch-name dependence** (96% of the jars are obfuscated). Everything else (numeric
ids, `@NetworkMod`, tick handlers, player trackers, `Packet250CustomPayload`) is the 1.6.4 work and is shared. M0 took about 2 hours.

## What differs between 1.4.7 and 1.5.2

| Area | 1.4.7 | Work |
|---|---|---|
| **Names in the jars** | notch (obfuscated) names in 96% of the jars (`yc`, members `a`, `b`); the dev sources used flat `net.minecraft.src.<Name>` (MCP 6.x), the last Forge builds (6.6.2.5xx) already the packaged MCP 7 names, and 20% of the jars mix notch with packaged descriptors | **Done in M0:** `legacy/1.4.7/notch.txt` (notch to flat MCP/SRG names, from the client jar and `joined.srg`), `forward/1.4.7/flat-names.tsv` (1,339 flat classes to their 1.5.2 packages), `ForwardRenames.flat`, the member lookups walk the flat hierarchy and compare packaged table keys. Open: `NotchNames` misses about 5% of member references (mod overrides called through a vanilla owner), closed in M1 |
| **Version detection** | no class-name markers: 1.4.7 shares `@PreInit`/`ISidedInventory`/`ForgeSubscribe` with 1.5.2 and 1.6.4 | **Done:** a per-version fingerprint of the obfuscated member references (right version 90 to 100%, wrong ones 0 to 50%), 1.4.7-only FML classes, flat names, declared version, file name; the existing 1.5.2/1.6.4/1.7.10/1.12.2 detection is unchanged (checked on 162 jars) |
| **Packaging** | `.zip` is 34% of the mods, ModLoader mods have no `@Mod` and no mcmod.info | **Done:** `mods/1.4.7/` folder (zips accepted there), `BaseMod` subclasses become mod declarations (`LegacyModDecl.modLoader`) |
| **Textures** | integer indices into sprite sheets; see below | the largest new item, 1 to 1.5 days |
| **ModLoader** | `BaseMod`, `ModLoader.registerBlock/addName/addRecipe/addShapedRecipe/registerEntityID/addOverride/getUniqueSpriteIndex/registerTileEntity`, `MLProp`, `IBlockRender`... as default-package classes of a reobfuscated jar | hand-written shim (about 600 to 900 lines; FML's `ModLoaderModContainer` semantics: `load()`, `modsLoaded()`, `addRenderer`, `takenFromCrafting`...), 3 to 4 hours |
| **Lifecycle** | `@PreInit`/`@Init`/`@PostInit` in 81% of the jars (no `@EventHandler`) | shared with 1.5.2 |
| **Numeric ids, network, ticks, trackers** | as 1.6.4 (51 of 65 jars use ids; 43 `@NetworkMod`; 27 tick handlers) | shared with 1.6.4/1.5.2: this is *their* M1/M2 |
| **Liquids** | `net.minecraftforge.liquids.*` (`LiquidStack(itemID, amount, meta)`, `ITankContainer`, `LiquidContainerRegistry`) in 13 jars (20%) | adapter onto fluids (`FluidStack`, `IFluidHandler`, capabilities); also used by the 1.5.2 and 1.6.x mods, so it pays three times; 3 to 5 hours |
| **Small deltas** | `RenderEngine` (37 jars), `AABBPool`/`Vec3Pool` (20/12), `ISidedInventory` (Forge's own, 18), `IArmorTextureProvider` (8), `SoundPool` (7), `TexturePackList`/`ITexturePack` (8/7), `IClassTransformer` coremods (10), `StepSound`... | `RenderEngine` and the pools are 1.5.2/1.6.4 items; `ISidedInventory` and the armor provider are a capability adapter and a texture hook; sounds need a `SoundPool` facade |
| **Jar-mod style** | Better Than Wolves (and its add-ons) replace vanilla classes | out of scope, as in every version |

## Sprite sheets

The 1.5.2 layer already has to turn string texture paths into `ResourceLocation`s (the `RenderEngine.bindTexture(String)` family, 37 of 65 jars here), and 1.6.4 has `Icon`s that the shim maps onto `IIcon`. 1.4.7 needs both of those plus
index-based textures. What the measurements say about size ([corpus](Legacy-1.4.7-Corpus.md#sprite-sheets-the-big-difference-from-152)): 41 of 65 jars (63%) use Forge's per-block texture file, 22 (33%) set `blockIndexInTexture`/`iconIndex`/`setIconIndex`
by hand, 19 (29%) name the vanilla sheets `terrain.png`/`gui/items.png`, 42 reference at least one 256x256 sheet (255 sheets in total, 248 of them 256x256 with a 16-pixel grid), only 3 register animated textures.

Design (all native, no side channels):

1. **A sheet registry in the loader's asset step.** For each sheet PNG a mod ships (found by the string constants and `setTextureFile`/`preloadTexture` arguments, the same scan this corpus uses; the 256x256 grid is assumed), the
   pack generator cuts the 256 tiles lazily and writes them as individual sprites `legacy147/<mod>/<sheet>/<index>` into the generated resource pack, so the vanilla atlas stitches them like any other texture (animations: one `.png.mcmeta` per tile where a TextureFX
   updates it, a later item). Cutting only the indices the code can reach would need data-flow analysis; cutting the full 16x16 grid is cheap (a few hundred 16x16 sprites per sheet), and empty (fully transparent) tiles are skipped.
2. **Index to `IIcon` in the shim.** `Block.blockIndexInTexture`, `Block.getBlockTextureFromSideAndMetadata`, `Item.iconIndex`, `Item.getIconFromDamage` return ints; the shim's `getIcon(side, meta)` wraps them: `(textureFile(), index)` to the registered sprite.
   `textureFile()` defaults to `/terrain.png` or `/gui/items.png`, `setTextureFile` and `ITextureProvider.getTextureFile()` override it. Mods that override the int methods keep working; mods that override `getIcon` do not exist in this era.
3. **The vanilla sheets.** Indices into `terrain.png` and `gui/items.png` that a mod uses (copying the look of stone, planks, iron ingots) need a 256-entry table index to 1.21 sprite name each (about 500 entries, data; derived from the 1.4.7 `TextureMap` layout, a few hours).
   `ModLoader.addOverride("/terrain.png", file)` and `getUniqueSpriteIndex` allocate free slots in the vanilla sheets: the registry hands out virtual indices above 255 and maps them to the override file.
4. **Whole-image textures** (mobs, GUIs, armor, tile entity renderers: `bindTexture("/mods/x/gui.png")`, "/gfx/..." paths) go through the 1.5.2 string-path resolver; 1.4.7 only adds the leading-slash resource roots (`/gfx/<mod>/`, `/mods/<mod>/`, `/mob/`, `/armor/`)
   and the rule that a mod's file shadows vanilla's of the same path.
5. **Renderers.** `ISimpleBlockRenderingHandler` and tile entity renderers bind sheets and read `Tessellator.setColorOpaque`/`addVertexWithUV` with UVs computed from the index (`(index % 16) / 16.0`): those UVs need the sheet layout of the *original* sheet, not the atlas. The 1.7.10 shim already
   rewrites UV math for `IIcon`; for index-based code the cheap route is to keep the bound sheet as its own texture for tessellator-based rendering (the whole PNG bound to a single texture unit, as FML did) while block/item models use the cut sprites. That is a second path to maintain; 4 to 6 hours.

Total: about **1 to 1.5 days** with two agents (the cutter and the index shim; the vanilla index table; the renderer path), most of it after 1.5.2's string-path work is in. This is the main risk: if tessellator-based renderers (BuildCraft pipes, Forestry,
RedPower, 20 to 30% of the corpus) need the second path in full, add half a day.

## Milestones (after the 1.5.2 layer boots, wall-clock)

| Milestone | Content | State and estimate |
|---|---|---|
| M0 | Corpus (65 files), `scan_147.py`, `legacy/1.4.7` tables, `LegacyVersion.V1_4_7` (forward chain 1.4.7 to 1.5.2 to 1.6.4 to 1.7.10), flat-name relocation, notch-fingerprint detection, `mods/1.4.7` + zips, runtime family mapping | **done** (this page's date); 64 of 65 jars detect and translate in `checkLegacyMod` |
| M1 | Close the notch lookup gap (5%), member forwards for what the 1.4.7 jars call (4,320 synthesized members over 64 jars: the id family, `registerBlock`, `Item.setItemName`...), `BaseMod`/`ModLoader` emulation, liquids adapter, load without sprite sheets (blocks and items register with a placeholder) | 4 to 6 hours; the server test boots the corpus subset |
| M2 | Sprite-sheet registry + index shim + vanilla index table, string-path textures, `ISidedInventory`/armor provider hooks, entities and world generation as in 1.5.2 | 1 to 1.5 days; first mods rendering |
| M3 | Client verification (BuildCraft pipes, RedPower, Forestry bees, Twilight Forest, Mystcraft), sounds, GUIs | 0.5 to 1 day, in parallel with M2 |

So 1.4.7 is playable-tier roughly 1.5 to 2.5 days after 1.5.2; with the 1.6.4 and 1.5.2 estimates the 0.4.0 milestone stays at **4 to 5 days** (the roadmap said 3 to 4, range 2 to 5; sprite sheets and ModLoader move it to the upper half).

## What blocks M1

- The **1.5.2 forward tables** (`forward/1.5.2/*`: EntityLiving/EntityLivingBase, the 1.5.2 to 1.6.4 class and member deltas, string-path textures) are not written yet; 1.4.7 currently falls back to the 1.6.4 tables only, which is what the synthesized-member numbers measure.
- **`BaseMod`/`ModLoader`** does not exist in the shim: the inspector finds ModLoader mods, but they would be linked against empty synthesized classes (6 pure ModLoader jars of 65, 18 using the `ModLoader` class).
- **Sprite sheets** (above) for anything that renders; until then blocks and items would have no textures.
- The 5% of unresolved notch members (`TileEntity.a`...), best fixed in `NotchNames` by resolving overrides through the jar's own class hierarchy for 1.4.7 as well.
- The **missing corpus core**: IC2, Railcraft, Thermal Expansion, Thaumcraft 3, EE, Natura, Tinkers' Construct, Mo' Creatures, Biomes O' Plenty are not obtainable (no 1.4.7 file reachable). Hexxit/Tekkit-style content (the tech trio plus Thaumcraft) cannot be tested; BuildCraft 3.4, Forestry 2, RedPower 2,
  Applied Energistics and Twilight Forest are the substitutes.
- No game run was done in M0 (no instances were started): everything above is offline link/translation evidence, not runtime evidence.
- Coremods: `IClassTransformer`-style plugins (CodeChickenCore and 9 more jars here) depend on the coremod API work done in parallel for 1.6.4/1.5.2.

## Risks and cost drivers

- **Value:** 1.4.7 has the smallest pool of the three old numeric-id versions (about 175 to 410 mods, see the roadmap evidence) and many of its mods have 1.5.2/1.6.4/1.7.10 versions; what exists only here is mostly the Hexxit/Tekkit Lite/Feed The Beast Classic-era tech mods
  and ModLoader mods. Kept because it is a cheap delta *and* because the notch-name and sprite-sheet machinery is what 1.2.5 (the next ModLoader-era target) needs too.
- **Sprite sheets and tessellator UVs** are the one place where this version can be much more work than the rest (see the 4 to 6 hour renderer path above).
- **Detection of tiny jars** (a few hundred bytes, no obfuscated member references) relies on declared versions and the file name; one corpus jar (hopper-1.0.jar) is still not recognised.
- **Shared maintenance:** `ForwardRenames.flat` is the only 1.4.7-specific hook in the shared remapper path; everything else is data or reuses the 1.6.4 classes.

## Status 2026-11-07 (M1 done, M2 started)

Server loads of the corpus in batches (`./gradlew runServerSubTest`, mods in `run-subtest/mods/1.4.7`) and one client session (Applied Energistics rv9 and RedPower 2, with Sodium, Iris and shaders):

- **Loads and runs** (no mod failed): ModLoader mods (`mod_SprintKey`, `mod_Gibbing`, Entity Detector, CCTV), Hats, Portal Gun, Gravity Gun, Dragon Mounts, More Swords, Grimoire of Gaia (48 mobs alive), Resource Engineering, BiblioCraft,
  Custom NPCs, CameraCraft, Extra Buttons, RedPower 2 (all ten modules), Applied Energistics, Mystcraft, Factorization, ComputerCraft, MiscPeripherals, BuildCraft, Forestry, Twilight Forest (dimension generates). Per-mod ratings are in `docs/data/mods.json` (source "client").
- **Sprite sheets work for the main sheets**: tiles are cut by `SheetCutter`, a sheet that code names without its folder (`GfxPath("block.png")`) is found by file name, and an index-based block or item now wins over the icon a default `registerIcons` would register (that placeholder hid every index texture of the item sheet). AE's quartz tools and ore draw correctly in the client.
- **Fixed this round:** a default-package mod class (ModLoader `mod_X`) kept two private roots; items a numeric-id mod never registers are registered (More Swords); constructor adaptation casts narrowed parameters instead of passing null (AI task owners: every Grimoire mob failed to tick); notch member lookup through packaged Forge interfaces (`ISidedInventory` implementors kept notch method names); one synthesized name per class (flat and packaged `TextureFX` were two classes, a `VerifyError` for RedPower's animated textures); `BiomeCacheBlock` (Twilight Forest chunk generation); the home area on `EntityLiving` (ghasts).
- **Open:** RedPower's own sheets (`worlditems1.png`, `base1.png`) are cut but its items name tiles through a path the layer does not read yet (`iconIndex` stays -1), tessellator renderers (RedPower multipart, AE cables) are untested, texture animations (`TextureFXManager`) link but do nothing, the liquids API adapter is not written, `@PreInit/@Init/@PostInit` work (every mod above uses them), Mystcraft's age dimensions, Forestry/Binnie's duplicate genetics API classes, Immibis' mods need IC2/ComputerCraft present.
- **What blocks "supported":** the open items above plus client verification of GUIs for more than one mod; the corpus has no IC2, Railcraft, Thermal Expansion or Thaumcraft 3.

## Status 2026-11-07 (evening: RedPower renders)

Verified in a real client (Sodium, Iris, Complementary shaders; RedPower 2 all modules in the Core/Digital/Mechanical/Compat zips, Applied Energistics rv9, BuildCraft, ComputerCraft, MiscPeripherals, Entity Detector, CCTV, Old Vein Miner, Block Helper, Hopper, rpcDrive); screenshots in `docs/screenshots/old-versions/1.4.7`.

- **Item textures of RedPower:** the cause was not an unread path. RedPower's tools call `Item.setIconCoord(column, row)`, which the shim lacked (a synthesized no-op), so `iconIndex` stayed -1; `ItemParts`-style items (`getIconFromDamage(int)` overrides) already worked through the bridge. `setIconCoord` is now a real method; all RedPower items draw.
- **Tessellator and multipart renderers:** three defects hid every RedPower block drawn by its own handler. `RenderBlocks.overrideBlockTexture` is an `int` (-1 = none) in 1.4.7 and an `IIcon` in 1.7.10, so the mod's first check (`>= 0`, draw nothing) always passed; reads of it now go through `mcc$overrideIndex()` (member-manual `G` rule). A handler that binds a sprite sheet per part while the Tessellator is drawing (Forge 1.4.7 drew the pending vertices with the old texture before binding) now flushes on `bindTexture(String)` while a block is captured, and each captured quad remembers the sheet it was drawn under; `IsbrhQuads` maps its tile-position UVs to the cut tiles of the atlas (vanilla `/terrain.png` goes through the 256-entry table). `ISimpleBlockRenderingHandler.shouldRender3DInInventory()` lost its argument in 1.7 (bridge rule added, 1.6.4 table, so it also helps 1.6.4 handlers): block items now show their 3D icons.
- **GUIs:** images are bound by `getTexture(path)` ids; the id is mapped back to the location (the draw code names textures through the render context), so the alloy furnace and deployer GUIs draw. Container slots and clicks sync (items moved into both machines).
- **Other generic fixes:** boolean `isProvidingWeakPower/StrongPower` bridge (1.4.7 returned a boolean), `Block.getBlockTextureFromSideAndMetadata`, `isDefaultTexture`, `getBlockBrightness`, vanilla blocks' `/terrain.png` index for covers, `EntityPickupFX` (ComputerCraft's turtle client failed verification), `EntityLivingBase.getDropItemId` (reflection by name), dirty marks from the server thread no longer assert in Sodium, `Synth.dispatch` survives a tile class that mentions an absent optional API (a MiscPeripherals tile crashed the world tick on IC2's `ic2.api.Direction`), and the sheet cutter also cuts a mod's own 256x256 `blocks.png`/`items.png` by name (BuildCraft builds those paths at run time).
- **Liquids:** `net.minecraftforge.liquids` resolves to the 1.6.4 shim (fluid capability adapter) through the forward chain; BuildCraft 3.4.6 links; not exercised at run time.
- **Still open:** texture animations (`TextureFXManager`: RedPower's `TextureAnimFX`, water/lava tiles of mods) link but do nothing; AE cables placed through the item path and AE GUIs were not tried; survival use of wires and gates (power, logic) and RedPower world generation untried; the RedPower Mining module is not in the corpus; Forestry/Binnie duplicate genetics API; Better Than Wolves is a modified-game jar mod (see below).
- **What blocks "supported":** animations, a survival session of the RedPower flagship (wire, gate, machine chains; ore generation), GUIs of AE, (the BuildCraft block sheets are cut now and draw). Rating: RedPower works, Entity Detector works, BuildCraft works, AE partial, ComputerCraft partial, CCTV and MiscPeripherals loads.
- **Better Than Wolves 4.65b** (old-only, a modified-game jar mod): the loader translates it (475 classes, 253 synthesized API classes for the vanilla classes it replaces, 3 unresolved references) and discovers `mod_FCBetterThanWolves`, but construction fails with `ClassNotFoundException: mod_FCBetterThanWolves`. It replaces vanilla classes (mobs, blocks, world generation) rather than adding to them, which the shim cannot express; it would need the vanilla-class-replacement semantics of a patched `minecraft.jar` (the replaced classes loaded in place of the game's) or a rewrite of its changes as hooks. Not near working; no time spent beyond this check.
