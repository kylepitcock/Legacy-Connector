package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.multiplayer.ServerData;

/**
 * 1.7.10's add/edit server screen. Displaying it opens the modern edit-server screen on a copy of the entry; Done copies
 * the name and address back and reports to the parent with {@code confirmClicked(result, 0)}, as 1.7.10 did.
 */
public class GuiScreenAddServer extends GuiScreen {
    protected final GuiScreen parentScreen;
    protected final ServerData serverData;

    public GuiScreenAddServer(GuiScreen parent, ServerData data) {
        this.parentScreen = parent;
        this.serverData = data;
    }

    public GuiScreen mcc$parent() {
        return parentScreen;
    }

    /** The modern screen doing this one's job, hosted under {@code modernParent}. */
    public net.minecraft.client.gui.screens.Screen mcc$modern(net.minecraft.client.gui.screens.Screen modernParent) {
        var modern = new net.minecraft.client.multiplayer.ServerData(serverData.serverName == null ? "" : serverData.serverName,
                serverData.serverIP == null ? "" : serverData.serverIP, net.minecraft.client.multiplayer.ServerData.Type.OTHER);
        return new net.minecraft.client.gui.screens.EditServerScreen(modernParent, ok -> {
            if (ok) {
                serverData.serverName = modern.name;
                serverData.serverIP = modern.ip;
            }
            if (parentScreen != null) {
                legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(parentScreen);
                parentScreen.confirmClicked(ok, 0);
            } else {
                net.minecraft.client.Minecraft.getInstance().setScreen(modernParent);
            }
        }, modern);
    }
}
