package legacyconnector.v1_7_10.net.minecraft.client.network;

/** Client connection handle; legacy handlers mostly ignore it and use Minecraft.getMinecraft(). */
public class NetHandlerPlayClient implements legacyconnector.v1_7_10.net.minecraft.network.INetHandler {
    public static final NetHandlerPlayClient INSTANCE = new NetHandlerPlayClient();
}
