package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.IModel;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraft.client.resources.model.BakedModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Models of the mods' custom model loaders (OBJ-style loaders, connected textures, multi-layer models...), wired in as 1.12's
 * ModelLoader did: every model location of a 1.12.2 mod block or item that a loader accepts is loaded through it; its textures are
 * stitched into the block atlas, and it is baked and drawn in place of the game's model. Also keeps the models being baked, so
 * {@code IModel.bake} of a game model during ModelBakeEvent sees this bake's models.
 */
public final class CustomModels1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Models1122");
    private static volatile Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> baking;

    private CustomModels1122() {}

    /** The models of the bake in progress (ModelBakeEvent), or null. */
    public static Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> baking() {
        return baking;
    }

    static void setBaking(Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> models) {
        baking = models;
    }

    private static boolean anyLoader() {
        return !org.kylepitcock.legacyconnector.v1_12_2.bridge.ClientRegistrations1122.modelLoaders().isEmpty();
    }

    /** Registers the textures of loader models with the block atlas (run before it is stitched). */
    static void registerTextures() {
        if (!anyLoader()) return;
        int models = 0;
        for (String key : Models1122.allLocations().keySet()) {
            IModel model = load(key);
            if (model == null) continue;
            models++;
            try {
                registerTextures(model, new java.util.HashSet<>());
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("listing the textures of the model " + key, e);
            }
        }
        int variants = 0;
        for (var variant : org.kylepitcock.legacyconnector.v1_12_2.Assets1122.loaderVariants()) {
            IModel model = loadVariant(variant);
            if (model == null) continue;
            variants++;
            try {
                registerTextures(model, new java.util.HashSet<>());
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("listing the textures of the model " + variant.ns() + ":" + variant.path(), e);
            }
        }
        if (models > 0) LOG.info("{} 1.12.2 model location(s) come from custom model loaders", models);
        if (variants > 0) LOG.info("{} 1.12.2 block variant(s) are models of custom model loaders", variants);
    }

    /** The textures of a model and of all its dependencies (asked of the registry, so plain ones are baked as standalone models). */
    private static void registerTextures(IModel model, java.util.Set<IModel> seen) {
        if (!seen.add(model)) return;
        for (ResourceLocation texture : model.getTextures()) Sprites1122.register(texture.toString());
        for (ResourceLocation dependency : model.getDependencies()) registerTextures(ModelLoaderRegistry.getModelOrMissing(dependency), seen);
    }

    /**
     * The loader model of a blockstate variant, set up as Forge's variant loader did: the loader's model, retextured with the variant's
     * textures, given the variant's custom data (a map of JSON strings).
     */
    private static IModel loadVariant(org.kylepitcock.legacyconnector.v1_12_2.Assets1122.LoaderVariant variant) {
        String key = variant.ns() + ":models/" + variant.path();
        try {
            IModel model = ModelLoaderRegistry.getModel(new ResourceLocation(variant.ns(), "models/" + variant.path()));
            var source = variant.source();
            var textures = com.google.common.collect.ImmutableMap.<String, String>builder();
            if (source.has("textures") && source.get("textures").isJsonObject()) {
                for (var t : source.getAsJsonObject("textures").entrySet()) {
                    if (t.getValue().isJsonPrimitive()) textures.put(t.getKey(), t.getValue().getAsString());
                }
            }
            var retexture = textures.build();
            if (!retexture.isEmpty()) model = model.retexture(retexture);
            var custom = com.google.common.collect.ImmutableMap.<String, String>builder();
            if (source.has("custom") && source.get("custom").isJsonObject()) {
                for (var c : source.getAsJsonObject("custom").entrySet()) custom.put(c.getKey(), c.getValue().toString());
            }
            var customData = custom.build();
            if (!customData.isEmpty()) model = model.process(customData);
            return model;
        } catch (Exception | LinkageError e) {
            RenderErrors1122.report("loading the model " + key, e);
            return null;
        }
    }

    /** Bakes the loader models of the block variants no json describes, as the models of their metadata states. */
    private static void bakeVariants(Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> models) {
        for (var variant : org.kylepitcock.legacyconnector.v1_12_2.Assets1122.loaderVariants()) {
            IModel model = loadVariant(variant);
            if (model == null) continue;
            try {
                if (!(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(variant.block()) instanceof
                        org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block block)) continue;
                IBakedModel baked = model.bake(model.getDefaultState(), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter());
                if (baked == null) continue;
                var state = block.defaultBlockState().setValue(org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block.META, variant.meta());
                models.put(net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state), Models1122.modern(baked));
            } catch (RuntimeException | LinkageError ex) {
                LOG.warn("Could not bake the loader model {}:{} of {} meta {} ({}): {}", variant.ns(), variant.path(), variant.block(), variant.meta(),
                        variant.source(), ex.toString(), ex);
            }
        }
    }

    /** Bakes the loader models and puts them in place of the game's models of the same locations. */
    static void bakeInto(Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> models) {
        if (!anyLoader()) return;
        bakeVariants(models);
        for (var e : Models1122.allLocations().entrySet()) {
            IModel model = load(e.getKey());
            if (model == null) continue;
            try {
                IBakedModel baked = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
                if (baked == null) continue;
                BakedModel modern = Models1122.modern(baked);
                for (var location : e.getValue()) models.put(location, modern);
            } catch (RuntimeException | LinkageError ex) {
                RenderErrors1122.report("baking the model " + e.getKey(), ex);
            }
        }
    }

    /**
     * The original parent of a plain json model (the loader keeps a parent no valid id can hold under {@code legacyconnector:legacy_parent}) when
     * a custom loader accepts it, or null.
     */
    private static ResourceLocation loaderParent(ResourceLocation location) {
        if (location instanceof ModelResourceLocation) return null;
        String path = location.getPath();
        if (!path.startsWith("models/")) return null;
        try {
            var resource = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation(location.getNamespace(), path.endsWith(".json") ? path : path + ".json"));
            String raw;
            try (java.io.Reader reader = new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
                var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                var value = json.get("legacyconnector:legacy_parent");
                if (value == null || !value.isJsonPrimitive()) return null;
                raw = value.getAsString();
            }
            ResourceLocation parent = new ResourceLocation(raw);
            String parentPath = parent.getPath();
            ResourceLocation actual = parentPath.startsWith("models/") ? parent : new ResourceLocation(parent.getNamespace(), "models/" + parentPath);
            return ModelLoaderRegistry.mcc$loaderFor(actual) != null ? parent : null;
        } catch (java.io.IOException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /**
     * The loader model of a block's {@code inventory} blockstate variant ({@code "inventory": [{}]} with {@code defaults.model} a loader file such as
     * Mekanism's glow_panel.obj.mek) for an item registered at {@code mod:block#inventory} that has no item json: Forge loaded that variant for the
     * item. Without it the item drew the plain converted model and lost the loader model's own item transforms (Mekanism's glow panels).
     */
    private static ResourceLocation inventoryVariantModel(String key) {
        try {
            ModelResourceLocation mrl = new ModelResourceLocation(key);
            if (!"inventory".equals(mrl.getVariant())) return null;
            var resource = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation(mrl.getNamespace(), "blockstates/" + mrl.getPath() + ".json"));
            try (java.io.Reader reader = new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
                var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                var variants = json.has("variants") && json.get("variants").isJsonObject() ? json.getAsJsonObject("variants") : null;
                if (variants == null || !variants.has("inventory")) return null;
                String model = null;
                var inv = variants.get("inventory");
                if (inv.isJsonArray() && inv.getAsJsonArray().size() > 0) inv = inv.getAsJsonArray().get(0);
                if (inv.isJsonObject() && inv.getAsJsonObject().has("model")) model = inv.getAsJsonObject().get("model").getAsString();
                if (model == null && json.has("defaults") && json.get("defaults").isJsonObject() && json.getAsJsonObject("defaults").has("model")) {
                    model = json.getAsJsonObject("defaults").get("model").getAsString();
                }
                if (model == null) return null;
                ResourceLocation parsed = new ResourceLocation(model);
                // a blockstate's models live under models/block/ (the variant loader's folder), unless the name already gives a folder
                String path = parsed.getPath();
                if (!path.startsWith("block/") && !path.startsWith("item/")) path = "block/" + path;
                ResourceLocation actual = new ResourceLocation(parsed.getNamespace(), "models/" + path);
                return ModelLoaderRegistry.mcc$loaderFor(actual) != null ? actual : null;
            }
        } catch (java.io.IOException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** The loader model of a legacy location string ({@code domain:path#variant}), or null when no loader takes it. */
    private static IModel load(String key) {
        ResourceLocation location;
        ResourceLocation fallback = null;
        try {
            ModelResourceLocation mrl = new ModelResourceLocation(key);
            location = mrl;
            if ("inventory".equals(mrl.getVariant())) {
                // an item's model: Forge loaded the file models/item/<path> (ModelLoader.getItemLocation, then ModelLoaderRegistry's actual location)
                // and gave that plain location to the custom loaders (Tinkers' Construct reads its .tmat/.tcon json from it)
                String path = mrl.getPath();
                ResourceLocation itemLocation = new ResourceLocation(mrl.getNamespace(), path.startsWith("models/") ? path : "models/item/" + path);
                if (ModelLoaderRegistry.mcc$loaderFor(mrl) == null) {
                    location = itemLocation;
                } else if (ModelLoaderRegistry.mcc$loaderFor(itemLocation) != null) {
                    // a loader that accepts by file extension takes both (Tinkers' .tmat: the MRL has no json to read): the item json first, then
                    // the MRL itself, which a loader that accepts "mod:name" by its plain path (Lycanites' equipment parts) is given
                    location = itemLocation;
                    fallback = mrl;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        if (ModelLoaderRegistry.mcc$loaderFor(location) == null) {
            // a json model whose parent a loader makes ("parent": "mod:item/internal/x$from_item(mod:x)"): Forge loaded parents through the
            // loaders too, so the item draws the loader's model (the modern bakery cannot, the name is no valid id)
            ResourceLocation parent = loaderParent(location);
            if (parent == null) parent = inventoryVariantModel(key);
            if (parent == null) return null;
            location = parent;
        }
        try {
            IModel model = ModelLoaderRegistry.getModel(location);
            if (fallback != null && model == ModelLoaderRegistry.getMissingModel()) return ModelLoaderRegistry.getModel(fallback);
            return model;
        } catch (Exception | LinkageError e) {
            if (fallback != null) {
                try {
                    return ModelLoaderRegistry.getModel(fallback);
                } catch (Exception | LinkageError e2) {
                    // reported below with the first failure
                }
            }
            RenderErrors1122.report("loading the model " + key, e);
            return null;
        }
    }
}
