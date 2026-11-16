# 1.12.2: stub ranking (synthesized members by number of jars)

Generated 2026-11-16 by `tools/dev/agent-tasks/make_stub_ranking.py` from the offline probe of 662 jar(s) (581 links, 49 unresolved, 26 missing-dependency, 6 obsolete); 1225 distinct synthesized members. Regenerate after implementing; implemented members drop out. Format: `n  member  # jars`.

## server-side API (top 150 of 639)

```
  5  net.minecraft.realms.RealmsBridge.<init>()V  # GalaxySpace-1.12.2-2.2.0.jar, Wynntils-MC1.12.2-v1.15.1-beta.10.jar, aether-1.12.2-v1.5.4.1.jar ...
  5  net.minecraft.world.gen.feature.WorldGenDoublePlant.setPlantType(Lnet.minecraft.block.BlockDoublePlant$EnumPlantType;)V  # ConquestReforged-3.0.2-mc1.12.2.jar, ProjectVibrantJourneys-1.12.2-1.6.4.jar, Traverse-1.12.2-1.6.0-69.jar ...
  5  net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer.<init>()V  # +mysteriumlib-1.5.7.jar, 131-CNPC-Gecko-Addon-1.12.2-(05Jul20_03Feb22)-1.1.1.jar, AdvancedRocketry-1.12.2-2.1.9-2.jar ...
  4  net.minecraft.entity.passive.EntityTameable.NO_ANIMATION:Lnet.ilexiconn.llibrary.server.animation.Animation;  # fossilsarcheology-8.0.6.jar, iceandfire-1.9.1-1.12.2.jar, lepidodendron-1.12.2-63.02.jar ...
  4  net.minecraft.server.MinecraftServer.<init>(Ljava.io.File;Ljava.net.Proxy;Lnet.minecraft.util.datafix.DataFixer;Lcom.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;Lcom.mojang.authlib.minecraft.MinecraftSessionService;Lcom.mojang.authlib.GameProfileRepository;Lnet.minecraft.server.management.PlayerProfileCache;)V -> ()V  # UniversalTweaks-1.12.2-1.21.0.jar, lan-properties_forge-1.0.0+1.12-1.12.2.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar ...
  4  net.minecraftforge.fml.common.eventhandler.IEventExceptionHandler.handleException(Lnet.minecraftforge.fml.common.eventhandler.EventBus;Lnet.minecraftforge.fml.common.eventhandler.Event;[Lnet.minecraftforge.fml.common.eventhandler.IEventListener;ILjava.lang.Throwable;)V  # CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, armorpointspp-forge-1.12.2-3.1.4.jar, gnetum-1.4.3.jar ...
  3  net.minecraft.block.Block.getAiPathNodeType(Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.world.IBlockAccess;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.entity.EntityLiving;)Lnet.minecraft.pathfinding.PathNodeType;  # DoubleSlabs-1.12-0.12.2.jar, Fluidlogged-API-v3.3.3-mc1.12.2.jar, lepidodendron-1.12.2-63.02.jar
  3  net.minecraft.network.NetworkManager.createNetworkManagerAndConnect(Ljava.net.InetAddress;IZ)Lnet.minecraft.network.NetworkManager;  # EvergreenHUD-1.12.2-forge-2.1.7.jar, MineTogether-1.12.2-2.4.7.jar, viaforge-mc1122-4.4.0+4.jar
  3  net.minecraft.server.integrated.IntegratedServer.getNetworkSystem()Lnet.minecraft.network.NetworkSystem;  # EasyLAN-forge-1.12.2-v1.6a.jar, MineTogether-1.12.2-2.4.7.jar, UniversalTweaks-1.12.2-1.21.0.jar
  3  net.minecraft.world.gen.structure.StructureComponent.generateDoor(Lnet.minecraft.world.World;Lnet.minecraft.world.gen.structure.StructureBoundingBox;Ljava.util.Random;IIILnet.minecraft.util.EnumFacing;Lnet.minecraft.block.BlockDoor;)V  # HeroesExpansion-1.12.2-1.3.5.jar, iceandfire-1.9.1-1.12.2.jar, moreplanets-mc1.12.2-v2.4.1.jar
  3  net.minecraftforge.common.util.EnumHelper.addArt(Ljava.lang.String;Ljava.lang.String;IIII)Lnet.minecraft.entity.item.EntityPainting$EnumArt;  # JSON-Paintings-v1.5.0-mc1.12.2.jar, mcw-paintings-1.0.5-1.12.2forge.jar, moreplanets-mc1.12.2-v2.4.1.jar
  3  net.minecraftforge.fluids.BlockFluidClassic.getBlockLiquidHeight(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.block.material.Material;)F  # NTM-CE-1.12.2-2.6.1.0.jar, _NTM-Extended-1.12.2-3.0.3.jar, hbm-1.8.4a-G.jar
  3  net.minecraftforge.fluids.Fluid.<init>(Ljava.lang.String;Lnet.minecraft.util.ResourceLocation;Lnet.minecraft.util.ResourceLocation;Ljava.awt.Color;)V -> (Ljava.lang.String;Lnet.minecraft.util.ResourceLocation;Lnet.minecraft.util.ResourceLocation;)V  # NTM-CE-1.12.2-2.6.1.0.jar, _NTM-Extended-1.12.2-3.0.3.jar, hbm-1.8.4a-G.jar
  3  net.minecraftforge.fml.common.EnhancedRuntimeException$WrappedPrintStream.println(Ljava.lang.String;)V  # ModernSplash-1.12.2-1.5.3.jar, NoCubes-1.12.2-0.4.0-pre-2dca66a0.jar, TerrariaCraft-1.12.2-12.6.2.jar
  3  net.minecraftforge.fml.common.EnhancedRuntimeException.<init>(Ljava.lang.Throwable;)V  # ModernSplash-1.12.2-1.5.3.jar, NoCubes-1.12.2-0.4.0-pre-2dca66a0.jar, TerrariaCraft-1.12.2-12.6.2.jar
  3  net.minecraftforge.fml.common.FMLModContainer.<init>(Ljava.lang.String;Lnet.minecraftforge.fml.common.discovery.ModCandidate;Ljava.util.Map;)V -> ()V  # FVTM-4.12.116.jar, modularwarfare-2.4.2f.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  3  net.minecraftforge.fml.common.asm.transformers.AccessTransformer.<init>(Ljava.lang.String;)V  # Galacticraft-1.12.2-4.0.6.jar, _supermartijn642corelib-1.1.26b-forge-mc1.12.jar, fusion-1.3.16-forge-mc1.12.jar
  3  net.minecraftforge.fml.common.discovery.ContainerType.DIR:Lnet.minecraftforge.fml.common.discovery.ContainerType;  # FVTM-4.12.116.jar, modularwarfare-2.4.2f.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  3  net.minecraftforge.fml.common.discovery.ContainerType.JAR:Lnet.minecraftforge.fml.common.discovery.ContainerType;  # FVTM-4.12.116.jar, modularwarfare-2.4.2f.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  3  net.minecraftforge.fml.common.discovery.ModCandidate.<init>(Ljava.io.File;Ljava.io.File;Lnet.minecraftforge.fml.common.discovery.ContainerType;)V -> (Ljava.io.File;)V  # FVTM-4.12.116.jar, modularwarfare-2.4.2f.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  3  net.minecraftforge.fml.common.thread.SidedThreadGroups.SERVER:Lnet.minecraftforge.fml.common.thread.SidedThreadGroup;  # HammerLib-1.12.2-12.2.66.jar, MCT-ImmersiveTechnology-1.12.2-1.11.216-release.jar, TerrariaCraft-1.12.2-12.6.2.jar
  3  net.minecraftforge.fml.relauncher.libraries.Artifact.getFile()Ljava.io.File;  # !mixinbooter-12.3.jar, Sledgehammer-1.12.2-2.0.26.jar, `FermiumBooter-1.6.4.jar
  3  net.minecraftforge.fml.relauncher.libraries.LibraryManager.gatherLegacyCanidates(Ljava.io.File;)Ljava.util.List;  # !mixinbooter-12.3.jar, Sledgehammer-1.12.2-2.0.26.jar, `FermiumBooter-1.6.4.jar
  2  net.minecraft.advancements.CriteriaTriggers.BRED_ANIMALS:Lnet.minecraft.advancements.critereon.BredAnimalsTrigger;  # OceanicExpanse-1.2.2.jar, horse_colors-1.12.2-1.4.0.1.jar
  2  net.minecraft.advancements.critereon.BredAnimalsTrigger.trigger(Lnet.minecraft.entity.player.EntityPlayerMP;Lnet.minecraft.entity.passive.EntityAnimal;Lnet.minecraft.entity.passive.EntityAnimal;Lnet.minecraft.entity.EntityAgeable;)V  # OceanicExpanse-1.2.2.jar, horse_colors-1.12.2-1.4.0.1.jar
  2  net.minecraft.block.BlockFire.getEncouragement(Lnet.minecraft.block.Block;)I  # NarutoMod-1.12.2-0.3.1.beta.jar, _NTM-Extended-1.12.2-3.0.3.jar
  2  net.minecraft.block.BlockLiquid.getFlow(Lnet.minecraft.world.IBlockAccess;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.block.state.IBlockState;)Lnet.minecraft.util.math.Vec3d;  # Fluidlogged-API-v3.3.3-mc1.12.2.jar, NoCubes-1.12.2-0.4.0-pre-2dca66a0.jar
  2  net.minecraft.command.server.CommandBanPlayer.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar, e4mc-retro_forge-1.1.0+1.9-1.12.2.jar
  2  net.minecraft.command.server.CommandListBans.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar, e4mc-retro_forge-1.1.0+1.9-1.12.2.jar
  2  net.minecraft.command.server.CommandPardonPlayer.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar, e4mc-retro_forge-1.1.0+1.9-1.12.2.jar
  2  net.minecraft.command.server.CommandWhitelist.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar, e4mc-retro_forge-1.1.0+1.9-1.12.2.jar
  2  net.minecraft.creativetab.CreativeTabs.createIcon()Lnet.minecraft.item.ItemStack;  # TerrariaCraft-1.12.2-12.6.2.jar, tellme-1.12.2-0.7.0-dev.20200919.184303.jar
  2  net.minecraft.entity.Entity.nextStepDistance:I  # MoBends_1.12.2-2.0.0-alpha.3.jar, [[[valkyrien-skies-1.12.2-1.1.6.jar
  2  net.minecraft.entity.EntityCreature.blockUsingShield(Lnet.minecraft.entity.EntityLivingBase;)V  # NarutoMod-1.12.2-0.3.1.beta.jar, SCP Lockdown-1.12.2-2.4.3-release.jar
  2  net.minecraft.entity.ai.EntityAIWander.x:D  # NarutoMod-1.12.2-0.3.1.beta.jar, SCP Lockdown-1.12.2-2.4.3-release.jar
  2  net.minecraft.entity.ai.EntityAIWander.y:D  # NarutoMod-1.12.2-0.3.1.beta.jar, SCP Lockdown-1.12.2-2.4.3-release.jar
  2  net.minecraft.entity.ai.EntityAIWander.z:D  # NarutoMod-1.12.2-0.3.1.beta.jar, SCP Lockdown-1.12.2-2.4.3-release.jar
  2  net.minecraft.entity.ai.EntityAIZombieAttack.<init>(Lnet.minecraft.entity.monster.EntityZombie;DZ)V -> (Lnet.minecraft.entity.EntityCreature;DZ)V  # OceanicExpanse-1.2.2.jar, nuclearcraft-1.12.2-2.19a.jar
  2  net.minecraft.entity.player.EntityPlayer.PLAYER_MODEL_FLAG:Lnet.minecraft.network.datasync.DataParameter;  # bbs-2.4-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  net.minecraft.entity.player.EntityPlayerMP.<init>(Lnet.minecraft.server.MinecraftServer;Lnet.minecraft.world.WorldServer;Lcom.mojang.authlib.GameProfile;Lnet.minecraft.server.management.PlayerInteractionManager;)V -> (Lnet.minecraft.world.World;)V  # DoubleSlabs-1.12-0.12.2.jar, Galacticraft-1.12.2-4.0.6.jar
  2  net.minecraft.entity.player.EntityPlayerMP.clearInvulnerableDimensionChange()V  # DoubleSlabs-1.12-0.12.2.jar, NarutoMod-1.12.2-0.3.1.beta.jar
  2  net.minecraft.entity.player.EntityPlayerMP.getLastActiveTime()J  # DoubleSlabs-1.12-0.12.2.jar, NarutoMod-1.12.2-0.3.1.beta.jar
  2  net.minecraft.entity.player.EntityPlayerMP.handleFalling(DZ)V  # DoubleSlabs-1.12-0.12.2.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  2  net.minecraft.entity.player.EntityPlayerMP.lastExperience:I  # BrandonsCore-1.12.2-2.4.20.162-universal.jar, MoBends_1.12.2-2.0.0-alpha.3.jar
  2  net.minecraft.entity.player.EntityPlayerMP.lastFoodLevel:I  # BrandonsCore-1.12.2-2.4.20.162-universal.jar, MoBends_1.12.2-2.0.0-alpha.3.jar
  2  net.minecraft.entity.player.EntityPlayerMP.lastHealth:F  # BrandonsCore-1.12.2-2.4.20.162-universal.jar, MoBends_1.12.2-2.0.0-alpha.3.jar
  2  net.minecraft.entity.player.EntityPlayerMP.managedPosX:D  # CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, MoBends_1.12.2-2.0.0-alpha.3.jar
  2  net.minecraft.entity.player.EntityPlayerMP.managedPosZ:D  # CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, MoBends_1.12.2-2.0.0-alpha.3.jar
  2  net.minecraft.inventory.ContainerEnchantment.<init>(Lnet.minecraft.entity.player.InventoryPlayer;Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;)V  # ImprovableSkills-1.12.2-12.4.68.jar, NotEnoughItems-1.12.2-2.4.3.245-universal.jar
  2  net.minecraft.inventory.ContainerEnchantment.enchantClue:[I  # ImprovableSkills-1.12.2-12.4.68.jar, jsmacros-1.12.2-FORGE-1.8.3.jar
  2  net.minecraft.inventory.ContainerEnchantment.enchantLevels:[I  # ImprovableSkills-1.12.2-12.4.68.jar, jsmacros-1.12.2-FORGE-1.8.3.jar
  2  net.minecraft.item.Item.getHorseArmorTexture(Lnet.minecraft.entity.EntityLiving;Lnet.minecraft.item.ItemStack;)Ljava.lang.String;  # Call to Battle 3.jar, horse_colors-1.12.2-1.4.0.1.jar
  2  net.minecraft.item.ItemBanner.appendHoverTextFromTileEntityTag(Lnet.minecraft.item.ItemStack;Ljava.util.List;)V  # ConquestReforged-3.0.2-mc1.12.2.jar, [1.12.2]-Epic-Knights-Armor-and-Weapons-6.2v(uncomplete).jar
  2  net.minecraft.item.ItemMonsterPlacer.applyItemEntityDataToEntity(Lnet.minecraft.world.World;Lnet.minecraft.entity.player.EntityPlayer;Lnet.minecraft.item.ItemStack;Lnet.minecraft.entity.Entity;)V  # SimpleHats-0.4.1.jar, champions-1.12.2-1.0.11.10.jar
  2  net.minecraft.launchwrapper.IClassNameTransformer.unmapClassName(Ljava.lang.String;)Ljava.lang.String;  # !mixinbooter-12.3.jar, vintagefix-0.7.2.jar
  2  net.minecraft.network.NetHandlerPlayServer.update()V  # Call to Battle 3.jar, Call to Battle Vehicles.jar
  2  net.minecraft.network.NetworkSystem.addEndpoint(Ljava.net.InetAddress;I)V  # MineTogether-1.12.2-2.4.7.jar, UniversalTweaks-1.12.2-1.21.0.jar
  2  net.minecraft.network.play.server.SPacketCollectItem.getCollectedItemEntityID()I  # Mantle-0.11.0+1.12.2-forge.jar, jsmacros-1.12.2-FORGE-1.8.3.jar
  2  net.minecraft.network.play.server.SPacketCollectItem.getEntityID()I  # Mantle-0.11.0+1.12.2-forge.jar, jsmacros-1.12.2-FORGE-1.8.3.jar
  2  net.minecraft.network.play.server.SPacketEntityMetadata.getDataManagerEntries()Ljava.util.List;  # Mantle-0.11.0+1.12.2-forge.jar, Wynntils-MC1.12.2-v1.15.1-beta.10.jar
  2  net.minecraft.network.play.server.SPacketEntityMetadata.getEntityId()I  # Mantle-0.11.0+1.12.2-forge.jar, Wynntils-MC1.12.2-v1.15.1-beta.10.jar
  2  net.minecraft.network.play.server.SPacketPlayerListHeaderFooter.getFooter()Lnet.minecraft.util.text.ITextComponent;  # Wynntils-MC1.12.2-v1.15.1-beta.10.jar, minihud-forge-1.12.2-0.19.0-dev.20210503.192331.jar
  2  net.minecraft.network.play.server.SPacketSpawnPosition.getSpawnPos()Lnet.minecraft.util.math.BlockPos;  # Wynntils-MC1.12.2-v1.15.1-beta.10.jar, minihud-forge-1.12.2-0.19.0-dev.20210503.192331.jar
  2  net.minecraft.network.play.server.SPacketTimeUpdate.getTotalWorldTime()J  # jsmacros-1.12.2-FORGE-1.8.3.jar, minihud-forge-1.12.2-0.19.0-dev.20210503.192331.jar
  2  net.minecraft.network.play.server.SPacketUpdateBossInfo.getOperation()Lnet.minecraft.network.play.server.SPacketUpdateBossInfo$Operation;  # Wynntils-MC1.12.2-v1.15.1-beta.10.jar, jsmacros-1.12.2-FORGE-1.8.3.jar
  2  net.minecraft.network.play.server.SPacketUpdateBossInfo.getUniqueId()Ljava.util.UUID;  # Wynntils-MC1.12.2-v1.15.1-beta.10.jar, jsmacros-1.12.2-FORGE-1.8.3.jar
  2  net.minecraft.server.MinecraftServer.setBuildLimit(I)V  # CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, lan-properties_forge-1.0.0+1.12-1.12.2.jar
  2  net.minecraft.server.MinecraftServer.setCanSpawnAnimals(Z)V  # EasyLAN-forge-1.12.2-v1.6a.jar, lan-properties_forge-1.0.0+1.12-1.12.2.jar
  2  net.minecraft.server.MinecraftServer.setCanSpawnNPCs(Z)V  # EasyLAN-forge-1.12.2-v1.6a.jar, lan-properties_forge-1.0.0+1.12-1.12.2.jar
  2  net.minecraft.server.MinecraftServer.setMOTD(Ljava.lang.String;)V  # EasyLAN-forge-1.12.2-v1.6a.jar, lan-properties_forge-1.0.0+1.12-1.12.2.jar
  2  net.minecraft.server.dedicated.DedicatedServer.getMaxTickTime()J  # MineTogether-1.12.2-2.4.7.jar, industrialcraft-2-2.8.222-ex112.jar
  2  net.minecraft.server.integrated.IntegratedServer.getGameType()Lnet.minecraft.world.GameType;  # AdvancedRocketry-1.12.2-2.1.9-2.jar, EasyLAN-forge-1.12.2-v1.6a.jar
  2  net.minecraft.server.integrated.IntegratedServer.setCanSpawnAnimals(Z)V  # Cloudflared-1.12.2-12.2.3.jar, UniversalTweaks-1.12.2-1.21.0.jar
  2  net.minecraft.server.integrated.IntegratedServer.setCanSpawnNPCs(Z)V  # Cloudflared-1.12.2-12.2.3.jar, UniversalTweaks-1.12.2-1.21.0.jar
  2  net.minecraft.util.EnumFacing.getFront(I)Lnet.minecraft.util.EnumFacing;  # Chameleon-1.12.2-4.1.3.jar, hbm-1.8.4a-G.jar
  2  net.minecraft.util.IStringSerializable.ADA_BIG:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.ADA_FLAT:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.PRI_BIG:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.PRI_FLAT:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.PURE_BIG:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.PURE_FLAT:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.SIM_BIG:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.SIM_FLAT:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.village.VillageCollection.markDirty()V  # AsmodeusCore-1.12.2-1.0.5.jar, Galacticraft-1.12.2-4.0.6.jar
  2  net.minecraft.world.ChunkCache.chunkArray:[[Lnet.minecraft.world.chunk.Chunk;  # NoCubes-1.12.2-0.4.0-pre-2dca66a0.jar, [[[valkyrien-skies-1.12.2-1.1.6.jar
  2  net.minecraft.world.World.calculateInitialWeather()V  # UniversalTweaks-1.12.2-1.21.0.jar, modularui-3.1.6.jar
  2  net.minecraft.world.World.processingLoadedTiles:Z  # Fluidlogged-API-v3.3.3-mc1.12.2.jar, RenderLib-1.12.2-1.4.5.jar
  2  net.minecraft.world.World.scheduledUpdatesAreImmediate:Z  # _NTM-Extended-1.12.2-3.0.3.jar, hbm-1.8.4a-G.jar
  2  net.minecraft.world.WorldProvider.getLightmapColors(FFFF[F)V  # JurassicReborn-1.12.2-1.3.4.jar, 永爱之刃-v11削弱.jar
  2  net.minecraft.world.WorldServer.lootTable:Lnet.minecraft.world.storage.loot.LootTableManager;  # AdvancedRocketry-1.12.2-2.1.9-2.jar, industrialcraft-2-2.8.222-ex112.jar
  2  net.minecraft.world.gen.structure.StructureVillagePieces$Village.biomeDoor()Lnet.minecraft.block.BlockDoor;  # HeroesExpansion-1.12.2-1.3.5.jar, JurassicReborn-1.12.2-1.3.4.jar
  2  net.minecraft.world.gen.structure.StructureVillagePieces$Village.generateDoor(Lnet.minecraft.world.World;Lnet.minecraft.world.gen.structure.StructureBoundingBox;Ljava.util.Random;IIILnet.minecraft.util.EnumFacing;Lnet.minecraft.block.BlockDoor;)V  # HeroesExpansion-1.12.2-1.3.5.jar, ImmersiveEngineering-0.12-98.jar
  2  net.minecraft.world.storage.WorldSavedData.get(Lnet.minecraft.world.World;)Lcom.dhanantry.scapeandrunparasites.world.SRPSaveData;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.world.storage.WorldSavedData.setTotalKills(IIZLnet.minecraft.world.World;Z)Z  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraftforge.advancements.critereon.ItemPredicates.register(Lnet.minecraft.util.ResourceLocation;Ljava.util.function.Function;)V  # TLS-2.3.2-1.12.2.jar, rustic-1.2.0.jar
  2  net.minecraftforge.common.ForgeChunkManager$ForceChunkEvent.getLocation()Lnet.minecraft.util.math.ChunkPos;  # ChickenChunks-1.12.2-2.4.2.74-universal.jar, CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar
  2  net.minecraftforge.common.ForgeChunkManager$ForceChunkEvent.getTicket()Lnet.minecraftforge.common.ForgeChunkManager$Ticket;  # ChickenChunks-1.12.2-2.4.2.74-universal.jar, CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar
  2  net.minecraftforge.common.ForgeChunkManager$UnforceChunkEvent.getLocation()Lnet.minecraft.util.math.ChunkPos;  # ChickenChunks-1.12.2-2.4.2.74-universal.jar, CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar
  2  net.minecraftforge.common.ForgeChunkManager$UnforceChunkEvent.getTicket()Lnet.minecraftforge.common.ForgeChunkManager$Ticket;  # ChickenChunks-1.12.2-2.4.2.74-universal.jar, CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar
  2  net.minecraftforge.common.ForgeModContainer.zombieBabyChance:F  # OceanicExpanse-1.2.2.jar, moreplanets-mc1.12.2-v2.4.1.jar
  2  net.minecraftforge.common.util.EnumHelper.addHorseArmor(Ljava.lang.String;Ljava.lang.String;I)Lnet.minecraft.entity.passive.HorseArmorType;  # Atlas-Lib-1.12.2-1.1.11.jar, [1.12.2]-Epic-Knights-Armor-and-Weapons-6.2v(uncomplete).jar
  2  net.minecraftforge.event.ForgeEventFactory.onItemUseStart(Lnet.minecraft.entity.EntityLivingBase;Lnet.minecraft.item.ItemStack;I)I  # FeederHelmet-14.0.0.jar, HeroesExpansion-1.12.2-1.3.5.jar
  2  net.minecraftforge.event.RegistryEvent$Register.<init>(Lnet.minecraft.util.ResourceLocation;Lnet.minecraftforge.registries.IForgeRegistry;)V -> (Lnet.minecraftforge.registries.IForgeRegistry;)V  # Dimensional Doors-3.2.3.jar, HammerLib-1.12.2-12.2.66.jar
  2  net.minecraftforge.fluids.BlockFluidClassic.blockMaterial:Lnet.minecraft.block.material.Material;  # hbm-1.8.4a-G.jar, mahoutsukai-1.12.2-v1.19.54.jar
  2  net.minecraftforge.fluids.BlockFluidClassic.getFilledPercentage(Lnet.minecraft.world.IBlockAccess;Lnet.minecraft.util.math.BlockPos;)F  # IndustrialUpgrade-1.12.2-3.3.0.43.jar, _NTM-Extended-1.12.2-3.0.3.jar
  2  net.minecraftforge.fml.common.FMLCommonHandler.processWindowMessages()V  # Cloudflared-1.12.2-12.2.3.jar, TerrariaCraft-1.12.2-12.6.2.jar
  2  net.minecraftforge.fml.common.FMLCommonHandler.stripSpecialChars(Ljava.lang.String;)Ljava.lang.String;  # Cloudflared-1.12.2-12.2.3.jar, TerrariaCraft-1.12.2-12.6.2.jar
  2  net.minecraftforge.fml.common.IFMLSidedHandler.getSide()Lnet.minecraftforge.fml.relauncher.Side;  # CarbonConfig-1.12.2-2.0.2.1.jar, Future-MC-0.2.21.jar
  2  net.minecraftforge.fml.common.ModContainerFactory.build(Lnet.minecraftforge.fml.common.discovery.asm.ASMModParser;Ljava.io.File;Lnet.minecraftforge.fml.common.discovery.ModCandidate;)Lnet.minecraftforge.fml.common.ModContainer;  # PolyPatcher-1.12.2-forge-1.10.4.jar, vintagefix-0.7.2.jar
  2  net.minecraftforge.fml.common.ModMetadata.requiredMods:Ljava.util.Set;  # allarrowsinfinityfix-1.8-forge-mc1.12.2.jar, fermiumasm-5.31.jar
  2  net.minecraftforge.fml.common.ModMetadata.updateJSON:Ljava.lang.String;  # YARCF-0.15-b2(1.12.2).jar, txloader-1.12.2-1.5.1.jar
  2  net.minecraftforge.fml.common.asm.FMLSanityChecker.fmlLocation:Ljava.io.File;  # ModernSplash-1.12.2-1.5.3.jar, TerrariaCraft-1.12.2-12.6.2.jar
  2  net.minecraftforge.fml.common.discovery.asm.ASMModParser.<init>(Ljava.io.InputStream;)V  # PolyPatcher-1.12.2-forge-1.10.4.jar, vintagefix-0.7.2.jar
  2  net.minecraftforge.fml.common.discovery.asm.ASMModParser.getASMType()Lorg.objectweb.asm.Type;  # PolyPatcher-1.12.2-forge-1.10.4.jar, vintagefix-0.7.2.jar
  2  net.minecraftforge.fml.common.discovery.asm.ASMModParser.getAnnotations()Ljava.util.LinkedList;  # PolyPatcher-1.12.2-forge-1.10.4.jar, vintagefix-0.7.2.jar
  2  net.minecraftforge.fml.common.discovery.asm.ASMModParser.getClassVersion()I  # PolyPatcher-1.12.2-forge-1.10.4.jar, vintagefix-0.7.2.jar
  2  net.minecraftforge.fml.common.discovery.asm.ModAnnotation.getASMType()Lorg.objectweb.asm.Type;  # PolyPatcher-1.12.2-forge-1.10.4.jar, vintagefix-0.7.2.jar
  2  net.minecraftforge.fml.common.discovery.asm.ModAnnotation.getValues()Ljava.util.Map;  # PolyPatcher-1.12.2-forge-1.10.4.jar, vintagefix-0.7.2.jar
  2  net.minecraftforge.fml.common.network.NetworkRegistry.registry()Ljava.util.Map;  # CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, Pixelmon-1.12.2-8.4.3-universal.jar
  2  net.minecraftforge.fml.relauncher.CoreModManager.getTransformers()Ljava.util.Map;  # PolySprint-1.12.2-forge-1.0.2.jar, Sledgehammer-1.12.2-2.0.26.jar
  2  net.minecraftforge.fml.relauncher.FMLRelaunchLog.info(Ljava.lang.String;[Ljava.lang.Object;)V  # 131-CNPC-Gecko-Addon-1.12.2-(05Jul20_03Feb22)-1.1.1.jar, AdvancedRocketry-1.12.2-2.1.9-2.jar
  2  net.minecraftforge.fml.relauncher.libraries.LibraryManager.flattenLists(Ljava.io.File;)Ljava.util.List;  # !mixinbooter-12.3.jar, `FermiumBooter-1.6.4.jar
  2  net.minecraftforge.fml.relauncher.libraries.Repository.resolveAll(Lnet.minecraftforge.fml.relauncher.libraries.Artifact;)Lnet.minecraftforge.fml.relauncher.libraries.Artifact;  # !mixinbooter-12.3.jar, `FermiumBooter-1.6.4.jar
  1  net.minecraft.advancements.Advancement$Builder.build(Lnet.minecraft.util.ResourceLocation;)Lnet.minecraft.advancements.Advancement;  # Wynntils-MC1.12.2-v1.15.1-beta.10.jar
  1  net.minecraft.advancements.CriteriaTriggers.ENTITY_HURT_PLAYER:Lnet.minecraft.advancements.critereon.EntityHurtPlayerTrigger;  # techguns-2.2.0.1.jar
  1  net.minecraft.advancements.CriteriaTriggers.PLAYER_HURT_ENTITY:Lnet.minecraft.advancements.critereon.PlayerHurtEntityTrigger;  # techguns-2.2.0.1.jar
  1  net.minecraft.advancements.critereon.EntityHurtPlayerTrigger.trigger(Lnet.minecraft.entity.player.EntityPlayerMP;Lnet.minecraft.util.DamageSource;FFZ)V  # techguns-2.2.0.1.jar
  1  net.minecraft.advancements.critereon.ImpossibleTrigger$Instance.<init>()V  # Cherry_on-1.12.2-1.3.35.jar
  1  net.minecraft.advancements.critereon.PlayerHurtEntityTrigger.trigger(Lnet.minecraft.entity.player.EntityPlayerMP;Lnet.minecraft.entity.Entity;Lnet.minecraft.util.DamageSource;FFZ)V  # techguns-2.2.0.1.jar
  1  net.minecraft.block.Block.getStateAtViewpoint(Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.world.IBlockAccess;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.util.math.Vec3d;)Lnet.minecraft.block.state.IBlockState;  # Fluidlogged-API-v3.3.3-mc1.12.2.jar
  1  net.minecraft.block.BlockChorusFlower.canSurvive(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;)Z  # simplytea-1.6.jar
  1  net.minecraft.block.BlockDragonEgg.<init>()V  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.block.BlockFire.canCatchFire(Lnet.minecraft.world.IBlockAccess;Lnet.minecraft.util.math.BlockPos;)Z  # Fluidlogged-API-v3.3.3-mc1.12.2.jar
  1  net.minecraft.block.BlockFire.getFlammability(Lnet.minecraft.block.Block;)I  # NarutoMod-1.12.2-0.3.1.beta.jar
  1  net.minecraft.block.BlockSnowBlock.<init>()V  # lepidodendron-1.12.2-63.02.jar
  1  net.minecraft.command.CommandHelp.<init>()V  # UniversalTweaks-1.12.2-1.21.0.jar
  1  net.minecraft.command.CommandLocate.<init>()V  # LucraftCore-1.12.2-2.4.17.jar
  1  net.minecraft.command.CommandServerKick.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar
  1  net.minecraft.command.server.CommandBanIp.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar
  1  net.minecraft.command.server.CommandDeOp.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar
  1  net.minecraft.command.server.CommandOp.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar
  1  net.minecraft.command.server.CommandPardonIp.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar
  1  net.minecraft.command.server.CommandSaveAll.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar
  1  net.minecraft.command.server.CommandSaveOff.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar
  1  net.minecraft.command.server.CommandSaveOn.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar
  1  net.minecraft.enchantment.EnchantmentFireAspect.<init>(Lnet.minecraft.enchantment.Enchantment$Rarity;[Lnet.minecraft.inventory.EntityEquipmentSlot;)V  # Inspirations-1.12.2-0.2.9.jar
  1  net.minecraft.enchantment.EnchantmentKnockback.<init>(Lnet.minecraft.enchantment.Enchantment$Rarity;[Lnet.minecraft.inventory.EntityEquipmentSlot;)V  # Inspirations-1.12.2-0.2.9.jar
  1  net.minecraft.entity.Entity.boundingBox:Lnet.minecraft.util.math.AxisAlignedBB;  # [[[valkyrien-skies-1.12.2-1.1.6.jar
  1  net.minecraft.entity.Entity.entityId:I  # MoBends_1.12.2-2.0.0-alpha.3.jar
  1  net.minecraft.entity.Entity.getOwner()Lnet.minecraft.entity.EntityLivingBase;  # srpcotesia-1.12.2-1.3.4h1.jar
```

## client-side API (top 150 of 551)

```
 10  net.minecraft.client.renderer.chunk.RenderChunk.getPosition()Lnet.minecraft.util.math.BlockPos;  # CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar ...
  8  net.minecraft.client.renderer.chunk.RenderChunk.boundingBox:Lnet.minecraft.util.math.AxisAlignedBB;  # NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, Sledgehammer-1.12.2-2.0.26.jar ...
  7  net.minecraft.client.renderer.chunk.RenderChunk.getCompiledChunk()Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar ...
  6  net.minecraft.client.Minecraft.getTutorial()Lnet.minecraft.client.tutorial.Tutorial;  # Call to Battle 3.jar, JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar ...
  6  net.minecraft.client.renderer.chunk.RenderChunk.compiledChunk:Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, _NTM-Extended-1.12.2-3.0.3.jar ...
  5  net.minecraft.client.renderer.chunk.CompiledChunk.getTileEntities()Ljava.util.List;  # NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, _NTM-Extended-1.12.2-3.0.3.jar ...
  5  net.minecraft.client.renderer.chunk.CompiledChunk.isLayerEmpty(Lnet.minecraft.util.BlockRenderLayer;)Z  # NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, _NTM-Extended-1.12.2-3.0.3.jar ...
  5  net.minecraft.client.renderer.chunk.CompiledChunk.isLayerStarted(Lnet.minecraft.util.BlockRenderLayer;)Z  # Fluidlogged-API-v3.3.3-mc1.12.2.jar, NoCubes-1.12.2-0.4.0-pre-2dca66a0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar ...
  5  net.minecraft.client.renderer.chunk.RenderChunk.getVertexBufferByLayer(I)Lnet.minecraft.client.renderer.vertex.VertexBuffer;  # NTM-CE-1.12.2-2.6.1.0.jar, _NTM-Extended-1.12.2-3.0.3.jar, hbm-1.8.4a-G.jar ...
  5  net.minecraft.client.settings.GameSettings.debugCamEnable:Z  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, [[[valkyrien-skies-1.12.2-1.1.6.jar ...
  5  net.minecraft.realms.RealmsBridge.switchToRealms(Lnet.minecraft.client.gui.GuiScreen;)V  # GalaxySpace-1.12.2-2.2.0.jar, Wynntils-MC1.12.2-v1.15.1-beta.10.jar, aether-1.12.2-v1.5.4.1.jar ...
  5  paulscode.sound.SoundSystem.play(Ljava.lang.String;)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, Pixelmon-1.12.2-8.4.3-universal.jar ...
  4  net.minecraft.client.entity.EntityPlayerSP.sprintingTicksLeft:I  # Galacticraft-1.12.2-4.0.6.jar, MoBends_1.12.2-2.0.0-alpha.3.jar, UniversalTweaks-1.12.2-1.21.0.jar ...
  4  net.minecraft.client.gui.GuiIngame.renderGameOverlay(F)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, PolyPatcher-1.12.2-forge-1.10.4.jar ...
  4  net.minecraft.client.gui.GuiIngameMenu.<init>()V  # EasyLAN-forge-1.12.2-v1.6a.jar, LoTAS1.12.2-2.1.3.jar, Wynntils-MC1.12.2-v1.15.1-beta.10.jar ...
  4  net.minecraft.client.gui.GuiNewChat.getChatComponent(II)Lnet.minecraft.util.text.ITextComponent;  # CustomCursorMod-1.2.2 1.12.2.jar, MineTogether-1.12.2-2.4.7.jar, PolyPatcher-1.12.2-forge-1.10.4.jar ...
  4  net.minecraft.client.gui.GuiShareToLan.<init>(Lnet.minecraft.client.gui.GuiScreen;)V  # EasyLAN-forge-1.12.2-v1.6a.jar, MineTogether-1.12.2-2.4.7.jar, music_player-forge-1.12.2-2.1.4.375.jar ...
  4  net.minecraft.client.renderer.ItemRenderer.renderOverlays(F)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, modularwarfare-2.4.2f.jar ...
  4  net.minecraft.client.renderer.RenderGlobal.createBindEntityOutlineFbs(II)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, bbs-fs-2.8-1.12.2-alpha.1.jar ...
  4  net.minecraft.client.renderer.RenderGlobal.setWorldAndLoadRenderers(Lnet.minecraft.client.multiplayer.WorldClient;)V  # Call to Battle 3.jar, CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, modularwarfare-2.4.2f.jar ...
  4  net.minecraft.client.renderer.chunk.CompiledChunk.DUMMY:Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar, fermiumasm-5.31.jar ...
  4  net.minecraft.client.renderer.chunk.RenderChunk.clearNeedsUpdate()V  # SCP Lockdown-1.12.2-2.4.3-release.jar, bbs-2.4-1.12.2.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar ...
  4  net.minecraft.client.renderer.chunk.RenderChunk.getWorld()Lnet.minecraft.world.World;  # CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, Sledgehammer-1.12.2-2.0.26.jar, fermiumasm-5.31.jar ...
  4  net.minecraft.client.renderer.chunk.RenderChunk.needsUpdate()Z  # SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar, bbs-2.4-1.12.2.jar ...
  4  net.minecraft.client.renderer.chunk.RenderChunk.setNeedsUpdate(Z)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar ...
  4  net.minecraft.client.renderer.chunk.RenderChunk.setPosition(III)V  # CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar ...
  4  net.minecraft.client.resources.ResourcePackFileNotFoundException.<init>(Ljava.io.File;Ljava.lang.String;)V  # JSON-Paintings-v1.5.0-mc1.12.2.jar, industrialcraft-2-2.8.222-ex112.jar, modularwarfare-2.4.2f.jar ...
  4  net.minecraft.client.settings.GameSettings.resourcePacks:Ljava.util.List;  # I18nUpdateMod-NLTS-Forge-0.1.1beta2-1.12.2.jar, OptionsEnforcer-1.12.2-1.4.1.jar, RPMTW-Update-Mod-1.12-1.3.0.jar ...
  4  net.minecraft.client.tutorial.Tutorial.handleMouse(Lnet.minecraft.util.MouseHelper;)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar ...
  4  paulscode.sound.SoundSystem.newSource(ZLjava.lang.String;Ljava.net.URL;Ljava.lang.String;ZFFFIF)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar ...
  4  paulscode.sound.SoundSystem.pause(Ljava.lang.String;)V  # IC2Classic-1.12.2-1.5.15.jar, Pixelmon-1.12.2-8.4.3-universal.jar, industrialcraft-2-2.8.222-ex112.jar ...
  4  paulscode.sound.SoundSystem.setPitch(Ljava.lang.String;F)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar ...
  4  paulscode.sound.SoundSystem.setVolume(Ljava.lang.String;F)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar ...
  4  paulscode.sound.SoundSystem.stop(Ljava.lang.String;)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar ...
  3  net.minecraft.client.entity.EntityPlayerSP.setPermissionLevel(I)V  # MineTogether-1.12.2-2.4.7.jar, UniversalTweaks-1.12.2-1.21.0.jar, bbs-2.4-1.12.2.jar
  3  net.minecraft.client.gui.GuiScreen.handleComponentHover(Lnet.minecraft.util.text.ITextComponent;II)V  # MineTogether-1.12.2-2.4.7.jar, TerrariaCraft-1.12.2-12.6.2.jar, 永爱之刃-v11削弱.jar
  3  net.minecraft.client.model.ModelEnderman.<init>(F)V  # ancientwarfare-1.12.2-2.10.2.jar, defiledlands-1.12.2-1.4.3.jar, techguns-2.2.0.1.jar
  3  net.minecraft.client.multiplayer.ServerAddress.fromString(Ljava.lang.String;)Lnet.minecraft.client.multiplayer.ServerAddress;  # Cloudflared-1.12.2-12.2.3.jar, EvergreenHUD-1.12.2-forge-2.1.7.jar, jsmacros-1.12.2-FORGE-1.8.3.jar
  3  net.minecraft.client.renderer.ChunkRenderContainer.initialize(DDD)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar
  3  net.minecraft.client.renderer.RenderGlobal.renderEntityOutlineFramebuffer()V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraft.client.renderer.RenderGlobal.viewFrustum:Lnet.minecraft.client.renderer.ViewFrustum;  # Inspirations-1.12.2-0.2.9.jar, NTM-CE-1.12.2-2.6.1.0.jar, NoCubes-1.12.2-0.4.0-pre-2dca66a0.jar
  3  net.minecraft.client.renderer.block.model.ModelBakery.<init>(Lnet.minecraft.client.resources.IResourceManager;Lnet.minecraft.client.renderer.texture.TextureMap;Lnet.minecraft.client.renderer.BlockModelShapes;)V -> ()V  # Mantle-0.11.0+1.12.2-forge.jar, RLFoliage-MC1.12-2.5.4.jar, TerrariaCraft-1.12.2-12.6.2.jar
  3  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.runChunkUploads(J)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar, bbs-2.4-1.12.2.jar, replaymod-1.12.2-2.6.24.jar
  3  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.updateChunkNow(Lnet.minecraft.client.renderer.chunk.RenderChunk;)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar, bbs-2.4-1.12.2.jar, replaymod-1.12.2-2.6.24.jar
  3  net.minecraft.client.renderer.chunk.CompiledChunk.isEmpty()Z  # SCP Lockdown-1.12.2-2.4.3-release.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar, replaymod-1.12.2-2.6.24.jar
  3  net.minecraft.client.renderer.chunk.CompiledChunk.setLayerStarted(Lnet.minecraft.util.BlockRenderLayer;)V  # Fluidlogged-API-v3.3.3-mc1.12.2.jar, NoCubes-1.12.2-0.4.0-pre-2dca66a0.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar
  3  net.minecraft.client.renderer.chunk.RenderChunk.deleteGlResources()V  # SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar
  3  net.minecraft.client.renderer.chunk.RenderChunk.multModelviewMatrix()V  # NTM-CE-1.12.2-2.6.1.0.jar, _NTM-Extended-1.12.2-3.0.3.jar, hbm-1.8.4a-G.jar
  3  net.minecraft.client.renderer.chunk.RenderChunk.renderChunksUpdated:I  # Bookshelf-1.12.2-2.3.590.jar, PolyPatcher-1.12.2-forge-1.10.4.jar, minihud-forge-1.12.2-0.19.0-dev.20210503.192331.jar
  3  net.minecraft.client.tutorial.Tutorial.openInventory()V  # modularwarfare-2.4.2f.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar, v1.12.2-v3.2.9.jar
  3  net.minecraft.util.MouseFilter.<init>()V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraft.util.MouseFilter.smooth(FF)F  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraftforge.client.ForgeHooksClient.getFogDensity(Lnet.minecraft.client.renderer.EntityRenderer;Lnet.minecraft.entity.Entity;Lnet.minecraft.block.state.IBlockState;FF)F  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraftforge.client.ForgeHooksClient.loadEntityShader(Lnet.minecraft.entity.Entity;Lnet.minecraft.client.renderer.EntityRenderer;)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraftforge.client.ForgeHooksClient.onDrawBlockHighlight(Lnet.minecraft.client.renderer.RenderGlobal;Lnet.minecraft.entity.player.EntityPlayer;Lnet.minecraft.util.math.RayTraceResult;IF)Z  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraftforge.client.ForgeHooksClient.onFogRender(Lnet.minecraft.client.renderer.EntityRenderer;Lnet.minecraft.entity.Entity;Lnet.minecraft.block.state.IBlockState;FIF)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraftforge.fml.client.FMLClientHandler.addModAsResource(Lnet.minecraftforge.fml.common.ModContainer;)V  # FVTM-4.12.116.jar, modularwarfare-2.4.2f.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  3  org.lwjgl.opengl.Display.getDrawable()Lorg.lwjgl.opengl.Drawable;  # DistantHorizons-3.3.3-1.12.2-forge.jar, ModernSplash-1.12.2-1.5.3.jar, TerrariaCraft-1.12.2-12.6.2.jar
  3  paulscode.sound.SoundBuffer.audioData:[B  # Matmos_7.0_1.12.2.jar, bbs-2.4-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  3  paulscode.sound.SoundSystem.removeSource(Ljava.lang.String;)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  3  paulscode.sound.SoundSystem.setPosition(Ljava.lang.String;FFF)V  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar, matmos-1.12.2-36.0.1.jar
  2  net.minecraft.client.Minecraft.drawSplashScreen(Lnet.minecraft.client.renderer.texture.TextureManager;)V  # ModernSplash-1.12.2-1.5.3.jar, TerrariaCraft-1.12.2-12.6.2.jar
  2  net.minecraft.client.audio.SoundManager.reloadSoundSystem()V  # Surge-1.12.2-2.0.79.jar, UniversalTweaks-1.12.2-1.21.0.jar
  2  net.minecraft.client.entity.AbstractClientPlayer.rotateElytraX:F  # MoBends_1.12.2-2.0.0-alpha.3.jar, Wynntils-MC1.12.2-v1.15.1-beta.10.jar
  2  net.minecraft.client.entity.AbstractClientPlayer.rotateElytraY:F  # MoBends_1.12.2-2.0.0-alpha.3.jar, Wynntils-MC1.12.2-v1.15.1-beta.10.jar
  2  net.minecraft.client.entity.AbstractClientPlayer.rotateElytraZ:F  # MoBends_1.12.2-2.0.0-alpha.3.jar, Wynntils-MC1.12.2-v1.15.1-beta.10.jar
  2  net.minecraft.client.gui.GuiChat.handleComponentHover(Lnet.minecraft.util.text.ITextComponent;II)V  # MineTogether-1.12.2-2.4.7.jar, Wynntils-MC1.12.2-v1.15.1-beta.10.jar
  2  net.minecraft.client.gui.GuiNewChat.chatLines:Ljava.util.List;  # HammerLib-1.12.2-12.2.66.jar, TouchController-0.2.1-beta15+forge-1.12.2.jar
  2  net.minecraft.client.gui.GuiPlayerTabOverlay.getPlayerName(Lnet.minecraft.client.network.NetworkPlayerInfo;)Ljava.lang.String;  # BetterPingDisplay-1.12.2-1.0.jar, LibrarianLib-Continuous-1.12.2-4.22-2.0-3.jar
  2  net.minecraft.client.gui.inventory.GuiContainer.updateDragSplitting()V  # BrandonsCore-1.12.2-2.4.20.162-universal.jar, HammerLib-1.12.2-12.2.66.jar
  2  net.minecraft.client.model.ModelHumanoidHead.head:Lnet.minecraft.client.model.ModelRenderer;  # CustomPlayerModels-1.12.2-0.6.27a.jar, MoBends_1.12.2-2.0.0-alpha.3.jar
  2  net.minecraft.client.model.ModelShulkerBullet.<init>()V  # champions-1.12.2-1.0.11.10.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.client.model.ModelSquid.<init>()V  # OceanicExpanse-1.2.2.jar, moreplanets-mc1.12.2-v2.4.1.jar
  2  net.minecraft.client.multiplayer.ServerAddress.getIP()Ljava.lang.String;  # EvergreenHUD-1.12.2-forge-2.1.7.jar, jsmacros-1.12.2-FORGE-1.8.3.jar
  2  net.minecraft.client.multiplayer.ServerAddress.getPort()I  # EvergreenHUD-1.12.2-forge-2.1.7.jar, jsmacros-1.12.2-FORGE-1.8.3.jar
  2  net.minecraft.client.network.NetHandlerLoginClient.<init>(Lnet.minecraft.network.NetworkManager;Lnet.minecraft.client.Minecraft;Lnet.minecraft.client.gui.GuiScreen;)V  # MineTogether-1.12.2-2.4.7.jar, replaymod-1.12.2-2.6.24.jar
  2  net.minecraft.client.network.NetworkPlayerInfo.getPlayerTeam()Lnet.minecraft.scoreboard.ScorePlayerTeam;  # BetterPingDisplay-1.12.2-1.0.jar, LibrarianLib-Continuous-1.12.2-4.22-2.0-3.jar
  2  net.minecraft.client.network.NetworkPlayerInfo.skinType:Ljava.lang.String;  # HammerLib-1.12.2-12.2.66.jar, RenderLib-1.12.2-1.4.5.jar
  2  net.minecraft.client.renderer.ActiveRenderInfo.updateRenderInfo(Lnet.minecraft.entity.Entity;Z)V  # JurassicReborn-1.12.2-1.3.4.jar, 永爱之刃-v11削弱.jar
  2  net.minecraft.client.renderer.BufferBuilder.putColorRGBA(IIII)V  # Call to Battle 3.jar, SilentLib-1.12.2-3.0.14+168.jar
  2  net.minecraft.client.renderer.ChunkRenderContainer.<init>()V  # CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar
  2  net.minecraft.client.renderer.ChunkRenderContainer.addRenderChunk(Lnet.minecraft.client.renderer.chunk.RenderChunk;Lnet.minecraft.util.BlockRenderLayer;)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar
  2  net.minecraft.client.renderer.ChunkRenderContainer.renderChunkLayer(Lnet.minecraft.util.BlockRenderLayer;)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar
  2  net.minecraft.client.renderer.EntityRenderer.farPlaneDistance:F  # Modern-Warfare-Cubed-0.1.9.jar, TouchController-0.2.1-beta15+forge-1.12.2.jar
  2  net.minecraft.client.renderer.EntityRenderer.getFOVModifier(FZ)F  # Modern-Warfare-Cubed-0.1.9.jar, TouchController-0.2.1-beta15+forge-1.12.2.jar
  2  net.minecraft.client.renderer.EntityRenderer.pointedEntity:Lnet.minecraft.entity.Entity;  # Draconic-Evolution-1.12.2-2.3.28.354-universal.jar, ReachFix-1.12.2-1.1.3.jar
  2  net.minecraft.client.renderer.RegionRenderCacheBuilder.<init>()V  # Nothirium-1.12.2-0.4.9-beta.jar, replaymod-1.12.2-2.6.24.jar
  2  net.minecraft.client.renderer.RegionRenderCacheBuilder.getWorldRendererByLayer(Lnet.minecraft.util.BlockRenderLayer;)Lnet.minecraft.client.renderer.BufferBuilder;  # Fluidlogged-API-v3.3.3-mc1.12.2.jar, Nothirium-1.12.2-0.4.9-beta.jar
  2  net.minecraft.client.renderer.RenderGlobal$ContainerLocalRenderInformation.renderChunk:Lnet.minecraft.client.renderer.chunk.RenderChunk;  # NTM-CE-1.12.2-2.6.1.0.jar, [1.12.2] SecurityCraft v1.10.1.jar
  2  net.minecraft.client.renderer.RenderGlobal.hasNoChunkUpdates()Z  # JurassicReborn-1.12.2-1.3.4.jar, bbs-2.4-1.12.2.jar
  2  net.minecraft.client.renderer.RenderGlobal.onEntityRemoved(Lnet.minecraft.entity.Entity;)V  # DramaticTrees-1.12.2-0.13.6.jar, 永爱之刃-v11削弱.jar
  2  net.minecraft.client.renderer.ViewFrustum.<init>(Lnet.minecraft.world.World;ILnet.minecraft.client.renderer.RenderGlobal;Lnet.minecraft.client.renderer.chunk.IRenderChunkFactory;)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar
  2  net.minecraft.client.renderer.ViewFrustum.renderChunks:[Lnet.minecraft.client.renderer.chunk.RenderChunk;  # litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar, vintagefix-0.7.2.jar
  2  net.minecraft.client.renderer.ViewFrustum.updateChunkPositions(DD)V  # litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar, tweakeroo-forge-1.12.2-0.10.0-dev.20201007.231650+notrd.jar
  2  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type.REBUILD_CHUNK:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type;  # fermiumasm-5.31.jar, replaymod-1.12.2-2.6.24.jar
  2  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getRegionRenderCacheBuilder()Lnet.minecraft.client.renderer.RegionRenderCacheBuilder;  # Fluidlogged-API-v3.3.3-mc1.12.2.jar, NoCubes-1.12.2-0.4.0-pre-2dca66a0.jar
  2  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.hasNoFreeRenderBuilders()Z  # Alfheim-1.6.jar, SCP Lockdown-1.12.2-2.4.3-release.jar
  2  net.minecraft.client.renderer.chunk.CompiledChunk.isVisible(Lnet.minecraft.util.EnumFacing;Lnet.minecraft.util.EnumFacing;)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar
  2  net.minecraft.client.renderer.chunk.ListChunkFactory.<init>()V  # AdvancedRocketry-1.12.2-2.1.9-2.jar, SCP Lockdown-1.12.2-2.4.3-release.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.<init>(Lnet.minecraft.world.World;Lnet.minecraft.client.renderer.RenderGlobal;I)V  # [1.12.2] SecurityCraft v1.10.1.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.getBlockPosOffset16(Lnet.minecraft.util.EnumFacing;)Lnet.minecraft.util.math.BlockPos;  # CubicChunks-1.12.2-0.0.1301.0-SNAPSHOT-all.jar, SCP Lockdown-1.12.2-2.4.3-release.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.needsImmediateUpdate()Z  # SCP Lockdown-1.12.2-2.4.3-release.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.preRenderBlocks(Lnet.minecraft.client.renderer.BufferBuilder;Lnet.minecraft.util.math.BlockPos;)V  # Fluidlogged-API-v3.3.3-mc1.12.2.jar, NoCubes-1.12.2-0.4.0-pre-2dca66a0.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.setFrameIndex(I)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar, litematica-forge-1.12.2-0.0.0-dev.20191217.195055.jar
  2  net.minecraft.client.renderer.chunk.VisGraph.<init>()V  # SCP Lockdown-1.12.2-2.4.3-release.jar, relictium-1.2.0.jar
  2  net.minecraft.client.renderer.chunk.VisGraph.computeVisibility()Lnet.minecraft.client.renderer.chunk.SetVisibility;  # NTM-CE-1.12.2-2.6.1.0.jar, relictium-1.2.0.jar
  2  net.minecraft.client.renderer.chunk.VisGraph.setOpaqueCube(Lnet.minecraft.util.math.BlockPos;)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, relictium-1.2.0.jar
  2  net.minecraft.client.resources.data.BaseMetadataSectionSerializer.<init>()V  # LibrarianLib-Continuous-1.12.2-4.22-2.0-3.jar, TerrariaCraft-1.12.2-12.6.2.jar
  2  net.minecraft.client.settings.GameSettings.chatHeightUnfocused:F  # Wynntils-MC1.12.2-v1.15.1-beta.10.jar, betterchat-1.5-for-1.12.2.jar
  2  net.minecraft.client.settings.GameSettings.getModelParts()Ljava.util.Set;  # InGameAccountSwitcher-Forge-1.12-8.0.1.jar, Wynntils-MC1.12.2-v1.15.1-beta.10.jar
  2  net.minecraft.client.util.SearchTree.<init>(Ljava.util.function.Function;Ljava.util.function.Function;)V -> ()V  # bettersearch-forge-1.12.2-1.6.4.jar, fermiumasm-5.31.jar
  2  net.minecraft.entity.monster.EntityGuardian.clientSideSpikesAnimation:F  # MoBends_1.12.2-2.0.0-alpha.3.jar, moreplanets-mc1.12.2-v2.4.1.jar
  2  net.minecraft.entity.monster.EntityGuardian.clientSideSpikesAnimationO:F  # MoBends_1.12.2-2.0.0-alpha.3.jar, moreplanets-mc1.12.2-v2.4.1.jar
  2  net.minecraft.network.handshake.client.C00Handshake.<init>(Ljava.lang.String;ILnet.minecraft.network.EnumConnectionState;Z)V  # LoTAS1.12.2-2.1.3.jar, MineTogether-1.12.2-2.4.7.jar
  2  net.minecraft.network.login.client.CPacketLoginStart.<init>(Lcom.mojang.authlib.GameProfile;)V  # LoTAS1.12.2-2.1.3.jar, MineTogether-1.12.2-2.4.7.jar
  2  net.minecraft.util.MouseFilter.reset()V  # JurassicReborn-1.12.2-1.3.4.jar, 永爱之刃-v11削弱.jar
  2  net.minecraftforge.client.event.GuiScreenEvent$PotionShiftEvent.<init>(Lnet.minecraft.client.gui.GuiScreen;)V  # Cyclic-1.12.2-1.20.14.jar, Trinkets and Baubles-Forge-1.12.2-0.33.4.jar
  2  net.minecraftforge.fml.client.FMLClientHandler.getClientPlayHandler()Lnet.minecraft.network.INetHandler;  # Tropicraft-7.1.9.jar, tellme-1.12.2-0.7.0-dev.20200919.184303.jar
  2  org.lwjgl.openal.AL.getDevice()Lorg.lwjgl.openal.ALCdevice;  # PymTech-1.12.2-1.0.2.jar, themidnight-0.3.5.jar
  2  org.lwjgl.opengl.GL15.glGetQueryObject(IILjava.nio.IntBuffer;)V  # !!!Graphene-0.2.0-1.12.2Forge-Tick-B.jar, fluxloading-1.4.1.jar
  2  org.lwjgl.util.glu.GLU.gluLookAt(FFFFFFFFF)V  # HammerLib-1.12.2-12.2.66.jar, modularui-3.1.6.jar
  2  paulscode.sound.SoundSystem.activate(Ljava.lang.String;)V  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.SoundSystem.cull(Ljava.lang.String;)V  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.SoundSystem.getVolume(Ljava.lang.String;)F  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.SoundSystem.newStreamingSource(ZLjava.lang.String;Ljava.net.URL;Ljava.lang.String;ZFFFIF)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, matmos-1.12.2-36.0.1.jar
  2  paulscode.sound.SoundSystem.playing(Ljava.lang.String;)Z  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.SoundSystem.quickPlay(ZLjava.net.URL;Ljava.lang.String;ZFFFIF)Ljava.lang.String;  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.SoundSystemConfig.THREAD_SYNC:Ljava.lang.Object;  # AmbientSounds_v3.1.5_mc1.12.2.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.codecs.CodecJOrbis.<init>()V  # bbs-2.4-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  paulscode.sound.codecs.CodecJOrbis.cleanup()V  # bbs-2.4-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  paulscode.sound.codecs.CodecJOrbis.endOfStream()Z  # bbs-2.4-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  paulscode.sound.codecs.CodecJOrbis.getAudioFormat()Ljavax.sound.sampled.AudioFormat;  # bbs-2.4-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  paulscode.sound.codecs.CodecJOrbis.initialize(Ljava.net.URL;)Z  # bbs-2.4-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  paulscode.sound.codecs.CodecJOrbis.read()Lpaulscode.sound.SoundBuffer;  # bbs-2.4-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  1  net.minecraft.client.Minecraft.<init>(Lnet.minecraft.client.main.GameConfiguration;)V -> ()V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.Minecraft.debug:Ljava.lang.String;  # jsmacros-1.12.2-FORGE-1.8.3.jar
  1  net.minecraft.client.Minecraft.draw(IIIIIIIIII)V  # simplytea-1.6.jar
  1  net.minecraft.client.Minecraft.getFrameTimer()Lnet.minecraft.util.FrameTimer;  # LibrarianLib-Continuous-1.12.2-4.22-2.0-3.jar
  1  net.minecraft.client.Minecraft.getProfileProperties()Lcom.mojang.authlib.properties.PropertyMap;  # CustomPlayerModels-1.12.2-0.6.27a.jar
  1  net.minecraft.client.Minecraft.mcProfiler:Lnet.minecraft.profiler.Profiler;  # hbm-1.8.4a-G.jar
  1  net.minecraft.client.Minecraft.renderViewEntity:Lnet.minecraft.entity.Entity;  # cinematic_respawn-1.12.2-forge-1.4.3.jar
  1  net.minecraft.client.Minecraft.scheduleResourcesRefresh()Lcom.google.common.util.concurrent.ListenableFuture;  # PolyPatcher-1.12.2-forge-1.10.4.jar
  1  net.minecraft.client.Minecraft.stopIntegratedServer()V  # LoTAS1.12.2-2.1.3.jar
  1  net.minecraft.client.Minecraft.storeTEInStack(Lnet.minecraft.item.ItemStack;Lnet.minecraft.tileentity.TileEntity;)Lnet.minecraft.item.ItemStack;  # TerrariaCraft-1.12.2-12.6.2.jar
  1  net.minecraft.client.audio.SoundHandler.<init>(Lnet.minecraft.client.resources.IResourceManager;Lnet.minecraft.client.settings.GameSettings;)V -> ()V  # fermiumasm-5.31.jar
  1  net.minecraft.client.audio.SoundList.<init>(Ljava.util.List;ZLjava.lang.String;)V  # ancientwarfare-1.12.2-2.10.2.jar
  1  net.minecraft.client.audio.SoundManager.getURLForSoundResource(Lnet.minecraft.util.ResourceLocation;)Ljava.net.URL;  # matmos-1.12.2-36.0.1.jar
  1  net.minecraft.client.audio.SoundManager.getVolume(Lnet.minecraft.util.SoundCategory;)F  # matmos-1.12.2-36.0.1.jar
  1  net.minecraft.client.audio.SoundManager.loaded:Z  # matmos-1.12.2-36.0.1.jar
  1  net.minecraft.client.audio.SoundManager.playingSounds:Ljava.util.Map;  # UniversalModCore-1.12.2-forge-1.3.1.jar
```

## mod-side API (top 35 of 35)

```
  5  com.mojang.realmsclient.dto.RealmsServer.ownerUUID:Ljava.lang.String;  # CraftPresence-2.7.1+1.12.2-forge.jar, Xaeros_Minimap_FP24.2.0_Forge_1.12.jar, journeymap-forge-1.12.2-6.0.10.jar ...
  4  com.mojang.realmsclient.dto.RealmsServer.id:J  # Xaeros_Minimap_FP24.2.0_Forge_1.12.jar, journeymap-forge-1.12.2-6.0.10.jar, xaerominimap-forge-1.12.2-26.6.0.jar ...
  3  mcp.mobius.waila.config.FormattingConfig.modNameFormat:Ljava.lang.String;  # DramaticTrees-1.12.2-0.13.6.jar, JSON-Paintings-v1.5.0-mc1.12.2.jar, iceandfire-1.9.1-1.12.2.jar
  2  baubles.common.network.PacketHandler.INSTANCE:Lnet.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;  # Botania+r1.10-364.4.jar, Trinkets and Baubles-Forge-1.12.2-0.33.4.jar
  2  com.mojang.realmsclient.dto.RealmsServer$WorldType.name()Ljava.lang.String;  # CraftPresence-2.7.1+1.12.2-forge.jar, simple-rpc-1.12.2-3.1.1.jar
  2  com.mojang.realmsclient.dto.RealmsServer.getDescription()Ljava.lang.String;  # CraftPresence-2.7.1+1.12.2-forge.jar, simple-rpc-1.12.2-3.1.1.jar
  2  com.mojang.realmsclient.dto.RealmsServer.getMinigameName()Ljava.lang.String;  # CraftPresence-2.7.1+1.12.2-forge.jar, simple-rpc-1.12.2-3.1.1.jar
  2  com.mojang.realmsclient.dto.RealmsServer.getName()Ljava.lang.String;  # CraftPresence-2.7.1+1.12.2-forge.jar, simple-rpc-1.12.2-3.1.1.jar
  2  com.mojang.realmsclient.dto.RealmsServer.worldType:Lcom.mojang.realmsclient.dto.RealmsServer$WorldType;  # CraftPresence-2.7.1+1.12.2-forge.jar, simple-rpc-1.12.2-3.1.1.jar
  2  mcp.mobius.waila.cbcore.LangUtil.translateG(Ljava.lang.String;[Ljava.lang.Object;)Ljava.lang.String;  # Cherry_on-1.12.2-1.3.35.jar, Pam's+HarvestCraft+1.12.2zg.jar
  1  baubles.common.Config.renderBaubles:Z  # RLArtifacts-1.1.2.jar
  1  baubles.common.container.SlotBauble.<init>(Lnet.minecraft.entity.player.EntityPlayer;Lbaubles.api.cap.IBaublesItemHandler;III)V  # Botania+r1.10-364.4.jar
  1  baubles.common.network.PacketOpenBaublesInventory.<init>()V  # Trinkets and Baubles-Forge-1.12.2-0.33.4.jar
  1  baubles.common.network.PacketSync.<init>(Lnet.minecraft.entity.player.EntityPlayer;ILnet.minecraft.item.ItemStack;)V  # Botania+r1.10-364.4.jar
  1  codechicken.nei.api.API.addFastTransferExemptContainer(Ljava.lang.Class;)V  # NotEnoughItems-1.12.2-2.4.3.245-universal.jar
  1  codechicken.nei.api.API.addItemFilter(Lcodechicken.lib.item.filtering.IItemFilterProvider;)V  # NotEnoughItems-1.12.2-2.4.3.245-universal.jar
  1  codechicken.nei.api.API.addLayoutStyle(ILlegacyconnector_extra.nei.codechicken.nei.layout.LayoutStyle;)V  # NotEnoughItems-1.12.2-2.4.3.245-universal.jar
  1  codechicken.nei.api.API.addOption(Llegacyconnector_extra.nei.codechicken.nei.config.Option;)V  # NotEnoughItems-1.12.2-2.4.3.245-universal.jar
  1  codechicken.nei.api.API.addSearchProvider(Llegacyconnector_extra.nei.codechicken.nei.widget.SearchField$ISearchProvider;)V  # NotEnoughItems-1.12.2-2.4.3.245-universal.jar
  1  codechicken.nei.api.API.addSortOption(Ljava.lang.String;Ljava.util.Comparator;)V  # NotEnoughItems-1.12.2-2.4.3.245-universal.jar
  1  codechicken.nei.api.API.addSubset(Ljava.lang.String;Lcodechicken.lib.item.filtering.IItemFilter;)V  # NotEnoughItems-1.12.2-2.4.3.245-universal.jar
  1  codechicken.nei.api.API.registerNEIGuiHandler(Llegacyconnector_extra.nei.codechicken.nei.api.INEIGuiHandler;)V  # NotEnoughItems-1.12.2-2.4.3.245-universal.jar
  1  com.mojang.realmsclient.dto.RealmsServer.minigameImage:Ljava.lang.String;  # simple-rpc-1.12.2-3.1.1.jar
  1  com.mojang.realmsclient.dto.RealmsServer.name:Ljava.lang.String;  # journeymap-forge-1.12.2-6.0.10.jar
  1  com.mojang.realmsclient.dto.RealmsServer.owner:Ljava.lang.String;  # xaeroworldmap-forge-1.12.2-1.47.0.jar
  1  com.mojang.realmsclient.dto.RealmsServer.players:Ljava.util.List;  # simple-rpc-1.12.2-3.1.1.jar
  1  mcp.mobius.waila.api.event.WailaRenderEvent$Pre.setCanceled(Z)V  # [1.12.2] SecurityCraft v1.10.1.jar
  1  mcp.mobius.waila.api.impl.ConfigHandler.showItem()Z  # JSON-Paintings-v1.5.0-mc1.12.2.jar
  1  mcp.mobius.waila.client.KeyEvent.key_show:Lnet.minecraft.client.settings.KeyBinding;  # UniversalTweaks-1.12.2-1.21.0.jar
  1  mcp.mobius.waila.config.FormattingConfig.blockFormat:Ljava.lang.String;  # DramaticTrees-1.12.2-0.13.6.jar
  1  mcp.mobius.waila.config.FormattingConfig.entityFormat:Ljava.lang.String;  # iceandfire-1.9.1-1.12.2.jar
  1  mcp.mobius.waila.config.OverlayConfig.fontcolor:I  # JSON-Paintings-v1.5.0-mc1.12.2.jar
  1  mcp.mobius.waila.overlay.DisplayUtil.drawString(Ljava.lang.String;IIIZ)V  # JSON-Paintings-v1.5.0-mc1.12.2.jar
  1  mcp.mobius.waila.overlay.DisplayUtil.getDisplayWidth(Ljava.lang.String;)I  # JSON-Paintings-v1.5.0-mc1.12.2.jar
  1  mcp.mobius.waila.utils.ModIdentification.nameFromStack(Lnet.minecraft.item.ItemStack;)Ljava.lang.String;  # DramaticTrees-1.12.2-0.13.6.jar
```

