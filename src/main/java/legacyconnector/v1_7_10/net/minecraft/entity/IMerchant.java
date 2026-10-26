package legacyconnector.v1_7_10.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipe;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList;

/** Something a player trades with (villagers, mods' traders); {@code EntityPlayer.displayGUIMerchant} opens the trading screen. */
public interface IMerchant {
    void setCustomer(EntityPlayer player);

    EntityPlayer getCustomer();

    MerchantRecipeList getRecipes(EntityPlayer player);

    void setRecipes(MerchantRecipeList recipes);

    void useRecipe(MerchantRecipe recipe);

    /** The item in the result slot changed (villagers make a sound). */
    void verifySellingItem(ItemStack stack);
}
