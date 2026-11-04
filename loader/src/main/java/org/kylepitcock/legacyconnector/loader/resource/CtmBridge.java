package org.kylepitcock.legacyconnector.loader.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * The 1.12 Connected Textures Mod keeps its settings in a texture's {@code .png.mcmeta} ({@code "ctm": {"type": ..., "extra": {...}}})
 * and its own model loader picks the tile. Without that loader the block atlas stitches the whole sheet of a {@code random} or
 * {@code pattern} texture (Chisel's 48x48 "bricks-chaotic" is nine variants) as one sprite and squeezes it onto a face. This bridge does
 * what the modern equivalent expects: the sprite becomes the sheet's first tile, and every tile is written as a numbered image beside an
 * OptiFine-format {@code .properties} file under {@code optifine/ctm}, which Continuity (the modern connected textures mod) reads:
 * {@code random} sheets with {@code method=random}, {@code pattern} sheets with {@code method=repeat}. Without Continuity the first tile
 * shows and the properties are never read. Other CTM types keep their plain 16x16 sprite.
 */
final class CtmBridge {
    private static final Pattern TEXTURE = Pattern.compile("assets/([^/]+)/textures/(blocks?)/(.+)[.]png([.]mcmeta)?");

    private record Sheet(String namespace, String folder, String path, byte[] png, byte[] meta) {}

    private final Map<String, byte[]> pngs = new LinkedHashMap<>();
    private final Map<String, byte[]> metas = new LinkedHashMap<>();

    /** Records a block texture or its .mcmeta (the file is still written as it is; {@link #apply} replaces what it converts). */
    void offer(String modernPath, byte[] data) {
        Matcher m = TEXTURE.matcher(modernPath);
        if (!m.matches()) return;
        if (m.group(4) != null) metas.put(modernPath.substring(0, modernPath.length() - ".mcmeta".length()), data);
        else pngs.put(modernPath, data);
    }

    void apply(Map<String, byte[]> output) {
        for (var meta : metas.entrySet()) {
            byte[] png = pngs.get(meta.getKey());
            Matcher m = TEXTURE.matcher(meta.getKey());
            if (png == null || !m.matches()) continue;
            try {
                convert(new Sheet(m.group(1), m.group(2), m.group(3), png, meta.getValue()), output);
            } catch (IOException | RuntimeException e) {
                // a texture this cannot slice keeps its sheet
            }
        }
    }

    private static void convert(Sheet sheet, Map<String, byte[]> output) throws IOException {
        JsonElement root = JsonParser.parseString(new String(sheet.meta(), StandardCharsets.UTF_8));
        if (!root.isJsonObject() || !root.getAsJsonObject().has("ctm") || !root.getAsJsonObject().get("ctm").isJsonObject()) return;
        JsonObject ctm = root.getAsJsonObject().getAsJsonObject("ctm");
        if (!ctm.has("type") || !ctm.get("type").isJsonPrimitive()) return;
        String type = ctm.get("type").getAsString().toLowerCase(java.util.Locale.ROOT);
        boolean random = type.equals("random") || type.equals("r");
        if (!random && !type.equals("pattern")) return;
        int size = ctm.has("extra") && ctm.get("extra").isJsonObject() && ctm.getAsJsonObject("extra").has("size")
                ? ctm.getAsJsonObject("extra").get("size").getAsInt() : (random ? 2 : 0);
        if (size < 2 || size > 8) return;
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(sheet.png()));
        if (image == null || image.getWidth() != image.getHeight() || image.getWidth() % size != 0) return;
        int tile = image.getWidth() / size;
        String dir = "assets/minecraft/optifine/ctm/legacyconnector/" + sheet.namespace() + "/" + sheet.folder() + "_" + sheet.path().replace('/', '_');
        StringBuilder tiles = new StringBuilder();
        for (int i = 0; i < size * size; i++) {
            BufferedImage part = image.getSubimage((i % size) * tile, (i / size) * tile, tile, tile);
            BufferedImage copy = new BufferedImage(tile, tile, BufferedImage.TYPE_INT_ARGB);
            copy.getGraphics().drawImage(part, 0, 0, null);
            output.put(dir + "/" + i + ".png", write(copy));
            if (i == 0) {
                // the sprite itself: the first tile (an animation next to it would no longer fit the sheet, so only static sheets are cut)
                if (output.containsKey("assets/" + sheet.namespace() + "/textures/" + sheet.folder() + "/" + sheet.path() + ".png.mcmeta")
                        && new String(output.get("assets/" + sheet.namespace() + "/textures/" + sheet.folder() + "/" + sheet.path() + ".png.mcmeta"),
                                StandardCharsets.UTF_8).contains("\"animation\"")) return;
                output.put("assets/" + sheet.namespace() + "/textures/" + sheet.folder() + "/" + sheet.path() + ".png", write(copy));
            }
        }
        StringBuilder props = new StringBuilder();
        props.append("method=").append(random ? "random" : "repeat").append('\n');
        props.append("tiles=0-").append(size * size - 1).append('\n');
        props.append("matchTiles=").append(sheet.namespace()).append(':').append(sheet.folder()).append('/').append(sheet.path()).append('\n');
        if (!random) props.append("width=").append(size).append("\nheight=").append(size).append('\n');
        else props.append("symmetry=none\n");
        output.put(dir + "/ctm.properties", props.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] write(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
