package mccompat.v1_7_10.net.minecraft.world.gen;

import java.util.Random;

/** Octaves of simplex noise (vanilla's "Perlin" generator, used by biome terrain and swamp colouring). */
public class NoiseGeneratorPerlin {
    private final NoiseGeneratorSimplex[] octaves;
    private final int levels;

    public NoiseGeneratorPerlin(Random random, int levels) {
        this.levels = levels;
        this.octaves = new NoiseGeneratorSimplex[levels];
        for (int i = 0; i < levels; i++) octaves[i] = new NoiseGeneratorSimplex(random);
    }

    /** func_151601_a: noise at (x, z). */
    public double func_151601_a(double x, double z) {
        double sum = 0.0D, scale = 1.0D;
        for (int i = 0; i < levels; i++) {
            sum += octaves[i].func_151605_a(x * scale, z * scale) / scale;
            scale /= 2.0D;
        }
        return sum;
    }

    /** Fills {@code sizeX * sizeZ} values (x outermost); the 1.7.10 default persistence is 0.5. */
    public double[] func_151599_a(double[] out, double x, double z, int sizeX, int sizeZ, double scaleX, double scaleZ, double frequency) {
        return func_151600_a(out, x, z, sizeX, sizeZ, scaleX, scaleZ, frequency, 0.5D);
    }

    public double[] func_151600_a(double[] out, double x, double z, int sizeX, int sizeZ, double scaleX, double scaleZ, double frequency,
            double persistence) {
        int n = sizeX * sizeZ;
        if (out == null || out.length < n) out = new double[n];
        else java.util.Arrays.fill(out, 0, n, 0.0D);
        double freq = 1.0D, amp = 1.0D;
        for (int i = 0; i < levels; i++) {
            int index = 0;
            for (int dx = 0; dx < sizeX; dx++) {
                for (int dz = 0; dz < sizeZ; dz++) {
                    out[index++] += octaves[i].func_151605_a((x + dx) * scaleX * freq + octaves[i].xo, (z + dz) * scaleZ * freq + octaves[i].yo)
                            * 0.55D * amp;
                }
            }
            freq *= frequency;
            amp *= persistence;
        }
        return out;
    }
}
