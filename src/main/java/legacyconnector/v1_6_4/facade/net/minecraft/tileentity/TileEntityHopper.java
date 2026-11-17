package legacyconnector.v1_6_4.facade.net.minecraft.tileentity;

/** Facade of the 1.7.10 shim's {@code TileEntityHopper} for 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class TileEntityHopper {
    private TileEntityHopper() {}

    /** 1.6.4 {@code isCoolingDown()}: the hopper is waiting out the delay after a transfer. */
    public static boolean isCoolingDown(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityHopper self) {
        return self.isOnTransferCooldown();
    }
}
