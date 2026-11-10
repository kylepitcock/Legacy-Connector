package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.item.Item;

/**
 * The Beta-side identity of a modern stack Beta 1.7.3 had no item for (newer vanilla items, other modern mods' items). Original
 * containers hold it like any item: its damage is the modern item's registry id, so Beta's id/damage merge rules never combine two
 * different modern items, and the exact modern stack (components included) rides on the stack's native snapshot.
 */
public final class NativeStackItemB173 extends Item {
    /** The last Beta item slot; claimed only when no original item uses it. */
    public static final int ID = 31999;
    private static volatile NativeStackItemB173 instance;
    private static volatile boolean unavailable;

    private NativeStackItemB173() {
        super(ID, true);
        setHasSubtypes(true);
        setTranslationKey("legacyconnector.native_stack");
    }

    /** The shared item, claimed lazily after original content loaded; null if an original item owns the slot. */
    public static NativeStackItemB173 get() {
        var item = instance;
        if (item != null || unavailable) return item;
        synchronized (NativeStackItemB173.class) {
            if (instance != null || unavailable) return instance;
            Item owner = Item.ITEMS[ID];
            if (owner != null) {
                unavailable = true;
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3")
                        .warn("Beta item slot {} is used by {}; modern items cannot be placed into Beta inventories", ID, owner.getClass().getName());
                return null;
            }
            item = new NativeStackItemB173();
            Item.ITEMS[ID] = item;
            return instance = item;
        }
    }

    public static boolean is(int itemId) {
        return itemId == ID && get() != null;
    }
}
