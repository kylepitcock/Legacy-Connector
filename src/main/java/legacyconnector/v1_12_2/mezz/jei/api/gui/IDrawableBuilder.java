package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: builds texture drawables. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IDrawableBuilder {
    IDrawableBuilder setTextureSize(int width, int height);
    IDrawableBuilder addPadding(int paddingTop, int paddingBottom, int paddingLeft, int paddingRight);
    IDrawableBuilder trim(int trimTop, int trimBottom, int trimLeft, int trimRight);
    IDrawableStatic build();
    IDrawableAnimated buildAnimated(int ticksPerCycle, IDrawableAnimated.StartDirection startDirection, boolean inverted);
    IDrawableAnimated buildAnimated(ITickTimer tickTimer, IDrawableAnimated.StartDirection startDirection);
}
