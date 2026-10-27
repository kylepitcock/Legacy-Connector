package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** An entity's animation: 0 swing main arm, 1 hurt, 2 wake up, 3 swing off hand, 4 critical hit, 5 magic critical hit. */
public class SPacketAnimation implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.entity.Entity entity;
    private int entityId;
    private int type;

    public SPacketAnimation() {
    }

    public SPacketAnimation(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn, int typeIn) {
        this.entity = entityIn;
        this.entityId = entityIn.getEntityId();
        this.type = typeIn;
    }

    public int getEntityID() {
        return entityId;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        entityId = buf.readVarInt();
        type = buf.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(entityId);
        buf.writeByte(type);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return entity == null || entity.mcc$modern() == null ? null
                : new net.minecraft.network.protocol.game.ClientboundAnimatePacket(entity.mcc$modern(), type);
    }
}
