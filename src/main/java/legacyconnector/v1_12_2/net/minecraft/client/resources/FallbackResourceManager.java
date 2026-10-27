package legacyconnector.v1_12_2.net.minecraft.client.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * The packs of one resource domain, newest last; a lookup takes the newest pack that has the file. Mods put their own into
 * {@code SimpleReloadableResourceManager.domainResourceManagers}; legacy lookups through the client's resource manager consult them first.
 */
public class FallbackResourceManager implements IResourceManager {
    protected final List<IResourcePack> resourcePacks = new ArrayList<>();
    private final MetadataSerializer frmMetadataSerializer;

    public FallbackResourceManager(MetadataSerializer serializer) {
        this.frmMetadataSerializer = serializer;
    }

    public void addResourcePack(IResourcePack pack) {
        resourcePacks.add(pack);
    }

    @Override
    public Set<String> getResourceDomains() {
        return java.util.Collections.emptySet();
    }

    @Override
    public IResource getResource(ResourceLocation location) throws IOException {
        checkResourcePath(location);
        IResourcePack metaPack = null;
        ResourceLocation meta = getLocationMcmeta(location);
        for (int i = resourcePacks.size() - 1; i >= 0; --i) {
            IResourcePack pack = resourcePacks.get(i);
            if (metaPack == null && pack.resourceExists(meta)) metaPack = pack;
            if (pack.resourceExists(location)) {
                InputStream mcmeta = metaPack == null ? null : getInputStream(meta, metaPack);
                return new SimpleResource(pack.getPackName(), location, getInputStream(location, pack), mcmeta, frmMetadataSerializer);
            }
        }
        throw new FileNotFoundException(location.toString());
    }

    protected InputStream getInputStream(ResourceLocation location, IResourcePack pack) throws IOException {
        return pack.getInputStream(location);
    }

    private void checkResourcePath(ResourceLocation location) throws IOException {
        if (location.getPath().contains("..")) throw new IOException("Invalid relative path to resource: " + location);
    }

    @Override
    public List<IResource> getAllResources(ResourceLocation location) throws IOException {
        checkResourcePath(location);
        List<IResource> out = new ArrayList<>();
        ResourceLocation meta = getLocationMcmeta(location);
        for (IResourcePack pack : resourcePacks) {
            if (pack.resourceExists(location)) {
                InputStream mcmeta = pack.resourceExists(meta) ? getInputStream(meta, pack) : null;
                out.add(new SimpleResource(pack.getPackName(), location, getInputStream(location, pack), mcmeta, frmMetadataSerializer));
            }
        }
        if (out.isEmpty()) throw new FileNotFoundException(location.toString());
        return out;
    }

    static ResourceLocation getLocationMcmeta(ResourceLocation location) {
        return new ResourceLocation(location.getNamespace(), location.getPath() + ".mcmeta");
    }
}
