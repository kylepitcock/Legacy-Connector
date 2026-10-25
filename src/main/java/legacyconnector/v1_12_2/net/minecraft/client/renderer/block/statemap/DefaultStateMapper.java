package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;

public class DefaultStateMapper extends StateMapperBase {
    @Override
    protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
        return new ModelResourceLocation(state.getBlock().getRegistryName().toString(), getPropertyString(state.getProperties()));
    }
}
