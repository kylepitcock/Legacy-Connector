package org.kylepitcock.mccompat.v1_7_10;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemBlock;
import mccompat.v1_7_10.net.minecraft.util.IIcon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.mccompat.core.Ids;
import org.kylepitcock.mccompat.core.pack.GeneratedPack;
import org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns 1.7.10 icon-based rendering into modern JSON models.
 *
 * <p>1.7.10 blocks described their look in code: {@code registerBlockIcons} registered texture names and
 * {@code getIcon(side, meta)} picked one per face and metadata. This contributor runs exactly that code with a
 * recording icon register and writes one blockstate (16 metadata variants) and one cube/cross model per
 * metadata. Textures stay where the mod shipped them ({@code textures/blocks/}); the loader adds that folder to
 * the block atlas. Custom renderers (ISBRH, render types other than cube/cross) fall back to a cube.</p>
 */
public final class Assets1710 implements GeneratedPack.Contributor {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/Models");
    private static final Map<String, String> VANILLA_TEXTURE_RENAMES = Map.ofEntries(
            Map.entry("planks_oak", "oak_planks"), Map.entry("planks_spruce", "spruce_planks"), Map.entry("planks_birch", "birch_planks"),
            Map.entry("planks_jungle", "jungle_planks"), Map.entry("log_oak", "oak_log"), Map.entry("log_oak_top", "oak_log_top"),
            Map.entry("stonebrick", "stone_bricks"), Map.entry("brick", "bricks"), Map.entry("wool_colored_white", "white_wool"),
            Map.entry("iron_block", "iron_block"), Map.entry("gold_block", "gold_block"), Map.entry("glass", "glass"),
            Map.entry("furnace_front_off", "furnace_front"), Map.entry("furnace_side", "furnace_side"), Map.entry("furnace_top", "furnace_top"),
            Map.entry("grass_top", "grass_block_top"), Map.entry("grass_side", "grass_block_side"), Map.entry("stone_slab_top", "smooth_stone"),
            Map.entry("quartz_block_side", "quartz_block_side"), Map.entry("red_sand", "red_sand"), Map.entry("hardened_clay", "terracotta"),
            Map.entry("leaves_oak", "oak_leaves"), Map.entry("sapling_oak", "oak_sapling"), Map.entry("snow", "snow"), Map.entry("ice", "ice"));

