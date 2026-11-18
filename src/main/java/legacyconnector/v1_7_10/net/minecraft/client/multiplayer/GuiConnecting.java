package legacyconnector.v1_7_10.net.minecraft.client.multiplayer;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;

/**
 * 1.7.10's "Connecting to the server" screen. Mods open it to join a server (server redirects, menu replacements, quick-connect buttons);
 * displaying one starts the connection through the modern connect screen (see ClientGui), which shows the progress, the failure screen and
 * returns to the parent screen on cancel.
 */
public class GuiConnecting extends GuiScreen {
    private final GuiScreen previousGuiScreen;
    private final ServerAddress address;
    private final ServerData data;

    public GuiConnecting(GuiScreen parent, Minecraft mc, ServerData data) {
        this.previousGuiScreen = parent;
        this.data = data;
        this.address = ServerAddress.func_78860_a(data.serverIP);
    }

    public GuiConnecting(GuiScreen parent, Minecraft mc, String ip, int port) {
        this.previousGuiScreen = parent;
        this.address = ServerAddress.func_78860_a(ip + ":" + port);
        this.data = new ServerData("", ip + ":" + port);
    }

    /** Starts the connection with the modern client and returns the screen that shows it. */
    public net.minecraft.client.gui.screens.Screen mcc$connect(net.minecraft.client.gui.screens.Screen parent) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var modernData = new net.minecraft.client.multiplayer.ServerData(String.valueOf(data.serverName), String.valueOf(data.serverIP),
                net.minecraft.client.multiplayer.ServerData.Type.OTHER);
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(parent, mc, address.mcc$modern(), modernData, false, null);
        return mc.screen;
    }
}
