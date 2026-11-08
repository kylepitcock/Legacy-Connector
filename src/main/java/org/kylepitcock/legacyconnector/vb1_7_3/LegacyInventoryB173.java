package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Map;

import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

/**
 * A b1.7.3 {@code IInventory} as a modern {@link Container}: the inventory behind a {@code displayGUIChest} chest screen, and the container
 * the modern slots of a legacy menu name. One adapter per legacy inventory.
 */
public final class LegacyInventoryB173 implements Container {
    /** Keyed by identity (a mod's inventory class may define equals). */
    private static final Map<Inventory, LegacyInventoryB173> ADAPTERS = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    private final Inventory legacy;
    private SyncedStackB173[] stacks = new SyncedStackB173[0];

    private LegacyInventoryB173(Inventory legacy) {
        this.legacy = legacy;
    }

    public static LegacyInventoryB173 of(Inventory legacy) {
        if (legacy == null) return new LegacyInventoryB173(new legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingResultInventory());
        synchronized (ADAPTERS) {
            return ADAPTERS.computeIfAbsent(legacy, LegacyInventoryB173::new);
        }
    }

    public Inventory legacy() {
        return legacy;
    }

    private SyncedStackB173 sync(int slot) {
        if (slot >= stacks.length) {
            SyncedStackB173[] grown = java.util.Arrays.copyOf(stacks, Math.max(slot + 1, legacy.size()));
            for (int i = stacks.length; i < grown.length; i++) grown[i] = new SyncedStackB173();
            stacks = grown;
        }
        return stacks[slot];
    }

    private ItemStack stack(int slot) {
        try {
            return legacy.getStack(slot);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    @Override
    public int getContainerSize() {
        return legacy.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) if (!getItem(i).isEmpty()) return false;
        return true;
    }

    @Override
    public net.minecraft.world.item.ItemStack getItem(int slot) {
        if (slot < 0 || slot >= getContainerSize()) return net.minecraft.world.item.ItemStack.EMPTY;
        return sync(slot).get(stack(slot));
    }

    @Override
    public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= getContainerSize()) return net.minecraft.world.item.ItemStack.EMPTY;
        sync(slot).invalidate();
        return StacksB173.toModern(legacy.removeStack(slot, amount));
    }

    @Override
    public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
        net.minecraft.world.item.ItemStack s = getItem(slot).copy();
        setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
        return s;
    }

    @Override
    public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize()) return;
        sync(slot).invalidate();
        legacy.setStack(slot, stack == null || stack.isEmpty() ? null : StacksB173.toLegacy(stack));
    }

    @Override
    public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) {
        return StacksB173.toLegacy(stack) != null;
    }

    @Override
    public int getMaxStackSize() {
        return legacy.getMaxCountPerStack();
    }

    /** Writes stacks the modern code changed in place back, then {@code onInventoryChanged}. */
    @Override
    public void setChanged() {
        for (int i = 0; i < Math.min(stacks.length, getContainerSize()); i++) {
            ItemStack now = stack(i);
            if (stacks[i].changedInPlace(now)) {
                net.minecraft.world.item.ItemStack changed = stacks[i].modern();
                stacks[i].invalidate();
                legacy.setStack(i, changed.isEmpty() ? null : StacksB173.toLegacy(changed));
            }
        }
        legacy.markDirty();
    }

    @Override
    public boolean stillValid(Player player) {
        try {
            return legacy.canPlayerUse(PlayerViewB173.of(player));
        } catch (RuntimeException | LinkageError e) {
            return true;
        }
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
    }
}
