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

    // ------------------------------------------------------------------ Beta 1.7.3

    /** A Beta {@code IInventory} tile as an item handler (Beta has no sided inventories: the side is ignored). */
    public static IItemHandler inventoryB173(Object tile, Direction side, BlockEntity host) {
        if (!(tile instanceof legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory inv)) return null;
        return new InvWrapper(org.kylepitcock.legacyconnector.vb1_7_3.LegacyInventoryB173.of(inv));
    }

    // ------------------------------------------------------------------ 1.12.2

    public static IItemHandler items1122(Object tile, Direction side, BlockEntity host) {
        if (!(tile instanceof legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity t)) return null;
        EnumFacing facing = side == null ? null : EnumFacing.mcc$of(side);
        var handler = t.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
        return handler == null ? null : new ItemHandler1122(handler);
    }

    public static IFluidHandler fluids1122(Object tile, Direction side, BlockEntity host) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.FluidBridge1122.forTile(tile, side);
    }

    /** A Forge 1.12 item handler as NeoForge's (used by the item capabilities of 1.12.2 items). */
    public static IItemHandler wrapItems1122(legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler handler) {
        return new ItemHandler1122(handler);
    }

    public static IEnergyStorage energy1122(Object tile, Direction side, BlockEntity host) {
        if (!(tile instanceof legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity t)) return null;
        EnumFacing facing = side == null ? null : EnumFacing.mcc$of(side);
        var storage = t.getCapability(CapabilityEnergy.ENERGY, facing);
        if (storage == null && side == null) {
            // a sideless query of a tile that only answers per side: the first side that has one
            for (EnumFacing f : EnumFacing.values()) {
                storage = t.getCapability(CapabilityEnergy.ENERGY, f);
                if (storage != null) break;
            }
        }
        return storage == null ? null : new Energy1122(storage);
    }

    public static IEnergyStorage rf1122(Object tile, Direction side, BlockEntity host) {
        return tile instanceof legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity t
                ? org.kylepitcock.legacyconnector.v1_12_2.bridge.EnergyBridge1122.forTile(t, side) : null;
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

    /** A transfer result held to NeoForge's contract: between 0 and what was asked (a legacy mod may report more, or a negative number). */
    public static int clampTransfer(int requested, int moved) {
        return requested <= 0 ? 0 : Math.max(0, Math.min(requested, moved));
    }

    private record Energy1122(legacyconnector.v1_12_2.net.minecraftforge.energy.IEnergyStorage storage) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if (amount <= 0) return 0;
            try {
                return clampTransfer(amount, storage.receiveEnergy(amount, simulate));
            } catch (NullPointerException e) {
                return 0;   // a mod that cannot answer a sideless query (the facing is null)
            }
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            if (amount <= 0) return 0;
            try {
                return clampTransfer(amount, storage.extractEnergy(amount, simulate));
            } catch (NullPointerException e) {
                return 0;
            }
        }

        @Override
        public int getEnergyStored() {
            try {
                return Math.max(0, storage.getEnergyStored());
            } catch (NullPointerException e) {
                return 0;
            }
        }

        @Override
        public int getMaxEnergyStored() {
            try {
                return Math.max(0, storage.getMaxEnergyStored());
            } catch (NullPointerException e) {
                return 0;
            }
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
