# 1.7.10: stub ranking (synthesized members by number of jars)

Generated 2026-11-16 by `tools/dev/agent-tasks/make_stub_ranking.py` from the offline probe of 641 jar(s) (503 links, 69 missing-dependency, 48 unresolved, 21 obsolete); 1522 distinct synthesized members. Regenerate after implementing; implemented members drop out. Format: `n  member  # jars`.


Done since generation (batches 1-6 of the shim worker; rows below not yet pruned, regenerate to drop them): vanilla block/item classes (batch 1), world/server/Forge hooks (2), entities/AI/player inventory/tab list packet (3), PathNavigate.canNavigate/isPositionClear, PathFinder.openPoint, Block.requiresUpdates, EntityZombie.func_146070_a, TileEntityChest.getChestType, ISaveHandler.saveWorldInfo, WorldInfo.cloneNBTCompound, IChunkProvider.findClosestStructure, Chunk.getEntitiesOfTypeWithinAAAB, EntityLivingBase.activePotionsMap (4), server commands (ban/banlist/msg/pardon/summon/tp/whitelist), CommandHelp.getCommands, S1CPacketEntityMetadata, MerchantRecipeList.func_151391_a, BiomeEvent.CreateDecorator (5), ChunkProviderEnd, RegionFile/RegionFileCache, MouseFilter, StructureMineshaftStart, ForgeMessage.DimensionRegisterMessage, BaseAttributeMap.addAttributeInstance (6). False positives in the list: nested classes (ServerStatusResponse$PlayerCountData, ForgeVersion$Status), inherited members (EntityPlayer.performHurtAnimation, BlockFluidClassic.definedFluid). Left: FML loader internals (LoadController, ModDiscoverer, ClassPatchManager), LAN/NetworkSystem (shareToLAN, getNetworkSystem, provideLanClient), Thaumcraft-API members on ItemArmor, PotionHelper.func_77905_c, EntityTrackerEntry.sendEventsToPlayers, Packet.generatePacket, NEI/Waila internals.

## server-side API (top 150 of 614)

