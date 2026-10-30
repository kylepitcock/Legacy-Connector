package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import com.google.common.base.Joiner;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 1.12.2 model bakery: its public constants and the item variant registration (the game's own model bakery bakes the models). */
public class ModelBakery {
    public static final ModelResourceLocation MODEL_MISSING = new ModelResourceLocation("builtin/missing", "missing");
    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final Joiner JOINER = Joiner.on(" -> ");
    /** The marker parent of generated item models (item/generated). */
    public static final ModelBlock MODEL_GENERATED = mcc$marker("generation marker");
    /** The marker parent of models drawn by a tile entity item renderer (builtin/entity). */
    public static final ModelBlock MODEL_ENTITY = mcc$marker("block entity marker");
    /** The baked models by location; the game's own model manager holds the real ones (see ModelManager.getModel). */
    protected final legacyconnector.v1_12_2.net.minecraft.util.registry.RegistrySimple<ModelResourceLocation, IBakedModel> bakedRegistry =
            new legacyconnector.v1_12_2.net.minecraft.util.registry.RegistrySimple<>();

    private static ModelBlock mcc$marker(String name) {
        ModelBlock model;
        try {
            model = ModelBlock.deserialize("{\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{\"down\":{\"uv\":[0,0,16,16],\"texture\":\"\"}}}]}");
        } catch (RuntimeException e) {
            model = new ModelBlock(null, new java.util.ArrayList<>(), new java.util.HashMap<>(), true, true, ItemCameraTransforms.DEFAULT, new java.util.ArrayList<>());
        }
        model.name = name;
        return model;
    }

    public static void registerItemVariants(Item item, ResourceLocation... names) {
        legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.registerItemVariants(item, names);
    }
}
