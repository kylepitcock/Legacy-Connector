package legacyconnector.v1_12_2.net.minecraftforge.common.crafting;

/** Forge: a recipe with a fixed pattern size (JEI and crafting helpers lay the grid out from it). */
public interface IShapedRecipe extends legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe {
    int getRecipeWidth();

    int getRecipeHeight();
}
