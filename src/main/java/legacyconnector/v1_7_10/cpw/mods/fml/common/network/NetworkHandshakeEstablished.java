package legacyconnector.v1_7_10.cpw.mods.fml.common.network;

import legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;

/** The Netty user event a channel pipeline gets when a connection's handshake is done (on the side it completed on). */
public class NetworkHandshakeEstablished {
    public final NetworkDispatcher dispatcher;
    public final INetHandler netHandler;
    public final Side side;

    public NetworkHandshakeEstablished(NetworkDispatcher dispatcher, INetHandler netHandler, Side origin) {
        this.dispatcher = dispatcher;
        this.netHandler = netHandler;
        this.side = origin;
    }
}
