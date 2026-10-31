package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.LegacyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10 ore textures carry the old stone, netherrack or end stone as their background, which sits badly next to the modern
 * textures. For every block texture whose name says ore, the pixels that match the old host rock are replaced by the
 * modern host texture at the same place, and the result goes into the generated resource pack over the mod's own file.
 * The replacement textures are configurable (see {@link LegacyConfig}); disable with {@code oreBackgrounds.enabled = false} or
 * {@code -Dlegacyconnector.keepLegacyOreBackgrounds=true}.
 */
public final class OreBackgrounds {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/OreBackgrounds");
    /** Average colours of the 1.7.10 host rocks, and the modern textures that replace them. */
    private static final int[][] LEGACY_HOSTS = {{125, 125, 125}, {111, 54, 53}, {221, 223, 165}};
    private static final String[] HOST_KEYS = {"stone", "netherrack", "endStone"};
    private static final String[] HOST_DEFAULTS = {"stone", "netherrack", "end_stone"};
    /** Colour distance to a host's average that still counts as host rock; netherrack's texture varies more than stone's. */
    private static final double[] MATCH_DISTANCE = {48.0D, 64.0D, 48.0D};
    private static final double MIN_BACKGROUND_SHARE = 0.3D;

    private OreBackgrounds() {}

    public static void contribute(TextureMap blocks, Map<String, Supplier<byte[]>> out) {
        if (Boolean.getBoolean("legacyconnector.keepLegacyOreBackgrounds") || !LegacyConfig.oreBackgroundsEnabled()) return;
        BufferedImage[] hosts = new BufferedImage[HOST_KEYS.length];
        for (int i = 0; i < hosts.length; i++) {
            String configured = LegacyConfig.oreBackground(HOST_KEYS[i], HOST_DEFAULTS[i]);
            if (configured == null) continue; // "legacy": this host keeps the old rock
            byte[] bytes = LegacySprites.vanillaTexture("textures/block/" + configured + ".png");
            if (bytes == null && !configured.equals(HOST_DEFAULTS[i])) {
                LOG.warn("oreBackgrounds.{}: no vanilla block texture '{}'; using {}", HOST_KEYS[i], configured, HOST_DEFAULTS[i]);
                bytes = LegacySprites.vanillaTexture("textures/block/" + HOST_DEFAULTS[i] + ".png");
            }
            if (bytes == null) {
                LOG.info("The modern host textures are not available yet; ore textures keep their backgrounds");
                return;
            }
            try {
                hosts[i] = ImageIO.read(new ByteArrayInputStream(bytes));
            } catch (IOException | RuntimeException e) {
                return;
            }
        }
        int changed = 0;
        for (String name : blocks.mcc$sprites().keySet()) {
            int colon = name.indexOf(':');
            String domain = colon < 0 ? "minecraft" : name.substring(0, colon);
            String path = colon < 0 ? name : name.substring(colon + 1);
            if (domain.equals("minecraft") || !isOreName(path) && !ORE_SPRITES.contains(Ids.namespace(domain) + ":blocks/" + Ids.normalizePath(path))) continue;
            byte[] original = LegacySprites.modTexture(domain, "textures/blocks/" + path + ".png");
            if (original == null) continue;
            try {
                byte[] recolored = recolor(original, hosts, path.toLowerCase(Locale.ROOT));
                if (recolored == null) continue;
                String file = "assets/" + Ids.namespace(domain) + "/textures/blocks/" + Ids.normalizePath(path) + ".png";
                out.put(file, () -> recolored);
                changed++;
            } catch (IOException | RuntimeException e) {
                // keep the original texture
            }
        }
        LOG.info("Replaced the stone, netherrack or end stone background of {} legacy ore textures with the modern textures", changed);
    }

    /** Sprites ({@code namespace:blocks/name}) drawn by ore blocks, whatever their file names say (NetherOres' textures are called Copper, Tin ...). */
    private static final java.util.Set<String> ORE_SPRITES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Records a sprite used by an ore block (called while the block models are written). */
    public static void noteOreSprite(String sprite) {
        if (sprite != null) ORE_SPRITES.add(sprite);
    }

    /** Words that end in "ore" without being ores. */
    private static final java.util.Set<String> NOT_ORE = java.util.Set.of("core", "more", "store", "score", "before", "ignore", "restore",
            "explore", "shore", "chore", "bore", "snore", "pore", "spore", "fore", "adore", "galore", "lore", "sore", "wore", "tore", "yore");

    /** Whether a texture name names an ore: a whole word "ore"/"ores" (camelCase, dots, dashes and underscores separate words), or a word like "copperore". */
    public static boolean isOreName(String name) {
        for (String word : name.split("[^A-Za-z0-9]+|(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")) {
            String w = word.toLowerCase(Locale.ROOT);
            if (w.equals("ore") || w.equals("ores")) return true;
            if (w.length() > 4 && w.endsWith("ore") && !NOT_ORE.contains(w)) return true;
        }
        return false;
    }

    private static final java.util.Map<String, String> DEEP_DONE = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile BufferedImage deepslate;

