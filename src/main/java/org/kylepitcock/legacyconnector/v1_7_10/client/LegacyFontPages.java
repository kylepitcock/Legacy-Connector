package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

/**
 * 1.7.10 mods that copy vanilla's FontRenderer in unicode mode (Thaumcraft's Thaumonomicon, Mantle's book font) read the
 * unicode glyph pages {@code textures/font/unicode_page_XX.png} and {@code font/glyph_sizes.bin}, which 1.21 no longer
 * ships. Those pages were GNU Unifont, which 1.21 still ships as {@code font/unifont.zip}: the pages and glyph sizes are
 * rebuilt from it (16x16 cells, a glyph's first and last lit column as the size byte), so legacy unicode text has its
 * 1.7.10 shapes and widths. Before the game's resources can be read, page 0 falls back to the ASCII font drawn at double
 * size. They are served through the generated resource pack and, for fonts that read them early, through
 * {@link #page(int)} and {@link #glyphSizes()}.
 */
public final class LegacyFontPages {
    /** Unifont rows by code point: 16 bytes for an 8-wide glyph, 32 for a 16-wide one; null where Unifont has none. */
    private static byte[][] unifont;
    private static byte[] sizes;
    private static final byte[][] PAGES = new byte[256][];
    private static boolean asciiFallback;

    private LegacyFontPages() {}

    public static void contribute(Map<String, Supplier<byte[]>> out) {
        out.put("assets/minecraft/font/glyph_sizes.bin", LegacyFontPages::glyphSizes);
        for (int p = 0; p < 256; p++) {
            int page = p;
            out.put(String.format("assets/minecraft/textures/font/unicode_page_%02x.png", page), () -> page(page));
        }
    }

    public static byte[] page0() {
        return page(0);
    }

    /** The rebuilt unicode page, or null if neither Unifont nor the ASCII font can be read. */
    public static synchronized byte[] page(int page) {
        load();
        if (PAGES[page] == null && (unifont != null || page == 0)) {
            PAGES[page] = unifont != null ? encode(unifontPage(page)) : asciiPage0();
        }
        return PAGES[page];
    }

    /** The glyph sizes file: high nibble the first lit column, low nibble the last, as 1.7.10's file had them. */
    public static synchronized byte[] glyphSizes() {
        load();
        return sizes;
    }

    private static void load() {
        if (unifont != null) return;
        byte[][] glyphs = readUnifont();
        if (glyphs != null) {
            unifont = glyphs;
            sizes = new byte[65536];
            for (int c = 0; c < 65536; c++) sizes[c] = glyphSize(glyphs[c]);
            if (asciiFallback) java.util.Arrays.fill(PAGES, null); // fonts that read again get the real pages
            asciiFallback = false;
        } else if (sizes == null) {
            asciiFallback = true;
            sizes = new byte[65536];
            java.util.Arrays.fill(sizes, (byte) 0x0F);
            asciiPage0();
        }
    }

    private static byte glyphSize(byte[] rows) {
        if (rows == null) return 0;
        int width = rows.length / 16 * 8;
        int min = 99, max = -1;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < width; x++) {
                if (lit(rows, width, x, y)) {
                    min = Math.min(min, x);
                    max = Math.max(max, x);
                }
            }
        }
        return max < 0 ? 0 : (byte) (min << 4 | max);
    }

    private static boolean lit(byte[] rows, int width, int x, int y) {
        int bytesPerRow = width / 8;
        return (rows[y * bytesPerRow + x / 8] & (0x80 >> (x % 8))) != 0;
    }

    private static BufferedImage unifontPage(int page) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 256; i++) {
            byte[] rows = unifont[page << 8 | i];
            if (rows == null) continue;
            int width = rows.length / 16 * 8;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < width; x++) {
                    if (lit(rows, width, x, y)) image.setRGB((i % 16) * 16 + x, (i / 16) * 16 + y, 0xFFFFFFFF);
                }
            }
        }
        return image;
    }

    private static byte[][] readUnifont() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.getVanillaPackResources() == null) return null;
            var supplier = mc.getVanillaPackResources().getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("font/unifont.zip"));
            if (supplier == null) return null;
            try (ZipInputStream zip = new ZipInputStream(supplier.get())) {
                for (ZipEntry e; (e = zip.getNextEntry()) != null; ) {
                    if (!e.getName().endsWith(".hex")) continue;
                    byte[][] glyphs = new byte[65536][];
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zip, StandardCharsets.US_ASCII));
                    for (String line; (line = reader.readLine()) != null; ) {
                        int colon = line.indexOf(':');
                        if (colon < 0) continue;
                        int codePoint = Integer.parseInt(line, 0, colon, 16);
                        int digits = line.length() - colon - 1;
                        if (codePoint > 0xFFFF || (digits != 32 && digits != 64)) continue;
                        byte[] rows = new byte[digits / 2];
                        for (int i = 0; i < rows.length; i++) rows[i] = (byte) Integer.parseInt(line, colon + 1 + i * 2, colon + 3 + i * 2, 16);
                        glyphs[codePoint] = rows;
                    }
                    return glyphs;
                }
            }
        } catch (Exception | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Fonts").warn("Could not read Unifont for legacy unicode fonts: {}", e.toString());
        }
        return null;
    }

    /** Page 0 from the ASCII font at double size (only until the game's resources can be read). */
    private static byte[] asciiPage0() {
        if (PAGES[0] != null) return PAGES[0];
        BufferedImage ascii = readAscii();
        if (ascii == null || ascii.getWidth() != 128) return null;
        BufferedImage page = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
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
            if (max >= 0) sizes[index] = (byte) ((min * 2) << 4 | Math.min(15, max * 2 + 1));
            else sizes[index] = 0x07;
        }
        return PAGES[0] = encode(page);
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
