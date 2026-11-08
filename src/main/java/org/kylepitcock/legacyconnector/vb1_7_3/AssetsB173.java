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

    /**
     * Vanilla textures with transparent pixels, for mod blocks that reuse them. b1.7.3 drew all terrain with an alpha test, so a block
     * whose sprites have holes showed them even as an "opaque" block: such models are cutout ({@link #hasAlpha}).
     */
    private static final java.util.Set<String> VANILLA_SEE_THROUGH = java.util.Set.of("glass", "oak_leaves", "cobweb", "poppy", "dandelion",
            "oak_sapling", "red_mushroom", "brown_mushroom", "dead_bush", "torch", "ladder", "sugar_cane", "spawner", "rail", "rail_corner",
            "lever", "redstone_torch", "redstone_torch_off", "cactus_side", "cactus_top", "cactus_bottom");

    /** sheet + "#" + index -> texture reference, for one pack build. */
    private final Map<String, String> sprites = new HashMap<>();
    /** generated texture references with at least one pixel that is not fully opaque */
    private final java.util.Set<String> withAlpha = new java.util.HashSet<>();
    private final Map<String, BufferedImage> sheets = new HashMap<>();
    private Map<String, ContentB173.TextureOverride> overrides;
    private Map<String, Supplier<byte[]>> out;
    private int missing;
    private static final Map<Block,Map<Integer,String>> WORLD_TEXTURES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String,String> SPRITE_REFS=new java.util.concurrent.ConcurrentHashMap<>();
    /** Single-sprite texture used by original terrain/item particle renderers. */
    public static ResourceLocation particleSprite(int group,int index){
        String sheet=group==1?TERRAIN:group==2?ITEMS:null;if(sheet==null)return null;
        String reference=SPRITE_REFS.get(sheet+"#"+index);
        if(reference==null){String vanilla=(group==1?VANILLA_TERRAIN:VANILLA_ITEMS).get(index);if(vanilla!=null)reference="minecraft:"+(group==1?"block/":"item/")+vanilla;}
        if(reference==null)return null;var id=ResourceLocation.parse(reference);return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"textures/"+id.getPath()+".png");
    }

    /** Palette baked into the atlas for world-dependent face selection. */
    public static Map<Integer,String> worldTextures(Block block) { return WORLD_TEXTURES.getOrDefault(block,Map.of()); }
    public static boolean usesWorldTextures(Block block) {
        try { return block.getClass().getMethod("getTextureId",legacyconnector.vb1_7_3.net.minecraft.world.BlockView.class,int.class,int.class,int.class,int.class).getDeclaringClass()!=Block.class; }
        catch(ReflectiveOperationException e){return false;}
    }
    public static int[] faceTextures(Block block,net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state) {
        var view=new BlockQueryB173(level,pos,state);int[] faces=new int[6];
        synchronized(block) { for(int side=0;side<6;side++){final int s=side;faces[side]=safe(()->block.getTextureId(view,pos.getX(),pos.getY(),pos.getZ(),s),block.textureId);} }
        return faces;
    }

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
        AnimationsB173.registerHooks();
        ProjectileTexturesB173.contribute(out);
        sprites.clear();
        SPRITE_REFS.clear();
        sheets.clear();
        withAlpha.clear();
        missing = 0;
        overrides = new HashMap<>();
        for (var o : ContentB173.textureOverrides()) overrides.put(o.sheet() + "#" + o.index(), o);
        // Overrides can be consumed exclusively by particle callbacks, without an item or block model referencing them.
        for(var override:ContentB173.textureOverrides())texture(override.mod(),override.sheet(),override.index(),ITEMS.equals(override.sheet()));
        for(var animation:AnimationsB173.entries())if(animation.owner()!=null){
            int repeat=Math.max(1,animation.effect().replicate);
            for(int y=0;y<repeat;y++)for(int x=0;x<repeat;x++)texture(animation.owner(),animation.sheet(),animation.effect().sprite+x+y*16,animation.effect().atlas==1);
        }
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
        String[] metaModels = new String[16];
        Map<Integer,String> palette=new java.util.TreeMap<>();
        if(usesWorldTextures(block) || BlockRenderB173.hasRenderer(renderType)) {
            // Include faces only encountered beside neighbours or with a tile entity, before atlas stitching.
            var indices=new java.util.TreeSet<Integer>();for(int i=0;i<256;i++)indices.add(i);
            for(var o:ContentB173.textureOverrides())if(o.sheet().equals(sheet))indices.add(o.index());
            for(int index:indices){String key=sheet+"#"+index;String ref=sprites.get(key);if(ref==null){ref=resolve(mod,sheet,index,false);if(ref!=null)sprites.put(key,ref);}if(ref!=null)palette.put(index,ref);}
        }
        WORLD_TEXTURES.put(block,Map.copyOf(palette));
        for (int meta = 0; meta < 16; meta++) {
            final int m = meta;
            String[] tex = new String[6];
            int[] worldFaces=faceTextures(block,net.minecraft.world.level.EmptyBlockGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO,block.mcc$modern.defaultBlockState().setValue(LegacyB173Block.META,meta));
            for (int side = 0; side < 6; side++) {
                final int s = side;
                tex[side] = texture(mod, sheet, worldFaces[side], false);
            }
            boolean seeThrough = false;
            for (String t : tex) seeThrough |= hasAlpha(t);
            JsonObject model = blockModel(block, renderType, tex, opaque && !seeThrough, layer,meta);
            palette.forEach((index,ref)->model.getAsJsonObject("textures").addProperty("world_"+index,ref));
            String json = GSON.toJson(model);
            String name = modelByJson.get(json);
            if (name == null) {
                name = id.getNamespace() + ":block/b173/" + id.getPath() + "_m" + meta;
                modelByJson.put(json, name);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                out.put("assets/" + id.getNamespace() + "/models/block/b173/" + id.getPath() + "_m" + meta + ".json", () -> bytes);
            }
            metaModels[meta] = name;
            JsonObject variant = new JsonObject();
            variant.addProperty("model", name);
            if(renderType==10)variant.addProperty("y",new int[]{0,180,90,270}[meta&3]);
            if(renderType==2 && meta>=1 && meta<=4)variant.addProperty("y",new int[]{0,0,180,90,270}[meta]);
            variants.add("meta=" + meta, variant);
        }
        JsonObject state = new JsonObject();
        state.add("variants", variants);
        put("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json", state);

        var item = block.mcc$modern.asItem();
        if (item != net.minecraft.world.item.Items.AIR) itemModels(block, mod, sheet, renderType, metaModels, item);
    }

    /**
     * The block item's model: the block's own (metadata 0), or a flat icon for plants, crops and the like (render types 1, 2, 6: the
     * legacy item's {@code getIconFromDamage}). An item with subtypes gets an override per metadata on {@link LegacyB173BlockItem#META}
     * (every metadata, since overrides pick the last one whose threshold the value reaches).
     */
    private void itemModels(Block block, LegacyModRecord mod, String sheet, int renderType, String[] metaModels, net.minecraft.world.item.Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        Item legacy = block.id >= 0 && block.id < Item.ITEMS.length ? Item.ITEMS[block.id] : null;
        boolean subtypes = item instanceof LegacyB173BlockItem bi && bi.hasSubtypes();
        boolean flat = renderType == 1 || renderType == 2 || renderType == 6;
        String[] models = new String[16];
        String base = itemId.getNamespace() + ":item/b173/" + itemId.getPath() + "_m0";
        for (int meta = 0; meta < (subtypes ? 16 : 1); meta++) {
            if (!flat) {
                models[meta] = metaModels[meta];
                continue;
            }
            final int m = meta;
            String icon = texture(mod, sheet, legacy != null ? safe(() -> legacy.getTextureId(m), block.textureId)
                    : safe(() -> block.getTexture(2, m), block.textureId), false);
            JsonObject flatModel = new JsonObject();
            flatModel.addProperty("parent", "minecraft:item/generated");
            JsonObject t = new JsonObject();
            t.addProperty("layer0", icon);
            flatModel.add("textures", t);
            if (meta == 0) {
                models[0] = GSON.toJson(flatModel);
                // the subtypes that look like metadata 0 point to a copy (an override naming the item's own model would refer to itself)
                if (subtypes) put("assets/" + itemId.getNamespace() + "/models/item/b173/" + itemId.getPath() + "_m0.json", flatModel);
                continue;
            }
            String json = GSON.toJson(flatModel);
            if (json.equals(models[0])) {
                models[meta] = base;
            } else {
                models[meta] = itemId.getNamespace() + ":item/b173/" + itemId.getPath() + "_m" + meta;
                put("assets/" + itemId.getNamespace() + "/models/item/b173/" + itemId.getPath() + "_m" + meta + ".json", flatModel);
            }
        }
        JsonObject itemModel;
        if (flat) {
            itemModel = GSON.fromJson(models[0], JsonObject.class);
        } else {
            itemModel = new JsonObject();
            itemModel.addProperty("parent", metaModels[0]);
        }
        if (subtypes) {
            boolean differ = false;
            for (int meta = 1; meta < 16; meta++) differ |= !models[meta].equals(flat ? base : models[0]);
            if (differ) {
                JsonArray overrides = new JsonArray();
                for (int meta = 1; meta < 16; meta++) {
                    JsonObject o = new JsonObject();
                    JsonObject predicate = new JsonObject();
                    predicate.addProperty(LegacyB173BlockItem.META.toString(), meta / 16.0F);
                    o.add("predicate", predicate);
                    o.addProperty("model", models[meta]);
                    overrides.add(o);
                }
                itemModel.add("overrides", overrides);
            }
        }
        put("assets/" + itemId.getNamespace() + "/models/item/" + itemId.getPath() + ".json", itemModel);
    }

    private static JsonObject blockModel(Block block,int renderType,String[] tex,boolean opaque,int layer,int meta) {
        synchronized(block) {
            double[] bounds={block.minX,block.minY,block.minZ,block.maxX,block.maxY,block.maxZ};
            try {
                block.updateBoundingBox(new BlockQueryB173(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO,
                        block.mcc$modern.defaultBlockState().setValue(LegacyB173Block.META,meta)),0,0,0);
                return boundedBlockModel(block,renderType,tex,opaque,layer,meta);
            } finally { block.minX=bounds[0];block.minY=bounds[1];block.minZ=bounds[2];block.maxX=bounds[3];block.maxY=bounds[4];block.maxZ=bounds[5]; }
        }
    }
    private static JsonObject boundedBlockModel(Block block, int renderType, String[] tex, boolean opaque, int layer,int meta) {
        JsonObject model = new JsonObject();
        JsonObject textures = new JsonObject();
        switch (renderType) {
            case 11 -> {model.addProperty("parent","minecraft:block/fence_inventory");textures.addProperty("texture",tex[0]);textures.addProperty("particle",tex[0]);}
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
                model.addProperty("parent", meta>=1 && meta<=4?"minecraft:block/template_torch_wall":"minecraft:block/template_torch");
                textures.addProperty("torch", tex[0]);
                textures.addProperty("particle", tex[0]);
                model.addProperty("render_type", "minecraft:cutout");
            }
            case 10 -> {
                model.addProperty("parent","minecraft:block/stairs");
                textures.addProperty("bottom",tex[0]);textures.addProperty("top",tex[1]);textures.addProperty("side",tex[2]);textures.addProperty("particle",tex[2]);
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
                        final int face=i;
                        if(safe(()->block.getTexture(face,meta),block.textureId)<0) {
                            JsonArray uv=new JsonArray();for(int v:new int[]{16,0,0,16})uv.add(v);f.add("uv",uv);
                        }
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
        if (item.hasSubtypes()) {
            JsonArray variants = new JsonArray();
            for (int meta=0; meta<16; meta++) {
                final int m=meta;
                JsonObject variant=model.deepCopy();
                variant.getAsJsonObject("textures").addProperty("layer0",texture(mod,sheet,safe(() -> item.getTextureId(m),0),true));
                String path=id.getPath()+"_m"+meta;
                put("assets/"+id.getNamespace()+"/models/item/"+path+".json",variant);
                JsonObject entry=new JsonObject(), predicate=new JsonObject();
                predicate.addProperty(LegacyB173BlockItem.META.toString(),meta/16.0F);
                entry.add("predicate",predicate); entry.addProperty("model",id.getNamespace()+":item/"+path); variants.add(entry);
            }
            model.add("overrides",variants);
        }
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
        SPRITE_REFS.put(key,ref);
        return ref;
    }

    private String resolve(LegacyModRecord mod, String sheet, int index, boolean item) {
        var animation=AnimationsB173.find(sheet,index);
        if(animation!=null){
            var owner=animation.owner()!=null?animation.owner():mod;
            if(owner!=null){
                String name="animated_"+sheet.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_")+"_"+index;
                String ref=write(owner,name,AnimationsB173.image(animation.effect()));
                if(ref!=null){AnimationsB173.target(sheet,index,ref);withAlpha.add(ref);}return ref;
            }
        }
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

    private boolean hasAlpha(String ref) {
        if (ref.startsWith("minecraft:block/")) {
            String n = ref.substring("minecraft:block/".length());
            return VANILLA_SEE_THROUGH.contains(n) || n.startsWith("wheat_stage");
        }
        return withAlpha.contains(ref);
    }

    private String write(LegacyModRecord mod, String name, BufferedImage img) {
        String ns = mod != null ? mod.modernId : "legacyconnector";
        String path = "b173/" + name;
        if (img.getColorModel().hasAlpha()) {
            scan:
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    if ((img.getRGB(x, y) >>> 24) < 255) {
                        withAlpha.add(ns + ":block/" + path);
                        break scan;
                    }
                }
            }
        }
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

    /** An image from any b1.7.3 mod's jar (a GUI's {@code /ic2/sprites/GUIMacerator.png}), or null. */
    public static BufferedImage modImage(String path) {
        return image(null, path);
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
    /** Original legacy resource paths, shared by texture-pack and animation APIs. */
    public static byte[] resourceBytes(String path){
        byte[] bytes=resource(ContentB173.current(),path);if(bytes!=null)return bytes;
        for(var mod:LegacyMods.all())if(PlatformB173.VERSION.equals(mod.version)){bytes=resource(mod,path);if(bytes!=null)return bytes;}
        return null;
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
