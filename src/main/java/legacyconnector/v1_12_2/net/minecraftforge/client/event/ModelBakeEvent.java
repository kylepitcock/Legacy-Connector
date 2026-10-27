package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.registry.IRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired after the game baked its models: the registry shows them under their legacy locations, and a model put there replaces the
 * game's model of every modern state or item subtype the location stands for.
 */
public class ModelBakeEvent extends Event {
    private final ModelManager modelManager;
    private final IRegistry<ModelResourceLocation, IBakedModel> modelRegistry;
    private final ModelLoader modelLoader;

    public ModelBakeEvent(ModelManager modelManager, IRegistry<ModelResourceLocation, IBakedModel> modelRegistry, ModelLoader modelLoader) {
        this.modelManager = modelManager;
        this.modelRegistry = modelRegistry;
        this.modelLoader = modelLoader;
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    public IRegistry<ModelResourceLocation, IBakedModel> getModelRegistry() {
        return modelRegistry;
    }

    public ModelLoader getModelLoader() {
        return modelLoader;
    }
}
