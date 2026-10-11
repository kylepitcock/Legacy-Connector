package mccompat.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.init.Blocks;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.tags.BlockTags;

/** Base of legacy tree generators. */
public abstract class WorldGenAbstractTree extends WorldGenerator {
    public WorldGenAbstractTree(boolean notify) {
        super(notify);
    }

    /** Whether a tree may grow through this block (air, leaves, logs, saplings, grass/dirt and vines). */
    protected boolean func_150523_a(Block block) {
        if (block == null || block == Blocks.air) return true;
        var state = org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge.toModern(block, 0);
        return state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.SAPLINGS)
                || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || state.is(BlockTags.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.VINE);
    }

    /** Called after a successful generate (vanilla used it for cocoa and vines). */
    public void func_150524_b(World world, Random random, int x, int y, int z) {
    }

    protected boolean isReplaceable(World world, int x, int y, int z) {
        return func_150523_a(world.getBlock(x, y, z));
    }
}
