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
}
