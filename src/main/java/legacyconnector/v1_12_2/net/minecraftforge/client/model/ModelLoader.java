package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import java.util.Map;
import java.util.TreeMap;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.ItemMeshDefinition;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** Item model registration: which model file shows an item (and each of its subtypes). */
public class ModelLoader {
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

    public static void registerItemVariants(Item item, ResourceLocation... names) {
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

    /** Forge's multipart model wrapper; mods read its private part list by reflection (CTM). */
    public static class MultipartModel {
        private Map<Object, Object> partModels = new java.util.HashMap<>();
    }

    /** Forge's wrapper of a vanilla model; mods read the wrapped model by reflection (CTM). */
    public static class VanillaModelWrapper {
        private Object model;
    }
}
