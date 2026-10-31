package org.kylepitcock.legacyconnector.core.pack;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/**
 * Legacy mods drew much of their art from the vanilla textures of their time: plain copies under their own names (a stone
 * door that is old stone, fences that are old planks, Galacticraft's unlit torch) and machines painted onto old smooth
 * stone, cobblestone, planks or a dispenser front. Since the 1.14 texture update these sit badly next to the modern
 * blocks. This class recognises both from fingerprints of the old vanilla textures (tools/gen_texture_fingerprints.py;
 * the table holds hashes, never Mojang's pixels):
 * <ul>
 *     <li><b>exact copies</b> (also nearest-neighbour upscales) of an old block, item or GUI texture become the modern
 *     texture of the same thing;</li>
 *     <li><b>backgrounds</b>: when at least 30% of a block texture's pixels are an old vanilla base texture at the same
 *     place (the art the mod painted on top is the rest), those pixels take the modern base's pixels; the mod's own art
 *     stays. Ores are left to {@code OreBackgrounds}, which runs first.</li>
 * </ul>
 * Pure Java (no game classes): {@link ModernizedTextures} applies it in game, {@link #main} reports on jars offline.
 */
public final class TextureModernizer {
    /** Bumped when the matching changes: cached classifications of older versions are recomputed. */
    static final int VERSION = 1;
    /** Share of a texture's pixels that must be the old base (as {@code OreBackgrounds.MIN_BACKGROUND_SHARE}). */
    private static final double MIN_BASE_SHARE = 0.3D;
    /** Distinct colours the matched base pixels must have: a white or flat texture is not old snow or quartz. */
    private static final int MIN_BASE_COLOURS = 4;

    public enum Kind { EXACT, BASE }

    /** One texture to replace: the modern vanilla texture ({@code block/stone}), and for a background the old base's name. */
    public record Hit(String path, Kind kind, String modern, String base) {}

    private record Base(String legacy, String modern, byte[] fingerprints) {}

    private final Map<String, String> exact = new HashMap<>();
    /** Sizes of the old textures ({@code w << 32 | h}): a texture of another size (or a 2x, 4x... upscale) is not decoded. */
    private final Set<Long> exactSizes = new HashSet<>();
    private final List<Base> bases = new ArrayList<>();
    private final Map<String, Base> basesByName = new HashMap<>();
    private final String digest;

    private TextureModernizer(String table) {
        for (String line : table.split("\n")) {
            String[] p = line.trim().split(" ");
            if (p.length == 4 && p[0].equals("E")) {
                exact.put(p[1], p[2]);
                String[] size = p[3].split("x");
                exactSizes.add(Long.parseLong(size[0]) << 32 | Long.parseLong(size[1]));
            } else if (p.length == 4 && p[0].equals("B") && p[3].length() == 512) {
                Base b = new Base(p[1], p[2], HexFormat.of().parseHex(p[3]));
                bases.add(b);
                basesByName.put(b.legacy(), b);
            }
        }
        digest = sha1Hex(table.getBytes(StandardCharsets.UTF_8)).substring(0, 16);
    }

    private static volatile TextureModernizer instance;

    /** The fingerprint table shipped in the jar; an empty table when it is missing. */
    public static TextureModernizer get() {
        TextureModernizer t = instance;
        if (t != null) return t;
        String table = "";
        try (InputStream in = TextureModernizer.class.getResourceAsStream("/legacyconnector/legacy/texture-fingerprints.txt")) {
            if (in != null) table = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // no table: nothing is recognised
        }
        return instance = new TextureModernizer(table);
    }

    /** Identifies the table and the matching rules (cache key part). */
    public String key() {
        return VERSION + "-" + digest;
    }

