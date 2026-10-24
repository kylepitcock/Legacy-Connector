package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Legacy mods that make their own OpenGL textures (JourneyMap's map tiles, font and icon sheets) bind them by id. The
 * emulated renderer draws by texture location, so each id used this way gets a location backed by that GL texture.
 */
public final class RawGlTextures {
    private static final Map<Integer, ResourceLocation> BY_ID = new ConcurrentHashMap<>();

    private RawGlTextures() {}

    /** The location drawing with the given GL texture (render thread). */
    public static ResourceLocation location(int glId) {
        return BY_ID.computeIfAbsent(glId, id -> {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("legacyconnector", "gl/" + id);
            Minecraft.getInstance().getTextureManager().register(rl, new Raw(id));
            return rl;
        });
    }

    private static final class Raw extends AbstractTexture {
        Raw(int glId) {
            this.id = glId;
        }

        @Override
        public void load(ResourceManager manager) {
        }

        @Override
        public void releaseId() {
            // the mod owns the GL texture
        }

        @Override
        public void close() {
        }
    }
}
