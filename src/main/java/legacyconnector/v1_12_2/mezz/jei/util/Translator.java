package legacyconnector.v1_12_2.mezz.jei.util;

/** JEI 4 (Minecraft 1.12.2) internals some plugins reach into; here a small working copy. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public final class Translator {
    private Translator() {}

    public static String translateToLocal(String key) {
        return legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format(key);
    }

    public static String translateToLocalFormatted(String key, Object... format) {
        return legacyconnector.v1_12_2.net.minecraft.client.resources.I18n.format(key, format);
    }
}
