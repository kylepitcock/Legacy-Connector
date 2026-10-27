package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.resources.LanguageManager;
import legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings;

/** The language list; displaying it shows the modern language screen with the open screen as its parent. */
public class GuiLanguage extends GuiScreen {
    protected GuiScreen parentScreen;
    private final GameSettings game_settings_3;
    private final LanguageManager languageManager;

    public GuiLanguage(GuiScreen screen, GameSettings gameSettingsObj, LanguageManager manager) {
        this.parentScreen = screen;
        this.game_settings_3 = gameSettingsObj;
        this.languageManager = manager;
    }
}
