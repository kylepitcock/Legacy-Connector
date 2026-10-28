package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A custom payload on a named channel. */
public class SPacketCustomPayload implements Packet<INetHandlerPlayClient> {
    private String channel;
    private legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer data;

    public SPacketCustomPayload() {
    }

    public SPacketCustomPayload(String channelIn, legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer bufIn) {
        this.channel = channelIn;
        this.data = bufIn;
        if (bufIn.writerIndex() > 1048576) throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
    }

    public String getChannelName() {
        return channel;
    }

    public legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer getBufferData() {
        return data;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        channel = buf.readString(20);
        int i = buf.readableBytes();
        if (i < 0 || i > 1048576) throw new IOException("Payload may not be larger than 1048576 bytes");
        data = new legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer(buf.readBytes(i));
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeString(channel);
        buf.writeBytes(data);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    /** Sent as the layer's mod channel payload: the client's legacy channel of that name gets it. */
    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        var proxy = new legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal.FMLProxyPacket(
                new legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer(data.copy()), channel);
        return new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(org.kylepitcock.legacyconnector.v1_12_2.bridge.FmlChannelBridge1122.payload(proxy));
    }
}
