package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** A sign's four lines. */
public class S33PacketUpdateSign extends Packet {
    public int field_149352_a;
    public int field_149350_b;
    public int field_149351_c;
    public String[] field_149349_d;

    public S33PacketUpdateSign() {
    }

    public S33PacketUpdateSign(int x, int y, int z, String[] lines) {
        this.field_149352_a = x;
        this.field_149350_b = y;
        this.field_149351_c = z;
        this.field_149349_d = new String[] {lines[0], lines[1], lines[2], lines[3]};
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149352_a = data.readInt();
        field_149350_b = data.readShort();
        field_149351_c = data.readInt();
        field_149349_d = new String[4];
        for (int i = 0; i < 4; ++i) field_149349_d[i] = data.readStringFromBuffer(15);
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(field_149352_a);
        data.writeShort(field_149350_b);
        data.writeInt(field_149351_c);
        for (int i = 0; i < 4; ++i) data.writeStringToBuffer(field_149349_d[i]);
    }

    public int func_149346_c() {
        return field_149352_a;
    }

    public int func_149345_d() {
        return field_149350_b;
    }

    public int func_149344_e() {
        return field_149351_c;
    }

    public String[] func_149347_f() {
        return field_149349_d;
    }

    /** The sign's block entity already sends its text when it changes; this resends it. */
    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        var be = player.serverLevel().getBlockEntity(new net.minecraft.core.BlockPos(field_149352_a, field_149350_b, field_149351_c));
        if (be != null) {
            var packet = be.getUpdatePacket();
            if (packet != null) player.connection.send(packet);
        }
    }
}
