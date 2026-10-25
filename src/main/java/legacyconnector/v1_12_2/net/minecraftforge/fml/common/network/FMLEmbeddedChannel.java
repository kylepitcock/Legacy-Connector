package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/** A netty pipeline mods build their own packet handling in; the layer only keeps it (its packets are not carried yet). */
public class FMLEmbeddedChannel extends EmbeddedChannel {
    public FMLEmbeddedChannel(String channelName, Side source, ChannelHandler... handlers) {
        super();
        // the handler names mods address when they build their own pipeline (addBefore("fml:outbound", ...))
        pipeline().addLast("fml:outbound", new io.netty.channel.ChannelOutboundHandlerAdapter());
        pipeline().addLast("fml:inbound", new io.netty.channel.ChannelInboundHandlerAdapter());
        for (ChannelHandler h : handlers) pipeline().addLast(h);
    }
}
