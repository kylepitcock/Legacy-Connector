package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;

/** Several chunks for the client at once (sent as one modern chunk packet each). */
public class S26PacketMapChunkBulk extends Packet {
    public int[] field_149266_a = new int[0];
    public int[] field_149264_b = new int[0];
    public int[] field_149265_c = new int[0];
    public int[] field_149262_d = new int[0];
    public boolean field_149267_h;
    private final List<Chunk> mcc$chunks = new ArrayList<>();

    public S26PacketMapChunkBulk() {
    }

    public S26PacketMapChunkBulk(List<Chunk> chunks) {
        if (chunks != null) mcc$chunks.addAll(chunks);
        int n = mcc$chunks.size();
        field_149266_a = new int[n];
        field_149264_b = new int[n];
        field_149265_c = new int[n];
        field_149262_d = new int[n];
        for (int i = 0; i < n; i++) {
            Chunk c = mcc$chunks.get(i);
            field_149266_a[i] = c.xPosition;
            field_149264_b[i] = c.zPosition;
            field_149265_c[i] = 0xFFFF;
        }
        field_149267_h = true;
    }

    public static int func_149258_c() {
        return 5;
    }

    public int func_149255_a(int index) {
        return field_149266_a[index];
    }

    public int func_149253_b(int index) {
        return field_149264_b[index];
    }

    public int func_149254_d() {
        return field_149266_a.length;
    }

    public byte[] func_149256_c(int index) {
        return new byte[0];
    }

    public int[] func_149252_e() {
        return field_149265_c;
    }

    public int[] func_149257_f() {
        return field_149262_d;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        for (Chunk c : mcc$chunks) {
            var packet = S21PacketChunkData.mcc$modern(player, c);
            if (packet != null) player.connection.send(packet);
        }
    }
}
