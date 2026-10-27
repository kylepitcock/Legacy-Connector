package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2's small tree (the same shape from the same random numbers), with the mod's own wood and leaves when given; the default
 * is an oak. Vines and cocoa of jungle trees are not grown.
 */
public class WorldGenTrees extends WorldGenAbstractTree {
    private final int minTreeHeight;
    private final boolean vinesGrow;
    private final IBlockState metaWood;
    private final IBlockState metaLeaves;

    public WorldGenTrees(boolean notify) {
        this(notify, 4, null, null, false);
    }

    public WorldGenTrees(boolean notify, int minTreeHeightIn, IBlockState woodMeta, IBlockState leavesMeta, boolean growVines) {
        super(notify);
        this.minTreeHeight = minTreeHeightIn;
        this.metaWood = woodMeta;
        this.metaLeaves = leavesMeta;
        this.vinesGrow = growVines;
    }

    private void placeWood(World world, BlockPos pos) {
        if (metaWood == null) mcc$setModern(world, pos, net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());
        else setBlockAndNotifyAdequately(world, pos, metaWood);
    }

    private void placeLeaves(World world, BlockPos pos) {
        if (metaLeaves == null) {
            mcc$setModern(world, pos, net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, 1));
        } else {
            setBlockAndNotifyAdequately(world, pos, metaLeaves);
        }
    }

    private boolean openForLeaves(World world, BlockPos pos) {
        var s = mcc$modernAt(world, pos);
        return s.isAir() || s.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock || s.getBlock() instanceof net.minecraft.world.level.block.VineBlock
                || s.getBlock() instanceof org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block && world.getBlockState(pos).getMaterial()
                        == legacyconnector.v1_12_2.net.minecraft.block.material.Material.LEAVES;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        int i = rand.nextInt(3) + minTreeHeight;
        boolean flag = true;
        int top = worldIn.getHeight();
        if (position.getY() < 1 || position.getY() + i + 1 > top) return false;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int j = position.getY(); j <= position.getY() + 1 + i; ++j) {
            int k = 1;
            if (j == position.getY()) k = 0;
            if (j >= position.getY() + 1 + i - 2) k = 2;
            for (int l = position.getX() - k; l <= position.getX() + k && flag; ++l) {
                for (int i1 = position.getZ() - k; i1 <= position.getZ() + k && flag; ++i1) {
                    if (j >= 0 && j < top) {
                        if (!isReplaceable(worldIn, m.setPos(l, j, i1))) flag = false;
                    } else {
                        flag = false;
                    }
                }
            }
        }
        if (!flag) return false;
        BlockPos down = position.down();
        if (!mcc$canSustain(worldIn, down) || position.getY() >= top - i - 1) return false;
        setDirtAt(worldIn, down);
        for (int i3 = position.getY() - 3 + i; i3 <= position.getY() + i; ++i3) {
            int i4 = i3 - (position.getY() + i);
            int j1 = 1 - i4 / 2;
            for (int k1 = position.getX() - j1; k1 <= position.getX() + j1; ++k1) {
                int l1 = k1 - position.getX();
                for (int i2 = position.getZ() - j1; i2 <= position.getZ() + j1; ++i2) {
                    int j2 = i2 - position.getZ();
                    if (Math.abs(l1) != j1 || Math.abs(j2) != j1 || rand.nextInt(2) != 0 && i4 != 0) {
                        BlockPos leaf = new BlockPos(k1, i3, i2);
                        if (openForLeaves(worldIn, leaf)) placeLeaves(worldIn, leaf);
                    }
                }
            }
        }
        for (int j3 = 0; j3 < i; ++j3) {
            BlockPos up = position.up(j3);
            if (openForLeaves(worldIn, up)) placeWood(worldIn, up);
        }
        return true;
    }
}
