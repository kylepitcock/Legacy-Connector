package legacyconnector.v1_6_4.facade.net.minecraft.item;

/** Facade of the 1.7.10 shim's {@code ItemSword} for 1.5.2 and 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class ItemSword {
    private ItemSword() {}

    /** {@code func_82803_g()}: the damage the sword's tool material adds (1.5.2 and 1.6.4 returned it as an int; 1.7.10 as a float). */
    public static int func_82803_g(legacyconnector.v1_7_10.net.minecraft.item.ItemSword self) {
        return (int) self.func_150931_i();
    }
}
