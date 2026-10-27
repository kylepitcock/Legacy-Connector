package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.EventBus;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.FmlChannelBridge1122;

/**
 * A raw packet channel: packets in and out as {@link FMLProxyPacket}s; received ones are posted on this channel's bus as
 * {@link FMLNetworkEvent.ClientCustomPacketEvent} / {@link FMLNetworkEvent.ServerCustomPacketEvent}.
 */
public class FMLEventChannel {
    private final String name;
    private final EventBus bus;

    public FMLEventChannel() {
        this("unnamed");
    }

    public FMLEventChannel(String name) {
        this.name = name;
        this.bus = new EventBus("1.12.2-channel-" + name);
        FmlChannelBridge1122.registerEventChannel(this);
    }

    public void register(Object handler) {
        bus.register(handler);
    }

    public void unregister(Object handler) {
        bus.unregister(handler);
    }

    public EventBus mcc$bus() {
        return bus;
    }

    public String mcc$name() {
        return name;
    }

    public void sendToAll(FMLProxyPacket pkt) {
        FmlChannelBridge1122.send(pkt, FMLOutboundHandler.OutboundTarget.ALL, null);
    }

    public void sendTo(FMLProxyPacket pkt, EntityPlayerMP player) {
        FmlChannelBridge1122.send(pkt, FMLOutboundHandler.OutboundTarget.PLAYER, player);
    }

    public void sendToAllAround(FMLProxyPacket pkt, NetworkRegistry.TargetPoint point) {
        FmlChannelBridge1122.send(pkt, FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT, point);
    }

    public void sendToDimension(FMLProxyPacket pkt, int dimensionId) {
        FmlChannelBridge1122.send(pkt, FMLOutboundHandler.OutboundTarget.DIMENSION, dimensionId);
    }

    public void sendToServer(FMLProxyPacket pkt) {
        FmlChannelBridge1122.send(pkt, FMLOutboundHandler.OutboundTarget.TOSERVER, null);
    }
}
