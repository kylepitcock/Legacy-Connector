package legacyconnector.v1_12_2.mezz.jei.api.ingredients;

/** JEI 4 (Minecraft 1.12.2) API: every ingredient of the game. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IIngredientRegistry {
    <V> java.util.Collection<V> getAllIngredients(legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType<V> ingredientType);
    <V> java.util.Collection<V> getAllIngredients(Class<V> ingredientClass);
    java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getFuels();
    java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getPotionIngredients();
    boolean isFuel(legacyconnector.v1_12_2.net.minecraft.item.ItemStack itemStack);
}
