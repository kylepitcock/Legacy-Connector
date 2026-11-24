package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 API: the ingredient list on the side of the screen. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IIngredientListOverlay {
    Object getIngredientUnderMouse();
    default boolean isListDisplayed() {
        return true;
    }
}
