package org.kylepitcock.legacyconnector.compat.jade;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaDataAccessor;
import legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaDataProvider;
import legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaEntityAccessor;
import legacyconnector.v1_7_10.mcp.mobius.waila.api.IWailaEntityProvider;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.compat.waila.WailaBridge;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Waila -> Jade bridge, Jade half: one block and one entity component that run the legacy Waila providers matching
 * the looked-at legacy block/tile entity/entity and add their lines to Jade's tooltip. Loaded only by Jade.
 */
@WailaPlugin
public final class LegacyConnectorJadePlugin implements IWailaPlugin {
    private static final ResourceLocation BLOCKS = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_waila_blocks");
    private static final ResourceLocation ENTITIES = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_waila_entities");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new Blocks(), net.minecraft.world.level.block.Block.class);
        registration.registerEntityComponent(new Entities(), net.minecraft.world.entity.Entity.class);
        LegacyJade1122.registerClient(registration);
    }

    @Override
    public void register(snownee.jade.api.IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new BlockData(), net.minecraft.world.level.block.entity.BlockEntity.class);
        LegacyJade1122.registerCommon(registration);
    }

    private static final String DATA_KEY = "mcc_waila_1710";
    private static final ResourceLocation BLOCK_DATA = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_waila_block_data");

    /** Waila's synced tile data: the legacy NBT providers (machine progress, owner, ...) run on the server. */
    private static final class BlockData implements snownee.jade.api.IServerDataProvider<BlockAccessor> {
        @Override
        public void appendServerData(net.minecraft.nbt.CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof LegacyBlockEntity be)) return;
            TileEntity te = be.legacy();
            if (te == null) return;
            Block block = BlockBridge.legacyBlock(accessor.getBlockState());
            try {
                NBTTagCompound tag = new NBTTagCompound();
                te.writeToNBT(tag);
                World world = World.mcc$wrap(accessor.getLevel());
                legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP player =
                        EntityBridge.wrap(accessor.getPlayer()) instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP p ? p : null;
                var pos = accessor.getPosition();
                for (var r : WailaBridge.INSTANCE.nbt) {
                    if (!matches(r, block, te)) continue;
                    NBTTagCompound out = r.provider().getNBTData(player, te, tag, world, pos.getX(), pos.getY(), pos.getZ());
                    if (out != null) tag = out;
                }
                data.put(DATA_KEY, tag.mcc$modern());
            } catch (RuntimeException | LinkageError e) {
                // a broken legacy provider sends nothing
            }
        }

        @Override
        public boolean shouldRequestData(BlockAccessor accessor) {
            if (WailaBridge.INSTANCE.nbt.isEmpty() || !(accessor.getBlockEntity() instanceof LegacyBlockEntity be)) return false;
            TileEntity te = be.legacy();
            Block block = BlockBridge.legacyBlock(accessor.getBlockState());
            for (var r : WailaBridge.INSTANCE.nbt) if (matches(r, block, te)) return true;
            return false;
        }

        @Override
        public ResourceLocation getUid() {
            return BLOCK_DATA;
        }
    }

    /** Appends one line to Jade's tooltip. */
    static void addLine(ITooltip tooltip, String line) {
        // Waila layout markers (tabs, alignment, icons) have no Jade equivalent; drop them, keep the text.
        String text = line.replaceAll("¤¤[a-e]", " ").replaceAll("¤¥[a-c]", "").replaceAll("¤¤\\{[^}]*}", "");
        if (!text.isBlank()) tooltip.add(Component.literal(text));
    }

    private static void addLines(ITooltip tooltip, List<String> lines) {
        for (String line : lines) {
            if (line == null || line.isEmpty()) continue;
            addLine(tooltip, line);
        }
    }

    private static boolean matches(WailaBridge.Registration<?> r, Object block, Object te) {
        if (r.only() != null) return r.only() == block;
        return r.target() != null && (r.target().isInstance(block) || r.target().isInstance(te));
    }

    private static final class Blocks implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (WailaBridge.INSTANCE.isEmpty()) return;
            Block block = BlockBridge.legacyBlock(accessor.getBlockState());
            TileEntity te = accessor.getBlockEntity() instanceof LegacyBlockEntity be ? be.legacy() : null;
            if (block == null && te == null) return;
            Accessor legacy = new Accessor(accessor, block, te);
            ItemStack stack = legacy.getStack();
            var reg = WailaBridge.INSTANCE;
            List<String> lines = new ArrayList<>();
            try {
                for (var r : reg.stack) {
                    if (!matches(r, block, te)) continue;
                    ItemStack override = r.provider().getWailaStack(legacy, reg);
                    if (override != null) stack = override;
                }
                String name = stack == null ? "" : stack.getDisplayName();
                // Waila's head already holds the block's name; Jade shows its own, so an unchanged first line is dropped
                List<String> head = new legacyconnector.v1_7_10.mcp.mobius.waila.api.impl.TipList<>();
                head.add(name);
                for (var r : reg.head) if (matches(r, block, te)) head = nonNull(r.provider().getWailaHead(stack, head, legacy, reg));
                if (!head.isEmpty() && name.equals(head.get(0))) head = new ArrayList<>(head.subList(1, head.size()));
                List<String> body = new legacyconnector.v1_7_10.mcp.mobius.waila.api.impl.TipList<>();
                for (var r : reg.body) if (matches(r, block, te)) body = nonNull(call(r.provider(), stack, body, legacy));
                List<String> tail = new legacyconnector.v1_7_10.mcp.mobius.waila.api.impl.TipList<>();
                for (var r : reg.tail) if (matches(r, block, te)) tail = nonNull(r.provider().getWailaTail(stack, tail, legacy, reg));
                lines.addAll(head);
                lines.addAll(body);
                lines.addAll(tail);
            } catch (RuntimeException | LinkageError e) {
                return; // a broken legacy provider must not break Jade
            }
            addLines(tooltip, lines);
        }

        private static List<String> call(IWailaDataProvider p, ItemStack stack, List<String> tip, Accessor a) {
            return p.getWailaBody(stack, tip, a, WailaBridge.INSTANCE);
        }

        @Override
        public ResourceLocation getUid() {
            return BLOCKS;
        }
    }

    private static final class Entities implements IEntityComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (WailaBridge.INSTANCE.entityBody.isEmpty()) return;
            Entity entity = EntityBridge.wrap(accessor.getEntity());
            if (entity == null) return;
            IWailaEntityAccessor legacy = new EntityAccessorView(accessor, entity);
            List<String> lines = new ArrayList<>();
            try {
                for (var r : WailaBridge.INSTANCE.entityBody) {
                    if (r.target().isInstance(entity)) lines = nonNull(r.provider().getWailaBody(entity, lines, legacy, WailaBridge.INSTANCE));
                }
            } catch (RuntimeException | LinkageError e) {
                return;
            }
            addLines(tooltip, lines);
        }

        @Override
        public ResourceLocation getUid() {
            return ENTITIES;
        }
    }

    private static boolean matches(Class<?> target, Block block, TileEntity te) {
        return target != null && (target.isInstance(block) || target.isInstance(te));
    }

    private static List<String> nonNull(List<String> l) {
        return l == null ? new ArrayList<>() : l;
    }

    /** Legacy view of a Jade block accessor. */
    private record Accessor(BlockAccessor jade, Block block, TileEntity te) implements IWailaDataAccessor {
        @Override
        public World getWorld() {
            return World.mcc$wrap(jade.getLevel());
        }

        @Override
        public EntityPlayer getPlayer() {
            return EntityBridge.wrap(jade.getPlayer()) instanceof EntityPlayer p ? p : null;
        }

        @Override
        public Block getBlock() {
            return block;
        }

        @Override
        public int getBlockID() {
            return block == null ? 0 : Block.getIdFromBlock(block);
        }

        @Override
        public String getBlockQualifiedName() {
            return block == null ? "" : Block.blockRegistry.getNameForObject(block);
        }

        @Override
        public int getMetadata() {
            return BlockBridge.legacyMeta(jade.getBlockState());
        }

        @Override
        public TileEntity getTileEntity() {
            return te;
        }

        @Override
        public NBTTagCompound getNBTData() {
            var data = jade.getServerData();
            if (data != null && data.contains(DATA_KEY)) return new NBTTagCompound(data.getCompound(DATA_KEY));
            NBTTagCompound tag = new NBTTagCompound();
            if (te != null) te.writeToNBT(tag);
            return tag;
        }

        @Override
        public legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition getPosition() {
            var p = jade.getPosition();
            return new legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition(p.getX(), p.getY(), p.getZ(), jade.getSide().get3DDataValue(),
                    legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(jade.getHitResult().getLocation().x, jade.getHitResult().getLocation().y,
                            jade.getHitResult().getLocation().z));
        }

        @Override
        public int getNBTInteger(NBTTagCompound tag, String key) {
            return tag == null ? 0 : tag.getInteger(key);
        }

        @Override
        public double getPartialFrame() {
            return 0;
        }

        @Override
        public ForgeDirection getSide() {
            return ForgeDirection.getOrientation(jade.getSide().get3DDataValue());
        }

        @Override
        public ItemStack getStack() {
            return ItemStack.mcc$wrap(jade.getPickedResult());
        }
    }

    private record EntityAccessorView(EntityAccessor jade, Entity entity) implements IWailaEntityAccessor {
        @Override
        public World getWorld() {
            return World.mcc$wrap(jade.getLevel());
        }

        @Override
        public EntityPlayer getPlayer() {
            return EntityBridge.wrap(jade.getPlayer()) instanceof EntityPlayer p ? p : null;
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        @Override
        public NBTTagCompound getNBTData() {
            return new NBTTagCompound();
        }

        @Override
        public int getNBTInteger(NBTTagCompound tag, String key) {
            return tag == null ? 0 : tag.getInteger(key);
        }

        @Override
        public double getPartialFrame() {
            return 0;
        }
    }
}
