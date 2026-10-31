package org.kylepitcock.legacyconnector.api.internal;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.energy.CapabilityEnergy;
import legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EnergyBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FluidBridge;
import org.kylepitcock.legacyconnector.v1_7_10.gui.LegacyInventoryContainer;

/** The adapters Legacy Connector itself uses (and exposes as constants of {@code LegacyCapabilities}). */
public final class BuiltinAdapters {
    private BuiltinAdapters() {}

    // ------------------------------------------------------------------ 1.7.10

    public static IItemHandler inventory1710(Object tile, Direction side, BlockEntity host) {
        if (!(tile instanceof legacyconnector.v1_7_10.net.minecraft.inventory.IInventory inv)) return null;
        var container = LegacyInventoryContainer.of(inv);
        if (side != null && inv instanceof legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory) return new SidedInvWrapper(container, side);
        return new InvWrapper(container);
    }

    public static IFluidHandler fluids1710(Object tile, Direction side, BlockEntity host) {
        return FluidBridge.forTile(tile, side);
    }

    public static IEnergyStorage rf1710(Object tile, Direction side, BlockEntity host) {
        return tile instanceof legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity t ? EnergyBridge.forTile(t, side) : null;
    }

    // ------------------------------------------------------------------ 1.12.2

    public static IItemHandler items1122(Object tile, Direction side, BlockEntity host) {
        if (!(tile instanceof legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity t)) return null;
        EnumFacing facing = side == null ? null : EnumFacing.mcc$of(side);
        var handler = t.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
        return handler == null ? null : new ItemHandler1122(handler);
    }

    public static IEnergyStorage energy1122(Object tile, Direction side, BlockEntity host) {
        if (!(tile instanceof legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity t)) return null;
        EnumFacing facing = side == null ? null : EnumFacing.mcc$of(side);
        var storage = t.getCapability(CapabilityEnergy.ENERGY, facing);
        return storage == null ? null : new Energy1122(storage);
    }

    /** A Forge 1.12 item handler seen as NeoForge's; stacks are converted on every call (the legacy side owns the real ones). */
    private record ItemHandler1122(legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler handler) implements IItemHandler {
        @Override
        public int getSlots() {
            return handler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return Stacks1122.toModern(handler.getStackInSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            var legacy = Stacks1122.fromModern(stack);
            // a stack without a 1.12 equivalent is not accepted (an empty legacy stack would read as "all inserted")
            if (legacy.isEmpty() && !stack.isEmpty()) return stack;
            return Stacks1122.toModern(handler.insertItem(slot, legacy, simulate));
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return Stacks1122.toModern(handler.extractItem(slot, amount, simulate));
        }

        @Override
        public int getSlotLimit(int slot) {
            return handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            var legacy = Stacks1122.fromModern(stack);
            return !legacy.isEmpty() && handler.isItemValid(slot, legacy);
        }
    }

    private record Energy1122(legacyconnector.v1_12_2.net.minecraftforge.energy.IEnergyStorage storage) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            return storage.receiveEnergy(amount, simulate);
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return storage.extractEnergy(amount, simulate);
        }

        @Override
        public int getEnergyStored() {
            return storage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return storage.canExtract();
        }

        @Override
        public boolean canReceive() {
            return storage.canReceive();
        }
    }
}
