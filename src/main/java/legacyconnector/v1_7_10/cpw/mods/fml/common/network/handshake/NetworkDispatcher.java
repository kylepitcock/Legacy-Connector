package legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import java.net.SocketAddress;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;
import legacyconnector.v1_7_10.net.minecraft.network.NetworkManager;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge;

/**
 * FML's per-connection packet dispatcher. Mods find it on a channel ({@link #FML_DISPATCHER}, set on every legacy
 * channel while it handles a message), on a packet ({@code FMLProxyPacket.getDispatcher()}) or by connection
 * ({@link #get}), and answer the other end through {@link #sendProxy}. The modern network carries the packets; this
 * object only knows which end it stands for.
 */
public class NetworkDispatcher extends SimpleChannelInboundHandler<Packet> implements ChannelOutboundHandler {
    public static final AttributeKey<NetworkDispatcher> FML_DISPATCHER = AttributeKey.valueOf("legacyconnector:fml_dispatcher");
    public static final AttributeKey<Boolean> IS_LOCAL = AttributeKey.valueOf("legacyconnector:fml_is_local");

    public final NetworkManager manager;
    private final Side side;

    public NetworkDispatcher(NetworkManager manager) {
        super(Packet.class, false);
        this.manager = manager;
        this.side = manager != null && manager.isClientSide ? Side.CLIENT : Side.SERVER;
    }

    public static NetworkDispatcher get(NetworkManager manager) {
        return manager == null ? null : manager.mcc$dispatcher();
    }

    /** The dispatcher of a server-side player's connection. */
    public static NetworkDispatcher mcc$forPlayer(net.minecraft.server.level.ServerPlayer player) {
        return player == null || player.connection == null ? null : get(NetworkManager.mcc$of(player.connection.getConnection()));
    }

    public INetHandler getNetHandler() {
        return manager == null ? null : manager.getNetHandler();
    }

    /** Sends a custom payload to the other end of this connection. */
    public void sendProxy(FMLProxyPacket packet) {
        if (packet == null) return;
        if (side == Side.CLIENT) {
            FmlChannelBridge.send(packet, FMLOutboundHandler.OutboundTarget.TOSERVER, null);
            return;
        }
        var player = manager == null ? null : manager.mcc$player();
        if (player != null) {
            FmlChannelBridge.send(packet, FMLOutboundHandler.OutboundTarget.PLAYER,
                    org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(player));
        }
    }

    public Side mcc$side() {
        return side;
    }

    public void clientListenForServerHandshake() {
    }

    public void serverToClientHandshake(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP player) {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) {
        ctx.fireChannelRead(msg);
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) {
        ctx.bind(localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.disconnect(promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.close(promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.deregister(promise);
    }

    @Override
    public void read(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof FMLProxyPacket packet) {
            sendProxy(packet);
            promise.setSuccess();
        } else {
            ctx.write(msg, promise);
        }
    }

    @Override
    public void flush(ChannelHandlerContext ctx) {
        ctx.flush();
    }
}
