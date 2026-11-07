package legacyconnector.v1_6_4.facade.net.minecraft.item;

/** Facade of the 1.7.10 shim's {@code ItemFood} for 1.6.4 mods: the heal amount and saturation did not depend on the stack. Never instantiated. */
public final class ItemFood {
    private ItemFood() {}

    /** {@code getHealAmount()}: hunger points restored (1.7.10 asks with the stack). */
    public static int getHealAmount(legacyconnector.v1_7_10.net.minecraft.item.ItemFood self) {
        return self.getHealAmount(null);
    }

    /** {@code getSaturationModifier()}. */
    public static float getSaturationModifier(legacyconnector.v1_7_10.net.minecraft.item.ItemFood self) {
        return self.getSaturationModifier(null);
    }
}
