package legacyconnector.v1_12_2.net.minecraftforge.common;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** Something that grows on a block (crops, saplings, flowers): the block it places and the kind of soil it needs. */
public interface IPlantable {
    default EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
        return EnumPlantType.Plains;
    }

    IBlockState getPlant(IBlockAccess world, BlockPos pos);
}
