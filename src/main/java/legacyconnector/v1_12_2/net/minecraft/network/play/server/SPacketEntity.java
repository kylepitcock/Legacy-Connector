package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/**
 * An entity's relative move and/or look: position deltas in 1/4096 blocks, angles in 1/256 turns (the same encoding the modern
 * packets use); sent as the modern move-entity packet.
 */
public class SPacketEntity implements Packet<INetHandlerPlayClient> {
    protected int entityId;
    protected int posX;
    protected int posY;
    protected int posZ;
    protected byte yaw;
    protected byte pitch;
    protected boolean onGround;
    protected boolean rotating;

    public SPacketEntity() {
    }

    public SPacketEntity(int entityIdIn) {
        this.entityId = entityIdIn;
    }

    public int mcc$entityId() {
        return entityId;
    }

    public double getX() {
        return posX / 4096.0D;
    }

    public double getY() {
        return posY / 4096.0D;
    }

    public double getZ() {
        return posZ / 4096.0D;
    }

    public byte getYaw() {
        return yaw;
    }

    public byte getPitch() {
        return pitch;
    }

    public boolean isRotating() {
        return rotating;
    }

    public boolean getOnGround() {
        return onGround;
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
        // the base packet only marks the entity as still there (1.12 sent it as a keep-alive of the entity)
        return null;
    }

    public static class S15PacketEntityRelMove extends SPacketEntity {
        public S15PacketEntityRelMove() {
        }

        public S15PacketEntityRelMove(int entityIdIn, long xIn, long yIn, long zIn, boolean onGroundIn) {
            super(entityIdIn);
            this.posX = (int) xIn;
            this.posY = (int) yIn;
            this.posZ = (int) zIn;
            this.onGround = onGroundIn;
        }

        @Override
        public net.minecraft.network.protocol.Packet<?> mcc$modern() {
            return new net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Pos(entityId, (short) posX, (short) posY, (short) posZ, onGround);
        }
    }

    public static class S16PacketEntityLook extends SPacketEntity {
        public S16PacketEntityLook() {
            this.rotating = true;
        }

        public S16PacketEntityLook(int entityIdIn, byte yawIn, byte pitchIn, boolean onGroundIn) {
            super(entityIdIn);
            this.yaw = yawIn;
            this.pitch = pitchIn;
            this.rotating = true;
            this.onGround = onGroundIn;
        }

        @Override
        public net.minecraft.network.protocol.Packet<?> mcc$modern() {
            return new net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Rot(entityId, yaw, pitch, onGround);
        }
    }

    public static class S17PacketEntityLookMove extends SPacketEntity {
        public S17PacketEntityLookMove() {
            this.rotating = true;
        }

        public S17PacketEntityLookMove(int entityIdIn, long xIn, long yIn, long zIn, byte yawIn, byte pitchIn, boolean onGroundIn) {
            super(entityIdIn);
            this.posX = (int) xIn;
            this.posY = (int) yIn;
            this.posZ = (int) zIn;
            this.yaw = yawIn;
            this.pitch = pitchIn;
            this.onGround = onGroundIn;
            this.rotating = true;
        }

        @Override
        public net.minecraft.network.protocol.Packet<?> mcc$modern() {
            return new net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.PosRot(entityId, (short) posX, (short) posY, (short) posZ, yaw, pitch, onGround);
        }
    }
}
