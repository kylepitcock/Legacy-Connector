package org.kylepitcock.legacyconnector.v1_7_10.gui;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/**
 * Modern {@link Container} view of a legacy {@link IInventory}. Item stacks are shared, not copied: a legacy
 * inventory stores legacy stack views whose backing modern stack is exactly what this returns.
 * Also used for hopper/comparator/capability access to legacy inventories.
 */
public final class LegacyInventoryContainer implements net.minecraft.world.WorldlyContainer {
    private static final Map<IInventory, LegacyInventoryContainer> VIEWS = new WeakHashMap<>();
    private final IInventory inv;

    private LegacyInventoryContainer(IInventory inv) {
        this.inv = inv;
    }

    public static synchronized LegacyInventoryContainer of(IInventory inv) {
        return VIEWS.computeIfAbsent(inv, LegacyInventoryContainer::new);
    }

    public IInventory legacy() {
        return inv;
    }

    @Override
    public int getContainerSize() {
        return inv.getSizeInventory();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < inv.getSizeInventory(); i++) if (inv.getStackInSlot(i) != null) return false;
        return true;
    }

    @Override
    public net.minecraft.world.item.ItemStack getItem(int slot) {
        return ItemStack.mcc$toModern(inv.getStackInSlot(slot));
    }

    @Override
    public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) {
        return ItemStack.mcc$toModern(inv.decrStackSize(slot, amount));
    }

    @Override
    public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.mcc$toModern(inv.getStackInSlotOnClosing(slot));
    }

    @Override
    public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
        inv.setInventorySlotContents(slot, ItemStack.mcc$wrap(stack));
    }

    @Override
    public int getMaxStackSize() {
        return inv.getInventoryStackLimit();
    }

    @Override
    public void setChanged() {
        inv.markDirty();
    }

    @Override
    public boolean stillValid(Player player) {
        return inv.isUseableByPlayer((EntityPlayer) EntityBridge.wrap(player));
    }

    @Override
    public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) {
        return inv.isItemValidForSlot(slot, ItemStack.mcc$wrap(stack));
    }

    // Sided access: hoppers and item pipes honour the slots a legacy ISidedInventory offers on each face.
    private static final int[] NO_SLOTS = new int[0];

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction face) {
        if (inv instanceof legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory sided) {
            int[] slots = sided.getAccessibleSlotsFromSide(face.get3DDataValue());
            return slots == null ? NO_SLOTS : slots;
        }
        int[] all = new int[inv.getSizeInventory()];
        for (int i = 0; i < all.length; i++) all[i] = i;
        return all;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction face) {
        if (inv instanceof legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory sided) {
            return sided.canInsertItem(slot, ItemStack.mcc$wrap(stack), face == null ? 0 : face.get3DDataValue());
        }
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction face) {
        if (inv instanceof legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory sided) {
            return sided.canExtractItem(slot, ItemStack.mcc$wrap(stack), face == null ? 0 : face.get3DDataValue());
        }
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < inv.getSizeInventory(); i++) inv.setInventorySlotContents(i, null);
    }
}
