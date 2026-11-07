# 1.4.7 corpus: what real 1.4.7 mods use

Measured 2026-11-06 with `tools/compat/scan_147.py` and `./gradlew checkLegacyMod` over 65 Forge 1.4.7 mod files (`test-mods-147/`, gitignored; the list with versions and
download URLs is `test-mods-147/index.md`, next to the jars). Companion of [Legacy-1.4.7-Estimate](Legacy-1.4.7-Estimate.md); the older siblings are
[Legacy-1.6.4-Corpus](Legacy-1.6.4-Corpus.md) and [Legacy-1.6.4-Estimate](Legacy-1.6.4-Estimate.md). Back to [Home](Home.md).

## Corpus

65 files (43 `.jar`, 22 `.zip`): BuildCraft 3.4.6, Forestry 2.0.0.11 + Extra Bees, Twilight Forest 1.15.4, Mystcraft 0.10.1, RedPower 2 (Core, Mechanical, Digital, Compat; 2.0pr6),
Applied Energistics rv9, ComputerCraft 1.5 + MiscPeripherals + Immibis' Peripherals, Factorization 0.7.21, Better Than Wolves 4 (65b), Custom NPCs 1.4.7, BiblioCraft 1.1.2, Grimoire of Gaia 2,
the Immibis family (Core, Microblocks, Advanced Repulsion Systems, Dimensional Anchor, InfiniTubes, Liquid XP, TubeStuff), the iChun mods (Hats, Portal Gun, Gravity Gun, Mob Amputation),
CodeChickenCore 0.8.1.6, NEI 1.4.7.4, ChickenChunks 1.3.1.2 and Ender Storage 1.4.1.5 (those three are tagged 1.4.6 on CurseForge), Dragon Mounts, bspkrsCore, TreeCapitator, BlockHelper, Custom Player Models, CraftPresence,
UniLib, ModLoader-style mods (SprintMod, Mob Amputation, Cute Mob Models, mod_cctv, mod_EntityDetector, ...) and a tail of small utilities.

Sources: Modrinth (version filter 1.4.7; Modrinth is much better stocked than CurseForge for this version) and CurseForge via cfwidget + forgecdn. CurseForge's API only lists the newest files
of a project, so most 1.4.7 builds of the big mods are unreachable that way. **Not obtainable** (no 1.4.7 file listed anywhere we could reach, or CurseForge 403): IC2, Railcraft, Thermal Expansion,
Thaumcraft 3, Equivalent Exchange 2/3, Natura, Tinkers' Construct, Mo' Creatures, Biomes O' Plenty, Pam's HarvestCraft, Iron Chests, Flans, Forge Multipart, MineFactory Reloaded, Ars Magica, Galacticraft,
GregTech. Hexxit and Tekkit Lite are launcher packs, not mods; their content is the mods above plus those missing ones, so the corpus is thin on the tech-pack core (IC2, Railcraft, TE) and on Thaumcraft.
About 30 more files that Modrinth or CurseForge tag 1.4.7 turned out to be modern mods with a wrong tag, multi-platform jars without 1.4.7 code (CC: Tweaked, Pylon, Ears...) or empty shells; they were dropped,
as were about 30 older builds of mods that have two (the newest build is kept).

## Method and limits

