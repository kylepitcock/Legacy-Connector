package legacyconnector.v1_12_2.net.minecraft.util;

/**
 * Splits real time into game ticks: {@link #updateTimer} counts the whole ticks that passed ({@link #elapsedTicks}) and the fraction of the
 * next one ({@link #renderPartialTicks}). A mod's own timer runs on the system clock as 1.12's did; the game's timer is
 * {@link #mcc$game()}, synchronised with the modern client's frame timer.
 */
public class Timer {
    public int elapsedTicks;
    public float renderPartialTicks;
    public float elapsedPartialTicks;
    private long lastSyncSysClock;
    private final float tickLength;
    private static Timer mcc$game;

    public Timer(float tps) {
        this.tickLength = 1000.0F / tps;
        this.lastSyncSysClock = mcc$systemTime();
    }

    private static long mcc$systemTime() {
        return System.nanoTime() / 1000000L;
    }

    public void updateTimer() {
        long now = mcc$systemTime();
        elapsedPartialTicks = (float) (now - lastSyncSysClock) / tickLength;
        lastSyncSysClock = now;
        renderPartialTicks += elapsedPartialTicks;
        elapsedTicks = (int) renderPartialTicks;
        renderPartialTicks -= (float) elapsedTicks;
    }

    /**
     * The game's timer (20 ticks a second) with the modern client's partial tick of the frame being drawn; on a server, its own clock.
     * Callers read it each frame, so it is refreshed when asked for.
     */
    public static synchronized Timer mcc$game() {
        if (mcc$game == null) mcc$game = new Timer(20.0F);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            mcc$game.renderPartialTicks = mcc$clientPartialTick();
        } else {
            mcc$game.updateTimer();
        }
        return mcc$game;
    }

    private static float mcc$clientPartialTick() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc == null ? 0.0F : mc.getTimer().getGameTimeDeltaPartialTick(true);
    }
}
