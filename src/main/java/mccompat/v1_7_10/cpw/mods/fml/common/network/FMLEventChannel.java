package mccompat.v1_7_10.cpw.mods.fml.common.network;

import mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.EventBus;
import mccompat.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import org.kylepitcock.mccompat.v1_7_10.bridge.FmlChannelBridge;

/**
 * FML 7.10 "event driven" channel: raw {@link FMLProxyPacket}s in and out; incoming packets are posted as
 * {@link FMLNetworkEvent.ClientCustomPacketEvent}/{@link FMLNetworkEvent.ServerCustomPacketEvent} on this channel's bus.
 */
public final class FMLEventChannel {
    private final String name;
    private final EventBus bus;

    public FMLEventChannel(String name) {
        this.name = name;
        this.bus = new EventBus("1.7.10-channel-" + name);
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
        FmlChannelBridge.send(pkt, FMLOutboundHandler.OutboundTarget.ALL, null);
    }

    public void sendTo(FMLProxyPacket pkt, EntityPlayerMP player) {
        FmlChannelBridge.send(pkt, FMLOutboundHandler.OutboundTarget.PLAYER, player);
    }

    public void sendToAllAround(FMLProxyPacket pkt, NetworkRegistry.TargetPoint point) {
        FmlChannelBridge.send(pkt, FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT, point);
    }

    public void sendToDimension(FMLProxyPacket pkt, int dimensionId) {
        FmlChannelBridge.send(pkt, FMLOutboundHandler.OutboundTarget.DIMENSION, dimensionId);
    }

    public void sendToServer(FMLProxyPacket pkt) {
        FmlChannelBridge.send(pkt, FMLOutboundHandler.OutboundTarget.TOSERVER, null);
    }
}
