package legacyconnector.v1_6_4.facade.net.minecraft.entity;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/** Facade of the 1.7.10 shim's {@code Entity} for 1.6.4 mods: the members that took item ids. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class Entity {
    private Entity() {}

    /** {@code dropItem(itemId, count)}: drops {@code count} of the item with that id at the entity; null for an unused id or air, as in 1.6.4. */
    public static EntityItem dropItem(legacyconnector.v1_7_10.net.minecraft.entity.Entity self, int itemId, int count) {
        Item item = Ids.item(itemId);
        return item == null ? null : self.dropItem(item, count);
    }
}
