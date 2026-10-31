package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import io.netty.buffer.Unpooled;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLEmbeddedChannel;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.NetworkRegistry;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient;
import legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer;
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
 * Raw FML 7.10 channels ({@code NetworkRegistry.newChannel}) over {@link LegacyPayload}. Outbound packets leave the
 * legacy pipeline as {@link FMLProxyPacket}s; inbound bytes are injected into the receiving side's pipeline, where
 * the mod's own codec and handlers process them exactly as on FML.
 */
public final class FmlChannelBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/FmlChannels");
    static final String FAMILY = "1.7.10-fml";
    private static final Map<String, EnumMap<Side, FMLEmbeddedChannel>> CHANNELS = new ConcurrentHashMap<>();

    static {
        LegacyNetwork.registerReceiver(FAMILY, FmlChannelBridge::receive);
    }

    private FmlChannelBridge() {}

    private static final java.util.Set<String> UNKNOWN_CHANNELS = ConcurrentHashMap.newKeySet();

    public static void register(String name, EnumMap<Side, FMLEmbeddedChannel> channels) {
        CHANNELS.put(name, channels);
    }

    public static FMLEmbeddedChannel channel(String name, Side side) {
        EnumMap<Side, FMLEmbeddedChannel> channels = CHANNELS.get(name);
        return channels == null ? null : channels.get(side);
    }

    /**
     * A connection finished its handshake on {@code side}: as FML, every channel pipeline of that side gets the connection's
     * handler and a {@code NetworkHandshakeEstablished} user event (mods send their initial sync from it, OpenMods' id maps).
     */
    public static void fireHandshake(Side side, legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher dispatcher,
            legacyconnector.v1_7_10.net.minecraft.network.INetHandler handler) {
        var event = new legacyconnector.v1_7_10.cpw.mods.fml.common.network.NetworkHandshakeEstablished(dispatcher, handler, side);
        for (var e : CHANNELS.entrySet()) {
            FMLEmbeddedChannel channel = e.getValue().get(side);
            if (channel == null) continue;
            try {
                channel.attr(legacyconnector.v1_7_10.cpw.mods.fml.common.network.NetworkRegistry.NET_HANDLER).set(handler);
                channel.pipeline().fireUserEventTriggered(event);
            } catch (RuntimeException | LinkageError ex) {
                LOG.error("The handshake handler of channel {} failed", e.getKey(), ex);
            }
        }
    }

    public static boolean has(String name) {
        return CHANNELS.containsKey(name) || EVENT_CHANNELS.containsKey(name);
    }

    private static final Map<String, legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLEventChannel> EVENT_CHANNELS = new ConcurrentHashMap<>();

    /** The monitor of the mod that created each event channel ({@link org.kylepitcock.legacyconnector.core.LegacyModLocks}). */
    private static final Map<String, java.util.concurrent.locks.ReentrantLock> EVENT_CHANNEL_LOCKS = new ConcurrentHashMap<>();

    public static void registerEventChannel(legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLEventChannel channel) {
        EVENT_CHANNELS.put(channel.mcc$name(), channel);
        var mod = org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
        if (mod != null && mod.getMod() != null) EVENT_CHANNEL_LOCKS.put(channel.mcc$name(), org.kylepitcock.legacyconnector.core.LegacyModLocks.of(mod.getMod()));
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
                ServerLevel level = server == null || !(args instanceof Integer dim) ? null : DimensionBridge.loaded(server, dim);
                if (level != null) PacketDistributor.sendToPlayersInDimension(level, p);
            }
            case ALLAROUNDPOINT -> {
                if (args instanceof NetworkRegistry.TargetPoint t && server != null) {
                    ServerLevel level = DimensionBridge.loaded(server, t.dimension);
                    if (level != null) PacketDistributor.sendToPlayersNear(level, null, t.x, t.y, t.z, t.range, p);
                }
            }
            case DISPATCHER, REPLY -> {
                if (args instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher d) d.sendProxy(packet);
                else LOG.debug("Legacy outbound target {} on channel {} without a dispatcher", target, packet.channel());
            }
            default -> LOG.debug("Unsupported legacy outbound target {} on channel {}", target, packet.channel());
        }
    }

    /** A packet to every player who has the chunk loaded. */
    public static void sendToChunkWatchers(FMLProxyPacket packet, ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
        PacketDistributor.sendToPlayersTrackingChunk(level, chunk, payload(packet));
    }

    private static void receive(LegacyPayload payload, IPayloadContext context) {
        boolean server = context.player() instanceof ServerPlayer;
        var dispatcher = legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher.get(
                legacyconnector.v1_7_10.net.minecraft.network.NetworkManager.mcc$of(context.connection()));
        context.enqueueWork(() -> deliver(payload.channel(), payload.data(), server ? Side.SERVER : Side.CLIENT,
                server ? new NetHandlerPlayServer((EntityPlayerMP) EntityBridge.wrap(context.player())) : NetHandlerPlayClient.INSTANCE, dispatcher));
    }

    /** Feeds raw bytes into a side's pipeline (also used for tile entity description packets). */
    public static void deliver(String channelName, byte[] data, Side side, Object netHandler) {
        deliver(channelName, data, side, netHandler, null);
    }

    /** A received packet, as FML handed it on: carrying the connection's handler and dispatcher. */
    private static FMLProxyPacket inbound(byte[] data, String channelName, Object netHandler,
            legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher dispatcher) {
        FMLProxyPacket packet = new FMLProxyPacket(org.kylepitcock.legacyconnector.core.network.LegacyBuffers.wrap(data), channelName);
        if (netHandler instanceof legacyconnector.v1_7_10.net.minecraft.network.INetHandler h) packet.mcc$setHandler(h);
        packet.setDispatcher(dispatcher);
        return packet;
    }

    public static void deliver(String channelName, byte[] data, Side side, Object netHandler,
            legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher dispatcher) {
        var eventChannel = EVENT_CHANNELS.get(channelName);
        if (eventChannel != null) {
            FMLProxyPacket packet = inbound(data, channelName, netHandler, dispatcher);
            var event = side == Side.SERVER
                    ? new legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent(null, packet,
                            (legacyconnector.v1_7_10.net.minecraft.network.INetHandler) netHandler)
                    : new legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent(null, packet);
            try {
                // server-side handlers may touch what the mod's world generators use (CustomOreGen's debug wireframes)
                org.kylepitcock.legacyconnector.core.LegacyModLocks.serverSide(side == Side.SERVER ? EVENT_CHANNEL_LOCKS.get(channelName) : null,
                        () -> eventChannel.mcc$bus().post(event));
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
            if (side == Side.SERVER && channelName.startsWith("MC|") && netHandler instanceof NetHandlerPlayServer s) {
                VanillaPayloads1710.handleServer(channelName, data, s.playerEntity);
                return;
            }
            if (UNKNOWN_CHANNELS.add(channelName + "/" + side)) {
                LOG.warn("A {}-side custom payload arrived for legacy channel '{}' that no mod registered (logged once per channel; its data is dropped)", side, channelName);
            }
            return;
        }
        FMLEmbeddedChannel channel = channels.get(side);
        try {
            channel.attr(NetworkRegistry.NET_HANDLER).set(netHandler);
            channel.attr(legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher.FML_DISPATCHER).set(dispatcher);
            channel.writeInbound(inbound(data, channelName, netHandler, dispatcher));
            // Messages nobody consumed would accumulate in the embedded channel.
            while (channel.readInbound() != null) {
                // drop
            }
        } catch (RuntimeException e) {
            LOG.error("Legacy channel {} failed to handle a message", channelName, e);
        }
    }
}
