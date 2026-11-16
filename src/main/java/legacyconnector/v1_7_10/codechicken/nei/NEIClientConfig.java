package legacyconnector.v1_7_10.codechicken.nei;

import java.util.HashMap;
import java.util.Map;

/** NEI's client settings: the key bindings mods registered (the layer's JEI bridge owns the overlay, so NEI itself reports hidden). */
public class NEIClientConfig {
    public static final Map<String, Integer> keyBindings = new HashMap<>();

    public static int getKeyBinding(String name) {
        return keyBindings.getOrDefault(name, 0);
    }

    public static void addKeyBinding(String name, int defaultKey) {
        keyBindings.putIfAbsent(name, defaultKey);
    }

    /** NEI's logger (GTNH's NEI keeps one here; its addons log through it). */
    public static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger("NotEnoughItems");

    /** The settings addons read and write ({@code getIntSetting("inventory.itemsPerPage")}); defaults are what an unconfigured NEI gives. */
    private static final Map<String, Object> settings = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean getBooleanSetting(String name) {
        return settings.get(name) instanceof Boolean b && b;
    }

    public static void setBooleanSetting(String name, boolean value) {
        settings.put(name, value);
    }

    public static int getIntSetting(String name) {
        return settings.get(name) instanceof Integer i ? i : 0;
    }

    public static void setIntSetting(String name, int value) {
        settings.put(name, value);
    }

    public static boolean isHidden() {
        return true;
    }

    public static boolean isEnabled() {
        return false;
    }

    public static boolean canCheatItem(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
        return false;
    }

    public static boolean hasSMPCounterPart() {
        return false;
    }
}
