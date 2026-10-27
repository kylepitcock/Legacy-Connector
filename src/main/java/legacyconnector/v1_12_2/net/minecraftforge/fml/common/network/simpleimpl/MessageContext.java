package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl;

import legacyconnector.v1_12_2.net.minecraft.network.INetHandler;
import legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

public class MessageContext {
    /** The handler of the connection the message came in on: the sender's {@link NetHandlerPlayServer} on the server, null on the client. */
    public final INetHandler netHandler;
    public final Side side;
    private final NetHandlerPlayServer serverHandler;

    public MessageContext(Side side, NetHandlerPlayServer serverHandler) {
        this.side = side;
        this.serverHandler = serverHandler;
        this.netHandler = serverHandler;
    }

    public MessageContext(INetHandler handler, Side side) {
        this.side = side;
        this.netHandler = handler;
        this.serverHandler = handler instanceof NetHandlerPlayServer s ? s : null;
    }

    public NetHandlerPlayServer getServerHandler() {
        if (side != Side.SERVER) throw new UnsupportedOperationException("Attempt to get server handler on the client side");
        return serverHandler;
    }
}
