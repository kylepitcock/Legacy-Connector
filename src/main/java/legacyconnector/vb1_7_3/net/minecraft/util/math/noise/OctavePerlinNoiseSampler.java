package legacyconnector.vb1_7_3.net.minecraft.util.math.noise;

import java.util.Arrays;
import java.util.Random;

/** Beta octave composition, including fractional origins and increasing octave amplitudes. */
public class OctavePerlinNoiseSampler extends NoiseSampler {
    private final PerlinNoiseSampler[] octaveSamplers;
    private final int octaves;

    public OctavePerlinNoiseSampler(Random random, int octaves) {
        this.octaves = octaves;
        octaveSamplers = new PerlinNoiseSampler[octaves];
        for (int i = 0; i < octaves; i++) octaveSamplers[i] = new PerlinNoiseSampler(random);
    }

    public double sample(double x, double y) {
        double sum = 0.0D, frequency = 1.0D;
        for (int i = 0; i < octaves; i++) {
            sum += octaveSamplers[i].sample(x * frequency, y * frequency) / frequency;
            frequency /= 2.0D;
        }
        return sum;
    }

    public double[] create(double[] map, double x, double y, double z, int width, int height, int depth,
            double xScale, double yScale, double zScale) {
        if (map == null) map = new double[width * height * depth];
        else Arrays.fill(map, 0.0D);
        double frequency = 1.0D;
        for (int i = 0; i < octaves; i++) {
            octaveSamplers[i].create(map, x, y, z, width, height, depth,
                    xScale * frequency, yScale * frequency, zScale * frequency, frequency);
            frequency /= 2.0D;
        }
        return map;
    }

    public double[] create(double[] map, int x, int z, int width, int depth,
            double xScale, double zScale, double unused) {
        return create(map, (double) x, 10.0D, (double) z, width, 1, depth, xScale, 1.0D, zScale);
    }
}
