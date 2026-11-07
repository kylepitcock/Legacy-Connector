package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sprites that make their own pixels (a mod's {@code TextureAtlasSprite} subclass with a custom loader: Tinkers' Construct's material colored
 * parts, mods that draw overlays on other sprites). 1.12 ran their {@code load} while it stitched the block atlas; the modern atlas reads texture
 * files, so each such sprite is loaded here, while the resources are built, and written as a texture file under the name of the sprite. The
 * sources it asks for through the sprite getter are read from their texture files, or generated first when they are custom sprites themselves.
 */
public final class CustomSprites1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Sprites1122");

    private CustomSprites1122() {}

    /** Generates the custom sprites registered with the block atlas and adds their texture files to the pack. */
    static void generate(Map<String, Supplier<byte[]>> out) {
        TextureMap map = TextureMap.mcc$blocks();
        IResourceManager manager = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().getResourceManager();
        Set<String> done = new HashSet<>();
        int generated = 0;
        long start = System.nanoTime();
        for (TextureAtlasSprite sprite : map.mcc$registeredSprites()) {
            try {
                if (!sprite.hasCustomLoader(manager, new ResourceLocation(sprite.getIconName()))) continue;
            } catch (RuntimeException | LinkageError e) {
                continue;
            }
            if (generateOne(map, manager, sprite, done, out, 0)) generated++;
        }
        if (generated > 0) LOG.info("Generated {} 1.12.2 sprite(s) that make their own pixels in {} ms", generated, (System.nanoTime() - start) / 1_000_000L);
    }

    private static boolean generateOne(TextureMap map, IResourceManager manager, TextureAtlasSprite sprite, Set<String> done,
            Map<String, Supplier<byte[]>> out, int depth) {
        String name = sprite.getIconName();
        if (!done.add(name)) return false;
        if (depth > 8) return false;
        ResourceLocation location = new ResourceLocation(name);
        sprite.mcc$pin(true);
        Function<ResourceLocation, TextureAtlasSprite> getter = source -> {
            String sourceName = source.toString();
            TextureAtlasSprite found = map.getTextureExtry(sourceName);
            if (found == null) {
                found = new TextureAtlasSprite(sourceName);
                found.mcc$loadFromFile(manager);
            } else if (found != sprite) {
                boolean custom;
                try {
                    custom = found.hasCustomLoader(manager, source);
                } catch (RuntimeException | LinkageError e) {
                    custom = false;
                }
                if (custom && !done.contains(sourceName)) generateOne(map, manager, found, done, out, depth + 1);
                else if (!custom && found.getFrameCount() == 0) found.mcc$loadFromFile(manager);
            }
            return found;
        };
        try {
            if (sprite.load(manager, location, getter)) return false; // the default loading of the texture file
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("The sprite {} could not generate its pixels: {}", name, e.toString());
            return false;
        }
        try {
            int frames = sprite.getFrameCount();
            int width = sprite.getIconWidth();
            int height = sprite.getIconHeight();
            if (frames <= 0 || width <= 0 || height <= 0) return false;
            BufferedImage image = new BufferedImage(width, height * frames, BufferedImage.TYPE_INT_ARGB);
            for (int frame = 0; frame < frames; frame++) {
                int[][] data = sprite.getFrameTextureData(frame);
                if (data == null || data.length == 0 || data[0] == null || data[0].length < width * height) continue;
                image.setRGB(0, frame * height, width, height, data[0], 0, width);
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", bytes);
            byte[] png = bytes.toByteArray();
            net.minecraft.resources.ResourceLocation modern = Sprites1122.modernName(name);
            String base = "assets/" + modern.getNamespace() + "/textures/" + modern.getPath();
            out.put(base + ".png", () -> png);
            if (frames > 1) {
                byte[] meta = "{\"animation\":{}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                out.put(base + ".png.mcmeta", () -> meta);
            }
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            LOG.debug("The sprite {} could not be written: {}", name, e.toString());
            return false;
        }
    }
}
