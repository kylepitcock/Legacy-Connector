package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.RecipeBridge1122;

public class FurnaceRecipes {
    private static final FurnaceRecipes INSTANCE = new FurnaceRecipes();

    public static FurnaceRecipes instance() {
        return INSTANCE;
    }

    public void addSmeltingRecipe(ItemStack input, ItemStack stack, float experience) {
        RecipeBridge1122.addSmelting(input, stack, experience);
    }

    public void addSmeltingRecipeForBlock(Block input, ItemStack stack, float experience) {
        RecipeBridge1122.addSmelting(new ItemStack(input, 1, 32767), stack, experience);
    }

    public void addSmelting(Item input, ItemStack stack, float experience) {
        RecipeBridge1122.addSmelting(new ItemStack(input, 1, 32767), stack, experience);
    }

    public ItemStack getSmeltingResult(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    public float getSmeltingExperience(ItemStack stack) {
        return 0.0F;
    }
}
