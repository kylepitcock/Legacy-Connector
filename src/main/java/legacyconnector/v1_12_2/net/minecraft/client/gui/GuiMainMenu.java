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

    /**
     * The 1.12 title screen's buttons, for the subclasses that draw their own title screen and call {@code super.initGui()} for the
     * buttons (Singleplayer, Multiplayer, Options, Language, Quit: the 1.12 layout); the modern title screen has them itself.
     */
    @Override
    public void initGui() {
        int j = this.height / 4 + 48;
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, j, legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format("menu.singleplayer")));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, j + 24, legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format("menu.multiplayer")));
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, j + 72 + 12, 98, 20, legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format("menu.options")));
        this.buttonList.add(new GuiButton(4, this.width / 2 + 2, j + 72 + 12, 98, 20, legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format("menu.quit")));
        this.buttonList.add(new GuiButtonLanguage(5, this.width / 2 - 124, j + 72 + 12));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws java.io.IOException {
        switch (button.id) {
            case 0 -> this.mc.displayGuiScreen(new GuiOptions(this, legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$gameSettings(this.mc)));
            case 5 -> this.mc.displayGuiScreen(new GuiLanguage(this, legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$gameSettings(this.mc), this.mc.getLanguageManager()));
            case 1 -> this.mc.displayGuiScreen(new GuiWorldSelection(this));
            case 2 -> this.mc.displayGuiScreen(new GuiMultiplayer(this));
            case 4 -> this.mc.shutdown();
            default -> { }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
