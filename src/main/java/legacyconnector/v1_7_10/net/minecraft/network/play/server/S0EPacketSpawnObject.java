package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

/**
 * An object (arrow, minecart, item frame, falling block, ...) appears: id, object type, position in 1/32 blocks, angles in 1/256 turns,
 * the thrower's id (0 when none) and, when there is one, the motion in 1/8000 blocks per tick, in the 1.7.10 wire format. The modern game
 * spawns entities itself, so sending the packet to a player adds nothing there.
 */
public class S0EPacketSpawnObject extends Packet {
    private int field_149018_a;
    private int field_149016_b;
    private int field_149017_c;
    private int field_149014_d;
    private int field_149015_e;
    private int field_149012_f;
    private int field_149013_g;
    private int field_149021_h;
    private int field_149022_i;
    private int field_149019_j;
    private int field_149020_k;

    public S0EPacketSpawnObject() {
    }

    public S0EPacketSpawnObject(Entity entity, int type) {
        this(entity, type, 0);
    }

    public S0EPacketSpawnObject(Entity entity, int type, int throwerId) {
        field_149018_a = entity.getEntityId();
        field_149016_b = MathHelper.floor_double(Entity.mcc$get$posX(entity) * 32.0D);
        field_149017_c = MathHelper.floor_double(Entity.mcc$get$posY(entity) * 32.0D);
        field_149014_d = MathHelper.floor_double(Entity.mcc$get$posZ(entity) * 32.0D);
        field_149021_h = MathHelper.floor_float(Entity.mcc$get$rotationPitch(entity) * 256.0F / 360.0F);
        field_149022_i = MathHelper.floor_float(Entity.mcc$get$rotationYaw(entity) * 256.0F / 360.0F);
        field_149019_j = type;
        field_149020_k = throwerId;
        if (throwerId > 0) {
            double limit = 3.9D;
            field_149015_e = (int) (clamp(Entity.mcc$get$motionX(entity), limit) * 8000.0D);
            field_149012_f = (int) (clamp(Entity.mcc$get$motionY(entity), limit) * 8000.0D);
            field_149013_g = (int) (clamp(Entity.mcc$get$motionZ(entity), limit) * 8000.0D);
        }
    }

    private static double clamp(double v, double limit) {
        return v < -limit ? -limit : Math.min(v, limit);
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149018_a = data.readVarIntFromBuffer();
        field_149019_j = data.readByte();
        field_149016_b = data.readInt();
        field_149017_c = data.readInt();
        field_149014_d = data.readInt();
        field_149021_h = data.readByte();
        field_149022_i = data.readByte();
        field_149020_k = data.readInt();
        if (field_149020_k > 0) {
            field_149015_e = data.readShort();
            field_149012_f = data.readShort();
            field_149013_g = data.readShort();
        }
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeVarIntToBuffer(field_149018_a);
        data.writeByte(field_149019_j);
        data.writeInt(field_149016_b);
        data.writeInt(field_149017_c);
        data.writeInt(field_149014_d);
        data.writeByte(field_149021_h);
        data.writeByte(field_149022_i);
        data.writeInt(field_149020_k);
        if (field_149020_k > 0) {
            data.writeShort(field_149015_e);
            data.writeShort(field_149012_f);
            data.writeShort(field_149013_g);
        }
    }

    public int func_149001_c() {
        return field_149018_a;
    }

    public int func_148997_d() {
        return field_149016_b;
    }

    public int func_148998_e() {
        return field_149017_c;
    }

    public int func_148994_f() {
        return field_149014_d;
    }

    public int func_149010_g() {
        return field_149015_e;
    }

    public int func_149004_h() {
        return field_149012_f;
    }

    public int func_148999_i() {
        return field_149013_g;
    }

    public int func_149008_j() {
        return field_149021_h;
    }

    public int func_149006_k() {
        return field_149022_i;
    }

    public int func_148993_l() {
        return field_149019_j;
    }

    public int func_149009_m() {
        return field_149020_k;
    }

    public void func_148996_a(int x) {
        field_149016_b = x;
    }

    public void func_148995_b(int y) {
        field_149017_c = y;
    }

    public void func_149005_c(int z) {
        field_149014_d = z;
    }

    public void func_149003_d(int velX) {
        field_149015_e = velX;
    }

    public void func_149000_e(int velY) {
        field_149012_f = velY;
    }

    public void func_149007_f(int velZ) {
        field_149013_g = velZ;
    }

    public void func_149002_g(int throwerId) {
        field_149020_k = throwerId;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
    }
}
