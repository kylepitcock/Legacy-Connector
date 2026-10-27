package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/**
 * One side's netty pipeline of a mod channel, as FML built it: the mod's codecs and handlers behind {@link FMLOutboundHandler},
 * which turns the packets they write into network messages. Received bytes are fed in by {@code FmlChannelBridge1122}.
 */
public class FMLEmbeddedChannel extends EmbeddedChannel {
    private final String mcc$name;

    public FMLEmbeddedChannel(String channelName, Side source, ChannelHandler... handlers) {
        super(handlers);
        pipeline().addFirst("fml:outbound", new FMLOutboundHandler());
        this.mcc$name = channelName;
        attr(NetworkRegistry.FML_CHANNEL).set(channelName);
        attr(NetworkRegistry.CHANNEL_SOURCE).set(source);
        try {
            checkException();
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Network").error("Setting up legacy channel {} ({}) failed", channelName, source, t);
        }
    }

    public String mcc$name() {
        return mcc$name;
    }

    /** Encodes {@code object} through the pipeline without sending it. */
    public Packet<?> generatePacketFrom(Object object) {
        FMLOutboundHandler.OutboundTarget old = attr(FMLOutboundHandler.FML_MESSAGETARGET).get();
        attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.NOWHERE);
        writeOutbound(object);
        Packet<?> packet = (Packet<?>) outboundMessages().poll();
        attr(FMLOutboundHandler.FML_MESSAGETARGET).set(old);
        return packet;
    }

    public String findChannelHandlerNameForType(Class<? extends ChannelHandler> type) {
        String last = null;
        for (var e : pipeline()) {
            if (type.isInstance(e.getValue()) || e.getValue().getClass().getName().equals(type.getName())) return e.getKey();
            if (!(e.getValue() instanceof FMLOutboundHandler)) last = e.getKey();
        }
        return last;
    }

    public void cleanAttributes() {
        attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(null);
        attr(NetworkRegistry.NET_HANDLER).set(null);
    }
}
