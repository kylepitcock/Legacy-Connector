package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Patches of fire on netherrack (64 attempts around the origin). */
public class WorldGenFire extends WorldGenerator {
    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        for (int i = 0; i < 64; i++) {
            int fx = x + random.nextInt(8) - random.nextInt(8);
            int fy = y + random.nextInt(4) - random.nextInt(4);
            int fz = z + random.nextInt(8) - random.nextInt(8);
            if (world.isAirBlock(fx, fy, fz) && world.getBlock(fx, fy - 1, fz) == Blocks.netherrack) world.setBlock(fx, fy, fz, Blocks.fire, 0, 2);
        }
        return true;
    }
}
