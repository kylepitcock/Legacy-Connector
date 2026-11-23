package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: recipe lookups (focus creation). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IRecipeRegistry {
    <V> legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus<V> createFocus(legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus.Mode mode, V ingredient);
}
