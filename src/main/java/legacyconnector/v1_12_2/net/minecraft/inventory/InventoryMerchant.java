package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.IMerchant;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipe;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipeList;

/** 1.12.2 trading inventory: two payment slots and the result of the merchant's recipe they match (the selected one first). */
public class InventoryMerchant implements IInventory {
    private final IMerchant merchant;
    private final NonNullList<ItemStack> slots = NonNullList.withSize(3, ItemStack.EMPTY);
    private final EntityPlayer player;
    private MerchantRecipe currentRecipe;
    private int currentRecipeIndex;

    public InventoryMerchant(EntityPlayer thePlayerIn, IMerchant theMerchantIn) {
        this.player = thePlayerIn;
        this.merchant = theMerchantIn;
    }

    @Override
    public int getSizeInventory() {
        return slots.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : slots) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return slots.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = slots.get(index);
        if (index == 2 && !stack.isEmpty()) return ItemStackHelper.getAndSplit(slots, index, stack.getCount());
        ItemStack out = ItemStackHelper.getAndSplit(slots, index, count);
        if (!out.isEmpty() && inventoryResetNeededOnSlotChange(index)) resetRecipeAndSlots();
        return out;
    }

    private boolean inventoryResetNeededOnSlotChange(int slot) {
        return slot == 0 || slot == 1;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(slots, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        slots.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
        if (inventoryResetNeededOnSlotChange(index)) resetRecipeAndSlots();
    }

    @Override
    public String getName() {
        return "mob.villager";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
        return hasCustomName() ? new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(getName())
                : new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation(getName());
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return merchant.getCustomer() == player;
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
    public void markDirty() {
        resetRecipeAndSlots();
    }

    /** Finds the recipe the payment slots pay for (the selected one, else any) and shows its result, or clears the result slot. */
    public void resetRecipeAndSlots() {
        currentRecipe = null;
        ItemStack first = slots.get(0);
        ItemStack second = slots.get(1);
        if (first.isEmpty()) {
            first = second;
            second = ItemStack.EMPTY;
        }
        if (first.isEmpty()) {
            setInventorySlotContents(2, ItemStack.EMPTY);
        } else {
            MerchantRecipeList list = merchant.getRecipes(player);
            if (list != null) {
                MerchantRecipe recipe = list.canRecipeBeUsed(first, second, currentRecipeIndex);
                if (recipe != null && !recipe.isRecipeDisabled()) {
                    currentRecipe = recipe;
                    setInventorySlotContents(2, recipe.getItemToSell().copy());
                } else if (!second.isEmpty()) {
                    recipe = list.canRecipeBeUsed(second, first, currentRecipeIndex);
                    if (recipe != null && !recipe.isRecipeDisabled()) {
                        currentRecipe = recipe;
                        setInventorySlotContents(2, recipe.getItemToSell().copy());
                    } else {
                        setInventorySlotContents(2, ItemStack.EMPTY);
                    }
                } else {
                    setInventorySlotContents(2, ItemStack.EMPTY);
                }
            }
            merchant.verifySellingItem(getStackInSlot(2));
        }
    }

    public MerchantRecipe getCurrentRecipe() {
        return currentRecipe;
    }

    public void setCurrentRecipeIndex(int currentRecipeIndexIn) {
        this.currentRecipeIndex = currentRecipeIndexIn;
        resetRecipeAndSlots();
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
        slots.clear();
    }
}
