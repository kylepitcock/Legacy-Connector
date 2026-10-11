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

    public double func_151600_a(double[] out, double x, double z, int sizeX, int sizeZ, double scaleX, double scaleZ, double amplitude) {
        return func_151599_a(out, x, z, sizeX, sizeZ, scaleX, scaleZ, amplitude, 0.5D);
    }

    public double func_151599_a(double[] out, double x, double z, int sizeX, int sizeZ, double scaleX, double scaleZ, double amplitude,
            double persistence) {
        int index = 0;
        for (int dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                double v = 0.0D, freq = 1.0D, amp = 1.0D;
                for (int i = 0; i < levels; i++) {
                    v += octaves[i].func_151605_a((x + dx) * scaleX * freq + octaves[i].xo, (z + dz) * scaleZ * freq + octaves[i].yo) * amp;
                    freq *= amplitude;
                    amp *= persistence;
                }
                if (index < out.length) out[index++] = v;
            }
        }
        return 0.0D;
    }
}
