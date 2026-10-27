package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's hanging glowstone cluster under netherrack. */
public class WorldGenGlowStone1 extends WorldGenerator {
    public WorldGenGlowStone1() {
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (!worldIn.isAirBlock(position) || !mcc$modernAt(worldIn, position.up()).is(net.minecraft.world.level.block.Blocks.NETHERRACK)) return false;
        mcc$setModern(worldIn, position, net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState());
        for (int i = 0; i < 1500; ++i) {
            BlockPos p = position.add(rand.nextInt(8) - rand.nextInt(8), -rand.nextInt(12), rand.nextInt(8) - rand.nextInt(8));
            if (!worldIn.isAirBlock(p)) continue;
            int n = 0;
            for (var f : legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.values()) {
                if (mcc$modernAt(worldIn, p.offset(f)).is(net.minecraft.world.level.block.Blocks.GLOWSTONE)) ++n;
                if (n > 1) break;
            }
            if (n == 1) mcc$setModern(worldIn, p, net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState());
        }
        return true;
    }
}
