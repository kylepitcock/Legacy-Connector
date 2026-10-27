package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * The title screen. Displaying it shows the modern title screen; the title screen opening is announced to mods as this class in
 * {@code GuiOpenEvent} (main menu replacements check for it). Mods' subclasses are legacy screens of their own.
 */
public class GuiMainMenu extends GuiScreen {
    public static final ResourceLocation[] TITLE_PANORAMA_PATHS = {
        new ResourceLocation("textures/gui/title/background/panorama_0.png"), new ResourceLocation("textures/gui/title/background/panorama_1.png"),
        new ResourceLocation("textures/gui/title/background/panorama_2.png"), new ResourceLocation("textures/gui/title/background/panorama_3.png"),
        new ResourceLocation("textures/gui/title/background/panorama_4.png"), new ResourceLocation("textures/gui/title/background/panorama_5.png")};

    public GuiMainMenu() {
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
