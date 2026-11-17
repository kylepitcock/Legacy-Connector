package legacyconnector.v1_6_4.facade.net.minecraft.tileentity;

/** Facade of the 1.7.10 shim's {@code TileEntityBrewingStand} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class TileEntityBrewingStand {
    private TileEntityBrewingStand() {}

    /** 1.6.4 {@code setBrewTime(ticks)}. */
    public static void setBrewTime(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityBrewingStand self, int ticks) {
        self.func_145938_d(ticks);
    }
}
