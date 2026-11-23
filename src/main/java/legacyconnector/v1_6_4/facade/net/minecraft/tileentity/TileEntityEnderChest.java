package legacyconnector.v1_6_4.facade.net.minecraft.tileentity;

/** Facade of the 1.7.10 shim's {@code TileEntityEnderChest} for 1.6.4 mods: the lid fields under their 1.6.4 names. Never instantiated. */
public final class TileEntityEnderChest {
    private TileEntityEnderChest() {}

    /** 1.6.4 {@code lidAngle}. */
    public static float mcc$get$lidAngle(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityEnderChest self) {
        return self.field_145972_a;
    }

    public static void mcc$set$lidAngle(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityEnderChest self, float v) {
        self.field_145972_a = v;
    }

    /** 1.6.4 {@code prevLidAngle}. */
    public static float mcc$get$prevLidAngle(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityEnderChest self) {
        return self.field_145975_i;
    }

    public static void mcc$set$prevLidAngle(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityEnderChest self, float v) {
        self.field_145975_i = v;
    }

    /** 1.6.4 {@code numUsingPlayers}. */
    public static int mcc$get$numUsingPlayers(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityEnderChest self) {
        return self.field_145973_j;
    }

    public static void mcc$set$numUsingPlayers(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityEnderChest self, int v) {
        self.field_145973_j = v;
    }
}
