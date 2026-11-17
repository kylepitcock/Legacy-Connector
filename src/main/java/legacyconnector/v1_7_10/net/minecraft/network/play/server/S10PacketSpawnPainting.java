package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityHanging;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityPainting;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/**
 * A painting appears: id, art title, the block it hangs on and its facing, in the 1.7.10 wire format. The modern game spawns paintings
 * itself, so sending the packet to a player adds nothing there.
 */
public class S10PacketSpawnPainting extends Packet {
    private int field_148970_a;
    private int field_148968_b;
    private int field_148969_c;
    private int field_148966_d;
    private int field_148967_e;
    private String field_148964_f = "";

    public S10PacketSpawnPainting() {
    }

    public S10PacketSpawnPainting(EntityPainting painting) {
        field_148970_a = painting.getEntityId();
        field_148968_b = EntityHanging.mcc$get$field_146063_b(painting);
        field_148969_c = EntityHanging.mcc$get$field_146064_c(painting);
        field_148966_d = EntityHanging.mcc$get$field_146062_d(painting);
        field_148967_e = EntityHanging.mcc$get$hangingDirection(painting);
        EntityPainting.EnumArt art = EntityPainting.mcc$get$art(painting);
        field_148964_f = art == null ? "" : art.title;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_148970_a = data.readVarIntFromBuffer();
        field_148964_f = data.readStringFromBuffer(EntityPainting.EnumArt.maxArtTitleLength);
        field_148968_b = data.readInt();
        field_148969_c = data.readInt();
        field_148966_d = data.readInt();
        field_148967_e = data.readInt();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeVarIntToBuffer(field_148970_a);
        data.writeStringToBuffer(field_148964_f);
        data.writeInt(field_148968_b);
        data.writeInt(field_148969_c);
        data.writeInt(field_148966_d);
        data.writeInt(field_148967_e);
    }

    public int func_148965_c() {
        return field_148970_a;
    }

    public int func_148964_d() {
        return field_148968_b;
    }

    public int func_148963_e() {
        return field_148969_c;
    }

    public int func_148962_f() {
        return field_148966_d;
    }

    public int func_148966_g() {
        return field_148967_e;
    }

    public String func_148961_h() {
        return field_148964_f;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
    }
}
