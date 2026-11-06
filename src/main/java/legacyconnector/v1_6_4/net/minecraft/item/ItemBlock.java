package legacyconnector.v1_6_4.net.minecraft.item;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code ItemBlock} for 1.6.4 mods: the id of the block it places. See {@code Hierarchy.facadeOf}. */
public final class ItemBlock {
    private ItemBlock() {}

    public static int getBlockID(legacyconnector.v1_7_10.net.minecraft.item.ItemBlock self) {
        return Ids.blockId(self.mcc$block());
    }

    /** ItemBlock.blockID (1.6.4 kept the id as a field) */
    public static int mcc$get$blockID(legacyconnector.v1_7_10.net.minecraft.item.ItemBlock self) {
        return Ids.blockId(self.mcc$block());
    }
}
