package legacyconnector.v1_6_4.facade.codechicken.nei;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Facade of the bridged NEI {@code MultiItemRange} for 1.6.4 mods: ranges of item ids. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class MultiItemRange {
    private MultiItemRange() {}

    public static legacyconnector.v1_7_10.codechicken.nei.MultiItemRange add(legacyconnector.v1_7_10.codechicken.nei.MultiItemRange self, int id) {
        Item item = Ids.item(id);
        if (item != null) self.add(new ItemStack(item, 1, 32767));
        return self;
    }

    /** {@code add(id, firstDamage, lastDamage)}: the item with that id, every damage value of the range. */
    public static legacyconnector.v1_7_10.codechicken.nei.MultiItemRange add(legacyconnector.v1_7_10.codechicken.nei.MultiItemRange self, int id, int firstDamage, int lastDamage) {
        return add(self, Ids.item(id), firstDamage, lastDamage);
    }

    /** {@code add(item, firstDamage, lastDamage)}: every damage value of the range of the item. */
    public static legacyconnector.v1_7_10.codechicken.nei.MultiItemRange add(legacyconnector.v1_7_10.codechicken.nei.MultiItemRange self, Item item, int firstDamage, int lastDamage) {
        if (item != null) for (int damage = firstDamage; damage <= lastDamage && damage - firstDamage < 4096; damage++) self.add(new ItemStack(item, 1, damage));
        return self;
    }

    /** {@code add(first, last)}: the items with ids first..last. */
    public static legacyconnector.v1_7_10.codechicken.nei.MultiItemRange add(legacyconnector.v1_7_10.codechicken.nei.MultiItemRange self, int first, int last) {
        for (int id = first; id <= last; id++) add(self, id);
        return self;
    }
}
