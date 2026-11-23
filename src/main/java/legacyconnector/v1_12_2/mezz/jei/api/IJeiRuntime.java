package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: the running JEI. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IJeiRuntime {
    legacyconnector.v1_12_2.mezz.jei.api.IRecipeRegistry getRecipeRegistry();
    legacyconnector.v1_12_2.mezz.jei.api.IRecipesGui getRecipesGui();
    legacyconnector.v1_12_2.mezz.jei.api.IIngredientFilter getIngredientFilter();
}
