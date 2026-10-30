package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** Spawns another player's entity on the client; sent as the modern add-entity packet. */
public class SPacketSpawnPlayer implements Packet<INetHandlerPlayClient> {
    private int entityId;
    private UUID uniqueId;
    private double x;
    private double y;
    private double z;
    private byte yaw;
    private byte pitch;
    private net.minecraft.world.entity.Entity mcc$entity;

    public SPacketSpawnPlayer() {
    }

    public SPacketSpawnPlayer(EntityPlayer player) {
        var p = player.mcc$player();
        this.mcc$entity = p;
        this.entityId = player.getEntityId();
        this.uniqueId = player.getUniqueID();
        if (p != null) {
            this.x = p.getX();
            this.y = p.getY();
            this.z = p.getZ();
            this.yaw = (byte) (int) (p.getYRot() * 256.0F / 360.0F);
            this.pitch = (byte) (int) (p.getXRot() * 256.0F / 360.0F);
        }
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

    public byte getYaw() {
        return yaw;
    }

    public byte getPitch() {
        return pitch;
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
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(e.getId(), e.getUUID(), x, y, z, e.getXRot(), e.getYRot(), e.getType(), 0,
                e.getDeltaMovement(), e.getYHeadRot());
    }
}
