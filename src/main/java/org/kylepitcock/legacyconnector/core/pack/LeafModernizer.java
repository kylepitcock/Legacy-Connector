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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/**
 * Legacy mods drew most of their leaves as recolours of the old vanilla oak, spruce or jungle leaves: the same cut-out shape
 * and pixel pattern in other colours (Thaumcraft's silverwood, Witchery's rowan, Natura's sakura...). The 1.14 texture
 * update gave the vanilla leaves a new pattern, so those leaves look old next to modern trees. This class rebuilds such a
 * texture from the modern vanilla leaves of the same kind, in the mod's colours:
 * <ul>
 *     <li><b>recognition</b>: the cut-out mask of each old vanilla leaf texture is known by its hash only
 *     ({@code leaf-fingerprints.txt}, tools/gen_leaf_fingerprints.py, no pixels). A mod texture whose mask hashes to one of
 *     them (also after removing its decoration pixels: berries, flowers, fruit) is a recolour; it also becomes the reference
 *     of that kind's pattern. Textures whose mask differs more (edited holes) are recolours when their pixel pattern
 *     correlates with such a reference (rank correlation of the brightness).</li>
 *     <li><b>conversion</b>: the mod's leaf colours sorted by brightness form a colour ramp; each modern pixel takes the ramp
 *     colour at its own brightness rank (histogram matching), so hue, saturation and brightness range stay the mod's, and a
 *     grey texture the game tints stays grey. The cut-out comes from the modern texture; decoration pixels (colours that do
 *     not follow the leaf ramp) are kept where the mod drew them.</li>
 *     <li><b>fast (opaque) variants</b> (a fully opaque copy of a recognised texture with filled holes) get the converted
 *     pattern with the holes filled from the mod's own hole colours, as vanilla's fast leaves are opaque.</li>
 * </ul>
 * Hand-drawn leaves (other shapes), animated textures and sizes other than 16x16 (or clean upscales of it) stay as they are.
 * Pure Java (no game classes): {@code ModernLeaves} applies it in game, {@link #main} writes before/after samples offline.
 */
public final class LeafModernizer {
    /** Rank correlation of the brightness with a reference that makes an edited-shape texture a recolour. */
    private static final double MIN_CORRELATION = 0.7D;
    /** Mask pixels (besides decoration) an edited-shape recolour may differ from the old shape in. */
    private static final int MAX_MASK_DIFFERENCE = 64;
    /** Decoration pixels tried as "added on top of a hole" when matching the mask hash (2^n hashes). */
    private static final int MAX_ENUMERATED = 16;
    /** Drawn pixels a cut-out leaf texture has at least (the old oak leaves: 155 of 256). */
    private static final int MIN_FANCY_PIXELS = 120;
    /** RMS distance of the leaf colours from one colour ramp above which a texture is multi-coloured art. */
    private static final double MAX_SPREAD = 20D;
    /** Mean colour difference (per channel) between a fast texture and a fancy one on the leaf pixels to pair them. */
    private static final double MAX_PAIR_DIFFERENCE = 18D;

    /**
     * One texture to look at: its name (for the result), its PNG bytes, whether it is animated (has a .mcmeta), and whether
     * it only serves as a reference of an old pattern (another leaf texture of the pack, e.g. a plain copy of the old jungle
     * leaves: it gets no result).
     */
    public record Source(String name, byte[] png, boolean animated, boolean referenceOnly) {
        public Source(String name, byte[] png, boolean animated) {
            this(name, png, animated, false);
        }
    }

    /** The outcome for one texture: the new PNG, or null with the reason it was left alone. */
    public record Result(String name, byte[] png, String note) {
        public boolean converted() {
            return png != null;
        }
    }

    private final Map<String, String> kinds = new HashMap<>();
    private final String digest;

    private LeafModernizer(String table) {
        for (String line : table.split("\n")) {
            String[] p = line.trim().split(" ");
            if (p.length == 3 && p[0].equals("L")) kinds.put(p[2], p[1]);
        }
        digest = sha1Hex(table.getBytes(StandardCharsets.UTF_8)).substring(0, 12);
    }

    private static volatile LeafModernizer instance;

