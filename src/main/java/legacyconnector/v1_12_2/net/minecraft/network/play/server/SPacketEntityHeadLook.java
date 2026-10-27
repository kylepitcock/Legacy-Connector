package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** The yaw of an entity's head. */
public class SPacketEntityHeadLook implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.entity.Entity entity;
    private int entityId;
    private byte yaw;

    public SPacketEntityHeadLook() {
    }

    public SPacketEntityHeadLook(legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn, byte yawIn) {
        this.entity = entityIn;
        this.entityId = entityIn.getEntityId();
        this.yaw = yawIn;
    }

    public byte getYaw() {
        return yaw;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        entityId = buf.readVarInt();
        yaw = buf.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(entityId);
        buf.writeByte(yaw);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return entity == null || entity.mcc$modern() == null ? null : new net.minecraft.network.protocol.game.ClientboundRotateHeadPacket(entity.mcc$modern(), yaw);
    }
}
