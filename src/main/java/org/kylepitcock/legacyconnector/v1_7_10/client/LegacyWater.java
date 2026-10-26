package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

/**
 * 1.7.10's water textures were blue; the modern ones are grey and tinted by the renderer. Legacy code that draws the water
 * icon itself (Thaumcraft's crucible, mods' tanks) would show grey water, so legacy names of the water icons resolve to
 * copies of the modern textures pre-tinted with the default water colour.
 */
public final class LegacyWater {
    /** The default (plains) water colour. */
    private static final int WATER = 0x3F76E4;

    private LegacyWater() {
    }

    /** The sprite a legacy water icon name resolves to, or null when the name is not one of them. */
    public static String sprite(String bareName) {
        return switch (bareName) {
            case "water_still", "water" -> "legacyconnector:block/legacy_water_still";
            case "water_flow", "water_flowing" -> "legacyconnector:block/legacy_water_flow";
            default -> null;
        };
    }

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        for (String name : new String[] {"water_still", "water_flow"}) {
            byte[] tinted = tint(LegacySprites.vanillaTexture("textures/block/" + name + ".png"));
            if (tinted == null) continue;
            out.put("assets/legacyconnector/textures/block/legacy_" + name + ".png", () -> tinted);
            byte[] meta = LegacySprites.vanillaTexture("textures/block/" + name + ".png.mcmeta");
            if (meta != null) out.put("assets/legacyconnector/textures/block/legacy_" + name + ".png.mcmeta", () -> meta);
        }
    }

    private static byte[] tint(byte[] png) {
        if (png == null) return null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            int tr = WATER >> 16 & 255, tg = WATER >> 8 & 255, tb = WATER & 255;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int p = image.getRGB(x, y);
                    int r = (p >> 16 & 255) * tr / 255, g = (p >> 8 & 255) * tg / 255, b = (p & 255) * tb / 255;
                    result.setRGB(x, y, p & 0xFF000000 | r << 16 | g << 8 | b);
                }
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(result, "png", bytes);
            return bytes.toByteArray();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
