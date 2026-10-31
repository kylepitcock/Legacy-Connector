package legacyconnector.v1_7_10.codechicken.nei;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** The few client helpers of NEI that mods' recipe handlers call. */
public class NEIClientUtils {
    public static boolean areStacksSameType(ItemStack a, ItemStack b) {
        return NEIServerUtils.areStacksSameType(a, b);
    }

    public static boolean areStacksSameTypeCrafting(ItemStack a, ItemStack b) {
        return NEIServerUtils.areStacksSameTypeCrafting(a, b);
    }

    /** NEI translated its own keys; the name of a recipe handler falls back to the key. */
    public static String translate(String key, Object... params) {
        return key;
    }
}
