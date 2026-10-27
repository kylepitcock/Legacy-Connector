package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** Who rides an entity. */
public class SPacketSetPassengers implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.entity.Entity entity;
    private int entityId;

    public SPacketSetPassengers() {
    }

    public SPacketSetPassengers(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn) {
        this.entity = entityIn;
        this.entityId = entityIn.getEntityId();

    }

    public int getEntityID() {
        return entityId;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        entityId = buf.readVarInt();
        buf.readVarIntArray();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(entityId);
        buf.writeVarIntArray(new int[0]);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return entity == null || entity.mcc$modern() == null ? null
                : new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(entity.mcc$modern());
    }
}
