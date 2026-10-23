package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/** Positions of legacy icons (modern sprite ids) in the block atlas: {minU, maxU, minV, maxV, width, height}. */
public final class AtlasUV {
    private static final Map<String, float[]> CACHE = new ConcurrentHashMap<>();
    private static TextureAtlas cachedAtlas;

    private AtlasUV() {}

    /** The atlas was (re)stitched: sprite positions changed. */
    public static void invalidate() {
        CACHE.clear();
    }

    public static float[] of(String spriteId) {
        if (spriteId == null) return null;
        TextureAtlas atlas;
        try {
            atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        if (atlas == null) return null;
        if (atlas != cachedAtlas) {
            CACHE.clear(); // resources reloaded: the atlas was rebuilt
            cachedAtlas = atlas;
        }
        return CACHE.computeIfAbsent(spriteId, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) return null;
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite;
            try {
                sprite = atlas.getSprite(rl);
            } catch (IllegalStateException e) {
                return null; // the atlas is still being stitched (TextureStitchEvent.Post runs before it is ready)
            }
            var c = sprite.contents();
            return new float[] {sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), c.width(), c.height()};
        });
    }
}
