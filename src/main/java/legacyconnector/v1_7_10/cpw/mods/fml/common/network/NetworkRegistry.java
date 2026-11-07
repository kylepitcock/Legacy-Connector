package legacyconnector.v1_7_10.cpw.mods.fml.common.network;

import io.netty.channel.ChannelHandler;
import io.netty.util.AttributeKey;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.GuiBridge;

public enum NetworkRegistry {
    INSTANCE;

    public static final AttributeKey<String> FML_CHANNEL = AttributeKey.valueOf("fml:channelName");
    public static final AttributeKey<Side> CHANNEL_SOURCE = AttributeKey.valueOf("fml:channelSource");
    /** Server side: the NetHandlerPlayServer of the sending player. Client side: a NetHandlerPlayClient. */
    public static final AttributeKey<Object> NET_HANDLER = AttributeKey.valueOf("fml:netHandler");

    /**
     * FML's private {@code channels} table (side, then channel name to pipeline). Mods that build their own packet pipeline read it by
     * reflection (TheErebus' PacketPipeline: NoSuchFieldException: channels), so the field exists with FML's name and shape.
     */
    private final EnumMap<Side, Map<String, FMLEmbeddedChannel>> channels = new EnumMap<>(Side.class);

    NetworkRegistry() {
        for (Side side : Side.values()) channels.put(side, new ConcurrentHashMap<>());
    }

    /** Creates the client and server pipelines of a raw legacy channel (FML 7.10 "newChannel"). */
    public EnumMap<Side, FMLEmbeddedChannel> newChannel(String name, ChannelHandler... handlers) {
        EnumMap<Side, FMLEmbeddedChannel> created = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            FMLEmbeddedChannel channel = new FMLEmbeddedChannel(name, side, handlers);
            created.put(side, channel);
            this.channels.get(side).put(name, channel);
        }
        FmlChannelBridge.register(name, created);
        return created;
    }

    /** The pipeline of a channel created with newChannel (null when there is none). */
    public FMLEmbeddedChannel getChannel(String name, Side source) {
        return FmlChannelBridge.channel(name, source);
    }

    public boolean hasChannel(String name, Side source) {
        return FmlChannelBridge.has(name);
    }

    public FMLEventChannel newEventDrivenChannel(String name) {
        FMLEventChannel channel = new FMLEventChannel(name);
        FmlChannelBridge.registerEventChannel(channel);
        return channel;
    }

    public SimpleNetworkWrapper newSimpleChannel(String name) {
        return new SimpleNetworkWrapper(name);
    }

    public void registerGuiHandler(Object mod, IGuiHandler handler) {
        GuiBridge.register(mod, handler);
    }

    public static class TargetPoint {
        public final double x;
        public final double y;
        public final double z;
        public final double range;
        public final int dimension;

        public TargetPoint(int dimension, double x, double y, double z, double range) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.range = range;
            this.dimension = dimension;
        }
    }
}
