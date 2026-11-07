package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_6_4.forward.ChunkWire164;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.6.4's chunk data packet (mods resend a chunk after changing it behind the game's back: {@code new Packet51MapChunk(chunk, true, 65535)}).
 * Delivered as the modern chunk-with-light packet of the same chunk. The legacy wire format is produced too (see {@link ChunkWire164}): chunk
 * x and z, the full-chunk flag, the section mask and add mask as shorts, then the DEFLATE-compressed section arrays behind an int length.
 * Chunks built from a world carry full sky light and no block light, since the client recomputes both.
 */
public class Packet51MapChunk extends Packet {
    public int xCh;
    public int zCh;
    /** The section mask (a 1 for every section in the data). */
    public int yChMin;
    /** The "add" section mask (sections with block ids above 255). */
    public int yChMax;
    public boolean includeInitialize;
    private byte[] compressed = new byte[0];
    private Chunk chunk;

    public Packet51MapChunk() {
        this.isChunkDataPacket = true;
    }

    public Packet51MapChunk(Chunk chunk, boolean includeInitialize, int sectionMask) {
        this();
        this.chunk = chunk;
        this.xCh = chunk.xPosition;
        this.zCh = chunk.zPosition;
        this.includeInitialize = includeInitialize;
        ChunkWire164.Encoded data = ChunkWire164.encode(new ChunkSource(chunk), sectionMask, true, includeInitialize ? chunk.getBiomeArray() : null);
        this.yChMin = data.mask();
        this.yChMax = data.addMask();
        this.compressed = ChunkWire164.deflate(data.data());
    }

    /** 1.4.7 {@code getCompressedChunkData()}: the DEFLATE-compressed section arrays. */
    public byte[] getCompressedChunkData() {
        return compressed;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        xCh = in.readInt();
        zCh = in.readInt();
        includeInitialize = in.readBoolean();
        yChMin = in.readShort() & 65535;
        yChMax = in.readShort() & 65535;
        int length = in.readInt();
        if (length < 0 || length > 16 * 1024 * 1024) throw new IOException("Bad chunk data length " + length);
        compressed = new byte[length];
        in.readFully(compressed);
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(xCh);
        out.writeInt(zCh);
        out.writeBoolean(includeInitialize);
        out.writeShort(yChMin & 65535);
        out.writeShort(yChMax & 65535);
        out.writeInt(compressed.length);
        out.write(compressed);
    }

    @Override
    public int getPacketSize() {
        return 17 + compressed.length;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        if (chunk == null || !(chunk.mcc$modern() instanceof net.minecraft.world.level.chunk.LevelChunk live)) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(live,
                live.getLevel().getLightEngine(), null, null));
    }

    /** The blocks of a shim chunk by legacy section and index. */
    private record ChunkSource(Chunk chunk) implements ChunkWire164.Source {
        @Override
        public int id(int section, int index) {
            var block = chunk.getBlock(index & 15, section * 16 + (index >> 8), index >> 4 & 15);
            return block == null ? 0 : legacyconnector.v1_6_4.net.minecraft.util.Ids.blockId(block);
        }

        @Override
        public int meta(int section, int index) {
            return chunk.getBlockMetadata(index & 15, section * 16 + (index >> 8), index >> 4 & 15);
        }

        @Override
        public int blockLight(int section, int index) {
            return 0;
        }

        @Override
        public int skyLight(int section, int index) {
            return 15;
        }
    }
}
