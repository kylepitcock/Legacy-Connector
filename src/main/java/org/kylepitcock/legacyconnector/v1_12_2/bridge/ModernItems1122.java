package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler;
import legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * Modern block entities seen by 1.12.2 code: the NeoForge item handler capability of the position ({@code Capabilities.ItemHandler.BLOCK}) answers
 * Forge's {@code ITEM_HANDLER_CAPABILITY}, so transport pipes, quarries, hoppers of legacy mods find barrels and the chests of modern mods. Each call
 * asks the level again, so the handler is always the live one.
 */
public final class ModernItems1122 {
    private ModernItems1122() {}

    public static net.neoforged.neoforge.items.IItemHandler modern(BlockEntity be, EnumFacing side) {
        Level level = be.getLevel();
        if (level == null || be.isRemoved()) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, be.getBlockPos(), side == null ? null : side.mcc$modern());
    }

    /** Whether the block entity has an item handler on some side (or sideless). */
    public static boolean offers(BlockEntity be) {
        if (modern(be, null) != null) return true;
        for (EnumFacing f : EnumFacing.values()) if (modern(be, f) != null) return true;
        return false;
    }

    /** The answer to {@code getCapability} for the item handler capability, null for any other capability or none offered. */
    @SuppressWarnings("unchecked")
    public static <T> T capability(BlockEntity be, Capability<T> capability, EnumFacing side) {
        if (capability == null || capability != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return null;
        return modern(be, side) == null ? null : (T) new Legacy(be, side);
    }

    private record Legacy(BlockEntity be, EnumFacing side) implements IItemHandler {
        private net.neoforged.neoforge.items.IItemHandler live() {
            var h = modern(be, side);
            return h == null ? new net.neoforged.neoforge.items.ItemStackHandler(0) : h;
        }

        @Override
        public int getSlots() {
            return live().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return Stacks1122.fromModern(live().getStackInSlot(slot));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            var rest = live().insertItem(slot, Stacks1122.toModern(stack), simulate);
            return Stacks1122.fromModern(rest);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return Stacks1122.fromModern(live().extractItem(slot, amount, simulate));
        }

        @Override
        public int getSlotLimit(int slot) {
            return live().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return live().isItemValid(slot, Stacks1122.toModern(stack));
        }
    }
}
