package legacyconnector.v1_12_2.net.minecraft.client.multiplayer;

/**
 * The "connecting to server" screen: displaying it starts the game's own connection (and its connecting screen), returning to the parent
 * screen when the connection fails.
 */
public class GuiConnecting extends legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen {
    private final legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen previousGuiScreen;
    private final String host;
    private final ServerData server;
    private boolean started;

    public GuiConnecting(legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen parent, legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc,
            ServerData serverData) {
        this.previousGuiScreen = parent;
        this.server = serverData;
        this.host = serverData == null ? "" : serverData.serverIP;
    }

    public GuiConnecting(legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen parent, legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc,
            String hostName, int port) {
        this.previousGuiScreen = parent;
        this.server = null;
        this.host = port == 25565 ? hostName : hostName + ":" + port;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (started) return;
        started = true;
        org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.connect(previousGuiScreen, host, server);
    }
}
