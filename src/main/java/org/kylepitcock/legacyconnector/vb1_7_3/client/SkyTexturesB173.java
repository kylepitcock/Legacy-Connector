package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Beta mods swapped the sun and moon by writing another texture under {@code /terrain/sun.png} or {@code /terrain/moon.png} in the render
 * engine's name table. The native sky draws {@code environment/sun.png} and the eight-phase {@code environment/moon_phases.png}; the
 * table's override is placed on that texture (every phase shows the mod's image) and taken off again when the mod restores the default.
 */
public final class SkyTexturesB173 {
    private static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final ResourceLocation MOON = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    private static String appliedSun, appliedMoon;

    private SkyTexturesB173() {}

    public static void tick() {
        String sun = legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager.override("/terrain/sun.png");
        String moon = legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager.override("/terrain/moon.png");
        if (!java.util.Objects.equals(sun, appliedSun)) { apply(SUN, sun, 1, 1); appliedSun = sun; }
        if (!java.util.Objects.equals(moon, appliedMoon)) { apply(MOON, moon, 4, 2); appliedMoon = moon; }
    }

    private static void apply(ResourceLocation target, String path, int columns, int rows) {
        var textures = Minecraft.getInstance().getTextureManager();
        if (path == null) { textures.register(target, new SimpleTexture(target)); return; }
        java.awt.image.BufferedImage image = ClientGuiB173.read(path);
        if (image == null) { textures.register(target, new SimpleTexture(target)); return; }
        int w = image.getWidth(), h = image.getHeight();
        NativeImage sheet = new NativeImage(w * columns, h * rows, true);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int argb = image.getRGB(x, y);
            int abgr = (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
            for (int c = 0; c < columns; c++) for (int r = 0; r < rows; r++) sheet.setPixelRGBA(c * w + x, r * h + y, abgr);
        }
        textures.register(target, new DynamicTexture(sheet));
    }
}
