package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The 1.7.10 unlocalized names of vanilla blocks and items ({@code tile.oreIron}, {@code item.ingotIron}): they differ from the
 * registry names. Mods build ids and lang keys from them (Mystcraft's {@code ModMat_tile.stone} symbols).
 */
public final class VanillaNames {
    private static final Map<String, String> BLOCKS = new HashMap<>();
    private static final Map<String, String> ITEMS = new HashMap<>();

    static {
        try (InputStream in = VanillaNames.class.getResourceAsStream("/legacyconnector/legacy/1.7.10/vanilla-unlocalized-names.txt")) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (line.startsWith("#")) continue;
                        String[] p = line.trim().split(" ");
                        if (p.length == 3) (p[0].equals("b") ? BLOCKS : ITEMS).put(p[1], p[2]);
                    }
                }
            }
        } catch (java.io.IOException e) {
            // no table: registry names are used
        }
    }

    private VanillaNames() {}

    /** The name without its prefix for a vanilla {@code legacyName} ("minecraft:iron_ore" -> "oreIron"). */
    public static String unlocalized(boolean block, String legacyName) {
        if (legacyName.indexOf(':') >= 0 && !legacyName.startsWith("minecraft:")) return legacyName;
        String path = legacyName.startsWith("minecraft:") ? legacyName.substring("minecraft:".length()) : legacyName;
        return (block ? BLOCKS : ITEMS).getOrDefault(path, path);
    }
}
