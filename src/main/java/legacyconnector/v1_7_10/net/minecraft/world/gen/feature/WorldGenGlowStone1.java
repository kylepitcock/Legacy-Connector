package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A glowstone cluster hanging from a netherrack ceiling: grown block by block, each new block touching exactly one other. */
public class WorldGenGlowStone1 extends WorldGenerator {
    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return cluster(world, random, x, y, z);
    }

    static boolean cluster(World world, Random random, int x, int y, int z) {
        if (!world.isAirBlock(x, y, z)) return false;
        if (world.getBlock(x, y + 1, z) != Blocks.netherrack) return false;
        world.setBlock(x, y, z, Blocks.glowstone, 0, 2);
        for (int i = 0; i < 1500; i++) {
            int bx = x + random.nextInt(8) - random.nextInt(8);
            int by = y - random.nextInt(12);
            int bz = z + random.nextInt(8) - random.nextInt(8);
            if (world.getBlock(bx, by, bz).getMaterial() != Material.air) continue;
            int touching = 0;
            for (int side = 0; side < 6; side++) {
                Block b = switch (side) {
                    case 0 -> world.getBlock(bx - 1, by, bz);
                    case 1 -> world.getBlock(bx + 1, by, bz);
                    case 2 -> world.getBlock(bx, by - 1, bz);
                    case 3 -> world.getBlock(bx, by + 1, bz);
                    case 4 -> world.getBlock(bx, by, bz - 1);
                    default -> world.getBlock(bx, by, bz + 1);
                };
                if (b == Blocks.glowstone) touching++;
            }
            if (touching == 1) world.setBlock(bx, by, bz, Blocks.glowstone, 0, 2);
        }
        return true;
    }
}
