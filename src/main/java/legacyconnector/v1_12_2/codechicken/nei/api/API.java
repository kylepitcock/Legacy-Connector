package legacyconnector.v1_12_2.codechicken.nei.api;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.compat.nei.NeiBridge;

/** NEI's public registration API on 1.12.2: hidden items go to JEI; item panel layout calls are accepted and ignored. */
public class API {
    public static void hideItem(ItemStack stack) {
        if (stack != null) NeiBridge.hideModern(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack));
    }

    public static void hideItem(Item item) {
        if (item != null) hideItem(new ItemStack(item, 1, 32767));
    }

    public static void addItemListEntry(ItemStack stack) {
    }

    public static void setItemListEntries(Item item, Iterable<ItemStack> entries) {
    }

    public static void setOverrideName(ItemStack stack, String name) {
    }

    public static void addSetRange(String name, Object range) {
    }
}
