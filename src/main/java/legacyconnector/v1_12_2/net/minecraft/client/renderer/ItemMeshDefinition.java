package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

public interface ItemMeshDefinition {
    ModelResourceLocation getModelLocation(ItemStack stack);
}
