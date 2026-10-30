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
    public static java.util.List<String> getVariantNames(Item item) {
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

    /** Forge's multipart model wrapper; mods read its private part list by reflection (CTM). */
    public static class MultipartModel {
        private Map<Object, Object> partModels = new java.util.HashMap<>();
    }

    /** Forge's wrapper of a vanilla model; mods read the wrapped model by reflection (CTM). */
    public static class VanillaModelWrapper {
        private Object model;
    }
}
