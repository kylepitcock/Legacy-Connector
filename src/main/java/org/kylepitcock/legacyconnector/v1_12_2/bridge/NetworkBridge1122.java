package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
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

/** 1.12.2 SimpleNetworkWrapper channels over {@link LegacyPayload}; handlers run on the main thread. */
public final class NetworkBridge1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Network");
    private static final String VERSION = "1.12.2";
    private static final Map<String, Channel> CHANNELS = new ConcurrentHashMap<>();

    static {
        LegacyNetwork.registerReceiver(VERSION, NetworkBridge1122::receive);
    }

    private NetworkBridge1122() {}

    public static Channel channel(String name) {
        return CHANNELS.computeIfAbsent(name, Channel::new);
    }

    private static void receive(LegacyPayload payload, IPayloadContext context) {
        Channel channel = CHANNELS.get(payload.channel());
        if (channel == null) {
            LOG.debug("Message on unknown legacy channel {}", payload.channel());
            return;
        }
        context.enqueueWork(() -> channel.handle(payload, context));
    }

    public static final class Channel {
        private final String name;
        private final Map<Integer, Class<? extends IMessage>> types = new ConcurrentHashMap<>();
        private final Map<Class<?>, Integer> discriminators = new ConcurrentHashMap<>();
        private final Map<Integer, IMessageHandler<IMessage, IMessage>> serverHandlers = new ConcurrentHashMap<>();
        private final Map<Integer, IMessageHandler<IMessage, IMessage>> clientHandlers = new ConcurrentHashMap<>();

        Channel(String name) {
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        public void register(int discriminator, Class<? extends IMessage> type, IMessageHandler<?, ?> handler, Side side) {
            types.put(discriminator, type);
            discriminators.putIfAbsent(type, discriminator);
            (side == Side.SERVER ? serverHandlers : clientHandlers).put(discriminator, (IMessageHandler<IMessage, IMessage>) handler);
        }

        private LegacyPayload encode(IMessage message) {
            Integer d = discriminators.get(message.getClass());
            if (d == null) throw new IllegalArgumentException("Message " + message.getClass().getName() + " is not registered on channel " + name);
            ByteBuf buf = org.kylepitcock.legacyconnector.core.network.LegacyBuffers.buffer();
            message.toBytes(buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return new LegacyPayload(VERSION, name, d, data);
        }

        void handle(LegacyPayload payload, IPayloadContext context) {
            boolean toServer = context.player() instanceof ServerPlayer;
            var handler = (toServer ? serverHandlers : clientHandlers).get(payload.discriminator());
            Class<? extends IMessage> type = types.get(payload.discriminator());
            if (handler == null || type == null) return;
            try {
                var ctor = type.getDeclaredConstructor();
                ctor.setAccessible(true);
                IMessage message = ctor.newInstance();
                message.fromBytes(org.kylepitcock.legacyconnector.core.network.LegacyBuffers.wrap(payload.data()));
                MessageContext ctx = toServer
                        ? new MessageContext(Side.SERVER, new NetHandlerPlayServer((EntityPlayerMP) EntityBridge1122.wrap(context.player())))
                        : new MessageContext(Side.CLIENT, null);
                IMessage reply = handler.onMessage(message, ctx);
                if (reply != null) context.reply(encode(reply));
            } catch (Exception e) {
                LOG.error("Legacy message {} on channel {} failed", type.getName(), name, e);
            }
        }

        public void sendToServer(IMessage message) {
            PacketDistributor.sendToServer(encode(message));
        }

        public void sendTo(IMessage message, ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, encode(message));
        }

        public void sendToAll(IMessage message) {
            PacketDistributor.sendToAllPlayers(encode(message));
        }

        public void sendToDimension(IMessage message, int dimension) {
            var server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server == null ? null : legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$level(dimension);
            if (level != null) PacketDistributor.sendToPlayersInDimension(level, encode(message));
        }

        /** To every player tracking the entity (and the entity itself when it is a player). */
        public void sendToAllTracking(IMessage message, net.minecraft.world.entity.Entity entity) {
            if (entity != null) PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, encode(message));
        }

        /** To every player tracking the chunk the point is in. */
        public void sendToAllTracking(IMessage message, int dimension, double x, double z) {
            var server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server == null ? null : legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$level(dimension);
            if (level != null) {
                PacketDistributor.sendToPlayersTrackingChunk(level, new net.minecraft.world.level.ChunkPos(net.minecraft.util.Mth.floor(x) >> 4,
                        net.minecraft.util.Mth.floor(z) >> 4), encode(message));
            }
        }

        /** The message as a modern packet (server to client). */
        public net.minecraft.network.protocol.Packet<?> packet(IMessage message) {
            return new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(encode(message));
        }

        public void sendToAllAround(IMessage message, int dimension, double x, double y, double z, double range) {
            var server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server == null ? null : legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$level(dimension);
            if (level != null) PacketDistributor.sendToPlayersNear(level, null, x, y, z, range, encode(message));
        }
    }
}
