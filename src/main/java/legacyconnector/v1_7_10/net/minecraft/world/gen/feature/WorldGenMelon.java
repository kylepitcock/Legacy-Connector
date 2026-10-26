package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A patch of melons on grass (64 attempts around the origin; jungles). */
public class WorldGenMelon extends WorldGenerator {
    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        for (int i = 0; i < 64; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            if (Blocks.melon_block.canPlaceBlockAt(world, px, py, pz) && world.getBlock(px, py - 1, pz) == Blocks.grass) {
                world.setBlock(px, py, pz, Blocks.melon_block, 0, 2);
            }
        }
        return true;
    }
}
