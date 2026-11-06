package org.kylepitcock.legacyconnector.vb1_7_3;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.item.BlockItem;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.src.forge.ITextureProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client assets of the b1.7.3 mods. A b1.7.3 block names its textures by sprite index: {@code getTexture(side, meta)} into terrain.png
 * (a mod's own sprites are {@code ModLoader.addOverride} images put into free slots) or, for Forge's {@code ITextureProvider}, into the
 * block's own 256x256 sheet. Each index becomes a texture: the override image, the sheet's tile, or the modern texture of the vanilla
 * sprite at that index. A block gets one model per metadata (faces down, up, north, south, west, east as b1.7.3 sides 0-5; its bounds as
 * the element; cross, crop and torch shapes for those render types) and a blockstate over its {@code meta} property; items get a
 * flat model of their sprite (gui/items.png overrides or their sheet).
 */
public final class AssetsB173 implements GeneratedPack.Contributor {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String MISSING = "minecraft:missingno";
    static final String TERRAIN = "/terrain.png";
    static final String ITEMS = "/gui/items.png";

    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
            "purple", "blue", "brown", "green", "red", "black"};
    /** b1.7.3 terrain.png sprite index -> modern block texture, for the sprites mods reuse. */
    private static final Map<Integer, String> VANILLA_TERRAIN = new HashMap<>();

    static {
        String[] row = {"grass_block_top", "stone", "dirt", "grass_block_side", "oak_planks", "smooth_stone_slab_side", "smooth_stone", "bricks",
                "tnt_side", "tnt_top", "tnt_bottom", "cobweb", "poppy", "dandelion", null, "oak_sapling",
                "cobblestone", "bedrock", "sand", "gravel", "oak_log", "oak_log_top", "iron_block", "gold_block",
                "diamond_block", null, null, null, "red_mushroom", "brown_mushroom", null, null,
                "gold_ore", "iron_ore", "coal_ore", "bookshelf", "mossy_cobblestone", "obsidian", null, null,
                null, null, null, "crafting_table_top", "furnace_front", "furnace_side", "dispenser_front", null,
                "sponge", "glass", "diamond_ore", "redstone_ore", "oak_leaves", null, null, "dead_bush",
                null, null, null, "crafting_table_side", "crafting_table_front", "furnace_front_on", "furnace_top", null,
                "white_wool", "spawner", "snow", "ice", "grass_block_snow", "cactus_top", "cactus_side", "cactus_bottom",
                "clay", "sugar_cane", "note_block", "jukebox_top", null, null, null, null,
                "torch", null, null, "ladder", null, null, "farmland_moist", "farmland"};
        for (int i = 0; i < row.length; i++) if (row[i] != null) VANILLA_TERRAIN.put(i, row[i]);
        for (int s = 0; s < 8; s++) VANILLA_TERRAIN.put(88 + s, "wheat_stage" + s);
        VANILLA_TERRAIN.put(96, "lever");
        VANILLA_TERRAIN.put(99, "redstone_torch");
        VANILLA_TERRAIN.put(102, "pumpkin_top");
        VANILLA_TERRAIN.put(103, "netherrack");
        VANILLA_TERRAIN.put(104, "soul_sand");
        VANILLA_TERRAIN.put(105, "glowstone");
        VANILLA_TERRAIN.put(112, "rail_corner");
        VANILLA_TERRAIN.put(115, "redstone_torch_off");
        VANILLA_TERRAIN.put(116, "spruce_log");
        VANILLA_TERRAIN.put(117, "birch_log");
        VANILLA_TERRAIN.put(118, "pumpkin_side");
        VANILLA_TERRAIN.put(119, "carved_pumpkin");
        VANILLA_TERRAIN.put(120, "jack_o_lantern");
        VANILLA_TERRAIN.put(128, "rail");
        VANILLA_TERRAIN.put(144, "lapis_block");
        VANILLA_TERRAIN.put(160, "lapis_ore");
        VANILLA_TERRAIN.put(176, "sandstone_top");
        VANILLA_TERRAIN.put(192, "sandstone");
        VANILLA_TERRAIN.put(208, "sandstone_bottom");
        // BlockCloth: meta 0 is sprite 64, the others 113 + ((~meta & 8) >> 3) + (~meta & 7) * 16
        for (int m = 1; m < 16; m++) VANILLA_TERRAIN.put(113 + ((~m & 8) >> 3) + (~m & 7) * 16, COLORS[m] + "_wool");
    }

    /** b1.7.3 gui/items.png sprite index -> modern item texture, for the icons mods reuse. */
    private static final Map<Integer, String> VANILLA_ITEMS = new HashMap<>();

    static {
        String[] armor = {"leather", "chainmail", "iron", "diamond", "golden"};
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        for (int r = 0; r < 4; r++) for (int c = 0; c < 5; c++) VANILLA_ITEMS.put(r * 16 + c, armor[c] + "_" + pieces[r]);
        String[] tools = {"sword", "shovel", "pickaxe", "axe", "hoe"};
        String[] tiers = {"wooden", "stone", "iron", "diamond", "golden"};
        for (int r = 0; r < 5; r++) for (int c = 0; c < 5; c++) VANILLA_ITEMS.put((4 + r) * 16 + c, tiers[c] + "_" + tools[r]);
        Object[][] singles = {{5, "flint_and_steel"}, {6, "flint"}, {7, "coal"}, {8, "string"}, {9, "wheat_seeds"}, {10, "apple"},
                {11, "golden_apple"}, {12, "egg"}, {13, "sugar"}, {14, "snowball"}, {21, "bow"}, {22, "brick"}, {23, "iron_ingot"}, {24, "feather"},
                {25, "wheat"}, {26, "painting"}, {27, "sugar_cane"}, {28, "bone"}, {29, "cake"}, {30, "slime_ball"}, {37, "arrow"}, {39, "gold_ingot"},
                {40, "gunpowder"}, {41, "bread"}, {42, "oak_sign"}, {43, "oak_door"}, {44, "iron_door"}, {53, "stick"}, {54, "compass_00"},
                {55, "diamond"}, {56, "redstone"}, {57, "clay_ball"}, {58, "paper"}, {59, "book"}, {60, "filled_map"}, {69, "fishing_rod"},
                {70, "clock_00"}, {71, "bowl"}, {72, "mushroom_stew"}, {73, "glowstone_dust"}, {74, "bucket"}, {75, "water_bucket"},
                {76, "lava_bucket"}, {77, "milk_bucket"}, {86, "repeater"}, {87, "porkchop"}, {88, "cooked_porkchop"}, {89, "cod"},
                {90, "cooked_cod"}, {92, "cookie"}, {93, "shears"}, {102, "leather"}, {135, "minecart"}, {136, "oak_boat"},
                {151, "chest_minecart"}, {167, "furnace_minecart"}, {240, "music_disc_13"}, {241, "music_disc_cat"}};
        for (Object[] e : singles) VANILLA_ITEMS.put((Integer) e[0], (String) e[1]);
        // ItemDye: iconIndex 78 + (damage % 8) * 16 + damage / 8
        String[] dyes = {"ink_sac", "red_dye", "green_dye", "cocoa_beans", "lapis_lazuli", "purple_dye", "cyan_dye", "light_gray_dye", "gray_dye",
                "pink_dye", "lime_dye", "yellow_dye", "light_blue_dye", "magenta_dye", "orange_dye", "bone_meal"};
        for (int d = 0; d < 16; d++) VANILLA_ITEMS.put(78 + (d % 8) * 16 + d / 8, dyes[d]);
    }

    /** sheet + "#" + index -> texture reference, for one pack build. */
    private final Map<String, String> sprites = new HashMap<>();
    private final Map<String, BufferedImage> sheets = new HashMap<>();
    private Map<String, ContentB173.TextureOverride> overrides;
    private Map<String, Supplier<byte[]>> out;
    private int missing;

    @Override
    public synchronized void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (ContentB173.blocks().isEmpty() && ContentB173.items().isEmpty()) return;
        String selfTest = System.getProperty("legacyconnector.assetsSelfTest");
        if (type == PackType.SERVER_DATA && selfTest != null) {
            // a dedicated server never loads client assets: the self test generates them anyway (-PassetsDump=<dir> writes them out)
            Map<String, Supplier<byte[]>> files = new java.util.TreeMap<>();
            generate(files);
            if (!selfTest.equals("true") && !selfTest.isEmpty()) {
                for (var e : files.entrySet()) {
                    try {
                        Path path = Path.of(selfTest, e.getKey());
                        Files.createDirectories(path.getParent());
                        Files.write(path, e.getValue().get());
                    } catch (IOException | RuntimeException ex) {
                        LOG.warn("Could not write {}: {}", e.getKey(), ex.toString());
                    }
                }
            }
            return;
        }
        if (type == PackType.CLIENT_RESOURCES) generate(out);
    }

    private void generate(Map<String, Supplier<byte[]>> out) {
        this.out = out;
        sprites.clear();
        sheets.clear();
        missing = 0;
        overrides = new HashMap<>();
        for (var o : ContentB173.textureOverrides()) overrides.put(o.sheet() + "#" + o.index(), o);
        int blocks = 0, items = 0;
        for (Block block : ContentB173.blocks()) {
            if (block.mcc$modern == null || !(block.mcc$owner instanceof LegacyModRecord mod) || BridgeB173.isVanillaReplacement(block.mcc$modern)) continue;
            try {
                blockAssets(block, mod);
                blocks++;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("No models for b1.7.3 block {}: {}", block, e.toString());
            }
        }
        for (Item item : ContentB173.items()) {
            if (item.mcc$modern == null || item instanceof BlockItem || !(item.mcc$owner instanceof LegacyModRecord mod)
                    || BridgeB173.isVanillaReplacement(item.mcc$modern)) continue;
            try {
                itemAssets(item, mod);
                items++;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("No model for b1.7.3 item {}: {}", item, e.toString());
            }
        }
        LOG.info("b1.7.3 assets: {} blocks, {} items, {} textures; {} sprite references without a texture", blocks, items,
                sprites.values().stream().filter(s -> !s.startsWith("minecraft:")).distinct().count(), missing);
        this.out = null;
    }

    // ------------------------------------------------------------------------------------------------------------- blocks

    private void blockAssets(Block block, LegacyModRecord mod) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block.mcc$modern);
        String sheet = sheetOf(block, TERRAIN);
        int renderType = safe(block::getRenderType, 0);
        boolean opaque = safeBool(block::isOpaque, true);
        int layer = safe(block::getRenderLayer, 0);
        JsonObject variants = new JsonObject();
        Map<String, String> modelByJson = new LinkedHashMap<>();
        for (int meta = 0; meta < 16; meta++) {
            final int m = meta;
            String[] tex = new String[6];
            for (int side = 0; side < 6; side++) {
                final int s = side;
                tex[side] = texture(mod, sheet, safe(() -> block.getTexture(s, m), block.textureId), false);
            }
            JsonObject model = blockModel(block, renderType, tex, opaque, layer);
            String json = GSON.toJson(model);
            String name = modelByJson.get(json);
            if (name == null) {
                name = id.getNamespace() + ":block/b173/" + id.getPath() + "_m" + meta;
                modelByJson.put(json, name);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                out.put("assets/" + id.getNamespace() + "/models/block/b173/" + id.getPath() + "_m" + meta + ".json", () -> bytes);
            }
            JsonObject variant = new JsonObject();
            variant.addProperty("model", name);
            variants.add("meta=" + meta, variant);
        }
        JsonObject state = new JsonObject();
        state.add("variants", variants);
        put("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json", state);

        var item = block.mcc$modern.asItem();
        if (item != net.minecraft.world.item.Items.AIR) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            JsonObject itemModel = new JsonObject();
            if (renderType == 1 || renderType == 2 || renderType == 6) {
                itemModel.addProperty("parent", "minecraft:item/generated");
                JsonObject t = new JsonObject();
                t.addProperty("layer0", texture(mod, sheet, safe(() -> block.getTexture(0, 0), block.textureId), false));
                itemModel.add("textures", t);
            } else {
                itemModel.addProperty("parent", modelByJson.values().iterator().next());
            }
            put("assets/" + itemId.getNamespace() + "/models/item/" + itemId.getPath() + ".json", itemModel);
        }
    }

    private static JsonObject blockModel(Block block, int renderType, String[] tex, boolean opaque, int layer) {
        JsonObject model = new JsonObject();
        JsonObject textures = new JsonObject();
        switch (renderType) {
            case 1 -> { // crossed squares: flowers, saplings, reeds
                model.addProperty("parent", "minecraft:block/cross");
                textures.addProperty("cross", tex[0]);
                textures.addProperty("particle", tex[0]);
                model.addProperty("render_type", "minecraft:cutout");
            }
            case 6 -> { // crops
                model.addProperty("parent", "minecraft:block/crop");
                textures.addProperty("crop", tex[0]);
                textures.addProperty("particle", tex[0]);
                model.addProperty("render_type", "minecraft:cutout");
            }
            case 2 -> { // torches
                model.addProperty("parent", "minecraft:block/template_torch");
                textures.addProperty("torch", tex[0]);
                textures.addProperty("particle", tex[0]);
                model.addProperty("render_type", "minecraft:cutout");
            }
            default -> {
                String[] faces = {"down", "up", "north", "south", "west", "east"};
                for (int i = 0; i < 6; i++) textures.addProperty(faces[i], tex[i]);
                textures.addProperty("particle", tex[2]);
                double x0 = block.minX, y0 = block.minY, z0 = block.minZ, x1 = block.maxX, y1 = block.maxY, z1 = block.maxZ;
                boolean full = x0 <= 0 && y0 <= 0 && z0 <= 0 && x1 >= 1 && y1 >= 1 && z1 >= 1 || x1 <= x0 || y1 <= y0 || z1 <= z0;
                if (full) {
                    model.addProperty("parent", "minecraft:block/cube");
                } else {
                    model.addProperty("parent", "minecraft:block/block");
                    JsonObject element = new JsonObject();
                    element.add("from", vec(x0, y0, z0));
                    element.add("to", vec(x1, y1, z1));
                    JsonObject fs = new JsonObject();
                    boolean[] onEdge = {y0 <= 0, y1 >= 1, z0 <= 0, z1 >= 1, x0 <= 0, x1 >= 1};
                    for (int i = 0; i < 6; i++) {
                        JsonObject f = new JsonObject();
                        f.addProperty("texture", "#" + faces[i]);
                        if (onEdge[i]) f.addProperty("cullface", faces[i]);
                        fs.add(faces[i], f);
                    }
                    element.add("faces", fs);
                    JsonArray elements = new JsonArray();
                    elements.add(element);
                    model.add("elements", elements);
                }
                if (layer == 1) model.addProperty("render_type", "minecraft:translucent");
                else if (!opaque) model.addProperty("render_type", "minecraft:cutout");
            }
        }
        model.add("textures", textures);
        return model;
    }

    private static JsonArray vec(double x, double y, double z) {
        JsonArray a = new JsonArray();
        a.add(Math.round(Math.max(0, Math.min(1, x)) * 1600) / 100.0);
        a.add(Math.round(Math.max(0, Math.min(1, y)) * 1600) / 100.0);
        a.add(Math.round(Math.max(0, Math.min(1, z)) * 1600) / 100.0);
        return a;
    }

    // ------------------------------------------------------------------------------------------------------------- items

    private void itemAssets(Item item, LegacyModRecord mod) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item.mcc$modern);
        String sheet = sheetOf(item, ITEMS);
        JsonObject model = new JsonObject();
        model.addProperty("parent", safeBool(item::isHandheld, false) ? "minecraft:item/handheld" : "minecraft:item/generated");
        JsonObject t = new JsonObject();
        t.addProperty("layer0", texture(mod, sheet, safe(() -> item.getTextureId(0), 0), true));
        model.add("textures", t);
        put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", model);
    }

    // ------------------------------------------------------------------------------------------------------------- sprites

    private static String sheetOf(Object o, String fallback) {
        if (o instanceof ITextureProvider tp) {
            try {
                String f = tp.getTextureFile();
                if (f != null && !f.isBlank()) return f.startsWith("/") ? f : "/" + f;
            } catch (RuntimeException | LinkageError e) {
                // the default sheet
            }
        }
        return fallback;
    }

    /** The texture reference of sprite {@code index} of {@code sheet}, generating the texture file when it comes from a mod. */
    private String texture(LegacyModRecord mod, String sheet, int index, boolean item) {
        index = Math.abs(index); // b1.7.3 doors return -index for a mirrored face
        String key = sheet + "#" + index;
        String ref = sprites.get(key);
        if (ref != null) return ref;
        ref = resolve(mod, sheet, index, item);
        if (ref == null) {
            missing++;
            ref = MISSING;
        }
        sprites.put(key, ref);
        return ref;
    }

    private String resolve(LegacyModRecord mod, String sheet, int index, boolean item) {
        ContentB173.TextureOverride o = overrides.get(sheet + "#" + index);
        if (o != null) {
            LegacyModRecord owner = o.mod() != null ? o.mod() : mod;
            BufferedImage img = image(owner, o.path());
            if (img == null) return null;
            int size = Math.min(img.getWidth(), img.getHeight());
            return write(owner, (item ? "i" : "t") + index, img.getSubimage(0, 0, size, size));
        }
        if (sheet.equals(TERRAIN)) {
            String vanilla = VANILLA_TERRAIN.get(index);
            return vanilla == null ? null : "minecraft:block/" + vanilla;
        }
        if (sheet.equals(ITEMS)) {
            String vanilla = VANILLA_ITEMS.get(index);
            return vanilla == null ? null : "minecraft:item/" + vanilla;
        }
        BufferedImage img = sheets.computeIfAbsent(sheet, s -> image(mod, s));
        if (img == null || index < 0 || index > 255) return null;
        int tile = img.getWidth() / 16;
        if (tile <= 0 || img.getHeight() < tile * 16) return null;
        BufferedImage sprite = img.getSubimage((index % 16) * tile, (index / 16) * tile, tile, tile);
        String base = sheet.toLowerCase(Locale.ROOT).replaceAll("\\.png$", "").replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return write(mod, base + "_" + index, sprite);
    }

    private String write(LegacyModRecord mod, String name, BufferedImage img) {
        String ns = mod != null ? mod.modernId : "legacyconnector";
        String path = "b173/" + name;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bytes);
            byte[] data = bytes.toByteArray();
            out.put("assets/" + ns + "/textures/block/" + path + ".png", () -> data);
        } catch (IOException e) {
            return null;
        }
        return ns + ":block/" + path;
    }

    /** An image from the mod's jar (a ModLoader path such as {@code /nether/dirt.png}), else from any b1.7.3 mod's jar. */
    private static BufferedImage image(LegacyModRecord mod, String path) {
        byte[] data = resource(mod, path);
        if (data == null) {
            for (LegacyModRecord m : LegacyMods.all()) {
                if (m == mod || !PlatformB173.VERSION.equals(m.version)) continue;
                data = resource(m, path);
                if (data != null) break;
            }
        }
        if (data == null) return null;
        try {
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] resource(LegacyModRecord mod, String path) {
        if (mod == null || mod.container == null || path == null) return null;
        String rel = path.startsWith("/") ? path.substring(1) : path;
        try {
            Path p = mod.container.getModInfo().getOwningFile().getFile().findResource(rel.split("/"));
            return Files.isRegularFile(p) ? Files.readAllBytes(p) : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private void put(String path, JsonObject json) {
        byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
        out.put(path, () -> bytes);
    }

    private static int safe(java.util.function.IntSupplier s, int fallback) {
        try {
            return s.getAsInt();
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    private static boolean safeBool(java.util.function.BooleanSupplier s, boolean fallback) {
        try {
            return s.getAsBoolean();
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }
}
