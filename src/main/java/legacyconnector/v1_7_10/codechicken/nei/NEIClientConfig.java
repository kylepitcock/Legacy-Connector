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
