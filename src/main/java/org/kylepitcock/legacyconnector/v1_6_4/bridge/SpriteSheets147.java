package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import org.kylepitcock.legacyconnector.v1_7_10.Assets1710;

/**
 * Minecraft 1.4.7's textures are indices into 16x16 sprite sheets ({@code /terrain.png}, {@code /gui/items.png}, a mod's own sheet named by
 * {@code setTextureFile} / {@code ITextureProvider.getTextureFile()}); the game since 1.5 has icons. This maps (sheet, index) onto the sprite of the
 * generated resource pack:
 * <ul>
 *     <li>the vanilla sheets through the 256-entry tables {@code legacyconnector/v1_6_4/sheet-terrain.tsv} and {@code sheet-items.tsv} (index, tab,
 *     1.7.10 texture name), made from the 1.4.7 sheets by tools/gen_sheets_147.py;</li>
 *     <li>a mod's sheet through the tiles the loader cut from it ({@code assets/legacy147/textures/blocks/<sheet slug>/<index>.png}, see
 *     {@code SheetCutter} in the loader), sprite {@code legacy147:blocks/<slug>/<index>};</li>
 *     <li>ModLoader's {@code addOverride(sheet, file)} / {@code getUniqueSpriteIndex(sheet)}: virtual indices from 256 up, mapped to the single file
 *     the loader copied to {@code legacy147/override/<slug>}.</li>
 * </ul>
 * The whole sheet image stays in the jar for tessellator renderers that bind it themselves.
 */
public final class SpriteSheets147 {
    public static final String TERRAIN = "/terrain.png";
    public static final String ITEMS = "/gui/items.png";

    private static final Map<String, IIcon> ICONS = new ConcurrentHashMap<>();
    /** {@code sheet#index} of a ModLoader override (virtual index) to its sprite id. */
    private static final Map<String, String> OVERRIDES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> NEXT_VIRTUAL = new HashMap<>();
    private static volatile String[] terrain;
    private static volatile String[] items;

    private SpriteSheets147() {}

    /** The sprite of the tile {@code index} of the sheet the block or item names, as the bridge methods and the shim's default {@code getIcon} ask. */
    public static IIcon mcc$icon(Object owner, int index) {
        String sheet;
        boolean block = !(owner instanceof Item);
        if (owner instanceof Block b) sheet = b.getTextureFile();
        else if (owner instanceof Item i) sheet = i.getTextureFile();
        else sheet = TERRAIN;
        return icon(sheet, index, block);
    }

    public static IIcon icon(String sheet, int index, boolean block) {
        String key = normalize(sheet) + "#" + index + (block ? "b" : "i");
        IIcon icon = ICONS.get(key);
        if (icon == null) {
            icon = TextureAtlasSprite.makeAtlasSprite(spriteId(sheet, index, block));
            ICONS.put(key, icon);
        }
        return icon;
    }

    /** The modern sprite id of a tile (not registered; the pack generator and the atlas know it from the resource pack). */
    public static String spriteId(String sheet, int index, boolean block) {
        String norm = normalize(sheet);
        String override = OVERRIDES.get(norm + "#" + index);
        if (override != null) return override;
        if (norm.equals(TERRAIN) || norm.equals(ITEMS)) {
            String name = vanillaName(norm.equals(TERRAIN), index);
            if (name == null) return "minecraft:missingno";
            return Assets1710.texture("minecraft:" + name, norm.equals(TERRAIN));
        }
        return Assets1710.texture("legacy147:" + slug(norm) + "/" + index, true);
    }

    // ---- ModLoader

    /** {@code ModLoader.getUniqueSpriteIndex(sheet)}: a free index of the sheet (virtual: above the 256 the file has), which {@code addOverride} then fills. */
    public static synchronized int uniqueIndex(String sheet) {
        String norm = normalize(sheet);
        int next = NEXT_VIRTUAL.getOrDefault(norm, 256);
        NEXT_VIRTUAL.put(norm, next + 1);
        return next;
    }

    /** {@code ModLoader.addOverride(sheet, file)}: a new tile of the sheet, drawn from the (16x16) image {@code file} of the mod. */
    public static int override(String sheet, String file) {
        int index = uniqueIndex(sheet);
        override(sheet, file, index);
        return index;
    }

    public static void override(String sheet, String file, int index) {
        String norm = normalize(sheet);
        OVERRIDES.put(norm + "#" + index, Assets1710.texture("legacy147:override/" + slug(normalize(file)), true));
        ICONS.keySet().removeIf(k -> k.startsWith(norm + "#" + index));
    }

    // ---- names

    /** A sheet path in one form: leading slash, lower case. */
    public static String normalize(String path) {
        if (path == null) return TERRAIN;
        String p = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        return p.startsWith("/") ? p : "/" + p;
    }

    /** The folder name of a sheet's tiles; the same function as the loader's {@code SheetCutter.slug}. */
    public static String slug(String normalizedPath) {
        String p = normalizedPath.startsWith("/") ? normalizedPath.substring(1) : normalizedPath;
        StringBuilder sb = new StringBuilder(p.length());
        for (char c : p.toCharArray()) sb.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ? c : '_');
        return sb.toString();
    }

    private static String vanillaName(boolean isTerrain, int index) {
        String[] table = isTerrain ? terrain : items;
        if (table == null) {
            table = load(isTerrain ? "sheet-terrain.tsv" : "sheet-items.tsv");
            if (isTerrain) terrain = table;
            else items = table;
        }
        return index >= 0 && index < table.length ? table[index] : null;
    }

    private static String[] load(String file) {
        String[] out = new String[256];
        try (InputStream in = SpriteSheets147.class.getClassLoader().getResourceAsStream("legacyconnector/v1_6_4/" + file)) {
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] p = line.strip().split("\t");
                    if (p.length >= 2) {
                        int i = Integer.parseInt(p[0]);
                        if (i >= 0 && i < 256) out[i] = p[1];
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            throw new IllegalStateException("Corrupt sheet table " + file, e);
        }
        return out;
    }
}
