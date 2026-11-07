# 1.5.2 corpus: what real 1.5.2 mods use

Measured 2026-11-06 with `tools/compat/scan_152.py` (the 1.6.4 scanner with the 1.5.2 differences applied) over 67 Forge 1.5.2 mod files (`test-mods-152/`, gitignored; the list with versions and download
URLs is `test-mods-152/index.md`, next to the files). Companion of [Legacy-1.6.4-Corpus](Legacy-1.6.4-Corpus.md) and [Legacy-1.6.4-Estimate](Legacy-1.6.4-Estimate.md); back to [Home](Home.md).

## Corpus

67 files, about 45 different mods (most mods were taken in their two newest 1.5.2 builds, which matters for loading: see below): BuildCraft 3.7 (A), Forestry 2.2.8, Binnie's Mods (Extra Bees/Trees), Applied
Energistics (rv13, rv14), Mystcraft 0.10, Twilight Forest, Factorization 0.8, ForgeMultipart + CodeChickenCore + NEI + ChickenChunks + Ender Storage + Translocator, ComputerCraft 1.53, Better Than Wolves,
CustomNPCs, Grimoire of Gaia 2, Carpenter's Blocks, BiblioCraft, the Immibis family (Immibis Core, Microblocks, Peripherals, RedLogic, Advanced Machines, Advanced Repulsion Systems, Dimensional Anchor,
InfiniTubes, Liquid XP, Tubestuff), MiscPeripherals, Enchanting Plus, Backpacks, Dragon Mounts, TreeCapitator, the iChun mods (iChunUtil, Hats, Mob Amputation), More Player Models, Cute Mob Models, More Swords, Dirt Tools,
Multishot, Resource Engineering, Sprintable. Many are distributed as **zips** (19 of 67, among them the big ones: AE1, Mystcraft, ComputerCraft, BTW), which FML read from `mods/` and NeoForge does not: the loader picks up zips that contain classes.

Not obtainable (the authors disabled third-party downloads): Railcraft, Natura, Ender IO, Waila, JourneyMap, ExtrabiomesXL, Forge Essentials. No 1.5.2 file on CurseForge or Modrinth: IC2, Thaumcraft, Thermal Expansion,
Equivalent Exchange, RedPower 2, Mo' Creatures, Tinkers' Construct, Biomes O' Plenty, Mekanism, Iron Chests, Pam's, Inventory Tweaks, Ars Magica, MineFactory Reloaded. As with 1.6.4, the tech-pack core (IC2, Thermal
Expansion, Railcraft, RP2) is missing; BuildCraft, Forestry, AE1, Factorization and the Immibis mods cover the same API families.

## What 1.5.2 changes against 1.6.4

Measured differences in the code the mods actually contain:

