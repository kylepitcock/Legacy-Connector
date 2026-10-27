package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipe;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipeList;

/** 1.12.2 trading partner (villagers and mods' traders); {@code EntityPlayer.displayVillagerTradeGui} opens the modern trading screen on it. */
public interface IMerchant {
    void setCustomer(EntityPlayer player);

    EntityPlayer getCustomer();

    MerchantRecipeList getRecipes(EntityPlayer player);

    void setRecipes(MerchantRecipeList recipes);

    void useRecipe(MerchantRecipe recipe);

    void verifySellingItem(ItemStack stack);

    legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName();

    legacyconnector.v1_12_2.net.minecraft.world.World getWorld();

    legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getPos();
}
