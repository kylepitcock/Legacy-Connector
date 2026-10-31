package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_7_10.baubles.api.IBauble;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * Baubles' four slots (amulet, two rings, belt) held in memory per player, for packs that have the Baubles API (shaded in
 * mods such as Extra Utilities) but neither Baubles nor Curios: {@code BaublesApi.getBaubles} used to return null there and
 * Thaumcraft's runic shielding crashed every tick. Not persisted; with Curios installed the Curios-backed inventory is used.
 */
public final class PlainBaubles implements IInventory {
    private static final Map<EntityPlayer, PlainBaubles> BY_PLAYER = new WeakHashMap<>();
    private final ItemStack[] slots = new ItemStack[4];

    private PlainBaubles() {}

    public static synchronized IInventory of(EntityPlayer player) {
        return BY_PLAYER.computeIfAbsent(player, p -> new PlainBaubles());
    }

    @Override public int getSizeInventory() { return slots.length; }
    @Override public ItemStack getStackInSlot(int slot) { return slot >= 0 && slot < slots.length ? slots[slot] : null; }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack s = getStackInSlot(slot);
        if (s == null) return null;
        if (ItemStack.mcc$get$stackSize(s) <= amount) {
            slots[slot] = null;
            return s;
        }
        ItemStack out = s.splitStack(amount);
        if (ItemStack.mcc$get$stackSize(s) == 0) slots[slot] = null;
        return out;
    }

    @Override public ItemStack getStackInSlotOnClosing(int slot) { return null; }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot >= 0 && slot < slots.length) slots[slot] = stack;
    }

    @Override public String getInventoryName() { return "Baubles"; }
    @Override public boolean hasCustomInventoryName() { return false; }
    @Override public int getInventoryStackLimit() { return 1; }
    @Override public void markDirty() {}
    @Override public boolean isUseableByPlayer(EntityPlayer p) { return true; }
    @Override public void openInventory() {}
    @Override public void closeInventory() {}
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return stack != null && stack.getItem() instanceof IBauble; }
}
