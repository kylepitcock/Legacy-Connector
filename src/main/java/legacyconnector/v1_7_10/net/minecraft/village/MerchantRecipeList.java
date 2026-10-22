package legacyconnector.v1_7_10.net.minecraft.village;

import java.util.ArrayList;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** The trades of a villager. */
public class MerchantRecipeList extends ArrayList {
    public MerchantRecipeList() {}

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
