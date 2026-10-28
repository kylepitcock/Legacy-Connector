package legacyconnector.v1_12_2.net.minecraft.world.chunk;

/** 1.12.2 placeholder chunk for unloaded positions: air everywhere, nothing stored. */
public class EmptyChunk extends Chunk {
    public EmptyChunk(legacyconnector.v1_12_2.net.minecraft.world.World worldIn, int x, int z) {
        super(worldIn, x, z);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState setBlockState(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isEmptyBetween(int startY, int endY) {
        return true;
    }
}
