package legacyconnector.v1_7_10.cpw.mods.fml.common.registry;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations;

/** Code-registered translations; served to the client through the generated resource pack. */
public class LanguageRegistry {
    private static final LanguageRegistry INSTANCE = new LanguageRegistry();

    public static LanguageRegistry instance() {
        return INSTANCE;
    }

    public String getStringLocalization(String key) {
        return getStringLocalization(key, "en_US");
    }

    public String getStringLocalization(String key, String lang) {
        String v = GeneratedTranslations.get(lang, key);
        return v == null ? "" : v;
    }

    public void addStringLocalization(String key, String value) {
        addStringLocalization(key, "en_US", value);
    }

    public void addStringLocalization(String key, String lang, String value) {
        GeneratedTranslations.put(lang, key, value);
    }

    public void addNameForObject(Object obj, String lang, String name) {
        String key;
        if (obj instanceof ItemStack s) {
            if (s.getItem() == null) return; // a stack of an item that was never registered (an id left unclaimed): nothing to name
            key = s.getItem().getUnlocalizedName(s);
        }
        else if (obj instanceof Item i) key = i.getUnlocalizedName();
        else if (obj instanceof Block b) key = b.getUnlocalizedName();
        else return;
        addStringLocalization(key + ".name", lang, name);
    }

    public static void addName(Object obj, String name) {
        instance().addNameForObject(obj, "en_US", name);
    }

    /** Adds a whole language table (mods load their own .lang files and inject them). */
    public void injectLanguage(String language, java.util.HashMap<String, String> parsedLangFile) {
        if (parsedLangFile != null) parsedLangFile.forEach((k, v) -> addStringLocalization(k, language, v));
    }

    public void loadLocalization(String localizationFile, String lang, boolean isXML) {
        java.io.InputStream in = LanguageRegistry.class.getResourceAsStream(localizationFile);
        if (in == null) return;
        mcc$load(in, lang, isXML);
    }

    public void loadLocalization(java.net.URL localizationFile, String lang, boolean isXML) {
        try {
            mcc$load(localizationFile.openStream(), lang, isXML);
        } catch (java.io.IOException ignored) {
            // missing file: FML logged and continued
        }
    }

    private static final java.util.Set<java.nio.file.Path> MCC$SCANNED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.util.regex.Pattern MCC$EN_US = java.util.regex.Pattern.compile("assets/[^/]+/lang/en_us\\.(lang|json)",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * FML 1.7.10's {@code loadLanguagesFor}: before a mod is constructed, every {@code assets/<domain>/lang/en_US.lang} of its jar
     * goes into the server translation table, so {@code StatCollector.translateToLocal} works in static initializers and on the
     * server (ReactorCraft translates its ore names, "ore.cadmium", in the constructor of its ore enum). The converted
     * {@code en_us.json} of a translated jar is read the same way. Each jar is read once.
     */
    public static void mcc$loadLanguagesFor(java.nio.file.Path jar) {
        if (jar == null || java.nio.file.Files.isDirectory(jar)) return; // a mod folder's lang files are in the translated jar's resources
        if (!java.nio.file.Files.isRegularFile(jar) || !MCC$SCANNED.add(jar.toAbsolutePath())) return;
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var e = entries.nextElement();
                if (e.isDirectory() || !MCC$EN_US.matcher(e.getName()).matches()) continue;
                try (java.io.InputStream in = zip.getInputStream(e)) {
                    if (e.getName().endsWith(".json")) {
                        net.minecraft.locale.Language.loadFromJson(in, (k, v) -> GeneratedTranslations.putBundled("en_US", k, v));
                    } else {
                        legacyconnector.v1_7_10.net.minecraft.util.StringTranslate.parseLangFile(in)
                                .forEach((k, v) -> GeneratedTranslations.putBundled("en_US", k, v.trim()));
                    }
                } catch (java.io.IOException | RuntimeException ignored) {
                    // a broken lang file adds nothing, as in FML
                }
            }
        } catch (java.io.IOException | RuntimeException ignored) {
            // unreadable jar: FML logged and continued
        }
    }

    private void mcc$load(java.io.InputStream in, String lang, boolean isXML) {
        try (in) {
            java.util.Properties p = new java.util.Properties();
            if (isXML) p.loadFromXML(in);
            else p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            for (String k : p.stringPropertyNames()) addStringLocalization(k, lang, p.getProperty(k));
        } catch (java.io.IOException ignored) {
            // malformed file: FML logged and continued
        }
    }
}
