package legacyconnector.v1_7_10.codechicken.lib.math;

/** CodeChickenLib's math helpers (the shared constants and clamps its geometry code uses). */
public class MathHelper {
    public static final double phi = 1.618033988749894848204586834365D;
    public static final double pi = Math.PI;
    public static final double todeg = 57.29577951308232D;
    public static final double torad = 0.017453292519943D;
    public static final double sqrt2 = 1.414213562373095D;

    public static int floor_double(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }

    public static int floor_float(float f) {
        int i = (int) f;
        return f < i ? i - 1 : i;
    }

    public static int ceil_double(double d) {
        int i = (int) d;
        return d > i ? i + 1 : i;
    }

    public static int ceil_float(float f) {
        int i = (int) f;
        return f > i ? i + 1 : i;
    }

    public static double sin(double d) {
        return Math.sin(d);
    }

    public static double cos(double d) {
        return Math.cos(d);
    }

    public static double sqrt(double d) {
        return Math.sqrt(d);
    }

    public static float sin(float f) {
        return (float) Math.sin(f);
    }

    public static float cos(float f) {
        return (float) Math.cos(f);
    }

    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    public static double clip(double value, double min, double max) {
        if (value > max) value = max;
        if (value < min) value = min;
        return value;
    }

    public static float clip(float value, float min, float max) {
        if (value > max) value = max;
        if (value < min) value = min;
        return value;
    }

    public static int clip(int value, int min, int max) {
        if (value > max) value = max;
        if (value < min) value = min;
        return value;
    }

    public static boolean between(double min, double value, double max) {
        return min <= value && value <= max;
    }

    public static double approachLinear(double a, double b, double max) {
        return a > b ? a - b < max ? b : a - max : b - a < max ? b : a + max;
    }

    public static float approachLinear(float a, float b, float max) {
        return a > b ? a - b < max ? b : a - max : b - a < max ? b : a + max;
    }

    public static double interpolate(double a, double b, double d) {
        return a + (b - a) * d;
    }

    public static int roundAway(double d) {
        return (int) (d < 0 ? Math.floor(d) : Math.ceil(d));
    }

    public static double average(double... values) {
        double sum = 0;
        for (double v : values) sum += v;
        return values.length == 0 ? 0 : sum / values.length;
    }

    public static int compare(int a, int b) {
        return Integer.compare(a, b);
    }

    public static int compare(double a, double b) {
        return a == b ? 0 : a < b ? -1 : 1;
    }
}
