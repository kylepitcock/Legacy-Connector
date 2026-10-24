package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The dungeon (monster room) feature. The modern overworld already generates dungeons; for dimensions that create one
 * through legacy generators this places nothing, and subclasses override {@link #generate}.
 */
public class WorldGenDungeons extends WorldGenerator {
    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return false;
    }
}
