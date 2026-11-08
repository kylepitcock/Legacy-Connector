package legacyconnector.v1_7_10.cpw.mods.fml.common.network;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.EventBus;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge;

/**
 * FML 7.10 "event driven" channel: raw {@link FMLProxyPacket}s in and out; incoming packets are posted as
 * {@link FMLNetworkEvent.ClientCustomPacketEvent}/{@link FMLNetworkEvent.ServerCustomPacketEvent} on this channel's bus.
 */
public final class FMLEventChannel {
    private final String name;
    private final EventBus bus;
    /**
     * FML's private {@code channels} table of an event channel (per side, the pipeline whose attributes select the target and whose
     * writes leave through {@link FMLOutboundHandler}). Mods read it by reflection (TheErebus' PacketPipeline: NoSuchFieldException:
     * channels) and write FMLProxyPackets to it. Never registered with the bridge: incoming packets go to {@link #bus}.
     */
    private final java.util.EnumMap<legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side, FMLEmbeddedChannel> channels =
            new java.util.EnumMap<>(legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side.class);

    public FMLEventChannel(String name) {
        this.name = name;
        for (legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side side : legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side.values()) {
            channels.put(side, new FMLEmbeddedChannel(name, side));
        }
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
