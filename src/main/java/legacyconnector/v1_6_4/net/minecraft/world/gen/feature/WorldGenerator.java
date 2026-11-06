package legacyconnector.v1_6_4.net.minecraft.world.gen.feature;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code WorldGenerator} for 1.6.4 mods: block placement by id. See {@code Hierarchy.facadeOf}. */
public final class WorldGenerator {
    private WorldGenerator() {}

    public static void setBlockAndMetadata(legacyconnector.v1_7_10.net.minecraft.world.gen.feature.WorldGenerator self,
            legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int id, int meta) {
        legacyconnector.v1_7_10.net.minecraft.block.Block b = id <= 0 ? null : Ids.block(id);
        world.setBlock(x, y, z, b != null ? b : legacyconnector.v1_7_10.net.minecraft.init.Blocks.air, meta, 3);
    }
}
