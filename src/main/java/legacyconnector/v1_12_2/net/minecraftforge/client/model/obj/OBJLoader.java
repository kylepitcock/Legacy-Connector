package legacyconnector.v1_12_2.net.minecraftforge.client.model.obj;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResource;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.ICustomModelLoader;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.IModel;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoaderRegistry;

/**
 * Forge 1.12.2 OBJ loader: loads {@code <domain>:models/<path>.obj} for the domains a mod enabled with {@link #addDomain}, as
 * {@link OBJModel}s (cached until the next resource reload). It registers itself with {@link ModelLoaderRegistry} on first use.
 */
public enum OBJLoader implements ICustomModelLoader {
    INSTANCE;

    private final Set<String> enabledDomains = new HashSet<>();
    private final Map<ResourceLocation, OBJModel> cache = new HashMap<>();
    private final Map<ResourceLocation, Exception> errors = new HashMap<>();
    private boolean registered;

    public synchronized void addDomain(String domain) {
        enabledDomains.add(domain.toLowerCase(java.util.Locale.ROOT));
        if (!registered) {
            registered = true;
            ModelLoaderRegistry.registerLoader(this);
        }
    }

    @Override
    public synchronized void onResourceManagerReload(IResourceManager resourceManager) {
        cache.clear();
        errors.clear();
    }

    @Override
    public synchronized boolean accepts(ResourceLocation modelLocation) {
        return enabledDomains.contains(modelLocation.getNamespace()) && modelLocation.getPath().endsWith(".obj");
    }

    @Override
    public synchronized IModel loadModel(ResourceLocation modelLocation) throws Exception {
        ResourceLocation file = new ResourceLocation(modelLocation.getNamespace(), modelLocation.getPath());
        OBJModel cached = cache.get(file);
        if (cached != null) return cached;
        Exception previous = errors.get(file);
        if (previous != null) throw previous;
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        String path = file.getPath().startsWith("models/") ? file.getPath() : "models/" + file.getPath();
        try (IResource resource = manager.getResource(new ResourceLocation(file.getNamespace(), path))) {
            OBJModel model = new OBJModel.Parser(resource, manager).parse();
            cache.put(file, model);
            return model;
        } catch (Exception e) {
            errors.put(file, e);
            throw new ModelLoaderRegistry.LoaderException("Could not load the OBJ model " + modelLocation, e);
        }
    }
}
