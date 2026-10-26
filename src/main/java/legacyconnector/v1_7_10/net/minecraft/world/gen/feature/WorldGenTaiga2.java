package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The cone-shaped spruce of taigas: the modern spruce feature. */
public class WorldGenTaiga2 extends WorldGenAbstractTree {
    public WorldGenTaiga2(boolean notify) {
        super(notify);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return ModernFeature.place(world, "spruce", random, x, y, z);
    }
}
