package legacyconnector.v1_20_1.net.minecraftforge.network;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Forge 1.20.1's way to pick the receivers of a {@code SimpleChannel} message ({@code PacketDistributor.PLAYER.with(() -> player)}); each distributor
 * calls the matching {@code net.neoforged.neoforge.network.PacketDistributor} send method.
 */
public class PacketDistributor<T> {
    public static final PacketDistributor<ServerPlayer> PLAYER = new PacketDistributor<>((arg, payload) -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(arg, payload));
    public static final PacketDistributor<ResourceKey<Level>> DIMENSION = new PacketDistributor<>((dim, payload) -> {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = server == null ? null : server.getLevel(dim);
        if (level != null) net.neoforged.neoforge.network.PacketDistributor.sendToPlayersInDimension(level, payload);
    });
    public static final PacketDistributor<TargetPoint> NEAR = new PacketDistributor<>((point, payload) -> {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = server == null ? null : server.getLevel(point.dim);
        if (level != null) net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level, point.excluded, point.x, point.y, point.z, point.r, payload);
    });
    public static final PacketDistributor<Void> ALL = new PacketDistributor<>((arg, payload) -> net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(payload));
    public static final PacketDistributor<Void> SERVER = new PacketDistributor<>((arg, payload) -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload));
    public static final PacketDistributor<Entity> TRACKING_ENTITY = new PacketDistributor<>((e, payload) -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(e, payload));
    public static final PacketDistributor<Entity> TRACKING_ENTITY_AND_SELF = new PacketDistributor<>((e, payload) -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(e, payload));
    public static final PacketDistributor<LevelChunk> TRACKING_CHUNK = new PacketDistributor<>((chunk, payload) -> {
        if (chunk.getLevel() instanceof ServerLevel level) net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(level, chunk.getPos(), payload);
    });

    private final BiConsumer<T, CustomPacketPayload> sender;

    private PacketDistributor(BiConsumer<T, CustomPacketPayload> sender) {
        this.sender = sender;
    }

    public PacketTarget with(Supplier<T> input) {
        return new PacketTarget(payload -> sender.accept(input.get(), payload), packet -> sendRaw(input.get(), packet), this);
    }

    public PacketTarget noArg() {
        return new PacketTarget(payload -> sender.accept(null, payload), packet -> sendRaw(null, packet), this);
    }

    /** Sends a vanilla packet (not a mod payload) to the receivers this distributor picks for the argument. */
    @SuppressWarnings("unchecked")
    private void sendRaw(Object arg, net.minecraft.network.protocol.Packet<?> packet) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (this == PLAYER) {
            ServerPlayer p = (ServerPlayer) arg;
            if (p != null && p.connection != null) p.connection.send(packet);
        } else if (this == SERVER) {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) connection.send(packet);
        } else if (server == null) {
            return;
        } else if (this == ALL) {
            server.getPlayerList().broadcastAll(packet);
        } else if (this == DIMENSION) {
            server.getPlayerList().broadcastAll(packet, (ResourceKey<Level>) arg);
        } else if (this == NEAR) {
            TargetPoint t = (TargetPoint) arg;
            server.getPlayerList().broadcast(t.excluded, t.x, t.y, t.z, t.r, t.dim, packet);
        } else if (this == TRACKING_ENTITY && arg instanceof Entity e && e.level() instanceof ServerLevel level) {
            level.getChunkSource().broadcast(e, packet);
        } else if (this == TRACKING_ENTITY_AND_SELF && arg instanceof Entity e && e.level() instanceof ServerLevel level) {
            level.getChunkSource().broadcastAndSend(e, packet);
        } else if (this == TRACKING_CHUNK && arg instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel level) {
            level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false).forEach(p -> p.connection.send(packet));
        }
    }

    /** Which way messages of this distributor travel. */
    public NetworkDirection direction() {
        return this == SERVER ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT;
    }

    /** Where a message goes. */
    public static final class PacketTarget {
        private final Consumer<CustomPacketPayload> send;
        private final Consumer<net.minecraft.network.protocol.Packet<?>> sendRaw;
        private final PacketDistributor<?> distributor;

        PacketTarget(Consumer<CustomPacketPayload> send, Consumer<net.minecraft.network.protocol.Packet<?>> sendRaw, PacketDistributor<?> distributor) {
            this.send = send;
            this.sendRaw = sendRaw;
            this.distributor = distributor;
        }

        public void send(CustomPacketPayload payload) {
            send.accept(payload);
        }

        /** Forge's targets also carried plain vanilla packets (a mod sending a ClientboundSetEntityMotionPacket through a distributor). */
        public void send(net.minecraft.network.protocol.Packet<?> packet) {
            sendRaw.accept(packet);
        }

        public NetworkDirection getDirection() {
            return distributor.direction();
        }
    }

    /** The centre and radius for {@link #NEAR}. */
    public static final class TargetPoint {
        public final double x;
        public final double y;
        public final double z;
        public final double r;
        public final ResourceKey<Level> dim;
        final ServerPlayer excluded;

        public TargetPoint(double x, double y, double z, double r, ResourceKey<Level> dim) {
            this(null, x, y, z, r, dim);
        }

        public TargetPoint(ServerPlayer excluded, double x, double y, double z, double r, ResourceKey<Level> dim) {
            this.excluded = excluded;
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.dim = dim;
        }

        public static Supplier<TargetPoint> p(double x, double y, double z, double r, ResourceKey<Level> dim) {
            TargetPoint tp = new TargetPoint(x, y, z, r, dim);
            return () -> tp;
        }
    }
}
