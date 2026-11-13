package org.kylepitcock.legacyconnector.vb1_7_3;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * The few b1.7.3 terrain.png tiles that have no modern block texture to point at, as PNG files of {@code legacyconnector/b173/vanilla_sprites}:
 * the chest (top 25, side 26, front 27, which modern chests keep on entity models) and the animated fluids (205 and 206 water, 237 and 238 lava,
 * which modern fluids draw as tinted grayscale). Only these tiles are kept; {@code tools/dev/extract_b173_sprites.py} refills the folder from a
 * terrain.png.
 */
final class VanillaSpritesB173 {
    private static final String DIRECTORY = "/legacyconnector/b173/vanilla_sprites/";
    private static final Map<Integer, String> FILES = Map.of(25, "chest_top", 26, "chest_side", 27, "chest_front",
            205, "water_still", 206, "water_flow", 237, "lava_still", 238, "lava_flow");

    private VanillaSpritesB173() {}

    /** The tile of terrain.png cell {@code index}, or null when the cell is not one of these. */
    static BufferedImage sprite(int index) {
        String file = FILES.get(index);
        if (file == null) return null;
        try (var in = VanillaSpritesB173.class.getResourceAsStream(DIRECTORY + file + ".png")) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException error) {
            throw new java.io.UncheckedIOException("Cannot read the b1.7.3 sprite " + file, error);
        }
    }
}