    /** The fingerprint table shipped in the jar; nothing is recognised when it is missing. */
    public static LeafModernizer get() {
        LeafModernizer m = instance;
        if (m != null) return m;
        String table = "";
        try (InputStream in = LeafModernizer.class.getResourceAsStream("/legacyconnector/legacy/leaf-fingerprints.txt")) {
            if (in != null) table = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // no table
        }
        return instance = new LeafModernizer(table);
    }

    /** Identifies the table (cache key part). */
    public String key() {
        return digest;
    }

    /** A decoded 16x16 leaf texture. */
    private static final class Leaf {
        final Source source;
        final int scale;
        final int[] px = new int[256];
        final boolean[] mask = new boolean[256];
        boolean[] decoration;
        /** How far the leaf pixels stray from one colour ramp (RMS, colour units): large for multi-coloured art. */
        double spread;
        int opaque;
        /** Matched kind (modern texture name), the old shape and the pixels the pattern is compared on. */
        String kind;
        boolean[] shape;
        String how;
        int[] result;

        Leaf(Source source, int[] px16, int scale) {
            this.source = source;
            this.scale = scale;
            System.arraycopy(px16, 0, px, 0, 256);
            for (int i = 0; i < 256; i++) {
                mask[i] = (px[i] >>> 24) >= 128;
                if (mask[i]) opaque++;
            }
        }
    }

