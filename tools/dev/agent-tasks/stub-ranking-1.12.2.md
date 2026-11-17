# 1.12.2: stub ranking (synthesized members by number of jars)

Generated 2026-11-17 by `tools/dev/agent-tasks/make_stub_ranking.py` from the offline probe of 349 jar(s) (324 links, 12 unresolved, 11 obsolete, 2 missing-dependency); 447 distinct synthesized members. Regenerate after implementing; implemented members drop out. Format: `n  member  # jars`.

## server-side API (top 206 of 206)

```
  3  net.minecraft.entity.monster.EntitySpellcasterIllager$AIUseSpell.<init>(Lnet.minecraft.entity.monster.EntitySpellcasterIllager;)V  # Frozen-Fiend-1.5.jar, IllagersPlus-1.12.2-1.1.3.jar, ancientbeasts-1.9.99999.jar
  2  net.minecraft.entity.ai.EntityAIZombieAttack.<init>(Lnet.minecraft.entity.monster.EntityZombie;DZ)V -> (Lnet.minecraft.entity.EntityCreature;DZ)V  # Treasure2-mc1.12.2-f14.23.5.2859-v2.1.1.jar, nuclearcraft-1.12.2-2.19.jar
  2  net.minecraft.entity.monster.EntitySpellcasterIllager$AICastingApell.<init>(Lnet.minecraft.entity.monster.EntitySpellcasterIllager;)V  # Frozen-Fiend-1.5.jar, ancientbeasts-1.9.99999.jar
  2  net.minecraft.nbt.NBTTagCompound.tagMap:Ljava.util.Map;  # Morph-1.12.2-7.2.1.jar, llibrary-1.7.20-1.12.2.jar
  2  net.minecraft.pathfinding.WalkNodeProcessor.currentEntity:Lnet.minecraft.entity.EntityLiving;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar, lepidodendron-1.12.2-63.02.jar
  2  net.minecraft.realms.RealmsBridge.<init>()V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar, TheBetweenlands-3.9.6-universal.jar
  2  net.minecraft.server.MinecraftServer.USER_CACHE_FILE:Ljava.io.File;  # Chunk Pregenerator-V1.12-2.5.0.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.world.gen.feature.WorldGenDoublePlant.setPlantType(Lnet.minecraft.block.BlockDoublePlant$EnumPlantType;)V  # lepidodendron-1.12.2-63.02.jar, twilightforest-1.12.2-3.11.1021-universal.jar
  2  net.minecraftforge.event.RegistryEvent$Register.<init>(Lnet.minecraft.util.ResourceLocation;Lnet.minecraftforge.registries.IForgeRegistry;)V -> (Lnet.minecraftforge.registries.IForgeRegistry;)V  # DimensionalDoors-1.12.2-3.0.12.jar, FTBQuests-1202.9.0.15.jar
  2  net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer.<init>()V  # EnderCore-1.12.2-0.5.76-core.jar, EnderCore-1.12.2-0.5.76.jar
  2  net.minecraftforge.fml.common.network.internal.NetworkModHolder.testVanillaAcceptance()V  # Customized-Dungeon-Loot-1.12 -(v.1.0.3).jar, RecurrentComplex-1.4.8.2.jar
  1  net.minecraft.advancements.AdvancementList.loadAdvancements(Ljava.util.Map;)V  # Placebo-1.12.2-1.6.0.jar
  1  net.minecraft.advancements.AdvancementTreeNode.layout(Lnet.minecraft.advancements.Advancement;)V  # Placebo-1.12.2-1.6.0.jar
  1  net.minecraft.advancements.CriteriaTriggers.CHANGED_DIMENSION:Lnet.minecraft.advancements.critereon.ChangeDimensionTrigger;  # collective-1.12.2-2.25.jar
  1  net.minecraft.advancements.CriteriaTriggers.PLAYER_KILLED_ENTITY:Lnet.minecraft.advancements.critereon.KilledTrigger;  # Placebo-1.12.2-1.6.0.jar
  1  net.minecraft.advancements.FunctionManager.functions:Ljava.util.Map;  # tombstone-4.1.2-1.12.2.jar
  1  net.minecraft.advancements.critereon.ChangeDimensionTrigger.trigger(Lnet.minecraft.entity.player.EntityPlayerMP;Lnet.minecraft.world.DimensionType;Lnet.minecraft.world.DimensionType;)V  # collective-1.12.2-2.25.jar
  1  net.minecraft.advancements.critereon.ConstructBeaconTrigger.trigger(Lnet.minecraft.entity.player.EntityPlayerMP;Lnet.minecraft.tileentity.TileEntityBeacon;)V  # Atum-1.12.2-2.0.20.jar
  1  net.minecraft.advancements.critereon.KilledTrigger$Instance.<init>(Lnet.minecraft.util.ResourceLocation;Lnet.minecraft.advancements.critereon.EntityPredicate;Lnet.minecraft.advancements.critereon.DamageSourcePredicate;)V  # Placebo-1.12.2-1.6.0.jar
  1  net.minecraft.block.BlockBed.getSafeExitLocation(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;I)Lnet.minecraft.util.math.BlockPos;  # minecolonies-1.12.2-0.11.841-BETA-universal.jar
  1  net.minecraft.block.BlockContainer.getBlockLiquidHeight(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.block.material.Material;)F  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.block.BlockContainer.getStateAtViewpoint(Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.world.IBlockAccess;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.util.math.Vec3d;)Lnet.minecraft.block.state.IBlockState;  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.block.BlockRailBase$Rail.<init>(Lnet.minecraft.block.BlockRailBase;Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.block.state.IBlockState;)V  # StevesCarts-1.12.2-2.4.32.137.jar
  1  net.minecraft.block.BlockRailBase$Rail.place(ZZ)Lnet.minecraft.block.BlockRailBase$Rail;  # StevesCarts-1.12.2-2.4.32.137.jar
  1  net.minecraft.block.BlockSnowBlock.<init>()V  # lepidodendron-1.12.2-63.02.jar
  1  net.minecraft.command.CommandHelp.<init>()V  # FTBLib-5.4.7.2.jar
  1  net.minecraft.command.CommandReplaceItem.<init>()V  # ExtraBitManipulation-1.12.2-3.4.1.jar
  1  net.minecraft.entity.EntityLiving.inventoryArmor:Lnet.minecraft.util.NonNullList;  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.entity.EntityLivingBase.revengeTimer:I  # tombstone-4.1.2-1.12.2.jar
  1  net.minecraft.entity.EntityLivingBase.ridingEntity:Lnet.minecraft.entity.Entity;  # Morph-1.12.2-7.2.1.jar
  1  net.minecraft.entity.ai.EntityAIBreakDoor.entity:Lnet.minecraft.entity.EntityLiving;  # Fish's Undead Rising-1.4.2.jar
  1  net.minecraft.entity.ai.attributes.AttributeModifier.initShrinkingModifier()V  # Artifacts-1.12.2-1.2.3.jar
  1  net.minecraft.entity.boss.EntityDragon.animTime:F  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.entity.boss.EntityDragon.getHeadPartYOffset(I[D[D)F  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.entity.boss.EntityDragon.getMovementOffsets(IF)[D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.entity.boss.EntityDragon.prevAnimTime:F  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.entity.boss.dragon.phase.PhaseChargingPlayer.setTarget(Lnet.minecraft.util.math.Vec3d;)V  # ProgressiveBosses-1.5.4-mc1.12.x.jar
  1  net.minecraft.entity.item.EntityBoat.FLAGS:Lnet.minecraft.network.datasync.DataParameter;  # immersivepetroleum-1.12.2-1.1.9.jar
  1  net.minecraft.entity.item.EntityFireworkRocket.lifetime:I  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.entity.monster.EntityElderGuardian.setGhost()V  # JustEnoughResources-1.12.2-0.9.2.60.jar
  1  net.minecraft.entity.monster.EntityEnderman.CARRIABLE_BLOCKS:Ljava.util.Set;  # MenuMobs-1.12.2-1.21.jar
  1  net.minecraft.entity.monster.EntityEnderman.teleportToEntity(Lnet.minecraft.entity.Entity;)Z  # Instrumental-Mobs-1.2.1.jar
  1  net.minecraft.entity.monster.EntityMob.isOnScoreboardTeam(Lnet.minecraft.scoreboard.Team;)Z  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.entity.monster.EntityMob.updateEquipmentIfNeeded(Lnet.minecraft.entity.item.EntityItem;)V  # bewitchment-1.12.2-0.0.22.64.jar
  1  net.minecraft.entity.monster.EntityPolarBear.playWarningSound()V  # AncientSpellcraft-1.12.2-1.7.5.jar
  1  net.minecraft.entity.monster.EntityShulker.updateArmorModifier(I)V  # ProgressiveBosses-1.5.4-mc1.12.x.jar
  1  net.minecraft.entity.monster.EntitySlime.getSquishSound()Lnet.minecraft.util.SoundEvent;  # EerieEntities-1.12.2-1.0.7.jar
  1  net.minecraft.entity.monster.EntitySpellcasterIllager$AIUseSpell.spellCooldown:I  # ancientbeasts-1.9.99999.jar
  1  net.minecraft.entity.passive.EntityAnimal.setOnFireFromLava()V  # lepidodendron-1.12.2-63.02.jar
  1  net.minecraft.entity.passive.EntityHorse.getVariantTexturePaths()[Ljava.lang.String;  # journeymap-1.12.2-5.7.1.jar
  1  net.minecraft.entity.passive.EntityRabbit.getJumpCompletion(F)F  # Morph-1.12.2-7.2.1.jar
  1  net.minecraft.entity.passive.EntityRabbit.startJumping()V  # Morph-1.12.2-7.2.1.jar
  1  net.minecraft.entity.player.EntityPlayer.PLAYER_MODEL_FLAG:Lnet.minecraft.network.datasync.DataParameter;  # MenuMobs-1.12.2-1.21.jar
  1  net.minecraft.entity.player.EntityPlayer.ridingEntity:Lnet.minecraft.entity.Entity;  # Morph-1.12.2-7.2.1.jar
  1  net.minecraft.entity.player.EntityPlayer.updateCape()V  # MenuMobs-1.12.2-1.21.jar
  1  net.minecraft.entity.player.EntityPlayerMP.<init>(Lnet.minecraft.server.MinecraftServer;Lnet.minecraft.world.WorldServer;Lcom.mojang.authlib.GameProfile;Lnet.minecraft.server.management.PlayerInteractionManager;)V -> (Lnet.minecraft.world.World;)V  # Morph-1.12.2-7.2.1.jar
  1  net.minecraft.entity.player.EntityPlayerMP.queuedEndExit:Z  # collective-1.12.2-2.25.jar
  1  net.minecraft.entity.player.EntityPlayerMP.recipeBook:Lnet.minecraft.stats.RecipeBookServer;  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.entity.projectile.EntityFireball.registerFixesFireball(Lnet.minecraft.util.datafix.DataFixer;Ljava.lang.String;)V  # Fish's Undead Rising-1.4.2.jar
  1  net.minecraft.entity.projectile.EntityFishHook$State.valueOf(Ljava.lang.String;)Lnet.minecraft.entity.projectile.EntityFishHook$State;  # CraftTweaker2-1.12-4.1.20.700.jar
  1  net.minecraft.entity.projectile.EntityFishHook.currentState:Lnet.minecraft.entity.projectile.EntityFishHook$State;  # CraftTweaker2-1.12-4.1.20.700.jar
  1  net.minecraft.entity.projectile.EntityLlamaSpit.owner:Lnet.minecraft.entity.passive.EntityLlama;  # DimensionalDoors-1.12.2-3.0.12.jar
  1  net.minecraft.inventory.Container.doWorkMekanism()V  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.inventory.ContainerBeacon.<init>(Lnet.minecraft.inventory.IInventory;Lnet.minecraft.inventory.IInventory;)V  # Cyclic-1.12.2-1.20.8.jar
  1  net.minecraft.inventory.ContainerHorseInventory.horse:Lnet.minecraft.entity.passive.AbstractHorse;  # InventoryTweaks-1.63.jar
  1  net.minecraft.inventory.IInventory.isItemValidForSlotGas(ILnet.minecraft.item.ItemStack;)Z  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.inventory.IInventory.setInventorySlotContentsGas(ILnet.minecraft.item.ItemStack;)V  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.item.Item.getAmulet(Lnet.minecraft.entity.player.EntityPlayer;)Lnet.minecraft.item.ItemStack;  # Atum-1.12.2-2.0.20.jar
  1  net.minecraft.item.Item.getBaublesInventory(Lnet.minecraft.entity.player.EntityPlayer;)Lnet.minecraft.inventory.IInventory;  # Atum-1.12.2-2.0.20.jar
  1  net.minecraft.item.Item.getItemStackDisplayNameGas(Lnet.minecraft.item.ItemStack;)Ljava.lang.String;  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.item.Item.getSubItemsGas(Lnet.minecraft.util.NonNullList;)V  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.item.Item.lastMax:Lcom.creativemd.littletiles.common.util.place.PlacementPosition;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.item.Item.min:Lcom.creativemd.littletiles.common.util.place.PlacementPosition;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.item.ItemBoat.<init>(Lnet.minecraft.entity.item.EntityBoat$Type;)V  # base-1.12.2-3.14.0.jar
  1  net.minecraft.item.ItemElytra.isUsable(Lnet.minecraft.item.ItemStack;)Z  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.item.ItemFlintAndSteel.<init>()V  # Fish's Undead Rising-1.4.2.jar
  1  net.minecraft.item.crafting.RecipeTippedArrow.<init>()V  # bewitchment-1.12.2-0.0.22.64.jar
  1  net.minecraft.nbt.NBTTagCompound.readNBT(BLjava.lang.String;Ljava.io.DataInput;ILnet.minecraft.nbt.NBTSizeTracker;)Lnet.minecraft.nbt.NBTBase;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.network.NetHandlerPlayServer.lastPositionUpdate:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.network.NetHandlerPlayServer.networkTickCount:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.network.NetHandlerPlayServer.update()V  # AdHooks-1.12.2-3.3.0.0-build.0528.jar
  1  net.minecraft.network.NetworkManager.acceptInboundMessage(Ljava.lang.Object;)Z  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelActive(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelInactive(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelRead(Lio.netty.channel.ChannelHandlerContext;Ljava.lang.Object;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelReadComplete(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelRegistered(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelUnregistered(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelWritabilityChanged(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.createNetworkManagerAndConnect(Ljava.net.InetAddress;IZ)Lnet.minecraft.network.NetworkManager;  # FTBUtilities-5.4.1.131.jar
  1  net.minecraft.network.NetworkManager.enableEncryption(Ljavax.crypto.SecretKey;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.exceptionCaught(Lio.netty.channel.ChannelHandlerContext;Ljava.lang.Throwable;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.getExitMessage()Lnet.minecraft.util.text.ITextComponent;  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.handleDisconnection()V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.handlerAdded(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.handlerRemoved(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.isSharable()Z  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.sendPacket(Lnet.minecraft.network.Packet;Lio.netty.util.concurrent.GenericFutureListener;[Lio.netty.util.concurrent.GenericFutureListener;)V  # IntegratedTunnels-1.12.2-1.6.14.jar
  1  net.minecraft.network.NetworkManager.setCompressionThreshold(I)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.userEventTriggered(Lio.netty.channel.ChannelHandlerContext;Ljava.lang.Object;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.play.INetHandlerPlayClient.handleDisconnect(Lnet.minecraft.network.play.server.SPacketDisconnect;)V  # ForgeMultipart-1.12.2-2.6.2.83-universal.jar
  1  net.minecraft.network.play.server.SPacketBlockAction.<init>(Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.block.Block;II)V  # fairylights-2.2.0-1.12.2.jar
  1  net.minecraft.potion.PotionEffect.performEffect(Lnet.minecraft.entity.EntityLivingBase;)V  # CraftTweaker2-1.12-4.1.20.700.jar
  1  net.minecraft.potion.PotionHealth.<init>(ZI)V  # PotionCore-1.9_for_1.12.2.jar
  1  net.minecraft.server.MinecraftServer.reload()V  # FTBLib-5.4.7.2.jar
  1  net.minecraft.server.MinecraftServer.setPlayerList(Lnet.minecraft.server.management.PlayerList;)V  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.server.MinecraftServer.stopServer()V  # WorldgenBlockReplacer-1.12.2-2.2.0.jar
  1  net.minecraft.server.dedicated.DedicatedPlayerList.<init>(Lnet.minecraft.server.dedicated.DedicatedServer;)V  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.server.dedicated.DedicatedPlayerList.initializeConnectionToPlayer(Lnet.minecraft.network.NetworkManager;Lnet.minecraft.entity.player.EntityPlayerMP;Lnet.minecraft.network.NetHandlerPlayServer;)V  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.server.integrated.IntegratedPlayerList.<init>(Lnet.minecraft.server.integrated.IntegratedServer;)V  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.server.integrated.IntegratedPlayerList.initializeConnectionToPlayer(Lnet.minecraft.network.NetworkManager;Lnet.minecraft.entity.player.EntityPlayerMP;Lnet.minecraft.network.NetHandlerPlayServer;)V  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.server.integrated.IntegratedServer.loadAllWorlds(Ljava.lang.String;Ljava.lang.String;JLnet.minecraft.world.WorldType;Ljava.lang.String;)V  # Chunk Pregenerator-V1.12-2.5.0.jar
  1  net.minecraft.server.integrated.IntegratedServer.startServerThread()V  # Chunk Pregenerator-V1.12-2.5.0.jar
  1  net.minecraft.server.management.PlayerList.readPlayerDataFromFile(Lnet.minecraft.entity.player.EntityPlayerMP;)Lnet.minecraft.nbt.NBTTagCompound;  # Morph-1.12.2-7.2.1.jar
  1  net.minecraft.tileentity.TileEntity.addTileToENet()V  # nuclearcraft-1.12.2-2.19.jar
  1  net.minecraft.tileentity.TileEntity.addToIc2EnergyNetwork()V  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.deregister()V  # Mekanism-1.12.2-9.8.3.390.jar
  1  net.minecraft.tileentity.TileEntity.getEUCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Ljava.lang.Object;  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.getRFCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Ljava.lang.Object;  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.getTeslaCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Ljava.lang.Object;  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.hasEUCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Z  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.hasRFCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Z  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.hasTeslaCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Z  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.receiveUpdatePacket(Lnet.minecraft.nbt.NBTTagCompound;)V  # CreativeCore_v1.10.70_mc1.12.2.jar
  1  net.minecraft.tileentity.TileEntity.register()V  # Mekanism-1.12.2-9.8.3.390.jar
  1  net.minecraft.tileentity.TileEntity.removeFromIc2EnergyNetwork()V  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.removeTileFromENet()V  # nuclearcraft-1.12.2-2.19.jar
  1  net.minecraft.tileentity.TileEntityStructure.getPosition()Lnet.minecraft.util.math.BlockPos;  # FTBQuests-1202.9.0.15.jar
  1  net.minecraft.tileentity.TileEntityStructure.getStructureSize()Lnet.minecraft.util.math.BlockPos;  # FTBQuests-1202.9.0.15.jar
  1  net.minecraft.util.EnumTypeAdapterFactory.<init>()V  # minecolonies-1.12.2-0.11.841-BETA-universal.jar
  1  net.minecraft.util.ITickable.sendLoad()V  # gendustry-1.6.5.8-mc1.12.2.jar
  1  net.minecraft.util.ITickable.sendUnload()V  # gendustry-1.6.5.8-mc1.12.2.jar
  1  net.minecraft.util.math.AxisAlignedBB.getCornerX(Lnet.minecraft.util.math.AxisAlignedBB;Lcom.creativemd.creativecore.common.utils.math.box.BoxUtils$BoxCorner;)D  # CreativeCore_v1.10.70_mc1.12.2.jar
  1  net.minecraft.util.math.AxisAlignedBB.getCornerY(Lnet.minecraft.util.math.AxisAlignedBB;Lcom.creativemd.creativecore.common.utils.math.box.BoxUtils$BoxCorner;)D  # CreativeCore_v1.10.70_mc1.12.2.jar
  1  net.minecraft.util.math.AxisAlignedBB.getCornerZ(Lnet.minecraft.util.math.AxisAlignedBB;Lcom.creativemd.creativecore.common.utils.math.box.BoxUtils$BoxCorner;)D  # CreativeCore_v1.10.70_mc1.12.2.jar
  1  net.minecraft.util.text.Style$Serializer.<init>()V  # minecolonies-1.12.2-0.11.841-BETA-universal.jar
  1  net.minecraft.village.Village.findNearestVillageAggressor(Lnet.minecraft.entity.EntityLivingBase;)Lnet.minecraft.entity.EntityLivingBase;  # lycanitesmobs-1.12.2-2.0.8.10.jar
  1  net.minecraft.village.Village.getNearestTargetPlayer(Lnet.minecraft.entity.EntityLivingBase;)Lnet.minecraft.entity.player.EntityPlayer;  # lycanitesmobs-1.12.2-2.0.8.10.jar
  1  net.minecraft.world.ChunkCache.chunkArray:[[Lnet.minecraft.world.chunk.Chunk;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.World.findingSpawnPoint:Z  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.WorldServer.findingSpawnPoint:Z  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.WorldServer.pendingTickListEntriesHashSet:Ljava.util.Set;  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.world.WorldServer.pendingTickListEntriesTreeSet:Ljava.util.TreeSet;  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.world.biome.Biome.registerBiome(ILjava.lang.String;Lnet.minecraft.world.biome.Biome;)V  # AdvancedRocketry-1.12.2-1.7.0-232-universal.jar
  1  net.minecraft.world.biome.BiomePlains.<init>(ZLnet.minecraft.world.biome.Biome$BiomeProperties;)V  # MenuMobs-1.12.2-1.21.jar
  1  net.minecraft.world.biome.BiomeVoid.<init>(Lnet.minecraft.world.biome.Biome$BiomeProperties;)V  # arcaneworld-1.12.2-0.0.11.jar
  1  net.minecraft.world.biome.BiomeVoidDecorator.<init>()V  # arcaneworld-1.12.2-0.0.11.jar
  1  net.minecraft.world.chunk.storage.AnvilChunkLoader.fixer:Lnet.minecraft.util.datafix.DataFixer;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.chunk.storage.AnvilChunkLoader.loadEntities(Lnet.minecraft.world.World;Lnet.minecraft.nbt.NBTTagCompound;Lnet.minecraft.world.chunk.Chunk;)V  # Chunk Pregenerator-V1.12-2.5.0.jar
  1  net.minecraft.world.chunk.storage.AnvilChunkLoader.readChunkEntity(Lnet.minecraft.nbt.NBTTagCompound;Lnet.minecraft.world.World;Lnet.minecraft.world.chunk.Chunk;)Lnet.minecraft.entity.Entity;  # compactmachines3-1.12.2-3.0.18-b278.jar
  1  net.minecraft.world.chunk.storage.AnvilChunkLoader.writeChunkData(Lnet.minecraft.util.math.ChunkPos;Lnet.minecraft.nbt.NBTTagCompound;)V  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.chunk.storage.RegionFile.getChunkDataOutputStream(II)Ljava.io.DataOutputStream;  # Chunk Pregenerator-V1.12-2.5.0.jar
  1  net.minecraft.world.end.DragonFightManager.addPlayer(Lnet.minecraft.entity.player.EntityPlayerMP;)V  # stygian-1.0.4.jar
  1  net.minecraft.world.end.DragonFightManager.removePlayer(Lnet.minecraft.entity.player.EntityPlayerMP;)V  # stygian-1.0.4.jar
  1  net.minecraft.world.gen.ChunkGeneratorFlat.structureGenerators:Ljava.util.Map;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.gen.ChunkGeneratorOverworld.woodlandMansionGenerator:Lnet.minecraft.world.gen.structure.WoodlandMansion;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.gen.layer.IntCache.freeLargeArrays:Ljava.util.List;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.gen.layer.IntCache.freeSmallArrays:Ljava.util.List;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.gen.layer.IntCache.inUseLargeArrays:Ljava.util.List;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.gen.layer.IntCache.inUseSmallArrays:Ljava.util.List;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.gen.layer.IntCache.intCacheSize:I  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.gen.structure.MapGenEndCity.<init>(Lnet.minecraft.world.gen.ChunkGeneratorEnd;)V -> ()V  # stygian-1.0.4.jar
  1  net.minecraft.world.gen.structure.MapGenMineshaft$Type.MESA:Lnet.minecraft.world.gen.structure.MapGenMineshaft$Type;  # bettercaves-1.12.2-2.0.4.jar
  1  net.minecraft.world.gen.structure.MapGenMineshaft$Type.NORMAL:Lnet.minecraft.world.gen.structure.MapGenMineshaft$Type;  # bettercaves-1.12.2-2.0.4.jar
  1  net.minecraft.world.gen.structure.StructureMineshaftStart.<init>(Lnet.minecraft.world.World;Ljava.util.Random;IILnet.minecraft.world.gen.structure.MapGenMineshaft$Type;)V  # bettercaves-1.12.2-2.0.4.jar
  1  net.minecraft.world.gen.structure.template.Template.addEntitiesToWorld(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.util.Mirror;Lnet.minecraft.util.Rotation;Lnet.minecraft.world.gen.structure.StructureBoundingBox;)V  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.world.storage.SaveDataMemoryStorage.<init>()V  # blockdrops-1.12.2-1.4.0.jar
  1  net.minecraft.world.storage.loot.LootTableManager.registeredLootTables:Lcom.google.common.cache.LoadingCache;  # Placebo-1.12.2-1.6.0.jar
  1  net.minecraft.world.storage.loot.conditions.EntityHasProperty.properties:[Lnet.minecraft.world.storage.loot.properties.EntityProperty;  # JustEnoughResources-1.12.2-0.9.2.60.jar
  1  net.minecraftforge.common.capabilities.ICapabilityProvider.canConnect(Lnet.minecraft.util.EnumFacing;)Z  # EnderIO-1.12.2-5.3.70.jar
  1  net.minecraftforge.common.capabilities.ICapabilityProvider.node()Lli.cil.oc.api.network.Node;  # EnderIO-1.12.2-5.3.70.jar
  1  net.minecraftforge.common.capabilities.ICapabilityProvider.node(Lcom.enderio.core.common.util.DyeColor;)Lli.cil.oc.api.network.Node;  # EnderIO-1.12.2-5.3.70.jar
  1  net.minecraftforge.common.capabilities.ICapabilityProvider.onConnect(Lli.cil.oc.api.network.Node;)V  # EnderIO-1.12.2-5.3.70.jar
  1  net.minecraftforge.common.capabilities.ICapabilityProvider.onDisconnect(Lli.cil.oc.api.network.Node;)V  # EnderIO-1.12.2-5.3.70.jar
  1  net.minecraftforge.common.capabilities.ICapabilityProvider.onMessage(Lli.cil.oc.api.network.Message;)V  # EnderIO-1.12.2-5.3.70.jar
  1  net.minecraftforge.common.capabilities.ICapabilityProvider.sidedNode(Lnet.minecraft.util.EnumFacing;)Lli.cil.oc.api.network.Node;  # EnderIO-1.12.2-5.3.70.jar
  1  net.minecraftforge.common.config.Configuration$UnicodeInputStreamReader.<init>(Ljava.io.InputStream;Ljava.lang.String;)V  # bettercaves-1.12.2-2.0.4.jar
  1  net.minecraftforge.common.crafting.IRecipeContainer.getCraftMatrix()Lnet.minecraft.inventory.InventoryCrafting;  # NoMoreRecipeConflict-0.13(1.12.2).jar
  1  net.minecraftforge.common.crafting.IRecipeContainer.getCraftResult()Lnet.minecraft.inventory.InventoryCraftResult;  # NoMoreRecipeConflict-0.13(1.12.2).jar
  1  net.minecraftforge.common.util.CompoundDataFixer.<init>(Lnet.minecraft.util.datafix.DataFixer;)V -> ()V  # Treasure2-mc1.12.2-f14.23.5.2859-v2.1.1.jar
  1  net.minecraftforge.event.ForgeEventFactory.loadLootTable(Lnet.minecraft.util.ResourceLocation;Lnet.minecraft.world.storage.loot.LootTable;Lnet.minecraft.world.storage.loot.LootTableManager;)Lnet.minecraft.world.storage.loot.LootTable;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraftforge.event.ForgeEventFactory.onPlayerMultiBlockPlace(Lnet.minecraft.entity.player.EntityPlayer;Ljava.util.List;Lnet.minecraft.util.EnumFacing;Lnet.minecraft.util.EnumHand;)Lnet.minecraftforge.event.world.BlockEvent$MultiPlaceEvent;  # MCMultiPart-2.5.3.jar
  1  net.minecraftforge.event.brewing.PlayerBrewedPotionEvent.getStack()Lnet.minecraft.item.ItemStack;  # CraftTweaker2-1.12-4.1.20.700.jar
  1  net.minecraftforge.event.entity.ThrowableImpactEvent.getRayTraceResult()Lnet.minecraft.util.math.RayTraceResult;  # infinitylib-1.12.0.jar
  1  net.minecraftforge.event.entity.minecart.MinecartCollisionEvent.getCollider()Lnet.minecraft.entity.Entity;  # CraftTweaker2-1.12-4.1.20.700.jar
  1  net.minecraftforge.event.entity.player.PlayerFlyableFallEvent.getDistance()F  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraftforge.event.entity.player.PlayerFlyableFallEvent.getMultiplier()F  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraftforge.fluids.FluidUtil.tryFillContainerAndStow(Lnet.minecraft.item.ItemStack;Lnet.minecraftforge.fluids.capability.IFluidHandler;Lnet.minecraftforge.items.IItemHandler;ILnet.minecraft.entity.player.EntityPlayer;)Lnet.minecraftforge.fluids.FluidActionResult;  # CoFHCore-1.12.2-4.6.6.1-universal.jar
  1  net.minecraftforge.fluids.capability.IFluidHandler.getFacingGasTank()Lmekanism.api.gas.IGasHandler;  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraftforge.fml.common.FMLCommonHandler.loadLanguage(Ljava.util.Map;Ljava.io.InputStream;)Ljava.io.InputStream;  # lycanitesmobs-1.12.2-2.0.8.10.jar
  1  net.minecraftforge.fml.common.FMLModContainer.<init>(Ljava.lang.String;Lnet.minecraftforge.fml.common.discovery.ModCandidate;Ljava.util.Map;)V -> ()V  # wings-1.1.6-1.12.2.jar
  1  net.minecraftforge.fml.common.FMLModContainer.registerBus(Lcom.google.common.eventbus.EventBus;Lnet.minecraftforge.fml.common.LoadController;)Z  # wings-1.1.6-1.12.2.jar
  1  net.minecraftforge.fml.common.Loader.setupTestHarness([Lnet.minecraftforge.fml.common.ModContainer;)V  # DimensionalDoors-1.12.2-3.0.12.jar
  1  net.minecraftforge.fml.common.ModAPIManager.injectAPIModContainers(Ljava.util.List;Ljava.util.Map;)V  # bdlib-1.14.3.12-mc1.12.2.jar
  1  net.minecraftforge.fml.common.asm.transformers.AccessTransformer.<init>(Ljava.lang.String;)V  # InventoryTweaks-1.63.jar
  1  net.minecraftforge.fml.common.event.FMLStateEvent.<init>([Ljava.lang.Object;)V -> ()V  # RebornCore-FORK-1.12.2-3.19.5-universal.jar
  1  net.minecraftforge.fml.common.eventhandler.ASMEventHandler.getPriority()Lnet.minecraftforge.fml.common.eventhandler.EventPriority;  # OpenBlocks-1.12.2-1.8.1.jar
  1  net.minecraftforge.fml.common.eventhandler.ListenerList.register(ILnet.minecraftforge.fml.common.eventhandler.EventPriority;Lnet.minecraftforge.fml.common.eventhandler.IEventListener;)V  # future-mc-0.2.11.jar
  1  net.minecraftforge.fml.common.network.handshake.NetworkDispatcher.get(Lnet.minecraft.network.NetworkManager;)Lnet.minecraftforge.fml.common.network.handshake.NetworkDispatcher;  # LibVulpes-1.12.2-0.4.2-75-universal.jar
  1  net.minecraftforge.fml.common.network.internal.FMLProxyPacket.toC17Packet()Lnet.minecraft.network.Packet;  # architecturecraft-1.12-3.98.jar
  1  net.minecraftforge.fml.common.network.internal.FMLProxyPacket.toS3FPackets()Ljava.util.List;  # architecturecraft-1.12-3.98.jar
  1  net.minecraftforge.fml.common.registry.VillagerRegistry.addExtraVillageComponents(Ljava.util.List;Ljava.util.Random;I)V  # VillageNames-1.12.2-4.1.5.jar
  1  net.minecraftforge.fml.common.thread.SidedThreadGroup.newThread(Ljava.lang.Runnable;)Ljava.lang.Thread;  # HammerLib-1.12.2-2.0.6.32.jar
  1  net.minecraftforge.registries.GameData.getEntityClassMap()Ljava.util.Map;  # DoggyTalents-1.12.2-1.15.1.6.jar
  1  net.minecraftforge.registries.GameData.getWrapper(Ljava.lang.Class;)Lnet.minecraft.util.registry.RegistryNamespaced;  # JustEnoughResources-1.12.2-0.9.2.60.jar
  1  net.minecraftforge.server.permission.context.WorldContext.<init>(Lnet.minecraft.world.World;)V  # FTBLib-5.4.7.2.jar
```

