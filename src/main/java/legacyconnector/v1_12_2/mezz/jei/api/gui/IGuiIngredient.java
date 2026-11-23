package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: one slot. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IGuiIngredient<T> {
    java.util.List<T> getAllIngredients();
    T getDisplayedIngredient();
    boolean isInput();
}
