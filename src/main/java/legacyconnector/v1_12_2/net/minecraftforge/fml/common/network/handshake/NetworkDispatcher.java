package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.handshake;

import legacyconnector.v1_12_2.net.minecraft.network.INetHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.FMLOutboundHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/**
 * FML per-connection dispatcher. The layer has no FML handshake; a dispatcher stands for the connection a packet arrived on: its
 * handler, and {@link #sendProxy} sends a payload back over it (to that player, or to the server from a client).
 */
public class NetworkDispatcher {
    public static final io.netty.util.AttributeKey<NetworkDispatcher> FML_DISPATCHER = io.netty.util.AttributeKey.valueOf("fml:dispatcher");

    private final INetHandler netHandler;
    private final Side side;

    public NetworkDispatcher(INetHandler netHandler, Side side) {
        this.netHandler = netHandler;
        this.side = side;
    }

    public INetHandler getNetHandler() {
        return netHandler;
    }

    public void sendProxy(FMLProxyPacket packet) {
        if (packet == null) return;
        if (side == Side.SERVER && netHandler instanceof legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer h) {
            org.kylepitcock.legacyconnector.v1_12_2.bridge.FmlChannelBridge1122.send(packet, FMLOutboundHandler.OutboundTarget.PLAYER, h.player);
        } else {
            packet.setTarget(Side.SERVER);
            org.kylepitcock.legacyconnector.v1_12_2.bridge.FmlChannelBridge1122.send(packet, FMLOutboundHandler.OutboundTarget.TOSERVER, null);
        }
    }
}
