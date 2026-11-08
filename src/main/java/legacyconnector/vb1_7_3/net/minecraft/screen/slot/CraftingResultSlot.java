package legacyconnector.vb1_7_3.net.minecraft.screen.slot;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Beta 1.7.3 {@code SlotCrafting}: taking the result uses up one item of every grid slot (leaving its crafting return item, a bucket). */
public class CraftingResultSlot extends Slot {
    public Inventory input;
    public PlayerEntity player;

    public CraftingResultSlot(PlayerEntity player, Inventory input, Inventory result, int index, int x, int y) {
        super(result, index, x, y);
        this.player = player;
        this.input = input;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public void onTakeItem(ItemStack stack) {
        if (stack != null) {
            try {
                stack.onCraft(player == null ? null : player.world, player);
            } catch (RuntimeException | LinkageError ignored) {
                // statistics and achievements only
            }
        }
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getStack(i);
            if (s == null) continue;
            input.removeStack(i, 1);
            Item item = s.getItem();
            if (item != null && item.hasCraftingReturnItem()) input.setStack(i, new ItemStack(item.getCraftingReturnItem()));
        }
    }
}
