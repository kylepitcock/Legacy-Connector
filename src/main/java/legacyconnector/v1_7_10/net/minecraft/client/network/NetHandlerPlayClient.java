package legacyconnector.v1_7_10.net.minecraft.client.network;

/** Client connection handle; legacy handlers mostly ignore it and use Minecraft.getMinecraft(). */
public class NetHandlerPlayClient implements legacyconnector.v1_7_10.net.minecraft.network.play.INetHandlerPlayClient {
    public static final NetHandlerPlayClient INSTANCE = new NetHandlerPlayClient();

    /** Phantom: the players on the server, as tab list entries (redirected to {@link #mcc$get$playerInfoList}). */
    @Deprecated
    public java.util.List playerInfoList;

    public NetHandlerPlayClient() {
    }

    /** A handler a mod makes for a client world of its own (fake worlds); it talks to the game's real connection. */
    public NetHandlerPlayClient(legacyconnector.v1_7_10.net.minecraft.client.Minecraft mc, legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen screen,
            legacyconnector.v1_7_10.net.minecraft.network.NetworkManager manager) {
    }

    /** Queues a packet for the server: custom-channel packets go through the channel bridge, vanilla ones as their modern equivalent. */
    public void addToSendQueue(legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
        if (packet instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket fml) {
            org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge.send(fml,
                    legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget.TOSERVER, null);
        } else if (packet != null) {
            packet.mcc$sendToServer();
        }
    }

    /** The connection to the server. */
    public legacyconnector.v1_7_10.net.minecraft.network.NetworkManager getNetworkManager() {
        return org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.manager();
    }

    /** The server ended the connection (mods call it to drop the player with their reason after a failed handshake). */
    @Override
    public void handleDisconnect(legacyconnector.v1_7_10.net.minecraft.network.play.server.S40PacketDisconnect packet) {
        var manager = getNetworkManager();
        if (manager != null) manager.closeChannel(packet == null ? null : packet.func_149165_c());
    }

    /** The players the server lists, with their ping. */
    public static java.util.List mcc$get$playerInfoList(NetHandlerPlayClient handler) {
        java.util.List out = new java.util.ArrayList();
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection == null) return out;
        for (var info : connection.getListedOnlinePlayers()) {
            var entry = new legacyconnector.v1_7_10.net.minecraft.client.gui.GuiPlayerInfo(info.getProfile().getName());
            entry.responseTime = info.getLatency();
            out.add(entry);
        }
        return out;
    }

    public static void mcc$set$playerInfoList(NetHandlerPlayClient handler, java.util.List list) {
        // the server owns the player list
    }

    private static ClientConnection connectionView;

    /**
     * 1.7.10 rebuilt the client world for the new dimension here. The modern client switches worlds when the server's own
     * respawn packet arrives (sent by the teleport that moved the player), so a respawn packet handled by a mod changes nothing.
     */
    public void handleRespawn(legacyconnector.v1_7_10.net.minecraft.network.play.server.S07PacketRespawn packet) {
    }

    /** The modern connection behind the legacy network manager calls. */
    private static final class ClientConnection extends legacyconnector.v1_7_10.net.minecraft.network.NetworkManager {
        final net.minecraft.network.Connection modern;

        ClientConnection(net.minecraft.network.Connection modern) {
            this.modern = modern;
        }

        @Override
        public boolean isLocalChannel() {
            return modern.isMemoryConnection();
        }

        @Override
        public boolean isChannelOpen() {
            return modern.isConnected();
        }

        @Override
        public java.net.SocketAddress getSocketAddress() {
            return modern.getRemoteAddress();
        }

        @Override
        public io.netty.channel.Channel channel() {
            return modern.channel();
        }
    }
}