```
  4  net.minecraft.block.BlockSign.<init>(Ljava.lang.Class;Z)V  # Floocraft-1.7.10-1.7.jar, Game of Thrones 24.08.29.jar, etfuturum-2.6.2.jar ...
  4  net.minecraft.command.CommandTime.<init>()V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, Game of Thrones 24.08.29.jar, MineTradingCards-1.7.10-3.1.4.jar ...
  4  net.minecraft.entity.ai.EntityAITasks.<init>(Lnet.minecraft.profiler.Profiler;)V -> ()V  # DragonMounts-r41-1.7.10.jar, Game of Thrones 24.08.29.jar, Invasion-1.7.10-1.2.1.jar ...
  3  net.minecraft.block.BlockButtonWood.<init>()V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar, etfuturum-2.6.2.jar
  3  net.minecraft.block.BlockHay.<init>()V  # SaltyMod Expanded [1.7.10] [2.1.0].jar, VeganOption-mc1.7.10-0.2.2.jar, ashenwheat-1.7.10-1.2.4.1.jar
  3  net.minecraft.block.BlockSandStone.<init>()V  # Improving Minecraft-1.7.10-v5.jar, [1.7.10]Futurepack+18.7.jar, etfuturum-2.6.2.jar
  3  net.minecraft.block.BlockTrapDoor.disableValidation:Z  # 1.7.10-ASJCore-1.7.0.3.jar, VillageNames-1.7.10-4.4.13.jar, etfuturum-2.6.2.jar
  3  net.minecraft.entity.passive.EntityHorse.setEatingHaystack(Z)V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar, ancientwarfare-3.0.1-beta-MC1.7.10-FULL.jar
  3  net.minecraft.entity.passive.EntityHorse.setRearing(Z)V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar, ancientwarfare-3.0.1-beta-MC1.7.10-FULL.jar
  3  net.minecraft.item.crafting.IRecipe.input:[Ljava.lang.Object;  # Factorization-1.7.10-0.8.109.jar, UniDict-1.7.10-2.9.2.jar, immibis-core-59.1.4.jar
  3  net.minecraft.nbt.NBTTagEnd.<init>()V  # compatlib-1.7.10.jar, journeymap-forge-1.7.10-6.0.8.jar, optimizationsandtweaks-V1.18.0.jar
  3  net.minecraft.network.NetworkManager.provideLanClient(Ljava.net.InetAddress;I)Lnet.minecraft.network.NetworkManager;  # ApolloClient-v1.3.1.b21-2026-11-06.jar, ServerUtilities-2.5.2.jar, tabfaces-1.0.16.jar
  3  net.minecraft.network.ServerStatusResponse.setServerDescription(Lnet.minecraft.util.IChatComponent;)V  # Paradigm-forge-1.7.10-2.5.4.jar, ServerUtilities-2.5.2.jar, forgeessentials-1.7.10-7.5.3-server.jar
  3  net.minecraft.network.play.server.S23PacketBlockChange.func_148877_f()I  # XaerosWorldMap_1.14.1.33_Forge_1.7.10.jar, Xaeros_Minimap_21.10.44_Forge_1.7.10.jar, Xaeros_Minimap_FP21.10.40_Forge_1.7.10.jar
  3  net.minecraft.network.play.server.S23PacketBlockChange.func_148879_d()I  # XaerosWorldMap_1.14.1.33_Forge_1.7.10.jar, Xaeros_Minimap_21.10.44_Forge_1.7.10.jar, Xaeros_Minimap_FP21.10.40_Forge_1.7.10.jar
  3  net.minecraft.network.play.server.S38PacketPlayerListItem.<init>(Ljava.lang.String;ZI)V  # Paradigm-forge-1.7.10-2.5.4.jar, ServerUtilities-2.5.2.jar, tabfaces-1.0.16.jar
  3  net.minecraft.potion.Potion.<init>(Lnet.minecraft.util.ResourceLocation;ZI)V -> (IZI)V  # XaerosWorldMap_1.14.1.33_Forge_1.7.10.jar, Xaeros_Minimap_21.10.44_Forge_1.7.10.jar, Xaeros_Minimap_FP21.10.40_Forge_1.7.10.jar
  3  net.minecraft.server.MinecraftServer.getNetworkSystem()Lnet.minecraft.network.NetworkSystem;  # EasyLAN-v1.5-[1.7.10].jar, MiddleEarth-Tweaks-1.6.21.jar, mcheli-d4e3692.jar
  3  net.minecraft.server.MinecraftServer.getServerStatusResponse()Lnet.minecraft.network.ServerStatusResponse;  # Paradigm-forge-1.7.10-2.5.4.jar, forgeessentials-1.7.10-7.5.3-server.jar, tabfaces-1.0.16.jar
  3  net.minecraft.server.management.ServerConfigurationManager.respawnPlayer(Lnet.minecraft.entity.player.EntityPlayerMP;IZ)Lnet.minecraft.entity.player.EntityPlayerMP;  # 1.7.10-ASJCore-1.7.0.3.jar, HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, ThaumicConcilium-1.1.1.jar
  3  net.minecraft.util.Session.<init>(Ljava.lang.String;Ljava.lang.String;Ljava.lang.String;Ljava.lang.String;)V -> (Ljava.lang.String;Ljava.lang.String;Ljava.lang.String;)V  # InGameAccountSwitcher-Forge-1.7-8.0.1.jar, manametalmod-8.0.8-1.jar, mcheli-d4e3692.jar
  3  net.minecraft.world.chunk.Chunk.func_150804_b(Z)V  # Eln-1.24.6.jar, MiddleEarth-Tweaks-1.6.21.jar, optimizationsandtweaks-V1.18.0.jar
  3  net.minecraft.world.chunk.Chunk.func_150809_p()V  # AIImprovements-1.7.10-0.0.1b8.jar, HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, lumi-mc1.7.10-1.2.2.jar
  3  net.minecraft.world.gen.ChunkProviderEnd.<init>(Lnet.minecraft.world.World;J)V  # FarLands-1.0.1-mc1.7.10.jar, HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, paintedbiomes-1.7.10-0.5.1.jar
  3  net.minecraft.world.storage.WorldInfo.getNBTTagCompound()Lnet.minecraft.nbt.NBTTagCompound;  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar, forgeessentials-1.7.10-7.5.3-server.jar
  3  net.minecraftforge.common.ForgeHooks.onPlayerTossEvent(Lnet.minecraft.entity.player.EntityPlayer;Lnet.minecraft.item.ItemStack;Z)Lnet.minecraft.entity.item.EntityItem;  # itemphysic-1.3.1.jar, okprogressions-26.11.15.0.jar, theoffhandmod-1.1.4.jar
  3  net.minecraftforge.event.terraingen.SaplingGrowTreeEvent.x:I  # Game of Thrones 24.08.29.jar, HBM-NTM-.1.0.27_X5853_H261.jar, etfuturum-2.6.2.jar
  3  net.minecraftforge.event.terraingen.SaplingGrowTreeEvent.y:I  # Game of Thrones 24.08.29.jar, HBM-NTM-.1.0.27_X5853_H261.jar, etfuturum-2.6.2.jar
  3  net.minecraftforge.event.terraingen.SaplingGrowTreeEvent.z:I  # Game of Thrones 24.08.29.jar, HBM-NTM-.1.0.27_X5853_H261.jar, etfuturum-2.6.2.jar
  3  net.minecraftforge.fluids.BlockFluidClassic.definedFluid:Lnet.minecraftforge.fluids.Fluid;  # ActuallyAdditions-1.7.10-r21.jar, Alfheim-67.jar, ic2additions1.7.10-0.9.0.jar
  2  cpw.mods.fml.common.InjectedModContainer.<init>(Lcpw.mods.fml.common.ModContainer;Ljava.io.File;)V  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.LoadController.<init>(Lcpw.mods.fml.common.Loader;)V -> ()V  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.LoadController.distributeStateMessage(Lcpw.mods.fml.common.LoaderState;[Ljava.lang.Object;)V  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.LoadController.distributeStateMessage(Ljava.lang.Class;)V  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.LoadController.getActiveModList()Ljava.util.List;  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.LoadController.transition(Lcpw.mods.fml.common.LoaderState;Z)V  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.ModAPIManager.cleanupAPIContainers(Ljava.util.List;)V  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.ModAPIManager.manageAPI(Lcpw.mods.fml.common.ModClassLoader;Lcpw.mods.fml.common.discovery.ModDiscoverer;)V  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.ModContainer.getSigningCertificate()Ljava.security.cert.Certificate;  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.discovery.ModDiscoverer.<init>()V  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.discovery.ModDiscoverer.getASMTable()Lcpw.mods.fml.common.discovery.ASMDataTable;  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.discovery.ModDiscoverer.getNonModLibs()Ljava.util.List;  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.functions.ModIdFunction.<init>()V  # jarjar-0.4.0-beta.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.network.internal.FMLMessage$EntitySpawnMessage.<init>(Lcpw.mods.fml.common.registry.EntityRegistry$EntityRegistration;Lnet.minecraft.entity.Entity;Lcpw.mods.fml.common.ModContainer;)V  # CustomNPC-Plus-1.11.1.jar, Game of Thrones 24.08.29.jar
  2  cpw.mods.fml.common.patcher.ClassPatchManager.INSTANCE:Lcpw.mods.fml.common.patcher.ClassPatchManager;  # BetterLineBreak-1.7.10(Forge)-1.0.1.jar, compatlib-1.7.10.jar
  2  cpw.mods.fml.common.patcher.ClassPatchManager.getPatchedResource(Ljava.lang.String;Ljava.lang.String;Lnet.minecraft.launchwrapper.LaunchClassLoader;)[B  # BetterLineBreak-1.7.10(Forge)-1.0.1.jar, compatlib-1.7.10.jar
  2  cpw.mods.fml.common.registry.EntityRegistry$EntityRegistration.usesVanillaSpawning()Z  # CustomNPC-Plus-1.11.1.jar, Game of Thrones 24.08.29.jar
  2  cpw.mods.fml.common.registry.EntityRegistry.tryTrackingEntity(Lnet.minecraft.entity.EntityTracker;Lnet.minecraft.entity.Entity;)Z  # EntityRenderDistanceExtender-1.0c.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.common.registry.GameData$GameDataSnapshot.idMap:Ljava.util.Map;  # Additions to the Outer Rim-1.3.6-BETA (1).jar, schematica_plus-1.0.0-beta.4.jar
  2  cpw.mods.fml.common.registry.GameData.buildItemDataList()Lcpw.mods.fml.common.registry.GameData$GameDataSnapshot;  # Additions to the Outer Rim-1.3.6-BETA (1).jar, schematica_plus-1.0.0-beta.4.jar
  2  net.minecraft.block.Block.requiresUpdates()Z  # LycanitesMobsComplete 1.13.0.5 [1.7.10].jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.block.BlockGravel.<init>()V  # Game of Thrones 24.08.29.jar, etfuturum-2.6.2.jar
  2  net.minecraft.command.CommandHelp.getCommands()Ljava.util.Map;  # Factorization-1.7.10-0.8.109.jar, forgeessentials-1.7.10-7.5.3-server.jar
  2  net.minecraft.command.server.CommandBanPlayer.<init>()V  # EasyLAN-v1.5-[1.7.10].jar, e4mc-retro_forge-1.0.2+1.7.x.jar
  2  net.minecraft.command.server.CommandListBans.<init>()V  # EasyLAN-v1.5-[1.7.10].jar, e4mc-retro_forge-1.0.2+1.7.x.jar
  2  net.minecraft.command.server.CommandMessage.<init>()V  # Game of Thrones 24.08.29.jar, forgeessentials-1.7.10-7.5.3-server.jar
  2  net.minecraft.command.server.CommandPardonPlayer.<init>()V  # EasyLAN-v1.5-[1.7.10].jar, e4mc-retro_forge-1.0.2+1.7.x.jar
  2  net.minecraft.command.server.CommandSummon.<init>()V  # Game of Thrones 24.08.29.jar, mcheli-d4e3692.jar
  2  net.minecraft.command.server.CommandTeleport.<init>()V  # Game of Thrones 24.08.29.jar, mcheli-d4e3692.jar
  2  net.minecraft.command.server.CommandWhitelist.<init>()V  # EasyLAN-v1.5-[1.7.10].jar, e4mc-retro_forge-1.0.2+1.7.x.jar
  2  net.minecraft.enchantment.EnchantmentData.<init>(II)V -> (Lnet.minecraft.enchantment.Enchantment;I)V  # IC2Classic-1.7.10-1.2.6.jar, mobsinfo-0.6.2.jar
  2  net.minecraft.enchantment.EnchantmentProtection.getFireTimeForEntity(Lnet.minecraft.entity.Entity;I)I  # HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, Vale of Shadows - Stranger Things - The Upside Down[1.7.10].jar
  2  net.minecraft.entity.EntityLivingBase.activePotionsMap:Ljava.util.HashMap;  # 1.7.10-ASJCore-1.7.0.3.jar, Alfheim-67.jar
  2  net.minecraft.entity.EntityTrackerEntry.<init>(Lnet.minecraft.entity.Entity;IIZ)V -> (Lnet.minecraft.entity.Entity;)V  # optimizationsandtweaks-V1.18.0.jar, zombieplague2.jar
  2  net.minecraft.entity.EntityTrackerEntry.sendEventsToPlayers(Ljava.util.List;)V  # optimizationsandtweaks-V1.18.0.jar, zombieplague2.jar
  2  net.minecraft.entity.ai.EntityAIPlay.<init>(Lnet.minecraft.entity.passive.EntityVillager;D)V  # 1.7.10-zeldaswordskills-2.5.3.jar, NarutoDestinationMod-1.36.1.jar
  2  net.minecraft.entity.ai.EntityAIRunAroundLikeCrazy.<init>(Lnet.minecraft.entity.passive.EntityHorse;D)V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraft.entity.ai.EntityAITempt.isRunning()Z  # NotEnoughPets-1.7.10-2.4.1.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraft.entity.ai.EntityAIVillagerMate.<init>(Lnet.minecraft.entity.passive.EntityVillager;)V  # 1.7.10-zeldaswordskills-2.5.3.jar, NarutoDestinationMod-1.36.1.jar
  2  net.minecraft.entity.ai.EntityMoveHelper.limitAngle(FFF)F  # HBM-NTM-.1.0.27_X5853_H261.jar, RX-RNTM-[3.3_X1000].jar
  2  net.minecraft.entity.ai.attributes.BaseAttributeMap.addAttributeInstance(Lnet.minecraft.entity.ai.attributes.ModifiableAttributeInstance;)V  # Muya_1.11.1.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.entity.monster.EntitySlime.getJumpSound()Ljava.lang.String;  # fossilsarcheology-7.3.2.jar, yarrcutemobmodels-1.0.9-1.7.10.jar
  2  net.minecraft.entity.monster.EntityZombie.func_146070_a(Z)V  # 1.7.10-ASJCore-1.7.0.3.jar, The Titans MC 1.7.10 version 0.45.jar
  2  net.minecraft.entity.passive.EntityHorse.func_110201_q(F)F  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraft.entity.passive.EntityHorse.isSterile()Z  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraft.entity.passive.EntityHorse.setTemper(I)V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraft.entity.player.EntityPlayer.performHurtAnimation()V  # AM2PlayGround-Beta-0.12.1.jar, FSP-1.7.10-0.29.3.jar
  2  net.minecraft.entity.player.EntityPlayerMP.addMovementStat(DDD)V  # Game of Thrones 24.08.29.jar, MiddleEarth-Tweaks-1.6.21.jar
  2  net.minecraft.entity.player.EntityPlayerMP.isSpawnForced(I)Z  # Game of Thrones 24.08.29.jar, Muya_1.11.1.jar
  2  net.minecraft.entity.player.InventoryPlayer.damageArmor(F)V  # Alfheim-67.jar, zombieplague2.jar
  2  net.minecraft.entity.player.InventoryPlayer.getStrVsBlock(Lnet.minecraft.block.Block;)F  # Kingdom+Keys-0.7.0.jar, theoffhandmod-1.1.4.jar
  2  net.minecraft.entity.player.InventoryPlayer.setCurrentItem(Lnet.minecraft.item.Item;IZZ)V  # Kingdom+Keys-0.7.0.jar, theoffhandmod-1.1.4.jar
  2  net.minecraft.entity.projectile.EntityWitherSkull.<init>(Lnet.minecraft.world.World;DDDDDD)V -> (Lnet.minecraft.world.World;Lnet.minecraft.entity.EntityLivingBase;DDD)V  # The Titans MC 1.7.10 version 0.45.jar, manametalmod-8.0.8-1.jar
  2  net.minecraft.item.ItemArmor.getVisDiscount(Lnet.minecraft.item.ItemStack;Lnet.minecraft.entity.player.EntityPlayer;Lthaumcraft.api.aspects.Aspect;)I  # AM2PlayGround-Beta-0.12.1.jar, Alfheim-67.jar
  2  net.minecraft.item.ItemBucketMilk.<init>()V  # Improving Minecraft-1.7.10-v5.jar, ThaumicHorizons-1.9.0.jar
  2  net.minecraft.item.ItemLead.<init>()V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraft.item.ItemLead.func_150909_a(Lnet.minecraft.entity.player.EntityPlayer;Lnet.minecraft.world.World;III)Z  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraft.item.crafting.IRecipe.expand(Ljava.lang.Object;)Ljava.util.List;  # Factorization-1.7.10-0.8.109.jar, immibis-core-59.1.4.jar
  2  net.minecraft.launchwrapper.IClassNameTransformer.remapClassName(Ljava.lang.String;)Ljava.lang.String;  # forgeessentials-1.7.10-7.5.3-server.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.launchwrapper.IClassNameTransformer.unmapClassName(Ljava.lang.String;)Ljava.lang.String;  # forgeessentials-1.7.10-7.5.3-server.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.network.Packet.generatePacket(Lcom.google.common.collect.BiMap;I)Lnet.minecraft.network.Packet;  # Factorization-1.7.10-0.8.109.jar, hfr-1.0.2.2+mc1.7.10-forge.jar
  2  net.minecraft.network.ServerStatusResponse$PlayerCountData.<init>(II)V  # Paradigm-forge-1.7.10-2.5.4.jar, tabfaces-1.0.16.jar
  2  net.minecraft.network.ServerStatusResponse$PlayerCountData.getMaxPlayers()I  # Paradigm-forge-1.7.10-2.5.4.jar, tabfaces-1.0.16.jar
  2  net.minecraft.network.ServerStatusResponse$PlayerCountData.getOnlinePlayerCount()I  # Paradigm-forge-1.7.10-2.5.4.jar, tabfaces-1.0.16.jar
  2  net.minecraft.network.ServerStatusResponse$PlayerCountData.getPlayers()[Lcom.mojang.authlib.GameProfile;  # Paradigm-forge-1.7.10-2.5.4.jar, tabfaces-1.0.16.jar
  2  net.minecraft.network.ServerStatusResponse$PlayerCountData.setPlayers([Lcom.mojang.authlib.GameProfile;)V  # Paradigm-forge-1.7.10-2.5.4.jar, tabfaces-1.0.16.jar
  2  net.minecraft.network.ServerStatusResponse.getPlayerCountData()Lnet.minecraft.network.ServerStatusResponse$PlayerCountData;  # Paradigm-forge-1.7.10-2.5.4.jar, tabfaces-1.0.16.jar
  2  net.minecraft.network.ServerStatusResponse.getServerDescription()Lnet.minecraft.util.IChatComponent;  # Paradigm-forge-1.7.10-2.5.4.jar, tabfaces-1.0.16.jar
  2  net.minecraft.network.ServerStatusResponse.setPlayerCountData(Lnet.minecraft.network.ServerStatusResponse$PlayerCountData;)V  # Paradigm-forge-1.7.10-2.5.4.jar, tabfaces-1.0.16.jar
  2  net.minecraft.network.play.server.S1CPacketEntityMetadata.<init>(ILnet.minecraft.entity.DataWatcher;Z)V  # Paradigm-forge-1.7.10-2.5.4.jar, mcheli-d4e3692.jar
  2  net.minecraft.pathfinding.PathFinder.openPoint(III)Lnet.minecraft.pathfinding.PathPoint;  # etfuturum-2.6.2.jar, zombieplague2.jar
  2  net.minecraft.pathfinding.PathNavigate.canNavigate()Z  # ancientwarfare-3.0.1-beta-MC1.7.10-FULL.jar, etfuturum-2.6.2.jar
  2  net.minecraft.pathfinding.PathNavigate.isPositionClear(IIIIIILnet.minecraft.util.Vec3;DD)Z  # ancientwarfare-3.0.1-beta-MC1.7.10-FULL.jar, etfuturum-2.6.2.jar
  2  net.minecraft.potion.PotionHelper.func_77905_c(I)Ljava.lang.String;  # Game of Thrones 24.08.29.jar, etfuturum-2.6.2.jar
  2  net.minecraft.server.MinecraftServer.getServerProxy()Ljava.net.Proxy;  # OpenComputers-MC1.7.10-1.8.10+667626d-universal.jar, narutomod-0.5.2.jar
  2  net.minecraft.server.MinecraftServer.setAllowFlight(Z)V  # EasyLAN-v1.5-[1.7.10].jar, forgeessentials-1.7.10-7.5.3-server.jar
  2  net.minecraft.server.MinecraftServer.setOnlineMode(Z)V  # EasyLAN-v1.5-[1.7.10].jar, forgeessentials-1.7.10-7.5.3-server.jar
  2  net.minecraft.server.integrated.IntegratedServer.shareToLAN(Lnet.minecraft.world.WorldSettings$GameType;Z)Ljava.lang.String;  # portalfriends-forge-1.1.1-1.7.10.jar, steambridge-1.2-Forge-1.7.10.jar
  2  net.minecraft.server.management.ServerConfigurationManager.updatePlayerPertinentChunks(Lnet.minecraft.entity.player.EntityPlayerMP;)V  # Game of Thrones 24.08.29.jar, MiddleEarth-Tweaks-1.6.21.jar
  2  net.minecraft.tileentity.TileEntityChest.getChestType()I  # curvy_pipes-1.7.10-1.15.12.jar, zombieplague2.jar
  2  net.minecraft.util.JsonUtils.getJsonObjectBooleanFieldValue(Lcom.google.gson.JsonObject;Ljava.lang.String;)Z  # ServerUtilities-2.5.2.jar, campfirebackport-1.7.10-1.11.3.jar
  2  net.minecraft.village.MerchantRecipeList.func_151391_a(Lnet.minecraft.network.PacketBuffer;)V  # CustomNPC-Plus-1.11.1.jar, MorePlayerModels-Plus-4.2.jar
  2  net.minecraft.world.ChunkPosition.<init>(Lnet.minecraft.util.Vec3;)V -> (III)V  # ChocoCraft-4.1.5.jar, ChocoCraftPlus-1.7.10-4.5.10.jar
  2  net.minecraft.world.WorldProvider.canCoordinateBeSpawn(II)Z  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraft.world.WorldProvider.canDoLightning(Lnet.minecraft.world.chunk.Chunk;)Z  # RX-RNTM-[3.3_X1000].jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.world.WorldProvider.canDoRainSnowIce(Lnet.minecraft.world.chunk.Chunk;)Z  # RX-RNTM-[3.3_X1000].jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.world.WorldProvider.getDepartMessage()Ljava.lang.String;  # [1.7.10]Futurepack+18.7.jar, eyesintheshadows-0.0.3.jar
  2  net.minecraft.world.WorldServer.getDefaultTeleporter()Lnet.minecraft.world.Teleporter;  # ForgottenRelics-1.7.10-1.7.4.jar, enderutilities-1.7.10-0.5.3.jar
  2  net.minecraft.world.chunk.Chunk.getEntitiesOfTypeWithinAAAB(Ljava.lang.Class;Lnet.minecraft.util.AxisAlignedBB;Ljava.util.List;Lnet.minecraft.command.IEntitySelector;)V  # ServerUtilities-2.5.2.jar, forgeessentials-1.7.10-7.5.3-server.jar
  2  net.minecraft.world.chunk.IChunkProvider.findClosestStructure(Lnet.minecraft.world.World;Ljava.lang.String;III)Lnet.minecraft.world.ChunkPosition;  # AIImprovements-1.7.10-0.0.1b8.jar, structure-finder-1.7.10-forge-1.5.1.jar
  2  net.minecraft.world.gen.ChunkProviderServer.loadChunk(IILjava.lang.Runnable;)Lnet.minecraft.world.chunk.Chunk;  # ServerUtilities-2.5.2.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.world.gen.structure.StructureMineshaftStart.<init>(Lnet.minecraft.world.World;Ljava.util.Random;II)V  # bettercaves-1.0.jar, etfuturum-2.6.2.jar
  2  net.minecraft.world.storage.ISaveHandler.saveWorldInfo(Lnet.minecraft.world.storage.WorldInfo;)V  # LoTAS1.7.10-2.0.0.jar, forgeessentials-1.7.10-7.5.3-server.jar
  2  net.minecraft.world.storage.WorldInfo.<init>(Lnet.minecraft.world.WorldSettings;Ljava.lang.String;)V -> ()V  # AIImprovements-1.7.10-0.0.1b8.jar, LoTAS1.7.10-2.0.0.jar
  2  net.minecraft.world.storage.WorldInfo.cloneNBTCompound(Lnet.minecraft.nbt.NBTTagCompound;)Lnet.minecraft.nbt.NBTTagCompound;  # forgeessentials-1.7.10-7.5.3-server.jar, personalspace-1.1.0.jar
  2  net.minecraftforge.common.FishingHooks.removeFish(Lcom.google.common.base.Predicate;)V  # HBM-NTM-.1.0.27_X5853_H261.jar, RX-RNTM-[3.3_X1000].jar
  2  net.minecraftforge.common.FishingHooks.removeJunk(Lcom.google.common.base.Predicate;)V  # HBM-NTM-.1.0.27_X5853_H261.jar, RX-RNTM-[3.3_X1000].jar
  2  net.minecraftforge.common.FishingHooks.removeTreasure(Lcom.google.common.base.Predicate;)V  # HBM-NTM-.1.0.27_X5853_H261.jar, RX-RNTM-[3.3_X1000].jar
  2  net.minecraftforge.common.ForgeVersion$Status.OUTDATED:Lnet.minecraftforge.common.ForgeVersion$Status;  # narutomod-0.5.2.jar, okcore-26.11.15.1.jar
  2  net.minecraftforge.common.ForgeVersion$Status.UP_TO_DATE:Lnet.minecraftforge.common.ForgeVersion$Status;  # HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, okcore-26.11.15.1.jar
  2  net.minecraftforge.common.config.Configuration.get(Ljava.lang.String;Ljava.lang.String;[Ljava.lang.String;Ljava.lang.String;Ljava.util.regex.Pattern;)Lnet.minecraftforge.common.config.Property;  # campfirebackport-1.7.10-1.11.3.jar, torchkey-mc1.7.10-1.0.1.jar
  2  net.minecraftforge.common.network.ForgeMessage$DimensionRegisterMessage.<init>(II)V  # Pokecube Legacy 1.3.3.jar, forgeessentials-1.7.10-7.5.3-server.jar
  2  net.minecraftforge.event.ForgeEventFactory.fireZombieSummonAid(Lnet.minecraft.entity.monster.EntityZombie;Lnet.minecraft.world.World;IIILnet.minecraft.entity.EntityLivingBase;D)Lnet.minecraftforge.event.entity.living.ZombieEvent$SummonAidEvent;  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraftforge.event.ForgeEventFactory.onPotionBrewed([Lnet.minecraft.item.ItemStack;)V  # etfuturum-2.6.2.jar, zombieplague2.jar
  2  net.minecraftforge.event.terraingen.BiomeEvent$CreateDecorator.newBiomeDecorator:Lnet.minecraft.world.biome.BiomeDecorator;  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraftforge.event.terraingen.SaplingGrowTreeEvent.rand:Ljava.util.Random;  # Game of Thrones 24.08.29.jar, etfuturum-2.6.2.jar
  1  cpw.mods.fml.common.DuplicateModsFoundException.<init>(Lcom.google.common.collect.SetMultimap;)V  # optimizationsandtweaks-V1.18.0.jar
  1  cpw.mods.fml.common.FMLCommonHandler.computeBranding()V  # BetterTitleScreen-1.7.10-1.1.jar
  1  cpw.mods.fml.common.FMLCommonHandler.handleWorldDataSave(Lnet.minecraft.world.storage.SaveHandler;Lnet.minecraft.world.storage.WorldInfo;Lnet.minecraft.nbt.NBTTagCompound;)V  # forgeessentials-1.7.10-7.5.3-server.jar
  1  cpw.mods.fml.common.FMLCommonHandler.onPostClientTick()V  # optimizationsandtweaks-V1.18.0.jar
  1  cpw.mods.fml.common.FMLCommonHandler.onPreClientTick()V  # optimizationsandtweaks-V1.18.0.jar
  1  cpw.mods.fml.common.FMLCommonHandler.processWindowMessages()V  # Cloudflared-7.10.3.jar
  1  cpw.mods.fml.common.FMLCommonHandler.stripSpecialChars(Ljava.lang.String;)Ljava.lang.String;  # Cloudflared-7.10.3.jar
  1  cpw.mods.fml.common.FMLContainerHolder.getFMLContainer()Lcpw.mods.fml.common.ModContainer;  # gtnhlib-0.12.4.jar
  1  cpw.mods.fml.common.IFMLSidedHandler.getSide()Lcpw.mods.fml.relauncher.Side;  # CarbonConfig-1.7.10-2.0.2.1.jar
  1  cpw.mods.fml.common.IWorldGenerator.textureDomain:Ljava.lang.String;  # ic2additions1.7.10-0.9.0.jar
  1  cpw.mods.fml.common.LoadController.activeContainer()Lcpw.mods.fml.common.ModContainer;  # optimizationsandtweaks-V1.18.0.jar
  1  cpw.mods.fml.common.LoadController.getModObjectList()Lcom.google.common.collect.BiMap;  # optimizationsandtweaks-V1.18.0.jar
  1  cpw.mods.fml.common.LoadController.getModState(Lcpw.mods.fml.common.ModContainer;)Lcpw.mods.fml.common.LoaderState$ModState;  # optimizationsandtweaks-V1.18.0.jar
  1  cpw.mods.fml.common.LoadController.isInState(Lcpw.mods.fml.common.LoaderState;)Z  # optimizationsandtweaks-V1.18.0.jar
```

