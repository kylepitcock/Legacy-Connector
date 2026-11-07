package legacyconnector.v1_6_4.forward;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** The legacy chunk payload: layout, masks, DEFLATE round trip. */
class ChunkWire164Test {
    /** Section 0 is stone with a grass layer, section 2 holds one block above id 255, light and meta follow simple patterns. */
    private static final ChunkWire164.Source SOURCE = new ChunkWire164.Source() {
        @Override
        public int id(int section, int index) {
            if (section == 2 && index == 7) return 4000;
            return section == 0 ? (index >> 8 == 15 ? 2 : 1) : 0;
        }

        @Override
        public int meta(int section, int index) {
            return (index + section) & 15;
        }

        @Override
        public int blockLight(int section, int index) {
            return index % 5;
        }

        @Override
        public int skyLight(int section, int index) {
            return 15 - (index & 3);
        }
    };

    @Test
    void layoutIsIdsThenMetaThenLightThenAddThenBiomes() {
        byte[] biomes = new byte[256];
        biomes[5] = 9;
        ChunkWire164.Encoded e = ChunkWire164.encode(SOURCE, 0b101, true, biomes);
        assertEquals(0b101, e.mask());
        assertEquals(0b100, e.addMask(), "only section 2 has an id above 255");
        assertEquals(ChunkWire164.payloadLength(0b101, 0b100, true, true), e.data().length);
        assertEquals(1, e.data()[0]);
        assertEquals(2, e.data()[15 * 256]);
        assertEquals(4000 & 255, e.data()[4096 + 7] & 255, "section 2 starts after section 0's 4096 id bytes");
        int meta = 2 * 4096;
        assertEquals((0 + 1 & 15) << 4 | 0, e.data()[meta] & 255, "indices 0 and 1 share a byte, the even one in the low nibble");
        assertEquals(9, e.data()[e.data().length - 256 + 5]);
    }

    @Test
    void roundTripThroughDeflateKeepsEveryValue() throws IOException {
        byte[] biomes = new byte[256];
        for (int i = 0; i < 256; i++) biomes[i] = (byte) (i * 3);
        ChunkWire164.Encoded e = ChunkWire164.encode(SOURCE, 0b111, true, biomes);
        byte[] compressed = ChunkWire164.deflate(e.data());
        assertTrue(compressed.length < e.data().length, "the repetitive payload shrinks");
        byte[] data = ChunkWire164.inflate(compressed, ChunkWire164.payloadLength(e.mask(), e.addMask(), true, true));
        assertArrayEquals(e.data(), data);
        ChunkWire164.Decoded d = ChunkWire164.decode(data, e.mask(), e.addMask(), true, true);
        for (int s = 0; s < 3; s++) {
            for (int i = 0; i < 4096; i += 37) {
                assertEquals(SOURCE.id(s, i), d.ids()[s][i], "id " + s + "/" + i);
                assertEquals(SOURCE.meta(s, i), d.meta()[s][i]);
                assertEquals(SOURCE.blockLight(s, i), d.blockLight()[s][i]);
                assertEquals(SOURCE.skyLight(s, i), d.skyLight()[s][i]);
            }
        }
        assertEquals(4000, d.ids()[2][7]);
        assertNull(d.ids()[3], "sections outside the mask are absent");
        assertArrayEquals(biomes, d.biomes());
    }

    @Test
    void withoutSkyLightOrBiomesThePayloadIsSmaller() throws IOException {
        ChunkWire164.Encoded e = ChunkWire164.encode(SOURCE, 0b1, false, null);
        assertEquals(4096 + 2048 * 2, e.data().length);
        assertEquals(0, e.addMask());
        ChunkWire164.Decoded d = ChunkWire164.decode(e.data(), e.mask(), e.addMask(), false, false);
        assertNull(d.biomes());
        assertEquals(2, d.ids()[0][15 * 256]);
    }

    @Test
    void truncatedDataIsRejected() {
        byte[] compressed = ChunkWire164.deflate(new byte[100]);
        assertThrows(IOException.class, () -> ChunkWire164.inflate(compressed, 5000));
    }
}
