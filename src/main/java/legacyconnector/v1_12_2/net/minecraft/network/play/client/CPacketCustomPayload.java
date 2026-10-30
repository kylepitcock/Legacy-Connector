package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;

/** A payload on a named channel to the server: sent as the layer's mod channel payload, so the server's legacy channel of that name gets it. */
public class CPacketCustomPayload implements Packet<INetHandlerPlayServer> {
    private String channel;
    private PacketBuffer data;

    public CPacketCustomPayload() {
    }

    public CPacketCustomPayload(String channelIn, PacketBuffer bufIn) {
        this.channel = channelIn;
        this.data = bufIn;
        if (bufIn.writerIndex() > 32767) throw new IllegalArgumentException("Payload may not be larger than 32767 bytes");
    }

    public String getChannelName() {
        return channel;
    }

    public PacketBuffer getBufferData() {
        return data;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        channel = buf.readString(20);
        int i = buf.readableBytes();
        if (i < 0 || i > 32767) throw new IOException("Payload may not be larger than 32767 bytes");
        data = new PacketBuffer(buf.readBytes(i));
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeString(channel);
        buf.writeBytes(data.copy());
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        var proxy = new legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal.FMLProxyPacket(new PacketBuffer(data.copy()), channel);
        return new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(org.kylepitcock.legacyconnector.v1_12_2.bridge.FmlChannelBridge1122.payload(proxy));
    }
}
