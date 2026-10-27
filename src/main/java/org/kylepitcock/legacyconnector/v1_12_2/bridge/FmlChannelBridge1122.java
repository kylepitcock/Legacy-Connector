package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.network.INetHandler;
import legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.FMLEventChannel;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.FMLNetworkEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.FMLOutboundHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.NetworkRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.kylepitcock.legacyconnector.core.network.LegacyNetwork;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.12.2 raw FML channels ({@code NetworkRegistry.newChannel}, {@code newEventDrivenChannel}) over {@link LegacyPayload}: outbound
 * packets leave the mod's pipeline as {@link FMLProxyPacket}s; received bytes are fed into the receiving side's pipeline (or posted
 * on the event channel's bus), where the mod's own codecs and handlers process them as on FML.
 */
public final class FmlChannelBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/FmlChannels");
    static final String FAMILY = "1.12.2-fml";
    private static final Map<String, EnumMap<Side, FMLEmbeddedChannel>> CHANNELS = new ConcurrentHashMap<>();
    private static final Map<String, FMLEventChannel> EVENT_CHANNELS = new ConcurrentHashMap<>();

    static {
        LegacyNetwork.registerReceiver(FAMILY, FmlChannelBridge1122::receive);
    }

    private FmlChannelBridge1122() {}

    public static void register(String name, EnumMap<Side, FMLEmbeddedChannel> channels) {
        CHANNELS.put(name, channels);
    }

    public static void registerEventChannel(FMLEventChannel channel) {
        EVENT_CHANNELS.put(channel.mcc$name(), channel);
    }

    public static LegacyPayload payload(FMLProxyPacket packet) {
        return new LegacyPayload(FAMILY, packet.channel(), 0, packet.mcc$bytes());
    }

    private static ServerLevel level(int dimension) {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$level(dimension);
    }

    public static void send(FMLProxyPacket packet, FMLOutboundHandler.OutboundTarget target, Object args) {
        LegacyPayload p = payload(packet);
        switch (target) {
            case TOSERVER -> PacketDistributor.sendToServer(p);
            case ALL, DISPATCHER_REPLY_TO_ALL -> PacketDistributor.sendToAllPlayers(p);
            case PLAYER -> {
                if (args instanceof EntityPlayerMP player && player.mcc$serverPlayer() != null) PacketDistributor.sendToPlayer(player.mcc$serverPlayer(), p);
            }
            case DIMENSION -> {
                ServerLevel level = args instanceof Integer dim ? level(dim) : null;
                if (level != null) PacketDistributor.sendToPlayersInDimension(level, p);
            }
            case ALLAROUNDPOINT -> {
                if (args instanceof NetworkRegistry.TargetPoint t) {
                    ServerLevel level = level(t.dimension);
                    if (level != null) PacketDistributor.sendToPlayersNear(level, null, t.x, t.y, t.z, t.range, p);
                }
            }
            case TRACKING_POINT -> {
                if (args instanceof NetworkRegistry.TargetPoint t) {
                    ServerLevel level = level(t.dimension);
                    if (level != null) {
                        PacketDistributor.sendToPlayersTrackingChunk(level, new net.minecraft.world.level.ChunkPos(net.minecraft.util.Mth.floor(t.x) >> 4,
                                net.minecraft.util.Mth.floor(t.z) >> 4), p);
                    }
                }
            }
            case TRACKING_ENTITY -> {
                if (args instanceof legacyconnector.v1_12_2.net.minecraft.entity.Entity e && e.mcc$modern() != null) {
                    PacketDistributor.sendToPlayersTrackingEntity(e.mcc$modern(), p);
                }
            }
            case REPLY -> {
                if (args instanceof NetHandlerPlayServer h && h.player != null && h.player.mcc$serverPlayer() != null) {
                    PacketDistributor.sendToPlayer(h.player.mcc$serverPlayer(), p);
                } else if (args != null) {
                    PacketDistributor.sendToServer(p);
                }
            }
            default -> LOG.debug("Unsupported legacy outbound target {} on channel {}", target, packet.channel());
        }
    }

    private static void receive(LegacyPayload payload, IPayloadContext context) {
        boolean server = context.player() instanceof ServerPlayer;
        context.enqueueWork(() -> deliver(payload.channel(), payload.data(), server ? Side.SERVER : Side.CLIENT,
                server ? new NetHandlerPlayServer((EntityPlayerMP) EntityBridge1122.wrap(context.player())) : MessageContext.CLIENT_HANDLER,
                legacyconnector.v1_12_2.net.minecraft.network.NetworkManager.mcc$of(context.connection())));
    }

    public static void deliver(String channelName, byte[] data, Side side, Object netHandler) {
        deliver(channelName, data, side, netHandler, null);
    }

    private static void deliver(String channelName, byte[] data, Side side, Object netHandler, legacyconnector.v1_12_2.net.minecraft.network.NetworkManager manager) {
        FMLProxyPacket packet = new FMLProxyPacket(org.kylepitcock.legacyconnector.core.network.LegacyBuffers.wrap(data), channelName);
        if (netHandler instanceof INetHandler h) packet.mcc$setHandler(h);
        packet.setTarget(side);
        FMLEventChannel eventChannel = EVENT_CHANNELS.get(channelName);
        if (eventChannel != null) {
            FMLNetworkEvent.CustomPacketEvent<?> event = side == Side.SERVER ? new FMLNetworkEvent.ServerCustomPacketEvent(manager, packet)
                    : new FMLNetworkEvent.ClientCustomPacketEvent(manager, packet);
            try {
                eventChannel.mcc$bus().post(event);
                FMLProxyPacket reply = event.getReply();
                if (reply != null) {
                    if (netHandler instanceof NetHandlerPlayServer s) send(reply, FMLOutboundHandler.OutboundTarget.PLAYER, s.player);
                    else send(reply, FMLOutboundHandler.OutboundTarget.TOSERVER, null);
                }
            } catch (RuntimeException | LinkageError e) {
                LOG.error("Legacy event channel {} failed to handle a packet", channelName, e);
            }
            return;
        }
        EnumMap<Side, FMLEmbeddedChannel> channels = CHANNELS.get(channelName);
        FMLEmbeddedChannel channel = channels == null ? null : channels.get(side);
        if (channel == null) {
            LOG.debug("Message for unknown legacy channel {}", channelName);
            return;
        }
        try {
            channel.attr(NetworkRegistry.NET_HANDLER).set(netHandler instanceof INetHandler h ? h : null);
            channel.writeInbound(packet);
            while (channel.readInbound() != null) {
                // messages nobody consumed would pile up in the embedded channel
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.error("Legacy channel {} failed to handle a message", channelName, e);
        }
    }
}
