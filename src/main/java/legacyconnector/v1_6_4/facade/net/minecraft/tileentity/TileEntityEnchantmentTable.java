package legacyconnector.v1_6_4.facade.net.minecraft.tileentity;

/** Facade of the 1.7.10 shim's {@code TileEntityEnchantmentTable} for 1.6.4 mods: the book's target rotation under its 1.6.4 name. Never instantiated. */
public final class TileEntityEnchantmentTable {
    private TileEntityEnchantmentTable() {}

    /** 1.6.4 {@code bookRotation2}: the rotation the book turns towards. */
    public static float mcc$get$bookRotation2(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityEnchantmentTable self) {
        return self.field_145924_q;
    }

    public static void mcc$set$bookRotation2(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityEnchantmentTable self, float v) {
        self.field_145924_q = v;
    }
}
