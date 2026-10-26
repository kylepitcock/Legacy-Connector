package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * 1.7.10's resource manager class (mods cast the game's manager to it to reach {@code domainResourceManagers}).
 * The game's own manager ({@code LegacySprites.ModResources}) extends it and answers the lookups.
 */
public class SimpleReloadableResourceManager implements IReloadableResourceManager {
    /** Domain -> per-domain manager; mods inspect or extend it by reflection. */
    public Map domainResourceManagers = new HashMap();
    protected final List<IResourceManagerReloadListener> reloadListeners = new ArrayList<>();

    @Override
    public Set getResourceDomains() {
        return domainResourceManagers.keySet();
    }

    @Override
    public IResource getResource(ResourceLocation location) throws IOException {
        throw new java.io.FileNotFoundException(location.toString());
    }

    @Override
    public List getAllResources(ResourceLocation location) throws IOException {
        return List.of(getResource(location));
    }

    protected final java.util.Set setResourceDomains = new java.util.LinkedHashSet();

    /**
     * Puts a pack on top of its domains' managers: legacy lookups find its resources first, and the game is served them
     * (from the next resource reload for domains it did not have).
     */
    public void reloadResourcePack(legacyconnector.v1_7_10.net.minecraft.client.resources.IResourcePack pack) {
        if (pack == null) return;
        Set domains = pack.getResourceDomains();
        if (domains == null || domains.isEmpty()) {
            org.kylepitcock.legacyconnector.v1_7_10.client.LegacyResourcePacks.add(pack);
            return;
        }
        for (Object o : domains) {
            if (!(o instanceof String domain)) continue;
            setResourceDomains.add(domain);
            FallbackResourceManager fallback = domainResourceManagers.get(domain) instanceof FallbackResourceManager f ? f : null;
            if (fallback == null) {
                fallback = new FallbackResourceManager(new legacyconnector.v1_7_10.net.minecraft.client.resources.data.IMetadataSerializer());
                domainResourceManagers.put(domain, fallback);
            }
            fallback.addResourcePack(pack);
        }
    }

    /** Forgets the domain managers (the modern game owns resource loading; mods call this before reloading). */
    private void clearResources() {
        domainResourceManagers.clear();
    }

    @Override
    public void reloadResources(List resourcePacks) {
    }

    @Override
    public void registerReloadListener(IResourceManagerReloadListener listener) {
        reloadListeners.add(listener);
        listener.onResourceManagerReload(this);
    }
}
