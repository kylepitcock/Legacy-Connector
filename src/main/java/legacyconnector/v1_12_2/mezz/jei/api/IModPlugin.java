package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: a mod's plugin. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IModPlugin {
    default void registerItemSubtypes(legacyconnector.v1_12_2.mezz.jei.api.ISubtypeRegistry subtypeRegistry) {}
    default void registerIngredients(legacyconnector.v1_12_2.mezz.jei.api.IModIngredientRegistration registry) {}
    default void registerIngredients(legacyconnector.v1_12_2.mezz.jei.api.ingredients.IModIngredientRegistration registry) {}
    default void registerCategories(legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeCategoryRegistration registry) {}
    default void register(legacyconnector.v1_12_2.mezz.jei.api.IModRegistry registry) {}
    default void onRuntimeAvailable(legacyconnector.v1_12_2.mezz.jei.api.IJeiRuntime jeiRuntime) {}
}
