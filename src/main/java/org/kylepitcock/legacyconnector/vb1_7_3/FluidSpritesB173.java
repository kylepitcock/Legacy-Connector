package org.kylepitcock.legacyconnector.vb1_7_3;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Beta's animated fluid cells of terrain.png (205 and 206 water, 237 and 238 lava) as plain block textures. Modern fluids are tinted
 * grayscale textures that only work inside a fluid block, so a block that borrows the cell (a portal drawn with the water texture)
 * gets a still tile of the old colour: translucent blue water, opaque orange lava.
 */
final class FluidSpritesB173 {
    private FluidSpritesB173() {}

    /** The tile of a fluid cell, or null for any other index. */
    static BufferedImage sprite(int index) {
        boolean water = index == 205 || index == 206;
        boolean lava = index == 237 || index == 238;
        if (!water && !lava) return null;
        var random = new Random(index);
        var image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                // soft diagonal ripples with a little grain
                double wave = Math.sin((x + y) * 0.9) * 0.5 + Math.sin((x - 2 * y) * 0.6) * 0.5;
                int grain = random.nextInt(21) - 10;
                if (water) {
                    int r = clamp(0x2A + (int) (wave * 14) + grain / 2), g = clamp(0x4A + (int) (wave * 18) + grain),
                            b = clamp(0xE0 + (int) (wave * 16) + grain);
                    image.setRGB(x, y, (170 << 24) | (r << 16) | (g << 8) | b);
                } else {
                    int r = clamp(0xE8 + (int) (wave * 10) + grain), g = clamp(0x62 + (int) (wave * 34) + grain),
                            b = clamp(0x10 + (int) (wave * 8) + grain / 2);
                    image.setRGB(x, y, (255 << 24) | (r << 16) | (g << 8) | b);
                }
            }
        }
        return image;
    }

    private static int clamp(int value) { return Math.max(0, Math.min(255, value)); }
}
