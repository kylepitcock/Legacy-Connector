package legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/**
 * Raw custom payload of a legacy channel: channel name plus bytes. Received packets carry the handler of the connection
 * they came in on ({@link #handler()}) and its dispatcher; codecs copy the dispatcher onto the packets they produce.
 */
public class FMLProxyPacket extends Packet {
    private final ByteBuf payload;
    private final String channel;
    private Side target;
    private INetHandler netHandler;
    private NetworkDispatcher dispatcher;

    public FMLProxyPacket(ByteBuf payload, String channel) {
        this.payload = payload;
        this.channel = channel;
    }

    /** A packet the client sends (a custom payload a mod built with the vanilla packet class). */
    public FMLProxyPacket(legacyconnector.v1_7_10.net.minecraft.network.play.client.C17PacketCustomPayload packet) {
        this(Unpooled.wrappedBuffer(packet.getData() == null ? new byte[0] : packet.getData()), packet.getChannel());
        this.target = Side.SERVER;
    }

    /** A packet the server sends. */
    public FMLProxyPacket(legacyconnector.v1_7_10.net.minecraft.network.play.server.S3FPacketCustomPayload packet) {
        this(Unpooled.wrappedBuffer(packet.func_149168_d() == null ? new byte[0] : packet.func_149168_d()), packet.func_149169_c());
        this.target = Side.CLIENT;
    }

    public String channel() {
        return channel;
    }

    public ByteBuf payload() {
        return payload;
    }

    /** The handler of the connection this packet arrived on (null for packets being sent). */
    public INetHandler handler() {
        return netHandler;
    }

    public void mcc$setHandler(INetHandler handler) {
        this.netHandler = handler;
    }

    public void setTarget(Side target) {
        this.target = target;
    }

    public Side getTarget() {
        return target;
    }

    public NetworkDispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(NetworkDispatcher networkDispatcher) {
        this.dispatcher = networkDispatcher;
    }

    public legacyconnector.v1_7_10.net.minecraft.network.play.client.C17PacketCustomPayload toC17Packet() {
        return new legacyconnector.v1_7_10.net.minecraft.network.play.client.C17PacketCustomPayload(channel, mcc$bytes());
    }

    public legacyconnector.v1_7_10.net.minecraft.network.play.server.S3FPacketCustomPayload toS3FPacket() {
        return new legacyconnector.v1_7_10.net.minecraft.network.play.server.S3FPacketCustomPayload(channel, mcc$bytes());
    }

    public byte[] mcc$bytes() {
        ByteBuf copy = payload.duplicate();
        byte[] data = new byte[copy.readableBytes()];
        copy.readBytes(data);
        return data;
    }
}
