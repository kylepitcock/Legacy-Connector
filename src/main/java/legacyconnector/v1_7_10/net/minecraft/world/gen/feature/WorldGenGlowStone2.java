package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The second glowstone pass of the nether (the same hanging clusters, placed at any height). */
public class WorldGenGlowStone2 extends WorldGenerator {
    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return WorldGenGlowStone1.cluster(world, random, x, y, z);
    }
}
