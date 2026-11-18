# 1.12.2: stub ranking (synthesized members by number of jars)

Generated 2026-11-18 by `tools/dev/agent-tasks/make_stub_ranking.py` from the offline probe of 723 jar(s) (656 links, 31 unresolved, 24 missing-dependency, 12 obsolete); 700 distinct synthesized members. Regenerate after implementing; implemented members drop out. Format: `n  member  # jars`.

## server-side API (top 150 of 251)

```
  2  net.minecraft.entity.ai.EntityAIWander.y:D  # NarutoMod-1.12.2-0.3.1.beta.jar, SCP Lockdown-1.12.2-2.4.3-release.jar
  2  net.minecraft.entity.ai.EntityAIWander.z:D  # NarutoMod-1.12.2-0.3.1.beta.jar, SCP Lockdown-1.12.2-2.4.3-release.jar
  2  net.minecraft.entity.passive.EntityTameable.NO_ANIMATION:Lnet.ilexiconn.llibrary.server.animation.Animation;  # lepidodendron-1.12.2-63.02.jar, rats-3.2.13-1.12.2.jar
  2  net.minecraft.inventory.Container.doWorkMekanism()V  # AEAdditions-1.12.2-1.3.8.jar, ExtraCells-1.12.2-2.6.7.jar
  2  net.minecraft.inventory.ContainerBeacon.<init>(Lnet.minecraft.inventory.IInventory;Lnet.minecraft.inventory.IInventory;)V  # Cyclic-1.12.2-1.20.14.jar, Cyclic-1.12.2-1.20.8.jar
  2  net.minecraft.inventory.IInventory.isItemValidForSlotGas(ILnet.minecraft.item.ItemStack;)Z  # AEAdditions-1.12.2-1.3.8.jar, ExtraCells-1.12.2-2.6.7.jar
  2  net.minecraft.inventory.IInventory.setInventorySlotContentsGas(ILnet.minecraft.item.ItemStack;)V  # AEAdditions-1.12.2-1.3.8.jar, ExtraCells-1.12.2-2.6.7.jar
  2  net.minecraft.item.Item.getItemStackDisplayNameGas(Lnet.minecraft.item.ItemStack;)Ljava.lang.String;  # AEAdditions-1.12.2-1.3.8.jar, ExtraCells-1.12.2-2.6.7.jar
  2  net.minecraft.item.Item.getSubItemsGas(Lnet.minecraft.util.NonNullList;)V  # AEAdditions-1.12.2-1.3.8.jar, ExtraCells-1.12.2-2.6.7.jar
  2  net.minecraft.nbt.NBTTagCompound.tagMap:Ljava.util.Map;  # Morph-1.12.2-7.2.1.jar, llibrary-1.7.20-1.12.2.jar
  2  net.minecraft.pathfinding.WalkNodeProcessor.currentEntity:Lnet.minecraft.entity.EntityLiving;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar, lepidodendron-1.12.2-63.02.jar
  2  net.minecraft.util.IStringSerializable.ADA_BIG:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.ADA_FLAT:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.PRI_BIG:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.PRI_FLAT:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.PURE_BIG:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.PURE_FLAT:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.SIM_BIG:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.util.IStringSerializable.SIM_FLAT:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.world.storage.WorldSavedData.get(Lnet.minecraft.world.World;)Lcom.dhanantry.scapeandrunparasites.world.SRPSaveData;  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.world.storage.WorldSavedData.setTotalKills(IIZLnet.minecraft.world.World;Z)Z  # SRPExtra-1.9.8.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraftforge.common.capabilities.ICapabilityProvider.canConnect(Lnet.minecraft.util.EnumFacing;)Z  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  net.minecraftforge.common.capabilities.ICapabilityProvider.node()Lli.cil.oc.api.network.Node;  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  net.minecraftforge.common.capabilities.ICapabilityProvider.node(Lcom.enderio.core.common.util.DyeColor;)Lli.cil.oc.api.network.Node;  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  net.minecraftforge.common.capabilities.ICapabilityProvider.onConnect(Lli.cil.oc.api.network.Node;)V  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  net.minecraftforge.common.capabilities.ICapabilityProvider.onDisconnect(Lli.cil.oc.api.network.Node;)V  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  net.minecraftforge.common.capabilities.ICapabilityProvider.onMessage(Lli.cil.oc.api.network.Message;)V  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  net.minecraftforge.common.capabilities.ICapabilityProvider.sidedNode(Lnet.minecraft.util.EnumFacing;)Lli.cil.oc.api.network.Node;  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  net.minecraftforge.fml.common.Loader.setupTestHarness([Lnet.minecraftforge.fml.common.ModContainer;)V  # Dimensional Doors-3.2.3.jar, DimensionalDoors-1.12.2-3.0.12.jar
  2  net.minecraftforge.fml.common.event.FMLStateEvent.<init>([Ljava.lang.Object;)V -> ()V  # RebornCore-1.12.2-3.19.5-universal.jar, RebornCore-FORK-1.12.2-3.19.5-universal.jar
  2  net.minecraftforge.fml.common.network.internal.NetworkModHolder.testVanillaAcceptance()V  # Customized-Dungeon-Loot-1.12 -(v.1.0.3).jar, RecurrentComplex-1.4.8.2.jar
  2  net.minecraftforge.fml.common.registry.VillagerRegistry.addExtraVillageComponents(Ljava.util.List;Ljava.util.Random;I)V  # VillageNames-1.12.2-4.1.5.jar, VillageNames-1.12.2-4.4.13.jar
  2  net.minecraftforge.fml.common.thread.SidedThreadGroup.newThread(Ljava.lang.Runnable;)Ljava.lang.Thread;  # HammerLib-1.12.2-12.2.66.jar, HammerLib-1.12.2-2.0.6.32.jar
  2  net.minecraftforge.fml.relauncher.libraries.Artifact.getFile()Ljava.io.File;  # !mixinbooter-12.4.jar, `FermiumBooter-1.6.5.jar
  2  net.minecraftforge.fml.relauncher.libraries.Repository.resolveAll(Lnet.minecraftforge.fml.relauncher.libraries.Artifact;)Lnet.minecraftforge.fml.relauncher.libraries.Artifact;  # !mixinbooter-12.4.jar, `FermiumBooter-1.6.5.jar
  1  net.minecraft.advancements.AdvancementList.loadAdvancements(Ljava.util.Map;)V  # Placebo-1.12.2-1.6.0.jar
  1  net.minecraft.advancements.AdvancementTreeNode.layout(Lnet.minecraft.advancements.Advancement;)V  # Placebo-1.12.2-1.6.0.jar
  1  net.minecraft.advancements.FunctionManager.functions:Ljava.util.Map;  # tombstone-4.1.2-1.12.2.jar
  1  net.minecraft.advancements.critereon.ConstructBeaconTrigger.trigger(Lnet.minecraft.entity.player.EntityPlayerMP;Lnet.minecraft.tileentity.TileEntityBeacon;)V  # Atum-1.12.2-2.0.20.jar
  1  net.minecraft.block.BlockChorusFlower.canSurvive(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;)Z  # simplytea-1.6.jar
  1  net.minecraft.block.BlockContainer.getBlockLiquidHeight(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.block.material.Material;)F  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.block.BlockContainer.getStateAtViewpoint(Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.world.IBlockAccess;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.util.math.Vec3d;)Lnet.minecraft.block.state.IBlockState;  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.block.BlockRailBase$Rail.<init>(Lnet.minecraft.block.BlockRailBase;Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.block.state.IBlockState;)V  # StevesCarts-1.12.2-2.4.32.137.jar
  1  net.minecraft.block.BlockRailBase$Rail.place(ZZ)Lnet.minecraft.block.BlockRailBase$Rail;  # StevesCarts-1.12.2-2.4.32.137.jar
  1  net.minecraft.entity.Entity.getOwner()Lnet.minecraft.entity.EntityLivingBase;  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.entity.Entity.makeFlySound()Z  # Modern-Warfare-Cubed-0.1.9.jar
  1  net.minecraft.entity.Entity.playFlySound(F)F  # Modern-Warfare-Cubed-0.1.9.jar
  1  net.minecraft.entity.Entity.setOwner(Lnet.minecraft.entity.EntityLivingBase;)V  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.entity.EntityLiving.inventoryArmor:Lnet.minecraft.util.NonNullList;  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.entity.EntityLivingBase.FLAGS:Lnet.minecraft.network.datasync.DataParameter;  # bbs-fs-2.8-1.12.2-alpha.1.jar
  1  net.minecraft.entity.ai.attributes.AttributeModifier.initShrinkingModifier()V  # Artifacts-1.12.2-1.2.3.jar
  1  net.minecraft.entity.item.EntityBoat.FLAGS:Lnet.minecraft.network.datasync.DataParameter;  # immersivepetroleum-1.12.2-1.1.9.jar
  1  net.minecraft.entity.monster.EntityElderGuardian.setGhost()V  # JustEnoughResources-1.12.2-0.9.2.60.jar
  1  net.minecraft.entity.monster.EntityGuardian.wander:Lnet.minecraft.entity.ai.EntityAIWander;  # moreplanets-mc1.12.2-v2.4.1.jar
  1  net.minecraft.entity.monster.EntityMob.attackEntityAsMobMinimum(Lnet.minecraft.entity.EntityLivingBase;)Z  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.entity.monster.EntityMob.getFeral()Lcom.dhanantry.scapeandrunparasites.entity.ai.misc.EntityPFeral;  # SRPExtra-1.9.8.jar
  1  net.minecraft.entity.monster.EntityMob.isBeingPossessed()Z  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.entity.monster.EntityMob.setBurrowed(Z)V  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.entity.passive.EntityHorse.getVariantTexturePaths()[Ljava.lang.String;  # journeymap-1.12.2-5.7.1.jar
  1  net.minecraft.entity.passive.EntityTameable.getAnimation()Lnet.ilexiconn.llibrary.server.animation.Animation;  # lepidodendron-1.12.2-63.02.jar
  1  net.minecraft.entity.passive.EntityVillager$ListEnchantedBookForEmeralds.<init>()V  # moreplanets-mc1.12.2-v2.4.1.jar
  1  net.minecraft.entity.player.EntityPlayer.updateCape()V  # MenuMobs-1.12.2-1.21.jar
  1  net.minecraft.entity.player.EntityPlayerMP.addEntity(Lnet.minecraft.entity.Entity;)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.entity.player.EntityPlayerMP.removeEntity(Lnet.minecraft.entity.Entity;)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.inventory.Container.loadBaubles(Lnet.minecraft.entity.player.EntityPlayer;)V  # IC2Classic-1.12.2-1.5.15.jar
  1  net.minecraft.item.Item.getAmulet(Lnet.minecraft.entity.player.EntityPlayer;)Lnet.minecraft.item.ItemStack;  # Atum-1.12.2-2.0.20.jar
  1  net.minecraft.item.Item.getBauble(Lnet.minecraft.entity.player.EntityPlayer;Ljava.util.List;)V  # DarkUtils-1.12.2-1.8.230.jar
  1  net.minecraft.item.Item.getBaublesInventory(Lnet.minecraft.entity.player.EntityPlayer;)Lnet.minecraft.inventory.IInventory;  # Atum-1.12.2-2.0.20.jar
  1  net.minecraft.item.Item.hasBauble(Lnet.minecraft.entity.player.EntityPlayer;)Z  # DarkUtils-1.12.2-1.8.230.jar
  1  net.minecraft.item.Item.lastMax:Lcom.creativemd.littletiles.common.util.place.PlacementPosition;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.item.Item.min:Lcom.creativemd.littletiles.common.util.place.PlacementPosition;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.item.ItemBow.findDragonBoneArrow(Lnet.minecraft.entity.player.EntityPlayer;)Lnet.minecraft.item.ItemStack;  # spartanfire_rlcraft-1.4.0.jar
  1  net.minecraft.item.ItemBow.isDragonboneArrow(Lnet.minecraft.item.ItemStack;)Z  # spartanfire_rlcraft-1.4.0.jar
  1  net.minecraft.item.ItemSimpleFoiled.<init>()V  # defiledlands-1.12.2-1.4.3.jar
  1  net.minecraft.item.ItemSword.shouldProc(Lnet.minecraft.item.ItemStack;D)Z  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.item.crafting.CraftingManager.getRecipeById(I)Lnet.minecraft.item.crafting.IRecipe;  # calcmod-forge-1.12.2-1.3.0.jar
  1  net.minecraft.item.crafting.RecipeTippedArrow.<init>()V  # bewitchment-1.12.2-0.0.22.64.jar
  1  net.minecraft.item.crafting.ShapedRecipes.deserializeIngredient(Lcom.google.gson.JsonElement;)Lnet.minecraft.item.crafting.Ingredient;  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.launchwrapper.IClassNameTransformer.unmapClassName(Ljava.lang.String;)Ljava.lang.String;  # !mixinbooter-12.4.jar
  1  net.minecraft.nbt.NBTTagCompound.readNBT(BLjava.lang.String;Ljava.io.DataInput;ILnet.minecraft.nbt.NBTSizeTracker;)Lnet.minecraft.nbt.NBTBase;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.network.EnumConnectionState.STATES_BY_CLASS:Ljava.util.Map;  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.network.EnumConnectionState.directionMaps:Ljava.util.Map;  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.network.NetHandlerPlayServer.lastPositionUpdate:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.network.NetHandlerPlayServer.networkTickCount:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.network.NetworkManager.acceptInboundMessage(Ljava.lang.Object;)Z  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelActive(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelInactive(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelRead(Lio.netty.channel.ChannelHandlerContext;Ljava.lang.Object;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelReadComplete(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelRegistered(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelUnregistered(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelWritabilityChanged(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
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
  1  net.minecraft.network.play.server.SPacketBlockChange.blockState:Lnet.minecraft.block.state.IBlockState;  # IndustrialUpgrade-1.12.2-3.3.0.43.jar
  1  net.minecraft.network.play.server.SPacketCamera.<init>(Lnet.minecraft.entity.Entity;)V  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.network.play.server.SPacketRecipeBook$State.ADD:Lnet.minecraft.network.play.server.SPacketRecipeBook$State;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.network.play.server.SPacketRecipeBook$State.INIT:Lnet.minecraft.network.play.server.SPacketRecipeBook$State;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.network.play.server.SPacketRecipeBook$State.REMOVE:Lnet.minecraft.network.play.server.SPacketRecipeBook$State;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.network.play.server.SPacketRecipeBook$State.values()[Lnet.minecraft.network.play.server.SPacketRecipeBook$State;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.network.play.server.SPacketTabComplete.getMatches()[Ljava.lang.String;  # brigo_forge-1.1.1+1.12.x.jar
  1  net.minecraft.network.play.server.SPacketWorldBorder$Action.LERP_SIZE:Lnet.minecraft.network.play.server.SPacketWorldBorder$Action;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.network.play.server.SPacketWorldBorder$Action.SET_CENTER:Lnet.minecraft.network.play.server.SPacketWorldBorder$Action;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.network.play.server.SPacketWorldBorder$Action.SET_SIZE:Lnet.minecraft.network.play.server.SPacketWorldBorder$Action;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.network.play.server.SPacketWorldBorder$Action.SET_WARNING_BLOCKS:Lnet.minecraft.network.play.server.SPacketWorldBorder$Action;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.network.play.server.SPacketWorldBorder$Action.SET_WARNING_TIME:Lnet.minecraft.network.play.server.SPacketWorldBorder$Action;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.network.play.server.SPacketWorldBorder.<init>(Lnet.minecraft.world.border.WorldBorder;Lnet.minecraft.network.play.server.SPacketWorldBorder$Action;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.server.MinecraftServer.<init>(Ljava.io.File;Ljava.net.Proxy;Lnet.minecraft.util.datafix.DataFixer;Lcom.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;Lcom.mojang.authlib.minecraft.MinecraftSessionService;Lcom.mojang.authlib.GameProfileRepository;Lnet.minecraft.server.management.PlayerProfileCache;)V -> ()V  # lan-properties_forge-1.0.0+1.12-1.12.2.jar
  1  net.minecraft.server.MinecraftServer.getMaxWorldSize()I  # lan-properties_forge-1.0.0+1.12-1.12.2.jar
  1  net.minecraft.server.MinecraftServer.getNetworkCompressionThreshold()I  # lan-properties_forge-1.0.0+1.12-1.12.2.jar
  1  net.minecraft.server.MinecraftServer.reload()V  # FTBLib-5.4.7.2.jar
  1  net.minecraft.server.MinecraftServer.setPlayerIdleTimeout(I)V  # lan-properties_forge-1.0.0+1.12-1.12.2.jar
  1  net.minecraft.server.MinecraftServer.setPlayerList(Lnet.minecraft.server.management.PlayerList;)V  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.server.MinecraftServer.setResourcePack(Ljava.lang.String;Ljava.lang.String;)V  # lan-properties_forge-1.0.0+1.12-1.12.2.jar
  1  net.minecraft.server.MinecraftServer.stopServer()V  # WorldgenBlockReplacer-1.12.2-2.2.0.jar
  1  net.minecraft.server.dedicated.DedicatedPlayerList.<init>(Lnet.minecraft.server.dedicated.DedicatedServer;)V  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.server.dedicated.DedicatedPlayerList.initializeConnectionToPlayer(Lnet.minecraft.network.NetworkManager;Lnet.minecraft.entity.player.EntityPlayerMP;Lnet.minecraft.network.NetHandlerPlayServer;)V  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.server.integrated.IntegratedPlayerList.<init>(Lnet.minecraft.server.integrated.IntegratedServer;)V  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.server.integrated.IntegratedPlayerList.initializeConnectionToPlayer(Lnet.minecraft.network.NetworkManager;Lnet.minecraft.entity.player.EntityPlayerMP;Lnet.minecraft.network.NetHandlerPlayServer;)V  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.server.integrated.IntegratedServer.getWorldIconFile()Ljava.io.File;  # JurassicReborn-1.12.2-1.3.4.jar
  1  net.minecraft.server.integrated.IntegratedServer.isWorldIconSet()Z  # JurassicReborn-1.12.2-1.3.4.jar
  1  net.minecraft.server.integrated.IntegratedServer.loadAllWorlds(Ljava.lang.String;Ljava.lang.String;JLnet.minecraft.world.WorldType;Ljava.lang.String;)V  # Chunk Pregenerator-V1.12-2.5.0.jar
  1  net.minecraft.server.integrated.IntegratedServer.startServerThread()V  # Chunk Pregenerator-V1.12-2.5.0.jar
  1  net.minecraft.server.management.PlayerList.readPlayerDataFromFile(Lnet.minecraft.entity.player.EntityPlayerMP;)Lnet.minecraft.nbt.NBTTagCompound;  # Morph-1.12.2-7.2.1.jar
  1  net.minecraft.server.management.PlayerProfileCache.getByUUID(Ljava.util.UUID;)Lnet.minecraft.server.management.PlayerProfileCache$ProfileEntry;  # _supermartijn642corelib-1.1.26b-forge-mc1.12.jar
  1  net.minecraft.tileentity.TileEntity.addTileToENet()V  # nuclearcraft-1.12.2-2.19.jar
  1  net.minecraft.tileentity.TileEntity.addToIc2EnergyNetwork()V  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.deregister()V  # Mekanism-1.12.2-9.8.3.390.jar
  1  net.minecraft.tileentity.TileEntity.getEUCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Ljava.lang.Object;  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.getRFCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Ljava.lang.Object;  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.getRSChance(B)D  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.tileentity.TileEntity.getTeslaCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Ljava.lang.Object;  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.hasEUCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Z  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.hasRFCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Z  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.hasTeslaCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Z  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.receiveUpdatePacket(Lnet.minecraft.nbt.NBTTagCompound;)V  # CreativeCore_v1.10.70_mc1.12.2.jar
  1  net.minecraft.tileentity.TileEntity.register()V  # Mekanism-1.12.2-9.8.3.390.jar
  1  net.minecraft.tileentity.TileEntity.removeFromIc2EnergyNetwork()V  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.removeTileFromENet()V  # nuclearcraft-1.12.2-2.19.jar
  1  net.minecraft.tileentity.TileEntity.transferToRFMachine(Lnet.minecraft.tileentity.TileEntity;ILnet.minecraft.util.EnumFacing;)I  # _NTM-Extended-1.12.2-3.0.3.jar
  1  net.minecraft.tileentity.TileEntityShulkerBox.<init>(Lnet.minecraft.item.EnumDyeColor;)V -> ()V  # srpcotesia-1.12.2-1.3.4h1.jar
```

## client-side API (top 150 of 408)

```
  7  net.minecraft.client.renderer.chunk.RenderChunk.getCompiledChunk()Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # LittleTiles_v1.5.58_mc1.12.2.jar, NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar ...
  7  net.minecraft.client.renderer.chunk.RenderChunk.getPosition()Lnet.minecraft.util.math.BlockPos;  # LittleTiles_v1.5.58_mc1.12.2.jar, NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar ...
  5  net.minecraft.client.Minecraft.getTutorial()Lnet.minecraft.client.tutorial.Tutorial;  # Call to Battle 3.jar, JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar ...
  5  net.minecraft.client.renderer.chunk.CompiledChunk.getTileEntities()Ljava.util.List;  # NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, _NTM-Extended-1.12.2-3.0.3.jar ...
  5  net.minecraft.client.renderer.chunk.CompiledChunk.isLayerEmpty(Lnet.minecraft.util.BlockRenderLayer;)Z  # LittleTiles_v1.5.58_mc1.12.2.jar, NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar ...
  5  net.minecraft.client.renderer.chunk.RenderChunk.boundingBox:Lnet.minecraft.util.math.AxisAlignedBB;  # NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar ...
  5  net.minecraft.client.renderer.chunk.RenderChunk.compiledChunk:Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # NTM-CE-1.12.2-2.6.1.0.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, _NTM-Extended-1.12.2-3.0.3.jar ...
  5  paulscode.sound.SoundSystem.play(Ljava.lang.String;)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, Pixelmon-1.12.2-8.4.3-universal.jar ...
  4  net.minecraft.client.renderer.ItemRenderer.renderOverlays(F)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, modularwarfare-2.4.2f.jar ...
  4  net.minecraft.client.renderer.RenderGlobal.createBindEntityOutlineFbs(II)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, bbs-fs-2.8-1.12.2-alpha.1.jar ...
  4  net.minecraft.client.renderer.RenderGlobal.setWorldAndLoadRenderers(Lnet.minecraft.client.multiplayer.WorldClient;)V  # Call to Battle 3.jar, iChunUtil-1.12.2-7.2.2.jar, modularwarfare-2.4.2f.jar ...
  4  net.minecraft.client.renderer.chunk.RenderChunk.getVertexBufferByLayer(I)Lnet.minecraft.client.renderer.vertex.VertexBuffer;  # LittleTiles_v1.5.58_mc1.12.2.jar, NTM-CE-1.12.2-2.6.1.0.jar, _NTM-Extended-1.12.2-3.0.3.jar ...
  4  net.minecraft.client.renderer.chunk.RenderChunk.needsUpdate()Z  # LittleTiles_v1.5.58_mc1.12.2.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar ...
  4  net.minecraft.client.renderer.chunk.RenderChunk.setNeedsUpdate(Z)V  # LittleTiles_v1.5.58_mc1.12.2.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar ...
  4  net.minecraft.client.tutorial.Tutorial.handleMouse(Lnet.minecraft.util.MouseHelper;)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar ...
  4  paulscode.sound.SoundSystem.pause(Ljava.lang.String;)V  # IC2Classic-1.12.2-1.5.15.jar, Pixelmon-1.12.2-8.4.3-universal.jar, TheBetweenlands-3.9.6-universal.jar ...
  4  paulscode.sound.SoundSystem.setPitch(Ljava.lang.String;F)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, fairylights-2.2.0-1.12.2.jar ...
  4  paulscode.sound.SoundSystem.setVolume(Ljava.lang.String;F)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, fairylights-2.2.0-1.12.2.jar ...
  3  net.minecraft.client.gui.GuiCustomizeSkin.<init>(Lnet.minecraft.client.gui.GuiScreen;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar, HammerLib-1.12.2-12.2.66.jar, HammerLib-1.12.2-2.0.6.32.jar
  3  net.minecraft.client.gui.GuiIngame.renderGameOverlay(F)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraft.client.gui.inventory.GuiContainer.updateDragSplitting()V  # BrandonsCore-1.12.2-2.4.20.162-universal.jar, HammerLib-1.12.2-12.2.66.jar, HammerLib-1.12.2-2.0.6.32.jar
  3  net.minecraft.client.renderer.ChunkRenderContainer.initialize(DDD)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar, iChunUtil-1.12.2-7.2.2.jar
  3  net.minecraft.client.renderer.EntityRenderer.pointedEntity:Lnet.minecraft.entity.Entity;  # Draconic-Evolution-1.12.2-2.3.28.354-universal.jar, ReachFix-1.12.2-1.0.9.jar, ReachFix-1.12.2-1.1.3.jar
  3  net.minecraft.client.renderer.RenderGlobal$ContainerLocalRenderInformation.renderChunk:Lnet.minecraft.client.renderer.chunk.RenderChunk;  # NTM-CE-1.12.2-2.6.1.0.jar, [1.12.2] SecurityCraft v1.10.1.jar, iChunUtil-1.12.2-7.2.2.jar
  3  net.minecraft.client.renderer.RenderGlobal.renderEntityOutlineFramebuffer()V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraft.client.renderer.RenderGlobal.viewFrustum:Lnet.minecraft.client.renderer.ViewFrustum;  # Inspirations-1.12.2-0.2.9.jar, NTM-CE-1.12.2-2.6.1.0.jar, iChunUtil-1.12.2-7.2.2.jar
  3  net.minecraft.client.renderer.chunk.CompiledChunk.DUMMY:Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # LittleTiles_v1.5.58_mc1.12.2.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar
  3  net.minecraft.client.renderer.chunk.CompiledChunk.isLayerStarted(Lnet.minecraft.util.BlockRenderLayer;)Z  # LittleTiles_v1.5.58_mc1.12.2.jar, SCP Lockdown-1.12.2-2.4.3-release.jar, iChunUtil-1.12.2-7.2.2.jar
  3  net.minecraft.client.renderer.chunk.RenderChunk.multModelviewMatrix()V  # NTM-CE-1.12.2-2.6.1.0.jar, _NTM-Extended-1.12.2-3.0.3.jar, hbm-1.8.4a-G.jar
  3  net.minecraftforge.client.ForgeHooksClient.getFogDensity(Lnet.minecraft.client.renderer.EntityRenderer;Lnet.minecraft.entity.Entity;Lnet.minecraft.block.state.IBlockState;FF)F  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraftforge.client.ForgeHooksClient.loadEntityShader(Lnet.minecraft.entity.Entity;Lnet.minecraft.client.renderer.EntityRenderer;)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraftforge.client.ForgeHooksClient.onDrawBlockHighlight(Lnet.minecraft.client.renderer.RenderGlobal;Lnet.minecraft.entity.player.EntityPlayer;Lnet.minecraft.util.math.RayTraceResult;IF)Z  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraftforge.client.ForgeHooksClient.onFogRender(Lnet.minecraft.client.renderer.EntityRenderer;Lnet.minecraft.entity.Entity;Lnet.minecraft.block.state.IBlockState;FIF)V  # JurassicReborn-1.12.2-1.3.4.jar, Modern-Warfare-Cubed-0.1.9.jar, 永爱之刃-v11削弱.jar
  3  net.minecraftforge.client.event.GuiScreenEvent$PotionShiftEvent.<init>(Lnet.minecraft.client.gui.GuiScreen;)V  # Cyclic-1.12.2-1.20.14.jar, Cyclic-1.12.2-1.20.8.jar, Trinkets and Baubles-Forge-1.12.2-0.33.4.jar
  3  paulscode.sound.SoundBuffer.audioData:[B  # Matmos_7.0_1.12.2.jar, bbs-2.5-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  3  paulscode.sound.SoundSystem.newSource(ZLjava.lang.String;Ljava.net.URL;Ljava.lang.String;ZFFFIF)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  3  paulscode.sound.SoundSystem.removeSource(Ljava.lang.String;)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  3  paulscode.sound.SoundSystem.stop(Ljava.lang.String;)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  3  paulscode.sound.SoundSystemConfig.THREAD_SYNC:Ljava.lang.Object;  # AmbientSounds_v3.1.5_mc1.12.2.jar, TheBetweenlands-3.9.6-universal.jar, industrialcraft-2-2.8.222-ex112.jar
  2  net.minecraft.client.entity.EntityPlayerSP.sprintingTicksLeft:I  # Galacticraft-1.12.2-4.0.6.jar, walljump-1.12.2-1.3.2.jar
  2  net.minecraft.client.gui.GuiIngameMenu.<init>()V  # LoTAS1.12.2-2.1.3.jar, pokelucky-RG-6.16.14(1.12.2).jar
  2  net.minecraft.client.gui.GuiNewChat.chatLines:Ljava.util.List;  # HammerLib-1.12.2-12.2.66.jar, HammerLib-1.12.2-2.0.6.32.jar
  2  net.minecraft.client.gui.GuiScreenOptionsSounds.<init>(Lnet.minecraft.client.gui.GuiScreen;Lnet.minecraft.client.settings.GameSettings;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar, IC2Classic-1.12.2-1.5.15.jar
  2  net.minecraft.client.model.ModelShulkerBullet.<init>()V  # champions-1.12.2-1.0.11.10.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.client.network.NetHandlerPlayClient.handleCustomSound(Lnet.minecraft.network.play.server.SPacketCustomSound;)V  # HammerLib-1.12.2-12.2.66.jar, HammerLib-1.12.2-2.0.6.32.jar
  2  net.minecraft.client.renderer.ActiveRenderInfo.updateRenderInfo(Lnet.minecraft.entity.Entity;Z)V  # JurassicReborn-1.12.2-1.3.4.jar, 永爱之刃-v11削弱.jar
  2  net.minecraft.client.renderer.BufferBuilder.putColorRGBA(IIII)V  # Call to Battle 3.jar, SilentLib-1.12.2-3.0.14+168.jar
  2  net.minecraft.client.renderer.EntityRenderer.cameraPitch:D  # Galacticraft-1.12.2-4.0.6.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.EntityRenderer.cameraYaw:D  # Galacticraft-1.12.2-4.0.6.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.EntityRenderer.cameraZoom:D  # Galacticraft-1.12.2-4.0.6.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.EntityRenderer.farPlaneDistance:F  # Modern-Warfare-Cubed-0.1.9.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.EntityRenderer.getFOVModifier(FZ)F  # Modern-Warfare-Cubed-0.1.9.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.ItemRenderer.equippedProgressMainHand:F  # iChunUtil-1.12.2-7.2.2.jar, techguns-2.2.0.1.jar
  2  net.minecraft.client.renderer.RegionRenderCacheBuilder.getWorldRendererByLayer(Lnet.minecraft.util.BlockRenderLayer;)Lnet.minecraft.client.renderer.BufferBuilder;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.RenderGlobal.cloudTickCounter:I  # iChunUtil-1.12.2-7.2.2.jar, rftoolsdim-1.12-5.71.jar
  2  net.minecraft.client.renderer.RenderGlobal.hasNoChunkUpdates()Z  # JurassicReborn-1.12.2-1.3.4.jar, bbs-2.5-1.12.2.jar
  2  net.minecraft.client.renderer.RenderGlobal.lastViewEntityYaw:D  # PortalGun-1.12.2-7.1.0.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.RenderGlobal.onEntityRemoved(Lnet.minecraft.entity.Entity;)V  # DramaticTrees-1.12.2-0.13.6.jar, 永爱之刃-v11削弱.jar
  2  net.minecraft.client.renderer.RenderList.<init>()V  # SCP Lockdown-1.12.2-2.4.3-release.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.VboRenderList.<init>()V  # SCP Lockdown-1.12.2-2.4.3-release.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.ViewFrustum.<init>(Lnet.minecraft.world.World;ILnet.minecraft.client.renderer.RenderGlobal;Lnet.minecraft.client.renderer.chunk.IRenderChunkFactory;)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status.COMPILING:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type.REBUILD_CHUNK:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getRegionRenderCacheBuilder()Lnet.minecraft.client.renderer.RegionRenderCacheBuilder;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getStatus()Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.<init>()V  # SCP Lockdown-1.12.2-2.4.3-release.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.runChunkUploads(J)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar, bbs-2.5-1.12.2.jar
  2  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.updateChunkNow(Lnet.minecraft.client.renderer.chunk.RenderChunk;)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar, bbs-2.5-1.12.2.jar
  2  net.minecraft.client.renderer.chunk.CompiledChunk.isVisible(Lnet.minecraft.util.EnumFacing;Lnet.minecraft.util.EnumFacing;)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar
  2  net.minecraft.client.renderer.chunk.ListChunkFactory.<init>()V  # AdvancedRocketry-1.12.2-1.7.0-232-universal.jar, SCP Lockdown-1.12.2-2.4.3-release.jar
  2  net.minecraft.client.renderer.chunk.ListedRenderChunk.<init>(Lnet.minecraft.world.World;Lnet.minecraft.client.renderer.RenderGlobal;I)V  # [1.12.2] SecurityCraft v1.10.1.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.<init>(Lnet.minecraft.world.World;Lnet.minecraft.client.renderer.RenderGlobal;I)V  # [1.12.2] SecurityCraft v1.10.1.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.clearNeedsUpdate()V  # SCP Lockdown-1.12.2-2.4.3-release.jar, bbs-2.5-1.12.2.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.deleteGlResources()V  # SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.renderChunksUpdated:I  # Bookshelf-1.12.2-2.3.590.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.RenderChunk.setPosition(III)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, [1.12.2] SecurityCraft v1.10.1.jar
  2  net.minecraft.client.renderer.chunk.VisGraph.<init>()V  # SCP Lockdown-1.12.2-2.4.3-release.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.VisGraph.computeVisibility()Lnet.minecraft.client.renderer.chunk.SetVisibility;  # NTM-CE-1.12.2-2.6.1.0.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.chunk.VisGraph.setOpaqueCube(Lnet.minecraft.util.math.BlockPos;)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.entity.RenderDragon.ENDERCRYSTAL_BEAM_TEXTURES:Lnet.minecraft.util.ResourceLocation;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar, moreplanets-mc1.12.2-v2.4.1.jar
  2  net.minecraft.client.renderer.entity.RenderFireball.<init>(Lnet.minecraft.client.renderer.entity.RenderManager;F)V  # Fish's Undead Rising-1.4.2.jar, Instrumental-Mobs-1.2.1.jar
  2  net.minecraft.client.renderer.entity.layers.LayerVillagerArmor.<init>(Lnet.minecraft.client.renderer.entity.RenderLivingBase;)V  # VillageNames-1.12.2-4.1.5.jar, VillageNames-1.12.2-4.4.13.jar
  2  net.minecraft.client.tutorial.Tutorial.<init>(Lnet.minecraft.client.Minecraft;)V  # ElenaiDodge2-1.12.2-1.0.8b.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  2  net.minecraft.client.tutorial.Tutorial.openInventory()V  # modularwarfare-2.4.2f.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  2  net.minecraft.util.MovementInputFromOptions.<init>(Lnet.minecraft.client.settings.GameSettings;)V  # Chisel-MC1.12.2-1.0.2.45.jar, MenuMobs-1.12.2-1.21.jar
  2  net.minecraftforge.client.ForgeHooksClient.putQuadColor(Lnet.minecraft.client.renderer.BufferBuilder;Lnet.minecraft.client.renderer.block.model.BakedQuad;I)V  # ModularPowersuits-1.12.2-1.0.46.jar, Trinkets and Baubles-Forge-1.12.2-0.33.4.jar
  2  net.minecraftforge.client.ForgeHooksClient.renderLitItem(Lnet.minecraft.client.renderer.RenderItem;Lnet.minecraft.client.renderer.block.model.IBakedModel;ILnet.minecraft.item.ItemStack;)V  # GalaxySpace-1.12.2-2.2.0.jar, ThaumicAdditions-1.12.2-12.7.8.jar
  2  net.minecraftforge.common.model.Models.getHiddenModelPart(Lcom.google.common.collect.ImmutableList;)Lnet.minecraftforge.common.model.IModelPart;  # HeroesExpansion-1.12.2-1.3.5.jar, Numina-1.12.2-1.0.38.jar
  2  net.minecraftforge.fml.client.FMLClientHandler.addModAsResource(Lnet.minecraftforge.fml.common.ModContainer;)V  # modularwarfare-2.4.2f.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  2  net.minecraftforge.fml.client.FMLClientHandler.getClientPlayHandler()Lnet.minecraft.network.INetHandler;  # CoFHCore-1.12.2-4.6.6.1-universal.jar, Tropicraft-7.1.9.jar
  2  net.minecraftforge.fml.client.FMLConfigGuiFactory.<init>()V  # Login_Shield-1.12.2-6-g5654706.jar, chiselsandbits-14.33.jar
  2  org.lwjgl.openal.AL.getDevice()Lorg.lwjgl.openal.ALCdevice;  # PymTech-1.12.2-1.0.2.jar, themidnight-0.3.5.jar
  2  org.lwjgl.util.glu.GLU.gluLookAt(FFFFFFFFF)V  # HammerLib-1.12.2-12.2.66.jar, HammerLib-1.12.2-2.0.6.32.jar
  2  paulscode.sound.SoundSystem.activate(Ljava.lang.String;)V  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.SoundSystem.cull(Ljava.lang.String;)V  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.SoundSystem.getVolume(Ljava.lang.String;)F  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.SoundSystem.newStreamingSource(ZLjava.lang.String;Ljava.net.URL;Ljava.lang.String;ZFFFIF)V  # AmbientSounds_v3.1.5_mc1.12.2.jar, fairylights-2.2.0-1.12.2.jar
  2  paulscode.sound.SoundSystem.playing(Ljava.lang.String;)Z  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.SoundSystem.quickPlay(ZLjava.net.URL;Ljava.lang.String;ZFFFIF)Ljava.lang.String;  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.SoundSystem.setPosition(Ljava.lang.String;FFF)V  # IC2Classic-1.12.2-1.5.15.jar, industrialcraft-2-2.8.222-ex112.jar
  2  paulscode.sound.codecs.CodecJOrbis.<init>()V  # bbs-2.5-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  paulscode.sound.codecs.CodecJOrbis.cleanup()V  # bbs-2.5-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  paulscode.sound.codecs.CodecJOrbis.endOfStream()Z  # bbs-2.5-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  paulscode.sound.codecs.CodecJOrbis.getAudioFormat()Ljavax.sound.sampled.AudioFormat;  # bbs-2.5-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  paulscode.sound.codecs.CodecJOrbis.initialize(Ljava.net.URL;)Z  # bbs-2.5-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  2  paulscode.sound.codecs.CodecJOrbis.read()Lpaulscode.sound.SoundBuffer;  # bbs-2.5-1.12.2.jar, bbs-fs-2.8-1.12.2-alpha.1.jar
  1  net.minecraft.client.Minecraft.<init>(Lnet.minecraft.client.main.GameConfiguration;)V -> ()V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.Minecraft.draw(IIIIIIIIII)V  # simplytea-1.6.jar
  1  net.minecraft.client.Minecraft.getProfileProperties()Lcom.mojang.authlib.properties.PropertyMap;  # CustomPlayerModels-1.12.2-0.6.27a.jar
  1  net.minecraft.client.Minecraft.isJava64bit()Z  # itlt-1.12.2-1.0.3.jar
  1  net.minecraft.client.Minecraft.mcProfiler:Lnet.minecraft.profiler.Profiler;  # hbm-1.8.4a-G.jar
  1  net.minecraft.client.Minecraft.stopIntegratedServer()V  # LoTAS1.12.2-2.1.3.jar
  1  net.minecraft.client.audio.MovingSoundMinecart.<init>(Lnet.minecraft.entity.item.EntityMinecart;)V  # StevesCarts-1.12.2-2.4.32.137.jar
  1  net.minecraft.client.audio.MovingSoundMinecartRiding.<init>(Lnet.minecraft.entity.player.EntityPlayer;Lnet.minecraft.entity.item.EntityMinecart;)V  # StevesCarts-1.12.2-2.4.32.137.jar
  1  net.minecraft.client.audio.SoundManager.playingSounds:Ljava.util.Map;  # UniversalModCore-1.12.2-forge-1.3.1.jar
  1  net.minecraft.client.audio.SoundManager.reloadSoundSystem()V  # Surge-1.12.2-2.0.79.jar
  1  net.minecraft.client.entity.AbstractClientPlayer.rotateElytraX:F  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.client.entity.AbstractClientPlayer.rotateElytraY:F  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.client.entity.AbstractClientPlayer.rotateElytraZ:F  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.client.entity.EntityPlayerSP.closeScreenAndDropStack()V  # _supermartijn642corelib-1.1.26b-forge-mc1.12.jar
  1  net.minecraft.client.entity.EntityPlayerSP.interpTargetPitch:D  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.client.entity.EntityPlayerSP.interpTargetX:D  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.client.entity.EntityPlayerSP.interpTargetY:D  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.client.entity.EntityPlayerSP.interpTargetYaw:D  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.client.entity.EntityPlayerSP.interpTargetZ:D  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.client.entity.EntityPlayerSP.isCurrentViewEntity()Z  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.client.entity.EntityPlayerSP.newPosRotationIncrements:I  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.client.entity.EntityPlayerSP.recipeBook:Lnet.minecraft.stats.RecipeBook;  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.client.entity.EntityPlayerSP.setPermissionLevel(I)V  # bbs-2.5-1.12.2.jar
  1  net.minecraft.client.gui.FontRenderer.renderEngine:Lnet.minecraft.client.renderer.texture.TextureManager;  # BrandonsCore-1.12.2-2.4.20.162-universal.jar
  1  net.minecraft.client.gui.GuiBossOverlay.renderBossHealth()V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiChat.setCompletions([Ljava.lang.String;)V  # journeymap-1.12.2-5.7.1.jar
  1  net.minecraft.client.gui.GuiDisconnected.<init>(Lnet.minecraft.client.gui.GuiScreen;Ljava.lang.String;Lnet.minecraft.util.text.ITextComponent;)V  # UniversalModCore-1.12.2-forge-1.3.1.jar
  1  net.minecraft.client.gui.GuiDownloadTerrain.<init>()V  # Tropicraft-7.1.9.jar
  1  net.minecraft.client.gui.GuiIngame.chatListeners:Ljava.util.Map;  # FTBLib-5.4.7.2.jar
  1  net.minecraft.client.gui.GuiIngame.overlaySubtitle:Lnet.minecraft.client.gui.GuiSubtitleOverlay;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.persistantChatGUI:Lnet.minecraft.client.gui.GuiNewChat;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderAttackIndicator(FLnet.minecraft.client.gui.ScaledResolution;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderDemo(Lnet.minecraft.client.gui.ScaledResolution;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderExpBar(Lnet.minecraft.client.gui.ScaledResolution;I)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderHorseJumpBar(Lnet.minecraft.client.gui.ScaledResolution;I)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderHotbar(Lnet.minecraft.client.gui.ScaledResolution;F)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderPlayerStats(Lnet.minecraft.client.gui.ScaledResolution;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderPortal(FLnet.minecraft.client.gui.ScaledResolution;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderPotionEffects(Lnet.minecraft.client.gui.ScaledResolution;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderPumpkinOverlay(Lnet.minecraft.client.gui.ScaledResolution;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderScoreboard(Lnet.minecraft.scoreboard.ScoreObjective;Lnet.minecraft.client.gui.ScaledResolution;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderSelectedItem(Lnet.minecraft.client.gui.ScaledResolution;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiIngame.renderVignette(FLnet.minecraft.client.gui.ScaledResolution;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiOverlayDebug.<init>(Lnet.minecraft.client.Minecraft;)V  # 永爱之刃-v11削弱.jar
```

## mod-side API (top 25 of 25)

```
  5  com.mojang.realmsclient.dto.RealmsServer.id:J  # Xaeros_Minimap_FP24.2.0_Forge_1.12.jar, journeymap-1.12.2-5.7.1.jar, journeymap-forge-1.12.2-6.0.10.jar ...
  4  com.mojang.realmsclient.dto.RealmsServer.ownerUUID:Ljava.lang.String;  # Xaeros_Minimap_FP24.2.0_Forge_1.12.jar, journeymap-forge-1.12.2-6.0.10.jar, xaerominimap-forge-1.12.2-26.6.0.jar ...
  3  baubles.common.Config.renderBaubles:Z  # Artifacts-1.12.2-1.2.3.jar, RLArtifacts-1.1.2.jar, thaumicperiphery-0.3.1.jar
  3  baubles.common.network.PacketHandler.INSTANCE:Lnet.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;  # Botania r1.10-363.jar, Botania+r1.10-364.4.jar, Trinkets and Baubles-Forge-1.12.2-0.33.4.jar
  3  mcp.mobius.waila.config.FormattingConfig.modNameFormat:Ljava.lang.String;  # DramaticTrees-1.12.2-0.13.6.jar, TheBetweenlands-3.9.6-universal.jar, iceandfire-1.9.1-1.12.2.jar
  2  baubles.common.network.PacketSync.<init>(Lnet.minecraft.entity.player.EntityPlayer;ILnet.minecraft.item.ItemStack;)V  # Botania r1.10-363.jar, Botania+r1.10-364.4.jar
  2  com.mojang.realmsclient.dto.RealmsServer.name:Ljava.lang.String;  # journeymap-1.12.2-5.7.1.jar, journeymap-forge-1.12.2-6.0.10.jar
  2  mcp.mobius.waila.cbcore.LangUtil.translateG(Ljava.lang.String;[Ljava.lang.Object;)Ljava.lang.String;  # Cherry_on-1.12.2-1.3.35.jar, Pam's HarvestCraft 1.12.2zg.jar
  2  mcp.mobius.waila.config.FormattingConfig.entityFormat:Ljava.lang.String;  # TheBetweenlands-3.9.6-universal.jar, iceandfire-1.9.1-1.12.2.jar
  1  baubles.common.network.PacketOpenBaublesInventory.<init>()V  # Trinkets and Baubles-Forge-1.12.2-0.33.4.jar
  1  codechicken.nei.NEIController.<init>()V  # DankNull-1.12.2-1.7.101.jar
  1  codechicken.nei.NEIController.mouseClicked(Lnet.minecraft.client.gui.GuiScreen;III)Z  # DankNull-1.12.2-1.7.101.jar
  1  codechicken.nei.handler.NEIClientEventHandler.inputHandlers:Ljava.util.LinkedList;  # DankNull-1.12.2-1.7.101.jar
  1  com.mojang.realmsclient.dto.RealmsServer.owner:Ljava.lang.String;  # xaeroworldmap-forge-1.12.2-1.47.0.jar
  1  mcp.mobius.waila.addons.core.HUDHandlerEntities.maxhpfortext:F  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.addons.core.HUDHandlerEntities.nhearts:I  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.api.IWailaFMPAccessor.getTileEntity()Lnet.minecraft.tileentity.TileEntity;  # sonarcore-1.12.2-5.0.19-20.jar
  1  mcp.mobius.waila.api.event.WailaRenderEvent$Pre.setCanceled(Z)V  # [1.12.2] SecurityCraft v1.10.1.jar
  1  mcp.mobius.waila.api.impl.ConfigHandler.getConfig(Ljava.lang.String;Ljava.lang.String;Z)Z  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.api.impl.ModuleRegistrar.registerBodyProvider(Lmcp.mobius.waila.api.IWailaFMPProvider;Ljava.lang.String;)V  # sonarcore-1.12.2-5.0.19-20.jar
  1  mcp.mobius.waila.config.FormattingConfig.blockFormat:Ljava.lang.String;  # DramaticTrees-1.12.2-0.13.6.jar
  1  mcp.mobius.waila.config.FormattingConfig.metaFormat:Ljava.lang.String;  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.overlay.DisplayUtil.itemDisplayNameShort(Lnet.minecraft.item.ItemStack;)Ljava.lang.String;  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.utils.ModIdentification.findModContainer(Ljava.lang.String;)Lnet.minecraftforge.fml.common.ModContainer;  # TheBetweenlands-3.9.6-universal.jar
  1  mcp.mobius.waila.utils.ModIdentification.nameFromStack(Lnet.minecraft.item.ItemStack;)Ljava.lang.String;  # DramaticTrees-1.12.2-0.13.6.jar
```

## No faithful implementation possible

```
  5  net.minecraft.realms.RealmsBridge.<init>()V  # Realms was removed from the host game; nothing to bridge
  5  net.minecraft.realms.RealmsBridge.switchToRealms(Lnet.minecraft.client.gui.GuiScreen;)V  # Realms was removed from the host game; nothing to bridge
  5  net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer.<init>()V  # FML class-loader internals; no class loader of that kind exists on NeoForge
  4  net.minecraftforge.fml.common.asm.transformers.AccessTransformer.<init>(Ljava.lang.String;)V  # FML class-loader internals (the layer applies access transformers itself)
  3  net.minecraftforge.fml.common.FMLModContainer.<init>(Ljava.lang.String;Lnet.minecraftforge.fml.common.discovery.ModCandidate;Ljava.util.Map;)V -> ()V  # FML internals
  2  net.minecraft.entity.ai.EntityAIWander.x:D  # fields are only meaningful when the legacy task drives movement; the shim wraps the modern goal (needs a legacy-driven AI port)
  2  net.minecraft.realms.RealmsBridge.getNotificationScreen(Lnet.minecraft.client.gui.GuiScreen;)Lnet.minecraft.client.gui.GuiScreenRealmsProxy;  # Realms was removed from the host game; nothing to bridge
  2  net.minecraftforge.fml.common.discovery.ContainerType.DIR:Lnet.minecraftforge.fml.common.discovery.ContainerType;  # FML discovery internals
  2  net.minecraftforge.fml.common.discovery.ContainerType.JAR:Lnet.minecraftforge.fml.common.discovery.ContainerType;  # FML discovery internals
  2  net.minecraftforge.fml.common.discovery.ModCandidate.<init>(Ljava.io.File;Ljava.io.File;Lnet.minecraftforge.fml.common.discovery.ContainerType;)V -> (Ljava.io.File;)V  # FML discovery internals
  2  net.minecraftforge.fml.relauncher.libraries.LibraryManager.flattenLists(Ljava.io.File;)Ljava.util.List;  # FML library downloader, removed
  2  net.minecraftforge.fml.relauncher.libraries.LibraryManager.gatherLegacyCanidates(Ljava.io.File;)Ljava.util.List;  # FML library downloader, removed
  1  net.minecraft.network.NetworkManager.createNetworkManagerAndConnect(Ljava.net.InetAddress;IZ)Lnet.minecraft.network.NetworkManager;  # client connection internals; the modern Connection is not constructible this way
  1  net.minecraft.world.chunk.storage.RegionFile.getChunkDataOutputStream(II)Ljava.io.DataOutputStream;  # Anvil region IO is replaced by the host's own storage
  1  net.minecraftforge.fml.common.FMLModContainer.registerBus(Lcom.google.common.eventbus.EventBus;Lnet.minecraftforge.fml.common.LoadController;)Z  # FML internals
  1  net.minecraftforge.fml.relauncher.libraries.LibraryManager.MODSIDE:Ljava.util.jar.Attributes$Name;  # FML library downloader, removed
```
