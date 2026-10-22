package legacyconnector.v1_7_10.net.minecraft.util;

import java.util.Random;

public class MathHelper {
    public static final float[] SIN_TABLE = new float[65536];

    static {
        for (int i = 0; i < 65536; ++i) SIN_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0D / 65536.0D);
    }

    public static float sin(float v) {
        return SIN_TABLE[(int) (v * 10430.378F) & 65535];
    }

    public static float cos(float v) {
        return SIN_TABLE[(int) (v * 10430.378F + 16384.0F) & 65535];
    }

    public static float sqrt_float(float v) {
        return (float) Math.sqrt(v);
    }

    public static float sqrt_double(double v) {
        return (float) Math.sqrt(v);
    }

    public static int floor_float(float v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    public static int truncateDoubleToInt(double v) {
        return (int) (v + 1024.0D) - 1024;
    }

    public static int floor_double(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    public static long floor_double_long(double v) {
        long i = (long) v;
        return v < i ? i - 1L : i;
    }

    public static int func_154353_e(double v) {
        return (int) (v >= 0.0D ? v : -v + 1.0D);
    }

    public static float abs(float v) {
        return v >= 0.0F ? v : -v;
    }

    public static int abs_int(int v) {
        return v >= 0 ? v : -v;
    }

    public static int ceiling_float_int(float v) {
        int i = (int) v;
        return v > i ? i + 1 : i;
    }

    public static int ceiling_double_int(double v) {
        int i = (int) v;
        return v > i ? i + 1 : i;
    }

    public static int clamp_int(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static float clamp_float(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static double clamp_double(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static double denormalizeClamp(double a, double b, double t) {
        return t < 0.0D ? a : (t > 1.0D ? b : a + (b - a) * t);
    }

    public static double abs_max(double a, double b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }

    public static int bucketInt(int v, int bucket) {
        return v < 0 ? -((-v - 1) / bucket) - 1 : v / bucket;
    }

    public static boolean stringNullOrLengthZero(String s) {
        return s == null || s.length() == 0;
    }

    public static int getRandomIntegerInRange(Random r, int min, int max) {
        return min >= max ? min : r.nextInt(max - min + 1) + min;
    }

    public static float randomFloatClamp(Random r, float min, float max) {
        return min >= max ? min : r.nextFloat() * (max - min) + min;
    }

    public static double getRandomDoubleInRange(Random r, double min, double max) {
        return min >= max ? min : r.nextDouble() * (max - min) + min;
    }

    public static double average(long[] values) {
        long sum = 0L;
        for (long l : values) sum += l;
        return (double) sum / (double) values.length;
    }

    public static float wrapAngleTo180_float(float v) {
        v %= 360.0F;
        if (v >= 180.0F) v -= 360.0F;
        if (v < -180.0F) v += 360.0F;
        return v;
    }

    public static double wrapAngleTo180_double(double v) {
        v %= 360.0D;
        if (v >= 180.0D) v -= 360.0D;
        if (v < -180.0D) v += 360.0D;
        return v;
    }

    public static int parseIntWithDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable t) {
            return def;
        }
    }

    public static int parseIntWithDefaultAndMax(String s, int def, int max) {
        return Math.max(max, parseIntWithDefault(s, def));
    }

    public static double parseDoubleWithDefault(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (Throwable t) {
            return def;
        }
    }

    public static double parseDoubleWithDefaultAndMax(String s, double def, double max) {
        return Math.max(max, parseDoubleWithDefault(s, def));
    }

    public static int roundUpToPowerOfTwo(int v) {
        int j = v - 1;
        j |= j >> 1;
        j |= j >> 2;
        j |= j >> 4;
        j |= j >> 8;
        j |= j >> 16;
        return j + 1;
    }

    public static boolean isPowerOfTwo(int v) {
        return v != 0 && (v & v - 1) == 0;
    }

    public static int calculateLogBaseTwo(int v) {
        return 31 - Integer.numberOfLeadingZeros(roundUpToPowerOfTwo(v));
    }

    public static int func_154354_b(int a, int b) {
        if (b == 0) return 0;
        if (a < 0) b *= -1;
        int r = a % b;
        return r == 0 ? a : a + b - r;
    }
}
