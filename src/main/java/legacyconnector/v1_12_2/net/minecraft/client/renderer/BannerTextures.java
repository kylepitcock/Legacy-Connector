package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.LayeredColorMaskTexture;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.tileentity.BannerPattern;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Banner and shield textures composed from their patterns ({@link LayeredColorMaskTexture}), cached by pattern hash: mods
 * drawing banners or shields of their own get a texture location to bind. The 1.21.1 pattern masks sit at the same paths.
 */
public class BannerTextures {
    public static final Cache BANNER_DESIGNS = new Cache("B", new ResourceLocation("textures/entity/banner_base.png"), "textures/entity/banner/");
    public static final Cache SHIELD_DESIGNS = new Cache("S", new ResourceLocation("textures/entity/shield_base.png"), "textures/entity/shield/");
    public static final ResourceLocation SHIELD_BASE_TEXTURE = new ResourceLocation("textures/entity/shield_base_nopattern.png");
    public static final ResourceLocation BANNER_BASE_TEXTURE = new ResourceLocation("textures/entity/banner/base.png");

    public static class Cache {
        private final Map<String, CacheEntry> cacheMap = new LinkedHashMap<>();
        private final ResourceLocation cacheResourceLocation;
        private final String cacheResourceBase;
        private final String cacheId;
        private int nextTexture;

        public Cache(String id, ResourceLocation baseResource, String resourcePath) {
            cacheId = id;
            cacheResourceLocation = baseResource;
            cacheResourceBase = resourcePath;
        }

        public synchronized ResourceLocation getResourceLocation(String id, List<BannerPattern> patternList, List<EnumDyeColor> colorList) {
            if (id == null || id.isEmpty()) return null;
            id = cacheId + id;
            CacheEntry entry = cacheMap.get(id);
            if (entry == null) {
                if (cacheMap.size() >= 256 && !freeCacheSlot()) return BANNER_BASE_TEXTURE;
                List<String> textures = new ArrayList<>();
                for (BannerPattern pattern : patternList) textures.add(cacheResourceBase + pattern.getFileName() + ".png");
                entry = new CacheEntry();
                // the hash is upper/lower case and may hold characters a 1.21 path refuses: kept under a path of its own
                entry.textureLocation = new ResourceLocation("legacyconnector", "banner_cache/" + Integer.toHexString(id.hashCode()) + "_" + (nextTexture++));
                Minecraft.getMinecraft().getTextureManager().loadTexture(entry.textureLocation,
                        new LayeredColorMaskTexture(cacheResourceLocation, textures, colorList));
                cacheMap.put(id, entry);
            }
            entry.lastUseMillis = System.currentTimeMillis();
            return entry.textureLocation;
        }

        private boolean freeCacheSlot() {
            long now = System.currentTimeMillis();
            Iterator<String> it = cacheMap.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                CacheEntry entry = cacheMap.get(key);
                if (now - entry.lastUseMillis > 5000L) {
                    Minecraft.getMinecraft().getTextureManager().deleteTexture(entry.textureLocation);
                    it.remove();
                    return true;
                }
            }
            return cacheMap.size() < 256;
        }
    }

    static class CacheEntry {
        public long lastUseMillis;
        public ResourceLocation textureLocation;
    }
}
