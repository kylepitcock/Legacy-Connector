package org.kylepitcock.legacyconnector.compat.jade;

import java.util.List;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.IWailaDataAccessor;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.IWailaEntityAccessor;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.impl.ModuleRegistrar;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.impl.TipList;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.config.IPluginConfig;

/**
 * Waila (HWYLA) -> Jade bridge for 1.12.2 mods, Jade half (see {@link LegacyConnectorJadePlugin}): the providers the mods' {@code @WailaPlugin}
 * classes registered in {@link ModuleRegistrar} add their lines to Jade's tooltip, and their NBT providers sync server data to the client.
 */
final class LegacyJade1122 {
    private static final ResourceLocation BLOCKS = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_waila_blocks_1122");
    private static final ResourceLocation ENTITIES = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_waila_entities_1122");
    private static final ResourceLocation BLOCK_DATA = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_waila_block_data_1122");
    private static final ResourceLocation ENTITY_DATA = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_waila_entity_data_1122");
    /** Key of the synced legacy NBT in Jade's server data. */
    private static final String DATA_KEY = "mcc_waila_1122";

    private LegacyJade1122() {}

    static void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new Blocks(), net.minecraft.world.level.block.Block.class);
        registration.registerEntityComponent(new Entities(), net.minecraft.world.entity.Entity.class);
    }

    static void registerCommon(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new BlockData(), net.minecraft.world.level.block.entity.BlockEntity.class);
        registration.registerEntityDataProvider(new EntityData(), net.minecraft.world.entity.Entity.class);
    }

    private static void addLines(ITooltip tooltip, List<String> lines, String skipFirst) {
        boolean first = true;
        for (String line : lines) {
            boolean seed = first && line != null && line.equals(skipFirst);
            first = false;
            if (seed || line == null || line.isEmpty()) continue;
            LegacyConnectorJadePlugin.addLine(tooltip, line);
        }
    }

    private static boolean matches(Class<?> target, Object block, Object te) {
        return target != null && (target.isInstance(block) || target.isInstance(te));
    }

    private static List<String> nonNull(List<String> l, List<String> fallback) {
        return l == null ? fallback : l;
    }

    private static final class Blocks implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ModuleRegistrar reg = ModuleRegistrar.instance();
            if (reg.isEmpty()) return;
            Block block = StatesBridge1122.legacyBlock(accessor.getBlockState());
            TileEntity te = accessor.getBlockEntity() instanceof Legacy1122BlockEntity be ? be.legacy() : null;
            if (block == null && te == null) return;
            Accessor legacy = new Accessor(accessor, block, te);
            try {
                ItemStack stack = legacy.getStack();
                for (var r : reg.stack) {
                    if (!matches(r.target(), block, te)) continue;
                    ItemStack override = r.provider().getWailaStack(legacy, reg);
                    if (override != null) stack = override;
                }
                String name = stack == null ? "" : stack.getDisplayName();
                List<String> head = new TipList<>();
                head.add(name);
                for (var r : reg.head) if (matches(r.target(), block, te)) head = nonNull(r.provider().getWailaHead(stack, head, legacy, reg), head);
                List<String> body = new TipList<>();
                for (var r : reg.body) if (matches(r.target(), block, te)) body = nonNull(r.provider().getWailaBody(stack, body, legacy, reg), body);
                List<String> tail = new TipList<>();
                for (var r : reg.tail) if (matches(r.target(), block, te)) tail = nonNull(r.provider().getWailaTail(stack, tail, legacy, reg), tail);
                addLines(tooltip, head, name);
                addLines(tooltip, body, null);
                addLines(tooltip, tail, null);
            } catch (RuntimeException | LinkageError e) {
                // a broken legacy provider must not break Jade
            }
        }

        @Override
        public ResourceLocation getUid() {
            return BLOCKS;
        }
    }

    private static final class Entities implements IEntityComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            ModuleRegistrar reg = ModuleRegistrar.instance();
            if (reg.entityHead.isEmpty() && reg.entityBody.isEmpty() && reg.entityTail.isEmpty()) return;
            Entity entity = EntityBridge1122.wrap(accessor.getEntity());
            if (entity == null) return;
            IWailaEntityAccessor legacy = new EntityAccessorView(accessor, entity);
            try {
                String name = String.valueOf(entity.getName());
                List<String> head = new TipList<>();
                head.add(name);
                for (var r : reg.entityHead) if (r.target().isInstance(entity)) head = nonNull(r.provider().getWailaHead(entity, head, legacy, reg), head);
                List<String> body = new TipList<>();
                for (var r : reg.entityBody) if (r.target().isInstance(entity)) body = nonNull(r.provider().getWailaBody(entity, body, legacy, reg), body);
                List<String> tail = new TipList<>();
                for (var r : reg.entityTail) if (r.target().isInstance(entity)) tail = nonNull(r.provider().getWailaTail(entity, tail, legacy, reg), tail);
                addLines(tooltip, head, name);
                addLines(tooltip, body, null);
                addLines(tooltip, tail, null);
            } catch (RuntimeException | LinkageError e) {
                // a broken legacy provider must not break Jade
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ENTITIES;
        }
    }

    /** Server side: the legacy NBT providers add what the client cannot see (machine progress, owner, ...) to the data Jade sends. */
    private static final class BlockData implements IServerDataProvider<BlockAccessor> {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            ModuleRegistrar reg = ModuleRegistrar.instance();
            if (!(accessor.getBlockEntity() instanceof Legacy1122BlockEntity be)) return;
            TileEntity te = be.legacy();
            if (te == null) return;
            Block block = StatesBridge1122.legacyBlock(accessor.getBlockState());
            try {
                NBTTagCompound tag = new NBTTagCompound();
                te.writeToNBT(tag);
                World world = WorldBridge1122.wrap(accessor.getLevel());
                EntityPlayerMP player = EntityBridge1122.wrap(accessor.getPlayer()) instanceof EntityPlayerMP p ? p : null;
                BlockPos pos = new BlockPos(accessor.getPosition().getX(), accessor.getPosition().getY(), accessor.getPosition().getZ());
                for (var r : reg.nbt) {
                    if (!matches(r.target(), block, te)) continue;
                    NBTTagCompound out = r.provider().getNBTData(player, te, tag, world, pos);
                    if (out != null) tag = out;
                }
                data.put(DATA_KEY, tag.mcc$modern());
            } catch (RuntimeException | LinkageError e) {
                // a broken legacy provider sends nothing
            }
        }

        @Override
        public boolean shouldRequestData(BlockAccessor accessor) {
            ModuleRegistrar reg = ModuleRegistrar.instance();
            if (reg.nbt.isEmpty() || !(accessor.getBlockEntity() instanceof Legacy1122BlockEntity be)) return false;
            TileEntity te = be.legacy();
            Block block = StatesBridge1122.legacyBlock(accessor.getBlockState());
            for (var r : reg.nbt) if (matches(r.target(), block, te)) return true;
            return false;
        }

        @Override
        public ResourceLocation getUid() {
            return BLOCK_DATA;
        }
    }

    private static final class EntityData implements IServerDataProvider<EntityAccessor> {
        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            ModuleRegistrar reg = ModuleRegistrar.instance();
            Entity entity = EntityBridge1122.wrap(accessor.getEntity());
            if (entity == null) return;
            try {
                NBTTagCompound tag = new NBTTagCompound();
                entity.writeToNBT(tag);
                World world = WorldBridge1122.wrap(accessor.getLevel());
                EntityPlayerMP player = EntityBridge1122.wrap(accessor.getPlayer()) instanceof EntityPlayerMP p ? p : null;
                for (var r : reg.entityNbt) {
                    if (!r.target().isInstance(entity)) continue;
                    NBTTagCompound out = r.provider().getNBTData(player, entity, tag, world);
                    if (out != null) tag = out;
                }
                data.put(DATA_KEY, tag.mcc$modern());
            } catch (RuntimeException | LinkageError e) {
                // a broken legacy provider sends nothing
            }
        }

        @Override
        public boolean shouldRequestData(EntityAccessor accessor) {
            ModuleRegistrar reg = ModuleRegistrar.instance();
            if (reg.entityNbt.isEmpty()) return false;
            Entity entity = EntityBridge1122.wrap(accessor.getEntity());
            for (var r : reg.entityNbt) if (r.target().isInstance(entity)) return true;
            return false;
        }

        @Override
        public ResourceLocation getUid() {
            return ENTITY_DATA;
        }
    }

    /** The synced tag when a legacy NBT provider produced one, else what the client-side object can write itself. */
    private static NBTTagCompound synced(CompoundTag serverData) {
        return serverData != null && serverData.contains(DATA_KEY) ? new NBTTagCompound(serverData.getCompound(DATA_KEY)) : null;
    }

    /** Legacy view of a Jade block accessor. */
    private record Accessor(BlockAccessor jade, Block block, TileEntity te) implements IWailaDataAccessor {
        @Override
        public World getWorld() {
            return WorldBridge1122.wrap(jade.getLevel());
        }

        @Override
        public EntityPlayer getPlayer() {
            return EntityBridge1122.wrap(jade.getPlayer()) instanceof EntityPlayer p ? p : null;
        }

        @Override
        public Block getBlock() {
            return block;
        }

        @Override
        public int getMetadata() {
            IBlockState state = getBlockState();
            return block == null || state == null ? 0 : block.getMetaFromState(state);
        }

        @Override
        public IBlockState getBlockState() {
            return StatesBridge1122.toLegacy(jade.getBlockState());
        }

        @Override
        public TileEntity getTileEntity() {
            return te;
        }

        @Override
        public RayTraceResult getMOP() {
            return RayTraceResult.mcc$of(jade.getHitResult());
        }

        @Override
        public BlockPos getPosition() {
            var p = jade.getPosition();
            return new BlockPos(p.getX(), p.getY(), p.getZ());
        }

        @Override
        public Vec3d getRenderingPosition() {
            var c = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            var p = jade.getPosition();
            return new Vec3d(p.getX() - c.x, p.getY() - c.y, p.getZ() - c.z);
        }

        @Override
        public NBTTagCompound getNBTData() {
            NBTTagCompound tag = synced(jade.getServerData());
            if (tag != null) return tag;
            tag = new NBTTagCompound();
            if (te != null) te.writeToNBT(tag);
            return tag;
        }

        @Override
        public int getNBTInteger(NBTTagCompound tag, String key) {
            return tag == null ? 0 : tag.getInteger(key);
        }

        @Override
        public double getPartialFrame() {
            return net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        }

        @Override
        public EnumFacing getSide() {
            return EnumFacing.byIndex(jade.getSide().get3DDataValue());
        }

        @Override
        public ItemStack getStack() {
            return Stacks1122.fromModern(jade.getPickedResult());
        }
    }

    private record EntityAccessorView(EntityAccessor jade, Entity entity) implements IWailaEntityAccessor {
        @Override
        public World getWorld() {
            return WorldBridge1122.wrap(jade.getLevel());
        }

        @Override
        public EntityPlayer getPlayer() {
            return EntityBridge1122.wrap(jade.getPlayer()) instanceof EntityPlayer p ? p : null;
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        @Override
        public RayTraceResult getMOP() {
            return RayTraceResult.mcc$of(jade.getHitResult());
        }

        @Override
        public Vec3d getRenderingPosition() {
            var c = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            var e = jade.getEntity();
            return new Vec3d(e.getX() - c.x, e.getY() - c.y, e.getZ() - c.z);
        }

        @Override
        public NBTTagCompound getNBTData() {
            NBTTagCompound tag = synced(jade.getServerData());
            if (tag != null) return tag;
            tag = new NBTTagCompound();
            entity.writeToNBT(tag);
            return tag;
        }

        @Override
        public int getNBTInteger(NBTTagCompound tag, String key) {
            return tag == null ? 0 : tag.getInteger(key);
        }

        @Override
        public double getPartialFrame() {
            return net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        }
    }
}
