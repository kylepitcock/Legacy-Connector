package legacyconnector.v1_7_10.net.minecraft.client.resources;

import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;

/** 1.7.10's client-side translation entry point; the same table as {@link StatCollector}. */
public class I18n {
    public static String format(String key, Object... args) {
        return args == null || args.length == 0 ? StatCollector.translateToLocal(key) : StatCollector.translateToLocalFormatted(key, args);
    }
}
