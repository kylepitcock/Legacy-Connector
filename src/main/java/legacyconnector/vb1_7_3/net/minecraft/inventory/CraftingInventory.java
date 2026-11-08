package legacyconnector.vb1_7_3.net.minecraft.inventory;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler;

/** Beta 1.7.3 {@code InventoryCrafting}: a crafting grid that tells its container about every change (which recomputes the result). */
public class CraftingInventory implements Inventory {
    public ItemStack[] stacks;
    public int width;
    public ScreenHandler handler;

    public CraftingInventory(ScreenHandler handler, int width, int height) {
        this.stacks = new ItemStack[width * height];
        this.handler = handler;
        this.width = width;
    }

    @Override
    public int size() {
        return stacks.length;
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot >= 0 && slot < stacks.length ? stacks[slot] : null;
    }

    /** The stack at column {@code x}, row {@code y}. */
    public ItemStack getStack(int x, int y) {
        return x >= 0 && x < width ? getStack(x + y * width) : null;
    }

    @Override
    public String getName() {
        return "Crafting";
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack s = getStack(slot);
        if (s == null) return null;
        ItemStack taken;
        if (s.count <= amount) {
            taken = s;
            stacks[slot] = null;
        } else {
            taken = s.split(amount);
            if (s.count == 0) stacks[slot] = null;
        }
        changed();
        return taken;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= stacks.length) return;
        stacks[slot] = stack;
        changed();
    }

    private void changed() {
        if (handler != null) handler.onSlotUpdate(this);
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
