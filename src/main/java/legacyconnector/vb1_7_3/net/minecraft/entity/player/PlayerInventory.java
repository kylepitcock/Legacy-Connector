package legacyconnector.vb1_7_3.net.minecraft.entity.player;

import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Beta 1.7.3 {@code InventoryPlayer}: 36 main slots (0-8 the hotbar) and 4 armour slots. */
public class PlayerInventory implements Inventory {
    public ItemStack[] main = new ItemStack[36];
    public ItemStack[] armor = new ItemStack[4];
    public int selectedSlot;
    public PlayerEntity player;
    private ItemStack cursorStack;

    public ItemStack getCursorStack() { return cursorStack; }
    public void setCursorStack(ItemStack stack) { cursorStack = stack; }

    public PlayerInventory(PlayerEntity player) {
        this.player = player;
    }

    public ItemStack getSelectedItem() {
        return selectedSlot >= 0 && selectedSlot < 9 ? main[selectedSlot] : null;
    }

    /** MCP {@code consumeInventoryItem}: one item of that id less. */
    public boolean remove(int itemId) {
        for (int i = 0; i < main.length; i++) {
            if (main[i] != null && main[i].itemId == itemId) {
                if (--main[i].count <= 0) main[i] = null;
                return true;
            }
        }
        return false;
    }

    public boolean addStack(ItemStack stack) {
        if (stack == null) return false;
        for (int i = 0; i < main.length && stack.count > 0; i++) {
            ItemStack s = main[i];
            if (s != null && s.itemId == stack.itemId && s.getDamage() == stack.getDamage() && s.count < s.getMaxCount()
                    && org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.sameComponents(s, stack)) {
                int moved = Math.min(stack.count, s.getMaxCount() - s.count);
                s.count += moved;
                stack.count -= moved;
            }
        }
        for (int i = 0; i < main.length && stack.count > 0; i++) {
            if (main[i] == null) {
                main[i] = stack.copy();
                stack.count = 0;
            }
        }
        return stack.count == 0;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack[] array = slot >= main.length ? armor : main;
        int i = slot >= main.length ? slot - main.length : slot;
        if (array[i] == null) return null;
        if (array[i].count <= amount) {
            ItemStack s = array[i];
            array[i] = null;
            return s;
        }
        ItemStack s = array[i].split(amount);
        if (array[i].count == 0) array[i] = null;
        return s;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= main.length) armor[slot - main.length] = stack;
        else main[slot] = stack;
    }

    @Override
    public int size() {
        return main.length + 4;
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot >= main.length ? armor[slot - main.length] : main[slot];
    }

    public ItemStack getArmorStack(int slot) {
        return armor[slot];
    }

    @Override
    public String getName() {
        return "Inventory";
    }

    @Override
    public int getMaxCountPerStack() {
        return 64;
    }

    @Override
    public void markDirty() {}

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return !this.player.dead && player.getSquaredDistance(this.player.x, this.player.y, this.player.z) <= 64.0;
    }
}
