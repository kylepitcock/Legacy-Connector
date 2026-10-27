package legacyconnector.v1_12_2.net.minecraft.client.resources;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** Access to resource packs: the files of the mod jars and, on the client, the game's loaded packs. */
public interface IResourceManager {
    java.util.Set<String> getResourceDomains();

    IResource getResource(ResourceLocation location) throws java.io.IOException;

    java.util.List<IResource> getAllResources(ResourceLocation location) throws java.io.IOException;
}
