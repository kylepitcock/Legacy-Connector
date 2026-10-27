package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Base of 1.12.2 tree generators: what a tree may grow into, and the dirt under it. */
public abstract class WorldGenAbstractTree extends WorldGenerator {
    public WorldGenAbstractTree(boolean notify) {
        super(notify);
    }

    /** A modern state a trunk or leaves may replace (air, leaves, logs, saplings, vines, grass and dirt, plants). */
    static boolean growsInto(net.minecraft.world.level.block.state.BlockState s) {
        if (s.isAir() || s.canBeReplaced()) return true;
        var b = s.getBlock();
        return b instanceof net.minecraft.world.level.block.LeavesBlock || b instanceof net.minecraft.world.level.block.RotatedPillarBlock && s.is(net.minecraft.tags.BlockTags.LOGS)
                || b instanceof net.minecraft.world.level.block.SaplingBlock || b instanceof net.minecraft.world.level.block.VineBlock
                || s.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || s.is(net.minecraft.world.level.block.Blocks.DIRT);
    }

    protected boolean canGrowInto(Block blockType) {
        if (blockType == null) return true;
        if (blockType.mcc$modern instanceof net.minecraft.world.level.block.Block modern && !(modern instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block)) {
            return growsInto(modern.defaultBlockState());
        }
        IBlockState state = blockType.getDefaultState();
        Material m = state == null ? null : state.getMaterial();
        return m == Material.AIR || m == Material.LEAVES || m == Material.VINE || m == Material.PLANTS;
    }

    public void generateSaplings(World worldIn, Random random, BlockPos pos) {
    }

    /** Dirt under the trunk (grass and podzol become dirt, as 1.12 did). */
    protected void setDirtAt(World worldIn, BlockPos pos) {
        var s = mcc$modernAt(worldIn, pos);
        if (!s.is(net.minecraft.world.level.block.Blocks.DIRT)) mcc$setModern(worldIn, pos, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
    }

    public boolean isReplaceable(World world, BlockPos pos) {
        var s = mcc$modernAt(world, pos);
        if (s.getBlock() instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block) {
            IBlockState legacy = world.getBlockState(pos);
            return legacy == null || canGrowInto(legacy.getBlock());
        }
        return growsInto(s);
    }

    /** Whether the block under a tree can hold it (grass, dirt, podzol, farmland; a mod's soil by its legacy material). */
    protected boolean mcc$canSustain(World world, BlockPos pos) {
        var s = mcc$modernAt(world, pos);
        if (s.is(net.minecraft.tags.BlockTags.DIRT) || s.is(net.minecraft.world.level.block.Blocks.FARMLAND)) return true;
        if (s.getBlock() instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block) {
            IBlockState legacy = world.getBlockState(pos);
            return legacy != null && (legacy.getMaterial() == Material.GRASS || legacy.getMaterial() == Material.GROUND);
        }
        return false;
    }

    /** The trunk or leaves, placed so that modern leaves do not decay before their distance is computed. */
    @Override
    protected void setBlockAndNotifyAdequately(World worldIn, BlockPos pos, IBlockState state) {
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state);
        if (modern.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock && modern.hasProperty(net.minecraft.world.level.block.LeavesBlock.DISTANCE)) {
            mcc$setModern(worldIn, pos, modern.setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, 1));
            return;
        }
        super.setBlockAndNotifyAdequately(worldIn, pos, state);
    }
}