## client-side API (top 222 of 222)

```
  2  net.minecraft.client.gui.GuiCustomizeSkin.<init>(Lnet.minecraft.client.gui.GuiScreen;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar, HammerLib-1.12.2-2.0.6.32.jar
  2  net.minecraft.client.renderer.RegionRenderCacheBuilder.getWorldRendererByLayer(Lnet.minecraft.util.BlockRenderLayer;)Lnet.minecraft.client.renderer.BufferBuilder;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.RenderGlobal.cloudTickCounter:I  # iChunUtil-1.12.2-7.2.2.jar, rftoolsdim-1.12-5.71.jar
  2  net.minecraft.client.renderer.RenderGlobal.lastViewEntityYaw:D  # PortalGun-1.12.2-7.1.0.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status.COMPILING:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type.REBUILD_CHUNK:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getRegionRenderCacheBuilder()Lnet.minecraft.client.renderer.RegionRenderCacheBuilder;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getStatus()Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.CompiledChunk.isLayerStarted(Lnet.minecraft.util.BlockRenderLayer;)Z  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.getCompiledChunk()Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.getPosition()Lnet.minecraft.util.math.BlockPos;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.renderChunksUpdated:I  # Bookshelf-1.12.2-2.3.590.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.setNeedsUpdate(Z)V  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.entity.RenderFireball.<init>(Lnet.minecraft.client.renderer.entity.RenderManager;F)V  # Fish's Undead Rising-1.4.2.jar, Instrumental-Mobs-1.2.1.jar
  2  net.minecraft.realms.RealmsBridge.switchToRealms(Lnet.minecraft.client.gui.GuiScreen;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar, TheBetweenlands-3.9.6-universal.jar
  2  net.minecraft.util.MovementInputFromOptions.<init>(Lnet.minecraft.client.settings.GameSettings;)V  # Chisel-MC1.12.2-1.0.2.45.jar, MenuMobs-1.12.2-1.21.jar
  2  net.minecraftforge.fml.client.FMLConfigGuiFactory.<init>()V  # Login_Shield-1.12.2-6-g5654706.jar, chiselsandbits-14.33.jar
  2  paulscode.sound.SoundSystem.newStreamingSource(ZLjava.lang.String;Ljava.net.URL;Ljava.lang.String;ZFFFIF)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, fairylights-2.2.0-1.12.2.jar
  2  paulscode.sound.SoundSystem.play(Ljava.lang.String;)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, fairylights-2.2.0-1.12.2.jar
  2  paulscode.sound.SoundSystem.setPitch(Ljava.lang.String;F)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, fairylights-2.2.0-1.12.2.jar
  2  paulscode.sound.SoundSystem.setVolume(Ljava.lang.String;F)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, fairylights-2.2.0-1.12.2.jar
  2  paulscode.sound.SoundSystemConfig.THREAD_SYNC:Ljava.lang.Object;  # AmbientSounds_v3.1.5_mc1.12.2.jar, TheBetweenlands-3.9.6-universal.jar
  1  net.minecraft.client.Minecraft.isJava64bit()Z  # itlt-1.12.2-1.0.3.jar
  1  net.minecraft.client.audio.MovingSoundMinecart.<init>(Lnet.minecraft.entity.item.EntityMinecart;)V  # StevesCarts-1.12.2-2.4.32.137.jar
  1  net.minecraft.client.audio.MovingSoundMinecartRiding.<init>(Lnet.minecraft.entity.player.EntityPlayer;Lnet.minecraft.entity.item.EntityMinecart;)V  # StevesCarts-1.12.2-2.4.32.137.jar
  1  net.minecraft.client.entity.AbstractClientPlayer.PLAYER_MODEL_FLAG:Lnet.minecraft.network.datasync.DataParameter;  # conarm-1.12.2-1.2.5.10.jar
  1  net.minecraft.client.entity.AbstractClientPlayer.rotateElytraX:F  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.client.entity.AbstractClientPlayer.rotateElytraY:F  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.client.entity.AbstractClientPlayer.rotateElytraZ:F  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.client.entity.EntityOtherPlayerMP.PLAYER_MODEL_FLAG:Lnet.minecraft.network.datasync.DataParameter;  # wings-1.1.6-1.12.2.jar
  1  net.minecraft.client.entity.EntityPlayerSP.recipeBook:Lnet.minecraft.stats.RecipeBook;  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.client.entity.EntityPlayerSP.ridingEntity:Lnet.minecraft.entity.Entity;  # Hats-1.12.2-7.1.1.jar
  1  net.minecraft.client.gui.GuiChat.setCompletions([Ljava.lang.String;)V  # journeymap-1.12.2-5.7.1.jar
  1  net.minecraft.client.gui.GuiIngame.chatListeners:Ljava.util.Map;  # FTBLib-5.4.7.2.jar
  1  net.minecraft.client.gui.GuiNewChat.chatLines:Ljava.util.List;  # HammerLib-1.12.2-2.0.6.32.jar
  1  net.minecraft.client.gui.GuiScreenOptionsSounds.<init>(Lnet.minecraft.client.gui.GuiScreen;Lnet.minecraft.client.settings.GameSettings;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar
  1  net.minecraft.client.gui.GuiSnooper.<init>(Lnet.minecraft.client.gui.GuiScreen;Lnet.minecraft.client.settings.GameSettings;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar
  1  net.minecraft.client.gui.ScreenChatOptions.<init>(Lnet.minecraft.client.gui.GuiScreen;Lnet.minecraft.client.settings.GameSettings;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar
  1  net.minecraft.client.gui.ScreenChatOptions.updateNarratorButton()V  # randompatches-1.12.2-1.22.1.10.jar
  1  net.minecraft.client.gui.inventory.GuiContainer.updateDragSplitting()V  # HammerLib-1.12.2-2.0.6.32.jar
  1  net.minecraft.client.gui.inventory.GuiContainerCreative$CreativeSlot.slot:Lnet.minecraft.inventory.Slot;  # InventoryTweaks-1.63.jar
  1  net.minecraft.client.gui.inventory.GuiCrafting.recipeButton:Lnet.minecraft.client.gui.GuiButtonImage;  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.client.model.ModelDragon.head:Lnet.minecraft.client.model.ModelRenderer;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.model.ModelEvokerFangs.<init>()V  # Fish's Undead Rising-1.4.2.jar
  1  net.minecraft.client.model.ModelGuardian.guardianBody:Lnet.minecraft.client.model.ModelRenderer;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.model.ModelHumanoidHead.<init>()V  # p455w0rdslib-1.12.2-2.3.161.jar
  1  net.minecraft.client.model.ModelMagmaCube.core:Lnet.minecraft.client.model.ModelRenderer;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.model.ModelShulkerBullet.<init>()V  # champions-1.12.2-1.0.11.10.jar
  1  net.minecraft.client.network.NetHandlerPlayClient.handleCustomSound(Lnet.minecraft.network.play.server.SPacketCustomSound;)V  # HammerLib-1.12.2-2.0.6.32.jar
  1  net.minecraft.client.network.NetHandlerPlayClient.playerInfoMap:Ljava.util.Map;  # Morph-1.12.2-7.2.1.jar
  1  net.minecraft.client.renderer.ChunkRenderContainer.initialize(DDD)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.applyBobbing(F)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.cameraPitch:D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.cameraYaw:D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.cameraZoom:D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.cloudFog:Z  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.debugView:Z  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.debugViewDirection:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.farPlaneDistance:F  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.fogColorBlue:F  # PortalGun-1.12.2-7.1.0.jar
  1  net.minecraft.client.renderer.EntityRenderer.fogColorGreen:F  # PortalGun-1.12.2-7.1.0.jar
  1  net.minecraft.client.renderer.EntityRenderer.fogColorRed:F  # PortalGun-1.12.2-7.1.0.jar
  1  net.minecraft.client.renderer.EntityRenderer.getFOVModifier(FZ)F  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.hurtCameraEffect(F)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.pointedEntity:Lnet.minecraft.entity.Entity;  # ReachFix-1.12.2-1.0.9.jar
  1  net.minecraft.client.renderer.EntityRenderer.renderCloudsCheck(Lnet.minecraft.client.renderer.RenderGlobal;FIDDD)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.renderRainSnow(F)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.setupFog(IF)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.thirdPersonDistancePrev:F  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.torchFlickerX:F  # MenuMobs-1.12.2-1.21.jar
  1  net.minecraft.client.renderer.EntityRenderer.updateFogColor(F)V  # PortalGun-1.12.2-7.1.0.jar
  1  net.minecraft.client.renderer.ItemRenderer.equippedProgressMainHand:F  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RegionRenderCacheBuilder.getWorldRendererByLayerId(I)Lnet.minecraft.client.renderer.BufferBuilder;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal$ContainerLocalRenderInformation.renderChunk:Lnet.minecraft.client.renderer.chunk.RenderChunk;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.countEntitiesHidden:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.countEntitiesRendered:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.countEntitiesTotal:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.displayListEntitiesDirty:Z  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.entityOutlineFramebuffer:Lnet.minecraft.client.shader.Framebuffer;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.entityOutlineShader:Lnet.minecraft.client.shader.ShaderGroup;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.entityOutlinesRendered:Z  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.generateSky()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.generateSky2()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.generateStars()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.isOutlineActive(Lnet.minecraft.entity.Entity;Lnet.minecraft.entity.Entity;Lnet.minecraft.client.renderer.culling.ICamera;)Z  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.lastViewEntityPitch:D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.lastViewEntityX:D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.lastViewEntityY:D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.lastViewEntityZ:D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.postRenderDamagedBlocks()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.preRenderDamagedBlocks()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderChunkFactory:Lnet.minecraft.client.renderer.chunk.IRenderChunkFactory;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderContainer:Lnet.minecraft.client.renderer.ChunkRenderContainer;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderDispatcher:Lnet.minecraft.client.renderer.chunk.ChunkRenderDispatcher;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderDistanceChunks:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderEntitiesStartupCounter:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderManager:Lnet.minecraft.client.renderer.entity.RenderManager;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.setWorldAndLoadRenderers(Lnet.minecraft.client.multiplayer.WorldClient;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.spawnParticle0(IZDDDDDD[I)Lnet.minecraft.client.particle.Particle;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.stopChunkUpdates()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.updateClouds()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.updateDestroyBlockIcons()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.vboEnabled:Z  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.viewFrustum:Lnet.minecraft.client.renderer.ViewFrustum;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.world:Lnet.minecraft.client.multiplayer.WorldClient;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderList.<init>()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.VboRenderList.<init>()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.ViewFrustum.<init>(Lnet.minecraft.world.World;ILnet.minecraft.client.renderer.RenderGlobal;Lnet.minecraft.client.renderer.chunk.IRenderChunkFactory;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.ViewFrustum.deleteGlResources()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.ViewFrustum.markBlocksForUpdate(IIIIIIZ)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.ViewFrustum.renderChunks:[Lnet.minecraft.client.renderer.chunk.RenderChunk;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.ViewFrustum.updateChunkPositions(DD)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status.DONE:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status.PENDING:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status.UPLOADING:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type.RESORT_TRANSPARENCY:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.<init>(Lnet.minecraft.client.renderer.chunk.RenderChunk;Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type;D)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.finish()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getCompiledChunk()Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getLock()Ljava.util.concurrent.locks.ReentrantLock;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getType()Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.setCompiledChunk(Lnet.minecraft.client.renderer.chunk.CompiledChunk;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.<init>()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.<init>()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.DUMMY:Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.addTileEntity(Lnet.minecraft.tileentity.TileEntity;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.getState()Lnet.minecraft.client.renderer.BufferBuilder$State;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.getTileEntities()Ljava.util.List;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.isLayerEmpty(Lnet.minecraft.util.BlockRenderLayer;)Z  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.setLayerStarted(Lnet.minecraft.util.BlockRenderLayer;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.setLayerUsed(Lnet.minecraft.util.BlockRenderLayer;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.setState(Lnet.minecraft.client.renderer.BufferBuilder$State;)V  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.setVisibility(Lnet.minecraft.client.renderer.chunk.SetVisibility;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListChunkFactory.<init>()V  # AdvancedRocketry-1.12.2-1.7.0-232-universal.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.<init>(Lnet.minecraft.world.World;Lnet.minecraft.client.renderer.RenderGlobal;I)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.compiledChunk:Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.createRegionRenderCache(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.util.math.BlockPos;I)Lnet.minecraft.world.ChunkCache;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.finishCompileTask()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.getDistanceSq()D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.lockCompileTask:Ljava.util.concurrent.locks.ReentrantLock;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.makeCompileTaskChunk()Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.makeCompileTaskTransparency()Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.position:Lnet.minecraft.util.math.BlockPos$MutableBlockPos;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.postRenderBlocks(Lnet.minecraft.util.BlockRenderLayer;FFFLnet.minecraft.client.renderer.BufferBuilder;Lnet.minecraft.client.renderer.chunk.CompiledChunk;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.preRenderBlocks(Lnet.minecraft.client.renderer.BufferBuilder;Lnet.minecraft.util.math.BlockPos;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.renderChunksUpdated:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.renderGlobal:Lnet.minecraft.client.renderer.RenderGlobal;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.setNeedsUpdate(Z)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.setTileEntities:Ljava.util.Set;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.world:Lnet.minecraft.world.World;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ListedRenderChunk.worldView:Lnet.minecraft.world.ChunkCache;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.<init>(Lnet.minecraft.world.World;Lnet.minecraft.client.renderer.RenderGlobal;I)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.compiledChunk:Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.createRegionRenderCache(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.util.math.BlockPos;I)Lnet.minecraft.world.ChunkCache;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.finishCompileTask()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.getDistanceSq()D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.getLockCompileTask()Ljava.util.concurrent.locks.ReentrantLock;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.getVertexBufferByLayer(I)Lnet.minecraft.client.renderer.vertex.VertexBuffer;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.lockCompileTask:Ljava.util.concurrent.locks.ReentrantLock;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.makeCompileTaskChunk()Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.makeCompileTaskTransparency()Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.needsUpdate()Z  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.position:Lnet.minecraft.util.math.BlockPos$MutableBlockPos;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.postRenderBlocks(Lnet.minecraft.util.BlockRenderLayer;FFFLnet.minecraft.client.renderer.BufferBuilder;Lnet.minecraft.client.renderer.chunk.CompiledChunk;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.preRenderBlocks(Lnet.minecraft.client.renderer.BufferBuilder;Lnet.minecraft.util.math.BlockPos;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.renderGlobal:Lnet.minecraft.client.renderer.RenderGlobal;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.setTileEntities:Ljava.util.Set;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.world:Lnet.minecraft.world.World;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.worldView:Lnet.minecraft.world.ChunkCache;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.VisGraph.<init>()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.VisGraph.computeVisibility()Lnet.minecraft.client.renderer.chunk.SetVisibility;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.VisGraph.setOpaqueCube(Lnet.minecraft.util.math.BlockPos;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.entity.RenderDragon.ENDERCRYSTAL_BEAM_TEXTURES:Lnet.minecraft.util.ResourceLocation;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.client.renderer.entity.RenderDragon.renderCrystalBeams(DDDFDDDIDDD)V  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.client.renderer.entity.RenderFireball.doRender(Lnet.minecraft.entity.projectile.EntityFireball;DDDFF)V  # Instrumental-Mobs-1.2.1.jar
  1  net.minecraft.client.renderer.entity.layers.LayerVillagerArmor.<init>(Lnet.minecraft.client.renderer.entity.RenderLivingBase;)V  # VillageNames-1.12.2-4.1.5.jar
  1  net.minecraft.client.resources.DefaultResourcePack.getInputStreamAssets(Lnet.minecraft.util.ResourceLocation;)Ljava.io.InputStream;  # randompatches-1.12.2-1.22.1.10.jar
  1  net.minecraft.client.resources.ResourcePackRepository.getDownloadHeaders()Ljava.util.Map;  # journeymap-1.12.2-5.7.1.jar
  1  net.minecraft.client.resources.data.BaseMetadataSectionSerializer.<init>()V  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.client.tutorial.Tutorial.<init>(Lnet.minecraft.client.Minecraft;)V  # ElenaiDodge2-1.12.2-1.0.8b.jar
  1  net.minecraft.client.tutorial.Tutorial.createKeybindComponent(Ljava.lang.String;)Lnet.minecraft.util.text.ITextComponent;  # ElenaiDodge2-1.12.2-1.0.8b.jar
  1  net.minecraft.client.tutorial.Tutorial.getMinecraft()Lnet.minecraft.client.Minecraft;  # ElenaiDodge2-1.12.2-1.0.8b.jar
  1  net.minecraft.inventory.Slot.getBackgroundMap()Lnet.minecraft.client.renderer.texture.TextureMap;  # AE2WTLib-1.12.2-1.0.34.jar
  1  net.minecraft.network.handshake.client.C00Handshake.<init>(Ljava.lang.String;ILnet.minecraft.network.EnumConnectionState;)V  # FTBUtilities-5.4.1.131.jar
  1  net.minecraft.network.status.client.CPacketPing.<init>(J)V  # FTBUtilities-5.4.1.131.jar
  1  net.minecraft.network.status.client.CPacketServerQuery.<init>()V  # FTBUtilities-5.4.1.131.jar
  1  net.minecraft.realms.RealmsBridge.getNotificationScreen(Lnet.minecraft.client.gui.GuiScreen;)Lnet.minecraft.client.gui.GuiScreenRealmsProxy;  # TheBetweenlands-3.9.6-universal.jar
  1  net.minecraftforge.client.ForgeHooksClient.getDamageModel(Lnet.minecraft.client.renderer.block.model.IBakedModel;Lnet.minecraft.client.renderer.texture.TextureAtlasSprite;Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.world.IBlockAccess;Lnet.minecraft.util.math.BlockPos;)Lnet.minecraft.client.renderer.block.model.IBakedModel;  # ForgeMultipart-1.12.2-2.6.2.83-universal.jar
  1  net.minecraftforge.client.ForgeHooksClient.putQuadColor(Lnet.minecraft.client.renderer.BufferBuilder;Lnet.minecraft.client.renderer.block.model.BakedQuad;I)V  # ModularPowersuits-1.12.2-1.0.46.jar
  1  net.minecraftforge.client.ForgeHooksClient.renderLitItem(Lnet.minecraft.client.renderer.RenderItem;Lnet.minecraft.client.renderer.block.model.IBakedModel;ILnet.minecraft.item.ItemStack;)V  # ThaumicAdditions-1.12.2-12.7.8.jar
  1  net.minecraftforge.client.MinecraftForgeClient.getImageLayer(Lnet.minecraft.util.ResourceLocation;Lnet.minecraft.client.resources.IResourceManager;)Ljava.awt.image.BufferedImage;  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraftforge.client.MinecraftForgeClient.onRebuildChunk(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.world.ChunkCache;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraftforge.client.event.GuiScreenEvent$PotionShiftEvent.<init>(Lnet.minecraft.client.gui.GuiScreen;)V  # Cyclic-1.12.2-1.20.8.jar
  1  net.minecraftforge.common.model.Models.getHiddenModelPart(Lcom.google.common.collect.ImmutableList;)Lnet.minecraftforge.common.model.IModelPart;  # Numina-1.12.2-1.0.38.jar
  1  net.minecraftforge.common.model.Models.getParts(Lnet.minecraftforge.common.model.IModelPart;)Lcom.google.common.collect.UnmodifiableIterator;  # Numina-1.12.2-1.0.38.jar
  1  net.minecraftforge.common.model.animation.IClip.apply(Lnet.minecraftforge.common.model.animation.IJoint;)Lnet.minecraftforge.common.model.animation.IJointClip;  # OpenModsLib-1.12.2-0.12.2.jar
  1  net.minecraftforge.common.model.animation.IJointClip.apply(F)Lnet.minecraftforge.common.model.TRSRTransformation;  # OpenModsLib-1.12.2-0.12.2.jar
  1  net.minecraftforge.fml.client.FMLClientHandler.getClientPlayHandler()Lnet.minecraft.network.INetHandler;  # CoFHCore-1.12.2-4.6.6.1-universal.jar
  1  net.minecraftforge.fml.client.FMLClientHandler.setupServerList()V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar
  1  net.minecraftforge.fml.client.FMLClientHandler.startIntegratedServer(Ljava.lang.String;Ljava.lang.String;Lnet.minecraft.world.WorldSettings;)V  # Chunk Pregenerator-V1.12-2.5.0.jar
  1  net.minecraftforge.fml.client.FMLClientHandler.tryLoadExistingWorld(Lnet.minecraft.client.gui.GuiWorldSelection;Lnet.minecraft.world.storage.WorldSummary;)V  # CyclopsCore-1.12.2-1.6.7.jar
  1  net.minecraftforge.fml.client.GuiModList.selectModIndex(I)V  # MPUtils-1.12.2-1.5.7.jar
  1  net.minecraftforge.fml.client.GuiOldSaveLoadConfirm.<init>(Ljava.lang.String;Ljava.lang.String;Lnet.minecraft.client.gui.GuiScreen;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar
  1  net.minecraftforge.fml.client.config.GuiConfig.b()V  # MouseTweaks-2.10-mc1.12.2.jar
  1  net.minecraftforge.fml.client.config.GuiConfig.m()V  # MouseTweaks-2.10-mc1.12.2.jar
  1  org.lwjgl.openal.AL.getDevice()Lorg.lwjgl.openal.ALCdevice;  # themidnight-0.3.5.jar
  1  org.lwjgl.opengl.GL11.glColorPointer(IILjava.nio.DoubleBuffer;)V  # Numina-1.12.2-1.0.38.jar
  1  org.lwjgl.opengl.GL11.glVertexPointer(IILjava.nio.DoubleBuffer;)V  # Numina-1.12.2-1.0.38.jar
  1  org.lwjgl.util.glu.GLU.gluLookAt(FFFFFFFFF)V  # HammerLib-1.12.2-2.0.6.32.jar
  1  paulscode.sound.Library.getSource(Ljava.lang.String;)Lpaulscode.sound.Source;  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.SoundSystem.newSource(ZLjava.lang.String;Ljava.net.URL;Ljava.lang.String;ZFFFIF)V  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.SoundSystem.pause(Ljava.lang.String;)V  # TheBetweenlands-3.9.6-universal.jar
  1  paulscode.sound.SoundSystem.removeSource(Ljava.lang.String;)V  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.SoundSystem.stop(Ljava.lang.String;)V  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.Source.playing()Z  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.Source.setPitch(F)V  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.Source.sourceVolume:F  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.Source.stop()V  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.Source.toLoop:Z  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.libraries.ChannelLWJGLOpenAL.ALSource:Ljava.nio.IntBuffer;  # TheBetweenlands-3.9.6-universal.jar
  1  paulscode.sound.libraries.ChannelLWJGLOpenAL.attachedSource:Lpaulscode.sound.Source;  # TheBetweenlands-3.9.6-universal.jar
  1  paulscode.sound.libraries.SourceLWJGLOpenAL.channel:Lpaulscode.sound.Channel;  # TheBetweenlands-3.9.6-universal.jar
```

