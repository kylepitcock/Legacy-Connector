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

    /** Adds the key/value pairs of a 1.12 .lang file to the language table (Forge's language injection). */
    public static synchronized void inject(java.io.InputStream inputstream) {
        inject(instance, inputstream);
    }

    public static synchronized void inject(LanguageMap inst, java.io.InputStream inputstream) {
        inst.languageList.putAll(parseLangFile(inputstream));
        inst.lastUpdateTimeInMilliseconds = System.currentTimeMillis();
    }

    static LanguageMap getInstance() {
        return instance;
    }

    public static synchronized void replaceWith(Map<String, String> languageList) {
        instance.languageList.clear();
        instance.languageList.putAll(languageList);
        instance.lastUpdateTimeInMilliseconds = System.currentTimeMillis();
    }

    /** Prefix of 1.12's firework colour names (item.fireworksCharge.red ...), which the modern language file dropped for the colour names. */
    private static final String FIREWORK_COLOUR = "item.fireworksCharge.";

    /**
     * The modern language key standing in for a 1.12 vanilla key the modern language file no longer has, or the key itself. Mods label
     * dye colours with {@code item.fireworksCharge.<colour>} (BuildCraft's "Red Pipe Wire"); 1.21 names them {@code color.minecraft.<colour>}.
     */
    public static String modernKey(String key) {
        if (key == null || !key.startsWith(FIREWORK_COLOUR)) return key;
        String colour = key.substring(FIREWORK_COLOUR.length());
        if (colour.equals("silver")) colour = "light_gray";
        else colour = colour.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
        return "color.minecraft." + colour;
    }

    /** The game's translation of a key, with the 1.12 vanilla key aliases resolved. */
    public static String gameTranslation(String key) {
        var language = net.minecraft.locale.Language.getInstance();
        if (!language.has(key)) {
            String alias = modernKey(key);
            if (!alias.equals(key) && language.has(alias)) return language.getOrDefault(alias);
        }
        return language.getOrDefault(key);
    }

    public static boolean gameHasKey(String key) {
        var language = net.minecraft.locale.Language.getInstance();
        return language.has(key) || language.has(modernKey(key));
    }

    public synchronized String translateKey(String key) {
        String own = languageList.get(key);
        if (own != null) return own;
        return gameTranslation(key);
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
        return languageList.containsKey(key) || gameHasKey(key);
    }

    public long getLastUpdateTimeInMilliseconds() {
        return lastUpdateTimeInMilliseconds;
    }
}
