package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * One NeoForge payload type that carries every packet of the networking APIs of 1.20.1 libraries whose 1.21.1 build moved to payload types (Balm's
 * {@code registerClientboundPacket(id, class, encoder, decoder, handler)}, Resourceful Lib's {@code NetworkChannel}). A legacy mod registers packets by
 * id with encode/decode functions over a {@code FriendlyByteBuf}; the payload writes the id and the bytes the mod's own encoder produced and decodes with the
 * decoder registered under that id, so the mod's packet classes, codecs and handlers run as written. The library's own 1.21.1 networking is not involved
 * (its payload types have a different shape and the legacy classes are no {@code CustomPacketPayload}).
 */
public final class LegacyLibChannel {
    public static final CustomPacketPayload.Type<Payload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("legacyconnector", "lib_packets"));

    /** A registered packet: how to write, read and handle it; a handler is null in the direction the packet does not travel. */
    private record Reg(ResourceLocation id, Class<?> type, BiConsumer<Object, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, Object> decoder,
            BiConsumer<Player, Object> onClient, BiConsumer<ServerPlayer, Object> onServer) {}

    private static final Map<ResourceLocation, Reg> BY_ID = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Reg> BY_CLASS = new ConcurrentHashMap<>();
    private static final ThreadLocal<IPayloadContext> REPLY = new ThreadLocal<>();

    private LegacyLibChannel() {}

    /** A packet in transit: {@code message} is the mod's packet object (decoded on receive). */
    public record Payload(ResourceLocation id, Object message) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ---- registration ---------------------------------------------------------------------------------------------------

    /** Balm's {@code registerClientboundPacket(id, class, encoder, decoder, handler)}. */
    @SuppressWarnings("unchecked")
    public static <T> void registerClientbound(ResourceLocation id, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder,
            BiConsumer<Player, T> handler) {
        add(new Reg(id, type, (BiConsumer<Object, FriendlyByteBuf>) (BiConsumer<?, FriendlyByteBuf>) encoder, (Function<FriendlyByteBuf, Object>) decoder,
                (BiConsumer<Player, Object>) handler, null));
    }

    /** Balm's {@code registerServerboundPacket(id, class, encoder, decoder, handler)}. */
    @SuppressWarnings("unchecked")
    public static <T> void registerServerbound(ResourceLocation id, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder,
            BiConsumer<ServerPlayer, T> handler) {
        add(new Reg(id, type, (BiConsumer<Object, FriendlyByteBuf>) (BiConsumer<?, FriendlyByteBuf>) encoder, (Function<FriendlyByteBuf, Object>) decoder,
                null, (BiConsumer<ServerPlayer, Object>) handler));
    }

    /** A packet that travels either way (Resourceful Lib registers each direction on its own, with its own handler). */
    public static void register(ResourceLocation id, Class<?> type, BiConsumer<Object, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, Object> decoder,
            BiConsumer<Player, Object> onClient, BiConsumer<ServerPlayer, Object> onServer) {
        Reg old = BY_ID.get(id);
        if (old != null) {
            onClient = onClient != null ? onClient : old.onClient;
            onServer = onServer != null ? onServer : old.onServer;
        }
        add(new Reg(id, type, encoder, decoder, onClient, onServer));
    }

    private static void add(Reg r) {
        Reg old = BY_ID.get(r.id);
        if (old != null && old.type == r.type) {
            r = new Reg(r.id, r.type, r.encoder, r.decoder, r.onClient != null ? r.onClient : old.onClient, r.onServer != null ? r.onServer : old.onServer);
        }
        BY_ID.put(r.id, r);
        BY_CLASS.put(r.type, r);
    }

    private static Reg regOf(Object message) {
        for (Class<?> c = message.getClass(); c != null; c = c.getSuperclass()) {
            Reg r = BY_CLASS.get(c);
            if (r != null) return r;
        }
        throw new IllegalArgumentException("Packet " + message.getClass().getName() + " was not registered on a legacy library network");
    }

    /** The payload of a packet object (for a packet that is registered by class). */
    public static Payload wrap(Object message) {
        return new Payload(regOf(message).id, message);
    }

    /** The payload of a packet object registered under {@code id}. */
    public static Payload wrap(ResourceLocation id, Object message) {
        if (!BY_ID.containsKey(id)) throw new IllegalArgumentException("Packet " + id + " was not registered on a legacy library network");
        return new Payload(id, message);
    }

    // ---- sending --------------------------------------------------------------------------------------------------------

    public static void sendTo(Player player, Payload p) {
        if (player instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, p);
    }

    public static void sendToTracking(ServerLevel level, BlockPos pos, Payload p) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), p);
    }

    public static void sendToTracking(Entity entity, Payload p) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, p);
    }

    public static void sendToAll(MinecraftServer server, Payload p) {
        PacketDistributor.sendToAllPlayers(p);
    }

    public static void sendToServer(Payload p) {
        PacketDistributor.sendToServer(p);
    }

    /** The answer to the packet being handled (Balm's {@code reply}). */
    public static void reply(Payload p) {
        IPayloadContext ctx = REPLY.get();
        if (ctx == null) throw new IllegalStateException("reply() outside a packet handler");
        ctx.reply(p);
    }

    // ---- NeoForge ---------------------------------------------------------------------------------------------------------

    private static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of(LegacyLibChannel::encode, LegacyLibChannel::decode);

    private static void encode(RegistryFriendlyByteBuf buf, Payload p) {
        Reg r = BY_ID.get(p.id);
        buf.writeResourceLocation(p.id);
        r.encoder.accept(p.message, buf);
    }

    private static Payload decode(RegistryFriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        Reg r = BY_ID.get(id);
        if (r == null) throw new IllegalStateException("Unknown legacy library packet " + id);
        return new Payload(id, r.decoder.apply(buf));
    }

    private static void handle(Payload p, IPayloadContext ctx) {
        Reg r = BY_ID.get(p.id);
        if (r == null) return;
        REPLY.set(ctx);
        try {
            if (ctx.flow().isServerbound()) {
                if (r.onServer != null && ctx.player() instanceof ServerPlayer sp) r.onServer.accept(sp, p.message);
            } else if (r.onClient != null) {
                r.onClient.accept(ctx.player(), p.message);
            }
        } finally {
            REPLY.remove();
        }
    }

    /** Called by the runtime on {@link RegisterPayloadHandlersEvent}. */
    public static void registerAll(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playBidirectional(TYPE, CODEC, LegacyLibChannel::handle);
    }
}
