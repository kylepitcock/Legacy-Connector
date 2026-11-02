package legacyconnector.v1_12_2.net.minecraft.world.chunk;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;

public interface IBlockStatePaletteResizer {
    int onResize(int bits, IBlockState state);
}
