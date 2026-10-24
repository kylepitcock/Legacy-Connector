package legacyconnector.v1_7_10.net.minecraft.util;

/** The client frame timer. {@link #renderPartialTicks} is the fraction of the current tick at the time of drawing; the layer updates it each frame. */
public class Timer {
    public static final Timer INSTANCE = new Timer(20.0F);

    public int elapsedTicks;
    public float renderPartialTicks;
    public float timerSpeed = 1.0F;
    public float elapsedPartialTicks;
    private final float ticksPerSecond;

    public Timer(float ticksPerSecond) {
        this.ticksPerSecond = ticksPerSecond;
    }

    public void updateTimer() {
    }

    /** Called by the layer with the partial tick of the frame being drawn. */
    public static void mcc$frame(float partialTick) {
        INSTANCE.renderPartialTicks = partialTick;
        INSTANCE.elapsedPartialTicks = partialTick;
    }
}
