package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's bulk chunk packet: several chunks at once. Delivered as one {@link Packet51MapChunk} per chunk (full chunks with their initial data). */
public class Packet56MapChunks extends Packet {
    private final List<Packet51MapChunk> chunks = new ArrayList<>();

    public Packet56MapChunks() {
        this.isChunkDataPacket = true;
    }

    public Packet56MapChunks(List chunkList) {
        this();
        for (Object o : chunkList) {
            if (o instanceof Chunk chunk) chunks.add(new Packet51MapChunk(chunk, true, 65535));
        }
    }

    /** The number of chunks the packet carries. */
    public int getNumberOfChunks() {
        return chunks.size();
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        for (Packet51MapChunk chunk : chunks) chunk.mcc$send(player);
    }
}
