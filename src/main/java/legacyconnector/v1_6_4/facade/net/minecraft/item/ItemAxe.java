package legacyconnector.v1_6_4.facade.net.minecraft.item;

/** Facade of the 1.7.10 shim's {@code ItemAxe} for 1.6.4 mods: the array of blocks the tool is effective against. Never instantiated. */
public final class ItemAxe {
    private ItemAxe() {}

    /** 1.6.4 {@code ItemAxe.blocksEffectiveAgainst}: the 1.7.10 set as the array 1.6.4 had (a copy: the set is the one to extend). */
    public static legacyconnector.v1_7_10.net.minecraft.block.Block[] mcc$get$blocksEffectiveAgainst() {
        return legacyconnector.v1_7_10.net.minecraft.item.ItemAxe.field_150917_c.toArray(new legacyconnector.v1_7_10.net.minecraft.block.Block[0]);
    }
}