## mod-side API (top 19 of 19)

```
  2  baubles.common.Config.renderBaubles:Z  # Artifacts-1.12.2-1.2.3.jar, thaumicperiphery-0.3.1.jar
  2  mcp.mobius.waila.config.FormattingConfig.entityFormat:Ljava.lang.String;  # TheBetweenlands-3.9.6-universal.jar, iceandfire-1.9.1-1.12.2.jar
  2  mcp.mobius.waila.config.FormattingConfig.modNameFormat:Ljava.lang.String;  # TheBetweenlands-3.9.6-universal.jar, iceandfire-1.9.1-1.12.2.jar
  1  baubles.common.network.PacketHandler.INSTANCE:Lnet.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;  # Botania r1.10-363.jar
  1  baubles.common.network.PacketSync.<init>(Lnet.minecraft.entity.player.EntityPlayer;ILnet.minecraft.item.ItemStack;)V  # Botania r1.10-363.jar
  1  codechicken.nei.NEIController.<init>()V  # DankNull-1.12.2-1.7.101.jar
  1  codechicken.nei.NEIController.mouseClicked(Lnet.minecraft.client.gui.GuiScreen;III)Z  # DankNull-1.12.2-1.7.101.jar
  1  codechicken.nei.handler.NEIClientEventHandler.inputHandlers:Ljava.util.LinkedList;  # DankNull-1.12.2-1.7.101.jar
  1  com.mojang.realmsclient.dto.RealmsServer.id:J  # journeymap-1.12.2-5.7.1.jar
  1  com.mojang.realmsclient.dto.RealmsServer.name:Ljava.lang.String;  # journeymap-1.12.2-5.7.1.jar
  1  mcp.mobius.waila.addons.core.HUDHandlerEntities.maxhpfortext:F  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.addons.core.HUDHandlerEntities.nhearts:I  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.api.IWailaFMPAccessor.getTileEntity()Lnet.minecraft.tileentity.TileEntity;  # sonarcore-1.12.2-5.0.19-20.jar
  1  mcp.mobius.waila.api.impl.ConfigHandler.getConfig(Ljava.lang.String;Ljava.lang.String;Z)Z  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.api.impl.ModuleRegistrar.registerBodyProvider(Lmcp.mobius.waila.api.IWailaFMPProvider;Ljava.lang.String;)V  # sonarcore-1.12.2-5.0.19-20.jar
  1  mcp.mobius.waila.cbcore.LangUtil.translateG(Ljava.lang.String;[Ljava.lang.Object;)Ljava.lang.String;  # Pam's HarvestCraft 1.12.2zg.jar
  1  mcp.mobius.waila.config.FormattingConfig.metaFormat:Ljava.lang.String;  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.overlay.DisplayUtil.itemDisplayNameShort(Lnet.minecraft.item.ItemStack;)Ljava.lang.String;  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.utils.ModIdentification.findModContainer(Ljava.lang.String;)Lnet.minecraftforge.fml.common.ModContainer;  # TheBetweenlands-3.9.6-universal.jar
```

