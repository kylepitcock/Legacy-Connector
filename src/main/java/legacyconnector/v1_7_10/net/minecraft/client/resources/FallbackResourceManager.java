package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.client.resources.data.IMetadataSerializer;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyResourcePacks;

/**
 * The packs of one resource domain, newest last: a lookup takes the newest pack that has the resource. Packs added here are
 * also served to the game (mods add their own sound or texture folders to a domain's manager).
 */
public class FallbackResourceManager implements IResourceManager {
    protected final List resourcePacks = new ArrayList();
    private final IMetadataSerializer frmMetadataSerializer;

    public FallbackResourceManager(IMetadataSerializer serializer) {
        this.frmMetadataSerializer = serializer;
    }

    public void addResourcePack(IResourcePack pack) {
        resourcePacks.add(pack);
        LegacyResourcePacks.add(pack);
    }

    @Override
    public Set getResourceDomains() {
        return null;
    }

    @Override
    public IResource getResource(ResourceLocation location) throws IOException {
        for (int i = resourcePacks.size() - 1; i >= 0; i--) {
            IResourcePack pack = (IResourcePack) resourcePacks.get(i);
            if (pack.resourceExists(location)) return LegacyResourcePacks.resource(pack, location);
        }
        throw new FileNotFoundException(location.toString());
    }

    @Override
    public List getAllResources(ResourceLocation location) throws IOException {
        List out = new ArrayList();
        for (Object o : resourcePacks) {
            IResourcePack pack = (IResourcePack) o;
            if (pack.resourceExists(location)) out.add(LegacyResourcePacks.resource(pack, location));
        }
        if (out.isEmpty()) throw new FileNotFoundException(location.toString());
        return out;
    }

    static ResourceLocation getLocationMcmeta(ResourceLocation location) {
        return new ResourceLocation(location.getResourceDomain(), location.getResourcePath() + ".mcmeta");
    }
}
