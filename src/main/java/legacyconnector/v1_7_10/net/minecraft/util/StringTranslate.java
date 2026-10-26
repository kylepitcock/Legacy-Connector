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

    public synchronized String translateKey(String key) {
        return StatCollector.translateToLocal(key);
    }

    public synchronized String translateKeyFormat(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }

    public synchronized boolean containsTranslateKey(String key) {
        return StatCollector.canTranslate(key);
    }

    public long getLastUpdateTimeInMilliseconds() {
        return lastUpdateTimeInMilliseconds;
    }
}
