package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * {@link ModernCapabilityTile} of a block entity with an item handler (a storage or machine of a NeoForge mod that is not a vanilla
 * {@code Container}) as the legacy {@code ISidedInventory}: legacy machines that drop their products into the inventory beside them
 * ({@code ReikaInventoryHelper.addToIInv}) and sorters that read from it work on it. The slots are those of the sideless handler (the
 * first sided one when there is none); a side only decides whether insertion or extraction is possible.
 */
public final class ModernItemTile extends ModernCapabilityTile implements ISidedInventory {
    public ModernItemTile(BlockEntity host) {
        super(host);
    }

    static boolean hasItems(BlockEntity be) {
        return base(be) != null;
    }

    private static IItemHandler base(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null || be.isRemoved()) return null;
        var pos = be.getBlockPos();
        IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (h != null) return h;
        for (Direction d : Direction.values()) {
            h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, d);
            if (h != null) return h;
        }
        return null;
    }

    private IItemHandler handler() {
        return base(host);
    }

    private IItemHandler side(int side) {
        Level level = host.getLevel();
        if (level == null || host.isRemoved() || side < 0 || side > 5) return null;
        return level.getCapability(Capabilities.ItemHandler.BLOCK, host.getBlockPos(), Direction.from3DDataValue(side));
    }

    @Override
    public int getSizeInventory() {
        IItemHandler h = handler();
        return h == null ? 0 : h.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        IItemHandler h = handler();
        return h == null || slot < 0 || slot >= h.getSlots() ? null : ItemStack.mcc$wrap(h.getStackInSlot(slot));
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        IItemHandler h = handler();
        return h == null || slot < 0 || slot >= h.getSlots() ? null : ItemStack.mcc$wrap(h.extractItem(slot, amount, false));
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        IItemHandler h = handler();
        if (h == null || slot < 0 || slot >= h.getSlots()) return;
        net.minecraft.world.item.ItemStack wanted = ItemStack.mcc$toModern(stack);
        net.minecraft.world.item.ItemStack current = h.getStackInSlot(slot);
        // an item handler has no "set": take what is there beyond what is wanted, then insert what is missing
        if (!current.isEmpty() && (wanted.isEmpty() || !net.minecraft.world.item.ItemStack.isSameItemSameComponents(current, wanted))) {
            h.extractItem(slot, current.getCount(), false);
            current = h.getStackInSlot(slot);
        }
        if (wanted.isEmpty()) return;
        int have = current.isEmpty() ? 0 : current.getCount();
        if (wanted.getCount() < have) h.extractItem(slot, have - wanted.getCount(), false);
        else if (wanted.getCount() > have) h.insertItem(slot, wanted.copyWithCount(wanted.getCount() - have), false);
    }

    @Override
    public String getInventoryName() {
        return "container.modern";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        host.setChanged();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        IItemHandler h = handler();
        return h != null && slot >= 0 && slot < h.getSlots() && h.isItemValid(slot, ItemStack.mcc$toModern(stack));
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        IItemHandler h = side(side);
        if (h == null) return new int[0];
        int[] slots = new int[h.getSlots()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        IItemHandler h = side(side);
        if (h == null || slot < 0 || slot >= h.getSlots()) return false;
        var wanted = ItemStack.mcc$toModern(stack);
        return !wanted.isEmpty() && h.insertItem(slot, wanted, true).getCount() < wanted.getCount();
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        IItemHandler h = side(side);
        return h != null && slot >= 0 && slot < h.getSlots() && !h.extractItem(slot, 1, true).isEmpty();
    }
}
