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

    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    public static double fastInvSqrt(double v) {
        double d = 0.5D * v;
        long i = Double.doubleToRawLongBits(v);
        i = 6910469410427058090L - (i >> 1);
        v = Double.longBitsToDouble(i);
        return v * (1.5D - d * v * v);
    }

    public static double frac(double number) {
        return number - (double) lfloor(number);
    }

    public static long getCoordinateRandom(int x, int y, int z) {
        long i = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y;
        i = i * i * 42317861L + i * 11L;
        return i;
    }

    public static long getPositionRandom(Vec3i pos) {
        return getCoordinateRandom(pos.getX(), pos.getY(), pos.getZ());
    }

    public static int getInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    public static int getInt(String value, int defaultValue, int max) {
        return Math.max(max, getInt(value, defaultValue));
    }

    public static java.util.UUID getRandomUUID(Random rand) {
        long i = rand.nextLong() & -61441L | 16384L;
        long j = rand.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
        return new java.util.UUID(i, j);
    }

    public static java.util.UUID getRandomUUID() {
        return getRandomUUID(new Random());
    }

    public static int hsvToRGB(float hue, float saturation, float value) {
        int i = (int) (hue * 6.0F) % 6;
        float f = hue * 6.0F - (float) i;
        float f1 = value * (1.0F - saturation);
        float f2 = value * (1.0F - f * saturation);
        float f3 = value * (1.0F - (1.0F - f) * saturation);
        float r, g, b;
        switch (i) {
            case 0 -> { r = value; g = f3; b = f1; }
            case 1 -> { r = f2; g = value; b = f1; }
            case 2 -> { r = f1; g = value; b = f3; }
            case 3 -> { r = f1; g = f2; b = value; }
            case 4 -> { r = f3; g = f1; b = value; }
            case 5 -> { r = value; g = f1; b = f2; }
            default -> throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
        }
        return clamp((int) (r * 255.0F), 0, 255) << 16 | clamp((int) (g * 255.0F), 0, 255) << 8 | clamp((int) (b * 255.0F), 0, 255);
    }

    public static int intFloorDiv(int x, int y) {
        return x < 0 ? -((-x - 1) / y) - 1 : x / y;
    }

    private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[] {0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13,
            23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9};

    public static int smallestEncompassingPowerOfTwo(int value) {
        int i = value - 1;
        i = i | i >> 1;
        i = i | i >> 2;
        i = i | i >> 4;
        i = i | i >> 8;
        i = i | i >> 16;
        return i + 1;
    }

    private static boolean isPowerOfTwo(int value) {
        return value != 0 && (value & value - 1) == 0;
    }

    public static int log2DeBruijn(int value) {
        value = isPowerOfTwo(value) ? value : smallestEncompassingPowerOfTwo(value);
        return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int) ((long) value * 125613361L >> 27) & 31];
    }

    public static int log2(int value) {
        return log2DeBruijn(value) - (isPowerOfTwo(value) ? 0 : 1);
    }

    public static double nextDouble(Random random, double minimum, double maximum) {
        return minimum >= maximum ? minimum : random.nextDouble() * (maximum - minimum) + minimum;
    }

    public static float nextFloat(Random random, float minimum, float maximum) {
        return minimum >= maximum ? minimum : random.nextFloat() * (maximum - minimum) + minimum;
    }

    public static double positiveModulo(double numerator, double denominator) {
        return (numerator % denominator + denominator) % denominator;
    }

    public static float positiveModulo(float numerator, float denominator) {
        return (numerator % denominator + denominator) % denominator;
    }

    public static int rgb(float rIn, float gIn, float bIn) {
        return rgb(floor(rIn * 255.0F), floor(gIn * 255.0F), floor(bIn * 255.0F));
    }

    public static int rgb(int rIn, int gIn, int bIn) {
        int lvt_3_1_ = (rIn << 8) + gIn;
        return (lvt_3_1_ << 8) + bIn;
    }

    public static int wrapDegrees(int angle) {
        angle = angle % 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    // ---- 1.12.2 MathHelper, area F additions

    public static int absFloor(double value) {
        return (int) (value >= 0.0D ? value : -value + 1.0D);
    }

    public static double getDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    public static double getDouble(String value, double defaultValue, double max) {
        return Math.max(max, getDouble(value, defaultValue));
    }

    public static int multiplyColor(int colorA, int colorB) {
        int i = (colorA & 16711680) >> 16;
        int j = (colorB & 16711680) >> 16;
        int k = (colorA & 65280) >> 8;
        int l = (colorB & 65280) >> 8;
        int i1 = colorA & 255;
        int j1 = colorB & 255;
        int k1 = (int) ((float) i * (float) j / 255.0F);
        int l1 = (int) ((float) k * (float) l / 255.0F);
        int i2 = (int) ((float) i1 * (float) j1 / 255.0F);
        return colorA & -16777216 | k1 << 16 | l1 << 8 | i2;
    }

    public static double pct(double value, double min, double max) {
        return (value - min) / (max - min);
    }
}
