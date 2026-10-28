package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.BlockBush;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 patch of a small plant (mushrooms): 64 tries around the position, where the plant can stay. */
public class WorldGenBush extends WorldGenerator {
    private final BlockBush block;

    public WorldGenBush(BlockBush blockIn) {
        super(false);
        this.block = blockIn;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (block == null) return false;
        for (int i = 0; i < 64; ++i) {
            BlockPos p = position.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
            if (worldInAir(worldIn, p) && p.getY() < 255 && block.canBlockStay(worldIn, p, block.getDefaultState())) {
                worldIn.setBlockState(p, block.getDefaultState(), 2);
            }
        }
        return true;
    }

    private static boolean worldInAir(World world, BlockPos p) {
        return world.isAirBlock(p);
    }
}
