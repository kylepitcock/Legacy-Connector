package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.NetworkRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.NetworkBridge1122;

/** A legacy message channel over the shared modern payload. */
public class SimpleNetworkWrapper {
    private final NetworkBridge1122.Channel channel;

    public SimpleNetworkWrapper(String channelName) {
        this.channel = NetworkBridge1122.channel(channelName);
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

    public void sendToAllTracking(IMessage message, NetworkRegistry.TargetPoint point) {
        channel.sendToAllTracking(message, point.dimension, point.x, point.z);
    }

    public void sendToAllTracking(IMessage message, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        channel.sendToAllTracking(message, entity == null ? null : entity.mcc$modern());
    }

    /** The message as a vanilla packet, for mods that send it through a connection themselves. */
    public legacyconnector.v1_12_2.net.minecraft.network.Packet<?> getPacketFrom(IMessage message) {
        net.minecraft.network.protocol.Packet<?> modern = channel.packet(message);
        return new legacyconnector.v1_12_2.net.minecraft.network.Packet<legacyconnector.v1_12_2.net.minecraft.network.INetHandler>() {
            @Override
            public void readPacketData(legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer buf) {
            }

            @Override
            public void writePacketData(legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer buf) {
            }

            @Override
            public void processPacket(legacyconnector.v1_12_2.net.minecraft.network.INetHandler handler) {
            }

            @Override
            public net.minecraft.network.protocol.Packet<?> mcc$modern() {
                return modern;
            }
        };
    }

    public void sendToServer(IMessage message) {
        channel.sendToServer(message);
    }
}
