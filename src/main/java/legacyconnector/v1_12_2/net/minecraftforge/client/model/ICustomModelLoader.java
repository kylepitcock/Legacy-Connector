package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManagerReloadListener;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** Forge 1.12.2 custom model loader: claims model locations and loads them as {@link IModel}s. */
public interface ICustomModelLoader extends IResourceManagerReloadListener {
    boolean accepts(ResourceLocation modelLocation);

    IModel loadModel(ResourceLocation modelLocation) throws Exception;
}
