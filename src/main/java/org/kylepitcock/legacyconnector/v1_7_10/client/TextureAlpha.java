package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Whether a texture's texels are all fully opaque or fully clear, read once from its resource. Alpha blending such a texture
 * (at full vertex alpha, depth writes on) looks exactly like drawing it as a cutout, which is what legacy "lights" layers are:
 * Fisk's Heroes redraws a suit part with a second texture that is clear except for its lamps, blending on and lighting off.
 * Unknown textures (dynamic, atlases, missing resources) answer false, so they keep their blending.
 */
public final class TextureAlpha {
    private static final Map<ResourceLocation, Boolean> BINARY = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Boolean> SPARSE = new ConcurrentHashMap<>();
    /** Larger textures are not scanned (atlases, sky panoramas): they keep their blending. */
    private static final int MAX_TEXELS = 2048 * 2048;

    private TextureAlpha() {}

    public static boolean binary(ResourceLocation texture) {
        if (texture == null) return false;
        Boolean known = BINARY.get(texture);
        if (known != null) return known;
        return BINARY.computeIfAbsent(texture, TextureAlpha::scan);
    }

    /** A texture of only opaque and clear texels with at most 5% opaque ones (a lamp layer: Doctor Octopus's claw eye). */
    public static boolean sparse(ResourceLocation texture) {
        if (texture == null) return false;
        return SPARSE.computeIfAbsent(texture, TextureAlpha::scanSparse);
    }

    /** After a resource reload the textures may have changed. */
    public static void clear() {
        BINARY.clear();
        SPARSE.clear();
    }

    private static boolean scan(ResourceLocation texture) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isEmpty()) return false;
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                if (!image.format().hasAlpha()) return true;
                if (image.format() != NativeImage.Format.RGBA) return false;
                int w = image.getWidth(), h = image.getHeight();
                if ((long) w * h > MAX_TEXELS) return false;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int alpha = image.getPixelRGBA(x, y) >>> 24;
                        if (alpha != 0 && alpha != 255) return false;
                    }
                }
                return true;
            }
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    private static boolean scanSparse(ResourceLocation texture) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isEmpty()) return false;
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                if (image.format() != NativeImage.Format.RGBA) return false;
                int w = image.getWidth(), h = image.getHeight();
                if ((long) w * h > MAX_TEXELS) return false;
                long opaque = 0;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int alpha = image.getPixelRGBA(x, y) >>> 24;
                        if (alpha != 0 && alpha != 255) return false;
                        if (alpha == 255) opaque++;
                    }
                }
                return opaque * 20 <= (long) w * h;
            }
        } catch (Exception | LinkageError e) {
            return false;
        }
    }
}
