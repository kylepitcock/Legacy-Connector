package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import net.minecraft.world.Container;

/**
 * A native container (a crafting grid) as Beta's {@code IInventory} for one callback. Beta handed out the stacks themselves, which mods
 * change in place (a saw left in the grid gets one more item and one more point of wear): the stacks are kept for the callback and
 * {@link #writeBack} puts each one that changed into the native container.
 */
public final class ContainerViewB173 implements Inventory {
    private final Container container;
    private final ItemStack[] stacks;
    private final String[] seen;

    public ContainerViewB173(Container container) {
        this.container = container;
        stacks = new ItemStack[container.getContainerSize()];
        seen = new String[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = StacksB173.toLegacy(container.getItem(i));
            seen[i] = key(stacks[i]);
        }
    }

    private static String key(ItemStack stack) {
        return stack == null ? "" : stack.itemId + ":" + stack.count + ":" + stack.getDamage();
    }

    /** Writes the stacks the callback changed (or replaced) into the native container. */
    public void writeBack() {
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i] != null && stacks[i].count <= 0 ? null : stacks[i];
            if (key(stack).equals(seen[i])) continue;
            container.setItem(i, stack == null ? net.minecraft.world.item.ItemStack.EMPTY : StacksB173.toModern(stack));
            seen[i] = key(stack);
        }
        container.setChanged();
    }

    @Override public int size() { return stacks.length; }
    @Override public ItemStack getStack(int slot) { return slot >= 0 && slot < stacks.length ? stacks[slot] : null; }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = getStack(slot);
        if (stack == null) return null;
        if (stack.count <= amount) { stacks[slot] = null; return stack; }
        ItemStack split = stack.copy();
        split.count = amount;
        stack.count -= amount;
        return split;
    }

    @Override public void setStack(int slot, ItemStack stack) { if (slot >= 0 && slot < stacks.length) stacks[slot] = stack; }
    @Override public String getName() { return "Crafting"; }
    @Override public int getMaxCountPerStack() { return container.getMaxStackSize(); }
    @Override public void markDirty() {}
    @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
}
