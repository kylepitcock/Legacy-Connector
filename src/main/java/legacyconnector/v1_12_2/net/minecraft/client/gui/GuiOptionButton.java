package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** A 150x20 button of the options screens. */
public class GuiOptionButton extends GuiButton {
    public GuiOptionButton(int id, int x, int y, String text) {
        super(id, x, y, 150, 20, text);
    }

    public GuiOptionButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    private legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings.Options enumOptions;

    /** A button for an option (its screen steps the option and relabels the button when pressed). */
    public GuiOptionButton(int id, int x, int y, legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings.Options option, String text) {
        super(id, x, y, 150, 20, text);
        this.enumOptions = option;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings.Options getOption() {
        return enumOptions;
    }
}
