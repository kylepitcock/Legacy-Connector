package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A column of sugar cane next to water; the decorator's {@code reedGen}. */
public class WorldGenReed extends WorldGenerator {
    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        for (int i = 0; i < 20; i++) {
            int px = x + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(4) - random.nextInt(4);
            if (!world.isAirBlock(px, y, pz)) continue;
            if (world.getBlock(px - 1, y - 1, pz).getMaterial() == Material.water || world.getBlock(px + 1, y - 1, pz).getMaterial() == Material.water
                    || world.getBlock(px, y - 1, pz - 1).getMaterial() == Material.water || world.getBlock(px, y - 1, pz + 1).getMaterial() == Material.water) {
                int h = 2 + random.nextInt(random.nextInt(3) + 1);
                for (int k = 0; k < h; k++) {
                    if (Blocks.reeds.canBlockStay(world, px, y + k, pz)) world.setBlock(px, y + k, pz, Blocks.reeds, 0, 2);
                }
            }
        }
        return true;
    }
}
