package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import legacyconnector.v1_12_2.net.minecraft.network.INetHandler;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/**
 * The raw payload of a mod channel: its name and bytes. Packets that arrived carry the handler of their connection
 * ({@link #handler()}); sent ones travel as the layer's modern payload ({@link #mcc$modern}).
 */
public class FMLProxyPacket implements Packet<INetHandler> {
    private final String channel;
    private Side target;
    private final PacketBuffer payload;
    private INetHandler netHandler;
    private Object dispatcher;

    public FMLProxyPacket(PacketBuffer payload, String channel) {
        this.channel = channel;
        this.payload = payload;
    }

    public FMLProxyPacket(ByteBuf payload, String channel) {
        this(payload instanceof PacketBuffer pb ? pb : new PacketBuffer(payload), channel);
    }

    @Override
    public void readPacketData(PacketBuffer buf) {
        throw new UnsupportedOperationException("FMLProxyPacket is not read from a buffer");
    }

    @Override
    public void writePacketData(PacketBuffer buf) {
        buf.writeBytes(payload.duplicate());
    }

    @Override
    public void processPacket(INetHandler handler) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.FmlChannelBridge1122.deliver(channel, mcc$bytes(),
                handler instanceof legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer ? Side.SERVER : Side.CLIENT, handler);
    }

    public String channel() {
        return channel;
    }

    public ByteBuf payload() {
        return payload;
    }

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

    /** The handshake dispatcher of the connection (not modelled; the layer has no FML handshake). */
    public Object mcc$dispatcher() {
        return dispatcher;
    }

    public byte[] mcc$bytes() {
        ByteBuf copy = payload.duplicate();
        byte[] data = new byte[copy.readableBytes()];
        copy.readBytes(data);
        return data;
    }

    public FMLProxyPacket copy() {
        FMLProxyPacket p = new FMLProxyPacket(Unpooled.wrappedBuffer(mcc$bytes()), channel);
        p.target = target;
        p.netHandler = netHandler;
        return p;
    }

    /** Sent from the server to a client (the payload direction a vanilla connection carries it in). */
    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        var p = org.kylepitcock.legacyconnector.v1_12_2.bridge.FmlChannelBridge1122.payload(this);
        return target == Side.SERVER ? new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(p)
                : new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(p);
    }
}
