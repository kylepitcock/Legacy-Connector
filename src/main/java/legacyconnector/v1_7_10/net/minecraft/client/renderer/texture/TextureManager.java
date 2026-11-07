package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations;

/** Binding a texture just records it in the render context; modern draw calls name their texture explicitly. */
public class TextureManager implements ITickable {
    /** Textures by location (1.7.10's map; mods look it up by reflection to replace or drop a texture). */
    private final java.util.Map mapTextureObjects = new java.util.HashMap();
    public static final TextureManager INSTANCE = new TextureManager();

    public void bindTexture(ResourceLocation location) {
        if (location == null) return; // legacy renderers with an unset texture keep the previous one
        mcc$flush();
        var modern = TextureLocations.modern(location);
        // a mod's own atlas: its sprites are in the modern block atlas
        if (mcc$modAtlases.contains(modern)) modern = net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        RenderContext.bindTexture(modern);
    }

    /** 1.4.7 and 1.5.2 (RenderEngine.bindTexture(int)): a texture by the OpenGL id {@link #getTexture} returned. */
    public void bindTexture(int glId) {
        if (org.kylepitcock.legacyconnector.core.pack.GeneratedPack.building()) return;
        var named = mcc$handles.get(glId);
        if (named != null) {
            // a texture of a mod that the game has not loaded yet: bound by location, which loads it
            bindTexture(named);
            return;
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, glId);
    }

    /** Forge 1.5.2 (RenderEngine): textures are string paths, resolved by Textures152. */
    public void bindTexture(String path) {
        mcc$lastPath = path;
        // Forge 1.4.7 drew the vertices collected so far with the old texture before the new one was bound (a block renderer binds a
        // sheet per part while the chunk's Tessellator is drawing): the captured quads must keep the sheet they were added under
        if (RenderContext.capturing()) {
            var t = legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.mcc$get$instance();
            if (t.isDrawing) {
                t.draw();
                t.startDrawingQuads();
            }
        }
        bindTexture(org.kylepitcock.legacyconnector.v1_6_4.bridge.Textures152.location(path));
        // a 1.4.7 index sheet: tessellator UVs are tile positions of this image, not atlas UVs (IsbrhQuads maps them to the cut tiles)
        if (path != null && path.indexOf(':') < 0 && path.endsWith(".png")) RenderContext.setSheet(path);
    }

    /** The last path given to {@code bindTexture(String)} or {@code getTexture(String)} (Animations147 learns an animated tile's sheet from it). */
    public static volatile String mcc$lastPath;

    /** 1.4.7: the texture pack list (RenderEngine.texturePack). */
    public legacyconnector.v1_6_4.net.minecraft.client.texturepacks.TexturePackList texturePack = new legacyconnector.v1_6_4.net.minecraft.client.texturepacks.TexturePackList();

    /** 1.4.7 (RenderEngine.registerTextureFX): an animated tile. */
    public void registerTextureFX(legacyconnector.v1_6_4.net.minecraft.client.renderer.texturefx.TextureFX fx) {
        org.kylepitcock.legacyconnector.v1_6_4.bridge.Animations147.register(fx);
    }

    /** 1.5.2's cached binding is not tracked here (every bind goes through the render context). */
    public void resetBoundTexture() {
    }

