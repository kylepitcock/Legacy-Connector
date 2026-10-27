package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2's nether fire patches: fire on netherrack around the position. */
public class WorldGenFire extends WorldGenerator {
    public WorldGenFire() {
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        for (int i = 0; i < 64; ++i) {
            BlockPos p = position.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
            if (worldIn.isAirBlock(p) && mcc$modernAt(worldIn, p.down()).is(net.minecraft.world.level.block.Blocks.NETHERRACK)) mcc$setModern(worldIn, p, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
        }
        return true;
    }
}
