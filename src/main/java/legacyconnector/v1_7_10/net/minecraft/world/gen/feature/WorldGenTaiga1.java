package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The tall, bare-trunked taiga pine: the modern pine feature. */
public class WorldGenTaiga1 extends WorldGenAbstractTree {
    public WorldGenTaiga1() {
        super(false);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return ModernFeature.place(world, "pine", random, x, y, z);
    }
}
