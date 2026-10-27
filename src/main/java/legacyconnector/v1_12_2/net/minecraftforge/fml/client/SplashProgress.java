package legacyconnector.v1_12_2.net.minecraftforge.fml.client;

/**
 * FML's loading splash. Mods paused it to show their own windows or use the GL context while loading; the modern game's loading
 * overlay needs neither, so pausing and resuming do nothing.
 */
public class SplashProgress {
    public static void pause() {
    }

    public static void resume() {
    }

    public static void start() {
    }

    public static void finish() {
    }
}
