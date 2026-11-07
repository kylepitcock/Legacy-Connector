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

    /** Listeners told at the start of this reload (see {@link #mcc$reloading}); the notification queued when they registered must not repeat it. */
    private final Set<IResourceManagerReloadListener> toldBeforeStitch = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    @Override
    public void registerReloadListener(IResourceManagerReloadListener listener) {
        listeners.add(listener);
        notify(listener, true);
    }

    /**
     * Forge 1.12 mods registered their listeners before the game's own, so each reload told them before the models were loaded and the block atlas
     * stitched (their state is rebuilt by the stitch and model events that follow: Tinkers' Construct's texture generator clears itself here and
     * is filled by TextureStitchEvent.Pre). The generated resources are built at the start of a reload, where this runs; the listeners are told again
     * once the game has reloaded ({@link #mcc$reloaded}).
     */
    public void mcc$reloading() {
        for (var l : listeners) {
            synchronized (toldBeforeStitch) {
                toldBeforeStitch.add(l);
            }
            notify(l, false);
        }
    }

    /** Called after the game reloaded its resources. */
    public void mcc$reloaded() {
        synchronized (toldBeforeStitch) {
            toldBeforeStitch.clear();
        }
        for (var l : listeners) notify(l, false);
    }

    private void notify(IResourceManagerReloadListener listener, boolean registration) {
        Runnable run = () -> {
            if (registration) {
                synchronized (toldBeforeStitch) {
                    // the reload already told it, after which the state it rebuilds was filled: this stale call must not clear it
                    if (toldBeforeStitch.contains(listener)) return;
                }
            }
            try {
                listener.onResourceManagerReload(this);
            } catch (RuntimeException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Client").warn("A resource reload listener ({}) failed", listener.getClass().getName(), e);
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
            String path = location.getPath();
            // a vanilla texture under its 1.12 folder and name (minecraft:textures/blocks/gravel.png: Extra Utilities 2 reads them to draw its own)
            if (location.getNamespace().equals("minecraft") && path.endsWith(".png") && (path.startsWith("textures/blocks/") || path.startsWith("textures/items/"))) {
                path = "textures/" + org.kylepitcock.legacyconnector.v1_12_2.VanillaTextures1122.modern(path.substring("textures/".length(), path.length() - 4)) + ".png";
            }
            var id = net.minecraft.resources.ResourceLocation.tryBuild(location.getNamespace(), path);
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
            if (out.isEmpty() && id != null) {
                // while the resource packs are being built (a mod making sprites from vanilla textures), the game's manager has no packs yet
                var supplier = manager.getResourceStack(id).isEmpty() ? net.minecraft.client.Minecraft.getInstance().getVanillaPackResources()
                        .getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, id) : null;
                if (supplier != null) {
                    try (var in = supplier.get()) {
                        out.add(new Resource(location, in.readAllBytes(), false, "vanilla"));
                    }
                }
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
