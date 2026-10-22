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
        if (obj instanceof ItemStack s) key = s.getItem().getUnlocalizedName(s);
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
