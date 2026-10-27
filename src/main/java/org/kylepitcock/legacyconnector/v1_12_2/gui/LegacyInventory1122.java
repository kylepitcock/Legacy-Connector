package org.kylepitcock.legacyconnector.v1_12_2.gui;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/** Modern {@link net.minecraft.world.Container} view of a 1.12.2 {@link IInventory} (what the modern slots of a legacy menu belong to). */
public final class LegacyInventory1122 implements net.minecraft.world.Container {
    private static final Map<IInventory, LegacyInventory1122> VIEWS = new WeakHashMap<>();
    private static final LegacyInventory1122 NONE = new LegacyInventory1122(null);
    private final IInventory inv;

    private LegacyInventory1122(IInventory inv) {
        this.inv = inv;
    }

    public static synchronized LegacyInventory1122 of(IInventory inv) {
        return inv == null ? NONE : VIEWS.computeIfAbsent(inv, LegacyInventory1122::new);
    }

    public IInventory legacy() {
        return inv;
    }

    @Override
    public int getContainerSize() {
        return inv == null ? 0 : inv.getSizeInventory();
    }

    @Override
    public boolean isEmpty() {
        return inv == null || inv.isEmpty();
    }

    @Override
    public net.minecraft.world.item.ItemStack getItem(int slot) {
        return inv == null ? net.minecraft.world.item.ItemStack.EMPTY : Stacks1122.toModern(inv.getStackInSlot(slot));
    }

    @Override
    public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) {
        return inv == null ? net.minecraft.world.item.ItemStack.EMPTY : Stacks1122.toModern(inv.decrStackSize(slot, amount));
    }

    @Override
    public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
        return inv == null ? net.minecraft.world.item.ItemStack.EMPTY : Stacks1122.toModern(inv.removeStackFromSlot(slot));
    }

    @Override
    public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
        if (inv != null) inv.setInventorySlotContents(slot, Stacks1122.fromModern(stack));
    }

    @Override
    public int getMaxStackSize() {
        return inv == null ? 64 : inv.getInventoryStackLimit();
    }

    @Override
    public void setChanged() {
        if (inv != null) inv.markDirty();
    }

    @Override
    public boolean stillValid(Player player) {
        return inv == null || EntityBridge1122.wrap(player) instanceof EntityPlayer p && inv.isUsableByPlayer(p);
    }

    @Override
    public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) {
        return inv != null && inv.isItemValidForSlot(slot, Stacks1122.fromModern(stack));
    }

    @Override
    public void clearContent() {
        if (inv != null) inv.clear();
    }
}
