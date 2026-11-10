package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Random;
import legacyconnector.vb1_7_3.net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import legacyconnector.vb1_7_3.net.minecraft.util.math.noise.PerlinNoiseSampler;
import org.junit.jupiter.api.Test;

class NoiseB173Test {
    // Numeric fixtures captured from the named vanilla b1.7.3 classes in an isolated JShell.
    // Tests need neither a Minecraft jar nor any original mod fixture.
    @Test void permutationSeedingAndScalarSamplingMatchBeta() {
        var perlin = new PerlinNoiseSampler(new Random(0));
        assertEquals(187.1277535684242D, perlin.offsetX);
        assertEquals(61.57732241190038D, perlin.offsetY);
        assertEquals(163.1788608896277D, perlin.offsetZ);
        assertEquals(-0.02541810201094486D,
                new PerlinNoiseSampler(new Random(42)).sample(-123.25D, 17.125D, -9.75D), 1E-14D);
        assertEquals(1.842059942229212D,
                new OctavePerlinNoiseSampler(new Random(42), 4).sample(-123.25D, 17.125D), 1E-14D);
        var random = new Random(42);
        new OctavePerlinNoiseSampler(random, 4);
        assertEquals(-3992957075691771907L, random.nextLong());
    }

    @Test void fractionalVolumeMatchesBetaIncludingCachedYGradientsAndArrayOrder() {
        var noise = new OctavePerlinNoiseSampler(new Random(42), 4);
        double[] expected = {1.9929501287890967D, 1.338889820491556D, 0.9752213839701767D,
                2.357030474059011D, 2.1358446452572153D, 1.8547603330502178D,
                2.0039974427275427D, 1.259131650678705D, 0.8409192690173841D,
                2.4228865811705695D, 2.1012681478845323D, 1.761487408267541D};
        assertArrayEquals(expected, noise.create(null, -17.25D, 2.125D, 31.75D, 2, 3, 2, .125D, .25D, .5D), 1E-14D);
        var reuse = new double[15];
        Arrays.fill(reuse, 99.0D);
        assertSame(reuse, noise.create(reuse, -17.25D, 2.125D, 31.75D, 2, 3, 2, .125D, .25D, .5D));
        assertArrayEquals(expected, Arrays.copyOf(reuse, expected.length), 1E-14D);
        assertArrayEquals(new double[3], Arrays.copyOfRange(reuse, 12, 15));
    }

    @Test void flatFastPathMatchesBetaAndIgnoresLastArgument() {
        double[] expected = {1.7017429840447473D, 1.1449791487817944D, 0.3913914569740835D,
                1.6733780404779042D, 1.2680175854550026D, 0.6372813636115809D};
        var noise = new OctavePerlinNoiseSampler(new Random(-7), 3);
        assertArrayEquals(expected, noise.create(null, -17, 31, 2, 3, .125D, .5D, 123.0D), 1E-14D);
        assertArrayEquals(expected, noise.create(null, -17, 31, 2, 3, .125D, .5D, -99.0D), 1E-14D);
    }
}
