package mccompat.v1_7_10.net.minecraft.util;

import net.minecraft.locale.Language;

public class StatCollector {
    public static String translateToLocal(String key) {
        return Language.getInstance().getOrDefault(key);
    }

    public static String translateToLocalFormatted(String key, Object... args) {
        String pattern = translateToLocal(key);
        try {
            return String.format(pattern, args);
        } catch (RuntimeException e) {
            return "Format error: " + pattern;
        }
    }

    public static String translateToFallback(String key) {
        return translateToLocal(key);
    }

    public static boolean canTranslate(String key) {
        return Language.getInstance().has(key);
    }

    public static long getLastTranslationUpdateTimeInMilliseconds() {
        return 0L;
    }
}
