package org.kylepitcock.legacyconnector.v1_20_1.migrate;

/**
 * GeckoLib 4.8 (1.20.1) code against GeckoLib 4.9 (1.21.1), rows of {@code forward/1.20.1/lib-*.tsv}: the render methods take the colour as one packed
 * ARGB int where 4.8 had four floats.
 */
public final class GeckoCompat {
    private GeckoCompat() {
    }

    /** {@code (r, g, b, a)} floats of 0 to 1 as an ARGB int. */
    public static int pack(float r, float g, float b, float a) {
        return (to8(a) << 24) | (to8(r) << 16) | (to8(g) << 8) | to8(b);
    }

    private static int to8(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255.0F)));
    }

    public static float red(int argb) {
        return ((argb >> 16) & 255) / 255.0F;
    }

    public static float green(int argb) {
        return ((argb >> 8) & 255) / 255.0F;
    }

    public static float blue(int argb) {
        return (argb & 255) / 255.0F;
    }

    public static float alpha(int argb) {
        return ((argb >>> 24) & 255) / 255.0F;
    }

    /** {@code GeckoLib.initialize()}: 4.9 initialises itself as a mod. */
    public static void initialize() {
    }
}
