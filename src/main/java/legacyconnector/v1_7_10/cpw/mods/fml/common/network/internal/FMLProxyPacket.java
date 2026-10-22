package legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal;

import io.netty.buffer.ByteBuf;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** Raw custom payload of a legacy channel: channel name plus bytes. */
public class FMLProxyPacket extends Packet {
    private final ByteBuf payload;
    private final String channel;

    public FMLProxyPacket(ByteBuf payload, String channel) {
        this.payload = payload;
        this.channel = channel;
    }

    public String channel() {
        return channel;
    }

    public ByteBuf payload() {
        return payload;
    }

    public byte[] mcc$bytes() {
        ByteBuf copy = payload.duplicate();
        byte[] data = new byte[copy.readableBytes()];
        copy.readBytes(data);
        return data;
    }
}
