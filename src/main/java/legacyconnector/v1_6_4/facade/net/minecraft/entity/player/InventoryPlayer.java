package legacyconnector.v1_6_4.facade.net.minecraft.entity.player;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/** Facade of the 1.7.10 shim's {@code InventoryPlayer} for 1.6.4 mods: the methods that took item ids. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class InventoryPlayer {
    private InventoryPlayer() {}

    /** Removes one item of the id from the inventory; false if there is none. */
    public static boolean consumeInventoryItem(legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer self, int id) {
        Item item = Ids.item(id);
        return item != null && self.consumeInventoryItem(item);
    }

    public static boolean hasItem(legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer self, int id) {
        Item item = Ids.item(id);
        return item != null && self.hasItem(item);
    }

    /** Removes the items of the id (and damage, -1 for any); returns how many were removed. */
    public static int clearInventory(legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer self, int id, int damage) {
        Item item = id <= 0 ? null : Ids.item(id);
        return self.clearInventory(item, damage);
    }
}
