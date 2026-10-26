package legacyconnector.v1_7_10.net.minecraft.client.gui;

/**
 * 1.7.10's multiplayer server list screen. The modern game shows its own; this class carries the 1.7.10 fields mods find by
 * reflection (Fisk's Heroes reads the server being added).
 */
public class GuiMultiplayer extends GuiScreen {
    /** The server being added or edited. */
    private legacyconnector.v1_7_10.net.minecraft.client.multiplayer.ServerData selectedServer;
    /** Whether the add-server screen is open. */
    private boolean addingServer;
    protected final GuiScreen parentScreen;

    public GuiMultiplayer(GuiScreen parent) {
        this.parentScreen = parent;
    }
}
