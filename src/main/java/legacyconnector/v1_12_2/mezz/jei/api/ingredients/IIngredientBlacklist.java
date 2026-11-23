package legacyconnector.v1_12_2.mezz.jei.api.ingredients;

/** JEI 4 (Minecraft 1.12.2) API: hidden ingredients. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IIngredientBlacklist {
    <V> void addIngredientToBlacklist(V ingredient);
    <V> void removeIngredientFromBlacklist(V ingredient);
    <V> boolean isIngredientBlacklisted(V ingredient);
}
