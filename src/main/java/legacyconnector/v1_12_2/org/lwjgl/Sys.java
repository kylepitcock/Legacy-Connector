package legacyconnector.v1_12_2.org.lwjgl;

/** LWJGL 2's system helpers; mods read the clock from it. */
public final class Sys {
    private Sys() {}

    public static long getTime() {
        return System.nanoTime() / 1_000_000L;
    }

    public static long getTimerResolution() {
        return 1000L;
    }

    public static String getVersion() {
        return "2.9.1";
    }

    public static void alert(String title, String message) {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/Sys").warn("{}: {}", title, message);
    }

    public static boolean openURL(String url) {
        try {
            net.minecraft.Util.getPlatform().openUri(url);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean is64Bit() {
        return System.getProperty("os.arch", "").contains("64");
    }
}
