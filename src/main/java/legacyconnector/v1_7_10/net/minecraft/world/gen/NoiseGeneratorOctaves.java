package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.Random;

/** Several octaves of improved noise (vanilla terrain, biome and surface noise). */
public class NoiseGeneratorOctaves {
    private final NoiseGeneratorImproved[] generators;
    private final int octaves;

    public NoiseGeneratorOctaves(Random random, int octaves) {
        this.octaves = octaves;
        this.generators = new NoiseGeneratorImproved[octaves];
        for (int i = 0; i < octaves; i++) generators[i] = new NoiseGeneratorImproved(random);
    }

    /** 3D noise: {@code xSize * ySize * zSize} values, x outermost then z then y. */
    public double[] generateNoiseOctaves(double[] out, int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize,
            double xScale, double yScale, double zScale) {
        if (out == null) out = new double[xSize * ySize * zSize];
        else java.util.Arrays.fill(out, 0, Math.min(out.length, xSize * ySize * zSize), 0.0D);
        double frequency = 1.0D;
        for (int o = 0; o < octaves; o++) {
            double x = xOffset * frequency * xScale;
            double y = yOffset * frequency * yScale;
            double z = zOffset * frequency * zScale;
            long xl = floorLong(x), zl = floorLong(z);
            x -= xl;
            z -= zl;
            xl %= 16777216L;
            zl %= 16777216L;
            x += xl;
            z += zl;
            generators[o].populateNoiseArray(out, x, y, z, xSize, ySize, zSize, xScale * frequency, yScale * frequency, zScale * frequency,
                    frequency);
            frequency /= 2.0D;
        }
        return out;
    }

    /** 2D noise: {@code xSize * zSize} values. */
    public double[] generateNoiseOctaves(double[] out, int xOffset, int zOffset, int xSize, int zSize, double xScale, double zScale,
            double unused) {
        return generateNoiseOctaves(out, xOffset, 10, zOffset, xSize, 1, zSize, xScale, 1.0D, zScale);
    }

    private static long floorLong(double d) {
        long l = (long) d;
        return d < l ? l - 1L : l;
    }
}
