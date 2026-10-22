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
    }

    private static void addLines(ITooltip tooltip, List<String> lines) {
        for (String line : lines) {
            if (line == null || line.isEmpty()) continue;
            // Waila layout markers (tabs, alignment, icons) have no Jade equivalent; drop them, keep the text.
            String text = line.replaceAll("¤¤[a-e]", " ").replaceAll("¤¥[a-c]", "").replaceAll("¤¤\\{[^}]*}", "");
            if (!text.isBlank()) tooltip.add(Component.literal(text));
        }
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
            List<String> lines = new ArrayList<>();
            try {
                for (var r : WailaBridge.INSTANCE.head) if (matches(r.target(), block, te)) lines = nonNull(r.provider().getWailaHead(stack, new ArrayList<>(), legacy, WailaBridge.INSTANCE));
                List<String> body = new ArrayList<>();
                for (var r : WailaBridge.INSTANCE.body) if (matches(r.target(), block, te)) body = nonNull(call(r.provider(), stack, body, legacy));
                lines.addAll(body);
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
            NBTTagCompound tag = new NBTTagCompound();
            if (te != null) te.writeToNBT(tag);
            return tag;
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
