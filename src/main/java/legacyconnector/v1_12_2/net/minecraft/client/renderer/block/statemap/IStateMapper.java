package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap;

import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;

/** Maps each state of a block to the blockstate variant that models it. */
public interface IStateMapper {
    Map<IBlockState, ModelResourceLocation> putStateModelLocations(Block blockIn);
}
