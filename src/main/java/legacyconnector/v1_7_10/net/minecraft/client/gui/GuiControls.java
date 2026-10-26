package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** 1.7.10's controls screen; displaying it opens the modern key binds screen (see ClientGui). */
public class GuiControls extends GuiScreen {
    protected GuiScreen parentScreen;
    protected String screenTitle = "Controls";
    protected legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings options;
    public legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding buttonId;
    public long time;

    public GuiControls(GuiScreen parent, legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings) {
        this.parentScreen = parent;
        this.options = settings;
    }

    @Override
    public void initGui() {
        this.screenTitle = legacyconnector.v1_7_10.net.minecraft.client.resources.I18n.format("controls.title");
    }

    public GuiScreen mcc$parent() {
        return parentScreen;
    }
}
