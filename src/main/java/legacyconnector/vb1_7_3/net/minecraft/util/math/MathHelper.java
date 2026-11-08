package legacyconnector.vb1_7_3.net.minecraft.util.math;

/** Numeric helpers used by legacy raycasts and projectile trajectories. */
public final class MathHelper {
    public static float sin(float v) { return (float) Math.sin(v); }
    public static float cos(float v) { return (float) Math.cos(v); }
    public static float sqrt(double v) { return (float) Math.sqrt(v); }
    public static float sqrt(float v) { return (float) Math.sqrt(v); }
    public static int floor(double v) { return (int) Math.floor(v); }
    public static int floor(float v) { return (int) Math.floor(v); }
    public static float abs(float v) { return Math.abs(v); }
}
