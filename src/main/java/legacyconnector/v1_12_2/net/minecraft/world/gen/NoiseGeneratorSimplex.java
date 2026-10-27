package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.Random;

/** 2D simplex noise as in vanilla 1.12.2 (biome temperature, grass colour and surface noise). */
public class NoiseGeneratorSimplex {
    private static final int[][] grad3 = {{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};
    public static final double SQRT_3 = Math.sqrt(3.0D);
    private static final double F2 = 0.5D * (SQRT_3 - 1.0D);
    private static final double G2 = (3.0D - SQRT_3) / 6.0D;
    private final int[] p = new int[512];
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
        for (int i = 0; i < 256; p[i] = i++) {
            // identity
        }
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i) + i;
            int k = p[i];
            p[i] = p[j];
            p[j] = k;
            p[i + 256] = p[i];
        }
    }

    private static int fastFloor(double d) {
        return d > 0.0D ? (int) d : (int) d - 1;
    }

    private static double dot(int[] g, double x, double y) {
        return g[0] * x + g[1] * y;
    }

    public double getValue(double xin, double yin) {
        double s = (xin + yin) * F2;
        int i = fastFloor(xin + s), j = fastFloor(yin + s);
        double t = (i + j) * G2;
        double x0 = xin - (i - t), y0 = yin - (j - t);
        int i1 = x0 > y0 ? 1 : 0, j1 = x0 > y0 ? 0 : 1;
        double x1 = x0 - i1 + G2, y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0D + 2.0D * G2, y2 = y0 - 1.0D + 2.0D * G2;
        int ii = i & 255, jj = j & 255;
        int g0 = p[ii + p[jj]] % 12, g1 = p[ii + i1 + p[jj + j1]] % 12, g2 = p[ii + 1 + p[jj + 1]] % 12;
        return 70.0D * (corner(g0, x0, y0) + corner(g1, x1, y1) + corner(g2, x2, y2));
    }

    private static double corner(int g, double x, double y) {
        double t = 0.5D - x * x - y * y;
        if (t < 0.0D) return 0.0D;
        t *= t;
        return t * t * dot(grad3[g], x, y);
    }

    /** Adds {@code xSize * zSize} samples (z outermost) scaled by {@code amplitude} to the buffer. */
    public void add(double[] buffer, double x, double z, int xSize, int zSize, double xScale, double zScale, double amplitude) {
        int index = 0;
        for (int j = 0; j < zSize; j++) {
            double zin = (z + j) * zScale + yo;
            for (int k = 0; k < xSize; k++) {
                double xin = (x + k) * xScale + xo;
                double s = (xin + zin) * F2;
                int l = fastFloor(xin + s), i1 = fastFloor(zin + s);
                double t = (l + i1) * G2;
                double x0 = xin - (l - t), y0 = zin - (i1 - t);
                int j1 = x0 > y0 ? 1 : 0, k1 = x0 > y0 ? 0 : 1;
                double x1 = x0 - j1 + G2, y1 = y0 - k1 + G2;
                double x2 = x0 - 1.0D + 2.0D * G2, y2 = y0 - 1.0D + 2.0D * G2;
                int l1 = l & 255, i2 = i1 & 255;
                int g0 = p[l1 + p[i2]] % 12, g1 = p[l1 + j1 + p[i2 + k1]] % 12, g2 = p[l1 + 1 + p[i2 + 1]] % 12;
                buffer[index++] += 70.0D * (corner(g0, x0, y0) + corner(g1, x1, y1) + corner(g2, x2, y2)) * amplitude;
            }
        }
    }
}
