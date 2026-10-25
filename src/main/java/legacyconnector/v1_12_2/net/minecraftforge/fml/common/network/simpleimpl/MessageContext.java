package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl;

import legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

public class MessageContext {
    public final Side side;
    private final NetHandlerPlayServer serverHandler;

    public MessageContext(Side side, NetHandlerPlayServer serverHandler) {
        this.side = side;
        this.serverHandler = serverHandler;
    }

    public NetHandlerPlayServer getServerHandler() {
        if (side != Side.SERVER) throw new UnsupportedOperationException("Attempt to get server handler on the client side");
        return serverHandler;
    }
}
