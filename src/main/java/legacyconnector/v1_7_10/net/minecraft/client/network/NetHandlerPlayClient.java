package legacyconnector.v1_7_10.net.minecraft.client.network;

/** Client connection handle; legacy handlers mostly ignore it and use Minecraft.getMinecraft(). */
public class NetHandlerPlayClient implements legacyconnector.v1_7_10.net.minecraft.network.INetHandler {
    public static final NetHandlerPlayClient INSTANCE = new NetHandlerPlayClient();

    /** Queues a packet for the server: custom-channel packets go through the channel bridge, vanilla ones are not forwarded. */
    public void addToSendQueue(legacyconnector.v1_7_10.net.minecraft.network.Packet packet) {
        if (packet instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket fml) {
            org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge.send(fml,
                    legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget.TOSERVER, null);
        }
    }
}
