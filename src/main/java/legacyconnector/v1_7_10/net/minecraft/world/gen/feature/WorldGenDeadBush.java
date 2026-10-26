package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A few dead bushes (or the given plant) on the ground below the origin. */
public class WorldGenDeadBush extends WorldGenerator {
    private final Block field_150547_a;

    public WorldGenDeadBush(Block plant) {
        this.field_150547_a = plant;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        while (y > 0 && LegacyFeatures.airOrLeaves(world, x, y, z)) y--;
        for (int i = 0; i < 4; i++) {
            int px = x + random.nextInt(8) - random.nextInt(8);
            int py = y + random.nextInt(4) - random.nextInt(4);
            int pz = z + random.nextInt(8) - random.nextInt(8);
            if (world.isAirBlock(px, py, pz) && field_150547_a.canBlockStay(world, px, py, pz)) world.setBlock(px, py, pz, field_150547_a, 0, 2);
        }
        return true;
    }
}