`scan_147.py` parses every class (constant pool: classes, member references with owners, annotations, string constants; zips nested in zips included) and resolves each reference to the
MCP class first: a default-package short name (`yc`, `amq`) through `legacy/1.4.7/notch.txt` (written from the 1.4.7 client jar and Forge 6.6.2.534's `joined.srg`), a flat `net/minecraft/src/X` through
`forward/1.4.7/flat-names.tsv`, then through the 1.6.4 forward renames. Buckets: **present** (the shim has the class after the move into the 1.5.2 package), **renamed** (a forward rename was needed as well),
**synth** (not in the shim, but the 1.7.10 hierarchy knows it so the loader can synthesize it), **removed** (an API 1.7.10 dropped or never had), **modloader** (`BaseMod`, `ModLoader`...),
**unknown**. Members are resolved the same way (notch member name to SRG to MCP name through the 1.4.7 `methods.csv`/`fields.csv`) for the numeric-id and texture tables. Limits: "present"
says a class exists, not every method; "unresolved" in `checkLegacyMod` is low because the loader synthesizes missing classes, so the *synthesized members* count is the honest gap measure.

## Headline

Share of the 65 jars that use each API (`scan_147.py`):

- notch (obfuscated) vanilla names: 63 of 65 jars (96%)
- flat net.minecraft.src vanilla names: 0 of 65 jars (0%)
- ModLoader style (`extends BaseMod`): 6 of 65 jars (9%)
- uses the ModLoader class (alone or next to `@Mod`): 18 of 65 jars (27%)
- `@Mod` (FML): 56 of 65 jars (86%)
- both `@Mod` and `extends BaseMod`: 0 of 65 jars (0%)
- only ModLoader (`extends BaseMod`, no `@Mod`): 6 of 65 jars (9%)
- `@NetworkMod`: 43 of 65 jars (66%)
- `@PreInit/@Init/@PostInit`: 53 of 65 jars (81%)
- `@ForgeSubscribe`: 35 of 65 jars (53%)
- numeric ids: 51 of 65 jars (78%)
- Forge texture-file API (setTextureFile/getTextureFile/preloadTexture): 41 of 65 jars (63%)
- block/item index fields (blockIndexInTexture/iconIndex/setIconIndex): 22 of 65 jars (33%)
- texture FX / ModLoader.addOverride: 3 of 65 jars (4%)
- `terrain.png` or `gui/items.png` sheet strings: 19 of 65 jars (29%)
- `/mods/<id>/` texture paths: 3 of 65 jars (4%)

- **Obfuscated names are the norm.** 63 of 65 jars name Minecraft by its notch names (`yc`, `amq`, `ur`; members `a`, `b`...), none by the flat `net.minecraft.src.<Name>` that MCP 6.x sources used. FML deobfuscated them at load time,
  so the 1.4.7 jar does not carry MCP class names at all. (The two exceptions, bufferfix and Old Vein Miner, were never reobfuscated: they name Minecraft by the packaged MCP 7 names.) Version detection therefore cannot use class names like 1.5.2/1.6.4 do; it uses a
  **fingerprint**: the share of a jar's obfuscated member references that resolve in each version's `notch.txt` (same owner, name and descriptor). The obfuscation is re-randomized per Minecraft release, so the right version scores
  90 to 100% and the others 0 to 50% (see the Detection section).
- **Thirteen jars (20%) also carry packaged names** (`net/minecraft/world/World`, `net/minecraft/entity/player/EntityPlayer`) in the descriptors of methods that implement Forge/FML API (`IGuiHandler`, `ISidedInventory`, `IPacketHandler`): the
  last 1.4.7 builds of Forge (6.6.2.5xx, source zip dated Feb 2013) are already MCP 7 / packaged, so mods compiled against them mix notch names with those. The remapper treats both spellings alike (flat and packaged converge on the
  1.5.2 package names).
- **Class surface.** 743 distinct referenced classes exist in the shim once the flat name has moved into its 1.5.2 package, 47 more through a 1.6.4 forward rename, 127 are synthesizable (in the 1.7.10 jar but not the shim), 228 are not in 1.7.10 at all
  (the old tick/network/player-tracker/key-binding families, `AABBPool`/`Vec3Pool`, `liquids`, `RenderEngine`, texture FX, `SoundPool`; the list below).
- **Numeric ids: 51 of 65 jars (78%)**, the same shape as 1.6.4 (72% there): `itemID`/`blockID` fields, `Block.blocksList`, `World.getBlockId`/int `setBlock`, `new Block(int, Material)`/`new Item(int)`/`new ItemStack(int, ...)`. The 1.6.4 id facade is reused as it is (1.4.7 shares it,
  `LegacyVersion.shimPrefix`).
- **Old lifecycle annotations are the norm here: 53 of 65 jars (81%) use `@PreInit`/`@Init`/`@PostInit`** (1.6.4: 1 of 40), none uses `@EventHandler` (it does not exist yet). `@NetworkMod` 43 jars (66%), `ITickHandler`/`TickRegistry` 27/28, `IPacketHandler` 20,
  `PacketDispatcher` 14, `IConnectionHandler` 11, `IPlayerTracker` 8: the same adapters as 1.6.4, shared.
- **ModLoader is a real fraction.** 18 jars (27%) use the `ModLoader` class (alone or next to `@Mod`), 6 are pure ModLoader mods (`mod_X extends BaseMod`, no `@Mod`, e.g. Better Than Wolves, SprintMod, Cute Mob Models, Mob Amputation). In a reobfuscated jar `BaseMod`/`ModLoader`/`MLProp`
  are *default-package* classes. The layer had no `BaseMod` at all: the inspector now finds these mods (each `BaseMod` subclass is a mod, `LegacyModDecl.modLoader`), but the emulation is not written.
- **Zip files are 34% of the corpus.** 1.4.x mods (ModLoader ones above all, plus RedPower, AE, Mystcraft, ComputerCraft, Factorization-era) were shipped as `.zip` to be dropped into `mods/`; the locator accepts `.zip` in the `mods/1.4.7/` folder now.
- **Not translatable standalone:** Better Than Wolves needs its own patched vanilla (475 classes, 91% notch references including replaced vanilla internals); it stays out of scope like every jar-mod. NEI is bridged to JEI by project rule,
  CodeChickenCore is a coremod (see [Estimate](Legacy-1.4.7-Estimate.md)).

## Sprite sheets: the big difference from 1.5.2

1.4.7 has **no `Icon`s**. A block or item names its texture as an integer index into a 16x16 grid of 16x16-pixel tiles (`terrain.png`, `gui/items.png`) or into a sheet of its own chosen with Forge's `setTextureFile("/gfx/mod/blocks.png")`
or `ITextureProvider`/`getTextureFile()`; the per-side lookup is `getBlockTextureFromSideAndMetadata(side, meta)` returning an **index**, items use `setIconIndex(int)`/`getIconFromDamage(int)` returning an index, and the renderer binds the sheet with
`ForgeHooksClient.bindTexture(file, 0)` / `RenderEngine.getTexture(String)` + `bindTexture`. Mod sheets must be registered with `MinecraftForgeClient.preloadTexture(String)`. Measured in the corpus:

| Reference | Count | Jars |
|---|---:|---:|
| getTextureFile | 551 | 31 |
| RenderEngine.bindTexture | 267 | 36 |
| RenderEngine.getTexture | 241 | 35 |
| setTextureFile | 136 | 21 |
| MinecraftForgeClient.preloadTexture | 76 | 27 |
| getBlockTextureFromSideAndMetadata | 46 | 13 |
| getIconFromDamage | 38 | 9 |
| blockIndexInTexture (field) | 19 | 9 |
| getIconIndex | 19 | 10 |
| Item.setIconIndex | 17 | 12 |
| getBlockTextureFromSide | 14 | 5 |
| registerTextureFX | 9 | 3 |
| getBlockTexture(world,x,y,z,side) | 8 | 4 |
| iconIndex (field) | 5 | 1 |

| Style | Strings | Jars |
|---|---:|---:|
| .png string | 1117 | 48 |
| /gfx/ path | 232 | 4 |
| /mob/ path | 97 | 7 |
| /gui/ path | 72 | 12 |
| /armor/ path | 40 | 5 |
| terrain.png sheet | 39 | 17 |
| gui/items.png sheet | 18 | 4 |
| /misc/ path | 15 | 6 |
| /mods/<id>/ path | 6 | 3 |
| /terrain/ path | 4 | 2 |
| /item/ path | 4 | 4 |

- 42 of 65 jars reference at least one square 256x256 (or larger) PNG that exists in the jar, 255 such sheets in total (of 520 referenced PNGs that exist; the others are mob skins 64x32/128x64, GUIs and armor layers). Forestry alone has 58, Custom NPCs 25, RedPower Mechanical 21.
- The 16-pixel grid is the rule (256x256 sheets: 248 of the referenced PNGs), some mods use 512x512 sheets (6) for 32-pixel tiles.
- Only 3 jars (4%) register `TextureFX` animations (`registerTextureFX`, `FMLTextureFX`); `/mods/<id>/` style paths are rare (3 jars), `/gfx/<mod>/...` is the common convention (4 jars, 232 strings, BuildCraft first).
- `ModLoader.addOverride("/terrain.png", ...)` / `getUniqueSpriteIndex` (ModLoader mods claiming free slots in the vanilla sheet) is used by the ModLoader-only jars.

What it takes is in the [Estimate](Legacy-1.4.7-Estimate.md#sprite-sheets).

## Detection (checkLegacyMod)

`./gradlew checkLegacyMod -PlegacyJar=@list.txt` over the 65 jars: **64 detect as 1.4.7**, 1 does not (hopper-1.0.jar: 18 tiny classes, no version evidence). By evidence: 57 by the notch fingerprint (including BuildCraft, Forestry, Twilight Forest,
Mystcraft, Extra Bees, RedPower, AE), 4 by a declared 1.4.x version, 2 by the file name (PonySocks147, SprintingKey-MC1.4), 1 by the ModLoader fallback (SprintMod: a one-class BaseMod). The 1.4.7-only FML classes
(`FMLTextureFX`, `SpriteHelper`) are a second signal that agrees with the fingerprint for the 5 jars that use them. The existing detection is unchanged: the 1.5.2 corpus (67 files), the 1.6.4 corpus (47 files) and 48 1.7.10/1.12.2 jars get
exactly the same version with the 1.4.7 rule switched off and on (checked file by file; none flips to 1.4.7). Over the 64 jars: 13,006 classes, 71 unresolved references (all in CraftPresence, UniLib, Better Than Wolves and CameraCraft: libraries bundled in multi-platform jars),
613 synthesized API classes and 4,320 synthesized members (3,573 distinct member demands, 518 of them constructors).

Top synthesized members (the M1 work list): `World.getBlockId/setBlockWithNotify/setBlockAndMetadataWithNotify` (the int-id family, the 1.6.4 facade has it), `GameRegistry.registerBlock` (int-era overloads), `Item.setItemName`, `Item.setIconIndex`,
`Item.setTextureFile`/`Block.getBlockTextureFromSideAndMetadata` (sprite sheets), `MinecraftForgeClient.preloadTexture`, `AxisAlignedBB.getAABBPool` (20 jars), `Minecraft.renderEngine` (32 refs), `Configuration.getBlock/getItem`, `TileEntity.a`/`BlockContainer.a`
(199 obfuscated member names, 5%, that the notch table did not resolve; mostly overrides declared in a mod subclass and called through a vanilla owner, a lookup gap in `NotchNames` for 1.4.7 to close in M1).

## Numbers

Regenerate: `python tools/compat/scan_147.py test-mods-147 --md out.md [--mc 1.5.2]` (needs `tools/fetch_devtools.sh` for the SRG to MCP member names).

### Distinct referenced classes per bucket

| Bucket | Distinct classes | Class-file references |
|---|---:|---:|
| present | 743 | 35545 |
| renamed | 47 | 2000 |
| synth | 127 | 298 |
| removed | 228 | 2212 |
| modloader | 5 | 85 |
| unknown | 0 | 0 |
| obfuscated? | 10 | 721 |

### Numeric-id API references

| Reference | Count | Jars |
|---|---:|---:|
| itemID | 1087 | 45 |
| blockID | 857 | 37 |
| World.getBlockId | 729 | 38 |
| World.setBlock(int ids) | 573 | 34 |
| Block.blocksList | 404 | 36 |
| new ItemStack(int ...) | 311 | 32 |
| new Item(int ...) | 167 | 29 |
| World.setBlockMetadata(int ids) | 151 | 21 |
| Item.itemsList | 135 | 25 |
| new Block(int ...) | 134 | 19 |
| World.getBlockMaterial(x,y,z) | 100 | 11 |
| effectId | 54 | 7 |
| biomeID | 37 | 6 |
| World.isBlockOpaqueCube(x,y,z) | 12 | 5 |
| BiomeGenBase.biomeList | 12 | 7 |
| Enchantment.enchantmentsList | 11 | 5 |
| new Enchantment(int ...) | 4 | 2 |
| Potion.potionTypes | 3 | 3 |
| new Potion(int ...) | 2 | 1 |
| new BiomeGenBase(int ...) | 1 | 1 |

### Top 30 removed or not-in-1.7.10 classes by number of jars using them

| Class | Jars | Class files | Hint |
|---|---:|---:|---|
| `cpw/mods/fml/common/network/NetworkMod` | 43 | 58 | removed (annotation) |
| `net/minecraft/src/RenderEngine` | 37 | 285 | not in 1.7.10 |
| `cpw/mods/fml/common/TickType` | 29 | 61 | removed (tick events in 1.7.10) |
| `cpw/mods/fml/common/registry/TickRegistry` | 28 | 44 | removed (tick events in 1.7.10) |
| `cpw/mods/fml/common/ITickHandler` | 27 | 80 | removed (tick events in 1.7.10) |
| `cpw/mods/fml/common/network/Player` | 21 | 95 | removed (EntityPlayer) |
| `net/minecraft/src/AABBPool` | 20 | 153 | not in 1.7.10 |
| `cpw/mods/fml/common/network/IPacketHandler` | 20 | 34 | removed (SimpleNetworkWrapper / FMLEventChannel) |
| `net/minecraftforge/common/ISidedInventory` | 18 | 64 | not in 1.7.10 |
| `cpw/mods/fml/common/network/PacketDispatcher` | 14 | 76 | removed |
| `net/minecraftforge/liquids/LiquidStack` | 13 | 198 | net/minecraftforge/fluids/FluidStack |
| `net/minecraft/src/Vec3Pool` | 12 | 81 | not in 1.7.10 |
| `net/minecraftforge/liquids/ILiquidTank` | 12 | 66 | net/minecraftforge/fluids/IFluidTank |
| `net/minecraftforge/liquids/ITankContainer` | 12 | 51 | net/minecraftforge/fluids/IFluidHandler |
| `net/minecraft/src/TextureFX` | 11 | 21 | not in 1.7.10 |
| `cpw/mods/fml/common/network/IConnectionHandler` | 11 | 14 | removed (FMLNetworkEvent) |
| `cpw/mods/fml/relauncher/IClassTransformer` | 10 | 16 | not in 1.7.10 |
| `cpw/mods/fml/client/TextureFXManager` | 10 | 11 | not in 1.7.10 |
| `net/minecraftforge/liquids/LiquidContainerRegistry` | 9 | 19 | net/minecraftforge/fluids/FluidContainerRegistry |
| `net/minecraftforge/liquids/LiquidDictionary` | 8 | 17 | net/minecraftforge/fluids/FluidRegistry |
| `cpw/mods/fml/common/IPlayerTracker` | 8 | 16 | removed (PlayerEvent in 1.7.10) |
| `cpw/mods/fml/client/FMLTextureFX` | 8 | 13 | not in 1.7.10 |
| `net/minecraftforge/common/IArmorTextureProvider` | 8 | 13 | not in 1.7.10 |
| `net/minecraft/src/TexturePackList` | 8 | 11 | not in 1.7.10 |
| `net/minecraftforge/liquids/LiquidContainerData` | 7 | 30 | not in 1.7.10 |
| `net/minecraft/src/ITexturePack` | 7 | 11 | not in 1.7.10 |
| `net/minecraft/src/SoundPool` | 7 | 8 | not in 1.7.10 |
| `cpw/mods/fml/common/ICraftingHandler` | 6 | 13 | removed (PlayerEvent.ItemCraftedEvent) |
| `cpw/mods/fml/common/network/ITinyPacketHandler` | 6 | 8 | removed |
| `cpw/mods/fml/common/network/NetworkMod$SidedPacketHandler` | 6 | 6 | removed (annotation) |

### Top 30 synth classes by number of jars using them

| Class | Jars | Class files | Hint |
|---|---:|---:|---|
| `net/minecraft/src/SoundPoolEntry` | 7 | 8 | net/minecraft/client/audio/SoundPoolEntry |
| `net/minecraft/src/ItemFlintAndSteel` | 3 | 6 | net/minecraft/item/ItemFlintAndSteel |
| `net/minecraft/src/BlockRedstoneOre` | 3 | 5 | net/minecraft/block/BlockRedstoneOre |
| `net/minecraft/src/GuiChest` | 3 | 3 | net/minecraft/client/gui/inventory/GuiChest |
| `net/minecraft/src/BlockStoneBrick` | 2 | 5 | net/minecraft/block/BlockStoneBrick |
| `net/minecraft/src/ItemGlassBottle` | 2 | 5 | net/minecraft/item/ItemGlassBottle |
| `net/minecraft/src/ItemWritableBook` | 2 | 5 | net/minecraft/item/ItemWritableBook |
| `net/minecraftforge/client/IRenderContextHandler` | 2 | 4 | net/minecraftforge/client/IRenderContextHandler |
| `net/minecraft/src/BlockNetherStalk` | 2 | 3 | net/minecraft/block/BlockNetherWart |
| `net/minecraft/src/ItemSign` | 2 | 3 | net/minecraft/item/ItemSign |
| `net/minecraft/src/ItemBoat` | 2 | 3 | net/minecraft/item/ItemBoat |
| `net/minecraft/src/ItemHangingEntity` | 2 | 3 | net/minecraft/item/ItemHangingEntity |
| `net/minecraft/src/EntityPickupFX` | 2 | 3 | net/minecraft/client/particle/EntityPickupFX |
| `net/minecraft/src/GuiFurnace` | 2 | 3 | net/minecraft/client/gui/inventory/GuiFurnace |
| `net/minecraft/src/GuiBrewingStand` | 2 | 3 | net/minecraft/client/gui/inventory/GuiBrewingStand |
| `net/minecraft/src/ChunkProviderClient` | 2 | 2 | net/minecraft/client/multiplayer/ChunkProviderClient |
| `net/minecraft/src/GuiScreenBook` | 2 | 2 | net/minecraft/client/gui/GuiScreenBook |
| `net/minecraft/client/ClientBrandRetriever` | 2 | 2 | net/minecraft/client/ClientBrandRetriever |
| `net/minecraft/src/BiomeGenHell` | 1 | 8 | net/minecraft/world/biome/BiomeGenHell |
| `net/minecraft/src/BlockSilverfish` | 1 | 6 | net/minecraft/block/BlockSilverfish |
| `net/minecraft/src/EntityEnderEye` | 1 | 6 | net/minecraft/entity/item/EntityEnderEye |
| `net/minecraft/src/VillageSiege` | 1 | 6 | net/minecraft/village/VillageSiege |
| `net/minecraft/src/ContainerMerchant` | 1 | 6 | net/minecraft/inventory/ContainerMerchant |
| `net/minecraft/src/BlockRedstoneTorch` | 1 | 4 | net/minecraft/block/BlockRedstoneTorch |
| `net/minecraft/src/CommandTime` | 1 | 4 | net/minecraft/command/CommandTime |
| `net/minecraft/src/ItemBucketMilk` | 1 | 4 | net/minecraft/item/ItemBucketMilk |
| `net/minecraft/src/BanList` | 1 | 4 | net/minecraft/server/management/BanList |
| `net/minecraft/src/ContainerBrewingStand` | 1 | 4 | net/minecraft/inventory/ContainerBrewingStand |
| `net/minecraft/src/InventoryMerchant` | 1 | 4 | net/minecraft/inventory/InventoryMerchant |
| `net/minecraft/src/ItemRedstone` | 1 | 4 | net/minecraft/item/ItemRedstone |

### Top 30 renamed classes by number of jars using them

| Class | Jars | Class files | Hint |
|---|---:|---:|---|
| `net/minecraft/src/Packet` | 38 | 344 | net/minecraft/network/Packet |
| `net/minecraftforge/event/EventBus` | 38 | 76 | cpw/mods/fml/common/eventhandler/EventBus |
| `net/minecraftforge/event/ForgeSubscribe` | 35 | 62 | cpw/mods/fml/common/eventhandler/SubscribeEvent |
| `net/minecraftforge/common/Property` | 33 | 91 | net/minecraftforge/common/config/Property |
| `net/minecraftforge/common/Configuration` | 33 | 69 | net/minecraftforge/common/config/Configuration |
| `net/minecraftforge/common/ForgeDirection` | 28 | 468 | net/minecraftforge/common/util/ForgeDirection |
| `net/minecraft/src/StepSound` | 23 | 225 | net/minecraft/block/Block$SoundType |
| `net/minecraft/src/NetServerHandler` | 21 | 70 | net/minecraft/network/NetHandlerPlayServer |
| `net/minecraft/src/NetClientHandler` | 20 | 39 | net/minecraft/client/network/NetHandlerPlayClient |
| `net/minecraft/src/NetHandler` | 18 | 71 | net/minecraft/network/INetHandler |
| `net/minecraft/src/EnumMovingObjectType` | 15 | 39 | net/minecraft/util/MovingObjectPosition$MovingObjectType |
| `net/minecraftforge/event/Event` | 14 | 36 | cpw/mods/fml/common/eventhandler/Event |
| `net/minecraft/src/BlockHalfSlab` | 13 | 55 | net/minecraft/block/BlockSlab |
| `net/minecraft/src/EnumArmorMaterial` | 11 | 43 | net/minecraft/item/ItemArmor$ArmorMaterial |
| `net/minecraft/src/EnumToolMaterial` | 9 | 103 | net/minecraft/item/Item$ToolMaterial |
| `net/minecraft/src/EnumGameType` | 9 | 21 | net/minecraft/world/WorldSettings$GameType |
| `net/minecraftforge/common/ConfigCategory` | 9 | 11 | net/minecraftforge/common/config/ConfigCategory |
| `net/minecraft/src/ItemInWorldManager` | 8 | 17 | net/minecraft/server/management/ItemInWorldManager |
| `net/minecraftforge/event/Event$Result` | 8 | 10 | cpw/mods/fml/common/eventhandler/Event$Result |
| `net/minecraftforge/common/EnumHelper` | 8 | 8 | net/minecraftforge/common/util/EnumHelper |
| `net/minecraft/src/EntityFallingSand` | 6 | 25 | net/minecraft/entity/item/EntityFallingBlock |
| `net/minecraft/src/TileEntityRenderer` | 6 | 14 | net/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher |
| `net/minecraft/src/SpawnListEntry` | 4 | 13 | net/minecraft/world/biome/BiomeGenBase$SpawnListEntry |
| `net/minecraft/src/BlockFluid` | 4 | 13 | net/minecraft/block/BlockLiquid |
| `net/minecraftforge/event/Cancelable` | 4 | 6 | cpw/mods/fml/common/eventhandler/Cancelable |
| `net/minecraft/src/EntityEggInfo` | 4 | 5 | net/minecraft/entity/EntityList$EntityEggInfo |
| `net/minecraft/src/PlayerInstance` | 4 | 4 | net/minecraft/server/management/PlayerManager$PlayerInstance |
| `net/minecraft/src/BlockStationary` | 3 | 5 | net/minecraft/block/BlockStaticLiquid |
| `net/minecraft/src/IRegistry` | 3 | 4 | net/minecraft/util/IRegistry |
| `net/minecraft/src/BlockTripWireSource` | 2 | 7 | net/minecraft/block/BlockTripWireHook |

## Per jar

(`naming`: notch = obfuscated names; counts are distinct referenced classes per bucket)

| Jar | Classes | naming | present | renamed | synth | removed | modloader | unknown | numeric-id refs | texture refs | old net classes |
|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| BTWMod4-65b.zip | 884 | notch | 497 | 28 | 114 | 177 | 3 | 0 | 1936 | 239 | Packet250CustomPayload |
| BiblioCraft[v1.1.2].zip | 108 | notch | 87 | 8 | 3 | 5 | 0 | 0 | 22 | 36 | IGuiHandler, NetworkMod, NetworkRegistry |
| BlockHelper-1.4.7-2.0.0-pre14.jar | 224 | notch | 101 | 15 | 2 | 15 | 2 | 0 | 31 | 19 | IConnectionHandler, IPacketHandler, ITickHandler, NetworkMod, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| CameraCraft-Mod-1.4.6.zip | 110 | notch | 103 | 10 | 2 | 20 | 0 | 0 | 38 | 14 | IConnectionHandler, IGuiHandler, IPacketHandler, ITickHandler, NetworkMod, NetworkRegistry, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| ChickenChunks 1.3.1.2.jar | 47 | notch | 75 | 8 | 0 | 6 | 0 | 0 | 3 | 5 | IPlayerTracker, ITickHandler, NetworkMod, TickRegistry |
| CodeChickenCore 0.8.1.6.jar | 148 | notch | 116 | 10 | 0 | 22 | 0 | 0 | 31 | 9 | IPacketHandler, ITickHandler, NetworkRegistry, Packet250CustomPayload, Player, TickRegistry |
| ComputerCraft1.5.zip | 403 | notch | 112 | 6 | 4 | 13 | 2 | 0 | 32 | 26 | IGuiHandler, IPacketHandler, ITickHandler, NetworkMod, NetworkRegistry, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| CraftPresence-2.7.1+1.4.7-forge.jar | 672 | notch | 31 | 2 | 2 | 0 | 0 | 0 | 3 | 0 | - |
| CustomNPCs_1.4.7.zip | 300 | notch | 136 | 10 | 2 | 19 | 1 | 0 | 50 | 58 | IGuiHandler, IPacketHandler, ITickHandler, NetworkMod, NetworkRegistry, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| CustomPlayerModels-1.4.7-0.6.27a.jar | 3689 | notch | 114 | 8 | 1 | 9 | 0 | 0 | 8 | 3 | IConnectionHandler, ITickHandler, NetworkRegistry, Packet250CustomPayload, Player, TickRegistry |
| Drogtor-1.4-1.0.jar | 3 | notch | 17 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | - |
| EnderStorage 1.4.1.5.jar | 43 | notch | 71 | 9 | 0 | 9 | 0 | 0 | 14 | 5 | IPlayerTracker, ITickHandler, NetworkMod, TickRegistry |
| ExtraButtons-1.4.7.0.zip | 10 | notch | 35 | 7 | 0 | 2 | 0 | 0 | 8 | 6 | NetworkMod |
| Factorization-0.7.21.jar | 327 | notch | 182 | 21 | 1 | 29 | 0 | 0 | 73 | 66 | IConnectionHandler, IGuiHandler, IPacketHandler, ITickHandler, NetworkMod, NetworkRegistry, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| FontFixer-1.4.7-2.zip | 1 | notch | 5 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | - |
| GravityGun1.4.7v2.zip | 22 | notch | 105 | 10 | 1 | 16 | 0 | 0 | 22 | 4 | IConnectionHandler, IPacketHandler, IPlayerTracker, ITickHandler, NetworkMod, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| Grimoire-of-Gaia-2-Mod-1.4.7.zip | 160 | notch | 106 | 1 | 0 | 3 | 0 | 0 | 145 | 12 | NetworkMod |
| Hats1.2.0MC147.zip | 22 | notch | 71 | 8 | 0 | 14 | 0 | 0 | 0 | 2 | IConnectionHandler, IPacketHandler, IPlayerTracker, ITickHandler, NetworkMod, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| LegacyCustomPublish-1.4-1.0.jar | 2 | notch | 12 | 1 | 1 | 3 | 0 | 0 | 0 | 0 | - |
| MobAmputation1.4.6v1.zip | 11 | notch | 41 | 2 | 0 | 2 | 3 | 0 | 1 | 1 | - |
| More_Swords_Mod_2.3.5_For_1.4.7.jar | 44 | notch | 48 | 8 | 1 | 2 | 0 | 0 | 7 | 18 | NetworkMod |
| NotEnoughItems 1.4.7.4.jar | 177 | notch | 144 | 9 | 3 | 10 | 0 | 0 | 47 | 14 | IPlayerTracker, ITickHandler, TickRegistry |
| PonySocks147.jar | 7 | notch | 20 | 4 | 0 | 2 | 0 | 0 | 2 | 5 | NetworkMod |
| PortalGun1.4.7v5.zip | 78 | notch | 185 | 18 | 2 | 20 | 1 | 0 | 121 | 25 | IConnectionHandler, IPacketHandler, IPlayerTracker, ITickHandler, NetworkMod, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| RedPowerCompat-2.0pr6.zip | 82 | notch | 39 | 3 | 0 | 2 | 0 | 0 | 46 | 6 | IGuiHandler, NetworkMod, NetworkRegistry |
| RedPowerCore-2.0pr6.zip | 155 | notch | 98 | 10 | 0 | 11 | 0 | 0 | 80 | 33 | IGuiHandler, NetworkMod, NetworkRegistry |
| RedPowerDigital-2.0pr6.zip | 54 | notch | 43 | 3 | 1 | 2 | 0 | 0 | 13 | 11 | IGuiHandler, NetworkMod, NetworkRegistry |
| RedPowerMechanical-2.0pr6.zip | 204 | notch | 106 | 9 | 4 | 6 | 0 | 0 | 103 | 72 | IGuiHandler, NetworkMod, NetworkRegistry |
| ResourceEngineering_0.0.104.zip | 52 | notch | 62 | 11 | 0 | 7 | 0 | 0 | 78 | 31 | IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, Packet250CustomPayload, Player |
| SprintMod.jar | 1 | notch | 2 | 0 | 0 | 0 | 2 | 0 | 0 | 0 | - |
| SprintingKey-1.0-MC1.4.jar | 1 | notch | 8 | 0 | 0 | 3 | 0 | 0 | 0 | 0 | ITickHandler, TickRegistry |
| UniLib-1.2.1+1.4.7-forge.jar | 1016 | notch | 41 | 0 | 1 | 1 | 0 | 0 | 0 | 1 | - |
| XInputMod.jar | 53 | notch | 39 | 2 | 0 | 4 | 0 | 0 | 2 | 0 | ITickHandler, TickRegistry |
| [1.4.6]TreeCapitator.Forge.1.4.6.r07.Uni.CoreMod.jar | 18 | notch | 51 | 9 | 0 | 10 | 1 | 0 | 13 | 0 | IConnectionHandler, IPacketHandler, ITickHandler, NetworkMod, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| [1.4.7] Dirt Tools and Armor 24.07.19 (Forge).jar | 12 | notch | 18 | 5 | 0 | 1 | 0 | 0 | 0 | 3 | - |
| [1.4.7] Legendary Item 24.10.09 (Forge).jar | 13 | notch | 19 | 4 | 0 | 1 | 0 | 0 | 2 | 5 | - |
| [1.4.7] The Rings of Power 24.07.19 (Forge).jar | 13 | notch | 16 | 2 | 0 | 0 | 0 | 0 | 1 | 3 | - |
| [1.4.7]CuteMobModels.zip | 33 | notch | 38 | 1 | 1 | 0 | 3 | 0 | 17 | 1 | - |
| [1.4.7]bspkrsCorev2.02.zip | 18 | notch | 17 | 4 | 0 | 1 | 2 | 0 | 7 | 0 | Packet250CustomPayload |
| adv-repulsion-systems-52.0.6.jar | 122 | notch | 85 | 7 | 0 | 5 | 1 | 0 | 50 | 43 | IGuiHandler, NetworkMod, NetworkRegistry |
| appeng-rv9-i.zip | 363 | notch | 104 | 10 | 0 | 12 | 1 | 0 | 51 | 65 | IConnectionHandler, IGuiHandler, IPacketHandler, ITickHandler, NetworkMod, NetworkRegistry, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| auto_third_person-forge-1.4.7-2.0.3.jar | 37 | notch | 31 | 4 | 0 | 5 | 0 | 0 | 0 | 0 | ITickHandler, TickRegistry |
| bufferfix-1.0.jar | 3 | - | 3 | 0 | 0 | 2 | 0 | 0 | 0 | 0 | NetworkMod |
| buildcraft-A-1.4.7-3.4.6.jar | 497 | notch | 146 | 16 | 0 | 19 | 0 | 0 | 220 | 163 | IGuiHandler, IPacketHandler, ITickHandler, NetworkMod, NetworkRegistry, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| dimensional-anchor-52.2.0.jar | 45 | notch | 69 | 7 | 0 | 6 | 1 | 0 | 3 | 4 | IGuiHandler, NetworkMod, NetworkRegistry |
| dragonmount_v0.93_mc1.4.7_forge.jar | 43 | notch | 93 | 4 | 1 | 9 | 1 | 0 | 12 | 1 | IPacketHandler, ITickHandler, NetworkMod, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| extrabees-1.5.0.6b.zip | 552 | notch | 103 | 9 | 1 | 15 | 0 | 0 | 118 | 56 | IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, Packet250CustomPayload, Player |
| forestry-A-2.0.0.11.jar | 1060 | notch | 180 | 17 | 0 | 24 | 0 | 0 | 548 | 163 | IGuiHandler, IPacketHandler, ITickHandler, NetworkMod, NetworkRegistry, Packet250CustomPayload, Player, TickRegistry |
| hopper-1.0.jar | 18 | notch | 45 | 3 | 0 | 3 | 0 | 0 | 3 | 6 | IGuiHandler, NetworkMod, NetworkRegistry |
| immibis-core-52.4.6.jar | 98 | notch | 71 | 11 | 0 | 13 | 1 | 0 | 15 | 15 | IGuiHandler, IPacketHandler, ITickHandler, NetworkMod, NetworkRegistry, Packet250CustomPayload, PacketDispatcher, Player, TickRegistry |
| immibis-microblocks-52.1.2.jar | 73 | notch | 59 | 7 | 0 | 3 | 0 | 0 | 39 | 12 | - |
| immibis-peripherals-52.1.2.jar | 145 | notch | 73 | 4 | 0 | 5 | 1 | 0 | 15 | 6 | IPlayerTracker, NetworkMod, NetworkRegistry |
| infinitubes-52.0.2.jar | 51 | notch | 44 | 2 | 0 | 3 | 0 | 0 | 13 | 2 | NetworkMod |
| liquid-xp-52.0.7.jar | 26 | notch | 47 | 5 | 0 | 15 | 0 | 0 | 14 | 5 | NetworkMod |
| miscperipherals-3.1c.jar | 283 | notch | 152 | 13 | 0 | 21 | 0 | 0 | 92 | 33 | IGuiHandler, ITickHandler, NetworkMod, NetworkRegistry, PacketDispatcher, TickRegistry |
| mod_EntityDetector.jar | 6 | notch | 24 | 2 | 0 | 2 | 0 | 0 | 2 | 1 | NetworkMod |
| mod_cctv.jar | 10 | notch | 43 | 4 | 0 | 5 | 0 | 0 | 4 | 3 | ITickHandler, NetworkMod, TickRegistry |
| mystcraft-uni-1.4.7-0.10.1.00.zip | 435 | notch | 192 | 19 | 0 | 26 | 1 | 0 | 175 | 59 | IConnectionHandler, IPacketHandler, NetworkMod, Packet250CustomPayload, Player |
| ovm-0.6.1.jar | 12 | - | 15 | 4 | 0 | 7 | 0 | 0 | 0 | 0 | IPacketHandler, ITickHandler, NetworkMod, NetworkRegistry, Player, TickRegistry |
| rawinputv1.0-1.4.7.jar | 4 | notch | 9 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | - |
| rpcDrive-0.1.0.jar | 13 | notch | 19 | 4 | 0 | 1 | 0 | 0 | 0 | 0 | NetworkMod |
| sprintable_1.4.7_1.0.0_forge.jar | 2 | notch | 18 | 0 | 0 | 3 | 0 | 0 | 0 | 0 | ITickHandler, TickRegistry |
| tubestuff-52.3.2.jar | 49 | notch | 82 | 10 | 1 | 8 | 1 | 0 | 32 | 18 | IGuiHandler, NetworkMod, NetworkRegistry |
| twilightforest-1.15.4.jar | 405 | notch | 324 | 21 | 3 | 26 | 0 | 0 | 418 | 58 | IConnectionHandler, IGuiHandler, IPacketHandler, IPlayerTracker, NetworkMod, NetworkRegistry, Packet250CustomPayload, Player, TickRegistry |
| xcraft-2.2.0-forge+1.4.7.jar | 19 | notch | 41 | 4 | 1 | 5 | 0 | 0 | 4 | 0 | ITickHandler, NetworkMod, TickRegistry |
