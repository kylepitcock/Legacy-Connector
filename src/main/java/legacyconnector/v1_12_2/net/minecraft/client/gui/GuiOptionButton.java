package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** A 150x20 button of the options screens. */
public class GuiOptionButton extends GuiButton {
    public GuiOptionButton(int id, int x, int y, String text) {
        super(id, x, y, 150, 20, text);
    }

    public GuiOptionButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }
}
