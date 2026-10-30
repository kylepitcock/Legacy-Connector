package legacyconnector.v1_12_2.net.minecraft.util;

/**
 * 1.12.2's game timer: ticks to run this frame and the partial tick. A timer a mod makes counts real time itself ({@link #updateTimer});
 * the client's ({@code Minecraft.timer}) answers from the game's delta tracker. The fields are phantoms read through their accessors.
 */
public class Timer {
    @Deprecated
    public int elapsedTicks;
    @Deprecated
    public float renderPartialTicks;
    @Deprecated
    public float elapsedPartialTicks;
    @Deprecated
    private long lastSyncSysClock;
    @Deprecated
    private float tickLength;

    private final boolean mcc$client;
    private int mcc$elapsedTicks;
    private float mcc$renderPartialTicks;
    private float mcc$elapsedPartialTicks;
    private long mcc$lastSyncSysClock;
    private float mcc$tickLength;

    public Timer(float tps) {
        this(tps, false);
    }

    private Timer(float tps, boolean client) {
        this.mcc$client = client;
        this.mcc$tickLength = 1000.0F / tps;
        this.mcc$lastSyncSysClock = System.currentTimeMillis();
    }

    private static Timer client;

    /** The client's timer (a view of the game's delta tracker). */
    public static synchronized Timer mcc$client() {
        if (client == null) client = new Timer(20.0F, true);
        return client;
    }

    private static boolean live(Timer t) {
        return t.mcc$client && net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    }

    public void updateTimer() {
        if (live(this)) return;
        long i = System.currentTimeMillis();
        mcc$elapsedPartialTicks = (float) (i - mcc$lastSyncSysClock) / mcc$tickLength;
        mcc$lastSyncSysClock = i;
        mcc$renderPartialTicks += mcc$elapsedPartialTicks;
        mcc$elapsedTicks = (int) mcc$renderPartialTicks;
        mcc$renderPartialTicks -= (float) mcc$elapsedTicks;
    }

    public static int mcc$get$elapsedTicks(Timer t) {
        return t.mcc$elapsedTicks;
    }

    public static void mcc$set$elapsedTicks(Timer t, int v) {
        t.mcc$elapsedTicks = v;
    }

    public static float mcc$get$renderPartialTicks(Timer t) {
        return live(t) ? ClientAccess.partialTick() : t.mcc$renderPartialTicks;
    }

    public static void mcc$set$renderPartialTicks(Timer t, float v) {
        t.mcc$renderPartialTicks = v;
    }

    public static float mcc$get$elapsedPartialTicks(Timer t) {
        return live(t) ? ClientAccess.elapsed() : t.mcc$elapsedPartialTicks;
    }

    public static void mcc$set$elapsedPartialTicks(Timer t, float v) {
        t.mcc$elapsedPartialTicks = v;
    }

    public static long mcc$get$lastSyncSysClock(Timer t) {
        return t.mcc$lastSyncSysClock;
    }

    public static void mcc$set$lastSyncSysClock(Timer t, long v) {
        t.mcc$lastSyncSysClock = v;
    }

    public static float mcc$get$tickLength(Timer t) {
        return t.mcc$tickLength;
    }

    public static void mcc$set$tickLength(Timer t, float v) {
        t.mcc$tickLength = v;
    }

    /** The game's delta tracker, kept apart so this class verifies on a dedicated server. */
    private static final class ClientAccess {
        static float partialTick() {
            return net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        }

        static float elapsed() {
            return net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
        }
    }
}
