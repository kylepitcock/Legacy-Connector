package legacyconnector.v1_6_4.facade.net.minecraft.item;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code ItemStack} for 1.6.4 mods: {@code stack.itemID}. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class ItemStack {
    private ItemStack() {}

    /** ItemStack.itemID */
    public static int mcc$get$itemID(legacyconnector.v1_7_10.net.minecraft.item.ItemStack self) {
        return Ids.itemId(self.getItem());
    }

    public static void mcc$set$itemID(legacyconnector.v1_7_10.net.minecraft.item.ItemStack self, int id) {
        self.setItem(Ids.item(id));
    }
}
