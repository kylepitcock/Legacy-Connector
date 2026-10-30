package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.simpleimpl.IMessage;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.simpleimpl.MessageContext;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
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
 * 1.7.10 SimpleNetworkWrapper channels over {@link LegacyPayload}. Legacy handlers ran on the Netty thread and
 * frequently touched the world from there; here they run on the main thread, which is what they should have done.
 */
public final class NetworkBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Network");
    private static final String VERSION = "1.7.10";
    private static final Map<String, Channel> CHANNELS = new ConcurrentHashMap<>();

    static {
        LegacyNetwork.registerReceiver(VERSION, NetworkBridge::receive);
    }

    private NetworkBridge() {}

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

        /** The payload a message travels in (for {@code SimpleNetworkWrapper.getPacketFrom}). */
        public LegacyPayload payload(IMessage message) {
            return encode(message);
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
                message.fromBytes(org.kylepitcock.legacyconnector.core.network.LegacyBuffers.message(payload.discriminator(), payload.data()));
                MessageContext ctx = toServer
                        ? new MessageContext(Side.SERVER, new NetHandlerPlayServer((EntityPlayerMP) EntityBridge.wrap(context.player())))
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
            ServerLevel level = server == null ? null : DimensionBridge.loaded(server, dimension);
            if (level != null) PacketDistributor.sendToPlayersInDimension(level, encode(message));
        }

        public void sendToAllAround(IMessage message, int dimension, double x, double y, double z, double range) {
            var server = ServerLifecycleHooks.getCurrentServer();
            ServerLevel level = server == null ? null : DimensionBridge.loaded(server, dimension);
            if (level != null) PacketDistributor.sendToPlayersNear(level, null, x, y, z, range, encode(message));
        }
    }
}
