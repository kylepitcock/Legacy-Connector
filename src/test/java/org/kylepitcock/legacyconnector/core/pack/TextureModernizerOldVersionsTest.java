package org.kylepitcock.legacyconnector.core.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The texture modernizer is version independent: it classifies whatever textures the translated jar (1.4.7's cut sprite sheet tiles
 * under {@code assets/legacy147/textures/blocks/<sheet>/<index>.png}, the per-file textures of 1.5.2 and 1.6.4) or, for Beta 1.7.3,
 * the generated pack ({@code textures/block/b173/}) holds. Synthetic "old vanilla" art stands in for Mojang's pixels.
 */
class TextureModernizerOldVersionsTest {
    private static final int SIZE = 16;

    /** Synthetic old tile: a pattern with plenty of colours; {@code seed} makes another "version" of it. */
    private static int[] oldArt(int seed) {
        int[] px = new int[SIZE * SIZE];
        for (int i = 0; i < px.length; i++) px[i] = 0xFF000000 | ((i * 37 + seed * 101) & 0xFF) << 16 | ((i * 11 + seed * 53) & 0xFF) << 8 | ((i * 5 + seed) & 0xFF);
        return px;
    }

    private static byte[] png(int[] px, int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, px, 0, w);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static int[] decode(byte[] png) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        return img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
    }

    private static final int[] MODERN = new int[SIZE * SIZE];

    static {
        java.util.Arrays.fill(MODERN, 0xFF445566);
    }

    private static final Function<String, byte[]> MODERN_TEXTURES = name -> {
        try {
            return name.equals("block/stone") || name.equals("block/oak_planks") ? png(MODERN, SIZE, SIZE) : null;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    };

    /** A table with a Beta-like stone (exact + base), a 1.4.7 planks (exact) and a 1.5.2 stone variant (exact). */
    private static TextureModernizer table() {
        int[] stoneBeta = oldArt(1), planks147 = oldArt(2), stone152 = oldArt(3);
        StringBuilder fp = new StringBuilder();
        for (int i = 0; i < stoneBeta.length; i++) fp.append(String.format("%02x", TextureModernizer.fingerprint(stoneBeta[i], i)));
        return TextureModernizer.forTable(
                "E " + TextureModernizer.exactHash(SIZE, SIZE, stoneBeta) + " block/stone 16x16\n"
                        + "E " + TextureModernizer.exactHash(SIZE, SIZE, planks147) + " block/oak_planks 16x16\n"
                        + "E " + TextureModernizer.exactHash(SIZE, SIZE, stone152) + " block/stone 16x16\n"
                        + "B b173_stone block/stone " + fp + "\n");
    }

    @Test
    void exactCopiesOfEveryVersionsArtAreReplacedAndOwnArtIsNot() throws IOException {
        TextureModernizer m = table();
        String[] paths = {
                "assets/legacy147/textures/blocks/mymod_blocks_png/1.png",      // 1.4.7: tile of a mod's cut sprite sheet
                "assets/mymod/textures/blocks/stone_copy.png",                    // 1.5.2 / 1.6.4: a texture file
                "assets/mymod/textures/block/b173/t1.png"};                       // Beta 1.7.3: generated sprite
        int[][] art = {oldArt(2), oldArt(3), oldArt(1)};
        String[] expected = {"block/oak_planks", "block/stone", "block/stone"};
        for (int i = 0; i < paths.length; i++) {
            TextureModernizer.Hit hit = m.classify(paths[i], png(art[i], SIZE, SIZE));
            assertNotNull(hit, paths[i]);
            assertEquals(TextureModernizer.Kind.EXACT, hit.kind());
            assertEquals(expected[i], hit.modern());
            assertTrue(java.util.Arrays.equals(MODERN, decode(m.apply(hit, png(art[i], SIZE, SIZE), MODERN_TEXTURES))));
        }
        assertNull(m.classify(paths[0], png(oldArt(40), SIZE, SIZE)), "a mod's own art stays");
    }

    @Test
    void highResolutionUpscaleOfOldArtIsRecognised() throws IOException {
        int[] art = oldArt(1);
        int[] big = new int[32 * 32];
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) big[y * 32 + x] = art[(y / 2) * SIZE + x / 2];
        TextureModernizer m = table();
        TextureModernizer.Hit hit = m.classify("assets/mymod/textures/block/b173/t1.png", png(big, 32, 32));
        assertNotNull(hit);
        int[] made = decode(m.apply(hit, png(big, 32, 32), MODERN_TEXTURES));
        assertEquals(32 * 32, made.length);
        assertEquals(0xFF445566, made[5]);
    }

    @Test
    void oldBackgroundIsReplacedButTheModsOwnPixelsStay() throws IOException {
        int[] art = oldArt(1);
        int own = 0xFFFF00AA;
        for (int y = 5; y < 11; y++) for (int x = 5; x < 11; x++) art[y * SIZE + x] = own;  // the mod painted a square onto Beta stone
        TextureModernizer m = table();
        TextureModernizer.Hit hit = m.classify("assets/legacy147/textures/blocks/mymod_blocks_png/17.png", png(art, SIZE, SIZE));
        assertNotNull(hit);
        assertEquals(TextureModernizer.Kind.BASE, hit.kind());
        int[] made = decode(m.apply(hit, png(art, SIZE, SIZE), MODERN_TEXTURES));
        assertEquals(own, made[7 * SIZE + 7]);
        assertEquals(0xFF445566, made[0]);
    }

    @Test
    void scanFindsCutTilesAndFilesOfATranslatedJar(@TempDir Path dir) throws IOException {
        Path jar = dir.resolve("mod.jar");
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(jar))) {
            put(z, "assets/legacy147/textures/blocks/mymod_blocks_png/3.png", png(oldArt(2), SIZE, SIZE));
            put(z, "assets/legacy147/textures/blocks/mymod_blocks_png/4.png", png(oldArt(77), SIZE, SIZE));
            put(z, "assets/mymod/textures/blocks/plank.png", png(oldArt(2), SIZE, SIZE));
        }
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var hits = table().scan(zip);
            assertEquals(2, hits.size());
            assertTrue(hits.stream().allMatch(h -> h.modern().equals("block/oak_planks")));
        }
    }

    @Test
    void generatedBetaSpritesAreModernizedAnimatedOnesAreNot() throws IOException {
        Map<String, Supplier<byte[]>> out = new HashMap<>();
        byte[] old = png(oldArt(1), SIZE, SIZE), own = png(oldArt(90), SIZE, SIZE);
        out.put("assets/mymod/textures/block/b173/t1.png", () -> old);
        out.put("assets/mymod/textures/block/b173/t2.png", () -> own);
        out.put("assets/mymod/textures/block/b173/animated_terrain_png_14.png", () -> old);
        out.put("assets/mymod/textures/block/other.png", () -> old);
        var done = table().modernizeGenerated(out, MODERN_TEXTURES);
        assertEquals(1, done.size());
        assertTrue(java.util.Arrays.equals(MODERN, decode(out.get("assets/mymod/textures/block/b173/t1.png").get())));
        assertSame(own, out.get("assets/mymod/textures/block/b173/t2.png").get());
        assertSame(old, out.get("assets/mymod/textures/block/b173/animated_terrain_png_14.png").get());
        assertSame(old, out.get("assets/mymod/textures/block/other.png").get());
    }

    @Test
    void shippedTableCoversTheOlderVersions() {
        String table;
        try (var in = TextureModernizer.class.getResourceAsStream("/legacyconnector/legacy/texture-fingerprints.txt")) {
            table = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(e);
        }
        assertTrue(table.contains("\nB b173_"), "Beta 1.7.3 bases");
        assertTrue(HexFormat.isHexDigit('a'));
    }

    private static void put(ZipOutputStream z, String name, byte[] data) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(data);
        z.closeEntry();
    }
}
