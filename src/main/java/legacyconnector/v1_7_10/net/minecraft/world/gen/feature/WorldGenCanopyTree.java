package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The 2x2 dark oak of roofed forests: the modern dark oak feature. */
public class WorldGenCanopyTree extends WorldGenAbstractTree {
    public WorldGenCanopyTree(boolean notify) {
        super(notify);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return ModernFeature.place(world, "dark_oak", random, x, y, z);
    }
}
