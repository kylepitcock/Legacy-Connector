package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Lily pads on still water; the decorator's {@code waterlilyGen}. */
public class WorldGenWaterlily extends WorldGenerator {
    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        for (int i = 0; i < 10; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            if (world.isAirBlock(px, py, pz) && Blocks.waterlily.canPlaceBlockAt(world, px, py, pz)) world.setBlock(px, py, pz, Blocks.waterlily, 0, 2);
        }
        return true;
    }
}
