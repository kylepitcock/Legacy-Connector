package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;

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

    /** 1.4.7/1.5.2 {@code setBlock(world, x, y, z, blockId)}: places the block of that numeric id (id 0 clears), metadata 0. */
    protected void setBlock(World world, int x, int y, int z, int id) {
        Block block = id <= 0 ? legacyconnector.v1_7_10.net.minecraft.init.Blocks.air : legacyconnector.v1_6_4.net.minecraft.util.Ids.block(id);
        if (block == null) block = legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
        setBlockAndNotifyAdequately(world, x, y, z, block, 0);
    }

    protected void setBlockAndNotifyAdequately(World world, int x, int y, int z, Block block, int meta) {
        world.setBlock(x, y, z, block, meta, doBlockNotify ? 3 : 2);
    }
}
