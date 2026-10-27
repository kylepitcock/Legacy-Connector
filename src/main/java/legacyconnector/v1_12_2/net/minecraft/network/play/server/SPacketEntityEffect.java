package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A potion effect on an entity. */
public class SPacketEntityEffect implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect effect;

    public SPacketEntityEffect() {
    }

    public SPacketEntityEffect(int entityIdIn, legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect effect) {
        this.entityId = entityIdIn;
        this.effect = effect;
    }

    public int getEntityId() {
        return entityId;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        var modern = effect == null ? null : effect.mcc$toModern();
        return modern == null ? null : new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(entityId, modern, false);
    }
}
