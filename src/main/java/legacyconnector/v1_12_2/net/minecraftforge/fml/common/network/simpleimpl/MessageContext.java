package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl;

import legacyconnector.v1_12_2.net.minecraft.network.INetHandler;
import legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/** Where a message came from: the side handling it and that side's connection handler. */
public class MessageContext {
    /** The player's handler on the server; a client handler (only good for {@code getWorldThread}) on the client. */
    public final INetHandler netHandler;
    public final Side side;

    public MessageContext(INetHandler netHandler, Side side) {
        this.netHandler = netHandler == null ? CLIENT_HANDLER : netHandler;
        this.side = side;
    }

    public MessageContext(Side side, NetHandlerPlayServer serverHandler) {
        this(serverHandler, side);
    }

    public NetHandlerPlayServer getServerHandler() {
        if (side != Side.SERVER) throw new UnsupportedOperationException("Attempt to get server handler on the client side");
        return (NetHandlerPlayServer) netHandler;
    }

    /** The client's handler of a message from the server. */
    public static final INetHandler CLIENT_HANDLER = new legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient() {
        @Override
        public void onDisconnect(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent reason) {
        }
    };
}
