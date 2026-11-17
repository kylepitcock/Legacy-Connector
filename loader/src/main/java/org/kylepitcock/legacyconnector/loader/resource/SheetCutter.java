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
import org.objectweb.asm.tree.FieldInsnNode;
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
    private static final Set<String> SHEET_CALLS = Set.of("setTextureFile", "getUniqueSpriteIndex", "preloadTexture", "loadTexture", "bindTexture", "changeTexture", "addOverride", "registerSheet");
    /** Vanilla sheets: the runtime maps their indices to the game's own textures, nothing is cut. */
    private static final Set<String> VANILLA = Set.of("terrain.png", "gui/items.png", "particles.png", "misc/mapicons.png", "gui/gui.png");

    private final Set<String> sheets = new TreeSet<>();
    private final Set<String> singles = new TreeSet<>();
    /** Whether the jar has animated tiles (a TextureFX subclass): they draw into tiles that are blank in the sheet, which are then cut too. */
    private boolean animated;

    /** Looks for sheet and override paths in the (already remapped) classes. */
    public void scan(Iterable<ClassNode> nodes) {
        Map<String, Set<String>> pngFields = pngStaticFields(nodes);
        for (ClassNode node : nodes) {
            if (node.superName != null && node.superName.endsWith("TextureFX")) animated = true;
            for (MethodNode method : node.methods) {
                if (method.instructions == null) continue;
                boolean getter = method.name.equals("getTextureFile");
                String[] recent = new String[6];
                java.util.List<String> arrayLiterals = new java.util.ArrayList<>();
                int seen = 0;
                // the paths of the last png-holding static field read, when that read is more recent than the last png literal
                Set<String> lastField = null;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                        if (s.toLowerCase(Locale.ROOT).endsWith(".png")) {
                            // a literal stored into an array element (TE's Proxy fills String[] with its renderers' textures, then preloads and passes them on)
                            AbstractInsnNode nextReal = insn.getNext();
                            while (nextReal != null && nextReal.getOpcode() < 0) nextReal = nextReal.getNext();
                            if (nextReal != null && nextReal.getOpcode() == Opcodes.AASTORE) arrayLiterals.add(s);
                            if (getter) sheets.add(normalize(s));
                            recent[seen++ % recent.length] = s;
                            lastField = null;
                        }
                    } else if (insn instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETSTATIC
                            && pngFields.containsKey(field.owner + "." + field.name)) {
                        lastField = pngFields.get(field.owner + "." + field.name);
                        if (getter) for (String v : lastField) sheets.add(normalize(v));
                    } else if (insn instanceof MethodInsnNode call && call.getOpcode() != Opcodes.INVOKEDYNAMIC
                            && (SHEET_CALLS.contains(call.name) || renderConstructor(call)) && (seen > 0 || lastField != null)) {
                        int count = Math.min(seen, recent.length);
                        if (!arrayLiterals.isEmpty() && !call.name.equals("addOverride") && (!call.name.equals("bindTexture") && !call.name.equals("loadTexture") && !call.name.equals("changeTexture") || node.name.contains("Render"))) {
                            for (String v : arrayLiterals) sheets.add(normalize(v));
                            arrayLiterals.clear();
                        }
                        boolean renderer = node.name.contains("Render") && !node.name.contains("Gui") && !node.name.contains("GUI");
                        if (lastField != null) {
                            // setTextureFile(Props.blockSheet): every path the field is ever given (a mod's colour-blind sheet too)
                            boolean bind = call.name.equals("bindTexture") || call.name.equals("loadTexture") || call.name.equals("changeTexture");
                            if (!call.name.equals("addOverride") && (!bind || renderer)) for (String v : lastField) sheets.add(normalize(v));
                        } else if (call.name.equals("addOverride") && count >= 2 && call.desc.startsWith("(Ljava/lang/String;Ljava/lang/String;")) {
                            // addOverride(sheet, file): the file (the last string) is one tile, the first string is the sheet it joins
                            singles.add(normalize(recent[(seen - 1) % recent.length]));
                        } else if (call.name.equals("bindTexture") || call.name.equals("loadTexture") || call.name.equals("changeTexture")) {
                            // bound whole images are not sheets unless named by one of the sheet calls, except in a block/tile renderer: those draw
                            // tile-position UVs of what they bind (RedPower's wires bind /eloraam/wiring/redpower1.png and draw tessellator quads)
                            if (renderer) sheets.add(normalize(recent[(seen - 1) % recent.length]));
                        } else if (renderConstructor(call)) {
                            // the textures of a renderer: one path, or an array of them (TE's tesseract and conduit renderers take String[] filled with literals)
                            for (int i = 0; i < count; i++) if (recent[i] != null) sheets.add(normalize(recent[i]));
                        } else {
                            sheets.add(normalize(recent[(seen - 1) % recent.length]));
                        }
                        seen = 0;
                        lastField = null;
                    }
                }
            }
        }
        for (String v : VANILLA) sheets.remove(normalize(v));
    }

    /**
     * A renderer built with the path of its texture ({@code new RenderBlockX("/mod/gfx/render/x.png")}, which later binds the field through a helper such
     * as {@code CCRenderState.changeTexture(texture)}): the png literal before such a constructor is a texture the renderer draws, bound whole or as a sheet.
     */
    private static boolean renderConstructor(MethodInsnNode call) {
        return call.name.equals("<init>") && call.owner.contains("Render") && !call.owner.contains("Gui") && !call.owner.contains("GUI")
                && (call.desc.contains("Ljava/lang/String;"));
    }

    /**
     * Static String fields that are assigned a png literal anywhere in the jar ({@code Props.blockSheet = "/mod/gfx/blocks.png"},
     * switched to a colour-blind sheet by a config option): owner.name to every path assigned. Mods keep their sheet path in such a field and
     * pass the field to {@code setTextureFile}/{@code preloadTexture}, so the call sites have no literal (#248: none of the mod's blocks had tiles).
     */
    private static Map<String, Set<String>> pngStaticFields(Iterable<ClassNode> nodes) {
        Map<String, Set<String>> out = new java.util.HashMap<>();
        for (ClassNode node : nodes) {
            for (MethodNode method : node.methods) {
                if (method.instructions == null) continue;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (!(insn instanceof FieldInsnNode put) || put.getOpcode() != Opcodes.PUTSTATIC || !put.desc.equals("Ljava/lang/String;")) continue;
                    AbstractInsnNode prev = put.getPrevious();
                    while (prev != null && prev.getOpcode() < 0) prev = prev.getPrevious();
                    if (prev instanceof LdcInsnNode ldc && ldc.cst instanceof String s && s.toLowerCase(Locale.ROOT).endsWith(".png")) {
                        out.computeIfAbsent(put.owner + "." + put.name, k -> new TreeSet<>()).add(s);
                    }
                }
            }
        }
        return out;
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
            boolean sheet = sheets.contains(key) || relativeSheet(key);
            boolean single = singles.contains(key);
            if (!sheet && !single && !conventionalName(key)) continue;
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(e.getValue()));
                if (image == null) continue;
                // a sheet whose path the code builds at run time (BuildCraft: DefaultProps + "blocks.png"): by its conventional name and 256x256 size
                if (!sheet && !single && conventionalName(key) && image.getWidth() == 256 && image.getHeight() == 256) sheet = true;
                if (single) {
                    out.put("assets/legacy147/textures/blocks/override/" + slug(key) + ".png", e.getValue());
                }
                if (sheet) {
                    cutSheet(image, slug(key), out, animated);
                    // a non-square image is a model texture (a ModelBase's 128x64 skin, drawn by a block renderer): its UVs name regions of any size,
                    // not 16x16 tiles, so the image also goes to the atlas whole (IsbrhQuads maps such a sheet's UVs onto it; #248, TE's energy cell)
                    if (image.getWidth() != image.getHeight()) out.put("assets/legacy147/textures/blocks/whole/" + slug(key) + ".png", e.getValue());
                }
            } catch (IOException | RuntimeException ex) {
                // an unreadable image stays a whole texture
            }
        }
        return out;
    }

    /**
     * A sheet the code names without its folder ({@code getTextureFile() { return GfxPath("block.png"); }}, Applied Energistics): the one file of the
     * jar that ends with that name, cut under its real path (which is what the runtime asks for).
     */
    private boolean relativeSheet(String key) {
        for (String s : sheets) {
            if (s.indexOf('/', 1) < 0 && !VANILLA.contains(s.substring(1)) && key.endsWith(s) && key.length() > s.length()) return true;
        }
        return false;
    }

    /** A mod's own {@code blocks.png}, {@code items.png}...: the names mods give their tile sheets (the vanilla sheets are at the jar's top level and not cut). */
    private static boolean conventionalName(String key) {
        if (key.indexOf('/', 1) < 0) return false;
        String name = key.substring(key.lastIndexOf('/') + 1);
        return name.equals("blocks.png") || name.equals("items.png") || name.equals("block.png") || name.equals("item.png") || name.equals("terrain.png")
                || name.equals("gui/items.png");
    }

    private static void cutSheet(BufferedImage image, String slug, Map<String, byte[]> out, boolean keepEmpty) throws IOException {
        int tile = image.getWidth() / 16;
        if (tile < 1 || image.getWidth() % 16 != 0) return;
        int rows = image.getHeight() / tile;
        for (int row = 0; row < rows && row < 16; row++) {
            for (int col = 0; col < 16; col++) {
                BufferedImage sub = image.getSubimage(col * tile, row * tile, tile, tile);
                if (!keepEmpty && empty(sub)) continue;
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
