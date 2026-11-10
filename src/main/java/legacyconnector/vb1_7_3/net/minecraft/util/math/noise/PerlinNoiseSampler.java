package legacyconnector.vb1_7_3.net.minecraft.util.math.noise;

import java.util.Random;

/** Beta Perlin noise. Array coordinates are scaled after adding the sample index; the height-one path and cached Y gradients are intentional vanilla behavior. */
public class PerlinNoiseSampler extends NoiseSampler {
    private final int[] perm = new int[512];
    public double offsetX;
    public double offsetY;
    public double offsetZ;
    public PerlinNoiseSampler() {
        this(new Random());
    }

    public PerlinNoiseSampler(Random random) {
        this.offsetX = random.nextDouble() * 256.0D;
        this.offsetY = random.nextDouble() * 256.0D;
        this.offsetZ = random.nextDouble() * 256.0D;
        for (int i = 0; i < 256; i++) perm[i] = i;
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i) + i;
            int k = perm[i];
            perm[i] = perm[j];
            perm[j] = k;
            perm[i + 256] = perm[i];
        }
    }

    public final double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    public final double gradCoord(int hash, double x, double z) {
        int h = hash & 15;
        double u = (1 - ((h & 8) >> 3)) * x;
        double v = h < 4 ? 0.0D : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public final double gradCoord(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public double sample(double x, double y) {
        return sample(x, y, 0.0D);
    }

    public double sample(double x, double y, double z) {
        x += offsetX;
        y += offsetY;
        z += offsetZ;
        int ix = floor(x), iy = floor(y), iz = floor(z);
        int xc = ix & 255, yc = iy & 255, zc = iz & 255;
        x -= ix;
        y -= iy;
        z -= iz;
        double fx = fade(x), fy = fade(y), fz = fade(z);
        int a = perm[xc] + yc, aa = perm[a] + zc, ab = perm[a + 1] + zc;
        int b = perm[xc + 1] + yc, ba = perm[b] + zc, bb = perm[b + 1] + zc;
        return lerp(fz,
                lerp(fy, lerp(fx, gradCoord(perm[aa], x, y, z), gradCoord(perm[ba], x - 1, y, z)),
                        lerp(fx, gradCoord(perm[ab], x, y - 1, z), gradCoord(perm[bb], x - 1, y - 1, z))),
                lerp(fy, lerp(fx, gradCoord(perm[aa + 1], x, y, z - 1), gradCoord(perm[ba + 1], x - 1, y, z - 1)),
                        lerp(fx, gradCoord(perm[ab + 1], x, y - 1, z - 1), gradCoord(perm[bb + 1], x - 1, y - 1, z - 1))));
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }

    /** Adds this octave's noise (scaled by 1 / noiseScale) to {@code out} for the given block of samples. */
    public void create(double[] out, double xOffset, double yOffset, double zOffset, int xSize, int ySize, int zSize,
            double xScale, double yScale, double zScale, double noiseScale) {
        int[] p = perm;
        double inv = 1.0D / noiseScale;
        int index = 0;
        if (ySize == 1) {
            for (int xi = 0; xi < xSize; xi++) {
                double x = (xOffset + xi) * xScale + offsetX;
                int xf = floor(x);
                int xc = xf & 255;
                x -= xf;
                double fx = fade(x);
                for (int zi = 0; zi < zSize; zi++) {
                    double z = (zOffset + zi) * zScale + offsetZ;
                    int zf = floor(z);
                    int zc = zf & 255;
                    z -= zf;
                    double fz = fade(z);
                    int a = p[xc];
                    int aa = p[a] + zc;
                    int b = p[xc + 1];
                    int ba = p[b] + zc;
                    double d1 = lerp(fx, gradCoord(p[aa], x, z), gradCoord(p[ba], x - 1.0D, 0.0D, z));
                    double d2 = lerp(fx, gradCoord(p[aa + 1], x, 0.0D, z - 1.0D), gradCoord(p[ba + 1], x - 1.0D, 0.0D, z - 1.0D));
                    out[index++] += lerp(fz, d1, d2) * inv;
                }
            }
            return;
        }
        int lastY = -1;
        double d17 = 0, d18 = 0, d19 = 0, d20 = 0;
        for (int xi = 0; xi < xSize; xi++) {
            double x = (xOffset + xi) * xScale + offsetX;
            int xf = floor(x);
            int xc = xf & 255;
            x -= xf;
            double fx = fade(x);
            for (int zi = 0; zi < zSize; zi++) {
                double z = (zOffset + zi) * zScale + offsetZ;
                int zf = floor(z);
                int zc = zf & 255;
                z -= zf;
                double fz = fade(z);
                for (int yi = 0; yi < ySize; yi++) {
                    double y = (yOffset + yi) * yScale + offsetY;
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
                        d17 = lerp(fx, gradCoord(p[aa], x, y, z), gradCoord(p[ba], x - 1.0D, y, z));
                        d18 = lerp(fx, gradCoord(p[ab], x, y - 1.0D, z), gradCoord(p[bb], x - 1.0D, y - 1.0D, z));
                        d19 = lerp(fx, gradCoord(p[aa + 1], x, y, z - 1.0D), gradCoord(p[ba + 1], x - 1.0D, y, z - 1.0D));
                        d20 = lerp(fx, gradCoord(p[ab + 1], x, y - 1.0D, z - 1.0D), gradCoord(p[bb + 1], x - 1.0D, y - 1.0D, z - 1.0D));
                    }
                    double d21 = lerp(fy, d17, d18);
                    double d22 = lerp(fy, d19, d20);
                    out[index++] += lerp(fz, d21, d22) * inv;
                }
            }
        }
    }
}
