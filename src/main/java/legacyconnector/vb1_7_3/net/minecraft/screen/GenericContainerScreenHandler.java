package legacyconnector.vb1_7_3.net.minecraft.screen;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot;

/** Beta 1.7.3 {@code ContainerChest}: the chest's rows of nine, then the player's inventory and hotbar below them. */
public class GenericContainerScreenHandler extends ScreenHandler {
    public Inventory inventory;
    public int rows;

    public GenericContainerScreenHandler(Inventory playerInventory, Inventory inventory) {
        this.inventory = inventory;
        this.rows = inventory.size() / 9;
        int offset = (rows - 4) * 18;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + offset));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + offset));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    /** MCP {@code transferStackInSlot}: chest to the player's inventory (from the end), and back. */
    @Override
    public ItemStack quickMove(int index) {
        ItemStack result = null;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index < rows * 9) insertItem(stack, rows * 9, slots.size(), true);
            else insertItem(stack, 0, rows * 9, false);
            if (stack.count == 0) slot.setStack(null);
            else slot.markDirty();
        }
        return result;
    }
}
