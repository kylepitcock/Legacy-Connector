package org.kylepitcock.legacyconnector.v1_12_2.gui;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * A modern {@link net.minecraft.world.Container} (a chest, the crafting grid, a furnace) as a 1.12.2 {@link IInventory}. Stacks read are
 * live views of the modern ones; writes go straight to the modern container. One view per container, so slots of the same inventory
 * compare equal ({@code Slot.isSameInventory}).
 */
public final class ModernInventory1122 implements IInventory {
    private static final Map<net.minecraft.world.Container, ModernInventory1122> VIEWS = new WeakHashMap<>();
    private final net.minecraft.world.Container modern;

    private ModernInventory1122(net.minecraft.world.Container modern) {
        this.modern = modern;
    }

    public static ModernInventory1122 of(net.minecraft.world.Container modern) {
        synchronized (VIEWS) {
            return VIEWS.computeIfAbsent(modern, ModernInventory1122::new);
        }
    }

    public net.minecraft.world.Container mcc$modern() {
        return modern;
    }

    private static net.minecraft.world.entity.player.Player modern(EntityPlayer player) {
        return player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p : null;
    }

    @Override
    public int getSizeInventory() {
        return modern.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return modern.isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index < 0 || index >= modern.getContainerSize()) return ItemStack.EMPTY;
        return Stacks1122.view(modern.getItem(index));
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return Stacks1122.fromModern(modern.removeItem(index, count));
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return Stacks1122.fromModern(modern.removeItemNoUpdate(index));
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        modern.setItem(index, Stacks1122.toModernLive(stack));
    }

    @Override
    public int getInventoryStackLimit() {
        return modern.getMaxStackSize();
    }

    @Override
    public void markDirty() {
        modern.setChanged();
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        var p = modern(player);
        return p == null || modern.stillValid(p);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        var p = modern(player);
        if (p != null) modern.startOpen(p);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        var p = modern(player);
        if (p != null) modern.stopOpen(p);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return modern.canPlaceItem(index, Stacks1122.toModern(stack));
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        modern.clearContent();
    }

    @Override
    public String getName() {
        return modern instanceof net.minecraft.world.Nameable n ? n.getName().getString() : "container";
    }

    @Override
    public boolean hasCustomName() {
        return modern instanceof net.minecraft.world.Nameable n && n.hasCustomName();
    }
}
