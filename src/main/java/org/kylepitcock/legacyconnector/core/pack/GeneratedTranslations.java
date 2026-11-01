package org.kylepitcock.legacyconnector.core.pack;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Translations registered from code (LanguageRegistry.addName and friends), served as generated lang files. */
public final class GeneratedTranslations {
    private static final Map<String, Map<String, String>> BY_LANG = new ConcurrentHashMap<>();
    /**
     * The lang files bundled in legacy jars, read when the mods are constructed (FML 1.7.10 injected every jar's
     * {@code assets/<domain>/lang/en_US.lang} into the server translation table before construction, so names translated
     * in static initializers worked). Consulted by {@link #get} but not exported: the jars' converted lang files serve the client.
     */
    private static final Map<String, Map<String, String>> BUNDLED = new ConcurrentHashMap<>();

    private GeneratedTranslations() {}

    private static String lang(String legacy) {
        return legacy.toLowerCase(Locale.ROOT);
    }

    public static void put(String lang, String key, String value) {
        BY_LANG.computeIfAbsent(lang(lang), k -> new ConcurrentHashMap<>()).put(key, value);
    }

    /** The live table of one language (writes are served like {@link #put}). */
    public static Map<String, String> table(String lang) {
        return BY_LANG.computeIfAbsent(lang(lang), k -> new ConcurrentHashMap<>());
    }

    public static String get(String lang, String key) {
        if (key == null) return null;
        Map<String, String> m = BY_LANG.get(lang(lang));
        String v = m == null ? null : m.get(key);
        if (v != null) return v;
        Map<String, String> b = BUNDLED.get(lang(lang));
        return b == null ? null : b.get(key);
    }

    /** A translation from a legacy jar's own lang file (see {@link #BUNDLED}). */
    public static void putBundled(String lang, String key, String value) {
        BUNDLED.computeIfAbsent(lang(lang), k -> new ConcurrentHashMap<>()).put(key, value);
    }

    static void contribute(Map<String, Supplier<byte[]>> out) {
        for (var e : BY_LANG.entrySet()) {
            JsonObject json = new JsonObject();
            new TreeMap<>(e.getValue()).forEach(json::addProperty);
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            out.put("assets/legacyconnector/lang/" + e.getKey() + ".json", () -> bytes);
        }
    }
}
