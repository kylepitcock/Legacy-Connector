package legacyconnector.v1_12_2.mezz.jei.api.ingredients;

/** JEI 4 API: identifies an ingredient of a mod's own type. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IIngredientHelper<V> {
    default java.util.List<V> expandSubtypes(java.util.List<V> ingredients) {
        return ingredients;
    }
    default V getMatchingIngredient(V contained, java.util.Collection<V> ingredients) {
        for (V v : ingredients) if (java.util.Objects.equals(getUniqueId(v), getUniqueId(contained))) return v;
        return null;
    }
    String getDisplayName(V ingredient);
    String getUniqueId(V ingredient);
    default String getWildcardId(V ingredient) {
        return getUniqueId(ingredient);
    }
    default String getModId(V ingredient) {
        return "unknown";
    }
    default String getResourceId(V ingredient) {
        return getUniqueId(ingredient);
    }
    default V copyIngredient(V ingredient) {
        return ingredient;
    }
    String getErrorInfo(V ingredient);
    default boolean isValidIngredient(V ingredient) {
        return ingredient != null;
    }
    default boolean isIngredientOnServer(V ingredient) {
        return true;
    }
    default java.util.Collection<String> getOreDictNames(V ingredient) {
        return java.util.Collections.emptyList();
    }
    default java.util.Collection<String> getCreativeTabNames(V ingredient) {
        return java.util.Collections.emptyList();
    }
    default java.awt.Color getColor(V ingredient) {
        return null;
    }
}
