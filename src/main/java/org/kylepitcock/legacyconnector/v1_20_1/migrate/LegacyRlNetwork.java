package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Resourceful Lib 2 (1.20.1) {@code networking} package over {@link LegacyLibChannel}: Resourceful Lib 3 replaced it by {@code network} with a different shape
 * ({@code PacketType} objects, {@code NetworkPacketPayload}), so the 1.20.1 interfaces and the channel class live here under the old package's names (the
 * loader renames them, {@code lib-classes.tsv}). A packet registers an id, a handler (encode, decode, handle to a context) and its class; the channel
 * carries them as one legacy payload type.
 */
public final class LegacyRlNetwork {
    private LegacyRlNetwork() {}

    /** {@code networking.base.Packet}. */
    public interface LegacyRlPacket<T extends LegacyRlPacket<T>> {
        ResourceLocation getID();

        LegacyRlPacketHandler<T> getHandler();
    }

    /** {@code networking.base.PacketHandler}. */
    public interface LegacyRlPacketHandler<T extends LegacyRlPacket<T>> {
        void encode(T message, FriendlyByteBuf buffer);

        T decode(FriendlyByteBuf buffer);

        LegacyRlPacketContext handle(T message);
    }

    /** {@code networking.base.PacketContext}: runs on the receiving side with the receiving player and that player's level. */
    @FunctionalInterface
    public interface LegacyRlPacketContext {
        void apply(Player player, Level level);
    }

    /** {@code networking.base.DatalessPacketHandler}: a packet without fields. */
    public abstract static class LegacyRlDatalessPacketHandler<T extends LegacyRlPacket<T>> implements LegacyRlPacketHandler<T> {
        private final Supplier<T> factory;

        public LegacyRlDatalessPacketHandler(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public void encode(T message, FriendlyByteBuf buffer) {}

        @Override
        public T decode(FriendlyByteBuf buffer) {
            return factory.get();
        }
    }

    /** {@code networking.base.NetworkDirection}. */
    public enum LegacyRlDirection {
        CLIENT_TO_SERVER,
        SERVER_TO_CLIENT
    }

    /** {@code networking.NetworkChannel}. */
    public static class LegacyRlChannel {
        public LegacyRlChannel(String modId, int version, String name) {}

        public LegacyRlChannel(String modId, int version, String name, boolean optional) {}

        public LegacyRlChannel(String modId, int version, String name, BooleanSupplier optional) {}

        @SuppressWarnings({"unchecked", "rawtypes"})
        public final <T extends LegacyRlPacket<T>> void registerPacket(LegacyRlDirection direction, ResourceLocation id, LegacyRlPacketHandler<T> handler, Class<T> type) {
            if (direction == LegacyRlDirection.SERVER_TO_CLIENT) {
                LegacyLibChannel.register(id, type, (o, buf) -> handler.encode((T) o, buf), buf -> handler.decode(buf),
                        (player, msg) -> handler.handle((T) msg).apply(player, player.level()), null);
            } else {
                LegacyLibChannel.register(id, type, (o, buf) -> handler.encode((T) o, buf), buf -> handler.decode(buf), null,
                        (player, msg) -> handler.handle((T) msg).apply(player, player.level()));
            }
        }

        private static <T extends LegacyRlPacket<T>> LegacyLibChannel.Payload payload(T packet) {
            return LegacyLibChannel.wrap(packet.getID(), packet);
        }

        public final <T extends LegacyRlPacket<T>> void sendToServer(T packet) {
            LegacyLibChannel.sendToServer(payload(packet));
        }

        public final <T extends LegacyRlPacket<T>> void sendToPlayer(T packet, Player player) {
            LegacyLibChannel.sendTo(player, payload(packet));
        }

        public final <T extends LegacyRlPacket<T>> void sendToPlayers(T packet, Collection<? extends Player> players) {
            LegacyLibChannel.Payload p = payload(packet);
            for (Player player : players) LegacyLibChannel.sendTo(player, p);
        }

        public final <T extends LegacyRlPacket<T>> void sendToAllPlayers(T packet, MinecraftServer server) {
            LegacyLibChannel.sendToAll(server, payload(packet));
        }

        public final <T extends LegacyRlPacket<T>> void sendToPlayersInLevel(T packet, Level level) {
            if (level instanceof ServerLevel server) {
                LegacyLibChannel.Payload p = payload(packet);
                for (ServerPlayer player : server.players()) LegacyLibChannel.sendTo(player, p);
            }
        }

        public final <T extends LegacyRlPacket<T>> void sendToAllLoaded(T packet, Level level, BlockPos pos) {
            if (level instanceof ServerLevel server) PacketDistributor.sendToPlayersTrackingChunk(server, new ChunkPos(pos), payload(packet));
        }

        public final <T extends LegacyRlPacket<T>> void sendToPlayersInRange(T packet, Level level, BlockPos pos, double range) {
            if (level instanceof ServerLevel server) {
                LegacyLibChannel.Payload p = payload(packet);
                double max = range * range;
                for (ServerPlayer player : server.players()) {
                    if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= max) LegacyLibChannel.sendTo(player, p);
                }
            }
        }

        public final boolean canSendPlayerPackets(Player player) {
            return true;
        }
    }
}
