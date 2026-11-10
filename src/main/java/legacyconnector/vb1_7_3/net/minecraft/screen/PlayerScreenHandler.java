package legacyconnector.vb1_7_3.net.minecraft.screen;

import legacyconnector.vb1_7_3.net.minecraft.class_277$1;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingResultInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.recipe.CraftingRecipeManager;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.CraftingResultSlot;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot;

/** Beta's player container, also the superclass used by extended player inventories. */
public class PlayerScreenHandler extends ScreenHandler {
    public CraftingInventory craftingInput = new CraftingInventory(this, 2, 2);
    public Inventory craftingResult = new CraftingResultInventory();
    public boolean isLocal;

    public PlayerScreenHandler(PlayerInventory inventory) { this(inventory, true); }

    public PlayerScreenHandler(PlayerInventory inventory, boolean isLocal) {
        this.isLocal = isLocal;
        addSlot(new CraftingResultSlot(inventory.player, craftingInput, craftingResult, 0, 144, 36));
        for (int row = 0; row < 2; row++) for (int col = 0; col < 2; col++)
            addSlot(new Slot(craftingInput, col + row * 2, 88 + col * 18, 26 + row * 18));
        for (int type = 0; type < 4; type++)
            addSlot(new class_277$1(this, inventory, inventory.size() - 1 - type, 8, 8 + type * 18, type));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        onSlotUpdate(craftingInput);
    }

    @Override public void onSlotUpdate(Inventory inventory) {
        craftingResult.setStack(0, CraftingRecipeManager.getInstance().craft(craftingInput));
    }

    @Override public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        for (int i = 0; i < 4; i++) {
            ItemStack stack = craftingInput.getStack(i);
            if (stack != null) { player.dropItem(stack); craftingInput.setStack(i, null); }
        }
    }

    @Override public boolean canUse(PlayerEntity player) { return true; }

    /** Original fixed ranges intentionally exclude slots appended by subclasses. */
    @Override public ItemStack quickMove(int index) {
        if (index < 0 || index >= slots.size()) return null;
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return null;
        ItemStack stack = slot.getStack(), before = stack.copy();
        if (index == 0) insertItem(stack, 9, 45, true);
        else if (index >= 9 && index < 36) insertItem(stack, 36, 45, false);
        else if (index >= 36 && index < 45) insertItem(stack, 9, 36, false);
        else insertItem(stack, 9, 45, false);
        if (stack.count == 0) slot.setStack(null); else slot.markDirty();
        if (stack.count == before.count) return null;
        slot.onTakeItem(stack);
        return before;
    }
}
