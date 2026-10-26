package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** An entity's motion, in 1/8000 blocks per tick (limited to 3.9 blocks per tick). */
public class S12PacketEntityVelocity extends Packet {
    public int field_149417_a;
    public int field_149415_b;
    public int field_149416_c;
    public int field_149414_d;

    public S12PacketEntityVelocity() {
    }

    public S12PacketEntityVelocity(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        this(entity.getEntityId(), entity.mcc$entity().getDeltaMovement().x, entity.mcc$entity().getDeltaMovement().y, entity.mcc$entity().getDeltaMovement().z);
    }

    public S12PacketEntityVelocity(int entityId, double motionX, double motionY, double motionZ) {
        this.field_149417_a = entityId;
        double limit = 3.9D;
        motionX = Math.max(-limit, Math.min(limit, motionX));
        motionY = Math.max(-limit, Math.min(limit, motionY));
        motionZ = Math.max(-limit, Math.min(limit, motionZ));
        this.field_149415_b = (int) (motionX * 8000.0D);
        this.field_149416_c = (int) (motionY * 8000.0D);
        this.field_149414_d = (int) (motionZ * 8000.0D);
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149417_a = data.readInt();
        field_149415_b = data.readShort();
        field_149416_c = data.readShort();
        field_149414_d = data.readShort();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(field_149417_a);
        data.writeShort(field_149415_b);
        data.writeShort(field_149416_c);
        data.writeShort(field_149414_d);
    }

    public int func_149412_c() {
        return field_149417_a;
    }

    public int func_149411_d() {
        return field_149415_b;
    }

    public int func_149410_e() {
        return field_149416_c;
    }

    public int func_149409_f() {
        return field_149414_d;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(field_149417_a,
                new net.minecraft.world.phys.Vec3(field_149415_b / 8000.0D, field_149416_c / 8000.0D, field_149414_d / 8000.0D)));
    }
}
