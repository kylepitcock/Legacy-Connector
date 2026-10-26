package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * A custom payload from the client on a named channel. Sent over the legacy channel bridge; the server side handles
 * vanilla's own channels ({@code MC|ItemName} renames in anvils, {@code MC|TrSel} villager trades, {@code MC|Beacon}).
 */
public class C17PacketCustomPayload extends Packet {
    public String channel;
    public int length;
    public byte[] data;

    public C17PacketCustomPayload() {
    }

    public C17PacketCustomPayload(String channel, io.netty.buffer.ByteBuf data) {
        this(channel, legacyconnector.v1_7_10.net.minecraft.network.play.server.S3FPacketCustomPayload.mcc$bytes(data));
    }

    public C17PacketCustomPayload(String channel, byte[] data) {
        this.channel = channel;
        this.data = data;
        if (data != null) {
            this.length = data.length;
            if (this.length >= 32767) throw new IllegalArgumentException("Payload may not be larger than 32k");
        }
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        channel = buf.readStringFromBuffer(20);
        length = buf.readShort();
        if (length > 0 && length < 32767) {
            data = new byte[length];
            buf.readBytes(data);
        }
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(channel);
        buf.writeShort((short) length);
        if (data != null) buf.writeBytes(data);
    }

    public String getChannel() {
        return channel;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public void mcc$sendToServer() {
        org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge.send(
                new legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket(this),
                legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget.TOSERVER, null);
    }
}
