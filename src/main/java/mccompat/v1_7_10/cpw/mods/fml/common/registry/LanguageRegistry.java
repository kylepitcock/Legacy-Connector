package mccompat.v1_7_10.cpw.mods.fml.common.registry;

import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import org.kylepitcock.mccompat.core.pack.GeneratedTranslations;

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
}
