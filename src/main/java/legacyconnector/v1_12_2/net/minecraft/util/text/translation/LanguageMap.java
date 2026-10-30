package legacyconnector.v1_12_2.net.minecraft.util.text.translation;

import com.google.common.base.Splitter;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The server-side language table: the modern game's language (en_us on a server) with the 1.12 mods' lang files, which the
 * layer merged into it.
 */
public class LanguageMap {
    private static final Pattern NUMERIC_VARIABLE_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    private static final Splitter EQUAL_SIGN_SPLITTER = Splitter.on('=').limit(2);
    private static final LanguageMap instance = new LanguageMap();
    public final Map<String, String> languageList = new java.util.concurrent.ConcurrentHashMap<>();
    private long lastUpdateTimeInMilliseconds = System.currentTimeMillis();

    public LanguageMap() {
    }

    /** A 1.12 .lang file as key/value pairs (comments and blank lines skipped, number formats made %s). */
    public static Map<String, String> parseLangFile(java.io.InputStream inputstream) {
        Map<String, String> table = new java.util.HashMap<>();
        try {
            for (String line : org.apache.commons.io.IOUtils.readLines(inputstream, java.nio.charset.StandardCharsets.UTF_8)) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] kv = com.google.common.collect.Iterables.toArray(EQUAL_SIGN_SPLITTER.split(line), String.class);
                if (kv != null && kv.length == 2) table.put(kv[0], NUMERIC_VARIABLE_PATTERN.matcher(kv[1]).replaceAll("%$1s"));
            }
        } catch (RuntimeException e) {
            // an unreadable file adds nothing
        }
        return table;
    }

    static LanguageMap getInstance() {
        return instance;
    }

    public static synchronized void replaceWith(Map<String, String> languageList) {
        instance.languageList.clear();
        instance.languageList.putAll(languageList);
        instance.lastUpdateTimeInMilliseconds = System.currentTimeMillis();
    }

    public synchronized String translateKey(String key) {
        String own = languageList.get(key);
        if (own != null) return own;
        return net.minecraft.locale.Language.getInstance().getOrDefault(key);
    }

    public synchronized String translateKeyFormat(String key, Object... format) {
        String s = translateKey(key);
        try {
            return String.format(s, format);
        } catch (java.util.IllegalFormatException e) {
            return "Format error: " + s;
        }
    }

    public synchronized boolean isKeyTranslated(String key) {
        return languageList.containsKey(key) || net.minecraft.locale.Language.getInstance().has(key);
    }

    public long getLastUpdateTimeInMilliseconds() {
        return lastUpdateTimeInMilliseconds;
    }
}
