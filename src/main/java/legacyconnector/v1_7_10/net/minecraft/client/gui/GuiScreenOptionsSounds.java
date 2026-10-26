package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** 1.7.10's sound options; displaying it opens the modern sound options screen (see ClientGui). */
public class GuiScreenOptionsSounds extends GuiScreen {
    protected GuiScreen field_146505_f;
    protected legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings game_settings_4;
    protected String field_146507_a = "Options";

    public GuiScreenOptionsSounds(GuiScreen parent, legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings) {
        this.field_146505_f = parent;
        this.game_settings_4 = settings;
    }

    public GuiScreen mcc$parent() {
        return field_146505_f;
    }
}
