package legacyconnector.v1_6_4.forward;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * The payload of the 1.4.7 to 1.6.4 chunk packets ({@code Packet51MapChunk}, {@code Packet56MapChunks}) before DEFLATE: for the 16 sections of 16 blocks
 * high selected by a bit mask, the low 8 bits of the block ids (4096 bytes per section, index {@code y << 8 | z << 4 | x}), then the metadata nibbles
 * (2048 per section), the block light nibbles, the sky light nibbles (overworld only), the high 4 bits of the ids of the sections in the "add" mask
 * (sections with an id above 255), and finally the 256 biome bytes of a full chunk. Each array is stored for all selected sections before the next kind starts.
 */
public final class ChunkWire164 {
    private ChunkWire164() {}

    /** The blocks and light of one chunk, by section (0 to 15) and index within the section. */
    public interface Source {
        int id(int section, int index);

        int meta(int section, int index);

        int blockLight(int section, int index);

        int skyLight(int section, int index);
    }

    /** The encoded payload with the section masks it describes. */
    public record Encoded(byte[] data, int mask, int addMask) {}

    /** The arrays read back; entries of sections that are not in the mask are null. */
    public record Decoded(int[][] ids, int[][] meta, int[][] blockLight, int[][] skyLight, byte[] biomes) {}

    public static Encoded encode(Source src, int mask, boolean skyLight, byte[] biomes) {
        mask &= 65535;
        int addMask = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int s = 0; s < 16; s++) {
            if ((mask & (1 << s)) == 0) continue;
            for (int i = 0; i < 4096; i++) {
                int id = src.id(s, i);
                out.write(id & 255);
                if (id > 255) addMask |= 1 << s;
            }
        }
        for (int s = 0; s < 16; s++) if ((mask & (1 << s)) != 0) nibbles(out, src, s, 0);
        for (int s = 0; s < 16; s++) if ((mask & (1 << s)) != 0) nibbles(out, src, s, 1);
        if (skyLight) for (int s = 0; s < 16; s++) if ((mask & (1 << s)) != 0) nibbles(out, src, s, 2);
        for (int s = 0; s < 16; s++) if ((addMask & (1 << s)) != 0) nibbles(out, src, s, 3);
        if (biomes != null) out.write(biomes, 0, 256);
        return new Encoded(out.toByteArray(), mask, addMask);
    }

    private static void nibbles(ByteArrayOutputStream out, Source src, int section, int kind) {
        for (int i = 0; i < 4096; i += 2) {
            out.write(nibble(src, section, i, kind) | nibble(src, section, i + 1, kind) << 4);
        }
    }

    private static int nibble(Source src, int section, int index, int kind) {
        return switch (kind) {
            case 0 -> src.meta(section, index);
            case 1 -> src.blockLight(section, index);
            case 2 -> src.skyLight(section, index);
            default -> src.id(section, index) >> 8;
        } & 15;
    }

    public static Decoded decode(byte[] data, int mask, int addMask, boolean skyLight, boolean hasBiomes) {
        int[][] ids = new int[16][];
        int[][] meta = new int[16][];
        int[][] block = new int[16][];
        int[][] sky = new int[16][];
        int p = 0;
        for (int s = 0; s < 16; s++) {
            if ((mask & (1 << s)) == 0) continue;
            ids[s] = new int[4096];
            for (int i = 0; i < 4096; i++) ids[s][i] = data[p++] & 255;
        }
        p = readNibbles(data, p, mask, meta);
        p = readNibbles(data, p, mask, block);
        if (skyLight) p = readNibbles(data, p, mask, sky);
        int[][] add = new int[16][];
        p = readNibbles(data, p, addMask, add);
        for (int s = 0; s < 16; s++) {
            if (add[s] == null) continue;
            for (int i = 0; i < 4096; i++) ids[s][i] |= add[s][i] << 8;
        }
        byte[] biomes = null;
        if (hasBiomes) {
            biomes = new byte[256];
            System.arraycopy(data, p, biomes, 0, 256);
        }
        return new Decoded(ids, meta, block, sky, biomes);
    }

    private static int readNibbles(byte[] data, int p, int mask, int[][] into) {
        for (int s = 0; s < 16; s++) {
            if ((mask & (1 << s)) == 0) continue;
            into[s] = new int[4096];
            for (int i = 0; i < 4096; i += 2) {
                int b = data[p++];
                into[s][i] = b & 15;
                into[s][i + 1] = b >> 4 & 15;
            }
        }
        return p;
    }

    public static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(-1);
        try {
            deflater.setInput(data);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(data.length / 4 + 64);
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer));
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    public static byte[] inflate(byte[] compressed, int expectedLength) throws IOException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            byte[] out = new byte[expectedLength];
            int read = 0;
            while (read < expectedLength && !inflater.finished()) read += inflater.inflate(out, read, expectedLength - read);
            if (read != expectedLength) throw new IOException("Bad compressed chunk data: " + read + " of " + expectedLength + " bytes");
            return out;
        } catch (DataFormatException e) {
            throw new IOException("Bad compressed chunk data", e);
        } finally {
            inflater.end();
        }
    }

    /** The size of the uncompressed payload for the masks, as the receiving client computes it. */
    public static int payloadLength(int mask, int addMask, boolean skyLight, boolean hasBiomes) {
        int sections = Integer.bitCount(mask & 65535);
        return sections * 4096 + sections * 2048 * (skyLight ? 3 : 2) + Integer.bitCount(addMask & 65535) * 2048 + (hasBiomes ? 256 : 0);
    }
}
