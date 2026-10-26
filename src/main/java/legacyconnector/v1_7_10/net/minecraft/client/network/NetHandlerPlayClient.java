package legacyconnector.v1_7_10.net.minecraft.client.network;

/** Client connection handle; legacy handlers mostly ignore it and use Minecraft.getMinecraft(). */
public class NetHandlerPlayClient implements legacyconnector.v1_7_10.net.minecraft.network.play.INetHandlerPlayClient {
    public static final NetHandlerPlayClient INSTANCE = new NetHandlerPlayClient();

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
}
