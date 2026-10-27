package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A block that falls when nothing is under it (sand, gravel): a modern falling block entity carries its state. */
public class BlockFalling extends Block {
    /** World generation sets this so falling blocks drop at once instead of becoming entities. */
    public static boolean fallInstantly;

    public BlockFalling() {
        super(Material.SAND);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
    }

    public BlockFalling(Material material) {
        super(material);
    }

    private void mcc$scheduleFall(World world, BlockPos pos) {
        var level = world == null ? null : world.mcc$level();
        if (level != null && mcc$modern instanceof net.minecraft.world.level.block.Block modern) level.scheduleTick(pos.mcc$modern(), modern, tickRate(world));
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        mcc$scheduleFall(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        mcc$scheduleFall(world, pos);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!world.isRemote) checkFallable(world, pos);
    }

    private void checkFallable(World world, BlockPos pos) {
        var level = world.mcc$level();
        var mp = pos.mcc$modern();
        var below = mp.below();
        if (!(level instanceof net.minecraft.server.level.ServerLevel server) || mp.getY() < level.getMinBuildHeight()) return;
        var belowState = level.getBlockState(below);
        if (!(belowState.isAir() || net.minecraft.world.level.block.FallingBlock.isFree(belowState))) return;
        var state = level.getBlockState(mp);
        if (fallInstantly || !level.isAreaLoaded(mp, 32)) {
            level.removeBlock(mp, false);
            var p = below;
            while ((level.getBlockState(p).isAir() || net.minecraft.world.level.block.FallingBlock.isFree(level.getBlockState(p))) && p.getY() > level.getMinBuildHeight()) {
                p = p.below();
            }
            if (p.getY() > level.getMinBuildHeight()) level.setBlockAndUpdate(p.above(), state);
            return;
        }
        net.minecraft.world.entity.item.FallingBlockEntity.fall(server, mp, state);
    }

    @Override
    public int tickRate(World world) {
        return 2;
    }

    /** Whether a falling block passes through the state (air, fire, liquids). */
    public static boolean canFallThrough(IBlockState state) {
        Material m = state.getMaterial();
        return m == Material.FIRE || m == Material.AIR || m == Material.WATER || m == Material.LAVA;
    }

    public void onEndFalling(World world, BlockPos pos, IBlockState fallingState, IBlockState hitState) {
    }

    public void onBroken(World world, BlockPos pos) {
    }

    public int getDustColor(IBlockState state) {
        return -16777216;
    }
}
