package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The 1.7.10 icon name of a vanilla block's face, read from the modern model of the block state its metadata maps to (the
 * sapling's meta 2 is the birch sapling's sprite). Covers the vanilla blocks {@code VanillaIcons} has no rule for; null while
 * the models are not loaded yet.
 */
public final class VanillaModelIcons {
    private static final Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final ConcurrentHashMap<Long, String> CACHE = new ConcurrentHashMap<>();
    private static final String NONE = "";

    private VanillaModelIcons() {}

    public static String name(BlockState state, int side) {
        if (state == null || state.isAir()) return null;
        long key = (long) net.minecraft.world.level.block.Block.getId(state) << 3 | (side & 7);
        String cached = CACHE.get(key);
        if (cached != null) return cached.isEmpty() ? null : cached;
        var models = Minecraft.getInstance().getModelManager();
        var model = models.getBlockModelShaper().getBlockModel(state);
        if (model == null || model == models.getMissingModel()) return null; // not baked yet: ask again later
        Direction dir = SIDES[Math.max(0, Math.min(5, side))];
        RandomSource random = RandomSource.create(42L);
        var sprite = pick(model.getQuads(state, dir, random, ModelData.EMPTY, null), dir);
        if (sprite == null) sprite = pick(model.getQuads(state, null, random, ModelData.EMPTY, null), dir);
        if (sprite == null) sprite = model.getParticleIcon(ModelData.EMPTY);
        String name = sprite == null ? NONE : org.kylepitcock.legacyconnector.v1_7_10.Assets1710.legacyVanillaTexture(sprite.contents().name(), true);
        CACHE.put(key, name == null ? NONE : name);
        return name == null || name.isEmpty() ? null : name;
    }

    private static final ConcurrentHashMap<Long, String> ITEM_CACHE = new ConcurrentHashMap<>();

    /** The 1.7.10 icon name of a vanilla item's render pass: the sprite of that layer of its modern model (layer 0 if it has fewer). */
    public static String itemName(net.minecraft.world.item.Item item, int pass) {
        if (item == null || item == net.minecraft.world.item.Items.AIR) return null;
        long key = (long) net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(item) << 4 | (pass & 15);
        String cached = ITEM_CACHE.get(key);
        if (cached != null) return cached.isEmpty() ? null : cached;
        var models = Minecraft.getInstance().getModelManager();
        var model = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(item);
        if (model == null || model == models.getMissingModel()) {
            // Not baked yet: the legacy item models are generated before the first bake, and mods name vanilla icons in them
            // (Witchery's brews draw Items.potionitem's render pass 1, the glass bottle, over their liquid). Read the layer
            // from the vanilla item model file instead; not cached, the baked model answers once it exists.
            return fromModelFile(item, pass);
        }
        java.util.List<net.minecraft.client.renderer.texture.TextureAtlasSprite> layers = new java.util.ArrayList<>();
        for (BakedQuad q : model.getQuads(null, null, RandomSource.create(42L), ModelData.EMPTY, null)) {
            if (!layers.contains(q.getSprite())) layers.add(q.getSprite());
        }
        var sprite = layers.isEmpty() ? model.getParticleIcon(ModelData.EMPTY) : layers.get(Math.min(pass, layers.size() - 1));
        String name = sprite == null ? null : org.kylepitcock.legacyconnector.v1_7_10.Assets1710.legacyVanillaTexture(sprite.contents().name(), false);
        ITEM_CACHE.put(key, name == null ? NONE : name);
        return name;
    }

    /**
     * The 1.7.10 icon name of a render pass of a vanilla item from its model file in the vanilla resources ({@code layerN},
     * else {@code layer0}); null for models without layers (block items, builtin models).
     */
    private static String fromModelFile(net.minecraft.world.item.Item item, int pass) {
        try {
            var mc = Minecraft.getInstance();
            if (mc == null || mc.getVanillaPackResources() == null) return null;
            var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (!key.getNamespace().equals("minecraft")) return null;
            var file = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "models/item/" + key.getPath() + ".json");
            var supplier = mc.getVanillaPackResources().getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES, file);
            if (supplier == null) return null;
            com.google.gson.JsonObject json;
            try (var in = new java.io.InputStreamReader(supplier.get(), java.nio.charset.StandardCharsets.UTF_8)) {
                json = com.google.gson.JsonParser.parseReader(in).getAsJsonObject();
            }
            if (!json.has("textures")) return null;
            var textures = json.getAsJsonObject("textures");
            String layer = null;
            for (int p = Math.min(pass, 7); p >= 0 && layer == null; p--) {
                if (textures.has("layer" + p)) layer = textures.get("layer" + p).getAsString();
            }
            if (layer == null) return null;
            var sprite = net.minecraft.resources.ResourceLocation.tryParse(layer.indexOf(':') < 0 ? "minecraft:" + layer : layer);
            return sprite == null ? null : org.kylepitcock.legacyconnector.v1_7_10.Assets1710.legacyVanillaTexture(sprite, false);
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    /** The sprite of the quad facing the side, else of the first quad. */
    private static net.minecraft.client.renderer.texture.TextureAtlasSprite pick(java.util.List<BakedQuad> quads, Direction dir) {
        if (quads.isEmpty()) return null;
        for (BakedQuad q : quads) if (q.getDirection() == dir) return q.getSprite();
        return quads.get(0).getSprite();
    }
}
