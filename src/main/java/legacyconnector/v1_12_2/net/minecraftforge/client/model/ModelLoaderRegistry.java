package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ClientRegistrations1122;

/**
 * Forge 1.12.2 model registry. Models come from the mods' custom loaders ({@link #registerLoader}) when one accepts the location;
 * every other location (json and blockstate models) is the model the game baked under it, whose {@link IModel#bake} returns that baked
 * model. Plain model locations asked for here are also given to the game as standalone models, so they are baked from the next resource
 * reload on.
 */
public class ModelLoaderRegistry {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Models1122");
    private static final Map<ResourceLocation, IModel> CACHE = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final IModel MISSING = new GameModel(null);

    public static void registerLoader(ICustomModelLoader loader) {
        if (loader == null) return;
        ClientRegistrations1122.addModelLoader(loader);
        try {
            if (legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft().getResourceManager()
                    instanceof legacyconnector.v1_12_2.net.minecraft.client.resources.IReloadableResourceManager manager) {
                manager.registerReloadListener(loader);
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("Could not register the model loader {} for resource reloads: {}", loader.getClass().getName(), e.toString());
        }
    }

    public static boolean loaded(ResourceLocation location) {
        return CACHE.containsKey(location);
    }

    /** The custom loader that accepts a location, or null. */
    public static ICustomModelLoader mcc$loaderFor(ResourceLocation location) {
        for (Object o : ClientRegistrations1122.modelLoaders()) {
            if (!(o instanceof ICustomModelLoader loader)) continue;
            try {
                if (loader.accepts(location)) return loader;
            } catch (RuntimeException | LinkageError e) {
                LOG.debug("Model loader {} failed on {}: {}", loader.getClass().getName(), location, e.toString());
            }
        }
        return null;
    }

    public static IModel getModel(ResourceLocation location) throws Exception {
        IModel cached = CACHE.get(location);
        if (cached != null) return cached;
        ICustomModelLoader loader = mcc$loaderFor(location);
        IModel model;
        if (loader != null) {
            model = loader.loadModel(location);
            if (model == null) throw new LoaderException("Loader " + loader + " returned null for " + location);
        } else {
            if (!(location instanceof ModelResourceLocation)) REQUESTED.add(location);
            model = new GameModel(location);
        }
        CACHE.put(location, model);
        return model;
    }

    public static IModel getModelOrMissing(ResourceLocation location) {
        try {
            return getModel(location);
        } catch (Exception e) {
            return getMissingModel();
        }
    }

    public static IModel getModelOrLogError(ResourceLocation location, String error) {
        try {
            return getModel(location);
        } catch (Exception e) {
            LOG.error(error, e);
            return getMissingModel();
        }
    }

    public static IModel getMissingModel() {
        return MISSING;
    }

    /** Forgets loaded models (resources were reloaded). */
    public static void clearModelCache(legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager manager) {
        CACHE.clear();
    }

    /** Plain model locations mods asked for, to be baked by the game as standalone models. */
    public static Set<ResourceLocation> mcc$requested() {
        return Set.copyOf(REQUESTED);
    }

    /** The game's missing model, as a legacy baked model. */
    public static IBakedModel mcc$missingBaked() {
        return org.kylepitcock.legacyconnector.v1_12_2.client.Models1122.legacy(net.minecraft.client.Minecraft.getInstance().getModelManager().getMissingModel());
    }

    /** A model the game itself bakes (json model, blockstate variant). */
    private static final class GameModel implements IModel {
        private final ResourceLocation location;

        GameModel(ResourceLocation location) {
            this.location = location;
        }

        @Override
        public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
            if (location == null) return mcc$missingBaked();
            if (location instanceof ModelResourceLocation mrl) {
                var baking = org.kylepitcock.legacyconnector.v1_12_2.client.CustomModels1122.baking();
                if (baking != null) {
                    for (var modern : org.kylepitcock.legacyconnector.v1_12_2.client.Models1122.modernLocations(mrl)) {
                        var found = baking.get(modern);
                        if (found != null) return org.kylepitcock.legacyconnector.v1_12_2.client.Models1122.legacy(found);
                    }
                }
                return org.kylepitcock.legacyconnector.v1_12_2.client.Models1122.legacyModel(mrl);
            }
            var key = net.minecraft.client.resources.model.ModelResourceLocation.standalone(location.mcc$modern());
            var baking = org.kylepitcock.legacyconnector.v1_12_2.client.CustomModels1122.baking();
            var modern = baking != null ? baking.get(key) : null;
            if (modern == null) modern = net.minecraft.client.Minecraft.getInstance().getModelManager().getModel(key);
            return org.kylepitcock.legacyconnector.v1_12_2.client.Models1122.legacy(modern);
        }
    }

    public static class LoaderException extends Exception {
        public LoaderException(String message) {
            super(message);
        }

        public LoaderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
