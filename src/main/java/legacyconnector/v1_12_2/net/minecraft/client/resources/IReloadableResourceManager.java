package legacyconnector.v1_12_2.net.minecraft.client.resources;

public interface IReloadableResourceManager extends IResourceManager {
    void registerReloadListener(IResourceManagerReloadListener reloadListener);
}