    /** 1.5.2 RenderEngine.getTexture(path): the OpenGL id of a texture, 0 until it is loaded. */
    public int getTexture(String path) {
        mcc$lastPath = path;
        var modern = TextureLocations.modern(org.kylepitcock.legacyconnector.v1_6_4.bridge.Textures152.location(path));
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.getTextureManager() == null) return 0;
        var tex = mc.getTextureManager().getTexture(modern, null);
        if (tex != null) {
            // draws of the legacy GUI code name their texture through the render context, so the id is mapped back to its location when it is bound
            mcc$handles.putIfAbsent(tex.getId(), org.kylepitcock.legacyconnector.v1_6_4.bridge.Textures152.location(path));
            return tex.getId();
        }
        // not loaded yet (a mod's GUI image is first bound through this id): a handle that bindTexture(int) maps back to the location
        var location = org.kylepitcock.legacyconnector.v1_6_4.bridge.Textures152.location(path);
        return mcc$handleIds.computeIfAbsent(location, l -> {
            int id = 0x40000000 + mcc$handleIds.size();
            mcc$handles.put(id, l);
            return id;
        });
    }

    private final java.util.Map<Integer, ResourceLocation> mcc$handles = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<ResourceLocation, Integer> mcc$handleIds = new java.util.concurrent.ConcurrentHashMap<>();

    private final java.util.Set<net.minecraft.resources.ResourceLocation> mcc$modAtlases = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** 1.7.10 loaded the texture right away; a mod's atlas registers its items' icons then (see TextureMap#loadTexture). */
    public boolean loadTickableTexture(ResourceLocation location, ITickableTextureObject texture) {
        return loadTexture(location, texture);
    }

    /** The block and item atlases (mods look sprites up by name through them); other textures are not tracked. */
    public ITextureObject getTexture(ResourceLocation location) {
        if (location == null) return null;
        String path = location.getResourcePath();
        if (path.equals("textures/atlas/blocks.png")) return org.kylepitcock.legacyconnector.v1_7_10.Assets1710.blockAtlas();
        if (path.equals("textures/atlas/items.png")) return org.kylepitcock.legacyconnector.v1_7_10.Assets1710.itemAtlas();
        return (ITextureObject) mapTextureObjects.get(location);
    }

    /** Atlases loaded through {@link #loadTextureMap}, by texture type (0 blocks and 1 items always name the vanilla atlases). */
    private final java.util.Map<Integer, ResourceLocation> mapTextureMapTypes = new java.util.HashMap<>();

    public ResourceLocation getResourceLocation(int spriteIndex) {
        if (spriteIndex > 1) {
            ResourceLocation custom = mapTextureMapTypes.get(spriteIndex);
            if (custom != null) return custom;
        }
        return new ResourceLocation(spriteIndex == 0 ? "textures/atlas/blocks.png" : "textures/atlas/items.png");
    }

    /** Loads a mod's own atlas and remembers it as the atlas of its texture type. */
    public boolean loadTextureMap(ResourceLocation location, TextureMap map) {
        if (!loadTickableTexture(location, map)) return false;
        mapTextureMapTypes.put(map.getTextureType(), location);
        return true;
    }

    private int mcc$dynamicCount;

    /**
     * Registers a texture object under a location so legacy code can bind it. Atlases and dynamic textures map to their modern
     * counterparts; any other object (SimpleTexture, a mod's own texture class) loads itself as in 1.7.10 (on the render thread,
     * later if called elsewhere) and is registered as a modern texture showing its GL texture. A load failing with an
     * IOException leaves the missing texture there and answers false, as 1.7.10 did.
     */
    public boolean loadTexture(ResourceLocation location, ITextureObject texture) {
        if (location == null || texture == null) return false;
        if (texture instanceof TextureMap map) {
            mapTextureObjects.put(location, map);
            mcc$modAtlases.add(TextureLocations.modern(location.getResourceDomain(), location.getResourcePath()));
            try {
                map.loadTexture(null);
            } catch (RuntimeException e) {
                TextureMap.logger.warn("Loading atlas {} failed: {}", location, e.toString());
            }
            return true;
        }
        var modern = TextureLocations.modern(location.getResourceDomain(), location.getResourcePath());
        if (texture instanceof DynamicTexture dynamic) {
            mapTextureObjects.put(location, dynamic);
            mcc$pending.put(modern, dynamic);
            mcc$flush();
            return true;
        }
        mapTextureObjects.put(location, texture);
        var manager = net.minecraft.client.Minecraft.getInstance() == null ? null : net.minecraft.client.Minecraft.getInstance().getTextureManager();
        if (manager == null || !com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            synchronized (mcc$pendingObjects) {
                mcc$pendingObjects.put(location, texture);
            }
            com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(this::mcc$flushObjects);
            return true;
        }
        return mcc$register(manager, location, modern, texture);
    }

    /** Texture objects registered off the render thread or before the client's texture manager existed. */
    private final java.util.Map<ResourceLocation, ITextureObject> mcc$pendingObjects = new java.util.LinkedHashMap<>();

    private void mcc$flushObjects() {
        var manager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
        if (manager == null) return;
        java.util.List<java.util.Map.Entry<ResourceLocation, ITextureObject>> entries;
        synchronized (mcc$pendingObjects) {
            entries = new java.util.ArrayList<>(mcc$pendingObjects.entrySet());
            mcc$pendingObjects.clear();
        }
        for (var e : entries) {
            ResourceLocation location = e.getKey();
            if (mapTextureObjects.get(location) != e.getValue()) continue; // replaced or deleted meanwhile
            mcc$register(manager, location, TextureLocations.modern(location.getResourceDomain(), location.getResourcePath()), e.getValue());
        }
    }

    private boolean mcc$register(net.minecraft.client.renderer.texture.TextureManager manager, ResourceLocation location,
            net.minecraft.resources.ResourceLocation modern, ITextureObject texture) {
        var wrapper = new org.kylepitcock.legacyconnector.v1_7_10.client.LegacyGlTexture(texture);
        try {
            manager.register(modern, wrapper);
        } catch (RuntimeException | LinkageError e) {
            TextureMap.logger.warn("Registering texture {} ({}) failed", location, texture.getClass().getName(), e);
            manager.register(modern, net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getTexture());
        }
        if (manager.getTexture(modern, null) != wrapper) {
            mapTextureObjects.put(location, TextureUtil.missingTexture);
            return false;
        }
        return true;
    }

    /** Forgets a texture and frees its GL texture. */
    public void deleteTexture(ResourceLocation location) {
        if (location == null) return;
        Object old = mapTextureObjects.remove(location);
        var manager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
        if (old != null && manager != null) manager.release(TextureLocations.modern(location.getResourceDomain(), location.getResourcePath()));
    }

    /** Animated textures tick with the modern texture manager. */
    @Override
    public void tick() {
    }

    /** Mods create dynamic textures while loading, before the client's texture manager exists: those wait here. */
    private final java.util.Map<net.minecraft.resources.ResourceLocation, DynamicTexture> mcc$pending = new java.util.LinkedHashMap<>();

    private synchronized void mcc$flush() {
        if (mcc$pending.isEmpty()) return;
        var manager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
        if (manager == null) return;
        mcc$pending.forEach((id, texture) -> manager.register(id, texture.mcc$modern()));
        mcc$pending.clear();
    }

    public ResourceLocation getDynamicTextureLocation(String name, DynamicTexture texture) {
        ResourceLocation location = new ResourceLocation("legacyconnector", "dynamic/" + name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_") + mcc$dynamicCount++);
        loadTexture(location, texture);
        return location;
    }
}
