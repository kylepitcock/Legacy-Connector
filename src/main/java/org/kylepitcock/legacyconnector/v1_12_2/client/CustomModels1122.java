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
                for (ResourceLocation texture : model.getTextures()) Sprites1122.register(texture.toString());
                for (ResourceLocation dependency : model.getDependencies()) {
                    IModel dep = ModelLoaderRegistry.getModelOrMissing(dependency);
                    for (ResourceLocation texture : dep.getTextures()) Sprites1122.register(texture.toString());
                }
            } catch (RuntimeException | LinkageError e) {
                RenderErrors1122.report("listing the textures of the model " + key, e);
            }
        }
        if (models > 0) LOG.info("{} 1.12.2 model location(s) come from custom model loaders", models);
    }

    /** Bakes the loader models and puts them in place of the game's models of the same locations. */
    static void bakeInto(Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> models) {
        if (!anyLoader()) return;
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

    /** The loader model of a legacy location string ({@code domain:path#variant}), or null when no loader takes it. */
    private static IModel load(String key) {
        ModelResourceLocation location;
        try {
            location = new ModelResourceLocation(key);
        } catch (RuntimeException e) {
            return null;
        }
        if (ModelLoaderRegistry.mcc$loaderFor(location) == null) {
            // 1.12 asked the loaders for the model file a variant points at as well (domain:block/name, domain:item/name)
            return null;
        }
        try {
            return ModelLoaderRegistry.getModel(location);
        } catch (Exception | LinkageError e) {
            RenderErrors1122.report("loading the model " + key, e);
            return null;
        }
    }
}
