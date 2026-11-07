package legacyconnector.v1_6_4.net.minecraft.network.packet;

import io.netty.buffer.Unpooled;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket;
import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.PayloadWire;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge;

/**
 * 1.6.4's custom payload: a channel name and bytes, the transport of every mod's own protocol. Sent through the layer's FML channel bridge
 * (the same payload the 1.7.10 channels use), and received by the {@code IPacketHandler}s registered for the channel
 * ({@code bridge.Network164}).
 */
public class Packet250CustomPayload extends Packet {
    public String channel;
    public int length;
    public byte[] data;

    public Packet250CustomPayload() {}

    public Packet250CustomPayload(String channel, byte[] data) {
        this.channel = channel;
        this.data = data;
        this.length = data == null ? 0 : data.length;
    }

    /** The bytes to send (see {@link PayloadWire#bytes}). */
    public byte[] mcc$bytes() {
        return PayloadWire.bytes(data, length);
    }

    public FMLProxyPacket mcc$proxy() {
        return new FMLProxyPacket(Unpooled.wrappedBuffer(mcc$bytes()), channel == null ? "" : channel);
    }

    public void readPacketData(DataInput in) throws IOException {
        channel = PayloadWire.readString(in, 20);
        length = in.readShort();
        if (length > 0 && length < 32767) {
            data = new byte[length];
            in.readFully(data);
        }
    }

    public void writePacketData(DataOutput out) throws IOException {
        PayloadWire.writeString(channel, out);
        out.writeShort((short) length);
        if (data != null) out.write(data);
    }

    public int getPacketSize() {
        return 2 + (channel == null ? 0 : channel.length() * 2) + 2 + length;
    }

    @Override
    public void processPacket(INetHandler handler) {
        // received packets are routed by channel (Network164), not through the handler
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        FmlChannelBridge.send(mcc$proxy(), FMLOutboundHandler.OutboundTarget.PLAYER, EntityBridge.wrap(player));
    }

    @Override
    public void mcc$sendToServer() {
        FmlChannelBridge.send(mcc$proxy(), FMLOutboundHandler.OutboundTarget.TOSERVER, null);
    }
}
