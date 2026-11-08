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

    /** The stack in the local player's hand (null for an empty hand). */
    public static ItemStack getHeldItem() {
        var player = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.mcc$get$thePlayer(legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft());
        return player == null ? null : player.getHeldItem();
    }

    /** Whether the stack is a real item (not null, with an item): NEI's item list only holds those. */
    public static boolean isValidItem(ItemStack stack) {
        return stack != null && stack.getItem() != null;
    }

    /** NEI translated its own keys; the name of a recipe handler falls back to the key. */
    public static String translate(String key, Object... params) {
        return key;
    }
}