    /** What the texture at {@code path} is, or null when it is the mod's own art. */
    public Hit classify(String path, byte[] png) {
        if (!worthDecoding(png)) return null;
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException | RuntimeException e) {
            return null;
        }
        if (image == null) return null;
        int w = image.getWidth(), h = image.getHeight();
        if (w > 1024 || h > 1024) return null;
        int[] px = argb(image);
        // exact copies, also at 2x, 4x ... as long as the picture is made of solid blocks
        int[] level = px;
        int lw = w, lh = h;
        while (true) {
            String modern = exact.get(exactHash(lw, lh, level));
            if (modern != null) return new Hit(path, Kind.EXACT, modern, null);
            int[] half = halve(level, lw, lh);
            if (half == null) break;
            level = half;
            lw /= 2;
            lh /= 2;
        }
        if (w != h || w % 16 != 0 || w > 64) return null;
        // ores belong to OreBackgrounds (configurable host rocks, deepslate variants)
        String stem = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
        if (org.kylepitcock.legacyconnector.v1_7_10.client.OreBackgrounds.isOreName(stem)) return null;
        int[] fp = fingerprints(px, w);
        int needed = (int) Math.ceil(MIN_BASE_SHARE * px.length);
        int best = -1;
        Base bestBase = null;
        for (Base b : bases) {
            int raw = 0;
            for (int i = 0; i < fp.length; i++) if (fp[i] == (b.fingerprints()[baseIndex(i, w)] & 0xFF)) raw++;
            if (raw < needed || raw <= best) continue;
            boolean[] mask = baseMask(fp, w, b);
            int n = 0;
            Set<Integer> colours = new HashSet<>();
            for (int i = 0; i < mask.length; i++) {
                if (mask[i]) {
                    n++;
                    if (colours.size() < MIN_BASE_COLOURS) colours.add(px[i]);
                }
            }
            if (n > best && n >= needed && colours.size() >= MIN_BASE_COLOURS) {
                best = n;
                bestBase = b;
            }
        }
        return bestBase == null ? null : new Hit(path, Kind.BASE, bestBase.modern(), bestBase.legacy());
    }

    /**
     * The modernised texture, or null when it cannot be made. {@code modernTexture} reads a vanilla texture of the running
     * game by name ({@code block/stone} -> {@code textures/block/stone.png}).
     */
    public byte[] apply(Hit hit, byte[] png, Function<String, byte[]> modernTexture) throws IOException {
        byte[] modernPng = modernTexture.apply(hit.modern());
        if (modernPng == null) return null;
        BufferedImage modern = ImageIO.read(new ByteArrayInputStream(modernPng));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        if (modern == null || image == null) return null;
        int w = image.getWidth(), h = image.getHeight();
        int mw = modern.getWidth(), mh = modern.getHeight();
        int[] mpx = argb(modern);
        int[] px;
        if (hit.kind() == Kind.EXACT) {
            if (w == mw && h == mh) return modernPng;
            px = new int[w * h];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = mpx[(y * mh / h) * mw + x * mw / w];
        } else {
            Base base = basesByName.get(hit.base());
            if (base == null || w != h) return null;
            px = argb(image);
            boolean[] mask = baseMask(fingerprints(px, w), w, base);
            for (int i = 0; i < px.length; i++) {
                if (!mask[i]) continue;
                int x = i % w, y = i / w;
                px[i] = mpx[(y * mh / h) * mw + x * mw / w];
            }
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, px, 0, w);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(out, "png", bytes);
        return bytes.toByteArray();
    }

    /**
     * The pixels of a (square) texture that show the old base: opaque, with the fingerprint of the base's pixel at the same
     * place (an upscaled texture compares each pixel with the base pixel it covers), and next to another such pixel (a lone
     * hit inside the mod's art is a fingerprint collision, one in 256).
     */
    private static boolean[] baseMask(int[] fp, int size, Base base) {
        boolean[] hit = new boolean[fp.length];
        for (int i = 0; i < fp.length; i++) hit[i] = fp[i] == (base.fingerprints()[baseIndex(i, size)] & 0xFF);
        boolean[] mask = new boolean[fp.length];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int i = y * size + x;
                if (!hit[i]) continue;
                mask[i] = x > 0 && hit[i - 1] || x < size - 1 && hit[i + 1] || y > 0 && hit[i - size] || y < size - 1 && hit[i + size];
            }
        }
        return mask;
    }

    /** The 16x16 base pixel a pixel of a square texture covers (an upscaled texture: several pixels cover one). */
    private static int baseIndex(int i, int size) {
        int scale = size / 16;
        return (i / size / scale) * 16 + (i % size) / scale;
    }

    /** Each pixel's fingerprint at the base position it covers; -1 for pixels that are not opaque (never base). */
    private static int[] fingerprints(int[] px, int size) {
        int[] fp = new int[px.length];
        for (int i = 0; i < px.length; i++) fp[i] = (px[i] >>> 24) == 255 ? fingerprint(px[i], baseIndex(i, size)) : -1;
        return fp;
    }

    /**
     * Whether a PNG's size (from its header) can be a copy or a background at all; most mod textures are not decoded
     * (models, GUIs and high resolution art).
     */
    private boolean worthDecoding(byte[] png) {
        if (png.length < 24 || png[12] != 'I' || png[13] != 'H' || png[14] != 'D' || png[15] != 'R') return true; // not a plain PNG: let ImageIO decide
        long w = readInt(png, 16) & 0xFFFFFFFFL, h = readInt(png, 20) & 0xFFFFFFFFL;
        if (w == h && w % 16 == 0 && w <= 64 && !bases.isEmpty()) return true;
        if (exactSizes.contains(w << 32 | h)) return true;
        // upscaled copies: only of icons (a 512x512 model texture is not a 2x GUI sheet often enough to decode them all)
        for (int k = 0; k < 3 && w <= 128 && h <= 128; k++) {
            if (w % 2 != 0 || h % 2 != 0) break;
            w /= 2;
            h /= 2;
            if (exactSizes.contains(w << 32 | h)) return true;
        }
        return false;
    }

    private static int readInt(byte[] b, int at) {
        return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | b[at + 3] & 0xFF;
    }

    /** The picture at half size when every 2x2 block is one colour (a nearest-neighbour upscale), else null. */
    private static int[] halve(int[] px, int w, int h) {
        if (w % 2 != 0 || h % 2 != 0 || w < 2 || h < 2) return null;
        int[] out = new int[(w / 2) * (h / 2)];
        for (int y = 0; y < h; y += 2) {
            for (int x = 0; x < w; x += 2) {
                int p = px[y * w + x];
                if (px[y * w + x + 1] != p || px[(y + 1) * w + x] != p || px[(y + 1) * w + x + 1] != p) return null;
                out[(y / 2) * (w / 2) + x / 2] = p;
            }
        }
        return out;
    }

    /** First 16 hex digits of SHA-1 over width, height and the ARGB pixels (as tools/gen_texture_fingerprints.py). */
    static String exactHash(int w, int h, int[] px) {
        byte[] bytes = new byte[8 + px.length * 4];
        putInt(bytes, 0, w);
        putInt(bytes, 4, h);
        for (int i = 0; i < px.length; i++) putInt(bytes, 8 + i * 4, px[i]);
        return sha1Hex(bytes).substring(0, 16);
    }

    /** Position-salted 8-bit hash of one pixel: murmur3's finaliser (as tools/gen_texture_fingerprints.py). */
    static int fingerprint(int argb, int index) {
        int h = argb ^ (index * 0x9E3779B9);
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return h & 0xFF;
    }

    private static void putInt(byte[] b, int at, int v) {
        b[at] = (byte) (v >>> 24);
        b[at + 1] = (byte) (v >>> 16);
        b[at + 2] = (byte) (v >>> 8);
        b[at + 3] = (byte) v;
    }

    private static String sha1Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Plain ARGB pixels, fully transparent ones as 0 (their colour is invisible and editors store anything there).
     * {@code getRGB} converts greyscale PNGs through a colour space, which makes them far lighter; those are read from the
     * raster (as {@code OreBackgrounds.pixel}).
     */
    static int[] argb(BufferedImage image) {
        int w = image.getWidth(), h = image.getHeight();
        int[] px;
        var model = image.getColorModel();
        if (model.getNumColorComponents() == 1 && model.getPixelSize() > 1 && !(model instanceof java.awt.image.IndexColorModel)) {
            px = new int[w * h];
            var raster = image.getRaster();
            int shift = Math.max(0, model.getComponentSize(0) - 8);
            int alphaShift = model.hasAlpha() ? Math.max(0, model.getComponentSize(1) - 8) : 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int v = raster.getSample(x, y, 0) >> shift;
                    int a = model.hasAlpha() ? raster.getSample(x, y, 1) >> alphaShift : 255;
                    px[y * w + x] = (a << 24) | (v << 16) | (v << 8) | v;
                }
            }
        } else {
            px = image.getRGB(0, 0, w, h, null, 0, w);
        }
        for (int i = 0; i < px.length; i++) if ((px[i] >>> 24) == 0) px[i] = 0;
        return px;
    }

    /** Whether a jar entry is a texture this applies to: a mod's own (not vanilla overrides), not animated. */
    static boolean candidate(String entry, Set<String> names) {
        return entry.startsWith("assets/") && entry.endsWith(".png") && entry.contains("/textures/") && !entry.startsWith("assets/minecraft/")
                && !names.contains(entry + ".mcmeta");
    }

    /** Classifies every texture of a jar (or mod directory listing). */
    public List<Hit> scan(ZipFile zip) throws IOException {
        Set<String> names = new HashSet<>();
        for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) names.add(e.nextElement().getName());
        List<String> textures = new ArrayList<>();
        for (String name : new java.util.TreeSet<>(names)) if (candidate(name, names)) textures.add(name);
        // decoding is the cost (a big jar has thousands of textures); ZipFile reads concurrently
        return textures.parallelStream().map(name -> {
            ZipEntry entry = zip.getEntry(name);
            if (entry == null || entry.getSize() > 4_000_000) return null;
            try {
                if (!worthDecoding(header(zip, entry))) return null;
                try (InputStream in = zip.getInputStream(entry)) {
                    return classify(name, in.readAllBytes());
                }
            } catch (IOException | RuntimeException e) {
                return null;
            }
        }).filter(java.util.Objects::nonNull).toList();
    }

    /** The first bytes of an entry (enough for the PNG header). */
    private static byte[] header(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readNBytes(24);
        }
    }

    /**
     * Offline report: {@code <modern client jar> <sample dir> <jar>...}. Prints how many textures of each jar each rule
     * changes and writes before/after pictures (8x) of a few per jar into the sample directory.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: TextureModernizer <1.21.1 client jar> <sample dir> <legacy jar>...");
            return;
        }
        TextureModernizer m = get();
        System.out.println("fingerprint table: " + m.exact.size() + " exact, " + m.bases.size() + " bases");
        Path samples = Path.of(args[1]);
        Files.createDirectories(samples);
        try (ZipFile client = new ZipFile(args[0])) {
            Function<String, byte[]> modern = name -> {
                ZipEntry e = client.getEntry("assets/minecraft/textures/" + name + ".png");
                if (e == null) return null;
                try (InputStream in = client.getInputStream(e)) {
                    return in.readAllBytes();
                } catch (IOException ex) {
                    return null;
                }
            };
            int[] total = {0, 0};
            for (int a = 2; a < args.length; a++) {
                Path jar = Path.of(args[a]);
                try (ZipFile zip = new ZipFile(jar.toFile())) {
                    long start = System.nanoTime();
                    List<Hit> hits = m.scan(zip);
                    long ms = (System.nanoTime() - start) / 1_000_000;
                    int exactCount = 0, baseCount = 0, made = 0;
                    Map<String, Integer> perTarget = new TreeMap<>();
                    String stem = jar.getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("\\.jar$", "");
                    for (Hit hit : hits) {
                        if (hit.kind() == Kind.EXACT) exactCount++;
                        else baseCount++;
                        perTarget.merge((hit.kind() == Kind.EXACT ? "copy of " : "on ") + hit.modern(), 1, Integer::sum);
                        byte[] before;
                        try (InputStream in = zip.getInputStream(zip.getEntry(hit.path()))) {
                            before = in.readAllBytes();
                        }
                        byte[] after = m.apply(hit, before, modern);
                        if (after == null) continue;
                        if (made++ < 40) writeSample(samples.resolve(stem + "_" + hit.path().replaceAll(".*/textures/", "").replace('/', '_')), before, after);
                    }
                    total[0] += exactCount;
                    total[1] += baseCount;
                    System.out.printf("%s: modernized %d textures (%d exact copies, %d backgrounds) in %d ms%n", jar.getFileName(), exactCount + baseCount,
                            exactCount, baseCount, ms);
                    for (Hit hit : hits) System.out.println("    " + hit.path() + " -> " + (hit.kind() == Kind.EXACT ? "copy of " + hit.modern() : "on " + hit.base() + " -> " + hit.modern()));
                } catch (IOException | RuntimeException e) {
                    System.out.println(jar.getFileName() + ": " + e);
                }
            }
            System.out.printf("total: %d exact copies, %d backgrounds%n", total[0], total[1]);
        }
    }

    /** before | after, side by side at 8x, as one PNG. */
    private static void writeSample(Path file, byte[] before, byte[] after) throws IOException {
        BufferedImage a = ImageIO.read(new ByteArrayInputStream(before));
        BufferedImage b = ImageIO.read(new ByteArrayInputStream(after));
        if (a == null || b == null) return;
        int scale = Math.max(1, 128 / Math.max(a.getWidth(), a.getHeight()));
        int w = a.getWidth() * scale, h = a.getHeight() * scale;
        BufferedImage out = new BufferedImage(w * 2 + 8, h, BufferedImage.TYPE_INT_ARGB);
        var g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(a, 0, 0, w, h, null);
        g.drawImage(b, w + 8, 0, w, h, null);
        g.dispose();
        ImageIO.write(out, "png", file.toFile());
    }
}
