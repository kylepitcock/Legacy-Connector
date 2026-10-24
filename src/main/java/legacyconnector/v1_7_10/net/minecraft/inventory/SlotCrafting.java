package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * The result slot of a crafting grid: taking the result uses up one of each ingredient (leaving container items such as
 * buckets behind) and fires the FML crafting event.
 */
public class SlotCrafting extends Slot {
    private final IInventory craftMatrix;
    private final EntityPlayer thePlayer;
    private int amountCrafted;

    public SlotCrafting(EntityPlayer player, IInventory craftingInventory, IInventory outputInventory, int index, int x, int y) {
        super(outputInventory, index, x, y);
        this.thePlayer = player;
        this.craftMatrix = craftingInventory;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        if (getHasStack()) amountCrafted += Math.min(amount, ItemStack.mcc$get$stackSize(getStack()));
        return super.decrStackSize(amount);
    }

    @Override
    protected void onCrafting(ItemStack stack, int amount) {
        amountCrafted += amount;
        onCrafting(stack);
    }

    @Override
    protected void onCrafting(ItemStack stack) {
        stack.onCrafting(legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$worldObj(thePlayer), thePlayer, amountCrafted);
        amountCrafted = 0;
    }

    @Override
    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
        FMLCommonHandler.instance().firePlayerCraftingEvent(player, stack, craftMatrix);
        onCrafting(stack);
        for (int i = 0; i < craftMatrix.getSizeInventory(); ++i) {
            ItemStack ingredient = craftMatrix.getStackInSlot(i);
            if (ingredient == null) continue;
            craftMatrix.decrStackSize(i, 1);
            if (ingredient.getItem().hasContainerItem(ingredient)) {
                ItemStack container = ingredient.getItem().getContainerItem(ingredient);
                if (container != null && container.isItemStackDamageable() && container.getItemDamage() > container.getMaxDamage()) continue;
                if (!ingredient.getItem().doesContainerItemLeaveCraftingGrid(ingredient) || !thePlayer.inventory.addItemStackToInventory(container)) {
                    if (craftMatrix.getStackInSlot(i) == null) craftMatrix.setInventorySlotContents(i, container);
                    else thePlayer.dropPlayerItemWithRandomChoice(container, false);
                }
            }
        }
    }
}
