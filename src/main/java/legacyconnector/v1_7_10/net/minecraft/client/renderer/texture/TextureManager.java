package legacyconnector.v1_7_10.net.minecraft.client.renderer.texture;

import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.TextureLocations;

/** Binding a texture just records it in the render context; modern draw calls name their texture explicitly. */
public class TextureManager {
    /** Textures by location (1.7.10's map; mods look it up by reflection to replace or drop a texture). */
    private final java.util.Map mapTextureObjects = new java.util.HashMap();
    public static final TextureManager INSTANCE = new TextureManager();

    public void bindTexture(ResourceLocation location) {
        if (location == null) return; // legacy renderers with an unset texture keep the previous one
        mcc$flush();
        var modern = TextureLocations.modern(location.getResourceDomain(), location.getResourcePath());
        // a mod's own atlas: its sprites are in the modern block atlas
        if (mcc$modAtlases.contains(modern)) modern = net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
        RenderContext.bindTexture(modern);
    }

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

    /** Registers a dynamic texture under a location so legacy code can bind it; other texture objects are not tracked. */
    public boolean loadTexture(ResourceLocation location, ITextureObject texture) {
        if (location != null && texture instanceof TextureMap map) {
            mapTextureObjects.put(location, map);
            mcc$modAtlases.add(TextureLocations.modern(location.getResourceDomain(), location.getResourcePath()));
            try {
                map.loadTexture(null);
            } catch (RuntimeException e) {
                TextureMap.logger.warn("Loading atlas {} failed: {}", location, e.toString());
            }
            return true;
        }
        if (location == null || !(texture instanceof DynamicTexture dynamic)) return false;
        var modern = TextureLocations.modern(location.getResourceDomain(), location.getResourcePath());
        mcc$pending.put(modern, dynamic);
        mcc$flush();
        return true;
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
