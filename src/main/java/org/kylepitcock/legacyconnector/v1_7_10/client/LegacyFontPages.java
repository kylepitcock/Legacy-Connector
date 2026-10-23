package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

/**
 * 1.7.10 mods that copy vanilla's FontRenderer (Mantle's book font) read the unicode glyph pages and
 * {@code font/glyph_sizes.bin}, which 1.21 no longer ships. Page 0 (Latin) and its glyph sizes are rebuilt from the
 * game's ASCII font texture, drawn at double size, so such fonts show readable text.
 */
public final class LegacyFontPages {
    private LegacyFontPages() {}

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        BufferedImage ascii = readAscii();
        if (ascii == null || ascii.getWidth() != 128) return;
        BufferedImage page = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        byte[] sizes = new byte[65536];
        for (int index = 0; index < 256; index++) {
            int cx = index % 16, cy = index / 16;
            int min = 99, max = -1;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int argb = ascii.getRGB(cx * 8 + x, cy * 8 + y);
                    if ((argb >>> 24) == 0) continue;
                    min = Math.min(min, x);
                    max = Math.max(max, x);
                    for (int dy = 0; dy < 2; dy++) for (int dx = 0; dx < 2; dx++) page.setRGB(cx * 16 + x * 2 + dx, cy * 16 + y * 2 + dy, argb);
                }
            }
            if (max >= 0) sizes[index] = (byte) ((min * 2) << 4 | Math.min(15, max * 2 + 2));
        }
        byte[] png = encode(page);
        if (png == null) return;
        out.put("assets/minecraft/textures/font/unicode_page_00.png", () -> png);
        out.put("assets/minecraft/font/glyph_sizes.bin", () -> sizes);
    }

    private static BufferedImage readAscii() {
        try (InputStream in = net.minecraft.client.Minecraft.class.getResourceAsStream("/assets/minecraft/textures/font/ascii.png")) {
            return in == null ? null : ImageIO.read(in);
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    private static byte[] encode(BufferedImage image) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bytes);
            return bytes.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
