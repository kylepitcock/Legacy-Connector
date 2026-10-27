package legacyconnector.v1_12_2.net.minecraftforge.client;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Forge's item model mesher: the model location an item stack was registered with (ModelLoader or a mesh definition). */
public class ItemModelMesherForge extends legacyconnector.v1_12_2.net.minecraft.client.renderer.ItemModelMesher {
    public ItemModelMesherForge(legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelManager manager) {
        super(manager);
    }

    /** The registered model location of the stack's item and metadata, or null (the item's default model then). */
    public ModelResourceLocation getLocation(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.mcc$model(stack.getItem(), getMetadata(stack));
    }
}
