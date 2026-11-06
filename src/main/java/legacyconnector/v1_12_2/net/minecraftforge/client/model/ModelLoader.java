package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import java.util.Map;
import java.util.TreeMap;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.ItemMeshDefinition;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Item model registration: which model file shows an item (and each of its subtypes). An instance is handed to {@code ModelBakeEvent}
 * (a {@link legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelBakery}, as in Forge).
 */
public class ModelLoader extends legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelBakery {
    private static final Map<Item, Map<Integer, ModelResourceLocation>> CUSTOM = new java.util.IdentityHashMap<>();
    private static final Map<Item, ItemMeshDefinition> MESH = new java.util.IdentityHashMap<>();

    public static synchronized void setCustomModelResourceLocation(Item item, int metadata, ModelResourceLocation model) {
        CUSTOM.computeIfAbsent(item, k -> new TreeMap<>()).put(metadata, model);
    }

    private static final Map<legacyconnector.v1_12_2.net.minecraft.block.Block, legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper> STATE_MAPPERS =
            new java.util.IdentityHashMap<>();

    public static synchronized void setCustomStateMapper(legacyconnector.v1_12_2.net.minecraft.block.Block block,
            legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper mapper) {
        STATE_MAPPERS.put(block, mapper);
    }

    /**
     * Forge's field (by delegate), read by reflection: mods (Botania's ModelHandler) look a block's mapper up in it to name its state
     * models. A view of the registered mappers.
     */
    @SuppressWarnings("unused")
    private static final Map<legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<legacyconnector.v1_12_2.net.minecraft.block.Block>,
            legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper> customStateMappers = new java.util.AbstractMap<>() {
        @Override
        public legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper get(Object key) {
            if (!(key instanceof legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<?> d)
                    || !(d.get() instanceof legacyconnector.v1_12_2.net.minecraft.block.Block block)) return null;
            return mcc$stateMapper(block);
        }

        @Override
        public legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper put(
                legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<legacyconnector.v1_12_2.net.minecraft.block.Block> key,
                legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper value) {
            if (key == null || key.get() == null) return null;
            var old = mcc$stateMapper(key.get());
            setCustomStateMapper(key.get(), value);
            return old;
        }

        @Override
        public java.util.Set<Entry<legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<legacyconnector.v1_12_2.net.minecraft.block.Block>,
                legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper>> entrySet() {
            java.util.Set<Entry<legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<legacyconnector.v1_12_2.net.minecraft.block.Block>,
                    legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper>> out = new java.util.LinkedHashSet<>();
            synchronized (ModelLoader.class) {
                for (var e : STATE_MAPPERS.entrySet()) out.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey().delegate, e.getValue()));
            }
            return out;
        }
    };

    /** The mapper a block's state variants come from, or null for the default (property string) one. */
    public static synchronized legacyconnector.v1_12_2.net.minecraft.client.renderer.block.statemap.IStateMapper mcc$stateMapper(legacyconnector.v1_12_2.net.minecraft.block.Block block) {
        return STATE_MAPPERS.get(block);
    }

    public static synchronized void setCustomMeshDefinition(Item item, ItemMeshDefinition meshDefinition) {
        MESH.put(item, meshDefinition);
    }

    private static final Map<Item, java.util.Set<ResourceLocation>> VARIANTS = new java.util.IdentityHashMap<>();

    /** Models an item may show besides its own (ModelBakery.registerItemVariants): loaded with the item's models. */
    public static synchronized void registerItemVariants(Item item, ResourceLocation... names) {
        if (item == null || names == null) return;
        var set = VARIANTS.computeIfAbsent(item, k -> new java.util.LinkedHashSet<>());
        for (ResourceLocation n : names) if (n != null) set.add(n);
    }

    /** The extra models registered for an item. */
    public static synchronized java.util.Set<ResourceLocation> mcc$itemVariants(Item item) {
        var set = VARIANTS.get(item);
        return set == null ? java.util.Set.of() : java.util.Set.copyOf(set);
    }

    /** Every extra item model any item registered (ModelBakery.registerItemVariants), as modern "namespace:item/path" model ids. */
    public static synchronized java.util.Map<String, net.minecraft.resources.ResourceLocation> mcc$allItemVariantModels() {
        java.util.Map<String, net.minecraft.resources.ResourceLocation> out = new java.util.LinkedHashMap<>();
        for (var set : VARIANTS.values()) {
            for (ResourceLocation v : set) {
                String path = v.getPath().toLowerCase(java.util.Locale.ROOT);
                String ns = v.getNamespace().toLowerCase(java.util.Locale.ROOT);
                try {
                    out.put(v.toString(), net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ns, path.startsWith("item/") || path.startsWith("block/") ? path : "item/" + path));
                } catch (RuntimeException e) {
                    // not a valid model name
                }
            }
        }
        return out;
    }

    /** The models an item may show, in registration order (the order {@link #mcc$mesh} results are numbered in). */
    public static synchronized java.util.List<ResourceLocation> mcc$variantList(Item item) {
        var set = VARIANTS.get(item);
        return set == null ? java.util.List.of() : java.util.List.copyOf(set);
    }

    /** The per-stack model chooser a mod gave an item, or null. */
    public static synchronized ItemMeshDefinition mcc$mesh(Item item) {
        return MESH.get(item);
    }

    /** The model a registered item subtype was given, or null. */
    public static synchronized ModelResourceLocation mcc$model(Item item, int meta) {
        var m = CUSTOM.get(item);
        if (m != null && m.get(meta) != null) return m.get(meta);
        ItemMeshDefinition mesh = MESH.get(item);
        if (mesh != null) {
            try {
                return mesh.getModelLocation(new ItemStack(item, 1, meta));
            } catch (RuntimeException | LinkageError e) {
                return null;
            }
        }
        return null;
    }

    /** Sprites by name from the block atlas (the texture getter models are baked with). */
    public static java.util.function.Function<ResourceLocation, legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite> defaultTextureGetter() {
        return location -> legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap.mcc$blocks().getAtlasSprite(location.toString());
    }

    public static ModelResourceLocation getInventoryVariant(String s) {
        if (s.contains("#")) return new ModelResourceLocation(s);
        return new ModelResourceLocation(s, "inventory");
    }

    /** The variant names an item's models were registered under (each subtype's model location). */
    public java.util.List<String> getVariantNames(Item item) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int meta = 0; meta < 16; meta++) {
            ModelResourceLocation m = mcc$model(item, meta);
            if (m != null && !names.contains(m.toString())) names.add(m.toString());
        }
        for (ResourceLocation v : mcc$itemVariants(item)) if (!names.contains(v.toString())) names.add(v.toString());
        return names;
    }

    /** A plain white sprite for untextured quads (Forge's "white" texture). */
    public static final class White extends legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite {
        public static final ResourceLocation LOCATION = new ResourceLocation("white");
        public static final White INSTANCE = new White();

        private White() {
            super(LOCATION.toString());
        }

        public static void register(legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap map) {
            map.setTextureEntry(INSTANCE);
        }
    }

    /**
     * Forge's private map of the IModel behind each state's model location. Mods read it by reflection in ModelBakeEvent (CTM:
     * {@code ReflectionHelper.getPrivateValue(ModelLoader.class, loader, "stateModels")}) to wrap the models that carry connected-texture
     * data; it stays empty here (the game's own bakery owns the models), so those mods find nothing to wrap and the blockstates' plain
     * base models draw (connected textures show their unconnected texture) instead of the handler failing on a null map.
     */
    @SuppressWarnings("unused")
    private final Map<ModelResourceLocation, IModel> stateModels = new java.util.LinkedHashMap<>();

    /**
     * Forge's loader of vanilla (json) models, and the way to the model loader instance: CodeChickenLib's blockstate loader reads
     * {@code VanillaLoader.INSTANCE} and calls {@code getLoader()} by reflection to reach the loader's block model shapes and resource
     * manager. It claims no model location here (the game's own bakery loads the json models).
     */
    public enum VanillaLoader implements ICustomModelLoader {
        INSTANCE;

        private ModelLoader loader;

        public synchronized ModelLoader getLoader() {
            if (loader == null) loader = new ModelLoader();
            return loader;
        }

        @Override
        public boolean accepts(ResourceLocation modelLocation) {
            return false;
        }

        @Override
        public IModel loadModel(ResourceLocation modelLocation) throws Exception {
            return ModelLoaderRegistry.getModel(modelLocation);
        }

        @Override
        public void onResourceManagerReload(legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager resourceManager) {
        }
    }

    /** Forge's records of what failed while baking (private; mods read them by reflection to report broken models: Blood Magic). They stay empty here. */
    @SuppressWarnings("unused")
    private final Map<ModelResourceLocation, Exception> loadingExceptions = new java.util.LinkedHashMap<>();
    @SuppressWarnings("unused")
    private final java.util.Set<ModelResourceLocation> missingVariants = new java.util.LinkedHashSet<>();
    @SuppressWarnings("unused")
    private final com.google.common.collect.SetMultimap<ModelResourceLocation, String> missingTextures = com.google.common.collect.HashMultimap.create();

    /** Forge's multipart model wrapper; mods read its private part list by reflection (CTM). */
    public static class MultipartModel {
        private Map<Object, Object> partModels = new java.util.HashMap<>();
    }

    /** Forge's wrapper of a vanilla model; mods read the wrapped model by reflection (CTM). */
    public class VanillaModelWrapper implements IModel {
        private legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelBlock model;
        private final ResourceLocation location;
        private final boolean uvlock;
        private final legacyconnector.v1_12_2.net.minecraftforge.client.model.animation.ModelBlockAnimation animation;

        /** Forge's (private, inner) constructor: mods reflect it to wrap their own json models (AE2's UVL models). */
        public VanillaModelWrapper(ResourceLocation location, legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelBlock model,
                boolean uvlock, legacyconnector.v1_12_2.net.minecraftforge.client.model.animation.ModelBlockAnimation animation) {
            this.location = location;
            this.model = model;
            this.uvlock = uvlock;
            this.animation = animation;
        }

        @Override
        public java.util.Optional<legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelBlock> asVanillaModel() {
            return java.util.Optional.ofNullable(model);
        }
    }

    /** Forge's loader of plain json models (a singleton with the owning ModelLoader in a private field; AE2's UVL models read it by reflection). */
    public static final class VanillaLoader implements ICustomModelLoader {
        public static final VanillaLoader INSTANCE = new VanillaLoader();
        private ModelLoader loader;

        private VanillaLoader() {
        }

        @Override
        public boolean accepts(ResourceLocation modelLocation) {
            return false;
        }

        @Override
        public IModel loadModel(ResourceLocation modelLocation) {
            return ModelLoaderRegistry.getMissingModel();
        }

        @Override
        public void onResourceManagerReload(legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager resourceManager) {
        }
    }
}
