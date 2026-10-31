package legacyconnector.v1_12_2.net.minecraft.client.renderer.texture;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManagerReloadListener;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyGlTexture1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations;

/**
 * 1.12.2 texture manager. Binding a location makes it the texture of the render context (modern draws name their texture); the
 * block atlas binds the modern block atlas, vanilla textures their 1.21 files. Texture objects a mod registers become modern textures
 * under their location ({@link LegacyGlTexture1122}; dynamic textures as modern dynamic textures).
 */
public class TextureManager implements ITickable, IResourceManagerReloadListener {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Textures");
    public static final ResourceLocation RESOURCE_LOCATION_EMPTY = new ResourceLocation("");

    private final Map<ResourceLocation, ITextureObject> mapTextureObjects = new HashMap<>();
    private final Map<String, Integer> mapTextureCounters = new HashMap<>();
    private final IResourceManager resourceManager;
    /** Texture objects registered before the client's texture manager existed or off the render thread. */
    private final Map<ResourceLocation, ITextureObject> pending = new LinkedHashMap<>();

    public TextureManager(IResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /** The client's texture manager (Minecraft.getTextureManager()). */
    public static TextureManager mcc$instance() {
        return legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().renderEngine;
    }

    /** The modern texture a legacy location shows. */
    public static net.minecraft.resources.ResourceLocation mcc$modern(ResourceLocation location) {
        String path = location.getPath();
        if (path.equals("textures/atlas/blocks.png")) return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        return TextureLocations.modern(location.getNamespace(), path);
    }

    public void bindTexture(ResourceLocation resource) {
        if (resource == null) return; // legacy renderers with an unset texture keep the previous one
        flush();
        var modern = mcc$modern(resource);
        // 1.12's HUD sheet (hearts, armor, food) is gone since 1.20.2: composed from the modern sprites the first time it is bound
        // (Tinkers' Mantle cancels the vanilla hearts and draws its own from it; nothing showed)
        if (modern.equals(org.kylepitcock.legacyconnector.v1_7_10.client.LegacyIcons.LOCATION)) org.kylepitcock.legacyconnector.v1_7_10.client.LegacyIcons.ensure();
        RenderContext.bindTexture(modern);
    }

    public boolean loadTickableTexture(ResourceLocation textureLocation, ITickableTextureObject textureObj) {
        return loadTexture(textureLocation, textureObj);
    }

    public boolean loadTexture(ResourceLocation textureLocation, ITextureObject textureObj) {
        if (textureLocation == null || textureObj == null) return false;
        synchronized (this) {
            mapTextureObjects.put(textureLocation, textureObj);
        }
        if (textureObj instanceof TextureMap) return true; // the block atlas is the modern one
        var manager = modernManager();
        if (manager == null || !com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            synchronized (pending) {
                pending.put(textureLocation, textureObj);
            }
            com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(this::flush);
            return true;
        }
        return register(manager, textureLocation, textureObj);
    }

    private static net.minecraft.client.renderer.texture.TextureManager modernManager() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc == null ? null : mc.getTextureManager();
    }

    private boolean register(net.minecraft.client.renderer.texture.TextureManager manager, ResourceLocation location, ITextureObject texture) {
        var modern = mcc$modern(location);
        net.minecraft.client.renderer.texture.AbstractTexture wrapper = texture instanceof DynamicTexture dynamic ? dynamic.mcc$modern() : new LegacyGlTexture1122(texture);
        try {
            manager.register(modern, wrapper);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.warn("Registering texture {} ({}) failed", location, texture.getClass().getName(), e);
            manager.register(modern, net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getTexture());
        }
        if (manager.getTexture(modern, null) != wrapper) {
            synchronized (this) {
                mapTextureObjects.put(location, TextureUtil.MISSING_TEXTURE);
            }
            return false;
        }
        return true;
    }

    private void flush() {
        java.util.List<Map.Entry<ResourceLocation, ITextureObject>> entries;
        synchronized (pending) {
            if (pending.isEmpty()) return;
            entries = new java.util.ArrayList<>(pending.entrySet());
            pending.clear();
        }
        var manager = modernManager();
        if (manager == null) return;
        for (var e : entries) {
            synchronized (this) {
                if (mapTextureObjects.get(e.getKey()) != e.getValue()) continue; // replaced or deleted meanwhile
            }
            register(manager, e.getKey(), e.getValue());
        }
    }

    /** The block atlas, or a texture object registered under the location. */
    public ITextureObject getTexture(ResourceLocation textureLocation) {
        if (textureLocation == null) return null;
        if (textureLocation.getPath().equals("textures/atlas/blocks.png")) return TextureMap.mcc$blocks();
        synchronized (this) {
            return mapTextureObjects.get(textureLocation);
        }
    }

    public ResourceLocation getDynamicTextureLocation(String name, DynamicTexture texture) {
        int count;
        synchronized (this) {
            Integer i = mapTextureCounters.get(name);
            count = i == null ? 1 : i + 1;
            mapTextureCounters.put(name, count);
        }
        ResourceLocation location = new ResourceLocation("legacyconnector", String.format(Locale.ROOT, "dynamic/%s_%d", name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"), count));
        loadTexture(location, texture);
        return location;
    }

    /** Animated textures tick with the modern texture manager. */
    @Override
    public void tick() {
    }

    public void deleteTexture(ResourceLocation textureLocation) {
        if (textureLocation == null) return;
        ITextureObject old;
        synchronized (this) {
            old = mapTextureObjects.remove(textureLocation);
        }
        var manager = modernManager();
        if (old != null && manager != null) {
            var modern = mcc$modern(textureLocation);
            if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) manager.release(modern);
            else com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(() -> manager.release(modern));
        }
    }

    /** The modern texture manager reloads the registered textures itself (they are modern textures). */
    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
    }
}
