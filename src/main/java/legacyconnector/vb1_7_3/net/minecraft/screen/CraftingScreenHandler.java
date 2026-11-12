package legacyconnector.vb1_7_3.net.minecraft.screen;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingResultInventory;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.recipe.CraftingRecipeManager;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.CraftingResultSlot;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta's crafting table container ({@code ContainerWorkbench}): a 3x3 grid, a result slot and the player's inventory. Mods extend it
 * for their own crafting blocks and read {@code input} and {@code result} directly.
 */
public class CraftingScreenHandler extends ScreenHandler {
    /** Vanilla's crafting table block id. */
    private static final int CRAFTING_TABLE = 58;

    public CraftingInventory input = new CraftingInventory(this, 3, 3);
    public Inventory result = new CraftingResultInventory();
    public World world;
    public int x;
    public int y;
    public int z;

    public CraftingScreenHandler(PlayerInventory inventory, World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        addSlot(new CraftingResultSlot(inventory.player, input, result, 0, 124, 35));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            addSlot(new Slot(input, col + row * 3, 30 + col * 18, 17 + row * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        onSlotUpdate(input);
    }

    @Override public void onSlotUpdate(Inventory inventory) {
        result.setStack(0, CraftingRecipeManager.getInstance().craft(input));
    }

    @Override public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (world != null && world.isRemote) return;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = input.getStack(i);
            if (stack != null) player.dropItem(stack);
        }
    }

    @Override public boolean canUse(PlayerEntity player) {
        if (world.getBlockId(x, y, z) != CRAFTING_TABLE) return false;
        return player.getSquaredDistance(x + 0.5D, y + 0.5D, z + 0.5D) <= 64.0D;
    }

    @Override public ItemStack quickMove(int index) {
        if (index < 0 || index >= slots.size()) return null;
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return null;
        ItemStack stack = slot.getStack(), before = stack.copy();
        if (index == 0) insertItem(stack, 10, 46, true);
        else if (index >= 10 && index < 37) insertItem(stack, 37, 46, false);
        else if (index >= 37 && index < 46) insertItem(stack, 10, 37, false);
        else insertItem(stack, 10, 46, false);
        if (stack.count == 0) slot.setStack(null); else slot.markDirty();
        if (stack.count == before.count) return null;
        slot.onTakeItem(stack);
        return before;
    }
}
