package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/** A netty pipeline mods build their own packet handling in; the layer only keeps it (its packets are not carried yet). */
public class FMLEmbeddedChannel extends EmbeddedChannel {
    public FMLEmbeddedChannel(String channelName, Side source, ChannelHandler... handlers) {
        super(handlers);
    }
}
