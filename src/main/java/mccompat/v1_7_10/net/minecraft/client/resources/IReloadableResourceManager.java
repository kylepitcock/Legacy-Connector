package mccompat.v1_7_10.net.minecraft.client.resources;

import java.util.List;

public interface IReloadableResourceManager extends IResourceManager {
    void reloadResources(List resourcePacks);

    void registerReloadListener(IResourceManagerReloadListener listener);
}
