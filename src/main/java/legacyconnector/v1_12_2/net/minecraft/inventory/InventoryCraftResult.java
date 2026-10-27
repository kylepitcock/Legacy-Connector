package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** The one-slot result inventory of a crafting grid, remembering the recipe that made it. */
public class InventoryCraftResult implements IInventory {
    private final NonNullList<ItemStack> stackResult = NonNullList.withSize(1, ItemStack.EMPTY);
    private IRecipe recipeUsed;

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : stackResult) if (!itemstack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return stackResult.get(0);
    }

    @Override
    public String getName() {
        return "Result";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return ItemStackHelper.getAndRemove(stackResult, 0);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(stackResult, 0);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        stackResult.set(0, stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        stackResult.clear();
    }

    public void setRecipeUsed(IRecipe recipe) {
        this.recipeUsed = recipe;
    }

    public IRecipe getRecipeUsed() {
        return recipeUsed;
    }
}
