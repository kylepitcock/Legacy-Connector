package org.kylepitcock.mccompat.v1_7_10.bridge;

import io.netty.buffer.Unpooled;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mccompat.v1_7_10.cpw.mods.fml.common.network.FMLEmbeddedChannel;
import mccompat.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler;
import mccompat.v1_7_10.cpw.mods.fml.common.network.NetworkRegistry;
import mccompat.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket;
import mccompat.v1_7_10.cpw.mods.fml.relauncher.Side;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import mccompat.v1_7_10.net.minecraft.network.NetHandlerPlayClient;
import mccompat.v1_7_10.net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.kylepitcock.mccompat.core.network.LegacyNetwork;
import org.kylepitcock.mccompat.core.network.LegacyPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Raw FML 7.10 channels ({@code NetworkRegistry.newChannel}) over {@link LegacyPayload}. Outbound packets leave the
 * legacy pipeline as {@link FMLProxyPacket}s; inbound bytes are injected into the receiving side's pipeline, where
 * the mod's own codec and handlers process them exactly as on FML.
 */
public final class FmlChannelBridge {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/FmlChannels");
    static final String FAMILY = "1.7.10-fml";
    private static final Map<String, EnumMap<Side, FMLEmbeddedChannel>> CHANNELS = new ConcurrentHashMap<>();

    static {
        LegacyNetwork.registerReceiver(FAMILY, FmlChannelBridge::receive);
    }

    private FmlChannelBridge() {}

    public static void register(String name, EnumMap<Side, FMLEmbeddedChannel> channels) {
        CHANNELS.put(name, channels);
    }

    public static boolean has(String name) {
        return CHANNELS.containsKey(name) || EVENT_CHANNELS.containsKey(name);
    }

    private static final Map<String, mccompat.v1_7_10.cpw.mods.fml.common.network.FMLEventChannel> EVENT_CHANNELS = new ConcurrentHashMap<>();

    public static void registerEventChannel(mccompat.v1_7_10.cpw.mods.fml.common.network.FMLEventChannel channel) {
        EVENT_CHANNELS.put(channel.mcc$name(), channel);
    }

    private static LegacyPayload payload(FMLProxyPacket packet) {
        return new LegacyPayload(FAMILY, packet.channel(), 0, packet.mcc$bytes());
    }

    public static void send(FMLProxyPacket packet, FMLOutboundHandler.OutboundTarget target, Object args) {
        LegacyPayload p = payload(packet);
        var server = ServerLifecycleHooks.getCurrentServer();
        switch (target) {
            case TOSERVER -> PacketDistributor.sendToServer(p);
            case ALL -> PacketDistributor.sendToAllPlayers(p);
            case PLAYER -> {
                if (args instanceof EntityPlayerMP player) PacketDistributor.sendToPlayer(player.mcc$serverPlayer(), p);
            }
            case DIMENSION -> {
                ServerLevel level = server == null || !(args instanceof Integer dim) ? null : DimensionBridge.level(server, dim);
                if (level != null) PacketDistributor.sendToPlayersInDimension(level, p);
            }
            case ALLAROUNDPOINT -> {
                if (args instanceof NetworkRegistry.TargetPoint t && server != null) {
                    ServerLevel level = DimensionBridge.level(server, t.dimension);
                    if (level != null) PacketDistributor.sendToPlayersNear(level, null, t.x, t.y, t.z, t.range, p);
                }
            }
            default -> LOG.debug("Unsupported legacy outbound target {} on channel {}", target, packet.channel());
        }
    }

    private static void receive(LegacyPayload payload, IPayloadContext context) {
        boolean server = context.player() instanceof ServerPlayer;
        context.enqueueWork(() -> deliver(payload.channel(), payload.data(), server ? Side.SERVER : Side.CLIENT,
                server ? new NetHandlerPlayServer((EntityPlayerMP) EntityBridge.wrap(context.player())) : NetHandlerPlayClient.INSTANCE));
    }

    /** Feeds raw bytes into a side's pipeline (also used for tile entity description packets). */
    public static void deliver(String channelName, byte[] data, Side side, Object netHandler) {
        var eventChannel = EVENT_CHANNELS.get(channelName);
        if (eventChannel != null) {
            FMLProxyPacket packet = new FMLProxyPacket(Unpooled.wrappedBuffer(data), channelName);
            var event = side == Side.SERVER
                    ? new mccompat.v1_7_10.cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent(null, packet,
                            (mccompat.v1_7_10.net.minecraft.network.INetHandler) netHandler)
                    : new mccompat.v1_7_10.cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent(null, packet);
            try {
                eventChannel.mcc$bus().post(event);
                if (event.reply != null) {
                    if (netHandler instanceof NetHandlerPlayServer s) send(event.reply, FMLOutboundHandler.OutboundTarget.PLAYER, s.playerEntity);
                    else send(event.reply, FMLOutboundHandler.OutboundTarget.TOSERVER, null);
                }
            } catch (RuntimeException e) {
                LOG.error("Legacy event channel {} failed to handle a packet", channelName, e);
            }
            return;
        }
        EnumMap<Side, FMLEmbeddedChannel> channels = CHANNELS.get(channelName);
        if (channels == null) {
            LOG.debug("Message for unknown legacy channel {}", channelName);
            return;
        }
        FMLEmbeddedChannel channel = channels.get(side);
        try {
            channel.attr(NetworkRegistry.NET_HANDLER).set(netHandler);
            channel.writeInbound(new FMLProxyPacket(Unpooled.wrappedBuffer(data), channelName));
            // Messages nobody consumed would accumulate in the embedded channel.
            while (channel.readInbound() != null) {
                // drop
            }
        } catch (RuntimeException e) {
            LOG.error("Legacy channel {} failed to handle a message", channelName, e);
        }
    }
}
