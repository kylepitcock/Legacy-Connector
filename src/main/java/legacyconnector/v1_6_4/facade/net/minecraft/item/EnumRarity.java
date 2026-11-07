package legacyconnector.v1_6_4.facade.net.minecraft.item;

/** Facade of the 1.7.10 shim's {@code EnumRarity} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class EnumRarity {
    private EnumRarity() {}

    /** 1.6.4's {@code rarityColor} was the colour code index (0-15, the order of the formatting colours); 1.7.10 holds the formatting. */
    public static int mcc$get$rarityColor(legacyconnector.v1_7_10.net.minecraft.item.EnumRarity self) {
        return self.rarityColor == null ? 15 : self.rarityColor.ordinal();
    }
}
