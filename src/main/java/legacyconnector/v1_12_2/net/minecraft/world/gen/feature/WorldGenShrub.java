package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's jungle bush: one log on the ground with a mound of leaves. */
public class WorldGenShrub extends WorldGenTrees {
    private final IBlockState leavesMetadata;
    private final IBlockState woodMetadata;

    public WorldGenShrub(IBlockState wood, IBlockState leaves) {
        super(false);
        this.woodMetadata = wood;
        this.leavesMetadata = leaves;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        while (position.getY() > 0) {
            var s = mcc$modernAt(worldIn, position);
            if (!s.isAir() && !(s.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock)) break;
            position = position.down();
        }
        if (!mcc$canSustain(worldIn, position)) return true;
        position = position.up();
        setBlockAndNotifyAdequately(worldIn, position, woodMetadata);
        for (int y = position.getY(); y <= position.getY() + 2; ++y) {
            int dy = y - position.getY();
            int k = 2 - dy;
            for (int x = position.getX() - k; x <= position.getX() + k; ++x) {
                int dx = x - position.getX();
                for (int z = position.getZ() - k; z <= position.getZ() + k; ++z) {
                    int dz = z - position.getZ();
                    if (Math.abs(dx) != k || Math.abs(dz) != k || rand.nextInt(2) != 0) {
                        BlockPos p = new BlockPos(x, y, z);
                        var s = mcc$modernAt(worldIn, p);
                        if (s.isAir() || s.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) setBlockAndNotifyAdequately(worldIn, p, leavesMetadata);
                    }
                }
            }
        }
        return true;
    }
}
