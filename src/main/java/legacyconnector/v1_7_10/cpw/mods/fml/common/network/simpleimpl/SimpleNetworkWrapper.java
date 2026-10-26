package legacyconnector.v1_7_10.cpw.mods.fml.common.network.simpleimpl;

import legacyconnector.v1_7_10.cpw.mods.fml.common.network.NetworkRegistry;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.NetworkBridge;

/**
 * Legacy message channel. All legacy channels share one modern payload type ({@code legacyconnector:legacy}) that carries
 * the channel name, discriminator and the bytes written by {@link IMessage#toBytes}.
 */
public class SimpleNetworkWrapper {
    private final NetworkBridge.Channel channel;

    public SimpleNetworkWrapper(String channelName) {
        this.channel = NetworkBridge.channel(channelName);
    }

    public <REQ extends IMessage, REPLY extends IMessage> void registerMessage(Class<? extends IMessageHandler<REQ, REPLY>> handlerClass,
                                                                               Class<REQ> messageType, int discriminator, Side side) {
        try {
            var ctor = handlerClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            registerMessage(ctor.newInstance(), messageType, discriminator, side);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot instantiate message handler " + handlerClass.getName(), e);
        }
    }

    public <REQ extends IMessage, REPLY extends IMessage> void registerMessage(IMessageHandler<? super REQ, ? extends REPLY> handler,
                                                                               Class<REQ> messageType, int discriminator, Side side) {
        channel.register(discriminator, messageType, handler, side);
    }

    public void sendToAll(IMessage message) {
        channel.sendToAll(message);
    }

    public void sendTo(IMessage message, EntityPlayerMP player) {
        channel.sendTo(message, player.mcc$serverPlayer());
    }

    public void sendToAllAround(IMessage message, NetworkRegistry.TargetPoint point) {
        channel.sendToAllAround(message, point.dimension, point.x, point.y, point.z, point.range);
    }

    public void sendToDimension(IMessage message, int dimensionId) {
        channel.sendToDimension(message, dimensionId);
    }

    public void sendToServer(IMessage message) {
        channel.sendToServer(message);
    }

    /** The message as a packet, for mods that send it through a vanilla path themselves (to a player's connection, chunk watchers). */
    public legacyconnector.v1_7_10.net.minecraft.network.Packet getPacketFrom(IMessage message) {
        return new MessagePacket(channel.payload(message));
    }

    /** A channel message wrapped as a packet: delivered to a player or the server as the channel's payload. */
    private static final class MessagePacket extends legacyconnector.v1_7_10.net.minecraft.network.Packet {
        private final org.kylepitcock.legacyconnector.core.network.LegacyPayload payload;

        MessagePacket(org.kylepitcock.legacyconnector.core.network.LegacyPayload payload) {
            this.payload = payload;
        }

        @Override
        public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }

        @Override
        public void mcc$sendToServer() {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload);
        }
    }
}
