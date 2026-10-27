package legacyconnector.v1_12_2.net.minecraftforge.common;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** Forge: a plant (block or seed item) that soil blocks are asked to sustain ({@code Block.canSustainPlant}). */
public interface IPlantable {
    EnumPlantType getPlantType(IBlockAccess world, BlockPos pos);

    IBlockState getPlant(IBlockAccess world, BlockPos pos);
}
