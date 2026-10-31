package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A cactus column on sand; the decorator's {@code cactusGen}. */
public class WorldGenCactus extends WorldGenerator {
    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        for (int i = 0; i < 10; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            if (!world.isAirBlock(px, py, pz)) continue;
            int h = 1 + random.nextInt(random.nextInt(3) + 1);
            for (int k = 0; k < h; k++) {
                var below = world.getBlock(px, py + k - 1, pz);
                if (k == 0 ? below == Blocks.sand : below == Blocks.cactus) world.setBlock(px, py + k, pz, Blocks.cactus, 0, 2);
            }
        }
        return true;
    }
}
