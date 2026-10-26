package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** 1.7.10's chat settings; displaying it opens the modern chat options screen (see ClientGui). */
public class ScreenChatOptions extends GuiScreen {
    protected GuiScreen field_146396_g;
    protected legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings game_settings;
    protected String field_146401_i = "Chat Settings";

    public ScreenChatOptions(GuiScreen parent, legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings) {
        this.field_146396_g = parent;
        this.game_settings = settings;
    }

    public GuiScreen mcc$parent() {
        return field_146396_g;
    }
}
