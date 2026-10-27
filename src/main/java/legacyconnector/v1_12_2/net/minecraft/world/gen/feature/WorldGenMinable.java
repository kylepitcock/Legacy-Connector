package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import com.google.common.base.Predicate;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2's ore vein (the same shape from the same random numbers). The default target is stone: the modern overworld's stone,
 * granite, diorite, andesite and tuff (1.12's stone variants) and deepslate.
 */
public class WorldGenMinable extends WorldGenerator {
    private final IBlockState oreBlock;
    private final int numberOfBlocks;
    private final Predicate<IBlockState> predicate;

    public WorldGenMinable(IBlockState state, int blockCount) {
        this(state, blockCount, null);
    }

    public WorldGenMinable(IBlockState state, int blockCount, Predicate<IBlockState> p_i45631_3_) {
        this.oreBlock = state;
        this.numberOfBlocks = blockCount;
        this.predicate = p_i45631_3_;
    }

    private boolean replaceable(World world, BlockPos pos) {
        var modern = mcc$modernAt(world, pos);
        if (predicate == null) return isStone(modern);
        IBlockState state = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(modern);
        if (state != null && predicate.apply(state)) return true;
        // a "replace stone" predicate also replaces the modern stone variants and deepslate
        if (isStone(modern) && state != null && !modern.is(net.minecraft.world.level.block.Blocks.STONE)) {
            IBlockState stone = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            return stone != null && predicate.apply(stone);
        }
        return false;
    }

    private static boolean isStone(net.minecraft.world.level.block.state.BlockState s) {
        return s.is(net.minecraft.world.level.block.Blocks.STONE) || s.is(net.minecraft.world.level.block.Blocks.GRANITE) || s.is(net.minecraft.world.level.block.Blocks.DIORITE)
                || s.is(net.minecraft.world.level.block.Blocks.ANDESITE) || s.is(net.minecraft.world.level.block.Blocks.TUFF)
                || s.is(net.minecraft.world.level.block.Blocks.DEEPSLATE);
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        float f = rand.nextFloat() * (float) Math.PI;
        double d0 = position.getX() + 8 + MathHelper.sin(f) * numberOfBlocks / 8.0F;
        double d1 = position.getX() + 8 - MathHelper.sin(f) * numberOfBlocks / 8.0F;
        double d2 = position.getZ() + 8 + MathHelper.cos(f) * numberOfBlocks / 8.0F;
        double d3 = position.getZ() + 8 - MathHelper.cos(f) * numberOfBlocks / 8.0F;
        double d4 = position.getY() + rand.nextInt(3) - 2;
        double d5 = position.getY() + rand.nextInt(3) - 2;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < numberOfBlocks; ++i) {
            float f1 = (float) i / (float) numberOfBlocks;
            double d6 = d0 + (d1 - d0) * f1;
            double d7 = d4 + (d5 - d4) * f1;
            double d8 = d2 + (d3 - d2) * f1;
            double d9 = rand.nextDouble() * numberOfBlocks / 16.0D;
            double d10 = (MathHelper.sin((float) Math.PI * f1) + 1.0F) * d9 + 1.0D;
            double d11 = (MathHelper.sin((float) Math.PI * f1) + 1.0F) * d9 + 1.0D;
            int j = MathHelper.floor(d6 - d10 / 2.0D);
            int k = MathHelper.floor(d7 - d11 / 2.0D);
            int l = MathHelper.floor(d8 - d10 / 2.0D);
            int i1 = MathHelper.floor(d6 + d10 / 2.0D);
            int j1 = MathHelper.floor(d7 + d11 / 2.0D);
            int k1 = MathHelper.floor(d8 + d10 / 2.0D);
            for (int l1 = j; l1 <= i1; ++l1) {
                double d12 = (l1 + 0.5D - d6) / (d10 / 2.0D);
                if (d12 * d12 >= 1.0D) continue;
                for (int i2 = k; i2 <= j1; ++i2) {
                    double d13 = (i2 + 0.5D - d7) / (d11 / 2.0D);
                    if (d12 * d12 + d13 * d13 >= 1.0D) continue;
                    for (int j2 = l; j2 <= k1; ++j2) {
                        double d14 = (j2 + 0.5D - d8) / (d10 / 2.0D);
                        if (d12 * d12 + d13 * d13 + d14 * d14 < 1.0D) {
                            p.setPos(l1, i2, j2);
                            if (replaceable(worldIn, p)) worldIn.setBlockState(p.toImmutable(), oreBlock, 2);
                        }
                    }
                }
            }
        }
        return true;
    }
}
