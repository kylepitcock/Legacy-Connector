package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.LegacyConfig;
import org.kylepitcock.legacyconnector.core.pack.LeafModernizer;
import org.kylepitcock.legacyconnector.core.pack.TextureModernizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy leaf textures in the modern leaf pattern ({@link LeafModernizer}): the icons legacy leaf blocks (BlockLeaves
 * subclasses, blocks of leaves material) register are recognised as recolours of the old vanilla leaves and rebuilt from the
 * modern vanilla leaves in the mod's colours; fancy and fast (opaque) variants alike. The results go into the generated
 * resource pack over the mod's own files, and are kept for later reloads. Disable with {@code textures.modernLeaves = false}.
 */
public final class ModernLeaves {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Leaves");
    /** Legacy icon names ({@code natura:sakura_leaves_fancy}) registered by leaf blocks, in registration order. */
    private static final Set<String> ICONS = java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());
    /** The results for the icon set they were made for (a reload with the same icons reuses them). */
    private static volatile List<String> doneFor;
    private static volatile Map<String, byte[]> done = Map.of();

    private ModernLeaves() {}

    /** Whether a legacy block draws leaves: a BlockLeaves subclass, or a block of the leaves material. */
    public static boolean isLeafBlock(Block b) {
        if (b instanceof legacyconnector.v1_7_10.net.minecraft.block.BlockLeaves) return true;
        try {
            return b.getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.leaves;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** The number of icons registered so far (call before a leaf block's registerBlockIcons). */
    public static int mark(TextureMap blocks) {
        return blocks.mcc$sprites().size();
    }

    /** Notes the icons a leaf block registered since {@link #mark}. */
    public static void noteIcons(TextureMap blocks, int mark) {
        int i = 0;
        for (String name : blocks.mcc$sprites().keySet()) {
            if (i++ >= mark) ICONS.add(name);
        }
    }

    /** Writes the converted leaf textures into the generated pack (client resources, after all icons are registered). */
    public static void contribute(Map<String, Supplier<byte[]>> out) {
        if (!LegacyConfig.modernLeavesEnabled()) return;
        List<String> names;
        synchronized (ICONS) {
            names = List.copyOf(ICONS);
        }
        if (names.isEmpty()) return;
        Map<String, byte[]> made = done;
        if (!names.equals(doneFor)) {
            made = convert(names);
            if (made == null) return; // the vanilla textures are not readable yet: try again on the next pack build
            done = made;
            doneFor = names;
        }
        for (var e : made.entrySet()) {
            byte[] png = e.getValue();
            out.put(e.getKey(), () -> png);
        }
    }

    /** Pack path -> converted texture, logging what happened to each icon; null when the modern textures are unavailable. */
    private static Map<String, byte[]> convert(List<String> names) {
        if (LegacySprites.vanillaTexture("textures/block/oak_leaves.png") == null) {
            LOG.info("The modern vanilla leaves are not readable yet; legacy leaf textures stay as they are for now");
            return null;
        }
        long start = System.nanoTime();
        boolean modernizer = LegacyConfig.modernizeTexturesEnabled();
        List<LeafModernizer.Source> sources = new ArrayList<>();
        Map<String, String> files = new java.util.HashMap<>();
        List<String> skipped = new ArrayList<>();
        for (String name : names) {
            int colon = name.indexOf(':');
            String domain = colon < 0 ? "minecraft" : name.substring(0, colon);
            String path = colon < 0 ? name : name.substring(colon + 1);
            if (domain.equals("minecraft")) continue; // vanilla icons already map to the modern textures
            byte[] png = LegacySprites.modTexture(domain, "textures/blocks/" + path + ".png");
            if (png == null) {
                skipped.add(name + ": no texture file");
                continue;
            }
            if (modernizer) {
                // a straight copy of an old vanilla leaf texture becomes the modern texture itself (ModernizedTextures)
                var hit = TextureModernizer.get().classify("assets/" + domain + "/textures/blocks/" + path + ".png", png);
                if (hit != null && hit.kind() == TextureModernizer.Kind.EXACT) {
                    skipped.add(name + ": exact copy of an old vanilla texture (modernizeTextures replaces it with " + hit.modern() + ")");
                    continue;
                }
            }
            boolean animated = LegacySprites.modTexture(domain, "textures/blocks/" + path + ".png.mcmeta") != null;
            sources.add(new LeafModernizer.Source(name, png, animated));
            files.put(name, "assets/" + Ids.namespace(domain) + "/textures/blocks/" + Ids.normalizePath(path) + ".png");
        }
        List<LeafModernizer.Result> results = LeafModernizer.get().convert(sources,
                modern -> LegacySprites.vanillaTexture("textures/" + modern + ".png"));
        Map<String, byte[]> made = new java.util.LinkedHashMap<>();
        for (LeafModernizer.Result r : results) {
            if (r.converted()) {
                made.put(files.get(r.name()), r.png());
                LOG.info("Leaves {} -> modern {}", r.name(), r.note());
            } else {
                skipped.add(r.name() + ": " + r.note());
            }
        }
        for (String line : skipped) LOG.info("Leaves {} kept as they are: {}", line.substring(0, line.indexOf(": ")), line.substring(line.indexOf(": ") + 2));
        LOG.info("Legacy leaf textures: {} of {} rebuilt in the modern leaf pattern, {} kept, in {} ms (textures.modernLeaves = false keeps them all)",
                made.size(), made.size() + skipped.size(), skipped.size(), (System.nanoTime() - start) / 1_000_000);
        return made;
    }
}
