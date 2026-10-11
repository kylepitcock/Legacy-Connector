package mccompat.v1_7_10.net.minecraft.world.gen;

import java.util.Random;

/** 2D/3D simplex noise as in vanilla 1.7.10 (used by biome and terrain generators). */
public class NoiseGeneratorSimplex {
    private static final int[][] GRAD = {{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};
    public static final double field_151614_a = Math.sqrt(3.0D);
    private static final double F2 = 0.5D * (field_151614_a - 1.0D);
    private static final double G2 = (3.0D - field_151614_a) / 6.0D;
    private final int[] perm = new int[512];
    public double xo;
    public double yo;
    public double zo;

    public NoiseGeneratorSimplex() {
        this(new Random());
    }

    public NoiseGeneratorSimplex(Random random) {
        xo = random.nextDouble() * 256.0D;
        yo = random.nextDouble() * 256.0D;
        zo = random.nextDouble() * 256.0D;
        for (int i = 0; i < 256; perm[i] = i++) {
            // identity
        }
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i) + i;
            int k = perm[i];
            perm[i] = perm[j];
            perm[j] = k;
            perm[i + 256] = perm[i];
        }
    }

    private static int floor(double d) {
        return d > 0.0D ? (int) d : (int) d - 1;
    }

    private static double dot(int[] g, double x, double y) {
        return g[0] * x + g[1] * y;
    }

    /** func_151605_a: 2D noise at (x, y). */
    public double func_151605_a(double xin, double yin) {
        double s = (xin + yin) * F2;
        int i = floor(xin + s), j = floor(yin + s);
        double t = (i + j) * G2;
        double x0 = xin - (i - t), y0 = yin - (j - t);
        int i1 = x0 > y0 ? 1 : 0, j1 = x0 > y0 ? 0 : 1;
        double x1 = x0 - i1 + G2, y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0D + 2.0D * G2, y2 = y0 - 1.0D + 2.0D * G2;
        int ii = i & 255, jj = j & 255;
        int g0 = perm[ii + perm[jj]] % 12, g1 = perm[ii + i1 + perm[jj + j1]] % 12, g2 = perm[ii + 1 + perm[jj + 1]] % 12;
        double n0 = 0.0D, n1 = 0.0D, n2 = 0.0D;
        double t0 = 0.5D - x0 * x0 - y0 * y0;
        if (t0 >= 0.0D) {
            t0 *= t0;
            n0 = t0 * t0 * dot(GRAD[g0], x0, y0);
        }
        double t1 = 0.5D - x1 * x1 - y1 * y1;
        if (t1 >= 0.0D) {
            t1 *= t1;
            n1 = t1 * t1 * dot(GRAD[g1], x1, y1);
        }
        double t2 = 0.5D - x2 * x2 - y2 * y2;
        if (t2 >= 0.0D) {
            t2 *= t2;
            n2 = t2 * t2 * dot(GRAD[g2], x2, y2);
        }
        return 70.0D * (n0 + n1 + n2);
    }
}
