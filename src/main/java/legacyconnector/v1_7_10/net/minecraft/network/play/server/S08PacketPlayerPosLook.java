package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * Moves the player's client to a position (y is the eye height, 1.62 above the feet, as in 1.7.10); sent as the modern
 * teleport, which also moves the player on the server.
 */
public class S08PacketPlayerPosLook extends Packet {
    public double field_148940_a;
    public double field_148938_b;
    public double field_148939_c;
    public float field_148936_d;
    public float field_148937_e;
    public boolean field_148935_f;

    public S08PacketPlayerPosLook() {
    }

    public S08PacketPlayerPosLook(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.field_148940_a = x;
        this.field_148938_b = y;
        this.field_148939_c = z;
        this.field_148936_d = yaw;
        this.field_148937_e = pitch;
        this.field_148935_f = onGround;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_148940_a = data.readDouble();
        field_148938_b = data.readDouble();
        field_148939_c = data.readDouble();
        field_148936_d = data.readFloat();
        field_148937_e = data.readFloat();
        field_148935_f = data.readBoolean();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeDouble(field_148940_a);
        data.writeDouble(field_148938_b);
        data.writeDouble(field_148939_c);
        data.writeFloat(field_148936_d);
        data.writeFloat(field_148937_e);
        data.writeBoolean(field_148935_f);
    }

    public double func_148932_c() {
        return field_148940_a;
    }

    public double func_148928_d() {
        return field_148938_b;
    }

    public double func_148933_e() {
        return field_148939_c;
    }

    public float func_148931_f() {
        return field_148936_d;
    }

    public float func_148930_g() {
        return field_148937_e;
    }

    public boolean func_148929_h() {
        return field_148935_f;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.teleport(field_148940_a, field_148938_b - 1.62D, field_148939_c, field_148936_d, field_148937_e);
    }
}