## client-side API (top 150 of 462)

```
  8  net.minecraft.client.renderer.entity.RenderPlayer.doRender(Lnet.minecraft.client.entity.AbstractClientPlayer;DDDFF)V  # AM2.5-LTS-1.6.7.jar, ApolloClient-v1.3.1.b21-2026-11-06.jar, MorePlayerModels-Plus-4.2.jar ...
  6  net.minecraft.client.gui.ChatLine.getChatComponent()Lnet.minecraft.util.IChatComponent;  # Alfheim-67.jar, ApolloClient-v1.3.1.b21-2026-11-06.jar, CrossTie-1.0.0-Alpha17.jar ...
  6  org.lwjgl.input.Cursor.<init>(IIIIILjava.nio.IntBuffer;Ljava.nio.IntBuffer;)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, CustomCursorMod1.2.2-1.7.10.jar, TailsLegacy-4.0.3+1.7.10-forge.jar ...
  6  org.lwjgl.input.Mouse.setNativeCursor(Lorg.lwjgl.input.Cursor;)Lorg.lwjgl.input.Cursor;  # ApolloClient-v1.3.1.b21-2026-11-06.jar, CustomCursorMod1.2.2-1.7.10.jar, TailsLegacy-4.0.3+1.7.10-forge.jar ...
  5  net.minecraft.client.gui.GuiGameOver.<init>()V  # EnviroMine-1.3.132.jar, cinematic_respawn-1.7.10-forge-1.4.3.jar, enviromine-1.3.148-ESE-0x01.jar ...
  5  net.minecraft.client.gui.inventory.GuiChest.<init>(Lnet.minecraft.inventory.IInventory;Lnet.minecraft.inventory.IInventory;)V  # Game of Thrones 24.08.29.jar, backtonew-1.7.10-b1.0.jar, etfuturum-2.6.2.jar ...
  5  net.minecraft.network.handshake.client.C00Handshake.<init>(ILjava.lang.String;ILnet.minecraft.network.EnumConnectionState;)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, LoTAS1.7.10-2.0.0.jar, ServerUtilities-2.5.2.jar ...
  4  net.minecraft.client.audio.SoundEventAccessorComposite.func_148720_g()Lnet.minecraft.client.audio.SoundPoolEntry;  # HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, OpenComputers-MC1.7.10-1.8.10+667626d-universal.jar, matmos-1.7.10-36.0.1.jar ...
  4  net.minecraft.client.audio.SoundHandler.getSound(Lnet.minecraft.util.ResourceLocation;)Lnet.minecraft.client.audio.SoundEventAccessorComposite;  # HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, OpenComputers-MC1.7.10-1.8.10+667626d-universal.jar, matmos-1.7.10-36.0.1.jar ...
  4  net.minecraft.client.audio.SoundHandler.sndRegistry:Lnet.minecraft.client.audio.SoundRegistry;  # DynamicSurroundings-1.7.10-1.0.9.0.jar, UniversalModCore-1.7.10-forge-1.3.1.jar, esm-legacy-1.7.10-1.1.1.jar ...
  4  net.minecraft.client.gui.GuiNewChat.<init>(Lnet.minecraft.client.Minecraft;)V -> ()V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, Salutation-1.7.10-1.0.2.jar, tabbychat-1.12.2.jar ...
  4  net.minecraft.client.gui.GuiScreenBook.<init>(Lnet.minecraft.entity.player.EntityPlayer;Lnet.minecraft.item.ItemStack;Z)V  # AM2PlayGround-Beta-0.12.1.jar, Hardcore+Wither-1.7.10-1.1.3-21-universal.jar, HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar ...
  4  net.minecraft.client.multiplayer.GuiConnecting.<init>(Lnet.minecraft.client.gui.GuiScreen;Lnet.minecraft.client.Minecraft;Lnet.minecraft.client.multiplayer.ServerData;)V  # BHMenu-Forge-1.7.10-2.4.7.jar, CraftPresence-2.7.1+1.7.10-forge.jar, ServerRedirect-ForgeMC7-1.4.5.jar ...
  4  net.minecraft.client.multiplayer.PlayerControllerMP.gameIsSurvivalOrAdventure()Z  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, ApolloClient-v1.3.1.b21-2026-11-06.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar ...
  4  net.minecraft.client.multiplayer.PlayerControllerMP.syncCurrentPlayItem()V  # backhand-1.4.1.jar, effortless-0.10.1.jar, okcore-26.11.15.1.jar ...
  4  net.minecraft.client.multiplayer.ServerAddress.func_78860_a(Ljava.lang.String;)Lnet.minecraft.client.multiplayer.ServerAddress;  # ApolloClient-v1.3.1.b21-2026-11-06.jar, Cloudflared-7.10.3.jar, PingDisplay.jar ...
  4  net.minecraft.client.renderer.EntityRenderer.activateNextShader()V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, Alfheim-67.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar ...
  4  net.minecraft.client.renderer.entity.RenderFallingBlock.<init>()V  # EnviroMine-1.3.132.jar, Game of Thrones 24.08.29.jar, HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar ...
  4  net.minecraft.client.renderer.entity.RenderManager.doRenderEntity(Lnet.minecraft.entity.Entity;DDDFFZ)Z  # MatterOverdrive-1.7.10-0.4.5.jar, ThaumicHorizons-1.9.0.jar, manametalmod-8.0.8-1.jar ...
  4  net.minecraft.client.renderer.entity.RenderPlayer.renderEquippedItems(Lnet.minecraft.client.entity.AbstractClientPlayer;F)V  # AM2.5-LTS-1.6.7.jar, NarutoDestinationMod-1.36.1.jar, etfuturum-2.6.2.jar ...
  4  net.minecraft.client.resources.data.IMetadataSerializer.registerMetadataSectionType(Lnet.minecraft.client.resources.data.IMetadataSectionSerializer;Ljava.lang.Class;)V  # Factorization-1.7.10-0.8.109.jar, MatterOverdrive-1.7.10-0.4.5.jar, immibis-core-59.1.4.jar ...
  4  net.minecraft.client.resources.data.PackMetadataSection.<init>(Lnet.minecraft.util.IChatComponent;I)V  # BetterFoliage-LegacyEdition-1.2.4.jar, CatFrame-0.10.10.jar, CatFrame-0.10.11.jar ...
  4  net.minecraft.util.ScreenShotHelper.saveScreenshot(Ljava.io.File;IILnet.minecraft.client.shader.Framebuffer;)Lnet.minecraft.util.IChatComponent;  # [1.7.10] SecurityCraft v1.8.13.jar, compatlib-1.7.10.jar, optimizationsandtweaks-V1.18.0.jar ...
  3  net.minecraft.client.Minecraft.getWorldProviderName()Ljava.lang.String;  # ApolloClient-v1.3.1.b21-2026-11-06.jar, zombieplague2.jar, 永爱之剑0.1.jar
  3  net.minecraft.client.audio.SoundHandler.missing_sound:Lnet.minecraft.client.audio.SoundPoolEntry;  # HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, matmos-1.7.10-36.0.1.jar, okcore-26.11.15.1.jar
  3  net.minecraft.client.audio.SoundManager.sndSystem:Lnet.minecraft.client.audio.SoundManager$SoundSystemStarterThread;  # DynamicSurroundings-1.7.10-1.0.9.0.jar, OpenComputers-MC1.7.10-1.8.10+667626d-universal.jar, matmos-1.7.10-36.0.1.jar
  3  net.minecraft.client.audio.SoundPoolEntry.getSoundPoolEntryLocation()Lnet.minecraft.util.ResourceLocation;  # OpenComputers-MC1.7.10-1.8.10+667626d-universal.jar, matmos-1.7.10-36.0.1.jar, okcore-26.11.15.1.jar
  3  net.minecraft.client.entity.EntityClientPlayerMP.getHorseJumpPower()F  # ApolloClient-v1.3.1.b21-2026-11-06.jar, Game of Thrones 24.08.29.jar, 永爱之剑0.1.jar
  3  net.minecraft.client.entity.EntityClientPlayerMP.isRidingHorse()Z  # ApolloClient-v1.3.1.b21-2026-11-06.jar, theoffhandmod-1.1.4.jar, 永爱之剑0.1.jar
  3  net.minecraft.client.gui.ChatLine.<init>(ILnet.minecraft.util.IChatComponent;I)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, mcheli-d4e3692.jar, tabbychat-1.12.2.jar
  3  net.minecraft.client.gui.GuiIngame.renderGameOverlay(FZII)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, divineintervention-1.0.0-1.7.10.jar, stereoscopic-angelica-1.7.10-0.3.0.jar
  3  net.minecraft.client.gui.GuiNewChat.getChatComponent(II)Lnet.minecraft.util.IChatComponent;  # CustomCursorMod1.2.2-1.7.10.jar, Game of Thrones 24.08.29.jar, manametalmod-8.0.8-1.jar
  3  net.minecraft.client.gui.GuiOptionSlider.<init>(IIILnet.minecraft.client.settings.GameSettings$Options;)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, Controlling-1.7.10-1.0.0.4.jar, zombieplague2.jar
  3  net.minecraft.client.gui.GuiShareToLan.<init>(Lnet.minecraft.client.gui.GuiScreen;)V  # EasyLAN-v1.5-[1.7.10].jar, NarutoDestinationMod-1.36.1.jar, zombieplague2.jar
  3  net.minecraft.client.gui.GuiSleepMP.<init>()V  # Salutation-1.7.10-1.0.2.jar, manametalmod-8.0.8-1.jar, optimizationsandtweaks-V1.18.0.jar
  3  net.minecraft.client.gui.inventory.CreativeCrafting.<init>(Lnet.minecraft.client.Minecraft;)V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar, inventorypets-1.7.10-1.5.2.jar
  3  net.minecraft.client.multiplayer.PlayerControllerMP.sendPacketDropItem(Lnet.minecraft.item.ItemStack;)V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar, inventorypets-1.7.10-1.5.2.jar
  3  net.minecraft.client.multiplayer.ServerAddress.getIP()Ljava.lang.String;  # ApolloClient-v1.3.1.b21-2026-11-06.jar, PingDisplay.jar, ServerUtilities-2.5.2.jar
  3  net.minecraft.client.multiplayer.ServerAddress.getPort()I  # ApolloClient-v1.3.1.b21-2026-11-06.jar, PingDisplay.jar, ServerUtilities-2.5.2.jar
  3  net.minecraft.client.network.NetHandlerLoginClient.<init>(Lnet.minecraft.network.NetworkManager;Lnet.minecraft.client.Minecraft;Lnet.minecraft.client.gui.GuiScreen;)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, LoTAS1.7.10-2.0.0.jar, steambridge-1.2-Forge-1.7.10.jar
  3  net.minecraft.client.renderer.EntityRenderer.renderWorld(FJ)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, MatterOverdrive-1.7.10-0.4.5.jar, stereoscopic-angelica-1.7.10-0.3.0.jar
  3  net.minecraft.client.renderer.OpenGlHelper.glUniformMatrix4(IZLjava.nio.FloatBuffer;)V  # IvToolkit-1.2.1.jar, UniversalModCore-1.7.10-forge-1.3.1.jar, curvy_pipes-1.7.10-1.15.12.jar
  3  net.minecraft.client.renderer.RenderGlobal.renderEntities(Lnet.minecraft.entity.EntityLivingBase;Lnet.minecraft.client.renderer.culling.ICamera;F)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, npcdbc-1.2.1.jar, swansong-mc1.7.10-1.3.7.jar
  3  net.minecraft.client.renderer.Tessellator.reset()V  # Factorization-1.7.10-0.8.109.jar, gtnhlib-0.12.4.jar, swansong-mc1.7.10-1.3.7.jar
  3  net.minecraft.client.renderer.WorldRenderer.getGLCallListForPass(I)I  # Factorization-1.7.10-0.8.109.jar, neodymium-mc1.7.10-0.4.3-unofficial.jar, optimizationsandtweaks-V1.18.0.jar
  3  net.minecraft.client.renderer.entity.RenderEnderman.<init>()V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  3  net.minecraft.client.renderer.entity.RenderLightningBolt.<init>()V  # HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, Pokecube Legacy 1.3.3.jar, ThaumicHorizons-1.9.0.jar
  3  net.minecraft.client.renderer.texture.DynamicTexture.deleteGlTexture()V  # CustomPlayerModels-1.7.10-0.6.27a.jar, hfr-1.0.2.2+mc1.7.10-forge.jar, journeymap-forge-1.7.10-6.0.8.jar
  3  net.minecraft.client.renderer.texture.TextureManager.<init>(Lnet.minecraft.client.resources.IResourceManager;)V -> ()V  # Factorization-1.7.10-0.8.109.jar, mcheli-d4e3692.jar, optimizationsandtweaks-V1.18.0.jar
  3  net.minecraft.client.renderer.texture.TextureUtil.allocateTextureImpl(IIIIF)V  # Xaeros_Minimap_21.10.44_Forge_1.7.10.jar, Xaeros_Minimap_FP21.10.40_Forge_1.7.10.jar, swansong-mc1.7.10-1.3.7.jar
  3  net.minecraft.client.shader.Framebuffer.checkFramebufferComplete()V  # HandmadeGunsOverdrive-4.0.5.1.jar, mcheli-d4e3692.jar, modularui2-2.4.0-1.7.10.jar
  3  net.minecraft.client.shader.ShaderLinkHelper.getStaticShaderLinkHelper()Lnet.minecraft.client.shader.ShaderLinkHelper;  # 1.7.10-ASJCore-1.7.0.3.jar, ApolloClient-v1.3.1.b21-2026-11-06.jar, optimizationsandtweaks-V1.18.0.jar
  3  net.minecraft.client.shader.ShaderLinkHelper.setNewStaticShaderLinkHelper()V  # 1.7.10-ASJCore-1.7.0.3.jar, ApolloClient-v1.3.1.b21-2026-11-06.jar, optimizationsandtweaks-V1.18.0.jar
  3  net.minecraft.network.login.client.C00PacketLoginStart.<init>(Lcom.mojang.authlib.GameProfile;)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, LoTAS1.7.10-2.0.0.jar, steambridge-1.2-Forge-1.7.10.jar
  3  net.minecraft.network.play.client.C02PacketUseEntity$Action.ATTACK:Lnet.minecraft.network.play.client.C02PacketUseEntity$Action;  # Game of Thrones 24.08.29.jar, Paradigm-forge-1.7.10-2.5.4.jar, backhand-1.4.1.jar
  3  net.minecraft.network.play.client.C02PacketUseEntity.getAction()Lnet.minecraft.network.play.client.C02PacketUseEntity$Action;  # Game of Thrones 24.08.29.jar, Paradigm-forge-1.7.10-2.5.4.jar, backhand-1.4.1.jar
  3  net.minecraft.util.MouseFilter.<init>()V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, optimizationsandtweaks-V1.18.0.jar, swansong-mc1.7.10-1.3.7.jar
  3  net.minecraftforge.client.ForgeHooksClient.renderMainMenu(Lnet.minecraft.client.gui.GuiMainMenu;Lnet.minecraft.client.gui.FontRenderer;II)V  # Game of Thrones 24.08.29.jar, aether-1.7.10-v1.1.2.5.jar, fossilsarcheology-7.3.2.jar
  3  net.minecraftforge.client.GuiIngameForge.<init>(Lnet.minecraft.client.Minecraft;)V -> ()V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, optimizationsandtweaks-V1.18.0.jar, 永爱之剑0.1.jar
  3  org.lwjgl.opengl.ContextCapabilities.GL_EXT_pixel_buffer_object:Z  # XaerosWorldMap_1.14.1.33_Forge_1.7.10.jar, Xaeros_Minimap_21.10.44_Forge_1.7.10.jar, Xaeros_Minimap_FP21.10.40_Forge_1.7.10.jar
  3  org.lwjgl.opengl.GL11.glNormalPointer(ILjava.nio.ByteBuffer;)V  # curvy_pipes-1.7.10-1.15.12.jar, hfr-1.0.2.2+mc1.7.10-forge.jar, optimizationsandtweaks-V1.18.0.jar
  3  org.lwjgl.opengl.GL11.glTexCoordPointer(IILjava.nio.ShortBuffer;)V  # hfr-1.0.2.2+mc1.7.10-forge.jar, optimizationsandtweaks-V1.18.0.jar, rple-mc1.7.10-2.2.4.jar
  2  cpw.mods.fml.client.FMLClientHandler.connectToServerAtStartup(Ljava.lang.String;I)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, optimizationsandtweaks-V1.18.0.jar
  2  cpw.mods.fml.client.GuiModList.drawLine(Ljava.lang.String;II)I  # 1.7.10-ASJCore-1.7.0.3.jar, schematica_plus-1.0.0-beta.4.jar
  2  net.minecraft.client.Minecraft.debugInfoEntities()Ljava.lang.String;  # ApolloClient-v1.3.1.b21-2026-11-06.jar, 永爱之剑0.1.jar
  2  net.minecraft.client.Minecraft.debugInfoRenders()Ljava.lang.String;  # ApolloClient-v1.3.1.b21-2026-11-06.jar, 永爱之剑0.1.jar
  2  net.minecraft.client.Minecraft.getEntityDebug()Ljava.lang.String;  # ApolloClient-v1.3.1.b21-2026-11-06.jar, 永爱之剑0.1.jar
  2  net.minecraft.client.Minecraft.mcDefaultResourcePack:Lnet.minecraft.client.resources.DefaultResourcePack;  # ResourcePackOrganizer  MC-1.7.10  v1.0.2.jar, modernsplash-1.3.1.jar
  2  net.minecraft.client.Minecraft.mouseHelper:Lnet.minecraft.util.MouseHelper;  # manametalmod-8.0.8-1.jar, rawinput-1.7.10-v1.4.2.jar
  2  net.minecraft.client.Minecraft.scheduleResourcesRefresh()V  # Pokecube Legacy 1.3.3.jar, mcpatcher-mc1.7.10-0.3.0.jar
  2  net.minecraft.client.audio.PositionedSoundRecord.attenuationType:Lnet.minecraft.client.audio.ISound$AttenuationType;  # Game of Thrones 24.08.29.jar, matmos-1.7.10-36.0.1.jar
  2  net.minecraft.client.audio.SoundEventAccessorComposite.<init>(Lnet.minecraft.util.ResourceLocation;DDLnet.minecraft.client.audio.SoundCategory;)V  # Game of Thrones 24.08.29.jar, UniversalModCore-1.7.10-forge-1.3.1.jar
  2  net.minecraft.client.audio.SoundEventAccessorComposite.addSoundToEventPool(Lnet.minecraft.client.audio.ISoundEventAccessor;)V  # Game of Thrones 24.08.29.jar, UniversalModCore-1.7.10-forge-1.3.1.jar
  2  net.minecraft.client.audio.SoundManager.playingSounds:Ljava.util.Map;  # DynamicSurroundings-1.7.10-1.0.9.0.jar, UniversalModCore-1.7.10-forge-1.3.1.jar
  2  net.minecraft.client.audio.SoundPoolEntry.<init>(Lnet.minecraft.util.ResourceLocation;DDZ)V  # Game of Thrones 24.08.29.jar, UniversalModCore-1.7.10-forge-1.3.1.jar
  2  net.minecraft.client.audio.SoundRegistry.registerSound(Lnet.minecraft.client.audio.SoundEventAccessorComposite;)V  # Game of Thrones 24.08.29.jar, UniversalModCore-1.7.10-forge-1.3.1.jar
  2  net.minecraft.client.gui.ChatLine.getChatLineID()I  # ApolloClient-v1.3.1.b21-2026-11-06.jar, tabbychat-1.12.2.jar
  2  net.minecraft.client.gui.ChatLine.getUpdatedCounter()I  # ApolloClient-v1.3.1.b21-2026-11-06.jar, tabbychat-1.12.2.jar
  2  net.minecraft.client.gui.GuiButtonLanguage.<init>(III)V  # aether-1.7.10-v1.1.2.5.jar, narutomod-0.5.2.jar
  2  net.minecraft.client.gui.GuiChat.supportedProtocols:Ljava.util.Set;  # 1.7.10-ASJCore-1.7.0.3.jar, UniversalModCore-1.7.10-forge-1.3.1.jar
  2  net.minecraft.client.gui.GuiControls.keyBindingList:Lnet.minecraft.client.gui.GuiKeyBindingList;  # BetterSprinting-1.7.10-v2.3.0.jar, Controlling-1.7.10-1.0.0.4.jar
  2  net.minecraft.client.gui.GuiDownloadTerrain.<init>(Lnet.minecraft.client.network.NetHandlerPlayClient;)V  # Game of Thrones 24.08.29.jar, manametalmod-8.0.8-1.jar
  2  net.minecraft.client.gui.GuiIngame.renderStreamIndicator(FF)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.gui.GuiMerchant.getMerchant()Lnet.minecraft.entity.IMerchant;  # FSP-1.7.10-0.29.3.jar, itemscroller-1.7.10-0.12.0-alpha.1.jar
  2  net.minecraft.client.gui.GuiOptionsRowList.<init>(Lnet.minecraft.client.Minecraft;IIIII[Lnet.minecraft.client.settings.GameSettings$Options;)V  # TipTheScales-1.7.10-3.0.0.2.jar, manametalmod-8.0.8-1.jar
  2  net.minecraft.client.gui.GuiScreenRealmsProxy.func_154321_a()Lnet.minecraft.realms.RealmsScreen;  # 1.7.10-ASJCore-1.7.0.3.jar, CraftPresence-2.7.1+1.7.10-forge.jar
  2  net.minecraft.client.gui.GuiScreenWorking.<init>()V  # Notes-1.7.10-1.1.1-forge.jar, Pokecube Legacy 1.3.3.jar
  2  net.minecraft.client.gui.GuiTextField.func_146197_a(IIZ)I  # Salutation-1.7.10-1.0.2.jar, tabbychat-1.12.2.jar
  2  net.minecraft.client.gui.GuiYesNo.setButtonDelay(I)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, 永爱之剑0.1.jar
  2  net.minecraft.client.gui.achievement.GuiAchievement.displayUnformattedAchievement(Lnet.minecraft.stats.Achievement;)V  # HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, IC2Classic-1.7.10-1.2.6.jar
  2  net.minecraft.client.gui.achievement.GuiAchievement.updateAchievementWindow()V  # optimizationsandtweaks-V1.18.0.jar, stereoscopic-angelica-1.7.10-0.3.0.jar
  2  net.minecraft.client.gui.inventory.GuiContainer.updateDragSplitting()V  # cybersus_main-1.6.1.2.jar, okcore-26.11.15.1.jar
  2  net.minecraft.client.model.ModelBiped.Bbreast2:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.Bbreast:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.Bleftarm:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.Brightarm:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.b:I  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.body:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.bottom2:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.bottom:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.hip2:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.hip:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.leftleg:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.rightleg:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.model.ModelBiped.waist:Lnet.minecraft.client.model.ModelRenderer;  # NBArmors-3.1.jar, npcdbc-1.2.1.jar
  2  net.minecraft.client.multiplayer.GuiConnecting.<init>(Lnet.minecraft.client.gui.GuiScreen;Lnet.minecraft.client.Minecraft;Ljava.lang.String;I)V  # narutomod-0.5.2.jar, portalfriends-forge-1.1.1-1.7.10.jar
  2  net.minecraft.client.multiplayer.PlayerControllerMP.clickBlockCreative(Lnet.minecraft.client.Minecraft;Lnet.minecraft.client.multiplayer.PlayerControllerMP;IIII)V  # backhand-1.4.1.jar, theoffhandmod-1.1.4.jar
  2  net.minecraft.client.multiplayer.ServerData.<init>(Ljava.lang.String;Ljava.lang.String;Z)V -> (Ljava.lang.String;Ljava.lang.String;)V  # CraftPresence-2.7.1+1.7.10-forge.jar, ServerRedirect-ForgeMC7-1.4.5.jar
  2  net.minecraft.client.multiplayer.ServerData.getBase64EncodedIconData()Ljava.lang.String;  # CraftPresence-2.7.1+1.7.10-forge.jar, steambridge-1.2-Forge-1.7.10.jar
  2  net.minecraft.client.multiplayer.ServerData.isLanServer()Z  # CarbonConfig-1.7.10-2.0.2.1.jar, CraftPresence-2.7.1+1.7.10-forge.jar
  2  net.minecraft.client.multiplayer.WorldClient.clientChunkProvider:Lnet.minecraft.client.multiplayer.ChunkProviderClient;  # Factorization-1.7.10-0.8.109.jar, zombieplague2.jar
  2  net.minecraft.client.network.OldServerPinger.<init>()V  # BHMenu-Forge-1.7.10-2.4.7.jar, CraftPresence-2.7.1+1.7.10-forge.jar
  2  net.minecraft.client.renderer.EntityRenderer.getNightVisionBrightness(Lnet.minecraft.entity.player.EntityPlayer;F)F  # rple-mc1.7.10-2.2.4.jar, swansong-mc1.7.10-1.3.7.jar
  2  net.minecraft.client.renderer.EntityRenderer.hurtCameraEffect(F)V  # ArmoredArms-v1.3.9-1.7.10-release.jar, swansong-mc1.7.10-1.3.7.jar
  2  net.minecraft.client.renderer.EntityRenderer.random:Ljava.util.Random;  # Alfheim-67.jar, zombieplague2.jar
  2  net.minecraft.client.renderer.EntityRenderer.setupViewBobbing(F)V  # ArmoredArms-v1.3.9-1.7.10-release.jar, swansong-mc1.7.10-1.3.7.jar
  2  net.minecraft.client.renderer.EntityRenderer.updateRenderer()V  # Game of Thrones 24.08.29.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.renderer.RenderGlobal.<init>(Lnet.minecraft.client.Minecraft;)V -> ()V  # Factorization-1.7.10-0.8.109.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.renderer.RenderGlobal.hasCloudFog(DDDF)Z  # ApolloClient-v1.3.1.b21-2026-11-06.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.renderer.RenderGlobal.mapSoundPositions:Ljava.util.Map;  # Alfheim-67.jar, HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar
  2  net.minecraft.client.renderer.RenderGlobal.onEntityCreate(Lnet.minecraft.entity.Entity;)V  # CustomNPC-Plus-1.11.1.jar, Factorization-1.7.10-0.8.109.jar
  2  net.minecraft.client.renderer.RenderGlobal.onEntityDestroy(Lnet.minecraft.entity.Entity;)V  # CustomNPC-Plus-1.11.1.jar, Factorization-1.7.10-0.8.109.jar
  2  net.minecraft.client.renderer.RenderGlobal.renderClouds(F)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.renderer.entity.Render.updateIcons(Lnet.minecraft.client.renderer.texture.IIconRegister;)V  # OpenComputers-MC1.7.10-1.8.10+667626d-universal.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.renderer.entity.RenderBlaze.<init>()V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, The Titans MC 1.7.10 version 0.45.jar
  2  net.minecraft.client.renderer.entity.RenderLiving.canRenderName(Lnet.minecraft.entity.EntityLiving;)Z  # CustomNPC-Plus-1.11.1.jar, NotEnoughPets-1.7.10-2.4.1.jar
  2  net.minecraft.client.renderer.entity.RenderLiving.func_110827_b(Lnet.minecraft.entity.EntityLiving;DDDFF)V  # CustomNPC-Plus-1.11.1.jar, LycanitesMobsComplete 1.13.0.5 [1.7.10].jar
  2  net.minecraft.client.renderer.entity.RenderManager.renderEntityStatic(Lnet.minecraft.entity.Entity;FZ)Z  # Game of Thrones 24.08.29.jar, manametalmod-8.0.8-1.jar
  2  net.minecraft.client.renderer.entity.RenderMooshroom.<init>(Lnet.minecraft.client.model.ModelBase;F)V  # NotEnoughPets-1.7.10-2.4.1.jar, etfuturum-2.6.2.jar
  2  net.minecraft.client.renderer.entity.RenderPlayer.renderLivingAt(Lnet.minecraft.client.entity.AbstractClientPlayer;DDD)V  # ApolloClient-v1.3.1.b21-2026-11-06.jar, narutomod-0.5.2.jar
  2  net.minecraft.client.renderer.entity.RenderPlayer.renderOffsetLivingLabel(Lnet.minecraft.client.entity.AbstractClientPlayer;DDDLjava.lang.String;FD)V  # cybersus_main-1.6.1.2.jar, narutomod-0.5.2.jar
  2  net.minecraft.client.renderer.entity.RenderSquid.<init>(Lnet.minecraft.client.model.ModelBase;F)V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraft.client.renderer.entity.RenderSquid.doRender(Lnet.minecraft.entity.passive.EntitySquid;DDDFF)V  # 1.7.10-TerraFirmaCraft-0.79.30.925.jar, TerraFirmaCraft-TFRU-1.7.10-0.82.3.jar
  2  net.minecraft.client.renderer.entity.RenderTNTPrimed.<init>()V  # HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, Improving Minecraft-1.7.10-v5.jar
  2  net.minecraft.client.renderer.tileentity.RenderEnderCrystal.<init>()V  # HardcoreEnderExpansion  MC-1.7.10  v1.8.6.jar, etfuturum-2.6.2.jar
  2  net.minecraft.client.renderer.tileentity.TileEntitySignRenderer.<init>()V  # Floocraft-1.7.10-1.7.jar, shaderfixer-5.5-FINAL.jar
  2  net.minecraft.client.resources.ResourcePackRepository.<init>(Ljava.io.File;Ljava.io.File;Lnet.minecraft.client.resources.IResourcePack;Lnet.minecraft.client.resources.data.IMetadataSerializer;Lnet.minecraft.client.settings.GameSettings;)V -> ()V  # ResourcePackOrganizer  MC-1.7.10  v1.0.2.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.resources.SimpleReloadableResourceManager.<init>(Lnet.minecraft.client.resources.data.IMetadataSerializer;)V -> ()V  # Factorization-1.7.10-0.8.109.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.resources.data.AnimationMetadataSectionSerializer.<init>()V  # Factorization-1.7.10-0.8.109.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.resources.data.FontMetadataSectionSerializer.<init>()V  # Factorization-1.7.10-0.8.109.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.resources.data.LanguageMetadataSectionSerializer.<init>()V  # Factorization-1.7.10-0.8.109.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.resources.data.PackMetadataSection.func_152805_a()Lnet.minecraft.util.IChatComponent;  # CatFrame-0.10.10.jar, CatFrame-0.10.11.jar
  2  net.minecraft.client.resources.data.PackMetadataSection.getPackFormat()I  # CatFrame-0.10.10.jar, CatFrame-0.10.11.jar
  2  net.minecraft.client.resources.data.PackMetadataSectionSerializer.<init>()V  # Factorization-1.7.10-0.8.109.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.client.resources.data.TextureMetadataSectionSerializer.<init>()V  # Factorization-1.7.10-0.8.109.jar, optimizationsandtweaks-V1.18.0.jar
  2  net.minecraft.network.NetHandlerPlayServer.processPlayerDigging(Lnet.minecraft.network.play.client.C07PacketPlayerDigging;)V  # Game of Thrones 24.08.29.jar, schematica_plus-1.0.0-beta.4.jar
  2  net.minecraft.network.play.client.C02PacketUseEntity$Action.INTERACT:Lnet.minecraft.network.play.client.C02PacketUseEntity$Action;  # Game of Thrones 24.08.29.jar, backhand-1.4.1.jar
  2  net.minecraft.network.play.client.C02PacketUseEntity.getEntityFromWorld(Lnet.minecraft.world.World;)Lnet.minecraft.entity.Entity;  # Game of Thrones 24.08.29.jar, backhand-1.4.1.jar
  2  net.minecraft.network.play.client.C0EPacketClickWindow.getWindowId()I  # AM2.5-LTS-1.6.7.jar, Paradigm-forge-1.7.10-2.5.4.jar
  2  net.minecraft.network.play.client.C14PacketTabComplete.<init>(Ljava.lang.String;)V  # Salutation-1.7.10-1.0.2.jar, tabbychat-1.12.2.jar
```

