package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's entity velocity packet (motion in 1/8000 blocks per tick, at most 3.9 blocks): delivered as the modern set-motion packet. */
public class Packet28EntityVelocity extends Packet {
    public int entityId;
    public int motionX;
    public int motionY;
    public int motionZ;

    public Packet28EntityVelocity() {}

    public Packet28EntityVelocity(Entity entity) {
        this(entity.getEntityId(), entity.mcc$entity().getDeltaMovement().x, entity.mcc$entity().getDeltaMovement().y,
                entity.mcc$entity().getDeltaMovement().z);
    }

    public Packet28EntityVelocity(int entityId, double x, double y, double z) {
        S12PacketEntityVelocity s = new S12PacketEntityVelocity(entityId, x, y, z); // clamps and scales like 1.6.4 did
        this.entityId = entityId;
        this.motionX = s.func_149411_d();
        this.motionY = s.func_149410_e();
        this.motionZ = s.func_149409_f();
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        entityId = in.readInt();
        motionX = in.readShort();
        motionY = in.readShort();
        motionZ = in.readShort();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(entityId);
        out.writeShort(motionX);
        out.writeShort(motionY);
        out.writeShort(motionZ);
    }

    @Override
    public int getPacketSize() {
        return 10;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entityId,
                new net.minecraft.world.phys.Vec3(motionX / 8000.0D, motionY / 8000.0D, motionZ / 8000.0D)));
    }
}
