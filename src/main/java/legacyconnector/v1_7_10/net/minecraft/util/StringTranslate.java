package legacyconnector.v1_7_10.net.minecraft.util;

import java.util.Map;
import java.util.regex.Pattern;
import org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations;

/**
 * 1.7.10's server-side translation table. Mods reach its {@code languageList} by reflection and put their
 * translations straight in: that map is the live code-registered en_US table (see {@link StatCollector}).
 */
public class StringTranslate {
    private static final Pattern numericVariablePattern = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    private static StringTranslate instance = new StringTranslate();
    private final Map<String, String> languageList = GeneratedTranslations.table("en_US");
    private long lastUpdateTimeInMilliseconds = System.currentTimeMillis();

    static StringTranslate getInstance() {
        return instance;
    }

    public static synchronized void inject(java.io.InputStream in) {
        try (in) {
            for (String line : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                int eq = line.indexOf('=');
                if (eq > 0) instance.languageList.put(line.substring(0, eq), numericVariablePattern.matcher(line.substring(eq + 1).trim()).replaceAll("%$1s"));
            }
            instance.lastUpdateTimeInMilliseconds = System.currentTimeMillis();
        } catch (java.io.IOException ignored) {
            // unreadable: as vanilla, nothing is added
        }
    }

    /**
     * Forge's lang-file reader: {@code key=value} lines (not {@code #} comments) into a map, with {@code %d}/{@code %f}
     * placeholders turned into {@code %s}. Unreadable input gives what was read so far.
     */
    public static java.util.HashMap<String, String> parseLangFile(java.io.InputStream in) {
        java.util.HashMap<String, String> table = new java.util.HashMap<>();
        try {
            String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String line : text.split("\r?\n|\r")) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                table.put(line.substring(0, eq), numericVariablePattern.matcher(line.substring(eq + 1)).replaceAll("%$1s"));
            }
        } catch (Exception ignored) {
            // as Forge: a broken stream leaves the table as far as it got
        }
        return table;
    }

    /** 1.4.7: the entry a name key maps to ({@code key.name}), for blocks, items and entities. */
    public synchronized String translateNamedKey(String key) {
        return StatCollector.translateToLocal(key + ".name");
    }

    public synchronized String translateKey(String key) {
        return StatCollector.translateToLocal(key);
    }

    public synchronized String translateKeyFormat(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }

    /** 1.4.7/1.5.2: the languages the game knows, keyed by code; mods (Immibis Core) iterate the keys to load their own language files. */
    public java.util.TreeMap<String, Object> getLanguageList() {
        java.util.TreeMap<String, Object> languages = new java.util.TreeMap<>();
        languages.put("en_US", "English (US)");
        return languages;
    }

    public synchronized boolean containsTranslateKey(String key) {
        return StatCollector.canTranslate(key);
    }

    /** 1.4.7: the selected language as a code ({@code en_US}); mods pick their own language files by it (BuildCraft's {@code lang/buildcraft/<code>.properties}). */
    public String getCurrentLanguage() {
        String code = "en_us";
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getLanguageManager() != null) code = mc.getLanguageManager().getSelected();
        } catch (RuntimeException | LinkageError e) {
            // a server: English
        }
        int us = code.indexOf('_');
        return us < 0 ? code : code.substring(0, us) + "_" + code.substring(us + 1).toUpperCase(java.util.Locale.ROOT);
    }

    public long getLastUpdateTimeInMilliseconds() {
        return lastUpdateTimeInMilliseconds;
    }
}
