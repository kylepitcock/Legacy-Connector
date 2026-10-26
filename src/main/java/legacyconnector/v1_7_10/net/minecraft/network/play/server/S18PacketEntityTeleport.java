package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** An entity's absolute position (in 1/32 block) and rotation (in 1/256 turns). */
public class S18PacketEntityTeleport extends Packet {
    public int field_149458_a;
    public int field_149456_b;
    public int field_149457_c;
    public int field_149454_d;
    public byte field_149453_f;
    public byte field_149455_e;

    public S18PacketEntityTeleport() {
    }

    public S18PacketEntityTeleport(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        net.minecraft.world.entity.Entity e = entity.mcc$entity();
        this.field_149458_a = e.getId();
        this.field_149456_b = net.minecraft.util.Mth.floor(e.getX() * 32.0D);
        this.field_149457_c = net.minecraft.util.Mth.floor(e.getY() * 32.0D);
        this.field_149454_d = net.minecraft.util.Mth.floor(e.getZ() * 32.0D);
        this.field_149453_f = (byte) (int) (e.getYRot() * 256.0F / 360.0F);
        this.field_149455_e = (byte) (int) (e.getXRot() * 256.0F / 360.0F);
    }

    public S18PacketEntityTeleport(int entityId, int x, int y, int z, byte yaw, byte pitch) {
        this.field_149458_a = entityId;
        this.field_149456_b = x;
        this.field_149457_c = y;
        this.field_149454_d = z;
        this.field_149453_f = yaw;
        this.field_149455_e = pitch;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149458_a = data.readInt();
        field_149456_b = data.readInt();
        field_149457_c = data.readInt();
        field_149454_d = data.readInt();
        field_149453_f = data.readByte();
        field_149455_e = data.readByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(field_149458_a);
        data.writeInt(field_149456_b);
        data.writeInt(field_149457_c);
        data.writeInt(field_149454_d);
        data.writeByte(field_149453_f);
        data.writeByte(field_149455_e);
    }

    public int func_149451_c() {
        return field_149458_a;
    }

    public int func_149449_d() {
        return field_149456_b;
    }

    public int func_149448_e() {
        return field_149457_c;
    }

    public int func_149446_f() {
        return field_149454_d;
    }

    public byte func_149450_g() {
        return field_149453_f;
    }

    public byte func_149447_h() {
        return field_149455_e;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        var e = player.serverLevel().getEntity(field_149458_a);
        net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeVarInt(field_149458_a);
        buf.writeDouble(field_149456_b / 32.0D);
        buf.writeDouble(field_149457_c / 32.0D);
        buf.writeDouble(field_149454_d / 32.0D);
        buf.writeByte(field_149453_f);
        buf.writeByte(field_149455_e);
        buf.writeBoolean(e != null && e.onGround());
        player.connection.send(net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket.STREAM_CODEC.decode(buf));
    }
}
