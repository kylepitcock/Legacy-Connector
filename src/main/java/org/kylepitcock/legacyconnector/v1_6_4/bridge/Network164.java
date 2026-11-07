package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import legacyconnector.v1_6_4.cpw.mods.fml.common.network.IPacketHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.network.ITinyPacketHandler;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet131MapData;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet250CustomPayload;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.Player;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;
import legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.DimensionBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.6.4's custom payload networking over the layer's FML channel bridge: {@code IPacketHandler}s registered per channel (and side) get the
 * payload bytes of the channel as a {@link Packet250CustomPayload}; {@code PacketDispatcher} sends any packet to a player, a dimension, everyone or
 * the server through the packet's own {@code mcc$send} (custom payloads travel as the bridge's payload). Tiny packets
 * ({@code ITinyPacketHandler}) travel on {@link #TINY_CHANNEL}, tagged with the mod's id.
 */
public final class Network164 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Network164");
    /** The channel tiny packets use (payload: mod id, tag, data). */
    public static final String TINY_CHANNEL = "mcc|tiny164";

    private record Registration(IPacketHandler handler, Side side) {}

    private static final Map<String, List<Registration>> CHANNELS = new ConcurrentHashMap<>();
    private static final Map<String, ITinyPacketHandler> TINY = new ConcurrentHashMap<>();

    static {
        FmlChannelBridge.registerRaw(TINY_CHANNEL, Network164::receiveTiny);
    }

    private static final java.util.concurrent.atomic.AtomicLong RECEIVED = new java.util.concurrent.atomic.AtomicLong();

    private Network164() {}

    /** Registered channels, handlers and the number of packets dispatched to mod handlers so far (see {@link SelfTest164}). */
    public static String summary() {
        int handlers = 0;
        for (List<Registration> l : CHANNELS.values()) handlers += l.size();
        return CHANNELS.size() + " channel(s), " + handlers + " packet handler(s), " + TINY.size() + " tiny handler(s), " + RECEIVED.get() + " packet(s) dispatched";
    }

    /**
     * Loopback self test of the receive path: a packet delivered to a channel reaches the handler registered for it as a
     * {@link Packet250CustomPayload} with its data, a handler of the other side does not see it.
     */
    static boolean loopback() {
        String channel = "mcc|selftest164";
        boolean[] got = new boolean[2];
        // straight into the table: the wire registration of a channel is closed once the game runs
        List<Registration> list = CHANNELS.computeIfAbsent(channel, c -> new CopyOnWriteArrayList<>());
        list.add(new Registration((manager, packet, player) -> got[0] = packet.channel.equals(channel) && packet.data.length == 3 && packet.data[2] == 7, Side.SERVER));
        list.add(new Registration((manager, packet, player) -> got[1] = true, Side.CLIENT));
        receive(channel, new byte[] {1, 2, 7}, Side.SERVER, null);
        CHANNELS.remove(channel);
        return got[0] && !got[1];
    }

    // ------------------------------------------------------------------ registration

    /** Registers {@code handler} for the channel; {@code side} null means packets arriving on either side. */
    public static void registerChannel(IPacketHandler handler, String channel, Side side) {
        if (handler == null || channel == null) return;
        CHANNELS.computeIfAbsent(channel, c -> new CopyOnWriteArrayList<>()).add(new Registration(handler, side));
        FmlChannelBridge.registerRaw(channel, Network164::receive);
    }

    public static void registerConnectionHandler(legacyconnector.v1_6_4.cpw.mods.fml.common.network.IConnectionHandler handler) {
        Players164.registerConnectionHandler(handler);
    }

    public static void registerTinyHandler(String modId, ITinyPacketHandler handler) {
        TINY.put(modId, handler);
    }

    /** The tiny packet handler of a mod (its instance or id) as its {@code @NetworkMod} declared it, or null. */
    public static ITinyPacketHandler tinyHandler(Object mod) {
        LegacyModRecord record = record(mod);
        return record == null ? null : TINY.get(record.legacyId);
    }

    // ------------------------------------------------------------------ receiving

    private static void receive(String channel, byte[] data, Side side, Object netHandler) {
        List<Registration> registrations = CHANNELS.get(channel);
        if (registrations == null) return;
        INetHandler handler = netHandler instanceof INetHandler h ? h : null;
        for (Registration r : registrations) {
            if (r.side != null && r.side != side) continue;
            RECEIVED.incrementAndGet();
            try {
                Packet250CustomPayload packet = new Packet250CustomPayload(channel, data);
                Player player = handler == null ? null : handler.getPlayer();
                r.handler.onPacketData(Manager164.of(handler), packet, player);
            } catch (RuntimeException | LinkageError e) {
                LOG.error("The packet handler {} failed on channel {}", r.handler.getClass().getName(), channel, e);
            }
        }
    }

    private static void receiveTiny(String channel, byte[] data, Side side, Object netHandler) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String modId = in.readUTF();
            short tag = in.readShort();
            byte[] payload = new byte[in.readInt()];
            in.readFully(payload);
            ITinyPacketHandler handler = TINY.get(modId);
            if (handler == null) return;
            Packet131MapData packet = new Packet131MapData((short) networkId(modId), tag, payload);
            handler.handle(netHandler instanceof INetHandler h ? h : null, packet);
        } catch (IOException | RuntimeException | LinkageError e) {
            LOG.error("A tiny packet failed to arrive", e);
        }
    }

    // ------------------------------------------------------------------ sending

    /** A tiny packet for the mod (its instance or legacy id). */
    public static Packet131MapData tinyPacket(Object mod, short tag, byte[] data) {
        LegacyModRecord record = record(mod);
        Packet131MapData packet = new Packet131MapData((short) networkId(mod), tag, data);
        packet.mcc$modId = record == null ? String.valueOf(mod) : record.legacyId;
        return packet;
    }

    /** The custom payload a tiny packet travels as. */
    public static Packet250CustomPayload tiny(Packet131MapData packet) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(packet.mcc$modId);
            out.writeShort(packet.uniqueID);
            out.writeInt(packet.itemData.length);
            out.write(packet.itemData);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new Packet250CustomPayload(TINY_CHANNEL, bytes.toByteArray());
    }

    public static void sendToServer(Packet packet) {
        if (packet != null) NetHandlerPlayClient.INSTANCE.addToSendQueue(packet);
    }

    public static void sendToPlayer(Packet packet, Player player) {
        if (packet == null || !(player instanceof EntityPlayerMP mp)) return;
        sendTo(packet, mp.mcc$serverPlayer());
    }

    public static void sendToAll(Packet packet) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (packet == null || server == null) return;
        for (ServerPlayer p : List.copyOf(server.getPlayerList().getPlayers())) sendTo(packet, p);
    }

    public static void sendToDimension(Packet packet, int dimension) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (packet == null || server == null) return;
        ServerLevel level = DimensionBridge.loaded(server, dimension);
        if (level == null) return;
        for (ServerPlayer p : List.copyOf(level.players())) sendTo(packet, p);
    }

    public static void sendToAllAround(double x, double y, double z, double range, int dimension, Packet packet) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (packet == null || server == null) return;
        ServerLevel level = DimensionBridge.loaded(server, dimension);
        if (level == null) return;
        for (ServerPlayer p : List.copyOf(level.players())) {
            if (p.distanceToSqr(x, y, z) < range * range) sendTo(packet, p);
        }
    }

    /** Sends through the player's handler, so tile entity packets and the like take the shim's usual way. */
    public static void sendTo(Packet packet, ServerPlayer player) {
        if (player == null || player.connection == null) return;
        if (EntityBridge.wrap(player) instanceof EntityPlayerMP mp) new NetHandlerPlayServer(mp).sendPacket(packet);
        else packet.mcc$send(player);
    }

    // ------------------------------------------------------------------ mods

    private static LegacyModRecord record(Object mod) {
        if (mod instanceof String id) return LegacyMods.byLegacyId(id);
        if (mod == null) return null;
        for (LegacyModRecord m : LegacyMods.ordered()) {
            if (m.instance == mod) return m;
        }
        return null;
    }

    /** A stable numeric id of a mod (FML numbered them by load order); -1 for a mod that is not a legacy mod. */
    public static int networkId(Object mod) {
        LegacyModRecord record = record(mod);
        if (record == null) return -1;
        return record.legacyId.hashCode() & 0x7FFF;
    }
}
