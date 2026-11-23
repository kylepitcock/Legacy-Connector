package legacyconnector.v1_12_2.mezz.jei.api.recipe;

/** JEI 4 (Minecraft 1.12.2) API: what the recipe screen was opened for. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IFocus<V> {
    enum Mode {
        INPUT, OUTPUT
    }
    V getValue();
    Mode getMode();
}
