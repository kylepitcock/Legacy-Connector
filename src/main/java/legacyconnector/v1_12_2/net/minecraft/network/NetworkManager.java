package legacyconnector.v1_12_2.net.minecraft.network;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** A connection: a view over the modern one (a fake player's is a modern connection that is never opened). */
public class NetworkManager {
    private final net.minecraft.network.Connection connection;

    public NetworkManager(EnumPacketDirection packetDirection) {
        this(new net.minecraft.network.Connection(packetDirection.mcc$modern()));
    }

    protected NetworkManager(net.minecraft.network.Connection connection) {
        this.connection = connection;
    }

    public static NetworkManager mcc$of(net.minecraft.network.Connection connection) {
        return connection == null ? null : new NetworkManager(connection);
    }

    public net.minecraft.network.Connection mcc$modern() {
        return connection;
    }

    public void sendPacket(Packet<?> packetIn) {
        var modern = packetIn == null ? null : packetIn.mcc$modern();
        if (modern != null && connection.isConnected()) connection.send(modern);
    }

    /** 1.12 send with completion listeners: the packet is sent, the listeners are not called (the modern connection reports no result here). */
    @SafeVarargs
    public final void sendPacket(Packet<?> packetIn, io.netty.util.concurrent.GenericFutureListener<? extends io.netty.util.concurrent.Future<? super Void>> listener,
            io.netty.util.concurrent.GenericFutureListener<? extends io.netty.util.concurrent.Future<? super Void>>... listeners) {
        sendPacket(packetIn);
    }

    public java.net.SocketAddress getRemoteAddress() {
        return connection.getRemoteAddress();
    }

    public void closeChannel(ITextComponent message) {
        connection.disconnect(message == null ? net.minecraft.network.chat.Component.empty() : message.mcc$modern());
    }

    public boolean isLocalChannel() {
        return connection.isMemoryConnection();
    }

    public boolean isChannelOpen() {
        return connection.isConnected();
    }

    public boolean hasNoChannel() {
        return connection.channel() == null;
    }

    public io.netty.channel.Channel channel() {
        return connection.channel();
    }

    public boolean isEncrypted() {
        return connection.isEncrypted();
    }

    public void disableAutoRead() {
        if (connection.channel() != null) connection.channel().config().setAutoRead(false);
    }


    private INetHandler mcc$handler;

    public EnumPacketDirection getDirection() {
        return connection.getReceiving() == net.minecraft.network.protocol.PacketFlow.CLIENTBOUND ? EnumPacketDirection.CLIENTBOUND
                : EnumPacketDirection.SERVERBOUND;
    }

    public void setNetHandler(INetHandler handler) {
        this.mcc$handler = handler;
    }

    public INetHandler getNetHandler() {
        return mcc$handler;
    }

    /** The modern connection handles its own packets; nothing is queued here. */
    public void processReceivedPackets() {
    }

    /** The channel attribute 1.12 kept the connection state in. */
    public static final io.netty.util.AttributeKey<EnumConnectionState> PROTOCOL_ATTRIBUTE_KEY = io.netty.util.AttributeKey.valueOf("legacy_protocol");

    /** The game switches the connection's protocol itself; the requested state is only remembered on the channel. */
    public void setConnectionState(EnumConnectionState state) {
        var ch = channel();
        if (ch != null) ch.attr(PROTOCOL_ATTRIBUTE_KEY).set(state);
    }

    // ---- netty handler surface (1.12's manager is a SimpleChannelInboundHandler; the modern connection owns the pipeline)

    public void channelActive(io.netty.channel.ChannelHandlerContext context) {
    }

    public void channelInactive(io.netty.channel.ChannelHandlerContext context) {
    }

    public void exceptionCaught(io.netty.channel.ChannelHandlerContext context, Throwable throwable) {
    }

    /** Sets the compression threshold of the connection (-1 turns compression off). */
    public void setCompressionThreshold(int threshold) {
        if (connection.channel() != null && connection.isConnected()) connection.setupCompression(threshold, false);
    }

    /** Encryption is negotiated by the modern login; a mod enabling it by hand has nothing left to do. */
    public void enableEncryption(javax.crypto.SecretKey key) {
    }

    /** Handles a lost connection: the modern connection closes itself and tells its listener. */
    public void handleDisconnection() {
        connection.handleDisconnection();
    }

    /** The reason the connection ended, a generic one when the modern connection recorded none. */
    public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getExitMessage() {
        return new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation("multiplayer.disconnect.generic");
    }

    // ---- gap batch (kept together at the end)

    /** Phantom: the netty channel (redirected to {@link #mcc$get$channel}). */
    @Deprecated
    public io.netty.channel.Channel channel;

    public static io.netty.channel.Channel mcc$get$channel(NetworkManager m) {
        return m.channel();
    }
}
