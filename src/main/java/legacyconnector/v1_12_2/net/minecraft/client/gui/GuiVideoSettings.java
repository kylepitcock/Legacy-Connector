package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;

/**
 * 1.12.2's video settings screen. Displaying this class itself shows the modern video settings; mods' subclasses run this screen:
 * the 1.12 option rows ({@code optionsRowList}) and Done.
 */
public class GuiVideoSettings extends GuiScreen {
    private static final GameSettings.Options[] VIDEO_OPTIONS = {GameSettings.Options.GRAPHICS, GameSettings.Options.RENDER_DISTANCE,
            GameSettings.Options.AMBIENT_OCCLUSION, GameSettings.Options.FRAMERATE_LIMIT, GameSettings.Options.ANAGLYPH, GameSettings.Options.VIEW_BOBBING,
            GameSettings.Options.GUI_SCALE, GameSettings.Options.ATTACK_INDICATOR, GameSettings.Options.GAMMA, GameSettings.Options.RENDER_CLOUDS,
            GameSettings.Options.PARTICLES, GameSettings.Options.USE_FULLSCREEN, GameSettings.Options.ENABLE_VSYNC, GameSettings.Options.MIPMAP_LEVELS,
            GameSettings.Options.USE_VBO, GameSettings.Options.ENTITY_SHADOWS};
    private final GuiScreen parentGuiScreen;
    protected String screenTitle = "Video Settings";
    private final GameSettings guiGameSettings;
    private GuiListExtended optionsRowList;

    public GuiVideoSettings(GuiScreen parentScreenIn, GameSettings gameSettingsIn) {
        this.parentGuiScreen = parentScreenIn;
        this.guiGameSettings = gameSettingsIn;
    }

    @Override
    public void initGui() {
        screenTitle = I18n.format("options.videoTitle");
        buttonList.clear();
        buttonList.add(new GuiButton(200, width / 2 - 100, height - 27, I18n.format("gui.done")));
        optionsRowList = new GuiOptionsRowList(mc, width, height, 32, height - 32, 25, VIDEO_OPTIONS);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (optionsRowList != null) optionsRowList.handleMouseInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) mcc$save();
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled && button.id == 200) {
            mcc$save();
            mc.displayGuiScreen(parentGuiScreen);
        }
    }

    private void mcc$save() {
        GameSettings s = guiGameSettings != null ? guiGameSettings : Minecraft.mcc$get$gameSettings(mc);
        if (s != null) s.saveOptions();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (optionsRowList != null) optionsRowList.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (optionsRowList != null) optionsRowList.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        if (optionsRowList != null) optionsRowList.drawScreen(mouseX, mouseY, partialTicks);
        drawCenteredString(fontRenderer, screenTitle, width / 2, 5, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
