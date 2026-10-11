package mccompat.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Big oak: the modern fancy oak feature. */
public class WorldGenBigTree extends WorldGenAbstractTree {
    public WorldGenBigTree(boolean notify) {
        super(notify);
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        return ModernFeature.place(world, "fancy_oak", random, x, y, z);
    }
}
