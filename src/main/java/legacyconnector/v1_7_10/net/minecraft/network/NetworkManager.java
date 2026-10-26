package legacyconnector.v1_7_10.net.minecraft.network;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.Connection;

/**
 * A network connection, as a view over the modern {@link Connection}: one per connection, so mods can key state by it
 * (FML's {@code NetworkDispatcher.get}). Mods also subclass it for fake players' connections (CoFH, EnderIO), which
 * have no modern connection behind them.
 */
public class NetworkManager {
    public static final NetworkManager MCC_DUMMY = new NetworkManager();
    private static final Map<Connection, NetworkManager> BY_CONNECTION = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private final Connection mcc$connection;
    /** Whether this is the client's end of the connection. */
    public boolean isClientSide;
    /** The connection's Netty channel (null for fake connections). */
    public io.netty.channel.Channel channel;
    private INetHandler mcc$netHandler;
    private legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher mcc$dispatcher;

    public NetworkManager() {
        this(false);
    }

    public NetworkManager(boolean isClientSide) {
        this.isClientSide = isClientSide;
        this.mcc$connection = null;
    }

    private NetworkManager(Connection connection) {
        this.mcc$connection = connection;
        this.isClientSide = connection.getReceiving() == net.minecraft.network.protocol.PacketFlow.CLIENTBOUND;
        this.channel = connection.channel();
    }

    /** The legacy view of a modern connection (always the same object for one connection). */
    public static NetworkManager mcc$of(Connection connection) {
        if (connection == null) return MCC_DUMMY;
        return BY_CONNECTION.computeIfAbsent(connection, NetworkManager::new);
    }

    public Connection mcc$connection() {
        return mcc$connection;
    }

    /** The player on the other end of a server-side connection, or null. */
    public net.minecraft.server.level.ServerPlayer mcc$player() {
        if (mcc$connection != null && mcc$connection.getPacketListener() instanceof net.minecraft.server.network.ServerGamePacketListenerImpl l) return l.player;
        return null;
    }

    /** FML's dispatcher of this connection (made on first use; also set as the channel's {@code FML_DISPATCHER} attribute). */
    public synchronized legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher mcc$dispatcher() {
        if (mcc$dispatcher == null) {
            mcc$dispatcher = new legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher(this);
            if (channel != null) {
                try {
                    channel.attr(legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher.FML_DISPATCHER).set(mcc$dispatcher);
                } catch (RuntimeException ignored) {
                    // a closed channel
                }
            }
        }
        return mcc$dispatcher;
    }

    public boolean isLocalChannel() {
        return mcc$connection != null && mcc$connection.isMemoryConnection();
    }

    public boolean isChannelOpen() {
        return mcc$connection == null || mcc$connection.isConnected();
    }

    public java.net.SocketAddress getSocketAddress() {
        return mcc$connection == null ? null : mcc$connection.getRemoteAddress();
    }

    public io.netty.channel.Channel channel() {
        return channel;
    }

    /** The legacy handler of this connection: the player's server handler, or the client's. */
    public INetHandler getNetHandler() {
        if (mcc$netHandler != null) return mcc$netHandler;
        var player = mcc$player();
        if (player != null) {
            return new NetHandlerPlayServer((legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP)
                    org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(player));
        }
        if (isClientSide) return legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient.INSTANCE;
        return null;
    }

    public void setNetHandler(INetHandler handler) {
        this.mcc$netHandler = handler;
    }

    /** Sends a packet to the other end (fake connections drop it). */
    public void scheduleOutboundPacket(Packet packet, io.netty.util.concurrent.GenericFutureListener... listeners) {
        if (packet == null || mcc$connection == null) return;
        if (isClientSide) {
            legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient.INSTANCE.addToSendQueue(packet);
        } else if (getNetHandler() instanceof NetHandlerPlayServer server) {
            server.sendPacket(packet);
        }
    }

    public void processReceivedPackets() {
    }

    public void closeChannel(legacyconnector.v1_7_10.net.minecraft.util.IChatComponent message) {
        if (mcc$connection != null) {
            mcc$connection.disconnect(message == null ? net.minecraft.network.chat.Component.empty() : message.mcc$toModern());
        }
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IChatComponent getExitMessage() {
        return null;
    }

    public void disableAutoRead() {
        if (channel != null) channel.config().setAutoRead(false);
    }
}
