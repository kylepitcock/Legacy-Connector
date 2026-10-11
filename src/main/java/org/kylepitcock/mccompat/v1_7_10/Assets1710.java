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
        IIconRegister blocks = name -> new Icon(texture(name, true));
        IIconRegister items = name -> new Icon(texture(name, false));
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
    }

    /** {@code "modid:name"} -> {@code modid:blocks/name}; bare vanilla names map to the modern vanilla textures. */
    static String texture(String legacy, boolean block) {
        if (legacy == null) return "minecraft:missingno";
        int colon = legacy.indexOf(':');
        if (colon < 0) {
            String n = VANILLA_TEXTURE_RENAMES.getOrDefault(legacy, legacy);
            return "minecraft:" + (block ? "block/" : "item/") + Ids.normalizePath(n);
        }
        return Ids.namespace(legacy.substring(0, colon)) + ":" + (block ? "blocks/" : "items/") + Ids.normalizePath(legacy.substring(colon + 1));
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

    private static JsonObject model(Block b, String[] faces) {
        JsonObject model = new JsonObject();
        JsonObject tex = new JsonObject();
        if (b.getRenderType() == 22) {
            // Chest-style: no geometry, the tile entity renderer draws it; keep the particle texture for breaking.
            tex.addProperty("particle", faces[1]);
            model.add("textures", tex);
            return model;
        }
        if (b.getRenderType() == 1) {
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
            if (item instanceof ItemBlock ib && ib.mcc$block().getRenderType() == 22) {
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
                    try {
                        IIcon second = item.getIconFromDamageForRenderPass(meta, 1);
                        if (second != null) tex.addProperty("layer1", second.getIconName());
                    } catch (RuntimeException ignored) {
                    }
                }
                model.add("textures", tex);
            }
            put(out, "assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", model);
        }
    }

    private static void put(Map<String, Supplier<byte[]>> out, String path, JsonObject json) {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        out.put(path, () -> bytes);
    }

    /** Recorded icon; its "name" is already the modern sprite id. UVs are full-sprite placeholders. */
    record Icon(String getIconName) implements IIcon {
        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }

        @Override
        public float getMinU() {
            return 0;
        }

        @Override
        public float getMaxU() {
            return 1;
        }

        @Override
        public float getInterpolatedU(double u) {
            return (float) (u / 16.0);
        }

        @Override
        public float getMinV() {
            return 0;
        }

        @Override
        public float getMaxV() {
            return 1;
        }

        @Override
        public float getInterpolatedV(double v) {
            return (float) (v / 16.0);
        }
    }

    @Override
    public String toString() {
        return "1.7.10 models";
    }
}
