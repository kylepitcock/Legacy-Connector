package legacyconnector.v1_12_2.net.minecraft.client.resources;

import java.util.Map;

/**
 * The loaded translations of the current language ({@code I18n.i18nLocale}). {@code properties} is the game's own table (mods read it, and
 * add entries to it at run time).
 */
public class Locale {
    /** Phantom: the translation table (redirected to {@link #mcc$get$properties}). */
    @Deprecated
    public Map<String, String> properties;

    @SuppressWarnings("unchecked")
    public static Map<String, String> mcc$get$properties(Locale l) {
        var language = net.minecraft.locale.Language.getInstance();
        for (Class<?> c = language.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                var f = c.getDeclaredField("storage");
                f.setAccessible(true);
                Object map = f.get(language);
                if (map instanceof Map<?, ?> m) {
                    try {
                        ((Map<String, String>) m).putAll(Map.of());
                        return (Map<String, String>) m;
                    } catch (UnsupportedOperationException e) {
                        // an immutable table: writable copy below
                    }
                    var copy = new java.util.HashMap<>((Map<String, String>) m);
                    f.set(language, copy);
                    return copy;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                // try the superclass
            }
        }
        return new java.util.HashMap<>();
    }

    public void loadLocaleDataFiles(IResourceManager resourceManager, java.util.List<String> languageList) {
    }

    public String formatMessage(String key, Object[] format) {
        return I18n.format(key, format);
    }

    public boolean hasKey(String key) {
        return I18n.hasKey(key);
    }
}
