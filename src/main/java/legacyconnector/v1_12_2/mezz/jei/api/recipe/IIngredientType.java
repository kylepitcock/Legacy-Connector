package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 4 (Minecraft 1.12.2) API: an ingredient type. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
@FunctionalInterface
public interface IIngredientType<T> {
    Class<? extends T> getIngredientClass();
}
