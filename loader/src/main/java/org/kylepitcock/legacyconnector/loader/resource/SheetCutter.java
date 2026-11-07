package org.kylepitcock.legacyconnector.loader.resource;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Minecraft 1.4.7's textures are indices into 16x16 sprite sheets (see {@code SpriteSheets147} in the runtime). This finds the sheets a jar's code
 * names ({@code setTextureFile("/mymod/blocks.png")}, a {@code getTextureFile()} that returns a path, {@code ModLoader.getUniqueSpriteIndex},
 * {@code preloadTexture}) and cuts each into per-tile PNGs of the generated resource pack, {@code assets/legacy147/textures/blocks/<slug>/<index>.png}
 * (the vanilla atlas stitches them like any other texture; fully transparent tiles are left out). Files given to {@code ModLoader.addOverride(sheet, file)}
 * are single tiles: copied to {@code assets/legacy147/textures/blocks/override/<slug>.png}. The whole sheet stays in the jar as it was, for renderers that bind it.
 *
 * <p>The tile size is a sixteenth of the width, so high resolution sheets work; the grid is assumed to be 16 columns.</p>
 */
public final class SheetCutter {
    /** Calls whose string argument is a sprite sheet. */
    private static final Set<String> SHEET_CALLS = Set.of("setTextureFile", "getUniqueSpriteIndex", "preloadTexture", "loadTexture", "bindTexture", "addOverride", "registerSheet");
    /** Vanilla sheets: the runtime maps their indices to the game's own textures, nothing is cut. */
    private static final Set<String> VANILLA = Set.of("terrain.png", "gui/items.png", "particles.png", "misc/mapicons.png", "gui/gui.png");

    private final Set<String> sheets = new TreeSet<>();
    private final Set<String> singles = new TreeSet<>();

    /** Looks for sheet and override paths in the (already remapped) classes. */
    public void scan(Iterable<ClassNode> nodes) {
        for (ClassNode node : nodes) {
            for (MethodNode method : node.methods) {
                if (method.instructions == null) continue;
                boolean getter = method.name.equals("getTextureFile");
                String[] recent = new String[6];
                int seen = 0;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                        if (s.toLowerCase(Locale.ROOT).endsWith(".png")) {
                            if (getter) sheets.add(normalize(s));
                            recent[seen++ % recent.length] = s;
                        }
                    } else if (insn instanceof MethodInsnNode call && call.getOpcode() != Opcodes.INVOKEDYNAMIC && SHEET_CALLS.contains(call.name) && seen > 0) {
                        int count = Math.min(seen, recent.length);
                        // addOverride(sheet, file): the file (the last string) is one tile, the first string is the sheet it joins
                        if (call.name.equals("addOverride") && count >= 2 && call.desc.startsWith("(Ljava/lang/String;Ljava/lang/String;")) {
                            singles.add(normalize(recent[(seen - 1) % recent.length]));
                        } else if (call.name.equals("bindTexture") || call.name.equals("loadTexture")) {
                            // bound whole images are not sheets unless named by one of the sheet calls: nothing to do
                        } else {
                            sheets.add(normalize(recent[(seen - 1) % recent.length]));
                        }
                        seen = 0;
                    }
                }
            }
        }
        sheets.removeAll(VANILLA);
    }

    public boolean found() {
        return !sheets.isEmpty() || !singles.isEmpty();
    }

    public Set<String> sheets() {
        return sheets;
    }

    /** The extra resources for the sheets found in {@code other} (jar entry name to bytes). */
    public Map<String, byte[]> cut(Map<String, byte[]> other) {
        Map<String, byte[]> out = new LinkedHashMap<>();
        for (var e : other.entrySet()) {
            String key = normalize(e.getKey());
            boolean sheet = sheets.contains(key);
            boolean single = singles.contains(key);
            if (!sheet && !single) continue;
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(e.getValue()));
                if (image == null) continue;
                if (single) {
                    out.put("assets/legacy147/textures/blocks/override/" + slug(key) + ".png", e.getValue());
                }
                if (sheet) cutSheet(image, slug(key), out);
            } catch (IOException | RuntimeException ex) {
                // an unreadable image stays a whole texture
            }
        }
        return out;
    }

    private static void cutSheet(BufferedImage image, String slug, Map<String, byte[]> out) throws IOException {
        int tile = image.getWidth() / 16;
        if (tile < 1 || image.getWidth() % 16 != 0) return;
        int rows = image.getHeight() / tile;
        for (int row = 0; row < rows && row < 16; row++) {
            for (int col = 0; col < 16; col++) {
                BufferedImage sub = image.getSubimage(col * tile, row * tile, tile, tile);
                if (empty(sub)) continue;
                BufferedImage copy = new BufferedImage(tile, tile, BufferedImage.TYPE_INT_ARGB);
                copy.getGraphics().drawImage(sub, 0, 0, null);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(copy, "png", bytes);
                out.put("assets/legacy147/textures/blocks/" + slug + "/" + (row * 16 + col) + ".png", bytes.toByteArray());
            }
        }
    }

    private static boolean empty(BufferedImage tile) {
        for (int y = 0; y < tile.getHeight(); y++) {
            for (int x = 0; x < tile.getWidth(); x++) {
                if ((tile.getRGB(x, y) >>> 24) != 0) return false;
            }
        }
        return true;
    }

    /** A path in the form the runtime looks it up by: leading slash, lower case. */
    public static String normalize(String path) {
        String p = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        return p.startsWith("/") ? p : "/" + p;
    }

    /** The folder name of a sheet's tiles; the same function as the runtime's {@code SpriteSheets147.slug}. */
    public static String slug(String normalizedPath) {
        String p = normalizedPath.startsWith("/") ? normalizedPath.substring(1) : normalizedPath;
        StringBuilder sb = new StringBuilder(p.length());
        for (char c : p.toCharArray()) sb.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ? c : '_');
        return sb.toString();
    }
}
