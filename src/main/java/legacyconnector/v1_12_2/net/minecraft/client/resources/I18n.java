package legacyconnector.v1_12_2.net.minecraft.client.resources;

/**
 * The client translation helper of 1.12: the current language's entries (legacy mods' {@code .lang} files included), formatted with
 * {@link String#format}. It reads the language table directly, so it also answers on a dedicated server (where 1.12 would have crashed).
 */
public class I18n {
    /** The loaded translations (a view of the game's table). */
    public static Locale i18nLocale = new Locale();

    public static void setLocale(Locale locale) {
        i18nLocale = locale;
    }

    public static String format(String key, Object... parameters) {
        String pattern = net.minecraft.locale.Language.getInstance().getOrDefault(key);
        if (parameters == null || parameters.length == 0) return pattern;
        try {
            return String.format(pattern, parameters);
        } catch (java.util.IllegalFormatException e) {
            return "Format error: " + pattern;
        }
    }

    public static boolean hasKey(String key) {
        return net.minecraft.locale.Language.getInstance().has(key);
    }
}
