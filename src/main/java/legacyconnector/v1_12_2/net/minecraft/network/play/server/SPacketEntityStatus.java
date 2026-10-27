package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** An entity status event (hurt, death, taming hearts...). */
public class SPacketEntityStatus implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.entity.Entity entity;
    private int entityId;
    private byte logicOpcode;

    public SPacketEntityStatus() {
    }

    public SPacketEntityStatus(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn, byte opcodeIn) {
        this.entity = entityIn;
        this.entityId = entityIn.getEntityId();
        this.logicOpcode = opcodeIn;
    }

    public int getEntityID() {
        return entityId;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        entityId = buf.readInt();
        logicOpcode = buf.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeInt(entityId);
        buf.writeByte(logicOpcode);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return entity == null || entity.mcc$modern() == null ? null
                : new net.minecraft.network.protocol.game.ClientboundEntityEventPacket(entity.mcc$modern(), logicOpcode);
    }
}
