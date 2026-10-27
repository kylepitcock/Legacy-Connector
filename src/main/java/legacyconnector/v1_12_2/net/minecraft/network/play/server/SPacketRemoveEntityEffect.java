package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A potion effect ending on an entity. */
public class SPacketRemoveEntityEffect implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private legacyconnector.v1_12_2.net.minecraft.potion.Potion effectId;

    public SPacketRemoveEntityEffect() {
    }

    public SPacketRemoveEntityEffect(int entityIdIn, legacyconnector.v1_12_2.net.minecraft.potion.Potion potionIn) {
        this.entityId = entityIdIn;
        this.effectId = potionIn;
    }

    public legacyconnector.v1_12_2.net.minecraft.potion.Potion getPotion() {
        return effectId;
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
        var modern = effectId == null ? null : effectId.mcc$modern();
        return modern == null ? null : new net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket(entityId, modern);
    }
}
