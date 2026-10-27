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

    public static final ResourceLocation MINECRAFT_TITLE_TEXTURES = new ResourceLocation("textures/gui/title/minecraft.png");
    /** The yellow splash line (one of the game's splashes). */
    protected String splashText = mcc$splash();

    public GuiMainMenu() {
    }

    private static String mcc$splash() {
        try {
            var splash = net.minecraft.client.Minecraft.getInstance().getSplashManager().getSplash();
            Object text = splash == null ? null
                    : org.kylepitcock.legacyconnector.v1_12_2.client.Fields1122.get(net.minecraft.client.gui.components.SplashRenderer.class, splash, "splash");
            return text instanceof String s ? s : "missingno";
        } catch (RuntimeException | LinkageError e) {
            return "missingno";
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