    /**
     * Converts what it can of the given textures (the icons of legacy leaf blocks); {@code modernTexture} reads a vanilla
     * texture of the running game by name ({@code block/oak_leaves}). One result per source that is not reference-only, in order.
     */
    public List<Result> convert(List<Source> sources, Function<String, byte[]> modernTexture) {
        Map<String, Result> results = new LinkedHashMap<>();
        List<Leaf> fancy = new ArrayList<>();
        List<Leaf> fast = new ArrayList<>();
        List<Leaf> referenceOnly = new ArrayList<>();
        for (Source s : sources) {
            if (s.referenceOnly()) {
                Leaf ref = decodeQuietly(s);
                if (ref != null && ref.opaque >= MIN_FANCY_PIXELS && ref.opaque < 250) {
                    ref.decoration = decoration(ref);
                    if (ref.spread <= MAX_SPREAD) referenceOnly.add(ref);
                }
                continue;
            }
            String why = null;
            Leaf leaf = null;
            if (s.animated()) why = "animated";
            else {
                try {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(s.png()));
                    if (image == null) why = "not a readable PNG";
                    else {
                        int w = image.getWidth(), h = image.getHeight();
                        if (w != h || w % 16 != 0) why = w + "x" + h + ", not square 16x16";
                        else {
                            int[] px16 = downscale(TextureModernizer.argb(image), w);
                            if (px16 == null) why = w + "x" + h + ", not an upscale of a 16x16 texture";
                            else leaf = new Leaf(s, px16, w / 16);
                        }
                    }
                } catch (IOException | RuntimeException e) {
                    why = "not readable (" + e + ")";
                }
            }
            if (leaf != null && leaf.opaque < 64) {
                why = "only " + leaf.opaque + " of 256 pixels drawn, no leaf texture";
                leaf = null;
            }
            results.put(s.name(), new Result(s.name(), null, why));
            if (leaf == null) continue;
            leaf.decoration = decoration(leaf);
            if (leaf.opaque >= 250) fast.add(leaf);
            else if (leaf.opaque < MIN_FANCY_PIXELS) results.put(s.name(), new Result(s.name(), null, "only " + leaf.opaque + " of 256 pixels drawn, not a whole leaf texture"));
            else if (leaf.spread > MAX_SPREAD) results.put(s.name(), new Result(s.name(), null,
                    String.format(Locale.ROOT, "multi-coloured (colours stray %.0f from one ramp), not a recolour", leaf.spread)));
            else fancy.add(leaf);
        }
        // 1: shapes known by their hash (references of each kind's old pattern)
        Map<String, List<Leaf>> references = new HashMap<>();
        for (Leaf leaf : fancy) {
            if (matchHash(leaf)) references.computeIfAbsent(leaf.kind, k -> new ArrayList<>()).add(leaf);
        }
        for (Leaf leaf : referenceOnly) {
            if (matchHash(leaf)) references.computeIfAbsent(leaf.kind, k -> new ArrayList<>()).add(leaf);
        }
        // 2: edited shapes whose pattern follows a reference
        for (Leaf leaf : fancy) {
            if (leaf.kind != null) continue;
            String why = matchPattern(leaf, references);
            if (why != null) results.put(leaf.source.name(), new Result(leaf.source.name(), null, why));
        }
        Map<String, int[]> modern = new HashMap<>();
        for (Leaf leaf : fancy) {
            if (leaf.kind == null) continue;
            int[] pattern = modern.computeIfAbsent(leaf.kind, k -> modernPixels(modernTexture.apply(k)));
            if (pattern == null) {
                results.put(leaf.source.name(), new Result(leaf.source.name(), null, "the modern texture " + leaf.kind + " is not available"));
                leaf.kind = null;
                continue;
            }
            leaf.result = recolour(leaf, pattern);
            int kept = indices(leaf.decoration).length;
            results.put(leaf.source.name(), new Result(leaf.source.name(), encode(leaf.result, leaf.scale),
                    kept == 0 ? leaf.how : leaf.how + ", " + kept + " decoration pixels kept"));
        }
        // 3: fast variants: an opaque copy of a converted fancy texture
        for (Leaf leaf : fast) {
            // the closest converted fancy texture; one named alike (leaves_fast / leaves_fancy, leaves_opaque / leaves) wins
            Leaf partner = null;
            double best = Double.MAX_VALUE;
            boolean named = false;
            for (Leaf f : fancy) {
                if (f.result == null) continue;
                double d = pairDifference(leaf, f);
                if (d > MAX_PAIR_DIFFERENCE) continue;
                boolean alike = stem(f.source.name()).equals(stem(leaf.source.name()));
                if (alike && !named || alike == named && d < best) {
                    best = d;
                    partner = f;
                    named = alike;
                }
            }
            if (partner == null || best > MAX_PAIR_DIFFERENCE) {
                results.put(leaf.source.name(), new Result(leaf.source.name(), null,
                        "opaque, and no converted leaf texture it is the fast variant of"));
                continue;
            }
            int[] px = opaque(leaf, partner, modern.get(partner.kind));
            results.put(leaf.source.name(), new Result(leaf.source.name(), encode(px, leaf.scale),
                    "fast variant of " + partner.source.name() + " (" + partner.kind + " pattern, opaque)"));
        }
        return new ArrayList<>(results.values());
    }

    /** A 16x16 (or clean upscale) still texture as a leaf, or null. */
    private static Leaf decodeQuietly(Source s) {
        if (s.animated()) return null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(s.png()));
            if (image == null || image.getWidth() != image.getHeight() || image.getWidth() % 16 != 0) return null;
            int[] px16 = downscale(TextureModernizer.argb(image), image.getWidth());
            return px16 == null ? null : new Leaf(s, px16, image.getWidth() / 16);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Whether the leaf's mask (or the mask without some decoration pixels) hashes to a known old shape. */
    private boolean matchHash(Leaf leaf) {
        String kind = kinds.get(maskHash(leaf.mask));
        if (kind != null) {
            leaf.kind = kind;
            leaf.shape = leaf.mask.clone();
            leaf.how = kind + " pattern (old shape)";
            return true;
        }
        int[] deco = indices(leaf.decoration);
        if (deco.length == 0 || deco.length > MAX_ENUMERATED) return false;
        boolean[] trial = leaf.mask.clone();
        for (int bits = 1; bits < 1 << deco.length; bits++) {
            for (int j = 0; j < deco.length; j++) trial[deco[j]] = (bits & 1 << j) == 0;
            kind = kinds.get(maskHash(trial));
            if (kind != null) {
                leaf.kind = kind;
                leaf.shape = trial.clone();
                leaf.how = kind + " pattern (old shape under the decoration)";
                return true;
            }
        }
        return false;
    }

    /** Matches an edited shape against the references; the reason it is not a recolour, or null when it matched. */
    private static String matchPattern(Leaf leaf, Map<String, List<Leaf>> references) {
        if (references.isEmpty()) return "not the old vanilla leaf shape (and no recoloured vanilla leaves to compare its pattern with)";
        double bestRho = -2;
        int bestDiff = 0;
        Leaf bestRef = null;
        for (List<Leaf> refs : references.values()) {
            for (Leaf ref : refs) {
                int diff = 0;
                List<Integer> common = new ArrayList<>();
                for (int i = 0; i < 256; i++) {
                    if (leaf.decoration[i] || ref.decoration[i]) continue;
                    if (leaf.mask[i] != ref.shape[i]) diff++;
                    else if (leaf.mask[i]) common.add(i);
                }
                if (common.size() < 48) continue;
                double[] a = new double[common.size()], b = new double[common.size()];
                for (int k = 0; k < a.length; k++) {
                    a[k] = luma(leaf.px[common.get(k)]);
                    b[k] = luma(ref.px[common.get(k)]);
                }
                double rho = spearman(a, b);
                if (rho > bestRho) {
                    bestRho = rho;
                    bestDiff = diff;
                    bestRef = ref;
                }
            }
        }
        if (bestRef == null) return "not the old vanilla leaf shape";
        String best = String.format(Locale.ROOT, "pattern correlation %.2f with %s, %d mask pixels differ", bestRho, bestRef.source.name(), bestDiff);
        if (bestRho < MIN_CORRELATION || bestDiff > MAX_MASK_DIFFERENCE) return "not a recolour of the old vanilla leaves (best: " + best + ")";
        leaf.kind = bestRef.kind;
        leaf.shape = bestRef.shape;
        leaf.how = bestRef.kind + " pattern (edited shape, " + best + ")";
        growDecoration(leaf, bestRef);
        return null;
    }

    /**
     * Decoration that is close to the leaf colours (white petals on grey leaves) but not where the reference pattern puts that
     * brightness: next to a decoration pixel, a pixel whose brightness is far from the recoloured reference's belongs to it.
     */
    private static void growDecoration(Leaf leaf, Leaf ref) {
        List<Double> own = new ArrayList<>(), theirs = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            if (leaf.mask[i] && ref.mask[i] && !leaf.decoration[i] && !ref.decoration[i]) {
                own.add(luma(leaf.px[i]));
                theirs.add(luma(ref.px[i]));
            }
        }
        if (own.size() < 48) return;
        own.sort(null);
        theirs.sort(null);
        for (int round = 0; round < 4; round++) {
            boolean[] grown = leaf.decoration.clone();
            for (int i = 0; i < 256; i++) {
                if (!leaf.mask[i] || leaf.decoration[i] || !ref.mask[i] || ref.decoration[i]) continue;
                int x = i % 16, y = i / 16;
                boolean[] d = leaf.decoration;
                if (!(x > 0 && d[i - 1] || x < 15 && d[i + 1] || y > 0 && d[i - 16] || y < 15 && d[i + 16])) continue;
                // the brightness the leaf's own ramp has at the reference pixel's rank
                double q = quantileOf(theirs, luma(ref.px[i]));
                double expected = own.get((int) Math.round(q * (own.size() - 1)));
                grown[i] = Math.abs(luma(leaf.px[i]) - expected) > 40;
            }
            if (Arrays.equals(grown, leaf.decoration)) break;
            leaf.decoration = grown;
        }
    }

    /** The share (0..1) of a sorted list below a value. */
    private static double quantileOf(List<Double> sorted, double v) {
        int lo = 0, hi = sorted.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (sorted.get(mid) < v) lo = mid + 1;
            else hi = mid;
        }
        return sorted.size() <= 1 ? 0.5 : Math.min(1, (double) lo / (sorted.size() - 1));
    }

    /**
     * Pixels that do not follow the leaf colours (berries, flowers, fruit): far from a robust linear fit of colour against
     * brightness, or far brighter or darker than the leaf pixels.
     */
    private static boolean[] decoration(Leaf leaf) {
        int[] idx = indices(leaf.mask);
        boolean[] out = new boolean[256];
        if (idx.length < 16) return out;
        // First guess, robust to up to half the pixels being decoration (Natura's apples): the leaf colour is the median tint
        // (each channel over the brightness), as for a grey texture the game tints.
        double[][] fit = new double[3][2];
        for (int c = 0; c < 3; c++) {
            double[] ratio = new double[idx.length];
            for (int k = 0; k < idx.length; k++) ratio[k] = channel(leaf.px[idx[k]], c) / Math.max(1, luma(leaf.px[idx[k]]));
            Arrays.sort(ratio);
            fit[c][1] = ratio[ratio.length / 2];
        }
        boolean[] use = new boolean[idx.length];
        double[] res = new double[idx.length];
        for (int round = 0; round < 3; round++) {
            for (int k = 0; k < idx.length; k++) res[k] = residual(leaf.px[idx[k]], fit);
            double[] sorted = res.clone();
            Arrays.sort(sorted);
            // the pixels near the model (and at least half of them) refine it as a linear fit
            double cut = Math.max(sorted[idx.length / 2], 40);
            for (int k = 0; k < idx.length; k++) use[k] = res[k] <= cut;
            fit = fitChannels(leaf.px, idx, use);
        }
        for (int k = 0; k < idx.length; k++) res[k] = residual(leaf.px[idx[k]], fit);
        // brightness range of the leaf pixels
        List<Double> lumas = new ArrayList<>();
        double rms = 0;
        int n = 0;
        for (int k = 0; k < idx.length; k++) {
            if (!use[k]) continue;
            lumas.add(luma(leaf.px[idx[k]]));
            rms += res[k] * res[k];
            n++;
        }
        rms = Math.sqrt(rms / Math.max(1, n));
        leaf.spread = rms;
        lumas.sort(null);
        // (deciles: a few white flowers must not widen the range they are judged by)
        double lo = lumas.get((int) (lumas.size() * 0.1)), hi = lumas.get((int) (lumas.size() * 0.9) - 1);
        double limit = Math.max(36, 3.5 * rms);
        double margin = Math.max(24, (hi - lo) * 0.6);
        for (int k = 0; k < idx.length; k++) {
            double l = luma(leaf.px[idx[k]]);
            out[idx[k]] = res[k] > limit || l > hi + margin || l < lo - margin;
        }
        // a decoration's own outline and highlights (white petals around a yellow centre) are less extreme: next to a
        // decoration pixel, anything brighter or darker than the leaf pixels, or off their colour, belongs to it
        double[] resAt = new double[256];
        for (int k = 0; k < idx.length; k++) resAt[idx[k]] = res[k];
        for (int round = 0; round < 3; round++) {
            boolean[] grown = out.clone();
            for (int i : idx) {
                if (out[i]) continue;
                int x = i % 16, y = i / 16;
                boolean near = x > 0 && out[i - 1] || x < 15 && out[i + 1] || y > 0 && out[i - 16] || y < 15 && out[i + 16];
                if (!near) continue;
                double l = luma(leaf.px[i]);
                grown[i] = resAt[i] > limit / 2 || l > hi + 8 || l < lo - 8;
            }
            if (Arrays.equals(grown, out)) break;
            out = grown;
        }
        return out;
    }

    /** Least squares fit of each colour channel as a + b * brightness over the used pixels. */
    private static double[][] fitChannels(int[] px, int[] idx, boolean[] use) {
        double sl = 0, sll = 0;
        double[] sc = new double[3], slc = new double[3];
        int n = 0;
        for (int k = 0; k < idx.length; k++) {
            if (!use[k]) continue;
            int p = px[idx[k]];
            double l = luma(p);
            sl += l;
            sll += l * l;
            for (int c = 0; c < 3; c++) {
                double v = channel(p, c);
                sc[c] += v;
                slc[c] += l * v;
            }
            n++;
        }
        double[][] fit = new double[3][2];
        double den = n * sll - sl * sl;
        for (int c = 0; c < 3; c++) {
            double b = Math.abs(den) < 1e-6 ? 0 : (n * slc[c] - sl * sc[c]) / den;
            fit[c][1] = b;
            fit[c][0] = (sc[c] - b * sl) / Math.max(1, n);
        }
        return fit;
    }

    private static double residual(int p, double[][] fit) {
        double l = luma(p), s = 0;
        for (int c = 0; c < 3; c++) {
            double d = channel(p, c) - (fit[c][0] + fit[c][1] * l);
            s += d * d;
        }
        return Math.sqrt(s);
    }

    /**
     * The modern pattern in the leaf's colours: every modern leaf pixel takes the colour of the mod's leaf ramp at its own
     * brightness rank; decoration pixels stay where the mod drew them.
     */
    private static int[] recolour(Leaf leaf, int[] modern) {
        List<Integer> ramp = new ArrayList<>();
        for (int i = 0; i < 256; i++) if (leaf.mask[i] && !leaf.decoration[i]) ramp.add(leaf.px[i]);
        ramp.sort((x, y) -> Double.compare(luma(x), luma(y)));
        int[] out = new int[256];
        int[] idx = indices(modernMask(modern));
        double[] q = quantiles(modern, idx);
        for (int k = 0; k < idx.length; k++) {
            int i = idx[k];
            int c = sample(ramp, q[k]);
            out[i] = (modern[i] & 0xFF000000) | (c & 0xFFFFFF);
        }
        for (int i = 0; i < 256; i++) if (leaf.decoration[i]) out[i] = leaf.px[i];
        return out;
    }

    /** The fast variant: the partner's converted pattern, its holes filled with the fast texture's own hole colours. */
    private static int[] opaque(Leaf fast, Leaf partner, int[] modern) {
        int[] out = partner.result.clone();
        List<Integer> holeColours = new ArrayList<>();
        for (int i = 0; i < 256; i++) if (!partner.shape[i] && !partner.decoration[i]) holeColours.add(fast.px[i] | 0xFF000000);
        if (holeColours.isEmpty()) {
            for (int i = 0; i < 256; i++) if ((out[i] >>> 24) >= 128) holeColours.add(darken(out[i]));
        }
        holeColours.sort((x, y) -> Double.compare(luma(x), luma(y)));
        // holes take a pattern from the brightness of their modern neighbours
        List<Integer> holes = new ArrayList<>();
        for (int i = 0; i < 256; i++) if ((out[i] >>> 24) < 128) holes.add(i);
        double[] shade = new double[holes.size()];
        for (int k = 0; k < shade.length; k++) {
            int i = holes.get(k), x = i % 16, y = i / 16;
            double s = 0;
            int n = 0;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int j = ((y + dy) & 15) * 16 + ((x + dx) & 15);
                    if ((modern[j] >>> 24) >= 128) {
                        s += luma(modern[j]);
                        n++;
                    }
                }
            }
            shade[k] = n == 0 ? 0 : s / n;
        }
        double[] q = ranks(shade);
        for (int k = 0; k < shade.length; k++) out[holes.get(k)] = sample(holeColours, q[k]) | 0xFF000000;
        for (int i = 0; i < 256; i++) out[i] |= 0xFF000000;
        return out;
    }

    private static int darken(int c) {
        int r = (c >> 16 & 255) * 2 / 5, g = (c >> 8 & 255) * 2 / 5, b = (c & 255) * 2 / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** A texture name without the words that tell fancy and fast variants apart. */
    private static String stem(String name) {
        String n = name.toLowerCase(Locale.ROOT).replaceAll("\\.png$", "");
        // whole words (leaves_fast), or glued to leaf/leaves (Thaumcraft's silverwoodleaveslow)
        n = n.replaceAll("(?<=[^a-z]|^)(fancy|fast|opaque|low)(?=[^a-z]|$)|(?<=leaf|leaves)(fancy|fast|opaque|low)$", "");
        return n.replaceAll("[^a-z0-9:/]+", "");
    }

    /**
     * Mean per-channel difference between a fast texture and a fancy one on the fancy one's drawn pixels; infinite when more
     * than a few of those pixels differ clearly (a fast texture with flowers is not the fast variant of the plain leaves).
     */
    private static double pairDifference(Leaf fast, Leaf fancy) {
        double s = 0;
        int n = 0, off = 0;
        for (int i = 0; i < 256; i++) {
            if (!fancy.mask[i]) continue;
            int a = fast.px[i], b = fancy.px[i], worst = 0;
            for (int c = 0; c < 3; c++) {
                int d = Math.abs(channel(a, c) - channel(b, c));
                s += d;
                worst = Math.max(worst, d);
            }
            if (worst > 48) off++;
            n += 3;
        }
        return n == 0 || off > 4 ? Double.MAX_VALUE : s / n;
    }

    /** The colour of a sorted ramp at quantile q (0..1), interpolated between neighbours. */
    private static int sample(List<Integer> ramp, double q) {
        if (ramp.isEmpty()) return 0;
        double pos = q * (ramp.size() - 1);
        int a = (int) Math.floor(pos), b = Math.min(ramp.size() - 1, a + 1);
        double t = pos - a;
        int ca = ramp.get(a), cb = ramp.get(b);
        int r = (int) Math.round(channel(ca, 0) * (1 - t) + channel(cb, 0) * t);
        int g = (int) Math.round(channel(ca, 1) * (1 - t) + channel(cb, 1) * t);
        int bl = (int) Math.round(channel(ca, 2) * (1 - t) + channel(cb, 2) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** Brightness quantiles (0..1, ties share their mean rank) of the given modern pixels. */
    private static double[] quantiles(int[] px, int[] idx) {
        double[] l = new double[idx.length];
        for (int k = 0; k < idx.length; k++) l[k] = luma(px[idx[k]]);
        return ranks(l);
    }

    /** Ranks scaled to 0..1, ties sharing their mean rank. */
    private static double[] ranks(double[] v) {
        Integer[] order = new Integer[v.length];
        for (int i = 0; i < v.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(v[a], v[b]));
        double[] r = new double[v.length];
        for (int i = 0; i < v.length; ) {
            int j = i;
            while (j + 1 < v.length && v[order[j + 1]] == v[order[i]]) j++;
            double mean = (i + j) / 2.0;
            for (int k = i; k <= j; k++) r[order[k]] = v.length <= 1 ? 0.5 : mean / (v.length - 1);
            i = j + 1;
        }
        return r;
    }

    private static double spearman(double[] a, double[] b) {
        double[] ra = ranks(a), rb = ranks(b);
        double ma = 0, mb = 0;
        for (int i = 0; i < ra.length; i++) {
            ma += ra[i];
            mb += rb[i];
        }
        ma /= ra.length;
        mb /= rb.length;
        double sab = 0, saa = 0, sbb = 0;
        for (int i = 0; i < ra.length; i++) {
            sab += (ra[i] - ma) * (rb[i] - mb);
            saa += (ra[i] - ma) * (ra[i] - ma);
            sbb += (rb[i] - mb) * (rb[i] - mb);
        }
        return saa == 0 || sbb == 0 ? 0 : sab / Math.sqrt(saa * sbb);
    }

    private static boolean[] modernMask(int[] px) {
        boolean[] m = new boolean[px.length];
        for (int i = 0; i < px.length; i++) m[i] = (px[i] >>> 24) >= 128;
        return m;
    }

    /** The modern texture's first 16x16 frame (downscaled when a resource pack is larger), or null. */
    private static int[] modernPixels(byte[] png) {
        if (png == null) return null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            if (image == null || image.getWidth() < 16 || image.getHeight() < image.getWidth()) return null;
            int w = image.getWidth();
            int[] px = TextureModernizer.argb(image);
            int[] out = new int[256];
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) out[y * 16 + x] = px[(y * w / 16) * w + x * w / 16];
            return out;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** A square texture as 16x16 when it is one or a clean (nearest-neighbour) upscale of one, else null. */
    private static int[] downscale(int[] px, int w) {
        int s = w / 16;
        int[] out = new int[256];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int p = px[(y * s) * w + x * s];
                for (int dy = 0; dy < s; dy++) for (int dx = 0; dx < s; dx++) if (px[(y * s + dy) * w + x * s + dx] != p) return null;
                out[y * 16 + x] = p;
            }
        }
        return out;
    }

    private static byte[] encode(int[] px16, int scale) {
        int w = 16 * scale;
        int[] px = new int[w * w];
        for (int y = 0; y < w; y++) for (int x = 0; x < w; x++) px[y * w + x] = px16[(y / scale) * 16 + x / scale];
        BufferedImage out = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, w, px, 0, w);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(out, "png", bytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }

    /** First 16 hex digits of SHA-1 over the mask, one byte per pixel (as tools/gen_leaf_fingerprints.py). */
    static String maskHash(boolean[] mask) {
        byte[] b = new byte[mask.length];
        for (int i = 0; i < mask.length; i++) b[i] = (byte) (mask[i] ? 1 : 0);
        return sha1Hex(b).substring(0, 16);
    }

    private static int[] indices(boolean[] set) {
        int n = 0;
        for (boolean b : set) if (b) n++;
        int[] out = new int[n];
        n = 0;
        for (int i = 0; i < set.length; i++) if (set[i]) out[n++] = i;
        return out;
    }

    private static double luma(int p) {
        return 0.299 * channel(p, 0) + 0.587 * channel(p, 1) + 0.114 * channel(p, 2);
    }

    private static int channel(int p, int c) {
        return p >> (16 - 8 * c) & 255;
    }

    private static String sha1Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Offline samples: {@code <1.21.1 client jar> <out dir> <legacy jar>...}. Converts the block textures of each jar whose
     * name says leaf or leaves (in game: the icons of leaf blocks), prints the outcome per texture and writes before/after
     * pictures (8x, on a checkerboard) into the out directory. A jar given as {@code ref:<jar>} only provides references
     * (as the other leaf textures of the pack do in game).
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: LeafModernizer <1.21.1 client jar> <out dir> <legacy jar>...");
            return;
        }
        Path outDir = Path.of(args[1]);
        Files.createDirectories(outDir);
        LeafModernizer m = get();
        System.out.println("leaf fingerprints: " + m.kinds.size());
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
            List<Source> sources = new ArrayList<>();
            for (int a = 2; a < args.length; a++) {
                boolean ref = args[a].startsWith("ref:");
                try (ZipFile zip = new ZipFile(ref ? args[a].substring(4) : args[a])) {
                    java.util.Set<String> names = new java.util.HashSet<>();
                    for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) names.add(e.nextElement().getName());
                    for (String n : new java.util.TreeSet<>(names)) {
                        String low = n.toLowerCase(Locale.ROOT);
                        if (!low.endsWith(".png") || !low.contains("/textures/blocks/") || !(low.contains("leaf") || low.contains("leaves"))) continue;
                        try (InputStream in = zip.getInputStream(zip.getEntry(n))) {
                            sources.add(new Source(n.replaceAll("^assets/", "").replace("/textures/blocks/", ":"), in.readAllBytes(), names.contains(n + ".mcmeta"), ref));
                        }
                    }
                }
            }
            long start = System.nanoTime();
            List<Result> results = m.convert(sources, modern);
            System.out.printf("%d textures in %d ms%n", sources.size(), (System.nanoTime() - start) / 1_000_000);
            int converted = 0;
            Map<String, byte[]> before = new HashMap<>();
            for (Source s : sources) before.put(s.name(), s.png());
            for (Result r : results) {
                System.out.println((r.converted() ? "CONVERTED " : "skipped   ") + r.name() + ": " + r.note());
                if (!r.converted()) continue;
                converted++;
                writeSample(outDir.resolve(r.name().replaceAll("[^A-Za-z0-9._-]", "_")), before.get(r.name()), r.png());
            }
            System.out.println(converted + " converted");
        }
    }

    /** before | after at 8x on a grey checkerboard (transparency visible), as one PNG. */
    private static void writeSample(Path file, byte[] before, byte[] after) throws IOException {
        BufferedImage a = ImageIO.read(new ByteArrayInputStream(before));
        BufferedImage b = ImageIO.read(new ByteArrayInputStream(after));
        if (a == null || b == null) return;
        int s = 8;
        int w = 16 * s;
        BufferedImage out = new BufferedImage(w * 2 + 8, w, BufferedImage.TYPE_INT_ARGB);
        var g = out.createGraphics();
        for (int y = 0; y < w; y += 8) {
            for (int x = 0; x < out.getWidth(); x += 8) {
                g.setColor(((x + y) / 8 & 1) == 0 ? new java.awt.Color(0xFF00FF) : new java.awt.Color(0xC000C0));
                g.fillRect(x, y, 8, 8);
            }
        }
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(a, 0, 0, w, w, 0, 0, 16 * (a.getWidth() / 16), 16 * (a.getWidth() / 16), null);
        g.drawImage(b, w + 8, 0, w, w, null);
        g.dispose();
        ImageIO.write(out, "png", file.resolveSibling(file.getFileName() + ".png").toFile());
    }
}
