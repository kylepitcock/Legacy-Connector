package mccompat.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.world.World;

/** Vanilla 1.7.10 feature generator base (ores, trees, structures placed by mod world generators). */
public abstract class WorldGenerator {
    private final boolean doBlockNotify;

    public WorldGenerator() {
        this(false);
    }

    public WorldGenerator(boolean notify) {
        this.doBlockNotify = notify;
    }

    public abstract boolean generate(World world, Random random, int x, int y, int z);

    public void setScale(double x, double y, double z) {
    }

    protected void func_150515_a(World world, int x, int y, int z, Block block) {
        setBlockAndNotifyAdequately(world, x, y, z, block, 0);
    }

    protected void setBlockAndNotifyAdequately(World world, int x, int y, int z, Block block, int meta) {
        world.setBlock(x, y, z, block, meta, doBlockNotify ? 3 : 2);
    }
}
