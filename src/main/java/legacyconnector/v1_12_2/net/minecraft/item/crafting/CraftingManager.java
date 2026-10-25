package legacyconnector.v1_12_2.net.minecraft.item.crafting;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.registry.RegistryNamespaced;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;

/** The recipe registry as 1.12 mods read it. */
public class CraftingManager {
    public static final RegistryNamespaced<ResourceLocation, IRecipe> REGISTRY = Registries1122.view(Registries1122.RECIPES, "air");

    public static IRecipe getRecipe(ResourceLocation name) {
        return REGISTRY.getObject(name);
    }

    public static ResourceLocation getNameForRecipe(IRecipe recipe) {
        return REGISTRY.getNameForObject(recipe);
    }
}
