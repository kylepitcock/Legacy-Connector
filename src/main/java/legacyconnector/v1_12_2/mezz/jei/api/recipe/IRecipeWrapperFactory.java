package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 4 (Minecraft 1.12.2) API: turns a mod's recipe into a wrapper. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
@FunctionalInterface
public interface IRecipeWrapperFactory<T> {
    IRecipeWrapper getRecipeWrapper(T recipe);
}
