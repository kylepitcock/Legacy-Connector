package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.settings.GameSettings;

/** 1.7.10's 150x20 button of the options screens, optionally tied to the option it changes. */
public class GuiOptionButton extends GuiButton {
    private final GameSettings.Options enumOptions;

    public GuiOptionButton(int id, int x, int y, String text) {
        this(id, x, y, null, text);
    }

    public GuiOptionButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
        this.enumOptions = null;
    }

    public GuiOptionButton(int id, int x, int y, GameSettings.Options option, String text) {
        super(id, x, y, 150, 20, text);
        this.enumOptions = option;
    }

    public GameSettings.Options returnEnumOptions() {
        return enumOptions;
    }
}
