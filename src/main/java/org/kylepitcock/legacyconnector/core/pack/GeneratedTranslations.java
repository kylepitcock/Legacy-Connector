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
        Map<String, String> m = BY_LANG.get(lang(lang));
        return m == null ? null : m.get(key);
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
