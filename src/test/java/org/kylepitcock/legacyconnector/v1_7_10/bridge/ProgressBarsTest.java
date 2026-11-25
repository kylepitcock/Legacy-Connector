package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Progress bar values survive the wire whole, so a mod's own split of a big value (IC2's energy) joins back exactly. */
class ProgressBarsTest {
    private static final int[] VALUES = {0, 1, 255, 30000, 32767, 32768, 40000, 65535, 65536, 600000, 10_000_000, Integer.MAX_VALUE, -1,
            -25536, -32768, Integer.MIN_VALUE};

    @Test
    void valuesRoundTripOverTheFullIntRange() {
        for (int v : VALUES) assertArrayEquals(new int[] {7, 3, v}, ProgressBars.decode(ProgressBars.encode(7, 3, v)));
        java.util.Random r = new java.util.Random(42);
        for (int i = 0; i < 10000; i++) {
            int v = r.nextInt();
            assertEquals(v, ProgressBars.decode(ProgressBars.encode(100, 1, v))[2]);
        }
    }

    /** IC2 1.4.7's BatBox container: sends energy & 65535 as bar 0 and energy >>> 16 as bar 1; the client ORs them into the tile. */
    @Test
    void ic2EnergySplitJoinsBack() {
        for (int energy : new int[] {0, 22280, 30000, 32767, 32768, 40000, 65535, 65536, 600000, 10_000_000, Integer.MAX_VALUE}) {
            for (boolean lowFirst : new boolean[] {true, false}) {
                int client = 0;
                int[][] bars = {{0, energy & 65535}, {1, energy >>> 16}};
                for (int k = 0; k < 2; k++) {
                    int[] bar = ProgressBars.decode(ProgressBars.encode(5, bars[lowFirst ? k : 1 - k][0], bars[lowFirst ? k : 1 - k][1]));
                    if (bar[1] == 0) client = client & -65536 | bar[2];
                    else client = client & 65535 | bar[2] << 16;
                }
                assertEquals(energy, client, "energy " + energy + " lowFirst " + lowFirst);
            }
        }
    }

    /** Had the value travelled as a signed short (1.4.7's packet), the low half of 40000 would be negative and, sent last, would corrupt the high half. */
    @Test
    void asShortItWouldBreak() {
        int energy = 40000;
        int low = (short) (energy & 65535);
        assertEquals(-25536, low);
        int client = (energy >>> 16) << 16;
        client = client & -65536 | low;
        assertEquals(-25536, client);
    }
}
