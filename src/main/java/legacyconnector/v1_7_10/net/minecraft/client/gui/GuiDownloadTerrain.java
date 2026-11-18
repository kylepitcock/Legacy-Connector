package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient;

/**
 * 1.7.10's "Downloading terrain" screen. Displaying one opens the modern level-loading screen, which closes by itself once the chunk the player
 * stands in has been received (the legacy screen closed when the client had the terrain too).
 */
public class GuiDownloadTerrain extends GuiScreen {
    private final NetHandlerPlayClient netHandler;

    public GuiDownloadTerrain(NetHandlerPlayClient handler) {
        this.netHandler = handler;
    }
}
