package mccompat.v1_7_10.net.minecraftforge.common;

import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.world.IBlockAccess;

/** A block (or seed item) that can be planted (Forge). */
public interface IPlantable {
    EnumPlantType getPlantType(IBlockAccess world, int x, int y, int z);

    Block getPlant(IBlockAccess world, int x, int y, int z);

    int getPlantMetadata(IBlockAccess world, int x, int y, int z);
}
