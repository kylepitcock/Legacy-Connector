package legacyconnector.v1_12_2.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.BlockFlower;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 flower patch of one flower type: 64 tries around the position, where the flower can stay. */
public class WorldGenFlowers extends WorldGenerator {
    private BlockFlower flower;
    private IBlockState state;

    public WorldGenFlowers(BlockFlower flowerIn, BlockFlower.EnumFlowerType type) {
        super(false);
        this.setGeneratedBlock(flowerIn, type);
    }

    public void setGeneratedBlock(BlockFlower flowerIn, BlockFlower.EnumFlowerType typeIn) {
        this.flower = flowerIn;
        if (flowerIn == null) {
            this.state = null;
            return;
        }
        try {
            this.state = flowerIn.getDefaultState().withProperty(flowerIn.getTypeProperty(), typeIn);
        } catch (RuntimeException e) {
            this.state = flowerIn.getDefaultState();
        }
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        if (flower == null || state == null) return false;
        for (int i = 0; i < 64; ++i) {
            BlockPos p = position.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
            if (worldIn.isAirBlock(p) && p.getY() < 255 && flower.canBlockStay(worldIn, p, state)) {
                worldIn.setBlockState(p, state, 2);
            }
        }
        return true;
    }
}
