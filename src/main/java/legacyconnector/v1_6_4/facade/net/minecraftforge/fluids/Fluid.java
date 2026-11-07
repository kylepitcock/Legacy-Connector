package legacyconnector.v1_6_4.facade.net.minecraftforge.fluids;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code Fluid} for 1.6.4 mods: the id of the fluid's block. See {@code Hierarchy.facadeOf}. */
public final class Fluid {
    private Fluid() {}

    public static int getBlockID(legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid self) {
        return Ids.blockId(self.getBlock());
    }
}
