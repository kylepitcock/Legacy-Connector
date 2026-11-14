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

    static {
        // the views hold their block entity (and through it its level): dropped when the game is done with it
        if (org.kylepitcock.legacyconnector.core.ViewLiveness.sweepDeadViews()) {
            org.kylepitcock.legacyconnector.core.ViewSweeper.register("1.12.2 game block entity", VIEWS,
                    org.kylepitcock.legacyconnector.core.ViewLiveness::blockEntityGone);
        }
    }

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
            if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity s) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntitySign(s);
            if (be instanceof net.minecraft.world.level.block.entity.SkullBlockEntity s) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntitySkull(s);
            if (be instanceof net.minecraft.world.level.block.entity.BannerBlockEntity b) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityBanner(b);
            if (be instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity s) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityMobSpawner(s);
            if (be instanceof net.minecraft.world.level.block.entity.HopperBlockEntity h) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityHopper(h);
            if (be instanceof net.minecraft.world.level.block.entity.DropperBlockEntity d) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityDropper(d);
            if (be instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity d) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityDispenser(d);
            if (be instanceof net.minecraft.world.level.block.entity.BedBlockEntity b) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityBed(b);
            if (be instanceof net.minecraft.world.level.block.piston.PistonMovingBlockEntity p) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityPiston(p);
            if (be instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity s) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityShulkerBox(s);
            if (be instanceof net.minecraft.world.level.block.entity.ComparatorBlockEntity c) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityComparator(c);
            if (be instanceof net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity g) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityEndGateway(g);
            if (be instanceof net.minecraft.world.level.block.entity.TheEndPortalBlockEntity) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityEndPortal();
            if (be instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity b) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityBeacon(b);
            if (be instanceof net.minecraft.world.level.block.entity.CommandBlockEntity c) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityCommandBlock(c);
            if (be instanceof net.minecraft.world.level.block.entity.StructureBlockEntity s) return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityStructure(s);
            if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityChest(chest);
            }
            if (be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
                return new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityFurnace(furnace);
            }
            if (be instanceof net.minecraft.world.Container c) return new InventoryView(c);
            // a modern chest or machine that is no Container but offers an item handler: the view answers the item handler capability
            if (ModernItems1122.offers(be)) return new ItemsTile(be);
            // a modern machine without an inventory but with the energy capability: legacy generators and cables push into it
            if (ModernEnergy1122.offers(be)) return new ModernEnergy1122.EnergyTile(be);
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

    /** A modern block entity with an item handler capability and no container, as a legacy tile entity answering the item handler capability. */
    public static final class ItemsTile extends TileEntity {
        private final BlockEntity modern;

        ItemsTile(BlockEntity modern) {
            this.modern = modern;
        }

        @Override
        public boolean hasCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<?> capability,
                legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
            if (capability == legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return ModernItems1122.modern(modern, facing) != null;
            if (capability == legacyconnector.v1_12_2.net.minecraftforge.energy.CapabilityEnergy.ENERGY) return ModernEnergy1122.modern(modern, facing) != null;
            return super.hasCapability(capability, facing);
        }

        @Override
        public <T> T getCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<T> capability,
                legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
            T items = ModernItems1122.capability(modern, capability, facing);
            if (items != null) return items;
            T energy = ModernEnergy1122.capability(modern, capability, facing);
            return energy != null ? energy : super.getCapability(capability, facing);
        }
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
        public boolean hasCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<?> capability,
                legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
            if (modern instanceof BlockEntity be) {
                if (capability == legacyconnector.v1_12_2.net.minecraftforge.energy.CapabilityEnergy.ENERGY) return ModernEnergy1122.modern(be, facing) != null;
                if (capability == legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
                    return ModernItems1122.modern(be, facing) != null || super.hasCapability(capability, facing);
                }
            }
            return super.hasCapability(capability, facing);
        }

        /** A modern machine with an inventory and the energy capability answers Forge Energy. */
        @Override
        public <T> T getCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<T> capability,
                legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
            if (modern instanceof BlockEntity be) {
                T energy = ModernEnergy1122.capability(be, capability, facing);
                if (energy != null) return energy;
                // the block entity's own handler of the side (a barrel, a modern mod's chest), else the whole inventory
                T items = ModernItems1122.capability(be, capability, facing);
                if (items != null) return items;
            }
            return super.getCapability(capability, facing);
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
