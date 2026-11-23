# 1.12.2: stub ranking (synthesized members by number of jars)

Generated 2026-11-23 by `tools/dev/agent-tasks/make_stub_ranking.py` from the offline probe of 721 jar(s) (654 links, 29 missing-dependency, 26 unresolved, 12 obsolete); 791 distinct synthesized members. Regenerate after implementing; implemented members drop out. Format: `n  member  # jars`.

## server-side API (top 200 of 200)

```
  4  net.minecraft.entity.passive.EntityTameable.NO_ANIMATION:Lnet.ilexiconn.llibrary.server.animation.Animation;  # fossilsarcheology-8.0.6.jar, iceandfire-1.9.1-1.12.2.jar, lepidodendron-1.12.2-63.02.jar ...
  2  net.minecraft.inventory.Container.doWorkMekanism()V  # AEAdditions-1.12.2-1.3.8.jar, ExtraCells-1.12.2-2.6.7.jar
  2  net.minecraft.inventory.IInventory.isItemValidForSlotGas(ILnet.minecraft.item.ItemStack;)Z  # AEAdditions-1.12.2-1.3.8.jar, ExtraCells-1.12.2-2.6.7.jar
  2  net.minecraft.inventory.IInventory.setInventorySlotContentsGas(ILnet.minecraft.item.ItemStack;)V  # AEAdditions-1.12.2-1.3.8.jar, ExtraCells-1.12.2-2.6.7.jar
  2  net.minecraft.item.Item.getItemStackDisplayNameGas(Lnet.minecraft.item.ItemStack;)Ljava.lang.String;  # AEAdditions-1.12.2-1.3.8.jar, ExtraCells-1.12.2-2.6.7.jar
  2  net.minecraft.item.Item.getSubItemsGas(Lnet.minecraft.util.NonNullList;)V  # AEAdditions-1.12.2-1.3.8.jar, ExtraCells-1.12.2-2.6.7.jar
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
  2  net.minecraftforge.fml.common.LoadController.loader:Lnet.minecraftforge.fml.common.Loader;  # !mixinbooter-12.4.jar, !mixinbooter-5.0.jar
  2  net.minecraftforge.fml.common.Loader.setupTestHarness([Lnet.minecraftforge.fml.common.ModContainer;)V  # Dimensional Doors-3.2.3.jar, DimensionalDoors-1.12.2-3.0.12.jar
  2  net.minecraftforge.fml.common.network.internal.NetworkModHolder.testVanillaAcceptance()V  # Customized-Dungeon-Loot-1.12 -(v.1.0.3).jar, RecurrentComplex-1.4.8.2.jar
  2  net.minecraftforge.fml.relauncher.libraries.Artifact.getFile()Ljava.io.File;  # !mixinbooter-12.4.jar, `FermiumBooter-1.6.5.jar
  2  net.minecraftforge.fml.relauncher.libraries.Repository.resolveAll(Lnet.minecraftforge.fml.relauncher.libraries.Artifact;)Lnet.minecraftforge.fml.relauncher.libraries.Artifact;  # !mixinbooter-12.4.jar, `FermiumBooter-1.6.5.jar
  1  net.minecraft.advancements.AdvancementList.loadAdvancements(Ljava.util.Map;)V  # Placebo-1.12.2-1.6.0.jar
  1  net.minecraft.advancements.AdvancementTreeNode.layout(Lnet.minecraft.advancements.Advancement;)V  # Placebo-1.12.2-1.6.0.jar
  1  net.minecraft.block.BlockContainer.getBlockLiquidHeight(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.block.material.Material;)F  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.block.BlockContainer.getStateAtViewpoint(Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.world.IBlockAccess;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.util.math.Vec3d;)Lnet.minecraft.block.state.IBlockState;  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraft.entity.Entity.getOwner()Lnet.minecraft.entity.EntityLivingBase;  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.entity.Entity.setOwner(Lnet.minecraft.entity.EntityLivingBase;)V  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.entity.ai.attributes.AttributeModifier.initShrinkingModifier()V  # Artifacts-1.12.2-1.2.3.jar
  1  net.minecraft.entity.monster.EntityMob.attackEntityAsMobMinimum(Lnet.minecraft.entity.EntityLivingBase;)Z  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.entity.monster.EntityMob.getFeral()Lcom.dhanantry.scapeandrunparasites.entity.ai.misc.EntityPFeral;  # SRPExtra-1.9.8.jar
  1  net.minecraft.entity.monster.EntityMob.isBeingPossessed()Z  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.entity.monster.EntityMob.setBurrowed(Z)V  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.entity.passive.EntityTameable.getAnimation()Lnet.ilexiconn.llibrary.server.animation.Animation;  # lepidodendron-1.12.2-63.02.jar
  1  net.minecraft.entity.passive.EntityVillager$ListEnchantedBookForEmeralds.<init>()V  # moreplanets-mc1.12.2-v2.4.1.jar
  1  net.minecraft.entity.player.EntityPlayer.updateCape()V  # MenuMobs-1.12.2-1.21.jar
  1  net.minecraft.inventory.Container.loadBaubles(Lnet.minecraft.entity.player.EntityPlayer;)V  # IC2Classic-1.12.2-1.5.15.jar
  1  net.minecraft.item.Item.getAmulet(Lnet.minecraft.entity.player.EntityPlayer;)Lnet.minecraft.item.ItemStack;  # Atum-1.12.2-2.0.20.jar
  1  net.minecraft.item.Item.getBauble(Lnet.minecraft.entity.player.EntityPlayer;Ljava.util.List;)V  # DarkUtils-1.12.2-1.8.230.jar
  1  net.minecraft.item.Item.getBaublesInventory(Lnet.minecraft.entity.player.EntityPlayer;)Lnet.minecraft.inventory.IInventory;  # Atum-1.12.2-2.0.20.jar
  1  net.minecraft.item.Item.hasBauble(Lnet.minecraft.entity.player.EntityPlayer;)Z  # DarkUtils-1.12.2-1.8.230.jar
  1  net.minecraft.item.Item.lastMax:Lcom.creativemd.littletiles.common.util.place.PlacementPosition;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.item.Item.min:Lcom.creativemd.littletiles.common.util.place.PlacementPosition;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.item.ItemBow.findDragonBoneArrow(Lnet.minecraft.entity.player.EntityPlayer;)Lnet.minecraft.item.ItemStack;  # spartanfire_rlcraft-1.4.0.jar
  1  net.minecraft.item.ItemBow.isDragonboneArrow(Lnet.minecraft.item.ItemStack;)Z  # spartanfire_rlcraft-1.4.0.jar
  1  net.minecraft.launchwrapper.IClassNameTransformer.unmapClassName(Ljava.lang.String;)Ljava.lang.String;  # !mixinbooter-12.4.jar
  1  net.minecraft.network.NetworkManager.acceptInboundMessage(Ljava.lang.Object;)Z  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelRead(Lio.netty.channel.ChannelHandlerContext;Ljava.lang.Object;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelReadComplete(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelRegistered(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelUnregistered(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.channelWritabilityChanged(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.handlerAdded(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.handlerRemoved(Lio.netty.channel.ChannelHandlerContext;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.isSharable()Z  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.NetworkManager.userEventTriggered(Lio.netty.channel.ChannelHandlerContext;Ljava.lang.Object;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar
  1  net.minecraft.network.play.INetHandlerPlayClient.handleDisconnect(Lnet.minecraft.network.play.server.SPacketDisconnect;)V  # ForgeMultipart-1.12.2-2.6.2.83-universal.jar
  1  net.minecraft.server.integrated.IntegratedServer.startServerThread()V  # Chunk Pregenerator-V1.12-2.5.0.jar
  1  net.minecraft.tileentity.TileEntity.addTileToENet()V  # nuclearcraft-1.12.2-2.19.jar
  1  net.minecraft.tileentity.TileEntity.addToIc2EnergyNetwork()V  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.getEUCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Ljava.lang.Object;  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.getRFCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Ljava.lang.Object;  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.getRSChance(B)D  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.tileentity.TileEntity.getTeslaCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Ljava.lang.Object;  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.hasEUCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Z  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.hasRFCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Z  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.hasTeslaCapability(Lnet.minecraftforge.common.capabilities.Capability;Lnet.minecraft.util.EnumFacing;)Z  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.receiveUpdatePacket(Lnet.minecraft.nbt.NBTTagCompound;)V  # CreativeCore_v1.10.70_mc1.12.2.jar
  1  net.minecraft.tileentity.TileEntity.removeFromIc2EnergyNetwork()V  # omlib-1.12.2-3.1.4-249.jar
  1  net.minecraft.tileentity.TileEntity.removeTileFromENet()V  # nuclearcraft-1.12.2-2.19.jar
  1  net.minecraft.tileentity.TileEntity.transferToRFMachine(Lnet.minecraft.tileentity.TileEntity;ILnet.minecraft.util.EnumFacing;)I  # _NTM-Extended-1.12.2-3.0.3.jar
  1  net.minecraft.util.EnumTypeAdapterFactory.<init>()V  # minecolonies-1.12.2-0.11.841-BETA-universal.jar
  1  net.minecraft.util.IStringSerializable.ADA_SMALL:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar
  1  net.minecraft.util.IStringSerializable.PRI_SMALL:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar
  1  net.minecraft.util.IStringSerializable.PURE_SMALL:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar
  1  net.minecraft.util.IStringSerializable.SIM_SMALL:Lcom.dhanantry.scapeandrunparasites.block.BlockGore$EnumType;  # SRPExtra-1.9.8.jar
  1  net.minecraft.util.ITickable.sendLoad()V  # gendustry-1.6.5.8-mc1.12.2.jar
  1  net.minecraft.util.ITickable.sendUnload()V  # gendustry-1.6.5.8-mc1.12.2.jar
  1  net.minecraft.util.math.AxisAlignedBB.getCornerX(Lnet.minecraft.util.math.AxisAlignedBB;Lcom.creativemd.creativecore.common.utils.math.box.BoxUtils$BoxCorner;)D  # CreativeCore_v1.10.70_mc1.12.2.jar
  1  net.minecraft.util.math.AxisAlignedBB.getCornerY(Lnet.minecraft.util.math.AxisAlignedBB;Lcom.creativemd.creativecore.common.utils.math.box.BoxUtils$BoxCorner;)D  # CreativeCore_v1.10.70_mc1.12.2.jar
  1  net.minecraft.util.math.AxisAlignedBB.getCornerZ(Lnet.minecraft.util.math.AxisAlignedBB;Lcom.creativemd.creativecore.common.utils.math.box.BoxUtils$BoxCorner;)D  # CreativeCore_v1.10.70_mc1.12.2.jar
  1  net.minecraft.util.text.Style$Serializer.<init>()V  # minecolonies-1.12.2-0.11.841-BETA-universal.jar
  1  net.minecraft.world.World.markTileEntitiesInChunkForRemoval(Lnet.minecraft.world.chunk.Chunk;)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.world.biome.Biome.addDefaultFlowers()V  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.world.chunk.Chunk.getBlockLightOpacity(III)I  # AsmodeusCore-1.12.2-1.0.5.jar
  1  net.minecraft.world.chunk.storage.AnvilChunkLoader.loadEntities(Lnet.minecraft.world.World;Lnet.minecraft.nbt.NBTTagCompound;Lnet.minecraft.world.chunk.Chunk;)V  # Chunk Pregenerator-V1.12-2.5.0.jar
  1  net.minecraft.world.chunk.storage.AnvilChunkLoader.writeChunkData(Lnet.minecraft.util.math.ChunkPos;Lnet.minecraft.nbt.NBTTagCompound;)V  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.world.gen.structure.MapGenEndCity.<init>(Lnet.minecraft.world.gen.ChunkGeneratorEnd;)V -> ()V  # stygian-1.0.4.jar
  1  net.minecraft.world.gen.structure.template.Template.addEntitiesToWorld(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.util.Mirror;Lnet.minecraft.util.Rotation;Lnet.minecraft.world.gen.structure.StructureBoundingBox;)V  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraft.world.storage.MapStorage.saveData(Lnet.minecraft.world.storage.WorldSavedData;)V  # ChickenChunks-1.12.2-2.4.2.74-universal.jar
  1  net.minecraft.world.storage.SaveDataMemoryStorage.<init>()V  # blockdrops-1.12.2-1.4.0.jar
  1  net.minecraft.world.storage.WorldSavedData.setCooldown(ILnet.minecraft.world.World;I)V  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.world.storage.WorldSavedData.setEvolutionPhase(IBZLnet.minecraft.world.World;Z)Z  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.world.storage.loot.conditions.EntityHasProperty.properties:[Lnet.minecraft.world.storage.loot.properties.EntityProperty;  # JustEnoughResources-1.12.2-0.9.2.60.jar
  1  net.minecraftforge.common.DungeonHooks$DungeonMob.itemWeight:I  # defiledlands-1.12.2-1.4.3.jar
  1  net.minecraftforge.common.ForgeModContainer.forgeCloudsEnabled:Z  # AsmodeusCore-1.12.2-1.0.5.jar
  1  net.minecraftforge.common.IMinecartCollisionHandler.getBoundingBox(Lnet.minecraft.entity.item.EntityMinecart;)Lnet.minecraft.util.math.AxisAlignedBB;  # terracartreloaded-1.12.2-1.2.3.jar
  1  net.minecraftforge.common.IMinecartCollisionHandler.getCollisionBox(Lnet.minecraft.entity.item.EntityMinecart;Lnet.minecraft.entity.Entity;)Lnet.minecraft.util.math.AxisAlignedBB;  # terracartreloaded-1.12.2-1.2.3.jar
  1  net.minecraftforge.common.IMinecartCollisionHandler.getMinecartCollisionBox(Lnet.minecraft.entity.item.EntityMinecart;)Lnet.minecraft.util.math.AxisAlignedBB;  # terracartreloaded-1.12.2-1.2.3.jar
  1  net.minecraftforge.common.IMinecartCollisionHandler.onEntityCollision(Lnet.minecraft.entity.item.EntityMinecart;Lnet.minecraft.entity.Entity;)V  # terracartreloaded-1.12.2-1.2.3.jar
  1  net.minecraftforge.common.capabilities.ICapabilityProvider.getMonitorableAccessor_Internal(Lcom.kotori316.fluidtank.tiles.Connection;Lnet.minecraftforge.common.capabilities.Capability;)Ljava.lang.Object;  # LargeFluidTank-12.5.3.jar
  1  net.minecraftforge.common.capabilities.ICapabilityProvider.isMonitorableAccessor_Internal(Lnet.minecraftforge.common.capabilities.Capability;)Z  # LargeFluidTank-12.5.3.jar
  1  net.minecraftforge.common.config.Configuration$UnicodeInputStreamReader.<init>(Ljava.io.InputStream;Ljava.lang.String;)V  # bettercaves-1.12.2-2.0.4.jar
  1  net.minecraftforge.common.config.IFieldWrapper.getCategory()Ljava.lang.String;  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraftforge.common.config.IFieldWrapper.getKeys()[Ljava.lang.String;  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraftforge.common.config.IFieldWrapper.getValue(Ljava.lang.String;)Ljava.lang.Object;  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraftforge.common.config.IFieldWrapper.handlesKey(Ljava.lang.String;)Z  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraftforge.common.config.IFieldWrapper.hasKey(Ljava.lang.String;)Z  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraftforge.common.config.IFieldWrapper.setValue(Ljava.lang.String;Ljava.lang.Object;)V  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraftforge.common.config.IFieldWrapper.setupConfiguration(Lnet.minecraftforge.common.config.Configuration;Ljava.lang.String;Ljava.lang.String;ZZZ)V  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraftforge.common.util.CompoundDataFixer.<init>(Lnet.minecraft.util.datafix.DataFixer;)V -> ()V  # Treasure2-mc1.12.2-f14.23.5.2859-v2.1.1.jar
  1  net.minecraftforge.common.util.EnumHelper.addStatus(Ljava.lang.String;)Lnet.minecraft.entity.player.EntityPlayer$SleepResult;  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraftforge.event.ForgeEventFactory.onEnchantmentLevelSet(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;IILnet.minecraft.item.ItemStack;I)I  # ImprovableSkills-1.12.2-12.4.68.jar
  1  net.minecraftforge.event.ForgeEventFactory.onPlayerDrops(Lnet.minecraft.entity.player.EntityPlayer;Lnet.minecraft.util.DamageSource;Ljava.util.List;Z)V  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraftforge.event.RegistryEvent$Register.setModContainer(Lnet.minecraftforge.fml.common.ModContainer;)V  # HammerLib-1.12.2-12.2.66.jar
  1  net.minecraftforge.event.world.BlockEvent$PlaceEvent.getItemInHand()Lnet.minecraft.item.ItemStack;  # fxcontrol-1.12-0.1.15.jar
  1  net.minecraftforge.fluids.Fluid.<init>(Ljava.lang.String;Lnet.minecraft.util.ResourceLocation;Lnet.minecraft.util.ResourceLocation;Lnet.minecraft.util.ResourceLocation;Ljava.awt.Color;)V -> (Ljava.lang.String;Lnet.minecraft.util.ResourceLocation;Lnet.minecraft.util.ResourceLocation;Ljava.awt.Color;)V  # hbm-1.8.4a-G.jar
  1  net.minecraftforge.fluids.capability.IFluidHandler.getFacingGasTank()Lmekanism.api.gas.IGasHandler;  # ExtraCells-1.12.2-2.6.7.jar
  1  net.minecraftforge.fluids.capability.TileFluidHandler.access$000(Lrustic.common.tileentity.TileEntityCondenser;)Lnet.minecraft.util.math.BlockPos;  # rustic-1.2.0.jar
  1  net.minecraftforge.fluids.capability.TileFluidHandler.access$100(Lrustic.common.tileentity.TileEntityCondenser;)Lnet.minecraft.world.World;  # rustic-1.2.0.jar
  1  net.minecraftforge.fluids.capability.TileFluidHandler.access$200(Lrustic.common.tileentity.TileEntityCondenser;)Lnet.minecraft.util.math.BlockPos;  # rustic-1.2.0.jar
  1  net.minecraftforge.fluids.capability.TileFluidHandler.access$300(Lrustic.common.tileentity.TileEntityCondenser;)Lnet.minecraft.world.World;  # rustic-1.2.0.jar
  1  net.minecraftforge.fluids.capability.TileFluidHandler.access$400(Lrustic.common.tileentity.TileEntityCondenser;)Lnet.minecraft.util.math.BlockPos;  # rustic-1.2.0.jar
  1  net.minecraftforge.fluids.capability.TileFluidHandler.access$500(Lrustic.common.tileentity.TileEntityCondenser;)Lnet.minecraft.util.math.BlockPos;  # rustic-1.2.0.jar
  1  net.minecraftforge.fluids.capability.TileFluidHandler.access$600(Lrustic.common.tileentity.TileEntityCondenser;)Lnet.minecraft.world.World;  # rustic-1.2.0.jar
  1  net.minecraftforge.fluids.capability.templates.VoidFluidHandler.<init>()V  # IC2Classic-1.12.2-1.5.15.jar
  1  net.minecraftforge.fml.common.CertificateHelper.getFingerprint(Ljava.security.cert.Certificate;)Ljava.lang.String;  # eternalwinter-1.0.3.jar
  1  net.minecraftforge.fml.common.FMLCommonHandler.processWindowMessages()V  # Cloudflared-1.12.2-12.2.3.jar
  1  net.minecraftforge.fml.common.MissingModsException.<init>(Ljava.lang.String;Ljava.lang.String;)V  # VideoPlayer-2.6-FORGE-1.12.2.jar
  1  net.minecraftforge.fml.common.MissingModsException.addMissingMod(Lnet.minecraftforge.fml.common.versioning.ArtifactVersion;Lnet.minecraftforge.fml.common.versioning.ArtifactVersion;Z)V  # VideoPlayer-2.6-FORGE-1.12.2.jar
  1  net.minecraftforge.fml.common.ModAPIManager.injectAPIModContainers(Ljava.util.List;Ljava.util.Map;)V  # bdlib-1.14.3.12-mc1.12.2.jar
  1  net.minecraftforge.fml.common.ModMetadata.requiredMods:Ljava.util.Set;  # allarrowsinfinityfix-1.8-forge-mc1.12.2.jar
  1  net.minecraftforge.fml.common.event.FMLInterModComms$IMCEvent.applyModContainer(Lnet.minecraftforge.fml.common.ModContainer;)V  # Tropicraft-7.1.9.jar
  1  net.minecraftforge.fml.common.event.FMLModIdMappingEvent$ModRemapping.key:Lnet.minecraft.util.ResourceLocation;  # RoughlyEnoughIDs-2.3.1.jar
  1  net.minecraftforge.fml.common.event.FMLModIdMappingEvent$ModRemapping.newId:I  # RoughlyEnoughIDs-2.3.1.jar
  1  net.minecraftforge.fml.common.event.FMLModIdMappingEvent.getRemaps(Lnet.minecraft.util.ResourceLocation;)Lcom.google.common.collect.ImmutableList;  # RoughlyEnoughIDs-2.3.1.jar
  1  net.minecraftforge.fml.common.eventhandler.ASMEventHandler.getPriority()Lnet.minecraftforge.fml.common.eventhandler.EventPriority;  # OpenBlocks-1.12.2-1.8.1.jar
  1  net.minecraftforge.fml.common.eventhandler.EventBus.busID:I  # 永爱之刃-v11削弱.jar
  1  net.minecraftforge.fml.common.eventhandler.EventBus.exceptionHandler:Lnet.minecraftforge.fml.common.eventhandler.IEventExceptionHandler;  # 永爱之刃-v11削弱.jar
  1  net.minecraftforge.fml.common.eventhandler.EventBus.shutdown:Z  # 永爱之刃-v11削弱.jar
  1  net.minecraftforge.fml.common.eventhandler.IEventExceptionHandler.handleException(Lnet.minecraftforge.fml.common.eventhandler.EventBus;Lnet.minecraftforge.fml.common.eventhandler.Event;[Lnet.minecraftforge.fml.common.eventhandler.IEventListener;ILjava.lang.Throwable;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraftforge.fml.common.eventhandler.ListenerList.register(ILnet.minecraftforge.fml.common.eventhandler.EventPriority;Lnet.minecraftforge.fml.common.eventhandler.IEventListener;)V  # future-mc-0.2.11.jar
  1  net.minecraftforge.fml.common.network.FMLEventChannel.sendToAllTracking(Lnet.minecraftforge.fml.common.network.internal.FMLProxyPacket;Lnet.minecraft.entity.Entity;)V  # HammerLib-1.12.2-12.2.66.jar
  1  net.minecraftforge.fml.common.network.FMLEventChannel.sendToAllTracking(Lnet.minecraftforge.fml.common.network.internal.FMLProxyPacket;Lnet.minecraftforge.fml.common.network.NetworkRegistry$TargetPoint;)V  # HammerLib-1.12.2-12.2.66.jar
  1  net.minecraftforge.fml.common.network.FMLNetworkEvent$CustomPacketRegistrationEvent.getOperation()Ljava.lang.String;  # modularwarfare-shining-2024.2.4.6f-fix1.jar
  1  net.minecraftforge.fml.common.network.FMLNetworkEvent$CustomPacketRegistrationEvent.getRegistrations()Lcom.google.common.collect.ImmutableSet;  # modularwarfare-shining-2024.2.4.6f-fix1.jar
  1  net.minecraftforge.fml.common.network.NetworkRegistry.channels:Ljava.util.EnumMap;  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraftforge.fml.common.network.PacketLoggingHandler.<init>()V  # [1.12.2]-Epic-Knights-Armor-and-Weapons-6.2v(uncomplete).jar
  1  net.minecraftforge.fml.common.network.handshake.NetworkDispatcher.get(Lnet.minecraft.network.NetworkManager;)Lnet.minecraftforge.fml.common.network.handshake.NetworkDispatcher;  # LibVulpes-1.12.2-0.4.2-75-universal.jar
  1  net.minecraftforge.fml.common.network.internal.FMLProxyPacket.<init>(Lnet.minecraft.network.play.server.SPacketCustomPayload;)V -> (Lnet.minecraft.network.PacketBuffer;Ljava.lang.String;)V  # industrialcraft-2-2.8.222-ex112.jar
  1  net.minecraftforge.fml.common.network.internal.FMLProxyPacket.toC17Packet()Lnet.minecraft.network.Packet;  # architecturecraft-1.12-3.98.jar
  1  net.minecraftforge.fml.common.network.internal.FMLProxyPacket.toS3FPackets()Ljava.util.List;  # architecturecraft-1.12-3.98.jar
  1  net.minecraftforge.fml.common.network.internal.NetworkModHolder$NetworkChecker.<init>(Lnet.minecraftforge.fml.common.network.internal.NetworkModHolder;)V  # Pixelmon-1.12.2-8.4.3-universal.jar
  1  net.minecraftforge.fml.common.network.simpleimpl.IMessage.getEffectedBlocks(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;I)Ljava.util.List;  # SpeedsterHeroes-1.12.2-2.2.1.jar
  1  net.minecraftforge.fml.relauncher.FMLInjectionData.containers:Ljava.util.List;  # !mixinbooter-12.4.jar
  1  net.minecraftforge.registries.GameData.getEntityClassMap()Ljava.util.Map;  # DoggyTalents-1.12.2-1.15.1.6.jar
  1  net.minecraftforge.registries.GameData.getWrapper(Ljava.lang.Class;)Lnet.minecraft.util.registry.RegistryNamespaced;  # JustEnoughResources-1.12.2-0.9.2.60.jar
  1  net.minecraftforge.server.permission.context.WorldContext.<init>(Lnet.minecraft.world.World;)V  # FTBLib-5.4.7.2.jar
```

## client-side API (top 393 of 393)

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
  2  net.minecraft.client.audio.SoundManager.playingSounds:Ljava.util.Map;  # LoTAS1.12.2-2.1.3.jar, UniversalModCore-1.12.2-forge-1.3.1.jar
  2  net.minecraft.client.entity.EntityPlayerSP.sprintingTicksLeft:I  # Galacticraft-1.12.2-4.0.6.jar, walljump-1.12.2-1.3.2.jar
  2  net.minecraft.client.gui.GuiIngameMenu.<init>()V  # LoTAS1.12.2-2.1.3.jar, pokelucky-RG-6.16.14(1.12.2).jar
  2  net.minecraft.client.gui.GuiNewChat.chatLines:Ljava.util.List;  # HammerLib-1.12.2-12.2.66.jar, HammerLib-1.12.2-2.0.6.32.jar
  2  net.minecraft.client.gui.GuiScreenOptionsSounds.<init>(Lnet.minecraft.client.gui.GuiScreen;Lnet.minecraft.client.settings.GameSettings;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar, IC2Classic-1.12.2-1.5.15.jar
  2  net.minecraft.client.gui.inventory.GuiContainer.drawJEIHighlights()V  # OpenComputers-MC1.12.2-1.7.5.192.jar, OpenComputers-MC1.12.2-1.8.10+258943363.jar
  2  net.minecraft.client.gui.inventory.GuiContainer.resetJEIHighlights()V  # OpenComputers-MC1.12.2-1.7.5.192.jar, OpenComputers-MC1.12.2-1.8.10+258943363.jar
  2  net.minecraft.client.model.ModelShulkerBullet.<init>()V  # champions-1.12.2-1.0.11.10.jar, srpcotesia-1.12.2-1.3.4h1.jar
  2  net.minecraft.client.network.NetHandlerPlayClient.handleCustomSound(Lnet.minecraft.network.play.server.SPacketCustomSound;)V  # HammerLib-1.12.2-12.2.66.jar, HammerLib-1.12.2-2.0.6.32.jar
  2  net.minecraft.client.renderer.ActiveRenderInfo.updateRenderInfo(Lnet.minecraft.entity.Entity;Z)V  # JurassicReborn-1.12.2-1.3.4.jar, 永爱之刃-v11削弱.jar
  2  net.minecraft.client.renderer.BufferBuilder.putColorRGBA(IIII)V  # Call to Battle 3.jar, SilentLib-1.12.2-3.0.14+168.jar
  2  net.minecraft.client.renderer.EntityRenderer.cameraPitch:D  # Galacticraft-1.12.2-4.0.6.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.EntityRenderer.cameraYaw:D  # Galacticraft-1.12.2-4.0.6.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.EntityRenderer.cameraZoom:D  # Galacticraft-1.12.2-4.0.6.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.EntityRenderer.farPlaneDistance:F  # Modern-Warfare-Cubed-0.1.9.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.EntityRenderer.getFOVModifier(FZ)F  # Modern-Warfare-Cubed-0.1.9.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.RegionRenderCacheBuilder.getWorldRendererByLayer(Lnet.minecraft.util.BlockRenderLayer;)Lnet.minecraft.client.renderer.BufferBuilder;  # LittleTiles_v1.5.58_mc1.12.2.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.RenderGlobal.cloudTickCounter:I  # iChunUtil-1.12.2-7.2.2.jar, rftoolsdim-1.12-5.71.jar
  2  net.minecraft.client.renderer.RenderGlobal.hasNoChunkUpdates()Z  # JurassicReborn-1.12.2-1.3.4.jar, bbs-2.5-1.12.2.jar
  2  net.minecraft.client.renderer.RenderGlobal.lastViewEntityYaw:D  # PortalGun-1.12.2-7.1.0.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.RenderGlobal.onEntityRemoved(Lnet.minecraft.entity.Entity;)V  # DramaticTrees-1.12.2-0.13.6.jar, 永爱之刃-v11削弱.jar
  2  net.minecraft.client.renderer.RenderGlobal.renderContainer:Lnet.minecraft.client.renderer.ChunkRenderContainer;  # [1.12.2] SecurityCraft v1.10.1.jar, iChunUtil-1.12.2-7.2.2.jar
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
  2  net.minecraft.client.renderer.chunk.VisGraph.setOpaqueCube(Lnet.minecraft.util.math.BlockPos;)V  # SCP Lockdown-1.12.2-2.4.3-release.jar, iChunUtil-1.12.2-7.2.2.jar
  2  net.minecraft.client.renderer.entity.RenderDragon.ENDERCRYSTAL_BEAM_TEXTURES:Lnet.minecraft.util.ResourceLocation;  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar, moreplanets-mc1.12.2-v2.4.1.jar
  2  net.minecraft.client.renderer.entity.RenderFireball.<init>(Lnet.minecraft.client.renderer.entity.RenderManager;F)V  # Fish's Undead Rising-1.4.2.jar, Instrumental-Mobs-1.2.1.jar
  2  net.minecraft.client.renderer.entity.layers.LayerVillagerArmor.<init>(Lnet.minecraft.client.renderer.entity.RenderLivingBase;)V  # VillageNames-1.12.2-4.1.5.jar, VillageNames-1.12.2-4.4.13.jar
  2  net.minecraft.client.tutorial.Tutorial.<init>(Lnet.minecraft.client.Minecraft;)V  # ElenaiDodge2-1.12.2-1.0.8b.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  2  net.minecraft.util.MovementInputFromOptions.<init>(Lnet.minecraft.client.settings.GameSettings;)V  # Chisel-MC1.12.2-1.0.2.45.jar, MenuMobs-1.12.2-1.21.jar
  2  net.minecraftforge.client.ForgeHooksClient.putQuadColor(Lnet.minecraft.client.renderer.BufferBuilder;Lnet.minecraft.client.renderer.block.model.BakedQuad;I)V  # ModularPowersuits-1.12.2-1.0.46.jar, Trinkets and Baubles-Forge-1.12.2-0.33.4.jar
  2  net.minecraftforge.client.ForgeHooksClient.renderLitItem(Lnet.minecraft.client.renderer.RenderItem;Lnet.minecraft.client.renderer.block.model.IBakedModel;ILnet.minecraft.item.ItemStack;)V  # GalaxySpace-1.12.2-2.2.0.jar, ThaumicAdditions-1.12.2-12.7.8.jar
  2  net.minecraftforge.common.model.Models.getHiddenModelPart(Lcom.google.common.collect.ImmutableList;)Lnet.minecraftforge.common.model.IModelPart;  # HeroesExpansion-1.12.2-1.3.5.jar, Numina-1.12.2-1.0.38.jar
  2  net.minecraftforge.fml.client.FMLClientHandler.addModAsResource(Lnet.minecraftforge.fml.common.ModContainer;)V  # modularwarfare-2.4.2f.jar, modularwarfare-shining-2024.2.4.6f-fix1.jar
  2  net.minecraftforge.fml.client.FMLClientHandler.getClientPlayHandler()Lnet.minecraft.network.INetHandler;  # CoFHCore-1.12.2-4.6.6.1-universal.jar, Tropicraft-7.1.9.jar
  2  net.minecraftforge.fml.client.FMLConfigGuiFactory.<init>()V  # Login_Shield-1.12.2-6-g5654706.jar, chiselsandbits-14.33.jar
  2  org.lwjgl.openal.AL.getDevice()Lorg.lwjgl.openal.ALCdevice;  # PymTech-1.12.2-1.0.2.jar, themidnight-0.3.5.jar
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
  1  net.minecraft.client.audio.SoundManager.loaded:Z  # LoTAS1.12.2-2.1.3.jar
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
  1  net.minecraft.client.gui.GuiOverlayDebug.renderDebugInfo(Lnet.minecraft.client.gui.ScaledResolution;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiScreen.handleComponentHover(Lnet.minecraft.util.text.ITextComponent;II)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.GuiScreen.inputField:Lnet.minecraft.client.gui.GuiTextField;  # brigo_forge-1.1.1+1.12.x.jar
  1  net.minecraft.client.gui.GuiShareToLan.<init>(Lnet.minecraft.client.gui.GuiScreen;)V  # pokelucky-RG-6.16.14(1.12.2).jar
  1  net.minecraft.client.gui.GuiShareToLan.allowCheatsButton:Lnet.minecraft.client.gui.GuiButton;  # nolancheating-1.12.2-v1.0.jar
  1  net.minecraft.client.gui.GuiShareToLan.gameModeButton:Lnet.minecraft.client.gui.GuiButton;  # nolancheating-1.12.2-v1.0.jar
  1  net.minecraft.client.gui.GuiSnooper.<init>(Lnet.minecraft.client.gui.GuiScreen;Lnet.minecraft.client.settings.GameSettings;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar
  1  net.minecraft.client.gui.ScreenChatOptions.<init>(Lnet.minecraft.client.gui.GuiScreen;Lnet.minecraft.client.settings.GameSettings;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar
  1  net.minecraft.client.gui.ScreenChatOptions.updateNarratorButton()V  # randompatches-1.12.2-1.22.1.10.jar
  1  net.minecraft.client.gui.chat.NormalChatListener.<init>(Lnet.minecraft.client.Minecraft;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.chat.OverlayChatListener.<init>(Lnet.minecraft.client.Minecraft;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.gui.inventory.GuiContainerCreative$CreativeSlot.slot:Lnet.minecraft.inventory.Slot;  # InventoryTweaks-1.63.jar
  1  net.minecraft.client.gui.inventory.GuiCrafting.recipeButton:Lnet.minecraft.client.gui.GuiButtonImage;  # FastWorkbench-1.12.2-1.7.3.jar
  1  net.minecraft.client.model.ModelArmorStandArmor.<init>(F)V  # Cherry_on-1.12.2-1.3.35.jar
  1  net.minecraft.client.model.ModelDragon.head:Lnet.minecraft.client.model.ModelRenderer;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.model.ModelEvokerFangs.<init>()V  # Fish's Undead Rising-1.4.2.jar
  1  net.minecraft.client.model.ModelGuardian.guardianBody:Lnet.minecraft.client.model.ModelRenderer;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.model.ModelHumanoidHead.<init>()V  # p455w0rdslib-1.12.2-2.3.161.jar
  1  net.minecraft.client.model.ModelHumanoidHead.head:Lnet.minecraft.client.model.ModelRenderer;  # CustomPlayerModels-1.12.2-0.6.27a.jar
  1  net.minecraft.client.model.ModelMagmaCube.core:Lnet.minecraft.client.model.ModelRenderer;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.multiplayer.PlayerControllerMP.isSpectatorMode()Z  # BetterSprinting-1.12.2-v3.0.1.jar
  1  net.minecraft.client.multiplayer.WorldClient.connection:Lnet.minecraft.client.network.NetHandlerPlayClient;  # UniversalModCore-1.12.2-forge-1.3.1.jar
  1  net.minecraft.client.network.NetHandlerPlayClient.access$000(Lnet.minecraft.client.network.NetHandlerPlayClient;)Lnet.minecraft.client.Minecraft;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.network.NetHandlerPlayClient.access$002(Lnet.minecraft.client.network.NetHandlerPlayClient;Lnet.minecraft.client.Minecraft;)Lnet.minecraft.client.Minecraft;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.network.NetHandlerPlayClient.access$100(Lnet.minecraft.client.network.NetHandlerPlayClient;)Lnet.minecraft.network.NetworkManager;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.network.NetHandlerPlayClient.access$200(Lnet.minecraft.client.network.NetHandlerPlayClient;)Lcom.google.common.util.concurrent.FutureCallback;  # 永爱之刃-v11削弱.jar
  1  net.minecraft.client.network.NetHandlerPlayClient.handleRespawn(Lnet.minecraft.network.play.server.SPacketRespawn;)V  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.client.particle.ParticleFallingDust.<init>(Lnet.minecraft.world.World;DDDFFF)V  # srpcotesia-1.12.2-1.3.4h1.jar
  1  net.minecraft.client.particle.ParticleFootStep.<init>(Lnet.minecraft.client.renderer.texture.TextureManager;Lnet.minecraft.world.World;DDD)V  # Modern-Warfare-Cubed-0.1.9.jar
  1  net.minecraft.client.renderer.BufferBuilder.quadSprite:Lnet.minecraft.client.renderer.texture.TextureAtlasSprite;  # NTM-CE-1.12.2-2.6.1.0.jar
  1  net.minecraft.client.renderer.BufferBuilder.quadSprites:[Lnet.minecraft.client.renderer.texture.TextureAtlasSprite;  # NTM-CE-1.12.2-2.6.1.0.jar
  1  net.minecraft.client.renderer.ChunkRenderContainer.addRenderChunk(Lnet.minecraft.client.renderer.chunk.RenderChunk;Lnet.minecraft.util.BlockRenderLayer;)V  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.ChunkRenderContainer.renderChunkLayer(Lnet.minecraft.util.BlockRenderLayer;)V  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.EntityRenderer.applyBobbing(F)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.cloudFog:Z  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.debugView:Z  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.debugViewDirection:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.hurtCameraEffect(F)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.orientCamera(F)V  # Galacticraft-1.12.2-4.0.6.jar
  1  net.minecraft.client.renderer.EntityRenderer.renderCloudsCheck(Lnet.minecraft.client.renderer.RenderGlobal;FIDDD)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.renderRainSnow(F)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.setupFog(IF)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.thirdPersonDistancePrev:F  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.EntityRenderer.torchFlickerX:F  # MenuMobs-1.12.2-1.21.jar
  1  net.minecraft.client.renderer.EntityRenderer.updateFogColor(F)V  # PortalGun-1.12.2-7.1.0.jar
  1  net.minecraft.client.renderer.EntityRenderer.updateShaderGroupSize(II)V  # bbs-fs-2.8-1.12.2-alpha.1.jar
  1  net.minecraft.client.renderer.GlStateManager.rotate(DFFF)V  # NTM-CE-1.12.2-2.6.1.0.jar
  1  net.minecraft.client.renderer.ItemRenderer.itemRenderer:Lnet.minecraft.client.renderer.RenderItem;  # IndustrialUpgrade-1.12.2-3.3.0.43.jar
  1  net.minecraft.client.renderer.RegionRenderCacheBuilder.getWorldRendererByLayerId(I)Lnet.minecraft.client.renderer.BufferBuilder;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal$ContainerLocalRenderInformation.<init>(Lnet.minecraft.client.renderer.RenderGlobal;Lnet.minecraft.client.renderer.chunk.RenderChunk;Lnet.minecraft.util.EnumFacing;I)V  # [1.12.2] SecurityCraft v1.10.1.jar
  1  net.minecraft.client.renderer.RenderGlobal.addChainedFilledBoxVertices(Lnet.minecraft.client.renderer.BufferBuilder;DDDDDDFFFF)V  # rtsbuilding-1.1.6-1.12.2-port.jar
  1  net.minecraft.client.renderer.RenderGlobal.broadcastSound(ILnet.minecraft.util.math.BlockPos;I)V  # DramaticTrees-1.12.2-0.13.6.jar
  1  net.minecraft.client.renderer.RenderGlobal.countEntitiesHidden:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.countEntitiesRendered:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.countEntitiesTotal:I  # iChunUtil-1.12.2-7.2.2.jar
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
  1  net.minecraft.client.renderer.RenderGlobal.onEntityAdded(Lnet.minecraft.entity.Entity;)V  # DramaticTrees-1.12.2-0.13.6.jar
  1  net.minecraft.client.renderer.RenderGlobal.playEvent(Lnet.minecraft.entity.player.EntityPlayer;ILnet.minecraft.util.math.BlockPos;I)V  # DramaticTrees-1.12.2-0.13.6.jar
  1  net.minecraft.client.renderer.RenderGlobal.playRecord(Lnet.minecraft.util.SoundEvent;Lnet.minecraft.util.math.BlockPos;)V  # DramaticTrees-1.12.2-0.13.6.jar
  1  net.minecraft.client.renderer.RenderGlobal.playSoundToAllNearExcept(Lnet.minecraft.entity.player.EntityPlayer;Lnet.minecraft.util.SoundEvent;Lnet.minecraft.util.SoundCategory;DDDFF)V  # DramaticTrees-1.12.2-0.13.6.jar
  1  net.minecraft.client.renderer.RenderGlobal.postRenderDamagedBlocks()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.preRenderDamagedBlocks()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderChunkFactory:Lnet.minecraft.client.renderer.chunk.IRenderChunkFactory;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderDispatcher:Lnet.minecraft.client.renderer.chunk.ChunkRenderDispatcher;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderDistanceChunks:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderEntitiesStartupCounter:I  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.renderManager:Lnet.minecraft.client.renderer.entity.RenderManager;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.spawnParticle(IZDDDDDD[I)V  # DramaticTrees-1.12.2-0.13.6.jar
  1  net.minecraft.client.renderer.RenderGlobal.spawnParticle(IZZDDDDDD[I)V  # DramaticTrees-1.12.2-0.13.6.jar
  1  net.minecraft.client.renderer.RenderGlobal.spawnParticle0(IZDDDDDD[I)Lnet.minecraft.client.particle.Particle;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.stopChunkUpdates()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.updateClouds()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.updateDestroyBlockIcons()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.RenderGlobal.vboEnabled:Z  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.ViewFrustum.deleteGlResources()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.ViewFrustum.getRenderChunk(Lnet.minecraft.util.math.BlockPos;)Lnet.minecraft.client.renderer.chunk.RenderChunk;  # NTM-CE-1.12.2-2.6.1.0.jar
  1  net.minecraft.client.renderer.ViewFrustum.markBlocksForUpdate(IIIIIIZ)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.ViewFrustum.renderChunks:[Lnet.minecraft.client.renderer.chunk.RenderChunk;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.ViewFrustum.updateChunkPositions(DD)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.block.model.ModelManager.modelRegistry:Lnet.minecraft.util.registry.IRegistry;  # IndustrialUpgrade-1.12.2-3.3.0.43.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status.DONE:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status.PENDING:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status.UPLOADING:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Status;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type.RESORT_TRANSPARENCY:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.<init>(Lnet.minecraft.client.renderer.chunk.RenderChunk;Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type;D)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.finish()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getCompiledChunk()Lnet.minecraft.client.renderer.chunk.CompiledChunk;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getLock()Ljava.util.concurrent.locks.ReentrantLock;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.getType()Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator$Type;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.isFinished()Z  # [1.12.2] SecurityCraft v1.10.1.jar
  1  net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator.setCompiledChunk(Lnet.minecraft.client.renderer.chunk.CompiledChunk;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.getDebugInfo()Ljava.lang.String;  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.hasNoChunkUpdates()Z  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.hasNoFreeRenderBuilders()Z  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.stopChunkUpdates()V  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.stopWorkerThreads()V  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.updateChunkLater(Lnet.minecraft.client.renderer.chunk.RenderChunk;)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.updateTransparencyLater(Lnet.minecraft.client.renderer.chunk.RenderChunk;)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.<init>()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.addTileEntity(Lnet.minecraft.tileentity.TileEntity;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.getState()Lnet.minecraft.client.renderer.BufferBuilder$State;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.isEmpty()Z  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.setLayerStarted(Lnet.minecraft.util.BlockRenderLayer;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.setLayerUsed(Lnet.minecraft.util.BlockRenderLayer;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.setState(Lnet.minecraft.client.renderer.BufferBuilder$State;)V  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.CompiledChunk.setVisibility(Lnet.minecraft.client.renderer.chunk.SetVisibility;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.IRenderChunkFactory.create(Lnet.minecraft.world.World;Lnet.minecraft.client.renderer.RenderGlobal;I)Lnet.minecraft.client.renderer.chunk.RenderChunk;  # SCP Lockdown-1.12.2-2.4.3-release.jar
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
  1  net.minecraft.client.renderer.chunk.RenderChunk.compileTask:Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;  # [1.12.2] SecurityCraft v1.10.1.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.createRegionRenderCache(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.util.math.BlockPos;I)Lnet.minecraft.world.ChunkCache;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.finishCompileTask()V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.getBlockPosOffset16(Lnet.minecraft.util.EnumFacing;)Lnet.minecraft.util.math.BlockPos;  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.getDistanceSq()D  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.getLockCompileTask()Ljava.util.concurrent.locks.ReentrantLock;  # LittleTiles_v1.5.58_mc1.12.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.lockCompileTask:Ljava.util.concurrent.locks.ReentrantLock;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.makeCompileTaskChunk()Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.makeCompileTaskTransparency()Lnet.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.needsImmediateUpdate()Z  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.position:Lnet.minecraft.util.math.BlockPos$MutableBlockPos;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.postRenderBlocks(Lnet.minecraft.util.BlockRenderLayer;FFFLnet.minecraft.client.renderer.BufferBuilder;Lnet.minecraft.client.renderer.chunk.CompiledChunk;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.preRenderBlocks(Lnet.minecraft.client.renderer.BufferBuilder;Lnet.minecraft.util.math.BlockPos;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.renderGlobal:Lnet.minecraft.client.renderer.RenderGlobal;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.setFrameIndex(I)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.setTileEntities:Ljava.util.Set;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.world:Lnet.minecraft.world.World;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.RenderChunk.worldView:Lnet.minecraft.world.ChunkCache;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.VboChunkFactory.<init>()V  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.chunk.VisGraph.computeVisibility()Lnet.minecraft.client.renderer.chunk.SetVisibility;  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraft.client.renderer.chunk.VisGraph.getVisibleFacings(Lnet.minecraft.util.math.BlockPos;)Ljava.util.Set;  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraft.client.renderer.entity.RenderBat.<init>(Lnet.minecraft.client.renderer.entity.RenderManager;)V  # NarutoMod-1.12.2-0.3.1.beta.jar
  1  net.minecraft.client.renderer.entity.RenderDragon.renderCrystalBeams(DDDFDDDIDDD)V  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.client.renderer.entity.RenderFireball.doRender(Lnet.minecraft.entity.projectile.EntityFireball;DDDFF)V  # Instrumental-Mobs-1.2.1.jar
  1  net.minecraft.client.renderer.entity.RenderLiving.TEXTURE3:Lnet.minecraft.util.ResourceLocation;  # SRPExtra-1.9.8.jar
  1  net.minecraft.client.resources.DefaultPlayerSkin.TEXTURE_ALEX:Lnet.minecraft.util.ResourceLocation;  # _supermartijn642corelib-1.1.26b-forge-mc1.12.jar
  1  net.minecraft.client.resources.DefaultPlayerSkin.TEXTURE_STEVE:Lnet.minecraft.util.ResourceLocation;  # _supermartijn642corelib-1.1.26b-forge-mc1.12.jar
  1  net.minecraft.client.resources.DefaultPlayerSkin.isSlimSkin(Ljava.util.UUID;)Z  # _supermartijn642corelib-1.1.26b-forge-mc1.12.jar
  1  net.minecraft.client.resources.DefaultResourcePack.getInputStreamAssets(Lnet.minecraft.util.ResourceLocation;)Ljava.io.InputStream;  # randompatches-1.12.2-1.22.1.10.jar
  1  net.minecraft.client.resources.FolderResourcePack.getFile(Ljava.lang.String;)Ljava.io.File;  # UniversalModCore-1.12.2-forge-1.3.1.jar
  1  net.minecraft.client.resources.FolderResourcePack.validatePath(Ljava.io.File;Ljava.lang.String;)Z  # industrialcraft-2-2.8.222-ex112.jar
  1  net.minecraft.client.resources.ResourcePackRepository$Entry.reResourcePack:Lnet.minecraft.client.resources.IResourcePack;  # fusion-1.3.16-forge-mc1.12.jar
  1  net.minecraft.client.resources.ResourcePackRepository.getDownloadHeaders()Ljava.util.Map;  # journeymap-1.12.2-5.7.1.jar
  1  net.minecraft.client.resources.data.BaseMetadataSectionSerializer.<init>()V  # Chocolate_Quest_Repoured-1.12.2-2.6.15B.jar
  1  net.minecraft.client.settings.GameSettings.narrator:I  # Call to Battle 3.jar
  1  net.minecraft.client.settings.GameSettings.sendSettingsToServer()V  # CustomPlayerModels-1.12.2-0.6.27a.jar
  1  net.minecraft.client.settings.KeyBinding.pressed:Z  # NTM-CE-1.12.2-2.6.1.0.jar
  1  net.minecraft.client.tutorial.Tutorial.createKeybindComponent(Ljava.lang.String;)Lnet.minecraft.util.text.ITextComponent;  # ElenaiDodge2-1.12.2-1.0.8b.jar
  1  net.minecraft.client.tutorial.Tutorial.getMinecraft()Lnet.minecraft.client.Minecraft;  # ElenaiDodge2-1.12.2-1.0.8b.jar
  1  net.minecraft.client.tutorial.Tutorial.reload()V  # modularwarfare-shining-2024.2.4.6f-fix1.jar
  1  net.minecraft.client.tutorial.Tutorial.stop()V  # Call to Battle 3.jar
  1  net.minecraft.entity.monster.EntityGuardian.clientSideSpikesAnimation:F  # moreplanets-mc1.12.2-v2.4.1.jar
  1  net.minecraft.entity.monster.EntityGuardian.clientSideSpikesAnimationO:F  # moreplanets-mc1.12.2-v2.4.1.jar
  1  net.minecraft.entity.player.EntityPlayer.displayGuiCommandBlock(Lnet.minecraft.tileentity.TileEntityCommandBlock;)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.entity.player.EntityPlayer.displayGuiEditCommandCart(Lnet.minecraft.tileentity.CommandBlockBaseLogic;)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.entity.player.EntityPlayer.openGuiHorseInventory(Lnet.minecraft.entity.passive.AbstractHorse;Lnet.minecraft.inventory.IInventory;)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.entity.player.EntityPlayerMP.displayGuiCommandBlock(Lnet.minecraft.tileentity.TileEntityCommandBlock;)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.entity.player.EntityPlayerMP.displayGuiEditCommandCart(Lnet.minecraft.tileentity.CommandBlockBaseLogic;)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.entity.player.EntityPlayerMP.handleClientSettings(Lnet.minecraft.network.play.client.CPacketClientSettings;)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.entity.player.EntityPlayerMP.openGuiHorseInventory(Lnet.minecraft.entity.passive.AbstractHorse;Lnet.minecraft.inventory.IInventory;)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.entity.player.InventoryPlayer.fillStackedContents(Lnet.minecraft.client.util.RecipeItemHelper;Z)V  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraft.inventory.Slot.getBackgroundMap()Lnet.minecraft.client.renderer.texture.TextureMap;  # AE2WTLib-1.12.2-1.0.34.jar
  1  net.minecraft.item.Item.doOpenPiGui()Z  # Draconic-Evolution-1.12.2-2.3.28.354-universal.jar
  1  net.minecraft.network.handshake.client.C00Handshake.<init>(Ljava.lang.String;ILnet.minecraft.network.EnumConnectionState;)V  # FTBUtilities-5.4.1.131.jar
  1  net.minecraft.network.handshake.client.C00Handshake.<init>(Ljava.lang.String;ILnet.minecraft.network.EnumConnectionState;Z)V  # LoTAS1.12.2-2.1.3.jar
  1  net.minecraft.network.login.client.CPacketLoginStart.<init>(Lcom.mojang.authlib.GameProfile;)V  # LoTAS1.12.2-2.1.3.jar
  1  net.minecraft.network.status.client.CPacketPing.<init>(J)V  # FTBUtilities-5.4.1.131.jar
  1  net.minecraft.network.status.client.CPacketServerQuery.<init>()V  # FTBUtilities-5.4.1.131.jar
  1  net.minecraftforge.client.ForgeHooksClient.getDamageModel(Lnet.minecraft.client.renderer.block.model.IBakedModel;Lnet.minecraft.client.renderer.texture.TextureAtlasSprite;Lnet.minecraft.block.state.IBlockState;Lnet.minecraft.world.IBlockAccess;Lnet.minecraft.util.math.BlockPos;)Lnet.minecraft.client.renderer.block.model.IBakedModel;  # ForgeMultipart-1.12.2-2.6.2.83-universal.jar
  1  net.minecraftforge.client.ForgeHooksClient.postDraw(Lnet.minecraft.client.renderer.vertex.VertexFormatElement$EnumUsage;Lnet.minecraft.client.renderer.vertex.VertexFormat;IILjava.nio.ByteBuffer;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraftforge.client.ForgeHooksClient.preDraw(Lnet.minecraft.client.renderer.vertex.VertexFormatElement$EnumUsage;Lnet.minecraft.client.renderer.vertex.VertexFormat;IILjava.nio.ByteBuffer;)V  # 永爱之刃-v11削弱.jar
  1  net.minecraftforge.client.GuiIngameForge.renderGameOverlay(F)V  # 永爱之刃-v11削弱.jar
  1  net.minecraftforge.client.MinecraftForgeClient.getImageLayer(Lnet.minecraft.util.ResourceLocation;Lnet.minecraft.client.resources.IResourceManager;)Ljava.awt.image.BufferedImage;  # ThaumicAugmentation-1.12.2-2.1.8.jar
  1  net.minecraftforge.client.MinecraftForgeClient.onRebuildChunk(Lnet.minecraft.world.World;Lnet.minecraft.util.math.BlockPos;Lnet.minecraft.world.ChunkCache;)V  # iChunUtil-1.12.2-7.2.2.jar
  1  net.minecraftforge.client.model.ModelLoader.makeBakedQuad(Lnet.minecraft.client.renderer.block.model.BlockPart;Lnet.minecraft.client.renderer.block.model.BlockPartFace;Lnet.minecraft.client.renderer.texture.TextureAtlasSprite;Lnet.minecraft.util.EnumFacing;Lnet.minecraft.client.renderer.block.model.ModelRotation;Z)Lnet.minecraft.client.renderer.block.model.BakedQuad;  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraftforge.client.model.ModelLoader.textureMap:Lnet.minecraft.client.renderer.texture.TextureMap;  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraftforge.common.model.Models.getParts(Lnet.minecraftforge.common.model.IModelPart;)Lcom.google.common.collect.UnmodifiableIterator;  # Numina-1.12.2-1.0.38.jar
  1  net.minecraftforge.common.model.TRSRTransformation.slerp(Lnet.minecraftforge.common.model.TRSRTransformation;F)Lnet.minecraftforge.common.model.TRSRTransformation;  # UniversalModCore-1.12.2-forge-1.3.1.jar
  1  net.minecraftforge.common.model.animation.IClip.apply(Lnet.minecraftforge.common.model.animation.IJoint;)Lnet.minecraftforge.common.model.animation.IJointClip;  # OpenModsLib-1.12.2-0.12.2.jar
  1  net.minecraftforge.common.model.animation.IJointClip.apply(F)Lnet.minecraftforge.common.model.TRSRTransformation;  # OpenModsLib-1.12.2-0.12.2.jar
  1  net.minecraftforge.fml.client.FMLClientHandler.getSavesDirectory()Ljava.io.File;  # Call to Battle 3.jar
  1  net.minecraftforge.fml.client.FMLClientHandler.getWorldThread(Lnet.minecraft.network.INetHandler;)Lnet.minecraft.util.IThreadListener;  # weaponmaster_ydm-forge-1.12.2-4.2.3.jar
  1  net.minecraftforge.fml.client.FMLClientHandler.renderClouds(IF)Z  # SCP Lockdown-1.12.2-2.4.3-release.jar
  1  net.minecraftforge.fml.client.FMLClientHandler.setupServerList()V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar
  1  net.minecraftforge.fml.client.FMLClientHandler.showInGameModOptions(Lnet.minecraft.client.gui.GuiIngameMenu;)V  # pokelucky-RG-6.16.14(1.12.2).jar
  1  net.minecraftforge.fml.client.FMLClientHandler.startIntegratedServer(Ljava.lang.String;Ljava.lang.String;Lnet.minecraft.world.WorldSettings;)V  # Chunk Pregenerator-V1.12-2.5.0.jar
  1  net.minecraftforge.fml.client.FMLClientHandler.tryLoadExistingWorld(Lnet.minecraft.client.gui.GuiWorldSelection;Lnet.minecraft.world.storage.WorldSummary;)V  # CyclopsCore-1.12.2-1.6.7.jar
  1  net.minecraftforge.fml.client.GuiModList.selectModIndex(I)V  # MPUtils-1.12.2-1.5.7.jar
  1  net.minecraftforge.fml.client.GuiOldSaveLoadConfirm.<init>(Ljava.lang.String;Ljava.lang.String;Lnet.minecraft.client.gui.GuiScreen;)V  # CustomMainMenu-MC1.12.2-2.0.9.1.jar
  1  net.minecraftforge.fml.client.config.GuiConfig.b()V  # MouseTweaks-2.10-mc1.12.2.jar
  1  net.minecraftforge.fml.client.config.GuiConfig.m()V  # MouseTweaks-2.10-mc1.12.2.jar
  1  net.minecraftforge.fml.common.network.NetworkRegistry.getLocalGuiContainer(Lnet.minecraftforge.fml.common.ModContainer;Lnet.minecraft.entity.player.EntityPlayer;ILnet.minecraft.world.World;III)Ljava.lang.Object;  # DoubleSlabs-1.12-0.12.2.jar
  1  net.minecraftforge.fml.common.network.NetworkRegistry.getRemoteGuiContainer(Lnet.minecraftforge.fml.common.ModContainer;Lnet.minecraft.entity.player.EntityPlayerMP;ILnet.minecraft.world.World;III)Lnet.minecraft.inventory.Container;  # DoubleSlabs-1.12-0.12.2.jar
  1  org.lwjgl.input.Mouse.isClipMouseCoordinatesToWindow()Z  # bbs-fs-2.8-1.12.2-alpha.1.jar
  1  org.lwjgl.opengl.ContextCapabilities.GL_ARB_texture_multisample:Z  # Modern-Warfare-Cubed-0.1.9.jar
  1  org.lwjgl.opengl.Display.getParent()Ljava.awt.Canvas;  # bbs-fs-2.8-1.12.2-alpha.1.jar
  1  org.lwjgl.opengl.GL11.glColorPointer(IILjava.nio.DoubleBuffer;)V  # Numina-1.12.2-1.0.38.jar
  1  org.lwjgl.opengl.GL11.glGetLight(IILjava.nio.FloatBuffer;)V  # bbs-fs-2.8-1.12.2-alpha.1.jar
  1  org.lwjgl.opengl.GL11.glVertexPointer(IILjava.nio.DoubleBuffer;)V  # Numina-1.12.2-1.0.38.jar
  1  org.lwjgl.opengl.GL15.glGetQueryObject(IILjava.nio.IntBuffer;)V  # fluxloading-1.4.1.jar
  1  org.lwjgl.util.WaveData.create(Ljava.io.InputStream;)Lorg.lwjgl.util.WaveData;  # Modern-Warfare-Cubed-0.1.9.jar
  1  org.lwjgl.util.WaveData.data:Ljava.nio.ByteBuffer;  # Modern-Warfare-Cubed-0.1.9.jar
  1  org.lwjgl.util.WaveData.dispose()V  # Modern-Warfare-Cubed-0.1.9.jar
  1  org.lwjgl.util.WaveData.format:I  # Modern-Warfare-Cubed-0.1.9.jar
  1  org.lwjgl.util.WaveData.samplerate:I  # Modern-Warfare-Cubed-0.1.9.jar
  1  org.lwjgl.util.vector.Quaternion.setFromMatrix(Lorg.lwjgl.util.vector.Matrix3f;Lorg.lwjgl.util.vector.Quaternion;)Lorg.lwjgl.util.vector.Quaternion;  # modularwarfare-2.4.2f.jar
  1  paulscode.sound.Library.getSource(Ljava.lang.String;)Lpaulscode.sound.Source;  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.SoundBuffer.<init>([BLjavax.sound.sampled.AudioFormat;)V  # netmusic-1.12.2-1.1.0.jar
  1  paulscode.sound.SoundBuffer.audioFormat:Ljavax.sound.sampled.AudioFormat;  # Matmos_7.0_1.12.2.jar
  1  paulscode.sound.SoundBuffer.cleanup()V  # bbs-fs-2.8-1.12.2-alpha.1.jar
  1  paulscode.sound.SoundBuffer.trimData(I)V  # Matmos_7.0_1.12.2.jar
  1  paulscode.sound.SoundSystem.fadeOut(Ljava.lang.String;Ljava.lang.String;J)V  # Pixelmon-1.12.2-8.4.3-universal.jar
  1  paulscode.sound.SoundSystem.flush(Ljava.lang.String;)V  # industrialcraft-2-2.8.222-ex112.jar
  1  paulscode.sound.SoundSystem.randomNumberGenerator:Ljava.util.Random;  # IC2Classic-1.12.2-1.5.15.jar
  1  paulscode.sound.SoundSystemConfig.setCodec(Ljava.lang.String;Ljava.lang.Class;)V  # netmusic-1.12.2-1.1.0.jar
  1  paulscode.sound.SoundSystemException.getMessage()Ljava.lang.String;  # netmusic-1.12.2-1.1.0.jar
  1  paulscode.sound.Source.playing()Z  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.Source.setPitch(F)V  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.Source.sourceVolume:F  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.Source.stop()V  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.Source.toLoop:Z  # AmbientSounds_v3.1.5_mc1.12.2.jar
  1  paulscode.sound.libraries.ChannelLWJGLOpenAL.ALSource:Ljava.nio.IntBuffer;  # TheBetweenlands-3.9.6-universal.jar
  1  paulscode.sound.libraries.ChannelLWJGLOpenAL.attachedSource:Lpaulscode.sound.Source;  # TheBetweenlands-3.9.6-universal.jar
  1  paulscode.sound.libraries.SourceLWJGLOpenAL.channel:Lpaulscode.sound.Channel;  # TheBetweenlands-3.9.6-universal.jar
```

## mod-side API (top 183 of 183)

```
 20  mezz.jei.api.IModRegistry.addRecipeHandlers([Lmezz.jei.api.recipe.IRecipeHandler;)V  # AdditionalBanners-1.12.2-1.1.77.jar, AdvancedRocketry-1.12.2-1.7.0-232-universal.jar, Age-of-Weapons-1.12.2-(v.0.13.6).jar ...
 13  mezz.jei.api.IModRegistry.addRecipeCategories([Lmezz.jei.api.recipe.IRecipeCategory;)V  # AdditionalBanners-1.12.2-1.1.77.jar, AdvancedRocketry-1.12.2-1.7.0-232-universal.jar, Age-of-Weapons-1.12.2-(v.0.13.6).jar ...
 13  mezz.jei.api.IModRegistry.addRecipes(Ljava.util.Collection;)V  # AdditionalBanners-1.12.2-1.1.77.jar, AdvancedRocketry-1.12.2-1.7.0-232-universal.jar, Age-of-Weapons-1.12.2-(v.0.13.6).jar ...
 12  mezz.jei.api.IJeiHelpers.recipeTransferHandlerHelper()Lmezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar, ImmersiveEngineering-0.12-98.jar ...
 12  mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper.createUserErrorWithTooltip(Ljava.lang.String;)Lmezz.jei.api.recipe.transfer.IRecipeTransferError;  # DankNull-1.12.2-1.7.101.jar, EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar ...
 11  mezz.jei.api.BlankModPlugin.<init>()V  # AdditionalBanners-1.12.2-1.1.77.jar, AdvancedRocketry-1.12.2-1.7.0-232-universal.jar, Age-of-Weapons-1.12.2-(v.0.13.6).jar ...
 10  mezz.jei.api.IGuiHelper.createDrawable(Lnet.minecraft.util.ResourceLocation;IIIIII)Lmezz.jei.api.gui.IDrawableStatic;  # Cherry_on-1.12.2-1.3.35.jar, EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar ...
 10  mezz.jei.api.IModRegistry.addGhostIngredientHandler(Ljava.lang.Class;Lmezz.jei.api.gui.IGhostIngredientHandler;)V  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar, FTBLib-5.4.7.2.jar ...
 10  mezz.jei.api.IModRegistry.addRecipeRegistryPlugin(Lmezz.jei.api.recipe.IRecipeRegistryPlugin;)V  # Chisel-MC1.12.2-1.0.2.45.jar, EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar ...
  9  mezz.jei.api.gui.ICraftingGridHelper.setInputs(Lmezz.jei.api.gui.IGuiIngredientGroup;Ljava.util.List;II)V  # AoA3-3.3.6.jar, BloodMagic-1.12.2-2.4.3-105.jar, EnderStorage-1.12.2-2.4.6.137-universal.jar ...
  9  mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper.createInternalError()Lmezz.jei.api.recipe.transfer.IRecipeTransferError;  # DankNull-1.12.2-1.7.101.jar, EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar ...
  9  mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper.createUserErrorForSlots(Ljava.lang.String;Ljava.util.Collection;)Lmezz.jei.api.recipe.transfer.IRecipeTransferError;  # DankNull-1.12.2-1.7.101.jar, EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar ...
  9  mezz.jei.api.recipe.transfer.IRecipeTransferRegistry.addRecipeTransferHandler(Lmezz.jei.api.recipe.transfer.IRecipeTransferInfo;)V  # AoA3-3.3.6.jar, Draconic-Evolution-1.12.2-2.3.28.354-universal.jar, ElectroblobsWizardry-4.3.14.jar ...
  9  mezz.jei.util.Translator.translateToLocal(Ljava.lang.String;)Ljava.lang.String;  # AdditionalBanners-1.12.2-1.1.77.jar, GrimoireOfGaia3-1.12.2-1.7.2.jar, NTM-CE-1.12.2-2.6.1.0.jar ...
  8  mezz.jei.api.IJeiHelpers.getVanillaRecipeFactory()Lmezz.jei.api.recipe.IVanillaRecipeFactory;  # BloodMagic-1.12.2-2.4.3-105.jar, Bountiful Baubles-1.12.2-0.1.6.jar, MekanismTools-1.12.2-9.8.3.390.jar ...
  8  mezz.jei.api.IJeiRuntime.getIngredientListOverlay()Lmezz.jei.api.IIngredientListOverlay;  # Botania r1.10-363.jar, Botania+r1.10-364.4.jar, BrandonsCore-1.12.2-2.4.20.162-universal.jar ...
  8  mezz.jei.api.IRecipeRegistry.getRecipeWrapper(Ljava.lang.Object;Ljava.lang.String;)Lmezz.jei.api.recipe.IRecipeWrapper;  # Botania r1.10-363.jar, Botania+r1.10-364.4.jar, Galacticraft-1.12.2-4.0.6.jar ...
  8  mezz.jei.api.recipe.IVanillaRecipeFactory.createAnvilRecipe(Lnet.minecraft.item.ItemStack;Ljava.util.List;Ljava.util.List;)Lmezz.jei.api.recipe.IRecipeWrapper;  # BloodMagic-1.12.2-2.4.3-105.jar, Bountiful Baubles-1.12.2-0.1.6.jar, MekanismTools-1.12.2-9.8.3.390.jar ...
  7  mezz.jei.api.IIngredientListOverlay.getIngredientUnderMouse()Ljava.lang.Object;  # Botania r1.10-363.jar, Botania+r1.10-364.4.jar, BrandonsCore-1.12.2-2.4.20.162-universal.jar ...
  7  mezz.jei.api.gui.IGuiIngredientGroup.init(IZLmezz.jei.api.ingredients.IIngredientRenderer;IIIIII)V  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar, Inspirations-1.12.2-0.2.9.jar ...
  6  mezz.jei.api.IItemBlacklist.addItemToBlacklist(Lnet.minecraft.item.ItemStack;)V  # Cherry_on-1.12.2-1.3.35.jar, IC2Classic-1.12.2-1.5.15.jar, SpeedsterHeroes-1.12.2-2.2.1.jar ...
  6  mezz.jei.api.IJeiHelpers.getItemBlacklist()Lmezz.jei.api.IItemBlacklist;  # Cherry_on-1.12.2-1.3.35.jar, IC2Classic-1.12.2-1.5.15.jar, SpeedsterHeroes-1.12.2-2.2.1.jar ...
  6  mezz.jei.api.gui.IRecipeLayout.getRecipeCategory()Lmezz.jei.api.recipe.IRecipeCategory;  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar, JustEnoughCalculation-1.12.2-3.2.5.jar ...
  6  mezz.jei.api.ingredients.IModIngredientRegistration.register(Lmezz.jei.api.recipe.IIngredientType;Ljava.util.Collection;Lmezz.jei.api.ingredients.IIngredientHelper;Lmezz.jei.api.ingredients.IIngredientRenderer;)V  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar, Mekanism-1.12.2-9.8.3.390.jar ...
  5  com.mojang.realmsclient.dto.RealmsServer.id:J  # Xaeros_Minimap_FP24.2.0_Forge_1.12.jar, journeymap-1.12.2-5.7.1.jar, journeymap-forge-1.12.2-6.0.10.jar ...
  5  mezz.jei.JustEnoughItems.getProxy()Lmezz.jei.startup.ProxyCommon;  # DankNull-1.12.2-1.7.101.jar, NTM-CE-1.12.2-2.6.1.0.jar, Quark-r1.6-179.jar ...
  5  mezz.jei.api.gui.ICraftingGridHelper.setInputs(Lmezz.jei.api.gui.IGuiIngredientGroup;Ljava.util.List;)V  # BloodMagic-1.12.2-2.4.3-105.jar, RecipeStages-1.1.3.8.jar, TechReborn-1.12.2-2.27.3.1084-universal.jar ...
  5  mezz.jei.plugins.vanilla.crafting.ShapelessRecipeWrapper.<init>(Lmezz.jei.api.IJeiHelpers;Lnet.minecraft.item.crafting.IRecipe;)V  # AE2WTLib-1.12.2-1.0.34.jar, DankNull-1.12.2-1.7.101.jar, IC2Classic-1.12.2-1.5.15.jar ...
  5  mezz.jei.startup.ProxyCommon.sendPacketToServer(Lmezz.jei.network.packets.PacketJei;)V  # DankNull-1.12.2-1.7.101.jar, NTM-CE-1.12.2-2.6.1.0.jar, Quark-r1.6-179.jar ...
  5  mezz.jei.startup.StackHelper$MatchingItemsResult.matchingItems:Ljava.util.Map;  # DankNull-1.12.2-1.7.101.jar, NTM-CE-1.12.2-2.6.1.0.jar, Quark-r1.6-179.jar ...
  5  mezz.jei.startup.StackHelper$MatchingItemsResult.missingItems:Ljava.util.List;  # DankNull-1.12.2-1.7.101.jar, NTM-CE-1.12.2-2.6.1.0.jar, Quark-r1.6-179.jar ...
  5  mezz.jei.startup.StackHelper.getMatchingItems(Ljava.util.Map;Ljava.util.Map;)Lmezz.jei.startup.StackHelper$MatchingItemsResult;  # DankNull-1.12.2-1.7.101.jar, NTM-CE-1.12.2-2.6.1.0.jar, Quark-r1.6-179.jar ...
  4  com.mojang.realmsclient.dto.RealmsServer.ownerUUID:Ljava.lang.String;  # Xaeros_Minimap_FP24.2.0_Forge_1.12.jar, journeymap-forge-1.12.2-6.0.10.jar, xaerominimap-forge-1.12.2-26.6.0.jar ...
  4  mezz.jei.api.IRecipeRegistry.hideRecipe(Lmezz.jei.api.recipe.IRecipeWrapper;)V  # Galacticraft-1.12.2-4.0.6.jar, RecipeStages-1.1.3.8.jar, ThaumicJEI-1.12.2-1.6.0-27.jar ...
  4  mezz.jei.api.IRecipeRegistry.hideRecipeCategory(Ljava.lang.String;)V  # CraftTweaker2-1.12-4.1.20.698.jar, CraftTweaker2-1.12-4.1.20.700.jar, GalaxySpace-1.12.2-2.2.0.jar ...
  4  mezz.jei.api.IRecipeRegistry.unhideRecipe(Lmezz.jei.api.recipe.IRecipeWrapper;Ljava.lang.String;)V  # HammerLib-1.12.2-12.2.66.jar, HammerLib-1.12.2-2.0.6.32.jar, ThaumicJEI-1.12.2-1.6.0-27.jar ...
  4  mezz.jei.api.ingredients.IIngredientRegistry.addIngredientsAtRuntime(Lmezz.jei.api.recipe.IIngredientType;Ljava.util.Collection;)V  # CraftTweaker2-1.12-4.1.20.698.jar, CraftTweaker2-1.12-4.1.20.700.jar, FTBQuests-1202.9.0.15.jar ...
  3  baubles.common.Config.renderBaubles:Z  # Artifacts-1.12.2-1.2.3.jar, RLArtifacts-1.1.2.jar, thaumicperiphery-0.3.1.jar
  3  baubles.common.network.PacketHandler.INSTANCE:Lnet.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;  # Botania r1.10-363.jar, Botania+r1.10-364.4.jar, Trinkets and Baubles-Forge-1.12.2-0.33.4.jar
  3  mcp.mobius.waila.config.FormattingConfig.modNameFormat:Ljava.lang.String;  # DramaticTrees-1.12.2-0.13.6.jar, TheBetweenlands-3.9.6-universal.jar, iceandfire-1.9.1-1.12.2.jar
  3  mezz.jei.api.IJeiRuntime.getItemListOverlay()Lmezz.jei.api.IItemListOverlay;  # OpenComputers-MC1.12.2-1.7.5.192.jar, OpenComputers-MC1.12.2-1.8.10+258943363.jar, extrautils2-1.12-1.9.9.jar
  3  mezz.jei.api.IModRegistry.addGlobalGuiHandlers([Lmezz.jei.api.gui.IGlobalGuiHandler;)V  # FTBLib-5.4.7.2.jar, Quark-r1.6-179.jar, rtsbuilding-1.1.6-1.12.2-port.jar
  3  mezz.jei.api.IRecipeRegistry.getRecipeCategories()Ljava.util.List;  # CraftTweaker2-1.12-4.1.20.698.jar, CraftTweaker2-1.12-4.1.20.700.jar, PackagedAuto-1.12.2-1.0.24.73.jar
  3  mezz.jei.api.IRecipeRegistry.getRecipeCategory(Ljava.lang.String;)Lmezz.jei.api.recipe.IRecipeCategory;  # MoreTweaker-1.0.19.jar, ThaumicJEI-1.12.2-1.6.0-27.jar, ThaumicJEI-1.12.2-1.7.0.jar
  3  mezz.jei.api.ingredients.IIngredientRegistry.addIngredientsAtRuntime(Ljava.lang.Class;Ljava.util.List;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar, OpenComputers-MC1.12.2-1.8.10+258943363.jar, industrialcraft-2-2.8.222-ex112.jar
  3  mezz.jei.api.ingredients.IIngredientRegistry.removeIngredientsAtRuntime(Lmezz.jei.api.recipe.IIngredientType;Ljava.util.Collection;)V  # CraftTweaker2-1.12-4.1.20.698.jar, CraftTweaker2-1.12-4.1.20.700.jar, FTBQuests-1202.9.0.15.jar
  3  mezz.jei.api.ingredients.IModIngredientRegistration.register(Ljava.lang.Class;Ljava.util.Collection;Lmezz.jei.api.ingredients.IIngredientHelper;Lmezz.jei.api.ingredients.IIngredientRenderer;)V  # ImmersiveConvergence-1.12.2-1.0.184-release.jar, Inspirations-1.12.2-0.2.9.jar, MCTImmersiveTechnology-1.12.2-1.9.100.jar
  3  mezz.jei.api.recipe.transfer.IRecipeTransferInfo.getInventorySlots(Lnet.minecraft.inventory.Container;)Ljava.util.List;  # Quark-r1.6-179.jar, industrialcraft-2-2.8.222-ex112.jar, rtsbuilding-1.1.6-1.12.2-port.jar
  3  mezz.jei.ingredients.Ingredients.<init>()V  # CraftTweaker2-1.12-4.1.20.698.jar, CraftTweaker2-1.12-4.1.20.700.jar, GrimoireOfGaia3-1.12.2-1.7.2.jar
  3  mezz.jei.network.packets.PacketRecipeTransfer.<init>(Ljava.util.Map;Ljava.util.List;Ljava.util.List;ZZ)V  # Quark-r1.6-179.jar, astralsorcery-1.12.2-1.10.27.jar, industrialcraft-2-2.8.222-ex112.jar
  3  mezz.jei.plugins.vanilla.brewing.BrewingRecipeWrapper.<init>(Ljava.util.List;Lnet.minecraft.item.ItemStack;Lnet.minecraft.item.ItemStack;)V  # Cherry_on-1.12.2-1.3.35.jar, CraftTweaker2-1.12-4.1.20.698.jar, CraftTweaker2-1.12-4.1.20.700.jar
  3  mezz.jei.transfer.BasicRecipeTransferHandler.<init>(Lmezz.jei.startup.StackHelper;Lmezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;Lmezz.jei.api.recipe.transfer.IRecipeTransferInfo;)V  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar, industrialcraft-2-2.8.222-ex112.jar
  3  mezz.jei.transfer.BasicRecipeTransferInfo.<init>(Ljava.lang.Class;Ljava.lang.String;IIII)V  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar, Quark-r1.6-179.jar
  3  mezz.jei.util.Log.get()Lorg.apache.logging.log4j.Logger;  # Quark-r1.6-179.jar, astralsorcery-1.12.2-1.10.27.jar, industrialcraft-2-2.8.222-ex112.jar
  2  baubles.common.network.PacketSync.<init>(Lnet.minecraft.entity.player.EntityPlayer;ILnet.minecraft.item.ItemStack;)V  # Botania r1.10-363.jar, Botania+r1.10-364.4.jar
  2  com.mojang.realmsclient.dto.RealmsServer.name:Ljava.lang.String;  # journeymap-1.12.2-5.7.1.jar, journeymap-forge-1.12.2-6.0.10.jar
  2  mcp.mobius.waila.cbcore.LangUtil.translateG(Ljava.lang.String;[Ljava.lang.Object;)Ljava.lang.String;  # Cherry_on-1.12.2-1.3.35.jar, Pam's HarvestCraft 1.12.2zg.jar
  2  mcp.mobius.waila.config.FormattingConfig.entityFormat:Ljava.lang.String;  # TheBetweenlands-3.9.6-universal.jar, iceandfire-1.9.1-1.12.2.jar
  2  mezz.jei.api.IItemListOverlay.getStackUnderMouse()Lnet.minecraft.item.ItemStack;  # OpenComputers-MC1.12.2-1.7.5.192.jar, OpenComputers-MC1.12.2-1.8.10+258943363.jar
  2  mezz.jei.api.IItemListOverlay.getVisibleStacks()Lcom.google.common.collect.ImmutableList;  # OpenComputers-MC1.12.2-1.7.5.192.jar, OpenComputers-MC1.12.2-1.8.10+258943363.jar
  2  mezz.jei.api.IItemListOverlay.highlightStacks(Ljava.util.Collection;)V  # OpenComputers-MC1.12.2-1.7.5.192.jar, OpenComputers-MC1.12.2-1.8.10+258943363.jar
  2  mezz.jei.api.IRecipeRegistry.createRecipeLayoutDrawable(Lmezz.jei.api.recipe.IRecipeCategory;Lmezz.jei.api.recipe.IRecipeWrapper;Lmezz.jei.api.recipe.IFocus;)Lmezz.jei.api.gui.IRecipeLayoutDrawable;  # BrandonsCore-1.12.2-2.4.20.162-universal.jar, Draconic-Evolution-1.12.2-2.3.28.354-universal.jar
  2  mezz.jei.api.IRecipeRegistry.getRecipeCategories(Ljava.util.List;)Ljava.util.List;  # Chisel-MC1.12.2-1.0.2.45.jar, extrautils2-1.12-1.9.9.jar
  2  mezz.jei.api.IRecipeRegistry.getRecipeWrappers(Lmezz.jei.api.recipe.IRecipeCategory;Lmezz.jei.api.recipe.IFocus;)Ljava.util.List;  # Chisel-MC1.12.2-1.0.2.45.jar, NTM-CE-1.12.2-2.6.1.0.jar
  2  mezz.jei.api.IRecipeRegistry.unhideRecipe(Lmezz.jei.api.recipe.IRecipeWrapper;)V  # Galacticraft-1.12.2-4.0.6.jar, RecipeStages-1.1.3.8.jar
  2  mezz.jei.api.IRecipesGui.getIngredientUnderMouse()Ljava.lang.Object;  # Botania r1.10-363.jar, Botania+r1.10-364.4.jar
  2  mezz.jei.api.gui.IGuiItemStackGroup.init(IZLmezz.jei.api.ingredients.IIngredientRenderer;IIIIII)V  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  mezz.jei.api.gui.IRecipeLayoutDrawable.getIngredientUnderMouse(II)Ljava.lang.Object;  # BrandonsCore-1.12.2-2.4.20.162-universal.jar, Draconic-Evolution-1.12.2-2.3.28.354-universal.jar
  2  mezz.jei.api.gui.IRecipeLayoutDrawable.setPosition(II)V  # BrandonsCore-1.12.2-2.4.20.162-universal.jar, Draconic-Evolution-1.12.2-2.3.28.354-universal.jar
  2  mezz.jei.api.ingredients.IIngredientHelper.expandSubtypes(Ljava.util.List;)Ljava.util.List;  # CraftTweaker2-1.12-4.1.20.698.jar, CraftTweaker2-1.12-4.1.20.700.jar
  2  mezz.jei.api.ingredients.IIngredientRegistry.getIngredientHelper(Lmezz.jei.api.recipe.IIngredientType;)Lmezz.jei.api.ingredients.IIngredientHelper;  # CraftTweaker2-1.12-4.1.20.698.jar, CraftTweaker2-1.12-4.1.20.700.jar
  2  mezz.jei.api.recipe.IStackHelper.containsAnyStack(Ljava.lang.Iterable;Ljava.lang.Iterable;)Lnet.minecraft.item.ItemStack;  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  mezz.jei.api.recipe.transfer.IRecipeTransferInfo.canHandle(Lnet.minecraft.inventory.Container;)Z  # Quark-r1.6-179.jar, rtsbuilding-1.1.6-1.12.2-port.jar
  2  mezz.jei.api.recipe.transfer.IRecipeTransferInfo.getContainerClass()Ljava.lang.Class;  # Quark-r1.6-179.jar, industrialcraft-2-2.8.222-ex112.jar
  2  mezz.jei.api.recipe.transfer.IRecipeTransferInfo.getRecipeSlots(Lnet.minecraft.inventory.Container;)Ljava.util.List;  # Quark-r1.6-179.jar, rtsbuilding-1.1.6-1.12.2-port.jar
  2  mezz.jei.config.Constants.RECIPE_GUI_VANILLA:Lnet.minecraft.util.ResourceLocation;  # Future-MC-0.2.21.jar, future-mc-0.2.11.jar
  2  mezz.jei.config.ServerInfo.isJeiOnServer()Z  # DankNull-1.12.2-1.7.101.jar, NTM-CE-1.12.2-2.6.1.0.jar
  2  mezz.jei.gui.elements.DrawableBlank.<init>(II)V  # blockdrops-1.12.2-1.4.0.jar, gendustryjei-1.0.2.jar
  2  mezz.jei.gui.recipes.RecipeLayout.getPosX()I  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  mezz.jei.gui.recipes.RecipeLayout.getPosY()I  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
  2  mezz.jei.plugins.vanilla.crafting.ShapedOreRecipeWrapper.<init>(Lmezz.jei.api.IJeiHelpers;Lnet.minecraftforge.oredict.ShapedOreRecipe;)V  # TLS-2.3.2-1.12.2.jar, TechReborn-1.12.2-2.27.3.1084-universal.jar
  2  mezz.jei.plugins.vanilla.crafting.ShapelessRecipeWrapper.drawInfo(Lnet.minecraft.client.Minecraft;IIII)V  # IC2Classic-1.12.2-1.5.15.jar, RecipeStages-1.1.3.8.jar
  2  mezz.jei.plugins.vanilla.ingredients.fluid.FluidStackRenderer.<init>()V  # TerraFirmaCraft-MC1.12.2-1.7.23.181.jar, curvy_pipes-1.12.2-1.15.8.jar
  2  mezz.jei.transfer.BasicRecipeTransferHandler.transferRecipe(Lnet.minecraft.inventory.Container;Lmezz.jei.api.gui.IRecipeLayout;Lnet.minecraft.entity.player.EntityPlayer;ZZ)Lmezz.jei.api.recipe.transfer.IRecipeTransferError;  # EnderIO-1.12.2-5.3.70.jar, EnderIO-1.12.2-5.3.72.jar
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
  1  mezz.jei.Internal.getStackHelper()Lmezz.jei.startup.StackHelper;  # Quark-r1.6-179.jar
  1  mezz.jei.api.IBookmarkOverlay.getIngredientUnderMouse()Ljava.lang.Object;  # JustEnoughCalculation-1.12.2-3.2.5.jar
  1  mezz.jei.api.ICollapsibleGroupRegistry$Builder.addAny(Lmezz.jei.api.recipe.IIngredientType;Ljava.util.function.Predicate;)Lmezz.jei.api.ICollapsibleGroupRegistry$Builder;  # NTM-CE-1.12.2-2.6.1.0.jar
  1  mezz.jei.api.ICollapsibleGroupRegistry$Builder.build()V  # NTM-CE-1.12.2-2.6.1.0.jar
  1  mezz.jei.api.ICollapsibleGroupRegistry.newGroup(Ljava.lang.String;Ljava.lang.String;)Lmezz.jei.api.ICollapsibleGroupRegistry$Builder;  # NTM-CE-1.12.2-2.6.1.0.jar
  1  mezz.jei.api.IIngredientFilter.getFilteredIngredients()Lcom.google.common.collect.ImmutableList;  # moreoverlays-1.15.1-mc1.12.2.jar
  1  mezz.jei.api.IItemListOverlay.getFilterText()Ljava.lang.String;  # extrautils2-1.12-1.9.9.jar
  1  mezz.jei.api.IItemListOverlay.setFilterText(Ljava.lang.String;)V  # extrautils2-1.12-1.9.9.jar
  1  mezz.jei.api.IJeiHelpers.reload()V  # AdvancedRocketry-1.12.2-1.7.0-232-universal.jar
  1  mezz.jei.api.IJeiRuntime.getBookmarkOverlay()Lmezz.jei.api.IBookmarkOverlay;  # JustEnoughCalculation-1.12.2-3.2.5.jar
  1  mezz.jei.api.IModRegistry.addGuiScreenHandler(Ljava.lang.Class;Lmezz.jei.api.gui.IGuiScreenHandler;)V  # modularui-2.4.3.jar
  1  mezz.jei.api.IRecipeRegistry.addRecipe(Ljava.lang.Object;)V  # TheBetweenlands-3.9.6-universal.jar
  1  mezz.jei.api.IRecipeRegistry.getRecipeCatalysts(Lmezz.jei.api.recipe.IRecipeCategory;)Ljava.util.List;  # JustEnoughCalculation-1.12.2-3.2.5.jar
  1  mezz.jei.api.IRecipeRegistry.getRecipeCategories(Lmezz.jei.api.recipe.IFocus;)Ljava.util.List;  # NTM-CE-1.12.2-2.6.1.0.jar
  1  mezz.jei.api.IRecipeRegistry.getRecipeTransferHandler(Lnet.minecraft.inventory.Container;Lmezz.jei.api.recipe.IRecipeCategory;)Lmezz.jei.api.recipe.transfer.IRecipeTransferHandler;  # extrautils2-1.12-1.9.9.jar
  1  mezz.jei.api.IRecipeRegistry.removeRecipe(Ljava.lang.Object;)V  # TheBetweenlands-3.9.6-universal.jar
  1  mezz.jei.api.IRecipeRegistry.unhideRecipeCategory(Ljava.lang.String;)V  # JustEnoughResources-1.12.2-0.9.2.60.jar
  1  mezz.jei.api.ISubtypeRegistry$ISubtypeInterpreter.andThen(Ljava.util.function.Function;)Ljava.util.function.Function;  # jeibees-0.9.0.5-mc1.12.2.jar
  1  mezz.jei.api.ISubtypeRegistry$ISubtypeInterpreter.compose(Ljava.util.function.Function;)Ljava.util.function.Function;  # jeibees-0.9.0.5-mc1.12.2.jar
  1  mezz.jei.api.ISubtypeRegistry$ISubtypeInterpreter.getSubtypeInfo(Lnet.minecraft.item.ItemStack;)Ljava.lang.String;  # jeibees-0.9.0.5-mc1.12.2.jar
  1  mezz.jei.api.gui.ICraftingGridHelper.setInputStacks(Lmezz.jei.api.gui.IGuiItemStackGroup;Ljava.util.List;)V  # ThermalDynamics-1.12.2-2.5.6.1-universal.jar
  1  mezz.jei.api.gui.IGuiFluidStackGroup.init(IZLmezz.jei.api.ingredients.IIngredientRenderer;IIIIII)V  # TerraFirmaCraft-MC1.12.2-1.7.23.181.jar
  1  mezz.jei.api.gui.IRecipeLayoutDrawable.draw(Lnet.minecraft.client.Minecraft;II)V  # Draconic-Evolution-1.12.2-2.3.28.354-universal.jar
  1  mezz.jei.api.gui.IRecipeLayoutDrawable.drawOverlays(Lnet.minecraft.client.Minecraft;II)V  # BrandonsCore-1.12.2-2.4.20.162-universal.jar
  1  mezz.jei.api.gui.IRecipeLayoutDrawable.drawRecipe(Lnet.minecraft.client.Minecraft;II)V  # BrandonsCore-1.12.2-2.4.20.162-universal.jar
  1  mezz.jei.api.ingredients.IIngredientHelper.cheatIngredient(Ljava.lang.Object;Z)Lnet.minecraft.item.ItemStack;  # BrandonsCore-1.12.2-2.4.20.162-universal.jar
  1  mezz.jei.api.ingredients.IIngredientRegistry.getIngredientHelper(Ljava.lang.Class;)Lmezz.jei.api.ingredients.IIngredientHelper;  # extrautils2-1.12-1.9.9.jar
  1  mezz.jei.api.ingredients.IIngredientRegistry.getIngredientHelper(Ljava.lang.Object;)Lmezz.jei.api.ingredients.IIngredientHelper;  # BrandonsCore-1.12.2-2.4.20.162-universal.jar
  1  mezz.jei.api.ingredients.IIngredientRegistry.getIngredientRenderer(Ljava.lang.Class;)Lmezz.jei.api.ingredients.IIngredientRenderer;  # extrautils2-1.12-1.9.9.jar
  1  mezz.jei.api.ingredients.IIngredientRegistry.getIngredientType(Ljava.lang.Class;)Lmezz.jei.api.recipe.IIngredientType;  # ElevatorMod-1.12.2-1.4.2.jar
  1  mezz.jei.api.recipe.transfer.IRecipeTransferInfo.requireCompleteSets()Z  # rtsbuilding-1.1.6-1.12.2-port.jar
  1  mezz.jei.bookmarks.BookmarkList.add(Ljava.lang.Object;)Z  # FTBLib-5.4.7.2.jar
  1  mezz.jei.collect.ListMultiMap.<init>()V  # TechReborn-1.12.2-2.27.3.1084-universal.jar
  1  mezz.jei.collect.ListMultiMap.put(Ljava.lang.Object;Ljava.lang.Object;)Z  # TechReborn-1.12.2-2.27.3.1084-universal.jar
  1  mezz.jei.collect.ListMultiMap.toImmutable()Lcom.google.common.collect.ImmutableMultimap;  # TechReborn-1.12.2-2.27.3.1084-universal.jar
  1  mezz.jei.collect.Table.<init>(Ljava.util.Map;Ljava.util.function.Supplier;)V  # JustEnoughEnergistics-1.12.2-1.0.8.jar
  1  mezz.jei.collect.Table.get(Ljava.lang.Object;Ljava.lang.Object;)Ljava.lang.Object;  # JustEnoughEnergistics-1.12.2-1.0.8.jar
  1  mezz.jei.collect.Table.getRow(Ljava.lang.Object;)Ljava.util.Map;  # JustEnoughEnergistics-1.12.2-1.0.8.jar
  1  mezz.jei.collect.Table.hashBasedTable()Lmezz.jei.collect.Table;  # JustEnoughEnergistics-1.12.2-1.0.8.jar
  1  mezz.jei.collect.Table.put(Ljava.lang.Object;Ljava.lang.Object;Ljava.lang.Object;)Ljava.lang.Object;  # JustEnoughEnergistics-1.12.2-1.0.8.jar
  1  mezz.jei.collect.Table.toImmutable()Lcom.google.common.collect.ImmutableTable;  # JustEnoughEnergistics-1.12.2-1.0.8.jar
  1  mezz.jei.config.Config.isOverlayEnabled()Z  # AE2WTLib-1.12.2-1.0.34.jar
  1  mezz.jei.config.KeyBindings.bookmark:Lnet.minecraft.client.settings.KeyBinding;  # FTBLib-5.4.7.2.jar
  1  mezz.jei.config.KeyBindings.showRecipe:Lnet.minecraft.client.settings.KeyBinding;  # FTBLib-5.4.7.2.jar
  1  mezz.jei.config.KeyBindings.showUses:Lnet.minecraft.client.settings.KeyBinding;  # FTBLib-5.4.7.2.jar
  1  mezz.jei.gui.TooltipRenderer.drawHoveringText(Lnet.minecraft.client.Minecraft;Ljava.lang.String;II)V  # TechReborn-1.12.2-2.27.3.1084-universal.jar
  1  mezz.jei.gui.TooltipRenderer.drawHoveringText(Lnet.minecraft.client.Minecraft;Ljava.util.List;IILnet.minecraft.client.gui.FontRenderer;)V  # IntegratedDynamics-1.12.2-1.1.11.jar
  1  mezz.jei.gui.elements.DrawableBlank.draw(Lnet.minecraft.client.Minecraft;II)V  # gendustryjei-1.0.2.jar
  1  mezz.jei.gui.ingredients.GuiIngredient.<init>(IZLmezz.jei.api.ingredients.IIngredientRenderer;Lmezz.jei.api.ingredients.IIngredientHelper;Ljava.awt.Rectangle;III)V  # rechiseled-1.2.6b-forge-mc1.12.jar
  1  mezz.jei.gui.ingredients.GuiIngredient.draw(Lnet.minecraft.client.Minecraft;II)V  # rechiseled-1.2.6b-forge-mc1.12.jar
  1  mezz.jei.gui.ingredients.GuiIngredient.drawHighlight(Lnet.minecraft.client.Minecraft;Ljava.awt.Color;II)V  # rechiseled-1.2.6b-forge-mc1.12.jar
  1  mezz.jei.gui.ingredients.GuiIngredient.getRect()Ljava.awt.Rectangle;  # rechiseled-1.2.6b-forge-mc1.12.jar
  1  mezz.jei.gui.ingredients.GuiItemStackGroup.<init>(Lmezz.jei.api.recipe.IFocus;I)V  # Quark-r1.6-179.jar
  1  mezz.jei.gui.overlay.GuiProperties.create(Lnet.minecraft.client.gui.inventory.GuiContainer;)Lmezz.jei.gui.overlay.GuiProperties;  # modularui-2.4.3.jar
  1  mezz.jei.gui.overlay.IngredientListOverlay.hasKeyboardFocus()Z  # moreoverlays-1.15.1-mc1.12.2.jar
  1  mezz.jei.gui.recipes.RecipeClickableArea.<init>(IIII[Ljava.lang.String;)V  # TechReborn-1.12.2-2.27.3.1084-universal.jar
  1  mezz.jei.gui.recipes.RecipeClickableArea.checkHover(II)Z  # TechReborn-1.12.2-2.27.3.1084-universal.jar
  1  mezz.jei.gui.recipes.RecipeClickableArea.getRecipeCategoryUids()Ljava.util.List;  # TechReborn-1.12.2-2.27.3.1084-universal.jar
  1  mezz.jei.gui.recipes.RecipesGui.getParentScreen()Lnet.minecraft.client.gui.GuiScreen;  # just-enough-harvestcraft-1.12.2-1.7.2.jar
  1  mezz.jei.gui.recipes.RecipesGui.onStateChange()V  # blockdrops-1.12.2-1.4.0.jar
  1  mezz.jei.ingredients.IngredientListElement.create(Ljava.lang.Object;Lmezz.jei.api.ingredients.IIngredientHelper;Lmezz.jei.api.ingredients.IIngredientRenderer;Lmezz.jei.startup.IModIdHelper;I)Lmezz.jei.ingredients.IngredientListElement;  # curvy_pipes-1.12.2-1.15.8.jar
  1  mezz.jei.network.PacketIdServer.RECIPE_TRANSFER:Lmezz.jei.network.PacketIdServer;  # DankNull-1.12.2-1.7.101.jar
  1  mezz.jei.network.packets.PacketJei.<init>()V  # DankNull-1.12.2-1.7.101.jar
  1  mezz.jei.network.packets.PacketRecipeTransfer.<init>(Ljava.util.Map;Ljava.util.List;Ljava.util.List;IZZ)V  # NTM-CE-1.12.2-2.6.1.0.jar
  1  mezz.jei.plugins.jei.info.IngredientInfoRecipe.create(Lmezz.jei.api.IGuiHelper;Ljava.util.List;Lmezz.jei.api.recipe.IIngredientType;[Ljava.lang.String;)Ljava.util.List;  # MoreTweaker-1.0.19.jar
  1  mezz.jei.plugins.vanilla.anvil.AnvilRecipeWrapper.<init>(Ljava.util.List;Ljava.util.List;Ljava.util.List;)V  # LucraftCore-1.12.2-2.4.17.jar
  1  mezz.jei.plugins.vanilla.anvil.AnvilRecipeWrapper.drawInfo(Lnet.minecraft.client.Minecraft;IIII)V  # LucraftCore-1.12.2-2.4.17.jar
  1  mezz.jei.plugins.vanilla.crafting.ShapedOreRecipeWrapper.drawInfo(Lnet.minecraft.client.Minecraft;IIII)V  # TLS-2.3.2-1.12.2.jar
  1  mezz.jei.plugins.vanilla.crafting.ShapedRecipesWrapper.<init>(Lmezz.jei.api.IJeiHelpers;Lnet.minecraft.item.crafting.ShapedRecipes;)V  # TechReborn-1.12.2-2.27.3.1084-universal.jar
  1  mezz.jei.plugins.vanilla.ingredients.fluid.FluidStackHelper.<init>()V  # curvy_pipes-1.12.2-1.15.8.jar
  1  mezz.jei.recipes.BrokenCraftingRecipeException.<init>(Ljava.lang.String;Ljava.lang.Throwable;)V  # LucraftCore-1.12.2-2.4.17.jar
  1  mezz.jei.recipes.RecipeRegistry.addRecipe(Lmezz.jei.api.recipe.IRecipeWrapper;Ljava.lang.String;)V  # The-Hordes-1.12.2-1.6.3e.jar
  1  mezz.jei.recipes.RecipeRegistry.removeRecipe(Lmezz.jei.api.recipe.IRecipeWrapper;Ljava.lang.String;)V  # The-Hordes-1.12.2-1.6.3e.jar
  1  mezz.jei.recipes.RecipeTransferRegistry.getRecipeTransferHandlers()Lcom.google.common.collect.ImmutableTable;  # JustEnoughEnergistics-1.12.2-1.0.8.jar
  1  mezz.jei.render.IngredientRenderer.<init>(Lmezz.jei.gui.ingredients.IIngredientListElement;)V  # curvy_pipes-1.12.2-1.15.8.jar
  1  mezz.jei.render.IngredientRenderer.drawTooltip(Lnet.minecraft.client.Minecraft;II)V  # curvy_pipes-1.12.2-1.15.8.jar
  1  mezz.jei.startup.ForgeModIdHelper.getInstance()Lmezz.jei.startup.IModIdHelper;  # curvy_pipes-1.12.2-1.15.8.jar
  1  mezz.jei.startup.StackHelper.<init>(Lmezz.jei.api.ISubtypeRegistry;)V  # arcaneworld-1.12.2-0.0.11.jar
  1  mezz.jei.transfer.RecipeTransferErrorInternal.INSTANCE:Lmezz.jei.transfer.RecipeTransferErrorInternal;  # JustEnoughEnergistics-1.12.2-1.0.8.jar
  1  mezz.jei.transfer.RecipeTransferErrorTooltip.<init>(Ljava.lang.String;)V  # JustEnoughEnergistics-1.12.2-1.0.8.jar
  1  mezz.jei.util.ErrorUtil.checkNotEmpty([Ljava.lang.Object;Ljava.lang.String;)V  # TechReborn-1.12.2-2.27.3.1084-universal.jar
  1  mezz.jei.util.ErrorUtil.checkNotNull(Ljava.lang.Object;Ljava.lang.String;)Ljava.lang.Object;  # TechReborn-1.12.2-2.27.3.1084-universal.jar
  1  mezz.jei.util.ErrorUtil.getInfoFromBrokenCraftingRecipe(Ljava.lang.Object;Ljava.util.List;Lnet.minecraft.item.ItemStack;)Ljava.lang.String;  # LucraftCore-1.12.2-2.4.17.jar
```

## No faithful implementation possible

```
  5  net.minecraft.realms.RealmsBridge.<init>()V  # Realms was removed from the host game; nothing to bridge
  5  net.minecraft.realms.RealmsBridge.switchToRealms(Lnet.minecraft.client.gui.GuiScreen;)V  # Realms was removed from the host game; nothing to bridge
  5  net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer.<init>()V  # FML class-loader internals; no class loader of that kind exists on NeoForge
  4  net.minecraftforge.fml.common.asm.transformers.AccessTransformer.<init>(Ljava.lang.String;)V  # FML class-loader internals (the layer applies access transformers itself)
  3  net.minecraftforge.fml.common.FMLModContainer.<init>(Ljava.lang.String;Lnet.minecraftforge.fml.common.discovery.ModCandidate;Ljava.util.Map;)V -> ()V  # FML internals
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
