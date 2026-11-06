package legacyconnector.v1_6_4.facade.net.minecraft.client.resources;

/**
 * Facade of the 1.7.10 shim's {@code I18n} for 1.6.4 mods: {@code getString(key)} and {@code getStringParams(key, args)}
 * were 1.7.10's {@code format}. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class I18n {
    private I18n() {}

    public static String getString(String key) {
        return legacyconnector.v1_7_10.net.minecraft.client.resources.I18n.format(key);
    }

    public static String getStringParams(String key, Object... args) {
        return legacyconnector.v1_7_10.net.minecraft.client.resources.I18n.format(key, args);
    }
}
