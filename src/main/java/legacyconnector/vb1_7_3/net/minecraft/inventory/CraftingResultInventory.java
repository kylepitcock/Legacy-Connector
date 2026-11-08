package legacyconnector.vb1_7_3.net.minecraft.inventory;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Beta 1.7.3 {@code InventoryCraftResult}: one slot holding the crafting result. */
public class CraftingResultInventory implements Inventory {
    public ItemStack[] stacks = new ItemStack[1];

    public CraftingResultInventory() {}

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemStack getStack(int slot) {
        return stacks[0];
    }

    @Override
    public String getName() {
        return "Result";
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack s = stacks[0];
        stacks[0] = null;
        return s;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        stacks[0] = stack;
    }

    @Override
    public int getMaxCountPerStack() {
        return 64;
    }

    @Override
    public void markDirty() {}

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }
}
