package org.kylepitcock.legacyconnector.v1_12_2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Vanilla textures named the 1.12 way ("blocks/dirt", "minecraft:blocks/slime") as 1.21.1 names them. The loader's resource converter does
 * this for the model files of a mod's jar (VanillaTextureRenames, which this mirrors and whose table file it reads: build.gradle copies
 * legacyconnector/resource/vanilla-texture-renames.tsv from the loader); blockstate texture overrides turned into generated models at runtime
 * (Tinkers' Construct's slime dirt: "particle": "blocks/dirt") need the same rename.
 */
public final class VanillaTextures1122 {
    private VanillaTextures1122() {}

    /** "block/old" or "item/old" to the new basename path. */
    private static final Map<String, String> RENAMES = new HashMap<>();

    static {
        try (InputStream in = VanillaTextures1122.class.getResourceAsStream("/legacyconnector/resource/vanilla-texture-renames.tsv")) {
            if (in != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line; (line = r.readLine()) != null; ) {
                    if (line.isEmpty() || line.charAt(0) == '#') continue;
                    String[] f = line.split("\t");
                    if (f.length >= 3 && (f[0].equals("block") || f[0].equals("item"))) RENAMES.put(f[0] + "/" + f[1], f[0] + "/" + f[2]);
                }
            }
        } catch (IOException ignored) {
            // a missing table only leaves the folder move
        }
    }

    /** The 1.21.1 path of a vanilla texture path ({@code blocks/dirt} gives {@code block/dirt}). */
    public static String modern(String path) {
        if (path.startsWith("blocks/")) path = "block/" + path.substring("blocks/".length());
        else if (path.startsWith("items/")) path = "item/" + path.substring("items/".length());
        String renamed = RENAMES.get(path);
        return renamed != null ? renamed : path;
    }
}
