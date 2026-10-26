package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** 1.7.10's video settings; displaying it opens the modern video settings screen (see ClientGui). */
public class GuiVideoSettings extends GuiScreen {
    protected GuiScreen parentGuiScreen;
    protected String screenTitle = "Video Settings";
    protected legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings guiGameSettings;

    public GuiVideoSettings(GuiScreen parent, legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings) {
        this.parentGuiScreen = parent;
        this.guiGameSettings = settings;
    }

    public GuiScreen mcc$parent() {
        return parentGuiScreen;
    }
}
