package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: drawables and helpers for categories. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IGuiHelper {
    legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableBuilder drawableBuilder(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation resourceLocation, int u, int v, int width, int height);
    legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableStatic createDrawable(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation resourceLocation, int u, int v, int width, int height);
    legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableStatic createDrawable(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation resourceLocation, int u, int v, int width, int height, int paddingTop, int paddingBottom, int paddingLeft, int paddingRight);
    legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableAnimated createAnimatedDrawable(legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableStatic drawable, int ticksPerCycle, legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableAnimated.StartDirection startDirection, boolean inverted);
    legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableStatic getSlotDrawable();
    legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableStatic createBlankDrawable(int width, int height);
    <V> legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawable createDrawableIngredient(V ingredient);
    legacyconnector.v1_12_2.mezz.jei.api.gui.ICraftingGridHelper createCraftingGridHelper(int craftInputSlot1, int craftOutputSlot);
    legacyconnector.v1_12_2.mezz.jei.api.gui.ITickTimer createTickTimer(int ticksPerCycle, int maxValue, boolean countDown);
}
