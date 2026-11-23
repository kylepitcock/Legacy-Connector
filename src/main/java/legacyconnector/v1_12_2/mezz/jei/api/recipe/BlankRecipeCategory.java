package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 4 (Minecraft 1.12.2) API: the deprecated base of recipe categories (the optional methods have their defaults in IRecipeCategory). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
@Deprecated
public abstract class BlankRecipeCategory<T extends IRecipeWrapper> implements IRecipeCategory<T> {
}
