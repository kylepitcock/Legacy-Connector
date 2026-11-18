package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.IMerchant;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipe;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList;

/**
 * The three trading slots (1.7.10 {@code InventoryMerchant}): two things to pay with, and the result. Putting something in works out which of the
 * merchant's trades it matches ({@link #resetRecipeAndSlots}) and puts that trade's wares in the result slot.
 */
public class InventoryMerchant implements IInventory {
    private final IMerchant theMerchant;
    private final ItemStack[] theInventory = new ItemStack[3];
    private final EntityPlayer thePlayer;
    private MerchantRecipe currentRecipe;
    private int currentRecipeIndex;

    public InventoryMerchant(EntityPlayer player, IMerchant merchant) {
        this.thePlayer = player;
        this.theMerchant = merchant;
    }

    @Override
    public int getSizeInventory() {
        return theInventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return theInventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (theInventory[slot] == null) return null;
        if (slot == 2) {
            ItemStack result = theInventory[slot];
            theInventory[slot] = null;
            return result;
        }
        if (theInventory[slot].mcc$count() <= amount) {
            ItemStack result = theInventory[slot];
            theInventory[slot] = null;
            if (inventoryResetNeededOnSlotChange(slot)) resetRecipeAndSlots();
            return result;
        }
        ItemStack result = theInventory[slot].splitStack(amount);
        if (theInventory[slot].mcc$count() == 0) theInventory[slot] = null;
        if (inventoryResetNeededOnSlotChange(slot)) resetRecipeAndSlots();
        return result;
    }

    private boolean inventoryResetNeededOnSlotChange(int slot) {
        return slot == 0 || slot == 1;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (theInventory[slot] == null) return null;
        ItemStack result = theInventory[slot];
        theInventory[slot] = null;
        return result;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        theInventory[slot] = stack;
        if (stack != null && stack.mcc$count() > getInventoryStackLimit()) ItemStack.mcc$set$stackSize(stack, getInventoryStackLimit());
        if (inventoryResetNeededOnSlotChange(slot)) resetRecipeAndSlots();
    }

    @Override
    public String getInventoryName() {
        return "mob.villager";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return theMerchant.getCustomer() == player;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void markDirty() {
        resetRecipeAndSlots();
    }

    /** Finds the trade the two payment slots match and fills the result slot with its wares (or empties it). */
    public void resetRecipeAndSlots() {
        currentRecipe = null;
        ItemStack first = theInventory[0];
        ItemStack second = theInventory[1];
        if (first == null) {
            first = second;
            second = null;
        }
        if (first == null) {
            setInventorySlotContents(2, null);
        } else {
            MerchantRecipeList recipes = theMerchant.getRecipes(thePlayer);
            if (recipes != null) {
                MerchantRecipe recipe = recipes.canRecipeBeUsed(first, second, currentRecipeIndex);
                if (recipe != null && !recipe.isRecipeDisabled()) {
                    currentRecipe = recipe;
                    setInventorySlotContents(2, recipe.getItemToSell().copy());
                } else if (second != null) {
                    recipe = recipes.canRecipeBeUsed(second, first, currentRecipeIndex);
                    if (recipe != null && !recipe.isRecipeDisabled()) {
                        currentRecipe = recipe;
                        setInventorySlotContents(2, recipe.getItemToSell().copy());
                    } else {
                        setInventorySlotContents(2, null);
                    }
                } else {
                    setInventorySlotContents(2, null);
                }
            }
        }
        theMerchant.verifySellingItem(getStackInSlot(2));
    }

    public MerchantRecipe getCurrentRecipe() {
        return currentRecipe;
    }

    public void setCurrentRecipeIndex(int index) {
        this.currentRecipeIndex = index;
        resetRecipeAndSlots();
    }
}
