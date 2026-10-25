package legacyconnector.v1_12_2.net.minecraft.util.text.translation;

/** The server-side translation helper of 1.12 (deprecated there too): vanilla and loaded language entries. */
public class I18n {
    public static String translateToLocal(String key) {
        return net.minecraft.locale.Language.getInstance().getOrDefault(key);
    }

    public static String translateToLocalFormatted(String key, Object... format) {
        String pattern = translateToLocal(key);
        try {
            return String.format(pattern, format);
        } catch (java.util.IllegalFormatException e) {
            return pattern;
        }
    }

    public static boolean canTranslate(String key) {
        return net.minecraft.locale.Language.getInstance().has(key);
    }

    public static String translateToFallback(String key) {
        return translateToLocal(key);
    }
}
