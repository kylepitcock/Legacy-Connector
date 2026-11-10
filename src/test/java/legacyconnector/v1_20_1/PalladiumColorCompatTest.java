package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class PalladiumColorCompatTest {
    @Test
    void convertsWhiteSheepColorToNormalizedRgb() {
        assertArrayEquals(new float[] {230 / 255.0F, 230 / 255.0F, 230 / 255.0F},
                PalladiumColorCompat.rgbToFloatArray(0xE6E6E6), 0.000001F);
    }

    @Test
    void extractsRgbChannelsWithoutTreatingTheHighByteAsAlpha() {
        assertArrayEquals(new float[] {18 / 255.0F, 52 / 255.0F, 86 / 255.0F},
                PalladiumColorCompat.rgbToFloatArray(0xFF123456), 0.000001F);
    }
}
