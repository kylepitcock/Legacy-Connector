package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The forked acacia of savannas: the modern acacia feature. */
public class WorldGenSavannaTree extends WorldGenAbstractTree {
    public WorldGenSavannaTree(boolean notify) {
        super(notify);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return ModernFeature.place(world, "acacia", random, x, y, z);
    }
}
