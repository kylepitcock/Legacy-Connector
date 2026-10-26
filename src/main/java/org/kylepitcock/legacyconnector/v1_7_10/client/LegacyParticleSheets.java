package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10's particle sheets, which modern Minecraft split into one texture per sprite: {@code textures/particle/particles.png}
 * (16x16 cells; legacy particles pick a cell with {@code setParticleTextureIndex}), {@code textures/entity/explosion.png}
 * (4x4 frames of the large explosion) and {@code textures/particle/footprint.png}. Each is composed once from the modern
 * sprites at the old positions and registered under its old name, so particles and mods binding the sheet draw the right cell.
 */
public final class LegacyParticleSheets {
    public static final ResourceLocation PARTICLES = ResourceLocation.withDefaultNamespace("textures/particle/particles.png");
    public static final ResourceLocation EXPLOSION = ResourceLocation.withDefaultNamespace("textures/entity/explosion.png");
    public static final ResourceLocation FOOTPRINT = ResourceLocation.withDefaultNamespace("textures/particle/footprint.png");
    private static final Map<String, Boolean> DONE = new HashMap<>();

    private LegacyParticleSheets() {}

    /** The sheet for a legacy vanilla path, composed on first use; null when the path is not one of the sheets. */
    static ResourceLocation forPath(String path) {
        switch (path) {
            case "textures/particle/particles.png" -> {
                ensure(path, LegacyParticleSheets::particles, PARTICLES);
                return PARTICLES;
            }
            case "textures/entity/explosion.png" -> {
                ensure(path, LegacyParticleSheets::explosion, EXPLOSION);
                return EXPLOSION;
            }
            case "textures/particle/footprint.png" -> {
                ensure(path, LegacyParticleSheets::footprint, FOOTPRINT);
                return FOOTPRINT;
            }
            default -> {
                return null;
            }
        }
    }

    private static void ensure(String key, java.util.function.Supplier<NativeImage> build, ResourceLocation location) {
        if (DONE.containsKey(key) || !RenderSystem.isOnRenderThread()) return;
        DONE.put(key, Boolean.TRUE);
        try {
            NativeImage image = build.get();
            if (image != null) Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Particles").warn("Could not compose the legacy texture {}: {}", key, e.toString());
        }
    }

    private static NativeImage particles() {
        NativeImage probe = read("textures/particle/generic_0.png");
        int cell = probe == null ? 8 : Math.max(8, probe.getWidth());
        if (probe != null) probe.close();
        NativeImage sheet = new NativeImage(NativeImage.Format.RGBA, cell * 16, cell * 16, true);
        for (int i = 0; i < 8; i++) {
            put(sheet, cell, i, "generic_" + i);
            put(sheet, cell, 128 + i, "effect_" + i);
            put(sheet, cell, 144 + i, "spell_" + i);
            put(sheet, cell, 160 + i, "spark_" + i);
        }
        for (int i = 0; i < 4; i++) put(sheet, cell, 19 + i, "splash_" + i);
        put(sheet, cell, 32, "bubble");
        put(sheet, cell, 48, "flame");
        put(sheet, cell, 49, "lava");
        put(sheet, cell, 64, "note");
        put(sheet, cell, 65, "critical_hit");
        put(sheet, cell, 66, "enchanted_hit");
        put(sheet, cell, 80, "heart");
        put(sheet, cell, 81, "angry");
        put(sheet, cell, 82, "glint");
        put(sheet, cell, 112, "drip_fall");
        put(sheet, cell, 113, "drip_hang");
        put(sheet, cell, 114, "drip_land");
        for (int i = 0; i < 26; i++) put(sheet, cell, 225 + i, "sga_" + (char) ('a' + i));
        // the fishing bobber was cell (1, 2) of the sheet
        copy(sheet, read("textures/entity/fishing_hook.png"), cell, 33);
        return sheet;
    }

    private static NativeImage explosion() {
        NativeImage probe = read("textures/particle/explosion_0.png");
        int cell = probe == null ? 32 : Math.max(8, probe.getWidth());
        if (probe != null) probe.close();
        NativeImage sheet = new NativeImage(NativeImage.Format.RGBA, cell * 4, cell * 4, true);
        for (int i = 0; i < 16; i++) copy(sheet, read("textures/particle/explosion_" + i + ".png"), cell, i % 4, i / 4);
        return sheet;
    }

    /** Two small footprints (the texture no longer exists): dark grey, drawn translucent by EntityFootStepFX. */
    private static NativeImage footprint() {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 16, 16, true);
        int color = 0xFF303030; // ABGR
        for (int foot = 0; foot < 2; foot++) {
            int ox = foot == 0 ? 3 : 9, oy = foot == 0 ? 2 : 7;
            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < 4; x++) {
                    if ((y == 0 || y == 6) && (x == 0 || x == 3)) continue;
                    if (y == 4 && (x == 0 || x == 3)) continue;
                    image.setPixelRGBA(ox + x, oy + y, color);
                }
            }
        }
        return image;
    }

    private static void put(NativeImage sheet, int cell, int index, String sprite) {
        copy(sheet, read("textures/particle/" + sprite + ".png"), cell, index);
    }

    private static void copy(NativeImage sheet, NativeImage sprite, int cell, int index) {
        copy(sheet, sprite, cell, index % 16, index / 16);
    }

    /** Copies a sprite into the cell at (cx, cy), scaled to the cell size (resource packs may ship other resolutions). */
    private static void copy(NativeImage sheet, NativeImage sprite, int cell, int cx, int cy) {
        if (sprite == null) return;
        try {
            int w = sprite.getWidth(), h = Math.min(sprite.getHeight(), sprite.getWidth()); // animated strips: first frame
            for (int y = 0; y < cell; y++) {
                for (int x = 0; x < cell; x++) {
                    sheet.setPixelRGBA(cx * cell + x, cy * cell + y, sprite.getPixelRGBA(x * w / cell, y * h / cell));
                }
            }
        } finally {
            sprite.close();
        }
    }

    private static NativeImage read(String path) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.withDefaultNamespace(path));
        if (resource.isEmpty()) return null;
        try (InputStream in = resource.get().open()) {
            return NativeImage.read(in);
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }
}
