package legacyconnector.v1_7_10.net.minecraft.util;

public class StringUtils {
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String stripControlCodes(String text) {
        return text == null ? null : text.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }

    public static String ticksToElapsedTime(int ticks) {
        int seconds = ticks / 20;
        return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
    }
}
