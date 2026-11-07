package legacyconnector.v1_20_1.net.minecraftforge.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Forge 1.20.1's plantable marker (crops, saplings, flowers); removed in NeoForge 1.21.1, kept so the classes that implement it load. */
public interface IPlantable {
    default PlantType getPlantType(BlockGetter level, BlockPos pos) {
        return PlantType.PLAINS;
    }

    BlockState getPlant(BlockGetter level, BlockPos pos);
}
