package legacyconnector.v1_20_1.net.minecraftforge.client.gui.overlay;

import net.minecraft.client.gui.GuiGraphics;

/** Forge 1.20.1's HUD overlay; the layer adapts it to a NeoForge 1.21.1 {@code LayeredDraw.Layer}. */
@FunctionalInterface
public interface IGuiOverlay {
    void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight);
}
