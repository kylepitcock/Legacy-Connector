package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** An entity's absolute position and rotation. */
public class SPacketEntityTeleport implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.entity.Entity entity;
    private int entityId;

    public SPacketEntityTeleport() {
    }

    public SPacketEntityTeleport(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn) {
        this.entity = entityIn;
        this.entityId = entityIn.getEntityId();
    }

    public int getEntityId() {
        return entityId;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        entityId = buf.readVarInt();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(entityId);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return entity == null || entity.mcc$modern() == null ? null : new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(entity.mcc$modern());
    }
}
