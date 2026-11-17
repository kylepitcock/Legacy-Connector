package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityList;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

/**
 * A mob appears: id, type, position in 1/32 blocks, angles in 1/256 turns, motion in 1/8000 blocks per tick and the watched data, in the
 * 1.7.10 wire format (mods read and write it for their own spawn handshakes). The modern game spawns and syncs entities itself, so
 * sending the packet to a player adds nothing there.
 */
public class S0FPacketSpawnMob extends Packet {
    private int field_149042_a;
    private int field_149040_b;
    private int field_149041_c;
    private int field_149038_d;
    private int field_149039_e;
    private int field_149036_f;
    private int field_149037_g;
    private int field_149047_h;
    private byte field_149048_i;
    private byte field_149045_j;
    private byte field_149046_k;
    private List field_149044_m;

    public S0FPacketSpawnMob() {
    }

    public S0FPacketSpawnMob(EntityLivingBase entity) {
        field_149042_a = entity.getEntityId();
        field_149040_b = (byte) EntityList.getEntityID(entity);
        field_149041_c = MathHelper.floor_double(Entity.mcc$get$posX(entity) * 32.0D);
        field_149038_d = MathHelper.floor_double(Entity.mcc$get$posY(entity) * 32.0D);
        field_149039_e = MathHelper.floor_double(Entity.mcc$get$posZ(entity) * 32.0D);
        field_149048_i = (byte) (int) (Entity.mcc$get$rotationYaw(entity) * 256.0F / 360.0F);
        field_149045_j = (byte) (int) (Entity.mcc$get$rotationPitch(entity) * 256.0F / 360.0F);
        field_149046_k = (byte) (int) (EntityLivingBase.mcc$get$rotationYawHead(entity) * 256.0F / 360.0F);
        double limit = 3.9D;
        field_149036_f = (int) (clamp(Entity.mcc$get$motionX(entity), limit) * 8000.0D);
        field_149037_g = (int) (clamp(Entity.mcc$get$motionY(entity), limit) * 8000.0D);
        field_149047_h = (int) (clamp(Entity.mcc$get$motionZ(entity), limit) * 8000.0D);
        field_149044_m = entity.getDataWatcher().getAllWatched();
    }

    private static double clamp(double v, double limit) {
        return v < -limit ? -limit : Math.min(v, limit);
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149042_a = data.readVarIntFromBuffer();
        field_149040_b = data.readByte() & 255;
        field_149041_c = data.readInt();
        field_149038_d = data.readInt();
        field_149039_e = data.readInt();
        field_149048_i = data.readByte();
        field_149045_j = data.readByte();
        field_149046_k = data.readByte();
        field_149036_f = data.readShort();
        field_149037_g = data.readShort();
        field_149047_h = data.readShort();
        field_149044_m = S1CPacketEntityMetadata.mcc$readWatched(data);
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeVarIntToBuffer(field_149042_a);
        data.writeByte(field_149040_b & 255);
        data.writeInt(field_149041_c);
        data.writeInt(field_149038_d);
        data.writeInt(field_149039_e);
        data.writeByte(field_149048_i);
        data.writeByte(field_149045_j);
        data.writeByte(field_149046_k);
        data.writeShort(field_149036_f);
        data.writeShort(field_149037_g);
        data.writeShort(field_149047_h);
        S1CPacketEntityMetadata.mcc$writeWatched(data, field_149044_m);
    }

    public List func_149027_c() {
        return field_149044_m;
    }

    public int func_149024_d() {
        return field_149042_a;
    }

    public int func_149025_e() {
        return field_149040_b;
    }

    public int func_149023_f() {
        return field_149041_c;
    }

    public int func_149034_g() {
        return field_149038_d;
    }

    public int func_149029_h() {
        return field_149039_e;
    }

    public int func_149026_i() {
        return field_149036_f;
    }

    public int func_149033_j() {
        return field_149037_g;
    }

    public int func_149031_k() {
        return field_149047_h;
    }

    public byte func_149028_l() {
        return field_149048_i;
    }

    public byte func_149030_m() {
        return field_149045_j;
    }

    public byte func_149032_n() {
        return field_149046_k;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
    }
}
