package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** 1.7.10's language screen; displaying it opens the modern language screen (see ClientGui). */
public class GuiLanguage extends GuiScreen {
    protected GuiScreen field_146453_a;
    protected legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings game_settings_3;
    protected legacyconnector.v1_7_10.net.minecraft.client.resources.LanguageManager field_146454_h;

    public GuiLanguage(GuiScreen parent, legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings settings, legacyconnector.v1_7_10.net.minecraft.client.resources.LanguageManager languages) {
        this.field_146453_a = parent;
        this.game_settings_3 = settings;
        this.field_146454_h = languages;
    }

    public GuiScreen mcc$parent() {
        return field_146453_a;
    }
}
