package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class NetworkRegistry {
    public static final NetworkRegistry INSTANCE = new NetworkRegistry();
    public static final io.netty.util.AttributeKey<String> FML_CHANNEL = io.netty.util.AttributeKey.valueOf("fml:channelName");
    public static final io.netty.util.AttributeKey<legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side> CHANNEL_SOURCE =
            io.netty.util.AttributeKey.valueOf("fml:channelSource");
    public static final io.netty.util.AttributeKey<legacyconnector.v1_12_2.net.minecraft.network.INetHandler> NET_HANDLER =
            io.netty.util.AttributeKey.valueOf("fml:netHandler");
    public static final io.netty.util.AttributeKey<Boolean> FML_MARKER = io.netty.util.AttributeKey.valueOf("fml:hasMarker");

    public SimpleNetworkWrapper newSimpleChannel(String name) {
        return new SimpleNetworkWrapper(name);
    }

    private final java.util.Map<String, java.util.EnumMap<legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side, FMLEmbeddedChannel>> channels =
            new java.util.concurrent.ConcurrentHashMap<>();

    public java.util.EnumMap<legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side, FMLEmbeddedChannel> newChannel(String name, io.netty.channel.ChannelHandler... handlers) {
        var map = new java.util.EnumMap<legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side, FMLEmbeddedChannel>(legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.class);
        for (var side : legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side.values()) map.put(side, new FMLEmbeddedChannel(name, side, handlers));
        channels.put(name, map);
        org.kylepitcock.legacyconnector.v1_12_2.bridge.FmlChannelBridge1122.register(name, map);
        return map;
    }

    public boolean hasChannel(String name, legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side side) {
        return channels.containsKey(name);
    }

    public FMLEmbeddedChannel getChannel(String name, legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side side) {
        var map = channels.get(name);
        return map == null ? null : map.get(side);
    }

    public FMLEventChannel newEventDrivenChannel(String name) {
        return new FMLEventChannel(name);
    }

    public void registerGuiHandler(Object mod, IGuiHandler handler) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122.register(mod, handler);
    }

    public static class TargetPoint {
        public final int dimension;
        public final double x;
        public final double y;
        public final double z;
        public final double range;

        public TargetPoint(int dimension, double x, double y, double z, double range) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.range = range;
        }
    }
}