## mod-side API (top 150 of 413)

```
  7  com.mojang.realmsclient.gui.ChatFormatting.RED:Lcom.mojang.realmsclient.gui.ChatFormatting;  # AdvancedBackups-forge-1.7.10-3.7.1.jar, AntMan-Beta-1.7.10-1.5.4.jar, BetterLineBreak-1.7.10(Forge)-1.0.1.jar ...
  6  com.mojang.realmsclient.gui.ChatFormatting.GREEN:Lcom.mojang.realmsclient.gui.ChatFormatting;  # AdvancedBackups-forge-1.7.10-3.7.1.jar, AntMan-Beta-1.7.10-1.5.4.jar, BetterLineBreak-1.7.10(Forge)-1.0.1.jar ...
  6  com.mojang.realmsclient.gui.ChatFormatting.YELLOW:Lcom.mojang.realmsclient.gui.ChatFormatting;  # AntMan-Beta-1.7.10-1.5.4.jar, BetterLineBreak-1.7.10(Forge)-1.0.1.jar, TechReborn-1.7.10-0.7.21.1131-universal.jar ...
  5  codechicken.nei.NEIClientConfig.logger:Lorg.apache.logging.log4j.Logger;  # AntMan-Beta-1.7.10-1.5.4.jar, DangerRPG-1.7.10-1.1.4.9.jar, IronMan-1.7.10-1.1.0.jar ...
  5  codechicken.nei.recipe.TemplateRecipeHandler$RecipeTransferRectHandler.registerRectsToGuis(Ljava.util.List;Ljava.util.List;)V  # HBM-NTM-.1.0.27_X5853_H261.jar, HBM-NTM-[1.0.27_X5687].jar, HBM-NTM1.0.27_X5771_r.12.jar ...
  5  com.mojang.realmsclient.gui.ChatFormatting.GRAY:Lcom.mojang.realmsclient.gui.ChatFormatting;  # AntMan-Beta-1.7.10-1.5.4.jar, BetterLineBreak-1.7.10(Forge)-1.0.1.jar, MineTradingCards-1.7.10-3.1.4.jar ...
  5  com.mojang.realmsclient.gui.ChatFormatting.RESET:Lcom.mojang.realmsclient.gui.ChatFormatting;  # EnviroMine-1.3.132.jar, MineTradingCards-1.7.10-3.1.4.jar, UniversalModCore-1.7.10-forge-1.3.1.jar ...
  4  codechicken.nei.NEIClientConfig.getBooleanSetting(Ljava.lang.String;)Z  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar, OpenComputers-MC1.7.10-1.8.10+667626d-universal.jar, ae2_auto_pattern_upload-2.0.0-GTNH_290.jar ...
  4  codechicken.nei.recipe.HandlerInfo$Builder.<init>(Ljava.lang.String;Ljava.lang.String;Ljava.lang.String;)V  # AM2.5-LTS-1.6.7.jar, manametalmod-8.0.8-1.jar, neoecoae-7.3.12.jar ...
  4  codechicken.nei.recipe.HandlerInfo$Builder.build()Lcodechicken.nei.recipe.HandlerInfo;  # AM2.5-LTS-1.6.7.jar, manametalmod-8.0.8-1.jar, neoecoae-7.3.12.jar ...
  4  codechicken.nei.recipe.HandlerInfo$Builder.setDisplayStack(Lnet.minecraft.item.ItemStack;)Lcodechicken.nei.recipe.HandlerInfo$Builder;  # AM2.5-LTS-1.6.7.jar, manametalmod-8.0.8-1.jar, neoecoae-7.3.12.jar ...
  4  codechicken.nei.recipe.HandlerInfo$Builder.setHeight(I)Lcodechicken.nei.recipe.HandlerInfo$Builder;  # AM2.5-LTS-1.6.7.jar, manametalmod-8.0.8-1.jar, neoecoae-7.3.12.jar ...
  4  codechicken.nei.recipe.IRecipeHandler.getOverlayIdentifier()Ljava.lang.String;  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar, emi-1.1.24-beta.4+1.7.10+forge.jar, neoecoae-7.3.12.jar ...
  4  codechicken.nei.recipe.ShapelessRecipeHandler.stackorder:[[I  # BloodMagic-1.7.10-1.3.3-17.jar, Mekanism-1.7.10-9.1.1.1031.jar, Mekanism-Community-Edition-1.7.10-9.10.51-Core.jar ...
  4  com.mojang.realmsclient.gui.ChatFormatting.AQUA:Lcom.mojang.realmsclient.gui.ChatFormatting;  # AntMan-Beta-1.7.10-1.5.4.jar, EnviroMine-1.3.132.jar, UniversalModCore-1.7.10-forge-1.3.1.jar ...
  4  com.mojang.realmsclient.gui.ChatFormatting.DARK_PURPLE:Lcom.mojang.realmsclient.gui.ChatFormatting;  # TConstruct-1.7.10-1.8.8.jar, ToolUpgrades+1.7.10++(v.1.2.3).jar, UniversalModCore-1.7.10-forge-1.3.1.jar ...
  4  com.mojang.realmsclient.gui.ChatFormatting.ITALIC:Lcom.mojang.realmsclient.gui.ChatFormatting;  # BetterLineBreak-1.7.10(Forge)-1.0.1.jar, TConstruct-1.7.10-1.8.8.jar, UniversalModCore-1.7.10-forge-1.3.1.jar ...
  4  com.mojang.realmsclient.gui.ChatFormatting.WHITE:Lcom.mojang.realmsclient.gui.ChatFormatting;  # BetterLineBreak-1.7.10(Forge)-1.0.1.jar, UniversalModCore-1.7.10-forge-1.3.1.jar, okcore-26.11.15.1.jar ...
  3  baubles.common.items.ItemRing.<init>()V  # thaumicboots-1.3.1.2.jar, thaumicinsurgence-0.0.2.6.jar, thaumicinsurgence-0.2.6.jar
  3  baubles.common.items.ItemRing.getUnlocalizedName()Ljava.lang.String;  # thaumicboots-1.3.1.2.jar, thaumicinsurgence-0.0.2.6.jar, thaumicinsurgence-0.2.6.jar
  3  baubles.common.items.ItemRing.icon:Lnet.minecraft.util.IIcon;  # thaumicboots-1.3.1.2.jar, thaumicinsurgence-0.0.2.6.jar, thaumicinsurgence-0.2.6.jar
  3  baubles.common.items.ItemRing.setCreativeTab(Lnet.minecraft.creativetab.CreativeTabs;)Lnet.minecraft.item.Item;  # thaumicboots-1.3.1.2.jar, thaumicinsurgence-0.0.2.6.jar, thaumicinsurgence-0.2.6.jar
  3  baubles.common.items.ItemRing.setMaxStackSize(I)Lnet.minecraft.item.Item;  # thaumicboots-1.3.1.2.jar, thaumicinsurgence-0.0.2.6.jar, thaumicinsurgence-0.2.6.jar
  3  baubles.common.items.ItemRing.setUnlocalizedName(Ljava.lang.String;)Lnet.minecraft.item.Item;  # thaumicboots-1.3.1.2.jar, thaumicinsurgence-0.0.2.6.jar, thaumicinsurgence-0.2.6.jar
  3  codechicken.nei.ItemPanels.bookmarkPanel:Lcodechicken.nei.BookmarkPanel;  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar, modularui2-2.4.0-1.7.10.jar, schematica_plus-1.0.0-beta.4.jar
  3  codechicken.nei.NEIClientUtils.shiftKey()Z  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar, campfirebackport-1.7.10-1.11.3.jar, mobsinfo-0.6.2.jar
  3  codechicken.nei.api.API.registerBookmarkContainerHandler(Ljava.lang.Class;Lcodechicken.nei.api.IBookmarkContainerHandler;)V  # IronChest-6.2.0.jar, ezstorage-1.8.3.jar, statpatterns-aa751b2-1-7-10+aa751b2356-dirty.jar
  3  codechicken.nei.recipe.FurnaceRecipeHandler.afuels:Ljava.util.ArrayList;  # Factorization-1.7.10-0.8.109.jar, Game of Thrones 24.08.29.jar, NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  3  codechicken.nei.recipe.HandlerInfo$Builder.setWidth(I)Lcodechicken.nei.recipe.HandlerInfo$Builder;  # AM2.5-LTS-1.6.7.jar, neoecoae-7.3.12.jar, okprogressions-26.11.15.0.jar
  3  codechicken.nei.recipe.HandlerInfo.DEFAULT_WIDTH:I  # AM2.5-LTS-1.6.7.jar, emi-1.1.24-beta.4+1.7.10+forge.jar, jfmuy-26.11.15.0.jar
  3  codechicken.nei.recipe.StackInfo.getFluid(Lnet.minecraft.item.ItemStack;)Lnet.minecraftforge.fluids.FluidStack;  # ezstorage-1.8.3.jar, modularui2-2.4.0-1.7.10.jar, neoecoae-7.3.12.jar
  3  com.mojang.realmsclient.gui.ChatFormatting.DARK_GREEN:Lcom.mojang.realmsclient.gui.ChatFormatting;  # AntMan-Beta-1.7.10-1.5.4.jar, UniversalModCore-1.7.10-forge-1.3.1.jar, [1.7.10] The Rings of Power 24.07.19 (Forge).jar
  2  baubles.api.BaubleType.UNIVERSAL:Lbaubles.api.BaubleType;  # modularui2-2.4.0-1.7.10.jar, primal-0.20.0.jar
  2  baubles.api.expanded.BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType(Ljava.lang.String;)[I  # effortless-0.10.1.jar, etfuturum-2.6.2.jar
  2  baubles.api.expanded.BaubleExpandedSlots.tryAssignSlotOfType(Ljava.lang.String;)Z  # primal-0.20.0.jar, salisarcana-v2.10.0.jar
  2  baubles.api.expanded.BaubleExpandedSlots.tryAssignSlotsUpToMinimum(Ljava.lang.String;I)Z  # effortless-0.10.1.jar, etfuturum-2.6.2.jar
  2  baubles.api.expanded.BaubleExpandedSlots.tryRegisterType(Ljava.lang.String;)Z  # primal-0.20.0.jar, salisarcana-v2.10.0.jar
  2  baubles.api.expanded.BaubleItemHelper.addSlotInformation(Ljava.util.List;[Ljava.lang.String;)Z  # etfuturum-2.6.2.jar, okcore-26.11.15.1.jar
  2  baubles.api.expanded.BaubleItemHelper.onBaubleRightClick(Lnet.minecraft.item.ItemStack;Lnet.minecraft.world.World;Lnet.minecraft.entity.player.EntityPlayer;)Lnet.minecraft.item.ItemStack;  # etfuturum-2.6.2.jar, okcore-26.11.15.1.jar
  2  baubles.common.container.InventoryBaubles.blockEvents:Z  # Botania+r1.8-249.jar, ServerUtilities-2.5.2.jar
  2  baubles.common.container.InventoryBaubles.readNBT(Lnet.minecraft.nbt.NBTTagCompound;)V  # ServerUtilities-2.5.2.jar, deathlyhallows-1.1.8.jar
  2  baubles.common.container.InventoryBaubles.stackList:[Lnet.minecraft.item.ItemStack;  # Botania+r1.8-249.jar, ThaumicHorizons-1.9.0.jar
  2  baubles.common.container.SlotBauble.<init>(Lnet.minecraft.inventory.IInventory;Lbaubles.api.BaubleType;III)V -> (Lnet.minecraft.inventory.IInventory;III)V  # Botania+r1.8-249.jar, ServerUtilities-2.5.2.jar
  2  baubles.common.network.PacketHandler.INSTANCE:Lcpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;  # Alfheim-67.jar, Botania+r1.8-249.jar
  2  baubles.common.network.PacketSyncBauble.<init>(Lnet.minecraft.entity.player.EntityPlayer;I)V  # Alfheim-67.jar, Botania+r1.8-249.jar
  2  codechicken.nei.BookmarkPanel$BookmarkViewMode.DEFAULT:Lcodechicken.nei.BookmarkPanel$BookmarkViewMode;  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.BookmarkPanel.addRecipe(Lcodechicken.nei.recipe.Recipe;II)Z  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.BookmarkPanel.getGrid()Lcodechicken.nei.ItemsGrid;  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.ItemPanels.itemPanel:Lcodechicken.nei.ItemPanel;  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar, modularui2-2.4.0-1.7.10.jar
  2  codechicken.nei.ItemStackSet.contains(Lnet.minecraft.item.ItemStack;)Z  # Game of Thrones 24.08.29.jar, neverenoughanimations-v1.0.8-1-7-10+faadc07653-dirty.jar
  2  codechicken.nei.NEIClientConfig.getIntSetting(Ljava.lang.String;)I  # ApolloClient-v1.3.1.b21-2026-11-06.jar, NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  2  codechicken.nei.NEIClientUtils.getGuiContainer()Lnet.minecraft.client.gui.inventory.GuiContainer;  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar, neoecoae-7.3.12.jar
  2  codechicken.nei.NEIClientUtils.gl2DRenderContext(Ljava.lang.Runnable;)V  # emi-1.1.24-beta.4+1.7.10+forge.jar, neoecoae-7.3.12.jar
  2  codechicken.nei.SearchField.setFocus(Z)V  # modularui2-2.4.0-1.7.10.jar, okcore-26.11.15.1.jar
  2  codechicken.nei.api.API.addOption(Lcodechicken.nei.config.Option;)V  # OpenComputers-MC1.7.10-1.8.10+667626d-universal.jar, Thaumcraft4Tweaks-1.5.43.jar
  2  codechicken.nei.bookmark.BookmarkGrid.addGroup(Lcodechicken.nei.bookmark.BookmarkGroup;)I  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.event.NEIRegisterHandlerInfosEvent.registerHandlerInfo(Lcodechicken.nei.recipe.HandlerInfo;)V  # neoecoae-7.3.12.jar, okprogressions-26.11.15.0.jar
  2  codechicken.nei.guihook.GuiContainerManager.addSlotClickHandler(Lcodechicken.nei.guihook.IContainerSlotClickHandler;)V  # Mekanism-1.7.10-9.1.1.1031.jar, Mekanism-Community-Edition-1.7.10-9.10.51-Core.jar
  2  codechicken.nei.guihook.GuiContainerManager.getManager()Lcodechicken.nei.guihook.GuiContainerManager;  # modularui2-2.4.0-1.7.10.jar, neverenoughanimations-v1.0.8-1-7-10+faadc07653-dirty.jar
  2  codechicken.nei.recipe.FurnaceRecipeHandler$FuelPair.stack:Lcodechicken.nei.PositionedStack;  # Factorization-1.7.10-0.8.109.jar, Game of Thrones 24.08.29.jar
  2  codechicken.nei.recipe.FurnaceRecipeHandler$SmeltingPair.<init>(Lcodechicken.nei.recipe.FurnaceRecipeHandler;Lnet.minecraft.item.ItemStack;Lnet.minecraft.item.ItemStack;)V  # Game of Thrones 24.08.29.jar, etfuturum-2.6.2.jar
  2  codechicken.nei.recipe.GuiOverlayButton$ItemOverlayState.<init>(Lcodechicken.nei.PositionedStack;Z)V  # ezstorage-1.8.3.jar, modularui2-2.4.0-1.7.10.jar
  2  codechicken.nei.recipe.GuiRecipe.firstGui:Lnet.minecraft.client.gui.inventory.GuiContainer;  # Draconic-Evolution-1.7.10-1.0.2h.jar, TConstruct-1.7.10-1.8.8.jar
  2  codechicken.nei.recipe.GuiRecipe.getHandler()Lcodechicken.nei.recipe.IRecipeHandler;  # emi-1.1.24-beta.4+1.7.10+forge.jar, mobsinfo-0.6.2.jar
  2  codechicken.nei.recipe.GuiRecipe.getRecipeIndices()Ljava.util.List;  # emi-1.1.24-beta.4+1.7.10+forge.jar, mobsinfo-0.6.2.jar
  2  codechicken.nei.recipe.GuiRecipeButton$UpdateRecipeButtonsEvent$Post.buttonList:Ljava.util.List;  # neoecoae-7.3.12.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.recipe.GuiRecipeButton.<init>(Lcodechicken.nei.recipe.RecipeHandlerRef;IIILjava.lang.String;)V  # emi-1.1.24-beta.4+1.7.10+forge.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.recipe.GuiRecipeButton.handlerRef:Lcodechicken.nei.recipe.RecipeHandlerRef;  # emi-1.1.24-beta.4+1.7.10+forge.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.recipe.GuiRecipeButton.xPosition:I  # emi-1.1.24-beta.4+1.7.10+forge.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.recipe.GuiRecipeButton.yPosition:I  # emi-1.1.24-beta.4+1.7.10+forge.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.recipe.GuiRecipeTab.getHandlerInfo(Lcodechicken.nei.recipe.IRecipeHandler;)Lcodechicken.nei.recipe.HandlerInfo;  # emi-1.1.24-beta.4+1.7.10+forge.jar, jfmuy-26.11.15.0.jar
  2  codechicken.nei.recipe.GuiRecipeTab.handlerMap:Ljava.util.HashMap;  # AM2.5-LTS-1.6.7.jar, manametalmod-8.0.8-1.jar
  2  codechicken.nei.recipe.HandlerInfo$Builder.setShowOverlayButton(Z)Lcodechicken.nei.recipe.HandlerInfo$Builder;  # neoecoae-7.3.12.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.recipe.HandlerInfo.getHeight()I  # emi-1.1.24-beta.4+1.7.10+forge.jar, jfmuy-26.11.15.0.jar
  2  codechicken.nei.recipe.HandlerInfo.getWidth()I  # emi-1.1.24-beta.4+1.7.10+forge.jar, jfmuy-26.11.15.0.jar
  2  codechicken.nei.recipe.HandlerInfo.getYShift()I  # emi-1.1.24-beta.4+1.7.10+forge.jar, jfmuy-26.11.15.0.jar
  2  codechicken.nei.recipe.Recipe.getResult()Lnet.minecraft.item.ItemStack;  # emi-1.1.24-beta.4+1.7.10+forge.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.recipe.Recipe.of(Lcodechicken.nei.recipe.RecipeHandlerRef;)Lcodechicken.nei.recipe.Recipe;  # emi-1.1.24-beta.4+1.7.10+forge.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.recipe.RecipeCatalysts.getRecipeCatalysts(Lcodechicken.nei.recipe.IRecipeHandler;)Ljava.util.List;  # emi-1.1.24-beta.4+1.7.10+forge.jar, neoecoae-7.3.12.jar
  2  codechicken.nei.recipe.RecipeHandlerRef.handler:Lcodechicken.nei.recipe.IRecipeHandler;  # emi-1.1.24-beta.4+1.7.10+forge.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.recipe.RecipeHandlerRef.recipeIndex:I  # emi-1.1.24-beta.4+1.7.10+forge.jar, schematica_plus-1.0.0-beta.4.jar
  2  codechicken.nei.recipe.RecipeInfo.getStackPositioner(Lnet.minecraft.client.gui.inventory.GuiContainer;Ljava.lang.String;)Lcodechicken.nei.api.IStackPositioner;  # AntMan-Beta-1.7.10-1.5.4.jar, IronMan-1.7.10-1.1.0.jar
  2  codechicken.nei.recipe.ShapedRecipeHandler$CachedShapedRecipe.<init>(Lcodechicken.nei.recipe.ShapedRecipeHandler;Lnet.minecraft.item.crafting.ShapedRecipes;)V -> (Lcodechicken.nei.recipe.ShapedRecipeHandler;II[Ljava.lang.Object;Lnet.minecraft.item.ItemStack;)V  # Game of Thrones 24.08.29.jar, TechReborn-1.7.10-0.7.21.1131-universal.jar
  2  codechicken.nei.recipe.ShapedRecipeHandler.forgeShapedRecipe(Lnet.minecraftforge.oredict.ShapedOreRecipe;)Lcodechicken.nei.recipe.ShapedRecipeHandler$CachedShapedRecipe;  # Game of Thrones 24.08.29.jar, TechReborn-1.7.10-0.7.21.1131-universal.jar
  2  codechicken.nei.recipe.ShapelessRecipeHandler.forgeShapelessRecipe(Lnet.minecraftforge.oredict.ShapelessOreRecipe;)Lcodechicken.nei.recipe.ShapelessRecipeHandler$CachedShapelessRecipe;  # Game of Thrones 24.08.29.jar, TechReborn-1.7.10-0.7.21.1131-universal.jar
  2  codechicken.nei.recipe.TemplateRecipeHandler.getRecipeHeight(I)I  # emi-1.1.24-beta.4+1.7.10+forge.jar, jfmuy-26.11.15.0.jar
  2  codechicken.nei.util.ReadableNumberConverter.INSTANCE:Lcodechicken.nei.util.ReadableNumberConverter;  # ezstorage-1.8.3.jar, neverenoughanimations-v1.0.8-1-7-10+faadc07653-dirty.jar
  2  codechicken.nei.util.ReadableNumberConverter.toWideReadableForm(J)Ljava.lang.String;  # ezstorage-1.8.3.jar, neverenoughanimations-v1.0.8-1-7-10+faadc07653-dirty.jar
  2  com.mojang.realmsclient.gui.ChatFormatting.BLUE:Lcom.mojang.realmsclient.gui.ChatFormatting;  # MineTradingCards-1.7.10-3.1.4.jar, UniversalModCore-1.7.10-forge-1.3.1.jar
  2  com.mojang.realmsclient.gui.ChatFormatting.DARK_GRAY:Lcom.mojang.realmsclient.gui.ChatFormatting;  # AntMan-Beta-1.7.10-1.5.4.jar, UniversalModCore-1.7.10-forge-1.3.1.jar
  2  com.mojang.realmsclient.gui.ChatFormatting.DARK_RED:Lcom.mojang.realmsclient.gui.ChatFormatting;  # UniversalModCore-1.7.10-forge-1.3.1.jar, cybersus_main-1.6.1.2.jar
  2  com.mojang.realmsclient.gui.ChatFormatting.LIGHT_PURPLE:Lcom.mojang.realmsclient.gui.ChatFormatting;  # TechReborn-1.7.10-0.7.21.1131-universal.jar, UniversalModCore-1.7.10-forge-1.3.1.jar
  2  com.mojang.realmsclient.gui.ChatFormatting.stripFormatting(Ljava.lang.String;)Ljava.lang.String;  # geckolib-unofficial-1.7.10-1.0.3.jar, npcgecko-1.3.jar
  2  mcp.mobius.waila.api.impl.ModuleRegistrar.instance()Lmcp.mobius.waila.api.impl.ModuleRegistrar;  # Alfheim-67.jar, IC2Classic-1.7.10-1.2.6.jar
  2  mcp.mobius.waila.api.impl.ModuleRegistrar.registerBodyProvider(Lmcp.mobius.waila.api.IWailaDataProvider;Ljava.lang.Class;)V  # Alfheim-67.jar, IC2Classic-1.7.10-1.2.6.jar
  2  mcp.mobius.waila.api.impl.ModuleRegistrar.registerNBTProvider(Lmcp.mobius.waila.api.IWailaDataProvider;Ljava.lang.Class;)V  # Alfheim-67.jar, IC2Classic-1.7.10-1.2.6.jar
  1  baubles.api.expanded.BaubleExpandedSlots.getSlotType(I)Ljava.lang.String;  # ServerUtilities-2.5.2.jar
  1  baubles.common.container.InventoryBaubles.saveNBT(Lnet.minecraft.nbt.NBTTagCompound;)V  # ServerUtilities-2.5.2.jar
  1  baubles.common.container.SlotBauble.<init>(Lnet.minecraft.inventory.IInventory;Ljava.lang.String;III)V -> (Lnet.minecraft.inventory.IInventory;III)V  # ServerUtilities-2.5.2.jar
  1  baubles.common.event.EventHandlerNetwork.syncBaubles(Lnet.minecraft.entity.player.EntityPlayer;)V  # effortless-0.10.1.jar
  1  codechicken.nei.BookmarkPanel.addItem(Lnet.minecraft.item.ItemStack;Lcodechicken.nei.recipe.Recipe$RecipeId;I)Z  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.BookmarkPanel.contains(II)Z  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.BookmarkPanel.draggedStack:Lnet.minecraft.item.ItemStack;  # modularui2-2.4.0-1.7.10.jar
  1  codechicken.nei.BookmarkPanel.existsRecipe(Lcodechicken.nei.recipe.Recipe$RecipeId;I)Z  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.BookmarkPanel.getSlotMouseOver(II)Lcodechicken.nei.bookmark.BookmarksGridSlot;  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.BookmarkPanel.pullBookmarkItems(Lcodechicken.nei.recipe.chain.RecipeChainMath;Z)Z  # neoecoae-7.3.12.jar
  1  codechicken.nei.BookmarkPanel.removeItem(Lnet.minecraft.item.ItemStack;Lcodechicken.nei.recipe.Recipe$RecipeId;I)Z  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.BookmarkPanel.removeRecipe(Lcodechicken.nei.recipe.Recipe$RecipeId;I)Z  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.BookmarkPanel.removeSlot(IIZ)Z  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.BookmarkPanel.save()V  # schematica_plus-1.0.0-beta.4.jar
  1  codechicken.nei.BookmarkPanel.update()V  # schematica_plus-1.0.0-beta.4.jar
  1  codechicken.nei.ButtonCycled.x:I  # emi-1.1.24-beta.4+1.7.10+forge.jar
  1  codechicken.nei.ButtonCycled.y:I  # emi-1.1.24-beta.4+1.7.10+forge.jar
  1  codechicken.nei.DefaultBookmarkContainerHandler.<init>()V  # IronChest-6.2.0.jar
  1  codechicken.nei.FastTransferManager.clickSlot(Lnet.minecraft.client.gui.inventory.GuiContainer;I)V  # Draconic-Evolution-1.7.10-1.0.2h.jar
  1  codechicken.nei.FastTransferManager.clickSlot(Lnet.minecraft.client.gui.inventory.GuiContainer;II)V  # Draconic-Evolution-1.7.10-1.0.2h.jar
  1  codechicken.nei.FastTransferManager.clickSlot(Lnet.minecraft.client.gui.inventory.GuiContainer;III)V  # Draconic-Evolution-1.7.10-1.0.2h.jar
  1  codechicken.nei.ItemHistoryPanel.contains(II)Z  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.ItemList.access$000()Ljava.util.HashSet;  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemList.access$100()Ljava.util.HashSet;  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemList.getItemListFilter()Lcodechicken.nei.api.ItemFilter;  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemList.itemMap:Lcom.google.common.collect.ListMultimap;  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemList.loadCallbacks:Ljava.util.List;  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemList.updateFilter:Llegacyconnector_extra.notenoughitems_1_7_10_1_0_5_120_universal.codechicken.nei.RestartableTask;  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemPanel.contains(II)Z  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.ItemPanel.containsWithSubpanels(II)Z  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.ItemPanel.getGrid()Lcodechicken.nei.ItemsGrid;  # emi-1.1.24-beta.4+1.7.10+forge.jar
  1  codechicken.nei.ItemPanel.getNumPages()I  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemPanel.getSlotRect(I)Lcodechicken.lib.vec.Rectangle4i;  # OpenComputers-MC1.7.10-1.8.10+667626d-universal.jar
  1  codechicken.nei.ItemPanel.h:I  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemPanel.historyPanel:Lcodechicken.nei.ItemHistoryPanel;  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.ItemPanel.items:Ljava.util.ArrayList;  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemPanel.resize()V  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemPanel.scroll(I)V  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemPanel.updateItemList(Ljava.util.ArrayList;)V  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemPanel.w:I  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemPanel.x:I  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemPanel.y:I  # NotEnoughItems-1.7.10-1.0.5.120-universal.jar
  1  codechicken.nei.ItemQuantityField.handleKeyPress(IC)Z  # modularui2-2.4.0-1.7.10.jar
  1  codechicken.nei.ItemQuantityField.prepareStackWithQuantity(Lnet.minecraft.item.ItemStack;J)Lnet.minecraft.item.ItemStack;  # ae2_auto_pattern_upload-2.0.0-GTNH_290.jar
  1  codechicken.nei.ItemQuantityField.setFocus(Z)V  # modularui2-2.4.0-1.7.10.jar
  1  codechicken.nei.ItemStackAmount.addAll(Ljava.lang.Iterable;)V  # ezstorage-1.8.3.jar
  1  codechicken.nei.ItemStackMap.WILDCARD_TAG:Lnet.minecraft.nbt.NBTTagCompound;  # ForgeMultipart-1.7.10-1.2.0.345-universal.jar
  1  codechicken.nei.ItemStackSet.<init>()V  # neverenoughanimations-v1.0.8-1-7-10+faadc07653-dirty.jar
  1  codechicken.nei.ItemStackSet.add(Lnet.minecraft.item.ItemStack;)V  # neverenoughanimations-v1.0.8-1-7-10+faadc07653-dirty.jar
  1  codechicken.nei.ItemStackSet.remove(Lnet.minecraft.item.ItemStack;)Ljava.lang.Object;  # Game of Thrones 24.08.29.jar
  1  codechicken.nei.ItemsGrid.getColumns()I  # emi-1.1.24-beta.4+1.7.10+forge.jar
  1  codechicken.nei.ItemsGrid.getRows()I  # emi-1.1.24-beta.4+1.7.10+forge.jar
  1  codechicken.nei.ItemsGrid.getSlotRect(II)Lcodechicken.lib.vec.Rectangle4i;  # emi-1.1.24-beta.4+1.7.10+forge.jar
  1  codechicken.nei.ItemsGrid.isInvalidSlot(I)Z  # emi-1.1.24-beta.4+1.7.10+forge.jar
```

