package legacyconnector.v1_12_2.net.minecraft.client.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * The client's resource manager as 1.12 mods see it. Resources are looked up in the mod files first (legacy paths are case sensitive)
 * and then in the game's loaded packs. Reload listeners are told when they register and again on every resource reload, on the render
 * thread (mods upload textures from them).
 */
public class SimpleReloadableResourceManager implements IReloadableResourceManager {
    private final List<IResourceManagerReloadListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Per-domain pack stacks mods add their own packs to; lookups ask them first. */
    public final java.util.Map<String, FallbackResourceManager> domainResourceManagers = new java.util.concurrent.ConcurrentHashMap<>();
    public final legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer rmMetadataSerializer;

    public SimpleReloadableResourceManager() {
        this(new legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer());
    }

    public SimpleReloadableResourceManager(legacyconnector.v1_12_2.net.minecraft.client.resources.data.MetadataSerializer serializer) {
        this.rmMetadataSerializer = serializer;
    }

    /** Whether a pack was added for one of its domains (not a 1.12 method). */
    public boolean mcc$has(IResourcePack pack) {
        for (FallbackResourceManager m : domainResourceManagers.values()) if (m.resourcePacks.contains(pack)) return true;
        return false;
    }

    /** Adds a pack for its domains (what 1.12 did on a resource reload; mods call it to add a pack of their own). */
    public void reloadResourcePack(IResourcePack pack) {
        if (pack == null) return;
        for (String domain : pack.getResourceDomains()) {
            domainResourceManagers.computeIfAbsent(domain, d -> new FallbackResourceManager(rmMetadataSerializer)).addResourcePack(pack);
        }
    }

    @Override
    public void registerReloadListener(IResourceManagerReloadListener listener) {
        listeners.add(listener);
        notify(listener);
    }

    /** Called after the game reloaded its resources. */
    public void mcc$reloaded() {
        for (var l : listeners) notify(l);
    }

    private void notify(IResourceManagerReloadListener listener) {
        Runnable run = () -> {
            try {
                listener.onResourceManagerReload(this);
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Client").warn("A resource reload listener failed: {}", e.toString());
            }
        };
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient() || com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) run.run();
        else com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(run::run);
    }

    @Override
    public Set<String> getResourceDomains() {
        Set<String> out = new java.util.TreeSet<>();
        for (var info : net.neoforged.fml.ModList.get().getMods()) out.add(info.getModId());
        out.add("minecraft");
        return out;
    }

    @Override
    public IResource getResource(ResourceLocation location) throws IOException {
        FallbackResourceManager added = domainResourceManagers.get(location.getNamespace());
        if (added != null) {
            try {
                return added.getResource(location);
            } catch (IOException e) {
                // not in the packs mods added
            }
        }
        List<IResource> all = find(location, true);
        if (all.isEmpty()) throw new java.io.FileNotFoundException(location.toString());
        return all.get(0);
    }

    @Override
    public List<IResource> getAllResources(ResourceLocation location) throws IOException {
        List<IResource> all = find(location, false);
        FallbackResourceManager added = domainResourceManagers.get(location.getNamespace());
        if (added != null) {
            try {
                all.addAll(added.getAllResources(location));
            } catch (IOException e) {
                // not in the packs mods added
            }
        }
        if (all.isEmpty()) throw new java.io.FileNotFoundException(location.toString());
        return all;
    }

    private static List<IResource> find(ResourceLocation location, boolean first) throws IOException {
        List<IResource> out = new ArrayList<>();
        String ns = location.getNamespace();
        String raw = location.getPath();
        String lower = org.kylepitcock.legacyconnector.core.Ids.normalizePath(raw);
        for (var file : net.neoforged.fml.ModList.get().getModFiles()) {
            Path p = file.getFile().findResource("assets", ns, raw);
            if (p == null || !Files.isRegularFile(p)) p = file.getFile().findResource("assets", ns, lower);
            if (p != null && Files.isRegularFile(p)) {
                Path meta = p.resolveSibling(p.getFileName() + ".mcmeta");
                out.add(new Resource(location, Files.readAllBytes(p), Files.isRegularFile(meta), file.getFile().getFileName()));
                if (first) return out;
            }
        }
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) Client.find(location, out, first);
        return out;
    }

    /** The game's own packs (vanilla assets, resource packs), client only. */
    private static final class Client {
        static void find(ResourceLocation location, List<IResource> out, boolean first) throws IOException {
            var manager = net.minecraft.client.Minecraft.getInstance().getResourceManager();
            var id = net.minecraft.resources.ResourceLocation.tryBuild(location.getNamespace(), location.getPath());
            if (id == null) id = location.mcc$modern();
            for (var r : manager.getResourceStack(id)) {
                byte[] bytes;
                try (var in = r.open()) {
                    bytes = in.readAllBytes();
                }
                boolean meta;
                try {
                    meta = r.metadata() != net.minecraft.server.packs.resources.ResourceMetadata.EMPTY;
                } catch (IOException e) {
                    meta = false;
                }
                out.add(new Resource(location, bytes, meta, r.sourcePackId()));
                if (first) return;
            }
        }
    }

    private record Resource(ResourceLocation location, byte[] bytes, boolean meta, String pack) implements IResource {
        @Override
        public ResourceLocation getResourceLocation() {
            return location;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public boolean hasMetadata() {
            return meta;
        }

        @Override
        public String getResourcePackName() {
            return pack;
        }

        @Override
        public void close() {
        }
    }
}
