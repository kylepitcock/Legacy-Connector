package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.io.Reader;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Forge 1.12's {@code IModel.bake(state, format, textureGetter)} of a json model: the model baked with the sprites the mod's getter gives for
 * the model's textures (EnderIO's paint registry bakes a vanilla wall model with the paint block's sprite for every texture). The game's own
 * baked model of that location has the textures of its json, so this bakes the json again, through the vanilla baking code, with the getter.
 */
public final class JsonBake1122 {
    private JsonBake1122() {}

    /** The model at a modern model location baked with the mod's sprites, or null when it is no plain json model. */
    public static IBakedModel bake(ResourceLocation location, Function<legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation, TextureAtlasSprite> getter) {
        try {
            Map<ResourceLocation, BlockModel> cache = new HashMap<>();
            BlockModel model = load(location, cache);
            if (model == null) return null;
            model.resolveParents(id -> {
                BlockModel parent = load(id, cache);
                return parent == null ? null : (UnbakedModel) parent;
            });
            var sprites = new Function<net.minecraft.client.resources.model.Material, net.minecraft.client.renderer.texture.TextureAtlasSprite>() {
                @Override
                public net.minecraft.client.renderer.texture.TextureAtlasSprite apply(net.minecraft.client.resources.model.Material material) {
                    ResourceLocation texture = material.texture();
                    String path = texture.getPath();
                    if (path.startsWith("block/")) path = "blocks/" + path.substring(6);
                    else if (path.startsWith("item/")) path = "items/" + path.substring(5);
                    TextureAtlasSprite legacy = getter.apply(new legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation(texture.getNamespace(), path));
                    var modern = legacy == null ? null : legacy.mcc$modern();
                    return modern != null ? modern : Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
                }
            };
            ModelBaker baker = (ModelBaker) Proxy.newProxyInstance(JsonBake1122.class.getClassLoader(), new Class<?>[] {ModelBaker.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getModelTextureGetter")) return sprites;
                        if (method.getReturnType() == boolean.class) return false;
                        return null;
                    });
            var baked = model.bake(baker, sprites, BlockModelRotation.X0_Y0);
            return baked == null ? null : Models1122.legacy(baked);
        } catch (RuntimeException | LinkageError e) {
            RenderErrors1122.report("baking the json model " + location + " with a mod's textures", e);
            return null;
        }
    }

    private static BlockModel load(ResourceLocation id, Map<ResourceLocation, BlockModel> cache) {
        if (id.getPath().startsWith("builtin/")) return null;
        BlockModel known = cache.get(id);
        if (known != null) return known;
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(id.withPrefix("models/").withSuffix(".json"));
            if (resource.isEmpty() && "minecraft".equals(id.getNamespace())) {
                // vanilla models renamed since 1.12 (the oak fence gate models were "oak_fence_gate_closed", "oak_wall_gate_open"...)
                String path = id.getPath()
                        .replaceAll("^block/(.+)_fence_gate_closed$", "block/$1_fence_gate")
                        .replaceAll("^block/(.+)_wall_gate_closed$", "block/$1_fence_gate_wall")
                        .replaceAll("^block/(.+)_wall_gate_open$", "block/$1_fence_gate_wall_open");
                if (!path.equals(id.getPath())) resource = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.withDefaultNamespace(path).withPrefix("models/").withSuffix(".json"));
            }
            if (resource.isEmpty()) return null;
            try (Reader reader = resource.get().openAsReader()) {
                BlockModel model = BlockModel.fromStream(reader);
                cache.put(id, model);
                return model;
            }
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }
}
