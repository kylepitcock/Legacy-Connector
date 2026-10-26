package legacyconnector.v1_7_10.net.minecraft.village;

import java.util.ArrayList;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** The trades of a villager. */
public class MerchantRecipeList extends ArrayList {
    public MerchantRecipeList() {}

    public MerchantRecipeList(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        readRecipiesFromTags(tag);
    }

    /** Adds the trades saved under {@code Recipes} (villagers' {@code Offers} tag). */
    public void readRecipiesFromTags(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        var list = tag.getTagList("Recipes", 10);
        for (int i = 0; i < list.tagCount(); i++) add(new MerchantRecipe(list.getCompoundTagAt(i)));
    }

    /** The trades as saved by 1.7.10: a {@code Recipes} list of {@link MerchantRecipe#writeToTags} compounds. */
    public legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound getRecipiesAsTags() {
        var tag = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
        var list = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList();
        for (Object o : this) list.appendTag(((MerchantRecipe) o).writeToTags());
        tag.setTag("Recipes", list);
        return tag;
    }

    public MerchantRecipe canRecipeBeUsed(ItemStack buy, ItemStack secondBuy, int index) {
        if (index > 0 && index < size()) {
            MerchantRecipe r = (MerchantRecipe) get(index);
            return matches(r, buy, secondBuy) ? r : null;
        }
        for (Object o : this) {
            MerchantRecipe r = (MerchantRecipe) o;
            if (matches(r, buy, secondBuy)) return r;
        }
        return null;
    }

    private static boolean matches(MerchantRecipe r, ItemStack buy, ItemStack secondBuy) {
        if (r.isRecipeDisabled() || buy == null || r.getItemToBuy() == null) return false;
        if (buy.getItem() != r.getItemToBuy().getItem() || ItemStack.mcc$get$stackSize(buy) < ItemStack.mcc$get$stackSize(r.getItemToBuy())) return false;
        if (!r.hasSecondItemToBuy()) return secondBuy == null;
        return secondBuy != null && secondBuy.getItem() == r.getSecondItemToBuy().getItem() && ItemStack.mcc$get$stackSize(secondBuy) >= ItemStack.mcc$get$stackSize(r.getSecondItemToBuy());
    }

    public void addToListWithCheck(MerchantRecipe recipe) {
        for (Object o : this) {
            MerchantRecipe r = (MerchantRecipe) o;
            if (r.getItemToBuy() != null && recipe.getItemToBuy() != null && r.getItemToBuy().getItem() == recipe.getItemToBuy().getItem()
                    && r.getItemToSell() != null && recipe.getItemToSell() != null && r.getItemToSell().getItem() == recipe.getItemToSell().getItem()) {
                return;
            }
        }
        add(recipe);
    }
}
