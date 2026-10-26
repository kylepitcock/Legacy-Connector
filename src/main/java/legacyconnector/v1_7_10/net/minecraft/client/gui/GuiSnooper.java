package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** 1.7.10's snooper settings (telemetry); displaying it opens the modern telemetry screen (see ClientGui). */
public class GuiSnooper extends GuiScreen {
    protected GuiScreen field_146608_a;
    protected legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings game_settings_2;

    public GuiSnooper(GuiScreen parent, legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings) {
        this.field_146608_a = parent;
        this.game_settings_2 = settings;
    }

    public GuiScreen mcc$parent() {
        return field_146608_a;
    }
}
