package legacyconnector.v1_12_2.net.minecraft.client.gui;

/**
 * The add/edit server screen; displaying it shows the game's edit server screen. Done copies the name, address and resource pack choice
 * into the server entry and tells the parent ({@code confirmClicked(result, 0)}, as 1.12 did).
 */
public class GuiScreenAddServer extends GuiScreen {
    private final GuiScreen parentScreen;
    private final legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData serverData;

    public GuiScreenAddServer(GuiScreen parentScreen, legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData serverData) {
        this.parentScreen = parentScreen;
        this.serverData = serverData;
    }

    /** The game's screen doing the same job (not a 1.12 method). */
    public net.minecraft.client.gui.screens.Screen mcc$modern(net.minecraft.client.gui.screens.Screen parent) {
        var data = serverData == null ? new net.minecraft.client.multiplayer.ServerData("", "", net.minecraft.client.multiplayer.ServerData.Type.OTHER)
                : serverData.mcc$modern();
        return new net.minecraft.client.gui.screens.EditServerScreen(parent, ok -> {
            if (ok && serverData != null) {
                serverData.serverName = data.name;
                serverData.serverIP = data.ip;
                serverData.setResourceMode(switch (data.getResourcePackStatus()) {
                    case ENABLED -> legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData.ServerResourceMode.ENABLED;
                    case DISABLED -> legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData.ServerResourceMode.DISABLED;
                    default -> legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData.ServerResourceMode.PROMPT;
                });
            }
            if (parentScreen != null) {
                legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(parentScreen);
                parentScreen.confirmClicked(ok, 0);
            } else {
                net.minecraft.client.Minecraft.getInstance().setScreen(parent);
            }
        }, data);
    }
}
