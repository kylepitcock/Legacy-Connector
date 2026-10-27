package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** The options screen; displaying it shows the modern options with the open screen as their parent. */
public class GuiOptions extends GuiScreen {
    private final GuiScreen lastScreen;

    public GuiOptions(GuiScreen lastScreen, legacyconnector.v1_12_2.net.minecraft.client.settings.GameSettings settings) {
        this.lastScreen = lastScreen;
    }
}
