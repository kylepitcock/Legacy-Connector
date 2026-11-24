package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: registration of a mod's own ingredient types (recorded; the modern JEI needs modern helpers, renderers and codecs, so slots of such types stay out). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IModIngredientRegistration {
    legacyconnector.v1_12_2.mezz.jei.api.IJeiHelpers getJeiHelpers();
    <V> void register(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<V> ingredientType, java.util.Collection<V> allIngredients,
            legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientHelper<V> ingredientHelper,
            legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRenderer<V> ingredientRenderer);
}
