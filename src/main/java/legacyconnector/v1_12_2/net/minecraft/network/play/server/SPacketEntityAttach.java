package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A leash between two entities (the holder null to detach). */
public class SPacketEntityAttach implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.entity.Entity entity;
    private legacyconnector.v1_12_2.net.minecraft.entity.Entity vehicle;
    private int entityId;
    private int vehicleEntityId;

    public SPacketEntityAttach() {
    }

    public SPacketEntityAttach(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn, legacyconnector.v1_12_2.net.minecraft.entity.Entity vehicleIn) {
        this.entity = entityIn;
        this.vehicle = vehicleIn;
        this.entityId = entityIn.getEntityId();
        this.vehicleEntityId = vehicleIn != null ? vehicleIn.getEntityId() : -1;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getVehicleEntityId() {
        return vehicleEntityId;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        entityId = buf.readInt();
        vehicleEntityId = buf.readInt();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeInt(entityId);
        buf.writeInt(vehicleEntityId);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        if (entity == null || entity.mcc$modern() == null) return null;
        return new net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket(entity.mcc$modern(), vehicle == null ? null : vehicle.mcc$modern());
    }
}