## No faithful implementation possible

```
  3  cpw.mods.fml.common.FMLCommonHandler.handleExit(I)V  # process exit paths are the host's
  3  cpw.mods.fml.common.StartupQuery.abort()V  # FML startup dialog, headless/no equivalent
  3  cpw.mods.fml.common.discovery.ModCandidate.<init>(Ljava.io.File;Ljava.io.File;Lcpw.mods.fml.common.discovery.ContainerType;)V -> (Ljava.io.File;)V  # FML discovery internals
  3  cpw.mods.fml.relauncher.ModListHelper.additionalMods:Ljava.util.Map;  # FML relauncher internals
  3  net.minecraft.world.chunk.storage.RegionFile.getChunkDataInputStream(II)Ljava.io.DataInputStream;  # Anvil region IO is replaced by the host's own storage
  2  cpw.mods.fml.client.GuiConfirmation.<init>(Lcpw.mods.fml.common.StartupQuery;)V  # FML startup dialog, headless/no equivalent
  2  cpw.mods.fml.common.StartupQuery.confirm(Ljava.lang.String;)Z  # FML startup dialog, headless/no equivalent
  2  cpw.mods.fml.common.StartupQuery.getText()Ljava.lang.String;  # FML startup dialog, headless/no equivalent
  2  cpw.mods.fml.common.discovery.ContainerType.DIR:Lcpw.mods.fml.common.discovery.ContainerType;  # FML discovery internals
  2  cpw.mods.fml.common.discovery.ContainerType.JAR:Lcpw.mods.fml.common.discovery.ContainerType;  # FML discovery internals
  2  net.minecraft.world.chunk.storage.RegionFile.<init>(Ljava.io.File;)V  # Anvil region IO is replaced by the host's own storage
  2  net.minecraft.world.chunk.storage.RegionFile.close()V  # Anvil region IO is replaced by the host's own storage
  2  net.minecraft.world.chunk.storage.RegionFile.getChunkDataOutputStream(II)Ljava.io.DataOutputStream;  # Anvil region IO is replaced by the host's own storage
  2  net.minecraft.world.chunk.storage.RegionFileCache.createOrLoadRegionFile(Ljava.io.File;II)Lnet.minecraft.world.chunk.storage.RegionFile;  # Anvil region IO is replaced by the host's own storage
  1  cpw.mods.fml.client.GuiNotification.<init>(Lcpw.mods.fml.common.StartupQuery;)V  # FML startup dialog, headless/no equivalent
  1  cpw.mods.fml.common.StartupQuery.check()Z  # FML startup dialog, headless/no equivalent
  1  cpw.mods.fml.common.StartupQuery.finish()V  # FML startup dialog, headless/no equivalent
  1  cpw.mods.fml.common.StartupQuery.getResult()Ljava.lang.Boolean;  # FML startup dialog, headless/no equivalent
  1  cpw.mods.fml.common.StartupQuery.isSynchronous()Z  # FML startup dialog, headless/no equivalent
  1  cpw.mods.fml.common.StartupQuery.notify(Ljava.lang.String;)V  # FML startup dialog, headless/no equivalent
  1  cpw.mods.fml.common.StartupQuery.setResult(Z)V  # FML startup dialog, headless/no equivalent
  1  cpw.mods.fml.common.asm.transformers.AccessTransformer.processATFile(Lcom.google.common.io.CharSource;)V  # FML class-loader internals (the layer applies access transformers itself)
  1  cpw.mods.fml.common.discovery.ModCandidate.<init>(Ljava.io.File;Ljava.io.File;Lcpw.mods.fml.common.discovery.ContainerType;ZZ)V -> (Ljava.io.File;)V  # FML discovery internals
  1  cpw.mods.fml.common.discovery.ModCandidate.addClassEntry(Ljava.lang.String;)V  # FML discovery internals
  1  cpw.mods.fml.common.discovery.ModCandidate.mods:Ljava.util.List;  # FML discovery internals
  1  cpw.mods.fml.common.discovery.ModCandidate.table:Lcpw.mods.fml.common.discovery.ASMDataTable;  # FML discovery internals
  1  cpw.mods.fml.common.discovery.asm.ASMModParser.sendToTable(Lcpw.mods.fml.common.discovery.ASMDataTable;Lcpw.mods.fml.common.discovery.ModCandidate;)V  # FML discovery internals
  1  cpw.mods.fml.relauncher.CoreModManager.getAccessTransformers()Ljava.util.List;  # FML class-loader internals (the layer applies access transformers itself)
  1  cpw.mods.fml.relauncher.ModListHelper.parseModList(Ljava.io.File;)V  # FML relauncher internals
  1  net.minecraft.server.integrated.IntegratedServer.getNetworkSystem()Lnet.minecraft.network.NetworkSystem;  # NetworkSystem does not exist in 1.21.1
  1  net.minecraft.world.chunk.storage.RegionFile.chunkExists(II)Z  # Anvil region IO is replaced by the host's own storage
  1  net.minecraft.world.chunk.storage.RegionFileCache.getChunkInputStream(Ljava.io.File;II)Ljava.io.DataInputStream;  # Anvil region IO is replaced by the host's own storage
  1  net.minecraft.world.chunk.storage.RegionFileCache.getChunkOutputStream(Ljava.io.File;II)Ljava.io.DataOutputStream;  # Anvil region IO is replaced by the host's own storage
```
