package legacyconnector.v1_6_4.facade.net.minecraftforge.fluids;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code Fluid} for 1.6.4 mods: the id of the fluid's block. See {@code Hierarchy.facadeOf}. */
public final class Fluid {
    private Fluid() {}

    public static int getBlockID(legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid self) {
        return Ids.blockId(self.getBlock());
    }
    /** 1.6.4 {@code setBlockID(Block)} / {@code setBlockID(int)}: the block that holds the fluid in the world. */
    public static legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid setBlockID(legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid self,
            legacyconnector.v1_7_10.net.minecraft.block.Block block) {
        return self.setBlock(block);
    }

    public static legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid setBlockID(legacyconnector.v1_7_10.net.minecraftforge.fluids.Fluid self, int id) {
        legacyconnector.v1_7_10.net.minecraft.block.Block block = Ids.block(id);
        return block == null ? self : self.setBlock(block);
    }
}
