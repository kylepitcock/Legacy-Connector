package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The crack overlay of a block being broken (progress 0-9; other values remove it). */
public class S25PacketBlockBreakAnim extends Packet {
    public int field_148852_a;
    public int field_148850_b;
    public int field_148851_c;
    public int field_148848_d;
    public int field_148849_e;

    public S25PacketBlockBreakAnim() {
    }

    public S25PacketBlockBreakAnim(int breakerId, int x, int y, int z, int progress) {
        this.field_148852_a = breakerId;
        this.field_148850_b = x;
        this.field_148851_c = y;
        this.field_148848_d = z;
        this.field_148849_e = progress;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_148852_a = data.readVarIntFromBuffer();
        field_148850_b = data.readInt();
        field_148851_c = data.readInt();
        field_148848_d = data.readInt();
        field_148849_e = data.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeVarIntToBuffer(field_148852_a);
        data.writeInt(field_148850_b);
        data.writeInt(field_148851_c);
        data.writeInt(field_148848_d);
        data.writeByte(field_148849_e);
    }

    public int func_148845_c() {
        return field_148852_a;
    }

    public int func_148844_d() {
        return field_148850_b;
    }

    public int func_148843_e() {
        return field_148851_c;
    }

    public int func_148842_f() {
        return field_148848_d;
    }

    public int func_148846_g() {
        return field_148849_e;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket(field_148852_a,
                new net.minecraft.core.BlockPos(field_148850_b, field_148851_c, field_148848_d), field_148849_e));
    }
}
