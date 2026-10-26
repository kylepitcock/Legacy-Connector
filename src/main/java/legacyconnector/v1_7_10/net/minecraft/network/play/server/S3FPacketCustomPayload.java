package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** A custom payload from the server on a named channel; sent over the legacy channel bridge. */
public class S3FPacketCustomPayload extends Packet {
    public String channel;
    public byte[] data;

    public S3FPacketCustomPayload() {
    }

    public S3FPacketCustomPayload(String channel, io.netty.buffer.ByteBuf data) {
        this(channel, mcc$bytes(data));
    }

    public S3FPacketCustomPayload(String channel, byte[] data) {
        this.channel = channel;
        this.data = data;
        if (data != null && data.length >= 1048576) throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
    }

    public static byte[] mcc$bytes(io.netty.buffer.ByteBuf buf) {
        if (buf == null) return new byte[0];
        io.netty.buffer.ByteBuf copy = buf.duplicate();
        byte[] out = new byte[copy.readableBytes()];
        copy.readBytes(out);
        return out;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        channel = buf.readStringFromBuffer(20);
        data = new byte[buf.readUnsignedShort()];
        buf.readBytes(data);
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(channel);
        buf.writeShort(data.length);
        buf.writeBytes(data);
    }

    public String func_149169_c() {
        return channel;
    }

    public byte[] func_149168_d() {
        return data;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge.send(
                new legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket(this),
                legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget.PLAYER,
                org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(player));
    }
}
