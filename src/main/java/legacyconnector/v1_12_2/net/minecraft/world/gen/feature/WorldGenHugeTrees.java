package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Base of 1.12.2's 2x2 trees: height, room and soil checks, and leaf layers for mod trees built on it. */
public abstract class WorldGenHugeTrees extends WorldGenAbstractTree {
    protected final int baseHeight;
    protected final IBlockState woodMetadata;
    protected final IBlockState leavesMetadata;
    protected int extraRandomHeight;

    public WorldGenHugeTrees(boolean notify, int baseHeightIn, int extraRandomHeightIn, IBlockState woodMetadataIn, IBlockState leavesMetadataIn) {
        super(notify);
        this.baseHeight = baseHeightIn;
        this.extraRandomHeight = extraRandomHeightIn;
        this.woodMetadata = woodMetadataIn;
        this.leavesMetadata = leavesMetadataIn;
    }

    protected int getHeight(Random rand) {
        int i = rand.nextInt(3) + baseHeight;
        if (extraRandomHeight > 1) i += rand.nextInt(extraRandomHeight);
        return i;
    }

    private boolean isSpaceAt(World worldIn, BlockPos leavesPos, int height) {
        if (leavesPos.getY() < 1 || leavesPos.getY() + height + 1 > 256) return false;
        for (int i = 0; i <= 1 + height; ++i) {
            int j = i == 0 ? 1 : 2;
            for (int k = -j; k <= j; ++k) {
                for (int l = -j; l <= j; ++l) {
                    if (leavesPos.getY() + i < 0 || leavesPos.getY() + i >= 256 || !isReplaceable(worldIn, leavesPos.add(k, i, l))) return false;
                }
            }
        }
        return true;
    }

    private boolean ensureDirtsUnderneath(BlockPos pos, World worldIn) {
        BlockPos down = pos.down();
        if (!mcc$canSustain(worldIn, down) || pos.getY() < 2) return false;
        setDirtAt(worldIn, down);
        setDirtAt(worldIn, down.east());
        setDirtAt(worldIn, down.south());
        setDirtAt(worldIn, down.south().east());
        return true;
    }

    protected boolean ensureGrowable(World worldIn, Random rand, BlockPos treePos, int height) {
        return isSpaceAt(worldIn, treePos, height) && ensureDirtsUnderneath(treePos, worldIn);
    }

    private boolean leafAir(World world, BlockPos pos) {
        var s = mcc$modernAt(world, pos);
        return s.isAir() || s.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock;
    }

    protected void growLeavesLayerStrict(World worldIn, BlockPos layerCenter, int width) {
        int i = width * width;
        for (int j = -width; j <= width + 1; ++j) {
            for (int k = -width; k <= width + 1; ++k) {
                int l = j - 1;
                int i1 = k - 1;
                if (j * j + k * k <= i || l * l + i1 * i1 <= i || j * j + i1 * i1 <= i || l * l + k * k <= i) {
                    BlockPos p = layerCenter.add(j, 0, k);
                    if (leafAir(worldIn, p) && leavesMetadata != null) setBlockAndNotifyAdequately(worldIn, p, leavesMetadata);
                }
            }
        }
    }

    protected void growLeavesLayer(World worldIn, BlockPos layerCenter, int width) {
        int i = width * width;
        for (int j = -width; j <= width; ++j) {
            for (int k = -width; k <= width; ++k) {
                if (j * j + k * k <= i) {
                    BlockPos p = layerCenter.add(j, 0, k);
                    if (leafAir(worldIn, p) && leavesMetadata != null) setBlockAndNotifyAdequately(worldIn, p, leavesMetadata);
                }
            }
        }
    }
}
