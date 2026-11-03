package org.kylepitcock.legacyconnector.loader.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Vanilla texture paths of 1.12 ({@code blocks/dirt}, {@code items/dye_powder_blue}) as 1.21.1 names them. The folders became
 * {@code block/} and {@code item/} and about 400 basenames were renamed by the 1.13 flattening; the table is
 * {@code legacyconnector/resource/vanilla-texture-renames.tsv} (block and item names are kept apart: only the block
 * {@code brick} became {@code bricks}). The runtime mod has an identical copy of this class ({@code VanillaTextures1122}) that reads the same file.
 */
public final class VanillaTextureRenames {
    private static final String RESOURCE = "/legacyconnector/resource/vanilla-texture-renames.tsv";
    /** "block/old" or "item/old" to the new basename path. */
    private static final Map<String, String> RENAMES = new HashMap<>();

    static {
        try (InputStream in = VanillaTextureRenames.class.getResourceAsStream(RESOURCE)) {
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

    private VanillaTextureRenames() {}

    /** The 1.21.1 path of a vanilla texture path without namespace or extension. */
    public static String modern(String path) {
        if (path.startsWith("blocks/")) path = "block/" + path.substring("blocks/".length());
        else if (path.startsWith("items/")) path = "item/" + path.substring("items/".length());
        String renamed = RENAMES.get(path);
        return renamed != null ? renamed : path;
    }
}
