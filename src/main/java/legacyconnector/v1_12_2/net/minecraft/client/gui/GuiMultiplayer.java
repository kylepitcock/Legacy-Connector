package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** The server list; displaying it shows the modern server list with the open screen as its parent. */
public class GuiMultiplayer extends GuiScreen {
    private final GuiScreen parentScreen;
    /** True while the add-server screen opened from this list is showing. */
    public boolean addingServer;
    /** Phantom: the entry selected in the game's server list (when it is open). */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData selectedServer;
    private legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerList savedServerList;
    private final legacyconnector.v1_12_2.net.minecraft.client.network.ServerPinger oldServerPinger =
            new legacyconnector.v1_12_2.net.minecraft.client.network.ServerPinger();

    public GuiMultiplayer(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    /** The saved servers (read from servers.dat when first asked). */
    public legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerList getServerList() {
        if (savedServerList == null) savedServerList = new legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerList(mc);
        return savedServerList;
    }

    public legacyconnector.v1_12_2.net.minecraft.client.network.ServerPinger getOldServerPinger() {
        return oldServerPinger;
    }

    public static legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData mcc$get$selectedServer(GuiMultiplayer g) {
        var data = mcc$selectedModern();
        return data == null ? null : legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData.mcc$of(data);
    }

    private static net.minecraft.client.multiplayer.ServerData mcc$selectedModern() {
        if (!(net.minecraft.client.Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen screen)) return null;
        try {
            var f = net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen.class.getDeclaredField("serverSelectionList");
            f.setAccessible(true);
            var list = (net.minecraft.client.gui.screens.multiplayer.ServerSelectionList) f.get(screen);
            return list.getSelected() instanceof net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry e ? e.getServerData() : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Joins the selected server. */
    public void connectToSelected() {
        var data = mcc$get$selectedServer(this);
        if (data != null) org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.connect(this, data.serverIP, data);
    }

    public void connectToServer(legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData server) {
        if (server != null) org.kylepitcock.legacyconnector.v1_12_2.client.Minecraft1122.connect(this, server.serverIP, server);
    }
}
