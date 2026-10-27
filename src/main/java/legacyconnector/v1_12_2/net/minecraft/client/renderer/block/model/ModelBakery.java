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

    public static void registerItemVariants(Item item, ResourceLocation... names) {
        legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader.registerItemVariants(item, names);
    }
}
