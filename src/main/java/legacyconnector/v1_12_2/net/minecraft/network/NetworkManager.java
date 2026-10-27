package legacyconnector.v1_12_2.net.minecraft.network;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/**
 * A connection, as 1.12.2 mods see it: a view over a modern {@link net.minecraft.network.Connection} for real players, or a
 * detached one (mods build {@code new NetworkManager(EnumPacketDirection.SERVERBOUND)} for their fake players' handlers: it has no
 * channel and sends nothing).
 */
public class NetworkManager {
    private final EnumPacketDirection direction;
    /** The modern connection, null for a detached manager. */
    public net.minecraft.network.Connection mcc$connection;
    private INetHandler packetListener;

    public NetworkManager(EnumPacketDirection packetDirection) {
        this.direction = packetDirection;
    }

    public NetworkManager(net.minecraft.network.Connection connection) {
        this(EnumPacketDirection.SERVERBOUND);
        this.mcc$connection = connection;
    }

    public EnumPacketDirection getDirection() {
        return direction;
    }

    public void setNetHandler(INetHandler handler) {
        this.packetListener = handler;
    }

    public INetHandler getNetHandler() {
        return packetListener;
    }

    /** Sends through the player handler this manager belongs to (packets the shim knows become their modern form). */
    public void sendPacket(Packet<?> packet) {
        if (packetListener instanceof NetHandlerPlayServer handler) handler.sendPacket(packet);
    }

    public java.net.SocketAddress getRemoteAddress() {
        return mcc$connection == null ? null : mcc$connection.getRemoteAddress();
    }

    public boolean isChannelOpen() {
        return mcc$connection != null && mcc$connection.isConnected();
    }

    public boolean hasNoChannel() {
        return mcc$connection == null;
    }

    public boolean isLocalChannel() {
        return mcc$connection != null && mcc$connection.isMemoryConnection();
    }

    public boolean isEncrypted() {
        return mcc$connection != null && mcc$connection.isEncrypted();
    }

    public io.netty.channel.Channel channel() {
        return mcc$connection == null ? null : mcc$connection.channel();
    }

    public void closeChannel(ITextComponent message) {
        if (mcc$connection != null) {
            mcc$connection.disconnect(message == null ? net.minecraft.network.chat.Component.empty() : message.mcc$modern());
        }
    }

    /** The modern connection handles its own packets; nothing is queued here. */
    public void processReceivedPackets() {
    }

    public void disableAutoRead() {
        if (mcc$connection != null && mcc$connection.channel() != null) mcc$connection.channel().config().setAutoRead(false);
    }
}