    @Override
    public void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.CLIENT_RESOURCES) return;
        org.kylepitcock.mccompat.v1_7_10.entity.LegacySpawnEggs.contributeModels(out);
        var blocks = new mccompat.v1_7_10.net.minecraft.client.renderer.texture.TextureMap(0, "textures/blocks");
        var items = new mccompat.v1_7_10.net.minecraft.client.renderer.texture.TextureMap(1, "textures/items");
        for (Block b : BlockBridge.modBlocks()) {
            try {
                b.registerBlockIcons(blocks);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("registerBlockIcons failed for {}", b, e);
            }
            try {
                blockModels(b, out);
            } catch (RuntimeException | LinkageError e) {
                // One broken legacy block (e.g. from a mod that failed to load) must not cost every other model.
                LOG.warn("Model generation failed for {}: {}", b, e.toString());
            }
        }
        for (Item i : ItemBridge.modItems()) {
            try {
                i.registerIcons(items);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("registerIcons failed for {}", i, e);
            }
            try {
                itemModels(i, out);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Model generation failed for {}: {}", i, e.toString());
            }
        }
        bakeSprites(blocks, true, out);
        bakeSprites(items, false, out);
    }

    /** (called after all registerIcons) writes the textures of sprites mods built themselves, e.g. unstitched sheets. */
    private static void bakeSprites(mccompat.v1_7_10.net.minecraft.client.renderer.texture.TextureMap map, boolean block,
            Map<String, Supplier<byte[]>> out) {
        org.kylepitcock.mccompat.v1_7_10.client.LegacySprites.bake(map, block, out);
    }

    /** {@code "modid:name"} -> {@code modid:blocks/name}; bare vanilla names map to the modern vanilla textures. */
    private static final Map<String, String> VANILLA_RENAMES = loadRenames();

    /** 1.7.10 vanilla texture names that 1.13+ renamed ({@code blocks/planks_oak} -> {@code block/oak_planks}). */
    private static Map<String, String> loadRenames() {
        Map<String, String> out = new java.util.HashMap<>();
        try (var in = Assets1710.class.getResourceAsStream("/mccompat/legacy/1.7.10/texture-renames.txt")) {
            if (in != null) {
                for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                    String[] p = line.trim().split(" ");
                    if (p.length == 2) out.put(p[0], p[1]);
                }
            }
        } catch (java.io.IOException e) {
            // no table: legacy names are used as they are
        }
        return out;
    }

    /** {@code "modid:name"} -> {@code modid:blocks/name}; bare vanilla names map to the modern vanilla textures. */
    public static String texture(String legacy, boolean block) {
        if (legacy == null) return "minecraft:missingno";
        int colon = legacy.indexOf(':');
        String domain = colon < 0 ? "minecraft" : Ids.namespace(legacy.substring(0, colon));
        String name = colon < 0 ? legacy : legacy.substring(colon + 1);
        if (domain.equals("minecraft")) {
            // Vanilla names may carry their folder ("blocks/stone") or not.
            String bare = name.startsWith("blocks/") || name.startsWith("items/") ? name.substring(name.indexOf('/') + 1) : name;
            String folder = name.startsWith("items/") ? "items/" : name.startsWith("blocks/") ? "blocks/" : block ? "blocks/" : "items/";
            String renamed = VANILLA_RENAMES.get(folder + bare);
            if (renamed == null && !folder.equals(block ? "blocks/" : "items/")) renamed = null;
            if (renamed != null) return "minecraft:" + renamed;
            String n = VANILLA_TEXTURE_RENAMES.getOrDefault(bare, bare);
            return "minecraft:" + (folder.equals("items/") ? "item/" : "block/") + Ids.normalizePath(n);
        }
        return domain + ":" + (block ? "blocks/" : "items/") + Ids.normalizePath(name);
    }

    private static void blockModels(Block b, Map<String, Supplier<byte[]>> out) {
        if (b.mcc$modern == null) return;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b.mcc$modern);
        JsonObject variants = new JsonObject();
        for (int meta = 0; meta < 16; meta++) {
            String[] faces = new String[6];
            for (int side = 0; side < 6; side++) faces[side] = iconName(b, side, meta);
            boolean stairs = b.getRenderType() == 10;
            JsonObject model = stairs ? stairsModel(faces) : model(b, faces);
            String path = "block/" + id.getPath() + "_m" + meta;
            put(out, "assets/" + id.getNamespace() + "/models/" + path + ".json", model);
            JsonObject v = new JsonObject();
            v.addProperty("model", id.getNamespace() + ":" + path);
            if (stairs) {
                // Legacy facing bits: 0 east, 1 west, 2 south, 3 north; bit 2 upside-down. The model faces east.
                int y = new int[] {0, 180, 90, 270}[meta & 3];
                if (y != 0) v.addProperty("y", y);
                if ((meta & 4) != 0) v.addProperty("x", 180);
                v.addProperty("uvlock", true);
            }
            variants.add("meta=" + meta, v);
        }
        JsonObject state = new JsonObject();
        state.add("variants", variants);
        put(out, "assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json", state);
    }

    private static JsonObject stairsModel(String[] faces) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/stairs");
        JsonObject tex = new JsonObject();
        tex.addProperty("bottom", faces[0]);
        tex.addProperty("top", faces[1]);
        tex.addProperty("side", faces[2]);
        tex.addProperty("particle", faces[2]);
        model.add("textures", tex);
        return model;
    }

    private static boolean isbrhIn3d(Block b) {
        try {
            int rt = b.getRenderType();
            var h = mccompat.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.mcc$blockHandler(rt);
            return h != null && h.shouldRender3DInInventory(rt);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private static String iconName(Block b, int side, int meta) {
        try {
            IIcon icon = b.getIcon(side, meta);
            if (icon != null) return icon.getIconName();
        } catch (RuntimeException e) {
            // invalid metadata for this block (array index etc.): fall back to metadata 0
        }
        if (meta != 0) return iconName(b, side, 0);
        return "minecraft:missingno";
    }

    /**
     * Plants drawn by a mod's own block renderer (a custom render id): legacy plants are BlockBush subclasses and
     * their renderers draw crossed planes, which the vanilla cross model reproduces.
     */
    private static boolean plantLike(Block b, int renderType) {
        return renderType > 41 && b instanceof mccompat.v1_7_10.net.minecraft.block.BlockBush;
    }

    private static JsonObject model(Block b, String[] faces) {
        JsonObject model = new JsonObject();
        JsonObject tex = new JsonObject();
        if (b.getRenderType() == 22 || b.getRenderType() == -1) {
            // 22: chest; -1: not drawn as a block at all (a TileEntitySpecialRenderer draws it, e.g. BiblioCraft).
            // Chest-style: no geometry, the tile entity renderer draws it; keep the particle texture for breaking.
            tex.addProperty("particle", faces[1]);
            model.add("textures", tex);
            return model;
        }
        int rt = b.getRenderType();
        if (rt == 6) {
            // Crops: four planes in a '#'.
            model.addProperty("parent", "minecraft:block/crop");
            tex.addProperty("crop", faces[0]);
            model.addProperty("render_type", "minecraft:cutout");
        } else if (rt == 1 || rt == 40 || plantLike(b, rt)) {
            model.addProperty("parent", "minecraft:block/cross");
            tex.addProperty("cross", faces[0]);
            model.addProperty("render_type", "minecraft:cutout");
        } else {
            String[] names = {"down", "up", "north", "south", "west", "east"};
            for (int i = 0; i < 6; i++) tex.addProperty(names[i], faces[i]);
            double x1 = b.getBlockBoundsMinX(), y1 = b.getBlockBoundsMinY(), z1 = b.getBlockBoundsMinZ();
            double x2 = b.getBlockBoundsMaxX(), y2 = b.getBlockBoundsMaxY(), z2 = b.getBlockBoundsMaxZ();
            boolean full = x1 <= 0 && y1 <= 0 && z1 <= 0 && x2 >= 1 && y2 >= 1 && z2 >= 1;
            if (full) {
                model.addProperty("parent", "minecraft:block/cube");
            } else {
                model.addProperty("parent", "minecraft:block/block");
                JsonObject el = new JsonObject();
                el.add("from", vec(x1, y1, z1));
                el.add("to", vec(x2, y2, z2));
                JsonObject f = new JsonObject();
                for (String n : names) {
                    JsonObject face = new JsonObject();
                    face.addProperty("texture", "#" + n);
                    f.add(n, face);
                }
                el.add("faces", f);
                JsonArray els = new JsonArray();
                els.add(el);
                model.add("elements", els);
            }
            if (!b.isOpaqueCube()) model.addProperty("render_type", b.getRenderBlockPass() == 1 ? "minecraft:translucent" : "minecraft:cutout");
        }
        tex.addProperty("particle", faces[1]);
        model.add("textures", tex);
        return model;
    }

    private static JsonArray vec(double x, double y, double z) {
        JsonArray a = new JsonArray();
        a.add(Math.round(Math.max(0, Math.min(1, x)) * 1600) / 100.0);
        a.add(Math.round(Math.max(0, Math.min(1, y)) * 1600) / 100.0);
        a.add(Math.round(Math.max(0, Math.min(1, z)) * 1600) / 100.0);
        return a;
    }

    private static void itemModels(Item item, Map<String, Supplier<byte[]>> out) {
        for (var e : item.mcc$variants.entrySet()) {
            int meta = e.getKey();
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(e.getValue());
            JsonObject model = new JsonObject();
            IIcon icon = null;
            try {
                icon = item.getIconFromDamage(meta);
            } catch (RuntimeException ignored) {
            }
            if (mccompat.v1_7_10.net.minecraftforge.client.MinecraftForgeClient.mcc$hasRenderer(item)) {
                // Forge IItemRenderer: LegacyItemRenderer draws it (item/generated transforms, see that class).
                model.addProperty("parent", "minecraft:builtin/entity");
                model.add("display", GENERATED_DISPLAY.deepCopy());
                JsonObject tex = new JsonObject();
                tex.addProperty("particle", icon == null ? "minecraft:missingno" : icon.getIconName());
                model.add("textures", tex);
            } else if (item instanceof ItemBlock ib && isbrhIn3d(ib.mcc$block())) {
                // Drawn by the block's ISimpleBlockRenderingHandler (LegacyItemRenderer).
                model.addProperty("parent", "minecraft:builtin/entity");
                model.add("display", GENERATED_DISPLAY.deepCopy());
                JsonObject tex = new JsonObject();
                tex.addProperty("particle", iconName(ib.mcc$block(), 1, item.getMetadata(meta) & 15));
                model.add("textures", tex);
            } else if (item instanceof ItemBlock ib && ib.mcc$block().getRenderType() == -1) {
                // 1.7.10 drew blocks without a block model as their 2D icon.
                model.addProperty("parent", "minecraft:item/generated");
                JsonObject tex = new JsonObject();
                tex.addProperty("layer0", icon != null ? icon.getIconName() : iconName(ib.mcc$block(), 1, item.getMetadata(meta) & 15));
                model.add("textures", tex);
            } else if (item instanceof ItemBlock ib && ib.mcc$block().getRenderType() == 22) {
                // builtin/entity + chest display transforms; the BEWLR registered in Client1710 draws the legacy model.
                model.addProperty("parent", "minecraft:item/chest");
                JsonObject tex = new JsonObject();
                tex.addProperty("particle", iconName(ib.mcc$block(), 1, item.getMetadata(meta) & 15));
                model.add("textures", tex);
            } else if (item instanceof ItemBlock ib && (icon == null || ib.mcc$block().getItemIconName() == null)) {
                Block block = ib.mcc$block();
                ResourceLocation bid = BuiltInRegistries.BLOCK.getKey(block.mcc$modern);
                int blockMeta = item.getMetadata(meta) & 15;
                model.addProperty("parent", bid.getNamespace() + ":block/" + bid.getPath() + "_m" + blockMeta);
            } else {
                model.addProperty("parent", item.isFull3D() ? "minecraft:item/handheld" : "minecraft:item/generated");
                JsonObject tex = new JsonObject();
                tex.addProperty("layer0", icon == null ? "minecraft:missingno" : icon.getIconName());
                if (item.requiresMultipleRenderPasses()) {
                    // One layer per render pass; item/generated tints layer N with tint index N (the legacy pass).
                    int passes = 2;
                    try {
                        passes = Math.max(1, Math.min(8, item.getRenderPasses(meta)));
                    } catch (RuntimeException ignored) {
                    }
                    for (int pass = 0; pass < passes; pass++) {
                        try {
                            IIcon layer = item.getIconFromDamageForRenderPass(meta, pass);
                            if (layer != null) tex.addProperty("layer" + pass, layer.getIconName());
                        } catch (RuntimeException ignored) {
                        }
                    }
                }
                model.add("textures", tex);
            }
            put(out, "assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", model);
        }
    }

    /** item/generated's display transforms (builtin/entity models have none of their own). */
    private static final JsonObject GENERATED_DISPLAY = com.google.gson.JsonParser.parseString("""
            {"ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
             "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
             "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
             "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
             "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
             "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}}
            """).getAsJsonObject();

    /** Textures referenced by generated models, by the model's namespace (checked by the client self test). */
    public static final Map<String, java.util.Set<String>> TEXTURE_REFS = new java.util.concurrent.ConcurrentHashMap<>();

    private static void put(Map<String, Supplier<byte[]>> out, String path, JsonObject json) {
        if (json.has("textures") && path.startsWith("assets/")) {
            String ns = path.substring("assets/".length(), path.indexOf('/', "assets/".length()));
            for (var t : json.getAsJsonObject("textures").entrySet()) {
                String ref = t.getValue().getAsString();
                if (!ref.startsWith("#")) TEXTURE_REFS.computeIfAbsent(ns, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(ref);
            }
        }
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        out.put(path, () -> bytes);
    }

    /**
     * Recorded icon; its "name" is already the modern sprite id. On the client its UVs are the sprite's position in
     * the block atlas (legacy code draws atlas-textured quads with them); elsewhere they cover the whole texture.
     */
    record Icon(String getIconName) implements IIcon {
        private float[] uv() {
            return net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? org.kylepitcock.mccompat.v1_7_10.client.AtlasUV.of(getIconName) : null;
        }

        @Override
        public int getIconWidth() {
            float[] uv = uv();
            return uv == null ? 16 : (int) uv[4];
        }

        @Override
        public int getIconHeight() {
            float[] uv = uv();
            return uv == null ? 16 : (int) uv[5];
        }

        @Override
        public float getMinU() {
            float[] uv = uv();
            return uv == null ? 0 : uv[0];
        }

        @Override
        public float getMaxU() {
            float[] uv = uv();
            return uv == null ? 1 : uv[1];
        }

        @Override
        public float getInterpolatedU(double u) {
            return getMinU() + (getMaxU() - getMinU()) * (float) u / 16.0F;
        }

        @Override
        public float getMinV() {
            float[] uv = uv();
            return uv == null ? 0 : uv[2];
        }

        @Override
        public float getMaxV() {
            float[] uv = uv();
            return uv == null ? 1 : uv[3];
        }

        @Override
        public float getInterpolatedV(double v) {
            return getMinV() + (getMaxV() - getMinV()) * (float) v / 16.0F;
        }
    }

    @Override
    public String toString() {
        return "1.7.10 models";
    }
}
