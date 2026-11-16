package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;

/**
 * Several blocks of one chunk changed. Each entry is a position short ({@code x << 12 | z << 8 | y}, relative to the chunk) and the block id and
 * metadata ({@code id << 4 | meta}) it had when the packet was built. Delivered by sending the server's current state of every listed position.
 */
public class S22PacketMultiBlockChange extends Packet {
    private ChunkCoordIntPair chunk;
    private short[] positions = new short[0];
    private int[] blocks = new int[0];

    public S22PacketMultiBlockChange() {
    }

    public S22PacketMultiBlockChange(int count, short[] positions, Chunk chunk) {
        this.chunk = new ChunkCoordIntPair(chunk.xPosition, chunk.zPosition);
        this.positions = java.util.Arrays.copyOf(positions, count);
        this.blocks = new int[count];
        for (int i = 0; i < count; i++) {
            int x = positions[i] >> 12 & 15, z = positions[i] >> 8 & 15, y = positions[i] & 255;
            this.blocks[i] = Block.getIdFromBlock(chunk.getBlock(x, y, z)) << 4 | chunk.getBlockMetadata(x, y, z) & 15;
        }
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        chunk = new ChunkCoordIntPair(data.readInt(), data.readInt());
        int count = data.readShort() & 65535;
        data.readInt();
        positions = new short[count];
        blocks = new int[count];
        for (int i = 0; i < count; i++) {
            positions[i] = data.readShort();
            blocks[i] = data.readShort() & 65535;
        }
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(chunk == null ? 0 : chunk.chunkXPos);
        data.writeInt(chunk == null ? 0 : chunk.chunkZPos);
        data.writeShort(positions.length);
        data.writeInt(positions.length * 4);
        for (int i = 0; i < positions.length; i++) {
            data.writeShort(positions[i]);
            data.writeShort(blocks[i]);
        }
    }

    public ChunkCoordIntPair func_148920_c() {
        return chunk;
    }

    public int func_148921_d() {
        return positions.length;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        if (chunk == null) return;
        for (short p : positions) {
            var pos = new net.minecraft.core.BlockPos((chunk.chunkXPos << 4) + (p >> 12 & 15), p & 255, (chunk.chunkZPos << 4) + (p >> 8 & 15));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.level(), pos));
        }
    }
}
