package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122;

/** The result slot of a crafting grid: taking from it consumes the grid (leaving container items) and counts as crafting. */
public class SlotCrafting extends Slot {
    private final InventoryCrafting craftMatrix;
    private final EntityPlayer player;
    private int amountCrafted;

    public SlotCrafting(EntityPlayer player, InventoryCrafting craftingInventory, IInventory inventoryIn, int slotIndex, int xPosition, int yPosition) {
        super(inventoryIn, slotIndex, xPosition, yPosition);
        this.player = player;
        this.craftMatrix = craftingInventory;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        if (getHasStack()) amountCrafted += Math.min(amount, getStack().getCount());
        return super.decrStackSize(amount);
    }

    @Override
    protected void onCrafting(ItemStack stack, int amount) {
        amountCrafted += amount;
        onCrafting(stack);
    }

    @Override
    protected void onSwapCraft(int amount) {
        amountCrafted += amount;
    }

    @Override
    protected void onCrafting(ItemStack stack) {
        if (amountCrafted > 0) {
            stack.onCrafting(player.getEntityWorld(), player, amountCrafted);
            org.kylepitcock.legacyconnector.v1_12_2.bridge.Crafting1122.firePlayerCrafted(player, stack, craftMatrix);
        }
        amountCrafted = 0;
        if (inventory instanceof InventoryCraftResult result) {
            var recipe = result.getRecipeUsed();
            if (recipe != null && !recipe.isDynamic()) result.setRecipeUsed(null);
        }
    }

    @Override
    public ItemStack onTake(EntityPlayer thePlayer, ItemStack stack) {
        onCrafting(stack);
        NonNullList<ItemStack> remaining = legacyconnector.v1_12_2.net.minecraft.item.crafting.CraftingManager.getRemainingItems(craftMatrix,
                thePlayer.getEntityWorld());
        for (int i = 0; i < remaining.size(); ++i) {
            ItemStack itemstack = craftMatrix.getStackInSlot(i);
            ItemStack itemstack1 = remaining.get(i);
            if (!itemstack.isEmpty()) {
                craftMatrix.decrStackSize(i, 1);
                itemstack = craftMatrix.getStackInSlot(i);
            }
            if (!itemstack1.isEmpty()) {
                if (itemstack.isEmpty()) {
                    craftMatrix.setInventorySlotContents(i, itemstack1);
                } else if (ItemStack.areItemsEqual(itemstack, itemstack1) && ItemStack.areItemStackTagsEqual(itemstack, itemstack1)) {
                    itemstack1.grow(itemstack.getCount());
                    craftMatrix.setInventorySlotContents(i, itemstack1);
                } else if (!EntityPlayer.mcc$get$inventory(player).addItemStackToInventory(itemstack1)) {
                    Inventories1122.dropFromPlayer(player, itemstack1);
                }
            }
        }
        return stack;
    }
}
