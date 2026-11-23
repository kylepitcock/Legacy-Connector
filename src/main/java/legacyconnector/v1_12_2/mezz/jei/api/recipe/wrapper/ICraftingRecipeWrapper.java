package legacyconnector.v1_12_2.mezz.jei.api.recipe.wrapper;

/** JEI 4 (Minecraft 1.12.2) API: a crafting recipe. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface ICraftingRecipeWrapper extends legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapper {
    default legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getRegistryName() {
        return null;
    }
}
