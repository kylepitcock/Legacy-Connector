package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** The server list; displaying it shows the modern server list with the open screen as its parent. */
public class GuiMultiplayer extends GuiScreen {
    private final GuiScreen parentScreen;
    /** True while the add-server screen opened from this list is showing. */
    private boolean addingServer;

    public GuiMultiplayer(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }
}
