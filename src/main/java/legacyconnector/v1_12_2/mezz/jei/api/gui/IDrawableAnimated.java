package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: an animated drawable. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IDrawableAnimated extends IDrawable {
    enum StartDirection {
        TOP, BOTTOM, LEFT, RIGHT
    }
}
