package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: a value that changes with time. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface ITickTimer {
    int getValue();
    int getMaxValue();
}
