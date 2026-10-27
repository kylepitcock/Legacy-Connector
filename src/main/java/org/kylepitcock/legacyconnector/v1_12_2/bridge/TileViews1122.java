package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityLockableLoot;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Legacy views of the game's own block entities, so 1.12.2 code that asks a world for the tile entity at a chest, furnace, sign or
 * spawner gets the 1.12 class ({@code TileEntityChest}, {@code TileEntityFurnace}, ...) reading and writing the modern one. One view per
 * block entity, refreshed each time it is handed out. Any other block entity with an inventory is a plain lockable inventory view.
 */
public final class TileViews1122 {
    private static final Map<BlockEntity, TileEntity> VIEWS = new WeakHashMap<>();

    private TileViews1122() {}

    /** The legacy view of a game block entity (null for one no legacy class stands for). */
    public static TileEntity view(World world, BlockEntity be) {
        if (be == null) return null;
        TileEntity view;
        synchronized (VIEWS) {
            view = VIEWS.computeIfAbsent(be, TileViews1122::create);
        }
        if (view == null) return null;
        var p = be.getBlockPos();
        view.mcc$bind(world, new BlockPos(p.getX(), p.getY(), p.getZ()), be);
        refresh(view);
        return view;
    }

    private static void refresh(TileEntity view) {
        try {
            if (view instanceof legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityChest c) c.mcc$refresh();
            else if (view instanceof legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityFurnace f) f.mcc$refresh();
            else if (view instanceof Refreshable r) r.mcc$refresh();
        } catch (RuntimeException e) {
            // a view shows what it last read
        }
    }

    /** A view that reads state from its block entity when handed out. */
    public interface Refreshable {
        void mcc$refresh();
    }

    private static TileEntity create(BlockEntity be) {
        try {
            for (var factory : FACTORIES) {
                TileEntity t = factory.apply(be);
                if (t != null) return t;
            }
            if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityChest(chest);
            }
            if (be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
                return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityFurnace(furnace);
            }
            if (be instanceof net.minecraft.world.Container c) return new InventoryView(c);
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/TileEntities").debug("No legacy view of {}: {}", be.getClass().getName(), e.toString());
        }
        return null;
    }

    /** Views of further block entity kinds, tried in order before the built-in ones. */
    private static final java.util.List<java.util.function.Function<BlockEntity, TileEntity>> FACTORIES = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addFactory(java.util.function.Function<BlockEntity, TileEntity> factory) {
        FACTORIES.add(factory);
    }

    /** A game block entity with an inventory (a barrel, a brewing stand, a mod's modern container) as a legacy inventory tile entity. */
    public static final class InventoryView extends TileEntityLockableLoot {
        private final net.minecraft.world.Container modern;
        private final NonNullList<ItemStack> items;

        InventoryView(net.minecraft.world.Container modern) {
            this.modern = modern;
            this.items = new ModernStackList1122(modern);
        }

        @Override
        protected NonNullList<ItemStack> getItems() {
            return items;
        }

        @Override
        public int getSizeInventory() {
            return modern.getContainerSize();
        }

        @Override
        public int getInventoryStackLimit() {
            return modern.getMaxStackSize();
        }

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return modern.canPlaceItem(index, Stacks1122.toModern(stack));
        }

        @Override
        public String getName() {
            return modern instanceof net.minecraft.world.Nameable n ? n.getName().getString() : "container";
        }

        @Override
        public boolean hasCustomName() {
            return modern instanceof net.minecraft.world.Nameable n && n.hasCustomName();
        }

        @Override
        public void markDirty() {
            modern.setChanged();
        }

        @Override
        public String getGuiID() {
            return "minecraft:container";
        }

        @Override
        public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
            return null;
        }
    }
}
