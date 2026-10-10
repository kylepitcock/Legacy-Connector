package mccompat.v1_7_10.cpw.mods.fml.common.network;

import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import mccompat.v1_7_10.cpw.mods.fml.relauncher.Side;
import mccompat.v1_7_10.net.minecraft.network.Packet;

/**
 * Per-side Netty pipeline of a legacy FML channel, exactly as FML 7.10 built it: mods add codecs and inbound
 * handlers to it. The head of the pipeline is {@link FMLOutboundHandler}, which turns outbound
 * {@link mccompat.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket}s into real network messages.
 */
public class FMLEmbeddedChannel extends EmbeddedChannel {
    private final String mcc$name;
    private final Side mcc$side;

    public FMLEmbeddedChannel(String channelName, Side source, ChannelHandler... handlers) {
        super(new FMLOutboundHandler());
        this.mcc$name = channelName;
        this.mcc$side = source;
        attr(NetworkRegistry.FML_CHANNEL).set(channelName);
        attr(NetworkRegistry.CHANNEL_SOURCE).set(source);
        pipeline().addLast(handlers);
    }

    public String mcc$name() {
        return mcc$name;
    }

    public Side mcc$side() {
        return mcc$side;
    }

    /** Encodes {@code object} through the pipeline without sending it, returning the resulting packet. */
    public Packet generatePacketFrom(Object object) {
        FMLOutboundHandler.OutboundTarget old = attr(FMLOutboundHandler.FML_MESSAGETARGET).get();
        attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.NOWHERE);
        writeOutbound(object);
        Packet packet = (Packet) outboundMessages().poll();
        attr(FMLOutboundHandler.FML_MESSAGETARGET).set(old);
        return packet;
    }

    public String findChannelHandlerNameForType(Class<? extends ChannelHandler> type) {
        for (var e : pipeline()) {
            if (type.isInstance(e.getValue())) return e.getKey();
        }
        return null;
    }

    public void cleanAttributes() {
        attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(null);
        attr(NetworkRegistry.NET_HANDLER).set(null);
    }
}
