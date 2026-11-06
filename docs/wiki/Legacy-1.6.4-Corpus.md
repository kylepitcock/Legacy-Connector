# 1.6.4 corpus: what real 1.6.4 mods use

Measured 2026-11-06 with `tools/compat/scan_164.py` over 43 Forge 1.6.4 mod jars (`test-mods-164/`, gitignored; the list with versions and
CurseForge URLs is `test-mods-164/index.md`, next to the jars). Companion of [Legacy-1.6.4-Estimate](Legacy-1.6.4-Estimate.md); back to [Home](Home.md).

## Corpus

43 files: IC2 (2.0.397 experimental, the 1.6.4 line), Tinkers' Construct EX.30, Forestry 2.3.1.1, Magic Bees, Binnie's Mods, Gendustry, Twilight Forest 1.20.3, Ender IO 1.0.6,
Mystcraft 0.10.13, Witchery 0.20.5, Mo' Creatures 6.1, Lycanites Mobs, Custom NPCs, Thaumic Tinkerer, OpenBlocks + OpenPeripheralCore, BiblioCraft,
Carpenter's Blocks, ICBM + Universal Electricity, Minechem, ExtrabiomesXL, Backpacks, Decocraft, Modular Turrets, Simply Jetpacks, Forge Essentials,
the iChun mods (iChunUtil, Hats, Morph, Portal Gun, Gravity Gun, Mob Amputation), More Player Models, Dynamic Lights, CodeChickenCore + NEI + NEI Addons +
ChickenChunks + Ender Storage, TreeCapitator, JourneyMap, VoxelMap (LiteLoader).

Not obtainable (CurseForge returns 403: the authors disabled third-party downloads): BuildCraft 4, Railcraft, Thermal Expansion 3, CoFHCore, Redstone Arsenal, Waila,
Ars Magica 2, MineFactory Reloaded, ComputerCraft, Natura, Better Dungeons, Tinkers' Mechworks, MCA, MapWriter. Not on CurseForge/Modrinth with a 1.6.4 file under any slug tried:
Biomes O' Plenty, Thaumcraft, Iron Chests, Applied Energistics, Mekanism, Pam's HarvestCraft, Galacticraft, GregTech, Inventory Tweaks, Flans, Rei's Minimap. So the corpus is
thinner on the tech-pack mods (BuildCraft, Railcraft, TE) than a real FTB pack; those mostly use the same API families as IC2/Forestry/Ender IO, which are here.

## Method and limits

