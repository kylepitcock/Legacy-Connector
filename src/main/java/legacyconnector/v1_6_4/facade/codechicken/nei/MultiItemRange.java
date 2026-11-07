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

    /** {@code add(first, last)}: the items with ids first..last. */
    public static legacyconnector.v1_7_10.codechicken.nei.MultiItemRange add(legacyconnector.v1_7_10.codechicken.nei.MultiItemRange self, int first, int last) {
        for (int id = first; id <= last; id++) add(self, id);
        return self;
    }
}
