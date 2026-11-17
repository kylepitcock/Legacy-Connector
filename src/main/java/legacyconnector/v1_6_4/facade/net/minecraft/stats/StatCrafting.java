package legacyconnector.v1_6_4.facade.net.minecraft.stats;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code StatCrafting} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class StatCrafting {
    private StatCrafting() {}

    /** 1.6.4 {@code getItemID()}: the id of the item the statistic counts. */
    public static int getItemID(legacyconnector.v1_7_10.net.minecraft.stats.StatCrafting self) {
        return Ids.itemId(self.func_150959_a());
    }
}
