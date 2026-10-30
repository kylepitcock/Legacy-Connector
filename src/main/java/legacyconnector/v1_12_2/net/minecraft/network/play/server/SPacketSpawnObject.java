package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/**
 * Spawns a non-living entity (boat, minecart, projectile, ...) on the client; 1.12's object {@code type} id is kept for mods that read it.
 * Sent as the modern add-entity packet of the entity's own type.
 */
public class SPacketSpawnObject implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private UUID uniqueId;
    private double x;
    private double y;
    private double z;
    private int type;
    private int data;
    private net.minecraft.world.entity.Entity mcc$entity;

    public SPacketSpawnObject() {
    }

    public SPacketSpawnObject(Entity entityIn, int typeIn) {
        this(entityIn, typeIn, 0);
    }

    public SPacketSpawnObject(Entity entityIn, int typeIn, int dataIn) {
        this.mcc$entity = entityIn.mcc$modern();
        this.entityId = entityIn.getEntityId();
        this.uniqueId = entityIn.getUniqueID();
        this.type = typeIn;
        this.data = dataIn;
        if (mcc$entity != null) {
            this.x = mcc$entity.getX();
            this.y = mcc$entity.getY();
            this.z = mcc$entity.getZ();
        }
    }

    public SPacketSpawnObject(Entity entityIn, int typeIn, int dataIn, BlockPos pos) {
        this(entityIn, typeIn, dataIn);
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    public int getEntityID() {
        return entityId;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public int getType() {
        return type;
    }

    public int getData() {
        return data;
    }

    public void setData(int dataIn) {
        this.data = dataIn;
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
        var e = mcc$entity;
        if (e == null) return null;
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(e.getId(), e.getUUID(), x, y, z, e.getXRot(), e.getYRot(), e.getType(), data,
                e.getDeltaMovement(), e.getYHeadRot());
    }
}
