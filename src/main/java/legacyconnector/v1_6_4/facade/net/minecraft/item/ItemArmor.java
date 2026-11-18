package legacyconnector.v1_6_4.facade.net.minecraft.item;

/** Facade of the 1.7.10 shim's {@code ItemArmor} for 1.5.2 and 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class ItemArmor {
    private ItemArmor() {}

    /** {@code getMaxDamageArray()} (static): the durability factor of the helmet, chestplate, leggings and boots slots (11, 16, 15, 13). */
    public static int[] getMaxDamageArray() {
        return new int[] {11, 16, 15, 13};
    }
}
