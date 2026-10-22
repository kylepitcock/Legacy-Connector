package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.core.Ids;

/**
 * Vanilla 1.7.10 texture paths that moved. Mod textures keep their (normalized) paths, since the loader ships
 * them under the same names.
 */
public final class TextureLocations {
    private static final Map<String, String> RENAMES = Map.ofEntries(
            Map.entry("textures/atlas/items.png", "textures/atlas/blocks.png"),
            Map.entry("textures/gui/container/generic_54.png", "textures/gui/container/generic_54.png"),
            Map.entry("textures/entity/chest/normal.png", "textures/entity/chest/normal.png"),
            Map.entry("textures/entity/steve.png", "textures/entity/player/wide/steve.png"),
            Map.entry("textures/entity/alex.png", "textures/entity/player/slim/alex.png"),
            Map.entry("textures/entity/zombie_villager/zombie_villager.png", "textures/entity/zombie_villager/zombie_villager.png"),
            Map.entry("textures/entity/villager/villager.png", "textures/entity/villager/villager.png"));

    private TextureLocations() {}

    public static ResourceLocation modern(String domain, String path) {
        String ns = Ids.namespace(domain);
        String p = Ids.normalizePath(path);
        if (ns.equals("minecraft")) {
            p = RENAMES.getOrDefault(p, p);
            // 1.7.10 block and item textures under their old names (textures/blocks/planks_oak.png) live under other names now
            boolean block = p.startsWith("textures/blocks/");
            if ((block || p.startsWith("textures/items/")) && p.endsWith(".png")) {
                String name = p.substring(p.indexOf('/', "textures/".length()) + 1, p.length() - 4);
                String modern = org.kylepitcock.legacyconnector.v1_7_10.Assets1710.texture((block ? "blocks/" : "items/") + name, block);
                int colon = modern.indexOf(':');
                if (colon > 0) return ResourceLocation.fromNamespaceAndPath(modern.substring(0, colon), "textures/" + modern.substring(colon + 1) + ".png");
            }
        }
        return ResourceLocation.fromNamespaceAndPath(ns, p);
    }
}
