package legacyconnector.v1_6_4.facade.codechicken.nei.api;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.compat.nei.NeiBridge;

/** Facade of the bridged NEI API for 1.6.4 mods: NEI 1.6 addressed items by id. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class API {
    private API() {}

    /** {@code hideItem(itemId)}: every damage value of the item. */
    public static void hideItem(int id) {
        Item item = Ids.item(id);
        if (item != null) NeiBridge.hide(new ItemStack(item, 1, 32767));
    }
}
