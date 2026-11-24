package legacyconnector.v1_12_2.mezz.jei.api.ingredients;

/** JEI 4.x (Minecraft 1.12.2) API: registration of a mod's own ingredient types (JEI 4.5 and later name; recorded, the modern JEI shows no slots of such types). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IModIngredientRegistration {
    legacyconnector.v1_12_2.mezz.jei.api.IJeiHelpers getJeiHelpers();
    <V> void register(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<V> ingredientType, java.util.Collection<V> allIngredients,
            IIngredientHelper<V> ingredientHelper, IIngredientRenderer<V> ingredientRenderer);
}