The scanner parses each class's constant pool (class refs, field/method refs with owner, annotation descriptors; nested jars inside zips are included) and buckets every
referenced `net/minecraft`, `net/minecraftforge`, `cpw/mods/fml` class: **present** (same path in `src/main/java/legacyconnector/v1_7_10`), **renamed** (known 1:1 1.6.4 to 1.7.10 rename or package move, table
`RENAMED` in the script), **removed** (API gone in 1.7.10, needs a facade), **moved** (same simple name elsewhere in the shim), **obfuscated** (default-package notch names such as `ye`, `atv`: the jar was
not reobfuscated to MCP class names), **missing** (none of these). Limits: "present" only says the class exists, not that every method the mod calls does (member names in the jars are SRG
`func_`/`field_` names of 1.6.4 that differ from 1.7.10's, so members are not compared; only the numeric-id members are recognised, by both MCP and 1.6.4 SRG names that the scanner lists); the loader also
synthesizes API classes absent from the shim, so "missing" is an upper bound for what will not link.

## Headline

- **The class surface is small.** Of 1,020 distinct Minecraft/Forge classes the 40 non-obfuscated jars reference, 861 exist in the 1.7.10 shim under the same name, 74 are plain
  renames or package moves (`Icon`, `IconRegister`, `Configuration`, `ForgeDirection`, `StepSound`, `EnumToolMaterial`, `INetworkManager`, `Attribute`...), 10 moved package, 28 are removed APIs (the old
  tick/network/player-tracker/key-binding families, `AABBPool`, `Vec3Pool`, `SoundPool`) and only 47 are unaccounted for (61 class-file references in total, most single uses like
  `BlockCactus`, `Packet15Place`; the only ones used by more than one jar are `EnchantmentThorns` 4, `ItemFlintAndSteel` 3).
  By class-file references: 51,863 present, 5,204 renamed, 1,774 removed, 269 moved, 61 missing, 4,013 obfuscated.
- **Numeric ids: 31 of 43 jars (72%)**, i.e. 31 of 40 that are not LiteLoader/coremod libraries; 25 to 30 jars read `blockID`/`itemID`/`Block.blocksList`/`World.getBlockId` and 23 call
  the int `setBlock`. The constructors `new Block(int, Material)`, `new Item(int)`, `new ItemStack(int, ...)` appear in 18, 24 and 26 jars, biome/potion/enchantment ids in 7 to 17. This confirms the id facade
  as the biggest single item. Ids also come from the mod's config file in practically all of them.
- **Old network API: 35 of 43 jars (81%)** (`NetworkMod`, `IPacketHandler`, `Packet250CustomPayload`, `PacketDispatcher`, `Player`; 23 jars touch `Packet250CustomPayload` itself, 25 `PacketDispatcher`). Plus the old
  tick API (`ITickHandler`, `TickRegistry`) in 34 jars, `IPlayerTracker` in 17, `IConnectionHandler` in 12, `KeyBindingRegistry` in 12. These need adapters; they are the second biggest item and not in the
  estimate's id-facade line.
- **Lifecycle annotations are a non-issue.** 1.6.4 already has `@Mod.EventHandler`: 40 of 40 `@Mod` jars use it, only 1 (Mo' Creatures) still carries `@PreInit`/`@Init`/`@PostInit` as well. 0 jars use only the
  old forms. (The estimate's "1 to 2 days, old `@PreInit` forms" can shrink to a few hours.)
- **Event bus:** 40 of 40 use `@ForgeSubscribe` and `net.minecraftforge.event.Event`/`EventBus`/`EventPriority` (all moved to `cpw.mods.fml.common.eventhandler` in 1.7.10), `@SubscribeEvent` in none. A single remap rule family.
- **Obfuscated (notch) class names: 9 of 43 jars (21%)**: CodeChickenCore, NEI, ChickenChunks, Ender Storage, TreeCapitator, Backpacks, Decocraft, Modular Turrets, VoxelMap (those jars are coremod/ASM-based or were built
  without reobfuscation to MCP class names). They reference 303 distinct short names (`ye`, `atv`, `we`...). Handling them needs the 1.6.4 notch to MCP class map (FML's `joined.srg`/`fml_srg`) in the loader, which the
  estimate did not list. NEI itself is bridged to JEI by project rule, so that jar needs only to be recognised, not translated.
- **Rendering API:** `Icon`/`IconRegister` in 26/25 jars with 1,123/719 class-file uses, the heaviest single rename (a type-level alias or remap rule handles it); `StepSound` 19 jars; `Vec3Pool`/`AABBPool` 19/22 jars (call sites
  `worldObj.getWorldVec3Pool().getVecFromPool` become `Vec3.createVectorHelper`; needs a method-level rewrite, not only a class rename).
- **Largest jars add little new API:** IC2, Tinkers', Forestry, Twilight Forest, Ender IO and Mystcraft are blocked by the same four things (renamed types, numeric ids, old packets, old tick handlers); see the per-jar list below.

## Numbers

Regenerate: `python tools/compat/scan_164.py <dir-with-jars> --md out.md`.

Jars scanned: 43, shim classes (1.7.10 tree): 1339

### Distinct referenced classes per bucket

| Bucket | Distinct classes | Class-file references |
|---|---:|---:|
| present | 861 | 51863 |
| renamed | 74 | 5204 |
| removed | 28 | 1774 |
| moved | 10 | 269 |
| missing | 47 | 61 |
| obfuscated | 303 | 4013 |

### Per-jar share of Minecraft/Forge class references that are not present in the shim

| Jar | Classes | present | renamed+removed | moved | missing | obfuscated | numeric-id refs | old net classes | old lifecycle | new lifecycle |
|---|---:|---:|---:|---:|---:|---:|---:|---|---|---|
| BiblioCraft[v1.5.5].zip | 244 | 132 | 15+8 | 1 | 2 | 0 | 110 | IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, Packet, Packet250CustomPayload | - | yes |
| Carpenter's Blocks v3.2.8.6 - MC 1.6.4.jar | 157 | 108 | 18+9 | 2 | 1 | 0 | 75 | IPacketHandler, NetworkMod, Player, Packet, Packet250CustomPayload | - | yes |
| ChickenChunks-1.6.4-1.3.3.4-universal.jar | 49 | 34 | 2+5 | 1 | 0 | 45 | 0 | ITickHandler, NetworkMod, TickRegistry | - | yes |
| CodeChickenCore-1.6.4-0.9.0.9-universal.jar | 101 | 44 | 2+3 | 1 | 0 | 56 | 0 | ITickHandler, TickRegistry | - | - |
| CustomNPCs_1.6.2-1.6.4.zip | 449 | 203 | 21+17 | 2 | 6 | 0 | 81 | ITickHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| Decocraft_1.10.0_1.6.4.zip | 256 | 20 | 3+4 | 1 | 0 | 38 | 0 | IGuiHandler, IPacketHandler, NetworkMod, PacketDispatcher, Player | - | yes |
| DrZharks MoCreatures Mod v6.1.0 (1).zip | 1233 | 203 | 27+15 | 3 | 0 | 0 | 229 | ITickHandler, IGuiHandler, IPacketHandler, NetworkMod, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | yes | yes |
| DynamicLights_1.6.4.zip | 41 | 47 | 3+5 | 1 | 0 | 1 | 11 | ITickHandler, TickRegistry | - | yes |
| EnderIO-1.6.4-1.0.6.376.jar | 899 | 154 | 21+12 | 3 | 1 | 0 | 183 | ITickHandler, IConnectionHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| EnderStorage-1.6.4-1.4.3.6-universal.jar | 46 | 33 | 2+5 | 1 | 0 | 58 | 0 | ITickHandler, NetworkMod, TickRegistry | - | yes |
| ExtrabiomesXL-1.6.4-3.15.9.jar | 324 | 151 | 19+3 | 1 | 2 | 0 | 318 | NetworkMod, Packet | - | yes |
| GravityGun2.0.0.zip | 22 | 99 | 14+17 | 2 | 0 | 0 | 22 | ITickHandler, IConnectionHandler, IPacketHandler, ITinyPacketHandler, NetworkMod, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| Hats2.1.8.zip | 57 | 108 | 12+15 | 2 | 0 | 0 | 0 | ITickHandler, IConnectionHandler, IPacketHandler, ITinyPacketHandler, NetworkMod, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| ICBM-1.4.2.386.jar | 291 | 182 | 17+9 | 2 | 0 | 0 | 144 | ITickHandler, IGuiHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet | - | yes |
| JourneyMap4.0.3_Unlimited_MC1.6.4.jar | 376 | 127 | 14+7 | 1 | 3 | 0 | 6 | ITickHandler, TickRegistry | - | yes |
| LycanitesMobsComplete 1.5.1h [1.6.4].jar | 321 | 165 | 18+14 | 2 | 2 | 0 | 265 | ITickHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| Minechem-1.6.4-5.0.5.FINAL.jar | 317 | 165 | 16+13 | 1 | 0 | 0 | 121 | ITickHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| MobAmputation2.0.1.zip | 13 | 62 | 10+11 | 2 | 1 | 0 | 0 | ITickHandler, IConnectionHandler, ITinyPacketHandler, NetworkMod, PacketDispatcher, Player, TickRegistry, Packet | - | yes |
| ModularTurrets-1.6.4-v1.0.1.zip | 181 | 37 | 4+4 | 1 | 0 | 81 | 0 | IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player | - | yes |
| MorePlayerModels_1.6.2-1.6.4.zip | 95 | 114 | 11+14 | 2 | 0 | 0 | 5 | ITickHandler, IConnectionHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| Morph-Beta-0.7.2.zip | 54 | 141 | 17+18 | 3 | 2 | 0 | 0 | ITickHandler, IConnectionHandler, IPacketHandler, ITinyPacketHandler, NetworkMod, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| NotEnoughItems-1.6.4-1.6.1.9-universal.jar | 215 | 40 | 1+5 | 1 | 0 | 150 | 0 | ITickHandler, TickRegistry | - | - |
| OpenBlocks-1.2.9.jar | 582 | 259 | 25+10 | 5 | 2 | 0 | 105 | ITickHandler, IGuiHandler, NetworkMod, NetworkRegistry, Player, TickRegistry, Packet | - | yes |
| OpenPeripheralCore-0.4.1.jar | 256 | 66 | 5+4 | 1 | 1 | 0 | 4 | ITickHandler, TickRegistry | - | yes |
| PortalGun2.0.1.zip | 73 | 182 | 25+18 | 3 | 0 | 0 | 125 | ITickHandler, IConnectionHandler, IPacketHandler, ITinyPacketHandler, NetworkMod, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| SimplyJetpacks-0.3.1.1.jar | 35 | 61 | 10+11 | 1 | 0 | 0 | 5 | ITickHandler, IPacketHandler, NetworkMod, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| TConstruct_mc1.6.4_EX.30.jar | 543 | 356 | 33+15 | 4 | 4 | 0 | 450 | ITickHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| ThaumicTinkerer-2.3-141.jar | 726 | 211 | 24+13 | 2 | 1 | 0 | 175 | ITickHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| Universal-Electricity-3.0.0.1-dev.jar | 96 | 40 | 6+4 | 1 | 0 | 0 | 4 | ITickHandler, TickRegistry | - | yes |
| [1.6.4]TreeCapitator.Forge.1.6.4.r06.Universal.jar | 27 | 31 | 1+9 | 1 | 0 | 33 | 0 | ITickHandler, IConnectionHandler, IPacketHandler, NetworkMod, PacketDispatcher, Player, TickRegistry | - | yes |
| backpacks 1.6.4 - 3.0.1.jar | 206 | 40 | 8+10 | 1 | 0 | 104 | 0 | ITickHandler, IConnectionHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry | - | yes |
| binnie-mods-1.8.0.jar | 1036 | 162 | 22+9 | 1 | 1 | 0 | 194 | ITickHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| forestry-A-2.3.1.1.jar | 1275 | 252 | 27+10 | 1 | 2 | 0 | 433 | ITickHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| forgeessentials-1.6.4-1.3.0.4-server.zip | 1460 | 172 | 19+15 | 2 | 9 | 0 | 72 | ITickHandler, IPacketHandler, NetworkMod, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| gendustry-mc164-1.1.3.fix1.jar | 660 | 101 | 14+1 | 1 | 0 | 0 | 44 | IGuiHandler, NetworkMod, NetworkRegistry | - | yes |
| iChunUtil2.4.0.zip | 21 | 67 | 6+4 | 1 | 0 | 0 | 3 | ITickHandler, TickRegistry | - | yes |
| industrialcraft-2_2.0.397-experimental.jar | 811 | 268 | 30+18 | 2 | 1 | 0 | 435 | ITickHandler, IConnectionHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| magicbees-2.1.13.jar | 123 | 95 | 10+6 | 1 | 0 | 0 | 78 | ITickHandler, IGuiHandler, NetworkMod, NetworkRegistry, TickRegistry | - | yes |
| mod_voxelMap_1.4.9_for_1.6.4.litemod | 115 | 7 | 0+0 | 0 | 0 | 159 | 0 | - | - | - |
| mystcraft-uni-1.6.4-0.10.13.01.zip | 570 | 245 | 33+10 | 2 | 4 | 0 | 201 | IConnectionHandler, IPacketHandler, NetworkMod, PacketDispatcher, Player, Packet, Packet250CustomPayload | - | yes |
| neiaddons-mc164-1.10.5.70.jar | 91 | 60 | 6+10 | 0 | 0 | 0 | 30 | ITickHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| twilightforest-1.20.3.jar | 520 | 344 | 32+13 | 2 | 2 | 0 | 609 | IConnectionHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |
| witchery-1.6.4-0.20.5.zip | 742 | 394 | 31+15 | 4 | 5 | 0 | 661 | ITickHandler, IGuiHandler, IPacketHandler, NetworkMod, NetworkRegistry, PacketDispatcher, Player, TickRegistry, Packet, Packet250CustomPayload | - | yes |

- numeric ids: 31 of 43 jars (72%)
- old network API (Packet250CustomPayload / IPacketHandler / NetworkMod / PacketDispatcher): 35 of 43 jars (81%)
- old lifecycle annotations (@PreInit/@Init/@PostInit): 1 of 43 jars (2%)
- only old lifecycle annotations (no @EventHandler): 0 of 43 jars (0%)
- new lifecycle annotation (@EventHandler): 40 of 43 jars (93%)
- @ForgeSubscribe: 40 of 43 jars (93%)
- @SubscribeEvent: 0 of 43 jars (0%)
- obfuscated (notch) class names: 9 of 43 jars (20%)
- a @Mod class at all: 40 of 43 jars (93%)

### Numeric-id API references (all jars)

| Reference | Count | Jars |
|---|---:|---:|
| itemID | 1111 | 30 |
| blockID | 1096 | 26 |
| World.getBlockId | 719 | 25 |
| Block.blocksList | 490 | 27 |
| World.setBlock(int ids) | 423 | 23 |
| new ItemStack(int ...) | 406 | 26 |
| new Item(int ...) | 162 | 24 |
| new PotionEffect(int ...) | 160 | 17 |
| new ItemBlock(int ...) | 88 | 15 |
| new Block(int ...) | 84 | 18 |
| Item.itemsList | 83 | 22 |
| new BlockContainer(int ...) | 70 | 15 |
| biomeID | 43 | 11 |
| new ItemArmor(int ...) | 27 | 13 |
| BiomeGenBase.biomeList | 22 | 7 |
| Enchantment.enchantmentsList | 19 | 8 |
| new BlockFlower(int ...) | 14 | 5 |
| new ItemFood(int ...) | 14 | 9 |
| Potion.potionTypes | 11 | 7 |
| new ItemSword(int ...) | 11 | 5 |
| new BlockLog(int ...) | 10 | 3 |
| new BlockLeavesBase(int ...) | 9 | 5 |
| new ItemPickaxe(int ...) | 9 | 5 |
| new Enchantment(int ...) | 8 | 5 |
| new BlockStairs(int ...) | 6 | 6 |
| new ItemAxe(int ...) | 6 | 3 |
| new BlockHalfSlab(int ...) | 5 | 5 |
| new ItemSlab(int ...) | 5 | 3 |
| new BlockBreakable(int ...) | 5 | 4 |
| new BiomeGenBase(int ...) | 4 | 4 |
| new ItemTool(int ...) | 4 | 3 |
| new BlockLeaves(int ...) | 4 | 3 |
| new BlockSapling(int ...) | 4 | 4 |
| new ItemSpade(int ...) | 4 | 3 |
| new BlockFence(int ...) | 3 | 3 |
| new BlockWoodSlab(int ...) | 3 | 2 |
| new Potion(int ...) | 3 | 3 |
| new ItemBucket(int ...) | 3 | 3 |
| new BlockDoor(int ...) | 3 | 3 |
| new ItemHoe(int ...) | 3 | 2 |
| new ItemRecord(int ...) | 2 | 2 |
| new BlockFenceGate(int ...) | 2 | 2 |
| new BlockWall(int ...) | 2 | 2 |
| new BlockVine(int ...) | 2 | 2 |
| new BlockWood(int ...) | 2 | 2 |
| new BlockChest(int ...) | 2 | 2 |
| new ItemMultiTextureTile(int ...) | 2 | 2 |
| new BlockTorch(int ...) | 2 | 1 |
| new ItemMapBase(int ...) | 2 | 1 |
| new ItemMap(int ...) | 2 | 1 |
| new BlockRotatedPillar(int ...) | 2 | 1 |
| new ItemFlintAndSteel(int ...) | 1 | 1 |
| new BlockStep(int ...) | 1 | 1 |
| new BlockPressurePlate(int ...) | 1 | 1 |
| new BlockRail(int ...) | 1 | 1 |
| new BlockButton(int ...) | 1 | 1 |
| new BlockFire(int ...) | 1 | 1 |
| new BlockTrapDoor(int ...) | 1 | 1 |
| new BlockRailBase(int ...) | 1 | 1 |
| new BlockSand(int ...) | 1 | 1 |
| new BlockLadder(int ...) | 1 | 1 |
| new BlockWeb(int ...) | 1 | 1 |
| new BlockCocoa(int ...) | 1 | 1 |
| new BlockOreStorage(int ...) | 1 | 1 |
| new ItemDoor(int ...) | 1 | 1 |
| new ItemLeaves(int ...) | 1 | 1 |
| new ItemMonsterPlacer(int ...) | 1 | 1 |
| new BlockDragonEgg(int ...) | 1 | 1 |
| new BlockLilyPad(int ...) | 1 | 1 |
| new BlockIce(int ...) | 1 | 1 |
| new ItemBow(int ...) | 1 | 1 |
| new ItemColored(int ...) | 1 | 1 |
| new ItemSeeds(int ...) | 1 | 1 |

### Top 60 missing classes by number of jars using them

| Class | Jars | Class files | Hint |
|---|---:|---:|---|
| `net/minecraft/enchantment/EnchantmentThorns` | 4 | 8 |  |
| `net/minecraft/item/ItemFlintAndSteel` | 3 | 4 |  |
| `net/minecraft/item/ItemWritableBook` | 1 | 3 |  |
| `net/minecraft/util/EnumOS` | 1 | 2 |  |
| `net/minecraft/network/packet/Packet12PlayerLook` | 1 | 2 |  |
| `net/minecraft/block/BlockQuartz` | 1 | 1 |  |
| `net/minecraft/entity/ai/EntityAIDoorInteract` | 1 | 1 |  |
| `net/minecraft/entity/ai/EntityAINearestAttackableTargetSorter` | 1 | 1 |  |
| `net/minecraft/client/gui/GuiButtonMerchant` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet103SetSlot` | 1 | 1 |  |
| `net/minecraft/tileentity/WeightedRandomMinecart` | 1 | 1 |  |
| `cpw/mods/fml/common/network/OpenGuiPacket` | 1 | 1 |  |
| `net/minecraft/block/BlockStep` | 1 | 1 |  |
| `net/minecraft/world/gen/feature/WorldGenDesertWells` | 1 | 1 |  |
| `net/minecraft/client/gui/GuiGameOver` | 1 | 1 |  |
| `net/minecraft/world/chunk/storage/RegionFileCache` | 1 | 1 |  |
| `net/minecraft/item/ItemSaddle` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet7UseEntity` | 1 | 1 |  |
| `net/minecraftforge/client/GuiControlsScrollPanel` | 1 | 1 |  |
| `net/minecraft/client/model/ModelIronGolem` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet62LevelSound` | 1 | 1 |  |
| `net/minecraft/block/BlockCactus` | 1 | 1 |  |
| `net/minecraft/tileentity/TileEntityDaylightDetector` | 1 | 1 |  |
| `net/minecraft/block/BlockBaseRailLogic` | 1 | 1 |  |
| `net/minecraft/block/BlockComparator` | 1 | 1 |  |
| `net/minecraft/entity/EntitySelectorArmoredMob` | 1 | 1 |  |
| `net/minecraftforge/client/EnumHelperClient` | 1 | 1 |  |
| `net/minecraft/client/resources/data/BaseMetadataSectionSerializer` | 1 | 1 |  |
| `net/minecraft/block/BlockRedstoneLogic` | 1 | 1 |  |
| `net/minecraft/block/BlockMelon` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet201PlayerInfo` | 1 | 1 |  |
| `net/minecraft/item/ItemCarrotOnAStick` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet20NamedEntitySpawn` | 1 | 1 |  |
| `net/minecraft/block/BlockOreStorage` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet30Entity` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet19EntityAction` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet18Animation` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet29DestroyEntity` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet15Place` | 1 | 1 |  |
| `net/minecraft/block/BlockStationary` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet9Respawn` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet43Experience` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet41EntityEffect` | 1 | 1 |  |
| `net/minecraft/world/gen/structure/ComponentStrongholdStairs2` | 1 | 1 |  |
| `net/minecraftforge/common/ForgeDummyContainer` | 1 | 1 |  |
| `net/minecraft/block/BlockSilverfish` | 1 | 1 |  |
| `net/minecraft/network/packet/Packet32EntityLook` | 1 | 1 |  |

### Top 60 removed classes by number of jars using them

| Class | Jars | Class files | Hint |
|---|---:|---:|---|
| `cpw/mods/fml/common/TickType` | 35 | 90 | removed (tick events in 1.7.10) |
| `cpw/mods/fml/common/registry/TickRegistry` | 35 | 70 | removed (tick events in 1.7.10) |
| `cpw/mods/fml/common/network/NetworkMod` | 35 | 51 | removed (annotation) |
| `cpw/mods/fml/common/ITickHandler` | 34 | 116 | removed (tick events in 1.7.10) |
| `cpw/mods/fml/common/network/Player` | 30 | 133 | removed (EntityPlayer) |
| `net/minecraft/network/packet/Packet` | 27 | 397 | removed (Packet in net/minecraft/network) |
| `cpw/mods/fml/common/network/IPacketHandler` | 27 | 39 | removed (SimpleNetworkWrapper / FMLEventChannel) |
| `cpw/mods/fml/common/network/PacketDispatcher` | 25 | 266 | removed |
| `net/minecraft/network/packet/Packet250CustomPayload` | 23 | 197 | removed (FMLProxyPacket) |
| `net/minecraft/util/AABBPool` | 22 | 123 | removed (AxisAlignedBB.getBoundingBox is static) |
| `net/minecraft/util/Vec3Pool` | 19 | 90 | removed (Vec3.createVectorHelper) |
| `cpw/mods/fml/common/IPlayerTracker` | 17 | 56 | removed (PlayerEvent in 1.7.10) |
| `cpw/mods/fml/client/registry/KeyBindingRegistry` | 12 | 26 | removed (ClientRegistry.registerKeyBinding + KeyInputEvent) |
| `cpw/mods/fml/client/registry/KeyBindingRegistry$KeyHandler` | 12 | 26 | removed (ClientRegistry.registerKeyBinding + KeyInputEvent) |
| `cpw/mods/fml/common/network/IConnectionHandler` | 12 | 17 | removed (FMLNetworkEvent) |
| `cpw/mods/fml/common/network/NetworkMod$SidedPacketHandler` | 10 | 10 | removed (annotation) |
| `net/minecraft/client/audio/SoundPool` | 8 | 8 | removed (SoundHandler, sounds.json) |
| `cpw/mods/fml/common/IScheduledTickHandler` | 7 | 25 | removed (tick events in 1.7.10) |
| `cpw/mods/fml/common/network/NetworkModHandler` | 6 | 6 | removed |
| `cpw/mods/fml/common/network/ITinyPacketHandler` | 5 | 5 | removed |
| `cpw/mods/fml/common/ICraftingHandler` | 3 | 6 | removed (PlayerEvent.ItemCraftedEvent) |
| `net/minecraft/crash/CallableMinecraftVersion` | 3 | 3 | removed (crash report internals) |
| `net/minecraft/block/EnumMobType` | 2 | 3 | removed |
| `net/minecraft/src/ModLoader` | 2 | 3 | removed (ModLoader base mods) |
| `net/minecraft/client/audio/SoundPoolEntry` | 2 | 2 | removed (SoundHandler, sounds.json) |
| `net/minecraft/logging/ILogAgent` | 2 | 2 | removed (log4j) |
| `cpw/mods/fml/common/registry/ItemData` | 2 | 2 | removed |
| `cpw/mods/fml/common/IPickupNotifier` | 1 | 2 | removed (PlayerEvent.ItemPickupEvent) |

### Top 60 renamed classes by number of jars using them

| Class | Jars | Class files | Hint |
|---|---:|---:|---|
| `net/minecraftforge/event/ForgeSubscribe` | 40 | 206 | cpw/mods/fml/common/eventhandler/SubscribeEvent |
| `net/minecraftforge/common/Configuration` | 32 | 293 | net/minecraftforge/common/config/Configuration |
| `net/minecraftforge/common/Property` | 30 | 120 | net/minecraftforge/common/config/Property |
| `net/minecraftforge/common/ForgeDirection` | 28 | 873 | net/minecraftforge/common/util/ForgeDirection |
| `net/minecraft/network/INetworkManager` | 27 | 140 | net/minecraft/network/NetworkManager |
| `net/minecraft/util/Icon` | 26 | 1123 | net/minecraft/util/IIcon |
| `net/minecraft/client/renderer/texture/IconRegister` | 25 | 719 | net/minecraft/client/renderer/texture/IIconRegister |
| `net/minecraft/network/NetServerHandler` | 23 | 66 | net/minecraft/network/NetHandlerPlayServer |
| `net/minecraftforge/event/Event` | 22 | 171 | cpw/mods/fml/common/eventhandler/Event |
| `net/minecraft/util/ChatMessageComponent` | 21 | 87 | net/minecraft/util/IChatComponent / ChatComponentText |
| `net/minecraft/block/StepSound` | 19 | 192 | net/minecraft/block/Block$SoundType |
| `net/minecraft/block/BlockFluid` | 18 | 71 | net/minecraft/block/BlockLiquid |
| `net/minecraft/block/BlockHalfSlab` | 17 | 53 | net/minecraft/block/BlockSlab |
| `net/minecraft/item/EnumArmorMaterial` | 14 | 43 | net/minecraft/item/ItemArmor$ArmorMaterial |
| `net/minecraftforge/event/Event$Result` | 13 | 28 | cpw/mods/fml/common/eventhandler/Event |
| `net/minecraft/client/multiplayer/NetClientHandler` | 13 | 14 | net/minecraft/client/network/NetHandlerPlayClient |
| `net/minecraft/network/packet/Packet132TileEntityData` | 12 | 75 | net/minecraft/network/play/server/S35PacketUpdateTileEntity |
| `net/minecraft/util/EnumMovingObjectType` | 12 | 64 | net/minecraft/util/MovingObjectPosition$MovingObjectType |
| `net/minecraft/entity/ai/attributes/Attribute` | 11 | 160 | net/minecraft/entity/ai/attributes/IAttribute |
| `net/minecraftforge/event/EventPriority` | 11 | 30 | cpw/mods/fml/common/eventhandler/EventPriority |
| `net/minecraft/item/EnumToolMaterial` | 10 | 69 | net/minecraft/item/Item$ToolMaterial |
| `net/minecraft/client/resources/ResourceManager` | 10 | 23 | net/minecraft/client/resources/IResourceManager |
| `net/minecraft/network/packet/NetHandler` | 10 | 16 | net/minecraft/network/INetHandler |
| `net/minecraft/network/NetLoginHandler` | 10 | 11 | net/minecraft/network/NetHandlerLoginServer |
| `net/minecraft/network/packet/Packet1Login` | 10 | 11 | net/minecraft/network/play/server/S01PacketJoinGame |
| `net/minecraft/entity/ai/attributes/AttributeInstance` | 9 | 155 | net/minecraft/entity/ai/attributes/IAttributeInstance |
| `net/minecraftforge/event/Cancelable` | 9 | 29 | cpw/mods/fml/common/eventhandler/Cancelable |
| `net/minecraftforge/common/EnumHelper` | 8 | 10 | net/minecraftforge/common/util/EnumHelper |
| `net/minecraft/entity/EntityLivingData` | 7 | 49 | net/minecraft/entity/IEntityLivingData |
| `net/minecraft/client/renderer/tileentity/TileEntityRenderer` | 7 | 31 | net/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher |
| `net/minecraft/client/renderer/texture/TextureObject` | 7 | 13 | net/minecraft/client/renderer/texture/ITextureObject |
| `net/minecraft/network/packet/Packet131MapData` | 6 | 26 | net/minecraft/network/play/server/S34PacketMaps |
| `net/minecraft/client/resources/Resource` | 6 | 6 | net/minecraft/client/resources/IResource |
| `net/minecraft/world/gen/structure/ComponentVillageStartPiece` | 5 | 14 | net/minecraft/world/gen/structure/StructureVillagePieces$Start |
| `net/minecraft/world/gen/structure/ComponentVillage` | 5 | 8 | net/minecraft/world/gen/structure/StructureVillagePieces$Village |
| `net/minecraft/entity/EntityEggInfo` | 5 | 7 | net/minecraft/entity/EntityList$EntityEggInfo |
| `net/minecraft/world/gen/structure/StructureVillagePieceWeight` | 5 | 7 | net/minecraft/world/gen/structure/StructureVillagePieces$PieceWeight |
| `net/minecraft/entity/ai/EntityAITaskEntry` | 5 | 5 | net/minecraft/entity/ai/EntityAITasks$EntityAITaskEntry |
| `net/minecraft/world/biome/SpawnListEntry` | 4 | 29 | net/minecraft/world/biome/BiomeGenBase$SpawnListEntry |
| `net/minecraftforge/common/ConfigCategory` | 4 | 9 | net/minecraftforge/common/config/ConfigCategory |
| `net/minecraft/network/packet/Packet28EntityVelocity` | 4 | 7 | net/minecraft/network/play/server/S12PacketEntityVelocity |
| `net/minecraft/util/WeightedRandomItem` | 4 | 6 | net/minecraft/util/WeightedRandom$Item |
| `net/minecraft/entity/item/EntityFallingSand` | 3 | 9 | net/minecraft/entity/item/EntityFallingBlock |
| `net/minecraft/client/settings/EnumOptions` | 3 | 5 | net/minecraft/client/settings/GameSettings$Options |
| `net/minecraft/tileentity/TileEntityRecordPlayer` | 3 | 4 | net/minecraft/block/BlockJukebox$TileEntityJukebox |
| `net/minecraft/entity/EntityOwnable` | 3 | 4 | net/minecraft/entity/IEntityOwnable |
| `net/minecraft/entity/player/EnumStatus` | 3 | 3 | net/minecraft/entity/player/EntityPlayer$EnumStatus |
| `net/minecraftforge/client/event/sound/PlaySoundEvent` | 3 | 3 | net/minecraftforge/client/event/sound/PlaySoundEvent17 |
| `net/minecraftforge/client/event/sound/PlayStreamingEvent` | 3 | 3 | net/minecraftforge/client/event/sound/PlayStreamingSourceEvent |
| `net/minecraft/server/management/PlayerInstance` | 3 | 3 | net/minecraft/server/management/PlayerManager$PlayerInstance |
| `net/minecraft/network/packet/Packet51MapChunk` | 3 | 3 | net/minecraft/network/play/server/S21PacketChunkData |
| `net/minecraft/client/resources/ResourceManagerReloadListener` | 3 | 3 | net/minecraft/client/resources/IResourceManagerReloadListener |
| `net/minecraft/block/BlockTripWireSource` | 3 | 3 | net/minecraft/block/BlockTripWireHook |
| `net/minecraft/world/EnumGameType` | 2 | 8 | net/minecraft/world/WorldSettings$GameType |
| `net/minecraft/network/packet/Packet100OpenWindow` | 2 | 6 | net/minecraft/network/play/server/S2DPacketOpenWindow |
| `net/minecraftforge/event/Event$HasResult` | 2 | 6 | cpw/mods/fml/common/eventhandler/Event |
| `net/minecraft/client/gui/GuiSmallButton` | 2 | 4 | net/minecraft/client/gui/GuiOptionButton |
| `net/minecraft/world/storage/MapCoord` | 2 | 4 | net/minecraft/world/storage/MapData$MapCoord |
| `net/minecraftforge/common/Property$Type` | 2 | 3 | net/minecraftforge/common/config/Property |
| `net/minecraft/world/storage/MapInfo` | 2 | 3 | net/minecraft/world/storage/MapData$MapInfo |

### Top 60 moved classes by number of jars using them

| Class | Jars | Class files | Hint |
|---|---:|---:|---|
| `net/minecraftforge/event/EventBus` | 40 | 206 | ['cpw/mods/fml/common/eventhandler/EventBus'] |
| `net/minecraft/dispenser/IRegistry` | 7 | 19 | ['net/minecraft/util/IRegistry'] |
| `net/minecraftforge/common/FakePlayer` | 6 | 17 | ['net/minecraftforge/common/util/FakePlayer'] |
| `net/minecraft/item/ItemInWorldManager` | 6 | 11 | ['net/minecraft/server/management/ItemInWorldManager'] |
| `cpw/mods/fml/common/network/FMLNetworkHandler` | 6 | 6 | ['cpw/mods/fml/common/network/internal/FMLNetworkHandler'] |
| `net/minecraftforge/common/FakePlayerFactory` | 2 | 5 | ['net/minecraftforge/common/util/FakePlayerFactory'] |
| `net/minecraft/client/gui/GuiSlider` | 2 | 2 | ['cpw/mods/fml/client/config/GuiSlider'] |
| `net/minecraftforge/event/IEventListener` | 1 | 1 | ['cpw/mods/fml/common/eventhandler/IEventListener'] |
| `net/minecraftforge/event/ASMEventHandler` | 1 | 1 | ['cpw/mods/fml/common/eventhandler/ASMEventHandler'] |
| `net/minecraftforge/event/ListenerList` | 1 | 1 | ['cpw/mods/fml/common/eventhandler/ListenerList'] |

### Blockers of the most-used jars (non-present classes by number of class files, then numeric-id references)

- **industrialcraft-2_2.0.397-experimental.jar**: `Configuration` (renamed, 146), `Icon` (renamed, 58), `ForgeDirection` (renamed, 57), `EventBus` (moved, 33), `IconRegister` (renamed, 27), `StepSound` (renamed, 27); numeric: itemID 132, blockID 75, World.getBlockId 67, Block.blocksList 55
- **TConstruct_mc1.6.4_EX.30.jar**: `Icon` (renamed, 106), `IconRegister` (renamed, 66), `ForgeDirection` (renamed, 40), `StepSound` (renamed, 29), `Packet` (removed, 26), `INetworkManager` (renamed, 18); numeric: blockID 69, World.getBlockId 64, Block.blocksList 63, new ItemStack(int ...) 62
- **forestry-A-2.3.1.1.jar**: `ForgeDirection` (renamed, 128), `Icon` (renamed, 124), `IconRegister` (renamed, 62), `EventBus` (moved, 15), `Event` (renamed, 14), `Packet` (removed, 11); numeric: blockID 106, itemID 89, World.getBlockId 60, new ItemStack(int ...) 47
- **twilightforest-1.20.3.jar**: `Icon` (renamed, 82), `IconRegister` (renamed, 65), `AttributeInstance` (renamed, 44), `Attribute` (renamed, 44), `StructurePieceBlockSelector` (renamed, 35), `Vec3Pool` (removed, 24); numeric: blockID 237, itemID 110, World.getBlockId 57, World.setBlock(int ids) 47
- **EnderIO-1.6.4-1.0.6.376.jar**: `ForgeDirection` (renamed, 151), `Icon` (renamed, 98), `Packet` (removed, 45), `IconRegister` (renamed, 44), `PacketDispatcher` (removed, 24), `StepSound` (renamed, 16); numeric: new ItemStack(int ...) 44, itemID 30, World.getBlockId 26, Block.blocksList 20
- **NotEnoughItems-1.6.4-1.6.1.9-universal.jar**: `ye` (obfuscated, 91), `by` (obfuscated, 33), `atv` (obfuscated, 30), `we` (obfuscated, 27), `uf` (obfuscated, 26), `uy` (obfuscated, 24); numeric: none
- **witchery-1.6.4-0.20.5.zip**: `Packet` (removed, 98), `PacketDispatcher` (removed, 94), `Packet250CustomPayload` (removed, 76), `Icon` (renamed, 59), `EnumMovingObjectType` (renamed, 44), `IconRegister` (renamed, 38); numeric: blockID 173, World.getBlockId 113, itemID 112, World.setBlock(int ids) 56
- **mystcraft-uni-1.6.4-0.10.13.01.zip**: `Packet` (removed, 40), `Icon` (renamed, 27), `IconRegister` (renamed, 22), `Event` (renamed, 13), `Player` (removed, 13), `Packet250CustomPayload` (removed, 11); numeric: blockID 59, World.setBlock(int ids) 33, World.getBlockId 29, itemID 16
- **JourneyMap4.0.3_Unlimited_MC1.6.4.jar**: `ForgeSubscribe` (renamed, 4), `ResourceManager` (renamed, 3), `BlockFluid` (renamed, 3), `EnumOS` (missing, 2), `INetworkManager` (renamed, 2), `NetClientHandler` (renamed, 2); numeric: Block.blocksList 4, blockID 2
- **DrZharks MoCreatures Mod v6.1.0 (1).zip**: `AttributeInstance` (renamed, 55), `Attribute` (renamed, 55), `Icon` (renamed, 12), `IconRegister` (renamed, 12), `EntityLivingData` (renamed, 5), `StepSound` (renamed, 5); numeric: itemID 98, blockID 39, World.getBlockId 28, Block.blocksList 20

