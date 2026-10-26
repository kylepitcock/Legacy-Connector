package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** 1.7.10's options screen; displaying it opens the modern options screen (see ClientGui). */
public class GuiOptions extends GuiScreen implements GuiYesNoCallback {
    protected GuiScreen field_146441_g;
    protected legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings game_settings_1;
    protected String field_146442_a = "Options";

    public GuiOptions(GuiScreen parent, legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings) {
        this.field_146441_g = parent;
        this.game_settings_1 = settings;
    }

    public GuiScreen mcc$parent() {
        return field_146441_g;
    }
}
