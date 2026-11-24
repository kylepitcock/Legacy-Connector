package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 4 API: wrappers for vanilla categories (anvil, brewing) a plugin adds recipes to. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IVanillaRecipeFactory {
    IRecipeWrapper createAnvilRecipe(legacyconnector.v1_12_2.net.minecraft.item.ItemStack leftInput, java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> rightInputs, java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> outputs);
    IRecipeWrapper createBrewingRecipe(java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> ingredients, legacyconnector.v1_12_2.net.minecraft.item.ItemStack potionInput, legacyconnector.v1_12_2.net.minecraft.item.ItemStack potionOutput);
}
