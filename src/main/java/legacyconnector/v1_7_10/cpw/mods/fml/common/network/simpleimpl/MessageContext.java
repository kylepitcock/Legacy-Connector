package legacyconnector.v1_7_10.cpw.mods.fml.common.network.simpleimpl;

import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient;

public class MessageContext {
    public final Side side;
    private final NetHandlerPlayServer serverHandler;
    /** The handler of the connection the packet arrived on: the server's for a packet to the server, the client's otherwise. */
    public final legacyconnector.v1_7_10.net.minecraft.network.INetHandler netHandler;

    public MessageContext(Side side, NetHandlerPlayServer serverHandler) {
        this.side = side;
        this.serverHandler = serverHandler;
        this.netHandler = side == Side.SERVER && serverHandler != null ? serverHandler : NetHandlerPlayClient.INSTANCE;
    }

    public NetHandlerPlayServer getServerHandler() {
        return serverHandler;
    }

    public NetHandlerPlayClient getClientHandler() {
        return NetHandlerPlayClient.INSTANCE;
    }
}
