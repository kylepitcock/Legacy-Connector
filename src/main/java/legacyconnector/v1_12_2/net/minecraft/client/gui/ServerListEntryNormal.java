package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** A saved server in the server list: name, MOTD and player count (pinged when first drawn), double-click joins. */
public class ServerListEntryNormal implements GuiListExtended.IGuiListEntry {
    private final GuiMultiplayer owner;
    private final legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData server;
    private long lastClickTime;

    protected ServerListEntryNormal(GuiMultiplayer owner, legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData serverIn) {
        this.owner = owner;
        this.server = serverIn;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData getServerData() {
        return server;
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
        if (!server.pinged) {
            server.pinged = true;
            server.pingToServer = -2L;
            server.serverMOTD = "";
            server.populationInfo = "";
            try {
                owner.getOldServerPinger().ping(server);
            } catch (java.net.UnknownHostException e) {
                server.pingToServer = -1L;
                server.serverMOTD = legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format("multiplayer.status.cannot_resolve");
            }
        }
        var font = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().fontRenderer;
        if (font == null) return;
        font.drawString(server.serverName, x + 32 + 3, y + 1, 0xFFFFFF);
        if (server.serverMOTD != null) font.drawString(server.serverMOTD, x + 32 + 3, y + 12, 0x808080);
        if (server.populationInfo != null) font.drawString(server.populationInfo, x + listWidth - font.getStringWidth(server.populationInfo) - 17, y + 1, 0x808080);
    }

    @Override
    public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
    }

    @Override
    public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
        long now = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getSystemTime();
        if (now - lastClickTime < 250L) owner.connectToServer(server);
        lastClickTime = now;
        return false;
    }

    @Override
    public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
    }
}