    /**
     * The sprite name of an ore texture on a deepslate background (its stone pixels replaced by deepslate, written to the
     * generated pack as {@code <name>_deepslate}), or the name itself when the texture has no stone background, cannot be
     * read or this is not the client. Used by the deepslate variants of ores that generate below y=0.
     */
    public static String deepslateTexture(String sprite, Map<String, Supplier<byte[]>> out) {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient() || sprite == null) return sprite;
        String known = DEEP_DONE.get(sprite);
        if (known != null) {
            if (!known.equals(sprite)) generated(sprite, known, out);
            return known;
        }
        String result = sprite;
        try {
            int colon = sprite.indexOf(':');
            String domain = colon < 0 ? "minecraft" : sprite.substring(0, colon);
            String path = colon < 0 ? sprite : sprite.substring(colon + 1);
            if (!domain.equals("minecraft") && path.startsWith("blocks/")) {
                if (deepslate == null) {
                    byte[] host = LegacySprites.vanillaTexture("textures/block/deepslate.png");
                    if (host != null) deepslate = ImageIO.read(new ByteArrayInputStream(host));
                }
                byte[] original = deepslate == null ? null : LegacySprites.modTexture(domain, "textures/" + path + ".png");
                byte[] recolored = original == null ? null : recolor(original, new BufferedImage[] {deepslate, null, null}, path.toLowerCase(Locale.ROOT));
                if (recolored != null) {
                    result = sprite + "_deepslate";
                    String file = "assets/" + Ids.namespace(domain) + "/textures/" + path + "_deepslate.png";
                    out.put(file, () -> recolored);
                }
            }
        } catch (IOException | RuntimeException e) {
            // the ore keeps its normal texture
        }
        DEEP_DONE.put(sprite, result);
        return result;
    }

    private static void generated(String sprite, String deep, Map<String, Supplier<byte[]>> out) {
        // a later pack generation (reload) starts from an empty map: regenerate
        int colon = sprite.indexOf(':');
        String domain = colon < 0 ? "minecraft" : sprite.substring(0, colon);
        String path = colon < 0 ? sprite : sprite.substring(colon + 1);
        String file = "assets/" + Ids.namespace(domain) + "/textures/" + path + "_deepslate.png";
        if (out.containsKey(file)) return;
        byte[] original = LegacySprites.modTexture(domain, "textures/" + path + ".png");
        try {
            byte[] recolored = original == null || deepslate == null ? null : recolor(original, new BufferedImage[] {deepslate, null, null}, path.toLowerCase(Locale.ROOT));
            if (recolored != null) out.put(file, () -> recolored);
        } catch (IOException | RuntimeException e) {
            // keep going
        }
    }

    /** The texture with its host rock pixels replaced, or null when it has no recognisable host rock (or is animated). */
    static byte[] recolor(byte[] png, BufferedImage[] hosts, String lowerName) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        if (image == null || image.getWidth() != image.getHeight()) return null;
        int size = image.getWidth();
        int[] pixels = image.getRGB(0, 0, size, size, null, 0, size);
        int best = -1;
        double bestShare = 0;
        for (int host = 0; host < LEGACY_HOSTS.length; host++) {
            if (hosts[host] == null) continue;
            int near = 0;
            for (int p : pixels) if (isHost(p, host)) near++;
            double share = (double) near / pixels.length;
            if (share > bestShare) {
                bestShare = share;
                best = host;
            }
        }
        if (best < 0 || bestShare < MIN_BACKGROUND_SHARE) return null;
        BufferedImage replacement = hosts[best];
        int changed = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int p = pixels[y * size + x];
                if (!isHost(p, best) || (p >>> 24) < 200) continue;
                int hx = x * replacement.getWidth() / size;
                int hy = y * replacement.getHeight() / size;
                pixels[y * size + x] = pixel(replacement, hx, hy);
                changed++;
            }
        }
        if (changed == 0) return null;
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, size, size, pixels, 0, size);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(result, "png", bytes);
        return bytes.toByteArray();
    }

    /**
     * One pixel as plain ARGB. {@code getRGB} converts greyscale PNGs through a colour space, which makes them far lighter; the
     * sample is read from the raster instead.
     */
    private static int pixel(BufferedImage image, int x, int y) {
        var model = image.getColorModel();
        if (model.getNumColorComponents() == 1) {
            var raster = image.getRaster();
            int v = raster.getSample(x, y, 0);
            int shift = model.getComponentSize(0) > 8 ? model.getComponentSize(0) - 8 : 0;
            v >>= shift;
            int a = model.hasAlpha() ? raster.getSample(x, y, 1) >> (model.getComponentSize(1) > 8 ? model.getComponentSize(1) - 8 : 0) : 255;
            return (a << 24) | (v << 16) | (v << 8) | v;
        }
        return image.getRGB(x, y);
    }

    private static boolean isHost(int argb, int host) {
        if ((argb >>> 24) < 200) return false;
        int dr = ((argb >> 16) & 255) - LEGACY_HOSTS[host][0];
        int dg = ((argb >> 8) & 255) - LEGACY_HOSTS[host][1];
        int db = (argb & 255) - LEGACY_HOSTS[host][2];
        return Math.sqrt(dr * dr + dg * dg + db * db) < MATCH_DISTANCE[host];
    }
}
