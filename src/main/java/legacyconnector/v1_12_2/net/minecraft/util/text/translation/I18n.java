package legacyconnector.v1_12_2.net.minecraft.util.text.translation;

/** The server-side translation helper of 1.12 (deprecated there too): vanilla and loaded language entries. */
public class I18n {
    /** The language map mods add entries to through {@code languageList} (Bookshelf's roman numerals; public as Forge's access transformer made it). */
    public static final LanguageMap localizedName = LanguageMap.getInstance();
    public static final LanguageMap fallbackTranslator = new LanguageMap();

    public static String translateToLocal(String key) {
        return localizedName.translateKey(key);
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
        return localizedName.isKeyTranslated(key);
    }

    public static String translateToFallback(String key) {
        return fallbackTranslator.translateKey(key);
    }
}
