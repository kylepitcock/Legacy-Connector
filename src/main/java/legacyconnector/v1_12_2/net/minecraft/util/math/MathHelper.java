package legacyconnector.v1_12_2.net.minecraft.util.math;

import java.util.Random;

public class MathHelper {
    public static final float SQRT_2 = sqrt(2.0F);

    public static float sin(float v) {
        return (float) Math.sin(v);
    }

    public static float cos(float v) {
        return (float) Math.cos(v);
    }

    public static float sqrt(float v) {
        return (float) Math.sqrt(v);
    }

    public static float sqrt(double v) {
        return (float) Math.sqrt(v);
    }

    public static int floor(float v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    public static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    public static long lfloor(double v) {
        long l = (long) v;
        return v < l ? l - 1L : l;
    }

    public static int fastFloor(double v) {
        return (int) (v + 1024.0D) - 1024;
    }

    public static float abs(float v) {
        return Math.abs(v);
    }

    public static int abs(int v) {
        return Math.abs(v);
    }

    public static int ceil(float v) {
        int i = (int) v;
        return v > i ? i + 1 : i;
    }

    public static int ceil(double v) {
        int i = (int) v;
        return v > i ? i + 1 : i;
    }

    public static int clamp(int num, int min, int max) {
        return num < min ? min : Math.min(num, max);
    }

    public static float clamp(float num, float min, float max) {
        return num < min ? min : Math.min(num, max);
    }

    public static double clamp(double num, double min, double max) {
        return num < min ? min : Math.min(num, max);
    }

    public static double clampedLerp(double lowerBnd, double upperBnd, double slide) {
        return slide < 0.0D ? lowerBnd : slide > 1.0D ? upperBnd : lowerBnd + (upperBnd - lowerBnd) * slide;
    }

    public static double absMax(double a, double b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }

    public static int getInt(Random random, int minimum, int maximum) {
        return minimum >= maximum ? minimum : random.nextInt(maximum - minimum + 1) + minimum;
    }

    public static float wrapDegrees(float value) {
        value = value % 360.0F;
        if (value >= 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }

    public static double wrapDegrees(double value) {
        value = value % 360.0D;
        if (value >= 180.0D) value -= 360.0D;
        if (value < -180.0D) value += 360.0D;
        return value;
    }

    public static int hash(int x) {
        x = x ^ x >>> 16;
        x = x * -2048144789;
        x = x ^ x >>> 13;
        x = x * -1028477387;
        return x ^ x >>> 16;
    }

    public static int roundUp(int number, int interval) {
        if (interval == 0) return 0;
        if (number == 0) return interval;
        if (number < 0) interval *= -1;
        int r = number % interval;
        return r == 0 ? number : number + interval - r;
    }

    public static double average(long[] values) {
        long sum = 0L;
        for (long v : values) sum += v;
        return (double) sum / (double) values.length;
    }

    public static boolean epsilonEquals(float x, float y) {
        return abs(y - x) < 1.0E-5F;
    }
}
