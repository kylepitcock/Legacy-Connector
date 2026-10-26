package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

/**
 * Vanilla 1.7.10 GUI sheets that no longer exist, rebuilt from their modern sprites at the legacy layout so legacy screens
 * that bind them draw real art instead of the missing texture: {@code textures/gui/achievement/achievement_background.png}
 * (Thaumcraft's "Research Completed" popup draws its toast from it).
 */
public final class LegacyGuiTextures {
    private LegacyGuiTextures() {
    }

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        try {
            BufferedImage sheet = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sheet.createGraphics();
            // the achievement frames (normal and special) at (0, 202) and (26, 202), the toast at (96, 202)
            draw(g, "textures/gui/sprites/advancements/task_frame_obtained.png", 0, 202);
            draw(g, "textures/gui/sprites/advancements/challenge_frame_obtained.png", 26, 202);
            draw(g, "textures/gui/sprites/toast/advancement.png", 96, 202);
            g.dispose();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(sheet, "png", bytes);
            byte[] png = bytes.toByteArray();
            out.put("assets/minecraft/textures/gui/achievement/achievement_background.png", () -> png);
        } catch (IOException | RuntimeException e) {
            // legacy screens keep the missing texture
        }
    }

    private static void draw(Graphics2D g, String path, int x, int y) throws IOException {
        byte[] png = LegacySprites.vanillaTexture(path);
        if (png == null) return;
        g.drawImage(ImageIO.read(new ByteArrayInputStream(png)), x, y, null);
    }
}
