package legacyconnector.v1_6_4.facade.net.minecraft.world;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/**
 * Facade of the 1.7.10 shim's {@code IBlockAccess} (and so of {@code World}) for 1.6.4 mods: the id-returning reads.
 * See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class IBlockAccess {
    private IBlockAccess() {}

    /** The block id at a position: 0 for air. */
    public static int getBlockId(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess self, int x, int y, int z) {
        return Ids.blockId(self.getBlock(x, y, z));
    }

    /** The material of the block at a position. */
    public static legacyconnector.v1_7_10.net.minecraft.block.material.Material getBlockMaterial(
            legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess self, int x, int y, int z) {
        legacyconnector.v1_7_10.net.minecraft.block.Block b = self.getBlock(x, y, z);
        return b == null ? legacyconnector.v1_7_10.net.minecraft.block.material.Material.air : b.getMaterial();
    }
}
