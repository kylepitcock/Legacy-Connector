package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.Random;

/** Octaves of simplex noise (vanilla's "Perlin" generator: biome temperature, grass colour, surface depth). */
public class NoiseGeneratorPerlin extends NoiseGenerator {
    private final NoiseGeneratorSimplex[] noiseLevels;
    private final int levels;

    public NoiseGeneratorPerlin(Random random, int levels) {
        this.levels = levels;
        this.noiseLevels = new NoiseGeneratorSimplex[levels];
        for (int i = 0; i < levels; i++) noiseLevels[i] = new NoiseGeneratorSimplex(random);
    }

    public double getValue(double x, double z) {
        double sum = 0.0D, scale = 1.0D;
        for (int i = 0; i < levels; i++) {
            sum += noiseLevels[i].getValue(x * scale, z * scale) / scale;
            scale /= 2.0D;
        }
        return sum;
    }

    public double[] getRegion(double[] buffer, double x, double z, int xSize, int zSize, double xScale, double zScale, double frequency) {
        return getRegion(buffer, x, z, xSize, zSize, xScale, zScale, frequency, 0.5D);
    }

    public double[] getRegion(double[] buffer, double x, double z, int xSize, int zSize, double xScale, double zScale, double frequency,
            double persistence) {
        if (buffer != null && buffer.length >= xSize * zSize) {
            java.util.Arrays.fill(buffer, 0.0D);
        } else {
            buffer = new double[xSize * zSize];
        }
        double d1 = 1.0D, d0 = 1.0D;
        for (int j = 0; j < levels; j++) {
            noiseLevels[j].add(buffer, x, z, xSize, zSize, xScale * d0 * d1, zScale * d0 * d1, 0.55D / d1);
            d0 *= frequency;
            d1 *= persistence;
        }
        return buffer;
    }
}
