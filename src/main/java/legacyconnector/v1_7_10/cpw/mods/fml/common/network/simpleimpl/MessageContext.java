package legacyconnector.v1_7_10.cpw.mods.fml.common.network.simpleimpl;

import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient;

public class MessageContext {
    public final Side side;
    private final NetHandlerPlayServer serverHandler;

    public MessageContext(Side side, NetHandlerPlayServer serverHandler) {
        this.side = side;
        this.serverHandler = serverHandler;
    }

    public NetHandlerPlayServer getServerHandler() {
        return serverHandler;
    }

    public NetHandlerPlayClient getClientHandler() {
        return NetHandlerPlayClient.INSTANCE;
    }
}
