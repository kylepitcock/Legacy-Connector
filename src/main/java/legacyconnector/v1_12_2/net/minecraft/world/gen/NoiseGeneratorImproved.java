package legacyconnector.v1_12_2.net.minecraft.world.gen;

import java.util.Random;

/** Improved Perlin noise exactly as 1.12.2 computed it (terrain generators depend on its precise values). */
public class NoiseGeneratorImproved extends NoiseGenerator {
    private final int[] permutations = new int[512];
    public double xCoord;
    public double yCoord;
    public double zCoord;
    private static final double[] GX = {1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, -1.0D, 0.0D};
    private static final double[] GY = {1.0D, 1.0D, -1.0D, -1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D};
    private static final double[] GZ = {0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, -1.0D, -1.0D, 1.0D, 1.0D, -1.0D, -1.0D, 0.0D, 1.0D, 0.0D, -1.0D};
    private static final double[] G2X = {1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, -1.0D, 0.0D};
    private static final double[] G2Z = {0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, -1.0D, -1.0D, 1.0D, 1.0D, -1.0D, -1.0D, 0.0D, 1.0D, 0.0D, -1.0D};

    public NoiseGeneratorImproved() {
        this(new Random());
    }

    public NoiseGeneratorImproved(Random random) {
        this.xCoord = random.nextDouble() * 256.0D;
        this.yCoord = random.nextDouble() * 256.0D;
        this.zCoord = random.nextDouble() * 256.0D;
        for (int i = 0; i < 256; i++) permutations[i] = i;
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i) + i;
            int k = permutations[i];
            permutations[i] = permutations[j];
            permutations[j] = k;
            permutations[i + 256] = permutations[i];
        }
    }

    public static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /** grad2: 2D gradient. */
    public static double grad2(int hash, double x, double z) {
        int j = hash & 15;
        return G2X[j] * x + G2Z[j] * z;
    }

    public static double grad(int hash, double x, double y, double z) {
        int j = hash & 15;
        return GX[j] * x + GY[j] * y + GZ[j] * z;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }

    /** Adds this octave's noise (scaled by 1 / noiseScale) to {@code out} for the given block of samples. */
    public void populateNoiseArray(double[] out, double xOffset, double yOffset, double zOffset, int xSize, int ySize, int zSize,
            double xScale, double yScale, double zScale, double noiseScale) {
        int[] p = permutations;
        double inv = 1.0D / noiseScale;
        int index = 0;
        if (ySize == 1) {
            for (int xi = 0; xi < xSize; xi++) {
                double x = xOffset + xi * xScale + xCoord;
                int xf = floor(x);
                int xc = xf & 255;
                x -= xf;
                double fx = fade(x);
                for (int zi = 0; zi < zSize; zi++) {
                    double z = zOffset + zi * zScale + zCoord;
                    int zf = floor(z);
                    int zc = zf & 255;
                    z -= zf;
                    double fz = fade(z);
                    int a = p[xc];
                    int aa = p[a] + zc;
                    int b = p[xc + 1];
                    int ba = p[b] + zc;
                    double d1 = lerp(fx, grad2(p[aa], x, z), grad(p[ba], x - 1.0D, 0.0D, z));
                    double d2 = lerp(fx, grad(p[aa + 1], x, 0.0D, z - 1.0D), grad(p[ba + 1], x - 1.0D, 0.0D, z - 1.0D));
                    out[index++] += lerp(fz, d1, d2) * inv;
                }
            }
            return;
        }
        int lastY = -1;
        double d17 = 0, d18 = 0, d19 = 0, d20 = 0;
        for (int xi = 0; xi < xSize; xi++) {
            double x = xOffset + xi * xScale + xCoord;
            int xf = floor(x);
            int xc = xf & 255;
            x -= xf;
            double fx = fade(x);
            for (int zi = 0; zi < zSize; zi++) {
                double z = zOffset + zi * zScale + zCoord;
                int zf = floor(z);
                int zc = zf & 255;
                z -= zf;
                double fz = fade(z);
                for (int yi = 0; yi < ySize; yi++) {
                    double y = yOffset + yi * yScale + yCoord;
                    int yf = floor(y);
                    int yc = yf & 255;
                    y -= yf;
                    double fy = fade(y);
                    if (yi == 0 || yc != lastY) {
                        lastY = yc;
                        int a = p[xc] + yc;
                        int aa = p[a] + zc;
                        int ab = p[a + 1] + zc;
                        int b = p[xc + 1] + yc;
                        int ba = p[b] + zc;
                        int bb = p[b + 1] + zc;
                        d17 = lerp(fx, grad(p[aa], x, y, z), grad(p[ba], x - 1.0D, y, z));
                        d18 = lerp(fx, grad(p[ab], x, y - 1.0D, z), grad(p[bb], x - 1.0D, y - 1.0D, z));
                        d19 = lerp(fx, grad(p[aa + 1], x, y, z - 1.0D), grad(p[ba + 1], x - 1.0D, y, z - 1.0D));
                        d20 = lerp(fx, grad(p[ab + 1], x, y - 1.0D, z - 1.0D), grad(p[bb + 1], x - 1.0D, y - 1.0D, z - 1.0D));
                    }
                    double d21 = lerp(fy, d17, d18);
                    double d22 = lerp(fy, d19, d20);
                    out[index++] += lerp(fz, d21, d22) * inv;
                }
            }
        }
    }
}
