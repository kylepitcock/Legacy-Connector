package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The desert well: a sandstone ring with a water source, standing on sand over a solid base. */
public class WorldGenDesertWells extends WorldGenerator {
    public WorldGenDesertWells() {
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        while (world.isAirBlock(x, y, z) && y > 2) --y;
        if (world.getBlock(x, y, z) != Blocks.sand) return false;
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                if (world.isAirBlock(x + i, y - 1, z + j) && world.isAirBlock(x + i, y - 2, z + j)) return false;
            }
        }
        for (int i = -1; i <= 0; ++i) {
            for (int j = -2; j <= 2; ++j) {
                for (int k = -2; k <= 2; ++k) world.setBlock(x + j, y + i, z + k, Blocks.sandstone, 0, 2);
            }
        }
        world.setBlock(x, y, z, Blocks.flowing_water, 0, 2);
        world.setBlock(x - 1, y, z, Blocks.flowing_water, 0, 2);
        world.setBlock(x + 1, y, z, Blocks.flowing_water, 0, 2);
        world.setBlock(x, y, z - 1, Blocks.flowing_water, 0, 2);
        world.setBlock(x, y, z + 1, Blocks.flowing_water, 0, 2);
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                if (i == -2 || i == 2 || j == -2 || j == 2) world.setBlock(x + i, y + 1, z + j, Blocks.sandstone, 0, 2);
            }
        }
        world.setBlock(x + 2, y + 1, z, Blocks.stone_slab, 1, 2);
        world.setBlock(x - 2, y + 1, z, Blocks.stone_slab, 1, 2);
        world.setBlock(x, y + 1, z + 2, Blocks.stone_slab, 1, 2);
        world.setBlock(x, y + 1, z - 2, Blocks.stone_slab, 1, 2);
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                if (i == 0 && j == 0) world.setBlock(x + i, y + 4, z + j, Blocks.sandstone, 0, 2);
                else world.setBlock(x + i, y + 4, z + j, Blocks.stone_slab, 1, 2);
            }
        }
        for (int i = 1; i <= 3; ++i) {
            world.setBlock(x - 1, y + i, z - 1, Blocks.sandstone, 0, 2);
            world.setBlock(x - 1, y + i, z + 1, Blocks.sandstone, 0, 2);
            world.setBlock(x + 1, y + i, z - 1, Blocks.sandstone, 0, 2);
            world.setBlock(x + 1, y + i, z + 1, Blocks.sandstone, 0, 2);
        }
        return true;
    }
}
