package legacyconnector.vb1_7_3.net.minecraft.screen.slot;

import legacyconnector.vb1_7_3.net.minecraft.achievement.Achievements;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/** Beta furnace result slot: rejects insertion and runs the original crafting and achievement callbacks on pickup. */
public class FurnaceOutputSlot extends Slot {
    private final PlayerEntity player;

    public FurnaceOutputSlot(PlayerEntity player, Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.player = player;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public void onTakeItem(ItemStack stack) {
        stack.onCraft(player.world, player);
        if (stack.itemId == 265) player.increaseStat(Achievements.ACQUIRE_IRON, 1);
        if (stack.itemId == 350) player.increaseStat(Achievements.COOK_FISH, 1);
        super.onTakeItem(stack);
    }
}
