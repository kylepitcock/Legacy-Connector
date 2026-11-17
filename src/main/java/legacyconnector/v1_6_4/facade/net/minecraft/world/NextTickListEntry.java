package legacyconnector.v1_6_4.facade.net.minecraft.world;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code NextTickListEntry} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class NextTickListEntry {
    private NextTickListEntry() {}

    /** 1.6.4 {@code blockID}: the id of the block the scheduled update is for. */
    public static int mcc$get$blockID(legacyconnector.v1_7_10.net.minecraft.world.NextTickListEntry self) {
        return Ids.blockId(self.func_151351_a());
    }
}