- **No `EntityLivingBase`.** `EntityLiving` is the base of every living entity, the player included: 43 of 67 jars reference it (567 class files). In 1.7.10 `EntityPlayer` is no `EntityLiving` any more, so a plain
  class rename breaks bytecode verification wherever a player flows into an `EntityLiving` slot (it did in at least six mods on the first server load: Twilight Forest, BuildCraft, More Swords, Carpenter's Blocks, CustomNPCs, Grimoire of Gaia). The loader maps every type use to `EntityLivingBase`, keeps the real `EntityLiving` where a mod entity
  extends it or calls `super`, and casts the receiver where a member only 1.7.10's `EntityLiving` has is used (`Living152ClassRemapper`, `LivingCasts152`).
- **Textures are string paths.** `RenderEngine.bindTexture("/mods/id/textures/x.png")`, `/terrain.png`, `/gui/container.png`, `TextureStitched`, `Texture`, `ITexturePack`: 39 of 67 jars (`RenderEngine` alone: 36 jars, 340
  class files). `RenderEngine` became the shim's texture manager with a string overload that resolves the path (`Textures152`); `/mods/<id>/` files are the mod's own, other paths are vanilla sheets or class path files.
- **Lifecycle:** `@PreInit/@Init/@PostInit` in 58 of 67 jars, **`@Mod.EventHandler` in none of the 1.5.2 ones** (the only ones using it are the two mods that ship for both). So the lifecycle scan of 1.6.4 is inverted.
- **Forge classes in their 1.5 places:** `net.minecraftforge.common.ISidedInventory` (16 jars; a start index and size per side, 1.7.10 lists the slots), `net.minecraftforge.liquids.*` (the liquid API: 21 jars use `LiquidStack`,
  also in 1.6.4), `IThrowableEntity`, `Configuration$UnicodeInputStreamReader`.
- **No attributes, no horses, no `ResourceLocation`, no resource packs** (all 1.6): not referenced.
- **Notch names:** 18 of 67 jars (26%) name Minecraft by obfuscated names (BTW, ComputerCraft, CodeChickenCore, NEI, ChickenChunks, TreeCapitator...); 1,100 distinct short names. `legacy/1.5.2/notch.txt` (18,114 rows) resolves them.
- **Unchanged from 1.6.4** (the forward layer is shared): numeric ids in 52 of 67 jars (77%), the old network API (`NetworkMod`, `IPacketHandler`, `Packet250CustomPayload`) in 54 (80%), tick handlers in 38 jars, `AABBPool`
  and `Vec3Pool` in 29 and 22.

## Headline

- Of the 832 distinct Minecraft/Forge classes the 49 readable jars reference, 684 exist in the shim unchanged, 67 are renames or package moves, 33 are removed APIs (network, tick, player tracker, key bindings, pools,
  texture packs), 8 moved package, 40 are unaccounted for (150 class-file references; `LiquidContainerData` is the only one in more than 10 jars).
- By class-file references: 43,840 present, 6,193 renamed, 1,759 removed, 253 moved, 150 missing, 40,105 obfuscated.
- The biggest single items, in order: the numeric-id facade (shared with 1.6.4), the old network/tick adapters (shared), the string-path textures, the `EntityLiving` split, the liquid API (`LiquidStack` and friends: no
  equivalent behaviour in the shim yet; 21 jars), and the zip distribution of the mods.

## Link check (`checkLegacyMod` over the corpus, offline)

| | Unresolved references | Synthesized classes | Synthesized members |
|---|---:|---:|---:|
| First run (groundwork only) | 26 | 561 | 3,251 |
| After the forward tables, `EntityLiving` split, string textures (second run) | 21 | 408 | 2,558 |

(Totals over the 56 jars/zips that are legacy mods, 13,741 classes; the unresolved ones are almost all CodeChickenCore/NEI library calls and Scala reflection. "Synthesized" members link but do nothing: the
number to push down.) Regenerate: `python tools/compat/scan_152.py <dir> --md out.md`; link check: `./gradlew checkLegacyMod -PlegacyJar=<comma separated jars>`.

## Server load

The corpus was loaded on a dedicated server in one batch (the newest build of each mod, 51 mods discovered; two builds of one mod id make FML skip both, which is a corpus artefact, not a defect).
Results and the remaining failures are in the [Estimate](Legacy-1.5.2-Estimate.md) page under "Measured"; the link check after the first day: 1,801 synthesized members (3,251 before), 226 synthesized classes (561), 21 unresolved (26).

## Numbers from the scanner

Distinct referenced classes per bucket (class-file references): present 684 (43,840), renamed 67 (6,193), removed 33 (1,759), moved 8 (253), missing 40 (150), obfuscated 1,100 (40,105).

Most used removed or renamed classes (jars): `NetworkMod` 53, `Packet` 40, `TickType` 38, `TickRegistry` 34, `ITickHandler` 33, `AABBPool` 29, `Player` 27, `IPacketHandler` 26, `Packet250CustomPayload` 23, `Vec3Pool` 22;
`ForgeSubscribe` 43, `Icon` 42, `IconRegister` 40, `ForgeDirection` 39, `RenderEngine` 36, `Property`/`Configuration` 36, `INetworkManager` 29, `Packet132TileEntityData` 26, `StepSound` 24, `NetServerHandler` 22,
`LiquidStack`/`ILiquidTank`/`ITankContainer` 21.

Numeric-id API (references, jars): `itemID` 1,076 (43), `blockID` 909 (38), `World.getBlockId` 580 (40), `Block.blocksList` 422 (40), `World.setBlock(int ids)` 393 (37), `new ItemStack(int ...)` 349 (32),
`new Item(int ...)` 162 (34), `Item.itemsList` 149 (28), `new BlockContainer(int ...)` 102 (25), `new PotionEffect(int ...)` 91 (10), `biomeID` 57 (10).
