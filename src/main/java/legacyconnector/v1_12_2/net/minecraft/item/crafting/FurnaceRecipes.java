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
        Object[] s = RecipeBridge1122.smeltingFor(stack);
        return s == null ? ItemStack.EMPTY : (ItemStack) s[1];
    }

    public float getSmeltingExperience(ItemStack stack) {
        Object[] s = RecipeBridge1122.smeltingFor(stack);
        return s == null ? 0.0F : (Float) s[2];
    }

    public java.util.Map<ItemStack, ItemStack> getSmeltingList() {
        return new java.util.LinkedHashMap<>();
    }
}
